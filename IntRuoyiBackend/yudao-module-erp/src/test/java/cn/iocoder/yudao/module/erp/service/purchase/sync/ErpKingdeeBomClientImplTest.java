package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeBomClientImplTest {

    @Test
    void fetchApprovedBomByParentMaterialNumber_returnsApprovedUniqueBomRows() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeBomClientImpl client = new ErpKingdeeBomClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com", 200);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("ENG_BOM")))
                .andExpect(content().string(containsString("FMATERIALID.FNumber+%3D+%27MAT-001%27")))
                .andExpect(content().string(containsString("FDocumentStatus+%3D+%27C%27")))
                .andExpect(content().string(containsString("%22Limit%22%3A200")))
                .andRespond(withSuccess("""
                        [[310119,"YXN.069.001.1012_V1","C","MAT-001","Parent Product","Spec P","A001.02.070.105","Lace Wire","0.66","2","1"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeBomLine> lines = client.fetchApprovedBomByParentMaterialNumber(properties, "MAT-001");

        assertEquals(1, lines.size());
        assertEquals("310119", lines.get(0).getFid());
        assertEquals("YXN.069.001.1012_V1", lines.get(0).getBomVersion());
        assertEquals("MAT-001", lines.get(0).getParentMaterialNumber());
        assertEquals("A001.02.070.105", lines.get(0).getChildMaterialNumber());
        assertEquals(new BigDecimal("2"), lines.get(0).getNumerator());
        assertEquals(new BigDecimal("1"), lines.get(0).getDenominator());
        server.verify();
    }

    @Test
    void fetchApprovedBomByParentMaterialNumber_failsWhenUsageFieldIsInvalid() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeBomClientImpl client = new ErpKingdeeBomClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com", 200);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("""
                        [[310119,"YXN.069.001.1012_V1","C","MAT-001","Parent Product","Spec P","A001.02.070.105","Lace Wire","0.66","bad","1"]]
                        """, MediaType.APPLICATION_JSON));

        assertThrows(RuntimeException.class,
                () -> client.fetchApprovedBomByParentMaterialNumber(properties, "MAT-001"));
        server.verify();
    }

    @Test
    void fetchBomLinesModifiedBetween_usesModifyTimeWindowAndStableOrder() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeBomClientImpl client = new ErpKingdeeBomClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com", 200);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("ENG_BOM")))
                .andExpect(content().string(containsString("FModifyDate")))
                .andExpect(content().string(containsString("FDocumentStatus+%3D+%27C%27")))
                .andExpect(content().string(containsString("FModifyDate+%3E%3D+%272026-06-12+08%3A00%3A00%27")))
                .andExpect(content().string(containsString("FModifyDate+%3C+%272026-06-12+09%3A00%3A00%27")))
                .andExpect(content().string(containsString("FModifyDate+ASC")))
                .andRespond(withSuccess("""
                        [[310119,"YXN.069.001.1012_V1","C","MAT-001","Parent Product","Spec P","A001.02.070.105","Lace Wire","0.66","2","1","2026-06-12T08:30:00"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeBomLine> lines = client.fetchBomLinesModifiedBetween(properties,
                LocalDateTime.of(2026, 6, 12, 8, 0),
                LocalDateTime.of(2026, 6, 12, 9, 0));

        assertEquals(1, lines.size());
        assertEquals("YXN.069.001.1012_V1", lines.get(0).getBomVersion());
        assertEquals(LocalDateTime.of(2026, 6, 12, 8, 30), lines.get(0).getSourceModifyTime());
        server.verify();
    }

    private static ErpKingdeeProperties buildProperties(String baseUrl, int queryLimit) {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl(baseUrl);
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-password");
        properties.setLcid(2052);
        properties.getBom().setQueryLimit(queryLimit);
        return properties;
    }

}
