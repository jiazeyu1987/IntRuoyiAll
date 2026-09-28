package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrApprovalTaskAdapterSignaturePropagationTest {

    @Mock
    private MesProEdhrWorkTaskService workTaskService;
    @Mock
    private MesProEdhrReleaseService releaseService;
    @InjectMocks
    private MesProEdhrApprovalTaskAdapter adapter;

    @Test
    void reviewForwardsManagerSignaturePasswordToReleaseService() {
        Long workTaskId = 177L;
        Long releaseTransactionId = 9200L;
        String signaturePassword = "review-password-test-only";
        when(workTaskService.getReleaseApprovalTaskForReview(workTaskId, null))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(workTaskId)
                        .setBusinessScopeType("RELEASE_TRANSACTION")
                        .setBusinessScopeId(releaseTransactionId));
        when(releaseService.get(releaseTransactionId)).thenReturn(new MesProEdhrReleaseRespVO().setVersion(3));

        ApprovalTaskReviewContext context = ApprovalTaskReviewContext.of(188L, ApprovalModuleCode.EDHR,
                "EDHR_WORK_TASK", String.valueOf(workTaskId), String.valueOf(workTaskId), null,
                ApprovalTaskReviewResult.APPROVE, "符合放行要求", signaturePassword, false)
                .setSignatureSubjectId("signed-subject")
                .setSignatureEvidenceHash("a".repeat(64));
        adapter.review(context);

        ArgumentCaptor<MesProEdhrReleaseApproveReqVO> captor =
                ArgumentCaptor.forClass(MesProEdhrReleaseApproveReqVO.class);
        verify(releaseService).approve(captor.capture());
        assertEquals(signaturePassword, captor.getValue().getPassword());
    }
}
