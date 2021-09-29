package ir.rai.Data;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.CpxException;
import ilog.cplex.IloCplex;
import ir.rai.GTip;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class Assignment {
    public static XSSFRow row;

    public ArrayList<Station> stations = null;
    public ArrayList<Block> blocks = null;
    public ArrayList<Block> outputBlocks = null;
    Commodity commodity = null;
    public PathExceptions pathExceptions = null;
    public TreeSet<String> districts = null;
    public HashSet<String> wagons = null;
    public HashSet<String> mainCargoTypes = null;
    HashSet<String> cargoTypes = null;
    HashSet<String> transportKinds = null;

    public static XSSFCell cell;

    public static ArrayList<GTip> gTips = new ArrayList<>();

    public Assignment() {
        readData("./Data.xlsx");
        readGabari("./gabari.xlsx");
    }

    public LinkedHashMap<String, Integer> main(String origin, String destination) {
        commodity = new Commodity(origin, destination, stations);
        resetCommoditiesResult();
        return findPaths(commodity);
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

    public LinkedHashMap<String, Integer> findPaths(Commodity commodity) {
        LinkedHashMap<String, Integer> gabariResult = null;
        try {
            IloCplex model = new IloCplex();
            int stationA = commodity.getOriginId();
            int stationB = commodity.getDestinationId();
            String a = commodity.getOrigin();
            String b = commodity.getDestination();

            gabariResult = new LinkedHashMap<>(Objects.requireNonNull(doModel(blocks, pathExceptions,
                    stations, commodity, stationA, stationB, a, b, model)));

        } catch (IloException e) {
            e.printStackTrace();
        }
        System.gc();
        return gabariResult;
    }

    public static LinkedHashMap<String, Integer> doModel(ArrayList<Block> blocks, PathExceptions pathExceptions,
                                                         ArrayList<Station> stations, Commodity commodity,
                                                         int stationA, int stationB, String a, String b, IloCplex model) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        try {
            IloNumVar[] X = new IloNumVar[blocks.size()];
            IloNumExpr goalFunction;
            IloNumExpr constraint;
            //start solving model for the commodity
            //masirhai estesna baiad az masir khas beran
            int temp = pathExceptions.isException(districtOf(a, stations),
                    districtOf(b, stations));
            if (temp == 1 || temp == 2) {
                for (int j = 0; j < blocks.size(); j++) {
                    boolean flag = true;
                    for (int i = (temp - 1); i < pathExceptions.getBlocksMustbe().size(); ) {
                        if (blocks.get(j).equals(pathExceptions.getBlocksMustbe().get(i))) {
                            X[j] = model.numVar(1, 1, IloNumVarType.Int);
                            flag = false;
                        } else if ((a.equals("ری") && !b.equals("تهران")) && (blocks.get(j).getOrigin().equals("ری") && blocks.get(j).getDestination().equals("بهرام"))) {
                            X[j] = model.numVar(1, 1, IloNumVarType.Int);
                            flag = false;
                        }
                        i += 2;
                    }
                    if (flag) {
                        X[j] = model.numVar(0, 1, IloNumVarType.Int);
                    }
                }
            } else if (a.equals("ری") && !b.equals("تهران")) {
                for (int j = 0; j < blocks.size(); j++) {
                    if (blocks.get(j).getOrigin().equals("ری") && blocks.get(j).getDestination().equals("بهرام")) {
                        X[j] = model.numVar(1, 1, IloNumVarType.Int);
                    } else {
                        X[j] = model.numVar(0, 1, IloNumVarType.Int);
                    }
                }
            } else if (b.equals("ری") && !a.equals("تهران")) {
                for (int j = 0; j < blocks.size(); j++) {
                    if (blocks.get(j).getOrigin().equals("بهرام") && blocks.get(j).getDestination().equals("ری")) {
                        X[j] = model.numVar(1, 1, IloNumVarType.Int);
                    } else {
                        X[j] = model.numVar(0, 1, IloNumVarType.Int);
                    }
                }
            } else {
                for (int i = 0; i < blocks.size(); i++) {
                    X[i] = model.numVar(0, 1, IloNumVarType.Int);
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

            model.setOut(null);
            try {
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
                    ArrayList<Block> tempBlocks = new ArrayList<>();

                    while (!commodity.getBlocks().isEmpty()) {
                        for (Block block : commodity.getBlocks()) {
                            if (block.getOrigin().equals(tempOrigin)) {
                                tempBlocks.add(block);
                                tempOrigin = block.getDestination();
                                commodity.getBlocks().remove(block);
                                break;
                            }
                        }
                    }
                    commodity.setBlocks(tempBlocks);


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


                    model.clearModel();
                    for (int i = 0; i < blocks.size(); i++) {
                        if (X[i] != null) {
                            X[i] = null;
                        }
                    }
                    goalFunction = null;
                    constraint = null;
                } else {
                    result = null;
                    System.out.println("No path");
                    model.clearModel();
                }
                return result;
            } catch (CpxException e) {
                e.printStackTrace();
                return null;
            }
        } catch (IloException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void readData(String outPutFile) {
        stations = new ArrayList<>();
        blocks = new ArrayList<>();
        outputBlocks = new ArrayList<>();
        pathExceptions = new PathExceptions();
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
                        (int) row.getCell(4).getNumericCellValue(),
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
                        (int) row.getCell(4).getNumericCellValue(),
                        (int) row.getCell(5).getNumericCellValue(),
                        2,
                        ((row.getCell(5).getNumericCellValue() == 2) ? row.getCell(10).getNumericCellValue() : 0),
                        stations, (int) row.getCell(11).getNumericCellValue());
                blocks.add(block2);
            }

            //read exceptions
            XSSFSheet sheet4 = data.getSheetAt(3);
            for (int i = 0; i < 4; i++) {
                for (int j = 1; j < sheet4.getLastRowNum(); j++) {
                    XSSFRow row = sheet4.getRow(j + 1);
                    try {
                        if (!row.getCell(i).getStringCellValue().equals("")) {
                            switch (i) {
                                case 0:
                                    pathExceptions.getOriginDistricts().add(row.getCell(i).getStringCellValue());
                                    break;
                                case 1:
                                    pathExceptions.getDestinationDistricts().add(row.getCell(i).getStringCellValue());
                                    break;
                                case 2:
                                    for (Block block : blocks) {
                                        if ((block.getOrigin().equals(row.getCell(i).getStringCellValue()) &&
                                                block.getDestination().equals(row.getCell(i + 1).getStringCellValue()))
                                                || (block.getOrigin().equals(row.getCell(i + 1).getStringCellValue()) &&
                                                block.getDestination().equals(row.getCell(i).getStringCellValue()))) {

                                            pathExceptions.getBlocksMustbe().add(block);

                                        }
                                    }
                                    break;
                            }
                        }
                    } catch (IllegalStateException | NullPointerException e) {
                        break;
                    }
                }
            }

            XSSFSheet sheet5 = data.getSheetAt(4);
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
                        if (result[3].equals(row.getCell(j).getStringCellValue()))
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
                        if (result[4].equals(row.getCell(j).getStringCellValue()))
                            check = false;
                    }
                    if (check) {
                        row.createCell(row.getLastCellNum()).setCellValue(result[4]);
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
                if (station.getName().equals(correctDestinationName)) {
                    boolean check = true;
                    for (String name : station.getAlterNames()) {
                        if (result[4].equals(name)) {
                            check = false;
                            break;
                        }
                    }
                    if (check) {
                        station.getAlterNames().add(result[4]);
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
                            (float) row.getCell(1).getNumericCellValue() / 10;
                    gTip.getFreeSpace()[i][1] =
                            (float) (row.getCell(0).getNumericCellValue() / 10);

                    if (i == 0) {
                        gTip.getFreeSpace()[2 * freeSpaceCorners][0] =
                                (float) row.getCell(1).getNumericCellValue() / 10;
                        gTip.getFreeSpace()[2 * freeSpaceCorners][1] =
                                (float) (row.getCell(0).getNumericCellValue() / 10);
                    }

                    gTip.getFreeSpace()[2 * freeSpaceCorners - i - 1][0] =
                            (float) row.getCell(2).getNumericCellValue() / 10;
                    gTip.getFreeSpace()[2 * freeSpaceCorners - i - 1][1] =
                            (float) (row.getCell(0).getNumericCellValue() / 10);
                }

                for (int i = 0; i < allowedSpaceCorners; i++) {
                    row = sheet.getRow((i) + 4);
                    gTip.getAllowedSpace()[i][0] =
                            (float) row.getCell(5).getNumericCellValue() / 10;
                    gTip.getAllowedSpace()[i][1] =
                            (float) (row.getCell(4).getNumericCellValue() / 10);

                    if (i == 0) {
                        gTip.getAllowedSpace()[2 * allowedSpaceCorners][0] =
                                (float) row.getCell(5).getNumericCellValue() / 10;
                        gTip.getAllowedSpace()[2 * allowedSpaceCorners][1] =
                                (float) (row.getCell(4).getNumericCellValue() / 10);
                    }

                    gTip.getAllowedSpace()[2 * allowedSpaceCorners - i - 1][0] =
                            (float) row.getCell(6).getNumericCellValue() / 10;
                    gTip.getAllowedSpace()[2 * allowedSpaceCorners - i - 1][1] =
                            (float) (row.getCell(4).getNumericCellValue() / 10);
                }
                gTips.add(gTip);
            }
            System.out.println();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


