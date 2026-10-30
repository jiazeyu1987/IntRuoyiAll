package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListClientImpl;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionReplenishmentListClientImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeCodeQueryTest {
    private static final String BASE = "https://k3.example.com/K3Cloud/";

    @Test void inventoryUsesMaterialCode() {
        verifyExactCode("FMATERIALID.FNumber", (rest, props) ->
                new ErpKingdeeInventoryClientImpl(rest).fetchInventoryRowsByCode(props, "AB'001"));
    }

    @Test void inventoryPreservesErpModificationTime() {
        List<?> rows = verifyExactCode("FMATERIALID.FNumber", (rest, props) ->
                new ErpKingdeeInventoryClientImpl(rest).fetchInventoryRowsByCode(props, "AB'001"),
                "[[\"AB'001\",\"物料\",\"规格\",0,\"WH01\",\"仓库\",\"LOT1\",\"件\",\"ORG01\",\"组织\",\"2026-10-30 10:00:00\"]]");
        assertEquals(LocalDateTime.of(2026, 10, 30, 10, 0),
                ((ErpKingdeeInventoryRow) rows.get(0)).getSourceModifyTime());
    }

    @Test void materialListUsesOwnBillNumber() {
        verifyExactCode("FBillNo", (rest, props) ->
                new ErpKingdeeProductionMaterialListClientImpl(rest).fetchProductionMaterialListsByCode(props, "AB'001"));
    }

    @Test void pickListUsesOwnBillNumber() {
        verifyExactCode("FBillNo", (rest, props) ->
                new ErpKingdeeProductionPickListClientImpl(rest).fetchByCode(props, "AB'001"));
    }

    @Test void replenishmentListUsesOwnBillNumber() {
        verifyExactCode("FBillNo", (rest, props) ->
                new ErpKingdeeProductionReplenishmentListClientImpl(rest).fetchByCode(props, "AB'001"));
    }

    private void verifyExactCode(String field,
            BiFunction<RestTemplate, ErpKingdeeProperties, ? extends List<?>> fetch) {
        assertTrue(verifyExactCode(field, fetch, "[]").isEmpty());
    }

    private List<?> verifyExactCode(String field,
            BiFunction<RestTemplate, ErpKingdeeProperties, ? extends List<?>> fetch, String response) {
        var rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        var props = new ErpKingdeeProperties();
        props.setBaseUrl("https://k3.example.com");
        props.setAcctId("acct");
        props.setUsername("user");
        props.setPassword("password");
        props.setLcid(2052);
        props.getProductionOrder().setQueryLimit(1000);
        server.expect(requestTo(BASE + "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        server.expect(requestTo(BASE + "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(request -> {
                    String body = URLDecoder.decode(((MockClientHttpRequest) request).getBodyAsString(), StandardCharsets.UTF_8);
                    assertTrue(body.contains(field + " = 'AB''001'"));
                    assertFalse(body.contains(" LIKE "));
                    assertFalse(body.contains("FModifyDate >="));
                    assertFalse(body.contains("FMoBillNo ="));
                })
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        List<?> rows = fetch.apply(rest, props);
        server.verify();
        return rows;
    }
}
