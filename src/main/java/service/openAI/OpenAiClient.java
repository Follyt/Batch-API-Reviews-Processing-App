package service.openAI;

import java.util.Map;

public interface OpenAiClient {

    Map<String, Object> uploadBatchRequestFile(String jsonlContent);

    Map<String, Object> createBatch(String inputFileId);

    Map<String, Object> retrieveBatch(String batchId);

    String downloadFile(String fileId);
}
