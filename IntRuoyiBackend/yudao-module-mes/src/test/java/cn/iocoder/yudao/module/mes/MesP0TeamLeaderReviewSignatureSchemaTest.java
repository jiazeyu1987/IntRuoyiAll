package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionReviewReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportConfirmationReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewReqBO;
import org.junit.jupiter.api.Test;

import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesP0TeamLeaderReviewSignatureSchemaTest {

    @Test
    void submissionReviewSchemaMustPersistStructuredReviewSignature() throws Exception {
        assertField(MesProcessPoolSubmissionReviewDO.class, "reviewSignatureId", Long.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "reviewSignatureUserId", Long.class);
        assertField(MesProcessPoolSubmissionReviewDO.class, "reviewSignatureSnapshotJson", String.class);

        assertField(MesTeamLeaderSubmissionReviewReqVO.class, "signaturePassword", String.class);

        assertField(MesTeamLeaderSubmissionReviewReqBO.class, "signaturePassword", String.class);
        assertField(MesTeamLeaderSubmissionReviewReqBO.class, "reviewSignatureId", Long.class);
        assertField(MesTeamLeaderSubmissionReviewReqBO.class, "reviewSignatureUserId", Long.class);
        assertField(MesTeamLeaderSubmissionReviewReqBO.class, "reviewSignatureSnapshotJson", String.class);

        assertField(MesTeamLeaderReportAllocationConfirmReqVO.class, "signaturePassword", String.class);

        assertField(MesTeamLeaderReportConfirmationReqBO.class, "signaturePassword", String.class);
        assertField(MesTeamLeaderReportConfirmationReqBO.class, "reviewSignatureId", Long.class);
        assertField(MesTeamLeaderReportConfirmationReqBO.class, "reviewSignatureUserId", Long.class);
        assertField(MesTeamLeaderReportConfirmationReqBO.class, "reviewSignatureSnapshotJson", String.class);
    }

    @Test
    void reviewRequestsMustUsePasswordInsteadOfClientVisibleSignatureFields() throws Exception {
        assertTrue(hasAnnotation(MesTeamLeaderSubmissionReviewReqVO.class, "signaturePassword", NotBlank.class));
        assertFalse(hasAnnotation(MesTeamLeaderReportAllocationConfirmReqVO.class, "signaturePassword", NotBlank.class));
    }

    @Test
    void signatureMigrationMustAddFormalReviewSignatureColumns() throws Exception {
        String sql = Files.readString(resolveBackendPath(
                "sql/mysql/20260803_mes_process_pool_team_leader_review_signature.sql"), StandardCharsets.UTF_8);
        String normalizedSql = sql.replace("\r\n", "\n");

        assertTrue(normalizedSql.startsWith("-- release-migration: allowedEnvironments=test,backup,prod; "
                + "dependsOn=20260730_mes_process_pool_team_leader; type=schema; riskLevel=medium\n"));
        assertTrue(sql.contains("ALTER TABLE `mes_pro_process_pool_submission_review`"));
        assertTrue(sql.contains("'review_signature_id'"));
        assertTrue(sql.contains("bigint DEFAULT NULL COMMENT ''复核电子签名ID''"));
        assertTrue(sql.contains("'review_signature_user_id'"));
        assertTrue(sql.contains("bigint DEFAULT NULL COMMENT ''复核电子签名用户ID''"));
        assertTrue(sql.contains("'review_signature_snapshot_json'"));
        assertTrue(sql.contains("json DEFAULT NULL COMMENT ''复核电子签名快照JSON''"));
        assertTrue(sql.contains("KEY `idx_mes_pp_review_signature` (`tenant_id`, `review_signature_id`)"));
    }

    private static void assertField(Class<?> clazz, String name, Class<?> type) throws Exception {
        Field field = clazz.getDeclaredField(name);
        assertEquals(type, field.getType(), clazz.getSimpleName() + "." + name);
    }

    private static boolean hasAnnotation(Class<?> clazz, String name,
                                         Class<? extends java.lang.annotation.Annotation> annotationType) throws Exception {
        return clazz.getDeclaredField(name).getAnnotation(annotationType) != null;
    }

    private static Path resolveBackendPath(String relative) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.getParent().resolve(relative);
        }
        return cwd.resolve(relative);
    }
}
