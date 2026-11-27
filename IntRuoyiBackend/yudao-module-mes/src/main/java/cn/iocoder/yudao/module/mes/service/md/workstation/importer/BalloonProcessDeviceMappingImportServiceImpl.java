package cn.iocoder.yudao.module.mes.service.md.workstation.importer;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.BalloonProcessDeviceMappingImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkshopMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationWorkerMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.Sheet1MachineryProcessExcelParser;
import cn.iocoder.yudao.module.mes.service.dv.machinery.Sheet1ProcessResolver;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseLocationService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Validated
public class BalloonProcessDeviceMappingImportServiceImpl implements BalloonProcessDeviceMappingImportService {

    private static final String WORKSTATION_REMARK = "球囊扩张导管工序设备关系同步";
    private static final String MACHINE_BINDING_REMARK = "球囊扩张导管工序设备关系同步";
    private static final String WORKER_BINDING_REMARK = "球囊扩张导管工序人工资源同步";
    private static final int WORKSTATION_CAPACITY_SCALE = 2;
    private static final int DEFAULT_MANUAL_WORKER_COUNT = 5;

    @Resource
    private Sheet1MachineryProcessExcelParser parser;
    @Resource
    private Sheet1ProcessResolver processResolver;
    @Resource
    private MesMdWorkshopMapper workshopMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Resource
    private MesMdWorkstationWorkerMapper workstationWorkerMapper;
    @Resource
    private MesDvMachineryMapper machineryMapper;
    @Resource
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Resource
    private MesMdWorkstationService workstationService;
    @Resource
    private MesWmWarehouseService warehouseService;
    @Resource
    private MesWmWarehouseLocationService locationService;
    @Resource
    private MesWmWarehouseAreaService areaService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BalloonProcessDeviceMappingImportRespVO importMapping(MultipartFile file, Long workshopId) {
        MesMdWorkshopDO targetWorkshop = workshopMapper.selectById(workshopId);
        if (targetWorkshop == null) {
            throw ServiceExceptionUtil.invalidParamException("目标车间不存在: {}", workshopId);
        }

        var parsed = parser.parse(file);
        Map<String, MesProProcessDO> processMap = processResolver.resolve(parsed);
        NormalizedSheet normalizedSheet = normalize(parsed);
        Map<String, MesDvMachineryDO> machineryByCode = resolveExistingMachineryMap(normalizedSheet.deviceRows());
        validateExistingMachineryProcessPairs(normalizedSheet.deviceRows(), processMap, machineryByCode);
        WorkstationSyncResult workstationSyncResult = syncWorkstations(normalizedSheet, processMap, targetWorkshop);
        int machineryBindingCount = replaceWorkstationMachineBindings(
                normalizedSheet.deviceRows(), processMap, machineryByCode, workstationSyncResult.workstationIdByProcessId());
        replaceWorkstationWorkerBindings(normalizedSheet.manualRows(), processMap,
                workstationSyncResult.workstationIdByProcessId());

        return BalloonProcessDeviceMappingImportRespVO.builder()
                .processCount(normalizedSheet.processCodes().size())
                .machineryCount(machineryByCode.size())
                .machineryProcessCount(normalizedSheet.deviceRows().size())
                .reusedWorkstationCount(workstationSyncResult.reusedCount())
                .createdWorkstationCount(workstationSyncResult.createdCount())
                .machineryBindingCount(machineryBindingCount)
                .manualProcessCount(normalizedSheet.manualRows().size())
                .ignoredPlaceholderRowCount(normalizedSheet.ignoredPlaceholderRowCount())
                .createdMachineryCount(0)
                .updatedMachineryCount(0)
                .ignoredCapacityConflictPairCount(normalizedSheet.ignoredCapacityConflictPairs().size())
                .ignoredCapacityConflictPairs(normalizedSheet.ignoredCapacityConflictPairs())
                .build();
    }

