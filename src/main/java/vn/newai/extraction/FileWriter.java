package vn.newai.extraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class FileWriter {
	/**
	 * Write HTML String to file
	 * 
	 * @param html
	 *            HTML String to write
	 * @param filePath
	 *            path to save, must include "<code>.html</code>" extension.
	 */
	public static void writeHTMLFile(String html, String filePath) {
		try {
			File fileOut = new File(filePath);
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut), "UTF8"));
			out.append(html);
			out.flush();
			out.close();
			System.out.println("Write to HTML file completed in " + filePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write XSSFWorkbook object to Excel file
	 * 
	 * @param workbook
	 *            XSSFWorkbook object to write
	 * @param filePath
	 *            path to save, must include "<code>.xls</code> or
	 *            <code>.xlsx</code>" extension.
	 */
	public static void writeExcelFile(XSSFWorkbook workbook, String filePath) {
		try {
			FileOutputStream outputStream = new FileOutputStream(filePath, false);
			workbook.write(outputStream);
			workbook.close();
			outputStream.close();
			System.out.println("Write to Excel file completed in " + filePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write XSSFWorkbook object to Word file
	 * 
	 * @param wordDocument
	 *            XWPFDocument object to write
	 * @param filePath
	 *            path to save, must include "<code>.doc</code> or
	 *            <code>.docx</code>" extension.
	 */
	public static void writeWordFile(XWPFDocument wordDocument, String filePath) {
		try {
			FileOutputStream outputStream = new FileOutputStream(filePath, false);
			wordDocument.write(outputStream);
			wordDocument.close();
			outputStream.close();
			System.out.println("Write to Word file completed in  " + filePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
