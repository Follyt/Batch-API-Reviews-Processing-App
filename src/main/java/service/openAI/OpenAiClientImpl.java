package service.openAI;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiClientImpl implements OpenAiClient {

    private final RestTemplate restTemplate;
    private final OpenAiProperties properties;

    @Override
    public Map<String, Object> uploadBatchRequestFile(String jsonlContent) {
        String url = properties.getBaseUrl() + "/files";

        byte[] bytes = jsonlContent.getBytes(StandardCharsets.UTF_8);

        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "batch_requests.jsonl";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("purpose", "batch");
        body.add("file", fileResource);

        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("OpenAI file upload failed, status=" + response.getStatusCode());
        }

        return (Map<String, Object>) response.getBody();
    }

    @Override
    public Map<String, Object> createBatch(String inputFileId) {
        String url = properties.getBaseUrl() + "/batches";

        // Важно: endpoint в batch requests должен быть /v1/chat/completions
        Map<String, Object> body = Map.of(
                "input_file_id", inputFileId,
                "endpoint", "/v1/responses",
                "completion_window", "24h"
        );

        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("OpenAI batch create failed, status=" + response.getStatusCode());
        }

        return (Map<String, Object>) response.getBody();
    }

    @Override
    public Map<String, Object> retrieveBatch(String batchId) {
        String url = properties.getBaseUrl() + "/batches/" + batchId;

        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("OpenAI batch retrieve failed, status=" + response.getStatusCode());
        }

        return (Map<String, Object>) response.getBody();
    }

    @Override
    public String downloadFile(String fileId) {
        String url = properties.getBaseUrl() + "/files/" + fileId + "/content";

        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException(
                    "OpenAI file download failed, status=" + response.getStatusCode()
            );
        }

        return response.getBody();
    }


    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getApiKey());
        return headers;
    }
}
