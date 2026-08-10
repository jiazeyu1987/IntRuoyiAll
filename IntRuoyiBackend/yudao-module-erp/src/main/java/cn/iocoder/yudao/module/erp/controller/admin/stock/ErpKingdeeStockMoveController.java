package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.move.ErpKingdeeStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.move.ErpKingdeeStockMoveRespVO;
import cn.iocoder.yudao.module.erp.service.stock.kingdee.ErpKingdeeStockMoveListService;
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

@Tag(name = "管理后台 - ERP 金蝶调拨单")
@RestController
@RequestMapping("/erp/kingdee-stock-move")
@Validated
public class ErpKingdeeStockMoveController {

    @Resource
    private ErpKingdeeStockMoveListService stockMoveListService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 ERP 金蝶调拨单")
    @PreAuthorize("@ss.hasPermission('erp:kingdee-stock-move:query')")
    public CommonResult<PageResult<ErpKingdeeStockMoveRespVO>> getPage(
            @Valid ErpKingdeeStockMovePageReqVO pageReqVO) {
        return success(stockMoveListService.getPage(pageReqVO));
    }

}
