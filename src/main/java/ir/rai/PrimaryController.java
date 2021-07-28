package ir.rai;

import java.io.FileInputStream;
import java.io.IOException;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorImportFromJson;
import com.esri.core.geometry.Polygon;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PrimaryController {
    @FXML
    Group group;

    @FXML
    private void switchToSecondary() {

        GTip gTip = readGabari();

        Path path = new Path();
        path.getElements().add(new MoveTo(gTip.getAllowedSpace()[0][0], gTip.getAllowedSpace()[0][1]));
        for (int i = 1; i < gTip.getAllowedSpace().length; i++) {
            path.getElements().add(new LineTo(gTip.getAllowedSpace()[i][0], gTip.getAllowedSpace()[i][1]));
        }
        group.getChildren().add(path);

        path = new Path();
        path.getElements().add(new MoveTo(gTip.getFreeSpace()[0][0], gTip.getFreeSpace()[0][1]));
        for (int i = 1; i < gTip.getFreeSpace().length; i++) {
            path.getElements().add(new LineTo(gTip.getFreeSpace()[i][0], gTip.getFreeSpace()[i][1]));
        }
        group.getChildren().add(path);
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
                        (float) Math.abs((row.getCell(0).getNumericCellValue() / 10) - gTip.getMaxH());

                if (i == 0) {
                    gTip.getFreeSpace()[2 * freeSpaceCorners][0] =
                            (float) row.getCell(1).getNumericCellValue() / 10;
                    gTip.getFreeSpace()[2 * freeSpaceCorners][1] =
                            (float) Math.abs((row.getCell(0).getNumericCellValue() / 10) - gTip.getMaxH());
                }

                gTip.getFreeSpace()[2 * freeSpaceCorners - i - 1][0] =
                        (float) row.getCell(2).getNumericCellValue() / 10;
                gTip.getFreeSpace()[2 * freeSpaceCorners - i - 1][1] =
                        (float) Math.abs((row.getCell(0).getNumericCellValue() / 10) - gTip.getMaxH());
            }

            for (int i = 0; i < allowedSpaceCorners; i++) {
                row = sheet.getRow((i) + 4);
                gTip.getAllowedSpace()[i][0] =
                        (float) row.getCell(5).getNumericCellValue() / 10;
                gTip.getAllowedSpace()[i][1] =
                        (float) Math.abs((row.getCell(4).getNumericCellValue() / 10) - gTip.getMaxH());

                if (i == 0) {
                    gTip.getAllowedSpace()[2 * allowedSpaceCorners][0] =
                            (float) row.getCell(5).getNumericCellValue() / 10;
                    gTip.getAllowedSpace()[2 * allowedSpaceCorners][1] =
                            (float) Math.abs((row.getCell(4).getNumericCellValue() / 10) - gTip.getMaxH());
                }

                gTip.getAllowedSpace()[2 * allowedSpaceCorners - i - 1][0] =
                        (float) row.getCell(6).getNumericCellValue() / 10;
                gTip.getAllowedSpace()[2 * allowedSpaceCorners - i - 1][1] =
                        (float) Math.abs((row.getCell(4).getNumericCellValue() / 10) - gTip.getMaxH());
            }

            return gTip;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
