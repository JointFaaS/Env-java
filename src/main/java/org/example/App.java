package org.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.example.rpc.ContainerServer;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
        throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException, InterruptedException {
        String[] init_envs = {"RUNTIME", "FUNC_NAME", "WORK_HOST"};
        for (String env:init_envs) {
            try {
                System.setProperty(env, System.getenv(env));
            } catch (NullPointerException e) {
                System.setProperty(env, "");
            }
        }
        final ContainerServer server = new ContainerServer();
        ManagedChannel channel = ManagedChannelBuilder.forTarget(System.getProperty("WORK_HOST"))
            // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
            // needing certificates.
            .usePlaintext()
            .build();
        server.start(channel);
        server.blockUntilShutdown();
    }
}
