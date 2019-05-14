package io.flixion.sc.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.event.FactionDisbandEvent;

import io.flixion.sc.SpawnerChunk;
import io.flixion.sc.Util;
import io.flixion.sc.data.ChunkData;
import net.milkbowl.vault.economy.EconomyResponse;

public class ChunkHandler implements Listener {
	private Map<String, ChunkData> factionIDtoChunkDataMap = new HashMap<>();
	private Map<String, String> chunkToFactionIDMap = new HashMap<>();
	private Map<String, String> messageData = new HashMap<>();
	private Set<String> explosionLocationCache = new HashSet<>();
	public static int ALLOWED_CHUNKS;
	public static double SPAWNER_MINING_COST;
	public static int LOCKDOWN_TIMER_SECONDS;
	private Set<String> blacklistedCommands;
	private Map<Integer, String> raidProgressMessages = new HashMap<>();

	public ChunkHandler(Map<String, ChunkData> factionIDtoChunkDataMap, Map<String, String> chunkToFactionIDMap,
			Map<String, String> messageData, Set<String> blacklistedCommands, Set<String> raidProgressMessages) {
		super();
		this.factionIDtoChunkDataMap = factionIDtoChunkDataMap;
		this.chunkToFactionIDMap = chunkToFactionIDMap;
		this.messageData = messageData;
		this.blacklistedCommands = blacklistedCommands;
		for (String s : raidProgressMessages) {
			String [] data = s.split("#");
			this.raidProgressMessages.put(Integer.parseInt(data[1]), Util.addColor(data[0]));
		}
	}
	
