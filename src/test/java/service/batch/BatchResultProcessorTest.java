package service.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import domain.entity.ReviewTagBatch;
import domain.entity.ReviewTagResult;
import domain.enums.BatchStatus;
import domain.enums.TagResultStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.ReviewTagBatchRepository;
import repository.ReviewTagResultRepository;
import service.CategoryService.CategoryService;
import service.openAI.OpenAiClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchResultProcessorTest {

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private ReviewTagResultRepository resultRepository;

    @Mock
    private ReviewTagBatchRepository batchRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private BatchResultProcessor processor;

    private final ObjectMapper mapper = new ObjectMapper();

    private ReviewTagBatch batch;
    private ReviewTagResult pendingResult;

    @BeforeEach
    void setUp() {
        batch = new ReviewTagBatch();
        batch.setId(7L);
        batch.setOpenaiBatchId("batch-id");

        pendingResult = new ReviewTagResult(12, TagResultStatus.PENDING);
        pendingResult.setBatch(batch);
    }

    @Test
    void registersNewCategoriesAndUpdatesReviewResults() throws Exception {
        String payload = mapper.writeValueAsString(Map.of(
                "new_categories", List.of("Wi-Fi", "Parking"),
                "reviews", List.of(Map.of(
                        "review_id", "12",
                        "review_tags", List.of(Map.of(
                                "category", "Wi-Fi",
                                "sentiment", "Positive",
                                "tag_name", "fast"
                        ))
                ))
        ));

        String line = mapper.writeValueAsString(Map.of(
                "custom_id", "chunk_0001",
                "response", Map.of(
                        "body", Map.of(
                                "choices", List.of(Map.of(
                                        "message", Map.of(
                                                "content", List.of(Map.of(
                                                        "type", "text",
                                                        "text", Map.of("value", payload)
                                                ))
                                        )
                                ))
                        )
                )
        ));

        when(openAiClient.downloadFile("file-id"))
                .thenReturn(line);
        when(resultRepository.findByBatchId(batch.getId()))
                .thenReturn(List.of(pendingResult));

        batch.setOutputFileId("file-id");

        processor.processCompletedBatch(batch);

        verify(openAiClient).downloadFile("file-id");
        verify(categoryService).registerIfMissing("Wi-Fi");
        verify(categoryService).registerIfMissing("Parking");

        assertThat(pendingResult.getStatus()).isEqualTo(TagResultStatus.COMPLETED);
        assertThat(pendingResult.getResultJson()).contains("review_tags");
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(batch.getCompletedCount()).isEqualTo(1);

        ArgumentCaptor<List<ReviewTagResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(resultRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);

        verify(batchRepository).save(batch);
    }
}
