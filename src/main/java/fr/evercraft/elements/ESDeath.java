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
package fr.evercraft.elements;

import java.sql.Timestamp;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.entity.damage.DamageType;

public class ESDeath{

	private int id;
	private Entity killer;
	private Player victim;
	private DamageType reason;
	private Timestamp time;
	
	public ESDeath(Entity killer, Player victim, DamageType cause, Timestamp time) {
		this.killer = killer;
		this.victim = victim;
		this.reason = cause;
		this.time = time;
	}
	
	public ESDeath(Player killer, Player victim, DamageType cause, Timestamp time) {
		this.killer = killer;
		this.victim = victim;
		this.reason = cause;
		this.time = time;
	}

	public int getId() {
		return this.id;
	}

	public Entity getKiller() {
		return this.killer;
	}

	public Player getVictim() {
		return this.victim;
	}

	public DamageType getReason() {
		return this.reason;
	}

	public Timestamp getTime() {
		return this.time;
	}
}
