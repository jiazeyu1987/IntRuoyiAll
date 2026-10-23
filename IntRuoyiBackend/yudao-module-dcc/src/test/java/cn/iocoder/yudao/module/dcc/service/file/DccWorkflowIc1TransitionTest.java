package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteNodeSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.service.route.DccActionApprovalRoutePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_READY;
import static org.junit.jupiter.api.Assertions.*;

class DccWorkflowIc1TransitionTest {
    @Test
    void uploadWithoutTrainingGoesDirectlyToDocControlReview() {
        assertEquals("PENDING_DOC_CONTROL_REVIEW", next(false, null));
    }

    @Test
    void revisionRequiresTrainingEvidenceBeforeDocControlReview() {
        assertEquals("PENDING_APPLICANT_TRAINING_RECORD", next(true, null));
        assertEquals("PENDING_DOC_CONTROL_REVIEW", next(true, 42L));
    }

    @Test
    void obsoleteRouteTerminatesAfterApprovalAndRejectsThirdStage() {
        assertDoesNotThrow(() -> DccActionApprovalRoutePolicy.validateSaveNodes("OBSOLETE",
                List.of(node(1, "DEPT", "ALL", 100), node(2, "USER", "ANY", null)),
                CONTROLLED_FILE_ROUTE_NOT_READY));
        assertThrows(RuntimeException.class, () -> DccActionApprovalRoutePolicy.validateSaveNodes("OBSOLETE",
                List.of(node(1, "DEPT", "ALL", 100), node(2, "USER", "ANY", null),
                        node(3, "USER", "ANY", null)), CONTROLLED_FILE_ROUTE_NOT_READY));
    }

    private String next(boolean training, Long evidence) {
        return ReflectionTestUtils.invokeMethod(new DccControlledFileWorkflowServiceImpl(),
                "resolveNextStatusAfterApprove", DccControlledFileDO.builder()
                        .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                        .needTraining(training).trainingRecordFileId(evidence).build(),
                DccControlledFileStageCodeEnum.MATRIX_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW);
    }

    private DccApprovalRouteNodeSaveReqVO node(int stage, String source, String method, Integer ratio) {
        DccApprovalRouteNodeSaveReqVO node = new DccApprovalRouteNodeSaveReqVO();
        node.setStageNo(stage);
        node.setCandidateSourceType(source);
        node.setApproveMethod(method);
        node.setApproveRatio(ratio);
        node.setRequired(true);
        return node;
    }
}
