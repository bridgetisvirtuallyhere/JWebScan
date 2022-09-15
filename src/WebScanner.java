import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WebScanner {
    public static void main(String[] args) throws InterruptedException{
        // RejectedExecutionHandler implementation
        ConcreteRejectionHandler rejectionHandler = new ConcreteRejectionHandler();
        // Get the ThreadFactory implementation to use
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        // Creating the ThreadPoolExecutor for processing links
        ThreadPoolExecutor executorLinkPool = new ThreadPoolExecutor(2, 4, 10,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(4), threadFactory, rejectionHandler);

        // Get the ThreadFactory implementation to use
        threadFactory = Executors.defaultThreadFactory(); // Get a second executor, so we get a second pool and name
        // Creating the ThreadPoolExecutor for processing image meta-data
        ThreadPoolExecutor executorImagePool = new ThreadPoolExecutor(2, 6, 10,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(4), threadFactory, rejectionHandler);

        // Start the monitoring thread
        // MonitorThread monitor = new MonitorThread(executorImagePool, 3);
        // Thread monitorThread = new Thread(monitor);
        // monitorThread.start();

        BlockingQueue<String> linkQ = new LinkedBlockingQueue<>();
        BlockingQueue<String> imageQ = new LinkedBlockingQueue<>();
        // Add initial work to linkQ
        linkQ.put("https://www.humboldt.edu/about");
        // Since Thread Pool shutdown doesn't appear to work properly, I keep an ArrayList of my threads, so I can stop
        // them.
        ArrayList<LinkProducer> producers = new ArrayList<>();
        ArrayList<ImageConsumer> consumers = new ArrayList<>();

        //Submit work to the LinkProducer thread pool
        for(int i=0; i<8; i++){
            LinkProducer t = new LinkProducer(linkQ, imageQ);
            producers.add(t);
            executorLinkPool.execute(t);
        }

        //Submit work to the ImageConsumer thread pool
        for(int i=0; i<10; i++){
            ImageConsumer t = new ImageConsumer(imageQ);
            consumers.add(t);
            executorImagePool.execute(new ImageConsumer(imageQ));
        }

        Thread.sleep(30000); // This controls how long we run before shutting down.

        //shut down the pool
        for( LinkProducer p : producers) { p.stop(); }
        for( ImageConsumer c : consumers) { c.stop(); }
        shutdownAndAwaitTermination(executorLinkPool);
        shutdownAndAwaitTermination(executorImagePool);

        //shut down the monitor
        //Thread.sleep(5000);
        //monitor.shutdown();

    }

    static void shutdownAndAwaitTermination(ThreadPoolExecutor pool) {
        // Disable new tasks from being submitted
        pool.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks forcefully
                pool.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ex) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}