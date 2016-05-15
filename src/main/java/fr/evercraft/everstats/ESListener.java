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
package fr.evercraft.everstats;

import java.sql.Timestamp;
import java.util.Optional;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Creature;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DestructEntityEvent;

import fr.evercraft.elements.ESDeath;
import fr.evercraft.everapi.java.UtilsDate;
import fr.evercraft.everapi.plugin.EChat;

public class ESListener {
	private EverStats plugin;

	public ESListener(EverStats plugin) {
		this.plugin = plugin;
	}

	@Listener
	public void onEntityDeath(DestructEntityEvent event) {
		if (event.getTargetEntity() instanceof Player) {
			Player victim = (Player) event.getTargetEntity();
			Optional<EntityDamageSource> optDamageSource = event.getCause().first(EntityDamageSource.class);
			if (optDamageSource.isPresent()) {
				EntityDamageSource damageSource = optDamageSource.get();
				this.plugin.getEServer().broadcast(damageSource.toString());
				DamageType reason = damageSource.getType();
				if (damageSource.getSource() instanceof Player) {
					Player killer = (Player) damageSource.getSource();
					if (victim != killer) {
						Integer cooldown = this.plugin.getConfigs().get("config.cooldown").getInt();
						Timestamp date = new Timestamp(System.currentTimeMillis() - cooldown * 1000);
						if (this.plugin.getDataBases().check(victim.getUniqueId(), killer.getUniqueId(), date)){
							this.plugin.getDataBases().saveDeath(new ESDeath(victim, killer, reason, UtilsDate.getTimestamp()));
						} else {
							killer.sendMessage(EChat.of(this.plugin.getMessages().getMessagePrefix() 
									+ this.plugin.getMessages().getMessage("PLAYER_SPAWNKILL")));
						}
					}
				} else if (damageSource.getSource() instanceof Creature) {
					Entity killer = damageSource.getSource();
					this.plugin.getDataBases().saveDeath(new ESDeath(killer, victim, reason, UtilsDate.getTimestamp()));
				} else {
					Entity killer = null;
					this.plugin.getDataBases().saveDeath(new ESDeath(killer, victim, reason, UtilsDate.getTimestamp()));
				}
			} /*else {
				Entity killer = null;
				this.plugin.getDataBases().saveDeath(new ESDeath(killer, victim, reason, UtilsDate.getTimestamp()));
			}*/
		}
	}
}