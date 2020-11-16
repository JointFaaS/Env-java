package jointfaas.rpc;

import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jointfaas.worker.RegisterRequest;
import jointfaas.worker.RegisterResponse;
import jointfaas.worker.RegisterResponse.Code;
import jointfaas.worker.WorkerGrpc;

public class WorkClient {

  private static final Logger logger = Logger.getLogger(WorkClient.class.getName());

  private final WorkerGrpc.WorkerBlockingStub blockingStub;

  /**
   * Construct client for accessing HelloWorld server using the existing channel.
   */
  public WorkClient(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
    blockingStub = WorkerGrpc.newBlockingStub(channel);
  }


  public void Register(String id, String addr, String runtime, String funcName, Long memory,
      Long disk) {
    // read from /etc/hosts
    RegisterRequest request = RegisterRequest.newBuilder()
        .setId(id)
        .setAddr(addr)
        .setRuntime(runtime)
        .setFuncName(funcName)
        .setMemory(memory)
        .setDisk(disk)
        .build();
    RegisterResponse response;
    try {
      response = blockingStub.register(request);
      logger.info("register code: " + response.getCode());
      logger.info("register msg: " + response.getMsg());
      if (response.getCode() == Code.ERROR) {
        System.exit(1);
      }
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      System.exit(1);
    }
  }

//  public static void main(String[] args) throws Exception {
//    String user = "world";
//    // Access a service running on the local machine on port 50051
//    String target = "localhost:50051";
//    // Allow passing in the user and target strings as command line arguments
//    if (args.length > 0) {
//      if ("--help".equals(args[0])) {
//        System.err.println("Usage: [name [target]]");
//        System.err.println("");
//        System.err.println("  name    The name you wish to be greeted by. Defaults to " + user);
//        System.err.println("  target  The server to connect to. Defaults to " + target);
//        System.exit(1);
//      }
//      user = args[0];
//    }
//    if (args.length > 1) {
//      target = args[1];
//    }
//  }
}
