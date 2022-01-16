/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.spark.bukkit;

import com.google.common.base.Preconditions;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility for interacting with the server's {@link CommandMap} instance.
 */
enum CommandMapUtil {
    ;

    private static final Constructor<PluginCommand> COMMAND_CONSTRUCTOR;
    private static final Field COMMAND_MAP_FIELD;
    private static final Field KNOWN_COMMANDS_FIELD;

    static {
        try {
            COMMAND_CONSTRUCTOR = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            COMMAND_CONSTRUCTOR.setAccessible(true);
            COMMAND_MAP_FIELD = SimplePluginManager.class.getDeclaredField("commandMap");
            COMMAND_MAP_FIELD.setAccessible(true);
            KNOWN_COMMANDS_FIELD = SimpleCommandMap.class.getDeclaredField("knownCommands");
            KNOWN_COMMANDS_FIELD.setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static CommandMap getCommandMap() {
        try {
            return (CommandMap) COMMAND_MAP_FIELD.get(Bukkit.getServer().getPluginManager());
        } catch (Exception e) {
            throw new RuntimeException("Could not get CommandMap", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> getKnownCommandMap(CommandMap commandMap) {
        try {
            return (Map<String, Command>) KNOWN_COMMANDS_FIELD.get(commandMap);
        } catch (Exception e) {
            throw new RuntimeException("Could not get known commands map", e);
        }
    }

    /**
     * Registers a CommandExecutor with the server
     *
     * @param plugin the plugin instance
     * @param command the command instance
     * @param aliases the command aliases
     */
    public static void registerCommand(Plugin plugin, CommandExecutor command, String... aliases) {
        Preconditions.checkArgument(aliases.length != 0, "No aliases");
        CommandMap commandMap = getCommandMap();
        Map<String, Command> knownCommandMap = getKnownCommandMap(commandMap);

        for (String alias : aliases) {
            try {
                PluginCommand cmd = COMMAND_CONSTRUCTOR.newInstance(alias, plugin);

                commandMap.register(plugin.getDescription().getName(), cmd);
                knownCommandMap.put(plugin.getDescription().getName().toLowerCase() + ":" + alias.toLowerCase(), cmd);
                knownCommandMap.put(alias.toLowerCase(), cmd);

                cmd.setLabel(alias.toLowerCase());

                cmd.setExecutor(command);

                if (command instanceof TabCompleter) {
                    cmd.setTabCompleter((TabCompleter) command);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Unregisters a CommandExecutor with the server
     *
     * @param command the command instance
     */
    public static void unregisterCommand(CommandExecutor command) {
        CommandMap commandMap = getCommandMap();
        Map<String, Command> knownCommandMap = getKnownCommandMap(commandMap);

        Iterator<Command> iterator = knownCommandMap.values().iterator();
        while (iterator.hasNext()) {
            Command cmd = iterator.next();
            if (cmd instanceof PluginCommand) {
                CommandExecutor executor = ((PluginCommand) cmd).getExecutor();
                if (command == executor) {
                    cmd.unregister(commandMap);
                    iterator.remove();
                }
            }
        }
    }

}