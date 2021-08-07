package ir.rai;

import java.io.FileInputStream;
import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import jfxtras.styles.jmetro.Style;
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
    ComboBox<String> coordinationBox;

    @FXML
    TableView<Coordinate> table;

    private final ObservableList<Coordinate> data =
            FXCollections.observableArrayList(new CoordinateLW(4,5));
    @FXML
    private void initialize() {
        configureTable();
    }

    private void configureTable() {
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn column1 = new TableColumn("First Parameter");
        column1.setMinWidth(150);
        column1.setCellValueFactory(new PropertyValueFactory<Coordinate, Float>("x"));
        TableColumn column2 = new TableColumn("Second Parameter");
        column2.setMinWidth(150);
        column1.setCellValueFactory(new PropertyValueFactory<Coordinate, Float>("y"));

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
               data.add(new CoordinateXY(4,5));
            }
        });

    }

    @FXML
    private void switchToSecondary() throws ParseException {

        GTip gTip = readGabari();

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

        float[][] cargo = {
                {100, 300},
                {100, 200},
                {300, 200},
                {300, 300},
                {100, 300}
        };

        path = new Path();
        path.getElements().add(new MoveTo(cargo[0][0],
                Math.abs(gTip.getMaxH() - cargo[0][1])));
        for (int i = 1; i < gTip.getFreeSpace().length; i++) {
            path.getElements().add(new LineTo(gTip.getFreeSpace()[i][0],
                    Math.abs(gTip.getMaxH() - gTip.getFreeSpace()[i][1])));
        }

        StringBuffer result = analyzeGabari(gTip, cargo);
        System.out.println(result);
    }

    public GTip readGabari() {
        FileInputStream inFile;
        XSSFWorkbook workbook;
        GTip gTip = new GTip();
        try {
            inFile = new FileInputStream("./gabari.xlsx");
            workbook = new XSSFWorkbook(inFile);

            XSSFSheet sheet = workbook.getSheetAt(0);
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

            return gTip;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public StringBuffer analyzeGabari(GTip gTip, float[][] cargoG) throws ParseException {
        StringBuffer result = new StringBuffer();
        GeometryFactory fact = new GeometryFactory();
        WKTReader wktRdr = new WKTReader(fact);

        Geometry cargo = createPolygon(cargoG, wktRdr);
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
}
