package org.example;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.example.serverless.FnControl;
import org.example.serverless.SocketControl;

/**
 * Hello world!
 *
 */
public class App 
{

    static final private String ENTRY_POINT_CLASS = "jointfaas.Index";
    static final private String ENTRY_POINT_METHOD = "handle";

    public static void main( String[] args )
            throws IOException {
        Map<String, String> map = System.getenv();
        SocketControl socketControl = new SocketControl("/var/run/worker.sock", map.get("funcName"), map.get("envID"), true);
        try {
            FnControl fnControl = new FnControl(ENTRY_POINT_CLASS, ENTRY_POINT_METHOD);
            socketControl.setFnControl(fnControl);
        }catch (Exception e){
            e.printStackTrace();
            return;
            // TODO
            // if fnControl fails, send a fail signal to socket.
            // now, we will close the socket, it's a simple signal to inform Worker of fn failure
        }

        socketControl.run();
    }
}
