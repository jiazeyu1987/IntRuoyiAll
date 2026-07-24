package cn.iocoder.yudao.module.showroom.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.idev.excel.FastExcelFactory;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductShowroomWorkbookRowDTO;
import cn.iocoder.yudao.module.showroom.configpackage.ShowroomHallConfigPackageService;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCommentAnchorType;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductComment;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductAttachmentPolicy;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomAwardExcelImportExtras;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomKeywordExcelImportExtras;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomKeywordExcelImportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomKeywordExcelRow;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomNarrationExcelImportExtras;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomNarrationExcelImportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomNarrationExcelRow;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomProductExcelImportExtras;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomProductExcelExporter;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomProductResourcePackage;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardDetailRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardDraftReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardPageRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardPublishReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardCoverGenerateRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.hall.ShowroomHallConfigPackageImportRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.ShowroomAwardExcelExportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.product.ShowroomProductExcelVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.product.ShowroomProductImportExtra;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.ShowroomAwardExcelImportRow;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomAssignmentDetail;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomAssignmentSubmitResult;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalDetail;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomAssignmentCreate;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomFieldAssignment;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomVersionAudit;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalActorResolver;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;

@RestController
@RequestMapping("/showroom")
@Validated
public class ShowroomAdminController {

    private static final String PRODUCT_IMPORT_SHEET_NAME = "产品列表";

    public static final String SHOWROOM_PUBLICITY_ROLE_CODE = "showroom_publicity";
    private static final String SHOWROOM_PRODUCT_ACCESS_DENIED =
            "SHOWROOM_PRODUCT_ACCESS_DENIED: 当前用户只能访问指派给自己的产品";
    private static final String SHOWROOM_APPROVAL_ACCESS_DENIED =
            "SHOWROOM_APPROVAL_ACCESS_DENIED: 当前用户只能查看分配给自己的审批";

    private final ShowroomApiRuntime runtime;
    private final ShowroomWorkflowFacade workflowFacade;
    private final ShowroomAssignmentService assignmentService;
    private final ShowroomApprovalActorResolver approvalActorResolver;
    private final ShowroomProductCommentService commentService;
    private final ObjectProvider<MdmProductApi> mdmProductApiProvider;
    private final SecurityFrameworkService securityFrameworkService;
    private final FileService fileService;
    private final ShowroomHallConfigPackageService hallConfigPackageService;

    @Autowired
    public ShowroomAdminController(ShowroomApiRuntime runtime, ShowroomWorkflowFacade workflowFacade,
                                    ShowroomAssignmentService assignmentService,
                                    ShowroomApprovalActorResolver approvalActorResolver,
                                    ShowroomProductCommentService commentService,
                                    ObjectProvider<MdmProductApi> mdmProductApiProvider,
                                    SecurityFrameworkService securityFrameworkService,
                                    FileService fileService,
                                    ShowroomHallConfigPackageService hallConfigPackageService) {
        this.runtime = runtime;
        this.workflowFacade = workflowFacade;
        this.assignmentService = assignmentService;
        this.approvalActorResolver = approvalActorResolver;
        this.commentService = commentService;
        this.mdmProductApiProvider = mdmProductApiProvider;
        this.securityFrameworkService = securityFrameworkService;
        this.fileService = fileService;
        this.hallConfigPackageService = hallConfigPackageService;
    }

    @GetMapping("/company/current")
    public CommonResult<CompanyCurrentRespVO> getCompanyCurrent() {
        return success(runtime.getCompanyCurrent());
    }

    @GetMapping("/company/get")
    public CommonResult<CompanyCurrentRespVO> getCompany(@RequestParam("id") Long id,
                                                         @RequestParam(value = "revisionId", required = false) Long revisionId) {
        return success(runtime.getCompany(id, revisionId));
    }

    @PostMapping("/company/generate-narration-script")
    public CommonResult<CompanyNarrationScriptGenerateRespVO> generateCompanyNarrationScript(
            @RequestBody CompanyNarrationScriptGenerateReqVO reqVO) {
        requireOperatorUserId();
        return success(runtime.generateCompanyNarrationScript(reqVO));
    }

    @PostMapping("/company/translate-fields-to-en")
    public CommonResult<CompanyFieldTranslateRespVO> translateCompanyFieldsToEn(
            @RequestBody CompanyFieldTranslateReqVO reqVO) {
        requireOperatorUserId();
        return success(runtime.translateCompanyFieldsToEn(reqVO));
    }

    @PostMapping("/company/generate-narration-audio")
    public CommonResult<CompanyNarrationGenerateRespVO> generateCompanyNarrationAudio(
            @RequestBody CompanyNarrationGenerateReqVO reqVO) {
        requireOperatorUserId();
        return success(runtime.generateCompanyNarrationAudio(reqVO));
    }

    @PostMapping("/company/publish-narration")
    public CommonResult<CompanyNarrationPublishRespVO> publishCompanyNarration(
            @RequestBody CompanyNarrationPublishReqVO reqVO) {
        requireOperatorUserId();
        return success(runtime.publishCompanyNarration(reqVO));
    }

    @PutMapping("/company/publish")
    public CommonResult<CompanyCurrentRespVO> publishCompany(@RequestBody CompanyDraftReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        return success(runtime.publishCompany(reqVO, operatorUserId));
    }

    @PostMapping("/company/restore")
    public CommonResult<CompanyCurrentRespVO> restoreCompanyRevision(@RequestBody CompanyRevisionRestoreReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        return success(runtime.restoreCompanyRevision(reqVO, operatorUserId));
    }

    @PutMapping("/company/draft")
    public CommonResult<ShowroomCompanyRevision> saveCompanyDraft(@RequestBody CompanyDraftReqVO reqVO) {
        requirePublicityRole();
        return success(runtime.saveCompanyDraft(reqVO));
    }

    @PostMapping("/company/submit")
    public CommonResult<ShowroomChangeRequest> submitCompany(@RequestBody SubmitReqVO reqVO) {
        requirePublicityRole();
        Long publicityApproverUserId = approvalActorResolver.resolvePublicityApproverUserId();
        return success(workflowFacade.submit("COMPANY", reqVO.targetId(), reqVO.targetRevisionId(),
                reqVO.fieldCodes(), reqVO.moduleCode(), reqVO.submittedBy(), reqVO.submitterDeptId(),
                reqVO.supervisorUserId(), publicityApproverUserId));
    }

    @PostMapping("/release/publish")
    public CommonResult<ReleasePublishRespVO> publishRelease(@RequestBody ReleasePublishReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        if (!isShowroomPublicity(operatorUserId)) {
            throw exception0(FORBIDDEN.getCode(), "当前用户无权执行发布展厅release");
        }
        return success(runtime.publishRelease(reqVO, operatorUserId));
    }

    public CommonResult<ReleasePublishRespVO> publishRelease() {
        throw new IllegalStateException("SHOWROOM_SITE_SELECTOR_REQUIRED: siteKey and stage are required");
    }

    @GetMapping("/company/history")
    public CommonResult<List<VersionHistoryRespVO>> getCompanyHistory(@RequestParam("id") Long id) {
        return success(runtime.versionHistory("COMPANY", id));
    }

    @GetMapping("/prompt/current")
    public CommonResult<ImagePromptCurrentRespVO> getImagePromptCurrent(@RequestParam("sceneCode") String sceneCode) {
        requirePublicityRole("查看提示管理");
        return success(runtime.getImagePromptCurrent(sceneCode));
    }

    @GetMapping("/prompt/history")
    public CommonResult<List<ImagePromptHistoryItemRespVO>> getImagePromptHistory(
            @RequestParam("sceneCode") String sceneCode) {
        requirePublicityRole("查看提示管理");
        return success(runtime.getImagePromptHistory(sceneCode));
    }

    @PostMapping("/prompt/version")
    public CommonResult<ImagePromptCurrentRespVO> saveImagePromptVersion(
            @Validated @RequestBody ImagePromptVersionSaveReqVO reqVO) {
        requirePublicityRole("保存提示词版本");
        return success(runtime.saveImagePromptVersion(reqVO));
    }

