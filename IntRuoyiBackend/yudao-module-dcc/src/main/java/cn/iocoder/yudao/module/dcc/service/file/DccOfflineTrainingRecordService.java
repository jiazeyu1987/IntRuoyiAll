package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Current receive-task work, independent of the legacy reading/acknowledgement training chain. */
@Service
public class DccOfflineTrainingRecordService {
    public static final String SOURCE_TYPE = "DCC_OFFLINE_TRAINING_RECORD";
    public static final String STATUS = "PENDING_APPLICANT_TRAINING_RECORD";
    public static final String TEMPLATE_CODE = "dcc_task_assigned";
    private static final Set<String> PROCESS_KEYS = Set.of(
            DccControlledFileProcessDefinitionKeys.UPLOAD, DccControlledFileProcessDefinitionKeys.REVISION);

    @Resource private DccControlledFileMapper files;
    @Resource private DccControlledFileMasterMapper masters;
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccProjectCodeAssignmentMapper projectAssignments;
    @Resource private DccControlledFileAssignmentScopeService fileScope;
    @Resource private DccControlledFileCategoryPermissionSupport categories;
    @Resource private DccControlledFileVersionPolicy versions;
    @Resource private AdminUserApi users;
    @Resource private RoleApi roles;
    @Resource private PermissionApi permissions;
    @Resource private BpmProcessInstanceService processes;
    @Resource private BpmProcessDefinitionService definitions;
    @Resource private RuntimeService runtime;
    @Resource private NotifyMessageSendApi messages;

    public static boolean isNativeWaitingFile(DccControlledFileDO file) {
        return file != null && STATUS.equals(file.getStatus()) && file.getProcessDefinitionKey() != null
                && !file.getProcessDefinitionKey().isBlank() && PROCESS_KEYS.contains(file.getProcessDefinitionKey());
    }

    /** Grants only training operation metadata; file binary authorization remains a separate policy. */
    public boolean canUpload(Long actor, DccControlledFileDO file) {
        if (!isNativeWaitingFile(file) || !isEligibleActor(actor, file)) return false;
        requireWaiting(file);
        return true;
    }

    public Waiting requireUpload(Long actor, DccControlledFileDO file) {
        if (!isNativeWaitingFile(file) || !isEligibleActor(actor, file)) {
            throw new IllegalStateException("DCC_OFFLINE_TRAINING_ACTOR_FORBIDDEN");
        }
        return requireWaiting(file);
    }

    public List<DccControlledFileDO> listForActor(Long actor) {
        // Lack of the role is a legitimate empty native source, not a fallback for broken file facts.
        if (!hasEnabledDocumentControlAccount(actor)) return List.of();
        return files.selectOfflineTrainingCandidates(TenantContextHolder.getRequiredTenantId()).stream()
                .filter(file -> canUpload(actor, file)).toList();
    }

