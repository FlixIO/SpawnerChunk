package io.flixion.sc;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import io.flixion.sc.data.ChunkData;
import io.flixion.sc.handlers.ChunkHandler;
import io.flixion.sc.handlers.CommandHandler;
import net.milkbowl.vault.economy.Economy;

public class SpawnerChunk extends JavaPlugin {
	public static SpawnerChunk instance;
	private static ChunkHandler chunkHandler;
	private static File data;
	private static YamlConfiguration fileConf;
	private static Economy econ;
	
	public static SpawnerChunk getPL() {
		return instance;
	}
	
	public static Economy getEcon() {
		return econ;
	}
	
	public static ChunkHandler getChunkHandler() {
		return chunkHandler;
	}
	
	public static YamlConfiguration getDataConf() {
		return fileConf;
	}
	
	public static File getDataFile() {
		return data;
	}
	
	public void onEnable() {
		instance = this;
		getConfig().options().copyDefaults(true);
		saveConfig();
		if (!setupEconomy()) {
			Bukkit.getLogger().log(Level.SEVERE, "Dependancy [Vault] cannot be found, disabling!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
		}
		init();
	}
	
	private void init() {
		data = new File(getDataFolder(), "data.yml");
		if (!data.exists()) {
			saveResource("data.yml", false);
		}
		fileConf = YamlConfiguration.loadConfiguration(data);
		Map<String, ChunkData> factionChunkData = new HashMap<>();
		Map<String, String> chunkToFIDMap = new HashMap<>();
		if (fileConf.getConfigurationSection("spawnerChunkData") != null) {
			for (String key : fileConf.getConfigurationSection("spawnerChunkData").getKeys(false)) {
				if (key != null) {
					Set<String> chunkData = new HashSet<>(fileConf.getStringList("spawnerChunkData." + key + ".chunkData"));
					for (String s : chunkData) {
						chunkToFIDMap.put(s, key);
					}
					factionChunkData.put(key, new ChunkData(fileConf.getInt("spawnerChunkData." + key + "activeClaims"), chunkData, key));
				}
			}
		}
		getLogger().log(Level.INFO, "Imported SpawnerChunk Factions [" + factionChunkData.size() + " factions]");
		
		int lockdownTimerDuration = getConfig().getInt("lockdownTimerDuration");
		double spawnerMiningCost = getConfig().getDouble("spawnerMiningCost");
		int spawnerChunksPerFaction = getConfig().getInt("maxSpawnerChunksPerFaction");
		Set<String> raidProgressMessages = new HashSet<>(getConfig().getStringList("raidProgressMessages"));
		Set<String> commandBlacklist = new HashSet<>(getConfig().getStringList("lockdownCommandBlacklist"));
		Map<String, String> messageData = new HashMap<>();
		
		if (getConfig().getConfigurationSection("messages") != null) {
			for (String key : getConfig().getConfigurationSection("messages").getKeys(false)) {
				if (key != null) {
					messageData.put(key, Util.addColor(getConfig().getString("messages." + key)));
				}
			}
		}
		
		chunkHandler = new ChunkHandler(factionChunkData, chunkToFIDMap, messageData, commandBlacklist, raidProgressMessages);
		ChunkHandler.ALLOWED_CHUNKS = spawnerChunksPerFaction;
		ChunkHandler.LOCKDOWN_TIMER_SECONDS = lockdownTimerDuration;
		ChunkHandler.SPAWNER_MINING_COST = spawnerMiningCost;
		
		Bukkit.getPluginManager().registerEvents(chunkHandler, this);
		CommandHandler commandHandler = new CommandHandler();
		getCommand("lockdown").setExecutor(commandHandler);
		Bukkit.getPluginManager().registerEvents(commandHandler, this);
	}
	
	private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
}
