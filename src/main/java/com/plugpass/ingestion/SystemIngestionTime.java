package com.plugpass.ingestion;
import java.time.Duration;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
import org.springframework.stereotype.Component;
@Component
public final class SystemIngestionTime implements IngestionTime {
    public long nanoTime() { return System.nanoTime(); }
    public void sleep(Duration duration) {
        try { Thread.sleep(duration); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new PublicDataException(PublicDataFailure.TRANSPORT);
        }
    }
}
