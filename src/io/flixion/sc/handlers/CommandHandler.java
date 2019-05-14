package io.flixion.sc.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.struct.Role;

import io.flixion.sc.SpawnerChunk;
import io.flixion.sc.data.PersistData;

public class CommandHandler implements Listener, CommandExecutor {
	
	@EventHandler
	public void handleFCommands(PlayerCommandPreprocessEvent e) {
		if (e.getMessage().toLowerCase().startsWith("/f spawnerchunk")) {
			e.setCancelled(true);
			if (e.getMessage().equalsIgnoreCase("/f spawnerchunk")) {
				e.getPlayer().sendMessage("Usage: /f spawnerchunk claim | /f spawnerchunk unclaim");
			} else if (e.getMessage().equalsIgnoreCase("/f spawnerchunk claim")) {
				FPlayer fp = FPlayers.getInstance().getByPlayer(e.getPlayer());
				if (fp.hasFaction()) {
					if (fp.getRole() == Role.MODERATOR || fp.getRole() == Role.ADMIN) {
						if (Board.getInstance().getFactionAt(new FLocation(fp)).getId().equals(fp.getFactionId())) {
							if (!SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().containsKey(fp.getFactionId())) {
								SpawnerChunk.getChunkHandler().addNewFaction(fp.getFactionId());
							}
							if (SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fp.getFactionId()).hasAvailableChunks()) {
								if (!SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fp.getFactionId()).getPersistedChunks().contains(chunkString(e.getPlayer().getLocation().getChunk()))) {
									String chunkData = chunkString(e.getPlayer().getLocation().getChunk());
									if (validSpawnerChunk(e.getPlayer().getLocation().getChunk(), fp.getFactionId())) {
										SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fp.getFactionId()).addChunk(chunkData);
										SpawnerChunk.getChunkHandler().updateData(chunkData, fp.getFactionId());
										Bukkit.getScheduler().runTaskAsynchronously(SpawnerChunk.getPL(), new PersistData(SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fp.getFactionId())));
										e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("claimSpawnerChunk").replaceAll("%location%", chunkData));
									} else {
										e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failClaimSpawnerChunkNearbyFaction"));
									}
								} else {
									e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failClaimSpawnerChunkAlreadyOwned"));
								}
							} else {
								e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failClaimSpawnerChunkMaxChunks"));
							}
						} else {
							e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failClaimSpawnerChunkNotOwned"));
						}
					} else {
						e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failClaimSpawnerChunkPermissions"));
					}
				} else {
					e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failNotInFaction"));
				}
			} else if (e.getMessage().equalsIgnoreCase("/f spawnerchunk unclaim")) {
				FPlayer fp = FPlayers.getInstance().getByPlayer(e.getPlayer());
				if (fp.hasFaction()) {
					if (fp.getRole() == Role.MODERATOR || fp.getRole() == Role.ADMIN) {
						if (SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().containsKey(fp.getFactionId())) {
							String chunkData = chunkString(e.getPlayer().getLocation().getChunk());
							if (Board.getInstance().getFactionAt(new FLocation(fp)).getId().equals(fp.getFactionId()) && SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fp.getFactionId()).getPersistedChunks().contains(chunkData)) {
								SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fp.getFactionId()).removeChunk(chunkData);
								Bukkit.getScheduler().runTaskAsynchronously(SpawnerChunk.getPL(), new PersistData(SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fp.getFactionId())));
								SpawnerChunk.getChunkHandler().updateData(chunkData, fp.getFactionId());
								e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("unclaimSpawnerChunk").replaceAll("%location%", chunkData));
							} else {
								e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failUnclaimSpawnerChunkNotOwned"));
							}
						} else {
							e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failUnclaimSpawnerChunkNoChunks"));
						}
					} else {
						e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failUnclaimSpawnerChunkPermissions"));
					}
				} else {
					e.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failNotInFaction"));
				}
			}
		}
	}
	
	private String chunkString(Chunk c) {
		return c.getX() + "," + c.getZ() + "," + c.getWorld().getName();
	}
	
	private boolean validSpawnerChunk(Chunk c, String fID) {
		for (int x = c.getX() - 15; x < c.getX() + 15; x++) {
			for (int z = c.getZ() - 15; z < c.getZ() + 15; z++) {
				String chunkString = chunkString(c.getWorld().getChunkAt(x, z));
				if (SpawnerChunk.getChunkHandler().getActiveSpawnerChunks().containsKey(chunkString)) {
					if (SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().containsKey(fID)) {
						if (!SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fID).getPersistedChunks().contains(chunkString)) {
							return false;
						}
					} else {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("lockdown")) {
			if (sender instanceof Player) {
				FPlayer fp = FPlayers.getInstance().getByPlayer((Player) sender);
				if (fp.hasFaction()) {
					if (SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().containsKey(fp.getFactionId())) {
						if (SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fp.getFactionId()).getLockdownStatus()) {
							fp.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("lockdownInformation").replaceAll("%time%", SpawnerChunk.getChunkHandler().getFactionSpawnerChunkData().get(fp.getFactionId()).stringLockdownData()));
						} else {
							fp.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("lockdownNotActive"));
						}
					} else {
						fp.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("lockdownNoChunks"));
					}
				} else {
					fp.getPlayer().sendMessage(SpawnerChunk.getChunkHandler().getMessageData().get("failNotInFaction"));
				}
			}
		}
		return true;
	}
}
