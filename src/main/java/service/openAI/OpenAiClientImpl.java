package service.openAI;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClientImpl implements OpenAiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    @Value("${OPENAI_BASE_URL}")
    private String baseUrl;

    @Override
    public Map<String, Object> createBatch(String requestFileId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("input_file_id", "file-XXXXXXXXXXXXXXXXXXXX"); // <-- ВСТАВЬ РЕАЛЬНЫЙ file_id
        body.put("endpoint", "/v1/chat/completions");
        body.put("completion_window", "24h");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/v1/batches",
                entity,
                Map.class
        );

        return response.getBody();
    }


    @Override
    public Map<String, Object> retrieveBatch(String batchId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/v1/batches/" + batchId,
                HttpMethod.GET,
                entity,
                Map.class
        );

        return response.getBody();
    }
}
