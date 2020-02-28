package org.example;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.example.serverless.JarControl;
import org.example.serverless.Worker;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void jarControlTest()
        throws IllegalAccessException, InstantiationException, InvocationTargetException {
        JarControl jarControl = new JarControl("D:/index.jar", "jointfaas.Index");
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        jarControl.invoke(new ByteArrayInputStream(new byte[0]), res);
        System.out.println(res.toString());
    }

    @Test
    public void WorkerTest() throws InterruptedException {
        ByteArrayOutputStream res1 = new ByteArrayOutputStream();
        ByteArrayOutputStream res2 = new ByteArrayOutputStream();
        ExecutorService threadPool = new ThreadPoolExecutor(2, 3, 200, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
        threadPool.submit(new Worker(new JarControl("D:/index.jar", "jointfaas.Index"),1L, new ByteArrayInputStream(new byte[0]), res1));
        threadPool.submit(new Worker(new JarControl("D:/index.jar", "jointfaas.Index"),1L, new ByteArrayInputStream(new byte[0]), res2));
        threadPool.shutdown();
        while (!threadPool.awaitTermination(2000, TimeUnit.MILLISECONDS));
        System.out.println(res1.toString());
        System.out.println(res2.toString());
    }
}
