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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import fr.evercraft.everapi.services.StatsService;
import fr.evercraft.everapi.services.StatsSubject;
import fr.evercraft.everapi.util.Chronometer;
import fr.evercraft.everstats.EverStats;

public class EStatsService implements StatsService {
	private final EverStats plugin;
	
	private final ConcurrentMap<UUID, ESubject> subjects;
	private final LoadingCache<UUID, ESubject> cache;
	
	/**
	 * En Seconde
	 */
	private Long cooldown;

	public EStatsService(final EverStats plugin) {		
		this.plugin = plugin;
		
		this.subjects = new ConcurrentHashMap<UUID, ESubject>();
		this.cache = CacheBuilder.newBuilder()
					    .maximumSize(100)
					    .expireAfterAccess(5, TimeUnit.MINUTES)
					    .build(new CacheLoader<UUID, ESubject>() {
					    	/**
					    	 * Ajoute un joueur au cache
					    	 */
					        @Override
					        public ESubject load(UUID uuid){
					        	Chronometer chronometer = new Chronometer();
					        	
					        	ESubject subject = new ESubject(EStatsService.this.plugin, uuid);
					        	EStatsService.this.plugin.getLogger().debug("Loading user '" + uuid.toString() + "' in " +  chronometer.getMilliseconds().toString() + " ms");
					            return subject;
					        }
					    });
		this.reload();
	}

	public Long getCooldown() {
		return this.cooldown;
	}
	
	@Override
	public Optional<StatsSubject> get(UUID uuid) {
		return Optional.ofNullable(this.getSubject(uuid).orElse(null));
	}
	
	public Optional<ESubject> getSubject(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		
		try {
			if (!this.subjects.containsKey(uuid)) {
				return Optional.ofNullable(this.cache.get(uuid));
	    	}
	    	return Optional.ofNullable(this.subjects.get(uuid));
		} catch (ExecutionException e) {
			this.plugin.getLogger().warn("Error : Loading user (identifier='" + uuid + "';message='" + e.getMessage() + "')");
			return Optional.empty();
		}
	}
	
	@Override
	public boolean hasRegistered(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		
		try {
			return this.plugin.getGame().getServer().getPlayer(uuid).isPresent();
		} catch (IllegalArgumentException e) {}
		return false;
	}
	
	/**
	 * Rechargement : Vide le cache et recharge tous les joueurs
	 */
	public void reload() {
		this.cache.cleanUp();
		for (ESubject subject : this.subjects.values()) {
			subject.reload();
		}
		
		this.cooldown = this.plugin.getConfigs().getCooldown();
	}
	
	/**
	 * Ajoute un joueur à la liste
	 * @param identifier L'UUID du joueur
	 */
	public void registerPlayer(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		
		ESubject player = this.cache.getIfPresent(uuid);
		// Si le joueur est dans le cache
		if (player != null) {
			this.subjects.putIfAbsent(uuid, player);
			this.plugin.getLogger().debug("Loading player cache : " + uuid.toString());
		// Si le joueur n'est pas dans le cache
		} else {
			Chronometer chronometer = new Chronometer();
			player = new ESubject(this.plugin, uuid);
			this.subjects.putIfAbsent(uuid, player);
			this.plugin.getLogger().debug("Loading player '" + uuid.toString() + "' in " +  chronometer.getMilliseconds().toString() + " ms");
		}
		//this.plugin.getManagerEvent().post(player, PermUserEvent.Action.USER_ADDED);
	}
	
	/**
	 * Supprime un joueur à la liste et l'ajoute au cache
	 * @param identifier L'UUID du joueur
	 */
	public void removePlayer(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		
		ESubject player = this.subjects.remove(uuid);
		// Si le joueur existe
		if (player != null) {
			this.cache.put(uuid, player);
			//this.plugin.getManagerEvent().post(player, PermUserEvent.Action.USER_REMOVED);
			this.plugin.getLogger().debug("Unloading the player : " + uuid.toString());
		}
	}

	@Override
	public Collection<StatsSubject> getAll() {
		Set<StatsSubject> list = new HashSet<StatsSubject>();
		list.addAll(this.subjects.values());
		list.addAll(this.cache.asMap().values());
		return list;
	}
	
	/*
	 * Top
	 */
	
	@Override
	public LinkedHashMap<UUID, Double> getTopDeaths(int count) {
		return this.getTopKills(count, (long) 0);
	}
	
	@Override
	public LinkedHashMap<UUID, Double> getTopDeaths(int count, Long time) {
		Preconditions.checkNotNull(count, "count");
		Preconditions.checkNotNull(time, "time");
		
		return this.plugin.getDataBases().getTopDeaths(count, time);
	}
	
	@Override
	public LinkedHashMap<UUID, Double> getTopKills(int count) {
		return this.getTopKills(count, (long) 0);
	}
	
	@Override
	public LinkedHashMap<UUID, Double> getTopKills(int count, Long time) {
		Preconditions.checkNotNull(count, "count");
		Preconditions.checkNotNull(time, "time");
		
		return this.plugin.getDataBases().getTopKills(count, time);
	}
	
	@Override
	public LinkedHashMap<UUID, Double> getTopRatio(int count) {
		return this.getTopRatio(count, (long) 0);
	}
	
	@Override
	public LinkedHashMap<UUID, Double> getTopRatio(int count, Long time) {
		Preconditions.checkNotNull(count, "count");
		Preconditions.checkNotNull(time, "time");
		
		return this.plugin.getDataBases().getTopRatio(count, time);
	}
}