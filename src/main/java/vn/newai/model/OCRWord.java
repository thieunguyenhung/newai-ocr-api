package vn.newai.model;

public class OCRWord {
	private int topX, topY, bottomX, bottomY;
	private String content;

	private String type;

	public static final String TYPE_TABLE = "table";
	public static final String TYPE_TEXT = "text";

	public OCRWord() {
		this.topX = 0;
		this.topY = 0;
		this.bottomX = 0;
		this.bottomY = 0;
		this.content = "";
		this.type = TYPE_TEXT;
	}

	public OCRWord(int topX, int topY, int bottomX, int bottomY, String content, String type) {
		super();
		this.topX = topX;
		this.topY = topY;
		this.bottomX = bottomX;
		this.bottomY = bottomY;
		this.content = content;
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
		return "[" + topX + " " + topY + " " + bottomX + " " + bottomY + "] " + content;
	}
}
