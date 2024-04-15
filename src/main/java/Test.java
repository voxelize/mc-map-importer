import io.xol.enklume.MinecraftWorld;
import io.xol.enklume.MinecraftRegion;
import io.xol.enklume.MinecraftChunk;
import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

public class Test {
    public static void main(String[] args) {
        try {
            File mcFolder = new File(System.getProperty("user.home") + "/Desktop/test");
            MinecraftWorld mcWorld = new MinecraftWorld(mcFolder);
            MinecraftRegion mcRegion = mcWorld.getRegion(-1, 0);
            MinecraftChunk mcChunk = mcRegion.getChunk(28, 11);

            for (int y = 77; y <= 79; y++) {
                for (int x = 12 - 1; x <= 12 + 1; x++) {
                    for (int z = 9 - 1; z <= 9 + 1; z++) {
                        int blockType = mcChunk.getBlockID(x, y, z);
                        int blockMeta = mcChunk.getBlockMeta(x, y, z);
                        if (blockType != 0) {
                            System.out.println("Block at x = " + x + ", y = " + y + ", z = " + z + " is " + blockType + " with metadata " + blockMeta);
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (DataFormatException e) {
            e.printStackTrace();
        } 
    }
}
