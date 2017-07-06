package vn.newai.extraction;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import vn.newai.insertion.FileReader;
import vn.newai.model.OCRLine;
import vn.newai.model.OCRWord;
import vn.newai.model.TableCell;

public class OutputCombiner {
	private String fileName;
	private int numPage;
	private String folderPath;
	private Map<String, ArrayList<TableCell>> mapAllTable;

	public OutputCombiner(String folderPath, String fileName, Map<String, ArrayList<TableCell>> mapAllTable) {
		this.folderPath = folderPath;
		this.fileName = fileName;
		this.mapAllTable = mapAllTable;
		this.numPage = this.getNumPage();
	}

	private double getAspectRatio(String imagePath) {
		try {
			BufferedImage bimg = ImageIO.read(new File(imagePath));
			double ratio = Double.valueOf(bimg.getHeight()) * 1265d / 3508d;
			return Double.valueOf(bimg.getHeight()) / ratio;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return 1.0;
	}

	private int getNumPage() {
		File dir = new File(folderPath);
		File[] listOfFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".png");
			}
		});
		return listOfFiles.length;
	}

	/**
	 * Get combined content in HTML (v1)
	 * 
	 * @see newai.ocrtessopencv.extraction.FileWriter.writeHTMLFile
	 */
	private String getCombinedHTMLv1() {
		System.out.println("Start HTML combination process...");
		String resultHTML = "<html><meta charset='UTF-8'><style>table{border-collapse:collapse}td,th{padding:.3em;border:.1em solid black}tr:nth-child(even){background-color:#f2f2f2}</style><body>";
		for (int i = 0; i < this.numPage; i++) {
			final String pageNo = "page" + i;
			// Count table quantity in page i
			File dir = new File(folderPath);
			File[] listOfFiles = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.contains(pageNo + ".table");
				}
			});
			int numTable = listOfFiles.length;
			// Combine the text and tables in page i
			PageCombiner pageCombiner = new PageCombiner(this.folderPath, this.fileName, pageNo, numTable);
			ArrayList<OCRLine> listOCRLine = pageCombiner.combine();
			for (OCRLine line : listOCRLine) {
				switch (line.getType()) {
				case OCRLine.LINE_TYPE_TABLE:
					System.out.println("Reading structure in dir: " + this.folderPath + "/" + line.getContent());
					String[][] tableStructure = readTableStructure(this.folderPath + "/" + line.getContent());
					resultHTML += CSVExtractor.toHTML(mapAllTable.get(line.getContent()), tableStructure, false, null);
					break;
				case OCRLine.LINE_TYPE_TEXT:
					resultHTML += "<p>" + line.getContent() + "</p>";
					break;
				}
			}
		}
		resultHTML += "</body></html>";
		System.out.println("Done HTML combination process");
		return resultHTML;
	}

	/**
	 * Get combined content in HTML
	 * 
	 * @see newai.ocrtessopencv.extraction.FileWriter.writeHTMLFile
	 */
	public String getCombinedHTML() {
		File dir = new File(folderPath);
		File[] listOfFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.contains(".table");
			}
		});
		int numTable = listOfFiles.length;
		if (numTable > 0) {
			System.out.println("Use combine HTML function: getCombinedHTMLv1()");
			return getCombinedHTMLv1();
		}
		System.out.println("Use combine HTML function: getCombinedHTMLWithoutTable()");
		return getCombinedHTMLWithoutTable();
	}

	/**
	 * Get combined content in HTML (v2)
	 * 
	 * @see newai.ocrtessopencv.extraction.FileWriter.writeHTMLFile
	 */
	private String getCombinedHTMLWithoutTable() {
		System.out.println("Start HTML combination process...");
		double lastPageHeight = 0;
		String resultHTML = "<html><meta charset='UTF-8'><style>table{border-collapse:collapse}td,th{padding:.3em;border:.1em solid black}tr:nth-child(even){background-color:#f2f2f2}</style><body>";
		for (int i = 0; i < this.numPage; i++) {
			final String pageNo = "page" + i;
			// Combine the text and tables in page i
			PageCombinerWithoutTable combinerWithoutTable = new PageCombinerWithoutTable(this.folderPath, this.fileName, pageNo, 0);
			ArrayList<ArrayList<OCRWord>> mapLine = combinerWithoutTable.combine();
			double lastTopY = -1;
			double aspectRatio = this.getAspectRatio(folderPath + "/" + fileName + "-" + i + ".png");
			System.out.println("Calculated aspect ratio: " + aspectRatio);
			/** Loop through lines in a page */
			for (ArrayList<OCRWord> listWord : mapLine) {
				resultHTML += " <p>";
				/** Loop through words in a line */
				for (int k = 0; k < listWord.size(); k++) {
					OCRWord ocrWord = listWord.get(k);
					double topX = ocrWord.getTopX() / aspectRatio;
					double topY = ocrWord.getTopY() / aspectRatio + lastPageHeight;
					resultHTML += "<span style='position: absolute; left: " + topX + "px; top: " + topY + "px;'>" + ocrWord.getContent() + " </span>";
					if (k == listWord.size() - 1)
						lastTopY = ocrWord.getTopY() / aspectRatio;
				}
				resultHTML += " </p>";
			}
			lastPageHeight += lastTopY;
		}
		resultHTML += "</body></html>";
		System.out.println("Done HTML combination process");
		return resultHTML;
	}

	/**
	 * Get combined content in HTML (v2)
	 * 
	 * @see newai.ocrtessopencv.extraction.FileWriter.writeHTMLFile
	 */
	/*-private String getCombinedHTMLWithTable() {
		double lastPageHeight = 0;
		System.out.println("Start HTML combination process...");
		String resultHTML = "<html><meta charset='UTF-8'><style>table{border-collapse:collapse}td,th{padding:.3em;border:.1em solid black}tr:nth-child(even){background-color:#f2f2f2}</style><body>";
		for (int i = 0; i < this.numPage; i++) {
			final String pageNo = "page" + i;
			// Count table quantity in page i
			File dir = new File(folderPath);
			File[] listOfFiles = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.contains(pageNo + ".table");
				}
			});
			int numTable = listOfFiles.length;
			PageCombinerWithoutTable combinerWithoutTable = new PageCombinerWithoutTable(this.folderPath, this.fileName, pageNo, numTable);
			ArrayList<ArrayList<OCRWord>> mapLine = combinerWithoutTable.combine();
			double lastTopY = -1;
			double lastBotY = -1;
			double lastHeight = -1;
			double aspectRatio = this.getAspectRatio(folderPath + "/" + fileName + "-" + i + ".png");
			System.out.println("Calculated aspect ratio: " + aspectRatio);
			// Loop through lines in a page 
			for (ArrayList<OCRWord> listWord : mapLine) {
				if (null != listWord && listWord.size() > 0) {
					resultHTML += " <p>";
					// Loop through words in a line 
					for (int k = 0; k < listWord.size(); k++) {
						OCRWord ocrWord = listWord.get(k);
						double topX = ocrWord.getTopX() / aspectRatio;
						double topY = 0;
						if (lastTopY == -1 && lastBotY == -1 && lastHeight == -1)
							topY = ocrWord.getTopY() / aspectRatio;
						switch (ocrWord.getType()) {
						case OCRWord.TYPE_TABLE:
							System.out.println("Reading structure in dir: " + this.folderPath + "/" + ocrWord.getContent());
							String[][] tableStructure = readTableStructure(this.folderPath + "/" + ocrWord.getContent());
							double tableHeight = ocrWord.getBottomY() / Math.ceil(aspectRatio);
							double tableWidth = ocrWord.getBottomX() / Math.ceil(aspectRatio);
							topY = Math.abs(ocrWord.getTopY() / aspectRatio - lastBotY) + lastTopY + lastHeight + lastPageHeight;
							lastHeight = tableHeight;
							lastBotY = ocrWord.getTopY() / aspectRatio + tableHeight;
							resultHTML += "<span style='position: absolute; overflow: auto; width: " + tableWidth + "px; height: " + tableHeight + "px; left: " + topX + "px; top: " + topY + "px;'>";
							resultHTML += CSVExtractor.toHTML(mapAllTable.get(ocrWord.getContent()), tableStructure, false, null);
							break;
						case OCRWord.TYPE_TEXT:
							topY = Math.abs(ocrWord.getTopY() / aspectRatio - lastBotY) + lastTopY + lastHeight + lastPageHeight;
							if (k == listWord.size() - 1) {
								lastHeight = Math.abs(ocrWord.getBottomY() - ocrWord.getTopY()) / aspectRatio;
								lastBotY = ocrWord.getBottomY() / aspectRatio;
							}
							resultHTML += "<span style='position: absolute; left: " + topX + "px; top: " + topY + "px;'>";
							resultHTML += ocrWord.getContent();
							break;
						}
						resultHTML += " </span>";
						if (k == listWord.size() - 1)
							lastTopY = ocrWord.getTopY() / aspectRatio;
					}
					resultHTML += " </p>";
				}
			}
			lastPageHeight += lastTopY + lastHeight;
		}
		resultHTML += "</body></html>";
		System.out.println("Done HTML combination process");
		return resultHTML;
	}*/

	/**
	 * Get combined content in Excel. Write result to file in order to read
	 * 
	 * @see newai.ocrtessopencv.extraction.FileWriter.writeExcelFile
	 */
	public XSSFWorkbook getCombinedMicrosoftExcel() {
		System.out.println("Start Excel combination process...");
		XSSFWorkbook workbook = new XSSFWorkbook();
		for (int i = 0; i < this.numPage; i++) {
			final String pageNo = "page" + i;
			// Count table quantity in page i
			File dir = new File(folderPath);
			File[] listOfFiles = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.contains(pageNo + ".table");
				}
			});
			Arrays.sort(listOfFiles);
			for (File file : listOfFiles) {
				System.out.println("Reading structure in dir: " + file.getAbsolutePath());
				String[][] tableStructure = readTableStructure(file.getAbsolutePath());
				workbook = CSVExtractor.toExcel(mapAllTable.get(file.getName()), tableStructure, workbook, file.getName());
			}
		}
		System.out.println("Done Excel combination process");
		return workbook;
	}

	/**
	 * Get combined content in Microsoft Word (doc or docx file)
	 * 
	 * @see newai.ocrtessopencv.extraction.FileWriter.writeWordFile
	 */
	public XWPFDocument getCombinedMicrosoftWord() {
		System.out.println("Start MicrosoftWord combination process...");
		XWPFDocument wordDocument = new XWPFDocument();
		for (int i = 0; i < this.numPage; i++) {
			final String pageNo = "page" + i;
			// Count table quantity in page i
			File dir = new File(folderPath);
			File[] listOfFiles = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.contains(pageNo + ".table");
				}
			});
			int numTable = listOfFiles.length;
			// Combine the text and tables in page i
			PageCombiner pageCombiner = new PageCombiner(this.folderPath, this.fileName, pageNo, numTable);
			ArrayList<OCRLine> listOCRLine = pageCombiner.combine();
			// System.out.println(listOCRLine);
			for (OCRLine line : listOCRLine) {
				switch (line.getType()) {
				case OCRLine.LINE_TYPE_TABLE:
					System.out.println("Reading structure in dir: " + this.folderPath + "/" + line.getContent());
					String[][] tableStructure = readTableStructure(this.folderPath + "/" + line.getContent());
					wordDocument = CSVExtractor.toWord(mapAllTable.get(line.getContent()), tableStructure, wordDocument);
					break;
				case OCRLine.LINE_TYPE_TEXT:
					XWPFParagraph paragraph = wordDocument.createParagraph();
					XWPFRun run = paragraph.createRun();
					run.setText(line.getContent());
					break;
				}
			}
		}
		System.out.println("Done MicrosoftWord combination process");
		return wordDocument;
	}

	/**
	 * Read table structure from <b>table.info.txt</b> file in directory
	 */
	private String[][] readTableStructure(String folderPath) {
		try {
			int numRow, numCol;
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(folderPath + "/table.info.txt"), "UTF8"));
			String line = br.readLine();
			if (null != line && !line.isEmpty()) {
				String[] arrConfig = line.split(",");
				if (null != arrConfig && arrConfig.length >= 4) {
					numRow = Integer.valueOf(arrConfig[5]);
					numCol = Integer.valueOf(arrConfig[6]);
					br.close();
					return FileReader.readCSV(folderPath + "/struc.txt", numRow, numCol);
				}
			}
			br.close();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/*-public static void main(String[] args) {
		String folderPath = "/home/anonym/Documents/eclipseWS/newai-ocr-opencv-api/uploads/work-result";
		OCRTable ocrTable = new OCRTable(folderPath, "eng", "/home/anonym/Documents/eclipseWS/newai-ocr-opencv-api/src/main/webapp");
		OutputCombiner combiner = new OutputCombiner(folderPath, "work.png", ocrTable.ocrAllTable());
		// FileWriter.writeHTMLFile(combiner.getCombinedHTML(),
		// "/home/anonym/Documents/eclipseWS/newai-ocr-opencv-api/uploads/work.html");
		-FileWriter.writeExcelFile(combiner.getCombinedMicrosoftExcel(), "/home/anonym/Documents/eclipseWS/newai-ocr-opencv-api/uploads/work-result.xls");
		FileWriter.writeWordFile(combiner.getCombinedMicrosoftWord(), "/home/anonym/Documents/eclipseWS/newai-ocr-opencv-api/uploads/work.docx");
	}*/
}
