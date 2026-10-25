package gq.arcstudio.resourceLib;

import gq.arcstudio.resourceLib.Networking.ResourcePackSender;
import gq.arcstudio.resourceLib.Networking.ResourcePackServer;
import gq.arcstudio.resourceLib.ResourcePack.ResourcePackManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;

public final class ResourceLib extends JavaPlugin {

    private ResourcePackServer packServer;
    private String localHost;
    private int port = 61234;

    @Override
    public void onEnable() {
        packServer = new ResourcePackServer();

        localHost = Bukkit.getIp();
        if (localHost == null || localHost.isEmpty() || localHost.equals("0.0.0.0")) {
            try {
                localHost = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                localHost = "127.0.0.1";
            }
        }

        testOrIgnore();

        Bukkit.getScheduler().runTask(this, () -> {
            try {
                packServer.start(port);
                ResourcePackManager.getInstance().lock();
                getLogger().info("ResourcePack server started and Manager locked.");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    sendPack(player);
                }
            } catch (Exception e) {
                getLogger().severe("Failed to start resource pack server: " + e.getMessage());
                getServer().getPluginManager().disablePlugin(this);
            }
        });

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent e) {
                sendPack(e.getPlayer());
            }
        }, this);
    }

    private void sendPack(Player player) {
        if (packServer == null || packServer.getSha1() == null) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    sendPack(player);
                }
            }, 5L);
            return;
        }

        String address = "";
        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            address = player.getAddress().getAddress().getHostAddress();
        }

        String targetHost = (address.equals("127.0.0.1") || address.equals("::1") || address.equals("0:0:0:0:0:0:0:1") || address.equals(localHost))
                ? "127.0.0.1" : localHost;

        new ResourcePackSender(packServer, targetHost, port).sendTo(player);
    }

    @Override
    public void onDisable() {
        if (packServer != null) packServer.stop();
    }

    private void testOrIgnore() {
        for (String className : new String[]{"gq.arcstudio.resourceLib.test.testimplementation", "gq.arcstudio.resourceLib.test.testimplemetation"}) {
            try {
                Class<?> clazz = Class.forName(className);
                java.lang.reflect.Method method = clazz.getMethod("runTest");
                method.invoke(null);
                break;
            } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {}
        }
    }
}
