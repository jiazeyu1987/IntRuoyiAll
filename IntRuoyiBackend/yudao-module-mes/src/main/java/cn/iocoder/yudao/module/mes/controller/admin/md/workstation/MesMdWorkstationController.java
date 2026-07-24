package cn.iocoder.yudao.module.mes.controller.admin.md.workstation;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.BalloonProcessDeviceMappingImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityMetrics;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.importer.BalloonProcessDeviceMappingImportService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;

@Tag(name = "管理后台 - MES 工作站")
@RestController
@RequestMapping("/mes/md-workstation")
@Validated
public class MesMdWorkstationController {

    @Resource
    private MesMdWorkstationService workstationService;
    @Resource
    private MesMdWorkshopService workshopService;
    @Resource
    private MesMdProductionLineService productionLineService;
    @Resource
    private MesProProcessService processService;
    @Resource
    private MesMdWorkstationCapacityService workstationCapacityService;
    @Resource
    private MesMdWorkstationMachineService workstationMachineService;
    @Resource
    private MesDvMachineryService machineryService;
    @Resource
    private BalloonProcessDeviceMappingImportService balloonProcessDeviceMappingImportService;

    @PostMapping("/create")
    @Operation(summary = "创建工作站")
    @PreAuthorize("@ss.hasPermission('mes:md-workstation:create')")
    public CommonResult<Long> createWorkstation(@Valid @RequestBody MesMdWorkstationSaveReqVO createReqVO) {
        return success(workstationService.createWorkstation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工作站")
    @PreAuthorize("@ss.hasPermission('mes:md-workstation:update')")
    public CommonResult<Boolean> updateWorkstation(@Valid @RequestBody MesMdWorkstationSaveReqVO updateReqVO) {
        workstationService.updateWorkstation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工作站")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:md-workstation:delete')")
    public CommonResult<Boolean> deleteWorkstation(@RequestParam("id") Long id) {
        workstationService.deleteWorkstation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工作站")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:md-workstation:query')")
    public CommonResult<MesMdWorkstationRespVO> getWorkstation(@RequestParam("id") Long id) {
        MesMdWorkstationDO workstation = workstationService.getWorkstation(id);
        return success(BeanUtils.toBean(workstation, MesMdWorkstationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工作站分页")
    @PreAuthorize("@ss.hasPermission('mes:md-workstation:query')")
    public CommonResult<PageResult<MesMdWorkstationRespVO>> getWorkstationPage(@Valid MesMdWorkstationPageReqVO pageReqVO) {
        PageResult<MesMdWorkstationDO> pageResult = workstationService.getWorkstationPage(pageReqVO);
        return success(new PageResult<>(
                buildWorkstationRespVOList(pageResult.getList()),
                pageResult.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出工作站 Excel")
    @PreAuthorize("@ss.hasPermission('mes:md-workstation:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWorkstationExcel(@Valid MesMdWorkstationPageReqVO pageReqVO,
                                       HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesMdWorkstationDO> list = workstationService.getWorkstationPage(pageReqVO).getList();
        List<MesMdWorkstationRespVO> voList = buildWorkstationRespVOList(list);
        ExcelUtils.write(response, "工作站.xls", "数据", MesMdWorkstationRespVO.class, voList);
    }

    @PostMapping("/import-balloon-process-device-mapping")
    @Operation(summary = "同步球囊工序工作站设备关系")
    @PreAuthorize("@ss.hasPermission('mes:md-workstation:update')")
    public CommonResult<BalloonProcessDeviceMappingImportRespVO> importBalloonProcessDeviceMapping(
            @RequestParam("file") MultipartFile file,
            @RequestParam("workshopId") Long workshopId) {
        return success(balloonProcessDeviceMappingImportService.importMapping(file, workshopId));
    }

    private List<MesMdWorkstationRespVO> buildWorkstationRespVOList(List<MesMdWorkstationDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, MesMdWorkshopDO> workshopMap = workshopService.getWorkshopMap(
                convertSet(list, MesMdWorkstationDO::getWorkshopId));
        Map<Long, MesMdProductionLineDO> productionLineMap = productionLineService.getProductionLineMap(
                convertSet(list, MesMdWorkstationDO::getProductionLineId));
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(
                convertSet(list, MesMdWorkstationDO::getProcessId));
        Map<Long, MesMdWorkstationCapacityMetrics> capacityMetricsMap =
                workstationCapacityService.getCapacityMetricsUsingShiftHours(list);
        List<MesMdWorkstationMachineDO> workstationMachines =
                workstationMachineService.getWorkstationMachineListByWorkstationIds(
                        convertSet(list, MesMdWorkstationDO::getId));
        Map<Long, List<MesMdWorkstationMachineDO>> machineMap =
                convertMultiMap(workstationMachines, MesMdWorkstationMachineDO::getWorkstationId);
        Map<Long, MesDvMachineryDO> machineryMap =
                machineryService.getMachineryMap(convertSet(workstationMachines, MesMdWorkstationMachineDO::getMachineryId));
        return BeanUtils.toBean(list, MesMdWorkstationRespVO.class, vo -> {
            MapUtils.findAndThen(workshopMap, vo.getWorkshopId(),
                    workshop -> vo.setWorkshopName(workshop.getName()));
            MapUtils.findAndThen(productionLineMap, vo.getProductionLineId(),
                    line -> vo.setProductionLineName(line.getName()));
            MapUtils.findAndThen(processMap, vo.getProcessId(),
                    process -> vo.setProcessName(process.getName()));
            vo.setMachinerySummary(buildMachinerySummary(vo.getId(), machineMap, machineryMap));
            vo.setMachineryCount(getMachineryCount(vo.getId(), machineMap));
            MapUtils.findAndThen(capacityMetricsMap, vo.getId(), metrics -> vo
                    .setConfiguredWorkerCount(metrics.getConfiguredWorkerCount())
                    .setCurrentWorkerCount(metrics.getCurrentWorkerCount())
                    .setMachineryStandardHourlyCapacity(metrics.getMachineryStandardHourlyCapacity())
                    .setTodayCapacity(metrics.getTodayCapacity()));
        });
    }

    private Integer getMachineryCount(Long workstationId,
                                      Map<Long, List<MesMdWorkstationMachineDO>> machineMap) {
        return machineMap.getOrDefault(workstationId, Collections.emptyList()).size();
    }

    private String buildMachinerySummary(Long workstationId,
                                         Map<Long, List<MesMdWorkstationMachineDO>> machineMap,
                                         Map<Long, MesDvMachineryDO> machineryMap) {
        List<MesMdWorkstationMachineDO> machines = machineMap.getOrDefault(workstationId, Collections.emptyList());
        if (CollUtil.isEmpty(machines)) {
            return "未绑定";
        }
        return machines.stream()
                .sorted(Comparator.comparing(MesMdWorkstationMachineDO::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .map(machine -> buildMachinerySummaryItem(machine, machineryMap))
                .collect(Collectors.joining("；"));
    }

    private String buildMachinerySummaryItem(MesMdWorkstationMachineDO machine,
                                             Map<Long, MesDvMachineryDO> machineryMap) {
        MesDvMachineryDO machinery = machineryMap.get(machine.getMachineryId());
        if (machinery == null) {
            throw new IllegalStateException(String.format(
                    "工作站设备绑定缺少设备主数据: workstationMachineId=%s, machineryId=%s",
                    machine.getId(), machine.getMachineryId()));
        }
        String summary = machinery.getCode() + " / " + machinery.getName();
        return machine.getQuantity() == null ? summary : summary + " ×" + machine.getQuantity();
    }
}
