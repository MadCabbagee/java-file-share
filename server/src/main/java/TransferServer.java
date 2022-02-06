import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransferServer extends WebSocketServer {

  // uploaderKey uploaderWebsocket
  private final Map<UUID, WebSocket> uploaders;
  // downloaderId downloaderWebsocket
  private final Map<UUID, WebSocket> downloaders;

  public TransferServer(int port) {
    super(new InetSocketAddress(port));

    // if you use a normal hash map you will mess up the internal tree.
    // use if hashmaps contain isolated elements.s
    this.uploaders = new ConcurrentHashMap<>();
    this.downloaders = new ConcurrentHashMap<>();
  }

  public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
    System.out.printf("New connection from: %s\n", webSocket.getRemoteSocketAddress());
  }

  public void onClose(WebSocket webSocket, int code, String reason, boolean b) {
    System.out.printf("Connection closed: %s\n\tReason: %s\n\tCode: %s\n", webSocket.getRemoteSocketAddress(), reason, code);
    uploaders.remove(webSocket);
    downloaders.remove(webSocket);
  }

  public void onMessage(WebSocket sender, String msg) {
    JSONObject requestJson = parseRequest(msg);

    if (requestJson == null) return;

    String type = (String) requestJson.get("type");

    if (type.equals("upload")) {
      UUID key = genKey();
      uploaders.put(key, sender);

      requestJson.put("key", key.toString());

      sender.setAttachment(key);
      sender.send(requestJson.toJSONString());
    }
    else if (type.equals("download")) {
      UUID uploaderKey = UUID.fromString((String) requestJson.get("key"));
      UUID downloaderID = genKey();
      downloaders.put(downloaderID, sender);

      JSONObject fileRequest = new JSONObject();
      fileRequest.put("type", "transfer");

      fileRequest.put("key", uploaderKey.toString());
      fileRequest.put("id", downloaderID.toString());

      WebSocket uploader = uploaders.get(uploaderKey);
      uploader.send(fileRequest.toJSONString());
    }
    else if (type.equals("transfer")) {
      UUID key = UUID.fromString((String) requestJson.get("key"));
      UUID id = UUID.fromString((String) requestJson.get("id"));

      WebSocket uploader = uploaders.get(key);

      if (uploader != null) {
        if (uploader.equals(sender) && sender.getAttachment().equals(key)) {
          requestJson.replace("type", "download");
          downloaders.get(id).send(requestJson.toJSONString());
        }
      }
    }
  }

  private JSONObject parseRequest(String msg) {
    JSONParser parser = new JSONParser();

    try {
      return (JSONObject) parser.parse(msg);
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return null;
  }

  private UUID genKey() {
    return UUID.randomUUID();
  }

  public void onError(WebSocket webSocket, Exception e) {
    System.out.printf("Error: %s\n\tRemote Address: %s\n", e.getMessage(), webSocket.getRemoteSocketAddress());
  }

  public void onStart() {
    System.out.println("Server started.");
  }
}
