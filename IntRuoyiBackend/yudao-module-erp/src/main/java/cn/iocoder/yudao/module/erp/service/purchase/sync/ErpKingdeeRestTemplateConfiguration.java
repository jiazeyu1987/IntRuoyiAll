package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ErpKingdeeRestTemplateConfiguration {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    @SuppressWarnings("removal")
    public RestTemplate erpKingdeeRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(CONNECT_TIMEOUT.toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(READ_TIMEOUT.toMillis()));
        return new RestTemplate(requestFactory);
    }

}
