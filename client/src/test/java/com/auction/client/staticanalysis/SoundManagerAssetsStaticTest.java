package com.auction.client.staticanalysis;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundManagerAssetsStaticTest {

    private static final Path SOUND_EVENT_SOURCE = Paths.get(
            "src/main/java/com/auction/client/model/audio/SoundEvent.java");
    private static final Path SOUND_MANAGER_SOURCE = Paths.get(
            "src/main/java/com/auction/client/service/SoundManager.java");
    private static final Path SOUNDS_ROOT = Paths.get("src/main/resources/sounds");

    @Test
    void everySoundEvent_shouldHaveMappedExistingWavAsset() throws Exception {
        List<String> enumEvents = readSoundEventConstants();
        Map<String, String> expectedAssets = expectedAssetsByEvent();

        assertEquals(enumEvents, new ArrayList<>(expectedAssets.keySet()),
                "Update SoundManagerAssetsStaticTest when SoundEvent changes.");

        String managerSource = Files.readString(SOUND_MANAGER_SOURCE);
        for (Map.Entry<String, String> entry : expectedAssets.entrySet()) {
            String event = entry.getKey();
            String asset = entry.getValue();

            assertTrue(managerSource.contains("case " + event + ":") || managerSource.contains("case " + event + " ->"),
                    "SoundManager must map SoundEvent." + event);
            assertTrue(managerSource.contains('"' + asset + '"'),
                    "SoundManager must reference asset " + asset + " for " + event);
            assertTrue(Files.isRegularFile(SOUNDS_ROOT.resolve(asset)),
                    "Missing sound asset: " + SOUNDS_ROOT.resolve(asset));
        }
    }

    @Test
    void soundAssetsFolder_shouldNotContainUnusedWavFiles() throws Exception {
        Map<String, String> expectedAssets = expectedAssetsByEvent();
        List<String> actualWavFiles;
        try (Stream<Path> paths = Files.list(SOUNDS_ROOT)) {
            actualWavFiles = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".wav"))
                    .sorted()
                    .collect(Collectors.toList());
        }

        List<String> expectedWavFiles = expectedAssets.values().stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        assertEquals(expectedWavFiles, actualWavFiles,
                "Keep sound resources and SoundManager mappings in sync.");
    }

    private static List<String> readSoundEventConstants() throws Exception {
        String source = Files.readString(SOUND_EVENT_SOURCE);
        Matcher matcher = Pattern.compile("enum\\s+SoundEvent\\s*\\{(?<body>.*?)\\}", Pattern.DOTALL)
                .matcher(source);
        assertTrue(matcher.find(), "Could not parse SoundEvent enum.");

        String body = matcher.group("body")
                .replaceAll("//.*", "")
                .replaceAll("/\\*.*?\\*/", "");

        List<String> constants = Stream.of(body.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(token -> token.split("\\s+", 2)[0])
                .filter(token -> token.matches("[A-Z0-9_]+"))
                .collect(Collectors.toList());

        assertFalse(constants.isEmpty(), "SoundEvent enum should define at least one sound event.");
        return constants;
    }

    private static Map<String, String> expectedAssetsByEvent() {
        Map<String, String> assets = new LinkedHashMap<>();
        assets.put("BID_SUCCESS", "success.wav");
        assets.put("BID_ERROR", "error.wav");
        assets.put("OUTBID", "outbid.wav");
        assets.put("AUCTION_ENDING_SOON", "ending_soon.wav");
        assets.put("AUCTION_WON", "win.wav");
        assets.put("AUCTION_LOST", "lost.wav");
        assets.put("NOTIFICATION", "notification.wav");
        return assets;
    }
}