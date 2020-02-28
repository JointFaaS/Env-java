package org.example.serverless;

import java.io.Serializable;

public class Request implements Serializable {
  private byte[] data;
  private Long callID;

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
}
