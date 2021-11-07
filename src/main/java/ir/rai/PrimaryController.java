package ir.rai;

import com.jsevy.jdxf.DXFDocument;
import com.jsevy.jdxf.DXFGraphics;
import ir.rai.Data.Assignment;
import ir.rai.Data.Chart;
import ir.rai.Data.Corridor;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Callback;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.controlsfx.control.CheckComboBox;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.locationtech.jts.algorithm.distance.DistanceToPoint;
import org.locationtech.jts.algorithm.distance.PointPairDistance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;

import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static ir.rai.Data.Assignment.gTips;
import static ir.rai.Data.ExcelUtility.*;
import static java.lang.Math.max;
import static jfxtras.styles.jmetro.JMetroStyleClass.*;

public class PrimaryController {
    @FXML
    TabPane tabPane;

    @FXML
    Tab defaultTab;

    @FXML
    Button addButton, startBtm;

    @FXML
    TextField add1Para, add2Para, origin, destination;

    @FXML
    ComboBox<String> coordinationBox;

    @FXML
    CheckComboBox<String> corridors;
    @FXML
    ProgressBar progressBar;
    @FXML
    Label serviceResult;

    @FXML
    TableView<MyCoordinate> table;
    Assignment assignment;
    public static ArrayList<MyCoordinate> realData;
    private ObservableList<MyCoordinate> data =
            FXCollections.observableArrayList(
                    new MyCoordinate(10f, 450f),
                    new MyCoordinate(10f, 200f),
                    new MyCoordinate(410f, 200f),
                    new MyCoordinate(410f, 450f),
                    new MyCoordinate(10f, 450f)
            );

    public static ArrayList<Corridor> specialCorridors = new ArrayList<>();
    public static ArrayList<Tab> tabList;

    String outputFileLocation = "./output/output.xlsx";
    String outputDXFLocation = "./output/";

    @FXML
    private void initialize() {
        assignment = new Assignment();
        checkOutputFile(outputFileLocation);
        configureTable();
        calculationInitialize();
    }

