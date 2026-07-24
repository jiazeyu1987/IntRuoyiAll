package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.module.dcc.service.category.DccIntAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_POSITION_SYNC_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_POSITION_SYNC_RESPONSE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DccIntAuthPositionClientImplTest {

    @Test
    void listPositions_missingToken_failFast() {
        RestTemplate restTemplate = new RestTemplate();
        DccIntAuthProperties properties = new DccIntAuthProperties();
        properties.setBaseUrl("http://127.0.0.1:8020");
        DccIntAuthPositionClientImpl client = new DccIntAuthPositionClientImpl(restTemplate, properties);

        assertServiceException(client::listPositions, INTAUTH_POSITION_SYNC_CONFIG_MISSING);
    }

    @Test
    void listPositions_invalidPayload_failFast() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DccIntAuthProperties properties = buildConfiguredProperties();
        DccIntAuthPositionClientImpl client = new DccIntAuthPositionClientImpl(restTemplate, properties);

        server.expect(requestTo("http://127.0.0.1:8020/internal/quality-system/positions"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Token", "shared-token"))
                .andRespond(withSuccess("{\"positions\":[{}]}", MediaType.APPLICATION_JSON));

        assertServiceException(client::listPositions, INTAUTH_POSITION_SYNC_RESPONSE_INVALID);
        server.verify();
    }

    @Test
    void listPositions_success() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DccIntAuthProperties properties = buildConfiguredProperties();
        DccIntAuthPositionClientImpl client = new DccIntAuthPositionClientImpl(restTemplate, properties);

        server.expect(requestTo("http://127.0.0.1:8020/internal/quality-system/positions"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Token", "shared-token"))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {
                          "positions": [
                            { "id": 11, "name": "QA", "in_signoff": true, "in_compiler": false, "in_approver": false, "assigned_users": [] },
                            { "id": 12, "name": "Approver", "in_signoff": false, "in_compiler": false, "in_approver": true, "assigned_users": [] }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<DccIntAuthPositionClient.IntAuthPosition> positions = client.listPositions();

        assertEquals(2, positions.size());
        assertEquals(11L, positions.get(0).id());
        assertEquals("QA", positions.get(0).name());
        assertEquals(12L, positions.get(1).id());
        assertEquals("Approver", positions.get(1).name());
        server.verify();
    }

    private static DccIntAuthProperties buildConfiguredProperties() {
        DccIntAuthProperties properties = new DccIntAuthProperties();
        properties.setBaseUrl("http://127.0.0.1:8020");
        properties.setInternalServiceToken("shared-token");
        return properties;
    }
}
