package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProductionReleaseReportPersistenceContractTest {

    @Test
    void applicationVersionAndFinalHandoffWritesAreCasGuarded() throws Exception {
        Method advance = MesProcessPoolActiveOrderReleaseApplicationMapper.class.getMethod(
                "advanceReportVersion", Long.class, Integer.class);
        Method handoff = MesProcessPoolActiveOrderReleaseApplicationMapper.class.getMethod(
                "handoffReportsToManager", Long.class, Integer.class, String.class,
                Long.class, Long.class, String.class);
        String advanceSql = String.join("\n", advance.getAnnotation(Update.class).value());
        String handoffSql = String.join("\n", handoff.getAnnotation(Update.class).value());

        assertTrue(advanceSql.contains("version = #{expectedVersion}"));
        assertTrue(advanceSql.contains("application_status = 'REPORT_UPLOAD_PENDING'"));
        assertTrue(handoffSql.contains("application_status = 'MANAGER_RELEASE_PENDING'"));
        assertTrue(handoffSql.contains("release_transaction_id = #{releaseTransactionId}"));
        assertTrue(handoffSql.contains("release_approval_work_task_id = #{managerReleaseWorkTaskId}"));
        assertTrue(handoffSql.contains("version = #{expectedVersion}"));
    }

    @Test
    void completionReceiptCanOnlyReplaceAnApprovedReportPayload() throws Exception {
        Method update = MesProEdhrBatchExecutionTaskMapper.class.getMethod(
                "updateReleaseReportCompletionPayload", Long.class, String.class);
        String sql = String.join("\n", update.getAnnotation(Update.class).value());

        assertTrue(sql.contains("special_payload_json = #{payloadJson}"));
        assertTrue(sql.contains("status = 40"));
    }
}
