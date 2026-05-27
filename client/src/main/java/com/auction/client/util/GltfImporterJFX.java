package com.auction.client.util;

import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.scene.AmbientLight;
import javafx.scene.PointLight;
import javafx.scene.SubScene;
import javafx.scene.PerspectiveCamera;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public class GltfImporterJFX {
    private static final Logger logger = LoggerFactory.getLogger(GltfImporterJFX.class);

    private static class PrimitiveData {
        float[] positions;
        int[] indices;
        float[] texCoords;
        Image diffuseMap;
        Color diffuseColor;
        double metallic;
        double roughness;
        boolean isDial;
    }

    public static Node load(String glbUrl) {
        try {
            byte[] bytes = downloadBytes(glbUrl);
            return loadFromBytes(bytes, glbUrl);
        } catch (Exception e) {
            logger.warn("GLB download failed for URL {}: {}", glbUrl, e.getMessage(), e);
            return loadFromBytes(new byte[0], glbUrl);
        }
    }

    public static Node loadFromBytes(byte[] bytes, String glbUrl) {
        logger.info("Parsing 3D GLB model from bytes for: {}", glbUrl);
        
        Group root3D = new Group();
        boolean loaded = false;
        
        if (bytes != null && bytes.length > 12) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                
                int magic = buffer.getInt();
                int version = buffer.getInt();
                int length = buffer.getInt();
                
                if (magic == 0x46546C67) { // "glTF" magic signature in GLB
                    int jsonLength = buffer.getInt();
                    int jsonType = buffer.getInt(); // 0x4E4F534A ("JSON")
                    
                    byte[] jsonBytes = new byte[jsonLength];
                    buffer.get(jsonBytes);
                    String jsonString = new String(jsonBytes, StandardCharsets.UTF_8);
                    JSONObject gltf = new JSONObject(jsonString);
                    
                    // Align buffer position to 4-byte boundary
                    int currentPos = buffer.position();
                    int alignedPos = (currentPos + 3) & ~3;
                    if (alignedPos < buffer.limit()) {
                        buffer.position(alignedPos);
                    }
                    
                    int binLength = 0;
                    int binType = 0;
                    
                    if (buffer.remaining() >= 8) {
                        binLength = buffer.getInt();
                        binType = buffer.getInt(); // 0x004E4942 ("BIN")
                        
                        // Robust sliding-window scanning if signature is shifted due to non-standard exporter padding
                        if (binType != 0x004E4942) {
                            boolean foundBin = false;
                            for (int offset = currentPos; offset < Math.min(currentPos + 32, buffer.limit() - 4); offset++) {
                                if (buffer.getInt(offset) == 0x004E4942) {
                                    binType = 0x004E4942;
                                    binLength = buffer.getInt(offset - 4);
                                    buffer.position(offset + 4);
                                    foundBin = true;
                                    break;
                                }
                            }
                            if (!foundBin) {
                                throw new java.io.IOException("BIN chunk signature (0x004E4942) not found in GLB file.");
                            }
                        }
                    } else {
                        throw new java.io.IOException("GLB file too short to contain BIN chunk.");
                    }
                    
                    byte[] binBytes = new byte[binLength];
                    buffer.get(binBytes);
                    
                    org.json.JSONArray reqExts = gltf.optJSONArray("extensionsRequired");
                    if (reqExts != null) {
                        for (int i = 0; i < reqExts.length(); i++) {
                            if ("KHR_draco_mesh_compression".equals(reqExts.getString(i))) {
                                throw new java.io.IOException("Draco compression (KHR_draco_mesh_compression) is not supported. Please export and upload your GLB model without Draco compression.");
                            }
                        }
                    }

                    List<PrimitiveData> tempPrimitives = new ArrayList<>();
                    org.json.JSONArray imagesArray = gltf.optJSONArray("images");
                    Image[] loadedImages = new Image[imagesArray != null ? imagesArray.length() : 0];
                    
                    org.json.JSONArray nodesArray = gltf.optJSONArray("nodes");
                    org.json.JSONArray meshesArray = gltf.optJSONArray("meshes");
                    
                    if (nodesArray != null && meshesArray != null) {
                        for (int n = 0; n < nodesArray.length(); n++) {
                            JSONObject node = nodesArray.getJSONObject(n);
                            if (!node.has("mesh")) continue;
                            
                            String nodeName = node.optString("name", "");
                            if (nodeName.equals("unnamed") || nodeName.isEmpty()) {
                                continue; // Skip helper/guide shapes
                            }
                            
                            int meshIndex = node.getInt("mesh");
                            if (meshIndex < 0 || meshIndex >= meshesArray.length()) continue;
                            
                            // Parse node local transforms
                            float tx = 0.0f, ty = 0.0f, tz = 0.0f;
                            if (node.has("translation")) {
                                org.json.JSONArray t = node.getJSONArray("translation");
                                tx = (float) t.getDouble(0);
                                ty = (float) t.getDouble(1);
                                tz = (float) t.getDouble(2);
                            }
                            
                            float sx = 1.0f, sy = 1.0f, sz = 1.0f;
                            if (node.has("scale")) {
                                org.json.JSONArray s = node.getJSONArray("scale");
                                sx = (float) s.getDouble(0);
                                sy = (float) s.getDouble(1);
                                sz = (float) s.getDouble(2);
                            }
                            
                            float qx = 0.0f, qy = 0.0f, qz = 0.0f, qw = 1.0f;
                            if (node.has("rotation")) {
                                org.json.JSONArray r = node.getJSONArray("rotation");
                                qx = (float) r.getDouble(0);
                                qy = (float) r.getDouble(1);
                                qz = (float) r.getDouble(2);
                                qw = (float) r.getDouble(3);
                            }
                            
                            JSONObject meshObj = meshesArray.getJSONObject(meshIndex);
                            org.json.JSONArray primitives = meshObj.optJSONArray("primitives");
                            if (primitives == null) continue;
                            
                            for (int p = 0; p < primitives.length(); p++) {
                                JSONObject primitive = primitives.getJSONObject(p);
                                JSONObject attributes = primitive.optJSONObject("attributes");
                                if (attributes == null || !attributes.has("POSITION")) continue;
                                
                                int posAccessorIndex = attributes.getInt("POSITION");
                                float[] rawPositions = readFloatArray(binBytes, gltf, posAccessorIndex);
                                
                                // Apply node transformations directly to vertices!
                                float[] transformedPositions = new float[rawPositions.length];
                                for (int i = 0; i < rawPositions.length / 3; i++) {
                                    float x = rawPositions[i * 3];
                                    float y = rawPositions[i * 3 + 1];
                                    float z = rawPositions[i * 3 + 2];
                                    
                                    // 1. Scale
                                    x *= sx;
                                    y *= sy;
                                    z *= sz;
                                    
                                    // 2. Rotate (if non-identity rotation)
                                    if (qx != 0.0f || qy != 0.0f || qz != 0.0f || qw != 1.0f) {
                                        float txRot = 2.0f * (qy * z - qz * y);
                                        float tyRot = 2.0f * (qz * x - qx * z);
                                        float tzRot = 2.0f * (qx * y - qy * x);
                                        
                                        x = x + qw * txRot + (qy * tzRot - qz * tyRot);
                                        y = y + qw * tyRot + (qz * txRot - qx * tzRot);
                                        z = z + qw * tzRot + (qx * tyRot - qy * txRot);
                                    }
                                    
                                    // 3. Translate
                                    transformedPositions[i * 3]     = x + tx;
                                    transformedPositions[i * 3 + 1] = y + ty;
                                    transformedPositions[i * 3 + 2] = z + tz;
                                }
                                
                                float[] texCoords = null;
                                if (attributes.has("TEXCOORD_0")) {
                                    int texAccessorIndex = attributes.getInt("TEXCOORD_0");
                                    texCoords = readTexCoords(binBytes, gltf, texAccessorIndex);
                                }
                                
                                int[] indices;
                                if (primitive.has("indices")) {
                                    int indicesAccessorIndex = primitive.getInt("indices");
                                    indices = readIntArray(binBytes, gltf, indicesAccessorIndex);
                                } else {
                                    int vertexCount = transformedPositions.length / 3;
                                    indices = new int[vertexCount];
                                    for (int i = 0; i < vertexCount; i++) {
                                        indices[i] = i;
                                    }
                                }
                                
                                PrimitiveData primData = new PrimitiveData();
                                primData.positions = transformedPositions;
                                primData.indices = indices;
                                primData.texCoords = texCoords;
                                
                                Color diffColor = Color.color(0.85, 0.85, 0.85); // Standard PBR default is white/light-gray (Silver/Steel)
                                double metFactor = 0.0;
                                double roughFactor = 0.5;
                                Image diffuseMap = null;
                                boolean isDial = false;
                                int matIdx = primitive.optInt("material", -1);
                                if (matIdx >= 0) {
                                    org.json.JSONArray mats = gltf.optJSONArray("materials");
                                    if (mats != null && matIdx < mats.length()) {
                                        JSONObject mat = mats.getJSONObject(matIdx);
                                        String matName = mat.optString("name", "").toLowerCase();
                                        
                                        // Skip rendering glass cover meshes to ensure maximum dial clarity and eliminate JavaFX transparency/sorting/darkening issues
                                        if (matName.contains("glass") || matName.contains("lens") || matName.contains("crystal")) {
                                            continue;
                                        }
                                        
                                        JSONObject pbr = mat.optJSONObject("pbrMetallicRoughness");
                                        if (pbr != null) {
                                            org.json.JSONArray bcf = pbr.optJSONArray("baseColorFactor");
                                            if (bcf != null && bcf.length() >= 3) {
                                                double r = Math.max(0.0, Math.min(1.0, bcf.getDouble(0)));
                                                double g = Math.max(0.0, Math.min(1.0, bcf.getDouble(1)));
                                                double b = Math.max(0.0, Math.min(1.0, bcf.getDouble(2)));
                                                double a = bcf.length() > 3 ? Math.max(0.0, Math.min(1.0, bcf.getDouble(3))) : 1.0;
                                                diffColor = new Color(r, g, b, a);
                                            }
                                            
                                            // Load baseColorTexture if present
                                            JSONObject bct = pbr.optJSONObject("baseColorTexture");
                                            if (bct != null) {
                                                int texIdx = bct.getInt("index");
                                                org.json.JSONArray textures = gltf.optJSONArray("textures");
                                                if (textures != null && texIdx >= 0 && texIdx < textures.length()) {
                                                    JSONObject textureObj = textures.getJSONObject(texIdx);
                                                    if (textureObj.has("source")) {
                                                        int imgIdx = textureObj.getInt("source");
                                                        if (imgIdx >= 0 && imgIdx < loadedImages.length) {
                                                            if (loadedImages[imgIdx] == null) {
                                                                loadedImages[imgIdx] = readTextureImage(binBytes, gltf, imgIdx, glbUrl);
                                                            }
                                                            diffuseMap = loadedImages[imgIdx];
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            metFactor = pbr.optDouble("metallicFactor", 1.0);
                                            roughFactor = pbr.optDouble("roughnessFactor", 1.0);
                                            
                                            // Override specific properties for watch dial to ensure brilliant, vibrant sunburst look
                                            if (matName.contains("dial") && !matName.contains("print")) {
                                                isDial = true;
                                                // Keep the original base color (e.g. beautiful Jade Green) from the model
                                                // while optimizing the metallic and glossy lacquer reflection factors
                                                metFactor = 0.95; // Highly metallic sunburst sheen
                                                roughFactor = 0.08; // Ultra-glossy luxury lacquer finish
                                            }
                                        }
                                    }
                                }
                                primData.diffuseColor = diffColor;
                                primData.diffuseMap = diffuseMap;
                                primData.metallic = metFactor;
                                primData.roughness = roughFactor;
                                primData.isDial = isDial;
                                
                                tempPrimitives.add(primData);
                            }
                        }
                    }
                    
                    if (!tempPrimitives.isEmpty()) {
                        // Calculate Global Bounding Box across ALL primitives to prevent scrambled assemblies
                        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
                        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
                        
                        for (PrimitiveData prim : tempPrimitives) {
                            float[] positions = prim.positions;
                            for (int i = 0; i < positions.length / 3; i++) {
                                float x = positions[i * 3];
                                float y = positions[i * 3 + 1];
                                float z = positions[i * 3 + 2];
                                if (x < minX) minX = x; if (x > maxX) maxX = x;
                                if (y < minY) minY = y; if (y > maxY) maxY = y;
                                if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
                            }
                        }
                        
                        float centerX = (minX + maxX) / 2.0f;
                        float centerY = (minY + maxY) / 2.0f;
                        float centerZ = (minZ + maxZ) / 2.0f;
                        
                        float sizeX = maxX - minX;
                        float sizeY = maxY - minY;
                        float sizeZ = maxZ - minZ;
                        float diagonal = (float) Math.sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ);
                        
                        float targetDiagonal = 210.0f; // Perfect visual size consistency across all aspect ratios
                        float scale = diagonal > 0 ? targetDiagonal / diagonal : 1.0f;
                        
                        // Center and scale ALL primitives collectively
                        for (PrimitiveData prim : tempPrimitives) {
                            float[] positions = prim.positions;
                            for (int i = 0; i < positions.length / 3; i++) {
                                positions[i * 3]     = (positions[i * 3] - centerX) * scale;
                                positions[i * 3 + 1] = (positions[i * 3 + 1] - centerY) * scale;
                                positions[i * 3 + 2] = (positions[i * 3 + 2] - centerZ) * scale;
                            }
                            
                            TriangleMesh mesh = new TriangleMesh();
                            mesh.getPoints().addAll(positions);
                            if (prim.texCoords != null) {
                                mesh.getTexCoords().addAll(prim.texCoords);
                            } else {
                                mesh.getTexCoords().addAll(0.0f, 0.0f);
                            }
                            
                            int[] faces = new int[prim.indices.length * 2];
                            for (int i = 0; i < prim.indices.length / 3; i++) {
                                int v1 = prim.indices[i * 3];
                                int v2 = prim.indices[i * 3 + 1];
                                int v3 = prim.indices[i * 3 + 2];
                                
                                faces[i * 6]     = v1;
                                faces[i * 6 + 1] = prim.texCoords != null ? v1 : 0;
                                faces[i * 6 + 2] = v2;
                                faces[i * 6 + 3] = prim.texCoords != null ? v2 : 0;
                                faces[i * 6 + 4] = v3;
                                faces[i * 6 + 5] = prim.texCoords != null ? v3 : 0;
                            }
                            mesh.getFaces().addAll(faces);
                            
                            MeshView meshView = new MeshView(mesh);
                            meshView.setCullFace(javafx.scene.shape.CullFace.NONE); // Double-sided rendering to prevent back-face culling stripes!
                            
                            PhongMaterial material = new PhongMaterial();
                            if (prim.diffuseMap != null) {
                                material.setDiffuseMap(prim.diffuseMap);
                            }
                            
                             Color finalDiffColor = prim.diffuseColor;
                             
                               // 1. Emulate premium steel/chrome metal reflection bodies by darkening and cooling the diffuse color 
                               // of metallic elements (metallic > 0.38, excluding watch dials) to a brilliant silver-steel range (72% to 80% brightness),
                               // finding the perfect luxury midpoint between overexposed white and dark gunmetal.
                               if (prim.metallic > 0.38 && !prim.isDial) {
                                   finalDiffColor = Color.color(
                                       finalDiffColor.getRed() * 0.72,
                                       finalDiffColor.getGreen() * 0.74,
                                       finalDiffColor.getBlue() * 0.80,
                                       finalDiffColor.getOpacity()
                                   );
                               }
                               
                               // 2. Brighten watch dials slightly to compensate for glass reflection and bezel shadow
                               if (prim.isDial) {
                                   finalDiffColor = Color.color(
                                       Math.min(1.0, finalDiffColor.getRed() * 1.35),
                                       Math.min(1.0, finalDiffColor.getGreen() * 1.35),
                                       Math.min(1.0, finalDiffColor.getBlue() * 1.35),
                                       finalDiffColor.getOpacity()
                                   );
                               }
                               
                               // 3. Boost extremely dark diffuse colors (like black car body or tyres)
                               // so that ambient and diffuse lights can beautifully reveal their geometry
                               // while maintaining deep, high-contrast matte blacks.
                               double avgColor = (finalDiffColor.getRed() + finalDiffColor.getGreen() + finalDiffColor.getBlue()) / 3.0;
                               if (avgColor < 0.12) {
                                   double boostFactor = 0.16 / Math.max(0.01, avgColor);
                                   finalDiffColor = Color.color(
                                       Math.min(1.0, finalDiffColor.getRed() * boostFactor),
                                       Math.min(1.0, finalDiffColor.getGreen() * boostFactor),
                                       Math.min(1.0, finalDiffColor.getBlue() * boostFactor),
                                       finalDiffColor.getOpacity()
                                   );
                               }
                               material.setDiffuseColor(finalDiffColor);
                              
                              // Premium High-Contrast Glossy Specular Setup (mirroring professional showroom softbox reflections)
                              // Smooth surfaces (low roughness) get tight, brilliant specular highlights (like polished car lacquer).
                              // Rough surfaces get broad, matte, diffused specular reflections.
                              // Metallic case/strap elements get a perfectly balanced specular intensity (scaled by 1.10 instead of 1.35).
                              double specMultiplier = (prim.metallic > 0.38 && !prim.isDial) ? 1.10 : 1.35;
                              double specularIntensity = Math.min(0.9, Math.max(0.2, (1.0 - prim.roughness) * specMultiplier));
                              material.setSpecularColor(Color.color(specularIntensity, specularIntensity, specularIntensity));
                            material.setSpecularPower(Math.max(8.0, 128.0 * (1.0 - prim.roughness)));
                            
                            meshView.setMaterial(material);
                            
                            root3D.getChildren().add(meshView);
                        }
                        loaded = true;
                        logger.info("Successfully loaded and assembled {} meshes for: {}", tempPrimitives.size(), glbUrl);
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not parse GLB structure, falling back to beautiful placeholder shape: {}", e.getMessage(), e);
                // Dump details to error.txt inside cache_3d to debug remote environments
                try {
                    Files.createDirectories(Paths.get("client", "cache_3d"));
                    StringWriter sw = new StringWriter();
                    sw.write(e.toString());
                    sw.write(System.lineSeparator());
                    for (StackTraceElement element : e.getStackTrace()) {
                        sw.write("\tat " + element + System.lineSeparator());
                    }
                    Files.writeString(
                        Paths.get("client", "cache_3d", "error.txt"),
                        "Error loading GLB URL: " + glbUrl + "\n\nStacktrace:\n" + sw.toString(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                    );
                } catch (Exception ignored) {}
            }
        }
        
        if (!loaded) {
            // Draw the beautiful luxury placeholder watch model as fallback
            Sphere core = new Sphere(80);
            PhongMaterial coreMaterial = new PhongMaterial();
            coreMaterial.setDiffuseColor(Color.valueOf("#e040a0"));
            coreMaterial.setSpecularColor(Color.WHITE);
            coreMaterial.setSpecularPower(32.0);
            core.setMaterial(coreMaterial);
            
            Cylinder bezel = new Cylinder(100, 20);
            bezel.setRotationAxis(Rotate.X_AXIS);
            bezel.setRotate(90);
            PhongMaterial bezelMaterial = new PhongMaterial();
            bezelMaterial.setDiffuseColor(Color.valueOf("#7c52aa"));
            bezelMaterial.setSpecularColor(Color.GOLD);
            bezelMaterial.setSpecularPower(64.0);
            bezel.setMaterial(bezelMaterial);
            
            Box band1 = new Box(20, 180, 20);
            PhongMaterial bandMaterial = new PhongMaterial();
            bandMaterial.setDiffuseColor(Color.valueOf("#0096cc"));
            bandMaterial.setSpecularColor(Color.WHITE);
            band1.setMaterial(bandMaterial);
            
            Box band2 = new Box(180, 20, 20);
            band2.setMaterial(bandMaterial);
            
            root3D.getChildren().addAll(bezel, core, band1, band2);
        }
        
        // 1. Khởi tạo góc xoay trục Z cố định là 180 độ theo thiết lập mới của người dùng
        Rotate rInitial = new Rotate(180, Rotate.Z_AXIS);
        root3D.getTransforms().add(rInitial);
        
        // 2. Tạo group bàn xoay (Turntable) để xoay ngang (ry - quanh trục Y của model)
        Group turntableGroup = new Group(root3D);
        Rotate ry = new Rotate(0, Rotate.Y_AXIS);
        turntableGroup.getTransforms().add(ry);
        
        // 3. Tạo group xoay dọc (Pitch) để xoay dọc theo trục X tuyệt đối của màn hình (rx - quanh trục X tuyệt đối)
        // Điều này khắc phục triệt để lỗi "steering wheel" / gimbal lock khi kéo chuột dọc
        Group pitchGroup = new Group(turntableGroup);
        Rotate rx = new Rotate(0, Rotate.X_AXIS);
        pitchGroup.getTransforms().add(rx);
        
        // Create a parent group for the fixed room lights (so lights don't rotate with the watch!)
        Group sceneRoot = new Group();
        sceneRoot.getChildren().add(pitchGroup);
        
        // Create a SubScene for 3D rendering with a depth buffer
        SubScene subScene = new SubScene(sceneRoot, 550, 400, true, javafx.scene.SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        
        // Setup Perspective Camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(1.0); // Increased from 0.1 to avoid depth buffer precision loss / Z-fighting stripes
        camera.setFarClip(1000.0);
        Translate cameraTranslate = new Translate(0, 0, -315);
        camera.getTransforms().add(cameraTranslate);
        subScene.setCamera(camera);
        
        // Premium Showroom Multi-Point Studio Lighting System
        // We use a rich, bright AmbientLight (80% brightness) to ensure clean baseline visibility.
        AmbientLight ambientLight = new AmbientLight(Color.rgb(205, 205, 205));
        
        // 1. Key Light (Main light source, top-left front): Casts highly brilliant white primary highlights (98% intensity)
        PointLight keyLight = new PointLight(Color.rgb(250, 250, 250));
        keyLight.getTransforms().add(new Translate(-400, -300, -400));
        
        // 2. Fill Light (Secondary light source, top-right front): Strongly softens shadows and reveals details (72% intensity)
        PointLight fillLight = new PointLight(Color.rgb(180, 180, 190));
        fillLight.getTransforms().add(new Translate(400, -300, -400));
        
        // 3. Backlight / Rim Light (Separates object from background, top-back): Creates sharp, brilliant contour highlights (86% intensity)
        PointLight rimLight = new PointLight(Color.rgb(220, 220, 220));
        rimLight.getTransforms().add(new Translate(0, -350, 450));
        
        // 4. Overhead Studio Softbox (Top ceiling light): Casts bright overhead reflections onto wings, cockpit, and body (90% intensity)
        PointLight ceilingLight = new PointLight(Color.rgb(230, 230, 240));
        ceilingLight.getTransforms().add(new Translate(0, -500, 0));
        
        // 5. Frontal Camera Soft Light (On-axis fill light): Placed close to the camera to directly illuminate 
        // the recessed watch dial and face from the front with brilliant clarity (85% intensity).
        PointLight cameraLight = new PointLight(Color.rgb(215, 220, 230));
        cameraLight.getTransforms().add(new Translate(0, 0, -340));
        
        // 6. Grazing Studio Spotlight (Oblique side light): Placed close to the watch plane at a sharp angle 
        // to cast rich micro-shadows and high-contrast grazing highlights on the Tapisserie relief dots (75% intensity).
        PointLight grazingLight = new PointLight(Color.rgb(190, 195, 210));
        grazingLight.getTransforms().add(new Translate(-250, -200, -120));
        
        sceneRoot.getChildren().addAll(ambientLight, keyLight, fillLight, rimLight, ceilingLight, cameraLight, grazingLight);
        
        // Enable mouse rotation controls
        final double[] anchorX = {0};
        final double[] anchorY = {0};
        final double[] anchorAngleX = {0};
        final double[] anchorAngleY = {0};
        
        subScene.setOnMousePressed(event -> {
            anchorX[0] = event.getSceneX();
            anchorY[0] = event.getSceneY();
            anchorAngleX[0] = rx.getAngle();
            anchorAngleY[0] = ry.getAngle();
        });
        
        subScene.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - anchorX[0];
            double deltaY = event.getSceneY() - anchorY[0];
            
            // Xoay dọc tự do tuyệt đối theo trục X của màn hình (không giới hạn biên độ)
            rx.setAngle(anchorAngleX[0] + deltaY);
            
            // Xoay ngang tự do theo trục Y của bàn xoay
            ry.setAngle(anchorAngleY[0] - deltaX);
        });
        
        // Enable mouse scroll to zoom in and out
        subScene.setOnScroll(event -> {
            double deltaY = event.getDeltaY();
            double newZ = cameraTranslate.getZ();
            
            if (deltaY > 0) {
                newZ += 25; // Scroll up = zoom in
            } else if (deltaY < 0) {
                newZ -= 25; // Scroll down = zoom out
            }
            
            // Clamp zoom to prevent passing through the model or getting too far
            if (newZ > -150) newZ = -150;
            if (newZ < -900) newZ = -900;
            
            cameraTranslate.setZ(newZ);
            
            // Chặn sự kiện cuộn chuột lan ra ngoài trang (tránh làm cuộn cả trang web/ứng dụng)
            event.consume();
        });
        
        // Setup smooth automatic rotation timeline to make it feel alive!
        javafx.animation.Timeline rotationTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(30),
                event -> {
                    if (!subScene.isHover()) { // Only rotate when user is not dragging
                        // Xoay tự động quanh trục Y của bàn xoay cực kỳ chậm và sang trọng (0.15 độ mỗi khung hình)
                        ry.setAngle(ry.getAngle() + 0.15);
                    }
                }
            )
        );
        rotationTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        rotationTimeline.play(); // Auto-rotation enabled using Y-axis as the turntable vertical rotation axis
        
        subScene.getProperties().put("rotationTimeline", rotationTimeline);
        subScene.getProperties().put("rx", rx);
        subScene.getProperties().put("ry", ry);
        subScene.getProperties().put("cameraTranslate", cameraTranslate);
        
        return subScene;
    }

    private static byte[] downloadBytes(String urlString) throws Exception {
        URL url = new URL(urlString);
        try (InputStream in = url.openStream()) {
            return in.readAllBytes();
        }
    }

    private static float[] readFloatArray(byte[] binBytes, JSONObject gltf, int accessorIndex) {
        JSONObject accessor = gltf.getJSONArray("accessors").getJSONObject(accessorIndex);
        int bufferViewIndex = accessor.getInt("bufferView");
        int count = accessor.getInt("count");
        int byteOffset = accessor.optInt("byteOffset", 0);
        
        JSONObject bufferView = gltf.getJSONArray("bufferViews").getJSONObject(bufferViewIndex);
        int bvByteOffset = bufferView.optInt("byteOffset", 0);
        int byteStride = bufferView.optInt("byteStride", 0);
        
        int totalOffset = bvByteOffset + byteOffset;
        int numFloats = count * 3;
        float[] data = new float[numFloats];
        
        ByteBuffer byteBuf = ByteBuffer.wrap(binBytes).order(ByteOrder.LITTLE_ENDIAN);
        int elementSize = 12; // 3 * 4 bytes for VEC3
        
        for (int i = 0; i < count; i++) {
            int elementOffset = totalOffset + i * (byteStride > 0 ? byteStride : elementSize);
            byteBuf.position(elementOffset);
            data[i * 3]     = byteBuf.getFloat();
            data[i * 3 + 1] = byteBuf.getFloat();
            data[i * 3 + 2] = byteBuf.getFloat();
        }
        return data;
    }

    private static int[] readIntArray(byte[] binBytes, JSONObject gltf, int accessorIndex) {
        JSONObject accessor = gltf.getJSONArray("accessors").getJSONObject(accessorIndex);
        int bufferViewIndex = accessor.getInt("bufferView");
        int count = accessor.getInt("count");
        int byteOffset = accessor.optInt("byteOffset", 0);
        int componentType = accessor.getInt("componentType");
        
        JSONObject bufferView = gltf.getJSONArray("bufferViews").getJSONObject(bufferViewIndex);
        int bvByteOffset = bufferView.optInt("byteOffset", 0);
        
        int totalOffset = bvByteOffset + byteOffset;
        int[] data = new int[count];
        
        ByteBuffer byteBuf = ByteBuffer.wrap(binBytes).order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.position(totalOffset);
        
        if (componentType == 5123) { // UNSIGNED_SHORT (2 bytes)
            for (int i = 0; i < count; i++) {
                data[i] = byteBuf.getShort() & 0xFFFF;
            }
        } else if (componentType == 5125) { // UNSIGNED_INT (4 bytes)
            for (int i = 0; i < count; i++) {
                data[i] = byteBuf.getInt();
            }
        } else if (componentType == 5121) { // UNSIGNED_BYTE (1 byte)
            for (int i = 0; i < count; i++) {
                data[i] = byteBuf.get() & 0xFF;
            }
        }
        return data;
    }

    private static float[] readTexCoords(byte[] binBytes, JSONObject gltf, int accessorIndex) {
        JSONObject accessor = gltf.getJSONArray("accessors").getJSONObject(accessorIndex);
        int bufferViewIndex = accessor.getInt("bufferView");
        int count = accessor.getInt("count");
        int byteOffset = accessor.optInt("byteOffset", 0);
        
        JSONObject bufferView = gltf.getJSONArray("bufferViews").getJSONObject(bufferViewIndex);
        int bvByteOffset = bufferView.optInt("byteOffset", 0);
        int byteStride = bufferView.optInt("byteStride", 0);
        
        int totalOffset = bvByteOffset + byteOffset;
        int numFloats = count * 2;
        float[] data = new float[numFloats];
        
        ByteBuffer byteBuf = ByteBuffer.wrap(binBytes).order(ByteOrder.LITTLE_ENDIAN);
        int elementSize = 8; // 2 * 4 bytes for VEC2
        
        for (int i = 0; i < count; i++) {
            int elementOffset = totalOffset + i * (byteStride > 0 ? byteStride : elementSize);
            byteBuf.position(elementOffset);
            data[i * 2]     = byteBuf.getFloat();
            // Flip vertical coordinate to match JavaFX texture coordinate origin (top-left)
            data[i * 2 + 1] = 1.0f - byteBuf.getFloat();
        }
        return data;
    }

    private static Image readTextureImage(byte[] binBytes, JSONObject gltf, int imageIndex, String glbUrl) {
        try {
            org.json.JSONArray images = gltf.optJSONArray("images");
            if (images == null || imageIndex < 0 || imageIndex >= images.length()) {
                return null;
            }
            JSONObject imageObj = images.getJSONObject(imageIndex);
            if (imageObj.has("bufferView")) {
                int bvIdx = imageObj.getInt("bufferView");
                org.json.JSONArray bvs = gltf.optJSONArray("bufferViews");
                if (bvs != null && bvIdx >= 0 && bvIdx < bvs.length()) {
                    JSONObject bv = bvs.getJSONObject(bvIdx);
                    int byteOffset = bv.optInt("byteOffset", 0);
                    int byteLength = bv.getInt("byteLength");
                    if (byteOffset + byteLength <= binBytes.length) {
                        byte[] imgBytes = new byte[byteLength];
                        System.arraycopy(binBytes, byteOffset, imgBytes, 0, byteLength);
                        return new Image(new ByteArrayInputStream(imgBytes));
                    }
                }
            } else if (imageObj.has("uri")) {
                String uri = imageObj.getString("uri");
                if (uri.startsWith("data:")) {
                    int commaIdx = uri.indexOf(",");
                    if (commaIdx >= 0) {
                        String base64Data = uri.substring(commaIdx + 1);
                        byte[] imgBytes = Base64.getDecoder().decode(base64Data.trim());
                        return new Image(new ByteArrayInputStream(imgBytes));
                    }
                } else {
                    // Try downloading it relative to the glbUrl
                    if (glbUrl != null && !glbUrl.isBlank()) {
                        int lastSlash = glbUrl.lastIndexOf('/');
                        if (lastSlash >= 0) {
                            String baseUrl = glbUrl.substring(0, lastSlash + 1);
                            String absoluteUri = baseUrl + uri;
                            byte[] imgBytes = downloadBytes(absoluteUri);
                            return new Image(new ByteArrayInputStream(imgBytes));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load texture image at index {}: {}", imageIndex, e.getMessage());
        }
        return null;
    }
}
