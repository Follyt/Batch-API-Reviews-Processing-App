package service.openAI;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {
    private String apiKey;
    private String baseUrl = "https://api.openai.com";
    private int jsonlChunkSize = 5;
}
