package org.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.example.serverless.JarControl;
import org.example.serverless.SocketControl;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
        throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Map<String, String> map = System.getenv();
        SocketControl socketControl = new SocketControl("/var/run/worker.sock", map.get("funcName"), map.get("envID"));
        socketControl.run();
    }

    public static void jarControlTest()
        throws IllegalAccessException, InstantiationException, InvocationTargetException {
        JarControl jarControl = new JarControl("D:/index.jar", "jointfaas.Index");
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        jarControl.invoke(new ByteArrayInputStream(new byte[0]), res);
        System.out.println(res.toString());
    }
}
