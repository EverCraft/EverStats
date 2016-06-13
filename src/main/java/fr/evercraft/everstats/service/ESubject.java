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
import java.util.UUID;

import com.google.common.base.Preconditions;

import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.services.stats.StatsSubject;
import fr.evercraft.everstats.EverStats;

public class ESubject implements StatsSubject {
	
	private final EverStats plugin;
	
	private final UUID identifier;
	
	private int kill;
	private int death;

	public ESubject(final EverStats plugin, final UUID uuid) {
		Preconditions.checkNotNull(plugin, "plugin");
		Preconditions.checkNotNull(uuid, "uuid");
		
		this.plugin = plugin;
		this.identifier = uuid;
	}
	
	public void reload() {
		this.kill = 0;
		this.death = 0;
		
		this.load();
	}

	public void load() {
		PreparedStatement preparedStatement = null;
		Connection connection = null;
		String query = "SELECT v.victim, k.killer "
				+ "FROM ("
					+ "SELECT count(*) as killer "
					+ "FROM " + this.plugin.getDataBases().getTableDeath() +" " 
					+ "WHERE killer = ? "
					+ "AND `victim` NOT IN " + this.plugin.getDataBases().getBanned() + " "
				+ ") k, ("
					+ "SELECT count(*) as victim "
					+ "FROM everstats "
					+ "WHERE victim = ? "
					+ "AND `killer` NOT IN " + this.plugin.getDataBases().getBanned() + " "
				+ ") v ;";
		try {
			connection = this.plugin.getDataBases().getConnection();
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.identifier.toString());
			preparedStatement.setString(2, this.identifier.toString());
			ResultSet result = preparedStatement.executeQuery();
			if (result.next()) {
				this.kill = result.getInt("killer");
				this.death = result.getInt("victim");
			}
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
	}
	
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
		if (death != 0){
			ratio = this.kill/death;
		}
		return (int) Math.ceil(ratio);
	}
}