	@EventHandler
	public void commandsDuringLockdown(PlayerCommandPreprocessEvent e) {
		FPlayer fp = FPlayers.getInstance().getByPlayer(e.getPlayer());
		if (fp.hasFaction()) {
			if (factionIDtoChunkDataMap.containsKey(fp.getFactionId())) {
				if (factionIDtoChunkDataMap.get(fp.getFactionId()).getLockdownStatus()) {
					if (blacklistedCommands.contains(e.getMessage().toLowerCase())) {
						e.setCancelled(true);
						e.getPlayer().sendMessage(messageData.get("failCommandWhileLockdown").replaceAll("%command%", e.getMessage().toLowerCase()));
					}
				}
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void breakDuringLockdown(BlockBreakEvent e) {
		if (e.getBlock().getType() == Material.MOB_SPAWNER) {
			FPlayer fp = FPlayers.getInstance().getByPlayer(e.getPlayer());
			if (fp.hasFaction()) {
				if (factionIDtoChunkDataMap.containsKey(fp.getFactionId())) {
					if (factionIDtoChunkDataMap.get(fp.getFactionId()).getLockdownStatus()) {
						EconomyResponse transaction = SpawnerChunk.getEcon().withdrawPlayer(fp.getName(), SPAWNER_MINING_COST);
						if (transaction.transactionSuccess()) {
							fp.getPlayer().sendMessage(messageData.get("breakSpawnerSuccess").replaceAll("%cost%", SPAWNER_MINING_COST + ""));
						} else {
							fp.getPlayer().sendMessage(messageData.get("breakSpawnerFail").replaceAll("%cost%", SPAWNER_MINING_COST + ""));
							e.setCancelled(true);
						}
					}
				}
			}
		} else if (e.getBlock().getType() == Material.ICE) {
			e.getBlock().setType(Material.AIR);
			Block b = e.getBlock();
			performIteration(new Location(b.getLocation().getWorld(), + b.getX(),  b.getY(), b.getZ()), "SPAWNER", e.getPlayer(), new ItemStack(e.getPlayer().getItemInHand()), false);
		}
	}
	
	@EventHandler
	public void preventMelt (BlockFadeEvent e) {
		if (e.getBlock().getType() == Material.ICE) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void initLockdown(EntityExplodeEvent e) {
		if (e.getEntityType() == EntityType.PRIMED_TNT) {
			if (!explosionLocationCache.contains(locationString(e.getLocation()))) {
				String locString = locationString(e.getLocation());
				String chunkData = nearbySpawnerChunk(e.getLocation().getChunk());
				if (chunkData != null) {
					factionIDtoChunkDataMap.get(chunkToFactionIDMap.get(chunkData)).initLockdown();
					explosionLocationCache.add(locString);
					Bukkit.getScheduler().runTaskLater(SpawnerChunk.getPL(), new Runnable() {
						
						@Override
						public void run() {
							explosionLocationCache.remove(locString);
						}
					}, 2);
				}
			}
		}
	}
	
	@EventHandler
	public void handleDisband(FactionDisbandEvent e) {
		if (factionIDtoChunkDataMap.containsKey(e.getFaction().getId())) {
			SpawnerChunk.getDataConf().set("spawnerChunkData." + e.getFaction().getId(), null);
			try {
				SpawnerChunk.getDataConf().save(SpawnerChunk.getDataFile());
			} catch (IOException e1) {
				SpawnerChunk.getPL().getLogger().log(Level.SEVERE, "Unable to update information in data.yml", e);
			}
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	@EventHandler
	public void monitorWaterPlacement(PlayerBucketEmptyEvent e) {
		if (e.getBucket() == Material.WATER_BUCKET) {
			e.setCancelled(true);
			Block b = e.getBlockClicked();
			switch(e.getBlockFace()) {
			case UP:
				b = e.getBlockClicked().getLocation().add(0, 1, 0).getBlock();
				break;
			case DOWN:
				b = e.getBlockClicked().getLocation().subtract(0, 1, 0).getBlock();
				break;
			case NORTH:
				b = e.getBlockClicked().getLocation().subtract(0, 0, 1).getBlock();
				break;
			case SOUTH:
				b = e.getBlockClicked().getLocation().add(0, 0, 1).getBlock();
				break;
			case EAST:
				b = e.getBlockClicked().getLocation().add(1, 0, 0).getBlock();
				break;
			case WEST:
				b = e.getBlockClicked().getLocation().subtract(1, 0, 0).getBlock();
				break;
			}
			performIteration(new Location(b.getLocation().getWorld(), + b.getX(),  b.getY(), b.getZ()), "SPAWNER", e.getPlayer(), new ItemStack(e.getPlayer().getItemInHand()), true);
		}
	}
	
	@EventHandler
	public void monitorSpawnerPlacement(BlockPlaceEvent e) {
		if (e.getBlock().getType() == Material.MOB_SPAWNER) {
			FPlayer fp = FPlayers.getInstance().getByPlayer(e.getPlayer());
			if (fp.hasFaction()) {
				if (Board.getInstance().getFactionAt(new FLocation(e.getBlock().getLocation())).getId().equals(fp.getFactionId())) {
					if (SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().containsKey(fp.getFactionId()) && SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fp.getFactionId()).getPersistedChunks().contains(chunkString(e.getBlock().getChunk()))) {
						if (!checkValidSpawnerPlacement(e.getBlock(), "WATER")) {
							e.setBuild(false);
							fp.getPlayer().sendMessage(messageData.get("spawnerPlaceNearWater"));
						}
					} else {
						e.setBuild(false);
						fp.getPlayer().sendMessage(messageData.get("spawnerPlaceOutsideClaim"));
					}
				} else {
					e.setBuild(false);
					fp.getPlayer().sendMessage(messageData.get("spawnerPlaceOutsideClaim"));
				}
			} else {
				e.setBuild(false);
				fp.getPlayer().sendMessage(messageData.get("spawnerPlaceOutsideClaim"));
			}
		}
	}
	
	private void performIteration(final Location loc, final String type, final Player p, final ItemStack i, boolean bucket) {
		Bukkit.getScheduler().runTask(SpawnerChunk.getPL(), new Runnable() {
			
			@Override
			public void run() {
				if (p.getGameMode() != GameMode.CREATIVE && bucket == true) {
					p.setItemInHand(null);
				}
				boolean valid = true;
				for (int x = loc.getBlockX() - 7; x < loc.getBlockX() + 7; x++) {
					if (!valid) {
						break;
					}
					for (int y = loc.getBlockY() - 7; y < loc.getBlockY() + 7; y++) {
						if (!valid) {
							break;
						}
						for (int z = loc.getBlockZ() - 7; z < loc.getBlockZ() + 7; z++) {
							if (!valid) {
								break;
							}
							if (loc.getWorld().getBlockAt(x, y, z).getType().toString().contains(type)) {
								valid = false;
							}
						}
					}
				}
				if (!valid) {
					p.sendMessage(messageData.get("spawnerPlaceNearWater"));
					loc.getBlock().getState().update(true, true);
					if (p.getGameMode() != GameMode.CREATIVE && bucket == true) {
						p.getInventory().addItem(i);
						p.updateInventory();
					}
				} else {
					if (p.getGameMode() != GameMode.CREATIVE && bucket == true) {
						if (i.getAmount() > 1) {
							i.setAmount(i.getAmount() - 1);
							p.getInventory().addItem(i);
						}
						p.getInventory().addItem(new ItemStack(Material.BUCKET));
					}
					loc.getBlock().setType(Material.WATER, true);
					loc.getBlock().getState().update(true, true);
				}
			}
		});
	}
	
	private String nearbySpawnerChunk(Chunk c) {
		for (int x = c.getX() - 15; x < c.getX() + 15; x++) {
			for (int z = c.getZ() - 15; z < c.getZ() + 15; z++) {
				String chunkString = chunkString(c.getWorld().getChunkAt(x, z));
				if (SpawnerChunk.getChunkHandler().getActiveSpawnerChunks().containsKey(chunkString)) {
					return chunkString;
				}
			}
		}
		return null;
	}
	
	private boolean checkValidSpawnerPlacement(Block b, String type) {
		for (int x = b.getX() - 7; x < b.getX() + 7; x++) {
			for (int y = b.getY() - 7; y < b.getY() + 7; y++) {
				for (int z = b.getZ() - 7; z < b.getZ() + 7; z++) {
					if (b.getWorld().getBlockAt(x, y, z).getType().toString().contains(type)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private String locationString(Location loc) {
		return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "," + loc.getWorld().getName();
	}
	
	private String chunkString(Chunk c) {
		return c.getX() + "," + c.getZ() + "," + c.getWorld().getName();
	}

	public Map<String, ChunkData> getFactionSpawnerChunkData() {
		return factionIDtoChunkDataMap;
	}

	public Map<String, String> getMessageData() {
		return messageData;
	}

	public Map<String, String> getActiveSpawnerChunks() {
		return chunkToFactionIDMap;
	}
	
	public void setMessageData(Map<String, String> data) {
		this.messageData = data;
	}
	
	public void updateData(String chunkData, String id) {
		chunkToFactionIDMap.put(chunkData, id);
	}
	
	public void addNewFaction(String id) {
		factionIDtoChunkDataMap.put(id, new ChunkData(0, new HashSet<>(), id));
	}

	public Map<Integer, String> getRaidProgressMessages() {
		return raidProgressMessages;
	}
}
