package service.openAI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import service.batch.BatchProperties;

@Configuration
@EnableConfigurationProperties({OpenAiProperties.class, BatchProperties.class})
public class OpenAiConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}