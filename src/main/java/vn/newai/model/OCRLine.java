package vn.newai.model;

public class OCRLine {
	private int topX, topY, bottomX, bottomY;
	private String content;
	private String type;

	public static final String LINE_TYPE_TABLE = "table";
	public static final String LINE_TYPE_TEXT = "text";

	public OCRLine() {
		this.topX = 0;
		this.topY = 0;
		this.bottomX = 0;
		this.bottomY = 0;
		this.content = "";
		this.type = LINE_TYPE_TEXT;
	}

	public OCRLine(int topX, int topY, int bottomX, int bottomY, String content, String type) {
		this.topX = topX;
		this.topY = topY;
		this.bottomX = bottomX;
		this.bottomY = bottomY;
		this.content = content;
		this.type = type;
	}

	public int getTopX() {
		return topX;
	}

	public void setTopX(int topX) {
		this.topX = topX;
	}

	public int getTopY() {
		return topY;
	}

	public void setTopY(int topY) {
		this.topY = topY;
	}

	public int getBottomX() {
		return bottomX;
	}

	public void setBottomX(int bottomX) {
		this.bottomX = bottomX;
	}

	public int getBottomY() {
		return bottomY;
	}

	public void setBottomY(int bottomY) {
		this.bottomY = bottomY;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "[" + topX + " " + topY + " " + bottomX + " " + bottomY + "] " + type + ": " + content;
	}

}
