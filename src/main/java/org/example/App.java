package org.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.example.serverless.JarControl;
import org.example.serverless.SocketControl;
import org.example.serverless.Worker;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
        throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Map<String, String> map = System.getenv();
        SocketControl socketControl = new SocketControl("/var/run/worker.sock", map.get("funcName"), map.get("envID"), true);
        socketControl.run();
    }
}
