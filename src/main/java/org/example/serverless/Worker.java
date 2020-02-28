package org.example.serverless;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

public class Worker implements Runnable {

  private JarControl jarControl;
  private final OutputStream output;
  private ByteArrayInputStream input;
  private Long callID;

  public Worker(JarControl jarControl, Long callID, ByteArrayInputStream input, OutputStream output) {
    this.jarControl = jarControl;
    this.callID = callID;
    this.output = output;
    this.input = input;
  }

  private byte[] longToBytes(long x) {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.putLong(x);
    return buffer.array();
  }

  private void sendRequest(byte[] data) throws IOException {
    // data does not have size info
    byte[] cb = longToBytes(callID);
    byte[] sizeb = longToBytes(data.length);
    byte[] combine = new byte[cb.length + sizeb.length + data.length];
    System.arraycopy(cb, 0, combine, 0, cb.length);
    System.arraycopy(sizeb, 0, combine, cb.length, sizeb.length);
    System.arraycopy(data, 0, combine, cb.length + sizeb.length, data.length);
    synchronized (output) {
      output.write(combine);
      output.flush();
    }
  }

  @Override
  public void run() {
    try {
      ByteArrayOutputStream functionResult = new ByteArrayOutputStream();
      jarControl.invoke(input, functionResult);
      byte[] res = functionResult.toByteArray();
      System.out.println(functionResult.toString());
      sendRequest(res);
    } catch (IOException |IllegalAccessException | InvocationTargetException | InstantiationException e) {
      System.out.println(callID.toString().concat("request error:".concat(e.getMessage())));
    }
  }
}
