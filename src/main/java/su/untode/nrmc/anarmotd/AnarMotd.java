package su.untode.nrmc.anarmotd;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public final class AnarMotd extends JavaPlugin implements Listener
{
    public enum TitleMode
    {
        Hidden,
        Prefix,
        Subtitle,
    }

    private List<TextComponent> motds = null;
    private final Random random = new Random();

    private NamedTextColor getColor(@NotNull String value)
    {
        return switch(value.toLowerCase()) {
            case "black"        -> NamedTextColor.BLACK;
            case "dark_blue"    -> NamedTextColor.DARK_BLUE;
            case "dark_green"   -> NamedTextColor.DARK_GREEN;
            case "dark_aqua"    -> NamedTextColor.DARK_AQUA;
            case "dark_red"     -> NamedTextColor.DARK_RED;
            case "dark_purple"  -> NamedTextColor.DARK_PURPLE;
            case "gold"         -> NamedTextColor.GOLD;
            case "dark_gray"    -> NamedTextColor.DARK_GRAY;
            case "blue"         -> NamedTextColor.BLUE;
            case "green"        -> NamedTextColor.GREEN;
            case "aqua"         -> NamedTextColor.AQUA;
            case "red"          -> NamedTextColor.RED;
            case "light_purple" -> NamedTextColor.LIGHT_PURPLE;
            case "yellow"       -> NamedTextColor.YELLOW;
            case "white"        -> NamedTextColor.WHITE;
            default             -> NamedTextColor.GRAY;
        };
    }

    private Set<TextDecoration> getDecorations(@NotNull String value)
    {
        if(!value.equalsIgnoreCase("none")) {
            final String[] items = value.toLowerCase().split(",");
            final Set<TextDecoration> decorations = new HashSet<>();

            for(String item : items) {
                switch (item) {
                    case "bold"             -> decorations.add(TextDecoration.BOLD);
                    case "italic"           -> decorations.add(TextDecoration.ITALIC);
                    case "underline"        -> decorations.add(TextDecoration.UNDERLINED);
                    case "strikethrough"    -> decorations.add(TextDecoration.STRIKETHROUGH);
                    case "obfuscated"       -> decorations.add(TextDecoration.OBFUSCATED);
                }
            }

            return decorations;
        }

        return new HashSet<>();
    }

    @Override
    public void reloadConfig()
    {
        super.reloadConfig();

        FileConfiguration config = getConfig();
        config.addDefault("title.mode", "hidden");
        config.addDefault("title.color", "gray");
        config.addDefault("title.style", "none");
        config.addDefault("title.value", "A Minecraft Server");
        config.addDefault("motd.file", "plugins/AnarMotd/motds.txt");
        config.addDefault("motd.color", "gold");
        config.addDefault("motd.style", "none");
        config.options().copyDefaults(true);

        try {
            final TitleMode mode = switch(Objects.requireNonNull(config.getString("title.mode")).toLowerCase()) {
                case "prefix" -> TitleMode.Prefix;
                case "subtitle" -> TitleMode.Subtitle;
                default -> TitleMode.Hidden;
            };

            final NamedTextColor titleColor = getColor(Objects.requireNonNull(config.getString("title.color")));
            final NamedTextColor motdColor = getColor(Objects.requireNonNull(config.getString("motd.color")));
            final Set<TextDecoration> titleDec = getDecorations(Objects.requireNonNull(config.getString("title.style")));
            final Set<TextDecoration> motdDec = getDecorations(Objects.requireNonNull(config.getString("motd.style")));

            final String title = Objects.requireNonNull(StringUtils.rightPad(config.getString("title.value"), 4));
            final TextComponent titleComponent = Component.text(title, titleColor, titleDec);
            final TextComponent prefixComponent1 = Component.text(title.substring(0, 2), titleColor, titleDec);
            final TextComponent prefixComponent2 = Component.text(title.substring(2, 4), titleColor, titleDec);

            final List<String> entries = Files.readAllLines(Paths.get(Objects.requireNonNull(config.getString("motd.file"))));

            motds = new ArrayList<>();

            for(String entry : entries) {
                final TextComponent component = Component.text(entry, motdColor, motdDec);

                if(mode == TitleMode.Prefix) {
                    final TextComponent pcomp = Component.text()
                        .append(prefixComponent1).appendSpace()
                        .append(component).appendNewline()
                        .append(prefixComponent2)
                        .build();
                    motds.add(pcomp);
                    continue;
                }

                if(mode == TitleMode.Subtitle) {
                    final TextComponent scomp = Component.text()
                        .append(titleComponent).appendNewline()
                        .append(component)
                        .build();
                    motds.add(scomp);
                    continue;
                }

                motds.add(component);
            }
        }
        catch(Exception ex) {
            Bukkit.getLogger().warning(ex.toString());

            // If something is wrong with the configuration
            // or the MOTD file is not available, we just
            // default to using the vanilla MOTD.
            motds = null;
        }

        saveConfig();
    }

    @Override
    public void onEnable()
    {
        reloadConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(ServerListPingEvent event)
    {
        if(motds != null) {
            // In any other case, the server will fall
            // back to the value set in server.properties
            event.motd(motds.get(random.nextInt(motds.size())));
        }
    }
}
