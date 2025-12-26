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
import service.CategoryService.CategoryService;
import service.openAI.OpenAiClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchResultProcessor {

    private final OpenAiClient openAiClient;
    private final ReviewTagResultRepository resultRepository;
    private final ReviewTagBatchRepository batchRepository;
    private final CategoryService categoryService;

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

                JsonNode contentNode = extractContentNode(root.path("response").path("body"));


                String contentJsonText = extractContentJson(contentNode);
                if (contentJsonText == null) {
                    log.warn("Empty content for batch line with custom_id={}", root.path("custom_id").asText());
                    continue;
                }

                JsonNode payload = objectMapper.readTree(contentJsonText);
                JsonNode newCategories = payload.path("new_categories");
                registerNewCategories(newCategories);
                JsonNode reviewResults = payload.path("reviews");
                if (!reviewResults.isArray()) {
                    log.warn("Unexpected payload format for custom_id={}: reviews array missing", root.path("custom_id").asText());
                    continue;
                }

                for (JsonNode reviewNode : reviewResults) {
                    String reviewIdText = reviewNode.path("review_id").asText(null);
                    if (reviewIdText == null) {
                        log.warn("review_id missing in payload for custom_id={}", root.path("custom_id").asText());
                        continue;
                    }

                    Optional<ReviewTagResult> maybeResult = results.stream()
                            .filter(r -> String.valueOf(r.getReviewId()).equals(reviewIdText))
                            .findFirst();

                    if (maybeResult.isEmpty()) {
                        log.warn("Result not found for review_id={} (custom_id={})", reviewIdText, root.path("custom_id").asText());
                        continue;
                    }

                    ReviewTagResult result = maybeResult.get();
                    registerCategoriesFromTags(reviewNode.path("review_tags"));
                    result.setResultJson(reviewNode.toString());
                    result.setStatus(TagResultStatus.COMPLETED);
                    result.setModel("o3-mini");
                    result.setUpdatedAt(OffsetDateTime.now());
                    updated++;
                }

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

    /**
     * Extracts the JSON string returned by the model from the message content node. The Responses API
     * returns content as an array of text parts; we take the first text value if present.
     */
    private String extractContentJson(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode()) {
            return null;
        }

        if (contentNode.isArray() && !contentNode.isEmpty()) {
            JsonNode first = contentNode.get(0);
            if (first.has("text") && first.get("text").isTextual()) {
                return first.get("text").asText();
            }
            JsonNode textValue = first.path("text").path("value");
            if (textValue.isTextual()) {
                return textValue.asText();
            }
        }

        if (contentNode.isTextual()) {
            return contentNode.asText();
        }

        return contentNode.toString();
    }

    /**
     * Response body format differs between the Responses API ("output" array) and Chat Completions
     * ("choices" array). This helper normalizes both cases and returns the content array node if
     * present.
     */
    private JsonNode extractContentNode(JsonNode responseBody) {
        if (responseBody == null || responseBody.isMissingNode()) {
            return null;
        }

        // Responses API: body.output[...].type == "message"
        JsonNode output = responseBody.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                if ("message".equals(item.path("type").asText())) {
                    return item.path("content");
                }
            }
        }

        // Chat Completions style fallback
        JsonNode choices = responseBody.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content");
        }

        return null;
    }

    private void registerNewCategories(JsonNode newCategories) {
        if (newCategories == null || !newCategories.isArray()) {
            return;
        }

        for (JsonNode categoryNode : newCategories) {
            if (categoryNode.isTextual()) {
                categoryService.registerIfMissing(categoryNode.asText());
            }
        }
    }

    private void registerCategoriesFromTags(JsonNode reviewTags) {
        if (reviewTags == null || !reviewTags.isArray()) {
            return;
        }

        for (JsonNode tagNode : reviewTags) {
            JsonNode categoryNode = tagNode.path("category");
            if (categoryNode.isTextual()) {
                categoryService.registerIfMissing(categoryNode.asText());
            }
        }
    }

}
