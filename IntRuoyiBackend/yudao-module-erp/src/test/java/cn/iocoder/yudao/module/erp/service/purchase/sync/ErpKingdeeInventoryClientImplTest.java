package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

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

class ErpKingdeeInventoryClientImplTest {

    @Test
    void fetchInventoryRows_parsesWarehouseAndOrgFields() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeInventoryClientImpl client = new ErpKingdeeInventoryClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("username=kingdee-user")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("STK_Inventory")))
                .andRespond(withSuccess("""
                        [["MAT-001","产品A","12ml",5.0,"CK001","璞霖仓","LOT001","支","892","上海璞霖医疗器械有限公司"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeInventoryRow> rows = client.fetchInventoryRows(properties);

        assertEquals(1, rows.size());
        assertEquals("MAT-001", rows.get(0).getMaterialNumber());
        assertEquals("CK001", rows.get(0).getWarehouseNumber());
        assertEquals("璞霖仓", rows.get(0).getWarehouseName());
        assertEquals("892", rows.get(0).getStockOrgNumber());
        server.verify();
    }

    @Test
    void fetchInventoryRowsModifiedBetween_usesModifyTimeWindowAndKeepsZeroQuantityRows() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeInventoryClientImpl client = new ErpKingdeeInventoryClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("FUpdateTime")))
                .andExpect(content().string(containsString("FUpdateTime+%3E%3D+%272026-01-01+08%3A30%3A00%27")))
                .andExpect(content().string(containsString("FUpdateTime+%3C+%272026-01-02+08%3A30%3A00%27")))
                .andExpect(content().string(containsString("FUpdateTime+ASC")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FBaseQty+%3E+0"))))
                .andRespond(withSuccess("""
                        [["MAT-001","产品A","12ml",0,"CK001","璞霖仓","LOT001","支","892","上海璞霖医疗器械有限公司","2026-01-01 09:00:00"]]
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("%22StartRow%22%3A1")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeeInventoryRow> rows = client.fetchInventoryRowsModifiedBetween(properties,
                LocalDateTime.of(2026, 1, 1, 8, 30, 0),
                LocalDateTime.of(2026, 1, 2, 8, 30, 0));

        assertEquals(1, rows.size());
        assertEquals("MAT-001", rows.get(0).getMaterialNumber());
        assertEquals(0, rows.get(0).getQuantity().signum());
        assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0, 0), rows.get(0).getSourceModifyTime());
        server.verify();
    }

    @Test
    void fetchInventoryRowsModifiedBetween_parsesKingdeeIsoUpdateTime() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeInventoryClientImpl client = new ErpKingdeeInventoryClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("""
                        [["MAT-001","产品A","12ml",0,"CK001","璞霖仓","LOT001","支","892","上海璞霖医疗器械有限公司","2026-03-23T10:44:38.64"]]
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeeInventoryRow> rows = client.fetchInventoryRowsModifiedBetween(properties,
                LocalDateTime.of(2026, 3, 23, 10, 44, 0),
                LocalDateTime.of(2026, 3, 23, 10, 45, 0));

        assertEquals(LocalDateTime.of(2026, 3, 23, 10, 44, 38, 640_000_000),
                rows.get(0).getSourceModifyTime());
        server.verify();
    }

    @Test
    void fetchInventoryRowsModifiedBetween_exposesKingdeeArrayWrappedErrorResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeInventoryClientImpl client = new ErpKingdeeInventoryClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("""
                        [[{"Result":{"ResponseStatus":{"IsSuccess":false,"Errors":[{"Message":"元数据中标识为FUpdateTime的字段不存在"}]}}}]]
                        """, MediaType.APPLICATION_JSON));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.fetchInventoryRowsModifiedBetween(properties,
                        LocalDateTime.of(2026, 1, 1, 8, 30, 0),
                        LocalDateTime.of(2026, 1, 2, 8, 30, 0)));

        org.assertj.core.api.Assertions.assertThat(ex.getMessage()).contains("FUpdateTime");
        server.verify();
    }

    private static ErpKingdeeProperties buildProperties(String baseUrl) {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl(baseUrl);
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-password");
        properties.setLcid(2052);
        return properties;
    }

}
