package io.flixion.sc.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

import io.flixion.sc.SpawnerChunk;

public class PersistData implements Runnable{
	private ChunkData persistData;

	public PersistData(ChunkData persistData) {
		super();
		this.persistData = persistData;
	}
	
	@Override
	public void run() {
		if (SpawnerChunk.getDataFile().exists()) {
			SpawnerChunk.getDataConf().set("spawnerChunkData." + persistData.getID() + ".activeClaims", persistData.getClaimedChunks());
			SpawnerChunk.getDataConf().set("spawnerChunkData." + persistData.getID() + ".chunkData", new ArrayList<>(persistData.getPersistedChunks()));
			try {
				SpawnerChunk.getDataConf().save(SpawnerChunk.getDataFile());
			} catch (IOException e) {
				SpawnerChunk.getPL().getLogger().log(Level.SEVERE, "Unable to persist file data, IOEXCEPTION", e);
			}
		}
	}

}
