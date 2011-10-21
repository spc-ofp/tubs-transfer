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
import org.spc.ofp.observer.domain.ITrip;
import org.spc.ofp.observer.domain.Vessel;
import org.spc.ofp.observer.domain.VesselSighting;
import org.spc.ofp.observer.domain.purseseine.DayLog;
import org.spc.ofp.observer.domain.purseseine.FishingDay;
import org.spc.ofp.observer.domain.purseseine.LengthFrequencyDetail;
import org.spc.ofp.observer.domain.purseseine.LengthFrequencyHeader;
import org.spc.ofp.tubs.domain.AuditEntry;
import org.spc.ofp.tubs.domain.common.CommonRepository;
import org.spc.ofp.tubs.domain.purseseine.Activity;
import org.spc.ofp.tubs.domain.purseseine.Brail;
import org.spc.ofp.tubs.domain.purseseine.Day;
import org.spc.ofp.tubs.domain.purseseine.FishingSet;
import org.spc.ofp.tubs.domain.purseseine.LengthSample;
import org.spc.ofp.tubs.domain.purseseine.LengthSamplingHeader;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Corey Cole <coreyc@spc.int>
 *
 */
public class TubsTripProcessor implements ItemProcessor<ITrip, org.spc.ofp.tubs.domain.Trip> {

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
	
