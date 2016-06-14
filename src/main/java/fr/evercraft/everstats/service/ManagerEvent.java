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
package fr.evercraft.everstats.service;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageType;

import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.services.stats.event.EDeathStatsEvent;
import fr.evercraft.everapi.services.stats.event.EKillStatsEvent;
import fr.evercraft.everstats.EverStats;

public class ManagerEvent {
	private final EverStats plugin;
	
	public ManagerEvent(EverStats plugin) {
		this.plugin = plugin;
	}
	
	public void post(UUID uuid, Entity killer, DamageType cause, Long time) {
		Optional<EPlayer> player = this.plugin.getEServer().getEPlayer(uuid);
		if(player.isPresent()) {
			this.death(player.get(), killer, cause, time);
			if(killer instanceof Player) {
				Optional<EPlayer> killer_player = this.plugin.getEServer().getEPlayer(uuid);
				if(player.isPresent()) {
					this.kill(player.get(), killer_player.get(), cause, time);
				}
			}
		}
	}
	
	public void death(EPlayer victim, @Nullable Entity killer, DamageType damage, Long time) {
		if(killer == null) {
			this.plugin.getLogger().debug("Event StatsEvent.Death : (victim='" + victim.getUniqueId() + "';killer='null';damage='" + damage.getId() + "';time='" + time + "')");
			this.plugin.getGame().getEventManager().post(new EDeathStatsEvent(victim, damage, time, Cause.source(this.plugin).build()));
		} else {
			this.plugin.getLogger().debug("Event StatsEvent.Death : (victim='" + victim.getUniqueId() + "';killer='" + killer.getUniqueId() + "';damage='" + damage.getId() + "';time='" + time + "')");
			this.plugin.getGame().getEventManager().post(new EDeathStatsEvent(victim, killer, damage, time, Cause.source(this.plugin).build()));
		}
	}

	public void kill(EPlayer victim, EPlayer killer, DamageType damage, Long time) {
		this.plugin.getLogger().debug("Event StatsEvent.kill : (victim='" + victim.getUniqueId() + "';killer='" + killer.getUniqueId() + "';damage='" + damage.getId() + "';time='" + time + "')");
		this.plugin.getGame().getEventManager().post(new EKillStatsEvent(victim, killer, damage, time, Cause.source(this.plugin).build()));
	}
}
