package service.openAI;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.entity.Review;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class JsonlGeneratorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void groupsReviewsIntoConfiguredChunks() throws Exception {
        OpenAiProperties properties = new OpenAiProperties();
        properties.setJsonlChunkSize(5);

        JsonlGenerator generator = new JsonlGenerator(objectMapper, properties);

        List<Review> reviews = IntStream.rangeClosed(12, 17)
                .mapToObj(this::review)
                .toList();

        String jsonl = generator.generateJsonl(reviews);
        String[] lines = jsonl.split("\n");

        assertThat(lines).hasSize(2);

        Map<String, Object> firstLine = objectMapper.readValue(lines[0], new TypeReference<>() {});
        assertThat(firstLine.get("custom_id")).isEqualTo("chunk_0001");

        Map<String, Object> body = (Map<String, Object>) firstLine.get("body");
        List<Map<String, Object>> input = (List<Map<String, Object>>) body.get("input");
        Map<String, Object> inputItem = input.get(0);

        Map<String, Object> content = objectMapper.readValue(
                String.valueOf(inputItem.get("content")), new TypeReference<>() {}
        );

        List<Map<String, Object>> payloadReviews = (List<Map<String, Object>>) content.get("reviews");
        assertThat(payloadReviews).hasSize(5);
        assertThat(payloadReviews)
                .extracting(r -> r.get("review_id"))
                .containsExactly("12", "13", "14", "15", "16");
    }

    private Review review(int id) {
        Review review = new Review();
        setField(review, "reviewId", id);
        setField(review, "content", "content " + id);
        return review;
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
