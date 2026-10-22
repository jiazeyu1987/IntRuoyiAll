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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeStockMoveClientImplTest {
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
                rows.add(List.of("100", "MOVE-100", "2026-10-01 00:00:00", "C", "GENERAL", "NORMAL", "",
                        Integer.toString(page * 1000 + i + 1), "MAT-1", "物料", "规格", "个", 1,
                        "FROM", "调出仓", "TO", "调入仓", "", "", "LOT"));
            }
            server.expect(requestTo("https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                    .andExpect(content().string(containsString("%22StartRow%22%3A" + page * 1000)))
                    .andExpect(content().string(containsString("FBillEntry_FEntryID+ASC")))
                    .andRespond(withSuccess(JsonUtils.toJsonString(rows), MediaType.APPLICATION_JSON));
        }
        var moves = new ErpKingdeeStockMoveClientImpl(rest).fetchStockMoves(properties);
        assertEquals(1,moves.size());
        assertEquals(5001,moves.get(0).getLines().size());
        assertEquals("5001",moves.get(0).getLines().get(5000).getEntryId());
        server.verify();
    }
}
