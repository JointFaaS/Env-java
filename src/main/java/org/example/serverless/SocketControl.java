package org.example.serverless;

import com.sun.org.apache.xpath.internal.operations.Bool;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

public class SocketControl implements Runnable {
  private AFUNIXSocket socket;
  private JarControl jarControl;
  private AFUNIXSocketAddress endpoint;
  static final private String JAR_PATH = "/tmp/code/source";
  static final private String ENTRY_POINT = "jointfaas.Index";
  static final private int RETRY_TIME = 10;
  static final private int HEADER_SIZE = 16;

  public SocketControl(String path, String funcName, String envID, Boolean useThreadPool) {
    jarControl = new JarControl(JAR_PATH, ENTRY_POINT);

    final File socketFile = new File(path);
    try {
      endpoint = new AFUNIXSocketAddress(socketFile);
      // register
      JSONObject reg = new JSONObject();
      reg.put("funcName", funcName);
      reg.put("envID", envID);
      System.out.println(reg.toString());
      socket = AFUNIXSocket.newInstance();
      socket.connect(endpoint);
      register(reg.toString());
    } catch (IOException e) {
      retry();
    } finally {
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
  }



  private byte[] longToBytes(long x) {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.putLong(x);
    return buffer.array();
  }

  private long bytesToLong(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.put(bytes);
    buffer.flip(); // need flip
    return buffer.getLong();
  }

  private void register(String data) throws IOException {
    sendRequest(0L, data.getBytes());
  }

  private void sendRequest(Long callID, byte[] data) throws IOException {
    // data does not have size info
    OutputStream outputStream = socket.getOutputStream();
    byte[] cb = longToBytes(callID);
    byte[] sizeb = longToBytes(data.length);
    byte[] combine = new byte[cb.length + sizeb.length + data.length];
    System.arraycopy(cb, 0, combine, 0, cb.length);
    System.arraycopy(sizeb, 0, combine, cb.length, sizeb.length);
    System.arraycopy(data, 0, combine, cb.length + sizeb.length, data.length);
    outputStream.write(combine);
    outputStream.flush();
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
    if (size == 0L) {
      request.setData(new byte[0]);
    } else {
      byte[] d = new byte[size.intValue()];
      if (stream.read(d) != size.intValue()) {
        throw new IOException("read data error");
      }
      request.setData(d);
    }
    return request;
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      try {
        Request request = parseRequest(socket.getInputStream());
        InputStream input = new ByteArrayInputStream(request.getData());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // call the function
        jarControl.invoke(input, output);
        byte[] res = output.toByteArray();
        System.out.println(output.toString());
        sendRequest(request.getCallID(), res);
      }
      catch (IOException e) {
        retry();
        if(socket == null || !socket.isConnected()) {
          System.out.println("can not connect to socket");
          System.exit(1);
        }
      }
      catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
        System.out.println(e.getMessage());
        System.exit(1);
      }
    }
  }
}
