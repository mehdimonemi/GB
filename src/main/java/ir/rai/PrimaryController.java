package ir.rai;

import com.jsevy.jdxf.DXFDocument;
import com.jsevy.jdxf.DXFGraphics;
import ir.rai.Data.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Transform;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static ir.rai.Data.Assignment.*;
import static ir.rai.Data.ExcelUtility.*;
import static java.lang.Math.max;
import static jfxtras.styles.jmetro.JMetroStyleClass.*;

public class PrimaryController {
    @FXML
    TabPane tabPane;

    @FXML
    Tab defaultTab;

    @FXML
    Button addButton, startBtm, loadFile, deleteData;

    @FXML
    TextField add1Para, add2Para, origin, destination, outDirectory;

    @FXML
    ComboBox<String> coordinationBox, wagonHeight;

    @FXML
    CheckComboBox<String> corridors;
    @FXML
    ProgressBar progressBar;
    @FXML
    Label serviceResult;

    @FXML
    TableView<MyCoordinate> inputTable;
    TableView<Section> pathTable;
    TableView<String> districtTable;
    TableView<Result> overallTable;
    Assignment assignment;
    public static ArrayList<MyCoordinate> realData;
    private ObservableList<MyCoordinate> data = FXCollections.observableArrayList();
    ObservableList<Result> resultData;

    public static ArrayList<Corridor> specialCorridors = new ArrayList<>();
    public static ArrayList<Tab> tabList;

    String outputFileLocation = "";
    String outputDXFLocation = "";

    public Service commoditiesCheck;
    public Service calculationService;
    //these string is for names check result. we will pass it throw many classes
    final String[][] commodityCheckResult = {new String[4]};
    public static boolean namesCheck = true;

    Exception e;

    @FXML
    private void initialize() {
        assignment = new Assignment();
        configureLoadFile();
        configureInputTable();
        configureCheckBoxes();
        checkCommodities();
        calculationInitialize();
    }

