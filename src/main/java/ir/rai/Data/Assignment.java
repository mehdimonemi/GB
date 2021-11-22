package ir.rai.Data;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import ir.rai.GTip;
import ir.rai.Section;
import javafx.collections.ObservableList;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static ir.rai.PrimaryController.namesCheck;
import static ir.rai.PrimaryController.specialCorridors;

public class Assignment {
    public static XSSFRow row;

    public ArrayList<Station> stations = null;
    public static ArrayList<Block> blocks = null;
    public ArrayList<Block> outputBlocks = null;
    Commodity commodity = null;
    public TreeSet<String> districts = null;
    public HashSet<String> wagons = null;
    public HashSet<String> mainCargoTypes = null;
    HashSet<String> cargoTypes = null;
    HashSet<String> transportKinds = null;

    public static XSSFCell cell;

    public static ArrayList<Commodity> solutions;
    public static ArrayList<Block> stackForRemoveBlocks;
    public static boolean isFinished = false;
    public static Commodity previousCommodity = new Commodity();

    public static ArrayList<GTip> gTips = new ArrayList<>();
    public static ArrayList<Section> pathSection = null;
    public static ArrayList<String> pathDistricts = null;

    public Assignment() {
        readData("./Data.xlsx");
        readGabari("./gabari.xlsx");
    }

    public LinkedHashMap<String, Integer> main(String origin, String destination,
                                               ObservableList<String> selectedCorridors) {
        commodity = new Commodity(origin, destination, stations);
        resetCommoditiesResult();
        LinkedHashMap<String, Integer> gabariSections = null;
        try {
            IloCplex model = new IloCplex();
            ArrayList<Corridor> corridors = new ArrayList<>();
            for (String path : selectedCorridors) {
                for (Corridor corridor : specialCorridors) {
                    if (corridor.toString().equals(path)) {
                        corridors.add(corridor);
                    }
                }
            }

            solutions = new ArrayList<>();
            if (corridors.size() != 0) {
                mainLoop:
                for (int x = 0; x < Math.pow(2, corridors.size()); x++) {
                    ArrayList<Block> specialBlocks = new ArrayList<>();
                    for (int i = 0; i < corridors.size(); i++) {
                        Commodity path1;
                        if (corridors.get(i).isSingleBlock()) {
                            if ((x >> i) % 2 == 0)
                                specialBlocks.add(Block.get(corridors.get(i).getOrigin(), corridors.get(i).getDestination()));
                            else
                                specialBlocks.add(Block.get(corridors.get(i).getDestination(), corridors.get(i).getOrigin()));

                        } else {
                            if ((x >> i) % 2 == 0)
                                path1 = new Commodity(corridors.get(i).getRealOrigin(), corridors.get(i).getRealDestination(),
                                        stations);
                            else
                                path1 = new Commodity(corridors.get(i).getRealDestination(), corridors.get(i).getRealOrigin(),
                                        stations);

                            ArrayList<Block> temp = new ArrayList<>(findBlocks(blocks, stations,
                                    path1, path1.getOriginId(), path1.getDestinationId(),
                                    path1.getOrigin(), path1.getDestination(), model, new ArrayList<>(),
                                    new Block(), true));
                            if (temp.size() == 0)
                                continue mainLoop;
                            specialBlocks.addAll(temp);
                        }
                    }

                    previousCommodity = new Commodity();
                    Commodity commodity1 = new Commodity(origin, destination, stations);
                    stackForRemoveBlocks = new ArrayList<>();

                    //Recursion method
                    findBlocks(blocks, stations, commodity1,
                            commodity1.getOriginId(), commodity1.getDestinationId(), commodity1.getOrigin(),
                            commodity1.getDestination(), model, specialBlocks, new Block(), false);
                }
            } else {
                findBlocks(blocks, stations, commodity,
                        commodity.getOriginId(), commodity.getDestinationId(), commodity.getOrigin(),
                        commodity.getDestination(), model, new ArrayList<>(), new Block(), false);
            }
            Commodity selectedCommodity = minSolution(solutions);
            gabariSections = new LinkedHashMap<>(Objects.requireNonNull(getGabariSections(selectedCommodity)));
            pathSection = new ArrayList<>(getPathSections(selectedCommodity));
            pathDistricts = new ArrayList<>(getPathDistricts(selectedCommodity));
        } catch (IloException | NullPointerException e) {
            e.printStackTrace();
        }
        System.gc();
        return gabariSections;
    }

