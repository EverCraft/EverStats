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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;

import fr.evercraft.elements.ESDeath;
import fr.evercraft.elements.ESInformation;
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EDataBase;

public class ESDataBase extends EDataBase<EverStats> {
	private String table_death;
	private String banned;

	public ESDataBase(EverStats plugin) {
		super(plugin, true);
		
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
		initTable(this.getTableDeath(), death);
		return true;
	}

	public boolean saveDeath(ESDeath death) {
		boolean resultat = false;
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		String query = "INSERT INTO `" + this.getTableDeath() + 
				"` (`killer`, `victim`, `reason`, `time`) "
				+ "VALUES(?, ?, ?, ?);";
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			if (death.getKiller() != null) {
				if (death.getKiller() instanceof Player){
					preparedStatement.setString(1, death.getKiller().getUniqueId().toString());
				} else {
					preparedStatement.setString(1, death.getKiller().getType().getName());
				}
			} else {
				preparedStatement.setString(1, null);
			}
			preparedStatement.setString(2, death.getVictim().getUniqueId().toString());
			if (death.getReason() != null) {
				preparedStatement.setString(3, death.getReason().getName());
			} else {
				preparedStatement.setString(3, null);
			}
			preparedStatement.setString(4, death.getTime().toString());
			resultat = preparedStatement.execute();
			connection.close();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during saving : (identifier:'" + query + "'): " + e.getMessage());
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
	
	public boolean check(UUID victim, UUID killer, long time){
		boolean check = false;
		ResultSet resultat;
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		String query = "SELECT COUNT(*) "
			+ "FROM `" + this.getTableDeath() + "` "
			+ "WHERE `victim` = ?  AND `killer` = ? AND `time` >= ?;";
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, victim.toString());
			preparedStatement.setString(2, killer.toString());
			preparedStatement.setTimestamp(3, new Timestamp(time));
			resultat = preparedStatement.executeQuery();
			if (resultat.next()){
				int value = resultat.getInt(1);
				if (value == 0){
					check = true;
				}
			}
			connection.close();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during checking : (identifier:'" + query + "'): " + e.getMessage());
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
		return check;
	}
	
