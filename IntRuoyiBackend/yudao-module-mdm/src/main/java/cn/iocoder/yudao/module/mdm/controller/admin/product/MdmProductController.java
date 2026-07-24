package cn.iocoder.yudao.module.mdm.controller.admin.product;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductImportConfirmReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductExportExcelVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductImportExcelVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductImportPreviewRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductPageReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductReferenceRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductSaveReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductSimpleRespVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductDO;
import cn.iocoder.yudao.module.mdm.service.product.MdmProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 产品主数据")
@RestController
@RequestMapping("/mdm/product")
@Validated
public class MdmProductController {

    @Resource
    private MdmProductService productService;

    @GetMapping("/page")
    @Operation(summary = "获得产品主数据分页")
    @PreAuthorize("@ss.hasPermission('mdm:product:query')")
    public CommonResult<PageResult<MdmProductRespVO>> getProductPage(@Valid MdmProductPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(productService.getProductPage(pageReqVO), MdmProductRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得产品主数据详情")
    @Parameter(name = "id", description = "产品主数据编号", required = true)
    @PreAuthorize("@ss.hasPermission('mdm:product:query')")
    public CommonResult<MdmProductRespVO> getProduct(@RequestParam("id") Long id) {
        MdmProductDO product = productService.getProduct(id);
        return success(product == null ? null : BeanUtils.toBean(product, MdmProductRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "新增产品主数据")
    @PreAuthorize("@ss.hasPermission('mdm:product:create')")
    public CommonResult<Long> createProduct(@Valid @RequestBody MdmProductSaveReqVO reqVO) {
        return success(productService.createProduct(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改产品主数据")
    @PreAuthorize("@ss.hasPermission('mdm:product:update')")
    public CommonResult<Boolean> updateProduct(@Valid @RequestBody MdmProductSaveReqVO reqVO) {
        productService.updateProduct(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "启用或停用产品主数据")
    @PreAuthorize("@ss.hasPermission('mdm:product:update')")
    public CommonResult<Boolean> updateProductStatus(@RequestParam("id") Long id,
                                                     @RequestParam("status") String status) {
        productService.updateProductStatus(id, status);
        return success(true);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得产品主数据精简列表")
    @PreAuthorize("@ss.hasPermission('mdm:product:query')")
    public CommonResult<List<MdmProductSimpleRespVO>> getSimpleProductList(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "requireDccProductCode", required = false) Boolean requireDccProductCode,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(BeanUtils.toBean(productService.listSimpleProducts(status, requireDccProductCode, keyword),
                MdmProductSimpleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出产品主数据")
    @PreAuthorize("@ss.hasPermission('mdm:product:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProductExcel(@Valid MdmProductPageReqVO exportReqVO,
                                   HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MdmProductExportExcelVO> list = productService.getProductPage(exportReqVO).getList().stream()
                .map(MdmProductExportExcelVO::from)
                .toList();
        ExcelUtils.write(response, "产品主数据.xls", "产品主数据", MdmProductExportExcelVO.class, list);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得产品主数据导入模板")
    @PreAuthorize("@ss.hasPermission('mdm:product:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        List<MdmProductImportExcelVO> list = List.of(MdmProductImportExcelVO.builder()
                .productCode("PMD-0001")
                .dccProductCode("A1234567890123")
                .nameCn("示例产品")
                .nameEn("Sample Product")
                .modelSpecification("规格型号")
                .category("产品分类")
                .build());
        ExcelUtils.write(response, "产品主数据导入模板.xls", "产品主数据", MdmProductImportExcelVO.class, list);
    }

    @PostMapping("/import-preview")
    @Operation(summary = "预览产品主数据全量导入")
    @PreAuthorize("@ss.hasPermission('mdm:product:import')")
    public CommonResult<MdmProductImportPreviewRespVO> previewImport(@RequestParam("file") MultipartFile file)
            throws Exception {
        List<MdmProductImportExcelVO> list = ExcelUtils.read(file, MdmProductImportExcelVO.class);
        return success(productService.previewImport(list));
    }

    @PostMapping("/import-confirm")
    @Operation(summary = "确认产品主数据全量导入")
    @PreAuthorize("@ss.hasPermission('mdm:product:import')")
    public CommonResult<MdmProductImportPreviewRespVO> confirmImport(
            @Valid @RequestBody MdmProductImportConfirmReqVO reqVO) {
        return success(productService.confirmImport(reqVO.getBatchId()));
    }

    @GetMapping("/references")
    @Operation(summary = "获得产品主数据引用情况")
    @PreAuthorize("@ss.hasPermission('mdm:product:query')")
    public CommonResult<MdmProductReferenceRespVO> getReferences(@RequestParam("id") Long id) {
        return success(productService.getReferences(id));
    }

}
