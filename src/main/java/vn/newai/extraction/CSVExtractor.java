package vn.newai.extraction;

import java.util.ArrayList;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableCell.XWPFVertAlign;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import vn.newai.model.TableCell;

public class CSVExtractor {
	private static TableCell getCellById(ArrayList<TableCell> listTableCell, int cellId) {
		for (TableCell cell : listTableCell) {
			if (cell.getId() == cellId)
				return cell;
		}
		return null;
	}

	/**
	 * Convert 2D Array List to HTML
	 * 
	 * @param lsitTableCell
	 *            input CSV file content
	 * @param tableStructure
	 *            Table structure from struc.txt file
	 * @param saveToFile
	 *            <ul>
	 *            <li><b>true</b> if you want to save result to file.</li>
	 *            <li><b>false</b> otherwise.</li>
	 *            </ul>
	 * @param filePath
	 *            path to save, must include "<code>.html</code>" extension.
	 *            Leave it null if <b>saveToFile</b> is false.
	 */
	public static String toHTML(ArrayList<TableCell> listTableCell, String[][] tableStructure, boolean saveToFile, String filePath) {
		/** Exit if input is null or empty list */
		if (null == listTableCell || listTableCell.size() < 1) {
			return "";
		}

		/** Exit if there is no table structure */
		if (null == tableStructure || tableStructure.length < 1) {
			return "";
		}

		/*-for (int r = 0; r < tableStructure.length; r++) {
			for (int c = 0; c < tableStructure[r].length; c++) {
				System.out.print(tableStructure[r][c] + " ");
			}
			System.out.println();
		}*/

		/** Loop to detect column span */
		ArrayList<ArrayList<String>> listTableHTML = new ArrayList<>();
		// Loop row
		for (int r = 0; r < tableStructure.length; r++) {
			ArrayList<String> listRowHTML = new ArrayList<>();
			// Loop column
			String lastCellId = tableStructure[r][0];
			int colSpan = 1;
			for (int c = 1; c < tableStructure[r].length; c++) {
				if (lastCellId.equals(tableStructure[r][c])) {
					colSpan += 1;
					/** If last cell then create it already */
					if (c == tableStructure[r].length - 1) {
						String cellHTML = "<td id='" + tableStructure[r][c] + "' colspan='" + colSpan + "'>" + getCellById(listTableCell, Integer.valueOf(tableStructure[r][c])) + "</td>";
						listRowHTML.add(cellHTML);
					}
				} else {
					String cellHTML = "<td id='" + lastCellId + "'";
					if (colSpan > 1)
						cellHTML += " colspan='" + colSpan + "'";
					TableCell cell = getCellById(listTableCell, Integer.valueOf(lastCellId));
					if (null != cell)
						cellHTML += ">" + cell.getValue() + "</td>";
					else
						cellHTML += "></td>";
					colSpan = 1;
					listRowHTML.add(cellHTML);
					/** If last cell then create it already */
					if (c == tableStructure[r].length - 1) {
						cell = getCellById(listTableCell, Integer.valueOf(tableStructure[r][c]));
						if (null != cell)
							cellHTML = "<td id='" + tableStructure[r][c] + "'>" + cell.getValue() + "</td>";
						else
							cellHTML = "<td id='" + tableStructure[r][c] + "'></td>";
						listRowHTML.add(cellHTML);
					}
				}
				lastCellId = tableStructure[r][c];
			}
			listTableHTML.add(listRowHTML);
		}

		/** Loop to detect row span */
		ArrayList<Integer> visitedCellId = new ArrayList<>();
		/** Loop through cell id */
		for (TableCell cell : listTableCell) {
			/** Check if id is processed */
			if (!visitedCellId.contains(cell.getId())) {
				boolean firstCell = false; /*-To determine if reach this cell for the first time*/
				int rowSpan = 1; /*-Count row span*/
				int firstRow = 0; /*-Row number of first cell contain ID*/
				int firstCol = 0; /*-Column number of first cell contain ID*/
				visitedCellId.add(cell.getId());
				// loop row
				for (int r = 0; r < listTableHTML.size(); r++) {
					// loop column
					for (int c = 0; c < listTableHTML.get(r).size(); c++) {
						/** If cell is empty then continue */
						if (listTableHTML.get(r).get(c).isEmpty())
							continue;
						/** Use Jsoup to get id attribute */
						Document doc = Jsoup.parse("<table>" + listTableHTML.get(r).get(c) + "</table>");
						Element tdElement = doc.select("td").first();
						int cellIdHTML = Integer.valueOf(tdElement.attr("id"));
						if (cellIdHTML == cell.getId()) {
							if (firstCell) { /*-Not first time*/
								listTableHTML.get(r).set(c, "");
								rowSpan += 1;
							} else { /*-Reaches this cell for the first time*/
								firstRow = r;
								firstCol = c;
								firstCell = true;
							}
						}
					}
				}
				/** Use Jsoup to get HTML Element of the first cell */
				Document doc = Jsoup.parse("<table>" + listTableHTML.get(firstRow).get(firstCol) + "</table>");
				Element tdElement = doc.select("td").first();
				tdElement.attr("rowspan", String.valueOf(rowSpan)); /*-Add attribute rowspan to element*/
				listTableHTML.get(firstRow).set(firstCol, tdElement.toString());
			}
		}

		/** Combine all cells to HTML file */
		String resultHTML = "<table>";
		for (ArrayList<String> rowHTML : listTableHTML) {
			resultHTML += "<tr>";
			for (String cellHTML : rowHTML) {
				resultHTML += cellHTML;
			}
			resultHTML += "</tr>";
		}
		resultHTML += "</table>";

		// Check if save to file
		if (saveToFile) {
			if (null != filePath && !filePath.isEmpty()) {
				FileWriter.writeHTMLFile(resultHTML, filePath);
			}
		}
		return resultHTML;
	}

