package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchCapacityUnificationAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchFullConfigImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchPolicySettingsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchRouteConfigImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchShiftHoursSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchShiftHoursRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSmokeTestStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSmokeTestStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO;
import cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench.MesProSchedulerWorkbenchFullConfigPackageService;
import cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench.MesProSchedulerWorkbenchRouteConfigPackageService;
import cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench.MesProSchedulerWorkbenchSmokeTestService;
import cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench.MesProSchedulerWorkbenchService;
import cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench.MesProSchedulerWorkbenchRuntimeStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Tag(name = "管理后台 - MES 排产员工作台")
@RestController
@RequestMapping("/mes/pro/scheduler-workbench")
@Validated
public class MesProSchedulerWorkbenchController {

    @Resource
    private MesProSchedulerWorkbenchService schedulerWorkbenchService;
    @Resource
    private MesProSchedulerWorkbenchRuntimeStatusService runtimeStatusService;
    @Resource
    private MesProSchedulerWorkbenchSmokeTestService smokeTestService;
    @Resource
    private MesProSchedulerWorkbenchRouteConfigPackageService routeConfigPackageService;
    @Resource
    private MesProSchedulerWorkbenchFullConfigPackageService fullConfigPackageService;

    @GetMapping("/summary")
    @Operation(summary = "获得排产员工作台汇总")
    @Parameter(name = "date", description = "统计日期", example = "2026-06-10")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:query')")
    public CommonResult<MesProSchedulerWorkbenchSummaryRespVO> getSummary(
            @RequestParam("date") @DateTimeFormat(iso = DATE) LocalDate date) {
        return success(schedulerWorkbenchService.getSummary(date));
    }

    @GetMapping("/shift-hours")
    @Operation(summary = "获得排产员工作台班次小时设置")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:query')")
    public CommonResult<MesProSchedulerWorkbenchShiftHoursRespVO> getShiftHoursSetting() {
        return success(schedulerWorkbenchService.getShiftHoursSetting());
    }

    @PutMapping("/shift-hours")
    @Operation(summary = "保存排产员工作台班次小时设置")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:update')")
    public CommonResult<MesProSchedulerWorkbenchShiftHoursRespVO> saveShiftHoursSetting(
            @Valid @RequestBody MesProSchedulerWorkbenchShiftHoursSaveReqVO reqVO) {
        return success(schedulerWorkbenchService.saveShiftHoursSetting(reqVO.getShiftHours()));
    }

    @GetMapping("/policy-settings")
    @Operation(summary = "获得排产员工作台策略设置")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:query')")
    public CommonResult<MesProSchedulerWorkbenchPolicySettingsRespVO> getPolicySettings() {
        return success(schedulerWorkbenchService.getPolicySettings());
    }

    @GetMapping("/auto-schedule-job/status")
    @Operation(summary = "获得自动排产任务状态")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:query')")
    public CommonResult<MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO> getAutoScheduleJobStatus() {
        return success(runtimeStatusService.getAutoScheduleJobStatus());
    }

    @GetMapping("/night-shift-capacity/status")
    @Operation(summary = "获得可用夜班和产能状态")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:query')")
    public CommonResult<MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO> getNightShiftCapacityStatus() {
        return success(runtimeStatusService.getNightShiftCapacityStatus());
    }
    @GetMapping("/capacity-unification-audit")
    @Operation(summary = "获得产能统一审计")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:query')")
    public CommonResult<MesProSchedulerWorkbenchCapacityUnificationAuditRespVO> getCapacityUnificationAudit() {
        return success(schedulerWorkbenchService.getCapacityUnificationAudit());
    }

    @PutMapping("/policy-settings")
    @Operation(summary = "保存排产员工作台策略设置")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:update')")
    public CommonResult<MesProSchedulerWorkbenchPolicySettingsRespVO> savePolicySettings(
            @Valid @RequestBody MesProSchedulerWorkbenchPolicySettingsRespVO reqVO) {
        return success(schedulerWorkbenchService.savePolicySettings(reqVO));
    }

    @GetMapping("/route-config/export")
    @Operation(summary = "导出排产工艺路线配置包")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:query')")
    public void exportRouteConfigPackage(HttpServletResponse response) throws IOException {
        byte[] data = routeConfigPackageService.exportPackage();
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("排产工艺路线配置包.json"));
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(data);
    }

    @PostMapping("/route-config/import")
    @Operation(summary = "导入排产工艺路线配置包")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:update')")
    public CommonResult<MesProSchedulerWorkbenchRouteConfigImportRespVO> importRouteConfigPackage(
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(routeConfigPackageService.importPackage(file.getBytes()));
    }

    @GetMapping("/full-config/export")
    @Operation(summary = "导出排产员工作台全部数据包")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:query')")
    public void exportFullConfigPackage(HttpServletResponse response) throws IOException {
        byte[] data = fullConfigPackageService.exportPackage();
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("排产员工作台全部数据包.json"));
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(data);
    }

    @PostMapping("/full-config/import")
    @Operation(summary = "导入排产员工作台全部数据包")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:update')")
    public CommonResult<MesProSchedulerWorkbenchFullConfigImportRespVO> importFullConfigPackage(
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(fullConfigPackageService.importPackage(file.getBytes()));
    }

    @GetMapping("/smoke-test/status")
    @Operation(summary = "获得排产员工作台冒烟测试状态")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:query')")
    public CommonResult<MesProSchedulerWorkbenchSmokeTestStatusRespVO> getSmokeTestStatus() {
        return success(smokeTestService.getStatus());
    }

    @PostMapping("/smoke-test/start")
    @Operation(summary = "启动排产员工作台冒烟测试")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:smoke-test')")
    public CommonResult<MesProSchedulerWorkbenchSmokeTestStatusRespVO> startSmokeTest(
            @RequestBody(required = false) MesProSchedulerWorkbenchSmokeTestStartReqVO reqVO) {
        return success(smokeTestService.start(reqVO));
    }

    @PostMapping("/smoke-test/stop")
    @Operation(summary = "结束排产员工作台冒烟测试")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:smoke-test')")
    public CommonResult<MesProSchedulerWorkbenchSmokeTestStatusRespVO> stopSmokeTest() {
        return success(smokeTestService.stop());
    }

}
