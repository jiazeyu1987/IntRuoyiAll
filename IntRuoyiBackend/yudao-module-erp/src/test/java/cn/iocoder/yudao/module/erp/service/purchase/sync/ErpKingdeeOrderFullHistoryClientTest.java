package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeOrderFullHistoryClientTest {
    private static final String BASE = "https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.";

    @ParameterizedTest
    @CsvSource({"true,1000", "true,1001", "false,1000", "false,1001"})
    void fullHistoryReadsEveryPageWithoutDateFilterAndKeepsSplitDocument(boolean purchase, int rows) {
        var properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com");
        properties.setAcctId("acct");
        properties.setUsername("user");
        properties.setPassword("test-password");
        properties.setLcid(2052);
        properties.getPurchaseOrder().setPurchaseOrgNumber("881");
        properties.getPurchaseOrder().setQueryDays(365);
        properties.getPurchaseOrder().setQueryLimit(1000);
        properties.getSaleOrder().setQueryDays(365);
        properties.getSaleOrder().setQueryLimit(1000);
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(BASE + "AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        String baseFilter = purchase ? "FPurchaseOrgId.FNumber = '881'"
                : "(FBillNo <> '') and (FMaterialId.FNumber <> '')";
        String entryField = purchase ? "FPOOrderEntry_FEntryID" : "FSaleOrderEntry_FEntryID";
        expectHeader(server, "(" + baseFilter + ") and FID > 0", "[[10001,\"2026-01-01 09:00:00\"]]");
        for (int offset = 0; offset <= rows; offset += 1000) {
            int start = offset;
            var page = new ArrayList<List<Object>>();
            for (int i = offset; i < Math.min(rows, offset + 1000); i++) {
                page.add(purchase
                        ? List.of(10001, "PO20200101", "2020-01-01T00:00:00", "C", "A", "A", "881",
                        "SUP-001", "Supplier", "MAT-001", "Material", 1, 10, 0, "remark", "PCS",
                        0, 10, 10, 0, 0, rows * 10, 0, rows * 10, String.valueOf(i + 1), "PCS", 1, "2026-01-01 09:00:00")
                        : List.of(10001, "SO20200101", "2020-01-01T00:00:00", "C", "A", "A",
                        "C001", "Customer", "MAT-001", "Material", 1, 10, 10, 0, 10, "PCS",
                        0, 10, "remark", 0, 0, rows * 10, 0, rows * 10, String.valueOf(i + 1), "remark", "PCS", 1, "2026-01-01 09:00:00"));
            }
            server.expect(requestTo(BASE + "DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                    .andExpect(request -> {
                        String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
                        String query = URLDecoder.decode(body.substring(body.indexOf('=') + 1), StandardCharsets.UTF_8);
                        var json = JsonUtils.parseTree(query);
                        assertEquals(0, json.path("StartRow").asInt());
                        assertEquals(1000, json.path("Limit").asInt());
                        assertEquals("(" + baseFilter + ") and FID = 10001 and " + entryField + " > " + start,
                                json.path("FilterString").asText());
                        assertEquals(entryField + " ASC", json.path("OrderString").asText());
                        assertTrue(json.path("FieldKeys").asText().endsWith(",FModifyDate"));
                    })
                    .andRespond(withSuccess(JsonUtils.toJsonString(page), MediaType.APPLICATION_JSON));
            if (page.size() < 1000) break;
        }
        expectHeader(server, "(" + baseFilter + ") and FID = 10001", "[[10001,\"2026-01-01 09:00:00\"]]");
        expectHeader(server, "(" + baseFilter + ") and FID > 10001", "[]");
        if (purchase) {
            var orders = new ErpKingdeePurchaseOrderClientImpl(rest).fetchAllPurchaseOrders(properties);
            assertEquals(1, orders.size());
            assertEquals(2020, orders.get(0).getBillDate().getYear());
            assertEquals(rows, orders.get(0).getLines().size());
            assertEquals(String.valueOf(rows), orders.get(0).getLines().get(rows - 1).getEntryId());
        } else {
            var orders = new ErpKingdeeSaleOrderClientImpl(rest).fetchAllSaleOrders(properties);
            assertEquals(1, orders.size());
            assertEquals(2020, orders.get(0).getBillDate().getYear());
            assertEquals(rows, orders.get(0).getLines().size());
            assertEquals(String.valueOf(rows), orders.get(0).getLines().get(rows - 1).getEntryId());
        }
        server.verify();
    }
    private static void expectHeader(MockRestServiceServer server, String filter, String response) {
        server.expect(requestTo(BASE + "DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
                    var query = JsonUtils.parseTree(URLDecoder.decode(body.substring(body.indexOf('=') + 1),
                            StandardCharsets.UTF_8));
                    assertEquals(0, query.path("StartRow").asInt());
                    assertEquals(1, query.path("Limit").asInt());
                    assertEquals("FID,FModifyDate", query.path("FieldKeys").asText());
                    assertEquals("FID ASC", query.path("OrderString").asText());
                    assertEquals(filter, query.path("FilterString").asText());
                })
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

}
