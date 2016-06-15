package fr.evercraft.everstats.service.event;

import org.spongepowered.api.event.cause.Cause;

import fr.evercraft.everapi.services.stats.event.StatsReloadEvent;

public class EStatsReloadEvent implements StatsReloadEvent {

    private final Cause cause;

    public EStatsReloadEvent(Cause cause) {
    	this.cause = cause;
    }

	@Override
	public Cause getCause() {
		return this.cause;
	}
}
