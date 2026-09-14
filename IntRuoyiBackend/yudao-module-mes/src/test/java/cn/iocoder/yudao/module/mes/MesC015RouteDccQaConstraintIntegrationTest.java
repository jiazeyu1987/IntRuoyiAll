package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesC015RouteDccQaConstraintIntegrationTest extends BaseDbUnitTest {

    @Resource
    private DataSource dataSource;

    @Test
    void routeBindingConstraints_shouldAllowHistoryAndRejectDuplicateCurrentOrVersion() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        assertDoesNotThrow(() -> {
            insertRouteBinding(jdbcTemplate, 1L, 1001L, 2001L, 1L, true, 1L);
            insertRouteBinding(jdbcTemplate, 2L, 1001L, 2002L, 2L, true, 1L);
            insertRouteBinding(jdbcTemplate, 3L, 1001L, 2003L, 3L, false, 1L);
            insertRouteBinding(jdbcTemplate, 4L, 1001L, 2004L, 1L, false, 2L);
        });

        assertThrows(DuplicateKeyException.class,
                () -> insertRouteBinding(jdbcTemplate, 5L, 1001L, 2005L, 4L, false, 1L));
        assertThrows(DuplicateKeyException.class,
                () -> insertRouteBinding(jdbcTemplate, 6L, 1001L, 2006L, 2L, true, 1L));
        assertEquals(4, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mes_pro_route_dcc_project_binding", Integer.class));
    }

    @Test
    void qaRegulationConstraints_shouldAllowDeletedHistoryAndRejectDuplicateActiveDcc() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        assertDoesNotThrow(() -> jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mes_qa_inspection_regulation", Integer.class),
                "H2 fixture must include the reconciled QA master table");

        assertDoesNotThrow(() -> {
            insertQaRegulation(jdbcTemplate, 11L, 3001L, false, 1L);
            insertQaRegulation(jdbcTemplate, 12L, 3001L, true, 1L);
            insertQaRegulation(jdbcTemplate, 13L, 3001L, true, 1L);
            insertQaRegulation(jdbcTemplate, 14L, 3001L, false, 2L);
        });

        assertThrows(DuplicateKeyException.class,
                () -> insertQaRegulation(jdbcTemplate, 15L, 3001L, false, 1L));
        assertEquals(4, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mes_qa_inspection_regulation", Integer.class));
    }

    private static void insertRouteBinding(JdbcTemplate jdbcTemplate, Long id, Long routeId,
                                           Long dccProjectCodeId, Long version, boolean deleted, Long tenantId) {
        jdbcTemplate.update("""
                INSERT INTO mes_pro_route_dcc_project_binding
                    (id, route_id, dcc_project_code_id, version, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, routeId, dccProjectCodeId, version, deleted, tenantId);
    }

    private static void insertQaRegulation(JdbcTemplate jdbcTemplate, Long id, Long dccProjectCodeId,
                                           boolean deleted, Long tenantId) {
        jdbcTemplate.update("""
                INSERT INTO mes_qa_inspection_regulation
                    (id, dcc_project_code_id, owner_module, regulation_code, regulation_name,
                     lifecycle_status, deleted, tenant_id)
                VALUES (?, ?, 'MES_QA', ?, ?, 'DRAFT', ?, ?)
                """, id, dccProjectCodeId, "QA-" + id, "QA regulation " + id, deleted, tenantId);
    }
}