	public org.spc.ofp.tubs.domain.Trip process(final ITrip trip) throws Exception {
		// TODO Is this what we want? 
		if (null == trip) { 
			LOGGER.info("Input object was null, skipping...");
			return null; 
		} 
		
		LOGGER.debug("Gear Type: " + trip.getGearType());
		LOGGER.debug("isPurseSeineTrip()? " + trip.isPurseSeineTrip());
		LOGGER.debug("isLongLineTrip()? " + trip.isLongLineTrip());
		LOGGER.debug("isPoleAndLineTrip()? " + trip.isPoleAndLineTrip());
		
		final org.spc.ofp.tubs.domain.Trip tubsTrip =
		    trip.isPurseSeineTrip() ? new org.spc.ofp.tubs.domain.purseseine.PurseSeineTrip() :
		    trip.isLongLineTrip() ? new org.spc.ofp.tubs.domain.longline.LongLineTrip() :
		    null;

		LOGGER.debug("After checking for trip type, 'tubsTrip' is null? " + (null == tubsTrip));    
		// Skip trips with unsupported gear types.
		if (null == tubsTrip) { return null; }

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
			return fillPurseSeineDetails(
					(org.spc.ofp.observer.domain.purseseine.PurseSeineTrip) trip,
					(org.spc.ofp.tubs.domain.purseseine.PurseSeineTrip) tubsTrip);
		} else if (trip.isLongLineTrip()) {
			return fillLongLineDetails(
					(org.spc.ofp.observer.domain.longline.LongLineTrip) trip,
					(org.spc.ofp.tubs.domain.longline.LongLineTrip) tubsTrip);
		}
		// Should never get here...
		return tubsTrip;
	}
	
	protected org.spc.ofp.tubs.domain.purseseine.PurseSeineTrip fillPurseSeineDetails(
			final org.spc.ofp.observer.domain.purseseine.PurseSeineTrip obsvTrip,
			final org.spc.ofp.tubs.domain.purseseine.PurseSeineTrip tubsTrip) {
		
		// FIXME Gear and crew aren't here (yet!) because of issues in migrating source data		
		// DayLog holds most of the Purse Seine specific observer data	
		// SNARK Right about here lambda functionality would come in handy...
		tubsTrip.setDays(asTubsDays(obsvTrip.getFishingDays()));
		LOGGER.debug(String.format("Copied %d days into TUBS trip", tubsTrip.getDays().size()));
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
		if (null == observerDays) { 
			return Collections.emptyList();
		}
		
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
		final Integer activityId = DataCleaner.getPurseSeineActivity(dl.getS_act_id());
		if (null != activityId) {
			activity.setActivityType(
				repo.findReferenceValueById(activityId)
			);
		}
		final Integer detectionId = DataCleaner.getPurseSeineDetection(dl.getDet_id());
		if (null != detectionId) {
			activity.setDetectionMethod(
				repo.findReferenceValueById(detectionId)	
			);
		}
		final Integer associationId = DataCleaner.getPurseSeineAssociation(dl.getSch_id());
		if (null != associationId) {
			activity.setAssociationType(
				repo.findReferenceValueById(associationId)			
			);
		}
		activity.setLocalTime(combine(dl.getActdate(), dl.getActtime()));
		activity.setUtcTime(combine(dl.getUtc_adate(), dl.getUtc_atime()));
		
		activity.setBeacon(dl.getBeacon());
		activity.setComments(dl.getComment());
		activity.setEezCode(dl.getEz_id());
		BigDecimal fishingDays = null;
		if (null != dl.getFish_days()) {
			fishingDays = BigDecimal.valueOf(dl.getFish_days().doubleValue()).setScale(7);
			//fishingDays = new BigDecimal(dl.getFish_days());
		}
		activity.setFishingDays(fishingDays);
		activity.setLatitude(dl.getLat_long());
		activity.setLongitude(dl.getLon_long());
		activity.setSeaState(asTubsSeaState(dl.getSea_id()));
		activity.setWindDirection(dl.getWinddir());
		activity.setWindSpeed(dl.getWind_kts());
		
		// Only set this for an appropriate activity
		// This is a shortcut for the referenceId ACTIVE/Fishing that currently has id = 1
		if (null != dl.getS_act_id() && 1 == dl.getS_act_id().intValue()) {		
			LOGGER.debug("Copying DayLog data into FishingSet");
			activity.setFishingSet(asTubsFishingSet(dl));			
		}
		activity.setAuditEntry(new AuditEntry(dl.getEnteredby(), dl.getInserttime()));
		return activity;
	}
	
	protected Boolean containsSpecies(final Integer percentage) {
		if (null == percentage) { return null; }
		return percentage.intValue() > 0;
	}
	
	protected FishingSet asTubsFishingSet(final DayLog dl) {
		final FishingSet fset = new FishingSet();
		fset.setSetNumber(dl.getSetno());
		fset.setStartTime(combine(dl.getActdate(), dl.getActtime()));
		fset.setWeightOnBoard(dl.getLd_onboard());
		fset.setWeightOnBoardFromLog(dl.getLd_ves_onb());
		fset.setObservedSetRetainedTonnage(dl.getLd_tonnage());
		fset.setSetRetainedTonnageFromLog(dl.getLd_ves_ton());
		fset.setObservedNewOnboard(dl.getLd_newonbo());
		fset.setNewOnboardFromLog(dl.getLd_ves_new());
		fset.setTonsOfTunaObserved(dl.getTuna_catch());
		fset.setSumOfBrail1(dl.getLd_brails());
		fset.setSumOfBrail2(dl.getLd_brails2());
		fset.setVesselTonnageOnlyFromThisSet(dl.getOneset());
		fset.setTotalCatch(dl.getTot_catch());
		fset.setComments(dl.getPs3_commen());
		
		// Set a boolean true if percentage is greater than zero, null otherwise
		final Integer percentSKJ = dl.getPerc_skj();		
		fset.setSkipjackPercentage(percentSKJ);
		fset.setContainsSkipjack(containsSpecies(percentSKJ));
				
		final Integer percentBET = dl.getPerc_bet();
		fset.setBigeyePercentage(percentBET);
		fset.setContainsBigeye(containsSpecies(percentBET));
		
		final Integer percentYFT = dl.getPerc_yft();
		fset.setYellowfinPercentage(percentYFT);
		fset.setContainsYellowfin(containsSpecies(percentYFT));
		
		fset.setLargeSpecies(dl.getB_sp_id());
		fset.setLargeSpeciesCount(dl.getB_nbspecie());
		
		// Convert set times to java.util.Date in a rational way
		fset.setSkiffOff(combine(dl.getActdate(), dl.getActtime())); // NO separate Skiff Off in Observer?
		fset.setWinchOn(combine(dl.getActdate(), dl.getWnch_on()));
		fset.setRingUp(combine(dl.getActdate(), dl.getRing_up()));
		fset.setStartOfBrail(combine(dl.getActdate(), dl.getSbrail()));
		fset.setEndOfBrail(combine(dl.getActdate(), dl.getEbrail()));

		fset.setLengthSamples(asTubsLengthSamples(dl));
		fset.setAuditEntry(new AuditEntry(dl.getEnteredby(), dl.getInserttime()));
		fset.setCatchList(asTubsSetCatch(dl.getSetCatchList()));
		return fset;
	}
	
	protected List<org.spc.ofp.tubs.domain.purseseine.SetCatch> asTubsSetCatch(final List<org.spc.ofp.observer.domain.purseseine.SetCatch> scl) {
		if (null == scl) { return Collections.emptyList(); }
		final List<org.spc.ofp.tubs.domain.purseseine.SetCatch> tubsSetCatchList =
		    new ArrayList<org.spc.ofp.tubs.domain.purseseine.SetCatch>(scl.size());
		
		for (final org.spc.ofp.observer.domain.purseseine.SetCatch sc : scl) {
			tubsSetCatchList.add(asTubsSetCatch(sc));
		}		
		return tubsSetCatchList;
	}
	
	protected org.spc.ofp.tubs.domain.purseseine.SetCatch asTubsSetCatch(final org.spc.ofp.observer.domain.purseseine.SetCatch sc) {
		if (null == sc) { return null; }
		final org.spc.ofp.tubs.domain.purseseine.SetCatch tsc = new org.spc.ofp.tubs.domain.purseseine.SetCatch();
		tsc.setComments(sc.getComments());
		
		if (null != sc.getCond_id() && !sc.getCond_id().trim().isEmpty()) {			
			tsc.setCondition(repo.findConditionByCode(sc.getCond_id().trim()));
		}
		if (null != sc.getFate_id() && !sc.getFate_id().trim().isEmpty()) {
			tsc.setFate(repo.findFateByCode(sc.getFate_id().trim()));
		}
		
		tsc.setContainsLargeFish(sc.getLargefish());
		tsc.setCountFromLog(sc.getVesslog());		
		
		tsc.setObserverCount(sc.getSp_n());
		tsc.setObserverWeight(sc.getSp_c());		
		tsc.setSpeciesCode(sc.getSp_id());
		tsc.setVesselWeight(sc.getSp_c_ves());
		tsc.setSpeciesWeightEstimate(sc.getSp_w_est());
		tsc.setSpeciesWeightHigh(sc.getSp_w_h());
		tsc.setSpeciesWeightLow(sc.getSp_w_l());
		tsc.setAuditEntry(getAuditEntry());
		
		// FIXME Don't have good names for these properties
		tsc.setSp_c_est(sc.getSp_c_est());
		tsc.setSp_c_id(sc.getSp_c_id());
		tsc.setSp_c_spcomp(sc.getSp_c_spcom());
		tsc.setSp_n_est(sc.getSp_n_est());
		tsc.setSp_w_id(sc.getSp_w_id());
		return tsc;
	}
	
	protected List<LengthSamplingHeader> asTubsLengthSamples(final DayLog dl) {
		if (null == dl || null == dl.getHeaders()) { return Collections.emptyList(); }
		final List<LengthFrequencyHeader> headers = dl.getHeaders();
		
		final List<LengthSamplingHeader> tubsHeaders =
		    new ArrayList<LengthSamplingHeader>(headers.size());
		for (final LengthFrequencyHeader lfh : headers) {
			final LengthSamplingHeader header = new LengthSamplingHeader();						
			header.setBrailStartTime(dl.getSbrail());
			header.setBrailEndTime(dl.getEbrail());
			header.setFormId(lfh.getNbformused());
			//final ReferenceId protocol = ;
			header.setProtocolType(
			    repo.findReferenceValueById(
			        DataCleaner.getSamplingProtocol(lfh.getProtocol())));
			
			// Ignore for now - CLC
			//header.setPageTotals(pageTotals); // Single domain object
			//header.setColumnTotals(columnTotals); // List of domain objects
			final List<Brail> brails = new ArrayList<Brail>(1);
			final Brail brail = new Brail();
			brail.setComments(lfh.getProt_comme());
			brail.setFishPerBrail(lfh.getFish_brl());
			brail.setBrailNumber(lfh.getWhichbrail());
			
			brail.setFullBrailCount(lfh.getBrail_full());
			brail.setSevenEighthsBrailCount(lfh.getBrail_78());
			brail.setThreeQuartersBrailCount(lfh.getBrail_34());
			brail.setTwoThirdsBrailCount(lfh.getBrail_23());
			brail.setOneHalfBrailCount(lfh.getBrail_12());
			brail.setOneThirdBrailCount(lfh.getBrail_13());
			brail.setOneQuarterBrailCount(lfh.getBrail_14());
			brail.setOneEighthBrailCount(lfh.getBrail_18());
			
			brail.setTotalBrailCount(lfh.getTbrail());
			brail.setSumOfAllBrails(lfh.getSum_brails());
			
			brail.setPageNumber(lfh.getPage_no());
			
			brail.setAuditEntry(new AuditEntry(lfh.getEnteredby(), lfh.getInserttime()));
			brails.add(brail);			
			header.setBrails(brails); // List of domain objects
			
			header.setSamples(asTubsLengthSamples(lfh.getDetails())); // List of domain objects
			
			header.setAuditEntry(new AuditEntry(dl.getEnteredby(), dl.getInserttime()));
			tubsHeaders.add(header);
		}
		return tubsHeaders;
	}
	
	protected LengthSample asTubsLengthSample(final LengthFrequencyDetail detail) {
		final LengthSample sample = new LengthSample();
		sample.setLength(detail.getLen());
		sample.setSampleNumber(detail.getSample_no());
		sample.setSpeciesCode(detail.getSp_id());
        sample.setAuditEntry(getAuditEntry());
		return sample;
	}
	
	protected List<LengthSample> asTubsLengthSamples(final List<LengthFrequencyDetail> details) {
		if (null == details) { return Collections.emptyList(); }
		final List<LengthSample> lengthSamples = new ArrayList<LengthSample>(details.size());
		for (final LengthFrequencyDetail detail : details) {
			if (null == detail) { continue; }
			lengthSamples.add(asTubsLengthSample(detail));
		}
		return lengthSamples;
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
		// No audit trail in source system
		tubsDay.setAuditEntry(getAuditEntry());
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
		// No audit trail in source system
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
		report.setAuditEntry(getAuditEntry()); // No audit trail in source system
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
