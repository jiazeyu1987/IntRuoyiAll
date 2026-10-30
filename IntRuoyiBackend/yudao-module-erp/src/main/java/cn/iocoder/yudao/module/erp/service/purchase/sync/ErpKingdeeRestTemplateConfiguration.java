package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ErpKingdeeRestTemplateConfiguration {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    static final Duration PRODUCTION_MATERIAL_LIST_READ_TIMEOUT = Duration.ofSeconds(120);

    @Bean
    @Primary
    @SuppressWarnings("removal")
    public RestTemplate erpKingdeeRestTemplate() {
        return createRestTemplate(READ_TIMEOUT);
    }

    @Bean
    public RestTemplate erpKingdeeProductionMaterialListRestTemplate() {
        // Full PRD_PPBOM responses can stall beyond the shared 30-second read timeout.
        return createRestTemplate(PRODUCTION_MATERIAL_LIST_READ_TIMEOUT);
    }

    @SuppressWarnings("removal")
    private RestTemplate createRestTemplate(Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(CONNECT_TIMEOUT.toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(readTimeout.toMillis()));
        return new RestTemplate(requestFactory);
    }

}
