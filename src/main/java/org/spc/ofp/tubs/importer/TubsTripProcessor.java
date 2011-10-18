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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spc.ofp.observer.domain.FieldStaff;
import org.spc.ofp.observer.domain.FishTransfer;
import org.spc.ofp.observer.domain.Gen3;
import org.spc.ofp.observer.domain.Gen6Detail;
import org.spc.ofp.observer.domain.Gen6Header;
import org.spc.ofp.observer.domain.Port;
import org.spc.ofp.observer.domain.Trip;
import org.spc.ofp.observer.domain.Vessel;
import org.spc.ofp.observer.domain.VesselSighting;
import org.spc.ofp.observer.domain.purseseine.DayLog;
import org.spc.ofp.observer.domain.purseseine.FishingDay;
import org.spc.ofp.tubs.domain.common.CommonRepository;
import org.spc.ofp.tubs.domain.purseseine.Activity;
import org.spc.ofp.tubs.domain.purseseine.Day;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Corey Cole <coreyc@spc.int>
 *
 */
public class TubsTripProcessor implements ItemProcessor<Trip, org.spc.ofp.tubs.domain.Trip> {

	@Autowired
	protected CommonRepository repo;
	
	private static final String ENTERED_BY = "TubsTripProcessor"; // TODO Add SVN string?
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TubsTripProcessor.class);
	
	/**
	 * combine merges a date and a time to create a single java.util.Date instance
	 * that represents a date and time.  This utility is necessary due to source data
	 * that separates date and time fields.
	 * @param date
	 * @param time
	 * @return
	 */
	public static Date combine(final Date date, final String time) {
		if (null == date || null == time || "".equalsIgnoreCase(time)) { return date; }
		// FIXME Add a restriction on all Date fields in the TUBS domain such that they must be after Dec 31, 1980.
		
		final Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		try {
			// Get hour and minute
			final int hours = Integer.parseInt(time.trim().substring(0, 2));
			final int minutes = Integer.parseInt(time.trim().substring(2, 4));
			// Only set a valid hour and minute
			if (hours >= 0 && hours < 23 && minutes >= 0 && minutes < 59) {
				cal.set(Calendar.HOUR_OF_DAY, hours);
				cal.set(Calendar.MINUTE, minutes);
			}
		} catch (Exception ex) { } // NOPMD
		return cal.getTime();
	}
	
	public static String translateAnswer(final Boolean answer) {
		return answer == null ? null : answer ? "Y" : "N";
	}
	
	public org.spc.ofp.tubs.domain.Trip process(Trip trip) throws Exception {
		// TODO Is this what we want? 
		if (null == trip) { 
			LOGGER.info("Input object was null, skipping...");
			return null; 
		} 
		
		LOGGER.debug("Gear Type: " + trip.getGearType());
		LOGGER.debug("isPurseSeineTrip()? " + trip.isPurseSeineTrip());
		LOGGER.debug("isLongLineTrip()? " + trip.isLongLineTrip());
		LOGGER.debug("isPoleAndLineTrip()? " + trip.isPoleAndLineTrip());
		
		// Convert the generic 'trip' to a strongly-typed version based on gear.
		// At the same time, create a strongly-typed target trip.
		org.spc.ofp.tubs.domain.Trip tubsTrip = null;
		if (trip.isPurseSeineTrip()) {
			LOGGER.debug("Incoming trip is purse seine.  Converting source trip");
			trip = new org.spc.ofp.observer.domain.purseseine.PurseSeineTrip(trip);
			tubsTrip = new org.spc.ofp.tubs.domain.purseseine.PurseSeineTrip();
		} else if (trip.isLongLineTrip()) {
			LOGGER.debug("Incoming trip is long line.  Converting source trip");
			trip = new org.spc.ofp.observer.domain.longline.LongLineTrip(trip);
			tubsTrip = new org.spc.ofp.tubs.domain.longline.LongLineTrip();
		} else {
			LOGGER.debug("Incoming trip has unsupported gear type: " + trip.getGearType());
		}

		LOGGER.debug("After checking for trip type, 'tubsTrip' is null? " + (null == tubsTrip));    
		// Skip trips with unsupported gear types.
		if (null == tubsTrip) { return null; }
		
		LOGGER.debug("tubsTrip is of type " + tubsTrip.getClass().getName());
		    
		// Basic stuff
		tubsTrip.setAuditEntry(getAuditEntry());
		tubsTrip.setProgramCode(trip.getProgramId());
		tubsTrip.setStaffCode(trip.getObserverId());
		tubsTrip.setObserver(asTubsObserver(trip.getObserver()));
		tubsTrip.setTripNumber(trip.getTripNumber());
		
		// Departure
		tubsTrip.setDepartureDate(trip.getDepartureDate());
		tubsTrip.setDeparturePort(asTubsPort(trip.getDeparturePort()));
		
		// Return
		tubsTrip.setReturnDate(trip.getReturnDate());
		tubsTrip.setReturnPort(asTubsPort(trip.getReturnPort()));
		
		tubsTrip.setVessel(asTubsVessel(trip.getBoat()));
		
		// GEN-1
		tubsTrip.setVesselSightings(asTubsSightings(trip.getVesselSightings()));
		tubsTrip.setFishTransfers(asTubsTransfers(trip.getFishTransfers()));
		
		// GEN-2
		
		// GEN-3
		tubsTrip.setTripReport(asTubsGen3(trip.getGen3Report()));
		tubsTrip.getTripReport().setTrip(tubsTrip);
		
		// GEN-6
		tubsTrip.setPollutionReports(asTubsGen6(trip.getPollutionReports()));
		
		
		// Fill object graph based on gear type
		if (trip.isPurseSeineTrip()) {
			tubsTrip = fillPurseSeineDetails(
					(org.spc.ofp.observer.domain.purseseine.PurseSeineTrip) trip,
					(org.spc.ofp.tubs.domain.purseseine.PurseSeineTrip) tubsTrip);
		} else if (trip.isLongLineTrip()) {
			tubsTrip = fillLongLineDetails(
					(org.spc.ofp.observer.domain.longline.LongLineTrip) trip,
					(org.spc.ofp.tubs.domain.longline.LongLineTrip) tubsTrip);
		}
		return tubsTrip;
	}
	
	protected org.spc.ofp.tubs.domain.purseseine.PurseSeineTrip fillPurseSeineDetails(
			final org.spc.ofp.observer.domain.purseseine.PurseSeineTrip obsvTrip,
			final org.spc.ofp.tubs.domain.purseseine.PurseSeineTrip tubsTrip) {
		
		// FIXME Gear and crew aren't here (yet!) because of issues in migrating source data		
		// DayLog holds most of the Purse Seine specific observer data	
		// SNARK Right about here lambda functionality would come in handy...
		tubsTrip.setDays(asTubsDays(obsvTrip.getFishingDays()));
		obsvTrip.getDaylogs();
		return tubsTrip;
	}
	
	protected org.spc.ofp.tubs.domain.longline.LongLineTrip fillLongLineDetails(
			final org.spc.ofp.observer.domain.longline.LongLineTrip obsvTrip,
			final org.spc.ofp.tubs.domain.longline.LongLineTrip tubsTrip) {
		// FIXME Not implemented
		return tubsTrip;
	}
	
	public static org.spc.ofp.tubs.domain.AuditEntry getAuditEntry() {
		final org.spc.ofp.tubs.domain.AuditEntry auditEntry = new org.spc.ofp.tubs.domain.AuditEntry();
		auditEntry.setEnteredBy(ENTERED_BY);
		auditEntry.setEnteredDate(new Date());
		return auditEntry;
	}
	
	protected List<Day> asTubsDays(final List<FishingDay> observerDays) {
		if (null == observerDays) { return Collections.emptyList(); }
		
		final List<Day> tubsDays = new ArrayList<Day>(observerDays.size());
		for (final FishingDay fishingDay : observerDays) {
			tubsDays.add(asTubsDay(fishingDay));			
		}
		return tubsDays;
	}
	
	protected List<Activity> asTubsActivities(final List<DayLog> dll) {
		if (null == dll) { return Collections.emptyList(); }
		
		final List<Activity> tubsActivities = new ArrayList<Activity>(dll.size());
		for (final DayLog activity : dll) {
			tubsActivities.add(asTubsActivity(activity));
		}
		return tubsActivities;
	}
	
	protected Activity asTubsActivity(final DayLog dl) {
		if (null == dl) { return null; }
		Activity activity = new Activity();
		activity.setActivityType(
			repo.findReferenceValueById
				(DataCleaner.getPurseSeineActivity(dl.getS_act_id())
			)
		);
		activity.setDetectionMethod(
			repo.findReferenceValueById
				(DataCleaner.getPurseSeineDetection(dl.getDet_id())
			)	
		);
		activity.setAssociationType(
			repo.findReferenceValueById
				(DataCleaner.getPurseSeineAssociation(dl.getSch_id())
			)			
		);
		activity.setLocalTime(combine(dl.getActdate(), dl.getActtime()));
		// FIXME Activity is missing UTC time
		
		activity.setBeacon(dl.getBeacon());
		activity.setComments(dl.getComment());
		activity.setEezCode(dl.getEz_id());
		activity.setFishingDays(new BigDecimal(dl.getFish_days())); // TODO Confirm BigDecimal c'tor reacts appropriately to null value
		activity.setLatitude(dl.getLat_long());
		activity.setLongitude(dl.getLon_long());
		activity.setPayao(null); // TODO Confirm this isn't missing in source item
		activity.setSeaState(asTubsSeaState(dl.getSea_id()));
		activity.setWindDirection(dl.getWinddir());
		activity.setWindSpeed(dl.getWind_kts());
		
		return activity;
	}
	
	protected Day asTubsDay(final FishingDay observerDay) {
		if (null == observerDay) { return null; }
		final Day tubsDay = new Day();
		tubsDay.setAnchoredObjectsWithoutSchool(observerDay.getFadnofsh());
		tubsDay.setAnchoredObjectsWithSchool(observerDay.getFad_fsh());
		tubsDay.setFloatingObjectsWithoutSchool(observerDay.getLognofsh());
		tubsDay.setFloatingObjectsWithSchool(observerDay.getLog_fsh());
		tubsDay.setFreeSchoolCount(observerDay.getSch_fsh());
		tubsDay.setStartOfDay(combine(observerDay.getDaydate(), observerDay.getDaytime()));
		tubsDay.setUtcStartOfDay(combine(observerDay.getUtc_date(), observerDay.getUtc_time()));
		
		tubsDay.setActivities(asTubsActivities(observerDay.getActivities()));
		
		/*
		tubsDay.setActivities(activities);
		tubsDay.setDiaryPage(diaryPage);
		tubsDay.setGen3Flag(gen3Flag);
	
		
		// TODO Convert this to AuditEntry
		observerDay.getEnteredby();
		observerDay.getInserttime();
		*/
		return tubsDay;
	}
	
	protected org.spc.ofp.tubs.domain.TripReportHeader asTubsGen3(final Gen3 gen3) {
		final org.spc.ofp.tubs.domain.TripReportHeader header =
		    new org.spc.ofp.tubs.domain.TripReportHeader();
		if (null != gen3) {
			header.setQuestionOneAnswer			(translateAnswer(gen3.getQ1()));
			header.setQuestionTwoAnswer			(translateAnswer(gen3.getQ2()));
			header.setQuestionThreeAnswer 		(translateAnswer(gen3.getQ3()));
			header.setQuestionFourAnswer 		(translateAnswer(gen3.getQ4()));
			header.setQuestionFiveAnswer 		(translateAnswer(gen3.getQ5()));
			header.setQuestionSixAnswer 		(translateAnswer(gen3.getQ6()));
			header.setQuestionSevenAnswer 		(translateAnswer(gen3.getQ7()));
			header.setQuestionEightAnswer 		(translateAnswer(gen3.getQ8()));
			header.setQuestionNineAnswer 		(translateAnswer(gen3.getQ9()));
			header.setQuestionTenAnswer 		(translateAnswer(gen3.getQ10()));
			header.setQuestionElevenAnswer 		(translateAnswer(gen3.getQ11()));
			header.setQuestionTwelveAnswer 		(translateAnswer(gen3.getQ12()));
			header.setQuestionThirteenAnswer 	(translateAnswer(gen3.getQ13()));
			header.setQuestionFourteenAnswer 	(translateAnswer(gen3.getQ14()));
			header.setQuestionFifteenAnswer 	(translateAnswer(gen3.getQ15()));
			header.setQuestionSixteenAnswer 	(translateAnswer(gen3.getQ16()));
			header.setQuestionSeventeenAnswer 	(translateAnswer(gen3.getQ17()));
			header.setQuestionEighteenAnswer 	(translateAnswer(gen3.getQ18()));
			header.setQuestionNineteenAnswer 	(translateAnswer(gen3.getQ19()));
			header.setQuestionTwentyAnswer 		(translateAnswer(gen3.getQ20()));
			
			header.setDateForFirstComment(gen3.getDate1());
			header.setFirstComment(gen3.getComment1());
			
			header.setDateForSecondComment(gen3.getDate2());
			header.setSecondComment(gen3.getComment2());
			
			header.setDateForThirdComment(gen3.getDate3());
			header.setThirdComment(gen3.getComment3());	
		}
		header.setAuditEntry(getAuditEntry());
		// NOTE Legacy data setup doesn't have any details.
		return header;
	}
	
	protected Collection<org.spc.ofp.tubs.domain.PollutionReportHeader> asTubsGen6(final Collection<Gen6Header> preports) {
		final Collection<org.spc.ofp.tubs.domain.PollutionReportHeader> pollutionReports =
		    new ArrayList<org.spc.ofp.tubs.domain.PollutionReportHeader>(null == preports ? 0 : preports.size());
		if (null != preports) {
			for (final Gen6Header preport : preports) {
				pollutionReports.add(asTubsGen6(preport));
			}
		}
		return pollutionReports;
	}
	
	protected org.spc.ofp.tubs.domain.PollutionReportHeader asTubsGen6(final Gen6Header preport) {
		final org.spc.ofp.tubs.domain.PollutionReportHeader report =
		    new org.spc.ofp.tubs.domain.PollutionReportHeader();
		// Fill header
		report.setComments(preport.getComments());
		report.setEezCode(preport.getEz_id());
		report.setIrcs(preport.getIrcs());
		report.setLatitude(preport.getLatitude());
		report.setLongitude(preport.getLongitude());
		report.setTimestamp(combine(preport.getDate(), preport.getTime()));
		report.setSeaState(asTubsSeaState(preport.getSeacond()));
		report.setVesselName(preport.getVesselname());
		report.setWindDirection(preport.getWinddir());
		report.setWindSpeed(preport.getWindspeed());
		report.setAuditEntry(getAuditEntry());
		// Fill details
		final List<org.spc.ofp.tubs.domain.PollutionReportDetails> details =
		    new ArrayList<org.spc.ofp.tubs.domain.PollutionReportDetails>(
		        null == preport.getDetails() ? 0 : preport.getDetails().size());
		if (null != preport.getDetails()) {
			for (final Gen6Detail detail : preport.getDetails()) {
				if (null == detail) { continue; }
				details.add(asTubsGen6(detail));
			}
		}
		report.setDetails(details); // FIXME Ensure that this sets the header value on every member...
		return report;
	}
	
	protected org.spc.ofp.tubs.domain.PollutionReportDetails asTubsGen6(final Gen6Detail g6detail) {
		final org.spc.ofp.tubs.domain.PollutionReportDetails details =
		    new org.spc.ofp.tubs.domain.PollutionReportDetails();
		details.setDescription(g6detail.getMaterial_i()); // FIXME ???
		details.setMaterial(g6detail.getPoll_type());
		details.setPollutionType(g6detail.getPoll_type());
		details.setQuantity(g6detail.getQuantity());
		//g6detail.getYn();
		// FIXME No AuditEntry?
		return details;
	}
	
	protected List<org.spc.ofp.tubs.domain.FishTransfer> asTubsTransfers(final Collection<FishTransfer> transfers) {
		final List<org.spc.ofp.tubs.domain.FishTransfer> tubsTransfers =
		    new ArrayList<org.spc.ofp.tubs.domain.FishTransfer>(null == transfers ? 0 : transfers.size());
		if (null != transfers) {
			for (final FishTransfer transfer : transfers) {
				final org.spc.ofp.tubs.domain.FishTransfer xfer = new org.spc.ofp.tubs.domain.FishTransfer();
				xfer.setTimestamp(combine(transfer.getDate(), transfer.getTime()));
				xfer.setLatitude(transfer.getLatitude());
				xfer.setLongitude(transfer.getLongitude());
				xfer.setVesselName(transfer.getR_name());
				xfer.setRegisteredCountryCode(transfer.getR_flag());
				xfer.setIrcs(transfer.getR_callsign());
				xfer.setSkipjackTransferred(transfer.getSkj_c());
				xfer.setYellowfinTransferred(transfer.getYft_c());
				xfer.setBigeyeTransferred(transfer.getBet_c());
				xfer.setMiscTransferred(transfer.getMix_c());
				xfer.setComments(transfer.getComment());
				xfer.setAuditEntry(getAuditEntry());
				tubsTransfers.add(xfer);
			}
		}
		return tubsTransfers;
	}
	
	protected List<org.spc.ofp.tubs.domain.VesselSighting> asTubsSightings(final Collection<VesselSighting> sightings) {
		final List<org.spc.ofp.tubs.domain.VesselSighting> tubsSightings =
		    new ArrayList<org.spc.ofp.tubs.domain.VesselSighting>(null == sightings ? 0 : sightings.size());
		if (null != sightings) {
			for (final VesselSighting sighting : sightings) {
				final org.spc.ofp.tubs.domain.VesselSighting tvs = new org.spc.ofp.tubs.domain.VesselSighting();
				// Sighting point in space and time
				tvs.setSightingDate(combine(sighting.getDate(), sighting.getTime()));
				tvs.setLatitude(sighting.getLatitude());
				tvs.setLongitude(sighting.getLongitude());
				tvs.setEezCode(sighting.getEz_id());
				// Relative location of sighted vessel
				tvs.setBearing(sighting.getBearing());
				tvs.setDistance(sighting.getDistance());
				tvs.setDistanceUnit(sighting.getDist_unit());
				// Vessel notes
				tvs.setIrcs(sighting.getS_callsign());
				tvs.setVesselName(sighting.getS_name());
				tvs.setRegisteredCountryCode(sighting.getS_flag());
				tvs.setComments(sighting.getComment());				
				tvs.setPhotoNumber(sighting.getPhoto_no());
				// Audit trail
				tvs.setAuditEntry(getAuditEntry());
				tubsSightings.add(tvs);
			}
		}
		return tubsSightings;
	}
	
	/**
	 * 
	 * @param fs
	 * @return
	 */
	protected org.spc.ofp.tubs.domain.common.Observer asTubsObserver(final FieldStaff fs) {
		// If the passed in FieldStaff entity is not null, check for an existing Observer with the same staff code
		if (null == fs) { return null; }
		assert repo != null : "CommonRepository not being instantiated";
		org.spc.ofp.tubs.domain.common.Observer to = repo.findByStaffCode(fs.getStaffCode());
		// If the Observer already exists, return that object
		if (null != to) { return to; }
		// Fill in the details
		to = new org.spc.ofp.tubs.domain.common.Observer();
		to.setStaffCode(fs.getStaffCode());
		to.setFirstName(fs.getFirstName());
		to.setFamilyName(fs.getLastName());
		to.setNationalityCountryCode(fs.getHomeCountry());
		return repo.saveObserver(to) ?
		    repo.findByStaffCode(fs.getStaffCode()) : 
		    null;
	}
	
	protected org.spc.ofp.tubs.domain.common.Port asTubsPort(final Port p) {
		if (null == p) { return null; }
		org.spc.ofp.tubs.domain.common.Port tp = repo.findPortById(p.getId());
		if (null != tp) { return tp; }
		tp = new org.spc.ofp.tubs.domain.common.Port();
		tp.setId(p.getId()); // TODO Don't _have_ to set this, and might not even _want_ to set this
		tp.setName(p.getName());
		tp.setCountryCode(p.getCountryCode());
		return repo.savePort(tp) ?
			repo.findPortById(p.getId()) :
			null;
	}
	
	protected org.spc.ofp.tubs.domain.common.Vessel asTubsVessel(final Vessel v) {
		if (null == v) { return null; }		
		org.spc.ofp.tubs.domain.common.Vessel tv = 
		    repo.findVesselById(v.getId());
		if (null != tv) { return tv; }
		tv = new org.spc.ofp.tubs.domain.common.Vessel();
		tv.setId(v.getId());
		tv.setFfaId(v.getFfaVid());
		tv.setGearType(
			"S".equalsIgnoreCase(v.getGearType()) ? "PS" :
			"L".equalsIgnoreCase(v.getGearType()) ? "LL" :
			"P".equalsIgnoreCase(v.getGearType()) ? "PL" :
			"OT"
		);
		tv.setName(v.getName());
		tv.setRegistrationNumber(v.getRegistrationNumber());
		tv.setInCountryCode(v.getC_boat_id());
		tv.setRegisteredCountryCode(v.getFlag());
		tv.setGrossTonnage(v.getGrossTonnage());
		tv.setAuditEntry(getAuditEntry());
		tv.setVesselCurstId(101L);
		return repo.saveVessel(tv) ?
		    repo.findVesselById(v.getId()) :
		    null;
	}
	
	protected org.spc.ofp.tubs.domain.common.SeaState asTubsSeaState(final String seaState) {
		// Protect JPA code from an invalid query
		if (null == seaState || "".equalsIgnoreCase(seaState.trim())) { return null ; }
		return repo.findSeaStateByCode(seaState);
	}

}
