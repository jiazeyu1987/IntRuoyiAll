package cn.iocoder.yudao.module.erp.controller.admin.nastablesync;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncPlanRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunOnceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncTestWriteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncTestWriteRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncTypeRespVO;
import cn.iocoder.yudao.module.erp.service.nastablesync.ErpNasTableSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP NAS 表格自动同步")
@RestController
@RequestMapping("/erp/nas-table-sync")
@Validated
public class ErpNasTableSyncController {

    private static final String PROFILE_CONFIG_PERMISSION = "mes:pro-batch-record-execution:golden-finger";

    @Resource
    private ErpNasTableSyncService nasTableSyncService;

    @GetMapping("/plan/get")
    @Operation(summary = "获取 ERP NAS 表格自动同步计划")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<ErpNasTableSyncPlanRespVO> getPlan() {
        return success(nasTableSyncService.getPlan());
    }

    @PutMapping("/plan/save")
    @Operation(summary = "保存 ERP NAS 表格自动同步计划")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<ErpNasTableSyncPlanRespVO> savePlan(
            @Valid @RequestBody ErpNasTableSyncPlanSaveReqVO reqVO) {
        return success(nasTableSyncService.savePlan(reqVO));
    }

    @GetMapping("/sync-types")
    @Operation(summary = "获取支持导出的 ERP 表类型")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<List<ErpNasTableSyncTypeRespVO>> getSyncTypes() {
        return success(nasTableSyncService.getSyncTypes());
    }

    @PostMapping("/plan/test-nas-write")
    @Operation(summary = "测试 NAS 写入")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<ErpNasTableSyncTestWriteRespVO> testNasWrite(
            @RequestBody(required = false) ErpNasTableSyncTestWriteReqVO reqVO) {
        return success(nasTableSyncService.testNasWrite(reqVO));
    }

    @PostMapping("/plan/run-once")
    @Operation(summary = "立即执行一次 ERP NAS 表格同步")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<ErpNasTableSyncRunOnceRespVO> runOnce() {
        return success(nasTableSyncService.runOnce());
    }

    @GetMapping("/run/page")
    @Operation(summary = "分页查询 ERP NAS 表格同步运行记录")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<PageResult<ErpNasTableSyncRunRespVO>> getRunPage(
            @Valid ErpNasTableSyncRunPageReqVO pageReqVO) {
        return success(nasTableSyncService.getRunPage(pageReqVO));
    }
}