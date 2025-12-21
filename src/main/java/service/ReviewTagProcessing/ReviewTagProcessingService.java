package service.ReviewTagProcessing;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface ReviewTagProcessingService {

    /**
     * Запускает один цикл обработки:
     * - берёт порцию отзывов
     * - создаёт batch
     * - резервирует отзывы
     */
    void processNextBatch() throws JsonProcessingException;
}
