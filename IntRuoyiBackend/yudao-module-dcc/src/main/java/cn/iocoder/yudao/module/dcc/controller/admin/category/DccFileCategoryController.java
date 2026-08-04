package cn.iocoder.yudao.module.dcc.controller.admin.category;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDistributionRuleRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDistributionRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccAdminFullConfigPackageImportRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixImportRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixEffectivePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDirectoryBindingSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryPermissionRuleRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixUserLookupRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixEffectivePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixUserLookupRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryTrainingRuleRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryTrainingRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileCategoryImportRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileCategoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileCategorySaveReqVO;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryDistributionRuleAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixSeedService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryViewMatrixAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryPermissionAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryTrainingRuleAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileCategoryAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccAdminFullConfigPackageService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileCategoryPermissionSupport;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 文件类别")
@RestController
@RequestMapping("/dcc/file-categories")
@Validated
public class DccFileCategoryController {

    @Resource
    private DccFileCategoryAdminService categoryAdminService;
    @Resource
    private DccCategoryPermissionAdminService permissionAdminService;
    @Resource
    private DccCategoryDistributionRuleAdminService distributionRuleAdminService;
    @Resource
    private DccCategoryTrainingRuleAdminService trainingRuleAdminService;
    @Resource
    private DccCategoryApprovalMatrixAdminService approvalMatrixAdminService;
    @Resource
    private DccCategoryViewMatrixAdminService viewMatrixAdminService;
    @Resource
    private DccCategoryApprovalMatrixSeedService approvalMatrixSeedService;
    @Resource
    private DccAdminFullConfigPackageService adminFullConfigPackageService;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;

    @GetMapping
    @Operation(summary = "获取文件类别列表")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<List<DccFileCategoryRespVO>> getCategoryList() {
        var bindingMap = categoryAdminService.getCategoryDirectoryBindingMap();
        var categories = categoryAdminService.getCategoryList();
        var matrixPositionMap = approvalMatrixAdminService.getActiveMatrixPositionIdsByCategoryIds(
                categories.stream().map(item -> item.getId()).toList());
        Long loginUserId = getLoginUserId();
        return success(convertList(categories, item -> {
            DccFileCategoryRespVO respVO = BeanUtils.toBean(item, DccFileCategoryRespVO.class);
            var matrixPositionIds = matrixPositionMap.get(item.getId());
            respVO.setDirectoryId(bindingMap.get(item.getId()));
            respVO.setSignoffPositionIds(matrixPositionIds == null
                    ? List.of() : matrixPositionIds.signoffPositionIds());
            respVO.setApprovalPositionIds(matrixPositionIds == null
                    ? List.of() : matrixPositionIds.approvalPositionIds());
            respVO.setCanUpload(loginUserId != null && permissionSupport.hasCategoryPermission(
                    item.getId(), loginUserId, DccFileCategoryPermissionActionEnum.UPLOAD));
            return respVO;
        }));
    }

    @GetMapping("/review-matrix")
    @Operation(summary = "获取审阅矩阵页签列表")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryReviewMatrixRowRespVO>> getReviewMatrixRows(
            @Valid DccCategoryReviewMatrixPageReqVO reqVO) {
        return success(approvalMatrixAdminService.getReviewMatrixRows(reqVO.getCode(), reqVO.getName(),
                reqVO.getActive(), reqVO.getConfigured()));
    }

    @GetMapping("/view-matrix")
    @Operation(summary = "获取查看矩阵页签列表")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryViewMatrixRowRespVO>> getViewMatrixRows(
            @Valid DccCategoryViewMatrixPageReqVO reqVO) {
        return success(viewMatrixAdminService.getViewMatrixRows(reqVO.getCode(), reqVO.getName(),
                reqVO.getActive(), reqVO.getConfigured()));
    }

    @PostMapping("/import-intauth")
    @Operation(summary = "从 IntAuth 一次性导入文件类别")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccFileCategoryImportRespVO> importCategoriesFromIntAuth() {
        var result = categoryAdminService.importCategoriesFromIntAuth();
        DccFileCategoryImportRespVO respVO = new DccFileCategoryImportRespVO();
        respVO.setTotalCount(result.getTotalCount());
        respVO.setCreatedCount(result.getCreatedCount());
        respVO.setAdoptedCount(result.getAdoptedCount());
        respVO.setUpdatedCount(result.getUpdatedCount());
        return success(respVO);
    }

