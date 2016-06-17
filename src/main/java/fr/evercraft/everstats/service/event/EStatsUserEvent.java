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
/**
 * This file is part of EverAPI.
 *
 * EverAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EverAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EverAPI.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.evercraft.everstats.service.event;

import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageType;

import fr.evercraft.everapi.event.StatsUserEvent;
import fr.evercraft.everapi.server.player.EPlayer;

public abstract class EStatsUserEvent implements StatsUserEvent {	

	private final Type type;
	private final EPlayer victim;
	private final DamageType damage;
	private final Long time;
	private final Cause cause;

    public EStatsUserEvent(Type type, EPlayer victim, DamageType damage, Long time, Cause cause) {
    	this.type = type;
		this.victim = victim;
		this.damage = damage;
		this.time = time;
		this.cause = cause;
	}
    
    @Override
    public Type getType() {
    	return this.type;
    }
    
    @Override
    public EPlayer getVictim() {
    	return this.victim;
    }
    
    @Override
    public DamageType getDamageType() {
    	return this.damage;
    }

    @Override
	public Long getTime() {
        return this.time;
    }

    @Override
	public Cause getCause() {
		return this.cause;
	}
}
