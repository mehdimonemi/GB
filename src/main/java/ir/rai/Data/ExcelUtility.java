package ir.rai.Data;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.awt.Color;


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
}
