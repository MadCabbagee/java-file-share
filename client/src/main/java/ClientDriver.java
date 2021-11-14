import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class ClientDriver {

  public static void main(String[] args) {
    Options cmdLineOptions = new Options();
    cmdLineOptions.addOption("ul", "upload", true, "Specify client as uploader. Takes file path as argument.");
    cmdLineOptions.addOption("dl", "download", true, "Specify client as downloader. Takes transfer key as argument.");
    cmdLineOptions.addOption("s", "server", true, "Specify the transfer server address that client should connect to for this transfer.");
    cmdLineOptions.addOption("p", "port", true, "Specify server port. Default is 83 if not specified");

    if (args.length == 0) {
      HelpFormatter optionFormatter = new HelpFormatter();
      String header = "Send files using a server as a medium to transfer files live without saving files to a server or directly connecting to another client.\n";
      String footer = "Upload EG: java -jar rtft.jar -ul \"./test.txt\" -s 127.0.0.1 -p 83\n" +
          "Download eg: java -jar rtft.jar -dl 27845 -s 127.0.0.1 -p 83\n";
      optionFormatter.printHelp("rtft", header, cmdLineOptions, footer);
    } else {
      CommandLineParser argParser = new DefaultParser();
      CommandLine cmd;
      try {
        cmd = argParser.parse(cmdLineOptions, args);
        int port = Integer.parseInt(cmd.getOptionValue('p', "83"));
        URI serverURI = new URI("ws://" + cmd.getOptionValue('s') + ":" + port);
        System.out.println(serverURI);
        if (cmd.hasOption("ul")) {
          Path filePath = Path.of(cmd.getOptionValue("ul"));
          TransferClient client = new TransferClient(serverURI);
          client.connect();
          Thread.sleep(5000);
          client.upload(filePath);
        } else if (cmd.hasOption("dl")) {
          short key = Short.parseShort(cmd.getOptionValue("dl"));
          TransferClient client = new TransferClient(serverURI);
          client.connect();
          Thread.sleep(5000);
          client.download(key);
        }
      } catch (ParseException | URISyntaxException | InterruptedException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }
}
