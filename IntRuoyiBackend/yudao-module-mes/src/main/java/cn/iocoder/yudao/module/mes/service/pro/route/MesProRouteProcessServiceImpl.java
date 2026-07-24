package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 工艺路线工序 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class MesProRouteProcessServiceImpl implements MesProRouteProcessService {

    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProProcessMapper processMapper;

    @Resource
    @Lazy
    private MesProRouteProductService routeProductService;
    @Resource
    @Lazy
    private MesProProcessService processService;
    @Resource
    @Lazy
    private MesProRouteService routeService;
    @Resource
    @Lazy
    private MesProRouteProcessFlowService routeProcessFlowService;
    @Resource
    @Lazy
    private MesMdWorkstationService workstationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRouteProcess(MesProRouteProcessSaveReqVO createReqVO) {
        // 1.0 已启用的工艺路线，不允许操作
        routeService.validateRouteNotEnable(createReqVO.getRouteId());
        // 1.1 校验工艺路线、工序存在
        validateRouteAndProcessExists(createReqVO.getRouteId(), createReqVO.getProcessId());
        // 1.2 校验绑定工作站属于当前工序
        validateBoundWorkstation(createReqVO.getProcessId(), createReqVO.getWorkstationId());
        // 1.3 校验唯一性
        validateSortUnique(null, createReqVO.getRouteId(), createReqVO.getSort());
        validateProcessUnique(null, createReqVO.getRouteId(), createReqVO.getProcessId());
        validateKeyProcessUnique(null, createReqVO.getRouteId(), createReqVO.getKeyFlag());

        // 2. 插入
        MesProRouteProcessDO routeProcess = BeanUtils.toBean(createReqVO, MesProRouteProcessDO.class);
        routeProcessMapper.insert(routeProcess);

        routeService.maintainRouteVersionAfterProcessChange(createReqVO.getRouteId());
        routeService.ensureDefaultScheduleArtifacts(createReqVO.getRouteId(), routeProcess.getId());
        return routeProcess.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRouteProcess(MesProRouteProcessSaveReqVO updateReqVO) {
        // 1.0 已启用的工艺路线，不允许操作
        routeService.validateRouteNotEnable(updateReqVO.getRouteId());
        // 1.1 校验存在
        validateRouteProcessExists(updateReqVO.getId());
        // 1.2 校验工艺路线、工序存在
        validateRouteAndProcessExists(updateReqVO.getRouteId(), updateReqVO.getProcessId());
        // 1.3 校验绑定工作站属于当前工序
        validateBoundWorkstation(updateReqVO.getProcessId(), updateReqVO.getWorkstationId());
        // 1.4 校验唯一性
        validateSortUnique(updateReqVO.getId(), updateReqVO.getRouteId(), updateReqVO.getSort());
        validateProcessUnique(updateReqVO.getId(), updateReqVO.getRouteId(), updateReqVO.getProcessId());
        validateKeyProcessUnique(updateReqVO.getId(), updateReqVO.getRouteId(), updateReqVO.getKeyFlag());

        // 2. 更新
        MesProRouteProcessDO updateObj = BeanUtils.toBean(updateReqVO, MesProRouteProcessDO.class);
        routeProcessMapper.updateById(updateObj);

        routeService.maintainRouteVersionAfterProcessChange(updateReqVO.getRouteId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRouteProcess(Long id) {
        // 1.1 校验存在
        MesProRouteProcessDO routeProcess = validateRouteProcessExists(id);
        // 1.2 已启用的工艺路线，不允许操作
        routeService.validateRouteNotEnable(routeProcess.getRouteId());

        // 2. 删除
        routeProcessMapper.deleteById(id);
        routeProcessFlowService.deleteByRouteProcessId(routeProcess.getRouteId(), id);

        routeService.maintainRouteVersionAfterProcessChange(routeProcess.getRouteId());
    }

    private MesProRouteProcessDO validateRouteProcessExists(Long id) {
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectById(id);
        if (routeProcess == null) {
            throw exception(PRO_ROUTE_PROCESS_NOT_EXISTS);
        }
        return routeProcess;
    }

    private void validateRouteAndProcessExists(Long routeId, Long processId) {
        if (routeService.getRoute(routeId) == null) {
            throw exception(PRO_ROUTE_NOT_EXISTS);
        }
        if (processService.getProcess(processId) == null) {
            throw exception(PRO_ROUTE_PROCESS_NOT_EXISTS);
        }
    }

    private void validateBoundWorkstation(Long processId, Long workstationId) {
        if (workstationId == null) {
            return;
        }
        MesMdWorkstationDO workstation = workstationService.validateWorkstationExistsAndEnable(workstationId);
        if (!Objects.equals(workstation.getProcessId(), processId)) {
            throw exception(PRO_WORKSTATION_PROCESS_MISMATCH);
        }
    }

    private void validateSortUnique(Long id, Long routeId, Integer sort) {
        MesProRouteProcessDO existing = routeProcessMapper.selectByRouteIdAndSort(routeId, sort);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(PRO_ROUTE_PROCESS_SORT_DUPLICATE);
        }
    }

    private void validateProcessUnique(Long id, Long routeId, Long processId) {
        MesProRouteProcessDO existing = routeProcessMapper.selectByRouteIdAndProcessId(routeId, processId);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(PRO_ROUTE_PROCESS_DUPLICATE);
        }
    }

    private void validateKeyProcessUnique(Long id, Long routeId, Boolean keyFlag) {
        if (!Boolean.TRUE.equals(keyFlag)) {
            return;
        }
        MesProRouteProcessDO existing = routeProcessMapper.selectKeyProcessByRouteId(routeId);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(PRO_ROUTE_PROCESS_KEY_DUPLICATE);
        }
    }

    @Override
    public MesProRouteProcessDO getRouteProcess(Long id) {
        return routeProcessMapper.selectById(id);
    }

    @Override
    public List<MesProRouteProcessDO> getRouteProcessListByRouteId(Long routeId) {
        return routeProcessMapper.selectListByRouteId(routeId);
    }

    @Override
    public List<MesProRouteProcessDO> getRouteProcessListByRouteIds(Collection<Long> routeIds) {
        if (CollUtil.isEmpty(routeIds)) {
            return Collections.emptyList();
        }
        return routeProcessMapper.selectListByRouteIds(routeIds);
    }

    @Override
    public List<MesProRouteProcessDO> getRouteProcessListByRouteIdsIgnoreDeleted(Collection<Long> routeIds) {
        if (CollUtil.isEmpty(routeIds)) {
            return Collections.emptyList();
        }
        return routeProcessMapper.selectListByRouteIdsIgnoreDeleted(routeIds);
    }

    @Override
    public MesProRouteProcessDO getRouteProcessByRouteIdAndProcessId(Long routeId, Long processId) {
        return routeProcessMapper.selectByRouteIdAndProcessId(routeId, processId);
    }

    @Override
    public MesProRouteProcessDO resolveCurrentRouteProcess(Long routeProcessId, Long routeId, Long sourceProcessId) {
        Long normalizedSourceProcessId = normalizeSourceProcessId(sourceProcessId);
        Long identityProcessId = normalizedSourceProcessId;
        Long effectiveRouteId = routeId;
        if (routeProcessId != null) {
            MesProRouteProcessDO currentSnapshotRelation = routeProcessMapper.selectById(routeProcessId);
            if (currentSnapshotRelation != null) {
                if (effectiveRouteId != null && !Objects.equals(effectiveRouteId, currentSnapshotRelation.getRouteId())) {
                    throw routeProcessIdentityNotFound(routeProcessId, routeId, normalizedSourceProcessId, null);
                }
                return currentSnapshotRelation;
            }
            MesProRouteProcessDO historicalSnapshotRelation =
                    routeProcessMapper.selectByIdIgnoreDeleted(routeProcessId);
            if (historicalSnapshotRelation == null
                    || (effectiveRouteId != null && !Objects.equals(effectiveRouteId, historicalSnapshotRelation.getRouteId()))) {
                throw routeProcessIdentityNotFound(routeProcessId, routeId, normalizedSourceProcessId, null);
            }
            effectiveRouteId = historicalSnapshotRelation.getRouteId();
            identityProcessId = historicalSnapshotRelation.getProcessId();
        }

        if (effectiveRouteId == null || identityProcessId == null) {
            throw routeProcessIdentityNotFound(routeProcessId, effectiveRouteId, identityProcessId, null);
        }
        MesProRouteProcessDO directRelation =
                routeProcessMapper.selectByRouteIdAndProcessId(effectiveRouteId, identityProcessId);
        if (directRelation != null) {
            return directRelation;
        }

        MesProProcessDO sourceProcess = processMapper.selectByIdIgnoreDeleted(identityProcessId);
        String processCode = sourceProcess == null ? null : StrUtil.trim(sourceProcess.getCode());
        if (StrUtil.isBlank(processCode)) {
            throw routeProcessIdentityNotFound(routeProcessId, effectiveRouteId, identityProcessId, processCode);
        }
        List<Long> currentProcessIds = processMapper.selectListByCode(processCode).stream()
                .map(MesProProcessDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<MesProRouteProcessDO> candidates =
                routeProcessMapper.selectListByRouteIdAndProcessIds(effectiveRouteId, currentProcessIds);
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        if (candidates.isEmpty()) {
            throw routeProcessIdentityNotFound(routeProcessId, effectiveRouteId, identityProcessId, processCode);
        }
        throw exception(PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS,
                effectiveRouteId, identityProcessId, processCode,
                candidates.stream().map(MesProRouteProcessDO::getId).toList());
    }

    @Override
    public MesProRouteProcessDO resolveFrozenRouteProcess(Long routeProcessId, Long routeId, Long sourceProcessId) {
        Long normalizedSourceProcessId = normalizeSourceProcessId(sourceProcessId);
        if (routeProcessId == null) {
            throw routeProcessIdentityNotFound(null, routeId, normalizedSourceProcessId, null);
        }
        MesProRouteProcessDO frozenRouteProcess = routeProcessMapper.selectById(routeProcessId);
        if (frozenRouteProcess == null) {
            frozenRouteProcess = routeProcessMapper.selectByIdIgnoreDeleted(routeProcessId);
        }
        if (frozenRouteProcess == null
                || (routeId != null && !Objects.equals(routeId, frozenRouteProcess.getRouteId()))) {
            throw routeProcessIdentityNotFound(routeProcessId, routeId, normalizedSourceProcessId, null);
        }
        if (normalizedSourceProcessId == null || Objects.equals(normalizedSourceProcessId, frozenRouteProcess.getProcessId())) {
            return frozenRouteProcess;
        }
        MesProProcessDO sourceProcess = processMapper.selectByIdIgnoreDeleted(normalizedSourceProcessId);
        String processCode = sourceProcess == null ? null : StrUtil.trim(sourceProcess.getCode());
        MesProProcessDO routeProcess = processMapper.selectByIdIgnoreDeleted(frozenRouteProcess.getProcessId());
        if (isSameProcessIdentity(sourceProcess, routeProcess)) {
            return frozenRouteProcess;
        }
        throw routeProcessIdentityNotFound(routeProcessId, routeId, normalizedSourceProcessId, processCode);
    }

    private Long normalizeSourceProcessId(Long sourceProcessId) {
        return sourceProcessId == null || sourceProcessId <= 0 ? null : sourceProcessId;
    }

    private boolean isSameProcessIdentity(MesProProcessDO sourceProcess, MesProProcessDO routeProcess) {
        if (sourceProcess == null || routeProcess == null) {
            return false;
        }
        String sourceCode = StrUtil.trim(sourceProcess.getCode());
        String routeCode = StrUtil.trim(routeProcess.getCode());
        if (StrUtil.isNotBlank(sourceCode) && StrUtil.equalsIgnoreCase(sourceCode, routeCode)) {
            return true;
        }
        String sourceName = StrUtil.trim(sourceProcess.getName());
        String routeName = StrUtil.trim(routeProcess.getName());
        if (StrUtil.isBlank(sourceName) || !StrUtil.equals(sourceName, routeName)) {
            return false;
        }
        String sourceProductName = StrUtil.trim(sourceProcess.getProductName());
        String routeProductName = StrUtil.trim(routeProcess.getProductName());
        return StrUtil.isBlank(sourceProductName)
                || StrUtil.isBlank(routeProductName)
                || StrUtil.equals(sourceProductName, routeProductName);
    }

    private RuntimeException routeProcessIdentityNotFound(Long routeProcessId, Long routeId,
                                                          Long sourceProcessId, String processCode) {
        return exception(PRO_ROUTE_PROCESS_IDENTITY_NOT_FOUND,
                routeId, sourceProcessId, routeProcessId, processCode);
    }

    @Override
    public Map<Long, Long> getProcessIdentityMap(Collection<Long> targetProcessIds) {
        if (CollUtil.isEmpty(targetProcessIds)) {
            return Collections.emptyMap();
        }
        List<Long> distinctTargetIds = targetProcessIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (distinctTargetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MesProProcessDO> targetProcesses =
                processMapper.selectListByIdsIgnoreDeleted(distinctTargetIds);
        Map<String, List<MesProProcessDO>> targetProcessesByCode = new LinkedHashMap<>();
        for (MesProProcessDO targetProcess : targetProcesses) {
            String code = StrUtil.trim(targetProcess.getCode());
            if (targetProcess.getId() == null || StrUtil.isBlank(code)) {
                throw routeProcessIdentityNotFound(null, null, targetProcess.getId(), code);
            }
            targetProcessesByCode.computeIfAbsent(code, key -> new ArrayList<>()).add(targetProcess);
        }
        if (targetProcesses.size() != distinctTargetIds.size()) {
            List<Long> foundIds = targetProcesses.stream().map(MesProProcessDO::getId).toList();
            Long missingId = distinctTargetIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw routeProcessIdentityNotFound(null, null, missingId, null);
        }

        Map<Long, Long> identityMap = new LinkedHashMap<>();
        for (Long targetId : distinctTargetIds) {
            identityMap.put(targetId, targetId);
        }
        for (MesProProcessDO alias : processMapper.selectListByCodesIgnoreDeleted(targetProcessesByCode.keySet())) {
            Long aliasId = alias.getId();
            String aliasCode = StrUtil.trim(alias.getCode());
            List<MesProProcessDO> codeTargets = targetProcessesByCode.get(aliasCode);
            if (aliasId == null || codeTargets == null) {
                continue;
            }
            List<Long> targetIds = codeTargets.stream()
                    .map(MesProProcessDO::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (targetIds.contains(aliasId)) {
                identityMap.put(aliasId, aliasId);
                continue;
            }
            if (Boolean.FALSE.equals(alias.getDeleted())) {
                continue;
            }
            if (targetIds.size() == 1) {
                identityMap.put(aliasId, targetIds.get(0));
                continue;
            }
            List<Long> activeTargetIds = codeTargets.stream()
                    .filter(target -> Boolean.FALSE.equals(target.getDeleted()))
                    .map(MesProProcessDO::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (activeTargetIds.size() == 1) {
                identityMap.put(aliasId, activeTargetIds.get(0));
                continue;
            }
            if (Boolean.TRUE.equals(alias.getDeleted())) {
                continue;
            }
            throw exception(PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS,
                    null, aliasId, aliasCode, targetIds);
        }
        return identityMap;
    }

    @Override
    public List<MesProRouteProcessDO> getRouteProcessListByProductId(Long productId) {
        // 1. 根据产品查找关联的工艺路线产品记录
        MesProRouteProductDO routeProduct = routeProductService.getRouteProductByItemId(productId);
        if (routeProduct == null) {
            return Collections.emptyList();
        }
        // 2. 返回该工艺路线的工序列表
        return routeProcessMapper.selectListByRouteId(routeProduct.getRouteId());
    }

    @Override
    public List<MesProRouteProcessDO> getRouteProcessListByProcessId(Long processId) {
        return routeProcessMapper.selectListByProcessId(processId);
    }

    @Override
    public List<MesProRouteProcessDO> getRouteProcessListByProcessIds(Collection<Long> processIds) {
        if (CollUtil.isEmpty(processIds)) {
            return Collections.emptyList();
        }
        return routeProcessMapper.selectListByProcessIds(processIds);
    }

    @Override
    public void deleteRouteProcessByRouteId(Long routeId) {
        routeProcessMapper.deleteByRouteId(routeId);
    }

}
