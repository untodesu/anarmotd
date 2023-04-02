package su.gprb.nrmc.motd;

import org.apache.commons.lang.StringUtils;
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

public class AnarMotd extends JavaPlugin implements CommandExecutor, Listener
{
    private enum TitleMode
    {
        // The plugin doesn't display
        // the title, leaving just a randomly
        // chosen message from a file.
        Disabled,

        // The plugin displays first four
        // letters of the title at the very
        // left side of the MOTD, followed by a
        // randomly chosen message from a file.
        Prefixed,

        // The plugin displays the title at the
        // first line, followed on the next line by
        // a randomly chosen message from a file.
        Subtitle,
    }

    // 2023-04-02: now that list contains pre-patched
    // motd strings and just chooses randomly between them
    // instead of constructing a new string every time someone
    // decides to ping the server through browser.
    private List<String> motds = null;
    private final Random random = new Random();

    @Override
    public void reloadConfig()
    {
        super.reloadConfig();

        FileConfiguration config = getConfig();
        config.addDefault("motd.file", "config/motd.txt");
        config.addDefault("motd.format", ChatColor.GOLD.toString());
        config.addDefault("title.mode", "disabled");
        config.addDefault("title.text", "A Minecraft Server");
        config.addDefault("title.format", ChatColor.GRAY.toString() + ChatColor.BOLD + ChatColor.ITALIC);
        config.options().copyDefaults(true);

        try {
            final String mformat = Objects.requireNonNull(config.getString("motd.format"));
            final String tformat = Objects.requireNonNull(config.getString("title.format"));
            final String title = Objects.requireNonNull(StringUtils.rightPad(config.getString("title.text"), 4));
            final String tx = (tformat + title.substring(0, 3) + ChatColor.RESET);
            final String ty = (tformat + title.substring(2, 4) + ChatColor.RESET);

            motds = Files.readAllLines(Paths.get(Objects.requireNonNull(config.getString("motd.file"))));
            motds.replaceAll(motd -> (mformat + motd));

            TitleMode mode = switch (Objects.requireNonNull(config.getString("title.mode")).toLowerCase()) {
                case "prefixed" -> TitleMode.Prefixed;
                case "subtitle" -> TitleMode.Subtitle;
                default -> TitleMode.Disabled;
            };

            switch (mode) {
                case Prefixed -> motds.replaceAll(motd -> (tx + " " + motd + ChatColor.RESET + System.lineSeparator() + ty));
                case Subtitle -> motds.replaceAll(motd -> (tformat + title + System.lineSeparator() + motd));
            }
        }
        catch(Exception e) {
            // If the file is inaccessible for some reason,
            // we just won't hijack the motd text, the server
            // would then use the default motd from server.properties
            motds = null;
        }

        saveConfig();

        Bukkit.getLogger().info("AnarMOTD reloaded!");
    }

    @Override
    public void onEnable()
    {
        // Register self as a command executor
        Objects.requireNonNull(getCommand("motd")).setExecutor(this);

        // Register self as an event handler
        getServer().getPluginManager().registerEvents(this, this);

        reloadConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        reloadConfig();
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(ServerListPingEvent event)
    {
        if(motds != null) {
            // In any other case, the server will fall
            // back to the value set in server.properties
            event.setMotd(motds.get(random.nextInt(motds.size())));
        }
    }
}
