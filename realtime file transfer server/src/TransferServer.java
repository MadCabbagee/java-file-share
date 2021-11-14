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

public class TransferServer extends WebSocketServer {

  private final Map<Short, WebSocket> uploaders;
  private final Map<Short, WebSocket> downloaders;

  public TransferServer(int port) {
    super(new InetSocketAddress(port));
    this.uploaders = new HashMap<>();
    this.downloaders = new HashMap<>();
  }

  public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
    System.out.printf("New connection from: %s\n", webSocket.getRemoteSocketAddress());
  }

  public void onClose(WebSocket webSocket, int code, String reason, boolean b) {
    System.out.printf("Connection closed: %s\n\tReason: %s\n\tCode: %s\n", webSocket.getRemoteSocketAddress(), reason, code);
  }

  public void onMessage(WebSocket sender, String msg) {
    System.out.printf("New message from %s\n\tMessage: %s\n", sender.getRemoteSocketAddress(), msg);
    JSONObject requestJson = parseRequest(msg);
    if (requestJson == null) return;

    String type = (String) requestJson.get("type");
    if (type.equals("upload")) {
      short key = genKey();
      uploaders.put(key, sender);

      requestJson.put("key", Short.toString(key));

      sender.setAttachment(key);
      sender.send(requestJson.toJSONString());
    } else if (type.equals("download")) {
      short uploaderKey = Short.parseShort((String) requestJson.get("key"));
      short downloaderID = genKey();
      downloaders.put(downloaderID, sender);

      JSONObject fileRequest = new JSONObject();
      fileRequest.put("type", "transfer");
      fileRequest.put("key", Short.toString(uploaderKey));
      fileRequest.put("id", Short.toString(downloaderID));

      WebSocket uploader = uploaders.get(uploaderKey);
      uploader.send(fileRequest.toJSONString());
    } else if (type.equals("transfer")) {
      short key = Short.parseShort((String) requestJson.get("key"));
      short id = Short.parseShort((String) requestJson.get("id"));
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

  private short genKey() {
    Random keyGen = new Random(System.currentTimeMillis());
    return (short) keyGen.nextInt(Short.MAX_VALUE);
  }

  public void onError(WebSocket webSocket, Exception e) {
    System.out.printf("Error: %s\n\tRemote Address: %s\n", e.getMessage(), webSocket.getRemoteSocketAddress());
  }

  public void onStart() {
    System.out.println("Server started.");
  }
}
