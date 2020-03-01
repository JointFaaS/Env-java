package org.example.serverless;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

public class Worker implements Runnable {

  private FnControl fnControl;
  private final OutputStream output;
  private Request request;

  public Worker(FnControl fnControl, Request request, OutputStream output) {
    this.fnControl = fnControl;
    this.request = request;
    this.output = output;
  }

  @Override
  public void run() {
    try {
      ByteArrayOutputStream functionResult = new ByteArrayOutputStream();
      fnControl.invoke(new ByteArrayInputStream(request.getData()), functionResult);
      Request response = new Request();
      response.setCallID(request.getCallID());
      response.setData(functionResult.toByteArray());
      response.sendTo(output);
    } catch (IOException |IllegalAccessException | InvocationTargetException | InstantiationException e) {
      System.out.println(request.getCallID().toString().concat("request error:".concat(e.getMessage())));
    }
  }
}
