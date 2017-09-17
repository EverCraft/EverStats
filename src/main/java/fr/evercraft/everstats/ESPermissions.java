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

import fr.evercraft.everapi.plugin.EnumPermission;
import fr.evercraft.everapi.plugin.file.EnumMessage;
import fr.evercraft.everstats.ESMessage.ESMessages;

public enum ESPermissions implements EnumPermission {
	
	EVERSTATS("commands.execute", ESMessages.PERMISSIONS_COMMANDS_EXECUTE),
	HELP("commands.help", ESMessages.PERMISSIONS_COMMANDS_HELP),
	RELOAD("commands.reload", ESMessages.PERMISSIONS_COMMANDS_RELOAD);
	
	private static final String PREFIX = "everstats";
	
	private final String permission;
	private final EnumMessage message;
	private final boolean value;
    
    private ESPermissions(final String permission, final EnumMessage message) {
    	this(permission, message, false);
    }
    
    private ESPermissions(final String permission, final EnumMessage message, final boolean value) {   	    	
    	this.permission = PREFIX + "." + permission;
    	this.message = message;
    	this.value = value;
    }

    @Override
    public String get() {
		return this.permission;
	}

	@Override
	public boolean getDefault() {
		return this.value;
	}

	@Override
	public EnumMessage getMessage() {
		return this.message;
	}
}
