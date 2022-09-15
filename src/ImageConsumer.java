import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import java.net.URL;

/*
 * Visits the URL that was passed in, extracts all of its links, and feeds them to the shared queue.
 */
public class ImageConsumer implements Runnable {
    private final BlockingQueue<String> imageQ;
    private boolean shouldExit;

    public ImageConsumer(BlockingQueue<String> imageQ){
        this.imageQ = imageQ;
        this.shouldExit = false;
    }

    @Override
    public void run() {
        Thread ct = Thread.currentThread();
        try {
            String url;
            // Get link from linkQ
            // while(!(url = imageQ.take()).equals("exit")) {
            while(!shouldExit) {
                url = imageQ.take();
                System.out.printf("%s Getting IMG MetaData: %s%n", ct.getName(), url);
                getHiddenSecrets(url);
            }
        } catch (InterruptedException ie) {
            System.err.println("Thread interrupted trying to get new URL from queue.");
        }
        System.out.println(ct.getName() + " End.");
    }

    public void stop() {
        this.shouldExit = true;
        Thread.currentThread().interrupt();
    }

    public static void getHiddenSecrets(String url) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(
                    new URL(url).openStream()
            );
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    System.out.printf("[%s] - %s = %s%n", directory.getName(), tag.getTagName(), tag.getDescription());
                }
                if (directory.hasErrors()) {
                    for (String error : directory.getErrors()) {
                        System.err.printf("ERROR: %s%n", error);
                    }
                }
            }
        } catch (IOException ioe) {
            System.err.println("Problem reading from data stream.");
        } catch (ImageProcessingException ipe) {
            System.out.println("Failed to process the image meta-data");
        }
    }
}
