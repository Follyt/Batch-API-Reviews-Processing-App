package service.ReviewTagProcessing;

public interface ReviewTagProcessingService {

    /**
     * Запускает один цикл обработки:
     * - берёт порцию отзывов
     * - создаёт batch
     * - резервирует отзывы
     */
    void processNextBatch();
}
