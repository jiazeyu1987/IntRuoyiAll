package cn.iocoder.yudao.module.erp.service.production.sync;

import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class ErpKingdeeProductionMaterialLineResolverTest {
    private static final String QUERY_URL = "https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static String error(String message) {
        return "[[{\"Result\":{\"ResponseStatus\":{\"IsSuccess\":false,\"ErrorCode\":500,\"Errors\":[{\"Message\":\""
                + message + "\"}]}}}]]";
    }

    @Test void deadlockRetriesTheIdenticalReadWithoutInventingLineNumbers() {
        var rest = new RestTemplate(); var server = MockRestServiceServer.bindTo(rest).build();
        var properties = new ErpKingdeeProperties(); properties.setBaseUrl("https://erp.example.com");
        var requests = new java.util.ArrayList<String>();
        var waits = new java.util.ArrayList<Integer>();
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3), requestTo(QUERY_URL))
                .andExpect(request -> requests.add(request.getBody().toString()))
                .andExpect(header("Cookie", "session"))
                .andRespond(request -> withSuccess(requests.size() < 3 ? error("已被选作死锁牺牲品")
                        : "[[2234891,7]]", MediaType.APPLICATION_JSON).createResponse(request));
        var resolver = new ErpKingdeeProductionMaterialLineResolver(rest) {
            @Override void waitBeforeRetry(int attempt) { waits.add(attempt); }
        };
        assertEquals(7, resolver.resolve(properties, "session", Set.of("2234891")).get("2234891"));
        assertEquals(1, new java.util.HashSet<>(requests).size());
        assertEquals(java.util.List.of(1, 2), waits); server.verify();
    }

    @Test void exhaustedDeadlockPreservesSourceFailureAfterThreeAttempts() {
        var rest = new RestTemplate(); var server = MockRestServiceServer.bindTo(rest).build();
        var properties = new ErpKingdeeProperties(); properties.setBaseUrl("https://erp.example.com");
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3), requestTo(QUERY_URL))
                .andRespond(withSuccess(error("已被选作死锁牺牲品"), MediaType.APPLICATION_JSON));
        var resolver = new ErpKingdeeProductionMaterialLineResolver(rest) {
            @Override void waitBeforeRetry(int attempt) { }
        };
        var failure = assertThrows(IllegalStateException.class,
                () -> resolver.resolve(properties, "session", Set.of("2234891")));
        assertTrue(failure.getMessage().contains("死锁牺牲品")); server.verify();
    }

    @Test void sourcePermissionErrorIsReportedWithoutRetryOrRowParsing() {
        var rest = new RestTemplate(); var server = MockRestServiceServer.bindTo(rest).build();
        var properties = new ErpKingdeeProperties(); properties.setBaseUrl("https://erp.example.com");
        server.expect(requestTo(QUERY_URL)).andRespond(withSuccess(error("无权查询单据"), MediaType.APPLICATION_JSON));
        var failure = assertThrows(IllegalStateException.class, () -> new ErpKingdeeProductionMaterialLineResolver(rest)
                .resolve(properties, "session", Set.of("2234891")));
        assertTrue(failure.getMessage().contains("无权查询单据")); server.verify();
    }

    @Test void invalidSequenceStillFailsAndIdentifiesTheActualValues() {
        var rest = new RestTemplate(); var server = MockRestServiceServer.bindTo(rest).build();
        var properties = new ErpKingdeeProperties(); properties.setBaseUrl("https://erp.example.com");
        server.expect(requestTo(QUERY_URL)).andRespond(withSuccess("[[2234891,0]]", MediaType.APPLICATION_JSON));
        var failure = assertThrows(IllegalStateException.class, () -> new ErpKingdeeProductionMaterialLineResolver(rest)
                .resolve(properties, "session", Set.of("2234891")));
        assertTrue(failure.getMessage().contains("entryId=2234891, sequence=0")); server.verify();
    }

    @Test void interruptedTaskDoesNotSubmitAQuery() {
        var rest = new RestTemplate(); var server = MockRestServiceServer.bindTo(rest).build();
        var properties = new ErpKingdeeProperties(); properties.setBaseUrl("https://erp.example.com");
        Thread.currentThread().interrupt();
        try {
            assertThrows(java.util.concurrent.CancellationException.class, () -> new ErpKingdeeProductionMaterialLineResolver(rest)
                    .resolve(properties, "session", Set.of("2234891")));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
        server.verify();
    }
    @Test void resolvesActualMaterialListSequenceRatherThanUsingEntryInternalId() {
        var rest = new RestTemplate(); var server = MockRestServiceServer.bindTo(rest).build();
        var properties = new ErpKingdeeProperties();properties.setBaseUrl("https://erp.example.com");
        server.expect(requestTo("https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("PRD_PPBOM")))
                .andExpect(content().string(containsString("FEntity_FSeq")))
                .andRespond(withSuccess("[[2234891,7],[2240859,12]]",MediaType.APPLICATION_JSON));
        var result = new ErpKingdeeProductionMaterialLineResolver(rest).resolve(properties,"session",Set.of("2234891","2240859"));
        assertEquals(7,result.get("2234891"));assertEquals(12,result.get("2240859"));server.verify();
    }
    @Test void missingReferencedEntryFailsInsteadOfInventingSequence() {
        var rest = new RestTemplate();var server = MockRestServiceServer.bindTo(rest).build();
        var properties = new ErpKingdeeProperties();properties.setBaseUrl("https://erp.example.com");
        server.expect(requestTo("https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class,()->new ErpKingdeeProductionMaterialLineResolver(rest)
                .resolve(properties,"session",Set.of("2234891")));server.verify();
    }
}
