package vn.newai.opencv;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class OpenCVTable {

	private static final int PADDING = 5;
	private static final int SCALE = 2;
	private static final int SCALE_MODE = Imgproc.INTER_CUBIC;
	private String opencvNativeLibPath;

	public OpenCVTable(String opencvNativeLibPath) {
		this.opencvNativeLibPath = opencvNativeLibPath;
	}

	public void extractTable(int page, String folderPath, String imgName) {

		System.out.println("Cuting table in file: " + imgName);
		// System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		System.load(this.opencvNativeLibPath + "libopencv_java320.so");

		Mat src = Imgcodecs.imread(folderPath + "/" + imgName);

		Rect r = new Rect(10, 10, src.width() - 10, src.height() - 10);

		src = src.submat(r);

		Mat src_gray = new Mat();

		if (src == null)
			System.out.println("Problem loading image!!!");

		if (src.channels() == 3) {
			Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_BGR2GRAY);
		} else {
			src_gray = src.clone();
		}

		Mat not_gray = new Mat();
		Core.bitwise_not(src_gray, not_gray);

		// Apply adaptiveThreshold at the bitwise_not of gray, notice the ~
		// symbol
		Mat bw = new Mat();
		Imgproc.adaptiveThreshold(not_gray, bw, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, -2);

		// Create the images that will use to extract the horizonta and vertical
		// lines
		Mat horizontal = bw.clone();
		Mat vertical = bw.clone();

		int scale = 15; // play with this variable in order to increase/decrease
						// the amount of lines to be detected

		// Specify size on horizontal axis
		int horizontalsize = horizontal.cols() / scale;

		// Create structure element for extracting horizontal lines through
		// morphology operations
		Mat horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(horizontalsize, 1));

		Imgproc.erode(horizontal, horizontal, horizontalStructure, new Point(-1, -1), 1);
		Imgproc.dilate(horizontal, horizontal, horizontalStructure, new Point(-1, -1), 1);

		int verticalsize = vertical.rows() / scale;

		// Create structure element for extracting vertical lines through
		// morphology operations
		Mat verticalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, verticalsize));

		// Apply morphology operations
		Imgproc.erode(vertical, vertical, verticalStructure, new Point(-1, -1), 1);
		Imgproc.dilate(vertical, vertical, verticalStructure, new Point(-1, -1), 1);

		Mat mask = new Mat();
		Core.add(horizontal, vertical, mask);
		// Imgcodecs.imwrite("mask.png", mask);

		Mat joints = new Mat();
		Core.bitwise_and(horizontal, vertical, joints);
		// Imgcodecs.imwrite("joint.png", joints);

		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

		int j = 0;

		for (int i = 0; i < contours.size(); i++) {
			double area = Imgproc.contourArea(contours.get(i));

			if (area < 100)
				continue;

			MatOfPoint2f approxContour2f = new MatOfPoint2f();
			Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approxContour2f, 3.0, true);
			Rect rect = Imgproc.boundingRect(new MatOfPoint(approxContour2f.toArray()));

			Mat roi = joints.submat(rect);

			List<MatOfPoint> joints_contours = new ArrayList<>();
			Mat hierarchy2 = new Mat();
			Imgproc.findContours(roi, joints_contours, hierarchy2, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

			if (joints_contours.size() <= 4)
				continue;

			Table table = new Table();
			table.setNo(j);

			table.setParentName(folderPath);

			table.setPage(page);
			table.setX(rect.x + 10);
			table.setY(rect.y + 10);
			table.setWidth(rect.width);
			table.setHeight(rect.height);

			try {
				fillTable(folderPath + "/" + imgName, table.getRect());
			} catch (IOException e) {
				e.printStackTrace();
			}

			Mat cell = src.submat(rect).clone();

			int top, bottom, left, right;
			top = 2;
			bottom = 2;
			left = 2;
			right = 2;

			Core.copyMakeBorder(cell, cell, top, bottom, left, right, Core.BORDER_CONSTANT, new Scalar(0, 0, 0));

			extractCells(cell, table);
			j++;

		}

	}

	public void extractCells(Mat src, Table table) {
		Mat src_gray = new Mat();

		if (src == null)
			System.out.println("Problem loading image!!!");

		if (src.channels() == 3) {
			Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_BGR2GRAY);
		} else {
			src_gray = src;
		}

		Mat not_gray = new Mat();
		Core.bitwise_not(src_gray, not_gray);

		// Apply adaptiveThreshold at the bitwise_not of gray, notice the ~
		// symbol
		Mat bw = new Mat();
		Imgproc.adaptiveThreshold(not_gray, bw, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, -2);

		// Create the images that will use to extract the horizonta and vertical
		// lines
		Mat horizontal = bw.clone();
		Mat vertical = bw.clone();

		int scale = 30; // play with this variable in order to increase/decrease

		// the amount of lines to be detected

		// Specify size on horizontal axis
		int horizontalsize = horizontal.cols() / scale;

		// Create structure element for extracting horizontal lines through
		// morphology operations
		Mat horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(horizontalsize, 1));

		Imgproc.erode(horizontal, horizontal, horizontalStructure, new Point(-1, -1), 1);
		Imgproc.dilate(horizontal, horizontal, horizontalStructure, new Point(-1, -1), 1);

		int verticalsize = vertical.rows() / scale;

		// Create structure element for extracting vertical lines through
		// morphology operations
		Mat verticalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, verticalsize));

		// Apply morphology operations
		Imgproc.erode(vertical, vertical, verticalStructure, new Point(-1, -1), 1);
		Imgproc.dilate(vertical, vertical, verticalStructure, new Point(-1, -1), 1);

		Mat mask = new Mat();
		Core.add(horizontal, vertical, mask);
		// Imgcodecs.imwrite("mask.png", mask);
		Imgproc.blur(mask, mask, new Size(5, 5));

		Mat joints = new Mat();
		Core.bitwise_and(horizontal, vertical, joints);

		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

		List<Rect> boundRect = new ArrayList<>();

		List<Mat> rois = new ArrayList<>();

		List<Cell> cells = new ArrayList<>();

		for (int i = 0; i < contours.size(); i++) {

			MatOfPoint2f approxContour2f = new MatOfPoint2f();
			Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approxContour2f, 3.0, true);
			Rect rect = Imgproc.boundingRect(new MatOfPoint(approxContour2f.toArray()));

			if (rect.height <= 20 || rect.width <= 20)
				continue;

			Mat cell = src.submat(rect).clone();
			if (cell.width() > 0 && cell.height() > 0) {
				boundRect.add(rect);
				rois.add(cell);
			}
		}

		File dir = new File(table.getParentName() + "/page" + table.getPage() + "." + "table" + table.getNo());
		dir.mkdir();

		for (int j = 1; j < rois.size(); j++) {
			cropCell(rois.get(j), j, table);
			cells.add(new Cell(j, boundRect.get(j)));
		}

		Collections.reverse(cells);

		List<Row> tableStruct = new ArrayList<>();
		List<Cell> cellInRow = new ArrayList<>();

		while (!cells.isEmpty()) {
			Iterator<Cell> it = cells.iterator();
			Cell firstCell = it.next();
			cellInRow.add(firstCell);
			int minY = firstCell.getRect().y, minHeight = firstCell.getRect().height;

			while (it.hasNext()) {
				Cell cell = it.next();

				int avg = cell.getRect().y + cell.getRect().height / 2;

				if (avg > minY && avg < minY + minHeight) {
					cellInRow.add(cell);
					if (cell.getRect().y < minY)
						minY = cell.getRect().y;
					if (cell.getRect().height < minHeight)
						minHeight = cell.getRect().height;
				}
			}

			if (!cellInRow.isEmpty()) {
				Row row = new Row();
				row.setCells(new ArrayList<>(cellInRow));
				row.sort();
				tableStruct.add(row);
				cells.removeAll(cellInRow);
				cellInRow.clear();
			}
		}

		for (Row ro : tableStruct) {
			System.out.println(ro);
		}

		int cols = maxCols(tableStruct);
		int rows = tableStruct.size();

		int[][] arr = new int[rows][cols];

		int longestId = longestRow(tableStruct);
		Row longestRow = tableStruct.get(longestId);

		for (int i = 0; i < tableStruct.size(); i++) {
			for (int c = 0; c < longestRow.getCells().size(); c++) {

				Rect longRect = longestRow.getCells().get(c).getRect();
				int avg = longRect.x + longRect.width / 2;

				List<Cell> cs = tableStruct.get(i).getCells();
				for (int j = 0; j < cs.size(); j++) {
					if (c == 0 && i == 0) {
						arr[i][c] = cs.get(0).getId();
						break;
					} else {
						Rect rect = cs.get(j).getRect();
						if (rect.x < avg && rect.x + rect.width > avg) {
							arr[i][c] = cs.get(j).getId();
							break;
						}
					}
				}
			}
		}

		fixSpan(arr);

		printStruct(dir + "/struc.txt", arr);

		table.setNumCells(rois.size());
		table.setNumCols(cols);
		table.setNumRows(rows);
		table.writeInfo();
	}

	public void cropCell(Mat src, int j, Table table) {

		Mat src_gray = new Mat();

		if (src == null)
			System.out.println("Problem loading image!!!");

		if (src.channels() == 3) {
			Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_BGR2GRAY);
		} else {
			src_gray = src;
		}

		Mat threshold_output = new Mat(), hierarchy = new Mat();
		List<MatOfPoint> contours = new ArrayList<>();
		Imgproc.threshold(src_gray, threshold_output, 100, 255, Imgproc.THRESH_BINARY);

		Imgproc.findContours(threshold_output, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

		List<Point> points = new ArrayList<>();

		for (int i = 1; i < contours.size(); i++) {
			points.addAll(contours.get(i).toList());
		}

		if (contours.size() > 1) {
			MatOfPoint2f approxContour2f = new MatOfPoint2f();
			MatOfPoint2f allContour = new MatOfPoint2f();
			allContour.fromList(points);
			Imgproc.approxPolyDP(allContour, approxContour2f, 3.0, true);
			Rect rect = Imgproc.boundingRect(new MatOfPoint(approxContour2f.toArray()));

			if (rect.x - PADDING > 0)
				rect.x -= PADDING;
			else
				rect.x = 1;
			if (rect.y - PADDING > 0)
				rect.y -= PADDING;
			else
				rect.y = 1;

			if (rect.height + PADDING * 2 < src.height())
				rect.height += PADDING * 2;
			else
				rect.height = src.height();

			if (rect.width + PADDING * 2 < src.width())
				rect.width += PADDING * 2;
			else
				rect.width = src.width();

			if (rect.x + rect.width > src.cols())
				rect.width = src.cols() - rect.x;
			if (rect.y + rect.height > src.rows())
				rect.height = src.rows() - rect.y;

			Mat crop = src.submat(rect);

			Mat rsz = new Mat();

			Imgproc.resize(crop, rsz, new Size(crop.width() * SCALE, crop.height() * SCALE), 0, 0, SCALE_MODE);

			Imgcodecs.imwrite(table.getParentName() + "/page" + table.getPage() + "." + "table" + table.getNo() + "/" + j + ".png", rsz);

		} else
			Imgcodecs.imwrite(table.getParentName() + "/page" + table.getPage() + "." + "table" + table.getNo() + "/" + j + "-blank" + ".png", src);

	}

	public static void fillTable(String fileName, Rectangle rect) throws IOException {
		File image = new File(fileName);
		// String ext = FilenameUtils.getExtension(fileName);

		String ext = "png";
		BufferedImage bi = ImageIO.read(image);

		Graphics2D graphics = bi.createGraphics();
		graphics.setPaint(new Color(255, 255, 255));
		graphics.fill(rect);
		ImageIO.write(bi, ext, image);
	}

	private int maxCols(List<Row> table) {
		int max = 0;
		for (Row row : table) {
			if (row.getCells().size() > max)
				max = row.getCells().size();
		}
		return max;
	}

	private int longestRow(List<Row> table) {
		int longest = 0;
		int longestId = 0;
		for (int i = 0; i < table.size(); i++) {
			Row row = table.get(i);
			if (row.getCells().size() > longest) {
				longest = row.getCells().size();
				longestId = i;
			}
		}
		return longestId;
	}

	private void printStruct(String path, int[][] arr) {
		try {
			PrintWriter writer = new PrintWriter(path, "UTF-8");

			for (int i = 0; i < arr.length; i++) {
				for (int j = 0; j < arr[i].length; j++) {
					writer.print(String.valueOf(arr[i][j]));
					if (j != arr[i].length - 1)
						writer.print(",");
				}

				writer.println();
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void fixSpan(int[][] arr) {
		for (int i = 0; i < arr.length; i++) {
			for (int j = 0; j < arr[i].length; j++) {
				if (arr[i][j] == 0)
					if (i - 1 >= 0)
						arr[i][j] = arr[i - 1][j];
			}
		}
	}

}