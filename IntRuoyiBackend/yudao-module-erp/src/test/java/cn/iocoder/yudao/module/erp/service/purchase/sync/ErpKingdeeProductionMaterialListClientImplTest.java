package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeProductionMaterialListClientImplTest {

    @Test
    void plainTextJsonIsSupported() {
        RestTemplate template = new ErpKingdeeRestTemplateConfiguration()
                .erpKingdeeProductionMaterialListRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        server.expect(requestTo(containsString("ValidateUser")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.TEXT_PLAIN)
                        .header(HttpHeaders.SET_COOKIE, "session=test; Path=/"));
        server.expect(requestTo(containsString("ExecuteBillQuery")))
                .andRespond(withSuccess("[]", MediaType.TEXT_PLAIN));
        new ErpKingdeeProductionMaterialListClientImpl(template).forEachProductionMaterialListPage(
                buildProperties(), page -> org.junit.jupiter.api.Assertions.fail("empty response must not emit a page"));
        server.verify();
    }

    @Test
    void responseBodyTimeoutReportsPageAndPreservesCause() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        server.expect(requestTo(containsString("ValidateUser")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.TEXT_PLAIN)
                        .header(HttpHeaders.SET_COOKIE, "session=test; Path=/"));
        SocketTimeoutException timeout = new SocketTimeoutException("Read timed out");
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3),
                requestTo(containsString("ExecuteBillQuery"))).andRespond(request -> {
            MockClientHttpResponse response = new MockClientHttpResponse(new InputStream() {
                private boolean started;

                @Override
                public int read() throws SocketTimeoutException {
                    if (!started) {
                        started = true;
                        return '[';
                    }
                    throw timeout;
                }
            }, HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
            return response;
        });
        RestClientException error = assertThrows(RestClientException.class,
                () -> new ErpKingdeeProductionMaterialListClientImpl(template) {
                    @Override void waitBeforeRetry(int attempt) { }
                }
                        .forEachProductionMaterialListPage(buildProperties(), page -> { }));
        assertTrue(error.getMessage().contains("生产用料清单读取超时"));
        assertTrue(error.getMessage().contains("StartRow=0"));
        assertEquals(timeout, error.getMostSpecificCause());
        assertTrue(error.getMessage().contains("连续3次尝试失败"));
        server.verify();
    }

    @Test
    void retriesOnlyFailedPageAndContinuesWithoutDuplicatingRows() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        java.util.concurrent.atomic.AtomicInteger committed = new java.util.concurrent.atomic.AtomicInteger();
        java.util.ArrayList<Integer> waits = new java.util.ArrayList<>();
        ErpKingdeeProductionMaterialListClientImpl client = new ErpKingdeeProductionMaterialListClientImpl(template) {
            @Override void waitBeforeRetry(int attempt) { waits.add(attempt); }
        };
        ErpKingdeeProperties properties = buildProperties();
        properties.getProductionOrder().setQueryLimit(1);
        server.expect(requestTo(containsString("ValidateUser")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.TEXT_PLAIN)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        server.expect(requestTo(containsString("ExecuteBillQuery")))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andExpect(content().string(containsString("FBillNo+ASC%2CFMaterialID2.FNumber+ASC")))
                .andRespond(withSuccess("[[1001,\"B001\",\"P001\",\"MO001\",1,\"C\",\"M001\",\"物料\",\"\",\"\",1,1,\"支\",\"\",1,\"\",\"\",\"\"]]", MediaType.TEXT_PLAIN));
        server.expect(requestTo(containsString("ExecuteBillQuery")))
                .andExpect(request -> assertEquals(1, committed.get()))
                .andExpect(content().string(containsString("%22StartRow%22%3A1")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withException(new SocketTimeoutException()));
        server.expect(requestTo(containsString("ExecuteBillQuery")))
                .andExpect(request -> assertEquals(1, committed.get()))
                .andExpect(content().string(containsString("%22StartRow%22%3A1")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withException(new java.net.SocketException("Connection reset")));
        server.expect(requestTo(containsString("ExecuteBillQuery")))
                .andExpect(request -> assertEquals(1, committed.get()))
                .andExpect(content().string(containsString("%22StartRow%22%3A1")))
                .andRespond(withSuccess("[]", MediaType.TEXT_PLAIN));
        java.util.ArrayList<ErpKingdeeProductionMaterialList> received = new java.util.ArrayList<>();
        client.forEachProductionMaterialListPage(properties, page -> { received.addAll(page); committed.incrementAndGet(); });
        assertEquals(1, received.size());
        assertEquals(List.of(1, 2), waits);
        server.verify();
    }

    @Test
    void consumerFailureStopsImmediatelyWithoutRetryingOrReadingNextPage() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        server.expect(requestTo(containsString("ValidateUser")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.TEXT_PLAIN)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        server.expect(requestTo(containsString("ExecuteBillQuery")))
                .andRespond(withSuccess("[[1001,\"B001\",\"P001\",\"MO001\",1,\"C\",\"M001\",\"物料\",\"\",\"\",1,1,\"支\",\"\",1,\"\",\"\",\"\"]]", MediaType.TEXT_PLAIN));
        ErpKingdeeProperties properties = buildProperties();
        properties.getProductionOrder().setQueryLimit(1);
        IllegalStateException databaseFailure = new IllegalStateException("database failed");
        assertEquals(databaseFailure, assertThrows(IllegalStateException.class, () ->
                new ErpKingdeeProductionMaterialListClientImpl(template).forEachProductionMaterialListPage(
                        properties, page -> { throw databaseFailure; })));
        server.verify();
    }

    @Test
    void interruptedBackoffStopsWithoutAnotherQuery() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        server.expect(requestTo(containsString("ValidateUser")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.TEXT_PLAIN)
                        .header(HttpHeaders.SET_COOKIE, "session=test"));
        server.expect(requestTo(containsString("ExecuteBillQuery")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withException(new SocketTimeoutException()));
        try {
            RestClientException failure = assertThrows(RestClientException.class, () ->
                    new ErpKingdeeProductionMaterialListClientImpl(template) {
                        @Override void waitBeforeRetry(int attempt) throws InterruptedException {
                            throw new InterruptedException();
                        }
                    }.forEachProductionMaterialListPage(buildProperties(), page -> { }));
            assertTrue(failure.getMessage().contains("被中断"));
            assertTrue(Thread.currentThread().isInterrupted());
            server.verify();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void incompleteRowsStillFailWithoutSubstitutingMissingFields() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionMaterialListClientImpl client =
                new ErpKingdeeProductionMaterialListClientImpl(restTemplate);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andRespond(withSuccess("[[1001,\"PPBOM001\"]]", MediaType.APPLICATION_JSON));
        ServiceException error = assertThrows(ServiceException.class,
                () -> client.forEachProductionMaterialListPage(buildProperties(), page -> { }));
        assertTrue(error.getMessage().contains("expected 18, actual 2"));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void queryErrorsAreReportedBeforeParsingRows(int mode) {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionMaterialListClientImpl client =
                new ErpKingdeeProductionMaterialListClientImpl(restTemplate);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andRespond(withSuccess("""
                        [[{"Result":{"ResponseStatus":{"IsSuccess":false,"ErrorCode":"FIELD_INVALID","Errors":[{"Message":"查询字段不存在"}]}}}]]
                        """, MediaType.APPLICATION_JSON));
        ServiceException error = assertThrows(ServiceException.class, () -> {
            if (mode == 0) {
                client.forEachProductionMaterialListPage(buildProperties(), page -> { });
            } else if (mode == 1) {
                client.fetchProductionMaterialListsModifiedBetween(buildProperties(),
                        LocalDateTime.of(2026, 10, 29, 0, 0), LocalDateTime.of(2026, 10, 30, 0, 0));
            } else {
                client.fetchProductionMaterialListsByProductionOrderNos(buildProperties(), Set.of("MO001"));
            }
        });
        assertTrue(error.getMessage().contains("查询字段不存在"));
        assertTrue(error.getMessage().contains("FIELD_INVALID"));
        server.verify();
    }

    @Test
    void fetchProductionMaterialListsModifiedBetween_queriesPrdPpbomAndParsesRows() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionMaterialListClientImpl client =
                new ErpKingdeeProductionMaterialListClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties();

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("PRD_PPBOM")))
                .andExpect(content().string(containsString("FMOEntryID")))
                .andExpect(content().string(containsString("FBillNo")))
                .andExpect(content().string(containsString("FMoBillNo")))
                .andExpect(content().string(containsString("FDocumentStatus")))
                .andExpect(content().string(containsString("FMaterialID.FNumber")))
                .andExpect(content().string(containsString("FMaterialID2.FNumber")))
                .andExpect(content().string(containsString("FMaterialID2.F_PAEZ_TUHAO")))
                .andExpect(content().string(containsString("FNeedDate2")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FNeedDate%2C"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FProductId"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FMoStatus"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FPlanBeginDate"))))
                .andExpect(content().string(containsString("FModifyDate+%3E%3D+%272026-06-12+08%3A00%3A00%27")))
                .andExpect(content().string(containsString("FModifyDate+%3C+%272026-06-12+09%3A00%3A00%27")))
                .andRespond(withSuccess("""
                        [[1001,"PPBOM0030888","AW.106.03.08.10","CODXMO20260",1,"C","A001.02.014.300","造影导管软端","4F","标准件",3,1000,"支","ZYDG-001",1,"直接领料","2026-06-13","2026-06-12T08:30:00"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionMaterialList> rows = client.fetchProductionMaterialListsModifiedBetween(properties,
                LocalDateTime.of(2026, 6, 12, 8, 0),
                LocalDateTime.of(2026, 6, 12, 9, 0));

        assertEquals(1, rows.size());
        ErpKingdeeProductionMaterialList row = rows.get(0);
        assertEquals("PRD_PPBOM", row.getFormId());
        assertEquals("1001", row.getEntryId());
        assertEquals("PPBOM0030888", row.getBillNo());
        assertEquals("AW.106.03.08.10", row.getProductCode());
        assertEquals("CODXMO20260", row.getProductionOrderNo());
        assertEquals("C", row.getProductionOrderStatus());
        assertEquals("A001.02.014.300", row.getChildMaterialCode());
        assertEquals("ZYDG-001", row.getDrawingNumber());
        assertEquals(new BigDecimal("1"), row.getRequiredQuantity());
        assertEquals(LocalDateTime.of(2026, 6, 13, 0, 0), row.getDemandTime());
        assertEquals(LocalDateTime.of(2026, 6, 12, 8, 30), row.getSourceModifyTime());
        server.verify();
    }

    @Test
    void fetchProductionMaterialListsByProductionOrderNos_queriesByOrderNoWithPaging() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionMaterialListClientImpl client =
                new ErpKingdeeProductionMaterialListClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties();
        properties.getProductionOrder().setQueryLimit(1);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("FMoBillNo+in+%28%27881MO090863%27%29")))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andExpect(content().string(containsString("%22Limit%22%3A1")))
                .andRespond(withSuccess("""
                        [[1001,"PPBOM0030818","YXN.037.011.1002","881MO090863",1,"C","A001.02.014.300","造影导管软端","4F","标准件",3,1000,"支","ZYDG-001",1,"直接领料","2024-02-13","2024-02-12 08:30:00"]]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("%22StartRow%22%3A1")))
                .andExpect(content().string(containsString("%22Limit%22%3A1")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionMaterialList> rows = client.fetchProductionMaterialListsByProductionOrderNos(
                properties, Set.of("881MO090863"));

        assertEquals(1, rows.size());
        assertEquals("881MO090863", rows.get(0).getProductionOrderNo());
        assertEquals("PPBOM0030818", rows.get(0).getBillNo());
        server.verify();
    }

    private static ErpKingdeeProperties buildProperties() {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com");
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-pass");
        properties.setLcid(2052);
        properties.getProductionOrder().setQueryLimit(500);
        return properties;
    }

}
