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

import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

import fr.evercraft.everapi.EverAPI;
import fr.evercraft.everapi.exception.PluginDisableException;
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EPlugin;
import fr.evercraft.everapi.services.StatsService;
import fr.evercraft.everstats.command.sub.ESReload;
import fr.evercraft.everstats.service.EStatsService;
import fr.evercraft.everstats.service.event.ManagerEvent;

@Plugin(id = "everstats", 
		name = "EverStats", 
		version = EverAPI.VERSION, 
		description = "Stats",
		url = "http://evercraft.fr/",
		authors = {"rexbut","lesbleu"},
		dependencies = {
		    @Dependency(id = "everapi", version = EverAPI.VERSION),
		    @Dependency(id = "spongeapi", version = EverAPI.SPONGEAPI_VERSION)
		})
public class EverStats extends EPlugin<EverStats> {

	private ESConfig configs;
	private ESMessage messages;
	
	private ESDataBase databases;
	private EStatsService service;
	private ManagerEvent event;
	
	@Override
	protected void onPreEnable() throws PluginDisableException {
		// Configurations
		this.configs = new ESConfig(this);
		this.messages = new ESMessage(this);
		
		// MySQL
		this.databases = new ESDataBase(this);
		if (!this.databases.isEnable()) {
			throw new PluginDisableException("This plugin requires a database");
		}
		
		this.event = new ManagerEvent(this);
		this.service = new EStatsService(this);
		this.getGame().getServiceManager().setProvider(this, StatsService.class, this.service);
	}
	
	@Override
	protected void onCompleteEnable() throws PluginDisableException {		
		// Commands
		ESCommand command = new ESCommand(this);
		command.add(new ESReload(this, command));
		
		// Listerners
		this.getGame().getEventManager().registerListeners(this, new ESListener(this));
	}
	
	@Override
	protected void onReload() throws PluginDisableException, ServerDisableException {
		super.onReload();
		
		this.databases.reload();
		if (!this.databases.isEnable()) {
			throw new PluginDisableException("This plugin requires a database");
		}
		
		this.service.reload();
		this.event.reload();
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

	public EStatsService getService() {
		return this.service;
	}

	public ManagerEvent getManagerEvent() {
		return this.event;
	}
}
