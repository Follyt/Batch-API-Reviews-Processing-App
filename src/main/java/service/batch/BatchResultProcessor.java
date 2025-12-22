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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        Set<String> categoriesToRegister = new LinkedHashSet<>();

        for (String line : jsonl.split("\n")) {
            if (line.isBlank()) continue;

            try {
                JsonNode root = objectMapper.readTree(line);

                JsonNode contentNode =
                        root.path("response")
                                .path("body")
                                .path("choices")
                                .get(0)
                                .path("message")
                                .path("content");

                // OpenAI Responses API returns message.content as an array; extract the text payload
                String contentJsonText = extractContentJson(contentNode);
                if (contentJsonText == null) {
                    log.warn("Empty content for batch line with custom_id={}", root.path("custom_id").asText());
                    continue;
                }

                JsonNode payload = objectMapper.readTree(contentJsonText);
                collectCategories(payload, categoriesToRegister);
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
                    result.setResultJson(reviewNode.toString());
                    result.setStatus(TagResultStatus.COMPLETED);
                    result.setModel("gpt-4.1-mini");
                    result.setUpdatedAt(OffsetDateTime.now());
                    updated++;
                }

            } catch (Exception e) {
                log.error("Failed to parse line: {}", line, e);
            }
        }

        registerCategories(categoriesToRegister);
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

            // Responses API usually returns {"type": "output_text", "text": {"value": "..."}}
            JsonNode textValue = first.path("text");
            if (textValue.hasNonNull("value")) {
                return textValue.path("value").asText();
            }

            if (textValue.isTextual()) {
                return textValue.asText();
            }
        }

        if (contentNode.isTextual()) {
            return contentNode.asText();
        }

        // Fallback: try to read "text" directly if caller passed a message object instead of array
        JsonNode textNode = contentNode.path("text");
        if (textNode.hasNonNull("value")) {
            return textNode.path("value").asText();
        }
        if (textNode.isTextual()) {
            return textNode.asText();
        }

        return contentNode.toString();
    }

    private void collectCategories(JsonNode payload, Set<String> categoriesToRegister) {
        JsonNode newCategories = payload.path("new_categories");
        if (newCategories.isArray()) {
            newCategories.forEach(node -> {
                if (node.isTextual()) {
                    categoriesToRegister.add(node.asText());
                }
            });
        }

        JsonNode reviewsNode = payload.path("reviews");
        if (reviewsNode.isArray()) {
            for (JsonNode reviewNode : reviewsNode) {
                JsonNode reviewTags = reviewNode.path("review_tags");
                if (reviewTags.isArray()) {
                    reviewTags.forEach(tag -> {
                        JsonNode category = tag.path("category");
                        if (category.isTextual()) {
                            categoriesToRegister.add(category.asText());
                        }
                    });
                }
            }
        }
    }

    private void registerCategories(Set<String> categoriesToRegister) {
        for (String category : categoriesToRegister) {
            try {
                categoryService.registerIfMissing(category);
            } catch (Exception e) {
                log.error("Failed to register category {}", category, e);
            }
        }
    }
}
