package vn.newai.insertion;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class FileReader {
	/**
	 * Read a CSV file
	 * 
	 * @param filePath
	 *            path to CSV file, must include "<code>.csv</code>" extension
	 */
	public static String[][] readCSV(String filePath, int numRow, int numCol) {
		String inputCSV[][] = new String[numRow][numCol];
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF8"));
			String line;
			int rowCounter = 0;
			while ((line = br.readLine()) != null) {
				String[] tempRow = line.split(",");
				inputCSV[rowCounter] = tempRow;
				rowCounter+=1;
			}
			br.close();
			return inputCSV;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Read a HTML file
	 * 
	 * @param filePath
	 *            path to HTML file, must include "<code>.html</code>" extension
	 */
	public static String readHTML(String filePath) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF8"));
			String line;
			StringBuilder stringBuilder = new StringBuilder();
			while ((line = br.readLine()) != null) {
				stringBuilder.append(line);
			}
			br.close();
			return stringBuilder.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Read table location file
	 * 
	 * @param filePath
	 *            path to file, must include file extension
	 */
	public static String readTableLocation(String filePath) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF8"));
			String line = br.readLine();
			br.close();
			return line;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}
