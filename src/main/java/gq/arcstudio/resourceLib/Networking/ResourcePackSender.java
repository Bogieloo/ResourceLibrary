package gq.arcstudio.resourceLib.Networking;

import org.bukkit.entity.Player;

public class ResourcePackSender {

    private final ResourcePackServer server;
    private final String host;
    private final int port;

    public ResourcePackSender(ResourcePackServer server, String host, int port) {
        this.server = server;
        this.host = host;
        this.port = port;
    }

    public void sendTo(Player player) {
        byte[] sha1 = server.getSha1();
        if (sha1 == null) {
            return;
        }
        player.setResourcePack(server.getPackUrl(host, port), sha1);
    }
}
