package vn.newai.model;

/**
 * A cell in table
 */
public class TableCell {
	/**
	 * Position of cell in table
	 */
	private int id;
	/**
	 * Value of cell
	 */
	private String value;

	public TableCell(int id, String value) {
		super();
		this.id = id;
		this.value = value;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return getValue();
	}
}
