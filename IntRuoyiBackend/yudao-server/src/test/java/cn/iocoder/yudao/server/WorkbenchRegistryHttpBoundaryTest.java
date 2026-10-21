package cn.iocoder.yudao.server;

import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.server.profileworkbench.WorkbenchSourceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** Real exception advice and MVC serialization; no service, DB or Redis is started. */
class WorkbenchRegistryHttpBoundaryTest {
    private WorkbenchSourceRegistry registry() {
        SourceAdapter adapter = new SourceAdapter() {
            public SourceDescriptor descriptor() {
                return new SourceDescriptor("ONE", "fixture", 1, 0, "一", "行政", "policy", "open", 1);
            }
            public SourceIdentityContract identityContract() { return SourceIdentityContract.SQL_BINARY_UNIQUE_TASK_KEY; }
            public RowOrderContract rowOrderContract() { return RowOrderContract.UTF8_BINARY_DESC; }
            public boolean isAuthorized(long user, long tenant) { return true; }
            public long count(QueryContext q) { throw new AssertionError("No business reads"); }
            public TaskChunk readChunk(QueryContext q, ChunkCursor c, int limit) { throw new AssertionError(); }
            public TaskSummary resolveVisibilityIdentity(QueryContext q, String key) { throw new AssertionError(); }
        };
        return new WorkbenchSourceRegistry(List.of(adapter), new WorkbenchSourceRegistry.Manifest(1, "revision",
            List.of(new WorkbenchSourceRegistry.Module("fixture", false, List.of("ONE")))));
    }
    private GlobalExceptionHandler handler() {
        return new GlobalExceptionHandler("workbench-contract", mock(ApiErrorLogCommonApi.class));
    }
    @Test void actualFilterHandlerPreservesAllRequestErrorCategories() {
        for (WorkbenchFailure category : WorkbenchFailure.values()) {
            ServiceException failure = category.exception();
            var result = handler().allExceptionHandler(new MockHttpServletRequest(), failure);
            assertEquals(category.code(), result.getCode());
            assertEquals(failure.getMessage(), result.getMsg());
        }
    }
    @RestController static class RequestFixture {
        final WorkbenchSourceRegistry registry;
        RequestFixture(WorkbenchSourceRegistry registry) { this.registry = registry; }
        @GetMapping("/registry") Object request(@RequestParam("operation") String operation) {
            switch (operation) {
                case "disabled": return registry.require("ONE");
                case "missing": return registry.require("UNKNOWN");
                case "revision": registry.requireRevision("wrong"); break;
                case "duplicate": registry.normalize(List.of("ONE", "ONE")); break;
                case "null": registry.require(null); break;
                default: throw new AssertionError();
            }
            throw new AssertionError("Rejected registry request must never succeed");
        }
    }
    @Test void actualMvcCommonResultPreservesRegistryFailures() throws Exception {
        MockMvc mvc = standaloneSetup(new RequestFixture(registry())).setControllerAdvice(handler()).build();
        String[] operations = {"disabled", "missing", "revision", "duplicate", "null"};
        WorkbenchFailure[] failures = {WorkbenchFailure.MODULE_DISABLED, WorkbenchFailure.SOURCE_NOT_REGISTERED,
            WorkbenchFailure.CONTRACT_VERSION_UNSUPPORTED, WorkbenchFailure.INVALID_QUERY, WorkbenchFailure.INVALID_QUERY};
        for (int i = 0; i < operations.length; i++) {
            mvc.perform(get("/registry").param("operation", operations[i])).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(failures[i].code()))
                .andExpect(jsonPath("$.msg").value(failures[i].exception().getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
        }
        assertThrows(IllegalStateException.class, () -> new WorkbenchSourceRegistry(List.of(),
            new WorkbenchSourceRegistry.Manifest(1, "revision", List.of(new WorkbenchSourceRegistry.Module("fixture", true, List.of("ONE"))))));
    }
    public static void main(String[] args) throws Exception {
        var test = new WorkbenchRegistryHttpBoundaryTest();
        test.actualFilterHandlerPreservesAllRequestErrorCategories();
        test.actualMvcCommonResultPreservesRegistryFailures();
        System.out.println("WorkbenchRegistryHttpBoundaryTest: 2 methods PASS, 18 handler categories + 5 MVC refusals + startup failure");
    }
}
