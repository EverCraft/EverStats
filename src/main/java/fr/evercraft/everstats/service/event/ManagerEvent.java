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
package fr.evercraft.everstats.service.event;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageType;

import fr.evercraft.everapi.event.ESpongeEventFactory;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everstats.EverStats;

public class ManagerEvent {
	private final EverStats plugin;
	
	public ManagerEvent(EverStats plugin) {
		this.plugin = plugin;
	}
	
	public void reload() {
		this.plugin.getELogger().debug("Event StatsReloadEvent");
		this.plugin.getGame().getEventManager().post(ESpongeEventFactory.createStatsSystemEventReload(Cause.source(this.plugin).build()));
	}
	
	public void post(UUID uuid, Optional<Entity> killer, DamageType cause, Long time) {
		Optional<EPlayer> player = this.plugin.getEServer().getEPlayer(uuid);
		if (player.isPresent()) {
			this.death(player.get(), killer, cause, time);
			if (killer.isPresent() && killer.get() instanceof Player) {
				EPlayer killer_player = this.plugin.getEServer().getEPlayer((Player) killer.get());
				this.kill(player.get(), killer_player, cause, time);
			}
		}
	}
	
	private void death(EPlayer victim, Optional<Entity> killer, DamageType damage, Long time) {
		if (killer.isPresent()) {
			this.plugin.getELogger().debug("Event StatsUserEvent.Death : (victim='" + victim.getUniqueId() + "';killer='" + killer.get().getUniqueId() + "';damage='" + damage.getId() + "';time='" + time + "')");
		} else {
			this.plugin.getELogger().debug("Event StatsUserEvent.Death : (victim='" + victim.getUniqueId() + "';killer='null';damage='" + damage.getId() + "';time='" + time + "')");
		}
		this.plugin.getGame().getEventManager().post(ESpongeEventFactory.createStatsUserEventDeath(victim, time, damage, killer, Cause.source(this.plugin).build()));
	}

	private void kill(EPlayer victim, EPlayer killer, DamageType damage, Long time) {
		this.plugin.getELogger().debug("Event StatsUserEvent.kill : (victim='" + victim.getUniqueId() + "';killer='" + killer.getUniqueId() + "';damage='" + damage.getId() + "';time='" + time + "')");
		this.plugin.getGame().getEventManager().post(ESpongeEventFactory.createStatsUserEventKill(victim, time, damage, killer, Cause.source(this.plugin).build()));
	}
}
