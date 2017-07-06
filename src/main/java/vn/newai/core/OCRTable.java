package vn.newai.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import vn.newai.model.TableCell;

public class OCRTable {
	private String parentPath;
	private String lang = "eng";
	private String tessdataPath;

	public OCRTable(String parentPath, String lang, String tessdataPath) {
		this.parentPath = parentPath;
		this.lang = lang;
		this.tessdataPath = tessdataPath;
	}

	/**
	 * OCR all tables exist in parents folder
	 * 
	 * @return {@code Map<String, ArrayList<TableCell>>} a Map with key is id of
	 *         table, value is ArrayList of all cells in table
	 */
	public Map<String, ArrayList<TableCell>> ocrAllTable() {
		Map<String, ArrayList<TableCell>> mapAllTable = new LinkedHashMap<>();
		// Get all folder contain table in path
		File folder = new File(this.parentPath);
		File[] arrFiles = folder.listFiles();
		for (File file : arrFiles) {
			/*-If file is directory and contain "table" in its name then add to list table*/
			if (file.isDirectory() && file.getName().contains("table")) {
				OCROneTable ocrOneTable = new OCROneTable(parentPath + "/" + file.getName(), lang); /*-do OCR a table (all PNG file in directory)*/
				mapAllTable.put(file.getName(), ocrOneTable.doOCR()); /*-add to list table*/
			}
		}
		return mapAllTable;
	}

	private class OCROneTable {
		private String folderPath;
		private String lang = "eng";
		private int numCol, numRow, numCell;

		/**
		 * Constructor. Must contain <b>table.info.txt</b> file in directory to
		 * read table structure
		 * 
		 * @param folderPath
		 *            Path to directory that contain all PNG file of table
		 * @param lang
		 *            Language to OCR
		 */
		public OCROneTable(String folderPath, String lang) {
			this.folderPath = folderPath;
			this.lang = lang;
			readTableStructure();
			removeShadow();
		}

		/**
		 * Read table structure from <b>table.info.txt</b> file in directory
		 */
		private boolean readTableStructure() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(folderPath + "/table.info.txt"), "UTF8"));
				String line = br.readLine();
				if (null != line && !line.isEmpty()) {
					String[] arrConfig = line.split(",");
					if (null != arrConfig && arrConfig.length >= 4) {
						this.numCell = Integer.valueOf(arrConfig[4]);
						this.numRow = Integer.valueOf(arrConfig[5]);
						this.numCol = Integer.valueOf(arrConfig[6]);
						br.close();
						return true;
					}
				}
				br.close();
				return false;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		/**
		 * Remove shadow in each cells of table
		 * 
		 * @throws IOException
		 */
		private void removeShadow() {
			System.out.println("Remove shadow in table " + folderPath);
			File[] arrFiles = new File(this.folderPath).listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".png");
				}
			});
			try {
				for (File file : arrFiles) {
					String cmdArg = "convert -auto-orient -respect-parenthesis \\( " + file.getAbsolutePath() + " -contrast-stretch 0 \\) \\( -clone 0 -colorspace gray -negate -lat 15x15+5% -contrast-stretch 0 \\) -compose copy_opacity -composite -fill white -opaque none -alpha off -modulate 100,200,100 " + file.getAbsolutePath();
					File tempScript = File.createTempFile("script_" + file.getName(), null);
					Writer streamWriter = new OutputStreamWriter(new FileOutputStream(tempScript));
					PrintWriter printWriter = new PrintWriter(streamWriter);
					printWriter.println(cmdArg);
					printWriter.close();
					executeCommands(tempScript);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Execute the command in script file
		 * 
		 * @param scriptFile
		 *            file contains command. This file will be delete when
		 *            command executed
		 */
		private void executeCommands(File scriptFile) {
			try {
				ProcessBuilder pb = new ProcessBuilder("bash", scriptFile.toString());
				pb.inheritIO();
				Process process = pb.start();
				process.waitFor();
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			} finally {
				scriptFile.delete();
			}
		}

		/**
		 * Do OCR all PNG files in directory of a table
		 */
		public ArrayList<TableCell> doOCR() {
			System.out.println("OCR-ing cells of table in dir: " + folderPath);
			System.out.println("cell: " + numCell);
			System.out.println("row: " + numRow);
			System.out.println("col: " + numCol);

			ArrayList<TableCell> listTableCell = new ArrayList<>();
			try {
				for (int i = this.numCell - 1; i >= 1; i--) {
					ITesseract instance = new Tesseract();
					instance.setLanguage(lang);
					instance.setDatapath(tessdataPath);
					instance.setPageSegMode(3);

					String result = "";
					File imageFile = new File(folderPath + "/" + i + ".png");
					if (imageFile.exists()) {
						result = instance.doOCR(new File(folderPath + "/" + i + ".png"));
						result = this.removeRedundantNewLine(result).trim();
						/*-If result is empty, change segment mode to 7 and OCR again*/
						if (null == result || result.isEmpty()) {
							System.out.println("Try hard with segment mode 7");
							instance.setPageSegMode(7);
							result = instance.doOCR(new File(folderPath + "/" + i + ".png"));
							if (null == result || result.isEmpty()) {
								System.out.println("Try hard with segment mode 8");
								instance.setPageSegMode(8);
								result = instance.doOCR(new File(folderPath + "/" + i + ".png"));
								if (null == result || result.isEmpty()) {
									System.out.println("Try hard with segment mode 10");
									instance.setPageSegMode(10);
									result = instance.doOCR(new File(folderPath + "/" + i + ".png"));
								}
							}
						}
					}
					// remove new line
					result = this.removeRedundantNewLine(result).trim();
					if (null == result || result.isEmpty())
						result = " ";
					// create TableCell object
					TableCell cell = new TableCell(i, result);
					// add to cell list
					listTableCell.add(cell);
				}
				return listTableCell;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		/**
		 * Remove all double consecutive newlines by a newline</br>
		 * "<b>\n \n</b>" --> "<b>\n</b>"
		 * 
		 * @param content
		 *            String to remove redundant newlines
		 */
		private String removeRedundantNewLine(String content) {
			content = content.replaceAll("\n \n", "\n");
			while (content.contains("\n")) {
				content = content.replaceAll("\n", " ");
			}
			return content;
		}

	}
}
