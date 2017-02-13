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
package fr.evercraft.everstats;

import java.util.Optional;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.source.BlockDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.FallingBlockDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everstats.ESMessage.ESMessages;

public class ESListener {
	private EverStats plugin;

	public ESListener(EverStats plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Ajoute le joueur dans le cache
	 */
	@Listener
    public void onClientConnectionEvent(final ClientConnectionEvent.Auth event) {
		this.plugin.getService().get(event.getProfile().getUniqueId());
    }
	
	/**
	 * Ajoute le joueur à la liste
	 */
	@Listener
    public void onClientConnectionEvent(final ClientConnectionEvent.Join event) {
		this.plugin.getService().registerPlayer(event.getTargetEntity().getUniqueId());
    }
    
	/**
	 * Supprime le joueur de la liste
	 */
    @Listener
    public void onClientConnectionEvent(final ClientConnectionEvent.Disconnect event) {
        this.plugin.getService().removePlayer(event.getTargetEntity().getUniqueId());
    }
	
	@Listener
	public void onPlayerDeath(DestructEntityEvent.Death event, @Getter("getTargetEntity") Player player_sponge) {
		EPlayer victim = this.plugin.getEServer().getEPlayer(player_sponge);
		
		Optional<IndirectEntityDamageSource> optIndirectEntity = event.getCause().first(IndirectEntityDamageSource.class);
		if (optIndirectEntity.isPresent()){
			IndirectEntityDamageSource damageSource = optIndirectEntity.get();
			sendDeath(victim, damageSource.getIndirectSource(), damageSource.getType());
		} else {
			Optional<BlockDamageSource> optBlockDamage = event.getCause().first(BlockDamageSource.class);
			if (optBlockDamage.isPresent()){
				BlockDamageSource damageSource = optBlockDamage.get();
				sendDeath(victim, damageSource.getType());
			} else {
				Optional<FallingBlockDamageSource> optFallingBlock = event.getCause().first(FallingBlockDamageSource.class);
				if (optFallingBlock.isPresent()){
					FallingBlockDamageSource damageSource = optFallingBlock.get();
					sendDeath(victim, damageSource.getSource(), damageSource.getType());
				} else {
					Optional<EntityDamageSource> optEntityDamage = event.getCause().first(EntityDamageSource.class);
					if (optEntityDamage.isPresent()){
						EntityDamageSource damageSource = optEntityDamage.get();
						sendDeath(victim, damageSource.getSource(), damageSource.getType());
					} else {
						Optional<DamageSource> optDamage = event.getCause().first(DamageSource.class);
						if (optDamage.isPresent()){
							DamageSource damageSource = optDamage.get();
							sendDeath(victim, damageSource.getType());
						}
					}
				}
			}
		}
	}
	
	private void sendDeath(EPlayer victim, Entity entity, DamageType reason){
		if (entity instanceof Player) {
			Player killer = (Player) entity;
			if (!victim.equals(killer)) {
				if (!victim.addDeath(killer, reason, System.currentTimeMillis())) {
					ESMessages.PLAYER_SPAWNKILL.sender()
						.replace("<time>", this.plugin.getService().getCooldown().toString())
						.sendTo(killer);
				}
			}
		} else {
			victim.addDeath(entity, reason, System.currentTimeMillis());
		}
	}
	
	private void sendDeath(EPlayer victim, DamageType reason){
		victim.addDeath(reason, System.currentTimeMillis());
	}
}