package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease;

import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesPqcProductionReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesPqcProductionReleaseDecisionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesPqcProductionReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesProductionReleaseReportUploadTaskRespVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProductionReleaseControllerJsonTest {

    @Test
    void pqcDecisionReceiptSerializesAllLongIdsAsStrings() throws Exception {
        MesPqcProductionReleaseDecisionRespVO response = new MesPqcProductionReleaseDecisionRespVO()
                .setApplicationId(9007199254740993L)
                .setPqcReleaseWorkTaskId(9007199254740994L)
                .setDecision("APPROVE")
                .setStatus("REPORT_UPLOAD_PENDING")
                .setBatchExecutionId(9007199254740995L)
                .setBatchRecordEvidenceIds(List.of(9007199254740996L))
                .setProcessInspectionEvidenceIds(List.of(9007199254740997L))
                .setLossReportEvidenceIds(List.of(9007199254740998L))
                .setReportUploadTasks(List.of(new MesProductionReleaseReportUploadTaskRespVO()
                        .setNodeType("INCOMING_INSPECTION_REPORT")
                        .setBatchTaskId(9007199254740999L)
                        .setWorkTaskId(9007199254741000L)
                        .setCandidateUserIds(List.of(9007199254741001L))
                        .setStatus("TODO")))
                .setVersion(2)
                .setDecidedBy(9007199254741002L);

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(response));

        for (String field : Set.of("applicationId", "pqcReleaseWorkTaskId", "batchExecutionId", "decidedBy")) {
            assertTrue(json.get(field).isTextual(), field + " must be serialized as a string");
        }
        assertTrue(json.get("batchRecordEvidenceIds").get(0).isTextual());
        assertTrue(json.get("processInspectionEvidenceIds").get(0).isTextual());
        assertTrue(json.get("lossReportEvidenceIds").get(0).isTextual());
        assertTrue(json.get("reportUploadTasks").get(0).get("batchTaskId").isTextual());
        assertTrue(json.get("reportUploadTasks").get(0).get("workTaskId").isTextual());
        assertTrue(json.get("reportUploadTasks").get(0).get("candidateUserIds").get(0).isTextual());
    }

    @Test
    void approveAndRejectRequestsEnforceIdempotencyAndRejectReasonContracts() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        MesPqcProductionReleaseApproveReqVO approve = new MesPqcProductionReleaseApproveReqVO();
        approve.setApplicationId(1L);
        approve.setPqcReleaseWorkTaskId(2L);
        approve.setExpectedVersion(1);
        approve.setIdempotencyKey("pqc-approve-1");
        assertTrue(validator.validate(approve).isEmpty());
        approve.setIdempotencyKey("contains space");
        assertTrue(validator.validate(approve).stream()
                .anyMatch(item -> item.getPropertyPath().toString().equals("idempotencyKey")));

        MesPqcProductionReleaseRejectReqVO reject = new MesPqcProductionReleaseRejectReqVO();
        reject.setApplicationId(1L);
        reject.setPqcReleaseWorkTaskId(2L);
        reject.setExpectedVersion(1);
        reject.setIdempotencyKey("pqc-reject-1");
        reject.setRejectReason("不符合放行要求");
        assertTrue(validator.validate(reject).isEmpty());
        reject.setRejectReason("");
        assertTrue(validator.validate(reject).stream()
                .anyMatch(item -> item.getPropertyPath().toString().equals("rejectReason")));
    }

    @Test
    void controllerExposesExactSp2PathsAndPermissions() throws Exception {
        Method approve = MesProductionReleaseController.class.getMethod(
                "approve", MesPqcProductionReleaseApproveReqVO.class);
        Method reject = MesProductionReleaseController.class.getMethod(
                "reject", MesPqcProductionReleaseRejectReqVO.class);
        Method get = MesProductionReleaseController.class.getMethod("get", Long.class);

        assertEquals("/pqc/approve", approve.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("/pqc/reject", reject.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("/get", get.getAnnotation(GetMapping.class).value()[0]);
        assertEquals("@ss.hasPermission('mes:pro-production-release:pqc-approve')",
                approve.getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-production-release:pqc-approve')",
                reject.getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-production-release:query')",
                get.getAnnotation(PreAuthorize.class).value());
    }
}
