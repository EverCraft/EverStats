/*
 * This file is part of EverStats.
 *
 * EverStats is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EverStats is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EverStats.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.evercraft.everstats.service;

import java.util.Optional;
import java.util.UUID;

public class EDeath {
	private final Long time;
	
	private Optional<UUID> killer;
	
	public EDeath(Long time, String killer) {
		this.time = time;
		
		try {
			this.killer = Optional.ofNullable(UUID.fromString(killer));
		} catch (IllegalArgumentException e) {
			this.killer = Optional.empty();
		}
	}

	public EDeath(Long time, UUID killer) {
		this.time = time;
		this.killer = Optional.ofNullable(killer);
	}
	
	public EDeath(Long time) {
		this.time = time;
		this.killer = Optional.empty();
	}
	

	public Long getTime() {
		return this.time;
	}

	public Optional<UUID> getKiller() {
		return this.killer;
	}
}
