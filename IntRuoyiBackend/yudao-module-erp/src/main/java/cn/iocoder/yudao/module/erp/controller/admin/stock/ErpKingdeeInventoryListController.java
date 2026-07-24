package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.ErpKingdeeInventoryListPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.ErpKingdeeInventoryListRespVO;
import cn.iocoder.yudao.module.erp.service.stock.kingdee.ErpKingdeeInventoryListService;
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

@Tag(name = "管理后台 - ERP 即时库存")
@RestController
@RequestMapping("/erp/inventory-list")
@Validated
public class ErpKingdeeInventoryListController {

    @Resource
    private ErpKingdeeInventoryListService inventoryListService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 ERP 即时库存")
    @PreAuthorize("@ss.hasPermission('erp:inventory-list:query')")
    public CommonResult<PageResult<ErpKingdeeInventoryListRespVO>> getPage(
            @Valid ErpKingdeeInventoryListPageReqVO pageReqVO) {
        return success(inventoryListService.getPage(pageReqVO));
    }

}
