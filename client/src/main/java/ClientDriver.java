import org.apache.commons.cli.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class ClientDriver {

  public static void main(String[] args) {
    Options cmdLineOptions = createOptions();

    parseArgs(cmdLineOptions, args);
  }

  private static void parseArgs(Options cmdLineOptions, String[] args) {
    if (args.length == 0) {
      printUsageMessage(cmdLineOptions);
      return;
    }

    CommandLineParser argParser = new DefaultParser();
    CommandLine cmd;
    int port;
    URI serverURI;
    TransferClient client = null;

    try {
      cmd = argParser.parse(cmdLineOptions, args);
      port = Integer.parseInt(cmd.getOptionValue('p', "83"));
      serverURI = new URI("ws://" + cmd.getOptionValue('s') + ":" + port);

      System.out.println("Connecting to: '" + serverURI + '\'');
      client = connectToServer(serverURI);

      if (cmd.hasOption("cs")) {
        parseChunkSize(cmd, client, cmdLineOptions);
      }

      if (cmd.hasOption("ul")) {
        startUpload(
            client,
            Path.of(
                cmd.getOptionValue("ul")
            )
        );
      }
      else if (cmd.hasOption("dl")) {
        startDownload(
            client,
            cmd.getOptionValue("dl")
        );
      }
    }
    catch (ParseException e) {
      System.out.println("Invalid arguments.");
      printUsageMessage(cmdLineOptions);
      client.close();
    }
    catch (URISyntaxException e) {
      System.out.println("Invalid server address.");
      printUsageMessage(cmdLineOptions);
      client.close();
    }
  }

  private static void parseChunkSize(CommandLine cmd, TransferClient client, Options cmdLineOptions) {
    short chunkSize = Short.parseShort(cmd.getOptionValue("cs"));

    if (chunkSize >= 1024 && chunkSize <= Short.MAX_VALUE) {
      client.setChunkSize(chunkSize);
    }
    else if (cmd.hasOption("ul")) {
      System.out.println("Invalid chunk size: '" + chunkSize +
          "'. Falling back to default or uploader specified chunk size.");
    }
    else if (cmd.hasOption("dl")) {
      System.out.println("Invalid chunk size: '" + chunkSize +
          "'. Falling back to default or uploader specified chunk size.");
    }
    else {
      System.out.println("Chunk size specified without transfer type.");
    }
  }

  private static void startDownload(TransferClient client, String key) {
    client.download(key);
  }

  private static void startUpload(TransferClient client, Path filePath) {
    client.upload(filePath);
  }

  private static TransferClient connectToServer(URI serverURI) {
    TransferClient client = new TransferClient(serverURI);
    client.connect();

    return client;
  }

  private static void printUsageMessage(Options cmdLineOptions) {
    HelpFormatter optionFormatter = new HelpFormatter();
    String header = "Send files using a server as a medium to transfer files live without saving files to a server or directly connecting to another client.\n";
    String footer = "Upload EG: java -jar rtft.jar -ul \"./test.txt\" -s 127.0.0.1 -p 83\n" +
        "Download eg: java -jar rtft.jar -dl 27845 -s 127.0.0.1 -p 83\n";

    optionFormatter.printHelp("rtft", header, cmdLineOptions, footer);
  }

  private static Options createOptions() {
    Options cliOptions = new Options();

    cliOptions.addOption("ul", "upload", true,
        "Specify client as uploader. Takes file path as argument."
    );

    cliOptions.addOption("dl", "download", true,
        "Specify client as downloader. Takes transfer key as argument."
    );

    cliOptions.addOption("s", "server", true,
        "Specify the transfer server address that client should connect to for this transfer."
    );

    cliOptions.addOption("p", "port", true,
        "Specify server port. Default is 83 if not specified"
    );

    cliOptions.addOption("cs", "chunk-size", true,
        "Files are sent in chunks of bytes. Specify the size of each chunk in bytes. Valid values are 1024-" + Short.MAX_VALUE
    );

    return cliOptions;
  }
}
