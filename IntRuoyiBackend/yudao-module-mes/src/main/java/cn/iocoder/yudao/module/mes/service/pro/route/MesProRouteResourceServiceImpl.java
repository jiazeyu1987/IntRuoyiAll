package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourcePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;

@Service
@Validated
public class MesProRouteResourceServiceImpl implements MesProRouteResourceService {

    public static final String RESOURCE_TYPE_MACHINE = "MACHINE";
    public static final String RESOURCE_TYPE_WORKER = "WORKER";
    public static final String RESOURCE_TYPE_UNCONFIGURED = "UNCONFIGURED";

    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteService routeService;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProProcessService processService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdWorkstationService workstationService;
    @Resource
    private MesMdWorkstationMachineService workstationMachineService;
    @Resource
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Resource
    private MesDvMachineryService machineryService;
    @Resource
    private MesDvMachineryProcessService machineryProcessService;

    @Override
    public PageResult<MesProRouteResourceRespVO> getResourcePage(MesProRouteResourcePageReqVO pageReqVO) {
        List<MesProRouteProductDO> routeProducts = routeProductMapper.selectList(
                new LambdaQueryWrapperX<MesProRouteProductDO>()
                        .eqIfPresent(MesProRouteProductDO::getRouteId, pageReqVO.getRouteId())
                        .eqIfPresent(MesProRouteProductDO::getItemId, pageReqVO.getProductId()));
        if (routeProducts.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        List<Long> routeIds = distinct(routeProducts, MesProRouteProductDO::getRouteId);
        List<Long> itemIds = distinct(routeProducts, MesProRouteProductDO::getItemId);
        Map<Long, MesProRouteDO> routeMap = routeService.getRouteMap(routeIds);
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(itemIds);

        List<MesProRouteProcessDO> routeProcesses = routeProcessService.getRouteProcessListByRouteIds(routeIds);
        Map<Long, List<MesProRouteProcessDO>> routeProcessMap = routeProcesses.stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesProRouteProcessDO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.groupingBy(MesProRouteProcessDO::getRouteId, LinkedHashMap::new, Collectors.toList()));

        List<Long> processIds = distinct(routeProcesses, MesProRouteProcessDO::getProcessId);
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(processIds);
        List<MesMdWorkstationDO> workstations = workstationService.getWorkstationListByProcessIds(processIds);
        Map<Long, List<MesMdWorkstationDO>> workstationMap = convertMultiMap(workstations, MesMdWorkstationDO::getProcessId);

        List<Long> workstationIds = distinct(workstations, MesMdWorkstationDO::getId);
        List<MesMdWorkstationMachineDO> machineBindings =
                workstationMachineService.getWorkstationMachineListByWorkstationIds(workstationIds);
        List<MesMdWorkstationWorkerDO> workerBindings =
                workstationWorkerService.getWorkstationWorkerListByWorkstationIds(workstationIds);
        Map<Long, List<MesMdWorkstationMachineDO>> machineBindingMap =
                convertMultiMap(machineBindings, MesMdWorkstationMachineDO::getWorkstationId);
        Map<Long, List<MesMdWorkstationWorkerDO>> workerBindingMap =
                convertMultiMap(workerBindings, MesMdWorkstationWorkerDO::getWorkstationId);

        List<Long> machineryIds = distinct(machineBindings, MesMdWorkstationMachineDO::getMachineryId);
        Map<Long, MesDvMachineryDO> machineryMap = machineryService.getMachineryMap(machineryIds);
        Map<String, MesDvMachineryProcessDO> machineryProcessMap = buildMachineryProcessMap(
                machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                        machineryIds, processIds));

