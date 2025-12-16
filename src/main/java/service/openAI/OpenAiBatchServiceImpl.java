package service.openAI;

import domain.entity.ReviewTagBatch;
import domain.enums.BatchStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiBatchServiceImpl implements OpenAiBatchService {

    private final OpenAiClient openAiClient;

    @Override
    @Transactional
    public String sendBatch(ReviewTagBatch batch) {

        Map<String, Object> response =
                openAiClient.createBatch(batch.getRequestFileId());

        String openAiBatchId = (String) response.get("id");

        batch.setOpenaiBatchId(openAiBatchId);
        batch.setStatus(BatchStatus.IN_PROGRESS);
        batch.setUpdatedAt(OffsetDateTime.now());

        log.info("OpenAI batch sent, id={}", openAiBatchId);

        return openAiBatchId;
    }

    @Override
    public void pollBatchResult(ReviewTagBatch batch) {
        Map<String, Object> response =
                openAiClient.retrieveBatch(batch.getOpenaiBatchId());

        String status = (String) response.get("status");

        log.info("OpenAI batch {} status={}", batch.getOpenaiBatchId(), status);

        // дальше разберём
    }
}