    private NormalizedSheet normalize(Sheet1MachineryProcessExcelParser.ParsedSheet parsedSheet) {
        Map<String, NormalizedDeviceRow> deviceRowMap = new LinkedHashMap<>();
        Map<String, CapacityConflictAccumulator> capacityConflictMap = new LinkedHashMap<>();
        for (Sheet1MachineryProcessExcelParser.DeviceRow deviceRow : parsedSheet.deviceRows()) {
            Integer quantity = toPositiveInt(deviceRow.deviceQuantity(), "设备数量", deviceRow.sourceRowNo());
            String key = buildProcessDeviceKey(deviceRow.processCode(), deviceRow.machineryCode());
            NormalizedDeviceRow normalized = new NormalizedDeviceRow(
                    deviceRow.sourceRowNo(),
                    deviceRow.machineryCode(),
                    deviceRow.processCode(),
                    deviceRow.processName(),
                    deviceRow.deviceName(),
                    quantity);
            NormalizedDeviceRow existing = deviceRowMap.get(key);
            if (existing == null) {
                deviceRowMap.put(key, normalized);
                capacityConflictMap.put(key, new CapacityConflictAccumulator(deviceRow.processName(), deviceRow.machineryCode())
                        .add(deviceRow.sourceRowNo(), deviceRow.tenHalfHourDailyCapacity().stripTrailingZeros().toPlainString()));
                continue;
            }
            if (!Objects.equals(existing.deviceName(), normalized.deviceName())
                    || !Objects.equals(existing.deviceQuantity(), normalized.deviceQuantity())) {
                throw ServiceExceptionUtil.invalidParamException(
                        "Excel 第 {} 行与第 {} 行的工序设备配置冲突: 工序 [{}], 设备编码 [{}]",
                        existing.sourceRowNo(), normalized.sourceRowNo(), normalized.processName(), normalized.machineryCode());
            }
            capacityConflictMap.get(key)
                    .add(deviceRow.sourceRowNo(), deviceRow.tenHalfHourDailyCapacity().stripTrailingZeros().toPlainString());
        }

        Map<String, NormalizedManualRow> manualRowMap = new LinkedHashMap<>();
        for (Sheet1MachineryProcessExcelParser.ManualRow manualRow : parsedSheet.manualRows()) {
            NormalizedManualRow normalized = new NormalizedManualRow(
                    manualRow.sourceRowNo(),
                    manualRow.processCode(),
                    manualRow.processName(),
                    manualRow.manualDailyCapacity().stripTrailingZeros().toPlainString(),
                    normalizeWorkstationCapacity(manualRow.singleStandardHourlyCapacity()));
            NormalizedManualRow existing = manualRowMap.get(normalized.processCode());
            if (existing == null) {
                manualRowMap.put(normalized.processCode(), normalized);
                continue;
            }
            if (!Objects.equals(existing.manualDailyCapacity(), normalized.manualDailyCapacity())) {
                throw ServiceExceptionUtil.invalidParamException(
                        "Excel 第 {} 行与第 {} 行的人工产能冲突: 工序 [{}]",
                        existing.sourceRowNo(), normalized.sourceRowNo(), normalized.processName());
            }
        }

        Set<String> processCodes = new LinkedHashSet<>();
        deviceRowMap.values().forEach(row -> processCodes.add(row.processCode()));
        manualRowMap.values().forEach(row -> processCodes.add(row.processCode()));
        for (String processCode : manualRowMap.keySet()) {
            if (deviceRowMap.values().stream().anyMatch(row -> processCode.equals(row.processCode()))) {
                throw ServiceExceptionUtil.invalidParamException("工序 [{}] 同时存在设备映射和纯人工行，无法同步", processCode);
            }
        }

        List<BalloonProcessDeviceMappingImportRespVO.IgnoredCapacityConflictPair> warnings = capacityConflictMap.values()
                .stream()
                .filter(CapacityConflictAccumulator::hasConflict)
                .map(CapacityConflictAccumulator::toWarning)
                .toList();

        return new NormalizedSheet(
                new ArrayList<>(deviceRowMap.values()),
                manualRowMap,
                processCodes,
                parsedSheet.ignoredPlaceholderRowCount(),
                warnings);
    }

    private Set<String> selectExistingMachineryProcessKeys(Collection<Long> machineryIds) {
        Set<String> result = new LinkedHashSet<>();
        for (MesDvMachineryProcessDO row : machineryProcessMapper.selectListByMachineryIds(machineryIds)) {
            if (row.getProcessId() == null || row.getMachineryId() == null) {
                continue;
            }
            result.add(buildMachineryProcessKey(row.getProcessId(), row.getMachineryId()));
        }
        return result;
    }

