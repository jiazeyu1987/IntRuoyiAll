package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_DEFECT_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED;

@Service
@Validated
public class MesTeamLeaderLossReasonServiceImpl implements MesTeamLeaderLossReasonService {

    private final MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService;
    private final MesProcessPoolDefectReasonMapper defectReasonMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProRouteMapper routeMapper;
    private final MesProProcessMapper processMapper;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;

    public MesTeamLeaderLossReasonServiceImpl(
            MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService,
            MesProcessPoolDefectReasonMapper defectReasonMapper,
            MesProRouteProcessMapper routeProcessMapper,
            MesProRouteMapper routeMapper,
            MesProProcessMapper processMapper,
            MesProcessPoolTeamMaintenanceAuditMapper auditMapper) {
        this.routeStartAuthorizationService = routeStartAuthorizationService;
        this.defectReasonMapper = defectReasonMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.routeMapper = routeMapper;
        this.processMapper = processMapper;
        this.auditMapper = auditMapper;
    }

    @Override
    public List<MesTeamLeaderLossReasonRow> listLossReasonRows(Long leaderUserId) {
        List<MesProRouteProcessDO> routeProcesses = routeStartAuthorizationService
                .listAuthorizedRouteProcesses(leaderUserId);
        if (routeProcesses.isEmpty()) {
            return List.of();
        }
        Set<Long> routeProcessIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<MesProcessPoolDefectReasonDO>> reasonsByRouteProcess = defectReasonMapper.selectList(
                        new LambdaQueryWrapperX<MesProcessPoolDefectReasonDO>()
                                .in(MesProcessPoolDefectReasonDO::getRouteProcessId, routeProcessIds)
                                .eq(MesProcessPoolDefectReasonDO::getReasonType,
                                        MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS))
                .stream()
                .sorted(Comparator
                        .comparing(MesProcessPoolDefectReasonDO::getReasonCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesProcessPoolDefectReasonDO::getId))
                .collect(Collectors.groupingBy(MesProcessPoolDefectReasonDO::getRouteProcessId,
                        LinkedHashMap::new, Collectors.toList()));
        Map<Long, MesProRouteDO> routeMap = loadRouteMap(routeProcesses);
        Map<Long, MesProProcessDO> processMap = loadProcessMap(routeProcesses);
        return routeProcesses.stream()
                .map(routeProcess -> toRow(routeProcess, routeMap, processMap, reasonsByRouteProcess))
                .toList();
    }

