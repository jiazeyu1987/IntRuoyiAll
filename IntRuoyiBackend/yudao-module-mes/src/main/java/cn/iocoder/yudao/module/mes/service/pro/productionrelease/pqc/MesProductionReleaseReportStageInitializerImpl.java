package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class MesProductionReleaseReportStageInitializerImpl
        implements MesProductionReleaseReportStageInitializer {

    private static final String TASK_TYPE_FILL = "FILL";
    private static final String BUSINESS_SCOPE_RELEASE_REPORT_NODE = "RELEASE_REPORT_NODE";
    private static final String OWNER_CONFIG_KEY = "batchRecordAttachmentOwners";
    private static final Set<String> REQUIRED_NODE_TYPES = Set.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");

    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final PermissionApi permissionApi;
    private final AdminUserApi adminUserApi;

    public MesProductionReleaseReportStageInitializerImpl(
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            PermissionApi permissionApi,
            AdminUserApi adminUserApi) {
        this.batchExecutionMapper = batchExecutionMapper;
        this.batchTaskMapper = batchTaskMapper;
        this.workTaskMapper = workTaskMapper;
        this.permissionApi = permissionApi;
        this.adminUserApi = adminUserApi;
    }

    @Override
    public MesProductionReleaseReportStageInitializationResult initializeRequiredReportStage(
            MesProductionReleaseReportStageInitializationCommand command) {
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(command.getBatchExecutionId());
        if (batch == null || !Objects.equals(command.getRouteId(), batch.getRouteId())
                || !Objects.equals(command.getRouteVersionId(), batch.getRouteVersionId())) {
            throw blocker(command, "release batch does not match the frozen route version");
        }
        Map<String, OwnerConfig> owners = parseOwnerConfigs(command, batch.getRouteSnapshotJson());
        List<MesProEdhrBatchExecutionTaskDO> reportTasks = batchTaskMapper
                .selectListByBatchExecutionId(batch.getId()).stream()
                .filter(task -> REQUIRED_NODE_TYPES.contains(task.getNodeType()))
                .sorted(Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getRouteProcessSort)
                        .thenComparing(MesProEdhrBatchExecutionTaskDO::getId))
                .toList();
        if (reportTasks.size() != 4 || reportTasks.stream().map(MesProEdhrBatchExecutionTaskDO::getNodeType)
                .collect(java.util.stream.Collectors.toSet()).size() != 4) {
            throw blocker(command, "release batch must contain exactly four frozen report nodes");
        }

        List<MesProductionReleaseReportUploadTaskReceipt> receipts = new ArrayList<>();
        for (MesProEdhrBatchExecutionTaskDO batchTask : reportTasks) {
            List<Long> candidates = resolveEnabledUsers(command, owners.get(batchTask.getNodeType()));
            MesProEdhrWorkTaskDO workTask = workTaskMapper.selectActiveByBusinessScopeAndType(
                    BUSINESS_SCOPE_RELEASE_REPORT_NODE, batchTask.getId(), TASK_TYPE_FILL);
            String candidateSnapshot = candidates.stream().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
            if (workTask == null) {
                workTask = new MesProEdhrWorkTaskDO()
                        .setTaskType(TASK_TYPE_FILL)
                        .setTaskCode("PRR-" + command.getApplicationId() + "-" + batchTask.getId())
                        .setBatchExecutionId(batch.getId())
                        .setBatchTaskId(batchTask.getId())
                        .setBusinessScopeType(BUSINESS_SCOPE_RELEASE_REPORT_NODE)
                        .setBusinessScopeId(batchTask.getId())
                        .setWorkOrderId(batch.getWorkOrderId())
                        .setWorkOrderCode(batch.getWorkOrderCode())
                        .setBatchCode(batch.getBatchCode())
                        .setRouteId(batch.getRouteId())
                        .setProcessName(batchTask.getProcessName())
                        .setAssigneeUserId(candidates.get(0))
                        .setCandidateSourceType("FROZEN_REPORT_OWNER")
                        .setCandidateUserSnapshot(candidateSnapshot)
                        .setStatus(MesProEdhrWorkTaskStatus.TODO)
                        .setActionUrl("/mes/production-release/report?applicationId=" + command.getApplicationId()
                                + "&nodeType=" + batchTask.getNodeType())
                        .setRemark("production release required report upload");
                if (workTaskMapper.insert(workTask) != 1 || workTask.getId() == null) {
                    throw blocker(command, "report upload work task persistence failed");
                }
            } else if (!Objects.equals(batch.getId(), workTask.getBatchExecutionId())
                    || !Objects.equals(batchTask.getId(), workTask.getBatchTaskId())
                    || !Objects.equals(candidateSnapshot, workTask.getCandidateUserSnapshot())) {
                throw blocker(command, "existing report upload work task does not match its frozen owner snapshot");
            }
            receipts.add(new MesProductionReleaseReportUploadTaskReceipt()
                    .setNodeType(batchTask.getNodeType())
                    .setBatchTaskId(batchTask.getId())
                    .setWorkTaskId(workTask.getId())
                    .setCandidateUserIds(candidates)
                    .setStatus(workTask.getStatus()));
        }
        String reportSnapshotHash = MesReleaseFlowIdempotency.payloadHash(
                String.valueOf(command.getApplicationId()), String.valueOf(batch.getId()),
                command.getSourceSnapshotHash(), receipts.stream()
                        .map(item -> item.getNodeType() + ":" + item.getBatchTaskId() + ":"
                                + item.getWorkTaskId() + ":" + item.getCandidateUserIds())
                        .collect(java.util.stream.Collectors.joining("|")));
        return new MesProductionReleaseReportStageInitializationResult()
                .setReportUploadTasks(List.copyOf(receipts))
                .setReportSnapshotHash(reportSnapshotHash);
    }

    private Map<String, OwnerConfig> parseOwnerConfigs(
            MesProductionReleaseReportStageInitializationCommand command,
            String routeSnapshotJson) {
        try {
            JSONObject snapshot = JSON.parseObject(routeSnapshotJson);
            JSONObject configSnapshots = snapshot == null ? null : snapshot.getJSONObject("configSnapshots");
            JSONArray rawOwners = configSnapshots == null ? null : configSnapshots.getJSONArray(OWNER_CONFIG_KEY);
            if (rawOwners == null || rawOwners.isEmpty()) {
                throw blocker(command, "frozen route report owner configuration is missing");
            }
            Map<String, OwnerConfig> result = new LinkedHashMap<>();
            for (Object value : rawOwners) {
                JSONObject item = value instanceof JSONObject object ? object : JSON.parseObject(JSON.toJSONString(value));
                String nodeType = StrUtil.trim(item.getString("attachmentCode"));
                String sourceType = StrUtil.trim(item.getString("candidateSourceType"));
                JSONArray sourceIdsValue = item.getJSONArray("candidateSourceIds");
                List<Long> sourceIds = new ArrayList<>();
                if (sourceIdsValue != null) {
                    for (int index = 0; index < sourceIdsValue.size(); index++) {
                        Long id = sourceIdsValue.getLong(index);
                        if (id != null && id > 0 && !sourceIds.contains(id)) {
                            sourceIds.add(id);
                        }
                    }
                }
                if (!REQUIRED_NODE_TYPES.contains(nodeType) || StrUtil.isBlank(sourceType) || sourceIds.isEmpty()
                        || result.putIfAbsent(nodeType, new OwnerConfig(sourceType, List.copyOf(sourceIds))) != null) {
                    throw blocker(command, "frozen report owner configuration is invalid or duplicated");
                }
            }
            if (!result.keySet().equals(REQUIRED_NODE_TYPES)) {
                throw blocker(command, "all four frozen report owner configurations are required");
            }
            return result;
        } catch (MesReleaseFlowBlockerException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw blocker(command, "frozen route report owner configuration cannot be parsed");
        }
    }

    private List<Long> resolveEnabledUsers(
            MesProductionReleaseReportStageInitializationCommand command,
            OwnerConfig owner) {
        if (owner == null) {
            throw blocker(command, "frozen report owner is missing");
        }
        List<Long> users;
        if ("USER".equals(owner.sourceType()) || "USERS".equals(owner.sourceType())) {
            users = owner.sourceIds();
        } else if ("ROLE".equals(owner.sourceType()) || "ROLE_GROUP".equals(owner.sourceType())) {
            Set<Long> resolved = permissionApi.getUserRoleIdListByRoleIds(new LinkedHashSet<>(owner.sourceIds()));
            users = resolved == null ? List.of() : resolved.stream().filter(Objects::nonNull).sorted().toList();
        } else {
            throw blocker(command, "report owner source type must be USER, USERS, ROLE or ROLE_GROUP");
        }
        if (users.isEmpty()) {
            throw blocker(command, "report owner candidate set is empty");
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(new LinkedHashSet<>(users));
        if (userMap == null || users.stream().anyMatch(userId -> {
            AdminUserRespDTO user = userMap.get(userId);
            return user == null || !CommonStatusEnum.isEnable(user.getStatus());
        })) {
            throw blocker(command, "report owner candidate contains a missing or disabled user");
        }
        return users.stream().distinct().sorted().toList();
    }

    private MesReleaseFlowBlockerException blocker(
            MesProductionReleaseReportStageInitializationCommand command, String reason) {
        return new MesReleaseFlowBlockerException(reason, new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_2)
                .setCurrentStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(MesReleaseFlowBlockerType.REPORT_OWNER_REQUIRED)
                        .setObjectType("REPORT_UPLOAD_STAGE")
                        .setObjectId(command == null ? null : String.valueOf(command.getApplicationId()))
                        .setReason(reason)
                        .setSuggestion("configure all four frozen report owners before PQC approval"))));
    }

    private record OwnerConfig(String sourceType, List<Long> sourceIds) {
    }
}
