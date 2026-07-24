package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesProEdhrApprovalStatusMappingTest {

    @Test
    void reviewDoneKeepsExecutionSubmittedAndDoesNotMeanApproved() {
        assertEquals(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_SUBMITTED,
                MesProEdhrApprovalStatusMapping.executionStatusAfterReviewApproval(MesProEdhrWorkTaskStatus.DONE));
        assertEquals(MesProEdhrApprovalStatusMapping.ACTION_RESULT_REVIEW_INTERMEDIATE,
                MesProEdhrApprovalStatusMapping.resolveReviewApprovalResultType(true));
        assertEquals(MesProEdhrApprovalStatusMapping.ACTION_RESULT_REVIEW_TO_APPROVE,
                MesProEdhrApprovalStatusMapping.resolveReviewApprovalResultType(false));
    }

    @Test
    void approveDoneIsTheOnlyFinalApprovedTransition() {
        assertEquals(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_APPROVED,
                MesProEdhrApprovalStatusMapping.executionStatusAfterFinalApproval(MesProEdhrWorkTaskStatus.DONE));
        assertEquals(MesProEdhrApprovalStatusMapping.ACTION_RESULT_FINAL_APPROVED,
                MesProEdhrApprovalStatusMapping.resolveApproveResultType());
    }

    @Test
    void reviewRejectCreatesRejectedReworkState() {
        assertEquals(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_REJECTED,
                MesProEdhrApprovalStatusMapping.executionStatusAfterReviewReject(MesProEdhrWorkTaskStatus.DONE));
        assertEquals(MesProEdhrApprovalStatusMapping.ACTION_RESULT_REVIEW_REJECTED_REWORK,
                MesProEdhrApprovalStatusMapping.resolveReviewRejectResultType());
    }

    @Test
    void unfinishedWorkTaskCannotDriveExecutionStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> MesProEdhrApprovalStatusMapping.executionStatusAfterReviewApproval(MesProEdhrWorkTaskStatus.TODO));
        assertThrows(IllegalArgumentException.class,
                () -> MesProEdhrApprovalStatusMapping.executionStatusAfterFinalApproval(MesProEdhrWorkTaskStatus.TODO));
        assertThrows(IllegalArgumentException.class,
                () -> MesProEdhrApprovalStatusMapping.executionStatusAfterReviewReject(MesProEdhrWorkTaskStatus.TODO));
    }
}
