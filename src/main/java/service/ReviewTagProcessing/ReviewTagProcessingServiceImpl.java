package service.ReviewTagProcessing;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.entity.Review;
import domain.entity.ReviewTagBatch;
import domain.entity.ReviewTagResult;
import domain.enums.BatchStatus;
import domain.enums.TagResultStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.ReviewRepository;
import repository.ReviewTagBatchRepository;
import repository.ReviewTagResultRepository;
import service.batch.BatchProperties;
import service.openAI.OpenAiBatchService;
import service.openAI.OpenAiFileService;

import java.util.List;

@Slf4j
@Service
public class ReviewTagProcessingServiceImpl implements ReviewTagProcessingService {

    private final ReviewRepository reviewRepository;
    private final ReviewTagResultRepository resultRepository;
    private final ReviewTagBatchRepository batchRepository;
    private final OpenAiBatchService openAiBatchService;
    private final OpenAiFileService openAiFileService;
    private final BatchProperties batchProperties;

    public ReviewTagProcessingServiceImpl(
            ReviewRepository reviewRepository,
            ReviewTagResultRepository resultRepository,
            ReviewTagBatchRepository batchRepository,
            OpenAiBatchService openAiBatchService,
            OpenAiFileService openAiFileService,
            BatchProperties batchProperties
    ) {
        this.reviewRepository = reviewRepository;
        this.resultRepository = resultRepository;
        this.batchRepository = batchRepository;
        this.openAiBatchService = openAiBatchService;
        this.openAiFileService = openAiFileService;
        this.batchProperties = batchProperties;
    }

    @Override
    @Transactional
    public void processNextBatch() throws JsonProcessingException {

        log.info("processNextBatch called");

        int portionSize = resolvePortionSize();
        List<Review> reviews = reviewRepository.findNextUnprocessed(PageRequest.of(0, portionSize));
        if (reviews.isEmpty()) {
            log.info("No reviews to process");
            return;
        }

        ReviewTagBatch batch = new ReviewTagBatch();
        batch.setStatus(BatchStatus.CREATED);
        batch.setRequestedCount(reviews.size());
        batch = batchRepository.save(batch);

        ReviewTagBatch finalBatch = batch;
        List<ReviewTagResult> results = reviews.stream()
                .map(review -> {
                    ReviewTagResult result = new ReviewTagResult(review.getReviewId(), TagResultStatus.PENDING);
                    result.setBatch(finalBatch);
                    return result;
                })
                .toList();
        resultRepository.saveAll(results);

        // 1) Генерация + upload JSONL → получаем request_file_id
        String requestFileId = openAiFileService.uploadBatchFile(reviews);
        batch.setRequestFileId(requestFileId);
        batchRepository.save(batch);

        // 2) Создание Batch в OpenAI → сохранит openai_batch_id и поставит IN_PROGRESS
        openAiBatchService.sendBatch(batch);

        log.info("Batch {} sent to OpenAI (requestedCount={})", batch.getId(), batch.getRequestedCount());
    }

    private int resolvePortionSize() {
        int portionSize = batchProperties.getPortionSize();
        if (portionSize <= 0) {
            throw new IllegalStateException("app.batch.portion-size must be positive");
        }
        return portionSize;
    }
}