    private Map<String, MesDvMachineryDO> resolveExistingMachineryMap(List<NormalizedDeviceRow> deviceRows) {
        Set<String> machineryCodes = deviceRows.stream()
                .map(NormalizedDeviceRow::machineryCode)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        if (machineryCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, MesDvMachineryDO> machineryByCode = new LinkedHashMap<>();
        for (MesDvMachineryDO machinery : machineryMapper.selectListByCodes(machineryCodes)) {
            machineryByCode.put(machinery.getCode(), machinery);
        }
        if (machineryByCode.size() != machineryCodes.size()) {
            Set<String> missing = new LinkedHashSet<>(machineryCodes);
            missing.removeAll(machineryByCode.keySet());
            throw ServiceExceptionUtil.invalidParamException("以下设备编码不存在: {}", String.join(", ", missing));
        }
        return machineryByCode;
    }

    private void validateExistingMachineryProcessPairs(List<NormalizedDeviceRow> deviceRows,
                                                       Map<String, MesProProcessDO> processMap,
                                                       Map<String, MesDvMachineryDO> machineryByCode) {
        Set<Long> machineryIds = machineryByCode.values().stream()
                .map(MesDvMachineryDO::getId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Set<String> existingPairKeys = selectExistingMachineryProcessKeys(machineryIds);

        List<String> missingPairs = new ArrayList<>();
        for (NormalizedDeviceRow row : deviceRows) {
            Long processId = processMap.get(row.processCode()).getId();
            Long machineryId = machineryByCode.get(row.machineryCode()).getId();
            String key = buildMachineryProcessKey(processId, machineryId);
            if (!existingPairKeys.contains(key)) {
                missingPairs.add(row.processName() + "（" + row.processCode() + "）" + " + " + row.machineryCode());
            }
        }
        if (!missingPairs.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException(
                    "以下工序设备对在设备工序明细中不存在: {}", String.join(", ", missingPairs));
        }
    }

    private WorkstationSyncResult syncWorkstations(NormalizedSheet normalizedSheet,
                                                   Map<String, MesProProcessDO> processMap,
                                                   MesMdWorkshopDO targetWorkshop) {
        Map<Long, List<MesMdWorkstationDO>> workstationMap = new LinkedHashMap<>();
        List<MesMdWorkstationDO> existingWorkstations = workstationMapper.selectListByProcessIds(
                processMap.values().stream().map(MesProProcessDO::getId).toList());
        for (MesMdWorkstationDO workstation : existingWorkstations) {
            workstationMap.computeIfAbsent(workstation.getProcessId(), key -> new ArrayList<>()).add(workstation);
        }

        int reusedCount = 0;
        int createdCount = 0;
        Map<Long, Long> workstationIdByProcessId = new LinkedHashMap<>();
        for (String processCode : normalizedSheet.processCodes()) {
            MesProProcessDO process = processMap.get(processCode);
            List<MesMdWorkstationDO> candidates = workstationMap.getOrDefault(process.getId(), List.of());
            if (candidates.size() > 1) {
                throw ServiceExceptionUtil.invalidParamException("工序 [{}] 存在多个有效工作站，无法自动同步", processCode);
            }
            if (candidates.isEmpty()) {
                MesMdWorkstationSaveReqVO createReqVO = buildCreateWorkstationReq(process, targetWorkshop,
                        resolveSingleStandardHourlyCapacity(processCode, normalizedSheet.manualRows()));
                applyVirtualWarehouseHierarchyIfMissing(createReqVO);
                Long workstationId = workstationService.createWorkstation(createReqVO);
                if (workstationId == null) {
                    throw ServiceExceptionUtil.invalidParamException("工序 [{}] 创建工作站未返回编号，无法同步", processCode);
                }
                workstationIdByProcessId.put(process.getId(), workstationId);
                createdCount++;
                continue;
            }

            MesMdWorkstationDO existing = candidates.get(0);
            BigDecimal singleStandardHourlyCapacity = resolveSingleStandardHourlyCapacity(processCode, normalizedSheet.manualRows());
            MesMdWorkstationSaveReqVO updateReqVO = buildUpdateWorkstationReq(existing, process, targetWorkshop,
                    singleStandardHourlyCapacity);
            applyVirtualWarehouseHierarchyIfMissing(updateReqVO);
            workstationService.updateWorkstation(updateReqVO);
            workstationMapper.updateSingleStandardHourlyCapacity(existing.getId(), singleStandardHourlyCapacity);
            workstationIdByProcessId.put(process.getId(), existing.getId());
            reusedCount++;
        }
        return new WorkstationSyncResult(workstationIdByProcessId, reusedCount, createdCount);
    }

    private void applyVirtualWarehouseHierarchyIfMissing(MesMdWorkstationSaveReqVO reqVO) {
        if (reqVO.getWarehouseId() != null || reqVO.getLocationId() != null || reqVO.getAreaId() != null) {
            return;
        }
        MesWmWarehouseDO warehouse = warehouseService.ensureWarehouseByCode(
                MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE);
        MesWmWarehouseLocationDO location = locationService.getWarehouseLocationByCode(
                MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION);
        MesWmWarehouseAreaDO area = areaService.getWarehouseAreaByCode(
                MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA);
        if (warehouse == null || warehouse.getId() == null
                || location == null || location.getId() == null
                || area == null || area.getId() == null
                || !Objects.equals(location.getWarehouseId(), warehouse.getId())
                || !Objects.equals(area.getLocationId(), location.getId())) {
            throw ServiceExceptionUtil.invalidParamException("虚拟线边仓储层级不完整，无法同步工作站");
        }
        reqVO.setWarehouseId(warehouse.getId());
        reqVO.setLocationId(location.getId());
        reqVO.setAreaId(area.getId());
    }

    private BigDecimal resolveSingleStandardHourlyCapacity(String processCode,
                                                           Map<String, NormalizedManualRow> manualRows) {
        NormalizedManualRow manualRow = manualRows.get(processCode);
        return manualRow == null ? null : manualRow.singleStandardHourlyCapacity();
    }

    private BigDecimal normalizeWorkstationCapacity(BigDecimal singleStandardHourlyCapacity) {
        return singleStandardHourlyCapacity.setScale(WORKSTATION_CAPACITY_SCALE, RoundingMode.HALF_UP);
    }

    private MesMdWorkstationSaveReqVO buildCreateWorkstationReq(MesProProcessDO process,
                                                                MesMdWorkshopDO targetWorkshop,
                                                                BigDecimal singleStandardHourlyCapacity) {
        MesMdWorkstationSaveReqVO reqVO = new MesMdWorkstationSaveReqVO();
        reqVO.setCode("WS-" + process.getCode());
        reqVO.setName(process.getName() + "-工位");
        reqVO.setAddress(targetWorkshop.getName());
        reqVO.setWorkshopId(targetWorkshop.getId());
        reqVO.setProcessId(process.getId());
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setRemark(WORKSTATION_REMARK);
        reqVO.setSingleStandardHourlyCapacity(singleStandardHourlyCapacity);
        return reqVO;
    }

    private MesMdWorkstationSaveReqVO buildUpdateWorkstationReq(MesMdWorkstationDO existing,
                                                                MesProProcessDO process,
                                                                MesMdWorkshopDO targetWorkshop,
                                                                BigDecimal singleStandardHourlyCapacity) {
        MesMdWorkstationSaveReqVO reqVO = new MesMdWorkstationSaveReqVO();
        reqVO.setId(existing.getId());
        reqVO.setCode("WS-" + process.getCode());
        reqVO.setName(process.getName() + "-工位");
        reqVO.setAddress(targetWorkshop.getName());
        reqVO.setWorkshopId(targetWorkshop.getId());
        reqVO.setProcessId(process.getId());
        reqVO.setWarehouseId(existing.getWarehouseId());
        reqVO.setLocationId(existing.getLocationId());
        reqVO.setAreaId(existing.getAreaId());
        reqVO.setSingleStandardHourlyCapacity(singleStandardHourlyCapacity);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setRemark(existing.getRemark() != null ? existing.getRemark() : WORKSTATION_REMARK);
        return reqVO;
    }

    private int replaceWorkstationMachineBindings(List<NormalizedDeviceRow> deviceRows,
                                                  Map<String, MesProProcessDO> processMap,
                                                  Map<String, MesDvMachineryDO> machineryByCode,
                                                  Map<Long, Long> workstationIdByProcessId) {
        for (Long workstationId : workstationIdByProcessId.values()) {
            workstationMachineMapper.deleteByWorkstationId(workstationId);
        }
        int count = 0;
        for (NormalizedDeviceRow row : deviceRows) {
            MesProProcessDO process = processMap.get(row.processCode());
            MesDvMachineryDO machinery = machineryByCode.get(row.machineryCode());
            Long workstationId = workstationIdByProcessId.get(process.getId());
            if (workstationId == null) {
                throw ServiceExceptionUtil.invalidParamException("工序 [{}] 未找到同步后的工作站，无法绑定设备", row.processName());
            }
            workstationMachineMapper.insert(MesMdWorkstationMachineDO.builder()
                    .workstationId(workstationId)
                    .machineryId(machinery.getId())
                    .quantity(row.deviceQuantity())
                    .remark(MACHINE_BINDING_REMARK)
                    .build());
            count++;
        }
        return count;
    }

    private void replaceWorkstationWorkerBindings(Map<String, NormalizedManualRow> manualRows,
                                                  Map<String, MesProProcessDO> processMap,
                                                  Map<Long, Long> workstationIdByProcessId) {
        for (Long workstationId : workstationIdByProcessId.values()) {
            workstationWorkerMapper.deleteByWorkstationId(workstationId);
        }
        for (NormalizedManualRow row : manualRows.values()) {
            MesProProcessDO process = processMap.get(row.processCode());
            Long workstationId = workstationIdByProcessId.get(process.getId());
            if (workstationId == null) {
                throw ServiceExceptionUtil.invalidParamException("工序 [{}] 未找到同步后的工作站，无法配置人数", row.processName());
            }
            workstationWorkerMapper.insert(MesMdWorkstationWorkerDO.builder()
                    .workstationId(workstationId)
                    .quantity(DEFAULT_MANUAL_WORKER_COUNT)
                    .remark(WORKER_BINDING_REMARK)
                    .build());
        }
    }

    private Integer toPositiveInt(java.math.BigDecimal value, String fieldName, Integer sourceRowNo) {
        if (value == null) {
            throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行{} 不能为空", sourceRowNo, fieldName);
        }
        try {
            int intValue = value.intValueExact();
            if (intValue <= 0) {
                throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行{} 必须大于 0", sourceRowNo, fieldName);
            }
            return intValue;
        } catch (ArithmeticException exception) {
            throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行{} 必须为正整数", sourceRowNo, fieldName);
        }
    }

    private String buildProcessDeviceKey(String processCode, String machineryCode) {
        return processCode + "::" + machineryCode;
    }

    private String buildMachineryProcessKey(Long processId, Long machineryId) {
        return processId + "::" + machineryId;
    }

    private record NormalizedSheet(List<NormalizedDeviceRow> deviceRows,
                                   Map<String, NormalizedManualRow> manualRows,
                                   Set<String> processCodes,
                                   Integer ignoredPlaceholderRowCount,
                                   List<BalloonProcessDeviceMappingImportRespVO.IgnoredCapacityConflictPair> ignoredCapacityConflictPairs) {
    }

    private record NormalizedDeviceRow(Integer sourceRowNo,
                                       String machineryCode,
                                       String processCode,
                                       String processName,
                                       String deviceName,
                                       Integer deviceQuantity) {
    }

    private record NormalizedManualRow(Integer sourceRowNo,
                                       String processCode,
                                       String processName,
                                       String manualDailyCapacity,
                                       BigDecimal singleStandardHourlyCapacity) {
    }

    private record WorkstationSyncResult(Map<Long, Long> workstationIdByProcessId,
                                         Integer reusedCount,
                                         Integer createdCount) {
    }

    private static final class CapacityConflictAccumulator {
        private final String processName;
        private final String machineryCode;
        private final List<Integer> sourceRowNos = new ArrayList<>();
        private final Set<String> dailyCapacities = new LinkedHashSet<>();

        private CapacityConflictAccumulator(String processName, String machineryCode) {
            this.processName = processName;
            this.machineryCode = machineryCode;
        }

        private CapacityConflictAccumulator add(Integer sourceRowNo, String dailyCapacity) {
            sourceRowNos.add(sourceRowNo);
            dailyCapacities.add(dailyCapacity);
            return this;
        }

        private boolean hasConflict() {
            return dailyCapacities.size() > 1;
        }

        private BalloonProcessDeviceMappingImportRespVO.IgnoredCapacityConflictPair toWarning() {
            return BalloonProcessDeviceMappingImportRespVO.IgnoredCapacityConflictPair.builder()
                    .processName(processName)
                    .machineryCode(machineryCode)
                    .sourceRowNos(sourceRowNos)
                    .dailyCapacities(new ArrayList<>(dailyCapacities))
                    .build();
        }
    }
}
