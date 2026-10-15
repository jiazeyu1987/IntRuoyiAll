package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRowNumberAllocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real approved writer + OWNER/folders/catalog/relation + actual GxP kernel on the existing H2 fixture. */
@Import(DccProductCatalogRowNumberAllocator.class)
class DccW01ApprovedRequestSequenceTest extends DccApprovedProjectLeaderOwnerTest {
    @MockitoSpyBean DccProductCatalogRowNumberAllocator sequence;
    @BeforeEach void sequenceFixture() {
        assertEquals(1L, jdbc.queryForObject("SELECT last_row_no FROM dcc_product_catalog_source_sequence WHERE data_source='瑛泰产品'", Long.class),
                "shared empty catalog fixture must provide its true floor");
        assertTrue(products.selectList().isEmpty());
    }
    @AfterEach void sequenceClean() throws Exception {
        // The missing-table scenario deliberately drops the table; recreate its exact fixture definition for shared cleanup.
        try (var input = getClass().getResourceAsStream("/sql/create_tables.sql")) {
            if (input == null) throw new IllegalStateException("Missing shared fixture resource");
            String fixture = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            int start = fixture.indexOf("CREATE TABLE IF NOT EXISTS dcc_product_catalog_source_sequence"), end = fixture.indexOf(';', start);
            if (start < 0 || end < start) throw new IllegalStateException("Missing shared sequence definition");
            jdbc.execute(fixture.substring(start, end + 1));
        }
    }
    Long request(String suffix) {
        Long id = approvedRequest();
        jdbc.update("UPDATE dcc_project_product_create_request SET project_code=?,product_code=?,product_name=?,project_name=? WHERE id=?",
                "SEQUENCE-P-" + suffix, "SEQUENCE-C-" + suffix, "产品" + suffix, "项目" + suffix, id);
        return id;
    }
    void actor() {
        TenantContextHolder.setTenantId(1L); TenantContextHolder.setIgnore(false);
        var user = new LoginUser().setId(1L).setTenantId(1L).setUserType(2)
                .setInfo(Map.of("username", "actual-approver", "nickname", "批准人"));
        SecurityFrameworkUtils.setLoginUser(user, new org.springframework.mock.web.MockHttpServletRequest());
    }
    @Test void twoApprovedRequestsConcurrentCommitEveryBusinessAssetWithDifferentRowNumbers() throws Exception {
        Long first = request("A"), second = request("B");
        var pool = Executors.newFixedThreadPool(2); var ready = new CountDownLatch(2); var release = new CountDownLatch(1);
        java.util.function.Function<Long, Future<Long>> submit = id -> pool.submit(() -> {
            actor(); ready.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("test release timeout");
                return writer.writeApprovedRequest(id).getGeneratedProductCatalogId();
            } finally { TenantContextHolder.clear(); org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        });
        try {
            var a = submit.apply(first); var b = submit.apply(second); assertTrue(ready.await(10, TimeUnit.SECONDS)); release.countDown();
            Long productA = a.get(15, TimeUnit.SECONDS), productB = b.get(15, TimeUnit.SECONDS);
            assertNotEquals(productA, productB);
            assertEquals(Set.of(2, 3), Set.of(products.selectById(productA).getOriginalRowNo(), products.selectById(productB).getOriginalRowNo()));
            assertEquals(3L, jdbc.queryForObject("SELECT last_row_no FROM dcc_product_catalog_source_sequence WHERE data_source='瑛泰产品'", Long.class));
            assertEquals(2, projects.selectList().size()); assertEquals(2, products.selectList().size());
            assertEquals(2, relations.selectList().size()); assertEquals(2, folderRows.selectList().size());
            assertEquals(2, accessRules.selectList().size()); assertEquals(2, events.selectList().size());
            for (Long id : List.of(first, second)) {
                var completed = requests.selectById(id); assertEquals("COMPLETED", completed.getStatus());
                assertEquals("ACTIVE", relations.selectById(completed.getRelationId()).getRelationStatus());
                var owner = accessRules.selectListByProjectCodeId(completed.getGeneratedProjectCodeId()).get(0);
                assertEquals("OWNER", owner.getAccessLevel()); assertEquals(7L, owner.getSubjectId());
                assertEquals("正式目录", folders.readProjectFolders(7L, completed.getGeneratedProjectCodeId()).get(0).getName());
                assertNotNull(completed.getCompletedTime());
            }
            verify(sequence, times(2)).allocate("瑛泰产品");
        } finally { release.countDown(); pool.shutdownNow(); }
    }
    @Test void lateAuditFailureRollsBackSequenceAndEveryAssetThenRetryUsesSameUncommittedNumber() {
        Long id = request("ROLLBACK");
        doThrow(new IllegalStateException("actual final GxP append failure")).when(actualAuditKernel).append(any());
        assertThrows(RuntimeException.class, () -> writer.writeApprovedRequest(id));
        noGeneratedAssets(id);
        assertEquals(1L, jdbc.queryForObject("SELECT last_row_no FROM dcc_product_catalog_source_sequence WHERE data_source='瑛泰产品'", Long.class));
        reset(actualAuditKernel);
        var completed = writer.writeApprovedRequest(id);
        assertEquals(2, products.selectById(completed.getGeneratedProductCatalogId()).getOriginalRowNo());
        assertEquals(1, events.selectList().size()); assertEquals(1, accessRules.selectList().size());
    }
    @Test void completedSameRequestReplayAllocatesNothingAndPreservesAllCommittedAssets() {
        Long id = request("REPLAY"); var completed = writer.writeApprovedRequest(id);
        clearInvocations(sequence);
        String requestBefore = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(requests.selectById(id));
        assertThrows(RuntimeException.class, () -> writer.writeApprovedRequest(id));
        verifyNoInteractions(sequence);
        assertEquals(requestBefore, cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(requests.selectById(id)));
        assertEquals(2L, jdbc.queryForObject("SELECT last_row_no FROM dcc_product_catalog_source_sequence WHERE data_source='瑛泰产品'", Long.class));
        assertEquals(1, products.selectList().size()); assertEquals(1, projects.selectList().size());
        assertEquals(1, accessRules.selectList().size()); assertEquals(1, folderRows.selectList().size()); assertEquals(1, events.selectList().size());
        assertEquals("COMPLETED", completed.getStatus());
    }
    @Test void missingSequenceSchemaFailsWithoutAssetsOrLazyMaxFallback() {
        Long id = request("MISSING"); jdbc.execute("DROP TABLE dcc_product_catalog_source_sequence");
        assertThrows(org.springframework.dao.DataAccessException.class, () -> writer.writeApprovedRequest(id));
        noGeneratedAssets(id);
    }
}
