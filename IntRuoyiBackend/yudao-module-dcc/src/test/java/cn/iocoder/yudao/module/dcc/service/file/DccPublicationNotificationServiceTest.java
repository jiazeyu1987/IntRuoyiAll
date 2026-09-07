package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationDeliveryMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_MANAGE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_REASON_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.clearInvocations;

class DccPublicationNotificationServiceTest extends BaseMockitoUnitTest {

    @Mock private DccPublicationFollowupBatchMapper batchMapper;
    @Mock private DccPublicationNotificationCandidateMapper candidateMapper;
    @Mock private DccPublicationNotificationDeliveryMapper deliveryMapper;
    @Mock private DccPublicationNotificationAuditMapper auditMapper;
    @Mock private DccPublicationNotificationPostCommitScheduler scheduler;
    @Mock private DccPublicationNotificationDispatchOrchestrator orchestrator;
    @Mock private PermissionApi permissionApi;
    @Mock private DccPublicationFollowupStatusService statusService;
    @InjectMocks private DccPublicationNotificationServiceImpl service;

    private final AtomicLong ids = new AtomicLong(100L);
    private final AtomicReference<DccPublicationNotificationDeliveryDO> inserted = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        lenient().when(deliveryMapper.insertOrKeepExisting(any())).thenAnswer(invocation -> {
            DccPublicationNotificationDeliveryDO row = invocation.getArgument(0);
            row.setId(ids.getAndIncrement());
            inserted.set(row);
            return 1;
        });
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void materializeForPublicationBatch_onlyActiveCandidatesBecomePendingDeliveries() {
        when(batchMapper.selectById(7L)).thenReturn(batch());
        when(candidateMapper.selectListByBatchId(1L, 7L)).thenReturn(List.of(
                candidate(11L, 21L, "ACTIVE"),
                candidate(12L, 22L, "INACTIVE"),
                candidate(13L, 23L, "MISSING")));
        when(deliveryMapper.selectByCandidateId(1L, 11L)).thenAnswer(invocation -> inserted.get());

        service.materializeForPublicationBatch(7L);

        ArgumentCaptor<DccPublicationNotificationDeliveryDO> captor =
                ArgumentCaptor.forClass(DccPublicationNotificationDeliveryDO.class);
        verify(deliveryMapper).insertOrKeepExisting(captor.capture());
        assertEquals(21L, captor.getValue().getUserId());
        assertEquals("PENDING", captor.getValue().getStatus());
        verify(auditMapper).insert(any(DccPublicationNotificationAuditDO.class));
        verify(scheduler).scheduleAfterCommit(1L, 7L);
    }

    @Test
    void retryDelivery_requiresDocControlRoleAndApprovePermissionBeforeDispatch() {
        when(permissionApi.hasAnyRoles(9L, "doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(false);

        assertServiceException(() -> service.retryDelivery(9L, 100L, 2, "再次发送"),
                PUBLICATION_NOTIFICATION_MANAGE_DENIED);

        verifyNoInteractions(orchestrator);
    }

    @Test
    void retryDelivery_requiresReasonAndDelegatesOnlyAfterAllThreePermissionsPass() {
        when(permissionApi.hasAnyRoles(9L, "doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L,
                "dcc:controlled-file:publication-followup:manage")).thenReturn(true);

        assertServiceException(() -> service.retryDelivery(9L, 100L, 2, " "),
                PUBLICATION_NOTIFICATION_REASON_REQUIRED);
        service.retryDelivery(9L, 100L, 2, "人工恢复");

        verify(orchestrator).retryDelivery(1L, 9L, 100L, 2, "人工恢复");
    }

    @Test
    void materializeForPublicationBatch_duplicateValidatesIdentityAndWritesNoDuplicateAudit() {
        when(batchMapper.selectById(7L)).thenReturn(batch());
        when(candidateMapper.selectListByBatchId(1L, 7L)).thenReturn(
                List.of(candidate(11L, 21L, "ACTIVE")));
        DccPublicationNotificationDeliveryDO existing = DccPublicationNotificationDeliveryDO.builder()
                .id(88L).batchId(7L).candidateId(11L).userId(21L)
                .businessKey("DCC_PUBLICATION:7:USER:21").templateCode("dcc_publication_released")
                .status("PENDING").creationToken("existing-token").build();
        existing.setTenantId(1L);
        when(deliveryMapper.selectByCandidateId(1L, 11L)).thenReturn(existing);

        service.materializeForPublicationBatch(7L);

        verify(auditMapper, never()).insert(any(DccPublicationNotificationAuditDO.class));
        clearInvocations(scheduler, auditMapper);
        existing.setUserId(22L);

        assertThrows(IllegalStateException.class, () -> service.materializeForPublicationBatch(7L));
        verify(scheduler, never()).scheduleAfterCommit(1L, 7L);
        verify(auditMapper, never()).insert(any(DccPublicationNotificationAuditDO.class));
    }

    private DccPublicationFollowupBatchDO batch() {
        DccPublicationFollowupBatchDO batch = DccPublicationFollowupBatchDO.builder()
                .id(7L).publishedControlledFileId(70L).publishedMasterId(700L).build();
        batch.setTenantId(1L);
        return batch;
    }

    private DccPublicationNotificationCandidateDO candidate(Long id, Long userId, String status) {
        DccPublicationNotificationCandidateDO row = DccPublicationNotificationCandidateDO.builder()
                .id(id).batchId(7L).userId(userId).resolutionStatus(status).build();
        row.setTenantId(1L);
        return row;
    }
}
