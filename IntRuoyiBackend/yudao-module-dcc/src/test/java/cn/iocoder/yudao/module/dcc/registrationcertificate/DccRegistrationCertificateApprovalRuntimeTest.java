package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateBpmBindingDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateBpmBindingMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateGrantMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalCallbackCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.change.DccRegistrationCertificateChangeService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.grant.DccRegistrationCertificateGrantService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_APPROVAL_REJECT_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_STATUS_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVER_ROLE_CODE;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVAL_PERMISSION;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.REQUEST_TYPE_UPLOAD_CERTIFICATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DccRegistrationCertificateApprovalRuntimeTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ACTOR_ID = 99L;

    @Mock
    private DccRegistrationCertificateAccessRequestMapper requestMapper;
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
    private DccRegistrationCertificateChangeService changeService;

    private DccRegistrationCertificateApprovalService service;

    @BeforeEach
    void setUp() {
        service = new DccRegistrationCertificateApprovalService(
                requestMapper, bindingMapper, grantMapper, grantService,
                bpmProcessInstanceApi, companyScopeApi, roleApi, permissionApi,
                businessClock, renewalService, uploadService, changeService);
        lenient().when(requestMapper.updateById(any(DccRegistrationCertificateAccessRequestDO.class))).thenReturn(1);
        lenient().when(bindingMapper.updateById(any(DccRegistrationCertificateBpmBindingDO.class))).thenReturn(1);
        lenient().when(grantMapper.updateById(any(DccRegistrationCertificateGrantDO.class))).thenReturn(1);
    }

    @Test
    void approveTransitionsBindingAndCreatesGrantsExactlyOnce() {
        DccRegistrationCertificateAccessRequestDO request = request("BPM_BOUND");
        DccRegistrationCertificateBpmBindingDO binding = binding("RUNNING");
        when(bindingMapper.selectByProcessInstanceId(TENANT_ID, "proc-1")).thenReturn(binding);
        when(requestMapper.selectById(1001L)).thenReturn(request);
        when(grantService.createGrantsForApprovedRequest(eq(TENANT_ID), eq(200L), eq(1001L),
                eq("approval-1"), any(LocalDateTime.class)))
                .thenReturn(List.of(DccRegistrationCertificateGrantDO.builder().id(7001L).build()));

        DccRegistrationCertificateApprovalResult result = service.approve(
                TENANT_ID, 200L,
                new DccRegistrationCertificateApprovalCallbackCommand(
                        "proc-1", "approval-1", null, LocalDateTime.of(2026, 8, 19, 10, 0)));

        assertEquals("APPROVED", result.status());
        assertEquals(List.of(7001L), result.grantIds());
        assertEquals("APPROVED", request.getStatus());
        assertEquals("APPROVED", binding.getStatus());
        verify(grantService).createGrantsForApprovedRequest(eq(TENANT_ID), eq(200L), eq(1001L),
                eq("approval-1"), any(LocalDateTime.class));
        verify(renewalService, never()).approveRenewalRequest(any(), any(), any(), any());
    }

    @Test
    void approveRenewalUploadTriggersRenewalAndDoesNotCreateAccessGrant() {
        DccRegistrationCertificateAccessRequestDO request = request("BPM_BOUND");
        request.setRequestType(REQUEST_TYPE_UPLOAD_CERTIFICATE);
        request.setDetailJson("{\"operation\":\"RENEWAL_CERTIFICATE\"}");
        DccRegistrationCertificateBpmBindingDO binding = binding("RUNNING");
        when(bindingMapper.selectByProcessInstanceId(TENANT_ID, "proc-1")).thenReturn(binding);
        when(requestMapper.selectById(1001L)).thenReturn(request);

        DccRegistrationCertificateApprovalResult result = service.approve(
                TENANT_ID, 200L,
                new DccRegistrationCertificateApprovalCallbackCommand(
                        "proc-1", "approval-renewal-1", null,
                        LocalDateTime.of(2026, 8, 19, 10, 0)));

        assertEquals("APPROVED", result.status());
        assertEquals(List.of(), result.grantIds());
        verify(renewalService).approveRenewalRequest(TENANT_ID, 200L, 1001L, "approval-renewal-1");
        verify(grantService, never()).createGrantsForApprovedRequest(any(), any(), any(), any(), any());
        verify(uploadService, never()).approveUploadRequest(any(), any(), any(), any());
    }

    @Test
    void approveInitialUploadTriggersUploadAndDoesNotCreateAccessGrant() {
        DccRegistrationCertificateAccessRequestDO request = request("BPM_BOUND");
        request.setRequestType(REQUEST_TYPE_UPLOAD_CERTIFICATE);
        request.setDetailJson("{\"operation\":\"UPLOAD_CERTIFICATE\"}");
        DccRegistrationCertificateBpmBindingDO binding = binding("RUNNING");
        when(bindingMapper.selectByProcessInstanceId(TENANT_ID, "proc-1")).thenReturn(binding);
        when(requestMapper.selectById(1001L)).thenReturn(request);

        DccRegistrationCertificateApprovalResult result = service.approve(
                TENANT_ID, 200L,
                new DccRegistrationCertificateApprovalCallbackCommand(
                        "proc-1", "approval-upload-1", null,
                        LocalDateTime.of(2026, 8, 19, 10, 0)));

        assertEquals("APPROVED", result.status());
        assertEquals(List.of(), result.grantIds());
        verify(uploadService).approveUploadRequest(TENANT_ID, 200L, 1001L, "approval-upload-1");
        verify(renewalService, never()).approveRenewalRequest(any(), any(), any(), any());
        verify(grantService, never()).createGrantsForApprovedRequest(any(), any(), any(), any(), any());
    }

    @Test
    void rejectRequiresReasonAndDoesNotCreateGrant() {
        ServiceException error = assertThrows(ServiceException.class, () -> service.reject(
                TENANT_ID, 200L,
                new DccRegistrationCertificateApprovalCallbackCommand("proc-1", "approval-1", "  ", null)));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_APPROVAL_REJECT_REASON_REQUIRED.getCode(), error.getCode());
        verify(grantService, never()).createGrantsForApprovedRequest(any(), any(), any(), any(), any());
    }

    @Test
    void rejectPersistsReasonAndWithdrawCancelsNativeProcess() {
        DccRegistrationCertificateAccessRequestDO request = request("BPM_BOUND");
        DccRegistrationCertificateBpmBindingDO binding = binding("RUNNING");
        when(bindingMapper.selectByProcessInstanceId(TENANT_ID, "proc-1")).thenReturn(binding);
        when(requestMapper.selectById(1001L)).thenReturn(request);

        DccRegistrationCertificateApprovalResult rejected = service.reject(
                TENANT_ID, 200L,
                new DccRegistrationCertificateApprovalCallbackCommand("proc-1", "approval-1", "缺少正式文件", null));
        assertEquals("REJECTED", rejected.status());
        assertEquals("缺少正式文件", request.getRejectReason());
        verify(renewalService, never()).rejectRenewalRequest(any(), any(), any(), any());
        verify(uploadService, never()).rejectUploadRequest(any(), any(), any(), any(), any());

        request.setStatus("BPM_BOUND");
        binding.setStatus("RUNNING");
        when(bindingMapper.selectByRequestId(TENANT_ID, 1001L)).thenReturn(binding);
        DccRegistrationCertificateApprovalResult withdrawn = service.withdraw(TENANT_ID, ACTOR_ID, 1001L, "申请人撤回");
        assertEquals("WITHDRAWN", withdrawn.status());
        verify(bpmProcessInstanceApi).cancelProcessInstance(ACTOR_ID, "proc-1", "申请人撤回");
    }

    @Test
    void rejectInitialUploadDelegatesToUploadCleanup() {
        DccRegistrationCertificateAccessRequestDO request = request("BPM_BOUND");
        request.setRequestType(REQUEST_TYPE_UPLOAD_CERTIFICATE);
        request.setDetailJson("{\"operation\":\"UPLOAD_CERTIFICATE\"}");
        DccRegistrationCertificateBpmBindingDO binding = binding("RUNNING");
        when(bindingMapper.selectByProcessInstanceId(TENANT_ID, "proc-1")).thenReturn(binding);
        when(requestMapper.selectById(1001L)).thenReturn(request);

        DccRegistrationCertificateApprovalResult rejected = service.reject(
                TENANT_ID, 200L,
                new DccRegistrationCertificateApprovalCallbackCommand(
                        "proc-1", "approval-upload-reject-1", "缺少正式文件", null));

        assertEquals("REJECTED", rejected.status());
        verify(uploadService).rejectUploadRequest(
                TENANT_ID, 200L, 1001L, "approval-upload-reject-1", "缺少正式文件");
        verify(renewalService, never()).rejectRenewalRequest(any(), any(), any(), any());
        verify(grantService, never()).createGrantsForApprovedRequest(any(), any(), any(), any(), any());
    }

    @Test
    void withdrawIsRestrictedToRequesterAndRevokeNeedsActiveGrantReason() {
        DccRegistrationCertificateAccessRequestDO request = request("SUBMITTED");
        when(requestMapper.selectById(1001L)).thenReturn(request);
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.withdraw(TENANT_ID, 200L, 1001L, "申请人撤回"));
        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT.getCode(), error.getCode());

        DccRegistrationCertificateGrantDO grant = DccRegistrationCertificateGrantDO.builder()
                .id(7001L).ownerCompanyId(10L).status("ACTIVE").build();
        grant.setTenantId(TENANT_ID);
        when(grantMapper.selectById(7001L)).thenReturn(grant);
        RoleRespDTO role = new RoleRespDTO();
        role.setId(8L);
        role.setCode(APPROVER_ROLE_CODE);
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(roleApi.getRoleByCode(APPROVER_ROLE_CODE)).thenReturn(role);
        when(permissionApi.hasAnyPermissionsInRoles(List.of(8L), APPROVAL_PERMISSION)).thenReturn(true);
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(8L))).thenReturn(java.util.Set.of(200L));
        service.revokeGrant(TENANT_ID, 200L, 7001L, "范围失效");
        assertEquals("REVOKED", grant.getStatus());
        assertEquals(200L, grant.getRevokedBy());

        grant.setStatus("REVOKED");
        ServiceException grantError = assertThrows(ServiceException.class,
                () -> service.revokeGrant(TENANT_ID, 200L, 7001L, "再次撤销"));
        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_STATUS_INVALID.getCode(), grantError.getCode());
    }

    @Test
    void terminalCallbackRejectsDifferentApprovalKeyReplay() {
        DccRegistrationCertificateAccessRequestDO request = request("APPROVED");
        DccRegistrationCertificateBpmBindingDO binding = binding("APPROVED");
        binding.setDetailJson("{\"terminalApprovalKey\":\"approval-1\"}");
        when(bindingMapper.selectByProcessInstanceId(TENANT_ID, "proc-1")).thenReturn(binding);
        when(requestMapper.selectById(1001L)).thenReturn(request);

        ServiceException error = assertThrows(ServiceException.class, () -> service.approve(
                TENANT_ID, 200L,
                new DccRegistrationCertificateApprovalCallbackCommand(
                        "proc-1", "approval-2", null, LocalDateTime.of(2026, 8, 19, 10, 0))));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT.getCode(), error.getCode());
        verify(grantService, never()).createGrantsForApprovedRequest(any(), any(), any(), any(), any());
    }

    @Test
    void nativeCancellationUsesRequesterIdentityAndDoesNotCancelProcessAgain() {
        DccRegistrationCertificateAccessRequestDO request = request("BPM_BOUND");
        DccRegistrationCertificateBpmBindingDO binding = binding("RUNNING");
        when(bindingMapper.selectByProcessInstanceId(TENANT_ID, "proc-1")).thenReturn(binding);
        when(requestMapper.selectById(1001L)).thenReturn(request);

        DccRegistrationCertificateApprovalResult result = service.cancelFromNative(
                TENANT_ID, ACTOR_ID,
                new DccRegistrationCertificateApprovalCallbackCommand(
                        "proc-1", "BPM:proc-1:4", "申请人撤回", LocalDateTime.of(2026, 8, 19, 10, 0)));

        assertEquals("WITHDRAWN", result.status());
        assertEquals("WITHDRAWN", request.getStatus());
        assertEquals("WITHDRAWN", binding.getStatus());
        verify(bpmProcessInstanceApi, never()).cancelProcessInstance(any(), any(), any());
    }

    private static DccRegistrationCertificateAccessRequestDO request(String status) {
        DccRegistrationCertificateAccessRequestDO request = DccRegistrationCertificateAccessRequestDO.builder()
                .id(1001L).ownerCompanyId(10L).certificateId(2001L).requesterUserId(ACTOR_ID)
                .requestType("VIEW_OLD_CERTIFICATE").requestKey("request-1001").status(status)
                .requestedAt(LocalDateTime.of(2026, 8, 19, 9, 0)).build();
        request.setTenantId(TENANT_ID);
        return request;
    }

    private static DccRegistrationCertificateBpmBindingDO binding(String status) {
        DccRegistrationCertificateBpmBindingDO binding = DccRegistrationCertificateBpmBindingDO.builder()
                .id(9001L).requestId(1001L).businessKey("DCC_REG_CERT_ACCESS:1001")
                .bpmProcessInstanceId("proc-1").status(status).build();
        binding.setTenantId(TENANT_ID);
        return binding;
    }
}
