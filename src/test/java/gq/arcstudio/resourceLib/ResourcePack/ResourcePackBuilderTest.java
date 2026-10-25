package gq.arcstudio.resourceLib.ResourcePack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gq.arcstudio.resourceLib.Networking.ResourcePackServer;
import gq.arcstudio.resourceLib.ResourceEntry;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourcePackBuilderTest {

    @TempDir
    Path tempDir;

    private MockedStatic<Bukkit> mockedBukkit;
    private Server mockedServer;
    private Logger mockedLogger;

    @BeforeEach
    void setUp() {
        ResourcePackManager.getInstance().clear();
        mockedBukkit = mockStatic(Bukkit.class);
        mockedServer = mock(Server.class);
        mockedLogger = Logger.getLogger("ResourceLibTest");
        mockedBukkit.when(Bukkit::getServer).thenReturn(mockedServer);
        mockedBukkit.when(Bukkit::getLogger).thenReturn(mockedLogger);
        when(mockedServer.getVersion()).thenReturn("1.19.4");
    }

    @AfterEach
    void tearDown() {
        ResourcePackManager.getInstance().clear();
        mockedBukkit.close();
    }

    @Test
    void testJsonMerging() throws IOException {
        Path file1 = tempDir.resolve("file1.json");
        Files.writeString(file1, "{\"key1\": \"value1\", \"nested\": {\"a\": 1}}");

        Path file2 = tempDir.resolve("file2.json");
        Files.writeString(file2, "{\"key2\": \"value2\", \"nested\": {\"b\": 2}}");

        ResourceEntry entry1 = new ResourceEntry("assets/test.json", file1.toFile(), false);
        ResourceEntry entry2 = new ResourceEntry("assets/test.json", file2.toFile(), false);

        byte[] zipBytes = ResourcePackBuilder.buildZip(Arrays.asList(entry1, entry2));
        assertNotNull(zipBytes);

        JsonObject merged = getJsonFromZip(zipBytes, "assets/test.json");
        assertEquals("value1", merged.get("key1").getAsString());
        assertEquals("value2", merged.get("key2").getAsString());
        assertEquals(1, merged.getAsJsonObject("nested").get("a").getAsInt());
        assertEquals(2, merged.getAsJsonObject("nested").get("b").getAsInt());
    }

    @Test
    void testJsonConflictForceFalse() throws IOException {
        Path file1 = tempDir.resolve("file1.json");
        Files.writeString(file1, "{\"key1\": \"original\"}");

        Path file2 = tempDir.resolve("file2.json");
        Files.writeString(file2, "{\"key1\": \"override\"}");

        ResourceEntry entry1 = new ResourceEntry("assets/test.json", file1.toFile(), false);
        ResourceEntry entry2 = new ResourceEntry("assets/test.json", file2.toFile(), false);

        byte[] zipBytes = ResourcePackBuilder.buildZip(Arrays.asList(entry1, entry2));
        JsonObject merged = getJsonFromZip(zipBytes, "assets/test.json");
        assertEquals("original", merged.get("key1").getAsString());
    }

    @Test
    void testJsonConflictForceTrue() throws IOException {
        Path file1 = tempDir.resolve("file1.json");
        Files.writeString(file1, "{\"key1\": \"original\"}");

        Path file2 = tempDir.resolve("file2.json");
        Files.writeString(file2, "{\"key1\": \"override\"}");

        ResourceEntry entry1 = new ResourceEntry("assets/test.json", file1.toFile(), false);
        ResourceEntry entry2 = new ResourceEntry("assets/test.json", file2.toFile(), true);

        byte[] zipBytes = ResourcePackBuilder.buildZip(Arrays.asList(entry1, entry2));
        JsonObject merged = getJsonFromZip(zipBytes, "assets/test.json");
        assertEquals("override", merged.get("key1").getAsString());
    }

    @Test
    void testJsonArrayMerging() throws IOException {
        Path file1 = tempDir.resolve("file1.json");
        Files.writeString(file1, "{\"elements\": [1, 2]}");

        Path file2 = tempDir.resolve("file2.json");
        Files.writeString(file2, "{\"elements\": [3, 4]}");

        ResourceEntry entry1 = new ResourceEntry("assets/test.json", file1.toFile(), false);
        ResourceEntry entry2 = new ResourceEntry("assets/test.json", file2.toFile(), false);

        byte[] zipBytes = ResourcePackBuilder.buildZip(Arrays.asList(entry1, entry2));
        JsonObject merged = getJsonFromZip(zipBytes, "assets/test.json");
        assertEquals(4, merged.getAsJsonArray("elements").size());
        assertEquals(1, merged.getAsJsonArray("elements").get(0).getAsInt());
        assertEquals(4, merged.getAsJsonArray("elements").get(3).getAsInt());
    }

    @Test
    void testRawResourceConflictForceFalse() throws IOException {
        Path file1 = tempDir.resolve("texture.png");
        Files.write(file1, new byte[]{1, 2, 3});

        Path file2 = tempDir.resolve("texture_override.png");
        Files.write(file2, new byte[]{4, 5, 6});

        ResourceEntry entry1 = new ResourceEntry("assets/texture.png", file1.toFile(), false);
        ResourceEntry entry2 = new ResourceEntry("assets/texture.png", file2.toFile(), false);

        byte[] zipBytes = ResourcePackBuilder.buildZip(Arrays.asList(entry1, entry2));
        byte[] extracted = getBytesFromZip(zipBytes, "assets/texture.png");
        assertArrayEquals(new byte[]{1, 2, 3}, extracted);
    }

    @Test
    void testRawResourceConflictForceTrue() throws IOException {
        Path file1 = tempDir.resolve("texture.png");
        Files.write(file1, new byte[]{1, 2, 3});

        Path file2 = tempDir.resolve("texture_override.png");
        Files.write(file2, new byte[]{4, 5, 6});

        ResourceEntry entry1 = new ResourceEntry("assets/texture.png", file1.toFile(), false);
        ResourceEntry entry2 = new ResourceEntry("assets/texture.png", file2.toFile(), true);

        byte[] zipBytes = ResourcePackBuilder.buildZip(Arrays.asList(entry1, entry2));
        byte[] extracted = getBytesFromZip(zipBytes, "assets/texture.png");
        assertArrayEquals(new byte[]{4, 5, 6}, extracted);
    }

    @ParameterizedTest
    @CsvSource({
            "1.17, 7",
            "1.17.0, 7",
            "1.17.1, 7",
            "1.18, 8",
            "1.18.0, 8",
            "1.18.2, 8",
            "1.19, 9",
            "1.19.0, 9",
            "1.19.2, 9",
            "1.19.3, 12",
            "1.19.4, 13",
            "1.20, 15",
            "1.20.0, 15",
            "1.20.1, 15",
            "1.20.2, 18",
            "1.20.4, 22",
            "1.20.6, 32",
            "1.21, 34",
            "1.21.0, 34",
            "1.21.1, 34",
            "1.21.2, 42",
            "1.21.3, 42",
            "1.21.4, 46",
            "1.21.5, 55",
            "'git-Paper-416 (MC: 1.21.4)', 46",
            "'git-Paper-50 (MC: 1.21.5)', 55",
            "'git-Spigot-756bb01 (MC: 1.20.4)', 22",
            "'1.21.4-R0.1-SNAPSHOT', 46"
    })
    void testPackFormatMapping(String serverVersion, int expectedFormat) throws IOException {
        when(mockedServer.getVersion()).thenReturn(serverVersion);
        byte[] zipBytes = ResourcePackBuilder.buildZip(Collections.emptyList());
        JsonObject meta = getJsonFromZip(zipBytes, "pack.mcmeta");
        assertEquals(expectedFormat, meta.getAsJsonObject("pack").get("pack_format").getAsInt());
    }

    @Test
    void testPackUrlFormatting() {
        ResourcePackServer server = new ResourcePackServer();
        assertEquals("http://127.0.0.1:61234/pack.zip", server.getPackUrl("127.0.0.1", 61234));
        assertEquals("http://[::1]:61234/pack.zip", server.getPackUrl("::1", 61234));
        assertEquals("http://[2001:db8::1]:61234/pack.zip", server.getPackUrl("2001:db8::1", 61234));
        assertEquals("http://[::1]:61234/pack.zip", server.getPackUrl("[::1]", 61234));
    }

    @Test
    void testPathNormalizationInManager() throws IOException {
        ResourcePackManager manager = ResourcePackManager.getInstance();
        Path dummy = tempDir.resolve("dummy.txt");
        Files.writeString(dummy, "dummy content");
        manager.registerResource("\\assets\\custom\\test.txt", dummy.toFile(), false);
        ResourceEntry registered = manager.getRegisteredResources().get(manager.getRegisteredResources().size() - 1);
        assertEquals("assets/custom/test.txt", registered.virtualPath);
    }

    @Test
    void testSkipMissingFileGracefully() throws IOException {
        File nonexistent = tempDir.resolve("deleted.png").toFile();
        ResourceEntry entry = new ResourceEntry("assets/deleted.png", nonexistent, false);
        byte[] zipBytes = ResourcePackBuilder.buildZip(Collections.singletonList(entry));
        assertNotNull(zipBytes);
        assertThrows(IOException.class, () -> getBytesFromZip(zipBytes, "assets/deleted.png"));
    }

    @Test
    void testServerHeadAndGetRequest() throws IOException {
        ResourcePackServer server = new ResourcePackServer();
        int testPort = 59876;
        server.start(testPort);

        try {
            URL url = new URL("http://127.0.0.1:" + testPort + "/pack.zip");

            HttpURLConnection headConn = (HttpURLConnection) url.openConnection();
            headConn.setRequestMethod("HEAD");
            assertEquals(200, headConn.getResponseCode());
            assertEquals("application/zip", headConn.getHeaderField("Content-Type"));
            assertNotNull(headConn.getHeaderField("Content-Length"));
            headConn.disconnect();

            HttpURLConnection getConn = (HttpURLConnection) url.openConnection();
            getConn.setRequestMethod("GET");
            assertEquals(200, getConn.getResponseCode());
            byte[] body = getConn.getInputStream().readAllBytes();
            assertTrue(body.length > 0);
            getConn.disconnect();
        } finally {
            server.stop();
        }
    }

    private JsonObject getJsonFromZip(byte[] zipBytes, String path) throws IOException {
        return JsonParser.parseString(new String(getBytesFromZip(zipBytes, path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private byte[] getBytesFromZip(byte[] zipBytes, String path) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(path)) {
                    return zis.readAllBytes();
                }
            }
        }
        throw new IOException("File not found in zip: " + path);
    }
}
