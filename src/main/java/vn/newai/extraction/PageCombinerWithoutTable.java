package vn.newai.extraction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import vn.newai.insertion.FileReader;
import vn.newai.model.OCRWord;

public class PageCombinerWithoutTable {
	private String folderPath, fileName, pageNo;
	private int numTable;
	private ArrayList<OCRWord> listTableLoc;
	private ArrayList<ArrayList<OCRWord>> listLines;

	public PageCombinerWithoutTable(String folderPath, String fileName, String pageNo, int numTable) {
		this.folderPath = folderPath;
		this.fileName = fileName;
		this.pageNo = pageNo;
		this.numTable = numTable;
		this.getOCRLine();
		this.getTableLoc();
	}

	/**
	 * Read page text from .HOCR file
	 */
	public ArrayList<ArrayList<OCRWord>> getOCRLine() {
		this.listLines = new ArrayList<>();
		Document doc = Jsoup.parse(FileReader.readHTML(folderPath + "/" + fileName + "-" + pageNo.replace("page", "") + ".png.hocr"));
		Elements listLine = doc.getElementsByClass("ocr_line");
		for (int i = 0; i < listLine.size(); i++) {
			Element spanLines = listLine.get(i);
			ArrayList<OCRWord> listWord = new ArrayList<>();
			Elements spanWords = spanLines.getElementsByClass("ocrx_word");
			int topY = Integer.valueOf(spanLines.attr("title").split(";")[0].split(" ")[2]);
			int botY = Integer.valueOf(spanLines.attr("title").split(";")[0].split(" ")[4]);
			for (Element word : spanWords) {
				OCRWord ocrWord = new OCRWord();
				String[] arrWordLoc = word.attr("title").split(";")[0].split(" ");
				if (null != arrWordLoc && arrWordLoc.length >= 4) {
					ocrWord.setTopX(Integer.valueOf(arrWordLoc[1]));
					ocrWord.setTopY(topY);
					ocrWord.setBottomX(Integer.valueOf(arrWordLoc[3]));
					ocrWord.setBottomY(botY);
				}
				ocrWord.setType(OCRWord.TYPE_TEXT);
				ocrWord.setContent(word.text());
				if (null != ocrWord.getContent() && !ocrWord.getContent().isEmpty())
					listWord.add(ocrWord);
			}
			if (null != listWord && listWord.size() > 0)
				this.listLines.add(listWord);
		}
		return this.listLines;
	}

	/**
	 * Read table location from file
	 */
	private void getTableLoc() {
		this.listTableLoc = new ArrayList<>();
		for (int i = 0; i < numTable; i++) {
			String strTableLoc = FileReader.readTableLocation(folderPath + "/" + pageNo + ".table" + i + "/table.info.txt");
			if (null != strTableLoc && !strTableLoc.isEmpty()) {
				String[] arrTableLoc = strTableLoc.split(",");
				if (null != arrTableLoc && arrTableLoc.length >= 4) {
					OCRWord tableWord = new OCRWord();
					tableWord.setTopX(Integer.valueOf(arrTableLoc[0]));
					tableWord.setTopY(Integer.valueOf(arrTableLoc[1]));
					tableWord.setBottomX(Integer.valueOf(arrTableLoc[2]));
					tableWord.setBottomY(Integer.valueOf(arrTableLoc[3]));
					tableWord.setContent(pageNo + ".table" + i); /*-put table id to line content*/
					tableWord.setType(OCRWord.TYPE_TABLE);
					this.listTableLoc.add(tableWord);
				}
			}
		}
	}

	/**
	 * Calculate the Euclidean Distance between two point
	 * 
	 * @param x1
	 *            point1 x value
	 * @param y1
	 *            point1 y value
	 * @param x2
	 *            point2 x value
	 * @param y2
	 *            point2 y value
	 */
	private double getEuclideanDistance(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	}

	/**
	 * Sort ascending
	 * 
	 * @param map
	 *            Map to sort
	 */
	private <K, V extends Comparable<? super V>> Map<K, V> sortByValueAscending(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	/**
	 * Get the final result with text and table (if exist)
	 */
	public ArrayList<ArrayList<OCRWord>> combine() {
		/*-If there are only tables, then add all tables to listOCRLine*/
		if (null == listLines || listLines.size() < 1) {
			for (OCRWord tableLoc : listTableLoc) {
				ArrayList<OCRWord> listTableInLine = new ArrayList<>();
				listTableInLine.add(tableLoc);
				listLines.add(listTableInLine);
			}
			return this.listLines;
		}
		/*-If page contain text and tables*/
		if (numTable > 0) {
			for (OCRWord tableLoc : listTableLoc) {
				Map<Integer, Double> listDistance = new LinkedHashMap<>();
				for (int i = 0; i < listLines.size(); i++) {
					if (null != listLines.get(i) && listLines.get(i).size() > 0) {
						OCRWord word = listLines.get(i).get(0);
						listDistance.put(i, getEuclideanDistance(tableLoc.getTopX(), tableLoc.getTopY(), word.getTopX(), word.getTopY()));
					}
				}
				listDistance = sortByValueAscending(listDistance);
				for (Map.Entry<Integer, Double> entry : listDistance.entrySet()) {
					int index = entry.getKey();
					ArrayList<OCRWord> listTableInLine = new ArrayList<>();
					listTableInLine.add(tableLoc);
					/*-if index = listOCRLine size then just add to last of the list*/
					if (index == listLines.size() - 1)
						listLines.add(listTableInLine);
					/*-if index = 0 and topY of table < topY of first line then add to first line*/
					else if (index == 0 && tableLoc.getTopY() < listLines.get(0).get(0).getTopY())
						listLines.add(0, listTableInLine);
					else
						listLines.add(index + 1, listTableInLine);
					break;
				}
			}
		}
		return listLines;
	}

}
