package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeProductionOrderFullHistoryTest {

    private static final String BASE = "https://k3.example.com/K3Cloud/";
    private static final String QUERY = BASE + "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final String ROW = "[310119,\"WO-001\",\"C\",\"2020-01-01T00:00:00\",\"MAT-001\",\"Material\",\"Spec\",12,\"\",\"\",\"\",\"5\",\"kg\",\"kg\",\"\",\"\",\"\",\"\",\"\",\"100\",\"Org\",false,\"\",\"1\"]";

    @Test
    void fullHistory_readsPastConfiguredCapThroughLastShortPage() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(template);
        login(server);
        server.expect(requestTo(QUERY))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andExpect(content().string(containsString("%22Limit%22%3A1000")))
                .andExpect(content().string(containsString("FID+DESC%2CFTreeEntity_FEntryID+ASC")))
                .andExpect(content().string(not(containsString("FDate+%3E"))))
                .andRespond(withSuccess(page(1000), MediaType.APPLICATION_JSON));
        server.expect(requestTo(QUERY))
                .andExpect(content().string(containsString("%22StartRow%22%3A1000")))
                .andExpect(content().string(containsString("%22Limit%22%3A1000")))
                .andRespond(withSuccess("[" + ROW.replace("WO-001", "WO-1001") + "]", MediaType.APPLICATION_JSON));

        var orders = client.fetchProductionOrders(properties());

        assertEquals(1001, orders.size());
        assertEquals("WO-1001", orders.get(1000).getBillNo());
        assertEquals(LocalDate.of(2020, 1, 1).atStartOfDay(), orders.get(1000).getBillDate());
        server.verify();
    }

    @Test
    void fullHistory_readsEmptyTerminalPageAfterExactPageBoundary() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(template);
        login(server);
        server.expect(requestTo(QUERY)).andRespond(withSuccess(page(1000), MediaType.APPLICATION_JSON));
        server.expect(requestTo(QUERY))
                .andExpect(content().string(containsString("%22StartRow%22%3A1000")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(1000, client.fetchProductionOrders(properties()).size());
        server.verify();
    }

    @Test
    void datedInitialSync_retainsItsConfiguredCap() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(template);
        login(server);
        server.expect(requestTo(QUERY))
                .andExpect(content().string(containsString("%22Limit%22%3A2")))
                .andExpect(content().string(containsString("FDate+%3E%3D+%272020-01-01%27")))
                .andRespond(withSuccess(page(2), MediaType.APPLICATION_JSON));

        assertEquals(2, client.fetchProductionOrdersByBillDateRange(properties(),
                LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)).size());
        server.verify();
    }

    private static String page(int count) {
        return "[" + String.join(",", Collections.nCopies(count, ROW)) + "]";
    }

    private static ErpKingdeeProperties properties() {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com");
        properties.setAcctId("test-account");
        properties.setUsername("test-user");
        properties.setPassword("test-password");
        properties.setLcid(2052);
        properties.getProductionOrder().setQueryLimit(2);
        return properties;
    }

    private static void login(MockRestServiceServer server) {
        server.expect(requestTo(BASE + "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=test; Path=/"));
    }
}
