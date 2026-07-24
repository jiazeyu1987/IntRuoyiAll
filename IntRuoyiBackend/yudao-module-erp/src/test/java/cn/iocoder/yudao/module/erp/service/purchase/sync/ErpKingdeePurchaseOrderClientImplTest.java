package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeePurchaseOrderClientImplTest {

    @Test
    void fetchPurchaseOrders_usesConfiguredEndpointAndReturnsGroupedOrders() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeePurchaseOrderClientImpl client = new ErpKingdeePurchaseOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildConfiguredProperties();

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("acctID=acct"),
                        containsString("username=kingdee-user"),
                        containsString("lcid=2052"))))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(allOf(
                        containsString("PUR_PurchaseOrder"),
                        containsString("FPurchaseOrgId.FNumber"),
                        containsString("FCreateDate"))))
                .andRespond(withSuccess("""
                        [[10001,"PO20260512001","2026-05-12T09:30:00","C","A","A","881","SUP-001","Supplier A","MAT-001","Material A",3.5,15.2,13,"Line remark"],
                         [10001,"PO20260512001","2026-05-12T09:30:00","C","A","A","881","SUP-001","Supplier A","MAT-002","Material B",1,20,0,"Second line"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeePurchaseOrder> orders = client.fetchPurchaseOrders(properties);

        assertEquals(1, orders.size());
        assertEquals("10001", orders.get(0).getFid());
        assertEquals("PO20260512001", orders.get(0).getBillNo());
        assertEquals(2, orders.get(0).getLines().size());
        assertEquals("MAT-001", orders.get(0).getLines().get(0).getMaterialNumber());
        assertEquals("MAT-002", orders.get(0).getLines().get(1).getMaterialNumber());
        server.verify();
    }

    @Test
    void fetchPurchaseOrders_acceptsHostStyleBaseUrlAndAppendsK3CloudPath() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeePurchaseOrderClientImpl client = new ErpKingdeePurchaseOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildConfiguredProperties();
        properties.setBaseUrl("https://k3.example.com");

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeePurchaseOrder> orders = client.fetchPurchaseOrders(properties);

        assertEquals(0, orders.size());
        server.verify();
    }

    @Test
    void fetchPurchaseOrdersModifiedBetween_usesModifyTimeWindowAndParsesSourceModifyTime() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeePurchaseOrderClientImpl client = new ErpKingdeePurchaseOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildConfiguredProperties();

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
                .andExpect(content().string(containsString("FPurchaseOrgId.FNumber+%3D+%27881%27")))
                .andRespond(withSuccess("""
                        [[10001,"PO20260512001","2026-05-12T09:30:00","C","B","A","881","SUP-001","Supplier A","MAT-001","Material A",3.5,15.2,13,"Line remark","2026-01-01 09:00:00"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeePurchaseOrder> orders = client.fetchPurchaseOrdersModifiedBetween(properties,
                LocalDateTime.of(2026, 1, 1, 8, 30, 0),
                LocalDateTime.of(2026, 1, 2, 8, 30, 0));

        assertEquals(1, orders.size());
        assertEquals("10001", orders.get(0).getFid());
        assertEquals("B", orders.get(0).getCloseStatus());
        assertEquals("A", orders.get(0).getCancelStatus());
        assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0, 0), orders.get(0).getSourceModifyTime());
        server.verify();
    }

    private static ErpKingdeeProperties buildConfiguredProperties() {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com/K3Cloud");
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-password");
        properties.setLcid(2052);

        ErpKingdeeProperties.PurchaseOrderProperties purchaseOrder = new ErpKingdeeProperties.PurchaseOrderProperties();
        purchaseOrder.setPurchaseOrgNumber("881");
        purchaseOrder.setQueryDays(1);
        purchaseOrder.setQueryLimit(200);
        properties.setPurchaseOrder(purchaseOrder);
        return properties;
    }

}
