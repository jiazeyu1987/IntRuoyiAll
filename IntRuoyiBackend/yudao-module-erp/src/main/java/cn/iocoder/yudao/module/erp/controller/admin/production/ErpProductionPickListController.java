package cn.iocoder.yudao.module.erp.controller.admin.production;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListRespVO;
import cn.iocoder.yudao.module.erp.service.production.kingdee.ErpKingdeeProductionPickListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 生产领料单列表")
@RestController
@RequestMapping("/erp/production-pick-list")
@Validated
public class ErpProductionPickListController {

    @Resource
    private ErpKingdeeProductionPickListService productionPickListService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 ERP 生产领料单列表")
    @PreAuthorize("@ss.hasPermission('erp:production-pick-list:query')")
    public CommonResult<PageResult<ErpProductionPickListRespVO>> getPage(
            @Valid ErpProductionPickListPageReqVO pageReqVO) {
        return success(productionPickListService.getPage(pageReqVO));
    }

}
