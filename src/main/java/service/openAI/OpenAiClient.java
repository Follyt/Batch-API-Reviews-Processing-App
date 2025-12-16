package service.openAI;

import java.util.Map;

public interface OpenAiClient {
    Map<String, Object> createBatch(String requestFileId);
    Map<String, Object> retrieveBatch(String batchId);
}
