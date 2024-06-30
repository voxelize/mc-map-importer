import io.xol.enklume.MinecraftWorld;
import io.xol.enklume.MinecraftRegion;
import io.xol.enklume.MinecraftChunk;
import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Converter {
    public static final int START_X = -36;
    public static final int END_X = 36;
    public static final int START_Y = 15;
    public static final int END_Y = 31;
    public static final int START_Z = -36;
    public static final int END_Z = 36;

    public static final String INPUT_FOLDER = System.getProperty("user.home") + "/Desktop/KnockIt1";
    public static final String OUTPUT_FILE = System.getProperty("user.home") + "/Projects/temp/map.json";

    public static void main(String[] args) {
        try {
            File mcFolder = new File(INPUT_FOLDER);
            MinecraftWorld mcWorld = new MinecraftWorld(mcFolder);

            List<Map<String, Integer>> blocks = new ArrayList<>();

            // Save regions to avoid loading them multiple times
            Map<String, MinecraftRegion> regions = new HashMap<>();

            for (int x = START_X; x <= END_X; x++) {
                for (int y = START_Y; y <= END_Y; y++) {
                    for (int z = START_Z; z <= END_Z; z++) {
                        MinecraftRegion mcRegion = null;
                        MinecraftChunk mcChunk = null;

                        int chunkX = Math.floorDiv(x, 16);
                        int chunkZ = Math.floorDiv(z, 16);
                        int regionX = Math.floorDiv(chunkX, 32);
                        int regionZ = Math.floorDiv(chunkZ, 32);

                        if (regions.containsKey(regionX + "." + regionZ)) {
                            mcRegion = regions.get(regionX + "." + regionZ);
                        } else {
                            mcRegion = mcWorld.getRegion(regionX, regionZ);
                            regions.put(regionX + "." + regionZ, mcRegion);
                        }

                        // Modulus for negative numbers
                        int relChunkX = (chunkX % 32 + 32) % 32;
                        int relChunkZ = (chunkZ % 32 + 32) % 32;

                        mcChunk = mcRegion.getChunk(relChunkX, relChunkZ);

                        // Modulus for negative numbers
                        int relX = (x % 16 + 16) % 16;
                        int relZ = (z % 16 + 16) % 16;

                        int blockId = mcChunk.getBlockID(relX, y, relZ);
                        int blockMeta = mcChunk.getBlockMeta(relX, y, relZ);
                        if (blockId != 0) {
                            Map<String, Integer> blockMap = new HashMap<>();
                            blockMap.put("id", blockId);
                            blockMap.put("meta", blockMeta);
                            blockMap.put("vx", x);
                            blockMap.put("vy", y);
                            blockMap.put("vz", z);
                            blocks.add(blockMap);
                        }
                    }
                }
            }
            try {
                File file = new File(OUTPUT_FILE);
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileWriter writer = new FileWriter(file);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(blocks);
                writer.write(json);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
    }
}
