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
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeProductionPickListClientImplTest {

    private String queryError(String message) {
        return "[[{\"Result\":{\"ResponseStatus\":{\"IsSuccess\":false,\"ErrorCode\":500,\"Errors\":[{\"Message\":\""
                + message + "\"}]}}}]]";
    }

    @Test
    void deadlockRetriesTheSamePageAndEmitsNoDuplicateBatch() {
        var rest = new RestTemplate(); var server = MockRestServiceServer.bindTo(rest).build();
        expectLogin(server);
        var requests = new java.util.ArrayList<String>();
        var waits = new java.util.ArrayList<Integer>();
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3), requestTo(queryUrl()))
                .andExpect(request -> requests.add(request.getBody().toString()))
                .andRespond(request -> withSuccess(requests.size() < 3 ? queryError("已被选作死锁牺牲品") : "[]",
                        MediaType.APPLICATION_JSON).createResponse(request));
        var client = new ErpKingdeeProductionPickListClientImpl(rest) {
            @Override void waitBeforeRetry(int attempt) { waits.add(attempt); }
        };
        client.forEachProductionPickListBatch(buildProperties(), LocalDateTime.of(2026, 8, 13, 0, 0),
                LocalDateTime.of(2026, 8, 13, 23, 59), batch -> org.junit.jupiter.api.Assertions.fail("No successful data batch"));
        assertEquals(1, new java.util.HashSet<>(requests).size());
        assertEquals(List.of(1, 2), waits); server.verify();
    }

    @Test
    void exhaustedDeadlockFailsAfterThreeAttemptsWithOriginalSourceError() {
        var rest = new RestTemplate(); var server = MockRestServiceServer.bindTo(rest).build();
        expectLogin(server);
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3), requestTo(queryUrl()))
                .andRespond(withSuccess(queryError("已被选作死锁牺牲品"), MediaType.APPLICATION_JSON));
        var client = new ErpKingdeeProductionPickListClientImpl(rest) {
            @Override void waitBeforeRetry(int attempt) { }
        };
        var failure = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> client.fetchProductionPickLists(buildProperties(), LocalDateTime.of(2026,8,13,0,0),
                        LocalDateTime.of(2026,8,13,23,59)));
        assertTrue(failure.getMessage().contains("死锁牺牲品")); server.verify();
    }

    @Test
    void sourcePermissionFailureIsNeverRetried() {
        var rest = new RestTemplate(); var server = MockRestServiceServer.bindTo(rest).build();
        expectLogin(server);
        server.expect(requestTo(queryUrl())).andRespond(withSuccess(queryError("无权查询单据"), MediaType.APPLICATION_JSON));
        var failure = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> new ErpKingdeeProductionPickListClientImpl(rest).fetchProductionPickLists(buildProperties(),
                        LocalDateTime.of(2026,8,13,0,0), LocalDateTime.of(2026,8,13,23,59)));
        assertTrue(failure.getMessage().contains("无权查询单据")); server.verify();
    }

    @Test
    void streamingCommitsCompleteBillsBeforeReadingNextPageAndKeepsSplitBillIntact() {
        RestTemplate rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        expectLogin(server);
        List<List<ErpKingdeeProductionPickList>> batches = new java.util.ArrayList<>();
        var firstPage = new java.util.ArrayList<List<Object>>();
        for (int i = 0; i < 200; i++) firstPage.add(fullRow(i < 198 ? 1001 : 1002, 2001 + i));
        server.expect(requestTo(queryUrl()))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andRespond(withSuccess(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(firstPage),
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(queryUrl()))
                .andExpect(content().string(containsString("%22StartRow%22%3A200")))
                .andRespond(request -> {
                    assertEquals(1, batches.size());
                    assertEquals("1001", batches.get(0).get(0).getFid());
                    return withSuccess(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                            List.of(fullRow(1002, 2201))), MediaType.APPLICATION_JSON).createResponse(request);
                });

        new ErpKingdeeProductionPickListClientImpl(rest).forEachProductionPickListBatch(buildProperties(),
                LocalDateTime.of(2026, 8, 13, 0, 0), LocalDateTime.of(2026, 8, 13, 23, 59), batches::add);

        assertEquals(2, batches.size());
        assertEquals(198, batches.get(0).get(0).getLines().size());
        assertEquals("1002", batches.get(1).get(0).getFid());
        assertEquals(3, batches.get(1).get(0).getLines().size());
        server.verify();
    }

    @Test
    void streamingConsumerFailureStopsBeforeReadingNextPage() {
        RestTemplate rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        expectLogin(server);
        var page = new java.util.ArrayList<List<Object>>();
        for (int i = 0; i < 200; i++) page.add(fullRow(1001 + i, 2001 + i));
        server.expect(requestTo(queryUrl())).andRespond(withSuccess(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(page), MediaType.APPLICATION_JSON));
        IllegalStateException expected = new IllegalStateException("batch write failed");
        var actual = assertThrows(IllegalStateException.class,
                () -> new ErpKingdeeProductionPickListClientImpl(rest).forEachProductionPickListBatch(buildProperties(),
                        LocalDateTime.of(2026, 8, 13, 0, 0), LocalDateTime.of(2026, 8, 13, 23, 59),
                        batch -> { throw expected; }));
        org.junit.jupiter.api.Assertions.assertSame(expected, actual);
        server.verify();
    }

    private List<Object> fullRow(int fid, int entryId) {
        return java.util.Arrays.asList(fid, "PICK" + fid, "2026-08-13 07:30:00", "C",
                "881", "org", "881", "org", "OWNER", "owner", "remark", entryId,
                "MAT001", "material", "spec", "unit", 6, 6, 8, "WH1", "warehouse",
                "LOC1", "location", "LOT1", "MO001", 1, "PPBOM001", "WS1", "workshop", "AVL", "available", null);
    }

    @Test
    void readTimeoutRetriesSamePageWithoutChangingDataSource() {
        RestTemplate rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        expectLogin(server);
        server.expect(requestTo(queryUrl()))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("timeout"); });
        server.expect(requestTo(queryUrl()))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertTrue(new ErpKingdeeProductionPickListClientImpl(rest).fetchProductionPickLists(buildProperties(),
                LocalDateTime.of(1970,1,1,0,0),LocalDateTime.of(2026,11,17,12,0)).isEmpty());
        server.verify();
    }

    @Test
    void repeatedReadTimeoutFailsAfterThreeAttempts() {
        RestTemplate rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        expectLogin(server);
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3), requestTo(queryUrl()))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("timeout"); });
        assertThrows(org.springframework.web.client.ResourceAccessException.class,
                () -> new ErpKingdeeProductionPickListClientImpl(rest).fetchProductionPickLists(buildProperties(),
                        LocalDateTime.of(1970,1,1,0,0),LocalDateTime.of(2026,11,17,12,0)));
        server.verify();
    }

    @Test
    void nonTimeoutTransportFailureIsNotRetried() {
        RestTemplate rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        expectLogin(server);
        server.expect(requestTo(queryUrl()))
                .andRespond(request -> { throw new java.net.ConnectException("refused"); });
        assertThrows(org.springframework.web.client.ResourceAccessException.class,
                () -> new ErpKingdeeProductionPickListClientImpl(rest).fetchProductionPickLists(buildProperties(),
                        LocalDateTime.of(1970,1,1,0,0),LocalDateTime.of(2026,11,17,12,0)));
        server.verify();
    }

    @Test
    void allHistoryDoesNotIssueThousandsOfEmptyWeeklyQueries() {
        RestTemplate rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        var client = new ErpKingdeeProductionPickListClientImpl(rest);
        expectLogin(server);
        server.expect(requestTo(queryUrl()))
                .andExpect(content().string(containsString("FDate+%3E%3D+%271970-01-01%27")))
                .andExpect(content().string(containsString("FDate+%3C+%272026-11-18%27")))
                .andExpect(content().string(containsString("%22Limit%22%3A1%7D")))
                .andExpect(content().string(containsString("FDate+ASC%2CFID+ASC%2CFEntity_FEntryID+ASC")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertTrue(client.fetchProductionPickLists(buildProperties(),
                LocalDateTime.of(1970, 1, 1, 0, 0), LocalDateTime.of(2026, 11, 17, 12, 0)).isEmpty());
        server.verify();
    }

    @Test
    void allHistoryAdvancesByActualLargerPageAndKeepsTail() {
        RestTemplate rest = new RestTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        var client = new ErpKingdeeProductionPickListClientImpl(rest);
        expectLogin(server);
        var data = new java.util.ArrayList<List<Object>>();
        for (int i = 0; i < 1000; i++) {
            data.add(java.util.Arrays.asList(1001, "PICK001", "2026-08-13 07:30:00", "C",
                    "881", "org", "881", "org", "OWNER", "owner", "remark", 2001+i,
                    "MAT001", "material", "spec", "unit", 6,6,8,"WH1","warehouse",
                    "LOC1","location","LOT1","MO001",1,"PPBOM001","WS1","workshop","AVL","available",null));
        }
        server.expect(requestTo(queryUrl()))
                .andExpect(content().string(containsString("%22Limit%22%3A1%7D")))
                .andRespond(withSuccess(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(List.of(data.get(0))), MediaType.APPLICATION_JSON));
        server.expect(requestTo(queryUrl()))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andExpect(content().string(containsString("%22Limit%22%3A1000")))
                .andExpect(content().string(containsString("FDate+%3E%3D+%272026-08-13%27")))
                .andExpect(content().string(containsString("FDate+%3C+%272026-09-01%27")))
                .andRespond(withSuccess(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(data), MediaType.APPLICATION_JSON));
        var tail = new java.util.ArrayList<>(data.get(0));
        tail.set(11, 3001);
        server.expect(requestTo(queryUrl()))
                .andExpect(content().string(containsString("%22StartRow%22%3A1000")))
                .andRespond(withSuccess(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(List.of(tail)), MediaType.APPLICATION_JSON));
        for (String[] range : List.of(new String[]{"2026-09-01", "2026-10-01"},
                new String[]{"2026-10-01", "2026-11-01"}, new String[]{"2026-11-01", "2026-11-18"})) {
            server.expect(requestTo(queryUrl()))
                    .andExpect(content().string(containsString("FDate+%3E%3D+%27" + range[0] + "%27")))
                    .andExpect(content().string(containsString("FDate+%3C+%27" + range[1] + "%27")))
                    .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                    .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        }
        var rows = client.fetchProductionPickLists(buildProperties(),
                LocalDateTime.of(1970,1,1,0,0),LocalDateTime.of(2026,11,17,12,0));
        assertEquals(1,rows.size());
        assertEquals(1001,rows.get(0).getLines().size());
        assertEquals("3001",rows.get(0).getLines().get(1000).getEntryId());
        server.verify();
    }

    @Test
    void fetchProductionPickLists_usesExplicitOneYearWindowAndSmallerPageSize() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionPickListClientImpl client =
                new ErpKingdeeProductionPickListClientImpl(restTemplate);
        expectLogin(server);
        server.expect(manyTimes(), requestTo(queryUrl()))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
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
    void fetchProductionPickLists_splitsFullWindowIntoNonOverlappingDateRanges() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionPickListClientImpl client =
                new ErpKingdeeProductionPickListClientImpl(restTemplate);

        expectLogin(server);
        server.expect(requestTo(queryUrl()))
                .andExpect(content().string(containsString(
                        "FDate+%3E%3D+%272026-08-01%27")))
                .andExpect(content().string(containsString(
                        "FDate+%3C+%272026-08-08%27")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(queryUrl()))
                .andExpect(content().string(containsString(
                        "FDate+%3E%3D+%272026-08-08%27")))
                .andExpect(content().string(containsString(
                        "FDate+%3C+%272026-08-15%27")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionPickList> rows = client.fetchProductionPickLists(
                buildProperties(),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 14, 13, 0));

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
[[1001, "PICK001", "2026-08-13 07:30:00", "C", "881", "库存组织", "881", "生产组织", "OWNER", "货主", "备注", 2001, "MAT001", "物料一", "规格一", "支", 6, 6, 8, "WH1", "原料仓", "LOC1", "库位一", "LOT1", "MO001", 1, "PPBOM001", "WS1", "组装车间", "AVL", "可用", null, "08/13/2026 08:30:00"]]
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
[[{"Result": {"ResponseStatus": {"IsSuccess": false, "Errors": [{"Message": "元数据中字段不存在"}]}}}]]
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
