package com.auction.client.util;

import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TestGlb {
    public static void main(String[] args) {
        try {
            File file = new File("client/cache_3d/decb6a78-7b77-489c-ba3c-f6dad9c6a37b.glb");
            if (!file.exists()) {
                file = new File("cache_3d/decb6a78-7b77-489c-ba3c-f6dad9c6a37b.glb");
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            buffer.getInt(); // magic
            buffer.getInt(); // version
            buffer.getInt(); // length
            int jsonLength = buffer.getInt();
            buffer.getInt(); // jsonType
            byte[] jsonBytes = new byte[jsonLength];
            buffer.get(jsonBytes);
            String jsonString = new String(jsonBytes, StandardCharsets.UTF_8);
            JSONObject gltf = new JSONObject(jsonString);

            JSONArray nodes = gltf.getJSONArray("nodes");
            JSONArray accessors = gltf.getJSONArray("accessors");
            JSONArray meshes = gltf.getJSONArray("meshes");

            System.out.println("--- Inspecting Unnamed Node Meshes (33 to 39) ---");
            int[] unnamedNodes = {33, 34, 35, 36, 37, 38, 39};
            for (int nIdx : unnamedNodes) {
                JSONObject node = nodes.getJSONObject(nIdx);
                int mIdx = node.getInt("mesh");
                JSONObject mesh = meshes.getJSONObject(mIdx);
                JSONArray primitives = mesh.getJSONArray("primitives");
                JSONObject prim = primitives.getJSONObject(0);
                JSONObject attrs = prim.getJSONObject("attributes");
                int posIdx = attrs.getInt("POSITION");
                JSONObject acc = accessors.getJSONObject(posIdx);
                System.out.printf("Node %d (Mesh %d '%s'):\n", nIdx, mIdx, mesh.optString("name"));
                System.out.println("  POSITION Accessor: " + acc.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
