package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionBlockerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderProcessConfigListReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderLossReasonItem;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigDevice;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigParameter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigRow;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_APPROVAL_PROCESS_NOT_STARTED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE;

/**
 * 工艺路线版本候选工作流 Service 实现。
 */
@Service
@Validated
public class MesProRouteVersionWorkflowServiceImpl implements MesProRouteVersionWorkflowService {

    public static final String ROUTE_VERSION_APPROVAL_PROCESS_DEFINITION_KEY = "mes-route-version-approval-v1";

    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private MesProRouteService routeService;
    @Resource
    private MesProRouteControlledContentAdapter platformAdapter;
    @Resource
    private MesTeamLeaderProcessConfigService teamLeaderProcessConfigService;

    @Override
    public List<MesProRouteVersionDO> listByRouteId(Long routeId) {
        return routeVersionMapper.selectListByRouteId(routeId);
    }

    @Override
    public MesProRouteVersionDO getVersion(Long id) {
        MesProRouteVersionDO version = routeVersionMapper.selectById(id);
        if (version == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, id);
        }
        return version;
    }

    private MesProRouteVersionDO getVersionForUpdate(Long id) {
        MesProRouteVersionDO version = routeVersionMapper.selectByIdForUpdate(id);
        if (version == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, id);
        }
        return version;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO createCandidate(MesProRouteVersionCreateReqVO reqVO) {
        MesProRouteVersionDO active = routeVersionMapper.selectActiveByRouteIdForUpdate(reqVO.getRouteId());
        if (active == null) {
            throw exception(PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS, reqVO.getRouteId());
        }
        if (reqVO.getSourceRouteVersionId() != null
                && !Objects.equals(reqVO.getSourceRouteVersionId(), active.getId())) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    reqVO.getRouteId(), reqVO.getSourceRouteVersionId(), active.getId());
        }
        MesProRouteVersionDO openCandidate = routeVersionMapper.selectOpenCandidateByRouteId(reqVO.getRouteId());
        if (openCandidate != null) {
            if (MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(openCandidate.getLifecycleStatus())) {
                if (!Objects.equals(openCandidate.getSourceRouteVersionId(), active.getId())) {
                    throw exception(PRO_ROUTE_VERSION_CONFLICT,
                            reqVO.getRouteId(), openCandidate.getSourceRouteVersionId(), active.getId());
                }
                return openCandidate;
            }
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    reqVO.getRouteId(), openCandidate.getId(), openCandidate.getLifecycleStatus());
        }
        String routeSnapshotJson = routeService.buildCurrentRouteSnapshotJson(reqVO.getRouteId(), active.getId());
        if (Boolean.TRUE.equals(reqVO.getMigrateLegacyProductionConfig())) {
            routeSnapshotJson = migrateLegacyProductionConfigs(
                    active, routeSnapshotJson, reqVO.getMissingOveragePercent());
        }
        if (!MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(routeSnapshotJson)) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, active.getId());
        }
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .routeId(reqVO.getRouteId())
                .versionNo(nextVersionNo(reqVO.getRouteId()))
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(active.getId())
                .changeSummaryJson(buildChangeSummary(reqVO.getChangeReason()))
                .remark("工艺路线候选版本，发布后生效")
                .build();
        MesProRouteVersionSnapshotIdentityWriter.apply(candidate, routeSnapshotJson);
        routeVersionMapper.insert(candidate);
        platformAdapter.recordCandidateCreated(active, candidate, SecurityFrameworkUtils.getLoginUserId(),
                reqVO.getChangeReason());
        return candidate;
    }

    private String migrateLegacyProductionConfigs(MesProRouteVersionDO active, String routeSnapshotJson,
                                                  BigDecimal missingOveragePercent) {
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null || StrUtil.isBlank(routeSnapshotJson)) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, active.getId());
        }
        if (missingOveragePercent != null
                && (missingOveragePercent.compareTo(BigDecimal.ZERO) < 0
                || missingOveragePercent.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, active.getId());
        }
        JSONObject snapshot = JSON.parseObject(routeSnapshotJson);
        JSONObject configSnapshots = snapshot == null ? null : snapshot.getJSONObject("configSnapshots");
        JSONObject flowGraph = configSnapshots == null ? null : configSnapshots.getJSONObject("flowGraph");
        JSONArray nodes = flowGraph == null ? null : flowGraph.getJSONArray("nodes");
        if (nodes == null || nodes.isEmpty()) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, active.getId());
        }
        Map<ProductionProcessIdentity, MesTeamLeaderProcessConfigRow> rowsByIdentity = new LinkedHashMap<>();
        for (MesTeamLeaderProcessConfigRow row : teamLeaderProcessConfigService.listProcessConfigs(
                operatorId, new MesTeamLeaderProcessConfigListReqVO())) {
            if (row == null || !Objects.equals(active.getRouteId(), row.getRouteId())) {
                continue;
            }
            ProductionProcessIdentity identity = new ProductionProcessIdentity(
                    row.getRouteProcessId(), row.getProcessId());
            if (!identity.isComplete() || rowsByIdentity.putIfAbsent(identity, row) != null) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, active.getId());
            }
        }
        Set<ProductionProcessIdentity> expectedIdentities = new LinkedHashSet<>();
        JSONArray migratedConfigs = new JSONArray();
        for (Object rawNode : nodes) {
            if (!(rawNode instanceof JSONObject node)) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, active.getId());
            }
            ProductionProcessIdentity identity = new ProductionProcessIdentity(
                    node.getLong("routeProcessId"), node.getLong("processId"));
            if (!identity.isComplete() || !expectedIdentities.add(identity)) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, active.getId());
            }
            MesTeamLeaderProcessConfigRow row = rowsByIdentity.get(identity);
            if (row == null || (row.getOveragePercent() == null && missingOveragePercent == null)) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, active.getId());
            }
            migratedConfigs.add(toMigratedProductionConfig(active.getId(), row, missingOveragePercent));
        }
        if (!rowsByIdentity.keySet().equals(expectedIdentities)) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, active.getId());
        }
        configSnapshots.put("productionProcessConfigSchemaVersion", 1);
        configSnapshots.put("productionProcessConfigs", migratedConfigs);
        MesProRouteCandidateConfigServiceImpl.validateProductionProcessConfigs(active.getId(), configSnapshots);
        return snapshot.toJSONString();
    }

    private JSONObject toMigratedProductionConfig(Long routeVersionId, MesTeamLeaderProcessConfigRow row,
                                                  BigDecimal missingOveragePercent) {
        JSONObject config = new JSONObject(true);
        config.put("routeProcessId", row.getRouteProcessId());
        config.put("processId", row.getProcessId());
        config.put("processCode", row.getProcessCode());
        config.put("processName", row.getProcessName());
        config.put("sort", row.getSort());
        config.put("overagePercent",
                row.getOveragePercent() == null ? missingOveragePercent : row.getOveragePercent());
        config.put("lossReasons", toMigratedLossReasons(routeVersionId, row.getLossReasons()));
        config.put("deviceSelectionGroups", toMigratedDeviceGroups(routeVersionId, row.getDevices()));
        config.put("parameterRules", toMigratedParameterRules(routeVersionId, row));
        return config;
    }

    private JSONArray toMigratedLossReasons(Long routeVersionId, List<MesTeamLeaderLossReasonItem> reasons) {
        JSONArray result = new JSONArray();
        if (reasons == null) {
            return result;
        }
        for (MesTeamLeaderLossReasonItem reason : reasons) {
            if (reason == null || reason.getId() == null || StrUtil.isBlank(reason.getReasonCode())
                    || StrUtil.isBlank(reason.getReasonName()) || reason.getEnabled() == null) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
            JSONObject item = new JSONObject(true);
            item.put("id", reason.getId());
            item.put("reasonCode", reason.getReasonCode());
            item.put("reasonName", reason.getReasonName());
            item.put("enabled", reason.getEnabled());
            result.add(item);
        }
        return result;
    }

    private JSONArray toMigratedDeviceGroups(Long routeVersionId,
                                             List<MesTeamLeaderProcessConfigDevice> devices) {
        JSONArray result = new JSONArray();
        if (devices == null || devices.isEmpty()) {
            return result;
        }
        Map<DeviceGroupIdentity, List<Long>> deviceIdsByGroup = new LinkedHashMap<>();
        for (MesTeamLeaderProcessConfigDevice device : devices) {
            if (device == null || device.getDeviceId() == null || !Boolean.TRUE.equals(device.getMapped())
                    || StrUtil.isBlank(device.getDeviceGroupKey())
                    || !("SINGLE".equals(device.getSelectionMode())
                    || "MULTIPLE".equals(device.getSelectionMode()))) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
            DeviceGroupIdentity identity = new DeviceGroupIdentity(
                    device.getDeviceGroupKey().trim(), device.getSelectionMode());
            List<Long> ids = deviceIdsByGroup.computeIfAbsent(identity, ignored -> new ArrayList<>());
            if (ids.contains(device.getDeviceId())) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
            ids.add(device.getDeviceId());
        }
        int sort = 10;
        for (Map.Entry<DeviceGroupIdentity, List<Long>> entry : deviceIdsByGroup.entrySet()) {
            JSONObject group = new JSONObject(true);
            group.put("deviceGroupKey", entry.getKey().deviceGroupKey());
            group.put("selectionMode", entry.getKey().selectionMode());
            JSONArray deviceIds = new JSONArray();
            deviceIds.addAll(entry.getValue());
            group.put("deviceIds", deviceIds);
            group.put("sort", sort);
            sort += 10;
            result.add(group);
        }
        return result;
    }

    private JSONArray toMigratedParameterRules(Long routeVersionId, MesTeamLeaderProcessConfigRow row) {
        JSONArray result = new JSONArray();
        if (row.getDevices() == null) {
            return result;
        }
        for (MesTeamLeaderProcessConfigDevice device : row.getDevices()) {
            List<MesTeamLeaderProcessConfigParameter> parameters = device.getParameters() == null
                    ? List.of() : device.getParameters();
            for (MesTeamLeaderProcessConfigParameter parameter : parameters.stream()
                    .filter(item -> item != null && !Boolean.FALSE.equals(item.getEnabled()))
                    .sorted(Comparator.comparing(MesTeamLeaderProcessConfigParameter::getParameterCode,
                            Comparator.nullsLast(String::compareTo)))
                    .toList()) {
                if (device.getDeviceId() == null || StrUtil.isBlank(parameter.getParameterCode())
                        || StrUtil.isBlank(parameter.getParameterName())
                        || StrUtil.isBlank(parameter.getValueType())
                        || StrUtil.isBlank(parameter.getStandardText())) {
                    throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
                }
                JSONObject rule = new JSONObject(true);
                rule.put("routeProcessId", row.getRouteProcessId());
                rule.put("processId", row.getProcessId());
                rule.put("deviceId", device.getDeviceId());
                rule.put("parameterCode", parameter.getParameterCode());
                rule.put("parameterName", parameter.getParameterName());
                rule.put("unit", parameter.getUnit());
                rule.put("valueType", parameter.getValueType());
                rule.put("standardText", parameter.getStandardText());
                canonicalizeMigratedParameterRule(rule, parameter);
                result.add(rule);
            }
        }
        return result;
    }

    private void canonicalizeMigratedParameterRule(JSONObject rule,
                                                    MesTeamLeaderProcessConfigParameter parameter) {
        String valueType = StrUtil.trim(parameter.getValueType());
        if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD.equals(valueType)) {
            return;
        }
        if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_SELECT.equals(valueType)) {
            if (parameter.getOptionValues() != null && !parameter.getOptionValues().isEmpty()) {
                rule.put("optionValuesJson", JSON.toJSONString(parameter.getOptionValues()));
            }
            rule.put("defaultText", parameter.getDefaultText());
            return;
        }
        rule.put("defaultValue", parameter.getTargetValue());
        if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_BOOLEAN.equals(valueType)) {
            return;
        }
        rule.put("lowerLimit", parameter.getLowerLimit());
        rule.put("upperLimit", parameter.getUpperLimit());
        if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL.equals(valueType)) {
            rule.put("decimalScale", parameter.getDecimalScale());
        }
    }

    private record ProductionProcessIdentity(Long routeProcessId, Long processId) {
        private boolean isComplete() {
            return routeProcessId != null && processId != null;
        }
    }

    private record DeviceGroupIdentity(String deviceGroupKey, String selectionMode) {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO submitCandidate(Long id) {
        MesProRouteVersionDO candidate = getVersionForUpdate(id);
        if (Boolean.TRUE.equals(candidate.getActive())
                || !MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        Long openCandidateCount = routeVersionMapper.countOpenCandidatesByRouteId(candidate.getRouteId());
        if (openCandidateCount != null && openCandidateCount > 1) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    candidate.getRouteId(), candidate.getId(), openCandidateCount);
        }
        MesProRouteVersionBlockerRespVO blockers = getPublishBlockers(candidate);
        if (!Boolean.TRUE.equals(blockers.getPublishable())) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidate.getId());
        }
        Long submitterUserId = requireLoginUserId();
        LocalDateTime submittedTime = LocalDateTime.now();
        platformAdapter.recordSubmitted(candidate, submitterUserId, null);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        candidate.setSubmittedBy(submitterUserId);
        candidate.setSubmittedTime(submittedTime);
        candidate.setApprovalProcessInstanceId(null);

        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        update.setSubmittedBy(submitterUserId);
        update.setSubmittedTime(submittedTime);
        update.setApprovalProcessInstanceId(null);
        routeVersionMapper.updateById(update);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        candidate.setApprovalProcessInstanceId(null);
        platformAdapter.recordApproved(candidate, submitterUserId,
                "ROUTE_VERSION_READY_TO_PUBLISH:" + candidate.getId() + ":" + submittedTime);
        return candidate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO withdrawCandidate(Long id) {
        MesProRouteVersionDO candidate = getVersionForUpdate(id);
        if (Boolean.TRUE.equals(candidate.getActive())
                || !MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL.equals(candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        Long operatorUserId = requireLoginUserId();
        if (StrUtil.isBlank(candidate.getApprovalProcessInstanceId())) {
            throw exception(PRO_ROUTE_VERSION_APPROVAL_PROCESS_NOT_STARTED,
                    ROUTE_VERSION_APPROVAL_PROCESS_DEFINITION_KEY);
        }
        bpmProcessInstanceApi.cancelProcessInstance(operatorUserId, candidate.getApprovalProcessInstanceId(),
                "route version approval withdraw: routeVersionId=" + candidate.getId());
        if (routeVersionMapper.updateApprovalFieldsToDraft(candidate.getId()) != 1) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    candidate.getRouteId(), candidate.getLifecycleStatus(), "changed concurrently");
        }
        platformAdapter.recordWithdrawn(candidate, operatorUserId);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        candidate.setSubmittedBy(null);
        candidate.setSubmittedTime(null);
        candidate.setApprovalProcessInstanceId(null);
        return candidate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO reopenRejectedCandidate(Long id) {
        MesProRouteVersionDO candidate = getVersionForUpdate(id);
        if (Boolean.TRUE.equals(candidate.getActive())
                || !MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED.equals(candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        Long openCandidateCount = routeVersionMapper.countOpenCandidatesByRouteId(candidate.getRouteId());
        if (openCandidateCount != null && openCandidateCount > 0) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    candidate.getRouteId(), candidate.getId(), openCandidateCount);
        }
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        routeVersionMapper.updateById(update);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        return candidate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO cancelCandidate(Long id) {
        MesProRouteVersionDO candidate = getVersionForUpdate(id);
        if (Boolean.TRUE.equals(candidate.getActive())
                || !(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())
                || MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH.equals(candidate.getLifecycleStatus())
                || MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED.equals(candidate.getLifecycleStatus()))) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        Long operatorUserId = requireLoginUserId();
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED);
        routeVersionMapper.updateById(update);
        platformAdapter.recordCancelled(candidate, operatorUserId);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED);
        return candidate;
    }

    @Override
    public MesProRouteVersionBlockerRespVO getPublishBlockers(Long id) {
        return getPublishBlockers(getVersion(id));
    }

    private MesProRouteVersionBlockerRespVO getPublishBlockers(MesProRouteVersionDO candidate) {
        List<String> blockers = new ArrayList<>();
        if (Boolean.TRUE.equals(candidate.getActive())) {
            blockers.add("candidate is already active");
        }
        if (!(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())
                || MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH.equals(candidate.getLifecycleStatus()))) {
            blockers.add("candidate status is not publishable: " + candidate.getLifecycleStatus());
        }
        MesProRouteVersionDO active = routeVersionMapper.selectActiveByRouteId(candidate.getRouteId());
        if (active == null) {
            blockers.add("active route version does not exist");
        } else if (!Objects.equals(candidate.getSourceRouteVersionId(), active.getId())) {
            blockers.add("source active version drifted");
        }
        if (!MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(candidate.getRouteSnapshotJson())) {
            blockers.add("route version snapshot is incomplete");
        }
        MesProRouteVersionBlockerRespVO respVO = new MesProRouteVersionBlockerRespVO();
        respVO.setRouteVersionId(candidate.getId());
        respVO.setBlockers(blockers);
        respVO.setPublishable(blockers.isEmpty());
        return respVO;
    }

    private String nextVersionNo(Long routeId) {
        String maxVersionNo = routeVersionMapper.selectMaxVersionNoByRouteId(routeId);
        if (StrUtil.isBlank(maxVersionNo)) {
            return "V1";
        }
        String normalized = maxVersionNo.trim().toUpperCase();
        if (!normalized.matches("V\\d+")) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE, routeId, maxVersionNo);
        }
        return "V" + (Integer.parseInt(normalized.substring(1)) + 1);
    }

    private String buildChangeSummary(String changeReason) {
        JSONObject summary = new JSONObject(true);
        summary.put("changeReason", StrUtil.blankToDefault(changeReason, "工艺路线候选版本"));
        return summary.toJSONString();
    }

    private Long requireLoginUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw new IllegalStateException("route version submitter is required");
        }
        return loginUserId;
    }

}
