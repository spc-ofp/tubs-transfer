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
