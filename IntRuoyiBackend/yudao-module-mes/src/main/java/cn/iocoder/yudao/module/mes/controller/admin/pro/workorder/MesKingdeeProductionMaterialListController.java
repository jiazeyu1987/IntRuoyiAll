package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListGroupRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListOrderSyncReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListSyncRespVO;
import cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncResult;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 生产用料清单")
@RestController
@RequestMapping("/erp/production-material-list")
@Validated
public class MesKingdeeProductionMaterialListController {

    @Resource
    private MesKingdeeProductionMaterialListQueryService productionMaterialListQueryService;
    @Resource
    private MesKingdeeProductionMaterialListSyncService productionMaterialListSyncService;
    @Resource
    private ErpKingdeeSyncRuntimeService kingdeeSyncRuntimeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 ERP 生产用料清单")
    @PreAuthorize("@ss.hasPermission('erp:production-material-list:query')")
    public CommonResult<PageResult<MesKingdeeProductionMaterialListRespVO>> getPage(
            @Valid MesKingdeeProductionMaterialListPageReqVO pageReqVO) {
        return success(productionMaterialListQueryService.getPage(pageReqVO));
    }

    @GetMapping("/group-page")
    @Operation(summary = "分页查询 ERP 生产用料清单单据汇总")
    @PreAuthorize("@ss.hasPermission('erp:production-material-list:query')")
    public CommonResult<PageResult<MesKingdeeProductionMaterialListGroupRespVO>> getGroupPage(
            @Valid MesKingdeeProductionMaterialListPageReqVO pageReqVO) {
        return success(productionMaterialListQueryService.getGroupPage(pageReqVO));
    }

    @GetMapping("/detail-list")
    @Operation(summary = "查询 ERP 生产用料清单单据明细")
    @PreAuthorize("@ss.hasPermission('erp:production-material-list:query')")
    public CommonResult<List<MesKingdeeProductionMaterialListDetailRespVO>> getDetailList(
            @RequestParam("sourceBillNo") @NotBlank(message = "单据编号不能为空") String sourceBillNo) {
        return success(productionMaterialListQueryService.getDetailList(sourceBillNo));
    }

    @PostMapping("/sync-kingdee")
    @Operation(summary = "手动同步 ERP 生产用料清单")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-work-order:create', 'mes:pro-schedule-order:create')")
    public CommonResult<MesKingdeeProductionMaterialListSyncRespVO> syncKingdeeProductionMaterialList() {
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<MesKingdeeProductionMaterialListSyncResult> resultReference = new AtomicReference<>();
        kingdeeSyncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.MANUAL)
                .forceInitialWindowStart(true)
                .initialWindowStart(windowEnd.toLocalDate().minusYears(1).atStartOfDay())
                .windowEnd(windowEnd)
                .build(), context -> {
            MesKingdeeProductionMaterialListSyncResult result =
                    productionMaterialListSyncService.syncModifiedBetween(context.getWindowStart(), context.getWindowEnd());
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(windowEnd, result.getCreatedCount(), result.getUpdatedCount(), 0, 0);
        });
        return success(BeanUtils.toBean(resultReference.get(), MesKingdeeProductionMaterialListSyncRespVO.class));
    }

    @PostMapping("/sync-kingdee-by-production-order-nos")
    @Operation(summary = "按生产订单号同步 ERP 生产用料清单")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-work-order:create', 'mes:pro-schedule-order:create')")
    public CommonResult<MesKingdeeProductionMaterialListSyncRespVO> syncKingdeeProductionMaterialListByOrderNos(
            @Valid @RequestBody MesKingdeeProductionMaterialListOrderSyncReqVO reqVO) {
        MesKingdeeProductionMaterialListSyncResult result =
                productionMaterialListSyncService.syncByProductionOrderNos(reqVO.getProductionOrderNos());
        return success(BeanUtils.toBean(result, MesKingdeeProductionMaterialListSyncRespVO.class));
    }

}
