/**
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
package fr.evercraft.everstats.service.event;

import org.spongepowered.api.event.cause.Cause;

import fr.evercraft.everapi.event.StatsSystemEvent;

public class EReloadStatsSystemEvent extends EStatsSystemEvent implements StatsSystemEvent.Reload {
	
    public EReloadStatsSystemEvent(Cause cause) {
    	super(cause, Action.RELOADED);
    }
}
