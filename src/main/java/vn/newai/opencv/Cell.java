package vn.newai.opencv;
import org.opencv.core.Rect;

public class Cell {
	private int id;
	private Rect rect;
	
	public Cell(int id, Rect rect) {
		super();
		this.id = id;
		this.rect = rect;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Rect getRect() {
		return rect;
	}

	public void setRect(Rect rect) {
		this.rect = rect;
	}

	@Override
	public String toString() {
		return "Cell [id=" + id + ", rect=" + rect + "]";
	}

	
	
}
