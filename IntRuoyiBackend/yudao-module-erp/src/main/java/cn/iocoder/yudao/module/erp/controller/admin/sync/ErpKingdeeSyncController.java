package cn.iocoder.yudao.module.erp.controller.admin.sync;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeFullSyncReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeFullSyncRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeProductionOrderCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeProductionOrderCreateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeProductionOrderCreateService;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeSyncAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 金蝶同步运行")
@RestController
@RequestMapping("/erp/kingdee-sync")
@Validated
public class ErpKingdeeSyncController {

    @Resource
    private ErpKingdeeSyncAdminService syncAdminService;
    @Resource
    private ErpKingdeeProductionOrderCreateService productionOrderCreateService;

    @GetMapping("/run/page")
    @Operation(summary = "分页查询 ERP 金蝶同步运行记录")
    @PreAuthorize("@ss.hasPermission('erp:kingdee-sync:query')")
    public CommonResult<PageResult<ErpKingdeeSyncRunRespVO>> getRunPage(
            @Valid ErpKingdeeSyncRunPageReqVO pageReqVO) {
        return success(syncAdminService.getRunPage(pageReqVO));
    }

    @GetMapping("/watermark/list")
    @Operation(summary = "查询 ERP 金蝶同步水位")
    @PreAuthorize("@ss.hasPermission('erp:kingdee-sync:query')")
    public CommonResult<List<ErpKingdeeSyncWatermarkRespVO>> getWatermarkList() {
        return success(syncAdminService.getWatermarks());
    }

    @PostMapping("/full-sync")
    @Operation(summary = "全量同步 ERP 金蝶表格")
    @PreAuthorize("@ss.hasPermission('erp:kingdee-sync:query')")
    public CommonResult<ErpKingdeeFullSyncRespVO> runFullSync(
            @Valid @RequestBody ErpKingdeeFullSyncReqVO reqVO) {
        return success(syncAdminService.runFullSync(reqVO.getSyncType()));
    }

    @PostMapping("/production-order/create")
    @Operation(summary = "创建并提交 ERP 金蝶生产工单")
    @PreAuthorize("@ss.hasPermission('erp:kingdee-sync:query')")
    public CommonResult<ErpKingdeeProductionOrderCreateRespVO> createProductionOrder(
            @Valid @RequestBody ErpKingdeeProductionOrderCreateReqVO reqVO) {
        return success(productionOrderCreateService.createProductionOrder(reqVO));
    }
}