    public Waiting requireWaiting(DccControlledFileDO file) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (!isNativeWaitingFile(file) || file.getId() == null || file.getId() <= 0
                || !Objects.equals(file.getTenantId(), tenant) || Boolean.TRUE.equals(file.getDeleted())
                || file.getApprovedTime() == null || !Boolean.TRUE.equals(file.getNeedTraining())
                || file.getTrainingRecordFileId() != null || file.getPublishedFileId() != null
                || file.getMasterId() == null || file.getDccProjectCodeId() == null || file.getCategoryId() == null
                || file.getProcessInstanceId() == null || file.getProcessInstanceId().isBlank()) {
            throw new IllegalStateException("DCC_OFFLINE_TRAINING_FILE_INVALID");
        }
        var version = versions.parseStored(file);
        var master = masters.selectById(file.getMasterId());
        var project = projects.selectById(file.getDccProjectCodeId());
        if (version == null || version.isWorkingIteration() || master == null || project == null
                || !Objects.equals(master.getId(), file.getMasterId()) || !Objects.equals(master.getTenantId(), tenant)
                || !Objects.equals(master.getDccProjectCodeId(), file.getDccProjectCodeId())
                || !Objects.equals(project.getId(), file.getDccProjectCodeId()) || !Objects.equals(project.getTenantId(), tenant)
                || Boolean.TRUE.equals(master.getDeleted()) || Boolean.TRUE.equals(project.getDeleted())) {
            throw new IllegalStateException("DCC_OFFLINE_TRAINING_IDENTITY_INVALID");
        }
        String round = file.getProcessInstanceId();
        var process = processes.getProcessInstance(round);
        var variables = process == null ? null : process.getProcessVariables();
        if (process == null || process.isSuspended() || !round.equals(process.getId())
                || !tenant.toString().equals(process.getTenantId())
                || !Objects.equals(file.getProcessDefinitionKey(), process.getProcessDefinitionKey())
                || !file.getId().toString().equals(process.getBusinessKey()) || variables == null
                || !file.getId().toString().equals(String.valueOf(variables.get("controlledFileId")))) {
            throw new IllegalStateException("DCC_OFFLINE_TRAINING_PROCESS_INVALID");
        }
        var definition = definitions.getProcessDefinition(process.getProcessDefinitionId());
        var model = definitions.getProcessDefinitionBpmnModel(process.getProcessDefinitionId());
        if (definition == null || !Objects.equals(definition.getId(), process.getProcessDefinitionId())
                || !tenant.toString().equals(definition.getTenantId())
                || !Objects.equals(definition.getKey(), file.getProcessDefinitionKey())
                || model == null || model.getMainProcess() == null
                || !(model.getMainProcess().getFlowElement("TRAINING") instanceof ReceiveTask)) {
            throw new IllegalStateException("DCC_OFFLINE_TRAINING_DEFINITION_INVALID");
        }
        var waiting = runtime.createExecutionQuery().processInstanceId(round).activityId("TRAINING").list();
        if (waiting.size() != 1) throw new IllegalStateException("DCC_OFFLINE_TRAINING_EXECUTION_REQUIRED");
        var execution = waiting.get(0);
        if (execution.getId() == null || execution.getId().isBlank() || !round.equals(execution.getProcessInstanceId())
                || !tenant.toString().equals(execution.getTenantId()) || !"TRAINING".equals(execution.getActivityId())
                || execution.isSuspended()) throw new IllegalStateException("DCC_OFFLINE_TRAINING_EXECUTION_INVALID");
        return new Waiting(file, round, execution.getId());
    }

    private boolean hasEnabledDocumentControlAccount(Long actor) {
        if (actor == null || actor <= 0) return false;
        Long tenant = TenantContextHolder.getRequiredTenantId();
        AdminUserRespDTO account = users.getUser(actor);
        RoleRespDTO role = roles.getRoleByCode("doc_control");
        if (account == null || !Objects.equals(account.getId(), actor) || !Objects.equals(account.getTenantId(), tenant)
                || !Objects.equals(account.getStatus(), 0) || role == null || role.getId() == null
                || !"doc_control".equals(role.getCode()) || !Objects.equals(role.getStatus(), 0)) return false;
        Set<Long> assigned = Objects.requireNonNull(permissions.getUserRoleIdListByUserId(actor), "actual role membership");
        return assigned.contains(role.getId())
                && permissions.hasAnyPermissions(actor, "dcc:controlled-file:approve")
                && permissions.hasAnyPermissions(actor, "dcc:controlled-file:query");
    }

    private boolean isEligibleActor(Long actor, DccControlledFileDO file) {
        if (!hasEnabledDocumentControlAccount(actor) || !Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !categories.hasCategoryPermission(file.getCategoryId(), actor, DccFileCategoryPermissionActionEnum.TRAINING_RECORD)
                || !fileScope.isWithinAssignedFileScope(actor, file.getId())) return false;
        if (permissions.hasAnyPermissions(actor, "dcc:project-code:scope:all")
                || !permissions.hasAnyPermissions(actor, "dcc:project-code-assignment:execute")) return true;
        return Objects.requireNonNull(projectAssignments.selectActiveProjectCodeIdsByAssigneeUserId(
                TenantContextHolder.getRequiredTenantId(), actor, LocalDateTime.now()), "active assigned project scope")
                .contains(file.getDccProjectCodeId());
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void notifyWaiting(DccControlledFileDO file) {
        Waiting current = requireWaiting(file);
        RoleRespDTO role = roles.getRoleByCode("doc_control");
        if (role == null || role.getId() == null || !"doc_control".equals(role.getCode())
                || !Objects.equals(role.getStatus(), 0)) throw new IllegalStateException("DCC_OFFLINE_TRAINING_ROLE_REQUIRED");
        var recipients = Objects.requireNonNull(permissions.getUserRoleIdListByRoleIds(List.of(role.getId())), "doc control members")
                .stream().sorted().filter(actor -> isEligibleActor(actor, file)).toList();
        if (recipients.isEmpty()) throw new IllegalStateException("DCC_OFFLINE_TRAINING_RECIPIENT_REQUIRED");
        AdminUserRespDTO applicant = users.getUser(file.getRequesterId());
        if (applicant == null || !Objects.equals(applicant.getTenantId(), file.getTenantId())
                || !Objects.equals(applicant.getId(), file.getRequesterId()) || applicant.getNickname() == null
                || applicant.getNickname().isBlank()) throw new IllegalStateException("DCC_OFFLINE_TRAINING_APPLICANT_REQUIRED");
        String url = detailUrl(file.getId(), current.processInstanceId());
        var params = new LinkedHashMap<String, Object>();
        params.put("processInstanceName", file.getTitle());
        params.put("taskName", "文控上传线下培训记录");
        params.put("startUserNickname", applicant.getNickname());
        params.put("detailUrl", url);
        params.put("actionUrl", url);
        params.put("notifyTargetType", SOURCE_TYPE);
        params.put("notifyTargetId", file.getId().toString());
        params.put("notifyProcessInstanceId", current.processInstanceId());
        for (Long recipient : recipients) {
            var request = new NotifySendSingleToUserIdempotentReqDTO();
            request.setUserId(recipient);
            request.setTemplateCode(TEMPLATE_CODE);
            request.setTemplateParams(params);
            request.setBusinessKey(SOURCE_TYPE + ":" + file.getTenantId() + ":" + file.getId()
                    + ":" + current.processInstanceId() + ":" + recipient);
            Long message = messages.sendSingleMessageIdempotentlyToAdmin(request);
            if (message == null || message <= 0) throw new IllegalStateException("DCC_OFFLINE_TRAINING_MESSAGE_ID_REQUIRED");
        }
    }

    public static Map<String, String> detailQuery(String round) {
        return Map.of("management", "1", "from", "workbench", "returnTo", "/dcc/controlled-file/workbench",
                "processInstanceId", round);
    }

    public static String detailUrl(Long fileId, String round) {
        return "/dcc/controlled-file/detail/" + fileId + "?management=1&from=workbench&returnTo="
                + URLEncoder.encode("/dcc/controlled-file/workbench", StandardCharsets.UTF_8)
                + "&processInstanceId=" + URLEncoder.encode(round, StandardCharsets.UTF_8);
    }

    public record Waiting(DccControlledFileDO file, String processInstanceId, String executionId) { }
}
