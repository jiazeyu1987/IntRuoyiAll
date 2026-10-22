package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeProductionOrderExistenceTest {
    @Test void existenceChecksHaveNoDateStatusOrMaterialFilter() {
        var template = new RestTemplate(); var server = MockRestServiceServer.bindTo(template).build();
        expectLogin(server);
        server.expect(requestTo(queryUrl())).andExpect(content().string(org.hamcrest.Matchers.containsString("FID%2CFBillNo")))
                .andExpect(request -> {
                    String payload = URLDecoder.decode(((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsString(), StandardCharsets.UTF_8);
                    assertFalse(payload.contains("FDocumentStatus")); assertFalse(payload.contains("FDate"));
                    assertFalse(payload.contains("FStatus")); assertFalse(payload.contains("FMaterialId"));
                }).andRespond(withSuccess("[[101,\"MO-1\"],[102,\"MO-2\"]]", MediaType.APPLICATION_JSON));
        var result = new ErpKingdeeProductionOrderClientImpl(template).findExistingProductionOrderBillNos(properties(), List.of("MO-1","MO-2","MO-MISSING"));
        assertEquals(Set.of("MO-1","MO-2"), result); server.verify();
    }
    @Test void fullPagesAreReadUntilTheLastPage() {
        var template = new RestTemplate(); var server = MockRestServiceServer.bindTo(template).build(); expectLogin(server);
        String rows = java.util.stream.IntStream.range(0, 1000).mapToObj(i -> "["+(10000+i)+",\"MO-1\"]").collect(java.util.stream.Collectors.joining(",","[","]"));
        server.expect(requestTo(queryUrl())).andRespond(withSuccess(rows, MediaType.APPLICATION_JSON));
        server.expect(requestTo(queryUrl())).andExpect(content().string(org.hamcrest.Matchers.containsString("%22StartRow%22%3A1000")))
                .andRespond(withSuccess("[[20000,\"MO-2\"]]", MediaType.APPLICATION_JSON));
        assertEquals(Set.of("MO-1","MO-2"), new ErpKingdeeProductionOrderClientImpl(template).findExistingProductionOrderBillNos(properties(),List.of("MO-1","MO-2")));
        server.verify();
    }
    @Test void invalidResponseFailsInsteadOfReportingMissing() {
        var template = new RestTemplate(); var server = MockRestServiceServer.bindTo(template).build(); expectLogin(server);
        server.expect(requestTo(queryUrl())).andRespond(withSuccess("[[101,null]]",MediaType.APPLICATION_JSON));
        assertThrows(RuntimeException.class, () -> new ErpKingdeeProductionOrderClientImpl(template).findExistingProductionOrderBillNos(properties(),List.of("MO-1")));
        server.verify();
    }
    @Test void sourceBillNumberIsQueriedWithoutChangingWhitespace() {
        var template = new RestTemplate(); var server = MockRestServiceServer.bindTo(template).build();
        expectLogin(server);
        server.expect(requestTo(queryUrl())).andExpect(request -> {
            String payload = URLDecoder.decode(((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsString(), StandardCharsets.UTF_8);
            assertTrue(payload.contains("FBillNo = ' MO-1 '"));
        }).andRespond(withSuccess("[[101,\" MO-1 \"]]", MediaType.APPLICATION_JSON));
        assertEquals(Set.of(" MO-1 "), new ErpKingdeeProductionOrderClientImpl(template)
                .findExistingProductionOrderBillNos(properties(), List.of(" MO-1 ")));
        server.verify();
    }
    private static void expectLogin(MockRestServiceServer server) {
        server.expect(requestTo("https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON).header(HttpHeaders.SET_COOKIE,"session=test; Path=/"));
    }
    private static String queryUrl() { return "https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"; }
    private static ErpKingdeeProperties properties() {
        var p = new ErpKingdeeProperties(); p.setBaseUrl("https://erp.example.com");p.setAcctId("account");p.setUsername("user");p.setPassword("password");p.setLcid(2052);return p;
    }
}
