package gq.arcstudio.resourceLib.Networking;

import com.sun.net.httpserver.*;
import gq.arcstudio.resourceLib.ResourcePack.ResourcePackBuilder;
import gq.arcstudio.resourceLib.ResourcePack.ResourcePackManager;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.MessageDigest;

public class ResourcePackServer {

    private HttpServer server;
    private byte[] zipBytes;
    private byte[] sha1;

    public void start(int port) throws IOException {
        buildPack();
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/pack.zip", exchange -> {
            try {
                try {
                    Bukkit.getLogger().info("[ResourceLib] Serving resource pack to " + exchange.getRemoteAddress());
                } catch (Throwable ignored) {}

                exchange.getResponseHeaders().add("Content-Type", "application/zip");
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().add("Content-Length", String.valueOf(zipBytes.length));
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                    return;
                }
                exchange.sendResponseHeaders(200, zipBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(zipBytes);
                }
            } catch (Exception e) {
                try {
                    exchange.close();
                } catch (Exception ignored) {}
            }
        });

        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public String getPackUrl(String host, int port) {
        String formattedHost = host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
        return "http://" + formattedHost + ":" + port + "/pack.zip";
    }

    public byte[] getSha1() {
        return sha1;
    }

    public void buildPack() {
        try {
            zipBytes = ResourcePackBuilder.buildZip(ResourcePackManager.getInstance().getRegisteredResources());
            sha1 = MessageDigest.getInstance("SHA-1").digest(zipBytes);
            try {
                Bukkit.getLogger().info("[ResourceLib] Resource pack built, SHA-1: " + bytesToHex(sha1));
            } catch (Throwable ignored) {}
        } catch (Exception e) {
            throw new RuntimeException("Failed to build resource pack", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