	public ESInformation info(Player player){
		ESInformation data = null;
		PreparedStatement preparedStatement = null;
		Connection connection = null;
		String query = "SELECT v.victim, k.killer "
				+ "FROM ("
					+ "SELECT count(*) as killer "
					+ "FROM " + this.getTableDeath() +" " 
					+ "WHERE killer = ? "
					+ "AND `victim` NOT IN " + this.banned + " "
				+ ") k, ("
					+ "SELECT count(*) as victim "
					+ "FROM everstats "
					+ "WHERE victim = ? "
					+ "AND `killer` NOT IN " + this.banned + " "
				+ ") v ;";
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, player.getUniqueId().toString());
			preparedStatement.setString(2, player.getUniqueId().toString());
			ResultSet result = preparedStatement.executeQuery();
			if (result.next()){
				data = new ESInformation(result.getInt("killer"), result.getInt("victim"));
			}
			connection.close();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during info : (identifier:'" + query + "'): " + e.getMessage());
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
		return data;
	}
	
	public ArrayList<Map.Entry<UUID, Double>> topKill(Integer count){
		Map<UUID, Double> data = new HashMap<UUID, Double>();
		PreparedStatement preparedStatement = null;
		String query = "SELECT `killer`, count(*) as `kill` "
				+ "FROM " + this.getTableDeath() + " "
				+ "WHERE `killer` != `victim`"
				+ "AND `victim` NOT IN " + this.banned + " "
				+ "AND `killer` NOT IN " + this.banned + " "
				+ "GROUP BY `killer` "
				+ "HAVING `killer` IS NOT NULL "
				+ "ORDER BY `kill` DESC "
				+ "LIMIT " + count + " ;";
		Connection connection = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			ResultSet result = preparedStatement.executeQuery();
			while(result.next()){
				data.put(UUID.fromString(result.getString("killer")), result.getDouble("kill"));
			}
			connection.close();	
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during topKill : (identifier:'" + query + "'): " + e.getMessage());
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
		return new ArrayList<Map.Entry<UUID, Double>>(data.entrySet());
	}
	  
	public ArrayList<Map.Entry<UUID, Double>> topDeath(Integer count){
		Map<UUID, Double> data = new HashMap<UUID, Double>();
		String query = "SELECT `victim`, count(*) as `death` "
				+ "FROM " + this.getTableDeath() + " "
				+ "WHERE `killer` NOT IN " + this.banned + " "
				+ "AND `victim` NOT IN " + this.banned + " "
				+ "GROUP BY `victim` "
				+ "ORDER BY `death` DESC "
				+ "LIMIT " + count + " ;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try{
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			ResultSet result = preparedStatement.executeQuery();
			while(result.next()){
				data.put(UUID.fromString(result.getString("victim")), result.getDouble("death"));
			}
			connection.close();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during topDeath : (identifier:'" + query + "'): " + e.getMessage());
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
		return new ArrayList<Map.Entry<UUID, Double>>(data.entrySet());
	}
	
	public List<Map.Entry<UUID, Double>> topRatio(Integer count) {
		Map<UUID, Double> data = new HashMap<UUID, Double>();
		String query = "(SELECT v.victim as `uuid`, k.kill/v.death as `ratio` "
				+ "FROM "
					+ "(SELECT `victim`, count(*) as `death` "
					+ "FROM " + this.getTableDeath() + " "
					+ "WHERE `killer` NOT IN " + this.banned + " "
					+ "AND `victim` NOT IN " + this.banned + " "
					+ "GROUP BY `victim` "
					+ "HAVING count(*) > 0) v, "
					+ "(SELECT `killer`, count(*) as `kill` "
					+ "FROM " + this.getTableDeath() + " "
					+ "WHERE `victim` NOT IN " + this.banned + " "
					+ "AND `killer` NOT IN " + this.banned + " "
					+ "GROUP BY `killer` "
					+ "HAVING `killer` IS NOT NULL) k "
				+ "WHERE v.victim = k.killer) "
		  	+ "UNION "
		  		+ "(SELECT `killer`, count(*) as `ratio` "
		  		+ "FROM " + this.getTableDeath() + " "
		  		+ "WHERE `killer` NOT IN "
		  			+ "(SELECT `victim` as `killer` "
		  			+ "FROM " + this.getTableDeath() + " "
		  			+ "WHERE `victim` NOT IN " + this.banned + " "
					+ "AND `killer` NOT IN " + this.banned + " "
		  			+ "GROUP BY `victim` ) "
				+ "AND `victim` NOT IN " + this.banned + " "
				+ "AND `killer` NOT IN " + this.banned + " "
		  		+ "GROUP BY `killer`) "
		  	+ "ORDER BY `ratio` DESC "
		  	+ "LIMIT " + count + " ;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			ResultSet result = preparedStatement.executeQuery();
			while(result.next()){
				data.put(UUID.fromString(result.getString("uuid")), Math.ceil(result.getDouble("ratio")));
			}
			connection.close();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during topRatio : (identifier:'" + query + "'): " + e.getMessage());
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
		return new ArrayList<Map.Entry<UUID, Double>>(data.entrySet());
	  }	
	
	public ESInformation infoMonthly(Player player) {
		ESInformation data = null;
		String query = "SELECT v.victim, k.killer "
				+ "FROM ("
					+ "SELECT count(*) as killer "
					+ "FROM " + this.getTableDeath() + " " 
					+ "WHERE killer = ? "
					+ "AND MONTH(time) = MONTH(NOW()) "
					+ "AND YEAR(time) = YEAR(NOW()) "
					+ "AND `victim` NOT IN " + this.banned + " "
				+ ") k, ("
					+ "SELECT count(*) as victim "
					+ "FROM everstats "
					+ "WHERE victim = ? "
					+ "AND MONTH(time) = MONTH(NOW()) "
					+ "AND YEAR(time) = YEAR(NOW()) "
					+ "AND `killer` NOT IN " + this.banned + " "
				+ ") v ;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, player.getUniqueId().toString());
			preparedStatement.setString(2, player.getUniqueId().toString());
			ResultSet result = preparedStatement.executeQuery();
			if (result.next()){
				data = new ESInformation(result.getInt("killer"), result.getInt("victim"));
			}
			connection.close();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during infoMonthly : (identifier:'" + query + "'): " + e.getMessage());
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
		return data;
	}
	
	public ArrayList<Map.Entry<UUID, Double>> topKillMonthly(Integer count) {
		Map<UUID, Double> data = new HashMap<UUID, Double>();
		String query = "SELECT `killer`, count(*) as `kill` "
				+ "FROM " + this.getTableDeath() + " "
				+ "WHERE MONTH(time) = MONTH(NOW()) "
				+ "AND YEAR(time) = YEAR(NOW()) "
				+ "AND `killer` != `victim`"
				+ "AND `victim` NOT IN " + this.banned + " "
				+ "AND `killer` NOT IN " + this.banned + " "
				+ "GROUP BY `killer` "
				+ "HAVING `killer` IS NOT NULL "
				+ "ORDER BY `kill` DESC "
				+ "LIMIT " + count + " ;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			ResultSet result = preparedStatement.executeQuery();
			while(result.next()){
				data.put(UUID.fromString(result.getString("killer")), result.getDouble("kill"));
			}
			connection.close();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during topKillMonthly : (identifier:'" + query + "'): " + e.getMessage());
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
		return new ArrayList<Map.Entry<UUID, Double>>(data.entrySet());
	}
	  
	public ArrayList<Map.Entry<UUID, Double>> topDeathMonthly(Integer count) {
		Map<UUID, Double> data = new HashMap<UUID, Double>();
		String query = "SELECT `victim`, count(*) as `death` "
				+ "FROM " + this.getTableDeath() + " "
				+ "WHERE MONTH(time) = MONTH(NOW()) "
				+ "AND YEAR(time) = YEAR(NOW()) "
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
				data.put(UUID.fromString(result.getString("victim")), result.getDouble("death"));
			}
			connection.close();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during topDeathMonthly : (identifier:'" + query + "'): " + e.getMessage());
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
		return new ArrayList<Map.Entry<UUID, Double>>(data.entrySet());
	}
	
	public List<Map.Entry<UUID, Double>> topRatioMonthly(Integer count) {
		Map<UUID, Double> data = new HashMap<UUID, Double>();
		String query = "(SELECT v.victim as `uuid`, k.kill/v.death as `ratio` "
						+ "FROM "
							+ "(SELECT `victim`, count(*) as `death` "
							+ "FROM " + this.getTableDeath() + " "
							+ "WHERE MONTH(time) = MONTH(NOW()) "
							+ "AND YEAR(time) = YEAR(NOW()) "
							+ "AND `killer` NOT IN " + this.banned + " "
							+ "AND `victim` NOT IN " + this.banned + " "
							+ "GROUP BY `victim` "
							+ "HAVING count(*) > 0) v, "
							+ "(SELECT `killer`, count(*) as `kill` "
							+ "FROM " + this.getTableDeath() + " "
							+ "WHERE MONTH(time) = MONTH(NOW()) "
							+ "AND YEAR(time) = YEAR(NOW()) "
							+ "AND `victim` NOT IN " + this.banned + " "
							+ "AND `killer` NOT IN " + this.banned + " "
							+ "GROUP BY `killer` "
							+ "HAVING `killer` IS NOT NULL) k "
						+ "WHERE v.victim = k.killer) "
				  	+ "UNION "
				  		+ "(SELECT `killer`, count(*) as `ratio` "
				  		+ "FROM " + this.getTableDeath() + " "
				  		+ "WHERE `killer` NOT IN "
				  			+ "(SELECT `victim` as `killer` "
				  			+ "FROM " + this.getTableDeath() + " "
				  			+ "WHERE MONTH(time) = MONTH(NOW()) "
				  			+ "AND YEAR(time) = YEAR(NOW()) "
				  			+ "AND `victim` NOT IN " + this.banned + " "
							+ "AND `killer` NOT IN " + this.banned + " "
				  			+ "GROUP BY `victim` ) "
				  		+ "AND MONTH(time) = MONTH(NOW()) "
						+ "AND YEAR(time) = YEAR(NOW()) "
						+ "AND `victim` NOT IN " + this.banned + " "
						+ "AND `killer` NOT IN " + this.banned + " "
				  		+ "GROUP BY `killer`) "
				  	+ "ORDER BY `ratio` DESC "
				  	+ "LIMIT " + count + " ;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			ResultSet result = preparedStatement.executeQuery();
			while(result.next()){
				data.put(UUID.fromString(result.getString("uuid")), Math.ceil(result.getDouble("ratio")));
			}
			connection.close();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during topRatioMonthly : (identifier:'" + query + "'): " + e.getMessage());
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
		return new ArrayList<Map.Entry<UUID, Double>>(data.entrySet());
	}
	
	public void delete(Player player){
		String query = "DELETE FROM " + this.getTableDeath() + " "
				+ "WHERE `killer` = ? "
				+ "OR `victim` = ?;";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, player.getUniqueId().toString());
			preparedStatement.setString(2, player.getUniqueId().toString());
			preparedStatement.execute();
			connection.close();
		} catch (SQLException e) {
			this.plugin.getLogger().warn("Error during deleting : (identifier:'" + query + "'): " + e.getMessage());
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

	public String getTableDeath() {
		return this.getPrefix() + this.table_death;
	}

	public String getBanned() {
		return this.banned;
	}
}