    private ArrayList<String> getPathDistricts(Commodity selectedCommodity) {
        ArrayList<String> results = new ArrayList<>();
        for (Block block : selectedCommodity.getBlocks()) {
            if (!results.contains(block.getDistrict())) {
                results.add(block.getDistrict());
            }
        }
        return results;
    }

    private ArrayList<Section> getPathSections(Commodity selectedCommodity) {
        ArrayList<Section> results = new ArrayList<>();
        String origin = selectedCommodity.getOrigin();
        String destination = "";
        for (Block block : selectedCommodity.getBlocks()) {
            if (isMultiBlock(block.getDestinationId())) {
                destination = block.getDestination();
                results.add(new Section(origin, destination));
                origin = destination;
            }
        }
        if (!destination.equals(selectedCommodity.getDestination()))
            results.add(new Section(destination, selectedCommodity.getDestination()));

        return results;
    }

    private boolean isMultiBlock(int stationId) {
        int connection = 0;
        for (Block block : blocks) {
            if (block.getOriginId() == stationId) {
                connection++;
            }
            if (connection >= 3) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<Block> findBlocks(ArrayList<Block> blocks, ArrayList<Station> stations,
                                        Commodity commodity, int stationA, int stationB, String a,
                                        String b, IloCplex model, ArrayList<Block> exceptions,
                                        Block removeBlock, boolean isCorridor) {
        try {
            IloNumVar[] X = new IloNumVar[blocks.size()];
            IloNumExpr goalFunction;
            IloNumExpr constraint;
            //start solving model for the commodity
            //masirhai estesna baiad az masir khas beran
            for (int j = 0; j < blocks.size(); j++) {
                boolean flag = true;
                for (Block block : exceptions) {
                    if (block.equals(blocks.get(j))) {
                        X[j] = model.numVar(1, 1, IloNumVarType.Int);
                        if (blocks.get(j).getOriginId() == blocks.get(j - 1).getDestinationId() &&
                                blocks.get(j).getDestinationId() == blocks.get(j - 1).getOriginId()) {
                            X[j - 1] = model.numVar(0, 0, IloNumVarType.Int);
                        } else if (j + 1 < blocks.size()) {
                            if (blocks.get(j).getOriginId() == blocks.get(j + 1).getDestinationId() &&
                                    blocks.get(j).getDestinationId() == blocks.get(j + 1).getOriginId()) {
                                X[j + 1] = model.numVar(0, 0, IloNumVarType.Int);
                                j++;//add to counter
                            }
                        }
                        flag = false;
                    }
                }
                if (flag) {
                    X[j] = model.numVar(0, 1, IloNumVarType.Int);
                }
                if (blocks.get(j).equals(removeBlock)) {
                    X[j] = model.numVar(0, 0, IloNumVarType.Int);
                }
            }

            goalFunction = model.constant(0);
            for (int i = 0; i < blocks.size(); i++) {
                goalFunction = model.sum(goalFunction, model.prod(X[i], blocks.get(i).getLength()));
            }
            model.addMinimize(goalFunction);

            // constraints
            for (Station station : stations) {
                constraint = model.constant(0);
                if (station.getId() == stationA) {
                    for (int j = 0; j < blocks.size(); j++) {
                        if (stationA == blocks.get(j).getOriginId()) {
                            constraint = model.sum(constraint, X[j]);
                        }
                        if (stationA == blocks.get(j).getDestinationId()) {
                            constraint = model.sum(constraint, model.negative(X[j]));
                        }
                    }
                    model.addEq(constraint, 1);
                } else if (station.getId() == (stationB)) {
                    for (int j = 0; j < blocks.size(); j++) {
                        if (stationB == blocks.get(j).getOriginId()) {
                            constraint = model.sum(constraint, X[j]);
                        }
                        if (stationB == blocks.get(j).getDestinationId()) {
                            constraint = model.sum(constraint, model.negative(X[j]));
                        }
                    }
                    model.addEq(constraint, -1);
                } else {
                    for (int j = 0; j < blocks.size(); j++) {
                        if (station.getId() == (blocks.get(j).getOriginId())) {
                            constraint = model.sum(constraint, X[j]);
                        }
                        if (station.getId() == (blocks.get(j).getDestinationId())) {
                            constraint = model.sum(constraint, model.negative(X[j]));
                        }
                    }
                    model.addEq(constraint, 0);
                }
            } // end of constraints

            boolean flagForRemove = false;
            ArrayList<Block> tempBlocks = new ArrayList<>();
            model.setOut(null);
            if (model.solve()) {
                commodity.setDistance(model.getObjValue());
                commodity.setTonKilometer(model.getObjValue() * commodity.getTon());
                for (int i = 0; i < blocks.size(); i++) {
                    if (model.getValue(X[i]) > 0.5) {
                        commodity.getBlocks().add(blocks.get(i));
                    }
                }

                //sort blocks
                String tempOrigin = a;
                tempBlocks = new ArrayList<>();
                ArrayList<Block> commodityBlocks = new ArrayList<>(commodity.getBlocks());

                long stopTime = System.currentTimeMillis() + 500;
                try {
                    while (!commodityBlocks.isEmpty()) {
                        for (Block block : commodityBlocks) {
                            if (block.getOrigin().equals(tempOrigin)) {
                                tempBlocks.add(block);
                                tempOrigin = block.getDestination();
                                commodityBlocks.remove(block);
                                break;
                            }
                        }
                        if (System.currentTimeMillis() > stopTime) throw new Exception();
                    }
                } catch (Exception e) {
                    if (previousCommodity.getBlocks().size() > 0)
                        commodity = previousCommodity;

                    flagForRemove = true;
                } finally {
                    model.clearModel();
                    for (int i = 0; i < blocks.size(); i++) {
                        if (X[i] != null) {
                            X[i] = null;
                        }
                    }
                    goalFunction = null;
                    constraint = null;
                }
            } else {
                commodity.setDistance(Double.MAX_VALUE);
                commodity.setBlocks(new ArrayList<>());
                if (exceptions.size() > 0) {
                    if (previousCommodity.getBlocks().size() > 0)
                        commodity = previousCommodity;
                    flagForRemove = true;
                }
                model.clearModel();
            }
            if (flagForRemove || exceptions.size() > 0) {
                if (!flagForRemove) {
                    //finally, if blocks are sorted we consider the solution possible
                    commodity.setBlocks(tempBlocks);
                    previousCommodity = commodity;
                    solutions.add(commodity);
                }
                boolean flag = false;
                for (int i = 0; i < commodity.getBlocks().size(); i++) {
                    if (!stackForRemoveBlocks.contains(commodity.getBlocks().get(i))
                            && !exceptions.contains(commodity.getBlocks().get(i))) {
                        removeBlock = commodity.getBlocks().get(i);
                        stackForRemoveBlocks.add(commodity.getBlocks().get(i));
                        flag = true;
                        break;
                    }
                }
                if (flag)
                    findBlocks(blocks, stations, new Commodity(a, b, stations),
                            commodity.getOriginId(), commodity.getDestinationId(), commodity.getOrigin(),
                            commodity.getDestination(), model, exceptions, removeBlock, true);
                else return commodity.getBlocks();
            } else if (!isCorridor) {
                commodity.setBlocks(tempBlocks);
                solutions.add(commodity);
            }
        } catch (IloException e) {
            commodity.setDistance(Double.MAX_VALUE);
            commodity.setBlocks(new ArrayList<>());
            System.out.println("Problem in cplex library");
            return new ArrayList<Block>();
        }
        return commodity.getBlocks();
    }

    private Commodity minSolution(ArrayList<Commodity> solutions) {
        Commodity min = new Commodity();
        min.setDistance(Double.MAX_VALUE);
        for (Commodity commodity : solutions) {
            if (commodity.getDistance() < min.getDistance()) {
                min = commodity;
            }
        }
        return min;
    }

    void resetCommoditiesResult() {
        commodity.setHowMuchIsAllowed(1);
        commodity.setDistance(0);
        commodity.setTonKilometer(0);
        commodity.setBlocks(new ArrayList<>());

        for (Block block : blocks) {
            block.setDemandWentPlanTon(0);
            block.setDemandWentOperationTon(0);
            block.setDemandWentPlanWagon(0);
            block.setDemandWentOperatoinWagon(0);
            block.setDemandBackPlanTon(0);
            block.setDemandBackOperationTon(0);
            block.setDemandBackPlanWagon(0);
            block.setDemandBackOperationWagon(0);
            block.setDemandWentPlanTonKilometer(0);
            block.setDemandBackPlanTonKilometer(0);
            block.setAverageMovingDistance(0);
        }
    }

    public static LinkedHashMap<String, Integer> getGabariSections(Commodity commodity) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        String temp1 = commodity.getBlocks().get(0).getOrigin();
        String temp2 = commodity.getBlocks().get(0).getDestination();
        int temp3 = commodity.getBlocks().get(0).getGabariCode();
        for (Block block : commodity.getBlocks()) {
            if (block.getGabariCode() != temp3) {
                result.put(temp1 + " - " + temp2, temp3);
                temp1 = block.getOrigin();
                temp2 = block.getDestination();
                temp3 = block.getGabariCode();
            } else {
                temp2 = block.getDestination();
            }
        }
        result.put(temp1 + " - " + temp2, temp3);
        return result;
    }

    public void readData(String outPutFile) {
        stations = new ArrayList<>();
        blocks = new ArrayList<>();
        outputBlocks = new ArrayList<>();
        districts = new TreeSet();
        wagons = new HashSet();
        mainCargoTypes = new HashSet();
        cargoTypes = new HashSet();
        transportKinds = new HashSet();

        FileInputStream dataFile = null;
        XSSFWorkbook data = null;
        try {
            dataFile = new FileInputStream(outPutFile);
            data = new XSSFWorkbook(dataFile);
            // read stations data
            XSSFSheet sheet1 = data.getSheetAt(0);
            for (int i = 0; i < sheet1.getLastRowNum(); i++) {
                XSSFRow row = sheet1.getRow(i + 1);

                Station station = new Station((int) row.getCell(0).getNumericCellValue(),
                        row.getCell(1).getStringCellValue(),
                        row.getCell(2).getBooleanCellValue(),
                        row.getCell(3).getStringCellValue());
                if (!row.getCell(3).getStringCellValue().equals("null"))
                    districts.add(row.getCell(3).getStringCellValue());

                for (int j = 5; j < row.getLastCellNum(); j++) {
                    station.getAlterNames().add(row.getCell(j).getStringCellValue());
                }
                stations.add(station);
            }

            // read blocks data
            XSSFSheet sheet2 = data.getSheetAt(1);
            for (int i = 0; i < sheet2.getLastRowNum(); i++) {
                XSSFRow row = sheet2.getRow(i + 1);
                //raft
                Block block1 = new Block((int) row.getCell(0).getNumericCellValue(),
                        row.getCell(1).getStringCellValue(),
                        row.getCell(2).getStringCellValue(),
                        row.getCell(3).getStringCellValue(),
                        row.getCell(4).getNumericCellValue(),
                        (int) row.getCell(5).getNumericCellValue(),
                        1,
                        row.getCell(8).getNumericCellValue(),
                        stations, (int) row.getCell(11).getNumericCellValue());
                blocks.add(block1);

                //bargasht
                Block block2 = new Block((int) row.getCell(0).getNumericCellValue(),
                        row.getCell(1).getStringCellValue(),
                        row.getCell(3).getStringCellValue(),
                        row.getCell(2).getStringCellValue(),
                        row.getCell(4).getNumericCellValue(),
                        (int) row.getCell(5).getNumericCellValue(),
                        2,
                        ((row.getCell(5).getNumericCellValue() == 2) ? row.getCell(10).getNumericCellValue() : 0),
                        stations, (int) row.getCell(11).getNumericCellValue());
                blocks.add(block2);
            }

            //read special corridor
            XSSFSheet sheet4 = data.getSheetAt(2);
            for (int j = 1; j < sheet4.getLastRowNum(); j++) {
                XSSFRow row = sheet4.getRow(j + 1);

                if (row.getPhysicalNumberOfCells() >= 3) {
                    if (row.getCell(2).getStringCellValue().equals("یک بلاک"))
                        specialCorridors.add(new Corridor(
                                row.getCell(0).getStringCellValue(),
                                row.getCell(1).getStringCellValue(),
                                row.getCell(2).getStringCellValue()
                        ));
                    else
                        specialCorridors.add(new Corridor(
                                row.getCell(0).getStringCellValue(),
                                row.getCell(2).getStringCellValue(),
                                row.getCell(1).getStringCellValue(),
                                row.getCell(3).getStringCellValue())
                        );
                } else
                    specialCorridors.add(new Corridor(row.getCell(0).getStringCellValue(),
                            row.getCell(1).getStringCellValue()));
            }


            XSSFSheet sheet5 = data.getSheetAt(3);
            for (int i = 0; i < sheet5.getLastRowNum(); i++) {
                XSSFRow row = sheet5.getRow(i + 1);
                Block outputBlock = new Block(row.getCell(2).getStringCellValue(),
                        row.getCell(3).getStringCellValue(),
                        stations);
                outputBlocks.add(outputBlock);
            }

            stations.trimToSize();
            blocks.trimToSize();

        } catch (IOException | NullPointerException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void updateAlterNames(String[] result, XSSFWorkbook workBook) {

        String correctOriginName = findName(result[1])[0];
        String correctDestinationName = findName(result[2])[0];
        try {

            XSSFSheet sheet1 = workBook.getSheetAt(0);

            //update excel
            for (int i = 0; i < sheet1.getLastRowNum(); i++) {
                boolean finishedWithOrigin = false;
                boolean finishedWithDestination = false;
                XSSFRow row = sheet1.getRow(i + 1);
                if (row.getCell(1).getStringCellValue().equals(correctOriginName)) {
                    boolean check = true;
                    for (int j = 5; j < row.getLastCellNum(); j++) {
                        if (result[2].equals(row.getCell(j).getStringCellValue()))
                            check = false;
                    }
                    if (check) {
                        row.createCell(row.getLastCellNum()).setCellValue(result[3]);
                    }
                    finishedWithOrigin = true;
                }
                if (row.getCell(1).getStringCellValue().equals(correctDestinationName)) {
                    boolean check = true;
                    for (int j = 5; j < row.getLastCellNum(); j++) {
                        if (result[3].equals(row.getCell(j).getStringCellValue()))
                            check = false;
                    }
                    if (check) {
                        row.createCell(row.getLastCellNum()).setCellValue(result[3]);
                    }
                    finishedWithDestination = true;
                }
                if (finishedWithDestination && finishedWithOrigin)
                    break;
            }

            //station names
            for (Station station : stations) {
                if (station.getName().equals(correctOriginName)) {
                    boolean check = true;
                    for (String name : station.getAlterNames()) {
                        if (result[2].equals(name)) {
                            check = false;
                            break;
                        }
                    }
                    if (check) {
                        station.getAlterNames().add(result[2]);
                        break;
                    }
                }
                if (station.getName().equals(correctDestinationName)) {
                    boolean check = true;
                    for (String name : station.getAlterNames()) {
                        if (result[3].equals(name)) {
                            check = false;
                            break;
                        }
                    }
                    if (check) {
                        station.getAlterNames().add(result[3]);
                        break;
                    }
                }
            }
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public String[] findName(String stringCellValue) {
        for (Station station : stations) {
            for (String name : station.getAlterNames()) {
                if (name.equals(stringCellValue)) {
                    return new String[]{station.getName(), String.valueOf(station.getId())};
                }
            }
        }
        return null;
    }

    private boolean nameIsNotOkay(String stringCellValue) {
        for (Station station : stations) {
            for (String name : station.getAlterNames()) {
                if (name.equals(stringCellValue))
                    return false;
            }
        }
        return true;
    }

    public static String districtOf(String station, ArrayList<Station> stations) {
        for (Station station1 : stations) {
            if (station1.getName().equals(station)) {
                return station1.getDistrict();
            }
        }
        return "";
    }

    public void readGabari(String file) {
        FileInputStream inFile;
        XSSFWorkbook workbook;
        try {
            inFile = new FileInputStream(file);
            workbook = new XSSFWorkbook(inFile);

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                GTip gTip = new GTip();
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                gTip.setName(sheet.getRow(1).getCell(2).getStringCellValue());

                XSSFRow row;
                int freeSpaceCorners = 0;
                do {
                    freeSpaceCorners++;
                    row = sheet.getRow(freeSpaceCorners + 4);
                } while (row != null && row.getCell(0) != null);


                int allowedSpaceCorners = 0;
                do {
                    allowedSpaceCorners++;
                    row = sheet.getRow(allowedSpaceCorners + 4);
                } while (row != null && row.getCell(4) != null);

                gTip.setFreeSpace(new float[2 * freeSpaceCorners + 1][2]);
                gTip.setAllowedSpace(new float[2 * allowedSpaceCorners + 1][2]);

                for (int i = 0; i < freeSpaceCorners; i++) {
                    row = sheet.getRow((i) + 4);
                    gTip.getFreeSpace()[i][0] =
                            (float) row.getCell(1).getNumericCellValue();
                    gTip.getFreeSpace()[i][1] =
                            (float) (row.getCell(0).getNumericCellValue());

                    if (i == 0) {
                        gTip.getFreeSpace()[2 * freeSpaceCorners][0] =
                                (float) row.getCell(1).getNumericCellValue();
                        gTip.getFreeSpace()[2 * freeSpaceCorners][1] =
                                (float) (row.getCell(0).getNumericCellValue());
                    }

                    gTip.getFreeSpace()[2 * freeSpaceCorners - i - 1][0] =
                            (float) row.getCell(2).getNumericCellValue();
                    gTip.getFreeSpace()[2 * freeSpaceCorners - i - 1][1] =
                            (float) (row.getCell(0).getNumericCellValue());
                }

                for (int i = 0; i < allowedSpaceCorners; i++) {
                    row = sheet.getRow((i) + 4);
                    gTip.getAllowedSpace()[i][0] =
                            (float) row.getCell(5).getNumericCellValue();
                    gTip.getAllowedSpace()[i][1] =
                            (float) (row.getCell(4).getNumericCellValue());

                    if (i == 0) {
                        gTip.getAllowedSpace()[2 * allowedSpaceCorners][0] =
                                (float) row.getCell(5).getNumericCellValue();
                        gTip.getAllowedSpace()[2 * allowedSpaceCorners][1] =
                                (float) (row.getCell(4).getNumericCellValue());
                    }

                    gTip.getAllowedSpace()[2 * allowedSpaceCorners - i - 1][0] =
                            (float) row.getCell(6).getNumericCellValue();
                    gTip.getAllowedSpace()[2 * allowedSpaceCorners - i - 1][1] =
                            (float) (row.getCell(4).getNumericCellValue());
                }
                gTips.add(gTip);
            }
            System.out.println();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] manageNames(String[] result) {

        FileOutputStream file = null;
        XSSFWorkbook data = null;
        try (FileInputStream dataFile = new FileInputStream("./Data.xlsx")) {
            data = new XSSFWorkbook(dataFile);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        try {
            if (nameIsNotOkay(result[0]) || nameIsNotOkay(result[1])) {
                namesCheck = false;
                return result;
            }

            result[0] = findName(result[0])[0];
            result[1] = findName(result[1])[0];
            if (!namesCheck) {
                //we update alter names if only the station name is not duplicate (special)
                if (!stations.get(Integer.parseInt(findName(result[0])[1])).isSpecialTag() ||
                        !stations.get(Integer.parseInt(findName(result[1])[1])).isSpecialTag()
                ) {
                    updateAlterNames(result, data);
                    (new File("./Data.xlsx")).delete();
                    file = new FileOutputStream("./Data.xlsx");
                    data.write(file);
                    file.flush();
                    file.close();
                    namesCheck = true;
                }
            }
        } catch (IOException | NullPointerException |
                IllegalStateException e) {
            namesCheck = false;
        } finally {
            if (data != null) {
                try {
                    data.close();
                } catch (IOException e) {
                    namesCheck = false;
                }
            }
        }
        return result;
    }

}


