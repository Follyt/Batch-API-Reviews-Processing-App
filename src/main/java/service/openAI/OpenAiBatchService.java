package service.openAI;

import domain.entity.ReviewTagBatch;

public interface OpenAiBatchService {

    /**
     * Полный пайплайн:
     * - формирование input
     * - загрузка файла
     * - создание OpenAI batch
     * - сохранение requestFileId + openaiBatchId
     */
    void sendBatch(ReviewTagBatch batch);

    /**
     * Проверка статуса batch в OpenAI
     */
    void pollBatchResult(ReviewTagBatch batch);
}
