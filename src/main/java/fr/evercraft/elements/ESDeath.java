package fr.evercraft.elements;

import java.sql.Timestamp;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.entity.damage.DamageType;

public class ESDeath{

	private int id;
	private Player killer;
	private Player victim;
	private DamageType reason;
	private Timestamp time;

	public ESDeath(int id, Player killer, Player victim, DamageType cause, Timestamp time) {
		this.id = id;
		this.killer = killer;
		this.victim = victim;
		this.reason = cause;
		this.time = time;
	}

	public ESDeath(Player killer, Player victim, DamageType cause, Timestamp time) {
		this.killer = killer;
		this.victim = victim;
		this.reason = cause;
		this.time = time;
	}

	public int getId() {
		return this.id;
	}

	public Player getKiller() {
		return this.killer;
	}

	public Player getVictim() {
		return this.victim;
	}

	public DamageType getReason() {
		return this.reason;
	}

	public Timestamp getTime() {
		return this.time;
	}

}
