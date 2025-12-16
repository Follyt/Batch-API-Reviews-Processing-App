package service.openAI;

import domain.entity.ReviewTagBatch;

public interface OpenAiBatchService {

    String sendBatch(ReviewTagBatch batch);

    void pollBatchResult(ReviewTagBatch batch);
}

