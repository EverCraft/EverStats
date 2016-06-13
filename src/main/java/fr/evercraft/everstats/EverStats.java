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

import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

import fr.evercraft.everapi.exception.PluginDisableException;
import fr.evercraft.everapi.plugin.EPlugin;

@Plugin(id = "fr.evercraft.everstats", 
		name = "EverStats", 
		version = "1.2", 
		description = "Stats",
		url = "http://evercraft.fr/",
		authors = {"rexbut","lesbleu"},
		dependencies = {
		    @Dependency(id = "fr.evercraft.everapi", version = "1.2")
		})
public class EverStats extends EPlugin {

	private ESConfig configs;
	private ESMessage messages;
	
	private ESDataBase databases;
	
	@Override
	protected void onPreEnable() throws PluginDisableException {
		// Configurations
		this.configs = new ESConfig(this);
		this.messages = new ESMessage(this);
		
		// MySQL
		this.databases = new ESDataBase(this);
		if(!this.databases.isEnable()) {
			throw new PluginDisableException("This plugin requires a database");
		}
	}
	
	@Override
	protected void onCompleteEnable() throws PluginDisableException {		
		// Commands
		new ESCommand(this);
		
		// Listerners
		this.getGame().getEventManager().registerListeners(this, new ESListener(this));
	}
	
	@Override
	protected void onReload() throws PluginDisableException {
		// Configurations
		this.reloadConfigurations();
		this.databases.reload();
		if(!this.databases.isEnable()) {
			throw new PluginDisableException("This plugin requires a database");
		}
	}
	
	@Override
	protected void onDisable() {
	}
	
	public ESConfig getConfigs(){
		return this.configs;
	}
	
	public ESMessage getMessages(){
		return this.messages;
	}
	
	public ESDataBase getDataBases(){
		return this.databases;
	}
}
