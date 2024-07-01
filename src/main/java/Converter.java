import io.xol.enklume.MinecraftWorld;
import io.xol.enklume.MinecraftRegion;
import io.xol.enklume.MinecraftChunk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.HashMap;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import java.io.FileReader;

public class Converter {
    public static final int START_X = -36;
    public static final int END_X = 36;
    public static final int START_Y = 15;
    public static final int END_Y = 31;
    public static final int START_Z = -36;
    public static final int END_Z = 36;

    public static final String INPUT_FOLDER = System.getProperty("user.home") + "/Desktop/KnockIt1";
    public static final String OUTPUT_FOLDER = System.getProperty("user.home") + "/Desktop/koth";
    public static final String BLOCK_MAPPINGS = System.getProperty("user.home") + "/Desktop/block_mappings.json";

    public static void main(String[] args) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(new TypeToken<Map<String, Integer>>() {
        }.getType(),
                (JsonDeserializer<Map<String, Integer>>) (json, typeOfT, context) -> {
                    Map<String, Integer> map = new HashMap<>();
                    JsonObject jsonObject = json.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                        map.put(entry.getKey(), entry.getValue().getAsInt());
                    }
                    return map;
                });
        Gson gson = gsonBuilder.create();

        Map<String, Integer> blockMappings = new HashMap<>();
        try {
            FileReader reader = new FileReader(BLOCK_MAPPINGS);
            blockMappings = gson.fromJson(reader, new TypeToken<Map<String, Integer>>() {
            }.getType());
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            File mcFolder = new File(INPUT_FOLDER);
            MinecraftWorld mcWorld = new MinecraftWorld(mcFolder);

            // Save regions to avoid loading them multiple times
            Map<String, MinecraftRegion> regions = new HashMap<>();

            Integer startChunkX = Math.floorDiv(START_X, 16);
            Integer startChunkZ = Math.floorDiv(START_Z, 16);
            Integer endChunkX = Math.floorDiv(END_X, 16);
            Integer endChunkZ = Math.floorDiv(END_Z, 16);

            for (int chunkX = startChunkX; chunkX <= endChunkX; chunkX++) {
                for (int chunkZ = startChunkZ; chunkZ <= endChunkZ; chunkZ++) {
                    MinecraftRegion mcRegion = null;
                    MinecraftChunk mcChunk = null;

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
                    save(mcChunk, getChunkName(chunkX, chunkZ), blockMappings);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
    }

    // Save a certain chunk.
    public static boolean save(MinecraftChunk chunk, String chunkName, Map<String, Integer> blockMappings) {
        String path = getChunkFilePath(chunkName);
        File file = new File(path);
        File parentDir = file.getParentFile();
        File grandParentDir = parentDir.getParentFile();
        try {
            if (!grandParentDir.exists()) {
                grandParentDir.mkdirs();
            }
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create chunk file.", e);
        }

        Map<String, String> data = new HashMap<>();
        List<Integer> voxels = getVoxelData(chunk, blockMappings);
        data.put("id", NanoIdUtils.randomNanoId());
        data.put("voxels", toBase64(voxels));
        data.put("heightMap", "");

        Gson gson = new Gson();
        String json = gson.toJson(data);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to chunk file.", e);
        }

        return true;
    }

    private static List<Integer> getVoxelData(MinecraftChunk chunk, Map<String, Integer> blockMappings) {
        List<Integer> voxels = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    int blockId = chunk.getBlockID(x, y, z);
                    int blockMeta = chunk.getBlockMeta(x, y, z);
                    if (blockId != 0) {
                        int mappedBlockId = blockMappings.get(blockId + ":" + blockMeta);
                        voxels.add(mappedBlockId);
                    } else {
                        voxels.add(0);
                    }
                }
            }
        }

        return voxels;
    }

    private static String getChunkFilePath(String chunkName) {
        String path = OUTPUT_FOLDER + "/chunks/" + chunkName + ".json";
        return path;
    }

    public static String getChunkName(int cx, int cz) {
        return String.format("%d%s%d", cx, getConcat(), cz);
    }

    private static String getConcat() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return "_";
        } else {
            return "|";
        }
    }

    // TODO: Make this function work like the one in Rust
    public static String toBase64(List<Integer> data) {
        // Convert List<Integer> to byte array
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.size() * 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int value : data) {
            byteBuffer.putInt(value);
        }
        byte[] bytes = byteBuffer.array();

        // Compress the byte array using ZLIB
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream)) {
            deflaterOutputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] compressedBytes = byteArrayOutputStream.toByteArray();

        // Encode the compressed byte array to Base64
        return Base64.getEncoder().encodeToString(compressedBytes);
    }
}
