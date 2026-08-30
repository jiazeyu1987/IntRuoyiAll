package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.approval.adapter.DccRegistrationCertificateApprovalStatusListener;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateBpmBindingDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateBpmBindingMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateGrantMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalStartCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.grant.DccRegistrationCertificateGrantService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Collection;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_BPM_CANDIDATE_EMPTY;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVAL_PERMISSION;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVAL_TASK_DEFINITION_KEY;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVER_ROLE_CODE;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.PROCESS_DEFINITION_KEY;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.UPLOAD_APPROVAL_PERMISSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccRegistrationCertificateBpmIntegrationTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ACTOR_ID = 99L;
    private static final Long REQUEST_ID = 1001L;

    @Mock
    private DccRegistrationCertificateAccessRequestMapper requestMapper;
    @Mock
    private DccRegistrationCertificateAccessRequestFileMapper requestFileMapper;
    @Mock
    private DccRegistrationCertificateBpmBindingMapper bindingMapper;
    @Mock
    private DccRegistrationCertificateGrantMapper grantMapper;
    @Mock
    private DccRegistrationCertificateGrantService grantService;
    @Mock
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock
    private MdmCompanyScopeApi companyScopeApi;
    @Mock
    private RoleApi roleApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DccRegistrationCertificateBusinessClock businessClock;
    @Mock
    private DccRegistrationCertificateRenewalService renewalService;
    @Mock
    private DccRegistrationCertificateUploadService uploadService;
    @Mock
    private DccRegistrationCertificateApprovalService listenerApprovalService;

    private DccRegistrationCertificateApprovalService service;
    private DccRegistrationCertificateApprovalStatusListener listener;

    @BeforeEach
    void setUp() {
        service = new DccRegistrationCertificateApprovalService(
                requestMapper, bindingMapper, grantMapper, grantService,
                bpmProcessInstanceApi, companyScopeApi, roleApi, permissionApi,
                businessClock, renewalService, uploadService);
        lenient().when(roleApi.getRoleByCode(APPROVER_ROLE_CODE)).thenReturn(approverRole());
        listener = new DccRegistrationCertificateApprovalStatusListener(listenerApprovalService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void startUsesNativeOnlyWithStableBusinessKeyAndSelfReviewExclusion() {
        DccRegistrationCertificateAccessRequestDO request = submittedRequest();
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(request);
        when(bindingMapper.selectByRequestId(TENANT_ID, REQUEST_ID)).thenReturn(null);
        when(companyScopeApi.resolveRecipientUserIds(eq(10L), any(Collection.class), eq(APPROVAL_PERMISSION)))
                .thenReturn(new LinkedHashSet<>(List.of(ACTOR_ID, 120L, 110L)));
        when(bpmProcessInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("proc-1001");
        doAnswer(invocation -> {
            DccRegistrationCertificateBpmBindingDO binding = invocation.getArgument(0);
            binding.setId(9001L);
            return 1;
        }).when(bindingMapper).insert(any(DccRegistrationCertificateBpmBindingDO.class));
        when(requestMapper.updateById(any(DccRegistrationCertificateAccessRequestDO.class))).thenReturn(1);

        DccRegistrationCertificateApprovalResult result = service.startNativeApproval(
                TENANT_ID, ACTOR_ID,
                new DccRegistrationCertificateApprovalStartCommand(REQUEST_ID));

        assertEquals("proc-1001", result.processInstanceId());
        assertEquals("BPM_BOUND", result.status());
        assertEquals("BPM_BOUND", request.getStatus());
        assertEquals("proc-1001", request.getBpmProcessInstanceId());
        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> captor = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(bpmProcessInstanceApi).createProcessInstance(eq(ACTOR_ID), captor.capture());
        BpmProcessInstanceCreateReqDTO bpmRequest = captor.getValue();
        assertEquals(PROCESS_DEFINITION_KEY, bpmRequest.getProcessDefinitionKey());
        assertEquals("DCC_REG_CERT_ACCESS:1001", bpmRequest.getBusinessKey());
        assertEquals(List.of(110L, 120L), bpmRequest.getStartUserSelectAssignees().get(APPROVAL_TASK_DEFINITION_KEY));
        assertEquals(REQUEST_ID, bpmRequest.getVariables().get("requestId"));
    }

    @Test
    void startUsesUploadApprovalPermissionForUploadRequests() {
        DccRegistrationCertificateAccessRequestDO request = submittedUploadRequest();
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(request);
        when(bindingMapper.selectByRequestId(TENANT_ID, REQUEST_ID)).thenReturn(null);
        when(permissionApi.hasAnyPermissionsInRoles(List.of(8L), UPLOAD_APPROVAL_PERMISSION))
                .thenReturn(true);
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(8L)))
                .thenReturn(new LinkedHashSet<>(List.of(ACTOR_ID, 120L)));
        when(bpmProcessInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("proc-upload-1001");
        when(bindingMapper.insert(any(DccRegistrationCertificateBpmBindingDO.class))).thenReturn(1);
        when(requestMapper.updateById(any(DccRegistrationCertificateAccessRequestDO.class))).thenReturn(1);

        DccRegistrationCertificateApprovalResult result = service.startNativeApproval(
                TENANT_ID, ACTOR_ID, new DccRegistrationCertificateApprovalStartCommand(REQUEST_ID));

        assertEquals("proc-upload-1001", result.processInstanceId());
        verify(permissionApi).hasAnyPermissionsInRoles(List.of(8L), UPLOAD_APPROVAL_PERMISSION);
        verify(permissionApi).getUserRoleIdListByRoleIds(List.of(8L));
        verify(companyScopeApi, never()).resolveRecipientUserIds(eq(10L), any(Collection.class), eq(UPLOAD_APPROVAL_PERMISSION));
    }

    @Test
    void startCarriesRenewalOperationIntoNativeApprovalVariables() {
        DccRegistrationCertificateAccessRequestDO request = submittedUploadRequest();
        request.setDetailJson("{\"operation\":\"RENEWAL_CERTIFICATE\"}");
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(request);
        when(bindingMapper.selectByRequestId(TENANT_ID, REQUEST_ID)).thenReturn(null);
        when(permissionApi.hasAnyPermissionsInRoles(List.of(8L), UPLOAD_APPROVAL_PERMISSION))
                .thenReturn(true);
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(8L)))
                .thenReturn(new LinkedHashSet<>(List.of(ACTOR_ID, 120L)));
        when(bpmProcessInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("proc-renewal-1001");
        when(bindingMapper.insert(any(DccRegistrationCertificateBpmBindingDO.class))).thenReturn(1);
        when(requestMapper.updateById(any(DccRegistrationCertificateAccessRequestDO.class))).thenReturn(1);

        service.startNativeApproval(
                TENANT_ID, ACTOR_ID, new DccRegistrationCertificateApprovalStartCommand(REQUEST_ID));

        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> captor =
                ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(bpmProcessInstanceApi).createProcessInstance(eq(ACTOR_ID), captor.capture());
        assertEquals("RENEWAL_CERTIFICATE", captor.getValue().getVariables().get("requestOperation"));
    }

    @Test
    void approvedNativeContractCannotBeOverriddenByCallerSuppliedProcessRoleOrPermission() {
        DccRegistrationCertificateAccessRequestDO request = submittedRequest();
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(request);
        when(bindingMapper.selectByRequestId(TENANT_ID, REQUEST_ID)).thenReturn(null);
        when(companyScopeApi.resolveRecipientUserIds(eq(10L), any(Collection.class), any()))
                .thenReturn(Set.of(120L));
        when(bpmProcessInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("proc-fixed-contract");
        when(bindingMapper.insert(any(DccRegistrationCertificateBpmBindingDO.class))).thenReturn(1);
        when(requestMapper.updateById(any(DccRegistrationCertificateAccessRequestDO.class))).thenReturn(1);

        service.startNativeApproval(TENANT_ID, ACTOR_ID,
                new DccRegistrationCertificateApprovalStartCommand(REQUEST_ID));

        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> bpmCaptor =
                ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(bpmProcessInstanceApi).createProcessInstance(eq(ACTOR_ID), bpmCaptor.capture());
        assertEquals(PROCESS_DEFINITION_KEY, bpmCaptor.getValue().getProcessDefinitionKey());
        assertEquals(List.of(120L), bpmCaptor.getValue().getStartUserSelectAssignees()
                .get(APPROVAL_TASK_DEFINITION_KEY));

        ArgumentCaptor<Collection<Long>> roleIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(companyScopeApi).resolveRecipientUserIds(eq(10L), roleIdsCaptor.capture(),
                eq(APPROVAL_PERMISSION));
        assertEquals(List.of(8L), List.copyOf(roleIdsCaptor.getValue()));
    }

    @Test
    void accessRequestControllerMustWireSubmittedRequestIntoNativeApproval() {
        assertTrue(Arrays.stream(cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest
                        .DccRegistrationCertificateAccessRequestController.class.getDeclaredConstructors())
                .anyMatch(constructor -> Arrays.asList(constructor.getParameterTypes())
                        .contains(DccRegistrationCertificateApprovalService.class)),
                "access request submission must invoke the approved Native BPM adapter after durable request persistence");
        try {
            var submit = cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest
                    .DccRegistrationCertificateAccessRequestController.class.getDeclaredMethod(
                            "submit", String.class,
                            cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo
                                    .DccRegistrationCertificateAccessRequestSubmitReqVO.class);
            assertTrue(submit.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class),
                    "access request persistence and Native BPM binding must share one rollback boundary");
        } catch (NoSuchMethodException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    void duplicateStartReplaysBindingWithoutCreatingAnotherNativeProcess() {
        DccRegistrationCertificateAccessRequestDO request = submittedRequest();
        request.setStatus("BPM_BOUND");
        request.setBpmProcessInstanceId("proc-1001");
        DccRegistrationCertificateBpmBindingDO binding = runningBinding();
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(request);
        when(bindingMapper.selectByRequestId(TENANT_ID, REQUEST_ID)).thenReturn(binding);

        DccRegistrationCertificateApprovalResult result = service.startNativeApproval(
                TENANT_ID, ACTOR_ID,
                new DccRegistrationCertificateApprovalStartCommand(REQUEST_ID));

        assertEquals("proc-1001", result.processInstanceId());
        assertEquals("BPM_BOUND", result.status());
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any());
        verify(companyScopeApi, never()).resolveRecipientUserIds(any(), any(), any());
    }

    @Test
    void emptyCandidatesFailClosedBeforeBpmCreate() {
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(submittedRequest());
        when(bindingMapper.selectByRequestId(TENANT_ID, REQUEST_ID)).thenReturn(null);
        when(companyScopeApi.resolveRecipientUserIds(eq(10L), any(Collection.class), eq(APPROVAL_PERMISSION)))
                .thenReturn(Set.of(ACTOR_ID));

        ServiceException error = assertThrows(ServiceException.class, () -> service.startNativeApproval(
                TENANT_ID, ACTOR_ID,
                new DccRegistrationCertificateApprovalStartCommand(REQUEST_ID)));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_BPM_CANDIDATE_EMPTY.getCode(), error.getCode());
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any());
        verify(bindingMapper, never()).insert(any(DccRegistrationCertificateBpmBindingDO.class));
    }

    @Test
    void nativeCreateFailureIsVisibleAndDoesNotPersistBinding() {
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(submittedRequest());
        when(bindingMapper.selectByRequestId(TENANT_ID, REQUEST_ID)).thenReturn(null);
        when(companyScopeApi.resolveRecipientUserIds(eq(10L), any(Collection.class), eq(APPROVAL_PERMISSION)))
                .thenReturn(Set.of(120L));
        IllegalStateException outage = new IllegalStateException("BPM unavailable");
        doThrow(outage).when(bpmProcessInstanceApi)
                .createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> service.startNativeApproval(
                TENANT_ID, ACTOR_ID,
                new DccRegistrationCertificateApprovalStartCommand(REQUEST_ID)));

        assertTrue(thrown == outage);
        verify(bindingMapper, never()).insert(any(DccRegistrationCertificateBpmBindingDO.class));
    }

    @Test
    void concurrentDuplicateCancelsLoserNativeProcessAndReplaysWinner() {
        DccRegistrationCertificateAccessRequestDO request = submittedRequest();
        DccRegistrationCertificateAccessRequestDO winnerRequest = submittedRequest();
        winnerRequest.setStatus("BPM_BOUND");
        winnerRequest.setBpmProcessInstanceId("proc-winner");
        DccRegistrationCertificateBpmBindingDO winner = runningBinding();
        winner.setBpmProcessInstanceId("proc-winner");
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(request, winnerRequest);
        when(bindingMapper.selectByRequestId(TENANT_ID, REQUEST_ID)).thenReturn(null, winner);
        when(companyScopeApi.resolveRecipientUserIds(eq(10L), any(Collection.class), eq(APPROVAL_PERMISSION)))
                .thenReturn(Set.of(120L));
        when(bpmProcessInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("proc-loser");
        doThrow(new DuplicateKeyException("winner committed"))
                .when(bindingMapper).insert(any(DccRegistrationCertificateBpmBindingDO.class));

        DccRegistrationCertificateApprovalResult result = service.startNativeApproval(
                TENANT_ID, ACTOR_ID,
                new DccRegistrationCertificateApprovalStartCommand(REQUEST_ID));

        assertEquals("proc-winner", result.processInstanceId());
        verify(bpmProcessInstanceApi).cancelProcessInstance(
                ACTOR_ID, "proc-loser", "duplicate registration certificate access BPM binding");
    }

    @Test
    void persistenceFailureAfterNativeStartCancelsProcessAndPropagatesOriginalFailure() {
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(submittedRequest());
        when(bindingMapper.selectByRequestId(TENANT_ID, REQUEST_ID)).thenReturn(null);
        when(companyScopeApi.resolveRecipientUserIds(eq(10L), any(Collection.class), eq(APPROVAL_PERMISSION)))
                .thenReturn(Set.of(120L));
        when(bpmProcessInstanceApi.createProcessInstance(eq(ACTOR_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("proc-rollback");
        when(bindingMapper.insert(any(DccRegistrationCertificateBpmBindingDO.class))).thenReturn(1);
        IllegalStateException databaseFailure = new IllegalStateException("request update failed");
        doThrow(databaseFailure).when(requestMapper).updateById(any(DccRegistrationCertificateAccessRequestDO.class));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> service.startNativeApproval(
                TENANT_ID, ACTOR_ID,
                new DccRegistrationCertificateApprovalStartCommand(REQUEST_ID)));

        assertTrue(thrown == databaseFailure);
        verify(bpmProcessInstanceApi).cancelProcessInstance(
                ACTOR_ID, "proc-rollback", "registration certificate access BPM persistence failed");
    }

    @Test
    void nativeTerminalEventsDelegateToApprovalServiceWithStableCallbackKey() {
        TenantContextHolder.setTenantId(TENANT_ID);
        BpmProcessInstanceStatusEvent approve = new BpmProcessInstanceStatusEvent("test")
                .setId("proc-approve")
                .setProcessDefinitionKey(PROCESS_DEFINITION_KEY)
                .setBusinessKey("DCC_REG_CERT_ACCESS:1001")
                .setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus())
                .setActorUserId(200L);

        listener.onApplicationEvent(approve);

        ArgumentCaptor<cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalCallbackCommand>
                approveCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalCallbackCommand.class);
        verify(listenerApprovalService).approve(eq(TENANT_ID), eq(200L), approveCaptor.capture());
        assertEquals("proc-approve", approveCaptor.getValue().bpmProcessInstanceId());
        assertEquals("BPM:proc-approve:2", approveCaptor.getValue().approvalKey());

        BpmProcessInstanceStatusEvent reject = new BpmProcessInstanceStatusEvent("test")
                .setId("proc-reject")
                .setProcessDefinitionKey(PROCESS_DEFINITION_KEY)
                .setBusinessKey("DCC_REG_CERT_ACCESS:1002")
                .setStatus(BpmProcessInstanceStatusEnum.REJECT.getStatus())
                .setReason("材料不完整")
                .setActorUserId(201L);
        listener.onApplicationEvent(reject);
        verify(listenerApprovalService).reject(eq(TENANT_ID), eq(201L), any());

        BpmProcessInstanceStatusEvent cancel = new BpmProcessInstanceStatusEvent("test")
                .setId("proc-cancel")
                .setProcessDefinitionKey(PROCESS_DEFINITION_KEY)
                .setBusinessKey("DCC_REG_CERT_ACCESS:1003")
                .setStatus(BpmProcessInstanceStatusEnum.CANCEL.getStatus())
                .setReason("申请人撤回")
                .setActorUserId(ACTOR_ID);
        listener.onApplicationEvent(cancel);
        verify(listenerApprovalService).cancelFromNative(eq(TENANT_ID), eq(ACTOR_ID), any());
    }

    @Test
    void unrelatedNativeProcessDoesNotInvokeRegistrationApproval() {
        TenantContextHolder.setTenantId(TENANT_ID);
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent("test")
                .setId("proc-other").setProcessDefinitionKey("other-process")
                .setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus()).setActorUserId(200L);

        listener.onApplicationEvent(event);

        verify(listenerApprovalService, never()).approve(any(), any(), any());
        verify(listenerApprovalService, never()).reject(any(), any(), any());
        verify(listenerApprovalService, never()).cancelFromNative(any(), any(), any());
    }

    @Test
    void registrationNativeProcessUnknownTerminalStatusFailsFast() {
        TenantContextHolder.setTenantId(TENANT_ID);
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent("test")
                .setId("proc-unknown")
                .setProcessDefinitionKey(PROCESS_DEFINITION_KEY)
                .setBusinessKey("DCC_REG_CERT_ACCESS:1004")
                .setStatus(999)
                .setActorUserId(200L);

        ServiceException error = assertThrows(ServiceException.class, () -> listener.onApplicationEvent(event));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT.getCode(), error.getCode());
        verify(listenerApprovalService, never()).approve(any(), any(), any());
        verify(listenerApprovalService, never()).reject(any(), any(), any());
        verify(listenerApprovalService, never()).cancelFromNative(any(), any(), any());
    }

    private static DccRegistrationCertificateAccessRequestDO submittedRequest() {
        DccRegistrationCertificateAccessRequestDO request = DccRegistrationCertificateAccessRequestDO.builder()
                .id(REQUEST_ID).ownerCompanyId(10L).certificateId(2001L).requesterUserId(ACTOR_ID)
                .requestType("VIEW_OLD_CERTIFICATE").requestKey("request-1001").purpose("legacy review")
                .status("SUBMITTED").build();
        request.setTenantId(TENANT_ID);
        return request;
    }

    private static DccRegistrationCertificateAccessRequestDO submittedUploadRequest() {
        DccRegistrationCertificateAccessRequestDO request = submittedRequest();
        request.setRequestType("UPLOAD_CERTIFICATE");
        request.setDetailJson("{\"operation\":\"UPLOAD_CERTIFICATE\"}");
        return request;
    }

    private static DccRegistrationCertificateBpmBindingDO runningBinding() {
        DccRegistrationCertificateBpmBindingDO binding = DccRegistrationCertificateBpmBindingDO.builder()
                .id(9001L).requestId(REQUEST_ID).businessKey("DCC_REG_CERT_ACCESS:1001")
                .bpmProcessInstanceId("proc-1001").status("RUNNING").build();
        binding.setTenantId(TENANT_ID);
        return binding;
    }

    private static RoleRespDTO approverRole() {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(8L);
        role.setCode(APPROVER_ROLE_CODE);
        role.setName("注册证文控审批");
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return role;
    }
}
