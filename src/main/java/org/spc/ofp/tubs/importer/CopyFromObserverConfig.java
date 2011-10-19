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

import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Corey Cole <coreyc@spc.int>
 *
 */
@Configuration
public class CopyFromObserverConfig {

	@Bean(name = "ExistsFilterProcessor")
	public ExistsFilterProcessor existsFilterProcessor() {
		return new ExistsFilterProcessor();
	}
	
	@Bean(name = "ObserverTripProcessor")
	public ItemProcessor<String, org.spc.ofp.observer.domain.Trip> observerTripProcessor() {
		return new ObserverTripProcessor();
	}
	
	@Bean(name = "TubsTripProcessor")
	public ItemProcessor<org.spc.ofp.observer.domain.Trip, org.spc.ofp.tubs.domain.Trip> tubsTripProcessor() {
		return new TubsTripProcessor(); 
	}
}