	/**
	 * Convert 2D Array List to Microsoft Excel file
	 * 
	 * @param csvFileContent
	 *            input CSV file content
	 */
	public static XSSFWorkbook toExcel(ArrayList<TableCell> listTableCell, String[][] tableStructure, XSSFWorkbook workbook, String sheetName) {
		/** Exit if input is null or empty list */
		if (null == listTableCell || listTableCell.size() < 1) {
			return null;
		}

		/** Exit if there is no table structure */
		if (null == tableStructure || tableStructure.length < 1) {
			return null;
		}

		/** Exit if workbook is null */
		if (null == workbook) {
			return null;
		}

		/** Create excel sheet */
		XSSFSheet sheet = workbook.createSheet(sheetName);
		// Border
		XSSFCellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setBorderBottom(BorderStyle.THIN);
		cellStyle.setBorderTop(BorderStyle.THIN);
		cellStyle.setBorderLeft(BorderStyle.THIN);
		cellStyle.setBorderRight(BorderStyle.THIN);
		cellStyle.setAlignment(HorizontalAlignment.CENTER);
		cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

		int rowNum = 0;
		for (int r = 0; r < tableStructure.length; r++) {
			Row excelRow = sheet.createRow(rowNum++);
			int colNum = 0;
			for (int c = 0; c < tableStructure[r].length; c++) {
				TableCell cell = getCellById(listTableCell, Integer.valueOf(tableStructure[r][c]));
				Cell excelCell = excelRow.createCell(colNum++);
				excelCell.setCellStyle(cellStyle);
				if (null != cell)
					excelCell.setCellValue(cell.getValue());
				else
					excelCell.setCellValue("");
			}
		}

		/** Merge all cells that have the same id */
		ArrayList<TableCell> visitedCell = new ArrayList<>();
		for (TableCell cell : listTableCell) {
			if (!visitedCell.contains(cell)) {
				visitedCell.add(cell);
				boolean firstCell = false;
				int firstRow = -1, firstCol = -1, lastRow = -1, lastCol = -1;
				// loop row
				for (int r = 0; r < tableStructure.length; r++) {
					// loop column
					for (int c = 0; c < tableStructure[r].length; c++) {
						if (cell.getId() == Integer.valueOf(tableStructure[r][c])) {
							if (firstCell) { /*-Not first time*/
								lastRow = r;
								lastCol = c;
							} else { /*-Reaches this cell for the first time*/
								firstRow = r;
								firstCol = c;
								lastRow = r;
								lastCol = c;
								firstCell = true;
							}
						}
					}
				}
				if (firstRow != lastRow || firstCol != lastCol) {
					try {
						sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		return workbook;
	}

	/**
	 * Convert 2D Array List to Microsoft Word file
	 * 
	 * @param csvFileContent
	 *            input CSV file content
	 * @param filePath
	 *            path to save, must include "<code>.docx</code>" extension.
	 */
	public static XWPFDocument toWord(ArrayList<TableCell> listTableCell, String[][] tableStructure, XWPFDocument document) {
		/** Exit if input is null or empty list */
		if (null == listTableCell || listTableCell.size() < 1) {
			return null;
		}

		/** Exit if there is no table structure */
		if (null == tableStructure || tableStructure.length < 1) {
			return null;
		}

		/** Exit if workbook is null */
		if (null == document) {
			return null;
		}

		/** Create table in word document */
		XWPFTable table = document.createTable();
		for (int r = 0; r < tableStructure.length; r++) {
			XWPFTableRow tableRow = table.createRow();
			tableRow.getCell(0).setText(getCellById(listTableCell, Integer.valueOf(tableStructure[r][0])).getValue());
			for (int c = 1; c < tableStructure[r].length; c++) {
				TableCell cell = getCellById(listTableCell, Integer.valueOf(tableStructure[r][c]));
				if (null != cell)
					tableRow.addNewTableCell().setText(cell.getValue());
				else
					tableRow.addNewTableCell().setText("");
			}
		}
		table.removeRow(0);

		/** Center vertically cells */
		for (XWPFTableRow row : table.getRows()) {
			for (XWPFTableCell cell : row.getTableCells()) {
				cell.setVerticalAlignment(XWPFVertAlign.CENTER);
			}
		}

		/** Merge all cells that have the same id */
		for (TableCell cell : listTableCell) {
			boolean firstCell = false;
			int firstRow = -1, firstCol = -1, lastRow = -1, lastCol = -1;
			// loop row
			for (int r = 0; r < tableStructure.length; r++) {
				// loop column
				for (int c = 0; c < tableStructure[r].length; c++) {
					if (cell.getId() == Integer.valueOf(tableStructure[r][c])) {
						if (firstCell) { /*-Not first time*/
							lastRow = r;
							lastCol = c;
						} else { /*-Reaches this cell for the first time*/
							firstRow = r;
							firstCol = c;
							lastRow = r;
							lastCol = c;
							firstCell = true;
						}
					}
				}
			}
			/*-span column*/
			if (firstCol != lastCol) {
				for (int r = firstRow; r <= lastRow; r++) {
					for (int c = firstCol; c <= lastCol; c++) {
						if (c == firstCol)
							table.getRow(r).getCell(c).getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
						else
							table.getRow(r).getCell(c).getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
					}
				}
			}
			/*-span row*/
			if (firstRow != lastRow) {
				for (int c = firstCol; c <= lastCol; c++) {
					for (int r = firstRow; r <= lastRow; r++) {
						if (r == firstRow)
							table.getRow(r).getCell(c).getCTTc().addNewTcPr().addNewVMerge().setVal(STMerge.RESTART);
						else
							table.getRow(r).getCell(c).getCTTc().addNewTcPr().addNewVMerge().setVal(STMerge.CONTINUE);
					}
				}
			}
		}
		return document;
	}
}
