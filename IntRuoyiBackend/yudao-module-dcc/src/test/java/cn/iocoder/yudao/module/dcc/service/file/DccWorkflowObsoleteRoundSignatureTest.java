package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignoffAssignmentReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccWorkflowObsoleteRoundSignatureTest {
    @Test
    void obsoleteAssignmentHmacBindsActualObsoleteRoundInsteadOfOriginalUploadRound() throws Exception {
        var files = mock(DccControlledFileMapper.class);
        var masters = mock(DccControlledFileMasterMapper.class);
        var assignments = mock(DccControlledFileTaskAssigneeSnapshotMapper.class);
        var signatures = mock(DccControlledFileSignatureMapper.class);
        var bpm = mock(BpmTaskService.class);
        var runtime = mock(RuntimeService.class);
        var assignment = new DccWorkflowSignoffAssignmentService();
        ReflectionTestUtils.setField(assignment,"remediationService",mock(cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService.class));
        var signing = new DccSignatureVerificationServiceImpl();
        var evidence = spy(new DccControlledFileSignatureEvidenceServiceImpl());
        var signedEvidence = new AtomicReference<DccControlledFileSignatureEvidence>();
        var signedProjection = new AtomicReference<DccControlledFileSignatureDO>();
        var file = DccControlledFileDO.builder().id(10L).masterId(20L).tenantId(1L).versionNo("A/1")
                .fileNumber("SOP-001").sourceFileId(100L).publishedFileId(100L)
                .status("ACTIVE").processInstanceId("original-upload-round").build();
        when(files.selectById(10L)).thenReturn(file);
        when(files.selectByIdAndTenantForUpdate(1L, 10L)).thenReturn(file);
        when(masters.selectByIdForUpdate(20L)).thenReturn(DccControlledFileMasterDO.builder().id(20L).tenantId(1L).build());
        var row = DccControlledFileTaskAssigneeSnapshotDO.builder().id(1L).controlledFileId(10L).stageCode("MATRIX_REVIEW")
                .processInstanceId("obsolete-round-2").departmentId(51L).leaderUserId(99L).assigneeUserId(99L).build();
        when(assignments.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(row);
        when(assignments.updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class))).thenReturn(1);
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("obsolete-task-51");
        when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(task.getProcessDefinitionId()).thenReturn("dcc-controlled-file-obsolete:4:1");
        when(task.getProcessInstanceId()).thenReturn("obsolete-round-2");
        when(task.getTenantId()).thenReturn("1");
        when(task.getTaskLocalVariables()).thenReturn(Map.of(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID,
                "form-2:MATRIX_REVIEW:51"));
        when(bpm.getTask("obsolete-task-51")).thenReturn(task);
        when(bpm.validateTask(99L, "obsolete-task-51")).thenReturn(task);
        when(runtime.getVariables("obsolete-round-2")).thenReturn(Map.of("systemCode", "DCC", "objectType",
                "CONTROLLED_FILE", "actionCode", "OBSOLETE", "objectId", "10"));
        var directory = mock(AdminUserApi.class);
        when(directory.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setDeptId(51L).setStatus(0).setPostIds(Set.of(1L)));
        wire(assignment, "fileMapper", files, "masterMapper", masters, "snapshotMapper", assignments,
                "bpmTaskService", bpm, "runtimeService", runtime, "taskService", mock(TaskService.class),
                "adminUserApi", directory, "readinessService", mock(DccControlledFileRouteReadinessService.class),
                "signatureService", signing);
        var properties = new DccSignatureEvidenceProperties();
        properties.setHmacSecret("test-only-secret"); properties.setKeyVersion("test-round-key");
        var storage = mock(FileService.class);
        when(storage.getFile(100L)).thenReturn(FileDO.builder().id(100L).configId(1L).path("source.pdf").build());
        when(storage.getFileContent(1L, "source.pdf")).thenReturn("actual-frozen-source".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        wire(evidence, "controlledFileMapper", files, "fileService", storage, "signatureEvidenceProperties", properties);
        doAnswer(call -> {
            var result = (DccControlledFileSignatureEvidence) call.callRealMethod(); signedEvidence.set(result); return result;
        }).when(evidence).createEvidence(any());
        var users = mock(AdminUserService.class);
        when(users.getUser(99L)).thenReturn(AdminUserDO.builder().id(99L).username("leader").nickname("负责人").postIds(Set.of(1L)).build());
        var posts = mock(PostService.class);
        var post = new PostDO(); post.setId(1L); post.setName("QA");
        when(posts.getPostList(Set.of(1L))).thenReturn(List.of(post));
        var permissions = mock(PermissionService.class);
        when(permissions.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(1L));
        var roles = mock(RoleService.class);
        var role = new RoleDO(); role.setId(1L); role.setName("文控负责人");
        when(roles.getRoleList(Set.of(1L))).thenReturn(List.of(role));
        var images = mock(DccElectronicSignatureImageService.class);
        when(images.requireActiveSnapshot(99L)).thenReturn(DccElectronicSignatureImageSnapshot.builder().imageId(1L).versionNo(1)
                .fileId(101L).sha256("image-hash").contentType("image/png").fileSize(10L).imageStatus("ACTIVE").verifiedStatus("VALID").build());
        var unified = mock(ElectronicSignatureService.class);
        when(unified.sign(any(ElectronicSignatureCommand.class))).thenReturn(new ElectronicSignatureResult(1L, "VALID",
                LocalDateTime.now(), "test-clock", "test-version", "content-hash", "unified-hash", "SHA-256", "test-key"));
        when(signatures.insert(any(DccControlledFileSignatureDO.class))).thenAnswer(call -> {
            var projection=call.<DccControlledFileSignatureDO>getArgument(0);
            projection.setId(77L); signedProjection.set(projection); return 1;
        });
        wire(signing, "adminUserService", users, "postService", posts, "permissionService", permissions, "roleService", roles,
                "signatureImageService", images, "signatureEvidenceService", evidence, "signatureMapper", signatures,
                "electronicSignatureAuthorizationService", mock(DccElectronicSignatureAuthorizationService.class), "electronicSignatureService", unified);
        var copyBindings=mock(DccControlledFileSignatureBindingMapper.class);
        var savedBindings=new java.util.HashMap<Long,DccControlledFileSignatureBindingDO>();
        when(copyBindings.selectBySignatureId(anyLong())).thenAnswer(call->savedBindings.get(call.getArgument(0)));
        when(copyBindings.insert(any(DccControlledFileSignatureBindingDO.class))).thenAnswer(call->{
            var binding=call.<DccControlledFileSignatureBindingDO>getArgument(0);
            savedBindings.put(binding.getSignatureId(),binding);return 1;
        });
        when(signatures.selectListByControlledFileId(10L)).thenAnswer(ignored->List.of(signedProjection.get()));
        var bindings=new DccControlledFileSignatureBindingService();
        wire(bindings,"bindingMapper",copyBindings,"signatureMapper",signatures,"fileService",storage);
        wire(assignment,"signatureBindingService",bindings);
        var request = new DccSignoffAssignmentReqVO(); request.setTaskId("obsolete-task-51");
        request.setAssigneeUserId(99L); request.setPassword("test-credential"); request.setReason("指派本人");
        TenantContextHolder.setTenantId(1L);
        try {
            assignment.assign(99L, 10L, request);
            assertEquals("obsolete-round-2", JsonUtils.parseObject(signedEvidence.get().getCanonicalPayload(), Map.class).get("processInstanceId"));
            assertEquals("original-upload-round", file.getProcessInstanceId(), "cannot overwrite the historical file workflow to repair evidence");
            assertEquals("obsolete-round-2",signedProjection.get().getProcessInstanceId());
            assertEquals("SOP-001",signedProjection.get().getFileNumberSnapshot());
            assertEquals("v4-workflow",signedProjection.get().getEvidencePayloadVersion());
            when(signatures.selectById(77L)).thenAnswer(ignored->signedProjection.get());
            when(images.verifySignatureSnapshot(any(DccControlledFileSignatureDO.class)))
                    .thenReturn(DccElectronicSignatureImageSnapshot.builder().content(new byte[]{1}).build());
            var management=new DccElectronicSignatureManagementServiceImpl();
            wire(management,"signatureMapper",signatures,"controlledFileMapper",files,"signatureEvidenceProperties",properties,
                    "fileService",storage,"signatureImageService",images,"signatureBindingService",bindings);
            assertEquals("VALID",management.verifySignatureEvidence(77L).getVerificationStatus());
            file.setFileNumber("changed-current-metadata"); file.setProcessInstanceId(null);
            assertEquals("VALID",management.verifySignatureEvidence(77L).getVerificationStatus(),"historical round is immutable");
            signedProjection.get().setProcessInstanceId("forged-other-round");
            assertEquals("INVALID",management.verifySignatureEvidence(77L).getVerificationStatus(),"tampered round must fail HMAC");
        } finally { TenantContextHolder.clear(); }
    }

    private static void wire(Object target, Object... fields) {
        for (int i = 0; i < fields.length; i += 2) ReflectionTestUtils.setField(target, (String) fields[i], fields[i + 1]);
    }
}
