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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.UUID;

import fr.evercraft.everapi.exception.PluginDisableException;
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EDataBase;

public class ESDataBase extends EDataBase<EverStats> {
	private String table_death;
	private String banned;
	private String table_killstreaks;

	public ESDataBase(EverStats plugin) throws PluginDisableException {
		super(plugin, true);
		this.loadBanned();
	}
	
	public void reload() throws PluginDisableException {
		super.reload();
		this.loadBanned();
	}
	
	public void loadBanned() {
		this.banned = "( '" + String.join("' , '", this.plugin.getConfigs().getListString("top.banned")) + "' )";
	}

	public boolean init() throws ServerDisableException {
		this.table_death = "death";
		String death = "CREATE TABLE IF NOT EXISTS <table> (" + 
				"`id` int(11) NOT NULL AUTO_INCREMENT," + 
				"`victim` varchar(36) NOT NULL," + 
				"`killer` varchar(36)," + 
				"`reason` varchar(36)," + 
				"`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," + 
				"PRIMARY KEY (`id`));";
		this.initTable(this.getTableDeath(), death);
		
		this.table_killstreaks = "killstreaks";
		String killstreaks = "CREATE TABLE IF NOT EXISTS <table> (" + 
				"`uuid` varchar(36) NOT NULL," + 
				"`killstreaks` int(11) NOT NULL," + 
				"PRIMARY KEY (`uuid`));";
		this.initTable(this.getTableKillstreaks(), killstreaks);
		return true;
	}

	public String getTableDeath() {
		return this.getPrefix() + this.table_death;
	}
	
	public String getTableKillstreaks() {
		return this.getPrefix() + this.table_killstreaks;
	}

	public String getBanned() {
		return this.banned;
	}
	
	public LinkedHashMap<UUID, Double> getTopDeaths(int count, Long time) {
		LinkedHashMap<UUID, Double> players = new LinkedHashMap<UUID, Double>();
		String query =    "SELECT `victim`, count(*) as `death` "
						+ "FROM " + this.getTableDeath() + " "
						+ "WHERE `time` >= ? "
						+ "AND `killer` NOT IN " + this.banned + " "
						+ "AND `victim` NOT IN " + this.banned + " "
						+ "GROUP BY `victim` "
						+ "ORDER BY `death` DESC "
						+ "LIMIT " + count + " ;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setTimestamp(1, new Timestamp(time));
			ResultSet result = preparedStatement.executeQuery();
			while(result.next()){
				try {
					players.put(UUID.fromString(result.getString("victim")), result.getDouble("death"));
				} catch(IllegalArgumentException e) {
					this.plugin.getELogger().warn("getTopDeaths : " + result.getString("victim"));
				}
			}
			connection.close();
		} catch (SQLException e) {
			this.plugin.getELogger().warn("Error during TopDeath (time='" + time + "';count='" + count + "') : " + e.getMessage());
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
		return players;
	}

	public LinkedHashMap<UUID, Double> getTopKills(int count, Long time) {
		LinkedHashMap<UUID, Double> players = new LinkedHashMap<UUID, Double>();
		String query = 	  "SELECT `killer`, count(*) as `kill` "
						+ "FROM " + this.getTableDeath() + " "
						+ "WHERE `time` >= ? "
							+ "AND `victim` NOT IN " + this.getBanned() + " "
							+ "AND `killer` NOT IN " + this.getBanned() + " "
						+ "GROUP BY `killer` "
						+ "HAVING `killer` IS NOT NULL "
							+ "AND LENGTH(`killer`) = 36 "
						+ "ORDER BY `kill` DESC "
						+ "LIMIT " + count + " ;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setTimestamp(1, new Timestamp(time));
			ResultSet result = preparedStatement.executeQuery();
			while(result.next()){
				try {
					players.put(UUID.fromString(result.getString("killer")), result.getDouble("kill"));
				} catch(IllegalArgumentException e) {
					this.plugin.getELogger().warn("getTopKills : " + result.getString("killer"));
				}
			}
		} catch (SQLException e) {
			this.plugin.getELogger().warn("Error during TopKill (time='" + time + "';count='" + count + "') : " + e.getMessage());
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
		return players;
	}
	
