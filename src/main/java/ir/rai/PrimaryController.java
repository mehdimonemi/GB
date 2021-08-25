package ir.rai;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import ir.rai.Data.Assignment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Callback;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import static jfxtras.styles.jmetro.JMetroStyleClass.*;

public class PrimaryController {
    @FXML
    Pane pane;

    @FXML
    Button addButton;

    @FXML
    TextField add1Para, add2Para, origin, destination;

    @FXML
    ComboBox<String> coordinationBox;

    @FXML
    TableView<Coordinate> table;

    private final ObservableList<Coordinate> data =
            FXCollections.observableArrayList(
                    new Coordinate(100f, 300f),
                    new Coordinate(100f, 200f),
                    new Coordinate(300f, 200f),
                    new Coordinate(300f, 300f),
                    new Coordinate(100f, 300f)
            );

    ArrayList<GTip> gTips = new ArrayList<>();


    @FXML
    private void initialize() {
        Assignment assignment = new Assignment(origin.getText(), destination.getText());
        configureTable();
    }

    private void configureTable() {
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setEditable(true);

        Callback<TableColumn<Coordinate, Float>, TableCell<Coordinate, Float>> cellFactory =
                p -> new EditingCell();

        TableColumn column1 = new TableColumn("First Parameter");
        column1.setMinWidth(150);
        column1.setCellValueFactory(new PropertyValueFactory<Coordinate, Float>("x"));
        column1.setCellFactory(cellFactory);
        column1.setOnEditCommit(
                (EventHandler<CellEditEvent<Coordinate, Float>>) t -> {
                    ((Coordinate) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())).setX(t.getNewValue());
                });

        TableColumn<Coordinate, Float> column2 = new TableColumn<Coordinate, Float>("Second Parameter");
        column2.setMinWidth(150);
        column2.setCellValueFactory(new PropertyValueFactory<Coordinate, Float>("y"));
        column2.setCellFactory(cellFactory);
        column2.setOnEditCommit(
                (EventHandler<CellEditEvent<Coordinate, Float>>) t -> {
                    ((Coordinate) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())).setX(t.getNewValue());
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
                data.add(new Coordinate(Float.parseFloat(add1Para.getText()), Float.parseFloat(add2Para.getText())));
            }
        });

    }

    @FXML
    private void calculate() throws ParseException {

        analyzeCommodity();
        drawGabari(gTips.get(0));

    }

    private void drawGabari(GTip gTip) {
        Path path = new Path();
        path.getElements().add(new MoveTo(gTip.getAllowedSpace()[0][0],
                Math.abs(gTip.getMaxH() - gTip.getAllowedSpace()[0][1])));
        for (int i = 1; i < gTip.getAllowedSpace().length; i++) {
            path.getElements().add(new LineTo(gTip.getAllowedSpace()[i][0],
                    Math.abs(gTip.getMaxH() - gTip.getAllowedSpace()[i][1])));
        }
        pane.getChildren().add(path);

        path = new Path();
        path.getElements().add(new MoveTo(gTip.getFreeSpace()[0][0],
                Math.abs(gTip.getMaxH() - gTip.getFreeSpace()[0][1])));
        for (int i = 1; i < gTip.getFreeSpace().length; i++) {
            path.getElements().add(new LineTo(gTip.getFreeSpace()[i][0],
                    Math.abs(gTip.getMaxH() - gTip.getFreeSpace()[i][1])));
        }
        pane.getChildren().add(path);


        path = new Path();
        path.getElements().add(new MoveTo(data.get(0).getX(),
                Math.abs(gTip.getMaxH() - data.get(0).getY())));
        for (int i = 1; i < data.size(); i++) {
            path.getElements().add(new LineTo(data.get(i).getX(),
                    Math.abs(gTip.getMaxH() - data.get(i).getY())));
        }
        pane.getChildren().add(path);
    }

    private void analyzeCommodity() {

    }

    public void readGabari() {
        FileInputStream inFile;
        XSSFWorkbook workbook;
        GTip gTip = new GTip();
        try {
            inFile = new FileInputStream("./gabari.xlsx");
            workbook = new XSSFWorkbook(inFile);

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                gTip.setMaxH((int) sheet.getRow(1).getCell(0).getNumericCellValue() / 10);
                gTip.setMaxW((int) sheet.getRow(1).getCell(1).getNumericCellValue() / 10);

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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StringBuffer analyzeGabari(GTip gTip, ObservableList<Coordinate> cargoG) throws ParseException {
        StringBuffer result = new StringBuffer();
        GeometryFactory fact = new GeometryFactory();
        WKTReader wktRdr = new WKTReader(fact);

        float[][] cargoCoordinate = new float[cargoG.size()][2];

        for (int i = 0; i < cargoG.size(); i++) {
            cargoCoordinate[i][0] = cargoG.get(i).x;
            cargoCoordinate[i][1] = cargoG.get(i).y;
        }

        Geometry cargo = createPolygon(cargoCoordinate, wktRdr);
        Geometry allowSpace = createPolygon(gTip.getAllowedSpace(), wktRdr);
        Geometry freeSpace = createPolygon(gTip.getFreeSpace(), wktRdr);

        if (allowSpace.contains(cargo)) {
            result.append("cargo is okay with allow Space\n");
        } else {
            result.append("cargo is not okay with allow Space\n");
        }

        if (freeSpace.contains(cargo)) {
            result.append("cargo is okay with free Space\n");
        } else {
            result.append("cargo is not okay with free Space\n");
        }

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

    class EditingCell extends TableCell<Coordinate, Float> {

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
}
