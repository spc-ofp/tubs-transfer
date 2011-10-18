/*
 * Copyright (C) 2011 Secretariat of the Pacific Community
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.spc.ofp.tubs.importer;

import org.spc.ofp.tubs.domain.ImportStatus;
import org.spc.ofp.tubs.domain.common.CommonRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * @author Corey Cole <coreyc@spc.int>
 *
 */
public class ExistsFilterProcessor implements ItemProcessor<String, String> {

	@Autowired
	CommonRepository repo;
	
	private String sourceName;
	
	public void setSourceName(final String value) {
		this.sourceName = value;
	}
	
	public String process(final String input) throws Exception {
		Preconditions.checkNotNull(input, "Source Trip Id is null");
		Preconditions.checkArgument(!"".equalsIgnoreCase(input.trim()), "Source Trip Id is blank");
		
		// Not really the caller's fault, but still nothing we can do...
		if (Strings.isNullOrEmpty(Strings.nullToEmpty(sourceName).trim())) {
			return null;
		}

		ImportStatus is = null;
		try { 
			is = repo.findImportStatus(input, sourceName);		
		} catch (Exception ignoreMe) { } // NOPMD
		// Status of 'S' is success, anything else is 'go for it'
		return (null != is && "S".equalsIgnoreCase(is.getStatus())) ?
				null :
				input;
	}

}
