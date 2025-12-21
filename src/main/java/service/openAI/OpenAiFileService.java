package service.openAI;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiFileService {

    private final JsonlGenerator jsonlGenerator;
    private final OpenAiClient openAiClient;

    public String uploadBatchFile(List<Review> reviews) throws JsonProcessingException {
        String jsonl = jsonlGenerator.generateJsonl(reviews);

        Map<String, Object> response = openAiClient.uploadBatchRequestFile(jsonl);
        Object id = response.get("id");

        if (id == null) {
            throw new IllegalStateException("OpenAI upload response has no id: " + response);
        }

        return String.valueOf(id);
    }
}