    @PostMapping("/import-intauth-matrix")
    @Operation(summary = "按仓库内置 IntAuth 审批矩阵种子初始化类别审批矩阵")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccCategoryApprovalMatrixImportRespVO> importApprovalMatrixFromSeed() {
        var result = approvalMatrixSeedService.importSeededMatrix();
        DccCategoryApprovalMatrixImportRespVO respVO = new DccCategoryApprovalMatrixImportRespVO();
        respVO.setTotalCount(result.getTotalCount());
        respVO.setSeededCount(result.getSeededCount());
        respVO.setSkippedCount(result.getSkippedCount());
        return success(respVO);
    }

    @GetMapping("/admin-config-package/export")
    @Operation(summary = "导出文控管理员全量配置包")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public void exportAdminConfigPackage(HttpServletResponse response) throws IOException {
        byte[] data = adminFullConfigPackageService.exportPackage();
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("文控管理员全量配置包.json"));
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(data);
    }

    @PostMapping("/admin-config-package/import")
    @Operation(summary = "导入文控管理员全量配置包")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccAdminFullConfigPackageImportRespVO> importAdminConfigPackage(
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(adminFullConfigPackageService.importPackage(file.getBytes()));
    }

    @PostMapping
    @Operation(summary = "创建文件类别")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Long> createCategory(@Valid @RequestBody DccFileCategorySaveReqVO reqVO) {
        return success(categoryAdminService.createCategory(reqVO));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "更新文件类别")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Boolean> updateCategory(@PathVariable("id") Long id,
                                                @Valid @RequestBody DccFileCategorySaveReqVO reqVO) {
        reqVO.setId(id);
        categoryAdminService.updateCategory(reqVO);
        return success(true);
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "删除文件类别")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Boolean> deleteCategory(@PathVariable("id") Long id) {
        categoryAdminService.deleteCategory(id);
        return success(true);
    }

    @PutMapping("/{id:\\d+}/directory-binding")
    @Operation(summary = "更新类别目录绑定")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Boolean> bindDirectory(@PathVariable("id") Long id,
                                               @Valid @RequestBody DccCategoryDirectoryBindingSaveReqVO reqVO) {
        categoryAdminService.bindDirectory(id, reqVO);
        return success(true);
    }

    @GetMapping("/{id:\\d+}/permission-rules")
    @Operation(summary = "获取类别权限规则")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryPermissionRuleRespVO>> getPermissionRules(@PathVariable("id") Long id) {
        return success(convertList(permissionAdminService.getPermissionRules(id),
                item -> BeanUtils.toBean(item, DccCategoryPermissionRuleRespVO.class)));
    }

    @PutMapping("/{id:\\d+}/permission-rules")
    @Operation(summary = "替换类别权限规则")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryPermissionRuleRespVO>> replacePermissionRules(@PathVariable("id") Long id,
                                                                                      @RequestBody List<@Valid DccCategoryPermissionRuleSaveReqVO> reqVOList) {
        return success(convertList(permissionAdminService.replacePermissionRules(id, reqVOList),
                item -> BeanUtils.toBean(item, DccCategoryPermissionRuleRespVO.class)));
    }

    @GetMapping("/{id:\\d+}/distribution-rules")
    @Operation(summary = "获取类别分发规则")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryDistributionRuleRespVO>> getDistributionRules(@PathVariable("id") Long id) {
        return success(convertList(distributionRuleAdminService.getDistributionRules(id),
                item -> BeanUtils.toBean(item, DccCategoryDistributionRuleRespVO.class)));
    }

    @PutMapping("/{id:\\d+}/distribution-rules")
    @Operation(summary = "替换类别分发规则")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryDistributionRuleRespVO>> replaceDistributionRules(@PathVariable("id") Long id,
                                                                                          @RequestBody List<@Valid DccCategoryDistributionRuleSaveReqVO> reqVOList) {
        return success(convertList(distributionRuleAdminService.replaceDistributionRules(id, reqVOList),
                item -> BeanUtils.toBean(item, DccCategoryDistributionRuleRespVO.class)));
    }

    @GetMapping("/{id:\\d+}/training-rules")
    @Operation(summary = "获取类别培训规则")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryTrainingRuleRespVO>> getTrainingRules(@PathVariable("id") Long id) {
        return success(convertList(trainingRuleAdminService.getTrainingRules(id),
                item -> BeanUtils.toBean(item, DccCategoryTrainingRuleRespVO.class)));
    }

    @PutMapping("/{id:\\d+}/training-rules")
    @Operation(summary = "替换类别培训规则")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryTrainingRuleRespVO>> replaceTrainingRules(@PathVariable("id") Long id,
                                                                                  @RequestBody List<@Valid DccCategoryTrainingRuleSaveReqVO> reqVOList) {
        return success(convertList(trainingRuleAdminService.replaceTrainingRules(id, reqVOList),
                item -> BeanUtils.toBean(item, DccCategoryTrainingRuleRespVO.class)));
    }

    @GetMapping("/{id:\\d+}/matrix")
    @Operation(summary = "获取类别审批矩阵")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccCategoryApprovalMatrixRespVO> getApprovalMatrix(@PathVariable("id") Long id) {
        return success(approvalMatrixAdminService.getApprovalMatrix(id));
    }

    @PostMapping("/{id:\\d+}/matrix/effective-preview")
    @Operation(summary = "预览类别审批矩阵最终生效查看权限")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccCategoryReviewMatrixEffectivePreviewRespVO> previewApprovalMatrix(
            @PathVariable("id") Long id,
            @Valid @RequestBody DccCategoryApprovalMatrixSaveReqVO reqVO) {
        return success(approvalMatrixAdminService.previewApprovalMatrix(id, reqVO));
    }

    @PostMapping("/{id:\\d+}/view-matrix/effective-preview")
    @Operation(summary = "预览类别查看矩阵最终生效权限")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccCategoryViewMatrixEffectivePreviewRespVO> previewViewMatrix(
            @PathVariable("id") Long id,
            @Valid @RequestBody DccCategoryViewMatrixSaveReqVO reqVO) {
        return success(viewMatrixAdminService.previewViewMatrix(id, reqVO));
    }

    @GetMapping("/review-matrix/user-lookup")
    @Operation(summary = "按用户反查当前文件类型查看能力")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryReviewMatrixUserLookupRespVO>> getReviewMatrixUserLookup(
            @RequestParam("userId") Long userId) {
        return success(approvalMatrixAdminService.getUserReviewMatrixAccess(userId));
    }

    @GetMapping("/view-matrix/user-lookup")
    @Operation(summary = "按用户反查当前文件类型查看能力")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryViewMatrixUserLookupRespVO>> getViewMatrixUserLookup(
            @RequestParam("userId") Long userId) {
        return success(viewMatrixAdminService.getUserViewMatrixAccess(userId));
    }

    @PutMapping("/{id:\\d+}/view-matrix")
    @Operation(summary = "保存类别查看矩阵")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccCategoryViewMatrixRowRespVO.Rule>> saveViewMatrix(@PathVariable("id") Long id,
                                                                                  @Valid @RequestBody DccCategoryViewMatrixSaveReqVO reqVO) {
        return success(viewMatrixAdminService.saveViewMatrix(id, reqVO));
    }

    @PutMapping("/{id:\\d+}/matrix")
    @Operation(summary = "保存类别审批矩阵")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Long> saveApprovalMatrix(@PathVariable("id") Long id,
                                                 @Valid @RequestBody DccCategoryApprovalMatrixSaveReqVO reqVO) {
        return success(approvalMatrixAdminService.saveApprovalMatrix(id, reqVO).getId());
    }

    @DeleteMapping("/{id:\\d+}/matrix")
    @Operation(summary = "删除类别审批矩阵")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Boolean> deleteApprovalMatrix(@PathVariable("id") Long id) {
        approvalMatrixAdminService.deleteApprovalMatrix(id);
        return success(true);
    }
}
