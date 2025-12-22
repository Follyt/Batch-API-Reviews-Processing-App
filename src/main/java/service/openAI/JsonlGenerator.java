package service.openAI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JsonlGenerator {

    private static final String PROMPT_ID =
            "pmpt_68a383a9f09c8194bde5bbc066b3ed20047740d617bc3b56";
    private static final String PROMPT_VERSION = "62";

    private static final String CHUNK_ID_PATTERN = "chunk_%04d";

    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;

    public String generateJsonl(List<Review> reviews) {
        StringBuilder sb = new StringBuilder();

        int chunkSize = openAiProperties.getJsonlChunkSize();
        if (chunkSize <= 0) {
            throw new IllegalStateException("app.openai.jsonl-chunk-size must be positive");
        }

        List<List<Review>> reviewChunks = chunkReviews(reviews, chunkSize);
        int chunkIndex = 0;

        for (List<Review> chunk : reviewChunks) {
            chunkIndex++;

            // -------- root line --------
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("custom_id", String.format(CHUNK_ID_PATTERN, chunkIndex));
            line.put("method", "POST");
            line.put("url", "/v1/responses");

            // -------- body --------
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "o3-mini");

            Map<String, Object> prompt = new LinkedHashMap<>();
            prompt.put("id", PROMPT_ID);
            prompt.put("version", PROMPT_VERSION);
            body.put("prompt", prompt);

            // -------- payload --------
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("review_categories", List.of());

            List<Map<String, Object>> reviewsPayload = new ArrayList<>();
            for (Review review : chunk) {
                Map<String, Object> reviewObj = new LinkedHashMap<>();
                reviewObj.put("review_id", String.valueOf(review.getReviewId()));
                reviewObj.put("review", review.getContent());
                reviewsPayload.add(reviewObj);
            }

            payload.put("reviews", reviewsPayload);

            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Payload serialization failed", e);
            }

            // -------- input --------
            Map<String, Object> inputItem = new LinkedHashMap<>();
            inputItem.put("role", "user");
            inputItem.put("content", payloadJson);

            body.put("input", List.of(inputItem));

            // -------- text.format (JSON Schema) --------
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "object");
            schema.put("additionalProperties", false);
            schema.put("required", List.of("new_categories", "reviews"));

            Map<String, Object> schemaProps = new LinkedHashMap<>();

            schemaProps.put("new_categories", Map.of(
                    "type", "array",
                    "items", Map.of("type", "string")
            ));

            schemaProps.put("reviews", Map.of(
                    "type", "array",
                    "items", Map.of(
                            "type", "object",
                            "additionalProperties", false,
                            "required", List.of("review_id", "review_tags"),
                            "properties", Map.of(
                                    "review_id", Map.of("type", "string", "minLength", 1),
                                    "review_tags", Map.of(
                                            "type", "array",
                                            "items", Map.of(
                                                    "type", "object",
                                                    "additionalProperties", false,
                                                    "required", List.of("category", "sentiment", "tag_name"),
                                                    "properties", Map.of(
                                                            "category", Map.of("type", "string", "minLength", 1),
                                                            "sentiment", Map.of(
                                                                    "type", "string",
                                                                    "enum", List.of("Positive", "Negative", "Neutral")
                                                            ),
                                                            "tag_name", Map.of("type", "string", "minLength", 2)
                                                    )
                                            )
                                    )
                            )
                    )
            ));

            schema.put("properties", schemaProps);

            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", "categorized_reviews");
            format.put("strict", true);
            format.put("schema", schema);

            body.put("text", Map.of("format", format));

            line.put("body", body);

            try {
                sb.append(objectMapper.writeValueAsString(line)).append("\n");
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("JSONL serialization failed", e);
            }
        }

        return sb.toString();
    }

    private List<List<Review>> chunkReviews(List<Review> reviews, int chunkSize) {
        List<List<Review>> result = new ArrayList<>();
        for (int i = 0; i < reviews.size(); i += chunkSize) {
            int toIndex = Math.min(reviews.size(), i + chunkSize);
            result.add(new ArrayList<>(reviews.subList(i, toIndex)));
        }
        return result;
    }

}
