package service.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import service.ReviewTagProcessing.ReviewTagProcessingService;

@Slf4j
@Service
public class BatchPollingService {

    private final ReviewTagProcessingService processingService;

    public BatchPollingService(ReviewTagProcessingService processingService) {
        this.processingService = processingService;
    }

    @Scheduled(
            fixedDelayString = "${app.batch.poll-interval-seconds}000"
    )
    public void poll() throws JsonProcessingException {
        log.info("Batch polling started");
        processingService.processNextBatch();
    }
}
