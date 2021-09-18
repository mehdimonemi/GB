package ir.rai;

import ir.rai.Data.Assignment;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Callback;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.locationtech.jts.algorithm.distance.DistanceToPoint;
import org.locationtech.jts.algorithm.distance.PointPairDistance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static ir.rai.Data.Assignment.gTips;
import static ir.rai.Data.ExcelUtility.setCell;
import static ir.rai.Data.ExcelUtility.setStyle;
import static java.lang.Math.max;
import static jfxtras.styles.jmetro.JMetroStyleClass.*;

public class PrimaryController {
    @FXML
    TabPane tabPane;

    @FXML
    Button addButton;

    @FXML
    TextField add1Para, add2Para, origin, destination;

    @FXML
    ComboBox<String> coordinationBox;

    @FXML
    TableView<MyCoordinate> table;
    Assignment assignment;
    private final ObservableList<MyCoordinate> data =
            FXCollections.observableArrayList(
                    new MyCoordinate(10f, 300f),
                    new MyCoordinate(10f, 200f),
                    new MyCoordinate(390f, 200f),
                    new MyCoordinate(390f, 300f),
                    new MyCoordinate(10f, 300f)
            );

    String outputFileLocation = "./output.xlsx";

    @FXML
    private void initialize() {
        assignment = new Assignment();
        configureTable();
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
                    int rowIndex = row.getIndex();
                    MenuItem addTop = new MenuItem("Add top");

                    ;
                    addTop.setOnAction(event -> data.add(max(0, data.indexOf(row.getItem()) - 1),
                            new MyCoordinate(Float.parseFloat(add1Para.getText()),
                                    Float.parseFloat(add2Para.getText()))));
                    MenuItem addBottom = new MenuItem("Add bottom");
                    addBottom.setOnAction(event -> data.add(data.indexOf(row.getItem()) + 1,
                            new MyCoordinate(Float.parseFloat(add1Para.getText()),
                                    Float.parseFloat(add2Para.getText()))));
                    rowMenu.getItems().addAll(addTop, addBottom);

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
                    ((MyCoordinate) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())).setX(t.getNewValue());
                });

        TableColumn<MyCoordinate, Float> column2 = new TableColumn<MyCoordinate, Float>("Second Parameter");
        column2.setMinWidth(150);
        column2.setCellValueFactory(new PropertyValueFactory<MyCoordinate, Float>("y"));
        column2.setCellFactory(cellFactory);
        column2.setOnEditCommit(
                (EventHandler<CellEditEvent<MyCoordinate, Float>>) t -> {
                    ((MyCoordinate) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())).setY(t.getNewValue());
                });

        table.setItems(data);

        table.getColumns().addAll(column1, column2);

        addIfNotPresent(table.getStyleClass(), ALTERNATING_ROW_COLORS);
        addIfNotPresent(table.getStyleClass(), TABLE_GRID_LINES);

        coordinationBox.getItems().addAll("XY Coordination", "Length and Width");
        coordinationBox.setValue("XY Coordination");
        coordinationBox.valueProperty().addListener(observable -> coordinationBox.getValue());

        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                data.add(new MyCoordinate(Float.parseFloat(add1Para.getText()), Float.parseFloat(add2Para.getText())));
            }
        });

    }

    @FXML
    private void calculate() {
        LinkedHashMap<String, Integer> gabariResult =
                new LinkedHashMap<>(assignment.main(origin.getText(), destination.getText()));

        while (tabPane.getTabs().size() > 0) {
            tabPane.getTabs().remove(0);
        }

        isFileExist(outputFileLocation);
        prepareOutput(outputFileLocation);

        int rowCounter = 1;
        for (Map.Entry pair : gabariResult.entrySet()) {
            Tab tab = new Tab();
            tab.setClosable(false);

            Label label = new Label(pair.getKey().toString());
            label.setRotate(90);
            ArrayList<Coordinate[]> transportable = null;
            tab.setGraphic(new Group(label));

            double outOfAllow = 0;
            double outOfFree = 0;
            if ((Integer) pair.getValue() >= 1) {
                transportable = analyzeGabari(gTips.get((Integer) pair.getValue() - 1));
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
            tabPane.getTabs().add(tab);
        }
    }

    private void addToExcel(String outputFileLocation, int rowNumber,
                            ArrayList<Coordinate[]> transportable, double outOfAllow, double outOffree, int gTipNumber, String OD) {
        FileOutputStream outFile;

        try (
                FileInputStream inFile = new FileInputStream(outputFileLocation);
                XSSFWorkbook workbook = new XSSFWorkbook(inFile);
        ) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            XSSFRow row = sheet.createRow(rowNumber);

            Color color;
            if (rowNumber / 2.0 == 0) {
                color = new Color(90, 178, 198);
            } else
                color = new Color(255, 255, 255, 255);

            CellStyle style = setStyle(workbook, "B Zar", color);

            setCell(row.createCell(0), OD, style);
            if (gTipNumber >= 0) {
                GTip gTip = gTips.get(gTipNumber);
                setCell(row.createCell(1), gTip.getName(), style);
                setCell(row.createCell(2), transportable.get(1).length == 0 ? "قابل عبور" : "غیر قابل عبور", style);
                setCell(row.createCell(3), outOfAllow, style);
                setCell(row.createCell(4), transportable.get(2).length == 0 ? "قابل عبور" : "غیر قابل عبور", style);
                setCell(row.createCell(5), outOffree, style);
            } else {
                setCell(row.createCell(1), "نامشخص", style);
                setCell(row.createCell(2), "", style);
                setCell(row.createCell(3), "", style);
                setCell(row.createCell(4), "", style);
                setCell(row.createCell(5), "", style);
            }


            outFile = new FileOutputStream(outputFileLocation);
            workbook.write(outFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareOutput(String outputFileLocation) {
        FileOutputStream outFile;

        try (
                XSSFWorkbook workbook = new XSSFWorkbook();
        ) {
            XSSFSheet sheet = workbook.createSheet("خروجی");
            sheet.setRightToLeft(true);
            XSSFRow row = sheet.createRow(0);

            CellStyle style = setStyle(workbook, "B Zar", new Color(210, 202, 159));

            setCell(row.createCell(0), "مسیر", style);
            setCell(row.createCell(1), "تیب گاباری", style);
            setCell(row.createCell(2), "قابلیت عبور فضای آزاد", style);
            setCell(row.createCell(3), "بیرون زدگی از فضای آزاد", style);
            setCell(row.createCell(4), "قابلیت عبور از حد مجاز", style);
            setCell(row.createCell(5), "بیرون زدگی از حد مجاز", style);

            outFile = new FileOutputStream(outputFileLocation);
            workbook.write(outFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void isFileExist(String fileName) {
        File f = new File(fileName);
        if (f.exists() && !f.isDirectory()) {
            f.delete();
        }
    }

    private void drawGabari(ArrayList<Coordinate[]> result, int gTipNumber, double outOfAllow, double outOfFree, Tab tab) {
        FlowPane box = new FlowPane();
        box.setMinHeight(800);
        box.setHgap(100);
        box.setPadding(new Insets(10, 10, 10, 10));
        GridPane header = new GridPane();
        header.setHgap(20);
        header.setVgap(5);
        header.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        header.setPadding(new Insets(0, 0, 10, 10));
        Pane pane = new Pane();

        if (gTipNumber >= 0) {
            Path path = null;
            GTip gTip = gTips.get(gTipNumber);

            header.add(new Label("نوع گاباری"), 0, 0);
            header.add(new Label(gTip.getName()), 1, 0);

            header.add(new Label("بیرون زدگی از فضای آزاد"), 0, 1);
            header.add(new Label(outOfAllow + " " + "سانتی متر"), 1, 1);

            header.add(new Label("بیرون زدگی از حد مجاز"), 0, 2);
            header.add(new Label(outOfFree + " " + "سانتی متر"), 1, 2);

            for (int j = 0; j < result.size(); j++) {
                path = new Path();
                if (result.get(j).length > 0) {
                    path.getElements().add(new MoveTo(result.get(j)[0].x,
                            Math.abs(gTip.getMaxH() - result.get(j)[0].y)));
                    for (int i = 1; i < result.get(j).length; i++) {
                        path.getElements().add(new LineTo(result.get(j)[i].x,
                                Math.abs(gTip.getMaxH() - result.get(j)[i].y)));
                    }
                    if (j == 0)
                        path.setStroke(new javafx.scene.paint.Color(0, 1, 0, 1));
                    if (j == 1)
                        path.setStroke(new javafx.scene.paint.Color(1, 0.8, 0, 1));
                    if (j == 2)
                        path.setStroke(new javafx.scene.paint.Color(1, 0, 0, 1));

                    path.setStrokeWidth(2);
                    pane.getChildren().add(path);
                }
            }

            path = new Path();
            path.getElements().add(new MoveTo(gTip.getAllowedSpace()[0][0],
                    Math.abs(gTip.getMaxH() - gTip.getAllowedSpace()[0][1])));
            for (int i = 1; i < gTip.getAllowedSpace().length; i++) {
                path.getElements().add(new LineTo(gTip.getAllowedSpace()[i][0],
                        Math.abs(gTip.getMaxH() - gTip.getAllowedSpace()[i][1])));
            }
            path.setStrokeWidth(2);
            pane.getChildren().add(path);

            path = new Path();
            path.getElements().add(new MoveTo(gTip.getFreeSpace()[0][0],
                    Math.abs(gTip.getMaxH() - gTip.getFreeSpace()[0][1])));
            for (int i = 1; i < gTip.getFreeSpace().length; i++) {
                path.getElements().add(new LineTo(gTip.getFreeSpace()[i][0],
                        Math.abs(gTip.getMaxH() - gTip.getFreeSpace()[i][1])));
            }
            path.setStrokeWidth(2);
            AnchorPane.setLeftAnchor(pane, 50.0);

            pane.getChildren().addAll(path);
        } else {
            pane.getChildren().add(new Label("گاباری این بخش مسیر ناشناخته است"));
        }
        box.setAlignment(Pos.TOP_CENTER);
        box.getChildren().addAll(header, pane);
        tab.setContent(box);
    }

    public ArrayList<Coordinate[]> analyzeGabari(GTip gTip) {
        GeometryFactory fact = new GeometryFactory();
        WKTReader wktRdr = new WKTReader(fact);

        float[][] cargoCoordinate = new float[data.size()][2];

        for (int i = 0; i < data.size(); i++) {
            cargoCoordinate[i][0] = data.get(i).x;
            cargoCoordinate[i][1] = data.get(i).y;
        }

        Geometry cargo = createPolygon(cargoCoordinate, wktRdr);
        Geometry allowSpace = createPolygon(gTip.getAllowedSpace(), wktRdr);
        Geometry freeSpace = createPolygon(gTip.getFreeSpace(), wktRdr);

        Coordinate[] cargo1 = cargo.intersection(allowSpace).getCoordinates();
        Coordinate[] cargo2 = cargo.difference(allowSpace).getCoordinates();
        Coordinate[] cargo3 = null;
        Coordinate[] cargo4 = null;
        if (cargo2.length > 0) {
            cargo3 = createPolygon(cargo2, wktRdr).difference(freeSpace).getCoordinates();
        }

        if (cargo3.length > 0)
            cargo4 = createPolygon(cargo2, wktRdr).intersection(freeSpace).getCoordinates();
        else
            cargo4 = cargo2;

        ArrayList<Coordinate[]> result = new ArrayList<>();
        result.add(cargo1);
        result.add(cargo4);
        result.add(cargo3);

        return result;
    }

    public Geometry createPolygon(float[][] cordinates, WKTReader wktRdr) {
        StringBuilder wktA = new StringBuilder("POLYGON ((");
        for (int i = 0; i < cordinates.length; i++) {
            if (i != cordinates.length - 1) {
                wktA.append(cordinates[i][0]).append(" ").append(cordinates[i][1]).append(", ");
            } else {
                wktA.append(cordinates[i][0]).append(" ").append(cordinates[i][1]).append("))");
            }
        }
        try {
            return wktRdr.read(wktA.toString());
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public Geometry createPolygon(Coordinate[] cordinates, WKTReader wktRdr) {
        StringBuilder wktA = new StringBuilder("POLYGON ((");
        for (int i = 0; i < cordinates.length; i++) {
            if (i != cordinates.length - 1) {
                wktA.append(cordinates[i].x).append(" ").append(cordinates[i].y).append(", ");
            } else {
                wktA.append(cordinates[i].x).append(" ").append(cordinates[i].y).append("))");
            }
        }
        try {
            return wktRdr.read(wktA.toString());
        } catch (ParseException e) {
            System.out.println(e.getMessage());
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
}
