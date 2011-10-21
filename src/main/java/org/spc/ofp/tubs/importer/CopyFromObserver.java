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

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.spc.ofp.observer.domain.TripIdRepository;
import org.spc.ofp.tubs.domain.AuditEntry;
import org.spc.ofp.tubs.domain.ImportStatus;
import org.spc.ofp.tubs.domain.purseseine.PurseSeineTrip;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import com.google.common.base.Throwables;

/**
 * @author Corey Cole <coreyc@spc.int>
 *
 */
@Component
public class CopyFromObserver {

	@Resource(name = "observer.TripIdRepository")
	protected TripIdRepository tripIdRepository;
	
	@Resource(name = "ExistsFilterProcessor")
	protected ExistsFilterProcessor existsFilterProcessor;
	
	@Resource(name = "ObserverTripProcessor")
	protected ItemProcessor<String, org.spc.ofp.observer.domain.ITrip> observerTripProcessor;
	
	@Resource(name = "TubsTripProcessor")
	protected ItemProcessor<org.spc.ofp.observer.domain.ITrip, org.spc.ofp.tubs.domain.Trip> tubsTripProcessor;
	
	@Resource(name = "purseseine.TripRepository")
	protected org.spc.ofp.tubs.domain.purseseine.TripRepository targetTripRepository;
	
	@Resource
	protected org.spc.ofp.tubs.domain.common.CommonRepository commonRepo;
	
	private static final String[] SPRING_CONFIGS = {
		"tubs-context.xml", /* Target setup */
		"observer-context.xml", /* Source setup */
		"copy-observer-to-tubs.xml" /* Setup for this application */
	};
	
	private static final String SOURCE_NAME = "FoxPro Observer";
	private static final String ENTERED_BY = "TubsTripProcessor"; // TODO Add SVN string?
	
	/**
	 * @param args
	 */
	public static void main(final String[] args) throws Exception {		
		// FIXME Add argument parsing.
		// FIXME With all the required libraries, will probably have to use Maven to execute
		final ApplicationContext ctx = new ClassPathXmlApplicationContext(SPRING_CONFIGS);
		ctx.getBean(CopyFromObserver.class).doCopy();
	}
	
	public CopyFromObserver() {}
	
	private static AuditEntry getAuditEntry() {
		final AuditEntry entry = new AuditEntry();
		entry.setEnteredBy(ENTERED_BY);
		entry.setEnteredDate(new Date());
		return entry;
	}
	
	// FIXME Accept arguments for gear type, limit, and year from caller
	public void doCopy() {
		existsFilterProcessor.setSourceName(SOURCE_NAME);
		// tripIdRepository is the driving query
		final List<Long> tripIds = tripIdRepository.findTripIdsByGearAndYear("S", 12L, "1999", "2000");
		for (final Long tripId : tripIds) {
			final String id = Integer.toString(tripId.intValue());
			System.out.println("Processing tripId: " + id);
			final ImportStatus status = new ImportStatus();
			status.setSourceId(id);			
			status.setSourceName(SOURCE_NAME);
			status.setStatus("F"); // Assume import will fail
			status.setAuditEntry(getAuditEntry());
			try {
				// Check to see if trip already exists
				final String checkedId = existsFilterProcessor.process(id);
				// existsFilterProcessor returns null to signal that this ID has already been copied
				if (null == checkedId || "".equalsIgnoreCase(checkedId.trim())) { continue; }
				System.out.println("...doesn't exist in target system...");
				// Convert the ID to an Observer trip
				final org.spc.ofp.observer.domain.ITrip sourceTrip = observerTripProcessor.process(checkedId);
				// Convert the Observer trip to a TUBS trip
				final org.spc.ofp.tubs.domain.purseseine.PurseSeineTrip targetTrip = (PurseSeineTrip)tubsTripProcessor.process(sourceTrip);
				if (null == targetTrip) { continue; }
				System.out.println("...can be converted to a TUBS object...");
				// Write the trip using JPA
				targetTripRepository.save(targetTrip);
				System.out.println("...written to target DB with ID=" + targetTrip.getId());
				status.setTripId(targetTrip.getId());
				status.setStatus("S");
			} catch (Exception ex) {
				status.setComments(
				    String.format(
				        "Error summary: {%s}\nFull stack trace:\n%s",
				        ex.getMessage(),
				        Throwables.getStackTraceAsString(ex)
				    )
				);
				
				System.out.println(
				    String.format(
				        "Skipping trip %s due to error {%s}",
				        id,
				        ex.getMessage()
				    )
				);
				ex.printStackTrace(System.err);
			}
			commonRepo.saveImportStatus(status);
		}
	}

}