	public LinkedHashMap<UUID, Double> getTopRatio(int count, Long time) {
		LinkedHashMap<UUID, Double> players = new LinkedHashMap<UUID, Double>();
		String query =   "(SELECT v.victim as `uuid`, k.kill/v.death as `ratio` "
						+ "FROM "
						   + "(SELECT `victim`, count(*) as `death` "
							+ "FROM " + this.getTableDeath() + " "
								+ "WHERE `time` >= ? "
								+ "AND `killer` NOT IN " + this.getBanned() + " "
								+ "AND `victim` NOT IN " + this.getBanned() + " "
							+ "GROUP BY `victim` "
							+ "HAVING count(*) > 0) v, "
							+ "(SELECT `killer`, count(*) as `kill` "
							+ "FROM " + this.getTableDeath() + " "
								+ "WHERE `time` >= ? "
								+ "AND `victim` NOT IN " + this.getBanned() + " "
								+ "AND `killer` NOT IN " + this.getBanned() + " "
							+ "GROUP BY `killer` "
							+ "HAVING `killer` IS NOT NULL) k "
						+ "WHERE v.victim = k.killer"
						+ "AND LENGTH(k.killer) = 36) "
				  	+ "UNION "
				  		+ "(SELECT `killer`, count(*) as `ratio` "
				  		+ "FROM " + this.getTableDeath() + " "
				  		+ "WHERE `killer` NOT IN "
				  			+ "(SELECT `victim` as `killer` "
				  			+ "FROM " + this.getTableDeath() + " "
				  				+ "WHERE `time` >= ? "
				  				+ "AND `victim` NOT IN " + this.banned + " "
				  				+ "AND `killer` NOT IN " + this.banned + " "
				  			+ "GROUP BY `victim` ) "
				  			+ "WHERE `time` >= ? "
						+ "AND `victim` NOT IN " + this.banned + " "
						+ "AND `killer` NOT IN " + this.banned + " "
				  		+ "GROUP BY `killer` "
				  		+ "HAVING LENGTH(`killer`) = 36) "
				  	+ "ORDER BY `ratio` DESC "
				  	+ "LIMIT " + count + " ;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			Timestamp timestamp = new Timestamp(time);
			preparedStatement.setTimestamp(1, timestamp);
			preparedStatement.setTimestamp(2, timestamp);
			preparedStatement.setTimestamp(3, timestamp);
			preparedStatement.setTimestamp(4, timestamp);
			ResultSet result = preparedStatement.executeQuery();
			while(result.next()) {
				try {
					players.put(UUID.fromString(result.getString("uuid")), Math.ceil(result.getDouble("ratio")));
				} catch(IllegalArgumentException e) {
					this.plugin.getELogger().warn("getTopRatio : " + result.getString("ratio"));
				}
			}
		} catch (SQLException e) {
			this.plugin.getELogger().warn("Error during TopRatio (time='" + time + "';count='" + count + "') : " + e.getMessage());
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
		return players;
	}
	
	public LinkedHashMap<UUID, Double> getTopKillstreaks(int count) {
		LinkedHashMap<UUID, Double> players = new LinkedHashMap<UUID, Double>();
		String query =    "SELECT `victim`, count(*) as `death` "
						+ "FROM " + this.getTableDeath() + " "
						+ "WHERE `time` >= ? "
						+ "AND `killer` NOT IN " + this.banned + " "
						+ "AND `victim` NOT IN " + this.banned + " "
						+ "GROUP BY `victim` "
						+ "ORDER BY `death` DESC "
						+ "LIMIT " + count + " ;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			ResultSet result = preparedStatement.executeQuery();
			while(result.next()){
				try {
					players.put(UUID.fromString(result.getString("victim")), result.getDouble("death"));
				} catch(IllegalArgumentException e) {
					this.plugin.getELogger().warn("getTopKillstreaks : " + result.getString("victim"));
				}
			}
			connection.close();
		} catch (SQLException e) {
			this.plugin.getELogger().warn("Error during TopKillstreaks (';count='" + count + "') : " + e.getMessage());
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
		return players;
	}
}
