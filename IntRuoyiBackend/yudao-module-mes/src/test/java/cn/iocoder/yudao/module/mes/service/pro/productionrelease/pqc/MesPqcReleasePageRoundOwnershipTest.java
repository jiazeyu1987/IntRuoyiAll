package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesPqcReleasePageRoundOwnershipTest extends BaseDbUnitTest {

    private static final long TENANT_ID = 1L;
    private static final long ACTOR_USER_ID = 900L;

    @Resource
    private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;

    @Resource
    private MesProEdhrNonconformanceReviewMapper reviewMapper;

    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private MesPqcProductionReleaseServiceImpl service;

    @BeforeEach
    void setUpRoundFixture() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mes_pro_edhr_nonconformance_review (
                    id BIGINT PRIMARY KEY,
                    review_code VARCHAR(64),
                    source_type VARCHAR(32) NOT NULL,
                    source_id BIGINT NOT NULL,
                    active_order_id BIGINT NOT NULL,
                    batch_execution_id BIGINT,
                    batch_execution_code VARCHAR(64),
                    work_order_id BIGINT,
                    work_order_code VARCHAR(128),
                    batch_code VARCHAR(128),
                    previous_batch_status INT,
                    previous_work_order_temporary_frozen BOOLEAN,
                    review_status VARCHAR(32) NOT NULL,
                    disposition VARCHAR(64),
                    nonconformance_reason VARCHAR(500),
                    review_material_url VARCHAR(1024),
                    review_material_file_id BIGINT,
                    review_materials_json CLOB,
                    review_opinion VARCHAR(500),
                    qa_signature VARCHAR(500),
                    qa_user_id BIGINT,
                    frozen_at TIMESTAMP,
                    closed_at TIMESTAMP,
                    unfrozen_at TIMESTAMP,
                    voided_at TIMESTAMP,
                    trace_snapshot_json CLOB,
                    remark VARCHAR(500),
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    creator VARCHAR(64) DEFAULT '',
                    updater VARCHAR(64) DEFAULT '',
                    deleted BIT NOT NULL DEFAULT FALSE,
                    tenant_id BIGINT NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.update("DELETE FROM mes_pro_edhr_nonconformance_review");
        seedApplicationAndTask(1L);
        insertReview(101L, 1001L, 1001L, "closed", "rework", "first-round rework",
                LocalDateTime.of(2026, 9, 1, 10, 0), LocalDateTime.of(2026, 9, 2, 10, 0));
        service = new MesPqcProductionReleaseServiceImpl(
                applicationMapper, null, null, null, null, null, null, null, null, null, null,
                reviewMapper, Clock.systemUTC());
    }

    @Test
    void pqcPageKeepsReviewOwnershipByApplicationRoundAcrossFiveViews() {
        PageResult<MesPqcProductionReleasePageItem> firstRoundSnapshot = page("REWORKED", 1);
        assertEquals(1L, firstRoundSnapshot.getTotal());
        assertEquals(List.of(1L), ids(firstRoundSnapshot));
        assertEquals(101L, firstRoundSnapshot.getList().get(0).getNonconformanceReviewId());
        assertEquals("rework", firstRoundSnapshot.getList().get(0).getNonconformanceDisposition());

        for (long id = 2L; id <= 7L; id++) {
            seedApplicationAndTask(id);
        }
        insertReview(102L, 1001L, 1001L, "closed", "void", "second-round void",
                LocalDateTime.of(2026, 9, 4, 10, 0), LocalDateTime.of(2026, 9, 5, 10, 0));
        insertReview(105L, 1005L, 1005L, "closed", "concession_release", "concession release",
                LocalDateTime.of(2026, 9, 12, 10, 0), LocalDateTime.of(2026, 9, 13, 10, 0));
        insertReview(106L, 1006L, 1006L, "closed", "rework", "independent rework",
                LocalDateTime.of(2026, 9, 13, 10, 0), LocalDateTime.of(2026, 9, 14, 10, 0));
        insertReview(107L, 1007L, 1007L, "closed", "void", "independent void",
                LocalDateTime.of(2026, 9, 14, 10, 0), LocalDateTime.of(2026, 9, 15, 10, 0));

        assertPage("PENDING", 1L, 3L);
        assertPage("RELEASED", 1L, 4L);
        assertPage("CONCESSION_RELEASED", 1L, 5L);

        PageResult<MesPqcProductionReleasePageItem> firstReworkPage = page("REWORKED", 1);
        assertEquals(2L, firstReworkPage.getTotal());
        assertEquals(List.of(6L), ids(firstReworkPage));

        PageResult<MesPqcProductionReleasePageItem> secondReworkPage = page("REWORKED", 2);
        assertEquals(2L, secondReworkPage.getTotal());
        assertEquals(List.of(1L), ids(secondReworkPage));
        assertEquals(101L, secondReworkPage.getList().get(0).getNonconformanceReviewId());
        assertEquals("rework", secondReworkPage.getList().get(0).getNonconformanceDisposition());

        PageResult<MesPqcProductionReleasePageItem> firstVoidPage = page("VOIDED", 1);
        assertEquals(2L, firstVoidPage.getTotal());
        assertEquals(List.of(7L), ids(firstVoidPage));
        PageResult<MesPqcProductionReleasePageItem> secondVoidPage = page("VOIDED", 2);
        assertEquals(2L, secondVoidPage.getTotal());
        assertEquals(List.of(2L), ids(secondVoidPage));
        assertEquals(102L, secondVoidPage.getList().get(0).getNonconformanceReviewId());
        assertEquals("void", secondVoidPage.getList().get(0).getNonconformanceDisposition());
    }

    private void assertPage(String viewStatus, long total, long expectedApplicationId) {
        PageResult<MesPqcProductionReleasePageItem> result = page(viewStatus, 1);
        assertEquals(total, result.getTotal(), viewStatus);
        assertEquals(List.of(expectedApplicationId), ids(result), viewStatus);
    }

    private PageResult<MesPqcProductionReleasePageItem> page(String viewStatus, int pageNo) {
        return service.getPqcReleasePage(ACTOR_USER_ID, new MesPqcProductionReleasePageQuery()
                .setPageNo(pageNo)
                .setPageSize(1)
                .setViewStatus(viewStatus));
    }

    private static List<Long> ids(PageResult<MesPqcProductionReleasePageItem> result) {
        return result.getList().stream().map(MesPqcProductionReleasePageItem::getApplicationId).toList();
    }

    private void seedApplicationAndTask(long id) {
        long activeOrderId = id == 1L || id == 2L ? 1001L : 1000L + id;
        long workOrderId = id == 1L || id == 2L ? 5001L : 5000L + id;
        String workOrderCode = id == 1L || id == 2L ? "WO-ROUND-1" : "WO-" + id;
        String batchCode = id == 1L || id == 2L ? "B-ROUND-1" : "B-" + id;
        long workTaskId = 100L + id;
        String applicationStatus = switch ((int) id) {
            case 1, 2, 6, 7 -> "PQC_RELEASE_REJECTED";
            case 3 -> "PQC_RELEASE_PENDING";
            default -> "RELEASED";
        };
        String pqcDecision = switch ((int) id) {
            case 1, 6 -> "NONCONFORMANCE_REWORK";
            case 2, 7 -> "NONCONFORMANCE_VOID";
            default -> null;
        };
        Long batchExecutionId = id == 1L || id == 2L ? 1001L : 2000L + id;
        LocalDateTime appliedAt = switch ((int) id) {
            case 1 -> LocalDateTime.of(2026, 9, 1, 9, 0);
            case 2 -> LocalDateTime.of(2026, 9, 4, 9, 0);
            default -> LocalDateTime.of(2026, 9, 10, 9, 0).plusDays(id - 3L);
        };
        jdbcTemplate.update("""
                    INSERT INTO mes_pro_process_pool_active_order_release_application
                        (id, active_order_id, work_order_id, work_order_code, route_id, route_version_id,
                         product_id, batch_code, batch_execution_id, pqc_release_work_task_id, pqc_decision,
                         application_status, source_snapshot_hash, version, request_idempotency_key,
                         business_idempotency_key, applied_by, applied_at, tenant_id, deleted)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, FALSE)
                    """,
                    id, activeOrderId, workOrderId, workOrderCode, 700L, 701L, 9001L, batchCode,
                    batchExecutionId, workTaskId, pqcDecision, applicationStatus, "hash-" + id,
                    "request-" + id, "business-" + id, ACTOR_USER_ID,
                    appliedAt, TENANT_ID);

            jdbcTemplate.update("""
                    INSERT INTO mes_pro_edhr_work_task
                        (id, task_code, task_type, batch_execution_id, business_scope_type, business_scope_id,
                         assignee_user_id, candidate_user_snapshot, status, action_url, signature_cell_key,
                         tenant_id, deleted)
                    VALUES (?, ?, 'PQC_PRODUCTION_RELEASE', ?, 'RELEASE_APPLICATION', ?, ?, '900', 'TODO',
                            '/mes/pro/production-release', 'PQC_RELEASE', ?, FALSE)
                    """,
                    workTaskId, "PQC-TASK-" + id, batchExecutionId, id, ACTOR_USER_ID, TENANT_ID);
    }

    private void insertReview(Long id, Long sourceId, Long activeOrderId, String reviewStatus,
                               String disposition, String reason, LocalDateTime frozenAt, LocalDateTime closedAt) {
        jdbcTemplate.update("""
                INSERT INTO mes_pro_edhr_nonconformance_review
                    (id, source_type, source_id, active_order_id, review_status, disposition,
                     nonconformance_reason, frozen_at, closed_at, deleted, tenant_id)
                VALUES (?, 'ACTIVE_ORDER', ?, ?, ?, ?, ?, ?, ?, FALSE, ?)
                """, id, sourceId, activeOrderId, reviewStatus, disposition, reason,
                frozenAt, closedAt, TENANT_ID);
    }
}
