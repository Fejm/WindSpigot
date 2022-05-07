package ga.windpvp.windspigot.world;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import ga.windpvp.windspigot.async.AsyncUtil;
import ga.windpvp.windspigot.async.ResettableLatch;
import ga.windpvp.windspigot.async.world.AsyncWorldTicker;
import ga.windpvp.windspigot.config.WindSpigotConfig;
import javafixes.concurrency.ReusableCountLatch;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.NetworkManager;
import net.minecraft.server.PlayerConnection;
import net.minecraft.server.WorldServer;

public class WorldTickerManager {

	// List of cached world tickers
	private List<WorldTicker> worldTickers = new ArrayList<>();

	// Latch to wait for world tick completion
	private final ResettableLatch worldTickLatch;
	
	// Latch to wait for entity tracking operations
	private final ResettableLatch trackLatch;

	// Lock for ticking
	public final static Object LOCK = new Object();

	// Executor for world ticking
	private final Executor worldTickExecutor = Executors
			.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("WindSpigot Parallel World Thread %d").build());
	
	// Instance
	private static WorldTickerManager worldTickerManagerInstance;
	
	// Initializes the world ticker manager
	public WorldTickerManager() {
		worldTickerManagerInstance = this;
		
		// Initialize the world ticker latch
		if (WindSpigotConfig.parallelWorld) {
			this.worldTickLatch = new ResettableLatch();
		} else {
			this.worldTickLatch = null;
		}
		if (WindSpigotConfig.fullyParallelTracking) {
			this.trackLatch = new ResettableLatch();
		} else {
			this.trackLatch = null;
		}
	}

	// Caches Runnables for less Object creation
	private void cacheWorlds(boolean isAsync) {
		// Only create new world tickers if needed
		if (this.worldTickers.size() != MinecraftServer.getServer().worlds.size()) {
			worldTickers.clear();
						
			// Create world tickers
			for (WorldServer world : MinecraftServer.getServer().worlds) {
				
				// Decide between creating sync/async world tickers
				if (isAsync) {
					worldTickers.add(new AsyncWorldTicker(world)); 
				} else {
					worldTickers.add(new WorldTicker(world));
				}
				
			}
			// Null check to prevent resetting the latch when not using parallel worlds
			if (this.worldTickLatch != null) {
				// Reuse the latch
				this.worldTickLatch.reset(this.worldTickers.size());
			}
		}
	}

	// Ticks all worlds
	public void tick() {
		if (WindSpigotConfig.parallelWorld) {
			tickAsync();
		} else {
			tickSync();
		}
	}
	
	private void tickSync() {
		// Cache world tick runnables if not cached already
		this.cacheWorlds(false); // Cache them as sync world tickers

		// Tick each world on one thread
		for (WorldTicker ticker : this.worldTickers) {
			ticker.run();
		}
	}
	
	private void tickAsync() {
		// Cache world tick runnables if not cached already
		this.cacheWorlds(true); // Cache them as async world tickers

		// Tick each world with a reused runnable on its own thread, except the last ticker (that one is run sync)
		for (int index = 0; index < this.worldTickers.size(); index++) { 
			// Tick all worlds but one on a separate thread
			if (index < this.worldTickers.size() - 1) {
				AsyncUtil.run(this.worldTickers.get(index), this.worldTickExecutor);
			} else {
				// Run the last ticker on the main thread, no need to schedule it async as all
				// other tickers are running already
				this.worldTickers.get(index).run();
			}
		}

		try {
			// Wait for worlds to finish ticking then reset latch
			worldTickLatch.waitTillZero();
			this.worldTickLatch.reset(this.worldTickers.size());;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (WindSpigotConfig.fullyParallelTracking)
			handleFullyParallelTrackers();
	}
	
	private void handleFullyParallelTrackers () {
		/* 
		 * We modify this so that disabling/enabling automatic flushing is only called once,
		 * the tracker is then updated for each world parallel with multple threads.
		 */
		
		if (MinecraftServer.getServer().getPlayerList().getPlayerCount() != 0) // Tuinity
		{
			// Tuinity start - controlled flush for entity tracker packets
			List<NetworkManager> disabledFlushes = new java.util.ArrayList<>(MinecraftServer.getServer().getPlayerList().getPlayerCount());
			for (EntityPlayer player : MinecraftServer.getServer().getPlayerList().players) {
				PlayerConnection connection = player.playerConnection;
				if (connection != null) {
					connection.networkManager.disableAutomaticFlush();
					disabledFlushes.add(connection.networkManager);
				}
			}
			
			// Entity tracking is done completely parallel, each world has its own tracker
			// which is run at the same time as other worlds. Each tracker utilizes
			// multiple threads
			for (int index = 0; index < this.worldTickers.size(); index++) {
				if (index < this.worldTickers.size() - 1) {
					final int indexClone = index;
					AsyncUtil.run(() -> ((AsyncWorldTicker) this.worldTickers.get(indexClone)).handleParallelTracker(), this.worldTickExecutor);
				} else {
					((AsyncWorldTicker) this.worldTickers.get(index)).handleParallelTracker();
				}
			}
			
			try {
				// Wait for trackers to finish updating, then prepare for the next tick
				trackLatch.waitTillZero();
				trackLatch.reset(this.worldTickers.size());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			for (NetworkManager networkManager : disabledFlushes) {
				networkManager.enableAutomaticFlush();
			}
		}
		// Tuinity end - controlled flush for entity tracker packets
	}

	/**
	 * @return The world tick executor
	 */
	public Executor getExecutor() {
		return this.worldTickExecutor;
	}
	
	/**
	 * @return The count down latch for world ticking
	 */
	public ResettableLatch getWorldTickLatch() {
		return this.worldTickLatch;
	}
	
	/**
	 * @return The count down latch for parallel trackers
	 */
	public ResettableLatch getTrackerLatch() {
		return this.trackLatch;
	}
	
	/**
	 * @return The world ticker manager instance
	 */
	public static WorldTickerManager getInstance() {
		return worldTickerManagerInstance;
	}

}
