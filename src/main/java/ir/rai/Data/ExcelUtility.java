package ir.rai.Data;

import ir.rai.GTip;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.locationtech.jts.geom.Coordinate;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static ir.rai.Data.Assignment.gTips;


public class ExcelUtility {

    public static XSSFCellStyle setStyle(XSSFWorkbook workbook, String fontName, Color color) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setCharSet(FontCharset.ARABIC);
        font.setFontName(fontName);
        style.setFont(font);

        XSSFColor headingColor = new XSSFColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(headingColor);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    public static void setCell(XSSFCell cell, String value, CellStyle style) {
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    public static void setCell(XSSFCell cell, Double value, CellStyle style) {
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    public static void addToExcel(String outputFileLocation, int rowNumber,
                            ArrayList<Coordinate[]> transportable, double outOfAllow,
                            double outOffree, int gTipNumber, String OD) {
        FileOutputStream outFile;

        try (
                FileInputStream inFile = new FileInputStream(outputFileLocation);
                XSSFWorkbook workbook = new XSSFWorkbook(inFile);
        ) {
            XSSFSheet sheet = workbook.getSheet("خروجی");
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
}
