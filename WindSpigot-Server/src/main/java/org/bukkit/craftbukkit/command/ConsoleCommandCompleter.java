package org.bukkit.craftbukkit.command;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.Waitable;

// Paper start - JLine update
import net.minecraft.server.DedicatedServer; // Paper
import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
// Paper end

public class ConsoleCommandCompleter implements Completer {
	private final DedicatedServer server; // Paper - CraftServer -> DedicatedServer

	public ConsoleCommandCompleter(@NotNull DedicatedServer server) { // Paper - CraftServer -> DedicatedServe
		this.server = server;
	}

	// Paper start - Change method signature for JLine update
	@Override
	public void complete(@NotNull LineReader reader, @NotNull ParsedLine line, @NotNull List<Candidate> candidates) {
		final CraftServer server = this.server.server;
		final String buffer = line.line();
		// Paper end

		Waitable<List<String>> waitable = new Waitable<>() {
			@Override
			protected List<String> evaluate() {
				return server.getCommandMap().tabComplete(server.getConsoleSender(), buffer);
			}
		};
		server.getServer().processQueue.add(waitable); // Paper - Remove "this."
		try {
			List<String> offers = waitable.get();
			if (offers == null) {
				return; // Paper - Method returns void
			}

			// Paper start - JLine update
			for (String completion : offers) {
				if (completion.isEmpty()) {
					continue;
				}

				candidates.add(new Candidate(completion));
			}
			// Paper end

			// Paper start - JLine handles cursor now
			/*
			candidates.addAll(offers);

			final int lastSpace = buffer.lastIndexOf(' ');
			if (lastSpace == -1) {
				return cursor - buffer.length();
			} else {
				return cursor - (buffer.length() - lastSpace - 1);
			}
			*/
			// Paper end
		} catch (ExecutionException e) {
			server.getLogger().log(Level.WARNING, "Unhandled exception when tab completing", e); // Paper - Remove "this."
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		//return cursor; // Paper void method
	}
}
