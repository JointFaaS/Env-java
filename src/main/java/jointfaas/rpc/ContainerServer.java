package jointfaas.rpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ContainerServer {

  private static final Logger logger = Logger.getLogger(ContainerServer.class.getName());

  private Server server;

  final private Integer port = 55389;

  private String readHost() {
    FileReader fr = null;
    try {
      fr = new FileReader("/etc/hosts");
    } catch (FileNotFoundException e) {
      logger.log(Level.WARNING, e.getMessage());
      // read from
      return System.getProperty("HOST") + ":" + port.toString();
    }

    BufferedReader bf = new BufferedReader(fr);
    String str;
    String last = "";
    // the last line can get ip in the source
    while (true) {
      try {
        if ((str = bf.readLine()) == null) {
          break;
        }
      } catch (IOException e) {
        logger.log(Level.WARNING, e.getMessage());
        return "";
      }
      last = str;
    }

    try {
      bf.close();
      fr.close();
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage());
    }

    return last.split("\\s")[0] + ":" + port.toString();
  }

  private String readId() {
    FileReader fr = null;
    try {
      fr = new FileReader("/etc/hosts");
    } catch (FileNotFoundException e) {
      logger.log(Level.WARNING, e.getMessage());
      // read from
      return System.getProperty("HOST") + ":" + port.toString();
    }

    BufferedReader bf = new BufferedReader(fr);
    String str;
    String last = "";
    // the last line can get ip in the source
    while (true) {
      try {
        if ((str = bf.readLine()) == null) {
          break;
        }
      } catch (IOException e) {
        logger.log(Level.WARNING, e.getMessage());
        return "";
      }
      last = str;
    }

    try {
      bf.close();
      fr.close();
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage());
    }

    return last.split("\\s")[1];
  }


  public void start(ManagedChannel channel) throws IOException, InterruptedException {

    /* The port on which the server should run */
    server = ServerBuilder.forPort(port)
        .addService(new ContainerImpl())
        .build()
        .start();
    logger.info("Server started, listening on " + port);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          ContainerServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });

    // Create a communication channel to the server, known as a Channel. Channels are thread-safe
    // and reusable. It is common to create channels at the beginning of your application and reuse
    // them until the application shuts down.
    try {
      WorkClient client = new WorkClient(channel);
      String host = readHost();
      if (host.equals("")) {
        return;
      }
      String id = readId();
      System.out.println(host);
      System.out.println(id);
      String runTime = System.getProperty("RUNTIME");
      String funcName = System.getProperty("FUNC_NAME");
      Long memory = Long.parseLong(System.getProperty("MEMORY"));
      Long disk = 0L;
      client.Register(id, host, runTime, funcName, memory, disk);

    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  public void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }
}
