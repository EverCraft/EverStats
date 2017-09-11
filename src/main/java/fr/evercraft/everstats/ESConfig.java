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

import java.util.Arrays;

import fr.evercraft.everapi.plugin.file.EConfig;

public class ESConfig extends EConfig<EverStats> {

	public ESConfig(EverStats plugin) {
		super(plugin);
	}
	
	public void reload() {
		super.reload();
		this.plugin.getELogger().setDebug(this.isDebug());
	}

	public void loadDefault() {
		this.configDefault();
		this.sqlDefault();
		
		addDefault("top.banned", Arrays.asList(
				"86f8f95b-e5e6-45c4-bf85-4d64dbd0903f", 
				"f3345769-4c70-4a9f-9db9-bdb8f9e8a46c"));
		
		// Default cooldown in seconds
		addDefault("config.cooldown", 500);
	}

	public long getCooldown() {
		return this.get("config.cooldown").getLong(500);
	}
}