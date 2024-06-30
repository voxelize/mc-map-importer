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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Arrays;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AllBlocks {

  public static final String OUTPUT_FILE = System.getProperty("user.home") + "/Desktop/all_blocks.json";

  public static void main(String[] args) {
    try {
      File mcFolder = new File(Converter.INPUT_FOLDER);
      MinecraftWorld mcWorld = new MinecraftWorld(mcFolder);

      Set<List<Integer>> blockIds = new HashSet<>();

      // Save regions to avoid loading them multiple times
      Map<String, MinecraftRegion> regions = new HashMap<>();

      for (int x = Converter.START_X; x <= Converter.END_X; x++) {
        for (int y = Converter.START_Y; y <= Converter.END_Y; y++) {
          for (int z = Converter.START_Z; z <= Converter.END_Z; z++) {
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
              blockIds.add(Arrays.asList(blockId, blockMeta));
            }
          }
        }
      }
      try {
        File file = new File(OUTPUT_FILE);
        if (!file.exists()) {
          file.createNewFile();
        }
        List<String> sortedBlockIds = new ArrayList<>();
        blockIds.stream()
            .sorted((a, b) -> {
              for (int i = 0; i < a.size(); i++) {
                int cmp = a.get(i).compareTo(b.get(i));
                if (cmp != 0) {
                  return cmp;
                }
              }
              return 0;
            })
            .forEach(list -> sortedBlockIds.add(list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(":"))));
        FileWriter writer = new FileWriter(file);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(sortedBlockIds);
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