        List<MesProRouteResourceRespVO> rows = new ArrayList<>();
        for (MesProRouteProductDO routeProduct : routeProducts) {
            MesProRouteDO route = require(routeMap, routeProduct.getRouteId(), "route");
            MesMdItemDO item = require(itemMap, routeProduct.getItemId(), "item");
            List<MesProRouteProcessDO> productRouteProcesses =
                    routeProcessMap.getOrDefault(routeProduct.getRouteId(), Collections.emptyList());
            for (MesProRouteProcessDO routeProcess : productRouteProcesses) {
                MesProProcessDO process = require(processMap, routeProcess.getProcessId(), "process");
                List<MesMdWorkstationDO> processWorkstations =
                        workstationMap.getOrDefault(routeProcess.getProcessId(), Collections.emptyList());
                if (processWorkstations.isEmpty()) {
                    rows.add(buildUnconfiguredRow(routeProduct, route, item, routeProcess, process));
                    continue;
                }
                for (MesMdWorkstationDO workstation : processWorkstations) {
                    List<MesMdWorkstationMachineDO> machines =
                            machineBindingMap.getOrDefault(workstation.getId(), Collections.emptyList());
                    if (machines.isEmpty()) {
                        rows.add(buildWorkerRow(routeProduct, route, item, routeProcess, process, workstation,
                                workerBindingMap.getOrDefault(workstation.getId(), Collections.emptyList())));
                    } else {
                        for (MesMdWorkstationMachineDO machineBinding : machines) {
                            rows.add(buildMachineRow(routeProduct, route, item, routeProcess, process,
                                    workstation, machineBinding, machineryMap, machineryProcessMap));
                        }
                    }
                }
            }
        }

