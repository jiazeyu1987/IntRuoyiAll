package cn.iocoder.yudao.module.dcc.service.category;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_FILE_CATEGORY_SYNC_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_FILE_CATEGORY_SYNC_RESPONSE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DccIntAuthFileCategoryClientImplTest {

    @Test
    void listFileCategories_missingToken_failFast() {
        RestTemplate restTemplate = new RestTemplate();
        DccIntAuthProperties properties = new DccIntAuthProperties();
        properties.setBaseUrl("http://127.0.0.1:8020");
        DccIntAuthFileCategoryClientImpl client = new DccIntAuthFileCategoryClientImpl(restTemplate, properties);

        assertServiceException(client::listFileCategories, INTAUTH_FILE_CATEGORY_SYNC_CONFIG_MISSING);
    }

    @Test
    void listFileCategories_invalidPayload_failFast() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DccIntAuthProperties properties = buildConfiguredProperties();
        DccIntAuthFileCategoryClientImpl client = new DccIntAuthFileCategoryClientImpl(restTemplate, properties);

        server.expect(requestTo("http://127.0.0.1:8020/internal/quality-system/file-categories"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Token", "shared-token"))
                .andRespond(withSuccess("{\"file_categories\":[{}]}", MediaType.APPLICATION_JSON));

        assertServiceException(client::listFileCategories, INTAUTH_FILE_CATEGORY_SYNC_RESPONSE_INVALID);
        server.verify();
    }

    @Test
    void listFileCategories_success() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DccIntAuthProperties properties = buildConfiguredProperties();
        DccIntAuthFileCategoryClientImpl client = new DccIntAuthFileCategoryClientImpl(restTemplate, properties);

        server.expect(requestTo("http://127.0.0.1:8020/internal/quality-system/file-categories"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Token", "shared-token"))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {
                          "file_categories": [
                            { "id": 11, "name": "QA", "seeded_from_json": true, "is_active": true },
                            { "id": 12, "name": "SOP", "seeded_from_json": false, "is_active": true }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<DccIntAuthFileCategoryClient.IntAuthFileCategory> categories = client.listFileCategories();

        assertEquals(2, categories.size());
        assertEquals(11L, categories.get(0).id());
        assertEquals("QA", categories.get(0).name());
        assertEquals(true, categories.get(0).seededFromJson());
        assertEquals(true, categories.get(0).active());
        assertEquals(12L, categories.get(1).id());
        assertEquals("SOP", categories.get(1).name());
        server.verify();
    }

    private static DccIntAuthProperties buildConfiguredProperties() {
        DccIntAuthProperties properties = new DccIntAuthProperties();
        properties.setBaseUrl("http://127.0.0.1:8020");
        properties.setInternalServiceToken("shared-token");
        return properties;
    }

}
