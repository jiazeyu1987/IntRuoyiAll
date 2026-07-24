package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeSaleOrderClientImplTest {

    @Test
    void fetchSaleOrders_groupsRowsByFid() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeSaleOrderClientImpl client = new ErpKingdeeSaleOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com/K3Cloud");

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("username=kingdee-user")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("SAL_SaleOrder")))
                .andExpect(content().string(containsString("%22Limit%22%3A500")))
                .andExpect(content().string(containsString("FBillNo+%3C%3E+%27%27")))
                .andExpect(content().string(containsString("FMaterialId.FNumber+%3C%3E+%27%27")))
                .andRespond(withSuccess("""
                        [[348963,"908XSDD00103","2026-03-12T00:00:00","C","A","A","C000637","重庆普傲医疗器械有限公司","MAT-001","产品A",10,840.7,950,13,9500],
                         [348963,"908XSDD00103","2026-03-12T00:00:00","C","A","A","C000637","重庆普傲医疗器械有限公司","MAT-002","产品B",2,10,11.3,13,22.6]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeSaleOrder> orders = client.fetchSaleOrders(properties);

        assertEquals(1, orders.size());
        assertEquals("348963", orders.get(0).getFid());
        assertEquals("908XSDD00103", orders.get(0).getBillNo());
        assertEquals(2, orders.get(0).getLines().size());
        assertEquals("MAT-001", orders.get(0).getLines().get(0).getMaterialNumber());
        assertEquals("MAT-002", orders.get(0).getLines().get(1).getMaterialNumber());
        server.verify();
    }

    @Test
    void fetchSaleOrdersModifiedBetween_usesModifyTimeWindowAndParsesSourceModifyTime() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeSaleOrderClientImpl client = new ErpKingdeeSaleOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com/K3Cloud");

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("FModifyDate")))
                .andExpect(content().string(containsString("FCloseStatus")))
                .andExpect(content().string(containsString("FCancelStatus")))
                .andExpect(content().string(containsString("FModifyDate+%3E%3D+%272026-01-01+08%3A30%3A00%27")))
                .andExpect(content().string(containsString("FModifyDate+%3C+%272026-01-02+08%3A30%3A00%27")))
                .andExpect(content().string(containsString("FModifyDate+ASC")))
                .andExpect(content().string(containsString("FBillNo+%3C%3E+%27%27")))
                .andRespond(withSuccess("""
                        [[348963,"908XSDD00103","2026-03-12T00:00:00","C","B","A","C000637","重庆普傲医疗器械有限公司","MAT-001","产品A",10,840.7,950,13,9500,"2026-01-01 09:00:00"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeSaleOrder> orders = client.fetchSaleOrdersModifiedBetween(properties,
                LocalDateTime.of(2026, 1, 1, 8, 30, 0),
                LocalDateTime.of(2026, 1, 2, 8, 30, 0));

        assertEquals(1, orders.size());
        assertEquals("348963", orders.get(0).getFid());
        assertEquals("B", orders.get(0).getCloseStatus());
        assertEquals("A", orders.get(0).getCancelStatus());
        assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0, 0), orders.get(0).getSourceModifyTime());
        server.verify();
    }

    private static ErpKingdeeProperties buildProperties(String baseUrl) {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl(baseUrl);
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-password");
        properties.setLcid(2052);
        properties.getSaleOrder().setQueryDays(365);
        properties.getSaleOrder().setQueryLimit(500);
        return properties;
    }

}
