/*
 * Copyright (C) 2011 Secretariat of the Pacific Community
 *
 * This file is part of TUBS.
 *
 * TUBS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TUBS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with TUBS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.spc.ofp.tubs.importer;

/**
 * DataCleaner holds data cleanliness and lookup utilities that are
 * too much of a hassle to do via database lookups.
 * 
 * These are largely influenced by the "Field Cleaning Functions" module
 * in the DBF2TUBS Access utility program.
 * 
 * @author Corey Cole <coreyc@spc.int>
 *
 */
public class DataCleaner {

	public static Integer getGen6Activity(final Integer value) {
		if (null == value) { return null; }
		switch(value.intValue()) {
			case 1: // fishing
				return 1;
			case 2: // transshipping
			case 3: // bunkering
				return 18;
			case 4: // transiting
				return 3;
			case 5: // aground
				return 92;
			case 6: // drifting
				return 15;
			case 7: // searching
				return 2;
			default:
				return null;
		}
	}
	
	public static Integer getGen6Material(final String value) {
		if (null == value) { return null; }
		final char c = value.trim().charAt(0);
		switch(c) {
			case 'P':
				return 60;
			case 'M':
				return 61;
			case 'W':
				return 62;
			case 'C':
				return 63;
			case 'F':
				return 64;
			case 'G':
				return 65;
			default:
				return null;
		}
	}
	
	public static Integer getGen6Source(final String value) {
		if (null == value) { return null; }
		final char c = value.trim().charAt(0);
		switch(c) {
			case 'A':
				return 66;
			case 'B':
				return 67;
			case 'U':
				return 68;
			case 'L':
				return 69;
			case 'O':
				return 70;
			default:
				return null;
		}
	}
	
	public static String getLineMaterial(final String value) {
		if (null == value || value.trim().isEmpty()) { return null; }
		return value.toUpperCase().contains("MONO") ? "MO" :
			   value.toUpperCase().contains("MOMO") ? "MO" :
			   value.toUpperCase().contains("POLYESTER") ? "PR" :
               value.toUpperCase().contains("KUROLONG") ? "KR" :
               value.toUpperCase().contains("NYLON") ? "TN" :
               value.toUpperCase().contains("LOCK SILVER") ? "LS" :
               value.toUpperCase().contains("TARRED ROPE") ? "PR" :
               null;
	}
	
	public static Integer getPurseSeineAssociation(final Integer value) {
		if (null == value) { return null; }
		switch (value.intValue()) {
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
				return value.intValue() + 20;
			case 9:
				return 119;
			default:
				return null;
		}
	}
	
	public static Integer getPurseSeineDetection(final Integer value) {
		if (null == value) { return null; }
		switch (value.intValue()) {
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				return value.intValue() + 29;
			default:
				return null;
		}
	}
	
	public static Integer getPurseSeineActivity(final Integer value) {
		if (null == value) { return null; }
		switch(value.intValue()) {
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
				return value;
			case 11:
				return 12;
			case 12:
				return 13;
			case 13:
				return 14;
			case 14:
				return 15;
			case 16:
				return 18;
			case 21:
				return 19;
			case 22:
				return 20;
			case 23:
				return 10;
			case 24:
				return 11;
			case 25:
				return 16;
			case 26:
				return 17;
			case 94:
				return 118;
			default:
				return null;
		}
	}
	
	public static String getGen1Activity(final Integer value) {
		if (null == value) { return null; }
		switch(value.intValue()) {
			case 1:
				return "FI";
			case 2:
				return "PF";
			case 3:
				return "NF";
			case 4:
				return "DF";
			case 5:
			case 13:
				return "TG";
			case 6:
			case 14:
				return "SG";
			case 7:
			case 15:
				return "BG";
			case 8:
			case 16:
				return "OG";
			case 9:
				return "TR";
			case 10:
				return "SR";
			case 11:
				return "BR";
			case 12:
				return "OR";
			default:
				return null;
		}
	}
}
