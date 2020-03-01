package jointfaas.serverless;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class Request implements Serializable {
  private byte[] data;
  private Long callID;
  static final private int HEADER_SIZE = 16;

  public Long getCallID() {
    return callID;
  }

  public void setCallID(Long callID) {
    this.callID = callID;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public void sendTo(OutputStream output) throws IOException {
    byte[] header = new byte[HEADER_SIZE];
    ByteBuffer bb = ByteBuffer.wrap(header);
    bb.putLong(callID);
    bb.putLong(data.length);
    synchronized (output) {
      output.write(header);
      output.write(data);
      output.flush();
    }
  }

  public void parse(InputStream input) throws IOException {

  }
}
