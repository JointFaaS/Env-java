package org.example;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.example.serverless.FnControl;
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
        FnControl fnControl = new FnControl("D:/index.jar", "jointfaas.Index");
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        fnControl.invoke(new ByteArrayInputStream(new byte[0]), res);
        System.out.println(res.toString());
    }

    @Test
    public void WorkerTest() throws InterruptedException {
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        ExecutorService threadPool = new ThreadPoolExecutor(2, 3, 200, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
        threadPool.submit(new Worker(new FnControl("D:/index.jar", "jointfaas.Index"),1L, new ByteArrayInputStream(new byte[0]), res));
        threadPool.submit(new Worker(new FnControl("D:/index.jar", "jointfaas.Index"),1L, new ByteArrayInputStream(new byte[0]), res));
        threadPool.shutdown();
        while (!threadPool.awaitTermination(2000, TimeUnit.MILLISECONDS));
        System.out.println(res.toString());
    }
}
