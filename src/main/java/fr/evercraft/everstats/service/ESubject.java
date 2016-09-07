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
import fr.evercraft.everapi.services.StatsSubject;
import fr.evercraft.everstats.EverStats;

public class ESubject implements StatsSubject {
	
	private final EverStats plugin;
	
	private final UUID identifier;
	
	private final List<Long> kill_month;
	private final List<EDeath> death_month;
	
	private int kill;
	private int death;
	private int killstreaks;
	private int best_killstreaks;

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
		
		this.killstreaks = 0;
		this.best_killstreaks = 0;
		
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
			this.loadKillStreaks(connection);
			this.loadBestKillStreaks(connection);
			
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
	
	public void loadKillStreaks(Connection connection) {
		PreparedStatement preparedStatement = null;
		String query = 	 "SELECT COUNT(*) as killstreaks "
						+ "FROM " + this.plugin.getDataBases().getTableDeath() + " "
						+ "WHERE `killer` = ? "
						+ "AND `time` >= ("
							+ "SELECT COALESCE(MAX(`time`), '1970-01-01 00:00:01.000000') " 
							+ "FROM " + this.plugin.getDataBases().getTableDeath() + " "
							+ "WHERE `victim` = ? "
						+ ") ;";
		try {
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.identifier.toString());
			preparedStatement.setString(2, this.identifier.toString());
			
			ResultSet result = preparedStatement.executeQuery();
			while (result.next()) {
				this.killstreaks = result.getInt("killstreaks");
			}
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error while loading data (uuid='" + this.identifier + "';type='killstreaks') : " + e.getMessage());
		} finally {
			try {
				if (preparedStatement != null) {
					preparedStatement.close();
				}
			} catch (SQLException e) {}	
		}	
	}
	
	public void loadBestKillStreaks(Connection connection) {
		PreparedStatement preparedStatement = null;
		String query = 	 "SELECT * "
						+ "FROM " + this.plugin.getDataBases().getTableKillstreaks() + " "
						+ "WHERE `uuid` = ? "
						+ " ;";
		try {
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.identifier.toString());
			ResultSet result = preparedStatement.executeQuery();
			if (result.next()) {
				this.best_killstreaks = result.getInt("killstreaks");
			} else {
				this.insertKillstreaks(connection);
			}
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error while loading data (uuid='" + this.identifier + "';type='killstreaks') : " + e.getMessage());
		} finally {
			try {
				if (preparedStatement != null) {
					preparedStatement.close();
				}
			} catch (SQLException e) {}	
		}	
	}
	
	private void insertKillstreaks(Connection connection) {
		PreparedStatement preparedStatement = null;
		String query = 	  "INSERT INTO `" + this.plugin.getDataBases().getTableKillstreaks() + "` "
				+ "VALUES (?, ?);";
		try {
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.identifier.toString());
			preparedStatement.setInt(2, this.best_killstreaks);
			
			preparedStatement.execute();
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of player : " + e.getMessage());
		} finally {
			try {
				if (preparedStatement != null){
					preparedStatement.close();
				}
			} catch (SQLException e) {}
	    }
	}
	
	private void addKillstreaks(){
		if (this.killstreaks > this.best_killstreaks){
			this.best_killstreaks = this.killstreaks;
			updateKillstreaks();
		}
	}
	
	private void updateKillstreaks() {
		String query = 	  "UPDATE `" + this.plugin.getDataBases().getTableKillstreaks() + "` "
				+ "SET killstreaks = ? "
				+ "WHERE uuid = ?;";
		PreparedStatement preparedStatement = null;
		Connection connection = null;
		try {
			connection = this.plugin.getDataBases().getConnection();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setInt(1, this.killstreaks);
			preparedStatement.setString(2, this.identifier.toString());
			
			preparedStatement.execute();
		} catch (ServerDisableException e) {
			e.execute();
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of player : " + e.getMessage());
		} finally {
			try {
				if (preparedStatement != null){ 
					preparedStatement.close();
				}
			} catch (SQLException e) {}
	    }
	}
	
	public void delete(){
		String query = "DELETE FROM " + this.plugin.getDataBases().getTableDeath() + " "
					+  "WHERE `killer` = ? "
					+  "OR `victim` = ?;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = this.plugin.getDataBases().getConnection();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.identifier.toString());
			preparedStatement.setString(2, this.identifier.toString());
			preparedStatement.execute();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during deleting (uuid='" + this.identifier + "'): " + e.getMessage());
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
	}
	
	private boolean saveDeath(String killer, String cause, Long time) {
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
	
	/*
	 * Départ
	 */
	
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
				if (!isCooldown(killer.getUniqueId(), time)) {
					return false;
				}
				
				this.death_month.add(new EDeath(time, killer.getUniqueId()));
				
				Optional<ESubject> killer_subject = this.plugin.getService().getSubject(killer.getUniqueId());
				if (killer_subject.isPresent()) {
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
		this.killstreaks = 0;
		
		cause = damage.getName();
		
		final String async_killer = killer_name;
		final String async_cause = cause;
		this.plugin.getGame().getScheduler().createTaskBuilder().async().execute(() -> saveDeath(async_killer, async_cause, time)).submit(this.plugin);
		this.plugin.getManagerEvent().post(this.identifier, Optional.ofNullable(killer), damage, time);
		
		return true;
	}
	
	/**
	 * Ajoute un meurtre
	 * @param time L'heure du meurtre
	 */
	private void addKill(final Long time) {
		Preconditions.checkNotNull(time, "time");
		
		this.kill_month.add(time);
		this.kill++;
		this.killstreaks++;
		addKillstreaks();
		this.plugin.getLogger().warn("Kill :  " + this.kill);
		
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
			if (kill_time.getTime() > time_cooldown) {
				kill = kill_time.getKiller().isPresent() && kill_time.getKiller().get().equals(killer);
			} else {
				stop = true;
			}
			cpt--;
		}
		return !kill;
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
	public int getKillStreaks() {
		return this.killstreaks;
	}
	
	public int getBestKillStreaks() {
		return this.best_killstreaks;
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
		calendar.set(GregorianCalendar.MONTH, 1);
		calendar.set(GregorianCalendar.HOUR, 0);
		calendar.set(GregorianCalendar.MINUTE, 0);
		calendar.set(GregorianCalendar.SECOND, 0);
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