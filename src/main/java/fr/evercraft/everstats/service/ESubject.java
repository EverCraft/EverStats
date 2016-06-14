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
 * This file is part of EverEssentials.
 *
 * EverEssentials is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EverEssentials is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EverEssentials.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.evercraft.everstats.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.entity.damage.DamageType;

import com.google.common.base.Preconditions;

import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.services.stats.StatsSubject;
import fr.evercraft.everstats.EverStats;

public class ESubject implements StatsSubject {
	
	private final EverStats plugin;
	
	private final UUID identifier;
	
	private final List<Long> kill_month;
	private final List<EDeath> death_month;
	
	private int kill;
	private int death;

	public ESubject(final EverStats plugin, final UUID uuid) {
		Preconditions.checkNotNull(plugin, "plugin");
		Preconditions.checkNotNull(uuid, "uuid");
		
		this.plugin = plugin;
		this.identifier = uuid;
		
		this.kill_month = new CopyOnWriteArrayList<Long>();
		this.death_month = new CopyOnWriteArrayList<EDeath>();
		
		this.reload();
	}
	
	public void reload() {
		this.kill = 0;
		this.death = 0;
		
		this.kill_month.clear();
		this.death_month.clear();
		
		this.load();
	}

	public void load() {
		Connection connection = null;
		try {
			connection = this.plugin.getDataBases().getConnection();
			
			this.loadKillDeath(connection);
			this.loadKillMonthly(connection);
			this.loadDeathMonthly(connection);
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {}	
		}
	}
	
