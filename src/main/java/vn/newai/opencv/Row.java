package vn.newai.opencv;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Row {
	private List<Cell> cells;

	public List<Cell> getCells() {
		return cells;
	}

	public void setCells(List<Cell> cells) {
		this.cells = cells;
	}

	public void sort() {
		Collections.sort(cells, new Comparator<Cell>() {
			@Override
			public int compare(Cell c1, Cell c2) {
				return c1.getRect().x - c2.getRect().x;
			}
		});
	}

	@Override
	public String toString() {
		return "Row [cells=" + cells + "]";
	}

	public void reverse() {
		Collections.reverse(cells);
	}

}
