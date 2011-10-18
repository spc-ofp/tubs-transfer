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


import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spc.ofp.observer.domain.FieldStaffRepository;
import org.spc.ofp.observer.domain.Trip;
import org.spc.ofp.observer.domain.TripRepository;
import org.spc.ofp.observer.domain.VesselRepository;
import org.spc.ofp.observer.domain.longline.LongLineTrip;
import org.spc.ofp.observer.domain.purseseine.PurseSeineTrip;
import org.spc.ofp.observer.domain.purseseine.PurseSeineTripRepository;

import org.springframework.batch.item.ItemProcessor;

import com.google.common.base.Preconditions;

/**
 * @author Corey Cole <coreyc@spc.int>
 *
 */
public class ObserverTripProcessor implements ItemProcessor<String, Trip> {

	@Resource(name = "observer.TripRepository")
	TripRepository tripRepo;
	
	@Resource(name = "observer.PurseSeineTripRepository")
	PurseSeineTripRepository purseSeineTripRepo;
	
	@Resource(name = "observer.FieldStaffRepository")
	FieldStaffRepository observerRepo;
	
	@Resource(name = "observer.VesselRepository")
	VesselRepository vesselRepo;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ObserverTripProcessor.class);
	
	public Trip process(final String tripId) throws Exception {
		Preconditions.checkNotNull(tripId, "TripId is null");
		Preconditions.checkArgument(!"".equalsIgnoreCase(tripId.trim()), "TripId is blank");
		final long id = Long.parseLong(tripId); // Let parseLong throw the exception if it's not numeric
		
		LOGGER.debug("Building object graph for Observer Trip with tripId=" + tripId);
		
		String gearType = null;
		try {
			gearType = tripRepo.getTripType(id);
		} catch (Exception ex) { }
		
		LOGGER.debug("gearType=" + gearType);
		
		// TODO Returning null skips this trip -- we may want to do something else...
		if (null == gearType || "".equalsIgnoreCase(gearType.trim())) { return null; }
		
		final boolean isPurseSeine = "S".equalsIgnoreCase(gearType);
		final boolean isLongLine = "L".equalsIgnoreCase(gearType);
		
		LOGGER.debug("isPurseSeine? " + isPurseSeine);
		LOGGER.debug("isLongLine? " + isLongLine);
		
		// TODO Again, this should skip the trip -- we may want to do something else...
		if (!(isPurseSeine || isLongLine)) { return null; }

		// Delegate filling in the rest of the object graph to a specialized method
		return (isPurseSeine) ? processPurseSeine(id) : processLongLine(id);
	}
	
	private PurseSeineTrip processPurseSeine(final long tripId) {
		LOGGER.debug(String.format("ObserverTripProcessor thinks tripId={%s} is a Purse Seine trip", tripId));
		return purseSeineTripRepo.findById(tripId);
	}
	
	private LongLineTrip processLongLine(final long tripId) {	
		LOGGER.debug(String.format("ObserverTripProcessor thinks tripId={%s} is a Long Line trip", tripId));
		return null;
	}

}
