package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MesProEdhrManagerBatchVisibilityTest {
    private static final Long USER = 347L;
    private static final Long BATCH = 1225L;
    private static final Long RELEASE = 226L;
    @Mock private PermissionApi permissionApi;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    private MesProEdhrBatchExecutionVisibilityService service;
    private MesProEdhrBatchExecutionDO batch;

    @BeforeEach
    void setUp() {
        service = new MesProEdhrBatchExecutionVisibilityService();
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        ReflectionTestUtils.setField(service, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(service, "releaseTransactionMapper", releaseTransactionMapper);
        batch = new MesProEdhrBatchExecutionDO().setId(BATCH);
        lenient().when(releaseTransactionMapper.selectByBatchExecutionId(BATCH)).thenReturn(
                new MesProEdhrReleaseTransactionDO().setId(RELEASE).setBatchExecutionId(BATCH)
                        .setReleaseStatus("PENDING_APPROVAL"));
    }

    private MesProEdhrWorkTaskDO managerTask() {
        return new MesProEdhrWorkTaskDO().setId(2735L).setBatchExecutionId(BATCH)
                .setTaskType("RELEASE_APPROVE").setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(RELEASE).setOwnershipLocked(true).setStatus("TODO")
                .setAssigneeUserId(1L).setCandidateUserSnapshot("1, 223, 347");
    }

    private boolean visible(MesProEdhrWorkTaskDO task) {
        lenient().when(workTaskMapper.selectTimelineListByBatchExecutionId(BATCH)).thenReturn(List.of(task));
        return service.canViewBatch(batch, List.of(), USER);
    }

    @Test void frozenManagerCandidateCanReadScopeOnlyBatch() {
        assertTrue(visible(managerTask()));
    }

    @Test void completedManagerCandidateCanReadReleasedEvidence() {
        lenient().when(releaseTransactionMapper.selectByBatchExecutionId(BATCH)).thenReturn(
                new MesProEdhrReleaseTransactionDO().setId(RELEASE).setBatchExecutionId(BATCH)
                        .setReleaseStatus("RELEASED"));
        assertTrue(visible(managerTask().setStatus("DONE")));
    }

    @Test void roleMembershipOrDifferentCandidateDoesNotGrantBatchRead() {
        assertFalse(visible(managerTask().setCandidateUserSnapshot("1,223,1347")));
    }

    @Test void assigneeOutsideLockedSnapshotDoesNotGrantRead() {
        assertFalse(visible(managerTask().setAssigneeUserId(USER).setCandidateUserSnapshot("1,223")));
    }

    @Test void cancelledTaskDoesNotGrantRead() {
        assertFalse(visible(managerTask().setStatus("CANCELLED")));
    }

    @Test void oldTransactionCandidateDoesNotGrantCurrentBatchRead() {
        assertFalse(visible(managerTask().setBusinessScopeId(225L)));
    }

    @Test void unrelatedBatchOrTaskTypeDoesNotGrantRead() {
        assertFalse(visible(managerTask().setBatchExecutionId(999L)));
        assertFalse(visible(managerTask().setTaskType("FILL")));
        assertFalse(visible(managerTask().setBusinessScopeType("RELEASE_APPLICATION")));
    }

    @Test void unlockedOrEmptySnapshotDoesNotGrantRead() {
        assertFalse(visible(managerTask().setOwnershipLocked(false)));
        assertFalse(visible(managerTask().setCandidateUserSnapshot("")));
    }

    @Test void missingTransactionDoesNotGrantRead() {
        lenient().when(releaseTransactionMapper.selectByBatchExecutionId(BATCH)).thenReturn(null);
        assertFalse(visible(managerTask()));
    }

    @Test void overviewReadPreservesExistingBehavior() {
        lenient().when(permissionApi.hasAnyPermissions(USER,
                MesProEdhrBatchTaskVisibilityService.OVERVIEW_PERMISSION)).thenReturn(true);
        assertTrue(service.canViewBatch(batch, List.of(), USER));
        verifyNoInteractions(workTaskMapper, releaseTransactionMapper);
    }
}
