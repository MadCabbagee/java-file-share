import org.apache.commons.cli.*;

public class ServerDriver {

  public static void main(String[] args) {
    Options cmdLineOptions = new Options();
    cmdLineOptions.addOption("p", "port", true, "Specify listening port.");
    // todo allow to specify chunksize for server.

    CommandLineParser optionParser = new DefaultParser();
    try {
      CommandLine cmd = optionParser.parse(cmdLineOptions, args);
      int port = Integer.parseInt(cmd.getOptionValue('p', "83"));

      TransferServer server = new TransferServer(port);
      server.start();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
}
