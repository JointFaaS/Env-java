package org.example;

import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.example.rpc.ContainerImpl;
import org.example.rpc.ContainerServer;
import org.example.rpc.WorkClient;
import org.example.rpc.container.ContainerGrpc;
import org.example.rpc.container.ContainerGrpc.ContainerBlockingStub;
import org.example.rpc.container.InvokeResponse;
import org.example.rpc.container.LoadCodeRequest;
import org.example.rpc.container.LoadCodeResponse;
import org.example.rpc.container.InvokeRequest;
import org.example.rpc.container.SetEnvsRequest;
import org.example.rpc.container.SetEnvsResponse;
import org.example.rpc.work.RegisterRequest;
import org.example.rpc.work.RegisterResponse;
import org.example.rpc.work.RegisterResponse.Code;
import org.example.rpc.work.RegisterResponseOrBuilder;
import org.example.rpc.work.WorkerGrpc;
import org.example.serverless.JarControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel;

  private ManagedChannel containerChannel;

  private ContainerBlockingStub blockingStub;

  private ContainerServer server;


  private final WorkerGrpc.WorkerImplBase serviceImpl =
      mock(WorkerGrpc.WorkerImplBase.class, delegatesTo(
          new WorkerGrpc.WorkerImplBase() {
            // By default the client will receive Status.UNIMPLEMENTED for all RPCs.
            // You might need to implement necessary behaviors for your test here, like this:
            //
            @Override
            public void register(RegisterRequest request,
                StreamObserver<RegisterResponse> responseObserver) {
              RegisterResponse response = RegisterResponse.newBuilder()
                  .setCode(Code.OK)
                  .setMsg("OK")
                  .build();
              responseObserver.onNext(response);
              responseObserver.onCompleted();
            }
          }));


  @Before
  public void setUp() throws Exception {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder
        .forName(serverName).directExecutor().addService(serviceImpl).build().start());

    // Create a client channel and register for automatic graceful shutdown.
    channel = grpcCleanup.register(
        InProcessChannelBuilder.forName(serverName).directExecutor().build());

    System.setProperty("RUN_TIME", "java8");
    System.setProperty("FUNC_NAME", "");
    System.setProperty("WORK_HOST", "");
    System.setProperty("HOST", "127.0.0.1");
    System.setProperty("JAR_PATH", "D:/jfl/Env-java/index.jar");
    // start server
    ExecutorService fixedThreadPool = Executors.newFixedThreadPool(1);
    server = new ContainerServer();
    fixedThreadPool.execute(new Runnable() {
      @Override
      public void run() {
        try {
          server.start(channel);
          server.blockUntilShutdown();
        } catch (InterruptedException | IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
    containerChannel = ManagedChannelBuilder
        .forTarget("127.0.0.1:55389")
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext()
        .build();
    // Create a HelloWorldClient using the in-process channel;
    blockingStub = ContainerGrpc.newBlockingStub(containerChannel);
  }

  @After
  public void finalize() throws InterruptedException {
    server.stop();
  }

  @Test
  public void InvokeTest() throws InterruptedException {
    LoadCodeRequest loadCodeRequest = LoadCodeRequest.newBuilder()
        .setFuncName("hello")
        .setUrl("http://106.15.225.249:8081/index.jar")
        .build();
    LoadCodeResponse loadCodeResponse = blockingStub.loadCode(loadCodeRequest);
    Assert.assertEquals(LoadCodeResponse.Code.OK, loadCodeResponse.getCode());
    InvokeRequest invokeRequest = InvokeRequest.newBuilder()
        .setFuncName("hello")
        .setPayload(ByteString.copyFrom(new byte[1]))
        .build();
    InvokeResponse invokeResponse = blockingStub.invoke(invokeRequest);
    Assert.assertEquals(InvokeResponse.Code.OK, invokeResponse.getCode());
    Assert.assertEquals("hello world", new String(invokeResponse.getOutput().toByteArray()));
  }

  @Test
  public void LoadErrorURL() throws InterruptedException {
    LoadCodeRequest loadCodeRequest = LoadCodeRequest.newBuilder()
        .setFuncName("hello")
        .setUrl("http://106.15.225.249:80/index.jar")
        .build();
    LoadCodeResponse loadCodeResponse = blockingStub.loadCode(loadCodeRequest);
    Assert.assertEquals(LoadCodeResponse.Code.ERR, loadCodeResponse.getCode());
  }


  @Test
  public void LoadTwiceTest() throws InterruptedException {
    LoadCodeRequest loadCodeRequest = LoadCodeRequest.newBuilder()
        .setFuncName("hello")
        .setUrl("http://106.15.225.249:8081/index.jar")
        .build();
    LoadCodeResponse loadCodeResponse = blockingStub.loadCode(loadCodeRequest);
    Assert.assertEquals(LoadCodeResponse.Code.OK, loadCodeResponse.getCode());
    loadCodeResponse = blockingStub.loadCode(loadCodeRequest);
    Assert.assertEquals(LoadCodeResponse.Code.ERR, loadCodeResponse.getCode());
  }

  @Test
  public void InvokeWithErrorName() throws InterruptedException {
    LoadCodeRequest loadCodeRequest = LoadCodeRequest.newBuilder()
        .setFuncName("olleh")
        .setUrl("http://106.15.225.249:8081/index.jar")
        .build();
    LoadCodeResponse loadCodeResponse = blockingStub.loadCode(loadCodeRequest);
    Assert.assertEquals(LoadCodeResponse.Code.OK, loadCodeResponse.getCode());
    InvokeRequest invokeRequest = InvokeRequest.newBuilder()
        .setFuncName("hello")
        .setPayload(ByteString.copyFrom(new byte[1]))
        .build();
    InvokeResponse invokeResponse = blockingStub.invoke(invokeRequest);
    Assert.assertEquals(InvokeResponse.Code.FUNC_MISMATCH, invokeResponse.getCode());
  }

  @Test
  public void TestSetEnvs() throws InterruptedException {
    SetEnvsRequest setEnvsRequest = SetEnvsRequest.newBuilder()
        .addEnv("testkey=testvalue")
        .build();
    SetEnvsResponse setEnvsResponse = blockingStub.setEnvs(setEnvsRequest);
    Assert.assertEquals(SetEnvsResponse.Code.OK, setEnvsResponse.getCode());
  }

  @Test
  public void TestErrorSetEnvs() throws InterruptedException {
    SetEnvsRequest setEnvsRequest = SetEnvsRequest.newBuilder()
        .addEnv("testkey:testvalue")
        .build();
    SetEnvsResponse setEnvsResponse = blockingStub.setEnvs(setEnvsRequest);
    Assert.assertEquals(SetEnvsResponse.Code.INVALID_ENV, setEnvsResponse.getCode());
  }

}
