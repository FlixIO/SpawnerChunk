package io.flixion.sc.data;

import java.util.Date;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Factions;

import io.flixion.sc.SpawnerChunk;
import io.flixion.sc.Util;
import io.flixion.sc.handlers.ChunkHandler;

public class ChunkData {
	private int claimedChunks;
	private Set<String> persistedChunks;
	private boolean lockdown = false;
	private long lockdownInit;
	private String id;
	private BukkitTask lockdownTask;
	
	public ChunkData(int claimedChunks, Set<String> persistedChunks, String id) {
		super();
		this.claimedChunks = claimedChunks;
		this.persistedChunks = persistedChunks;
		this.id = id;
	}
	
	public boolean hasAvailableChunks() {
		if (claimedChunks < ChunkHandler.ALLOWED_CHUNKS) {
			return true;
		}
		return false;
	}
	
	public String getID() {
		return id;
	}
	
	public void addChunk(String data) {
		persistedChunks.add(data);
		claimedChunks++;
	}
	
	public void removeChunk(String data) {
		persistedChunks.remove(data);
		claimedChunks--;
	}
	
	public int getClaimedChunks() {
		return claimedChunks;
	}
	
	public Set<String> getPersistedChunks() {
		return persistedChunks;
	}
	
	public void initLockdown() {
		lockdown = true;
		lockdownInit = System.currentTimeMillis();
		if (lockdownTask != null) {
			lockdownTask.cancel();	
		} else {
			for (FPlayer fp : Factions.getInstance().getFactionById(id).getFPlayers()) {
				fp.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("lockdownActive"));
			}
		}
		lockdownTask = Bukkit.getScheduler().runTaskTimerAsynchronously(SpawnerChunk.getPL(), new Runnable() {
			int seconds = 0;
			@Override
			public void run() {
				if (SpawnerChunk.getChunkHandler().getRaidProgressMessages().containsKey(seconds)) {
					for (FPlayer fp : Factions.getInstance().getFactionById(id).getFPlayers()) {
						fp.getPlayer().sendMessage(Util.addColor(SpawnerChunk.getChunkHandler().getRaidProgressMessages().get(seconds)));
					}
				}
				if (seconds == ChunkHandler.LOCKDOWN_TIMER_SECONDS) {
					for (FPlayer fp : Factions.getInstance().getFactionById(id).getFPlayers()) {
						fp.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("lockdownEnd"));
					}
					lockdownTask.cancel();
					lockdown = false;
				}
				seconds++;
			}
			
		}, 0, 20);
	}
	
	public boolean getLockdownStatus() {
		return lockdown;
	}
	
	@SuppressWarnings("deprecation")
	public String stringLockdownData() {
		Date d = new Date(lockdownInit + (ChunkHandler.LOCKDOWN_TIMER_SECONDS * 1000) - System.currentTimeMillis());
		return " [" + d.getHours() + "h " + d.getMinutes() + "m " + d.getSeconds() + "s]";
	}
	
}
