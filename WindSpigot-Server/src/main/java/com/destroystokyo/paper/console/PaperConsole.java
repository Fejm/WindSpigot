package com.destroystokyo.paper.console;

import net.minecraft.server.DedicatedServer;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.bukkit.craftbukkit.command.ConsoleCommandCompleter;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

public class PaperConsole extends SimpleTerminalConsole
{
	private final DedicatedServer server;

	public PaperConsole(@NotNull DedicatedServer server)
	{
		this.server = server;
	}

	@Override
	protected LineReader buildReader(@NotNull LineReaderBuilder builder)
	{
		builder.appName("Paper")
		       .variable(LineReader.HISTORY_FILE, java.nio.file.Paths.get(".console_history"))
		       .completer(new ConsoleCommandCompleter(this.server))
		       .option(LineReader.Option.COMPLETE_IN_WORD, true);

		return super.buildReader(builder);
	}

	@Override
	protected boolean isRunning()
	{
		return !this.server.isStopped() && this.server.isRunning();
	}

	@Override
	protected void runCommand(@NotNull String command)
	{
		this.server.issueCommand(command, this.server);
	}

	@Override
	protected void shutdown()
	{
		this.server.safeShutdown();
	}
}