    private Set<Long> resolveVisibleProductScope(Long operatorUserId) {
        LinkedHashSet<Long> visibleProductIds = new LinkedHashSet<>(assignmentService.listVisibleProductIdsForUser(operatorUserId));
        workflowFacade.listPendingApprovalsForReviewer(operatorUserId).forEach(request -> {
            if (Objects.equals("PRODUCT", request.targetType()) && request.targetId() != null) {
                visibleProductIds.add(request.targetId());
            }
        });
        return visibleProductIds;
    }

    private Set<Long> resolveEditableProductScope(Long operatorUserId) {
        return new LinkedHashSet<>(assignmentService.listOpenProductIdsForAssignee(operatorUserId));
    }

    private void requireProductVisibleAccess(Long operatorUserId, Long productId) {
        if (isShowroomPublicity(operatorUserId)) {
            return;
        }
        if (productId == null) {
            throw new IllegalStateException(SHOWROOM_PRODUCT_ACCESS_DENIED);
        }
        if (assignmentService.hasVisibleProductAccess(operatorUserId, productId)) {
            return;
        }
        boolean pendingApprovalAccess = workflowFacade.listPendingApprovalsForReviewer(operatorUserId).stream()
                .anyMatch(request -> Objects.equals("PRODUCT", request.targetType())
                        && Objects.equals(productId, request.targetId()));
        if (!pendingApprovalAccess) {
            throw new IllegalStateException(SHOWROOM_PRODUCT_ACCESS_DENIED);
        }
    }

    private void requireProductEditAccess(Long operatorUserId, Long productId) {
        if (isShowroomPublicity(operatorUserId)) {
            return;
        }
        if (productId == null || !assignmentService.hasOpenProductAssignment(operatorUserId, productId)) {
            throw new IllegalStateException(
                    SHOWROOM_PRODUCT_ACCESS_DENIED + "，当前用户无权编辑该产品");
        }
    }

    private void requireProductManageAccess(Long operatorUserId, String actionLabel) {
        if (!isShowroomPublicity(operatorUserId)) {
            throw new IllegalStateException(
                    SHOWROOM_PRODUCT_ACCESS_DENIED + "，当前用户无权执行" + actionLabel);
        }
    }

