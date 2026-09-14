package cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncPlanRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncRunOnceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncTypeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;
import cn.iocoder.yudao.module.erp.service.kingdeeautosync.ErpKingdeeTableAutoSyncService;
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

@Tag(name = "管理后台 - ERP 表格自动同步")
@RestController
@RequestMapping("/erp/kingdee-table-auto-sync")
@Validated
public class ErpKingdeeTableAutoSyncController {

    private static final String PROFILE_CONFIG_PERMISSION = "mes:pro-batch-record-execution:golden-finger";

    @Resource
    private ErpKingdeeTableAutoSyncService tableAutoSyncService;

    @GetMapping("/plan/get")
    @Operation(summary = "获取 ERP 表格自动同步计划")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<ErpKingdeeTableAutoSyncPlanRespVO> getPlan() {
        return success(tableAutoSyncService.getPlan());
    }

    @PutMapping("/plan/save")
    @Operation(summary = "保存 ERP 表格自动同步计划")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<ErpKingdeeTableAutoSyncPlanRespVO> savePlan(
            @Valid @RequestBody ErpKingdeeTableAutoSyncPlanSaveReqVO reqVO) {
        return success(tableAutoSyncService.savePlan(reqVO));
    }

    @GetMapping("/sync-types")
    @Operation(summary = "获取支持同步的 ERP 表格类型")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<List<ErpKingdeeTableAutoSyncTypeRespVO>> getSyncTypes() {
        return success(tableAutoSyncService.getSyncTypes());
    }

    @PostMapping("/plan/run-once")
    @Operation(summary = "立即执行一次 ERP 表格同步")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<ErpKingdeeTableAutoSyncRunOnceRespVO> runOnce() {
        return success(tableAutoSyncService.runOnce());
    }

    @GetMapping("/run/page")
    @Operation(summary = "分页查询 ERP 表格同步运行记录")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<PageResult<ErpKingdeeSyncRunRespVO>> getRunPage(
            @Valid ErpKingdeeSyncRunPageReqVO pageReqVO) {
        return success(tableAutoSyncService.getRunPage(pageReqVO));
    }

    @GetMapping("/watermark/list")
    @Operation(summary = "查询 ERP 表格同步水位")
    @PreAuthorize("@ss.hasPermission('" + PROFILE_CONFIG_PERMISSION + "')")
    public CommonResult<List<ErpKingdeeSyncWatermarkRespVO>> getWatermarkList() {
        return success(tableAutoSyncService.getWatermarks());
    }
}
