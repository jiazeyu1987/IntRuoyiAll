package cn.iocoder.yudao.module.erp.service.production.sync;

import cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeProductionPickListClientImplTest {

    @Test
    void fetchProductionPickLists_usesExplicitOneYearWindowAndSmallerPageSize() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionPickListClientImpl client =
                new ErpKingdeeProductionPickListClientImpl(restTemplate);

        expectLogin(server);
        server.expect(requestTo(queryUrl()))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString(
                        "FDate+%3E%3D+%272025-08-22%27")))
                .andExpect(content().string(containsString(
                        "FDate+%3C+%272026-08-23%27")))
                .andExpect(content().string(containsString("%22Limit%22%3A200")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionPickList> rows = client.fetchProductionPickLists(
                buildProperties(),
                LocalDateTime.of(2025, 8, 22, 0, 0),
                LocalDateTime.of(2026, 8, 22, 13, 0));

        assertTrue(rows.isEmpty());
        server.verify();
    }

    @Test
    void fetchProductionPickListsModifiedBetween_queriesPrdPickMtrlAndParsesHeaderAndLine() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionPickListClientImpl client =
                new ErpKingdeeProductionPickListClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties();

        expectLogin(server);
        server.expect(requestTo(queryUrl()))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("PRD_PickMtrl")))
                .andExpect(content().string(containsString("FEntity_FEntryID")))
                .andExpect(content().string(containsString("FActualQty")))
                .andExpect(content().string(containsString("FAppQty")))
                .andExpect(content().string(containsString("FMoBillNo")))
                .andExpect(content().string(containsString("FPPBomBillNo")))
                .andExpect(content().string(not(containsString("FDeptId"))))
                .andExpect(content().string(not(containsString("FPPBomEntrySeq"))))
                .andExpect(content().string(not(containsString("FWorkShopId"))))
                .andExpect(content().string(containsString("FEntryWorkShopId")))
                .andExpect(content().string(containsString(
                        "FModifyDate+%3E%3D+%272026-08-13+08%3A00%3A00%27")))
                .andExpect(content().string(containsString(
                        "FModifyDate+%3C+%272026-08-13+09%3A00%3A00%27")))
                .andRespond(withSuccess("""
                        [[1001,"PICK001","2026-08-13 07:30:00","C","881","库存组织",
                        "881","生产组织","OWNER","货主","备注",2001,
                        "MAT001","物料一","规格一","支",6,6,8,"WH1","原料仓","LOC1",
                        "库位一","LOT1","MO001",1,"PPBOM001","WS1","组装车间",
                        "AVL","可用","08/13/2026 08:30:00"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionPickList> rows =
                client.fetchProductionPickListsModifiedBetween(properties,
                        LocalDateTime.of(2026, 8, 13, 8, 0),
                        LocalDateTime.of(2026, 8, 13, 9, 0));

        assertEquals(1, rows.size());
        ErpKingdeeProductionPickList pickList = rows.get(0);
        assertEquals("PRD_PickMtrl", ErpKingdeeProductionPickList.FORM_ID);
        assertEquals("1001", pickList.getFid());
        assertEquals("PICK001", pickList.getBillNo());
        assertEquals(LocalDateTime.of(2026, 8, 13, 7, 30), pickList.getBillDate());
        assertEquals(LocalDateTime.of(2026, 8, 13, 8, 30),
                pickList.getSourceModifyTime());
        assertEquals(1, pickList.getLines().size());
        ErpKingdeeProductionPickList.Line line = pickList.getLines().get(0);
        assertEquals("2001", line.getEntryId());
        assertEquals("MAT001", line.getMaterialNumber());
        assertEquals(new BigDecimal("6"), line.getActualQuantity());
        assertEquals(new BigDecimal("8"), line.getRequestedQuantity());
        assertEquals("MO001", line.getProductionOrderNo());
        assertEquals("PPBOM001", line.getProductionMaterialListNo());
        assertEquals("WH1", line.getWarehouseNumber());
        assertEquals("WS1", line.getWorkshopNumber());
        assertEquals("组装车间", line.getWorkshopName());
        server.verify();
    }

    @Test
    void fetchProductionPickLists_whenKingdeeReturnsError_failsExplicitly() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionPickListClientImpl client =
                new ErpKingdeeProductionPickListClientImpl(restTemplate);

        expectLogin(server);
        server.expect(requestTo(queryUrl()))
                .andRespond(withSuccess("""
                        {"Result":{"ResponseStatus":{"IsSuccess":false,
                        "Errors":[{"Message":"没有生产领料单查看权限"}]}}}
                        """, MediaType.APPLICATION_JSON));

        cn.iocoder.yudao.framework.common.exception.ServiceException exception =
                assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                        () -> client.fetchProductionPickLists(
                                buildProperties(),
                                LocalDateTime.of(2025, 8, 22, 0, 0),
                                LocalDateTime.of(2026, 8, 22, 13, 0)));
        assertEquals(ErrorCodeConstants.KINGDEE_PRODUCTION_PICK_LIST_REQUEST_FAIL.getCode(),
                exception.getCode());
        assertTrue(exception.getMessage().contains("没有生产领料单查看权限"));
        server.verify();
    }

    @Test
    void fetchProductionPickLists_whenKingdeeWrapsErrorInRows_failsExplicitly() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionPickListClientImpl client =
                new ErpKingdeeProductionPickListClientImpl(restTemplate);

        expectLogin(server);
        server.expect(requestTo(queryUrl()))
                .andRespond(withSuccess("""
                        [[{"Result":{"ResponseStatus":{"IsSuccess":false,
                        "Errors":[{"Message":"元数据中字段不存在"}]}}}]]
                        """, MediaType.APPLICATION_JSON));

        cn.iocoder.yudao.framework.common.exception.ServiceException exception =
                assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                        () -> client.fetchProductionPickLists(
                                buildProperties(),
                                LocalDateTime.of(2025, 8, 22, 0, 0),
                                LocalDateTime.of(2026, 8, 22, 13, 0)));
        assertEquals(ErrorCodeConstants.KINGDEE_PRODUCTION_PICK_LIST_REQUEST_FAIL.getCode(),
                exception.getCode());
        assertTrue(exception.getMessage().contains("元数据中字段不存在"));
        server.verify();
    }

    private static void expectLogin(MockRestServiceServer server) {
        server.expect(requestTo(
                        "https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));
    }

    private static String queryUrl() {
        return "https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    }

    private static ErpKingdeeProperties buildProperties() {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com");
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-password");
        properties.setLcid(2052);
        return properties;
    }

}