    private boolean isShowroomPublicity(Long operatorUserId) {
        return operatorUserId != null && (securityFrameworkService.hasRole(SHOWROOM_PUBLICITY_ROLE_CODE)
                || securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode()));
    }

    private MdmProductApi requireMdmProductApi() {
        MdmProductApi productApi = mdmProductApiProvider.getIfAvailable();
        if (productApi == null) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: mdm product api is required");
        }
        return productApi;
    }

    @GetMapping("/product/page")
    public CommonResult<PageResult<ProductPageRespVO>> getProductPage(PageQueryReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        if (isShowroomPublicity(operatorUserId)) {
            return success(runtime.listProducts(reqVO));
        }
        return success(runtime.listProducts(reqVO, resolveVisibleProductScope(operatorUserId),
                resolveEditableProductScope(operatorUserId)));
    }

    @GetMapping("/award/page")
    public CommonResult<PageResult<AwardPageRespVO>> getAwardPage(PageQueryReqVO reqVO) {
        requireOperatorUserId();
        return success(runtime.listAwards(reqVO));
    }

    @GetMapping("/award/get")
    public CommonResult<AwardDetailRespVO> getAward(@RequestParam("id") Long id,
                                                    @RequestParam(value = "revisionId", required = false) Long revisionId) {
        Long operatorUserId = requireOperatorUserId();
        return success(runtime.getAwardDetail(id, revisionId, isShowroomPublicity(operatorUserId)));
    }

    @PutMapping("/award/draft")
    public CommonResult<ShowroomAwardRevision> saveAwardDraft(@RequestBody AwardDraftReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "保存奖项草稿");
        return success(runtime.saveAwardDraft(reqVO));
    }

    @PutMapping("/award/publish")
    public CommonResult<AwardDetailRespVO> publishAward(@RequestBody AwardPublishReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "发布奖项");
        return success(runtime.publishAward(reqVO, operatorUserId));
    }

    @PostMapping("/award/generate-cover-image")
    public CommonResult<AwardCoverGenerateRespVO> generateAwardCoverImage(
            @RequestBody AwardCoverGenerateReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "奖项生图");
        return success(runtime.generateAwardCoverImage(reqVO, operatorUserId));
    }

    @DeleteMapping("/award/delete")
    public CommonResult<Boolean> deleteAward(@RequestParam("id") Long id) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "删除奖项");
        runtime.deleteAward(id);
        return success(true);
    }

    @GetMapping("/product/export-excel")
    public void exportProductExcel(PageQueryReqVO reqVO, HttpServletResponse response) throws IOException {
        Long operatorUserId = requireOperatorUserId();
        List<ShowroomProductExcelVO> rows = isShowroomPublicity(operatorUserId)
                ? runtime.listProductExcelRows(reqVO)
                : runtime.listProductExcelRows(reqVO, resolveVisibleProductScope(operatorUserId),
                resolveEditableProductScope(operatorUserId));
        List<ShowroomAwardExcelExportRow> awardRows = runtime.listAwardExcelRows();
        List<ShowroomNarrationExcelRow> narrationRows = runtime.listNarrationExcelRows(reqVO, rows);
        List<ShowroomKeywordExcelRow> keywordRows = runtime.listKeywordExcelRows();
        List<MdmProductShowroomWorkbookRowDTO> productMasterRows = requireMdmProductApi()
                .exportForShowroomWorkbook(rows.stream().map(ShowroomProductExcelVO::getProductCode).toList());
        byte[] content = ShowroomProductResourcePackage.build("产品列表", rows, productMasterRows, awardRows, narrationRows,
                keywordRows, fileService);
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("showroom-product-resource-package.zip"));
        response.setContentType("application/zip");
        response.getOutputStream().write(content);
    }

    @GetMapping("/product/get-import-template")
    public void getProductImportTemplate(HttpServletResponse response) throws IOException {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "下载产品导入模板");
        ShowroomProductExcelExporter.writeTemplate(response, "产品资料修改版-补充产品资料.xlsx", "产品列表",
                runtime.buildProductImportTemplateRows(), runtime.listKeywordExcelRows());
    }

    @PostMapping("/product/create")
    public CommonResult<ShowroomProductRevision> createProduct(@RequestBody ProductDraftReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "新建产品");
        return success(runtime.saveProductDraft(reqVO));
    }

    @PostMapping("/product/attachment/upload")
    public CommonResult<ProductAttachmentUploadRespVO> uploadProductAttachment(
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam("assetType") String assetType,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long operatorUserId = requireOperatorUserId();
        if (productId == null) {
            requireProductManageAccess(operatorUserId, "上传产品附件");
        } else {
            requireProductEditAccess(operatorUserId, productId);
        }
        String originalName = file.getOriginalFilename();
        String mimeType = file.getContentType();
        byte[] content = file.getBytes();
        String normalizedAssetType = ShowroomProductAttachmentPolicy.validateUpload(assetType, originalName,
                mimeType, content.length);
        Long fileId = fileService.createFileAndReturnId(content, originalName, "showroom/product-attachments",
                mimeType);
        FileDO saved = fileService.getFile(fileId);
        if (saved == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: uploaded attachment file not found: "
                    + fileId);
        }
        return success(new ProductAttachmentUploadRespVO(fileId, buildAdminFileAccessUrl(saved), originalName,
                mimeType, (long) content.length, normalizedAssetType));
    }

    @GetMapping("/product/get")
    public CommonResult<ProductDetailRespVO> getProduct(@RequestParam("id") Long id,
                                                       @RequestParam(value = "revisionId", required = false) Long revisionId) {
        Long operatorUserId = requireOperatorUserId();
        requireProductVisibleAccess(operatorUserId, id);
        return success(runtime.getProductDetail(id, revisionId, isShowroomPublicity(operatorUserId)
                || assignmentService.hasOpenProductAssignment(operatorUserId, id)));
    }

    @PutMapping("/product/draft")
    public CommonResult<ShowroomProductRevision> saveProductDraft(@RequestBody ProductDraftReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        if (reqVO.productId() == null) {
            requireProductManageAccess(operatorUserId, "新建产品草稿");
        } else {
            requireProductEditAccess(operatorUserId, reqVO.productId());
        }
        return success(runtime.saveProductDraft(reqVO));
    }

    @PostMapping("/product/import-excel")
    public CommonResult<ShowroomProductImportRespVO> importProductExcel(@RequestParam("file") MultipartFile file,
                                                                        @RequestParam("sameProductAction") String sameProductAction)
            throws Exception {
        return importProductExcel(file, sameProductAction, ShowroomProductImportMode.STANDARD, "导入产品 Excel");
    }

    @PostMapping("/product/import-base-workbook")
    public CommonResult<ShowroomProductImportRespVO> importProductBaseWorkbook(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sameProductAction") String sameProductAction) throws Exception {
        return importProductExcel(file, sameProductAction, ShowroomProductImportMode.BASE_WORKBOOK,
                "导入无产品图底表");
    }

    private CommonResult<ShowroomProductImportRespVO> importProductExcel(MultipartFile file,
                                                                         String sameProductAction,
                                                                         ShowroomProductImportMode importMode,
                                                                         String actionLabel)
            throws Exception {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, actionLabel);
        ShowroomProductImportSameAction resolvedSameProductAction =
                ShowroomProductImportSameAction.fromRequest(sameProductAction);
        byte[] content = file.getBytes();
        ShowroomProductResourcePackage.ParsedPackage parsedPackage = null;
        if (ShowroomProductImportMode.STANDARD.equals(importMode) && isZipFile(file, content)) {
            parsedPackage = ShowroomProductResourcePackage.parse(content);
            content = parsedPackage.workbookBytes();
        }
        List<ShowroomProductExcelVO> rows = FastExcelFactory.read(new ByteArrayInputStream(content),
                        ShowroomProductExcelVO.class, null)
                .autoCloseStream(false)
                .sheet(PRODUCT_IMPORT_SHEET_NAME)
                .doReadSync();
        List<MdmProductShowroomWorkbookRowDTO> productMasterRows = FastExcelFactory.read(
                        new ByteArrayInputStream(content), MdmProductShowroomWorkbookRowDTO.class, null)
                .autoCloseStream(false)
                .sheet("产品主数据")
                .doReadSync();
        Map<String, Long> productMasterIdsByCode = requireMdmProductApi().importFromShowroomWorkbook(productMasterRows);
        Map<Integer, ShowroomProductImportExtra> extrasByRowNo = ShowroomProductExcelImportExtras.read(content);
        List<ShowroomAwardExcelImportRow> awardRows = ShowroomAwardExcelImportExtras.read(content, importMode);
        List<ShowroomNarrationExcelImportRow> narrationRows = ShowroomNarrationExcelImportExtras.read(content,
                importMode);
        if (parsedPackage != null) {
            ShowroomProductResourcePackage.validateImportProductNarrationAlignment(rows, narrationRows, parsedPackage);
            narrationRows = ShowroomProductResourcePackage.applyPackageAudioAssets(narrationRows, parsedPackage);
        }
        List<ShowroomKeywordExcelImportRow> keywordRows = ShowroomKeywordExcelImportExtras.read(content,
                importMode);
        return success(runtime.importProductExcel(rows, extrasByRowNo, awardRows, narrationRows, keywordRows,
                productMasterIdsByCode,
                operatorUserId, resolvedSameProductAction, importMode, parsedPackage != null));
    }

    private static boolean isZipFile(MultipartFile file, byte[] content) {
        String filename = file == null ? "" : String.valueOf(file.getOriginalFilename()).toLowerCase();
        String contentType = file == null ? "" : String.valueOf(file.getContentType()).toLowerCase();
        return filename.endsWith(".zip") || "application/zip".equals(contentType)
                || "application/x-zip-compressed".equals(contentType);
    }

    @PutMapping("/product/publish")
    public CommonResult<ProductDetailRespVO> publishProduct(@RequestBody ProductPublishReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "保存并发布产品");
        return success(runtime.publishProduct(reqVO, operatorUserId));
    }

    @PostMapping("/product/batch-publish")
    public CommonResult<ProductBatchGenerateRespVO> batchPublishProducts(
            @RequestBody ProductBatchGenerateReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requirePublicityRole("批量发布产品");
        return success(runtime.batchPublishProducts(reqVO, operatorUserId));
    }

    @PostMapping("/product/batch-generate-sales-countries")
    public CommonResult<ProductSalesCountryBatchGenerateRespVO> batchGenerateProductSalesCountries(
            @RequestBody ProductBatchGenerateReqVO reqVO) {
        requireOperatorUserId();
        requirePublicityRole("批量补齐产品在售国家");
        return success(runtime.batchGenerateProductSalesCountries(reqVO));
    }

    @DeleteMapping("/product/delete")
    public CommonResult<Boolean> deleteProduct(@RequestParam("id") Long id) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "删除产品");
        runtime.deleteProduct(id);
        return success(true);
    }

    @PostMapping("/product/submit")
    public CommonResult<ShowroomChangeRequest> submitProduct(@RequestBody SubmitReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requireProductEditAccess(operatorUserId, reqVO.targetId());
        Long publicityApproverUserId = approvalActorResolver.resolvePublicityApproverUserId();
        return success(workflowFacade.submit("PRODUCT", reqVO.targetId(), reqVO.targetRevisionId(),
                reqVO.fieldCodes(), reqVO.moduleCode(), reqVO.submittedBy(), reqVO.submitterDeptId(),
                reqVO.supervisorUserId(), publicityApproverUserId));
    }

    @GetMapping("/product/history")
    public CommonResult<List<VersionHistoryRespVO>> getProductHistory(@RequestParam("id") Long id) {
        Long operatorUserId = requireOperatorUserId();
        requireProductVisibleAccess(operatorUserId, id);
        return success(runtime.versionHistory("PRODUCT", id));
    }

    @PostMapping("/product/translate-fields-to-en")
    public CommonResult<ProductFieldTranslateRespVO> translateProductFieldsToEn(
            @RequestBody ProductFieldTranslateReqVO reqVO) {
        requireOperatorUserId();
        return success(runtime.translateProductFieldsToEn(reqVO));
    }

    @PostMapping("/product/generate-narration-script")
    public CommonResult<ShowroomNarrationVersion> generateProductNarrationScript(
            @RequestBody ProductNarrationGenerateReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "生成讲解稿");
        return success(runtime.generateProductNarrationScript(reqVO.productId()));
    }

    @PostMapping("/product/generate-narration-audio")
    public CommonResult<ProductNarrationGenerateRespVO> generateProductNarrationAudio(
            @RequestBody ProductNarrationGenerateReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "生成语音");
        return success(runtime.generateProductNarrationAudio(reqVO.productId(), reqVO.sourceRevisionId()));
    }

    @PostMapping("/product/generate-cover-image")
    public CommonResult<ProductCoverGenerateRespVO> generateProductCoverImage(
            @RequestBody ProductCoverGenerateReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "生成AI封面");
        return success(runtime.generateProductCoverImage(reqVO, operatorUserId));
    }

    @PostMapping("/product/batch-generate-narration-audio")
    public CommonResult<ProductBatchGenerateRespVO> batchGenerateProductNarrationAudio(
            @RequestBody ProductBatchGenerateReqVO reqVO) {
        requireOperatorUserId();
        requirePublicityRole("批量生成产品语音");
        return success(runtime.batchGenerateProductNarrationAudio(reqVO));
    }

    @GetMapping("/product/batch-generate-narration-audio-state")
    public CommonResult<ProductBatchGenerateStateRespVO> getProductBatchGenerateNarrationAudioState() {
        requireOperatorUserId();
        requirePublicityRole("查看产品语音自动检查状态");
        return success(runtime.getProductBatchGenerateNarrationAudioState());
    }

    @PostMapping("/hall/generate-narration-audio")
    public CommonResult<HallNarrationGenerateRespVO> generateHallNarrationAudio(
            @RequestBody HallNarrationGenerateReqVO reqVO) {
        requireOperatorUserId();
        requirePublicityRole("\u751f\u6210\u5c55\u67dc\u8bed\u97f3");
        return success(runtime.generateHallNarrationAudio(reqVO));
    }

    @PostMapping("/hall/batch-generate-narration-audio")
    public CommonResult<HallNarrationBatchGenerateRespVO> batchGenerateHallNarrationAudio() {
        requireOperatorUserId();
        requirePublicityRole("\u4e00\u952e\u751f\u6210\u5c55\u67dc\u8bed\u97f3");
        return success(runtime.batchGenerateHallNarrationAudio());
    }

    @PostMapping("/product/batch-generate-narration-script/start")
    public CommonResult<ProductNarrationScriptBatchTaskRespVO> startBatchGenerateNarrationScript(
            @RequestBody ProductBatchGenerateReqVO reqVO) {
        requireOperatorUserId();
        requirePublicityRole("批量生成产品讲解稿");
        return success(runtime.startBatchGenerateNarrationScript(reqVO));
    }

    @GetMapping("/product/batch-generate-narration-script/status")
    public CommonResult<ProductNarrationScriptBatchTaskRespVO> getProductBatchGenerateNarrationScriptStatus() {
        requireOperatorUserId();
        requirePublicityRole("查看产品讲解任务状态");
        return success(runtime.getProductBatchGenerateNarrationScriptStatus());
    }

    @PostMapping("/product/batch-generate-cover-image")
    public CommonResult<ProductBatchGenerateRespVO> batchGenerateProductCoverImage(
            @RequestBody ProductBatchGenerateReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requirePublicityRole("批量生成产品封面");
        return success(runtime.batchGenerateProductCoverImage(reqVO, operatorUserId));
    }

    @GetMapping("/product/batch-generate-cover-image-state")
    public CommonResult<ProductCoverBatchTaskStateRespVO> getProductBatchGenerateCoverImageState() {
        requireOperatorUserId();
        requirePublicityRole("查看产品封面任务状态");
        return success(runtime.getProductBatchGenerateCoverImageState());
    }

    @PostMapping("/product/batch-translate-publish/start")
    public CommonResult<ProductTranslatePublishBatchTaskRespVO> startBatchTranslatePublishProducts(
            @RequestBody ProductBatchGenerateReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requirePublicityRole("批量翻译并发布产品");
        return success(runtime.startBatchTranslatePublishProducts(reqVO, operatorUserId));
    }

    @GetMapping("/product/batch-translate-publish/status")
    public CommonResult<ProductTranslatePublishBatchTaskRespVO> getProductBatchTranslatePublishStatus() {
        requireOperatorUserId();
        requirePublicityRole("查看产品翻译发布任务状态");
        return success(runtime.getProductBatchTranslatePublishStatus());
    }

    @GetMapping("/hall/page")
    public CommonResult<List<HallPageRespVO>> getHallPage(PageQueryReqVO reqVO) {
        return success(runtime.listHalls(reqVO));
    }

    @PostMapping("/hall/create")
    public CommonResult<ShowroomHall> createHall(@RequestBody HallSaveReqVO reqVO) {
        return success(runtime.createHall(reqVO));
    }

    @PutMapping("/hall/update")
    public CommonResult<ShowroomHall> updateHall(@RequestBody HallUpdateReqVO reqVO) {
        return success(runtime.updateHall(reqVO));
    }

    @DeleteMapping("/hall/delete")
    public CommonResult<Boolean> deleteHall(@RequestParam("id") Long id) {
        runtime.deleteHall(id);
        return success(true);
    }

    @PutMapping("/hall/update-product-mapping")
    public CommonResult<ShowroomHall> updateHallProductMapping(@RequestBody HallMappingReqVO reqVO) {
        return success(runtime.updateHallProductMapping(reqVO));
    }

    @PutMapping("/hall/update-canvas-layout")
    public CommonResult<ShowroomHall> updateHallCanvasLayout(@RequestBody HallMappingReqVO reqVO) {
        return success(runtime.updateHallCanvasLayout(reqVO));
    }

    @PutMapping("/hall/update-item-mapping")
    public CommonResult<ShowroomHall> updateHallItemMapping(@RequestBody HallItemMappingReqVO reqVO) {
        return success(runtime.updateHallItemMapping(reqVO));
    }

    @PutMapping("/hall/update-item-canvas-layout")
    public CommonResult<ShowroomHall> updateHallItemCanvasLayout(@RequestBody HallItemMappingReqVO reqVO) {
        return success(runtime.updateHallItemCanvasLayout(reqVO));
    }

    @PostMapping("/hall/calculate-bu-canvas-layout")
    public CommonResult<HallItemMappingReqVO> calculateHallBuCanvasLayout(@RequestBody HallItemMappingReqVO reqVO) {
        return success(runtime.calculateHallBuCanvasLayout(reqVO));
    }

    @PutMapping("/hall/update-canvas-background")
    public CommonResult<ShowroomHall> updateHallCanvasBackground(@RequestBody HallCanvasBackgroundReqVO reqVO) {
        return success(runtime.updateHallCanvasBackground(reqVO));
    }

    @PostMapping("/hall/publish-preview-asset")
    public CommonResult<HallPreviewAssetPublishRespVO> publishHallPreviewAsset(
            @RequestBody HallPreviewAssetPublishReqVO reqVO) {
        requireOperatorUserId();
        requirePublicityRole("发布展柜预览图");
        return success(runtime.publishHallPreviewAsset(reqVO));
    }

    @GetMapping("/hall/config-package/export")
    public void exportHallConfigPackage(HttpServletResponse response) throws IOException {
        requireOperatorUserId();
        requirePublicityRole("导出展柜配置包");
        byte[] content = hallConfigPackageService.exportPackage();
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("showroom-hall-config-package.zip"));
        response.setContentType("application/zip");
        response.getOutputStream().write(content);
    }

    @PostMapping("/hall/config-package/import")
    public CommonResult<ShowroomHallConfigPackageImportRespVO> importHallConfigPackage(
            @RequestParam("file") MultipartFile file) throws IOException {
        requireOperatorUserId();
        requirePublicityRole("导入展柜配置包");
        return success(hallConfigPackageService.importPackage(file.getBytes()));
    }

    @GetMapping("/hall/product-options")
    public CommonResult<List<HallProductOptionRespVO>> getHallProductOptions() {
        return success(runtime.listHallProductOptions());
    }

    @GetMapping("/hall/item-options")
    public CommonResult<List<HallItemOptionRespVO>> getHallItemOptions() {
        return success(runtime.listHallItemOptions());
    }

    @GetMapping("/approval/page")
    public CommonResult<List<ShowroomChangeRequest>> getApprovalPage() {
        Long reviewerUserId = requireOperatorUserId();
        return success(workflowFacade.listPendingApprovalsForReviewer(reviewerUserId));
    }

    @GetMapping("/approval/get")
    public CommonResult<ShowroomApprovalDetail> getApproval(@RequestParam("id") Long id) {
        Long participantUserId = requireOperatorUserId();
        return success(workflowFacade.getApprovalForParticipant(id, participantUserId));
    }

    @PostMapping("/approval/supervisor-approve")
    public CommonResult<ShowroomChangeRequest> supervisorApprove(@RequestBody ApprovalActionReqVO reqVO) {
        return success(workflowFacade.supervisorApprove(
                reqVO.id(), reqVO.reviewerUserId(), reqVO.password(), reqVO.comment()));
    }

    @PostMapping("/approval/supervisor-reject")
    public CommonResult<ShowroomChangeRequest> supervisorReject(@RequestBody ApprovalRejectReqVO reqVO) {
        return success(workflowFacade.supervisorReject(
                reqVO.id(), reqVO.reviewerUserId(), reqVO.password(), reqVO.reason()));
    }

    @PostMapping("/approval/gaoxin-approve")
    public CommonResult<ShowroomChangeRequest> gaoxinApprove(@RequestBody ApprovalActionReqVO reqVO) {
        return success(workflowFacade.gaoxinApproveAndPublish(
                reqVO.id(), reqVO.reviewerUserId(), reqVO.password(), reqVO.comment()));
    }

    @PostMapping("/approval/gaoxin-reject")
    public CommonResult<ShowroomChangeRequest> gaoxinReject(@RequestBody ApprovalRejectReqVO reqVO) {
        return success(workflowFacade.gaoxinReject(
                reqVO.id(), reqVO.reviewerUserId(), reqVO.password(), reqVO.reason()));
    }

    @PostMapping("/assignment/create")
    public CommonResult<ShowroomFieldAssignment> createAssignment(@RequestBody AssignmentCreateReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        requireProductManageAccess(operatorUserId, "创建产品指派");
        return success(assignmentService.createAssignment(new ShowroomAssignmentCreate(reqVO.targetType(),
                reqVO.targetId(), reqVO.fieldCode(), reqVO.assigneeUserId(), operatorUserId, null)));
    }

    @GetMapping("/assignment/get")
    public CommonResult<ShowroomAssignmentDetail> getAssignment(@RequestParam("id") Long id) {
        return success(assignmentService.getAssignment(id));
    }

    @GetMapping("/assignment/page")
    public CommonResult<List<ShowroomAssignmentDetail>> getAssignmentPage(AssignmentPageReqVO reqVO) {
        return success(assignmentService.pageAssignments(reqVO.targetType(), reqVO.targetId(),
                reqVO.assigneeUserId(), reqVO.status(), reqVO.pageNo(), reqVO.pageSize()));
    }

    @PostMapping("/assignment/complete-and-submit")
    public CommonResult<ShowroomAssignmentSubmitResult> completeAndSubmitAssignment(
            @RequestBody AssignmentCompleteReqVO reqVO) {
        return success(assignmentService.completeAndSubmit(reqVO.assignmentId(), reqVO.fieldValue(),
                reqVO.operatorUserId(), reqVO.gaoxinUserId()));
    }

    @PostMapping("/product-comment/create")
    public CommonResult<ShowroomProductComment> createProductComment(@RequestBody ProductCommentCreateReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        return success(commentService.createThreadVisible(reqVO.productId(), reqVO.targetRevisionId(),
                reqVO.changeRequestId(), ShowroomCommentAnchorType.valueOf(reqVO.anchorType()),
                reqVO.anchorKey(), operatorUserId, reqVO.content()));
    }

    @GetMapping("/product-comment/page")
    public CommonResult<List<ShowroomProductComment>> getProductCommentPage(ProductCommentPageReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        ShowroomCommentAnchorType anchorType = reqVO.anchorType() == null ? null
                : ShowroomCommentAnchorType.valueOf(reqVO.anchorType());
        return success(commentService.pageByProductVisible(reqVO.productId(), anchorType, reqVO.anchorKey(),
                reqVO.changeRequestId(), reqVO.status(), operatorUserId));
    }

    @PostMapping("/product-comment/reply")
    public CommonResult<ShowroomProductComment> replyProductComment(@RequestBody ProductCommentReplyReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        return success(commentService.replyVisible(reqVO.commentId(), operatorUserId, reqVO.content()));
    }

    @PostMapping("/product-comment/resolve")
    public CommonResult<ShowroomProductComment> resolveProductComment(@RequestBody ProductCommentResolveReqVO reqVO) {
        Long operatorUserId = requireOperatorUserId();
        return success(commentService.resolveVisible(reqVO.commentId(), operatorUserId));
    }

    @PostMapping("/narration/generate-script")
    public CommonResult<ShowroomNarrationVersion> generateNarrationScript(@RequestBody NarrationDraftReqVO reqVO) {
        return success(runtime.saveNarrationDraft(reqVO));
    }

    @GetMapping("/narration/get")
    public CommonResult<NarrationVersionRespVO> getNarration(@RequestParam("targetType") String targetType,
                                                             @RequestParam("targetId") Long targetId,
                                                             @RequestParam("audienceType") String audienceType,
                                                             @RequestParam("language") String language) {
        return success(runtime.getNarration(targetType, targetId, audienceType, language));
    }

    @PostMapping("/narration/generate-audio")
    public CommonResult<ShowroomNarrationVersion> generateNarrationAudio(@RequestBody NarrationAudioGenerateReqVO reqVO) {
        return success(runtime.generateNarrationAudio(reqVO));
    }

    @GetMapping("/narration/tts-defaults")
    public CommonResult<NarrationTtsDefaultsRespVO> getNarrationTtsDefaults() {
        return success(runtime.getNarrationTtsDefaults());
    }

    @PutMapping("/narration/tts-default-voice")
    public CommonResult<Boolean> saveNarrationTtsDefaultVoice(@RequestBody NarrationTtsDefaultVoiceReqVO reqVO) {
        return success(runtime.saveNarrationTtsDefaultVoice(reqVO.voice()));
    }

    @PutMapping("/narration/tts-default-token")
    public CommonResult<Boolean> saveNarrationTtsDefaultToken(@RequestBody NarrationTtsDefaultTokenReqVO reqVO) {
        return success(runtime.saveNarrationTtsDefaultToken(reqVO.accessToken()));
    }

    @PutMapping("/narration/tts-default-appkey")
    public CommonResult<Boolean> saveNarrationTtsDefaultAppKey(@RequestBody NarrationTtsDefaultAppKeyReqVO reqVO) {
        return success(runtime.saveNarrationTtsDefaultAppKey(reqVO.appKey()));
    }

    @PutMapping("/narration/draft")
    public CommonResult<ShowroomNarrationVersion> saveNarrationDraft(@RequestBody NarrationDraftReqVO reqVO) {
        return success(runtime.saveNarrationDraft(reqVO));
    }

    @PostMapping("/narration/submit")
    public CommonResult<ShowroomNarrationVersion> submitNarration(@RequestBody NarrationSubmitReqVO reqVO) {
        return success(runtime.submitNarration(reqVO));
    }

    @PostMapping("/narration/supervisor-approve")
    public CommonResult<ShowroomNarrationVersion> supervisorApproveNarration(
            @RequestBody NarrationApprovalReqVO reqVO) {
        return success(runtime.supervisorApproveNarration(reqVO));
    }

    @PostMapping("/narration/gaoxin-approve")
    public CommonResult<ShowroomNarrationVersion> gaoxinApproveNarration(
            @RequestBody NarrationApprovalReqVO reqVO) {
        return success(runtime.gaoxinApproveNarration(reqVO));
    }

    @PostMapping("/narration/publish")
    public CommonResult<ShowroomNarrationVersion> publishNarration(@RequestBody NarrationPublishReqVO reqVO) {
        return success(runtime.publishNarration(reqVO));
    }

    public record CompanyDraftReqVO(Long companyId, String companyType, String displayName, String displayNameEn,
                                    Map<String, String> fields) {
    }

    public record CompanyCurrentRespVO(Long companyId, Long revisionId, int revisionNo, String status,
                                       Map<String, String> fields, String companyType, String displayName,
                                       String displayNameEn,
                                       boolean live) {
    }

    public record CompanyRevisionRestoreReqVO(Long companyId, Long sourceRevisionId) {
    }

    public record CompanyNarrationScriptGenerateReqVO(Long companyId, Long sourceRevisionId, String companyType,
                                                      String displayName, Map<String, String> fields,
                                                      Integer targetLength) {
    }

    public record CompanyNarrationScriptGenerateRespVO(Long companyId, Long sourceRevisionId, String introTextZh) {
    }

    public record CompanyFieldTranslateReqVO(Long companyId, List<String> fieldCodes, Map<String, String> fields,
                                             String introTextZh) {
    }

    public record CompanyFieldTranslateRespVO(Long companyId, Map<String, String> translatedFields,
                                              String introTextEn) {
    }

    public record CompanyNarrationGenerateReqVO(Long companyId, Long sourceRevisionId, String language,
                                                String scriptText) {
    }

    public record CompanyNarrationVersionRespVO(Long narrationVersionId, String language, String scriptText,
                                                Long audioFileId, Integer audioDurationSeconds, String audioUrl,
                                                String voice) {
    }

    public record CompanyNarrationGenerateRespVO(Long companyId, Long sourceRevisionId, String scriptText,
                                                 CompanyNarrationVersionRespVO narration, String voice) {
    }

    public record CompanyNarrationPublishReqVO(Long zhNarrationVersionId, Long enNarrationVersionId) {
    }

    public record CompanyNarrationPublishRespVO(Long companyId, Long zhNarrationVersionId, Long enNarrationVersionId) {
    }

    public record PageQueryReqVO(String keyword, Integer pageNo, Integer pageSize, String ownerCompanyId,
                                 String ownerType, String lifecycleStage, String incompleteStatus,
                                 String approvalStatus, Long productId) {

        public PageQueryReqVO(String keyword, Integer pageNo, Integer pageSize) {
            this(keyword, pageNo, pageSize, null, null, null, null, null, null);
        }

        public PageQueryReqVO(String keyword, Integer pageNo, Integer pageSize, String ownerCompanyId,
                              String ownerType, String lifecycleStage, String incompleteStatus,
                              String approvalStatus) {
            this(keyword, pageNo, pageSize, ownerCompanyId, ownerType, lifecycleStage, incompleteStatus,
                    approvalStatus, null);
        }
    }

    public record ProductAttachmentReqVO(String assetType, Long fileId, String originalName, String mimeType,
                                         Long size, Integer displayOrder) {
    }

    public record ProductAttachmentRespVO(Long id, String assetType, Long fileId, String url, String originalName,
                                          String mimeType, Long size, Integer displayOrder) {
    }

    public record ProductAttachmentUploadRespVO(Long fileId, String url, String originalName, String mimeType,
                                                Long size, String assetType) {
    }

    public record ProductDraftReqVO(Long productId, Long productMasterId, String productCode, String nameCn, String nameEn,
                                    String legacyProductCode, Map<String, String> fields,
                                    List<ProductAttachmentReqVO> attachments) {

        public ProductDraftReqVO(Long productId, Long productMasterId, String productCode, String nameCn, String nameEn,
                                 Map<String, String> fields, List<ProductAttachmentReqVO> attachments) {
            this(productId, productMasterId, productCode, nameCn, nameEn, null, fields, attachments);
        }

        public ProductDraftReqVO(Long productId, String productCode, String nameCn, String nameEn,
                                 Map<String, String> fields, List<ProductAttachmentReqVO> attachments) {
            this(productId, null, productCode, nameCn, nameEn, null, fields, attachments);
        }

        public ProductDraftReqVO(Long productId, Long productMasterId, String productCode, String nameCn, String nameEn,
                                 Map<String, String> fields) {
            this(productId, productMasterId, productCode, nameCn, nameEn, null, fields, List.of());
        }

        public ProductDraftReqVO(Long productId, Long productMasterId, String productCode, String nameCn, String nameEn,
                                 String legacyProductCode, Map<String, String> fields) {
            this(productId, productMasterId, productCode, nameCn, nameEn, legacyProductCode, fields, List.of());
        }

        public ProductDraftReqVO(Long productId, String productCode, String nameCn, String nameEn,
                                 Map<String, String> fields) {
            this(productId, null, productCode, nameCn, nameEn, null, fields, List.of());
        }
    }

    public record ProductPublishReqVO(Long productId, Long productMasterId, String productCode, String nameCn, String nameEn,
                                      Map<String, String> fields, Long sourceRevisionId, String narrationScriptText,
                                      boolean narrationGeneratedByAi, List<ProductAttachmentReqVO> attachments) {

        public ProductPublishReqVO(Long productId, String productCode, String nameCn, String nameEn,
                                   Map<String, String> fields, Long sourceRevisionId, String narrationScriptText,
                                   boolean narrationGeneratedByAi, List<ProductAttachmentReqVO> attachments) {
            this(productId, null, productCode, nameCn, nameEn, fields, sourceRevisionId, narrationScriptText,
                    narrationGeneratedByAi, attachments);
        }

        public ProductPublishReqVO(Long productId, Long productMasterId, String productCode, String nameCn, String nameEn,
                                   Map<String, String> fields, Long sourceRevisionId, String narrationScriptText,
                                   boolean narrationGeneratedByAi) {
            this(productId, productMasterId, productCode, nameCn, nameEn, fields, sourceRevisionId,
                    narrationScriptText, narrationGeneratedByAi, List.of());
        }

        public ProductPublishReqVO(Long productId, String productCode, String nameCn, String nameEn,
                                   Map<String, String> fields, Long sourceRevisionId, String narrationScriptText,
                                   boolean narrationGeneratedByAi) {
            this(productId, null, productCode, nameCn, nameEn, fields, sourceRevisionId, narrationScriptText,
                    narrationGeneratedByAi, List.of());
        }
    }

    public record ProductFieldTranslateReqVO(Long productId, String nameCn, Map<String, String> fields,
                                             String narrationScriptZh) {
    }

    public record ProductFieldTranslateRespVO(Long productId, String nameEn,
                                              Map<String, String> translatedFields,
                                              String narrationScriptEn) {
    }

    public record ProductNarrationGenerateReqVO(Long productId, Long sourceRevisionId) {
    }

    public record ProductNarrationGenerateRespVO(Long productId, Long zhNarrationVersionId,
                                                 Long enNarrationVersionId, String voice) {
    }

    public record HallNarrationGenerateReqVO(Long hallId) {
    }

    public record HallNarrationGenerateRespVO(Long hallId, Long zhNarrationVersionId,
                                              Long enNarrationVersionId, String voice) {
    }

    public record HallNarrationBatchGenerateFailureRespVO(Long hallId, String hallCode, String name, String reason) {
    }

    public record HallNarrationBatchGenerateRespVO(int matchedCount, int succeededCount, int failedCount,
                                                   List<HallNarrationBatchGenerateFailureRespVO> failures) {
    }

    public record ProductBatchGenerateReqVO(String keyword, String lifecycleStage, String incompleteStatus,
                                            String approvalStatus, String coverGenerationMode) {
        public ProductBatchGenerateReqVO(String keyword, String lifecycleStage, String incompleteStatus,
                                         String approvalStatus) {
            this(keyword, lifecycleStage, incompleteStatus, approvalStatus, null);
        }
    }

    public record ProductBatchGenerateFailureRespVO(Long productId, String productCode, String nameCn, String reason) {
    }

    public record ProductSalesCountryBatchGenerateRespVO(int matchedCount, int skippedCompletedCount,
                                                         int updatedProductCount, int generatedLanguageCount,
                                                         int failedCount,
                                                         List<ProductBatchGenerateFailureRespVO> failures) {
    }

    public record ProductBatchGenerateRespVO(int matchedCount, int publishedCount, int skippedUnpublishedCount,
                                             int skippedExistingCount, int skippedMissingScriptCount,
                                             int succeededCount, int failedCount, boolean autoCheckEnabled,
                                             int remainingActionableCount, Long taskId, String taskStatus,
                                             int remainingPendingCount, String nextCheckAt,
                                             List<ProductBatchGenerateFailureRespVO> failures) {
        public ProductBatchGenerateRespVO(int matchedCount, int publishedCount, int skippedUnpublishedCount,
                                          int skippedExistingCount, int skippedMissingScriptCount,
                                          int succeededCount, boolean autoCheckEnabled,
                                          int remainingActionableCount, Long taskId, String taskStatus,
                                          int remainingPendingCount, String nextCheckAt,
                                          List<ProductBatchGenerateFailureRespVO> failures) {
            this(matchedCount, publishedCount, skippedUnpublishedCount, skippedExistingCount,
                    skippedMissingScriptCount, succeededCount, 0, autoCheckEnabled, remainingActionableCount,
                    taskId, taskStatus, remainingPendingCount, nextCheckAt, failures);
        }

        public ProductBatchGenerateRespVO(int matchedCount, int publishedCount, int skippedUnpublishedCount,
                                          int skippedExistingCount, int succeededCount, int failedCount,
                                          List<ProductBatchGenerateFailureRespVO> failures) {
            this(matchedCount, publishedCount, skippedUnpublishedCount, skippedExistingCount, 0,
                    succeededCount, failedCount, false, 0, null, "", 0, null, failures);
        }

        public ProductBatchGenerateRespVO(int matchedCount, int publishedCount, int skippedUnpublishedCount,
                                          int succeededCount, int failedCount,
                                          List<ProductBatchGenerateFailureRespVO> failures) {
            this(matchedCount, publishedCount, skippedUnpublishedCount, 0, 0,
                    succeededCount, failedCount, false, 0, null, "", 0, null, failures);
        }
    }

    public record ProductBatchGenerateStateRespVO(boolean enabled, String keyword, String lifecycleStage,
                                                  String incompleteStatus, String approvalStatus, int matchedCount,
                                                  int publishedCount, int skippedUnpublishedCount,
                                                  int skippedExistingCount, int skippedMissingScriptCount,
                                                  int succeededCount, int failedCount,
                                                  int remainingActionableCount, Long lastRunAt,
                                                  String lastFailureMessage, Long lastFailureAt) {
    }

    public record ProductNarrationScriptTaskCurrentProductRespVO(Long productId, String productCode,
                                                                 String nameCn) {
    }

    public record ProductBatchTaskCurrentProductRespVO(Long productId, String productCode, String nameCn) {
    }

    public record ProductNarrationScriptBatchTaskRespVO(boolean active, boolean running, String keyword,
                                                        String lifecycleStage, String incompleteStatus,
                                                        String approvalStatus, int matchedCount,
                                                        int skippedCompletedCount, int generatedLanguageCount,
                                                        int failedCount, int remainingCount, Long startedAt,
                                                        Long lastRunAt, Long completedAt,
                                                        ProductNarrationScriptTaskCurrentProductRespVO currentProduct,
                                                        ProductBatchGenerateFailureRespVO lastFailure,
                                                        Long lastFailureAt) {
    }

    public record ProductCoverBatchTaskStateRespVO(boolean startAllowed,
                                                   boolean active,
                                                   boolean running,
                                                   String keyword,
                                                   String lifecycleStage,
                                                   String incompleteStatus,
                                                   String approvalStatus,
                                                   int matchedCount,
                                                   int publishedCount,
                                                   int skippedUnpublishedCount,
                                                   int skippedExistingCount,
                                                   int succeededCount,
                                                   int failedCount,
                                                   int remainingPendingCount,
                                                   Long taskId,
                                                   String taskStatus,
                                                   String nextCheckAt,
                                                   Long lastRunAt,
                                                   Long completedAt,
                                                   String lastFailureMessage,
                                                   ProductBatchTaskCurrentProductRespVO currentProduct) {
    }

    public record ProductTranslatePublishBatchTaskRespVO(boolean active, boolean running, String keyword,
                                                         String lifecycleStage, String incompleteStatus,
                                                         String approvalStatus, int matchedCount,
                                                         int succeededCount, int failedCount,
                                                         int remainingCount, Long startedAt,
                                                         Long lastRunAt, Long completedAt,
                                                         ProductBatchTaskCurrentProductRespVO currentProduct,
                                                         ProductBatchGenerateFailureRespVO lastFailure,
                                                         Long lastFailureAt,
                                                         List<ProductBatchGenerateFailureRespVO> failures) {
    }

    public record ProductCoverGenerateReqVO(Long productId, String productCode, String nameCn, String nameEn,
                                            Map<String, String> fields) {
    }

    public record ProductCoverGenerateRespVO(String coverImage) {
    }

    public record AwardCoverGenerateReqVO(Long awardId) {
    }

    public record ImagePromptCurrentRespVO(Long promptVersionId, String sceneCode, Integer versionNo,
                                           String templateText, String changeNote, List<String> placeholderCodes,
                                           Integer useCount, Long createTime, String creator, Long lastUsedAt) {
    }

    public record ImagePromptHistoryItemRespVO(Long promptVersionId, String sceneCode, Integer versionNo,
                                               String templateText, String changeNote, List<String> placeholderCodes,
                                               Integer useCount, Long createTime, String creator, Long lastUsedAt,
                                               boolean current) {
    }

    public record ImagePromptVersionSaveReqVO(
            @NotBlank(message = "sceneCode 不能为空")
            @Size(max = 64, message = "sceneCode 长度不能超过 64 个字符")
            String sceneCode,
            @NotBlank(message = "提示词模板不能为空")
            @Size(max = 12000, message = "提示词模板长度不能超过 12000 个字符")
            String templateText,
            @Size(max = 255, message = "版本说明长度不能超过 255 个字符")
            String changeNote) {
    }

    public record ShowroomProductImportFailureRespVO(Integer rowNo, String productCode, String reason) {
    }

    public record ShowroomAwardImportFailureRespVO(Integer rowNo, String awardCode, String reason) {
    }

    public record ShowroomProductImportRespVO(int totalRows, int successCount, int skippedCount, int failureCount,
                                              List<String> successProductCodes, List<String> skippedProductCodes,
                                              List<ShowroomProductImportFailureRespVO> failures,
                                              int awardTotalRows, int awardSuccessCount, int awardFailureCount,
                                              List<String> successAwardCodes, List<String> awardWarnings,
                                              List<ShowroomAwardImportFailureRespVO> awardFailures) {

        public ShowroomProductImportRespVO(int totalRows, int successCount, int skippedCount, int failureCount,
                                           List<String> successProductCodes, List<String> skippedProductCodes,
                                           List<ShowroomProductImportFailureRespVO> failures) {
            this(totalRows, successCount, skippedCount, failureCount, successProductCodes, skippedProductCodes,
                    failures, 0, 0, 0, List.of(), List.of(), List.of());
        }
    }

    public enum ShowroomProductImportSameAction {
        SKIP,
        OVERWRITE;

        public static ShowroomProductImportSameAction fromRequest(String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("相同产品处理方式不能为空");
            }
            String normalized = value.trim();
            for (ShowroomProductImportSameAction action : values()) {
                if (action.name().equalsIgnoreCase(normalized)) {
                    return action;
                }
            }
            throw new IllegalArgumentException("相同产品处理方式不支持：" + value);
        }
    }

    public record SubmitReqVO(Long targetId, Long targetRevisionId, List<String> fieldCodes, String moduleCode,
                              Long submittedBy, Long submitterDeptId, Long supervisorUserId, Long gaoxinUserId) {
    }

    public record HallSaveReqVO(String hallCode, String name, String nameEn, String description,
                                String descriptionEn) {
    }

    public record HallUpdateReqVO(Long hallId, String name, String nameEn, String description,
                                  String descriptionEn) {
    }

    public record HallMappingReqVO(Long hallId, List<HallProductMappingReqVO> products) {
    }

    public record HallProductMappingReqVO(Long productId, Integer displayOrder,
                                          BigDecimal layoutX, BigDecimal layoutY,
                                          BigDecimal layoutWidth, BigDecimal layoutHeight) {
        public HallProductMappingReqVO(Long productId, Integer displayOrder) {
            this(productId, displayOrder, null, null, null, null);
        }
    }

    public record HallItemMappingReqVO(Long hallId, List<HallItemMappingItemReqVO> items) {
    }

    public record HallItemMappingItemReqVO(String itemType, Long itemId, Integer displayOrder,
                                           BigDecimal layoutX, BigDecimal layoutY,
                                           BigDecimal layoutWidth, BigDecimal layoutHeight) {
    }

    public record HallCanvasBackgroundReqVO(Long hallId, String canvasBackgroundImageUrl) {
    }

    public record HallPreviewAssetPublishReqVO(Long hallId, Long imageFileId) {
    }

    public record HallPreviewAssetPublishRespVO(Long hallId, Long previewAssetVersionId, Long imageFileId,
                                                String previewImageUrl, boolean live) {
    }

    public record ApprovalActionReqVO(Long id, Long reviewerUserId,
                                      @NotBlank(message = "签名密码不能为空")
                                      String password,
                                      String comment) {
    }

    public record ApprovalRejectReqVO(Long id, Long reviewerUserId,
                                      @NotBlank(message = "签名密码不能为空")
                                      String password,
                                      @NotBlank(message = "驳回原因不能为空")
                                      String reason) {
    }

    public record AssignmentCreateReqVO(String targetType, Long targetId, String fieldCode, Long assigneeUserId,
                                        Long assignedBy) {
    }

    public record AssignmentPageReqVO(String targetType, Long targetId, Long assigneeUserId, String status,
                                      Integer pageNo, Integer pageSize) {
    }

    public record AssignmentCompleteReqVO(Long assignmentId, String fieldValue, Long operatorUserId,
                                          Long gaoxinUserId) {
    }

    public record DiscussionSummaryRespVO(int totalComments, int openComments, int resolvedComments) {
    }

    public record NarrationAvailabilityRespVO(Long narrationVersionId, String language, String audienceType,
                                              String status, boolean live, boolean audioReady) {
    }

    public record MaterialBlockerRespVO(String blockerCode, String message, String targetType, Long targetId,
                                        String language, List<String> missingFields, Long fileId, String assetId,
                                        String contentHash, String backendErrorCode) {
    }

    public record LatestNarrationRespVO(Long narrationVersionId, String language, String audienceType,
                                        String status, boolean live, boolean audioReady, String audioUrl,
                                        String voice) {
    }

    public record NarrationVersionRespVO(Long id,
                                         ShowroomNarrationKey key,
                                         Long sourceRevisionId,
                                         Integer versionNo,
                                         String scriptText,
                                         Long audioFileId,
                                         String audioUrl,
                                         Integer audioDurationSeconds,
                                         String voice,
                                         String generationStatus,
                                         String status,
                                         boolean generatedByAi,
                                         Instant generatedAt,
                                         Instant publishedAt,
                                         boolean live) {
    }

    public record ProductAssignmentRespVO(Long assignmentId, Long assigneeUserId, String status) {
    }

    public record ProductDetailRespVO(Long productId, Long productMasterId, String productCode, String legacyProductCode,
                                       Long currentRevisionId, boolean incomplete,
                                       boolean live, Long revisionId, int revisionNo, String status, String nameCn,
                                      String nameEn, Map<String, String> fields, List<Long> relatedProductIds,
                                      DiscussionSummaryRespVO discussionSummary,
                                      List<NarrationAvailabilityRespVO> narrations,
                                      ProductAssignmentRespVO activeAssignment,
                                      boolean editable,
                                      List<ProductAttachmentRespVO> attachments,
                                      List<MaterialBlockerRespVO> materialBlockers) {
        public ProductDetailRespVO(Long productId, String productCode, Long currentRevisionId, boolean incomplete,
                                   boolean live, Long revisionId, int revisionNo, String status, String nameCn,
                                   String nameEn, Map<String, String> fields, List<Long> relatedProductIds,
                                   DiscussionSummaryRespVO discussionSummary,
                                   List<NarrationAvailabilityRespVO> narrations,
                                    ProductAssignmentRespVO activeAssignment,
                                    boolean editable) {
            this(productId, null, productCode, null, currentRevisionId, incomplete, live, revisionId, revisionNo,
                    status, nameCn, nameEn, fields, relatedProductIds, discussionSummary, narrations, activeAssignment,
                    editable, List.of(), List.of());
        }

    }

    public record ProductPageRespVO(Long productId, Long productMasterId, String productCode, String legacyProductCode,
                                    Long currentRevisionId, boolean incomplete,
                                    boolean live, ProductDetailRespVO revision, ProductDetailRespVO displayRevision,
                                    LatestNarrationRespVO latestNarration,
                                    boolean editable) {

        public ProductPageRespVO(Long productId, String productCode, Long currentRevisionId, boolean incomplete,
                                 boolean live, ProductDetailRespVO revision, ProductDetailRespVO displayRevision,
                                 LatestNarrationRespVO latestNarration,
                                 boolean editable) {
            this(productId, null, productCode, null, currentRevisionId, incomplete, live, revision, displayRevision,
                    latestNarration, editable);
        }

    }

    public record HallPageRespVO(Long hallId, String hallCode, String name, String nameEn, String description,
                                  String descriptionEn,
                                  String canvasBackgroundImageUrl,
                                  List<HallProductMappingReqVO> productMappings, int productCount,
                                  List<HallItemMappingItemReqVO> itemMappings,
                                  LatestNarrationRespVO zhNarration, LatestNarrationRespVO enNarration) {
    }

    public record HallProductOptionRespVO(Long productId, Long productMasterId, String productCode, String nameCn, Integer revisionNo,
                                          boolean incomplete, String previewImageUrl, List<Long> hallIds) {

        public HallProductOptionRespVO(Long productId, String productCode, String nameCn, Integer revisionNo,
                                       boolean incomplete, String previewImageUrl, List<Long> hallIds) {
            this(productId, null, productCode, nameCn, revisionNo, incomplete, previewImageUrl, hallIds);
        }
    }

    public record HallItemOptionRespVO(String itemType, Long itemId, String itemCode, String nameCn, String nameEn,
                                       Integer revisionNo, boolean incomplete, String previewImageUrl,
                                       List<Long> hallIds) {
    }

    private static String buildAdminFileAccessUrl(FileDO file) {
        return "/admin-api/infra/file/" + file.getConfigId() + "/get/"
                + UriUtils.encodePath(file.getPath(), StandardCharsets.UTF_8);
    }

    public record VersionDiffItemRespVO(String fieldCode, String label, String oldValue, String newValue,
                                        Long operatorId, String operatorAction, Instant createdAt) {
    }

    public record VersionHistoryRespVO(Long revisionId, Integer revisionNo, String status,
                                       List<VersionDiffItemRespVO> diffItems) {
    }

    public record ReleasePublishRespVO(String releaseId, String manifestHash, String rootDocumentId,
                                       int documentCount, int assetCount, long installBytes,
                                       Instant publishedAt) {
    }

    public record ReleasePublishReqVO(String siteKey, String stage) {
    }

    public record ProductCommentCreateReqVO(Long productId, Long targetRevisionId, Long changeRequestId,
                                            String anchorType, String anchorKey, Long createdBy, String content) {
    }

    public record ProductCommentPageReqVO(Long productId, String anchorType, String anchorKey, Long changeRequestId,
                                          String status) {
    }

    public record ProductCommentReplyReqVO(Long commentId, Long createdBy, String content) {
    }

    public record ProductCommentResolveReqVO(Long commentId, Long resolvedBy) {
    }

    public record NarrationDraftReqVO(String targetType, Long targetId, Long sourceRevisionId, String audienceType,
                                      String language, String scriptText, Long audioFileId,
                                      Integer audioDurationSeconds, boolean generatedByAi) {
    }

    public record NarrationAudioGenerateReqVO(Long narrationVersionId) {
    }

    public record NarrationTtsDefaultsRespVO(String defaultVoice, boolean voiceSaved, boolean voiceConfigured,
                                             String voiceSource, boolean appKeySaved, boolean appKeyConfigured,
                                             String appKeySource, String maskedAppKey, boolean tokenSaved,
                                             boolean tokenConfigured, String tokenSource, String maskedAccessToken) {
    }

    public record NarrationTtsDefaultVoiceReqVO(@NotBlank(message = "默认音色不能为空")
                                                @Size(max = 64, message = "默认音色长度不能超过 64 个字符")
                                                String voice) {
    }

    public record NarrationTtsDefaultTokenReqVO(@NotBlank(message = "AccessToken 不能为空")
                                                @Size(max = 500, message = "AccessToken 长度不能超过 500 个字符")
                                                String accessToken) {
    }

    public record NarrationTtsDefaultAppKeyReqVO(@NotBlank(message = "AppKey 不能为空")
                                                 @Size(max = 500, message = "AppKey 长度不能超过 500 个字符")
                                                 String appKey) {
    }

    public record NarrationSubmitReqVO(Long narrationVersionId, Long supervisorUserId, Long gaoxinUserId,
                                       boolean manualConfirmed) {
    }

    public record NarrationApprovalReqVO(Long narrationVersionId, Long reviewerUserId) {
    }

    public record NarrationPublishReqVO(Long narrationVersionId) {
    }

    private void requirePublicityRole() {
        Long operatorUserId = requireOperatorUserId();
        if (!isShowroomPublicity(operatorUserId)) {
            throw exception0(FORBIDDEN.getCode(), "当前用户无权编辑公司信息");
        }
    }

    private void requirePublicityRole(String actionLabel) {
        Long operatorUserId = requireOperatorUserId();
        if (!isShowroomPublicity(operatorUserId)) {
            throw exception0(FORBIDDEN.getCode(), "当前用户无权执行" + actionLabel);
        }
    }

    private Long requireOperatorUserId() {
        Long operatorUserId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorUserId == null) {
            throw exception0(FORBIDDEN.getCode(), "当前登录用户不存在，无法执行当前操作");
        }
        return operatorUserId;
    }

}
