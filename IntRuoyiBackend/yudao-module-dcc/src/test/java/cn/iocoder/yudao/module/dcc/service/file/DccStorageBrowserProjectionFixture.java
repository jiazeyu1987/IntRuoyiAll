package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Explicit SQL projection results for service/permission tests. MySQL recursive version ranking
 * is verified against MySQL separately; this fixture never reimplements that algorithm in Java. */
final class DccStorageBrowserProjectionFixture {
    private final DccControlledFileMapper files;
    private List<Long> projectedIds = List.of();

    DccStorageBrowserProjectionFixture(DccControlledFileQueryServiceImpl query, DccControlledFileMapper files) {
        this.files = files;
        var authorization = new DccBrowserSqlAuthorization();
        Map<String, String> ports = Map.of("categories", "permissionSupport", "matrix", "viewMatrixAccessService",
                "projects", "projectAccessService", "projectMapper", "projectCodeMapper",
                "directories", "directoryAccessPermissionService", "guard", "detailAuthorizationGuard",
                "mapper", "projectBrowserMapper", "archiveAccess", "archiveAccess");
        ports.forEach((target, source) -> ReflectionTestUtils.setField(authorization, target,
                ReflectionTestUtils.getField(query, source)));
        ReflectionTestUtils.setField(query, "browserSqlAuthorization", authorization);
        var mapper = mock(DccStorageBrowserMapper.class);
        ReflectionTestUtils.setField(query, "storageBrowserMapper", mapper);
        doAnswer(call -> 0L).when(mapper).countInvalidLatestMasters(any(), any(), any(), any(), any(), anyBoolean());
        doAnswer(call -> {
            verifySql(DccStorageBrowserSql.count(), call.getArguments());
            return (long) projectedIds.size();
        }).when(mapper).count(any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());
        doAnswer(call -> {
            verifySql(DccStorageBrowserSql.page(), call.getArguments());
            List<DccControlledFileDO> rows = new ArrayList<>();
            for (Long id : projectedIds) rows.add(Objects.requireNonNull(files.selectById(id)));
            return rows;
        }).when(mapper).page(any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyInt(), anyLong());
    }

    void rows(Long... ids) { projectedIds = List.of(ids); }

    private void verifySql(String script, Object[] args) throws Exception {
        assertEquals(1L, args[0]);
        var parameters = new HashMap<String,Object>();
        String[] names = {"tenant", "query", "candidate", "permission", "historyPermission", "ew", "latest", "working", "size", "offset"};
        for (int i = 0; i < args.length; i++) parameters.put(names[i], args[i]);
        var bound = new XMLLanguageDriver().createSqlSource(new Configuration(), script, Map.class).getBoundSql(parameters);
        String sql = bound.getSql();
        assertTrue(sql.contains("tenant_id=? AND deleted=0"));
        assertTrue(sql.contains("ROW_NUMBER() OVER(PARTITION BY COALESCE(f.master_id,f.id)"));
        assertTrue(sql.contains("f.version_sort_key COLLATE utf8mb4_bin DESC,f.id DESC"));
        if (!Boolean.TRUE.equals(args[7])) {
            assertTrue(sql.contains("f.controlled_time IS NOT NULL AND f.activated_time IS NOT NULL"));
            assertTrue(sql.contains("f.status IN ('ACTIVE','SUPERSEDED','OBSOLETE')"));
        } else {
            assertTrue(sql.contains("f.status != 'REJECTED' OR f.id=?"));
        }
        assertFalse(((QueryWrapper<?>) args[3]).getSqlSegment().isBlank());
        assertFalse(((QueryWrapper<?>) args[4]).getSqlSegment().isBlank());
        // Keep tenant interception parseability under test even though the H2 fixture uses explicit rows.
        try { assertNotNull(net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql)); }
        catch (net.sf.jsqlparser.JSQLParserException failure) { throw new AssertionError(sql, failure); }
    }
}
