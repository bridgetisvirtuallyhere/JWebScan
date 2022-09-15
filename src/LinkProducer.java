import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.BlockingQueue;

/*
 * Visits the URL that was passed in, extracts all of its links, and feeds them to the shared queue.
 */
public class LinkProducer implements Runnable {
    private final BlockingQueue<String> linkQ;
    private final BlockingQueue<String> imageQ;
    private boolean shouldExit;

    public LinkProducer(BlockingQueue<String> linkQ, BlockingQueue<String> imageQ){
        this.imageQ = imageQ;
        this.linkQ = linkQ;
        this.shouldExit = false;
    }

    @Override
    public void run() {
        try {
            String url;
            // Get link from linkQ
            //while(!(url = linkQ.take()).equals("exit"))
            while(!shouldExit) {
                url = linkQ.take();
                System.out.printf("%s LOADING URL: %s%n", Thread.currentThread().getName(), url);
                try {
                    if(!url.startsWith("http")) { continue; } // If the url doesn't start with http or https, skip it

                    Document doc = Jsoup.connect(url).get();
                    // Extract Links from linked page
                    Elements links = doc.select("a[href]");
                    // Put extracted links into linkQ
                    for (Element link : links) {
                        //System.out.printf(" * a: <%s>  (%s)%n", link.attr("abs:href"), link.text());
                        linkQ.put(link.attr("abs:href"));
                    }
                    // Extract img links  from linked page
                    Elements images = doc.select("img");
                    // Put extracted images into imageQ
                    for (Element image : images) {
                        //System.out.printf(" * img: <%s>", image.attr("abs:src"));
                        imageQ.put(image.attr("abs:src"));
                    }
                } catch (MalformedURLException mre) {
                    System.err.printf("Found a bad url %s%n", url);
                } catch (IOException ioe) {
                    System.err.printf("Error loading url %s%n", url);
                } catch (InterruptedException iei) {
                    System.err.printf("Thread interrupted adding url %s to the queue%n", url);
                    break;
                }
            }
        } catch (InterruptedException ie) {
            System.err.println("Thread interrupted trying to get new URL from queue.");
        }
        System.out.println(Thread.currentThread().getName() + " End.");
    }

    public void stop() {
        this.shouldExit = true;
        Thread.currentThread().interrupt();
    }
}
