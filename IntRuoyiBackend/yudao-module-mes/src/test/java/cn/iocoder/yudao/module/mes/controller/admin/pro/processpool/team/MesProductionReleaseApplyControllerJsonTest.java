package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderReleaseApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderReleaseApplyRespVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProductionReleaseApplyControllerJsonTest {

    @Test
    void receiptSerializesOnlySp1FieldsAndAllIdsAsStrings() throws Exception {
        MesTeamLeaderActiveOrderReleaseApplyRespVO response =
                new MesTeamLeaderActiveOrderReleaseApplyRespVO()
                        .setApplicationId(7001L)
                        .setActiveOrderId(2001L)
                        .setWorkOrderId(3001L)
                        .setWorkOrderCode("WO-3001")
                        .setBatchCode("BATCH-001")
                        .setRouteId(4001L)
                        .setRouteVersionId(4002L)
                        .setPqcReleaseWorkTaskId(8001L)
                        .setStatus("PQC_RELEASE_PENDING")
                        .setSourceSnapshotHash("source-hash")
                        .setVersion(1);

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(response));

        assertEquals(Set.of("activeOrderId", "applicationId", "appliedAt", "batchCode",
                        "pqcReleaseWorkTaskId", "routeId", "routeVersionId", "sourceSnapshotHash",
                        "status", "version", "workOrderCode", "workOrderId"),
                new TreeSet<>(json.properties().stream().map(java.util.Map.Entry::getKey).toList()));
        for (String idField : Set.of("applicationId", "activeOrderId", "workOrderId", "routeId",
                "routeVersionId", "pqcReleaseWorkTaskId")) {
            assertTrue(json.get(idField).isTextual(), idField + " must be serialized as a string");
        }
        assertFalse(json.has("batchExecutionId"));
        assertFalse(json.has("releaseTransactionId"));
        assertFalse(json.has("releaseApprovalWorkTaskId"));
    }

    @Test
    void requestRejectsNonVisibleOrOversizedIdempotencyKeysAndOversizedRemark() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertTrue(validator.validate(validRequest().setIdempotencyKey("contains space")).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("idempotencyKey")));
        assertTrue(validator.validate(validRequest().setIdempotencyKey("a".repeat(129))).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("idempotencyKey")));
        assertTrue(validator.validate(validRequest().setApplyRemark("x".repeat(501))).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("applyRemark")));
        assertTrue(validator.validate(validRequest()).isEmpty());
    }

    @Test
    void controllerExposesApplyAndAuthoritativeReceiptEndpoints() throws Exception {
        Method apply = MesProcessPoolTeamLeaderController.class.getMethod(
                "applyActiveOrderRelease", MesTeamLeaderActiveOrderReleaseApplyReqVO.class);
        Method get = MesProcessPoolTeamLeaderController.class.getMethod("getActiveOrderRelease", Long.class);

        assertEquals("/active-order/release/apply", apply.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("/active-order/release/get", get.getAnnotation(GetMapping.class).value()[0]);
        PreAuthorize authorization = get.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals("@ss.hasAnyPermissions('mes:pro-process-pool-team-leader:query', "
                + "'mes:pro-production-release:query')", authorization.value());
    }

    private static MesTeamLeaderActiveOrderReleaseApplyReqVO validRequest() {
        return new MesTeamLeaderActiveOrderReleaseApplyReqVO()
                .setActiveOrderId(2001L)
                .setIdempotencyKey("release-request-1")
                .setApplyRemark("ready");
    }
}