    private void configureCheckBoxes() {
        coordinationBox.getItems().addAll("XY Coordination", "Width and Height");
        coordinationBox.setValue("XY Coordination");
        coordinationBox.valueProperty().addListener(observable -> coordinationBox.getValue());

        for (Corridor corridor : specialCorridors) {
            corridors.getItems().add(corridor.toString());
        }


        corridors.checkModelProperty().addListener(observable -> corridors.getCheckModel().getCheckedItems());

        wagonHeight.getItems().addAll("0", "800", "1250", "1300");
        wagonHeight.setValue("0");
        wagonHeight.valueProperty().addListener(observable -> coordinationBox.getValue());
        wagonHeight.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            wagonHeight.setValue(newText);
        });

        deleteData.setOnAction(event -> inputTable.getItems().clear());
        addButton.setOnAction(actionEvent -> {
            data.add(new MyCoordinate(Float.parseFloat(add1Para.getText()), Float.parseFloat(add2Para.getText())));
            add1Para.setText("");
            add2Para.setText("");
        });
    }

    private void configureLoadFile() {
        loadFile.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File("./output"));
            File selectedDirectory = directoryChooser.showDialog(loadFile.getParent().getScene().getWindow());
            if (selectedDirectory != null) {
                outDirectory.setText(selectedDirectory.getName());
                outputFileLocation = selectedDirectory.getPath() + "/output.xlsx";
                outputDXFLocation = selectedDirectory.getPath() + "/";
                try {
                    if (isFileClose(outputFileLocation)) {
                        serviceResult.setStyle("-fx-text-fill: Black");
                        serviceResult.setText("");
                    } else {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    serviceResult.setStyle("-fx-text-fill: red");
                    serviceResult.setText("Output excel file is open. Please Close it First and retry");
                }

                try (
                        FileInputStream inFile = new FileInputStream(outputFileLocation);
                        XSSFWorkbook workbook = new XSSFWorkbook(inFile)
                ) {

                    XSSFSheet sheet = workbook.getSheet("ورودی واگن یا بار");
                    data = FXCollections.observableArrayList();
                    origin.setText(sheet.getRow(0).getCell(1).getStringCellValue());
                    destination.setText(sheet.getRow(1).getCell(1).getStringCellValue());

                    for (int i = 5; i <= sheet.getLastRowNum(); i++) {
                        data.add(new MyCoordinate(
                                (float) sheet.getRow(i).getCell(0).getNumericCellValue(),
                                (float) sheet.getRow(i).getCell(1).getNumericCellValue()));
                    }
                    inputTable.getItems().removeAll();
                    inputTable.setItems(data);
                    coordinationBox.setValue(sheet.getRow(2).getCell(1).getStringCellValue());
                    XSSFCell cell = sheet.getRow(3).getCell(1);
                    if (cell.getCellType().equals(CellType.STRING)) {
                        wagonHeight.setValue(cell.getStringCellValue());
                    } else {
                        wagonHeight.setValue(String.valueOf((int) cell.getNumericCellValue()));
                    }
                } catch (IOException e) {
                    serviceResult.setStyle("-fx-text-fill: red");
                    serviceResult.setText("There is no Output File, or the file has a problem");
                }
            }
        });
    }

    private void configureInputTable() {
        inputTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        inputTable.setEditable(true);
        inputTable.setId("inputTable");
        inputTable.getSelectionModel().setCellSelectionEnabled(false);

        inputTable.setRowFactory(
                tableView -> {
                    Font.loadFont("file:resources/Gandom-FD.ttf", 45);
                    TableRow<MyCoordinate> row = new TableRow<>();
                    ContextMenu rowMenu = new ContextMenu();
                    MenuItem addTop = new MenuItem("اضافه بالا");
                    addTop.setStyle("-fx-font: 12px 'Gandom'");
                    addTop.setOnAction(event -> data.add(max(0, data.indexOf(row.getItem())),
                            new MyCoordinate(Float.parseFloat(add1Para.getText()),
                                    Float.parseFloat(add2Para.getText()))));
                    MenuItem addBottom = new MenuItem("اضافه پایین");
                    addBottom.setStyle("-fx-font: 12px 'Gandom'");
                    addBottom.setOnAction(event -> data.add(data.indexOf(row.getItem()) + 1,
                            new MyCoordinate(Float.parseFloat(add1Para.getText()),
                                    Float.parseFloat(add2Para.getText()))));

                    MenuItem deleteBottom = new MenuItem("حذف");
                    deleteBottom.setStyle("-fx-font: 12px 'Gandom'");
                    deleteBottom.setOnAction(event -> data.remove(data.get(data.indexOf(row.getItem()))));
                    rowMenu.getItems().addAll(addTop, addBottom, deleteBottom);

                    // only display context menu for non-empty rows:
                    row.contextMenuProperty().bind(
                            Bindings.when(row.emptyProperty())
                                    .then((ContextMenu) null)
                                    .otherwise(rowMenu));
                    return row;
                });

        Callback<TableColumn<MyCoordinate, Float>, TableCell<MyCoordinate, Float>> cellFactory = p -> new EditingCell();

        TableColumn<MyCoordinate, Float> column1 = new TableColumn("X یا عرض");
        column1.setSortable(false);
        column1.setCellValueFactory(new PropertyValueFactory<>("x"));
        column1.setCellFactory(cellFactory);
        column1.setOnEditCommit(
                t -> t.getTableView().getItems().get(
                        t.getTablePosition().getRow()).setX(t.getNewValue()));

        TableColumn<MyCoordinate, Float> column2 = new TableColumn<>("Y یا ارتفاع");
        column2.setSortable(false);
        column2.setCellValueFactory(new PropertyValueFactory<>("y"));
        column2.setCellFactory(cellFactory);
        column2.setOnEditCommit(
                t -> t.getTableView().getItems().get(
                        t.getTablePosition().getRow()).setY(t.getNewValue()));

        inputTable.setItems(data);

        TableColumn mainColumn = new TableColumn("ابعاد بار");

        mainColumn.getColumns().addAll(column1, column2);


        inputTable.getColumns().addAll(mainColumn);

        inputTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addIfNotPresent(inputTable.getStyleClass(), ALTERNATING_ROW_COLORS);
        addIfNotPresent(inputTable.getStyleClass(), TABLE_GRID_LINES);
    }

    private void configureOverallTable() {
        overallTable = new TableView<>();
        overallTable.setId("overallTable");
        overallTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        overallTable.setEditable(false);
        overallTable.getSelectionModel().setCellSelectionEnabled(false);
        overallTable.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

        TableColumn<Result, String> column1 = new TableColumn<>("مسیر");
        TableColumn<Result, String> column2 = new TableColumn<>("تیب گاباری");
        TableColumn<Result, String> column3 = new TableColumn<>("قابلیت عبور از فضای مجاز");
        TableColumn<Result, String> column4 = new TableColumn<>("اندازه ورود به فضای آزاد");
        TableColumn<Result, String> column5 = new TableColumn<>("قابلیت عبور از فضای آزاد");
        TableColumn<Result, String> column6 = new TableColumn<>("اندازه ورود به فضای سازه");
        TableColumn<Result, String> column7 = new TableColumn<>("قابلیت عبور از فضای سازه");

        column1.setCellValueFactory(new PropertyValueFactory<>("section"));
        column2.setCellValueFactory(new PropertyValueFactory<>("gabariKind"));
        column3.setCellValueFactory(new PropertyValueFactory<>("allowedSpace"));
        column4.setCellValueFactory(new PropertyValueFactory<>("inFree"));
        column5.setCellValueFactory(new PropertyValueFactory<>("freeSpace"));
        column6.setCellValueFactory(new PropertyValueFactory<>("inStructure"));
        column7.setCellValueFactory(new PropertyValueFactory<>("structureSpace"));
        overallTable.getColumns().addAll(column7, column6, column5, column4, column3, column2, column1);

        overallTable.setRowFactory(tv -> new TableRow<Result>() {
            @Override
            protected void updateItem(Result item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null)
                    setStyle("");
                else if (item.getInFree().equals(""))
                    setStyle("-fx-background-color: rgba(107,152,112,1);");
                else if (item.getInStructure().equals(""))
                    setStyle("-fx-background-color: rgba(238,230,0,1);");
                else
                    setStyle("-fx-background-color: rgba(213,103,103,1);");
            }
        });

        makeHeaderWrappable(column1);
        makeHeaderWrappable(column2);
        makeHeaderWrappable(column3);
        makeHeaderWrappable(column4);
        makeHeaderWrappable(column5);
        makeHeaderWrappable(column6);
        makeHeaderWrappable(column7);

        column1.setPrefWidth(95);
        column2.setPrefWidth(95);
        column3.setPrefWidth(95);
        column4.setPrefWidth(95);
        column5.setPrefWidth(95);
        column6.setPrefWidth(95);
        column7.setPrefWidth(95);

        column1.setSortable(false);
        column2.setSortable(false);
        column3.setSortable(false);
        column4.setSortable(false);
        column5.setSortable(false);
        column6.setSortable(false);
        column7.setSortable(false);


        overallTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addIfNotPresent(overallTable.getStyleClass(), ALTERNATING_ROW_COLORS);
        addIfNotPresent(overallTable.getStyleClass(), TABLE_GRID_LINES);

        overallTable.setItems(resultData);
    }

    private void makeHeaderWrappable(TableColumn col) {
        Label label = new Label(col.getText());
        label.setStyle("-fx-padding: 8px;");
        label.setWrapText(true);
        label.setId("wrappableLabel");
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);

        StackPane stack = new StackPane();
        stack.getChildren().add(label);
        stack.setId("wrappableStack");
        stack.setAlignment(Pos.CENTER);

        stack.prefWidthProperty().bind(col.widthProperty().subtract(5));
        label.prefWidthProperty().bind(stack.prefWidthProperty());
        col.setText(null);
        col.setGraphic(stack);
    }

    private void buildFirstTab(ArrayList<Section> sections, ArrayList<String> districts, Tab tab) {
        VBox vBox = new VBox();
        vBox.setMinWidth(700);
        vBox.setPadding(new Insets(10, 10, 10, 10));
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setSpacing(20);
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        hBox.setMinWidth(700);
        hBox.setSpacing(20);
        Font.loadFont("file:resources/Gandom-FD.ttf", 45);
        vBox.setStyle("-fx-font: 12px 'Gandom'");

        Label header = new Label("مشخصات مسیر عبوری به شرح ذیل است:");

        hBox.getChildren().addAll(configureDistrictsTable(districts), configurePathSectionsTable(sections));
        vBox.getChildren().addAll(header, hBox);

        tab.setClosable(false);
        Label label = new Label("مسیر عبور");
        label.setRotate(90);
        tab.setGraphic(new Group(label));
        tab.setId("firstTab");
        tab.setContent(vBox);
        tabList.add(tab);
    }

    private void buildOverallTab(Tab tab) {
        VBox vBox = new VBox();
        vBox.setMinWidth(800);
        vBox.setPadding(new Insets(10, 10, 10, 10));
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setSpacing(20);
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        hBox.setMinWidth(800);
        hBox.setSpacing(20);
        Font.loadFont("file:resources/Gandom-FD.ttf", 45);
        vBox.setStyle("-fx-font: 12px 'Gandom'");

        Label header = new Label("خلاصه وضعیت بار/واگن در مبدا-مقصد وارد شده:");
        configureOverallTable();
        hBox.getChildren().addAll(overallTable);
        vBox.getChildren().addAll(header, hBox);

        tab.setClosable(false);
        Label label = new Label("خلاصه ارزیابی");
        label.setRotate(90);
        tab.setGraphic(new Group(label));
        tab.setId("firstTab");
        tab.setContent(vBox);
        tabList.add(1, tab);
    }

    private TableView<Section> configurePathSectionsTable(ArrayList<Section> sections) {
        ObservableList<Section> sectionsData = FXCollections.observableArrayList(sections);
        pathTable = new TableView<>();
        pathTable.setId("pathTable");
        pathTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        pathTable.setEditable(false);
        pathTable.getSelectionModel().setCellSelectionEnabled(false);
        TableColumn<Section, String> column1 = new TableColumn("مبدا");
        column1.setSortable(false);
        column1.setCellValueFactory(new PropertyValueFactory<>("origin"));

        TableColumn<Section, String> column2 = new TableColumn("مقصد");
        column2.setSortable(false);
        column2.setCellValueFactory(new PropertyValueFactory<>("destination"));

        TableColumn pathColumn = new TableColumn("مسیرها");
        pathColumn.getColumns().addAll(column2, column1);

        pathTable.getColumns().addAll(pathColumn);
        pathTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addIfNotPresent(pathTable.getStyleClass(), ALTERNATING_ROW_COLORS);
        addIfNotPresent(pathTable.getStyleClass(), TABLE_GRID_LINES);

        pathTable.setItems(sectionsData);
        pathTable.setMinHeight(600);
        return pathTable;
    }

    private TableView<String> configureDistrictsTable(ArrayList<String> districts) {
        ObservableList<String> districtsData = FXCollections.observableArrayList(districts);
        districtTable = new TableView<>();
        districtTable.setId("pathTable");
        districtTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        districtTable.setEditable(false);
        districtTable.getSelectionModel().setCellSelectionEnabled(false);
        TableColumn<String, String> column1 = new TableColumn<>("نام ناحیه");
        column1.setSortable(false);
        column1.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue()));

        TableColumn districtsColumn = new TableColumn("نواحی عبوری");
        districtsColumn.getColumns().addAll(column1);

        districtTable.getColumns().addAll(districtsColumn);
        districtTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addIfNotPresent(districtTable.getStyleClass(), ALTERNATING_ROW_COLORS);
        addIfNotPresent(districtTable.getStyleClass(), TABLE_GRID_LINES);

        districtTable.setItems(districtsData);
        districtTable.setMinHeight(600);
        return districtTable;
    }

    @FXML
    private void calculationInitialize() {
        calculationService = new Service() {
            @Override
            protected Task createTask() throws NullPointerException {
                return new Task() {
                    @Override
                    protected Object call() {
                        realData = new ArrayList<>();
                        tabList = new ArrayList<>();
                        resultData = FXCollections.observableArrayList();
                        deleteFiles(outputDXFLocation);

                        if (coordinationBox.getValue().equals("Width and Height")) {
                            changeToXY();
                        } else {
                            for (MyCoordinate coordinate : data) {
                                realData.add(new MyCoordinate(coordinate.getX(),
                                        coordinate.getY() + Float.parseFloat(wagonHeight.getValue())));
                            }
                        }

                        LinkedHashMap<String, Integer> gabariResult =
                                new LinkedHashMap<>(assignment.main(origin.getText(),
                                        destination.getText(), corridors.getCheckModel().getCheckedItems()));

                        deleteIfFileExist(outputFileLocation);
                        prepareExcel(outputFileLocation);

                        buildFirstTab(pathSection, pathDistricts, new Tab());
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

                                //check whether we have a result from the analysis
                                if (transportable.size() == 0) {
                                    drawGabari(pair.getKey().toString(), transportable, ((Integer) pair.getValue() - 1),
                                            outOfAllow, outOfFree, tab);
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
                                if (outOfAllow == 0)
                                    resultData.add(new Result(
                                            pair.getKey().toString(),
                                            gTips.get(((Integer) pair.getValue() - 1)).getName(),
                                            "قابل عبور"
                                    ));
                                else if (outOfFree == 0)
                                    resultData.add(new Result(
                                            pair.getKey().toString(),
                                            gTips.get(((Integer) pair.getValue() - 1)).getName(),
                                            "غیر قابل عبور",
                                            String.valueOf(Math.round(outOfAllow * 100.0) / 100.0),
                                            "قابل عبور"
                                    ));
                                else
                                    resultData.add(new Result(
                                            pair.getKey().toString(),
                                            gTips.get(((Integer) pair.getValue() - 1)).getName(),
                                            "غیر قابل عبور",
                                            String.valueOf(Math.round(outOfAllow * 100.0) / 100.0),
                                            "غیر قابل عبور",
                                            String.valueOf(Math.round(outOfFree * 100.0) / 100.0),
                                            "غیر قابل عبور"
                                    ));
                            } else {
                                addToExcel(outputFileLocation, rowCounter, null, 0, 0,
                                        ((Integer) pair.getValue() - 1), pair.getKey().toString());
                            }
                            rowCounter++;
                            drawGabari(pair.getKey().toString(), transportable,
                                    ((Integer) pair.getValue() - 1), outOfAllow, outOfFree, tab);
                            createDXF(transportable, ((Integer) pair.getValue() - 1), pair.getKey().toString());
                            tabList.add(tab);
                        }
                        buildOverallTab(new Tab());
                        return null;
                    }
                };
            }
        };

        calculationService.setOnRunning(e -> {
            progressBar.setVisible(true);
            serviceResult.setText("");
        });
        calculationService.setOnFailed(e -> {
            progressBar.setVisible(false);
            serviceResult.setStyle("-fx-text-fill: red");
            serviceResult.setText("Unsuccessful Gabari Calculation");
        });
        calculationService.setOnSucceeded(e -> {
            while (tabPane.getTabs().size() > 0) {
                tabPane.getTabs().remove(0);
            }
            for (Tab tab : tabList) {
                tabPane.getTabs().add(tab);
            }
            progressBar.setVisible(false);
            serviceResult.setStyle("-fx-text-fill: Green");
            serviceResult.setText("Done. See Results");
        });
    }

    private void createJPG(VBox tab, String sectionName) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem item1 = new MenuItem("Save as Png");
        contextMenu.getItems().add(item1);

        tab.setOnContextMenuRequested(e -> contextMenu.show(tab, e.getScreenX(), e.getScreenY()));
        tab.setOnMouseClicked(e -> contextMenu.hide());

        item1.setOnAction(e -> {
            WritableImage img = pixelScaleAwareCanvasSnapshot(tab, 3);
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File(outputDXFLocation));
            File selectedDirectory = directoryChooser.showDialog(loadFile.getParent().getScene().getWindow());
            BufferedImage img2 = SwingFXUtils.fromFXImage(img, null);
            if (selectedDirectory != null) {
                try {
                    File f = new File(selectedDirectory + "/" + sectionName + ".png");
                    ImageIO.write(img2, "png", f);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
    }

    public static WritableImage pixelScaleAwareCanvasSnapshot(VBox canvas, double pixelScale) {
        WritableImage writableImage = new WritableImage(
                (int) Math.rint(pixelScale * canvas.getWidth()), (int) Math.rint(pixelScale * canvas.getHeight()));
        SnapshotParameters spa = new SnapshotParameters();
        spa.setTransform(Transform.scale(pixelScale, pixelScale));
        return canvas.snapshot(spa, writableImage);
    }

    private void deleteFiles(String outputDXFLocation) {
        File[] files = (new File(outputDXFLocation)).listFiles((d, f) -> f.toLowerCase().endsWith("dxf"));
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }

        files = (new File(outputDXFLocation)).listFiles((d, f) -> f.toLowerCase().endsWith("png"));
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
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
            setCell(row1.createCell(0), "نوع", style);
            setCell(row1.createCell(1), coordinationBox.getValue(), style);

            row1 = sheet1.createRow(3);
            setCell(row1.createCell(0), "ارتفاع واگن", style);
            setCell(row1.createCell(1), Double.valueOf(wagonHeight.getValue()), style);

            row1 = sheet1.createRow(4);
            setCell(row1.createCell(0), "x", style);
            setCell(row1.createCell(1), "y", style);

            sheet1.setColumnWidth(0, 21 * 256);
            sheet1.setColumnWidth(1, 21 * 256);


            int counter = 5;
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
            setCell(row.createCell(2), "قابلیت عبور از فضای مجاز", style);
            setCell(row.createCell(3), "اندازه ورود به فضای آزاد", style);
            setCell(row.createCell(4), "قابلیت عبور از فضای آزاد", style);
            setCell(row.createCell(5), "اندازه ورود به فضای سازه", style);
            setCell(row.createCell(6), "قابلیت عبور از فضای سازه", style);

            sheet.setColumnWidth(0, 21 * 256);
            sheet.setColumnWidth(1, 21 * 256);
            sheet.setColumnWidth(2, 21 * 256);
            sheet.setColumnWidth(3, 21 * 256);
            sheet.setColumnWidth(4, 21 * 256);
            sheet.setColumnWidth(5, 21 * 256);
            sheet.setColumnWidth(6, 21 * 256);

            outFile = new FileOutputStream(outputFileLocation);
            workbook.write(outFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawGabari(String sectionName, ArrayList<Coordinate[]> result, int gTipNumber, double outOfAllow,
                            double outOfFree, Tab tab) {
        VBox parent = new VBox();
        parent.setMinWidth(700);
        parent.setAlignment(Pos.CENTER);
        HBox chartContainer = new HBox();
        int containerHeight = 600;
        chartContainer.setMinHeight(containerHeight);
        chartContainer.setPadding(new Insets(10, 10, 10, 10));
        GridPane header = new GridPane();
        header.setHgap(20);
        header.setVgap(5);
        header.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        header.setPadding(new Insets(0, 0, 10, 10));
        header.setAlignment(Pos.CENTER);
        Font.loadFont("file:resources/Gandom-FD.ttf", 45);
        header.setStyle("-fx-font: 12px 'Gandom'");
        Node chartCanvas = null;

        boolean haveCanvas = true;

        if (gTipNumber >= 0) {
            if (result.size() == 0) {
                header.add(new Label("مشکل در مشخصات وارد شده بار"), 0, 0);
                haveCanvas = false;
            } else {
                GTip gTip = gTips.get(gTipNumber);
                XYSeriesCollection dataset = new XYSeriesCollection();
                final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
                renderer.setDefaultShapesVisible(false);

                Label headerName = new Label(sectionName);
                GridPane.setHalignment(headerName, HPos.CENTER);
                header.add(headerName, 0, 0, 2, 1);
                header.add(new Label("نوع گاباری"), 0, 1);
                header.add(new Label(gTip.getName()), 1, 1);

                header.add(new Label("اندازه ورود به فضای آزاد"), 0, 2);
                header.add(new Label(new DecimalFormat("##.00").format(outOfAllow) + " " + "میلی متر"),
                        1, 2);

                header.add(new Label("اندازه ورود به فضای سازه"), 0, 3);
                header.add(new Label(new DecimalFormat("##.00").format(outOfFree) + " " + "میلی متر"),
                        1, 3);

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
                float stroke = 1.5f;
                for (int j = 0; j < coloringSchema[0]; j++) {
                    renderer.setSeriesPaint(lines, new Color(13, 169, 33, 255));
                    renderer.setSeriesStroke(lines, new BasicStroke(stroke), false);
                    lines++;
                    tab.setId("greenTab");
                }
                for (int j = 0; j < coloringSchema[1]; j++) {
                    renderer.setSeriesPaint(lines, new Color(250, 194, 0, 255));
                    renderer.setSeriesStroke(lines, new BasicStroke(stroke), false);
                    lines++;
                    tab.setId("yellowTab");
                }
                for (int j = 0; j < coloringSchema[2]; j++) {
                    renderer.setSeriesPaint(lines, new Color(178, 21, 21, 255));
                    renderer.setSeriesStroke(lines, new BasicStroke(stroke), false);
                    lines++;
                    tab.setId("redTab");
                }

                renderer.setSeriesPaint(lines, new Color(0, 0, 0, 255));
                renderer.setSeriesStroke(lines, new BasicStroke(stroke), false);
                renderer.setSeriesPaint(lines + 1, new Color(0, 0, 0, 255));
                renderer.setSeriesStroke(lines + 1, new BasicStroke(stroke), false);

                Chart chart = new Chart(dataset, renderer, sectionName);
                chartCanvas = useWorkaround(chart.chartViewer);
                chart.chartViewer.setMaxWidth(containerHeight / 1.125);
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
        createJPG(parent, sectionName);
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
            Coordinate[] cargoXfree;
            Coordinate[] cargo4;

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
        Geometry result;
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
            Font.loadFont("file:resources/Gandom-FD.ttf", 45);
            textField.setStyle("-fx-font: 12px 'Gandom'");
            textField.setAlignment(Pos.CENTER);
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
            textField.setOnKeyPressed(t -> {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(Float.parseFloat(textField.getText()));
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }
    }

    private void checkCommodities() {
        commoditiesCheck = new Service() {
            @Override
            public Task createTask() {
                return new Task() {
                    @Override
                    protected Void call() throws Exception {
                        //blow is our commoditiesCheck result:
                        commodityCheckResult[0][0] = origin.getText();//correct origin name that we get it from correct name dialog
                        commodityCheckResult[0][1] = destination.getText();//correct destination name that we get it from correct name dialog
                        if (namesCheck) {
                            commodityCheckResult[0][2] = origin.getText();//original origin name that might be incorrect
                            commodityCheckResult[0][3] = destination.getText();//original destination name that might be incorrect
                        }

                        if (isFileClose("./Data.xlsx")) {
                            commodityCheckResult[0] = assignment.manageNames(commodityCheckResult[0]);
                            if (!namesCheck)
                                throw e = new Exception("Origin & Destination Names Are Wrong. Please correct them first");
                        } else {
                            throw e = new Exception("Data Excel File is open. Please close it first.");
                        }
                        System.gc();
                        return null;
                    }
                };
            }
        };
        commoditiesCheck.setOnFailed(event -> {
            serviceResult.setStyle("-fx-text-fill: red");
            serviceResult.setText(e.getMessage());
        });

        commoditiesCheck.setOnSucceeded(event -> {
            if (!calculationService.isRunning()) {
                calculationService.reset();
                calculationService.start();
            }
        });

        startBtm.setOnAction(event -> {
            if (!commoditiesCheck.isRunning()) {
                commoditiesCheck.reset();
                commoditiesCheck.start();
            }
        });
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
                    if (!isDuplicate(gabari, i)) {
                        dxfGraphics.drawLine(gabari[i].x, -gabari[i].y, gabari[i + 1].x, -gabari[i + 1].y);
                    }

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
        for (MyCoordinate datum : data) {
            realData.add(new MyCoordinate(-(datum.x / 2),
                    datum.y + Float.parseFloat(wagonHeight.getValue())));
        }
        for (int i = data.size() - 1; i >= 0; i--) {
            realData.add(new MyCoordinate((data.get(i).x / 2),
                    data.get(i).y + Float.parseFloat(wagonHeight.getValue())));
        }
        realData.add(new MyCoordinate(-(data.get(0).x / 2),
                data.get(0).y + Float.parseFloat(wagonHeight.getValue())));
    }

    private void deleteIfFileExist(String fileName) {
        File f = new File(fileName);
        if (f.exists() && !f.isDirectory()) {
            f.delete();
        }
    }

    public boolean isFileClose(String fileName) {
        //  TO CHECK WHETHER A FILE IS OPENED
        //  OR NOT (not for .txt files)

        File file = new File(fileName);
        if (file == null) {
            return true;
        }

        // try to rename the file with the same name
        File sameFileName = new File(fileName);

        // if the file didnt accept the renaming operation
        if (file.renameTo(sameFileName)) {
            // if the file is renamed
            return true;
        } else return !file.exists() && !file.isDirectory();
    }

    public void showAllGabari() {
        if (!isFileClose("./temp.xlsx")) {
            serviceResult.setStyle("-fx-text-fill: red");
            serviceResult.setText("Temp excel file is open. Please Close it First and retry");
        } else {
            deleteIfFileExist("./temp.xlsx");

            FileOutputStream outFile;

            try (
                    FileInputStream inFile = new FileInputStream("./Data.xlsx");
                    XSSFWorkbook inWorkbook = new XSSFWorkbook(inFile);
                    XSSFWorkbook outWorkbook = new XSSFWorkbook();
            ) {
                XSSFSheet inSheet = inWorkbook.getSheetAt(3);

                XSSFSheet outSheet = outWorkbook.createSheet("خلاصه گاباری بلاک ها");
                outSheet.setRightToLeft(true);

                int counter = 1;
                for (Block ignored : outputBlocks) {
                    row = inSheet.getRow(counter);
                    Block block = Block.get(row.getCell(2).getStringCellValue(),
                            row.getCell(3).getStringCellValue());

                    String gabari;
                    if (block == null)
                        gabari = "گاباری ناشناخته است";
                    else
                        gabari = block.getGabariCode() - 1 < 0 ? "گاباری ناشناخته است" :
                                gTips.get(block.getGabariCode() - 1).getName();

                    setCell(
                            row.getCell(4),
                            gabari,
                            setStyle(inWorkbook, "B Zar", new Color(255, 255, 255, 0))
                    );
                    counter++;
                }

                CopySheet newCopy = new CopySheet();
                newCopy.copySheets(outSheet, inSheet, true);

                outFile = new FileOutputStream("./temp.xlsx");
                outWorkbook.write(outFile);

                Desktop.getDesktop().open(new File("./temp.xlsx"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
