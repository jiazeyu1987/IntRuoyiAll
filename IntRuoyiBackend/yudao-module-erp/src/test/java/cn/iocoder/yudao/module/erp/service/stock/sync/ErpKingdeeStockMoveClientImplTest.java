package cn.iocoder.yudao.module.erp.service.stock.sync;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeStockMoveClientImplTest {
    @Test void repeatedEntryAcrossPagesIsStoredOnceWithoutLosingOtherEntries() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        var properties = properties();
        expectLogin(server);
        List<List<Object>> firstPage = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) firstPage.add(row("100", Integer.toString(i), 1));
        expectPage(server, 0, firstPage);
        expectPage(server, 1000, List.of(row("100", "1000", 1), row("100", "1001", 2), row("101", "1000", 3)));

        var moves = new ErpKingdeeStockMoveClientImpl(rest).fetchStockMoves(properties);

        assertEquals(2, moves.size());
        assertEquals(1001, moves.get(0).getLines().size());
        assertEquals("1001", moves.get(0).getLines().get(1000).getEntryId());
        assertEquals(2, moves.get(0).getLines().get(1000).getQuantity().intValueExact());
        assertEquals(1, moves.get(1).getLines().size());
        assertEquals(3, moves.get(1).getLines().get(0).getQuantity().intValueExact());
        server.verify();
    }

    @Test void conflictingDuplicateEntryFailsWithItsSourceIdentity() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        expectLogin(server);
        expectPage(server, 0, List.of(row("304775", "668786", 1), row("304775", "668786", 2)));

        var error = assertThrows(RuntimeException.class,
                () -> new ErpKingdeeStockMoveClientImpl(rest).fetchStockMoves(properties()));

        assertTrue(error.getMessage().contains("conflicting duplicate entry: FID=304775, FEntryID=668786"));
        server.verify();
    }

    private static ErpKingdeeProperties properties() {
        var properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://erp.example.com");
        properties.setAcctId("test"); properties.setUsername("test"); properties.setPassword("test");
        properties.setLcid(2052);
        return properties;
    }

    private static List<Object> row(String fid, String entryId, int quantity) {
        return List.of(fid, "MOVE-" + fid, "2000-10-01 00:00:00", "C", "GENERAL", "NORMAL", "",
                entryId, "MAT-1", "物料", "规格", "个", quantity,
                "FROM", "调出仓", "TO", "调入仓", "", "", "LOT");
    }

    private static void expectLogin(MockRestServiceServer server) {
        server.expect(requestTo("https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
    }

    private static void expectPage(MockRestServiceServer server, int startRow, List<List<Object>> rows) {
        server.expect(requestTo("https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("%22StartRow%22%3A" + startRow)))
                .andExpect(content().string(containsString(URLEncoder.encode(
                        "\"OrderString\":\"FID ASC,FBillEntry_FEntryID ASC\"", StandardCharsets.UTF_8))))
                .andRespond(withSuccess(JsonUtils.toJsonString(rows), MediaType.APPLICATION_JSON));
    }

    @Test void fullSyncReadsAllEntriesBeyondTheFormerFiveThousandRowLimit() {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        var properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://erp.example.com");
        properties.setAcctId("test"); properties.setUsername("test"); properties.setPassword("test");
        properties.setLcid(2052);
        server.expect(requestTo("https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE,"session=test"));
        for (int page = 0; page < 6; page++) {
            List<List<Object>> rows = new ArrayList<>();
            int count = page == 5 ? 1 : 1000;
            for (int i = 0; i < count; i++) {
                rows.add(List.of("100", "MOVE-100", "2000-10-01 00:00:00", "C", "GENERAL", "NORMAL", "",
                        Integer.toString(page * 1000 + i + 1), "MAT-1", "物料", "规格", "个", 1,
                        "FROM", "调出仓", "TO", "调入仓", "", "", "LOT"));
            }
            server.expect(requestTo("https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                    .andExpect(content().string(containsString("%22StartRow%22%3A" + page * 1000)))
                    .andExpect(content().string(containsString("FBillEntry_FEntryID+ASC")))
                    .andExpect(content().string(containsString(URLEncoder.encode("\"FilterString\":\"(FBillNo <> '')\"", StandardCharsets.UTF_8))))
                    .andRespond(withSuccess(JsonUtils.toJsonString(rows), MediaType.APPLICATION_JSON));
        }
        var moves = new ErpKingdeeStockMoveClientImpl(rest).fetchStockMoves(properties);
        assertEquals(1,moves.size());
        assertEquals(2000,moves.get(0).getBillDate().getYear());
        assertEquals(5001,moves.get(0).getLines().size());
        assertEquals("5001",moves.get(0).getLines().get(5000).getEntryId());
        server.verify();
    }
}
