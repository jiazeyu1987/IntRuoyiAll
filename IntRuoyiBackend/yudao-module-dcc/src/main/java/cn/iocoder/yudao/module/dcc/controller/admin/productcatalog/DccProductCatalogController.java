package cn.iocoder.yudao.module.dcc.controller.admin.productcatalog;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRegistrationExpiryCompareReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRegistrationExpiryCompareRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogUpdateReqVO;
import cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - DCC 产品目录")
@RestController
@RequestMapping("/dcc/product-catalog")
@Validated
public class DccProductCatalogController {

    @Resource
    private DccProductCatalogService productCatalogService;

    @PostMapping("/create")
    @Operation(summary = "创建 DCC 产品目录")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:create')")
    public CommonResult<DccProductCatalogRespVO> createProductCatalog(
            @Valid @RequestBody DccProductCatalogSaveReqVO reqVO) {
        return success(productCatalogService.createProductCatalog(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 DCC 产品目录")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<Boolean> updateProductCatalog(
            @Valid @RequestBody DccProductCatalogUpdateReqVO reqVO) {
        productCatalogService.updateProductCatalog(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 DCC 产品目录")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:delete')")
    public CommonResult<Boolean> deleteProductCatalog(
            @RequestParam("dataSource") @NotBlank(message = "数据来源不能为空") String dataSource,
            @RequestParam("originalRowNo") @Min(value = 2, message = "原 sheet 行号必须大于表头行") Integer originalRowNo) {
        productCatalogService.deleteProductCatalog(dataSource, originalRowNo);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 DCC 产品目录分页")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query')")
    public CommonResult<PageResult<DccProductCatalogRespVO>> getProductCatalogPage(
            @Valid DccProductCatalogPageReqVO pageReqVO) {
        return success(productCatalogService.getProductCatalogPage(pageReqVO));
    }

    @PostMapping("/registration-expiry/compare")
    @Operation(summary = "比对 DCC 产品目录注册证有效期")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query')")
    public CommonResult<List<DccProductCatalogRegistrationExpiryCompareRespVO>> compareRegistrationExpiry(
            @Valid @RequestBody DccProductCatalogRegistrationExpiryCompareReqVO reqVO) {
        return success(productCatalogService.compareRegistrationExpiry(reqVO));
    }
}
