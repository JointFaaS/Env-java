package org.example.serverless;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

public class SocketControl implements Runnable {
  private AFUNIXSocket socket;
  private FnControl fnControl;
  private AFUNIXSocketAddress endpoint;
  private final OutputStream output;
  private final InputStream input;
  static final private int RETRY_TIME = 10;
  static final private int HEADER_SIZE = 16;
  private Boolean useThreadPool;
  public SocketControl(String path, String funcName, String envID, Boolean useThreadPool)
      throws IOException {
    this.useThreadPool = useThreadPool;
    this.fnControl = null;

    final File socketFile = new File(path);
    try {
      endpoint = new AFUNIXSocketAddress(socketFile);
      // register
      socket = AFUNIXSocket.newInstance();
      socket.connect(endpoint);
    } catch (IOException e) {
      retry();
    } finally {
      if (socket == null) {
        System.out.println("socket error");
        System.exit(1);
      }
      output = socket.getOutputStream();
      input = socket.getInputStream();

      JSONObject reg = new JSONObject();
      reg.put("funcName", funcName);
      reg.put("envID", envID);
      System.out.println(reg.toString());
      register(reg.toString());
      if(socket == null || !socket.isConnected()) {
        System.out.println("can not connect to socket");
        System.exit(1);
      }
    }
  }

  void retry() {
    int times = 0;
    while (times < RETRY_TIME) {
      try {
        socket.connect(endpoint);
        if (socket.isConnected()) {
          return;
        }
      } catch (IOException e) {
        ++times;
        System.out.printf("%d time to retry\n", times);
      }
    }
    System.out.println("socket error");
    System.exit(1);
  }

  private void register(String data) throws IOException {
    Request register = new Request();
    register.setCallID(0L);
    register.setData(data.getBytes());
    register.sendTo(output);
  }

  private Request parseRequest(InputStream stream) throws IOException {
    byte[] b = new byte[HEADER_SIZE];
    if(stream.read(b) != HEADER_SIZE) {
      throw new IOException("read head error");
    }
    ByteBuffer byteBuffer = ByteBuffer.wrap(b);
    Long callID = byteBuffer.getLong();
    Long size = byteBuffer.getLong();
    // let user handle with size and data
    Request request = new Request();
    request.setCallID(callID);
    byte[] d = new byte[size.intValue()];
    request.setData(d);
    if (stream.read(d) != size.intValue()) {
      throw new IOException("read data error");
    }
    return request;
  }

  @Override
  public void run() {
    ExecutorService threadPool = null;
    if (useThreadPool) {
     threadPool = new ThreadPoolExecutor(2, 3, 200, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
    }

    assert fnControl != null;
    while (!Thread.interrupted()) {
      try {
        Request request = parseRequest(input);
        Worker worker = new Worker(fnControl, request, output);
        // call the function
        if (useThreadPool) {
          assert threadPool != null;
          threadPool.submit(worker);
        } else {
          worker.run();
        }
      }
      catch (IOException e) {
        retry();
      }
    }
  }

  public void setFnControl(FnControl fnControl) {
    this.fnControl = fnControl;
  }
}
