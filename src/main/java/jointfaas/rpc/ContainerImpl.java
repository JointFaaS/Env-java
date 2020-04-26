package jointfaas.rpc;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import jointfaas.container.ContainerGrpc.ContainerImplBase;
import jointfaas.container.InvokeRequest;
import jointfaas.container.InvokeResponse;
import jointfaas.container.LoadCodeRequest;
import jointfaas.container.LoadCodeResponse;
import jointfaas.container.LoadCodeResponse.Code;
import jointfaas.container.SetEnvsRequest;
import jointfaas.container.SetEnvsResponse;
import jointfaas.serverless.JarControl;

public class ContainerImpl extends ContainerImplBase {
  private static final Logger logger = Logger.getLogger(ContainerImpl.class.getName());

  private ReentrantLock loadLock = new ReentrantLock();

  private String jarPath = System.getProperty("JAR_PATH");

  volatile private JarControl jarControl = null;
  final private String entry = "jointfaas.Index";

  @Override
  public void invoke(InvokeRequest req, StreamObserver<InvokeResponse> responseObserver)
  {
    // invoke func
    if (jarControl == null) {
      InvokeResponse reply = InvokeResponse.newBuilder()
          .setCode(InvokeResponse.Code.NOT_READY)
          .setOutput(ByteString.copyFromUtf8(""))
          .build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
      return;
    }

    if (!req.getFuncName().equals(System.getProperty("FUNC_NAME"))) {
      InvokeResponse reply = InvokeResponse.newBuilder()
          .setCode(InvokeResponse.Code.FUNC_MISMATCH)
          .setOutput(ByteString.copyFromUtf8(""))
          .build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
      return;
    }

    ByteArrayOutputStream res = new ByteArrayOutputStream();
    try {
      jarControl.invoke(new ByteArrayInputStream(req.getPayload().toByteArray()), res);
    } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
      InvokeResponse reply = InvokeResponse.newBuilder()
          .setCode(InvokeResponse.Code.RUNTIME_ERROR)
          .setOutput(ByteString.copyFrom(e.getMessage().getBytes()))
          .build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
      return;
    }
    InvokeResponse reply = InvokeResponse.newBuilder()
        .setCode(InvokeResponse.Code.OK)
        .setOutput(ByteString.copyFrom(res.toByteArray()))
        .build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }

  @Override
  public void setEnvs(SetEnvsRequest req, StreamObserver<SetEnvsResponse> responseStreamObserver) {
    int size = req.getEnvList().size();
    for (int i = 0; i < size; ++i) {
      String envLine = req.getEnvList().get(i);
      String[] pair = envLine.split("=");
      if (pair.length != 2) {
        SetEnvsResponse response = SetEnvsResponse.newBuilder()
            .setCode(SetEnvsResponse.Code.INVALID_ENV)
            .build();
        responseStreamObserver.onNext(response);
        responseStreamObserver.onCompleted();
        return;
      }
      logger.info(envLine);
      System.setProperty(pair[0], pair[1]);
    }
    SetEnvsResponse response = SetEnvsResponse.newBuilder()
        .setCode(SetEnvsResponse.Code.OK)
        .build();
    responseStreamObserver.onNext(response);
    responseStreamObserver.onCompleted();
  }

  @Override
  public void loadCode(LoadCodeRequest req, StreamObserver<LoadCodeResponse> responseStreamObserver) {
    String funcName = req.getFuncName();
    loadLock.lock();
    if (!System.getProperty("FUNC_NAME").equals(funcName)) {
      logger.log(Level.WARNING, "FUNC_NAME is not equal to loadCode funcName, which means a reload");
    }
    System.setProperty("FUNC_NAME", funcName);
    int byteSum = 0;
    int byteRead = 0;
    URL url = null;
    try {
      url = new URL(req.getUrl());
    } catch (MalformedURLException e) {
      logger.log(Level.WARNING, e.getMessage());
      LoadCodeResponse response = LoadCodeResponse.newBuilder()
          .setCode(LoadCodeResponse.Code.ERROR)
          .build();
      responseStreamObserver.onNext(response);
      responseStreamObserver.onCompleted();
      loadLock.unlock();
      return;
    }
    try {
      URLConnection conn = url.openConnection();
      InputStream inStream = conn.getInputStream();
      FileOutputStream fs = new FileOutputStream(jarPath);

      byte[] buffer = new byte[1024];
      int length;
      while ((byteRead = inStream.read(buffer)) != -1) {
        byteSum += byteRead;
        fs.write(buffer, 0, byteRead);
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage());
      LoadCodeResponse response = LoadCodeResponse.newBuilder()
          .setCode(LoadCodeResponse.Code.ERROR).build();
      responseStreamObserver.onNext(response);
      responseStreamObserver.onCompleted();
      File file = new File(jarPath);
      if (file.isFile()) {
        file.delete();
      }
      loadLock.unlock();
      return;
    }

    // file is download, load into application
    jarControl = new JarControl(jarPath, "jointfaas.Index");
    if (!jarControl.isReady()) {
      logger.log(Level.WARNING, "jarControl init error");
      LoadCodeResponse response = LoadCodeResponse.newBuilder()
          .setCode(LoadCodeResponse.Code.ERROR).build();
      responseStreamObserver.onNext(response);
      responseStreamObserver.onCompleted();
      loadLock.unlock();
      return;
    }

    // response
    LoadCodeResponse response = LoadCodeResponse.newBuilder().setCode(LoadCodeResponse.Code.OK).build();
    responseStreamObserver.onNext(response);
    responseStreamObserver.onCompleted();
    loadLock.unlock();
  }
}