    @Override
    public Long createLossReason(MesTeamLeaderLossReasonSaveReqBO reqBO) {
        validateSaveReq(reqBO);
        MesProRouteProcessDO routeProcess = requireAuthorizedRouteProcess(reqBO.getLeaderUserId(),
                reqBO.getRouteProcessId());
        MesProcessPoolDefectReasonDO reason = MesProcessPoolDefectReasonDO.builder()
                .leaderUserId(null)
                .routeProcessId(routeProcess.getId())
                .processId(routeProcess.getProcessId())
                .reasonType(MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS)
                .reasonCode(generateLossReasonCode(routeProcess.getId()))
                .reasonName(StrUtil.trim(reqBO.getReasonName()))
                .enabled(Boolean.TRUE)
                .build();
        defectReasonMapper.insert(reason);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "CREATE_LOSS_REASON",
                "DEFECT_REASON", reason.getId(), null, reason.toString());
        return reason.getId();
    }

    @Override
    public void updateLossReason(MesTeamLeaderLossReasonUpdateReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getId() == null
                || StrUtil.isBlank(reqBO.getReasonName())) {
            throw exception(PRO_PROCESS_POOL_DEFECT_REASON_REQUIRED, "lossReasonUpdate");
        }
        MesProcessPoolDefectReasonDO existing = requireLossReason(reqBO.getId());
        routeStartAuthorizationService.assertCanMaintainRouteProcess(reqBO.getLeaderUserId(),
                existing.getRouteProcessId());
        MesProcessPoolDefectReasonDO update = MesProcessPoolDefectReasonDO.builder()
                .id(existing.getId())
                .reasonName(StrUtil.trim(reqBO.getReasonName()))
                .enabled(reqBO.getEnabled() == null ? existing.getEnabled() : reqBO.getEnabled())
                .build();
        update.setRemark(StrUtil.blankToDefault(StrUtil.trim(reqBO.getRemark()), null));
        defectReasonMapper.updateById(update);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "UPDATE_LOSS_REASON",
                "DEFECT_REASON", existing.getId(), existing.toString(), update.toString());
    }

    @Override
    public void deleteLossReason(Long leaderUserId, Long reasonId) {
        if (leaderUserId == null || reasonId == null) {
            throw exception(PRO_PROCESS_POOL_DEFECT_REASON_REQUIRED, "lossReasonDelete");
        }
        MesProcessPoolDefectReasonDO existing = requireLossReason(reasonId);
        routeStartAuthorizationService.assertCanMaintainRouteProcess(leaderUserId, existing.getRouteProcessId());
        MesProcessPoolDefectReasonDO update = MesProcessPoolDefectReasonDO.builder()
                .id(existing.getId())
                .enabled(Boolean.FALSE)
                .build();
        update.setRemark(existing.getRemark());
        update.setUpdateTime(LocalDateTime.now());
        defectReasonMapper.updateById(update);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, leaderUserId, "DELETE_LOSS_REASON",
                "DEFECT_REASON", existing.getId(), existing.toString(), update.toString());
    }

    private MesProRouteProcessDO requireAuthorizedRouteProcess(Long leaderUserId, Long routeProcessId) {
        routeStartAuthorizationService.assertCanMaintainRouteProcess(leaderUserId, routeProcessId);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectById(routeProcessId);
        if (routeProcess == null || routeProcess.getProcessId() == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "routeProcessId=" + routeProcessId);
        }
        return routeProcess;
    }

    private MesProcessPoolDefectReasonDO requireLossReason(Long reasonId) {
        MesProcessPoolDefectReasonDO reason = defectReasonMapper.selectById(reasonId);
        if (reason == null || !MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS.equals(reason.getReasonType())
                || reason.getRouteProcessId() == null) {
            throw exception(PRO_PROCESS_POOL_DEFECT_REASON_REQUIRED, "lossReasonId=" + reasonId);
        }
        return reason;
    }

    private void validateSaveReq(MesTeamLeaderLossReasonSaveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getRouteProcessId() == null
                || StrUtil.isBlank(reqBO.getReasonName())) {
            throw exception(PRO_PROCESS_POOL_DEFECT_REASON_REQUIRED, "lossReason");
        }
    }

    private String generateLossReasonCode(Long routeProcessId) {
        Long existingCount = defectReasonMapper.selectCount(
                new LambdaQueryWrapperX<MesProcessPoolDefectReasonDO>()
                        .eq(MesProcessPoolDefectReasonDO::getRouteProcessId, routeProcessId)
                        .eq(MesProcessPoolDefectReasonDO::getReasonType,
                                MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS));
        long sequence = (existingCount == null ? 0L : existingCount) + 1L;
        while (true) {
            String candidate = "LOSS-" + routeProcessId + "-" + String.format("%03d", sequence);
            Long duplicateCount = defectReasonMapper.selectCount(
                    new LambdaQueryWrapperX<MesProcessPoolDefectReasonDO>()
                            .eq(MesProcessPoolDefectReasonDO::getRouteProcessId, routeProcessId)
                            .eq(MesProcessPoolDefectReasonDO::getReasonType,
                                    MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS)
                            .eq(MesProcessPoolDefectReasonDO::getReasonCode, candidate));
            if (duplicateCount == null || duplicateCount == 0L) {
                return candidate;
            }
            sequence++;
        }
    }

    private Map<Long, MesProRouteDO> loadRouteMap(List<MesProRouteProcessDO> routeProcesses) {
        Set<Long> routeIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getRouteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (routeIds.isEmpty()) {
            return Map.of();
        }
        return routeMapper.selectListByIdsIgnoreDeleted(routeIds).stream()
                .collect(Collectors.toMap(MesProRouteDO::getId, Function.identity(),
                        (left, ignored) -> left, LinkedHashMap::new));
    }

    private Map<Long, MesProProcessDO> loadProcessMap(List<MesProRouteProcessDO> routeProcesses) {
        Set<Long> processIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (processIds.isEmpty()) {
            return Map.of();
        }
        return processMapper.selectListByIdsIgnoreDeleted(processIds).stream()
                .collect(Collectors.toMap(MesProProcessDO::getId, Function.identity(),
                        (left, ignored) -> left, LinkedHashMap::new));
    }

    private MesTeamLeaderLossReasonRow toRow(MesProRouteProcessDO routeProcess,
                                             Map<Long, MesProRouteDO> routeMap,
                                             Map<Long, MesProProcessDO> processMap,
                                             Map<Long, List<MesProcessPoolDefectReasonDO>> reasonsByRouteProcess) {
        MesProRouteDO route = routeMap.get(routeProcess.getRouteId());
        MesProProcessDO process = processMap.get(routeProcess.getProcessId());
        List<MesTeamLeaderLossReasonItem> reasons = new ArrayList<>();
        for (MesProcessPoolDefectReasonDO reason : reasonsByRouteProcess.getOrDefault(routeProcess.getId(), List.of())) {
            reasons.add(new MesTeamLeaderLossReasonItem()
                    .setId(reason.getId())
                    .setReasonCode(reason.getReasonCode())
                    .setReasonName(reason.getReasonName())
                    .setEnabled(reason.getEnabled()));
        }
        return new MesTeamLeaderLossReasonRow()
                .setRouteId(routeProcess.getRouteId())
                .setRouteCode(route == null ? null : route.getCode())
                .setRouteName(route == null ? null : route.getName())
                .setRouteProcessId(routeProcess.getId())
                .setProcessId(routeProcess.getProcessId())
                .setProcessCode(process == null ? null : process.getCode())
                .setProcessName(process == null ? null : process.getName())
                .setSort(routeProcess.getSort())
                .setReasons(reasons);
    }

}
