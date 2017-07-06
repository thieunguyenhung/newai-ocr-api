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
import vn.newai.model.OCRLine;

public class PageCombiner {
	private String folderPath, fileName, pageNo;
	private int numTable;
	private ArrayList<OCRLine> listTableLoc, listOCRLine;

	public PageCombiner(String folderPath, String fileName, String pageNo, int numTable) {
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
	private void getOCRLine() {
		this.listOCRLine = new ArrayList<>();
		Document doc = Jsoup.parse(FileReader.readHTML(folderPath + "/" + fileName + "-" + pageNo.replace("page", "") + ".png.hocr"));
		Elements listLine = doc.getElementsByClass("ocr_line");
		for (Element span : listLine) {
			OCRLine line = new OCRLine();
			String[] arrLoc = span.attr("title").split(";")[0].split(" ");
			if (null != arrLoc && arrLoc.length >= 4) {
				line.setTopX(Integer.valueOf(arrLoc[1]));
				line.setTopY(Integer.valueOf(arrLoc[2]));
				line.setBottomX(Integer.valueOf(arrLoc[3]));
				line.setBottomY(Integer.valueOf(arrLoc[4]));
			}
			line.setContent(span.text());
			line.setType(OCRLine.LINE_TYPE_TEXT);
			this.listOCRLine.add(line);
		}
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
					OCRLine tableLine = new OCRLine();
					tableLine.setTopX(Integer.valueOf(arrTableLoc[0]));
					tableLine.setTopY(Integer.valueOf(arrTableLoc[1]));
					tableLine.setBottomX(Integer.valueOf(arrTableLoc[2]));
					tableLine.setBottomY(Integer.valueOf(arrTableLoc[3]));
					tableLine.setContent(pageNo + ".table" + i); /*-put table id to line content*/
					tableLine.setType(OCRLine.LINE_TYPE_TABLE);
					this.listTableLoc.add(tableLine);
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
	public ArrayList<OCRLine> combine() {
		/*-If there are only tables, then add all tables to listOCRLine*/
		if (null == listOCRLine || listOCRLine.size() < 1) {
			for (OCRLine tableLoc : listTableLoc) {
				listOCRLine.add(tableLoc);
			}
			return this.listOCRLine;
		}
		/*-If page contain text and tables*/
		if (numTable > 0) {
			for (OCRLine tableLoc : listTableLoc) {
				Map<Integer, Double> listDistance = new LinkedHashMap<>();
				for (int i = 0; i < listOCRLine.size(); i++) {
					OCRLine line = listOCRLine.get(i);
					listDistance.put(i, getEuclideanDistance(tableLoc.getTopX(), tableLoc.getTopY(), line.getTopX(), line.getTopY()));
				}
				listDistance = sortByValueAscending(listDistance);
				for (Map.Entry<Integer, Double> entry : listDistance.entrySet()) {
					int index = entry.getKey();
					/*-if index = listOCRLine size then just add to last of the list*/
					if (index == listOCRLine.size() - 1)
						listOCRLine.add(tableLoc);
					/*-if index = 0 and topY of table < topY of first line then add to first line*/
					else if (index == 0 && tableLoc.getTopY() < listOCRLine.get(0).getTopY())
						listOCRLine.add(0, tableLoc);
					else
						listOCRLine.add(index + 1, tableLoc);
					break;
				}
			}
		}
		System.out.println(pageNo + " OCRLine list:");
		for (int i = 0; i < listOCRLine.size(); i++) {
			System.out.println(i + ": " + listOCRLine.get(i));
		}
		return this.listOCRLine;
	}
}