	public void loadKillDeath(Connection connection) {
		PreparedStatement preparedStatement = null;
		String query = "SELECT v.victim, k.killer "
				+ "FROM ("
					+ "SELECT count(*) as killer "
					+ "FROM " + this.plugin.getDataBases().getTableDeath() +" " 
					+ "WHERE killer = ? "
					+ "AND `victim` NOT IN " + this.plugin.getDataBases().getBanned() + " "
				+ ") k, ("
					+ "SELECT count(*) as victim "
					+ "FROM " + this.plugin.getDataBases().getTableDeath() +" "
					+ "WHERE victim = ? "
					+ "AND `killer` NOT IN " + this.plugin.getDataBases().getBanned() + " "
				+ ") v ;";
		try {
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.identifier.toString());
			preparedStatement.setString(2, this.identifier.toString());
			ResultSet result = preparedStatement.executeQuery();
			if (result.next()) {
				this.kill = result.getInt("killer");
				this.death = result.getInt("victim");
			}
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error while loading data (uuid='" + this.identifier + "') : " + e.getMessage());
		} finally {
			try {
				if (preparedStatement != null) {
					preparedStatement.close();
				}
			} catch (SQLException e) {}	
		}
	}
	
	public void loadKillMonthly(Connection connection) {
		PreparedStatement preparedStatement = null;
		String query = 	  "SELECT id, time "
						+ "FROM " + this.plugin.getDataBases().getTableDeath() + " " 
						+ "WHERE killer = ? "
						+ "AND MONTH(time) = MONTH(NOW()) "
						+ "AND YEAR(time) = YEAR(NOW()) "
						+ "AND `victim` NOT IN " + this.plugin.getDataBases().getBanned() + ""
						+ "ORDER BY time ASC ; ";
		try {
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.identifier.toString());
			
			ResultSet result = preparedStatement.executeQuery();
			while (result.next()) {
				this.kill_month.add(result.getTimestamp("time").getTime());
			}
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error while loading data (uuid='" + this.identifier + "';type='kill_month') : " + e.getMessage());
		} finally {
			try {
				if (preparedStatement != null) {
					preparedStatement.close();
				}
			} catch (SQLException e) {}	
		}
	}
	
	public void loadDeathMonthly(Connection connection) {
		PreparedStatement preparedStatement = null;
		String query = 	  "SELECT id, time, killer "
						+ "FROM " + this.plugin.getDataBases().getTableDeath() + " "
						+ "WHERE victim = ? "
						+ "AND MONTH(time) = MONTH(NOW()) "
						+ "AND YEAR(time) = YEAR(NOW()) "
						+ "AND `killer` NOT IN " + this.plugin.getDataBases().getBanned() + " "
						+ "ORDER BY time ASC ; ";
		try {
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.identifier.toString());
			
			ResultSet result = preparedStatement.executeQuery();
			while (result.next()) {
				this.death_month.add(new EDeath(result.getTimestamp("time").getTime(), result.getString("killer")));
			}
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error while loading data (uuid='" + this.identifier + "';type='death_month') : " + e.getMessage());
		} finally {
			try {
				if (preparedStatement != null) {
					preparedStatement.close();
				}
			} catch (SQLException e) {}	
		}
	}
	
	public boolean saveDeath(String killer, String cause, Long time) {
		boolean resultat = false;
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		String query = "INSERT INTO `" + this.plugin.getDataBases().getTableDeath() + 
				"` (`killer`, `victim`, `reason`, `time`) "
				+ "VALUES(?, ?, ?, ?);";
		String name = "";
		try {
			connection = this.plugin.getDataBases().getConnection();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, killer);
			preparedStatement.setString(2, this.identifier.toString());
			preparedStatement.setString(3, cause);
			preparedStatement.setTimestamp(4, new Timestamp(time));
			resultat = preparedStatement.execute();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during saving : (uuid='" + query + "';killer='" + name + "';reason='" + cause + "';time='" + time + "') : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) {
					preparedStatement.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {}
		}
		return resultat;
	}
	
	@Override
	public boolean addDeath(final @Nullable Entity killer, final DamageType damage, final Long time) {
		Preconditions.checkNotNull(damage, "damage");
		Preconditions.checkNotNull(time, "time");
		
		String killer_name = null;
		String cause = null;
		
		if (killer != null) {
			if (killer instanceof Player){
				killer_name = killer.getUniqueId().toString();

				// Si le cooldown n'a pas été respecté
				if(!isCooldown(killer.getUniqueId(), time)) {
					return false;
				}
				
				this.death_month.add(new EDeath(time, killer.getUniqueId()));
				
				Optional<ESubject> killer_subject = this.plugin.getService().getSubject(killer.getUniqueId());
				if(killer_subject.isPresent()) {
					killer_subject.get().addKill(time);
				}
			} else {
				killer_name = killer.getType().getName();
				this.death_month.add(new EDeath(time));
			}
		} else {
			this.death_month.add(new EDeath(time));
		}
		
		this.death++;
		
		if (damage != null) {
			cause = damage.getName();
		}
		
		final String async_killer = killer_name;
		final String async_cause = cause;
		this.plugin.getGame().getScheduler().createTaskBuilder().async().execute(() -> saveDeath(async_killer, async_cause, time)).submit(this.plugin);
		this.plugin.getManagerEvent().post(this.identifier, killer, damage, time);
		this.plugin.getLogger().warn("Death :  " + this.death);
		
		return true;
	}
	
	/**
	 * Vérification du cooldown
	 * @param killer Le joueur qui la tué
	 * @param time L'heure du meurtre
	 * @return False si le cooldown n'est respecté
	 */
	public boolean isCooldown(UUID killer, Long time) {
		Preconditions.checkNotNull(killer, "killer");
		Preconditions.checkNotNull(time, "time");
		
		long time_cooldown = time - this.plugin.getService().getCooldown() * 1000;
		int cpt = this.death_month.size() - 1;
		boolean kill = false;
		boolean stop = false;
		
		while(cpt >= 0 && cpt < this.death_month.size() && !kill && !stop) {
			EDeath kill_time = this.death_month.get(cpt);
			if(kill_time.getTime() > time_cooldown) {
				kill = kill_time.getKiller().isPresent() && kill_time.getKiller().get().equals(killer);
			} else {
				stop = true;
			}
			cpt--;
		}
		return !kill;
	}
	
	/**
	 * Ajoute un meurtre
	 * @param time L'heure du meurtre
	 */
	private void addKill(Long time) {
		Preconditions.checkNotNull(time, "time");
		
		this.kill_month.add(time);
		this.kill++;
		this.plugin.getLogger().warn("Kill :  " + this.kill);
	}
	
	/*
	 * All
	 */
	
	@Override
	public int getKill() {
		return this.kill;
	}

	@Override
	public int getDeath() {
		return this.death;
	}
	
	@Override
	public int getRatio() {
		double ratio = this.kill;
		if (this.death != 0){
			ratio = ratio/death;
		}
		return (int) Math.ceil(ratio);
	}
	
	/*
	 * Monthly
	 */
	
	/**
	 * Permet d'avoir l'heure du mois dernier en Millisecondes
	 * @return En Millisecondes
	 */
	private Long getTimeMonthly() {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(GregorianCalendar.MONTH, -1);
		return calendar.getTimeInMillis();
	}
	
	@Override
	public int getKillMonthly() {
		Long time = this.getTimeMonthly();
		while(!this.kill_month.isEmpty() && this.kill_month.get(0) < time) {
			this.kill_month.remove(0);
		}
		
		return this.kill_month.size();
	}

	@Override
	public int getDeathMonthly() {
		Long time = this.getTimeMonthly();
		while(!this.death_month.isEmpty() && this.death_month.get(0).getTime() < time) {
			this.kill_month.remove(0);
		}
		
		return this.death_month.size();
	}
	
	@Override
	public int getRatioMonthly() {
		double ratio = this.getKillMonthly();
		double death = this.getDeathMonthly();
		if (death != 0){
			ratio = ratio/death;
		}
		return (int) Math.ceil(ratio);
	}
}