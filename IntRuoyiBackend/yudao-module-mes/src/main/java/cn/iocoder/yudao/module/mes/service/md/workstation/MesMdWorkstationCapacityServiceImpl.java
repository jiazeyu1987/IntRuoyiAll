package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workrecord.MesProWorkRecordDO;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.pro.workrecord.MesProWorkRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_EFFECTIVE_HOURS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_SHIFT_HOURS_REQUIRED;

@Service
public class MesMdWorkstationCapacityServiceImpl implements MesMdWorkstationCapacityService {

    @Resource
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Resource
    private MesMdWorkstationMachineService workstationMachineService;
    @Resource
    private MesDvMachineryProcessService machineryProcessService;
    @Resource
    private MesDvMachineryService machineryService;
    @Resource
    private MesProWorkRecordService workRecordService;

    @Override
    public Map<Long, MesMdWorkstationCapacityMetrics> getCapacityMetrics(Collection<MesMdWorkstationDO> workstations,
                                                                         BigDecimal effectiveHours) {
        if (CollUtil.isEmpty(workstations)) {
            return Collections.emptyMap();
        }
        BigDecimal checkedEffectiveHours = requireEffectiveHours(effectiveHours);
        return buildCapacityMetrics(workstations, workstation -> checkedEffectiveHours);
    }

    @Override
    public Map<Long, MesMdWorkstationCapacityMetrics> getCapacityMetricsUsingShiftHours(
            Collection<MesMdWorkstationDO> workstations) {
        if (CollUtil.isEmpty(workstations)) {
            return Collections.emptyMap();
        }
        workstations.forEach(this::requireWorkstationShiftHours);
        return buildCapacityMetrics(workstations, MesMdWorkstationDO::getShiftHours);
    }

    private Map<Long, MesMdWorkstationCapacityMetrics> buildCapacityMetrics(
            Collection<MesMdWorkstationDO> workstations,
            Function<MesMdWorkstationDO, BigDecimal> effectiveHoursResolver) {
        List<Long> workstationIds = workstations.stream().map(MesMdWorkstationDO::getId).toList();

        Map<Long, List<MesMdWorkstationWorkerDO>> workerMap = convertMultiMap(
                workstationWorkerService.getWorkstationWorkerListByWorkstationIds(workstationIds),
                MesMdWorkstationWorkerDO::getWorkstationId);
        Map<Long, List<MesMdWorkstationMachineDO>> machineMap = convertMultiMap(
                workstationMachineService.getWorkstationMachineListByWorkstationIds(workstationIds),
                MesMdWorkstationMachineDO::getWorkstationId);
        Map<Long, Integer> currentWorkerCountMap = convertMap(
                workRecordService.getClockInWorkRecordListByWorkstationIds(workstationIds),
                MesProWorkRecordDO::getWorkstationId,
                record -> 1,
                Integer::sum);

        List<Long> machineryIds = machineMap.values().stream()
                .flatMap(List::stream)
                .map(MesMdWorkstationMachineDO::getMachineryId)
                .filter(this::isPositiveId)
                .distinct()
                .toList();
        List<Long> processIds = workstations.stream()
                .map(MesMdWorkstationDO::getProcessId)
                .filter(this::isPositiveId)
                .distinct()
                .toList();
        Map<String, MesDvMachineryProcessDO> machineryProcessMap = new java.util.LinkedHashMap<>();
        if (CollUtil.isNotEmpty(machineryIds) && CollUtil.isNotEmpty(processIds)) {
            for (MesDvMachineryProcessDO machineryProcess
                    : machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                            machineryIds, processIds)) {
                String key = buildMachineryProcessKey(machineryProcess.getMachineryId(), machineryProcess.getProcessId());
                machineryProcessMap.merge(key, machineryProcess, this::pickProcessCapacityRow);
            }
        }
        Map<Long, MesDvMachineryDO> machineryMap = machineryService.getMachineryMap(machineryIds);

