import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class TransferClient extends WebSocketClient {

  private Path uploadFile;

  public TransferClient(URI serverAddress) {
    super(serverAddress);
  }

  @Override
  public void onOpen(ServerHandshake serverHandshake) {
    System.out.println("Connected to server.");
  }

  @Override
  public void onMessage(String msg) {
    System.out.println(msg);
    JSONObject requestJson = parseRequest(msg);
    if (requestJson == null) return;

    String type = (String) requestJson.get("type");
    if (type.equals("upload")) {
      short key = Short.parseShort((String) requestJson.get("key"));
      System.out.printf("transfer key: %d\nWaiting...\n", key);
    } else if (type.equals("download")) {
      byte[] fileBytes = ((String) requestJson.get("payload")).getBytes(StandardCharsets.ISO_8859_1);
      String fileName = (String) requestJson.get("fileName");
      Path downloadPath = Path.of(System.getProperty("user.home"), "Downloads", fileName);
      try {
        Files.write(downloadPath, fileBytes);
        System.out.printf("File saved to %s. Closing program.", downloadPath);
        System.exit(0);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else if (type.equals("transfer")) {
      try {
        byte[] fileBytes = Files.readAllBytes(uploadFile);
        requestJson.put("fileName", uploadFile.getFileName().toString());
        requestJson.put("payload", new String(fileBytes, StandardCharsets.ISO_8859_1));
        send(requestJson.toJSONString());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void onClose(int code, String reason, boolean isRemote) {
    System.out.println("Connection to server was closed.");
  }

  @Override
  public void onError(Exception e) {
    System.out.printf("Error encountered on socket.\n\tErr: %s\n", e.getMessage());
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

  public void upload(Path filePath) {
    this.uploadFile = filePath;
    JSONObject upload = new JSONObject();
    upload.put("type", "upload");
    send(upload.toJSONString());
  }

  public void download(short key) {
    JSONObject download = new JSONObject();
    download.put("type", "download");
    download.put("key", Short.toString(key));
    send(download.toJSONString());
  }
}
