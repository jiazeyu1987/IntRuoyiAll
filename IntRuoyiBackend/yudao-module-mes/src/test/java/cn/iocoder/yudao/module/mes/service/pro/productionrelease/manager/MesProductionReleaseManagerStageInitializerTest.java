package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportNodeEvidence;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportSnapshots;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseManagerStageInitializerTest {

    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProductionReleaseRequiredCandidateResolver candidateResolver;

    private MesProductionReleaseManagerStageInitializerImpl initializer;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        initializer = new MesProductionReleaseManagerStageInitializerImpl(
                applicationMapper, batchExecutionMapper, releaseTransactionMapper, workTaskMapper, candidateResolver);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createsPendingApprovalTransactionAndFrozenManagementRepresentativeTask() {
        MesProcessPoolActiveOrderReleaseApplicationDO application = application();
        List<MesProductionReleaseReportNodeEvidence> evidences = evidences();
        String reportSnapshotHash = MesProductionReleaseReportSnapshots.hash(application, evidences);
        when(applicationMapper.selectById(701L)).thenReturn(application);
        when(batchExecutionMapper.selectById(901L)).thenReturn(batch());
        when(candidateResolver.resolveRequiredCandidates(1L,
                MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(
                        77L, MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,
                        List.of(8101L, 8102L), "manager-candidate-hash"));
        AtomicLong ids = new AtomicLong(1000L);
        when(releaseTransactionMapper.insert(any(MesProEdhrReleaseTransactionDO.class))).thenAnswer(invocation -> {
            MesProEdhrReleaseTransactionDO transaction = invocation.getArgument(0);
            transaction.setId(ids.incrementAndGet());
            return 1;
        });
        when(workTaskMapper.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(invocation -> {
            MesProEdhrWorkTaskDO task = invocation.getArgument(0);
            task.setId(ids.incrementAndGet());
            return 1;
        });

        MesProductionReleaseManagerStageInitializationResult result = initializer.initializeManagerReleaseStage(
                new MesProductionReleaseManagerStageInitializationCommand()
                        .setApplicationId(701L)
                        .setBatchExecutionId(901L)
                        .setReportSnapshotHash(reportSnapshotHash)
                        .setReportEvidences(evidences)
                        .setExpectedApplicationVersion(4));

        assertNotNull(result.getReleaseTransactionId());
        assertNotNull(result.getManagerReleaseWorkTaskId());
        assertEquals("manager-candidate-hash", result.getManagerCandidateSnapshotHash());
        ArgumentCaptor<MesProEdhrReleaseTransactionDO> transactionCaptor =
                ArgumentCaptor.forClass(MesProEdhrReleaseTransactionDO.class);
        verify(releaseTransactionMapper).insert(transactionCaptor.capture());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL,
                transactionCaptor.getValue().getReleaseStatus());
        org.junit.jupiter.api.Assertions.assertTrue(
                transactionCaptor.getValue().getPrecheckSnapshotJson().contains(reportSnapshotHash));
        ArgumentCaptor<MesProEdhrWorkTaskDO> taskCaptor = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(workTaskMapper).insert(taskCaptor.capture());
        MesProEdhrWorkTaskDO task = taskCaptor.getValue();
        assertEquals("RELEASE_APPROVE", task.getTaskType());
        assertEquals("RELEASE_TRANSACTION", task.getBusinessScopeType());
        assertEquals("ROLE_GROUP", task.getCandidateSourceType());
        assertEquals(77L, task.getCandidateSourceId());
        assertEquals("8101,8102", task.getCandidateUserSnapshot());
        assertEquals("manager-candidate-hash", task.getResponsibilitySourceVersion());
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO application() {
        return new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(701L)
                .setBatchExecutionId(901L)
                .setApplicationStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setVersion(4)
                .setAppliedBy(7001L);
    }

    private MesProEdhrBatchExecutionDO batch() {
        return new MesProEdhrBatchExecutionDO()
                .setId(901L)
                .setBatchExecutionCode("BE-901")
                .setWorkOrderId(301L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setProductId(401L)
                .setProductCode("P-001")
                .setProductName("Product")
                .setRouteId(501L)
                .setRouteCode("R-001")
                .setRouteName("Route");
    }

    private List<MesProductionReleaseReportNodeEvidence> evidences() {
        return List.of(
                evidence(911L, "FINISHED_PRODUCT_INSPECTION_RECORD", null, 1011L, '1'),
                evidence(912L, "FINISHED_PRODUCT_INSPECTION_REPORT", null, 1012L, '2'),
                evidence(913L, "INCOMING_INSPECTION_REPORT", null, 1013L, '3'),
                evidence(914L, "STERILIZATION_REPORT", "STER-001", 1014L, '4'));
    }

    private MesProductionReleaseReportNodeEvidence evidence(
            Long batchTaskId, String nodeType, String sterilizationBatchNo, Long attachmentId, char hashDigit) {
        return new MesProductionReleaseReportNodeEvidence()
                .setBatchExecutionId(901L)
                .setBatchTaskId(batchTaskId)
                .setNodeType(nodeType)
                .setSterilizationBatchNo(sterilizationBatchNo)
                .setActiveAttachmentVersion(1)
                .setAttachmentIds(List.of(attachmentId))
                .setAttachmentHashes(List.of(String.valueOf(hashDigit).repeat(64)));
    }
}
