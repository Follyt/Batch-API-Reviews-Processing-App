package service.batch;

import domain.entity.ReviewTagBatch;
import domain.enums.BatchStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import service.openAI.OpenAiBatchService;
import repository.ReviewTagBatchRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchResultPollingService {

    private final ReviewTagBatchRepository batchRepository;
    private final OpenAiBatchService openAiBatchService;

    @Scheduled(fixedDelayString = "${app.batch.poll-interval-seconds:10}000")
    public void poll() {
        log.info("Polling OpenAI batches");

        List<ReviewTagBatch> batches =
                batchRepository.findByStatus(BatchStatus.IN_PROGRESS);

        for (ReviewTagBatch batch : batches) {
            openAiBatchService.pollBatchResult(batch);
        }
    }
}
