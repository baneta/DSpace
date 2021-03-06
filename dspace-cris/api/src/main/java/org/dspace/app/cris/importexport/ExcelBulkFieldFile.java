/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.importexport;

import org.apache.poi.ss.usermodel.Cell;

public class ExcelBulkFieldFile extends ExcelBulkField implements IBulkChangeFieldFile {
	
	public ExcelBulkFieldFile(Cell element) {
		super(element);
	}

	@Override
	public IBulkChangeFieldFileValue get(int y) {
		return new ExcelBulkFieldFileValue(getElement(), y);
	}
	
}
