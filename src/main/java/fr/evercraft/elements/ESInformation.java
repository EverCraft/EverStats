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
