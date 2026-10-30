package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ErpKingdeeRestTemplateConfigurationTest {

    @Test
    void materialListUsesDedicatedTimeoutWithoutChangingOtherClients() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ErpKingdeeRestTemplateConfiguration.class, ErpKingdeeProductionMaterialListClientImpl.class)) {
            RestTemplate shared = context.getBean("erpKingdeeRestTemplate", RestTemplate.class);
            RestTemplate materialList = context.getBean("erpKingdeeProductionMaterialListRestTemplate", RestTemplate.class);
            assertSame(shared, context.getBean(RestTemplate.class));
            assertSame(materialList, ReflectionTestUtils.getField(
                    context.getBean(ErpKingdeeProductionMaterialListClientImpl.class), "restTemplate"));
            assertEquals(30_000, ReflectionTestUtils.getField(shared.getRequestFactory(), "readTimeout"));
            assertEquals(120_000, ReflectionTestUtils.getField(materialList.getRequestFactory(), "readTimeout"));
            assertEquals(10_000, ReflectionTestUtils.getField(materialList.getRequestFactory(), "connectTimeout"));
        }
    }
}
