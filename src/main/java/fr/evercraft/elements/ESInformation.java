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
package fr.evercraft.elements;

public class ESInformation {
	private int kill;
	private int death;
	
	public ESInformation(int kill, int death) {
		this.kill = kill;
		this.death = death;
	}

	public int getKill() {
		return this.kill;
	}

	public int getDeath() {
		return this.death;
	}
	
	public int getRatio() {
		double ratio = this.kill;
		if (death != 0){
			ratio = this.kill/death;
		}
		return (int) Math.ceil(ratio);
	}
}