        return convertMap(workstations, MesMdWorkstationDO::getId, workstation -> {
            int configuredWorkerCount = workerMap.getOrDefault(workstation.getId(), Collections.emptyList()).stream()
                    .map(MesMdWorkstationWorkerDO::getQuantity)
                    .filter(quantity -> quantity != null)
                    .reduce(0, Integer::sum);
            int currentWorkerCount = currentWorkerCountMap.getOrDefault(workstation.getId(), 0);

            List<MesMdWorkstationMachineDO> machines = machineMap.getOrDefault(workstation.getId(), Collections.emptyList());
            BigDecimal machineryStandardHourlyCapacity = machines.stream()
                    .map(machine -> calculateMachineryCapacity(workstation, machine, machineryMap, machineryProcessMap))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal effectiveHours = effectiveHoursResolver.apply(workstation);
            BigDecimal todayCapacity;
            if (CollUtil.isNotEmpty(machines)) {
                todayCapacity = machineryStandardHourlyCapacity.multiply(effectiveHours);
            } else if (workstation.getSingleStandardHourlyCapacity() != null) {
                todayCapacity = workstation.getSingleStandardHourlyCapacity().multiply(effectiveHours);
            } else {
                todayCapacity = BigDecimal.ZERO;
            }

            return MesMdWorkstationCapacityMetrics.builder()
                    .configuredWorkerCount(configuredWorkerCount)
                    .currentWorkerCount(currentWorkerCount)
                    .machineryStandardHourlyCapacity(machineryStandardHourlyCapacity)
                    .todayCapacity(todayCapacity)
                    .build();
        });
    }

    private BigDecimal requireEffectiveHours(BigDecimal effectiveHours) {
        if (effectiveHours == null || effectiveHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(MD_WORKSTATION_EFFECTIVE_HOURS_REQUIRED);
        }
        return effectiveHours;
    }

    private void requireWorkstationShiftHours(MesMdWorkstationDO workstation) {
        BigDecimal shiftHours = workstation.getShiftHours();
        if (shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(MD_WORKSTATION_SHIFT_HOURS_REQUIRED, workstation.getId());
        }
    }

    private BigDecimal calculateMachineryCapacity(MesMdWorkstationDO workstation,
                                                  MesMdWorkstationMachineDO machineBinding,
                                                  Map<Long, MesDvMachineryDO> machineryMap,
                                                  Map<String, MesDvMachineryProcessDO> machineryProcessMap) {
        if (machineBinding.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        if (!isPositiveId(machineBinding.getMachineryId()) || !isPositiveId(workstation.getProcessId())) {
            return BigDecimal.ZERO;
        }
        MesDvMachineryProcessDO machineryProcess = machineryProcessMap.get(
                buildMachineryProcessKey(machineBinding.getMachineryId(), workstation.getProcessId()));
        if (machineryProcess != null && machineryProcess.getStandardHourlyCapacity() != null) {
            return machineryProcess.getStandardHourlyCapacity()
                    .multiply(BigDecimal.valueOf(machineBinding.getQuantity()));
        }
        return BigDecimal.ZERO;
    }

    private MesDvMachineryProcessDO pickProcessCapacityRow(MesDvMachineryProcessDO existing,
                                                           MesDvMachineryProcessDO current) {
        if (existing == null) {
            return current;
        }
        if (existing.getStandardHourlyCapacity() == null) {
            return current;
        }
        if (current.getStandardHourlyCapacity() == null) {
            return existing;
        }
        if (existing.getStandardHourlyCapacity().compareTo(current.getStandardHourlyCapacity()) == 0) {
            return existing;
        }
        throw new IllegalStateException(String.format("设备工序产能存在冲突: machineryId=%s, processId=%s",
                current.getMachineryId(), current.getProcessId()));
    }

    private String buildMachineryProcessKey(Long machineryId, Long processId) {
        return machineryId + ":" + processId;
    }

    private boolean isPositiveId(Long id) {
        return id != null && id > 0;
    }
}
