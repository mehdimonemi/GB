package ir.rai;

import java.io.FileInputStream;
import java.io.IOException;

import com.esri.core.geometry.*;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PrimaryController {
    @FXML
    Pane pane;

    @FXML
    private void switchToSecondary() {

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
                {100, gTip.getMaxH() - 300},
                {100, gTip.getMaxH() - 200},
                {300, gTip.getMaxH() - 200},
                {300, gTip.getMaxH() - 300},
                {100, gTip.getMaxH() - 300}
        };

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

    public StringBuffer analyzeGabari(GTip gTip, float[][] cargoG) {
        StringBuffer result = new StringBuffer();

        Polyline cargo = new Polyline();
        cargo.startPath(cargoG[0][0], cargoG[0][1]);
        for (int i = 1; i < cargoG.length; i++) {
            cargo.lineTo(cargoG[i][0], cargoG[i][1]);
        }

        Polyline allowSpace = new Polyline();
        allowSpace.startPath(gTip.getAllowedSpace()[0][0], gTip.getAllowedSpace()[0][1]);
        for (int i = 1; i < gTip.getAllowedSpace().length; i++) {
            allowSpace.lineTo(gTip.getAllowedSpace()[i][0], gTip.getAllowedSpace()[i][1]);
        }

        Polyline freeSpace = new Polyline();
        freeSpace.startPath(gTip.getFreeSpace()[0][0], gTip.getFreeSpace()[0][1]);
        for (int i = 1; i < gTip.getFreeSpace().length; i++) {
            freeSpace.lineTo(gTip.getFreeSpace()[i][0], gTip.getFreeSpace()[i][1]);
        }

        if (OperatorContains.local().execute(allowSpace, cargo, null, null)) {
            result.append("cargo is okay with allow Space\n");
        } else {
            result.append("cargo is not okay with allow Space\n");
        }

        if (OperatorContains.local().execute(freeSpace, cargo, null, null)) {
            result.append("cargo is okay with free Space\n");
        } else {
            result.append("cargo is not okay with free Space\n");
        }

        return result;
    }
}
