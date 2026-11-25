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
    void sourceDraftWithUnselectedSupplierMaterialAndUnitsIsRetained() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        String base = "https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.";
        server.expect(requestTo(base + "AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FID+%3E+0")))
                .andRespond(withSuccess("[[158553, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FPOOrderEntry_FEntryID+%3E+0")))
                .andRespond(withSuccess("[[158553, \"test\", \"2023-02-13T00:00:00\", \"Z\", \"A\", \"A\", \"881\", null, null, null, null, 0, 0, 0, null, null, 0, 0, 0, 0, 0, 0, 0, 0, \"1\", null, 0, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("%22FieldKeys%22%3A%22FID%2CFModifyDate%22")))
                .andRespond(withSuccess("[[158553, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FID+%3E+158553")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var orders = new ErpKingdeePurchaseOrderClientImpl(rest).fetchAllPurchaseOrders(buildConfiguredProperties());
        assertEquals(1, orders.size());
        assertEquals("158553", orders.get(0).getFid());
        assertEquals("Z", orders.get(0).getDocumentStatus());
        org.junit.jupiter.api.Assertions.assertNull(orders.get(0).getSupplierNumber());
        assertEquals(1, orders.get(0).getLines().size());
        org.junit.jupiter.api.Assertions.assertNull(orders.get(0).getLines().get(0).getMaterialNumber());
        org.junit.jupiter.api.Assertions.assertNull(orders.get(0).getLines().get(0).getUnitName());
        server.verify();
    }

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

        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FID+%3E+0")))
                .andRespond(withSuccess("[[10001, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FPOOrderEntry_FEntryID+%3E+0")))
                .andRespond(withSuccess("[[10001, \"PO20260512001\", \"2026-05-12T09:30:00\", \"C\", \"A\", \"A\", \"881\", \"SUP-001\", \"Supplier A\", \"MAT-001\", \"Material A\", 3.5, 15.2, 13, \"Line remark\", \"PCS\", 6.92, 53.2, 60.12, 0, 0, 53.2, 6.92, 60.12, \"100\", \"PCS\", 3.5, \"2026-01-01 09:00:00\"], [10001, \"PO20260512001\", \"2026-05-12T09:30:00\", \"C\", \"A\", \"A\", \"881\", \"SUP-001\", \"Supplier A\", \"MAT-002\", \"Material B\", 1, 20, 0, \"Second line\", \"PCS\", 6.92, 53.2, 60.12, 0, 0, 53.2, 6.92, 60.12, \"101\", \"PCS\", 1, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("%22FieldKeys%22%3A%22FID%2CFModifyDate%22")))
                .andRespond(withSuccess("[[10001, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FID+%3E+10001")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

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
                .andExpect(content().string(containsString("FID+%3E+0")))
                .andRespond(withSuccess("[[10001, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("FPOOrderEntry_FEntryID+%3E+0")))
                .andRespond(withSuccess("""
[[10001, "PO20260512001", "2026-05-12T09:30:00", "C", "B", "A", "881", "SUP-001", "Supplier A", "MAT-001", "Material A", 3.5, 15.2, 13, "Line remark", "PCS", 6.92, 53.2, 60.12, 0, 0, 53.2, 6.92, 60.12, "100", "PCS", 3.5, "2026-01-01 09:00:00"]]
""", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("%22FieldKeys%22%3A%22FID%2CFModifyDate%22")))
                .andRespond(withSuccess("[[10001, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("FID+%3E+10001")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

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


    @Test
    void incrementalReadsAllPagesWhenConfiguredLimitFallsInsideOneDocument() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        var properties = buildConfiguredProperties(); properties.getPurchaseOrder().setQueryLimit(1);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        String url = "https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("FID+%3E+0")))
                .andRespond(withSuccess("[[10001, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("FPOOrderEntry_FEntryID+%3E+0")))
                .andRespond(withSuccess("[[10001, \"PO20260512001\", \"2026-05-12T09:30:00\", \"C\", \"B\", \"A\", \"881\", \"SUP-001\", \"Supplier A\", \"MAT-001\", \"Material A\", 3.5, 15.2, 13, \"Line remark\", \"PCS\", 6.92, 53.2, 60.12, 0, 0, 53.2, 6.92, 60.12, \"101\", \"PCS\", 3.5, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("FPOOrderEntry_FEntryID+%3E+101")))
                .andRespond(withSuccess("[[10001, \"PO20260512001\", \"2026-05-12T09:30:00\", \"C\", \"B\", \"A\", \"881\", \"SUP-001\", \"Supplier A\", \"MAT-001\", \"Material A\", 3.5, 15.2, 13, \"Line remark\", \"PCS\", 6.92, 53.2, 60.12, 0, 0, 53.2, 6.92, 60.12, \"102\", \"PCS\", 3.5, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("FPOOrderEntry_FEntryID+%3E+102")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("%22FieldKeys%22%3A%22FID%2CFModifyDate%22")))
                .andRespond(withSuccess("[[10001, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("FID+%3E+10001")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var rows = new ErpKingdeePurchaseOrderClientImpl(rest).fetchPurchaseOrdersModifiedBetween(properties,
                LocalDateTime.of(2026,1,1,0,0),LocalDateTime.of(2026,1,2,0,0));
        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).getLines().size());
        assertEquals("102", rows.get(0).getLines().get(1).getEntryId());
        server.verify();
    }

    @Test void fullSyncReadsRemainingEntriesBeyondConfiguredPageSize() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        var properties = buildConfiguredProperties();
        properties.getPurchaseOrder().setQueryLimit(1);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        String url = "https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FID+%3E+0")))
                .andRespond(withSuccess("[[10001, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FPOOrderEntry_FEntryID+%3E+0")))
                .andRespond(withSuccess("[[10001, \"PO20260512001\", \"2026-05-12T09:30:00\", \"C\", \"A\", \"A\", \"881\", \"SUP-001\", \"Supplier A\", \"MAT-001\", \"Material A\", 3.5, 15.2, 13, \"Line remark\", \"PCS\", 6.92, 53.2, 60.12, 0, 0, 53.2, 6.92, 60.12, \"101\", \"PCS\", 3.5, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FPOOrderEntry_FEntryID+%3E+101")))
                .andRespond(withSuccess("[[10001, \"PO20260512001\", \"2026-05-12T09:30:00\", \"C\", \"A\", \"A\", \"881\", \"SUP-001\", \"Supplier A\", \"MAT-001\", \"Material A\", 3.5, 15.2, 13, \"Line remark\", \"PCS\", 6.92, 53.2, 60.12, 0, 0, 53.2, 6.92, 60.12, \"102\", \"PCS\", 3.5, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FPOOrderEntry_FEntryID+%3E+102")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("%22FieldKeys%22%3A%22FID%2CFModifyDate%22")))
                .andRespond(withSuccess("[[10001, \"2026-01-01 09:00:00\"]]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FID+%3E+10001")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var orders = new ErpKingdeePurchaseOrderClientImpl(rest).fetchPurchaseOrders(properties);
        assertEquals(1, orders.size());
        assertEquals(2, orders.get(0).getLines().size());
        server.verify();
    }
}
