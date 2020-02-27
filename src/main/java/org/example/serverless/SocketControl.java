package org.example.serverless;

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
  final private String JAR_PATH = "/tmp/code/source";
  final private String ENTRY_POINT = "jointfaas.Index";

  public SocketControl(String path, String funcName, String envID) throws IOException {
    JarControl jarControl = new JarControl(JAR_PATH, ENTRY_POINT);

    final File socketFile = new File(path);
    AFUNIXSocketAddress address = new AFUNIXSocketAddress(socketFile);
    // register
    JSONObject reg = new JSONObject();
    reg.put("funcName", funcName);
    reg.put("envID", envID);
    System.out.println(reg.toString());
    socket = AFUNIXSocket.newInstance();
    socket.connect(address);
    register(reg.toString());

  }

  public byte[] longToBytes(long x) {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.putLong(x);
    return buffer.array();
  }

  public long bytesToLong(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.put(bytes);
    buffer.flip(); // need flip
    return buffer.getLong();
  }

  private void register(String data) throws IOException {
    OutputStream outputStream = socket.getOutputStream();
    byte[] cb = longToBytes(0);

    byte[] db = data.getBytes(StandardCharsets.UTF_8);
    byte[] dbsize = longToBytes((long) db.length);
    outputStream.write(cb);
    outputStream.write(dbsize);
    outputStream.write(db);
    outputStream.flush();
  }

  private void sendRequest(Socket sock, Long callID, byte[] data) throws IOException {
    OutputStream outputStream = sock.getOutputStream();
    byte[] cb = longToBytes(callID);
    byte[] combine = new byte[cb.length + data.length];
    System.arraycopy(cb, 0, combine, 0, cb.length);
    System.arraycopy(data, 0, combine, cb.length, data.length);
    outputStream.write(combine);
    outputStream.flush();
  }

  private Request parseRequest(InputStream stream) throws IOException {
    byte[] b = new byte[16];
    if(stream.read(b) != 16) {
      throw new IOException("read error");
    }
    ByteBuffer byteBuffer = ByteBuffer.wrap(b);
    Long callID = byteBuffer.getLong();
    long size = byteBuffer.getLong();
    // let user handle with size and data
    Request request = new Request();
    request.setCallID(callID);
    if (size == 0L) {
      request.setData(new byte[0]);
    } else {
      byte[] d = new byte[(int)size];
      byteBuffer.get(d);
      request.setData(d);
    }
    return request;
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      System.out.println("Waiting for register");
      try {
        Request request = parseRequest(socket.getInputStream());
        InputStream input = new ByteArrayInputStream(request.getData());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // call the function
        jarControl.invoke(input, output);
        byte[] res = output.toByteArray();
        System.out.println(output.toString());

        long resize = (long) res.length;
        byte[] transfer = new byte[16 + res.length];
        System.arraycopy(longToBytes(resize), 0, transfer, 0, 8);
        System.arraycopy(res, 0, transfer, 8, res.length);

        sendRequest(socket, request.getCallID(), transfer);
      }
      catch (IOException e) {
        System.out.println(e.getMessage());
      }
      catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
        e.printStackTrace();
      }
    }
  }
}
