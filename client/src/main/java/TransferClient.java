import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

public class TransferClient extends WebSocketClient {

    private Path uploadFile;
    private int chunkSize = 0;
    private FileOutputStream fileWriter;

    public TransferClient(URI serverAddress) {
        super(serverAddress);
    }

    @Override
    public void connect() {
        super.connect();

        while (!isOpen()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // no need to catch we are just waiting for connection to open so we dont try to send something before
                //  the connection is opened by accident
            }
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("Connected to server.");
    }

    @Override
    public void onMessage(String msg) {
        JSONObject requestJson = parseRequest(msg);
        if (requestJson == null) return;

        String type = (String) requestJson.get("type");

        if (type.equals("upload")) {
            onUpload(requestJson);
        }
        else if (type.equals("download")) {
            onDownload(requestJson);
        }
        else if (type.equals("transfer")) {

            onTransfer(requestJson);
        }
    }

    private void onUpload(JSONObject requestJson) {
        UUID key = UUID.fromString((String) requestJson.get("key"));
        System.out.printf("transfer key: %s\nWaiting...\n", key);
    }

    private void onDownload(JSONObject requestJson) {
        String status = (String) requestJson.get("status");

        if (status.equals("start")) {
            System.out.println("Download starting.");
            downloadStart((String) requestJson.get("fileName"));
        }
        else if (status.equals("in-progress")) {
            System.out.println("Chunk Received.");
            downloadInProgress(
                ((String) requestJson.get("payload"))
                    .getBytes(StandardCharsets.ISO_8859_1)
            );
        }
        else if (status.equals("finish")) {
            System.out.println("Download finishing.");
            downloadFinish((String) requestJson.get("fileName"));
        }
    }

    private void downloadStart(String fileName) {
        Path downloadFile = Path.of(System.getProperty("user.home")).resolve("Downloads")
            .resolve(fileName);
        try {
            downloadFile = Files.createFile(downloadFile);
            this.fileWriter = new FileOutputStream(downloadFile.toFile());
        } catch (IOException e) {
            System.out.println("Unable to create file: '" + downloadFile + "'. Closing the connection.");
            close();
        }
    }

    private void downloadInProgress(byte[] chunkBytes) {
        System.out.println("\tWriting chunk to file.");
        try {
            fileWriter.write(chunkBytes);
        } catch (IOException e) {
            System.out.println("unable to write chunk. (check conversion from bytes to char array).");
            close();
        }
    }

    private void downloadFinish(String fileName) {
        Path downloadPath = Path.of(System.getProperty("user.home"), "Downloads", fileName);
        System.out.printf("File saved to %s. Closing program.\n", downloadPath);
        try {
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        close();
    }

    private void onTransfer(JSONObject requestJson) {
        try {
            if (requestJson.containsKey("chunkSize")) {
                short chunkSize = (short) requestJson.get("chunkSize");
                sendChunked(chunkSize, uploadFile, requestJson);
            } else {
                sendChunked(2048, uploadFile, requestJson);
            }
        } catch (IOException e) {
            System.out.println("Unable to stream the file into chunks. Check file path and permissions.");
        }
    }

    private void sendChunked(int chunkSizeBytes, Path uploadFile, JSONObject requestJson) throws IOException {
        requestJson.put("status", "start");
        requestJson.put("fileName", uploadFile.getFileName().toString());
        send(requestJson.toJSONString());
        requestJson.replace("status", "in-progress");

        FileInputStream uls = new FileInputStream(uploadFile.toFile());
        byte[] chunkBytes = new byte[chunkSizeBytes];

        int bytesRead = 0;
        while ((bytesRead = uls.read(chunkBytes)) != -1) {
            JSONObject chunk = (JSONObject) requestJson.clone();
            if (bytesRead < chunkSizeBytes) {
                byte[] leftOverBytes = Arrays.copyOfRange(chunkBytes, 0, bytesRead);
                chunk.put("payload", new String(leftOverBytes, StandardCharsets.ISO_8859_1));
            } else {
                chunk.put("payload", new String(chunkBytes, StandardCharsets.ISO_8859_1));
            }
            send(chunk.toJSONString());
        }

        uls.close();

        requestJson.replace("status", "finish");
        send(requestJson.toJSONString());
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
            System.out.println("Failed to parse response");
        }

        return null;
    }

    public void upload(Path filePath) {
        this.uploadFile = filePath;

        JSONObject upload = new JSONObject();
        upload.put("type", "upload");

        send(upload.toJSONString());
    }

    public void download(String key) {
        System.out.println("Requesting Download.");

        JSONObject download = new JSONObject();
        download.put("type", "download");
        download.put("key", key);

        send(download.toJSONString());
    }

    public void setChunkSize(int cs) {
        this.chunkSize = cs;
    }
}
