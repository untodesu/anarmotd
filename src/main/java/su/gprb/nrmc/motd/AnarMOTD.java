package su.gprb.nrmc.motd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class AnarMOTD extends JavaPlugin implements Listener
{
    private static class ReloadCommand implements CommandExecutor
    {
        AnarMOTD plugin;

        public ReloadCommand(AnarMOTD plugin)
        {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
        {
            plugin.reloadSettings();
            plugin.reloadSubtitles();
            Bukkit.getLogger().info("AnarMOTD reloaded!");
            return true;
        }
    }

    private String title = null;
    private String subtitleFile = null;
    private String subtitlePrefix = null;
    private List<String> subtitles = null;
    private final Random random = new Random();

    private void initSettings()
    {
        FileConfiguration config = getConfig();
        config.addDefault("title.text", "A Minecraft Server");
        config.addDefault("title.prefix", ChatColor.WHITE.toString() + ChatColor.BOLD + ChatColor.ITALIC);
        config.addDefault("subtitle.file", "config/motd.txt");
        config.addDefault("subtitle.prefix", ChatColor.GOLD.toString() + ChatColor.ITALIC);
        config.options().copyDefaults(true);
    }

    private void reloadSettings()
    {
        FileConfiguration config = getConfig();
        title = config.getString("title.prefix") + config.getString("title.text") + ChatColor.RESET;
        subtitleFile = config.getString("subtitle.file");
        subtitlePrefix = config.getString("subtitle.prefix");
        saveConfig();
    }

    private void reloadSubtitles()
    {
        try {
            // The text file ${PWD}/config/MOTD.txt contains text lines,
            // each line of which represents a randomly chosen MOTD subtitle.
            subtitles = Files.readAllLines(Paths.get(subtitleFile));

            // Patch subtitles with a prefix.
            // Subtitle is the last thing that would be present, so it seems like
            // a good idea to not give a shit about resetting the text format (&r)
            if(subtitlePrefix != null) {
                subtitles.replaceAll(s -> subtitlePrefix + s);
            }
        }
        catch(Exception ex) {
            // If the file is inaccessible for any
            // reason, we just won't display the subtitle.
            subtitles = null;
        }
    }

    @Override
    public void onEnable()
    {
        initSettings();
        reloadSettings();
        reloadSubtitles();
        Objects.requireNonNull(getCommand("motd")).setExecutor(new ReloadCommand(this));
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getLogger().info("AnarMOTD enabled");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(ServerListPingEvent event)
    {
        StringBuilder motd = new StringBuilder(title);

        if(subtitles != null) {
            motd.append(System.lineSeparator());
            motd.append(subtitles.get(random.nextInt(subtitles.size())));
        }

        event.setMotd(motd.toString());
    }
}
