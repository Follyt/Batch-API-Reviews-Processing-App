package service.openAI;

import domain.entity.ReviewTagBatch;
import domain.enums.BatchStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.ReviewTagBatchRepository;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiBatchServiceImpl implements OpenAiBatchService {

    private final ReviewTagBatchRepository reviewTagBatchRepository;
    private final OpenAiClient openAiClient;

    @Override
    @Transactional
    public void sendBatch(ReviewTagBatch batch) {

        if (batch.getRequestFileId() == null) {
            throw new IllegalStateException("requestFileId is null (batchId=" + batch.getId() + ")");
        }

        Map<String, Object> response = openAiClient.createBatch(batch.getRequestFileId());

        String openAiBatchId = String.valueOf(response.get("id"));
        batch.setOpenaiBatchId(openAiBatchId);
        batch.setStatus(BatchStatus.IN_PROGRESS);

        reviewTagBatchRepository.save(batch);

        log.info("OpenAI batch created: {}", openAiBatchId);
    }


    @Override
    @Transactional
    public void pollBatchResult(ReviewTagBatch batch) {

        Map<String, Object> response =
                openAiClient.retrieveBatch(batch.getOpenaiBatchId());

        String status = (String) response.get("status");

        log.info(
                "OpenAI batch {} status={}",
                batch.getOpenaiBatchId(),
                status
        );

        if (!"completed".equals(status)) {
            return;
        }

        String outputFileId = (String) response.get("output_file_id");
        String errorFileId  = (String) response.get("error_file_id");

        batch.setOutputFileId(outputFileId);
        batch.setErrorFileId(errorFileId);
        batch.setStatus(BatchStatus.COMPLETED);
        batch.setFinishedAt(OffsetDateTime.now());

        reviewTagBatchRepository.save(batch);

        log.info(
                "Batch {} completed. outputFileId={}",
                batch.getId(),
                outputFileId
        );

        // ⛔️ СЮДА МЫ ПРИДЁМ СЛЕДУЮЩИМ ШАГОМ:
        // - download output_file
        // - parse JSONL
        // - update review_tag_result
    }
}
