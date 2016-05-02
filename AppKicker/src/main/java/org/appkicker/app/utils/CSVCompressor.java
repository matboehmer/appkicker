package org.appkicker.app.utils;

/**
 * <p>
 * This class can be used to remove redundant information from from a table when
 * writing it into a csv file. Basically, it blanks those fields that do not
 * change from one row to the following one.
 * </p>
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class CSVCompressor {
	
	/** buffers the string of the csv file */
	private StringBuffer sb;

	/** data of the last row put to the table */
	private String[] lastRow;

	/**
	 * Add a new row of data to the table.
	 * 
	 * @param add
	 */
	public void add(String[] add) {
		if (sb == null) {

			sb = new StringBuffer();

			// if we do not have a first last row, we cannot compress anything
			// redundant
			int size = add.length;
			for (int i = 0; i < (size - 1); i++) {
				sb.append((add[i] == null) ? "nil" : add[i]);
				sb.append(',');
			}
			sb.append((add[size - 1] == null) ? "nil" : add[size - 1]);

		} else {
			sb.append('\n');

			// compress: do not add values that are contained in the last row
			int size = add.length;
			for (int i = 0; i < (size - 1); i++) {
				if(lastRow[i] == null) {
					if(add[i] != null) sb.append(add[i]);
				}
				else if (!lastRow[i].equals(add[i])) {
					sb.append((add[i] == null) ? "nil" : add[i]);
					}

				sb.append(',');
			}
			
			if(lastRow[size - 1] == null) {
				if(add[size - 1] != null) sb.append(add[size - 1]);
			} else if (!lastRow[size - 1].equals(add[size - 1])) {
				sb.append((add[size - 1] == null) ? "nil" : add[size - 1]);
			}
		}

		lastRow = add;
	}

	/**
	 * Returns the data as a "compressed" csv string with redundant values
	 * removed.
	 * 
	 * @return
	 */
	public String getCSV() {
		return sb.toString();
	}

}