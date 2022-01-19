package org.kryonite.kryoproxysync;

import com.google.inject.Inject;
import com.rabbitmq.client.Address;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kryonite.kryomessaging.api.MessagingService;
import org.kryonite.kryomessaging.service.DefaultActiveMqConnectionFactory;
import org.kryonite.kryomessaging.service.DefaultMessagingService;
import org.kryonite.kryoproxysync.listener.ProxyPingListener;
import org.kryonite.kryoproxysync.messaging.MessagingController;
import org.kryonite.kryoproxysync.playercount.PlayerCountManager;

@Slf4j
@AllArgsConstructor
@Plugin(id = "kryo-proxy-sync", name = "Kryo Proxy Sync", authors = "Kryonite Labs", version = "0.1.0")
public class KryoProxySyncPlugin {

  private final ProxyServer server;
  private MessagingService messagingService;

  @Inject
  public KryoProxySyncPlugin(ProxyServer server) {
    this.server = server;
  }

  @Subscribe
  public void onInitialize(ProxyInitializeEvent event) {
    PlayerCountManager playerCountManager = new PlayerCountManager();

    try {
      setupMessagingController(playerCountManager);
    } catch (IOException | TimeoutException exception) {
      log.error("Failed to setup MessagingService", exception);
      return;
    }

    server.getEventManager().register(this, new ProxyPingListener(playerCountManager));
  }

  private void setupMessagingController(PlayerCountManager playerCountManager)
      throws IOException, TimeoutException {
    MessagingController messagingController;
    if (messagingService == null) {
      messagingService = new DefaultMessagingService(new DefaultActiveMqConnectionFactory(
          List.of(Address.parseAddress(getEnv("RABBITMQ_ADDRESS"))),
          getEnv("RABBITMQ_USERNAME"),
          getEnv("RABBITMQ_PASSWORD")
      ));
    }

    messagingController = new MessagingController(messagingService, playerCountManager, server, getEnv("SERVER_NAME"));
    messagingController.setupPlayerCountChanged();
  }

  private String getEnv(String name) {
    String connectionString = System.getenv(name);
    if (connectionString == null) {
      connectionString = System.getProperty(name);
    }

    return connectionString;
  }
}
