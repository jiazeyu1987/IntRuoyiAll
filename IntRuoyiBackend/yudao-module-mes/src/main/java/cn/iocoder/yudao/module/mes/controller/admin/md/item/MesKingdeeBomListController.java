package cn.iocoder.yudao.module.mes.controller.admin.md.item;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee.MesKingdeeBomListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee.MesKingdeeBomListRespVO;
import cn.iocoder.yudao.module.mes.service.md.item.kingdee.MesKingdeeBomListService;
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

@Tag(name = "管理后台 - ERP 物料清单")
@RestController
@RequestMapping("/erp/bom-list")
@Validated
public class MesKingdeeBomListController {

    @Resource
    private MesKingdeeBomListService bomListService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 ERP 物料清单")
    @PreAuthorize("@ss.hasPermission('erp:bom-list:query')")
    public CommonResult<PageResult<MesKingdeeBomListRespVO>> getPage(@Valid MesKingdeeBomListPageReqVO pageReqVO) {
        return success(bomListService.getPage(pageReqVO));
    }

}