    private void checkOutputFile(String outputFileLocation) {
        try (
                FileInputStream inFile = new FileInputStream(outputFileLocation);
                XSSFWorkbook workbook = new XSSFWorkbook(inFile);
        ) {
            XSSFSheet sheet = workbook.getSheet("ورودی واگن یا بار");
            data = FXCollections.observableArrayList();
            origin.setText(sheet.getRow(0).getCell(1).getStringCellValue());
            destination.setText(sheet.getRow(1).getCell(1).getStringCellValue());

            int temp = sheet.getLastRowNum();
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                data.add(new MyCoordinate(
                        (float) sheet.getRow(i).getCell(0).getNumericCellValue(),
                        (float) sheet.getRow(i).getCell(1).getNumericCellValue()));
            }
        } catch (IOException e) {
            System.out.println("There is no Output File, or the file has a problem");
        }
    }

    private void configureTable() {
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setEditable(true);
        table.getSelectionModel().setCellSelectionEnabled(false);

        table.setRowFactory(
                tableView -> {
                    TableRow<MyCoordinate> row = new TableRow<>();
                    ContextMenu rowMenu = new ContextMenu();
                    MenuItem addTop = new MenuItem("Add top");
                    addTop.setOnAction(event -> data.add(max(0, data.indexOf(row.getItem())),
                            new MyCoordinate(Float.parseFloat(add1Para.getText()),
                                    Float.parseFloat(add2Para.getText()))));
                    MenuItem addBottom = new MenuItem("Add bottom");
                    addBottom.setOnAction(event -> data.add(data.indexOf(row.getItem()) + 1,
                            new MyCoordinate(Float.parseFloat(add1Para.getText()),
                                    Float.parseFloat(add2Para.getText()))));

                    MenuItem deleteBottom = new MenuItem("Delete");
                    deleteBottom.setOnAction(event -> data.remove(data.get(data.indexOf(row.getItem()))));
                    rowMenu.getItems().addAll(addTop, addBottom, deleteBottom);

                    // only display context menu for non-empty rows:
                    row.contextMenuProperty().bind(
                            Bindings.when(row.emptyProperty())
                                    .then((ContextMenu) null)
                                    .otherwise(rowMenu));
                    return row;
                });

        Callback<TableColumn<MyCoordinate, Float>, TableCell<MyCoordinate, Float>> cellFactory =
                p -> new EditingCell();

        TableColumn column1 = new TableColumn("First Parameter");
        column1.setMinWidth(150);
        column1.setCellValueFactory(new PropertyValueFactory<MyCoordinate, Float>("x"));
        column1.setCellFactory(cellFactory);
        column1.setOnEditCommit(
                (EventHandler<CellEditEvent<MyCoordinate, Float>>) t -> {
                    t.getTableView().getItems().get(
                            t.getTablePosition().getRow()).setX(t.getNewValue());
                });

        TableColumn<MyCoordinate, Float> column2 = new TableColumn<>("Second Parameter");
        column2.setMinWidth(170);
        column2.setCellValueFactory(new PropertyValueFactory<>("y"));
        column2.setCellFactory(cellFactory);
        column2.setOnEditCommit(
                t -> {
                    t.getTableView().getItems().get(
                            t.getTablePosition().getRow()).setY(t.getNewValue());
                });

        table.setItems(data);

        table.getColumns().addAll(column1, column2);

        addIfNotPresent(table.getStyleClass(), ALTERNATING_ROW_COLORS);
        addIfNotPresent(table.getStyleClass(), TABLE_GRID_LINES);

        coordinationBox.getItems().addAll("XY Coordination", "Width and Height");
        coordinationBox.setValue("XY Coordination");
        coordinationBox.valueProperty().addListener(observable -> coordinationBox.getValue());

        corridors.getItems().add("No exception");
        corridors.getCheckModel().check(0);
        for (Corridor corridor : specialCorridors) {
            corridors.getItems().add(corridor.toString());
        }

        corridors.checkModelProperty().addListener(observable -> corridors.getCheckModel().getCheckedItems());
        addButton.setOnAction(actionEvent ->
                data.add(new MyCoordinate(Float.parseFloat(add1Para.getText()), Float.parseFloat(add2Para.getText()))));
    }

    @FXML
    private void calculationInitialize() {
        Service service = new Service() {
            @Override
            protected Task createTask() throws NullPointerException {
                return new Task() {
                    @Override
                    protected Object call() throws Exception {
                        realData = new ArrayList<>();
                        tabList = new ArrayList<>();
                        if (coordinationBox.getValue().equals("Width and Height")) {
                            changeToXY();
                        } else {
                            realData.addAll(data);
                        }

                        LinkedHashMap<String, Integer> gabariResult =
                                new LinkedHashMap<>(assignment.main(origin.getText(), destination.getText(), corridors.getCheckModel().getCheckedItems()));

                        isFileExist(outputFileLocation);
                        prepareExcel(outputFileLocation);

                        int rowCounter = 1;
                        for (Map.Entry pair : gabariResult.entrySet()) {
                            Tab tab = new Tab();
                            tab.setClosable(false);

                            Label label = new Label(pair.getKey().toString());
                            label.setRotate(90);
                            ArrayList<Coordinate[]> transportable = new ArrayList<>();
                            tab.setGraphic(new Group(label));

                            double outOfAllow = 0;
                            double outOfFree = 0;
                            if ((Integer) pair.getValue() >= 1) {
                                transportable = analyzeGabari(gTips.get((Integer) pair.getValue() - 1));

                                //check whether we have a result from the analyze
                                if (transportable.size() == 0) {
                                    drawGabari(transportable, ((Integer) pair.getValue() - 1), outOfAllow, outOfFree, tab);
                                    tabList.add(tab);
                                    continue;
                                }
                                outOfAllow = transportable.get(1).length > 0 ?
                                        computeOutOfGabari(gTips.get((Integer) pair.getValue() - 1).getAllowedSpace(), transportable.get(1)) :
                                        0;
                                outOfFree = transportable.get(2).length > 0 ?
                                        computeOutOfGabari(gTips.get((Integer) pair.getValue() - 1).getFreeSpace(), transportable.get(2)) :
                                        0;
                                addToExcel(outputFileLocation,
                                        rowCounter, transportable,
                                        outOfAllow,
                                        outOfFree,
                                        ((Integer) pair.getValue() - 1),
                                        pair.getKey().toString());
                            } else {
                                addToExcel(outputFileLocation, rowCounter, null, 0, 0,
                                        ((Integer) pair.getValue() - 1), pair.getKey().toString());
                            }
                            rowCounter++;
                            drawGabari(transportable, ((Integer) pair.getValue() - 1), outOfAllow, outOfFree, tab);
                            createDXF(transportable, ((Integer) pair.getValue() - 1), pair.getKey().toString());
                            tabList.add(tab);
                        }
                        return null;
                    }
                };
            }
        };

        service.setOnRunning(e -> {
            progressBar.setVisible(true);
            serviceResult.setText("");
        });
        service.setOnFailed(e -> {
            progressBar.setVisible(false);
            serviceResult.setText("Unsuccessful Gabari Calculation");
        });
        service.setOnSucceeded(e -> {
            while (tabPane.getTabs().size() > 0) {
                tabPane.getTabs().remove(0);
            }
            for (Tab tab : tabList) {
                tabPane.getTabs().add(tab);
            }
            progressBar.setVisible(false);
            serviceResult.setText("Done. See Results");
        });

        startBtm.setOnAction(e -> {
            if (!service.isRunning()) {
                service.reset();
                service.start();
            }
        });
    }

    private void prepareExcel(String outputFileLocation) {
        FileOutputStream outFile;

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            XSSFSheet sheet1 = workbook.createSheet("ورودی واگن یا بار");
            sheet1.setRightToLeft(true);
            CellStyle style = setStyle(workbook, "B Zar", new Color(210, 202, 159));
            XSSFRow row1 = sheet1.createRow(0);
            setCell(row1.createCell(0), "مبدا", style);
            setCell(row1.createCell(1), origin.getText(), style);

            row1 = sheet1.createRow(1);
            setCell(row1.createCell(0), "مقصد", style);
            setCell(row1.createCell(1), destination.getText(), style);

            row1 = sheet1.createRow(2);
            setCell(row1.createCell(0), "x", style);
            setCell(row1.createCell(1), "y", style);

            int counter = 3;
            for (MyCoordinate coordinate : data) {
                row1 = sheet1.createRow(counter);
                setCell(row1.createCell(0), (double) coordinate.x, style);
                setCell(row1.createCell(1), (double) coordinate.y, style);
                counter++;
            }

            XSSFSheet sheet = workbook.createSheet("خروجی");
            sheet.setRightToLeft(true);
            XSSFRow row = sheet.createRow(0);

            setCell(row.createCell(0), "مسیر", style);
            setCell(row.createCell(1), "تیب گاباری", style);
            setCell(row.createCell(2), "قابلیت عبور فضای مجاز", style);
            setCell(row.createCell(3), "اندازه ورود به فضای مجاز", style);
            setCell(row.createCell(4), "قابلیت عبور از حد آزاد", style);
            setCell(row.createCell(5), "اندازه ورود به فضای آزاد", style);

            outFile = new FileOutputStream(outputFileLocation);
            workbook.write(outFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawGabari(ArrayList<Coordinate[]> result, int gTipNumber, double outOfAllow,
                            double outOfFree, Tab tab) {
        VBox parent = new VBox();
        parent.setAlignment(Pos.CENTER);
        HBox chartContainer = new HBox();
        int containerHeight = 600;
        chartContainer.setMinHeight(containerHeight);
        chartContainer.setMinWidth(containerHeight / 1.125);
        chartContainer.setPadding(new Insets(10, 10, 10, 10));
        GridPane header = new GridPane();
        header.setHgap(20);
        header.setVgap(5);
        header.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        header.setPadding(new Insets(0, 0, 10, 10));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-font: 14px 'B Nazanin'");
        Node chartCanvas = null;

        Boolean haveCanvas = true;

        if (gTipNumber >= 0) {
            if (result.size() == 0) {
                header.add(new Label("مشکل در مشخصات وارد شده بار"), 0, 0);
                haveCanvas = false;
            } else {
                GTip gTip = gTips.get(gTipNumber);
                XYSeriesCollection dataset = new XYSeriesCollection();
                final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
                renderer.setDefaultShapesVisible(false);
                renderer.setDefaultStroke(new BasicStroke(4));


                header.add(new Label("نوع گاباری"), 0, 0);
                header.add(new Label(gTip.getName()), 1, 0);

                header.add(new Label("اندازه ورود به فضای مجاز"), 0, 1);
                header.add(new Label(new DecimalFormat("##.00").format(outOfAllow) + " " + "سانتی متر"), 1, 1);

                header.add(new Label("اندازه ورود به فضای آزاد"), 0, 2);
                header.add(new Label(new DecimalFormat("##.00").format(outOfFree) + " " + "سانتی متر"), 1, 2);

                int[] coloringSchema = new int[3];
                int counter = 0;
                for (int i = 0; i < result.size(); i++) {
                    if (result.get(i).length > 0) {
                        XYSeries series = new XYSeries("Bar: part " + counter, false, true);
                        for (int j = 0; j < result.get(i).length; j++) {
                            if (isDuplicate(result.get(i), j)) {
                                series.add(result.get(i)[j].x, result.get(i)[j].y);
                                dataset.addSeries(series);
                                counter++;
                                coloringSchema[i] += 1;
                                series = new XYSeries("Bar: part " + counter, false, true);
                            } else {
                                series.add(result.get(i)[j].x, result.get(i)[j].y);
                            }
                        }
                    }
                }

                XYSeries series = new XYSeries("Allow", false, true);
                for (int i = 0; i < gTip.getAllowedSpace().length; i++) {
                    series.add(gTip.getAllowedSpace()[i][0], gTip.getAllowedSpace()[i][1]);
                }
                dataset.addSeries(series);

                series = new XYSeries("Free", false, true);
                for (int i = 0; i < gTip.getFreeSpace().length; i++) {
                    series.add(gTip.getFreeSpace()[i][0], gTip.getFreeSpace()[i][1]);
                }
                dataset.addSeries(series);

                int lines = 0;
                for (int j = 0; j < coloringSchema[0]; j++) {
                    renderer.setSeriesPaint(lines, new Color(0, 255, 0, 255));
                    lines++;
                }
                for (int j = 0; j < coloringSchema[1]; j++) {
                    renderer.setSeriesPaint(lines, new Color(255, 200, 0, 255));
                    lines++;
                    tab.setStyle("-fx-background-color: #d7d77b;");
                }
                for (int j = 0; j < coloringSchema[2]; j++) {
                    renderer.setSeriesPaint(lines, new Color(255, 0, 0, 255));
                    lines++;
                    tab.setStyle("-fx-background-color: #ce6161");
                }

                renderer.setSeriesPaint(lines, new Color(0, 0, 0, 255));
                renderer.setSeriesPaint(lines + 1, new Color(0, 0, 0, 255));

                Chart chart = new Chart(dataset, renderer);
                chartCanvas = useWorkaround(chart.chartViewer);
                AnchorPane.setLeftAnchor(chartCanvas, 50.0);
            }
        } else {
            header.add(new Label("گاباری این بخش مسیر ناشناخته است"), 0, 0);
            haveCanvas = false;
        }
        if (haveCanvas) {
            HBox.setHgrow(chartCanvas, Priority.ALWAYS);
            chartContainer.getChildren().addAll(chartCanvas);
            chartContainer.setAlignment(Pos.CENTER);
            parent.getChildren().addAll(header, chartContainer);
        } else {
            parent.getChildren().addAll(header);
        }
        tab.setContent(parent);
    }

    private Node useWorkaround(ChartViewer chartViewer) {
        if (true) {
            return new StackPane(chartViewer);
        }
        return chartViewer;
    }

    private boolean isDuplicate(Coordinate[] coordinates, int index) {
        if (index == 0)
            return false;
        for (int i = 0; i <= index - 1; i++) {
            if (coordinates[i].x == coordinates[index].x && coordinates[i].y == coordinates[index].y)
                return true;
        }
        return false;
    }

    public ArrayList<Coordinate[]> analyzeGabari(GTip gTip) {
        GeometryFactory fact = new GeometryFactory();
        WKTReader wktRdr = new WKTReader(fact);
        ArrayList<Coordinate[]> result = new ArrayList<>();

        try {
            float[][] cargoCoordinate = new float[realData.size()][2];

            for (int i = 0; i < realData.size(); i++) {
                cargoCoordinate[i][0] = realData.get(i).x;
                cargoCoordinate[i][1] = realData.get(i).y;
            }

            Geometry cargo = createPolygon(cargoCoordinate, wktRdr);
            Geometry allowSpace = createPolygon(gTip.getAllowedSpace(), wktRdr);
            Geometry freeSpace = createPolygon(gTip.getFreeSpace(), wktRdr);

            Coordinate[] cargoXallowIn = cargo.intersection(allowSpace).getCoordinates();
            Coordinate[] cargoXallowOut = cargo.difference(allowSpace).getCoordinates();
            Coordinate[] cargoXfree = null;
            Coordinate[] cargo4 = null;

            //first we should know how many sections cargoXallowOut has
            ArrayList<ArrayList<Coordinate>> sectionsAllow = new ArrayList<>();
            ArrayList<Coordinate> temp = new ArrayList<>();
            for (Coordinate coordinate : cargoXallowOut) {
                if (!temp.contains(coordinate)) {
                    temp.add(coordinate);
                } else {
                    temp.add(coordinate);
                    sectionsAllow.add(temp);
                    temp = new ArrayList<>();
                }
            }

            ArrayList<Coordinate[]> sectionsFree = new ArrayList<>();
            int allCoordinateSize = 0;
            int counter = 0;
            if (cargoXallowOut.length > 0) {
                for (ArrayList<Coordinate> coordinates : sectionsAllow) {
                    sectionsFree.add(createPolygon(coordinates, wktRdr).difference(freeSpace).getCoordinates());
                    allCoordinateSize += sectionsFree.get(counter).length;
                    counter++;
                }
            }

            //merging all section 2 coordinates
            cargoXfree = new Coordinate[allCoordinateSize];
            counter = 0;
            for (Coordinate[] coordinates : sectionsFree) {
                for (Coordinate coordinate : coordinates) {
                    cargoXfree[counter] = coordinate;
                    counter++;
                }
            }

            ArrayList<Coordinate[]> sections = new ArrayList<>();
            if (cargoXfree.length > 0) {
                allCoordinateSize = 0;
                counter = 0;
                for (ArrayList<Coordinate> coordinates : sectionsAllow) {
                    sections.add(createPolygon(coordinates, wktRdr).intersection(freeSpace).getCoordinates());
                    allCoordinateSize += sections.get(counter).length;
                    counter++;
                }
                //merging all section 2 coordinates
                cargo4 = new Coordinate[allCoordinateSize];
                counter = 0;
                for (Coordinate[] coordinates : sections) {
                    for (Coordinate coordinate : coordinates) {
                        cargo4[counter] = coordinate;
                        counter++;
                    }
                }
            } else
                cargo4 = cargoXallowOut;

            result.add(cargoXallowIn);
            result.add(cargo4);
            result.add(cargoXfree);
            return result;
        } catch (Exception e) {
            //returns empty list on errors
            return new ArrayList<>();
        }
    }

    public Geometry createPolygon(float[][] cordinates, WKTReader wktRdr) {
        Geometry result = null;
        StringBuilder wktA = new StringBuilder("POLYGON ((");
        for (int i = 0; i < cordinates.length; i++) {
            if (i != cordinates.length - 1) {
                wktA.append(cordinates[i][0]).append(" ").append(cordinates[i][1]).append(", ");
            } else {
                wktA.append(cordinates[i][0]).append(" ").append(cordinates[i][1]).append("))");
            }
        }
        try {
            result = wktRdr.read(wktA.toString());
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public Geometry createPolygon(ArrayList<Coordinate> cordinates, WKTReader wktRdr) {
        StringBuilder wktA = new StringBuilder("POLYGON ((");
        for (int i = 0; i < cordinates.size(); i++) {
            if (i != cordinates.size() - 1) {
                wktA.append(cordinates.get(i).x).append(" ").append(cordinates.get(i).y).append(", ");
            } else {
                wktA.append(cordinates.get(i).x).append(" ").append(cordinates.get(i).y).append("))");
            }
        }
        try {
            return wktRdr.read(wktA.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private void isFileExist(String fileName) {
        File f = new File(fileName);
        if (f.exists() && !f.isDirectory()) {
            f.delete();
        }
    }

    static class EditingCell extends TableCell<MyCoordinate, Float> {

        private TextField textField;

        public EditingCell() {
        }

        @Override
        public void startEdit() {
            super.startEdit();

            if (textField == null) {
                createTextField();
            }

            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            textField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();

            setText(String.valueOf(getItem()));
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        @Override
        public void updateItem(Float item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setGraphic(textField);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                } else {
                    setText(getString());
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
            textField.setOnKeyPressed(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        commitEdit(Float.parseFloat(textField.getText()));
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }
    }

    public double computeOutOfGabari(float[][] gabari, Coordinate[] outOfGabari) {
        double result = 0;
        GeometryFactory fact = new GeometryFactory();
        WKTReader wktRdr = new WKTReader(fact);

        PointPairDistance pointPairDistance;
        Geometry a = createPolygon(gabari, wktRdr);
        for (Coordinate coordinate : outOfGabari) {
            pointPairDistance = new PointPairDistance();
            DistanceToPoint.computeDistance(a, coordinate, pointPairDistance);
            result = max(result, pointPairDistance.getDistance());
        }
        return result;
    }

    public void createDXF(ArrayList<Coordinate[]> result, int gTipNumber, String name) {
        DXFDocument dxfDocument = new DXFDocument("Gabari");
        DXFGraphics dxfGraphics = dxfDocument.getGraphics();
        dxfGraphics.setStroke(new BasicStroke(3));

        if (gTipNumber >= 0) {
            GTip gTip = gTips.get(gTipNumber);
            for (int j = 0; j < result.size(); j++) {
                if (j == 0)
                    dxfGraphics.setColor(Color.green);
                if (j == 1)
                    dxfGraphics.setColor(Color.yellow);
                if (j == 2)
                    dxfGraphics.setColor(Color.RED);

                Coordinate[] gabari = result.get(j);
                for (int i = 0; i < gabari.length - 1; i++) {
                    if (isDuplicate(gabari, i)) {
                        //do nothing
                    } else
                        dxfGraphics.drawLine(gabari[i].x, -gabari[i].y, gabari[i + 1].x, -gabari[i + 1].y);
                }
            }

            dxfGraphics.setColor(Color.white);

            int temp;
            for (int i = 0; i < gTip.getAllowedSpace().length; i++) {
                if (i == gTip.getAllowedSpace().length - 1)
                    temp = 0;
                else
                    temp = i + 1;

                dxfGraphics.drawLine(
                        gTip.getAllowedSpace()[i][0],
                        -gTip.getAllowedSpace()[i][1],
                        gTip.getAllowedSpace()[temp][0],
                        -gTip.getAllowedSpace()[temp][1]);
            }

            for (int i = 0; i < gTip.getFreeSpace().length; i++) {
                if (i == gTip.getFreeSpace().length - 1)
                    temp = 0;
                else
                    temp = i + 1;

                dxfGraphics.drawLine(
                        gTip.getFreeSpace()[i][0],
                        -gTip.getFreeSpace()[i][1],
                        gTip.getFreeSpace()[temp][0],
                        -gTip.getFreeSpace()[temp][1]);
            }

        }

        String dxfText = dxfDocument.toDXFString();
        String filePath = outputDXFLocation + name + ".dxf";
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(dxfText);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void changeToXY() {
        for (int i = 0; i < data.size(); i++) {
            realData.add(new MyCoordinate(-(data.get(i).x / 2), data.get(i).y)
            );
        }
        for (int i = data.size() - 1; i >= 0; i--) {
            realData.add(new MyCoordinate((data.get(i).x / 2), data.get(i).y)
            );
        }
        realData.add(new MyCoordinate(-(data.get(0).x / 2), data.get(0).y));
    }

}
