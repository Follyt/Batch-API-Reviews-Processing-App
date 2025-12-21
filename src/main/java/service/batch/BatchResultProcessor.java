package service.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.entity.ReviewTagBatch;
import domain.entity.ReviewTagResult;
import domain.enums.BatchStatus;
import domain.enums.TagResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.ReviewTagBatchRepository;
import repository.ReviewTagResultRepository;
import service.openAI.OpenAiClient;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchResultProcessor {

    private final OpenAiClient openAiClient;
    private final ReviewTagResultRepository resultRepository;
    private final ReviewTagBatchRepository batchRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void processCompletedBatch(ReviewTagBatch batch) {

        if (batch.getOutputFileId() == null) {
            throw new IllegalStateException("output_file_id is null for batch " + batch.getId());
        }

        log.info("Processing output file {} for batch {}", batch.getOutputFileId(), batch.getId());

        String jsonl = openAiClient.downloadFile(batch.getOutputFileId());

        log.info("OPENAI OUTPUT FILE:\n{}", jsonl);

        List<ReviewTagResult> results =
                resultRepository.findByBatchId(batch.getId());

        int updated = 0;

        for (String line : jsonl.split("\n")) {
            if (line.isBlank()) continue;

            try {
                JsonNode root = objectMapper.readTree(line);

                long reviewId =
                        root.path("custom_id").asLong();

                JsonNode content =
                        root.path("response")
                                .path("body")
                                .path("choices")
                                .get(0)
                                .path("message")
                                .path("content");

                ReviewTagResult result = results.stream()
                        .filter(r -> r.getReviewId() == reviewId)
                        .findFirst()
                        .orElse(null);

                if (result == null) {
                    log.warn("Result not found for review_id={}", reviewId);
                    continue;
                }

                result.setResultJson(content.toString());
                result.setStatus(TagResultStatus.COMPLETED);
                result.setModel("gpt-4.1-mini");
                result.setUpdatedAt(OffsetDateTime.now());

                updated++;

            } catch (Exception e) {
                log.error("Failed to parse line: {}", line, e);
            }
        }

        resultRepository.saveAll(results);

        batch.setCompletedCount(updated);
        batch.setStatus(BatchStatus.COMPLETED);
        batch.setFinishedAt(OffsetDateTime.now());

        batchRepository.save(batch);

        log.info("Batch {} processed: {} results updated", batch.getId(), updated);
    }
}
