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

    private static String pageRow(String entryId, int quantity) {
        return "[348963,\"X'S\",\"2020-01-01T00:00:00\",\"C\",\"A\",\"A\",\"C001\",\"Customer\",\"MAT\",\"Material\","
                + quantity + ",10,11.3,13,113,\"PCS\",13,100,\"\",0,0,100,13,113,\""
                + entryId + "\",\"\",\"PCS\",10]";
    }

    @Test
    void identicalSourceEntryRepeatedAcrossPagesIsStoredOnce() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ValidateUser")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        for (int i = 0; i < 2; i++) {
            server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                    .andRespond(withSuccess("[" + pageRow("101", 10) + "]", MediaType.APPLICATION_JSON));
        }
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        var properties = buildProperties("https://k3.example.com/K3Cloud");
        properties.getSaleOrder().setQueryLimit(1);
        var orders = new ErpKingdeeSaleOrderClientImpl(rest).fetchSaleOrders(properties);
        assertEquals(1, orders.get(0).getLines().size());
        server.verify();
    }

    @Test
    void sameSourceEntryWithChangedFieldsFailsVisibly() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ValidateUser")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        for (int quantity : new int[]{10, 11}) {
            server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                    .andRespond(withSuccess("[" + pageRow("101", quantity) + "]", MediaType.APPLICATION_JSON));
        }
        var properties = buildProperties("https://k3.example.com/K3Cloud");
        properties.getSaleOrder().setQueryLimit(1);
        var error = org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> new ErpKingdeeSaleOrderClientImpl(rest).fetchSaleOrders(properties));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("entry changed during pagination"));
        server.verify();
    }

    @Test
    void targetedBillQueryEscapesQuoteAndReadsCompleteHistoricalBill() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ValidateUser")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FBillNo+%3D+%27X%27%27S%27")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FDate+%3E%3D"))))
                .andRespond(withSuccess("[" + pageRow("101", 10) + "]", MediaType.APPLICATION_JSON));
        var orders = new ErpKingdeeSaleOrderClientImpl(rest)
                .fetchSaleOrdersByBillNo(buildProperties("https://k3.example.com/K3Cloud"), "X'S");
        assertEquals("X'S", orders.get(0).getBillNo());
        assertEquals(2020, orders.get(0).getBillDate().getYear());
        server.verify();
    }

    @Test
    void historicalSourceWithoutPricingUnitKeepsNullAndSourcePrice() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andRespond(withSuccess("""
                [[110824,"881XSDD07479","2018-12-01T00:00:00","C","A","A","C001","Customer","MAT-001","Material",10,840.7,950,13,9500,"PCS",1093,8407,"",0,0,8407,1093,9500,"101","",null,0]]
                """, MediaType.APPLICATION_JSON));
        var orders = new ErpKingdeeSaleOrderClientImpl(rest).fetchAllSaleOrders(buildProperties("https://k3.example.com/K3Cloud"));
        assertEquals(1, orders.size());
        var line = orders.get(0).getLines().get(0);
        org.junit.jupiter.api.Assertions.assertNull(line.getPriceUnitName());
        assertEquals(new java.math.BigDecimal("840.7"), line.getPrice());
        assertEquals(java.math.BigDecimal.ZERO, line.getPriceUnitQuantity());
        server.verify();
    }

    @Test
    void malformedPageIncludesSourceErrorAndOffset() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andRespond(withSuccess("[[{\"Result\":{\"ResponseStatus\":{\"Errors\":[{\"Message\":\"source query failed\"}]}}}]]", MediaType.APPLICATION_JSON));
        var error = org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> new ErpKingdeeSaleOrderClientImpl(rest).fetchSaleOrders(buildProperties("https://k3.example.com/K3Cloud")));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("startRow=0"));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("source query failed"));
        server.verify();
    }

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
[[348963, "908XSDD00103", "2026-03-12T00:00:00", "C", "A", "A", "C000637", "重庆普傲医疗器械有限公司", "MAT-001", "产品A", 10, 840.7, 950, 13, 9500, "PCS", 1093, 8407, "ERP line remark", 0, 0, 8407, 1093, 9500, "100", "ERP header remark", "PCS", 10], [348963, "908XSDD00103", "2026-03-12T00:00:00", "C", "A", "A", "C000637", "重庆普傲医疗器械有限公司", "MAT-002", "产品B", 2, 10, 11.3, 13, 22.6, "PCS", 1093, 8407, "ERP line remark", 0, 0, 8407, 1093, 9500, "101", "ERP header remark", "PCS", 2]]
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
[[348963, "908XSDD00103", "2026-03-12T00:00:00", "C", "B", "A", "C000637", "重庆普傲医疗器械有限公司", "MAT-001", "产品A", 10, 840.7, 950, 13, 9500, "PCS", 1093, 8407, "ERP line remark", 0, 0, 8407, 1093, 9500, "100", "ERP header remark", "PCS", 10, "2026-01-01 09:00:00"]]
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

    @Test
    void fullHistoryUsesStableIdentityCursorAcrossOrderAndEntryBoundaries() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ValidateUser")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andRespond(withSuccess("[" + pageRow("101", 10) + "]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FID+%3C+348963+or+%28FID+%3D+348963+and+FSaleOrderEntry_FEntryID+%3E+101")))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andRespond(withSuccess("[" + pageRow("102", 10) + "]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FSaleOrderEntry_FEntryID+%3E+102")))
                .andRespond(withSuccess("[" + pageRow("1", 10).replace("348963", "348962") + "]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("FID+%3C+348962")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        var properties = buildProperties("https://k3.example.com/K3Cloud");
        properties.getSaleOrder().setQueryLimit(1);
        var orders = new ErpKingdeeSaleOrderClientImpl(rest).fetchAllSaleOrders(properties);
        assertEquals(2, orders.size());
        assertEquals(2, orders.get(0).getLines().size());
        assertEquals("348962", orders.get(1).getFid());
        server.verify();
    }

    @Test
    void fullHistoryRejectsSourceIgnoringTheIdentityCursor() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ValidateUser")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        for (int i=0;i<2;i++) server.expect(requestTo(org.hamcrest.Matchers.containsString("ExecuteBillQuery")))
                .andRespond(withSuccess("[" + pageRow("101", 10) + "]", MediaType.APPLICATION_JSON));
        var properties = buildProperties("https://k3.example.com/K3Cloud");
        properties.getSaleOrder().setQueryLimit(1);
        var error = org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> new ErpKingdeeSaleOrderClientImpl(rest).fetchAllSaleOrders(properties));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("source cursor did not advance"));
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


    @Test
    void incrementalReadsAllPagesWhenConfiguredLimitFallsInsideOneDocument() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        var properties = buildProperties("https://k3.example.com/K3Cloud"); properties.getSaleOrder().setQueryLimit(1);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        String url = "https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
        server.expect(requestTo(url)).andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andRespond(withSuccess("[[348963, \"908XSDD00103\", \"2026-03-12T00:00:00\", \"C\", \"B\", \"A\", \"C000637\", \"重庆普傲医疗器械有限公司\", \"MAT-001\", \"产品A\", 10, 840.7, 950, 13, 9500, \"PCS\", 1093, 8407, \"ERP line remark\", 0, 0, 8407, 1093, 9500, \"101\", \"ERP header remark\", \"PCS\", 10, \"2026-01-01 09:00:00\"]]",MediaType.APPLICATION_JSON));
        server.expect(requestTo(url)).andExpect(content().string(containsString("%22StartRow%22%3A1")))
                .andRespond(withSuccess("[[348963, \"908XSDD00103\", \"2026-03-12T00:00:00\", \"C\", \"B\", \"A\", \"C000637\", \"重庆普傲医疗器械有限公司\", \"MAT-001\", \"产品A\", 10, 840.7, 950, 13, 9500, \"PCS\", 1093, 8407, \"ERP line remark\", 0, 0, 8407, 1093, 9500, \"102\", \"ERP header remark\", \"PCS\", 10, \"2026-01-01 09:00:00\"]]",MediaType.APPLICATION_JSON));
        server.expect(requestTo(url)).andExpect(content().string(containsString("%22StartRow%22%3A2")))
                .andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        var rows = new ErpKingdeeSaleOrderClientImpl(rest).fetchSaleOrdersModifiedBetween(properties,
                LocalDateTime.of(2026,1,1,0,0),LocalDateTime.of(2026,1,2,0,0));
        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).getLines().size());
        assertEquals("102", rows.get(0).getLines().get(1).getEntryId());
        server.verify();
    }
}
