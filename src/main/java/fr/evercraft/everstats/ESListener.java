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

import java.util.Optional;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DestructEntityEvent;

import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everstats.ESMessage.ESMessages;

public class ESListener {
	private EverStats plugin;

	public ESListener(EverStats plugin) {
		this.plugin = plugin;
	}
	
	@Listener
	public void onEntityDeath(DestructEntityEvent event) {
		if (event.getTargetEntity() instanceof Player) {
			Player victim = (Player) event.getTargetEntity();
			Optional<DamageSource> optDamageSource = event.getCause().first(DamageSource.class);
			if (optDamageSource.isPresent()) {
				
				Optional<EPlayer> victim_player = this.plugin.getEServer().getEPlayer(victim);
				if(victim_player.isPresent()) {
					DamageSource damageSource = optDamageSource.get();
					if (damageSource instanceof EntityDamageSource){
						EntityDamageSource entityDamage = (EntityDamageSource) optDamageSource.get();
						DamageType reason = damageSource.getType();
						if (entityDamage.getSource() instanceof Player) {
							Player killer = (Player) entityDamage.getSource();
							if (!victim.equals(killer)) {
								if(!victim_player.get().addDeath(killer, reason, System.currentTimeMillis())) {
									killer.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.PLAYER_SPAWNKILL.get()
											.replaceAll("<time>", this.plugin.getService().getCooldown().toString())));
								}
							}
						} else {
							victim_player.get().addDeath(entityDamage.getSource(), reason, System.currentTimeMillis());
						}
					} else {
						victim_player.get().addDeath(damageSource.getType(), System.currentTimeMillis());
					}
				}
			}
		}	
	}
}