package ga.windpvp.windspigot.async.world;

import ga.windpvp.windspigot.config.WindSpigotConfig;
import ga.windpvp.windspigot.world.WorldTicker;
import ga.windpvp.windspigot.world.WorldTickerManager;
import net.minecraft.server.WorldServer;

// This is just a world ticker, but async and with fully parallel entity tracking
public class AsyncWorldTicker extends WorldTicker {

	public AsyncWorldTicker(WorldServer worldServer) {
		super(worldServer);
	}

	@Override
	public void run() {
		// Synchronize for safe entity teleportation
		synchronized (this.worldserver) {
			if (WindSpigotConfig.fullyParallelTracking) {
				super.run(false); // Don't handle entity tracking (we do this after)
			} else {
				super.run(true); // Handle entity tracking
			}
		}
		// Decrement the latch to show that this world is done ticking
		WorldTickerManager.getInstance().getWorldTickLatch().decrement();
	}

	public void handleParallelTracker() {
		super.handleTracker();
		WorldTickerManager.getInstance().getTrackerLatch().decrement();
	}
}
