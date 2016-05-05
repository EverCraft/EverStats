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
import java.sql.SQLException;
import fr.evercraft.elements.ESDeath;
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EDataBase;

public class ESDataBase extends EDataBase<EverStats> {
	private String table_death;
	
	public ESDataBase(EverStats plugin) {
		super(plugin, true);
	}

	public boolean init() throws ServerDisableException {
		this.table_death = "death";
		String death = 	"CREATE TABLE IF NOT EXISTS `" + this.table_death + "` (" +
				"`id` int(11) NOT NULL AUTO_INCREMENT," +
				"`victim` varchar(36) NOT NULL," +
				"`killer` varchar(36)," +
				"`reason` varchar(36)," +
				"`time` timestamp NOT NULL," +
				"PRIMARY KEY (`id`));";
		initTable(this.getTableDeath(), death);
		return true;
	}
	
	public boolean saveDeath(ESDeath kill){
		boolean resultat = false;
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		String query = "INSERT INTO `" + this.table_death + "` (`killer`, `victim`, `reason`, `time`) VALUES(?, ?, ?, ?);";
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(query);
			if (kill.getKiller() != null){
				preparedStatement.setString(1, kill.getKiller().getUniqueId().toString());
			} else {
				preparedStatement.setString(1, null);
			}
			preparedStatement.setString(2, kill.getVictim().getUniqueId().toString());
			if (kill.getReason() != null){
				preparedStatement.setString(3, kill.getReason().toString());
			} else {
				preparedStatement.setString(3, null);
			}
			preparedStatement.setString(4, kill.getTime().toString());
			resultat = preparedStatement.execute();
			connection.close();
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of log : (identifier:'" + query + "'): " + e.getMessage());
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
	
	public String getTableDeath() {
		return this.getPrefix() + this.table_death;
	}
}
