package org.bukkit.command.defaults;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class PluginsCommand extends BukkitCommand {
	public PluginsCommand(String name) {
		super(name);
		this.description = "Gets a list of plugins running on the server";
		this.usageMessage = "/plugins";
		this.setPermission("bukkit.command.plugins");
		this.setAliases(Arrays.asList("pl"));
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		if (!testPermission(sender))
			return true;

		sender.sendMessage("Plugins " + getPluginList());
		return true;
	}

	private String getPluginList() {
		// Paper start
		TreeMap<String, Plugin> plugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
			plugins.put(plugin.getDescription().getName(), plugin);
		}

		StringBuilder pluginList = new StringBuilder();
		for (Map.Entry<String, Plugin> entry : plugins.entrySet()) {
			if (pluginList.length() > 0) {
				pluginList.append(ChatColor.WHITE);
				pluginList.append(", ");
			}

			Plugin plugin = entry.getValue();

			pluginList.append(plugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED);
			pluginList.append(plugin.getDescription().getName());

			if (plugin.getDescription().getProvides().size() > 0) {
				pluginList.append(" (").append(String.join(", ", plugin.getDescription().getProvides())).append(")");
			}
		}

		return "(" + plugins.size() + "): " + pluginList.toString();
		// Paper end
	}

	// Spigot Start
	@Override
	public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
		return java.util.Collections.emptyList();
	}
	// Spigot End
}
