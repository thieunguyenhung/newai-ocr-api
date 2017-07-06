package vn.newai.opencv;
import java.awt.Rectangle;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Table {
	private String parentName;
	private int no;
	private int page;

	private int x;
	private int y;
	private int width;
	private int height;

	private int numCells;
	private int numCols;
	private int numRows;

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getNumCells() {
		return numCells;
	}

	public void setNumCells(int numCells) {
		this.numCells = numCells;
	}

	public int getNumCols() {
		return numCols;
	}

	public void setNumCols(int numCols) {
		this.numCols = numCols;
	}

	public int getNumRows() {
		return numRows;
	}

	public void setNumRows(int numRows) {
		this.numRows = numRows;
	}
	
	public Rectangle getRect(){
		return new Rectangle(x, y, width, height);
	}

	@Override
	public String toString() {
		return x + "," + y + "," + width + "," + height + "," + numCells + "," + numRows + "," + numCols;
	}

	public void writeInfo() {
		String fileName = parentName + "/page" + page + "." + "table" + no + "/table.info.txt";

		try {
			PrintWriter out = new PrintWriter(fileName);
			out.println(toString());
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
