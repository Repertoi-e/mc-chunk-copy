package thecsdev.chunkcopy;

import java.io.File;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.util.math.ChunkPos;
import thecsdev.chunkcopy.api.AutoChunkCopy;
import thecsdev.chunkcopy.api.ChunkCopyAPI;
import thecsdev.chunkcopy.api.config.ChunkCopyConfig;
import thecsdev.chunkcopy.api.data.ChunkData;
import thecsdev.chunkcopy.api.data.block.CDBChunkSections;
import thecsdev.chunkcopy.api.data.block.CDBEntitiesLegacy;
import thecsdev.chunkcopy.api.data.block.CDBEntityBlocksLegacy;
import thecsdev.chunkcopy.command.ChunkCopyCommand;
import thecsdev.chunkcopy.server.ChunkCopyServer;

/**
 * The main ChunkCopy class. This class
 * contains vital information about the mod.
 */
public abstract class ChunkCopy
{
	// ==================================================
	private static ChunkCopy Instance = null;
	// --------------------------------------------------
	public static final Logger LOGGER = LoggerFactory.getLogger(getModID());
	// --------------------------------------------------
	public static final String ModName     = "Chunk Copy";
	public static final String ModID       = "chunkcopy";
	public static final int    FileVersion = 759;

	// ==================================================
	public ChunkCopy()
	{
		Instance = this;
		
		//log stuff
		LOGGER.info("Initializing '" + getModName() + "' as '" + getClass().getSimpleName() + "'.");
		
		//register config keys and load the config -- work in progress
		ChunkCopyConfig.KEYS.add(ChunkCopyConfig.PASTE_ENTITIES);
		//ChunkCopyConfig.loadConfig();
		
		//handle API registrations
		ChunkData.registerChunkDataBlockType(CDBChunkSections.class);
		ChunkData.registerChunkDataBlockType(CDBEntityBlocksLegacy.class);
		ChunkData.registerChunkDataBlockType(CDBEntitiesLegacy.class);
		
		//register auto-paste handler
		ServerChunkEvents.CHUNK_LOAD.register((sWorld, sChunk) ->
		{
			//check if auto-copy is pasting
			if(!AutoChunkCopy.isPasting()) return;
			final ChunkPos scPos = sChunk.getPos();
			
			//convert the action into a task
			final Runnable task = () ->
			{
				try
				{
					//paste data into the chunk
					final String fileName = AutoChunkCopy.getFileName();
					ChunkCopyAPI.loadChunkDataIO(sWorld, sChunk.getPos(), fileName);
				}
				catch(Exception exc) {}
			};
			
			//run the task
			new Thread(() ->
			{
				try
				{
					//wait a bit for the chunk to be fully ready
					Thread.sleep(500);
					
					//make sure the chunk is fully ready
					while(!sWorld.isChunkLoaded(scPos.x, scPos.z))
						Thread.sleep(100);
					
					//run the task on this thread (pasting will be on the main server thread)
					task.run();
				}
				catch (Exception e) {}
			}).start();
		});
	}
	// --------------------------------------------------
	public static String getModName() { return ModName; }
	public static String getModID() { return ModID; }
	// --------------------------------------------------
	/**
	 * Returns the directory where this mod
	 * saves and loads it's data.
	 */
	public static File getModSavesDirectory()
	{
		File runDir = getRunDirectory();
		return new File(runDir.getAbsolutePath() + "/mods/" + ModID + "/");				
	}
	// --------------------------------------------------
	/**
	 * Returns the directory where Minecraft
	 * is currently running.
	 */
	public static File getRunDirectory() { return new File(System.getProperty("user.dir")); }
	// ==================================================
	/**
	 * Returns the registered {@link ChunkCopyCommand}.
	 * Will return null if the mod hasn't been initialized yet.
	 */
	@Nullable
	public abstract ChunkCopyCommand<?> getCommand();
	// ==================================================
}
