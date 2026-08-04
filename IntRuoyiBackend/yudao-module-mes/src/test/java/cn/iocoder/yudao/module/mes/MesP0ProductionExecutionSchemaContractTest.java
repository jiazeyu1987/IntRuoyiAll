package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventCreateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesP0ProductionExecutionSchemaContractTest {

    @Test
    void formalFieldsMustPersistTheProductionExecutionClosedLoop() throws Exception {
        assertField(MesProFrontlineFeedbackSubmitReqVO.class, "processPoolSubmissionIdempotencyKey", String.class);
        assertField(MesProFrontlineFeedbackSubmitReqVO.class, "actualEmployeeId", Long.class);
        assertField(MesProFrontlineFeedbackSubmitReqVO.class, "signatureId", Long.class);
        assertField(MesProFrontlineFeedbackSubmitReqVO.class, "signatureEmployeeId", Long.class);

        assertField(MesProFrontlineFeedbackSubmitRespVO.class, "feedbackId", Long.class);
        assertField(MesProFrontlineFeedbackSubmitRespVO.class, "recordbookEntryId", Long.class);
        assertField(MesProFrontlineFeedbackSubmitRespVO.class, "recordbookEventId", Long.class);
        assertField(MesProFrontlineFeedbackSubmitRespVO.class, "processPoolEventId", Long.class);

        assertField(MesProcessPoolSubmitEventCreateReqBO.class,
                "processPoolSubmissionIdempotencyKey", String.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "feedbackId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "recordbookEntryId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "recordbookEventId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "workOrderId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "routeId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "routeProcessId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "processId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "workstationId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "deviceId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "deviceAccountUserId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "actualEmployeeId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "signatureEmployeeId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "signatureId", Long.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "outputQuantity", BigDecimal.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "lossQuantity", BigDecimal.class);
        assertField(MesProcessPoolSubmitEventCreateReqBO.class, "submittedAt", LocalDateTime.class);

        assertField(MesProcessPoolSubmitEventResult.class, "feedbackId", Long.class);
        assertField(MesProcessPoolSubmitEventResult.class, "recordbookEntryId", Long.class);
        assertField(MesProcessPoolSubmitEventResult.class, "recordbookEventId", Long.class);
        assertField(MesProcessPoolSubmitEventResult.class, "processPoolEventId", Long.class);

        assertField(MesProProcessPoolEventDO.class, "eventType", String.class);
        assertField(MesProProcessPoolEventDO.class, "eventIdempotencyKey", String.class);
        assertField(MesProProcessPoolEventDO.class, "feedbackSourceId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "recordbookEntryId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "recordbookSourceId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "workOrderId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "routeId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "routeProcessId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "processId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "actualEmployeeId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "deviceAccountId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "deviceId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "workstationId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "signatureId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "signatureUserId", Long.class);
        assertField(MesProProcessPoolEventDO.class, "signatureSnapshot", String.class);

        assertField(MesProProcessPoolPqcRecordDO.class, "eventId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "productionSubmitEventId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "inspectionResult", String.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "actualEmployeeId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "signatureId", Long.class);
        assertField(MesProProcessPoolPqcRecordDO.class, "signatureUserId", Long.class);

        assertField(MesProProcessPoolQuantityFragmentDO.class, "eventId", Long.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "productionSubmitEventId", Long.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "qualityStatus", String.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "totalQuantity", BigDecimal.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "allocatedQuantity", BigDecimal.class);
        assertField(MesProProcessPoolQuantityFragmentDO.class, "availableQuantity", BigDecimal.class);

        assertField(MesProcessPoolSubmissionReviewDO.class, "eventId", Long.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "reviewSignatureId", Long.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "reviewSignatureUserId", Long.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "reviewSignatureSnapshotJson", String.class);
    }

    @Test
    void migrationsMustCreateFormalIndexesAndBackfillBlockers() throws Exception {
        String idempotencySql = readBackendSql("sql/mysql/20260803_mes_process_pool_event_idempotency.sql");
        assertTrue(idempotencySql.contains("event_idempotency_key"));
        assertTrue(idempotencySql.contains("recordbook_entry_id"));
        assertTrue(idempotencySql.contains("uk_mes_pro_process_pool_event_idem"));
        assertTrue(idempotencySql.contains("requires formal event_idempotency_key backfill"));

        String pqcBindingSql = readBackendSql("sql/mysql/20260803_mes_process_pool_pqc_structured_binding.sql");
        assertTrue(pqcBindingSql.contains("production_submit_event_id"));
        assertTrue(pqcBindingSql.contains("idx_mes_pro_process_pool_pqc_submit_event"));
        assertTrue(pqcBindingSql.contains("requires formal production_submit_event_id backfill"));

        String quantityFragmentSql = readBackendSql(
                "sql/mysql/20260803_mes_process_pool_quantity_fragment_submit_root.sql");
        assertTrue(quantityFragmentSql.contains("production_submit_event_id"));
        assertTrue(quantityFragmentSql.contains("idx_mes_pro_process_pool_fragment_submit_event"));
        assertTrue(quantityFragmentSql.contains("requires formal PRODUCTION_SUBMIT root event backfill"));

        String reviewSignatureSql = readBackendSql(
                "sql/mysql/20260803_mes_process_pool_team_leader_review_signature.sql");
        assertTrue(reviewSignatureSql.contains("review_signature_id"));
        assertTrue(reviewSignatureSql.contains("review_signature_user_id"));
        assertTrue(reviewSignatureSql.contains("review_signature_snapshot_json"));
        assertTrue(reviewSignatureSql.contains("idx_mes_pp_review_signature"));
    }

    private static void assertField(Class<?> clazz, String name, Class<?> type) throws Exception {
        Field field = clazz.getDeclaredField(name);
        assertEquals(type, field.getType(), clazz.getSimpleName() + "." + name);
    }

    private static String readBackendSql(String relative) throws Exception {
        return Files.readString(resolveBackendPath(relative), StandardCharsets.UTF_8);
    }

    private static Path resolveBackendPath(String relative) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.getParent().resolve(relative);
        }
        return cwd.resolve(relative);
    }
}
