package vn.newai.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.ITesseract.RenderedFormat;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import vn.newai.extraction.OutputCombiner;
import vn.newai.opencv.OpenCVTable;

public class OCR {
	private String folderPath;
	private String fileName;
	private String path;
	private String tessdataPath;
	private String lang = "eng";

	public OCR(File file, String lang, String tessdataPath) {
		this.fileName = file.getName();
		this.path = FilenameUtils.getFullPathNoEndSeparator(file.getAbsolutePath());
		this.lang = lang;
		this.tessdataPath = tessdataPath;
		this.folderPath = "";
		createDirectory();
	}

	private void createDirectory() {
		File file = new File(path + "/" + FilenameUtils.removeExtension(fileName));
		if (file.mkdirs()) {
			folderPath = file.getAbsolutePath();
			System.out.println("Created dir: " + file.getAbsolutePath());
		}
	}

	/**
	 * Delete created directory for processed file
	 * 
	 * @throws IOException
	 */
	public void deleteDirectory() throws IOException {
		FileUtils.deleteDirectory(new File(folderPath));
	}

	/**
	 * Get combined content in HTML
	 * 
	 * @see newai.ocrtessopencv.extraction.FileWriter.writeHTMLFile
	 */
	public String getCombinedHTML() {
		this.processing();
		OCRTable ocrTable = new OCRTable(folderPath, this.lang, this.tessdataPath);
		OutputCombiner combiner = new OutputCombiner(folderPath, this.fileName, ocrTable.ocrAllTable());
		return combiner.getCombinedHTML();
	}

	/**
	 * Get combined content in Excel. Write to file in order to read
	 * 
	 * @see newai.ocrtessopencv.extraction.FileWriter.writeExcelFile
	 */
	public XSSFWorkbook getCombinedMicrosoftExcel() {
		this.processing();
		OCRTable ocrTable = new OCRTable(folderPath, this.lang, this.tessdataPath);
		OutputCombiner combiner = new OutputCombiner(folderPath, this.fileName, ocrTable.ocrAllTable());
		return combiner.getCombinedMicrosoftExcel();
	}

	/**
	 * Get combined content in Microsoft Word (doc or docx file)
	 * 
	 * @see newai.ocrtessopencv.extraction.FileWriter.writeWordFile
	 */
	public XWPFDocument getCombinedMicrosoftWord() {
		this.processing();
		OCRTable ocrTable = new OCRTable(folderPath, this.lang, this.tessdataPath);
		OutputCombiner combiner = new OutputCombiner(folderPath, this.fileName, ocrTable.ocrAllTable());
		return combiner.getCombinedMicrosoftWord();
	}

	/**
	 * Process OCR file
	 * 
	 * @return HTML string
	 */
	private void processing() {
		System.out.println("Start processing: " + fileName);
		try {
			doConvertToPNG();
			renamePNGFile();
			cutTable();
			removeShadow();
			createHOCRFile();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// removeTemp();
		}
		System.out.println("Done processing: " + fileName);
	}

	private void doConvertToPNG() throws IOException {
		// create command
		ConvertCmd cmd = new ConvertCmd();
		// create the operation, add images and operators/options
		IMOperation op = new IMOperation();

		op.colorspace("sRGB");
		op.profile(tessdataPath + "/tessdata/adobe.icc");
		op.density(300);
		op.addRawArgs("-auto-orient");
		op.units("PixelsPerInch");
		op.alpha("off"); // No transparent background
		op.addImage(path + "/" + fileName);
		op.depth(8);
		op.addImage(folderPath + "/" + fileName + ".png");

		// execute the operation
		try {
			cmd.run(op);
		} catch (InterruptedException | IM4JavaException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Converted " + path + "/" + fileName);
		}
	}

	private void renamePNGFile() {
		File[] arrFiles = new File(this.folderPath).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".png");
			}
		});
		if (null != arrFiles && arrFiles.length == 1) {
			File oldFile = arrFiles[0];
			File newFile = new File(this.folderPath + "/" + this.fileName + "-0.png");
			oldFile.renameTo(newFile);
		}
	}

	private void cutTable() {
		File[] arrFiles = new File(this.folderPath).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(fileName) && name.toLowerCase().endsWith(".png");
			}
		});
		Arrays.sort(arrFiles);
		for (int i = 0; i < arrFiles.length; i++) {
			File file = arrFiles[i];
			OpenCVTable openCVTable = new OpenCVTable(this.tessdataPath + "opencv-native-lib/");
			openCVTable.extractTable(i, this.tessdataPath + "uploads/" + FilenameUtils.removeExtension(fileName), file.getName());
		}
	}

	private void removeShadow() throws IOException {
		File[] arrFiles = new File(this.folderPath).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(fileName) && name.toLowerCase().endsWith(".png");
			}
		});
		for (File file : arrFiles) {
			System.out.println("Remove shadow in file " + file.getAbsolutePath());
			String cmdArg = "convert -auto-orient -respect-parenthesis \\( " + file.getAbsolutePath() + " -contrast-stretch 0 \\) \\( -clone 0 -colorspace gray -negate -lat 15x15+5% -contrast-stretch 0 \\) -compose copy_opacity -composite -fill white -opaque none -alpha off -modulate 100,200,100 " + file.getAbsolutePath();
			File tempScript = File.createTempFile("script_" + file.getName(), null);
			Writer streamWriter = new OutputStreamWriter(new FileOutputStream(tempScript));
			PrintWriter printWriter = new PrintWriter(streamWriter);
			printWriter.println(cmdArg);
			printWriter.close();
			executeCommands(tempScript);
		}
	}

	private void createHOCRFile() {
		File[] arrFiles = new File(this.folderPath).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(fileName) && name.toLowerCase().endsWith(".png");
			}
		});
		for (File file : arrFiles) {
			try {
				System.out.println("Do HOCR file " + file.getName());

				ITesseract instance = new Tesseract();
				instance.setLanguage(lang);
				instance.setDatapath(tessdataPath);
				instance.setPageSegMode(3);
				List<RenderedFormat> lis = new ArrayList<>();
				lis.add(RenderedFormat.HOCR);

				// String result = instance.doOCR(file);
				instance.createDocuments(file.getAbsolutePath(), file.getAbsolutePath(), lis);
			} catch (TesseractException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Execute the command in script file
	 * 
	 * @param scriptFile
	 *            file contains command. This file will be delete when command
	 *            executed
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
}
