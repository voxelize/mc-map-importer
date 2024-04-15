# MC Map Importer

This is a tool built from [Enklume](https://github.com/Hugobros3/Enklume) to import an MC map into Voxelize. It reads an MC world and outputs a list of all the blocks (their coordinates, type and meta info) in a demarcated area.

To use it, follow these instructions

1. Install Java/JDK & Maven
2. Pull the [Enklume](https://github.com/Hugobros3/Enklume) repo and run `./gradlew publishToMavenLocal`
3. Pull this repo
4. Update constant variables in `Converter.java`
5. Run `mvn compile` & `mvn exec:java`

Once you have the list of blocks, you can import them to Voxelize through the front-end. To do this, it's important to convert the blocks' type and meta information into Voxelize blocks. This can be done by creating a list of `BlockUpdate`. Here is an example function to do so:

```typescript
const processBlocks = (update: {
  vx: number;
  vy: number;
  vz: number;
  id: number;
  meta: number;
}) => {
  const { id, meta } = update;
  if (world.registry.idMap.has(id * 100 + meta)) {
    update.type = id * 100 + meta;
    return update;
  }

  const stairIds = [
    53, 67, 108, 109, 114, 128, 134, 135, 136, 156, 163, 164, 180,
  ];
  const stairRotations = [
    { rotation: 0 },
    {
      rotation: 0,
      yRotation: Y_ROT_SEGMENTS / 2,
    },
    {
      rotation: 0,
      yRotation: Y_ROT_SEGMENTS * (3 / 4),
    },
    {
      rotation: 0,
      yRotation: Y_ROT_SEGMENTS * (1 / 4),
    },
    { rotation: 1 },
    {
      rotation: 1,
      yRotation: Y_ROT_SEGMENTS / 2,
    },
    {
      rotation: 1,
      yRotation: Y_ROT_SEGMENTS * (1 / 4),
    },
    {
      rotation: 1,
      yRotation: Y_ROT_SEGMENTS * (3 / 4),
    },
  ];

  // Rotatable Logs
  if (id === 17 || id === 162) {
    const n = id === 17 ? 4 : 2;
    const variation = meta % n;
    const rotation = Math.floor(meta / n) * 2;
    update.type = id * 100 + variation;
    update.rotation = rotation;
  }

  // Leaves
  else if (id === 18 || id === 161) {
    const n = id === 18 ? 4 : 2;
    const variation = meta % n;
    update.type = id * 100 + variation;
  }

  // Slabs
  else if (id === 126) {
    const n = 12;
    const variation = meta % n;
    update.type = id * 100 + variation;
  }

  // Stairs
  else if (stairIds.includes(id)) {
    const rotation = meta;
    update = { ...update, ...stairRotations[rotation] };
    update.type = id * 100;
  }

  // Rest
  else {
    update.type = id * 100;
  }

  if (world.registry.idMap.has(update.type)) {
    return update;
  }

  return false;
};
```

Some notes on this implementation

- For my registry, I chose to adopt MC's block IDs, but since Voxelize doesn't have meta information for blocks, the IDs for my registry are `id * 100 + meta`
- In most cases, the updated ID already exists in the registry, hence caught by the firt if-statement of the function, but more work is needed for rotations and special blocks like slabs
- For logs, MC stores the rotation and variety inside the meta in the following manner
  - All logs with a `py` rotation come first, followed by `px` and `pz`
  - Within the logs with `py` rotations are different varities of logs, e.g oak, spruce, birch and jungle
  - Voxelize's encoding for `py` rotation is 0, `px` is 2 and `pz` is 4
- For stairs, there are 8 possible combinations and hence meta ranges from 0-7
  - The first four combinations are when the base of the stair is touching its bottom block, and are composed of different orientations of the back of the stair (when meta = 0, back faces `px`, when 1 faces `nx`...)
  - The second four are when the base of the stair is touching its top block and its orientations are the same as before
- For slabs, all bottom slabs come first followed by top slabs, and within bottom and top slabs are the different variations of slabs
  - The way I setup my registry already mimics this encoding, so I just make sure the meta is within the correct range
- For the rest of the blocks, I just ignore the meta information

Once you process each block to match your registry, you can import the map on the front-end like so

```typescript
const chunkSize = Math.ceil(updatedWorldMap.length / 10);
for (let i = 0; i < updatedWorldMap.length; i += chunkSize) {
  const chunk = updatedWorldMap.slice(i, i + chunkSize);
  world.updateVoxels(chunk);
}
console.log("Map Loaded!");
```

where `updatedWorldMap` is of type `BlockUpdate[]`. Note that I divide it up into chunks because loading all blocks at once can cause your computer to freeze.

The final goal of this tool is to be able to upload MC map on Voxelize web and have it be imported automatically with some configurations.
