package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;

import org.junit.jupiter.api.Test;

import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrWorkTaskAuthorizationTest {

    @Test
    void parseCandidateSnapshotUserIds_trimsSkipsEmptyTokensAndKeepsFirstOccurrenceOrder() {
        List<Long> userIds = MesProEdhrWorkTaskAuthorization.parseCandidateSnapshotUserIds(" 88,89,,88, 90 ");

        assertEquals(List.of(88L, 89L, 90L), userIds);
    }

    @Test
    void containsCandidate_failsFastWhenSnapshotContainsInvalidToken() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> MesProEdhrWorkTaskAuthorization.containsCandidate("88,invalid-user", 88L));

        assertEquals(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void isAssignedOrCandidate_usesSameCandidateSnapshotRules() {
        MesProEdhrWorkTaskDO workTask = new MesProEdhrWorkTaskDO()
                .setAssigneeUserId(88L)
                .setCandidateUserSnapshot(" 89,90 ");

        assertTrue(MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(workTask, 88L));
        assertTrue(MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(workTask, 90L));
        assertFalse(MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(workTask, 91L));
        assertFalse(MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(workTask, null));
    }
}
