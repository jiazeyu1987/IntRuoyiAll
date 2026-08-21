package cn.iocoder.yudao.module.erp.controller.admin.production;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListSyncRespVO;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.production.kingdee.ErpKingdeeProductionPickListService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListSyncResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 生产领料单列表")
@RestController
@RequestMapping("/erp/production-pick-list")
@Validated
public class ErpProductionPickListController {

    @Resource
    private ErpKingdeeProductionPickListService productionPickListService;
    @Resource
    private ErpKingdeeSyncRuntimeService kingdeeSyncRuntimeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 ERP 生产领料单列表")
    @PreAuthorize("@ss.hasPermission('erp:production-pick-list:query')")
    public CommonResult<PageResult<ErpProductionPickListRespVO>> getPage(
            @Valid ErpProductionPickListPageReqVO pageReqVO) {
        return success(productionPickListService.getPage(pageReqVO));
    }

    @PostMapping("/sync-kingdee")
    @Operation(summary = "同步半年内 ERP 生产领料单")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-work-order:create', 'mes:pro-schedule-order:create')")
    public CommonResult<ErpProductionPickListSyncRespVO> syncKingdeeProductionPickLists() {
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<ErpKingdeeProductionPickListSyncResult> resultReference =
                new AtomicReference<>();
        kingdeeSyncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_PICK_LIST)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.MANUAL)
                .forceInitialWindowStart(true)
                .initialWindowStart(windowEnd.toLocalDate().minusMonths(6).atStartOfDay())
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeeProductionPickListSyncResult result = productionPickListService.syncModifiedBetween(
                    context.getWindowStart(), context.getWindowEnd());
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(),
                    result.getCreatedCount(), result.getUpdatedCount(), 0, 0);
        });
        return success(BeanUtils.toBean(resultReference.get(),
                ErpProductionPickListSyncRespVO.class));
    }

}