        rows = rows.stream()
                .filter(row -> matchesResourceType(row, pageReqVO.getResourceType()))
                .filter(row -> matchesKeyword(row, pageReqVO.getKeyword()))
                .sorted(resourceRowComparator())
                .toList();
        return page(rows, pageReqVO);
    }

    private MesProRouteResourceRespVO buildUnconfiguredRow(MesProRouteProductDO routeProduct,
                                                           MesProRouteDO route,
                                                           MesMdItemDO item,
                                                           MesProRouteProcessDO routeProcess,
                                                           MesProProcessDO process) {
        MesProRouteResourceRespVO row = buildBaseRow(routeProduct, route, item, routeProcess, process);
        row.setRowKey(buildRowKey(routeProduct, routeProcess, null, null, RESOURCE_TYPE_UNCONFIGURED));
        row.setResourceType(RESOURCE_TYPE_UNCONFIGURED);
        row.setCapacitySource("未配置");
        row.setBudgetHourlyCapacity(BigDecimal.ZERO);
        row.setBudgetDailyCapacity(BigDecimal.ZERO);
        return row;
    }

    private MesProRouteResourceRespVO buildWorkerRow(MesProRouteProductDO routeProduct,
                                                     MesProRouteDO route,
                                                     MesMdItemDO item,
                                                     MesProRouteProcessDO routeProcess,
                                                     MesProProcessDO process,
                                                     MesMdWorkstationDO workstation,
                                                     List<MesMdWorkstationWorkerDO> workers) {
        MesMdWorkstationWorkerDO worker = workers.stream()
                .min(Comparator.comparing(MesMdWorkstationWorkerDO::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
        Integer workerQuantity = worker == null ? null : worker.getQuantity();
        BigDecimal hourlyCapacity = workstation.getSingleStandardHourlyCapacity() == null
                ? BigDecimal.ZERO
                : workstation.getSingleStandardHourlyCapacity();

        MesProRouteResourceRespVO row = buildBaseRow(routeProduct, route, item, routeProcess, process);
        fillWorkstation(row, workstation);
        row.setRowKey(buildRowKey(routeProduct, routeProcess, workstation.getId(),
                worker == null ? null : worker.getId(), RESOURCE_TYPE_WORKER));
        row.setResourceType(RESOURCE_TYPE_WORKER);
        row.setWorkstationWorkerId(worker == null ? null : worker.getId());
        row.setPostId(worker == null ? null : worker.getPostId());
        row.setWorkerQuantity(workerQuantity == null || workerQuantity <= 0 ? 0 : workerQuantity);
        row.setSingleStandardHourlyCapacity(workstation.getSingleStandardHourlyCapacity());
        row.setBudgetHourlyCapacity(hourlyCapacity);
        row.setBudgetDailyCapacity(calculateDailyCapacity(hourlyCapacity, workstation.getShiftHours()));
        row.setCapacitySource(resolveWorkerCapacitySource(workstation));
        return row;
    }

    private MesProRouteResourceRespVO buildMachineRow(MesProRouteProductDO routeProduct,
                                                      MesProRouteDO route,
                                                      MesMdItemDO item,
                                                      MesProRouteProcessDO routeProcess,
                                                      MesProProcessDO process,
                                                      MesMdWorkstationDO workstation,
                                                      MesMdWorkstationMachineDO machineBinding,
                                                      Map<Long, MesDvMachineryDO> machineryMap,
                                                      Map<String, MesDvMachineryProcessDO> machineryProcessMap) {
        MesDvMachineryDO machinery = require(machineryMap, machineBinding.getMachineryId(), "machinery");
        MesDvMachineryProcessDO machineryProcess = machineryProcessMap.get(
                buildMachineryProcessKey(machineBinding.getMachineryId(), routeProcess.getProcessId()));
        BigDecimal standardHourlyCapacity = machineryProcess == null ? null : machineryProcess.getStandardHourlyCapacity();
        BigDecimal machineHourlyCapacity = standardHourlyCapacity == null || machineBinding.getQuantity() == null
                ? BigDecimal.ZERO
                : standardHourlyCapacity.multiply(BigDecimal.valueOf(machineBinding.getQuantity()));

        MesProRouteResourceRespVO row = buildBaseRow(routeProduct, route, item, routeProcess, process);
        fillWorkstation(row, workstation);
        row.setRowKey(buildRowKey(routeProduct, routeProcess, workstation.getId(),
                machineBinding.getId(), RESOURCE_TYPE_MACHINE));
        row.setResourceType(RESOURCE_TYPE_MACHINE);
        row.setWorkstationMachineId(machineBinding.getId());
        row.setMachineryId(machinery.getId());
        row.setMachineryCode(machinery.getCode());
        row.setMachineryName(machinery.getName());
        row.setMachineryQuantity(machineBinding.getQuantity());
        row.setMachineryStandardHourlyCapacity(standardHourlyCapacity);
        row.setBudgetHourlyCapacity(machineHourlyCapacity);
        row.setBudgetDailyCapacity(calculateDailyCapacity(machineHourlyCapacity, workstation.getShiftHours()));
        row.setCapacitySource(resolveMachineCapacitySource(machineryProcess, workstation));
        return row;
    }

    private BigDecimal calculateDailyCapacity(BigDecimal hourlyCapacity, BigDecimal shiftHours) {
        if (hourlyCapacity == null || shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return hourlyCapacity.multiply(shiftHours);
    }

    private String resolveWorkerCapacitySource(MesMdWorkstationDO workstation) {
        if (workstation.getSingleStandardHourlyCapacity() == null) {
            return "人工小时产能缺失";
        }
        if (workstation.getShiftHours() == null || workstation.getShiftHours().compareTo(BigDecimal.ZERO) <= 0) {
            return "班次小时缺失";
        }
        return "人工小时产能";
    }

    private String resolveMachineCapacitySource(MesDvMachineryProcessDO machineryProcess, MesMdWorkstationDO workstation) {
        if (machineryProcess == null) {
            return "设备工序产能缺失";
        }
        if (workstation.getShiftHours() == null || workstation.getShiftHours().compareTo(BigDecimal.ZERO) <= 0) {
            return "班次小时缺失";
        }
        return "设备工序产能";
    }

    private MesProRouteResourceRespVO buildBaseRow(MesProRouteProductDO routeProduct,
                                                   MesProRouteDO route,
                                                   MesMdItemDO item,
                                                   MesProRouteProcessDO routeProcess,
                                                   MesProProcessDO process) {
        MesProRouteResourceRespVO row = new MesProRouteResourceRespVO();
        row.setRouteProductId(routeProduct.getId());
        row.setProductId(routeProduct.getItemId());
        row.setProductCode(item.getCode());
        row.setProductName(item.getName());
        row.setRouteId(route.getId());
        row.setRouteCode(route.getCode());
        row.setRouteName(route.getName());
        row.setRouteProcessId(routeProcess.getId());
        row.setProcessId(process.getId());
        row.setProcessCode(process.getCode());
        row.setProcessName(process.getName());
        row.setSort(routeProcess.getSort());
        return row;
    }

    private void fillWorkstation(MesProRouteResourceRespVO row, MesMdWorkstationDO workstation) {
        row.setWorkstationId(workstation.getId());
        row.setWorkstationCode(workstation.getCode());
        row.setWorkstationName(workstation.getName());
    }

    private Map<String, MesDvMachineryProcessDO> buildMachineryProcessMap(List<MesDvMachineryProcessDO> rows) {
        Map<String, MesDvMachineryProcessDO> result = new LinkedHashMap<>();
        for (MesDvMachineryProcessDO row : rows) {
            result.merge(buildMachineryProcessKey(row.getMachineryId(), row.getProcessId()), row,
                    this::pickConsistentMachineProcess);
        }
        return result;
    }

    private MesDvMachineryProcessDO pickConsistentMachineProcess(MesDvMachineryProcessDO existing,
                                                                 MesDvMachineryProcessDO current) {
        if (existing.getStandardHourlyCapacity() == null) {
            return current;
        }
        if (current.getStandardHourlyCapacity() == null
                || existing.getStandardHourlyCapacity().compareTo(current.getStandardHourlyCapacity()) == 0) {
            return existing;
        }
        throw new IllegalStateException(String.format(Locale.ROOT,
                "设备工序产能存在冲突: machineryId=%s, processId=%s",
                current.getMachineryId(), current.getProcessId()));
    }

    private String buildMachineryProcessKey(Long machineryId, Long processId) {
        return machineryId + ":" + processId;
    }

    private String buildRowKey(MesProRouteProductDO routeProduct, MesProRouteProcessDO routeProcess,
                               Long workstationId, Long resourceId, String resourceType) {
        return routeProduct.getId() + ":" + routeProcess.getId() + ":" + workstationId + ":" + resourceId
                + ":" + resourceType;
    }

    private boolean matchesResourceType(MesProRouteResourceRespVO row, String resourceType) {
        return StrUtil.isBlank(resourceType) || resourceType.equals(row.getResourceType());
    }

    private boolean matchesKeyword(MesProRouteResourceRespVO row, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return contains(row.getProductCode(), lowerKeyword)
                || contains(row.getProductName(), lowerKeyword)
                || contains(row.getRouteCode(), lowerKeyword)
                || contains(row.getRouteName(), lowerKeyword)
                || contains(row.getProcessCode(), lowerKeyword)
                || contains(row.getProcessName(), lowerKeyword)
                || contains(row.getWorkstationCode(), lowerKeyword)
                || contains(row.getWorkstationName(), lowerKeyword)
                || contains(row.getMachineryCode(), lowerKeyword)
                || contains(row.getMachineryName(), lowerKeyword);
    }

    private boolean contains(String value, String lowerKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private Comparator<MesProRouteResourceRespVO> resourceRowComparator() {
        return Comparator.comparing(MesProRouteResourceRespVO::getProductCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(MesProRouteResourceRespVO::getRouteCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(MesProRouteResourceRespVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MesProRouteResourceRespVO::getMachineryCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(MesProRouteResourceRespVO::getRowKey, Comparator.nullsLast(String::compareTo));
    }

    private PageResult<MesProRouteResourceRespVO> page(List<MesProRouteResourceRespVO> rows,
                                                       MesProRouteResourcePageReqVO pageReqVO) {
        int pageNo = Objects.requireNonNullElse(pageReqVO.getPageNo(), 1);
        int pageSize = Objects.requireNonNullElse(pageReqVO.getPageSize(), 10);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    private <T, R> List<R> distinct(Collection<T> rows, java.util.function.Function<T, R> mapper) {
        Set<R> result = new LinkedHashSet<>();
        for (T row : rows) {
            R value = mapper.apply(row);
            if (value != null) {
                result.add(value);
            }
        }
        return new ArrayList<>(result);
    }

    private <K, V> V require(Map<K, V> map, K key, String label) {
        V value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("Missing " + label + ": " + key);
        }
        return value;
    }
}
