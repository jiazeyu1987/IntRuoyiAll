package cn.iocoder.yudao.module.showroom.integration;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.FastExcelFactory;
import com.sun.net.httpserver.HttpServer;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductShowroomWorkbookRowDTO;
import cn.iocoder.yudao.module.mdm.enums.MdmProductStatusConstants;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardSnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetOperations;
import cn.iocoder.yudao.module.showroom.configpackage.ShowroomHallConfigPackageService;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomProductImportMode;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomAwardExcelImportExtras;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomProductResourcePackage;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.ShowroomAwardExcelImportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.product.ShowroomProductExcelVO;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardDraft;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverImageService;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionRelationMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.keyword.ShowroomKeywordMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationTranslationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudioDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationGenerationStatus;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomPersistentNarrationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomProductNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationStatus;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.release.ShowroomLegacyWebsiteConfigProjector;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalActorResolver;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        ShowroomAdminController.class,
        ShowroomApiRuntime.class,
        ShowroomPersistentContentService.class
})
class ShowroomProductExcelImportExportIntegrationTest extends BaseDbUnitTest {

    private static final byte[] ONE_PIXEL_PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WnXl1QAAAAASUVORK5CYII=");
    private static final byte[] TEST_WAV_BYTES = new byte[]{
            'R', 'I', 'F', 'F', 12, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' '
    };
    private static final List<String> REFERENCE_PRODUCT_HEADERS = List.of(
            "展品编码", "旧产品编号", "产品名-中文", "产品名-英文", "展柜名称", "持证公司", "在售/在研", "BU",
            "在售国家", "适应症", "型号规格", "注册证信息", "卖点文案", "产品图", "奖项", "原材料表单");

    @Resource
    private ShowroomAdminController adminController;

    @Resource
    private ShowroomPersistentContentService contentService;

    @MockBean
    private ShowroomWorkflowFacade workflowFacade;
    @MockBean
    private ShowroomAssignmentService assignmentService;
    @MockBean
    private ShowroomVersionBundleService versionBundleService;
    @MockBean
    private ShowroomApprovalActorResolver approvalActorResolver;
    @MockBean
    private ShowroomProductCommentService commentService;
    @MockBean
    private SecurityFrameworkService securityFrameworkService;
    @MockBean
    private ShowroomProductCoverImageService productCoverImageService;
    @MockBean
    private ShowroomProductCoverBatchTaskService productCoverBatchTaskService;
    @MockBean
    private ShowroomImagePromptVersionService imagePromptVersionService;
    @MockBean
    private ShowroomPersistentNarrationService narrationService;
    @MockBean
    private ShowroomCompanyNarrationCodexService narrationCodexService;
    @MockBean
    private ShowroomCompanyNarrationTranslationService narrationTranslationService;
    @MockBean
    private ShowroomProductNarrationCodexService productNarrationCodexService;
    @MockBean
    private ShowroomPreviewAssetVersionMapper previewAssetVersionMapper;
    @MockBean
    private ShowroomPreviewAssetOperations previewAssetService;
    @MockBean
    private FileMapper fileMapper;
    @MockBean
    private FileService fileService;
    @MockBean
    private ConfigService configService;
    @MockBean
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;
    @MockBean
    private YudaoAiProperties yudaoAiProperties;
    @MockBean
    private ShowroomProductRevisionRelationMapper productRevisionRelationMapper;
    @MockBean
    private ShowroomChangeRequestMapper changeRequestMapper;
    @MockBean
    private ShowroomLegacyWebsiteConfigProjector legacyWebsiteConfigProjector;
    @MockBean
    private ShowroomHallConfigPackageService hallConfigPackageService;
    @MockBean
    private MdmProductApi mdmProductApi;
    @Resource
    private ShowroomKeywordMapper keywordMapper;

    private final AtomicLong narrationIds = new AtomicLong(10_000L);
    private final Map<Long, ShowroomNarrationVersion> narrationVersions = new HashMap<>();
    private final AtomicLong importedFileIds = new AtomicLong(50_000L);
    private final AtomicLong productMasterIds = new AtomicLong(7_000L);
    private final Map<Long, MdmProductRespDTO> productMastersById = new HashMap<>();
    private final Map<String, Long> productMasterIdsByCode = new HashMap<>();

    @BeforeEach
    void setUp() {
        narrationIds.set(10_000L);
        importedFileIds.set(50_000L);
        productMasterIds.set(7_000L);
        narrationVersions.clear();
        productMastersById.clear();
        productMasterIdsByCode.clear();
        when(commentService.pageByProduct(anyLong(), any(), any(), any())).thenReturn(List.of());
        when(narrationService.live(any(ShowroomNarrationKey.class))).thenReturn(Optional.empty());
        when(narrationService.draftScript(any())).thenAnswer(invocation -> {
            var command = (cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationDraftCommand) invocation.getArgument(0);
            return registerNarration(new ShowroomNarrationVersion(
                    narrationIds.incrementAndGet(),
                    command.key(),
                    command.sourceRevisionId(),
                    1,
                    command.scriptText(),
                    null,
                    null,
                    null,
                    ShowroomNarrationGenerationStatus.SCRIPT_GENERATED,
                    ShowroomNarrationStatus.DRAFT,
                    command.generatedByAi(),
                    Instant.now(),
                    null,
                    false
            ));
        });
        when(narrationService.generateAudio(anyLong())).thenAnswer(invocation -> {
            Long narrationId = invocation.getArgument(0);
            ShowroomNarrationVersion version = narrationVersions.get(narrationId);
            return registerNarration(version.withAudio(narrationId + 100_000L, 30,
                    ShowroomNarrationGenerationStatus.AUDIO_GENERATED));
        });
        when(narrationService.attachAudio(any())).thenAnswer(invocation -> {
            ShowroomNarrationAudioDraftCommand command = invocation.getArgument(0);
            ShowroomNarrationVersion version = narrationVersions.get(command.narrationVersionId());
            return registerNarration(version.withAudio(command.audioFileId(), command.audioDurationSeconds(),
                    command.voice(), ShowroomNarrationGenerationStatus.AUDIO_GENERATED));
        });
        when(narrationService.publishDirectly(anyLong())).thenAnswer(invocation -> {
            Long narrationId = invocation.getArgument(0);
            ShowroomNarrationVersion version = narrationVersions.get(narrationId);
            return registerNarration(version.withPublication(Instant.now(), true));
        });
        when(assignmentService.getLatestOpenWholeProductAssignment(anyLong())).thenReturn(null);
        when(changeRequestMapper.selectListByTarget(anyString(), anyLong())).thenReturn(List.of());
        when(productRevisionRelationMapper.selectListByProductRevisionId(anyLong())).thenReturn(List.of());
        when(productCoverImageService.uploadImportedCoverImage(anyString(), any(byte[].class), anyString(), anyString()))
                .thenAnswer(invocation -> "/admin-api/infra/file/28/get/showroom/product/"
                        + invocation.getArgument(0) + "-imported-cover.png");
        when(mdmProductApi.exportForShowroomWorkbook(any())).thenAnswer(invocation -> {
            Collection<String> productCodes = invocation.getArgument(0);
            return productCodes.stream()
                    .map(productCode -> MdmProductShowroomWorkbookRowDTO.builder()
                            .productCode(productCode)
                            .dccProductCode("DCC-" + productCode)
                            .nameCn("主数据-" + productCode)
                            .nameEn("Master " + productCode)
                            .modelSpecification("规格-" + productCode)
                            .category("展厅分类")
                            .build())
                    .toList();
        });
        when(mdmProductApi.importFromShowroomWorkbook(any())).thenAnswer(invocation -> {
            List<MdmProductShowroomWorkbookRowDTO> rows = invocation.getArgument(0);
            Map<String, Long> idsByCode = new LinkedHashMap<>();
            for (MdmProductShowroomWorkbookRowDTO row : rows) {
                String workbookProductCode = row.getProductCode();
                String productCode = resolveShowroomImportProductCode(workbookProductCode);
                Long productMasterId = productMasterIdsByCode.computeIfAbsent(workbookProductCode,
                        ignored -> productMasterIds.incrementAndGet());
                productMastersById.put(productMasterId, MdmProductRespDTO.builder()
                        .id(productMasterId)
                        .productCode(productCode)
                        .dccProductCode(row.getDccProductCode())
                        .nameCn(resolveShowroomImportNameCn(productCode, row.getNameCn()))
                        .nameEn(resolveShowroomImportNameEn(productCode, row.getNameEn()))
                        .modelSpecification(row.getModelSpecification())
                        .category(row.getCategory())
                        .status(MdmProductStatusConstants.ENABLE)
                        .build());
                idsByCode.put(workbookProductCode, productMasterId);
            }
            return idsByCode;
        });
        when(mdmProductApi.getProduct(anyLong())).thenAnswer(invocation ->
                productMastersById.get(invocation.getArgument(0)));
        when(fileService.createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    Long fileId = importedFileIds.incrementAndGet();
                    FileDO file = new FileDO();
                    file.setId(fileId);
                    file.setConfigId(28L);
                    file.setPath("showroom/narration/imported/" + invocation.getArgument(1));
                    when(fileMapper.selectById(fileId)).thenReturn(file);
                    return fileId;
                });
        when(securityFrameworkService.hasRole(anyString())).thenAnswer(invocation -> {
            String roleCode = invocation.getArgument(0);
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null) {
                return false;
            }
            return switch (roleCode) {
                case "showroom_publicity" -> loginUserId == 300L;
                case "super_admin" -> loginUserId == 1L;
                default -> false;
            };
        });
    }

    @Test
    void exportProductExcelShouldIgnorePaginationAndExcludeMediaColumns() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        seedPublishedAward("AWARD-001", "创新奖", "2026", "颁发单位",
                "/admin-api/infra/file/28/get/showroom/award/award-1.png");
        seedKeyword("介入手术", "Interventional Procedure");
        when(fileService.getFileContent(eq(28L), eq("showroom/award/award-1.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        var first = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "EXCEL-001", "产品一", "Product One", "product_001",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "core_selling_points", "操作顺畅",
                        "target_market", "中国",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/product/cover-1.png")))
                .revisionId(), 901L);
        when(fileService.getFileContent(eq(28L), eq("showroom/product/cover-1.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        var second = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "EXCEL-002", "产品二", "Product Two",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "R_AND_D",
                        "target_market", "市场二")))
                .revisionId(), 902L);
        contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "EXCEL-003", "未上柜产品", "Unassigned Product",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "target_market", "市场三")))
                .revisionId(), 903L);
        var importHall = contentService.listHalls().stream()
                .filter(hall -> "创新展柜".equals(hall.name()))
                .findFirst()
                .orElseThrow();
        contentService.replaceHallProductMappings(importHall.hallId(), List.of(
                new ShowroomHallProductMapping(first.productId(), 1),
                new ShowroomHallProductMapping(second.productId(), 2)));

        MockHttpServletResponse response = new MockHttpServletResponse();
        withLoginUser(300L, () -> {
            adminController.exportProductExcel(
                    new ShowroomAdminController.PageQueryReqVO("EXCEL-", 1, 1, null,
                            null, null, null, null, null),
                    response);
            return null;
        });

        byte[] workbookBytes = productWorkbookBytes(response);
        List<ShowroomProductExcelVO> rows = readProductRows(workbookBytes);
        List<String> header = readHeader(workbookBytes);
        Map<String, String> firstDataRow = readFirstDataRowByHeader(workbookBytes);

        assertEquals(3, rows.size());
        assertTrue(rows.stream().anyMatch(row -> "EXCEL-001".equals(row.getProductCode())));
        assertTrue(rows.stream().anyMatch(row -> "EXCEL-002".equals(row.getProductCode())));
        assertTrue(rows.stream().anyMatch(row ->
                "EXCEL-003".equals(row.getProductCode())
                        && (row.getHallName() == null || row.getHallName().isBlank())));
        assertEquals(REFERENCE_PRODUCT_HEADERS, header);
        assertEquals("创新展柜", firstDataRow.get("展柜名称"));
        assertEquals("product_001", firstDataRow.get("旧产品编号"));
        assertEquals("产品一", firstDataRow.get("产品名-中文"));
        assertEquals("中国", firstDataRow.get("在售国家"));
        assertEquals("操作顺畅", firstDataRow.get("卖点文案"));
        assertEquals("", firstDataRow.get("产品图"));
        assertEquals(2, countEmbeddedPictures(workbookBytes));
        assertEquals(List.of("产品列表", "产品主数据", "奖项", "讲解音频", "关键词中英对照"),
                readSheetNames(workbookBytes));
        List<Map<String, String>> productMasterRows = readSheetRowsByHeader(workbookBytes, "产品主数据");
        assertEquals(3, productMasterRows.size());
        assertTrue(productMasterRows.stream().anyMatch(row ->
                "EXCEL-001".equals(row.get("产品编码")) && "主数据-EXCEL-001".equals(row.get("中文名称"))));
        List<ShowroomAwardExcelImportRow> awardRows =
                ShowroomAwardExcelImportExtras.read(workbookBytes);
        assertEquals(1, awardRows.size());
        assertEquals("AWARD-001", awardRows.get(0).awardCode());
        assertEquals("创新奖", awardRows.get(0).nameCn());
        assertEquals("2026", awardRows.get(0).awardDateText());
        assertEquals("颁发单位", awardRows.get(0).issuer());
        assertEquals(ONE_PIXEL_PNG_BYTES.length, awardRows.get(0).coverImage().content().length);
        assertTrue(isCellWrapped(workbookBytes, "卖点文案", 1));
        assertFalse(header.contains("产品"));
        assertFalse(header.contains("产品编码"));
        assertFalse(header.contains("中文名称"));
        assertFalse(header.contains("英文名称"));
        assertFalse(header.contains("持证人"));
        assertFalse(header.contains("生命周期"));
        assertFalse(header.contains("管线布局"));
        assertFalse(header.contains("核心卖点"));
        assertFalse(header.contains("注册证"));
        assertFalse(header.contains("产品归属/类型"));
        assertEquals(first.productId(), contentService.getCurrentOrLatestProductRevision(first.productId()).productId());
        assertEquals(second.productId(), contentService.getCurrentOrLatestProductRevision(second.productId()).productId());
    }

    @Test
    void exportProductExcelShouldResolveYingtaiOwnerCompanyWhenCurrentOwnerIdIsStale() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        seedPublishedAward("AWARD-001", "创新奖", "2026", "颁发单位",
                "/admin-api/infra/file/28/get/showroom/award/award-1.png");
        when(fileService.getFileContent(eq(28L), eq("showroom/award/award-1.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "EXCEL-STALE-OWNER", "历史所属公司产品", "Stale Owner Product",
                Map.of(
                        "owner_company_id", "124",
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "core_selling_points", "中国\n历史数据导出"))).revisionId(), 903L);
        var importHall = contentService.listHalls().stream()
                .filter(hall -> "创新展柜".equals(hall.name()))
                .findFirst()
                .orElseThrow();
        contentService.replaceHallProductMappings(importHall.hallId(), List.of(
                new ShowroomHallProductMapping(product.productId(), 1)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        withLoginUser(300L, () -> {
            adminController.exportProductExcel(new ShowroomAdminController.PageQueryReqVO(
                    "EXCEL-STALE-OWNER", 1, 10, null, null, null, null, null, null), response);
            return null;
        });

        byte[] workbookBytes = productWorkbookBytes(response);
        List<ShowroomProductExcelVO> rows = readProductRows(workbookBytes);
        Map<String, String> firstDataRow = readFirstDataRowByHeader(workbookBytes);

        assertEquals(1, rows.size());
        assertEquals("EXCEL-STALE-OWNER", rows.get(0).getProductCode());
        assertEquals(ownerCompany.displayName(), firstDataRow.get("持证公司"));
    }

    @Test
    void exportProductExcelShouldFailWhenNonYingtaiOwnerCompanyIdIsStale() {
        seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "EXCEL-STALE-SUBSIDIARY", "历史子公司产品", "Stale Subsidiary Product",
                Map.of(
                        "owner_company_id", "124",
                        "product_owner_type", "SUBSIDIARY",
                        "lifecycle_stage", "REGISTERED"))).revisionId(), 904L);
        var importHall = contentService.listHalls().stream()
                .filter(hall -> "创新展柜".equals(hall.name()))
                .findFirst()
                .orElseThrow();
        contentService.replaceHallProductMappings(importHall.hallId(), List.of(
                new ShowroomHallProductMapping(product.productId(), 1)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                withLoginUser(300L, () -> {
                    adminController.exportProductExcel(new ShowroomAdminController.PageQueryReqVO(
                            "EXCEL-STALE-SUBSIDIARY", 1, 10, null, null, null, null, null, null), response);
                    return null;
                }));

        assertTrue(exception.getMessage().contains("当前产品所属公司不存在，无法导出产品资料：124"));
    }

    @Test
    void exportProductExcelShouldFailWhenCoverImageContentMissing() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        seedPublishedAward("AWARD-001", "创新奖", "2026", "颁发单位",
                "/admin-api/infra/file/28/get/showroom/award/award-1.png");
        when(fileService.getFileContent(eq(28L), eq("showroom/award/award-1.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "EXCEL-MISSING-COVER", "缺失封面产品", "Missing Cover Product",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/product/missing-cover.png")))
                .revisionId(), 903L);
        var importHall = contentService.listHalls().stream()
                .filter(hall -> "创新展柜".equals(hall.name()))
                .findFirst()
                .orElseThrow();
        contentService.replaceHallProductMappings(importHall.hallId(), List.of(
                new ShowroomHallProductMapping(product.productId(), 1)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                withLoginUser(300L, () -> {
                    adminController.exportProductExcel(new ShowroomAdminController.PageQueryReqVO(
                            "EXCEL-MISSING-COVER", 1, 10, null, null, null, null, null, null), response);
                    return null;
                }));

        assertTrue(exception.getMessage().contains("产品图"));
        assertTrue(exception.getMessage().contains("EXCEL-MISSING-COVER"));
    }

    @Test
    void exportProductExcelShouldEmbedExternalHttpCoverImage() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/cover.png", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, ONE_PIXEL_PNG_BYTES.length);
            exchange.getResponseBody().write(ONE_PIXEL_PNG_BYTES);
            exchange.close();
        });
        server.start();
        try {
            String coverUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/cover.png";
            ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
            seedPublishedAward("AWARD-001", "创新奖", "2026", "颁发单位",
                    "/admin-api/infra/file/28/get/showroom/award/award-1.png");
            when(fileService.getFileContent(eq(28L), eq("showroom/award/award-1.png")))
                    .thenReturn(ONE_PIXEL_PNG_BYTES);
            var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                    null, "EXCEL-EXTERNAL-COVER", "外部封面产品", "External Cover Product",
                    Map.of(
                            "owner_company_id", String.valueOf(ownerCompany.companyId()),
                            "product_owner_type", "YINGTAI",
                            "lifecycle_stage", "REGISTERED",
                            "cover_image", coverUrl)))
                    .revisionId(), 904L);
            var importHall = contentService.listHalls().stream()
                    .filter(hall -> "创新展柜".equals(hall.name()))
                    .findFirst()
                    .orElseThrow();
            contentService.replaceHallProductMappings(importHall.hallId(), List.of(
                    new ShowroomHallProductMapping(product.productId(), 1)));
            MockHttpServletResponse response = new MockHttpServletResponse();

            withLoginUser(300L, () -> {
                adminController.exportProductExcel(new ShowroomAdminController.PageQueryReqVO(
                        "EXCEL-EXTERNAL-COVER", 1, 10, null, null, null, null, null, null), response);
                return null;
            });

            assertEquals(2, countEmbeddedPictures(productWorkbookBytes(response)));
            verify(fileService, times(1)).getFileContent(eq(28L), eq("showroom/award/award-1.png"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void exportProductExcelShouldFailWhenAwardCoverMissing() {
        seedAwardDraft("AWARD-001", "缺封面奖项", "2026", "颁发单位", "");
        MockHttpServletResponse response = new MockHttpServletResponse();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                withLoginUser(300L, () -> {
                    adminController.exportProductExcel(new ShowroomAdminController.PageQueryReqVO(
                            "EXCEL-", 1, 10, null, null, null, null, null, null), response);
                    return null;
                }));

        assertTrue(exception.getMessage().contains("奖项封面"));
        assertTrue(exception.getMessage().contains("AWARD-001"));
        assertTrue(exception.getMessage().contains("缺封面奖项"));
    }

    @Test
    void exportProductExcelShouldFailWhenNoAwardCanBeExported() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                withLoginUser(300L, () -> {
                    adminController.exportProductExcel(new ShowroomAdminController.PageQueryReqVO(
                            "EXCEL-", 1, 10, null, null, null, null, null, null), response);
                    return null;
                }));

        assertTrue(exception.getMessage().contains("当前租户没有可导出的奖项"));
    }

    @Test
    void exportProductExcelShouldFailWhenIntProductNarrationIncomplete() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        seedPublishedAward("AWARD-001", "创新奖", "2026", "颁发单位",
                "/admin-api/infra/file/28/get/showroom/award/award-1.png");
        when(fileService.getFileContent(eq(28L), eq("showroom/award/award-1.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        when(fileService.getFileContent(eq(28L), eq("showroom/product/int-12.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "INT-12", "球囊扩张压力泵", "Balloon Inflation Device",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/product/int-12.png")))
                .revisionId(), 908L);
        mockAwardPublishNarrations(contentService.listAwards().get(0).awardId(),
                contentService.getCurrentOrLatestAwardRevision(contentService.listAwards().get(0).awardId()).revisionId());
        var importHall = contentService.listHalls().stream()
                .filter(hall -> "创新展柜".equals(hall.name()))
                .findFirst()
                .orElseThrow();
        contentService.replaceHallProductMappings(importHall.hallId(), List.of(
                new ShowroomHallProductMapping(product.productId(), 1)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                withLoginUser(300L, () -> {
                    adminController.exportProductExcel(new ShowroomAdminController.PageQueryReqVO(
                            "INT-12", 1, 10, null, null, null, null, null, null), response);
                    return null;
                }));

        assertTrue(exception.getMessage().contains("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_MISSING"));
        assertTrue(exception.getMessage().contains("INT-12"));
        assertTrue(exception.getMessage().contains("ZH"));
        assertTrue(exception.getMessage().contains("EN"));
    }

    @Test
    void exportProductExcelShouldIncludeNarrationsForAllExportedProducts() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        seedPublishedAward("AWARD-001", "创新奖", "2026", "颁发单位",
                "/admin-api/infra/file/28/get/showroom/award/award-1.png");
        when(fileService.getFileContent(eq(28L), eq("showroom/award/award-1.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        ShowroomAwardRevision currentAward = contentService.getCurrentOrLatestAwardRevision(
                contentService.listAwards().get(0).awardId());
        mockAwardPublishNarrations(currentAward.awardId(), currentAward.revisionId());
        var included = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "INT-12", "球囊扩张压力泵", "Balloon Inflation Device",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/product/int-12.png")))
                .revisionId(), 908L);
        var orphan = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "INT-99", "未入展柜产品", "Orphan Product",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/product/int-99.png")))
                .revisionId(), 908L);
        when(fileService.getFileContent(eq(28L), eq("showroom/product/int-12.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        when(fileService.getFileContent(eq(28L), eq("showroom/product/int-99.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        contentService.replaceHallProductMappings(contentService.listHalls().get(0).hallId(),
                List.of(new ShowroomHallProductMapping(included.productId(), 1)));
        mockProductPublishNarrations(included.productId(), included.revisionId());
        mockProductPublishNarrations(orphan.productId(), orphan.revisionId());

        MockHttpServletResponse response = new MockHttpServletResponse();
        withLoginUser(300L, () -> {
            adminController.exportProductExcel(new ShowroomAdminController.PageQueryReqVO(
                    "INT-", 1, 10, null, null, null, null, null, null), response);
            return null;
        });

        byte[] workbookBytes = unzipPackage(response.getContentAsByteArray()).get(ShowroomProductResourcePackage.WORKBOOK_PATH);
        List<Map<String, String>> narrationRows = readSheetRowsByHeader(workbookBytes, "讲解音频");
        assertTrue(narrationRows.stream().anyMatch(row ->
                "PRODUCT".equals(row.get("目标类型")) && "INT-12".equals(row.get("目标编码"))));
        assertTrue(narrationRows.stream().anyMatch(row ->
                "PRODUCT".equals(row.get("目标类型")) && "INT-99".equals(row.get("目标编码"))));
    }

    @Test
    void getProductImportTemplateShouldExposeTextOnlyExcelContract() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        withLoginUser(300L, () -> {
            adminController.getProductImportTemplate(response);
            return null;
        });

        List<String> header = readHeader(response.getContentAsByteArray());
        assertEquals(REFERENCE_PRODUCT_HEADERS, header);
        assertEquals(List.of("产品列表", "产品主数据", "奖项", "讲解音频", "关键词中英对照"),
                readSheetNames(response.getContentAsByteArray()));
        List<ShowroomAwardExcelImportRow> awardRows =
                ShowroomAwardExcelImportExtras.read(response.getContentAsByteArray());
        assertEquals(1, awardRows.size());
        assertEquals("AWARD-001", awardRows.get(0).awardCode());
        assertEquals("示例奖项", awardRows.get(0).nameCn());
        assertEquals(ONE_PIXEL_PNG_BYTES.length, awardRows.get(0).coverImage().content().length);
        assertFalse(header.contains("产品编码"));
        assertFalse(header.contains("中文名称"));
        assertFalse(header.contains("英文名称"));
        assertFalse(header.contains("持证人"));
        assertFalse(header.contains("生命周期"));
        assertFalse(header.contains("管线布局"));
        assertFalse(header.contains("核心卖点"));
        assertFalse(header.contains("注册证"));
        assertFalse(header.contains("产品归属/类型"));
        assertFalse(header.contains("临床效果"));
        assertFalse(header.contains("FIM状态"));
    }

    @Test
    void exportProductExcelShouldIncludeKeywordSheetAndBilingualNarrationAudioSheet() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        seedKeyword("高分子材料", "Polymer Material");
        seedKeyword("介入手术", "Interventional Procedure");
        seedPublishedAward("AWARD-001", "创新奖", "2026", "颁发单位",
                "/admin-api/infra/file/28/get/showroom/award/award-1.png");
        when(fileService.getFileContent(eq(28L), eq("showroom/award/award-1.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        var currentAward = contentService.getCurrentOrLatestAwardRevision(contentService.listAwards().get(0).awardId());
        mockAwardPublishNarrations(currentAward.awardId(), currentAward.revisionId());
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "EXCEL-AUDIO-001", "语音产品", "Audio Product",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/product/audio-cover.png"))).revisionId(), 901L);
        when(fileService.getFileContent(eq(28L), eq("showroom/product/audio-cover.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        contentService.replaceHallProductMappings(contentService.listHalls().get(0).hallId(),
                List.of(new ShowroomHallProductMapping(product.productId(), 1)));
        mockProductPublishNarrations(product.productId(), product.revisionId());
        mockProductLatestDraftWithoutAudio(product.productId(), product.revisionId());

        MockHttpServletResponse response = new MockHttpServletResponse();
        withLoginUser(300L, () -> {
            adminController.exportProductExcel(
                    new ShowroomAdminController.PageQueryReqVO("EXCEL-AUDIO-001", 1, 10, null,
                            null, null, null, null, null),
                    response);
            return null;
        });

        byte[] packageBytes = response.getContentAsByteArray();
        Map<String, byte[]> entries = unzipPackage(packageBytes);
        byte[] workbookBytes = entries.get(ShowroomProductResourcePackage.WORKBOOK_PATH);
        assertNotNull(workbookBytes);
        assertTrue(entries.containsKey(ShowroomProductResourcePackage.MANIFEST_PATH));
        assertTrue(entries.keySet().stream().anyMatch(path -> path.startsWith("assets/narration/product/excel-audio-001/")));
        assertTrue(entries.keySet().stream().anyMatch(path -> path.startsWith("assets/narration/award/award-001/")));
        List<String> sheetNames = readSheetNames(workbookBytes);
        List<Map<String, String>> keywordRows = readSheetRowsByHeader(workbookBytes, "关键词中英对照");
        List<Map<String, String>> narrationRows = readSheetRowsByHeader(workbookBytes, "讲解音频");

        assertEquals(List.of("产品列表", "产品主数据", "奖项", "讲解音频", "关键词中英对照"), sheetNames);
        assertEquals(2, keywordRows.size());
        assertTrue(keywordRows.stream().anyMatch(row ->
                "高分子材料".equals(row.get("中文关键词")) && "Polymer Material".equals(row.get("English Keyword"))));
        assertTrue(keywordRows.stream().anyMatch(row ->
                "介入手术".equals(row.get("中文关键词")) && "Interventional Procedure".equals(row.get("English Keyword"))));
        assertTrue(narrationRows.stream().anyMatch(row ->
                "PRODUCT".equals(row.get("目标类型"))
                        && "EXCEL-AUDIO-001".equals(row.get("目标编码"))
                        && "ZH".equals(row.get("语言"))
                        && "产品中文讲解".equals(row.get("讲解稿"))
                        && row.get("音频文件ID").isBlank()
                        && row.get("音频地址") != null
                        && row.get("音频地址").startsWith("package://assets/narration/product/excel-audio-001/")
                        && "30".equals(row.get("音频时长(秒)"))));
        assertTrue(narrationRows.stream().anyMatch(row ->
                "PRODUCT".equals(row.get("目标类型"))
                        && "EXCEL-AUDIO-001".equals(row.get("目标编码"))
                        && "EN".equals(row.get("语言"))
                        && "English product narration".equals(row.get("讲解稿"))
                        && row.get("音频文件ID").isBlank()
                        && row.get("音频地址") != null
                        && row.get("音频地址").startsWith("package://assets/narration/product/excel-audio-001/")
                        && "30".equals(row.get("音频时长(秒)"))));
        assertTrue(narrationRows.stream().anyMatch(row ->
                "AWARD".equals(row.get("目标类型"))
                        && "AWARD-001".equals(row.get("目标编码"))
                        && "ZH".equals(row.get("语言"))));
        assertTrue(narrationRows.stream().anyMatch(row ->
                "AWARD".equals(row.get("目标类型"))
                        && "AWARD-001".equals(row.get("目标编码"))
                        && "EN".equals(row.get("语言"))));
    }

    @Test
    void importExportedAwardExcelShouldPreserveExistingAwardFieldsAndCoverUrl() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "EXCEL-AWARD-ROUNDTRIP", "回导占位产品", "Roundtrip Placeholder",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/product/cover/product.png")))
                .revisionId(), 903L);
        contentService.replaceHallProductMappings(contentService.listHalls().get(0).hallId(),
                List.of(new ShowroomHallProductMapping(product.productId(), 1)));
        String awardCover = "/admin-api/infra/file/28/get/showroom/product/cover/20260614/product-AWARD-001-imported-cover-17cea3c1b5b9f597.png";
        var awardDraft = contentService.saveAwardDraft(new ShowroomAwardDraft(null, "AWARD-001",
                "创新奖", "Innovation Award", "中文讲解", "English narration",
                "颁发单位", "2026", awardCover));
        contentService.publishAwardRevision(awardDraft.revisionId(), 300L);
        mockProductPublishNarrations(product.productId(), product.revisionId());
        mockAwardPublishNarrations(awardDraft.awardId(), awardDraft.revisionId());
        when(fileService.getFileContent(eq(28L), eq("showroom/product/cover/product.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        when(fileService.getFileContent(eq(28L), eq("showroom/product/cover/20260614/product-AWARD-001-imported-cover-17cea3c1b5b9f597.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        when(productCoverImageService.importedCoverImageMatchesCurrentCover(eq(awardCover), any()))
                .thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        withLoginUser(300L, () -> {
            adminController.exportProductExcel(new ShowroomAdminController.PageQueryReqVO(
                    "EXCEL-AWARD-ROUNDTRIP", 1, 10, null, null, null, null, null, null), response);
            return null;
        });
        var importResult = importPackage(response.getContentAsByteArray(), "OVERWRITE");
        var currentAward = contentService.requireCurrentAwardRevision(awardDraft.awardId());

        assertEquals(1, importResult.awardSuccessCount());
        assertEquals("Innovation Award", currentAward.nameEn());
        assertEquals("中文讲解", currentAward.fields().get("description_zh"));
        assertEquals("English narration", currentAward.fields().get("description_en"));
        assertEquals(awardCover, currentAward.fields().get("cover_image"));
        verify(productCoverImageService, never()).uploadImportedCoverImage(eq("AWARD-001"),
                any(), anyString(), anyString());
    }

    @Test
    void importProductExcelShouldImportNarrationSheetAndKeywordSheet() throws Exception {
        seedOwnerCompany();
        when(fileService.getFileContent(eq(28L), eq("showroom/narration/import-product-zh.wav")))
                .thenReturn("product-zh".getBytes(StandardCharsets.UTF_8));
        when(fileService.getFileContent(eq(28L), eq("showroom/narration/import-product-en.wav")))
                .thenReturn("product-en".getBytes(StandardCharsets.UTF_8));
        when(fileService.getFileContent(eq(28L), eq("showroom/narration/import-award-zh.wav")))
                .thenReturn("award-zh".getBytes(StandardCharsets.UTF_8));
        when(fileService.getFileContent(eq(28L), eq("showroom/narration/import-award-en.wav")))
                .thenReturn("award-en".getBytes(StandardCharsets.UTF_8));

        byte[] excelBytes = addNarrationAndKeywordSheets(
                buildImportExcel(List.of(ShowroomProductExcelVO.builder()
                        .productCode("IMPORT-AUDIO-001")
                        .nameCn("导入语音产品")
                        .nameEn("Imported Audio Product")
                        .hallName("创新展柜")
                        .ownerCompanyName("瑛泰")
                        .lifecycleStage("已注册")
                        .pipelineLayout("导入BU")
                        .coreSellingPoints("中国")
                        .indicationContent("导入适应症")
                        .modelSpecification("导入型号")
                        .registrationCertificate("导入注册证")
                        .sellingPointsCopy("导入卖点文案")
                        .productImage("")
                        .awards("")
                        .rawMaterialSheet("")
                        .build())),
                List.of(
                        new NarrationSheetRow("PRODUCT", "IMPORT-AUDIO-001", "导入语音产品", "ZH",
                                "导入中文讲解", null,
                                "/admin-api/infra/file/28/get/showroom/narration/import-product-zh.wav",
                                30, "ruoxi"),
                        new NarrationSheetRow("PRODUCT", "IMPORT-AUDIO-001", "导入语音产品", "EN",
                                "Imported English narration", null,
                                "/admin-api/infra/file/28/get/showroom/narration/import-product-en.wav",
                                32, "ruoxi"),
                        new NarrationSheetRow("AWARD", "AWARD-001", "创新奖", "ZH",
                                "奖项中文讲解", null,
                                "/admin-api/infra/file/28/get/showroom/narration/import-award-zh.wav",
                                28, "ruoxi"),
                        new NarrationSheetRow("AWARD", "AWARD-001", "创新奖", "EN",
                                "Award English narration", null,
                                "/admin-api/infra/file/28/get/showroom/narration/import-award-en.wav",
                                29, "ruoxi")),
                List.of(
                        new KeywordSheetRow("高分子材料", "Polymer Material"),
                        new KeywordSheetRow("介入手术", "Interventional Procedure"))
        );

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes, "OVERWRITE");
        ShowroomProductSnapshot importedProduct = contentService.listProducts().stream()
                .filter(item -> "IMPORT-AUDIO-001".equals(item.productCode()))
                .findFirst()
                .orElseThrow();
        ShowroomProductRevision currentProduct = contentService.requireCurrentProductRevision(importedProduct.productId());
        ShowroomAwardSnapshot importedAward = contentService.listAwards().stream()
                .filter(item -> "AWARD-001".equals(item.awardCode()))
                .findFirst()
                .orElseThrow();
        ShowroomAwardRevision currentAward = contentService.requireCurrentAwardRevision(importedAward.awardId());

        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(1, response.awardSuccessCount(), response.awardFailures().toString());
        assertEquals(List.of("高分子材料", "介入手术"),
                keywordMapper.selectListOrdered().stream().map(cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO::getNameZh).toList());
        assertTrue(narrationVersions.values().stream().anyMatch(version ->
                version.key().targetType() == ShowroomNarrationTargetType.PRODUCT
                        && version.key().targetId().equals(importedProduct.productId())
                        && version.sourceRevisionId().equals(currentProduct.revisionId())
                        && version.key().language() == ShowroomNarrationLanguage.ZH
                        && "导入中文讲解".equals(version.scriptText())
                        && version.audioFileId() != null
                        && version.live()));
        assertTrue(narrationVersions.values().stream().anyMatch(version ->
                version.key().targetType() == ShowroomNarrationTargetType.PRODUCT
                        && version.key().targetId().equals(importedProduct.productId())
                        && version.sourceRevisionId().equals(currentProduct.revisionId())
                        && version.key().language() == ShowroomNarrationLanguage.EN
                        && "Imported English narration".equals(version.scriptText())
                        && version.audioFileId() != null
                        && version.live()));
        assertTrue(narrationVersions.values().stream().anyMatch(version ->
                version.key().targetType() == ShowroomNarrationTargetType.AWARD
                        && version.key().targetId().equals(importedAward.awardId())
                        && version.sourceRevisionId().equals(currentAward.revisionId())
                        && version.key().language() == ShowroomNarrationLanguage.ZH
                        && "奖项中文讲解".equals(version.scriptText())
                        && version.audioFileId() != null
                        && version.live()));
        assertTrue(narrationVersions.values().stream().anyMatch(version ->
                version.key().targetType() == ShowroomNarrationTargetType.AWARD
                        && version.key().targetId().equals(importedAward.awardId())
                        && version.sourceRevisionId().equals(currentAward.revisionId())
                        && version.key().language() == ShowroomNarrationLanguage.EN
                        && "Award English narration".equals(version.scriptText())
                        && version.audioFileId() != null
                        && version.live()));
    }

    @Test
    void importProductResourcePackageShouldRejectIntProductWithoutNarrationAssets() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "INT-12", "导入前无音频产品", "Package No Audio",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "pipeline_layout", "旧BU")))
                .revisionId(), 901L);
        byte[] workbook = buildImportExcel(List.of(ShowroomProductExcelVO.builder()
                .productCode("INT-12")
                .nameCn("导入后无音频产品")
                .nameEn("Package No Audio")
                .hallName("创新展柜")
                .ownerCompanyName("瑛泰")
                .lifecycleStage("已注册")
                .pipelineLayout("新BU")
                .coreSellingPoints("")
                .indicationContent("")
                .modelSpecification("")
                .registrationCertificate("")
                .sellingPointsCopy("")
                .productImage("")
                .awards("")
                .rawMaterialSheet("")
                .build()));
        byte[] packageBytes = buildResourcePackage(workbook);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> importPackage(packageBytes, "OVERWRITE"));
        ShowroomProductRevision current = contentService.requireCurrentProductRevision(product.productId());

        assertTrue(exception.getMessage().contains("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_MISSING"));
        assertTrue(exception.getMessage().contains("INT-12"));
        assertTrue(exception.getMessage().contains("ZH"));
        assertTrue(exception.getMessage().contains("EN"));
        assertEquals("导入前无音频产品", current.nameCn());
    }

    @Test
    void importProductResourcePackageShouldRejectProductNarrationCodeOutsideProductList() throws Exception {
        seedOwnerCompany();
        byte[] workbook = buildImportExcel(List.of(ShowroomProductExcelVO.builder()
                .productCode("INT-12")
                .nameCn("球囊扩张压力泵")
                .nameEn("Balloon Inflation Device")
                .hallName("创新展柜")
                .ownerCompanyName("瑛泰")
                .lifecycleStage("已注册")
                .pipelineLayout("新BU")
                .coreSellingPoints("")
                .indicationContent("")
                .modelSpecification("")
                .registrationCertificate("")
                .sellingPointsCopy("")
                .productImage("")
                .awards("")
                .rawMaterialSheet("")
                .build()));
        workbook = addNarrationAndKeywordSheets(workbook, List.of(
                new NarrationSheetRow("PRODUCT", "product_049", "旧产品", "ZH",
                        "旧产品中文讲解", null, "package://assets/narration/product/product_049/001-zh.wav", 30,
                        "xiaoyun"),
                new NarrationSheetRow("PRODUCT", "product_049", "旧产品", "EN",
                        "Legacy product English narration", null,
                        "package://assets/narration/product/product_049/002-en.wav", 30, "jenny")
        ), List.of());
        byte[] packageBytes = buildResourcePackage(workbook, """
                {"schemaVersion":"showroom-product-resource-package.v1","workbookPath":"product-data.xlsx","narrations":[
                  {"targetType":"PRODUCT","targetCode":"product_049","targetName":"旧产品","language":"ZH","scriptText":"旧产品中文讲解","audioDurationSeconds":30,"voice":"xiaoyun","audioAssetPath":"assets/narration/product/product_049/001-zh.wav"},
                  {"targetType":"PRODUCT","targetCode":"product_049","targetName":"旧产品","language":"EN","scriptText":"Legacy product English narration","audioDurationSeconds":30,"voice":"jenny","audioAssetPath":"assets/narration/product/product_049/002-en.wav"}
                ]}
                """, Map.of(
                "assets/narration/product/product_049/001-zh.wav", TEST_WAV_BYTES,
                "assets/narration/product/product_049/002-en.wav", TEST_WAV_BYTES));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> importPackage(packageBytes, "OVERWRITE"));

        assertTrue(exception.getMessage().contains("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_TARGET_MISMATCH"));
        assertTrue(exception.getMessage().contains("product_049"));
        assertTrue(exception.getMessage().contains("INT-12"));
    }

    @Test
    void importProductBaseWorkbookShouldKeepExistingProductCoverWhenWorkbookHasNoProductImage() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        seedPublishedAward("AWARD-001", "创新奖", "2026", "颁发单位",
                "/admin-api/infra/file/28/get/showroom/award/award-1.png");
        when(fileService.getFileContent(eq(28L), eq("showroom/award/award-1.png")))
                .thenReturn(ONE_PIXEL_PNG_BYTES);
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "BASE-WORKBOOK-PRODUCT", "底表封面保留产品", "Base Workbook Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "原BU"),
                        Map.entry("indication_content", "原适应症"),
                        Map.entry("core_selling_points", "原在售国家"),
                        Map.entry("model_specification", "原型号"),
                        Map.entry("registration_certificate", "原注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/base-product-cover.png"))))
                .revisionId(), 904L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildBaseWorkbookImportExcel(List.of(
                productMaterialRow("BASE-WORKBOOK-PRODUCT", "底表封面保留产品", "Base Workbook Product",
                        "创新展柜", "瑛泰", "已注册", "原BU", "中国", "原适应症", "原型号", "原注册证", "原在售国家")
        ), List.of(baseAwardRow(1, "创新奖", "2026", "颁发单位")), false);

        ShowroomAdminController.ShowroomProductImportRespVO response = importBaseWorkbookExcel(excelBytes);
        var current = contentService.requireCurrentProductRevision(product.productId());

        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        assertEquals("/admin-api/infra/file/28/get/showroom/product/base-product-cover.png",
                current.fields().get("cover_image"));
        verify(productCoverImageService, never()).uploadImportedCoverImage(eq("BASE-WORKBOOK-PRODUCT"),
                any(byte[].class), anyString(), anyString());
    }

    @Test
    void importProductBaseWorkbookShouldKeepExistingAwardCoverWhenWorkbookHasNoAwardImage() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "BASE-WORKBOOK-AWARD", "底表奖项封面保留产品", "Base Workbook Award Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/base-award-product.png"))))
                .revisionId(), 905L);
        mockProductPublishNarrations(product.productId(), product.revisionId());
        String existingAwardCover = "/admin-api/infra/file/28/get/showroom/award/base-award-cover.png";
        var awardDraft = contentService.saveAwardDraft(new ShowroomAwardDraft(null, "AWARD-001",
                "创新奖", "Innovation Award", "已有中文", "Existing English",
                "颁发单位", "2026", existingAwardCover));
        contentService.publishAwardRevision(awardDraft.revisionId(), 300L);
        mockAwardPublishNarrations(awardDraft.awardId(), awardDraft.revisionId());

        byte[] excelBytes = buildBaseWorkbookImportExcel(List.of(
                productMaterialRow("BASE-WORKBOOK-AWARD", "底表奖项封面保留产品", "Base Workbook Award Product",
                        "创新展柜", "瑛泰", "已注册", "新BU", "中国", "适应症", "型号", "注册证", "卖点")
        ), List.of(baseAwardRow(1, "创新奖", "2026", "颁发单位")), false);

        ShowroomAdminController.ShowroomProductImportRespVO response = importBaseWorkbookExcel(excelBytes);
        var currentAward = contentService.requireCurrentAwardRevision(awardDraft.awardId());

        assertEquals(1, response.awardSuccessCount());
        assertEquals(0, response.awardFailureCount());
        assertEquals(existingAwardCover, currentAward.fields().get("cover_image"));
        verify(productCoverImageService, never()).uploadImportedCoverImage(eq("AWARD-001"),
                any(byte[].class), anyString(), anyString());
    }

    @Test
    void importProductBaseWorkbookShouldFailWhenNewAwardHasNoCover() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "BASE-WORKBOOK-NEW-AWARD", "底表新奖项产品", "Base Workbook New Award Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/base-new-award.png"))))
                .revisionId(), 906L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildBaseWorkbookImportExcel(List.of(
                productMaterialRow("BASE-WORKBOOK-NEW-AWARD", "底表新奖项产品", "Base Workbook New Award Product",
                        "创新展柜", "瑛泰", "已注册", "新BU", "中国", "适应症", "型号", "注册证", "卖点")
        ), List.of(baseAwardRow(2, "新增奖项", "2027", "新颁发单位")), false);

        ShowroomAdminController.ShowroomProductImportRespVO response = importBaseWorkbookExcel(excelBytes);

        assertEquals(1, response.awardFailureCount());
        assertNotNull(response.awardFailures());
        assertTrue(response.awardFailures().get(0).reason().contains("新奖项 AWARD-002 缺少封面，无法创建"));
    }

    @Test
    void strictImportShouldStillFailWhenAwardSheetHasNoCoverImage() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "STRICT-NO-AWARD-COVER", "严格模式奖项缺图产品", "Strict No Award Cover Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/strict-no-award-cover.png"))))
                .revisionId(), 907L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildBaseWorkbookImportExcel(List.of(
                productMaterialRow("STRICT-NO-AWARD-COVER", "严格模式奖项缺图产品", "Strict No Award Cover Product",
                        "创新展柜", "瑛泰", "已注册", "新BU", "中国", "适应症", "型号", "注册证", "卖点")
        ), List.of(baseAwardRow(1, "创新奖", "2026", "颁发单位")), false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> importExcel(excelBytes));

        assertTrue(exception.getMessage().contains("SHOWROOM_AWARD_IMPORT_COVER_MISSING"));
        assertTrue(exception.getMessage().contains("E 列") || exception.getMessage().contains("必须提供封面"));
    }

    @Test
    void importProductExcelShouldReadReplacementProductListHeaders() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-NEW", "导入前产品", "Import Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("target_market", "旧在售国家"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("core_selling_points", "旧卖点文案"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("clinical_effect", "旧临床效果"),
                        Map.entry("fim_status", "旧FIM状态"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/cover-1.png"))))
                .revisionId(), 903L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("IMPORT-NEW", "导入后产品", "Imported Product", "创新展柜",
                        "瑛泰", "研发中", "心内介入BU", "中国;欧盟", "新适应症",
                        "新型号", "新注册证信息", "奖项A", "原材料A")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("IMPORT-NEW"), response.successProductCodes());

        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals(2, current.revisionNo());
        assertEquals("PUBLISHED", current.status());
        assertEquals("导入后产品", current.nameCn());
        assertEquals("Imported Product", current.nameEn());
        assertEquals(String.valueOf(ownerCompany.companyId()), current.fields().get("owner_company_id"));
        assertEquals("YINGTAI", current.fields().get("product_owner_type"));
        assertEquals("R_AND_D", current.fields().get("lifecycle_stage"));
        assertEquals("中国;欧盟", current.fields().get("target_market"));
        assertEquals("心内介入BU", current.fields().get("pipeline_layout"));
        assertEquals("新适应症", current.fields().get("indication_content"));
        assertEquals("旧卖点文案", current.fields().get("core_selling_points"));
        assertEquals("新型号", current.fields().get("model_specification"));
        assertEquals("新注册证信息", current.fields().get("registration_certificate"));
        assertEquals("旧临床效果", current.fields().get("clinical_effect"));
        assertEquals("旧FIM状态", current.fields().get("fim_status"));
        assertEquals("/admin-api/infra/file/28/get/showroom/product/cover-1.png",
                current.fields().get("cover_image"));
        assertFalse(current.fields().containsKey("hall_name"));
        assertFalse(current.fields().containsKey("awards"));
        assertFalse(current.fields().containsKey("raw_material_sheet"));
        verify(productNarrationCodexService, never()).generateSalesCountries(any(), any());
        verify(productNarrationCodexService, never()).translateZhToEn(anyString());
        verify(narrationService, never()).generateAudio(anyLong());
        verify(narrationService, times(2)).attachAudio(any(ShowroomNarrationAudioDraftCommand.class));
        verify(productCoverImageService, never()).generateCoverImage(anyString(), anyString());
        verify(imagePromptVersionService, never()).renderProductCoverPrompt(anyLong(), anyString(), anyString());
    }

    @Test
    void importProductExcelShouldUseChineseNameColumnAndEmbeddedProductImageAsCover() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-IMAGE", "导入前中文名", "Import Image Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("target_market", "旧在售国家"),
                        Map.entry("core_selling_points", "旧卖点文案"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 903L);
        mockProductPublishNarrations(product.productId(), product.revisionId());
        when(productCoverImageService.uploadImportedCoverImage(eq("IMPORT-IMAGE"), any(byte[].class),
                eq("png"), eq("image/png")))
                .thenReturn("/admin-api/infra/file/28/get/showroom/product/imported-cover.png");

        byte[] excelBytes = buildProductMaterialImportExcelWithExtras(true, "IMPORT-IMAGE",
                "中文名权威列", "Import Image Product", "中国", "卖点第一行\n卖点第二行");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());

        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals("中文名权威列", current.nameCn());
        assertEquals("Import Image Product", current.nameEn());
        assertEquals("新BU", current.fields().get("pipeline_layout"));
        assertEquals("新适应症", current.fields().get("indication_content"));
        assertEquals("中国", current.fields().get("target_market"));
        assertEquals("卖点第一行\n卖点第二行", current.fields().get("core_selling_points"));
        assertEquals("/admin-api/infra/file/28/get/showroom/product/imported-cover.png",
                current.fields().get("cover_image"));
        verify(productCoverImageService).uploadImportedCoverImage(eq("IMPORT-IMAGE"), any(byte[].class),
                eq("png"), eq("image/png"));
        verify(productCoverImageService, never()).generateCoverImage(anyString(), anyString());
    }

    @Test
    void importProductExcelShouldFailWhenLegacyProductColumnConflictsWithChineseNameColumn() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-PRODUCT-CONFLICT", "导入前冲突产品", "Import Conflict Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 904L);

        byte[] excelBytes = buildProductMaterialImportExcelWithLegacyProductColumn(false,
                "IMPORT-PRODUCT-CONFLICT", "中文名权威列", "冲突产品列", "Import Conflict Product",
                "中国", "卖点文案");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.successCount());
        assertEquals(1, response.failureCount());
        assertTrue(response.failures().get(0).reason().contains("产品"));
        assertTrue(response.failures().get(0).reason().contains("产品名-中文"));
        assertEquals("导入前冲突产品", contentService.requireCurrentProductRevision(product.productId()).nameCn());
    }

    @Test
    void importProductExcelShouldRejectOldChineseNameHeader() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-OLD-CN-HEADER", "导入前旧表头产品", "Import Old Header Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 904L);

        byte[] excelBytes = buildProductMaterialImportExcelWithOldChineseNameHeader(
                "IMPORT-OLD-CN-HEADER", "旧表头中文名", "Import Old Header Product");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> importExcel(excelBytes));

        assertTrue(exception.getMessage().contains("产品名-中文"));
        assertTrue(exception.getMessage().contains("产品-中文"));
    }

    @Test
    void importProductExcelShouldKeepChineseNameWhenAuthorityColumnBlank() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-BLANK-CN", "导入前中文名", "Import Blank Chinese Name",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("core_selling_points", "旧在售国家"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 904L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildProductMaterialImportExcelWithProductColumn(false, "IMPORT-BLANK-CN",
                "", "Import Blank Chinese Name");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        assertEquals("导入前中文名", contentService.requireCurrentProductRevision(product.productId()).nameCn());
    }

    @Test
    void importProductExcelShouldSkipWhenOnlyProductCodeProvidedAndKeepCurrentData() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-ONLY-CODE", "只填编码产品", "Only Code Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("target_market", "旧国家"),
                        Map.entry("core_selling_points", "旧卖点"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 904L);
        var hall = contentService.listHalls().stream()
                .filter(item -> "创新展柜".equals(item.name()))
                .findFirst()
                .orElseThrow();
        contentService.replaceHallProductMappings(hall.hallId(), List.of(
                new ShowroomHallProductMapping(product.productId(), 1)));

        byte[] excelBytes = buildProductMaterialImportExcelWithValues(false,
                "IMPORT-ONLY-CODE", "", "", "", "", "", "", "", "", "", "", "");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.successCount(), response.failures().toString());
        assertEquals(1, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("IMPORT-ONLY-CODE"), response.skippedProductCodes());

        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals(1, current.revisionNo());
        assertEquals("只填编码产品", current.nameCn());
        assertEquals("Only Code Product", current.nameEn());
        assertEquals("REGISTERED", current.fields().get("lifecycle_stage"));
        assertEquals("旧BU", current.fields().get("pipeline_layout"));
        assertEquals("旧适应症", current.fields().get("indication_content"));
        assertEquals("旧国家", current.fields().get("target_market"));
        assertEquals("旧卖点", current.fields().get("core_selling_points"));
        assertEquals("旧型号", current.fields().get("model_specification"));
        assertEquals("旧注册证", current.fields().get("registration_certificate"));
        assertEquals("/admin-api/infra/file/28/get/showroom/product/old-cover.png",
                current.fields().get("cover_image"));
        assertEquals(List.of(product.productId()), contentService.getHall(hall.hallId()).productMappings().stream()
                .map(ShowroomHallProductMapping::productId)
                .toList());
        verify(narrationService, never()).attachAudio(any(ShowroomNarrationAudioDraftCommand.class));
    }

    @Test
    void importProductExcelShouldKeepBlankTextFieldsAndApplyNonBlankFields() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-PARTIAL-BLANK", "局部空值产品", "Partial Blank Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "R_AND_D"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("target_market", "旧国家"),
                        Map.entry("core_selling_points", "旧卖点"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 904L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildProductMaterialImportExcelWithValues(false,
                "IMPORT-PARTIAL-BLANK", "", "", "", "", "", "", "新国家", "", "", "", "");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());

        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals(2, current.revisionNo());
        assertEquals("局部空值产品", current.nameCn());
        assertEquals("Partial Blank Product", current.nameEn());
        assertEquals("R_AND_D", current.fields().get("lifecycle_stage"));
        assertEquals("旧BU", current.fields().get("pipeline_layout"));
        assertEquals("旧适应症", current.fields().get("indication_content"));
        assertEquals("新国家", current.fields().get("target_market"));
        assertEquals("旧卖点", current.fields().get("core_selling_points"));
        assertEquals("旧型号", current.fields().get("model_specification"));
        assertEquals("旧注册证", current.fields().get("registration_certificate"));
        assertEquals("/admin-api/infra/file/28/get/showroom/product/old-cover.png",
                current.fields().get("cover_image"));
    }

    @Test
    void importProductExcelShouldOverwriteSameProductWhenRequested() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-SAME-OVERWRITE", "相同覆盖产品", "Same Overwrite Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("core_selling_points", "旧国家\n旧卖点"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 904L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildProductMaterialImportExcelWithValues(false,
                "IMPORT-SAME-OVERWRITE", "", "", "", "", "", "", "", "", "", "", "");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes, "OVERWRITE");

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("IMPORT-SAME-OVERWRITE"), response.successProductCodes());

        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals(2, current.revisionNo());
        assertEquals("相同覆盖产品", current.nameCn());
        assertEquals("Same Overwrite Product", current.nameEn());
        assertEquals("REGISTERED", current.fields().get("lifecycle_stage"));
        assertEquals("旧BU", current.fields().get("pipeline_layout"));
        assertEquals("旧适应症", current.fields().get("indication_content"));
        assertEquals("旧国家\n旧卖点", current.fields().get("core_selling_points"));
        assertEquals("旧型号", current.fields().get("model_specification"));
        assertEquals("旧注册证", current.fields().get("registration_certificate"));
        assertEquals("/admin-api/infra/file/28/get/showroom/product/old-cover.png",
                current.fields().get("cover_image"));
    }

    @Test
    void importProductExcelShouldOverwriteLegacyProductCodeFromProductList() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "IMPORT-LEGACY-OVERWRITE", "导入前旧编号产品", "Legacy Before Product", null,
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("target_market", "旧国家"),
                        Map.entry("core_selling_points", "旧卖点"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 904L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("IMPORT-LEGACY-OVERWRITE", "导入后旧编号产品", "Legacy After Product",
                        "创新展柜", "瑛泰", "已注册", "旧BU", "旧国家", "旧适应症",
                        "旧型号", "旧注册证", "旧编号奖项", "原材料")
        ), "product_legacy_overwrite");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes, "OVERWRITE");

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        assertEquals("product_legacy_overwrite", contentService.getProduct(product.productId()).legacyProductCode());
        assertEquals("product_legacy_overwrite",
                contentService.listProducts().stream()
                        .filter(item -> "product_legacy_overwrite".equals(item.legacyProductCode()))
                        .findFirst()
                        .orElseThrow()
                        .legacyProductCode());
    }

    @Test
    void importProductExcelShouldRejectUnknownSameProductAction() throws Exception {
        byte[] excelBytes = buildProductMaterialImportExcelWithValues(false,
                "IMPORT-SAME-ACTION", "", "", "", "", "", "", "", "", "", "", "");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> importExcel(excelBytes, "UNKNOWN"));

        assertTrue(exception.getMessage().contains("相同产品处理方式"));
    }

    @Test
    void importProductExcelShouldKeepCoverWhenProductImageColumnHasNoEmbeddedImage() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-NO-IMAGE", "导入前无图产品", "Import No Image Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("core_selling_points", "旧在售国家"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 904L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildProductMaterialImportExcelWithProductColumn(false, "IMPORT-NO-IMAGE",
                "产品列无图中文名", "Import No Image Product");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());

        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals("产品列无图中文名", current.nameCn());
        assertEquals("/admin-api/infra/file/28/get/showroom/product/old-cover.png",
                current.fields().get("cover_image"));
        verify(productCoverImageService, never()).uploadImportedCoverImage(eq("IMPORT-NO-IMAGE"), any(byte[].class),
                anyString(), anyString());
    }

    @Test
    void importProductExcelShouldPublishWhenOnlyEmbeddedProductImageChanges() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-IMAGE-ONLY", "图片不变更文字产品", "Image Only Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "新BU"),
                        Map.entry("indication_content", "新适应症"),
                        Map.entry("core_selling_points", "中国\n卖点文案"),
                        Map.entry("model_specification", "新型号"),
                        Map.entry("registration_certificate", "新注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 904L);
        mockProductPublishNarrations(product.productId(), product.revisionId());
        when(productCoverImageService.importedCoverImageMatchesCurrentCover(
                eq("/admin-api/infra/file/28/get/showroom/product/old-cover.png"), any(byte[].class)))
                .thenReturn(false);
        when(productCoverImageService.uploadImportedCoverImage(eq("IMPORT-IMAGE-ONLY"), any(byte[].class),
                eq("png"), eq("image/png")))
                .thenReturn("/admin-api/infra/file/28/get/showroom/product/imported-cover-only.png");

        byte[] excelBytes = buildProductMaterialImportExcelWithExtras(true, "IMPORT-IMAGE-ONLY",
                "图片不变更文字产品", "Image Only Product", "中国", "卖点文案");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.successCount(), response.failures().toString());
        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals(2, current.revisionNo());
        assertEquals("/admin-api/infra/file/28/get/showroom/product/imported-cover-only.png",
                current.fields().get("cover_image"));
    }

    @Test
    void importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-MISSING-CURRENT-COVER", "旧封面缺失产品", "Missing Current Cover Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("core_selling_points", "旧国家\n旧卖点"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image",
                                "/admin-api/infra/file/28/get/showroom/product/cover/missing-current-cover.png"))))
                .revisionId(), 904L);
        mockProductPublishNarrations(product.productId(), product.revisionId());
        when(productCoverImageService.importedCoverImageMatchesCurrentCover(
                eq("/admin-api/infra/file/28/get/showroom/product/cover/missing-current-cover.png"),
                any(byte[].class)))
                .thenThrow(new IllegalStateException("SHOWROOM_COVER_GENERATION_FAILED: "
                        + "failed to read current product cover image: "
                        + "/admin-api/infra/file/28/get/showroom/product/cover/missing-current-cover.png"));
        when(productCoverImageService.uploadImportedCoverImage(eq("IMPORT-MISSING-CURRENT-COVER"), any(byte[].class),
                eq("png"), eq("image/png")))
                .thenReturn("/admin-api/infra/file/28/get/showroom/product/cover/imported-replacement-cover.png");

        byte[] excelBytes = buildProductMaterialImportExcelWithValues(true,
                "IMPORT-MISSING-CURRENT-COVER", "", "", "", "", "", "", "", "", "", "", "");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("IMPORT-MISSING-CURRENT-COVER"), response.successProductCodes());

        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals(2, current.revisionNo());
        assertEquals("/admin-api/infra/file/28/get/showroom/product/cover/imported-replacement-cover.png",
                current.fields().get("cover_image"));
        verify(productCoverImageService).uploadImportedCoverImage(eq("IMPORT-MISSING-CURRENT-COVER"),
                any(byte[].class), eq("png"), eq("image/png"));
    }

    @Test
    void importProductExcelShouldSkipWhenOnlyImportedProductImageMatchesCurrentCover() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-SAME-COVER", "同图跳过产品", "Same Cover Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("core_selling_points", "旧国家\n旧卖点"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/current-cover.png"))))
                .revisionId(), 904L);
        when(productCoverImageService.importedCoverImageMatchesCurrentCover(
                eq("/admin-api/infra/file/28/get/showroom/product/current-cover.png"), any(byte[].class)))
                .thenReturn(true);
        when(productCoverImageService.uploadImportedCoverImage(eq("IMPORT-SAME-COVER"), any(byte[].class),
                eq("png"), eq("image/png")))
                .thenReturn("/admin-api/infra/file/28/get/showroom/product/current-cover.png");

        byte[] excelBytes = buildProductMaterialImportExcelWithValues(true,
                "IMPORT-SAME-COVER", "", "", "", "", "", "", "", "", "", "", "");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.successCount(), response.failures().toString());
        assertEquals(1, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("IMPORT-SAME-COVER"), response.skippedProductCodes());

        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals(1, current.revisionNo());
        assertEquals("同图跳过产品", current.nameCn());
        assertEquals("Same Cover Product", current.nameEn());
        assertEquals("REGISTERED", current.fields().get("lifecycle_stage"));
        assertEquals("旧BU", current.fields().get("pipeline_layout"));
        assertEquals("旧适应症", current.fields().get("indication_content"));
        assertEquals("旧国家\n旧卖点", current.fields().get("core_selling_points"));
        assertEquals("旧型号", current.fields().get("model_specification"));
        assertEquals("旧注册证", current.fields().get("registration_certificate"));
        assertEquals("/admin-api/infra/file/28/get/showroom/product/current-cover.png",
                current.fields().get("cover_image"));
        verify(productCoverImageService).importedCoverImageMatchesCurrentCover(
                eq("/admin-api/infra/file/28/get/showroom/product/current-cover.png"), any(byte[].class));
        verify(productCoverImageService, never()).uploadImportedCoverImage(eq("IMPORT-SAME-COVER"),
                any(byte[].class), eq("png"), eq("image/png"));
    }

    @Test
    void importProductExcelShouldCanonicalizeLegacyImportedCoverUrlEvenWhenContentMatches() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-LEGACY-COVER", "旧封面路径产品", "Legacy Cover Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("core_selling_points", "旧国家\n旧卖点"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image",
                                "/admin-api/infra/file/28/get/showroom/product/cover/20260531/product-IMPORT-LEGACY-COVER-imported-cover.png"))))
                .revisionId(), 904L);
        mockProductPublishNarrations(product.productId(), product.revisionId());
        when(productCoverImageService.importedCoverImageMatchesCurrentCover(
                eq("/admin-api/infra/file/28/get/showroom/product/cover/20260531/product-IMPORT-LEGACY-COVER-imported-cover.png"),
                any(byte[].class)))
                .thenReturn(true);
        when(productCoverImageService.importedCoverImageUrlMatchesContentHash(
                eq("/admin-api/infra/file/28/get/showroom/product/cover/20260531/product-IMPORT-LEGACY-COVER-imported-cover.png"),
                any(byte[].class)))
                .thenReturn(false);
        when(productCoverImageService.uploadImportedCoverImage(eq("IMPORT-LEGACY-COVER"), any(byte[].class),
                eq("png"), eq("image/png")))
                .thenReturn("/admin-api/infra/file/28/get/showroom/product/cover/20260531/product-IMPORT-LEGACY-COVER-imported-cover-b7a35f69730887ea.png");

        byte[] excelBytes = buildProductMaterialImportExcelWithValues(true,
                "IMPORT-LEGACY-COVER", "", "", "", "", "", "", "", "", "", "", "");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("IMPORT-LEGACY-COVER"), response.successProductCodes());

        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals(2, current.revisionNo());
        assertEquals("/admin-api/infra/file/28/get/showroom/product/cover/20260531/product-IMPORT-LEGACY-COVER-imported-cover-b7a35f69730887ea.png",
                current.fields().get("cover_image"));
        verify(productCoverImageService).uploadImportedCoverImage(eq("IMPORT-LEGACY-COVER"),
                any(byte[].class), eq("png"), eq("image/png"));
    }

    @Test
    void importProductExcelShouldPublishWhenOnlyImportedProductImageDiffersFromCurrentCover() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-DIFFERENT-COVER", "异图发布产品", "Different Cover Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("pipeline_layout", "旧BU"),
                        Map.entry("indication_content", "旧适应症"),
                        Map.entry("core_selling_points", "旧国家\n旧卖点"),
                        Map.entry("model_specification", "旧型号"),
                        Map.entry("registration_certificate", "旧注册证"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/current-cover.png"))))
                .revisionId(), 904L);
        mockProductPublishNarrations(product.productId(), product.revisionId());
        when(productCoverImageService.importedCoverImageMatchesCurrentCover(
                eq("/admin-api/infra/file/28/get/showroom/product/current-cover.png"), any(byte[].class)))
                .thenReturn(false);
        when(productCoverImageService.uploadImportedCoverImage(eq("IMPORT-DIFFERENT-COVER"), any(byte[].class),
                eq("png"), eq("image/png")))
                .thenReturn("/admin-api/infra/file/28/get/showroom/product/imported-different-cover.png");

        byte[] excelBytes = buildProductMaterialImportExcelWithValues(true,
                "IMPORT-DIFFERENT-COVER", "", "", "", "", "", "", "", "", "", "", "");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.skippedCount());
        assertEquals(0, response.failureCount());

        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals(2, current.revisionNo());
        assertEquals("异图发布产品", current.nameCn());
        assertEquals("Different Cover Product", current.nameEn());
        assertEquals("REGISTERED", current.fields().get("lifecycle_stage"));
        assertEquals("旧BU", current.fields().get("pipeline_layout"));
        assertEquals("旧适应症", current.fields().get("indication_content"));
        assertEquals("旧国家\n旧卖点", current.fields().get("core_selling_points"));
        assertEquals("旧型号", current.fields().get("model_specification"));
        assertEquals("旧注册证", current.fields().get("registration_certificate"));
        assertEquals("/admin-api/infra/file/28/get/showroom/product/imported-different-cover.png",
                current.fields().get("cover_image"));
        verify(productCoverImageService).importedCoverImageMatchesCurrentCover(
                eq("/admin-api/infra/file/28/get/showroom/product/current-cover.png"), any(byte[].class));
        verify(productCoverImageService).uploadImportedCoverImage(eq("IMPORT-DIFFERENT-COVER"),
                any(byte[].class), eq("png"), eq("image/png"));
    }

    @Test
    void importProductExcelShouldPreferSellingPointsCopyOverSalesCountryColumn() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-SELLING", "导入前卖点产品", "Import Selling Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("target_market", "旧国家"),
                        Map.entry("core_selling_points", "旧卖点"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 905L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildProductMaterialImportExcelWithExtras(false, "IMPORT-SELLING",
                "卖点产品中文名", "Import Selling Product", "中国", "新的卖点文案");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        var sellingCurrent = contentService.requireCurrentProductRevision(product.productId());
        assertEquals("中国", sellingCurrent.fields().get("target_market"));
        assertEquals("新的卖点文案", sellingCurrent.fields().get("core_selling_points"));
    }

    @Test
    void importProductExcelShouldUseSalesCountryWhenSellingPointsCopyBlank() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-SELLING-BLANK", "导入前空卖点产品", "Import Selling Blank Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("target_market", "旧国家"),
                        Map.entry("core_selling_points", "旧卖点"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png"))))
                .revisionId(), 906L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildProductMaterialImportExcelWithExtras(false, "IMPORT-SELLING-BLANK",
                "空卖点产品中文名", "Import Selling Blank Product", "中国;欧盟", "");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        var blankSellingCurrent = contentService.requireCurrentProductRevision(product.productId());
        assertEquals("中国;欧盟", blankSellingCurrent.fields().get("target_market"));
        assertEquals("旧卖点", blankSellingCurrent.fields().get("core_selling_points"));
    }

    @Test
    void importProductExcelShouldPersistLongSalesCountryList() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-LONG-MARKET", "导入前长国家产品", "Import Long Market Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("target_market", "旧国家"),
                        Map.entry("core_selling_points", "旧卖点"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-long-market-cover.png"))))
                .revisionId(), 907L);
        mockProductPublishNarrations(product.productId(), product.revisionId());
        String longSalesCountryList = String.join("|", List.of(
                "中国", "阿根廷", "阿联酋", "阿曼", "阿塞拜疆", "巴基斯坦", "巴西", "比利时", "德国", "多米尼加",
                "法国", "哈萨克斯坦", "韩国", "捷克", "肯尼亚", "黎巴嫩", "罗马尼亚", "马来西亚", "毛里求斯",
                "美国", "孟加拉", "孟加拉国", "缅甸", "尼泊尔", "塞尔维亚", "沙特", "斯洛伐克", "台湾",
                "泰国", "突尼斯", "土耳其", "危地马拉", "乌拉圭", "西班牙", "希腊", "香港", "新加坡",
                "以色列", "意大利", "印度", "印度尼西亚", "约旦", "越南", "智利"))
                + "|测试国家".repeat(40);
        assertTrue(longSalesCountryList.length() > 255);

        byte[] excelBytes = buildProductMaterialImportExcelWithExtras(false, "IMPORT-LONG-MARKET",
                "长国家产品中文名", "Import Long Market Product", longSalesCountryList, "");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals(longSalesCountryList, current.fields().get("target_market"));
        assertEquals("旧卖点", current.fields().get("core_selling_points"));
    }

    @Test
    void importProductExcelShouldKeepSellingPointSegmentsWhenTheirCellsBlank() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var blankSalesCountry = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-BLANK-COUNTRY", "空国家产品", "Blank Country Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("target_market", "旧国家"),
                        Map.entry("core_selling_points", "旧卖点"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-country-cover.png"))))
                .revisionId(), 907L);
        mockProductPublishNarrations(blankSalesCountry.productId(), blankSalesCountry.revisionId());
        var blankCopy = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-BLANK-COPY", "空卖点产品", "Blank Copy Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("target_market", "旧国家"),
                        Map.entry("core_selling_points", "旧卖点"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-copy-cover.png"))))
                .revisionId(), 908L);
        mockProductPublishNarrations(blankCopy.productId(), blankCopy.revisionId());

        byte[] excelBytes = buildProductMaterialImportExcelWithRows(false, List.of(
                productMaterialRow("IMPORT-BLANK-COUNTRY", "空国家产品", "Blank Country Product", "创新展柜",
                        "瑛泰", "已注册", "", "", "", "", "", "新卖点"),
                productMaterialRow("IMPORT-BLANK-COPY", "空卖点产品", "Blank Copy Product", "创新展柜",
                        "瑛泰", "已注册", "", "新国家", "", "", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(2, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        var blankCountryCurrent = contentService.requireCurrentProductRevision(blankSalesCountry.productId());
        assertEquals("旧国家", blankCountryCurrent.fields().get("target_market"));
        assertEquals("新卖点", blankCountryCurrent.fields().get("core_selling_points"));
        var blankCopyCurrent = contentService.requireCurrentProductRevision(blankCopy.productId());
        assertEquals("新国家", blankCopyCurrent.fields().get("target_market"));
        assertEquals("旧卖点", blankCopyCurrent.fields().get("core_selling_points"));
    }

    @Test
    void importProductExcelShouldReplaceHallMappingsFromReplacementProductList() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product001 = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "product_001", "三通旋塞-OFF", "Manifold for Single use-OFF",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED"))).revisionId(), 903L);
        var product002 = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "product_002", "三通旋塞-ON", "Manifold for Single use-ON",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED"))).revisionId(), 904L);
        mockProductPublishNarrations(product001.productId(), product001.revisionId());
        var hall = contentService.createHall("hall_01", "心内介植入展柜", "Cardiology Showcase", "", "");
        contentService.replaceHallProductMappings(hall.hallId(), List.of(
                new ShowroomHallProductMapping(product001.productId(), 1),
                new ShowroomHallProductMapping(product002.productId(), 2)
        ));

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("product_001", "三通旋塞", "Manifold for Single use", "心内介植入展柜",
                        "瑛泰医疗", "已注册", "心内介入BU", "中国", "适应症",
                        "型号", "注册证信息", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        assertEquals(List.of(product001.productId()), contentService.getHall(hall.hallId()).productMappings().stream()
                .map(ShowroomHallProductMapping::productId)
                .toList());
        assertEquals("三通旋塞", contentService.requireCurrentProductRevision(product001.productId()).nameCn());
    }

    @Test
    void importBaseWorkbookShouldResolveLegacyProductCodeToCurrentIntProduct() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "INT-12", "导入前 INT 产品", "Before INT Product", "product_012",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED"))).revisionId(), 903L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("product_012", "导入后 INT 产品", "After INT Product", "创新展柜",
                        "瑛泰医疗", "已注册", "心内介入BU", "中国", "适应症",
                        "型号", "注册证信息", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importBaseWorkbookExcel(excelBytes);

        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("INT-12"), response.successProductCodes());
        assertEquals("导入后 INT 产品", contentService.requireCurrentProductRevision(product.productId()).nameCn());
        assertEquals("INT-12", contentService.getProduct(product.productId()).productCode());
        assertEquals("product_012", contentService.getProduct(product.productId()).legacyProductCode());
        var page = withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO("product_012", 1, 10)).getCheckedData());
        assertEquals(1, page.getList().size());
        assertEquals("INT-12", page.getList().get(0).productCode());
        assertEquals("product_012", page.getList().get(0).legacyProductCode());
    }

    @Test
    void importBaseWorkbookShouldSkipLegacyProductCodeWithoutExplicitMappingEvenWhenNameMatches() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "INT-38", "超滑导丝", "Hydrophilic Guide Wire", null,
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED"))).revisionId(), 905L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("product_037", "超滑导丝", "Hydrophilic Guide Wire", "创新展柜",
                        "瑛泰医疗", "已注册", "心内介入BU", "中国", "适应症",
                        "型号", "注册证信息", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importBaseWorkbookExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.successCount());
        assertEquals(1, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("product_037"), response.skippedProductCodes());
        assertEquals("超滑导丝", contentService.requireCurrentProductRevision(product.productId()).nameCn());
        assertEquals("INT-38", contentService.getProduct(product.productId()).productCode());
        assertNull(contentService.getProduct(product.productId()).legacyProductCode());
    }

    @Test
    void importBaseWorkbookShouldSkipDuplicateChineseNameWithoutExplicitLegacyMapping() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var fastFlator = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "INT-14", "按压式球囊扩张压力泵", "FastFlator™ Inflation Device", null,
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED"))).revisionId(), 906L);
        var inflationDevice = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "INT-15", "按压式球囊扩张压力泵", "Inflation Device II", null,
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED"))).revisionId(), 907L);
        mockProductPublishNarrations(fastFlator.productId(), fastFlator.revisionId());
        mockProductPublishNarrations(inflationDevice.productId(), inflationDevice.revisionId());

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("product_015", "按压式球囊扩张压力泵", "Inflation Device II", "创新展柜",
                        "瑛泰医疗", "已注册", "心内介入BU", "中国", "适应症",
                        "型号", "注册证信息", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importBaseWorkbookExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.successCount());
        assertEquals(1, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("product_015"), response.skippedProductCodes());
        assertNull(contentService.getProduct(inflationDevice.productId()).legacyProductCode());
        assertNull(contentService.getProduct(fastFlator.productId()).legacyProductCode());
    }

    @Test
    void importBaseWorkbookShouldSkipDuplicateSameNameGroupWithoutExplicitLegacyMapping() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var firstBandage = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "INT-18", "股动脉止血带", "Femoral Pressure Bandage", null,
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED"))).revisionId(), 908L);
        var secondBandage = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "INT-19", "股动脉止血带", "Femoral Pressure Bandage", null,
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED"))).revisionId(), 909L);
        mockProductPublishNarrations(firstBandage.productId(), firstBandage.revisionId());
        mockProductPublishNarrations(secondBandage.productId(), secondBandage.revisionId());

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("product_018", "股动脉止血带", "Femoral Pressure Bandage", "创新展柜",
                        "瑛泰医疗", "已注册", "心内介入BU", "中国", "适应症",
                        "型号", "注册证信息", "", ""),
                replacementRow("product_019", "股动脉止血带", "Femoral Pressure Bandage", "创新展柜",
                        "瑛泰医疗", "已注册", "心内介入BU", "中国", "适应症",
                        "型号", "注册证信息", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importBaseWorkbookExcel(excelBytes);

        assertEquals(2, response.totalRows());
        assertEquals(0, response.successCount());
        assertEquals(2, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("product_018", "product_019"), response.skippedProductCodes());
        assertNull(contentService.getProduct(firstBandage.productId()).legacyProductCode());
        assertNull(contentService.getProduct(secondBandage.productId()).legacyProductCode());
    }

    @Test
    void importBaseWorkbookShouldSkipWhenLegacyProductCodeHasNoMapping() throws Exception {
        seedOwnerCompany();

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("product_999", "无映射旧编号产品", "Legacy Product Without Mapping", "创新展柜",
                        "瑛泰医疗", "已注册", "心内介入BU", "中国", "适应症",
                        "型号", "注册证信息", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importBaseWorkbookExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.successCount());
        assertEquals(1, response.skippedCount());
        assertEquals(List.of("product_999"), response.skippedProductCodes());
        assertEquals(0, response.failureCount());
    }

    @Test
    void importBaseWorkbookShouldRejectOldProductCodeWhenItDoesNotResolveToCurrentIntProduct() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var oldProduct = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "product_012", "旧 product 产品", "Old Product",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED"))).revisionId(), 904L);
        mockProductPublishNarrations(oldProduct.productId(), oldProduct.revisionId());

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("product_012", "不应导入旧编号产品", "Should Not Import Old Product", "创新展柜",
                        "瑛泰医疗", "已注册", "心内介入BU", "中国", "适应症",
                        "型号", "注册证信息", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importBaseWorkbookExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.successCount());
        assertEquals(1, response.failureCount());
        assertEquals("product_012", response.failures().get(0).productCode());
        assertTrue(response.failures().get(0).reason()
                .contains("SHOWROOM_PRODUCT_LEGACY_CODE_INT_COUNT_MISMATCH"));
        assertEquals("旧 product 产品", contentService.requireCurrentProductRevision(oldProduct.productId()).nameCn());
    }

    @Test
    void importProductExcelShouldKeepHallMappingWhenHallNameBlank() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product001 = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-HALL-BLANK", "空展柜产品", "Blank Hall Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/hall-blank.png"))))
                .revisionId(), 903L);
        mockProductPublishNarrations(product001.productId(), product001.revisionId());
        var product002 = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-HALL-OTHER", "同柜产品", "Other Hall Product",
                Map.ofEntries(
                        Map.entry("owner_company_id", String.valueOf(ownerCompany.companyId())),
                        Map.entry("product_owner_type", "YINGTAI"),
                        Map.entry("lifecycle_stage", "REGISTERED"),
                        Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/product/hall-other.png"))))
                .revisionId(), 904L);
        var hall = contentService.listHalls().stream()
                .filter(item -> "创新展柜".equals(item.name()))
                .findFirst()
                .orElseThrow();
        contentService.replaceHallProductMappings(hall.hallId(), List.of(
                new ShowroomHallProductMapping(product001.productId(), 1),
                new ShowroomHallProductMapping(product002.productId(), 2)));

        byte[] excelBytes = buildProductMaterialImportExcelWithValues(false,
                "IMPORT-HALL-BLANK", "空展柜产品更新", "Blank Hall Product", "",
                "瑛泰", "已注册", "", "", "", "", "", "");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        assertEquals("空展柜产品更新", contentService.requireCurrentProductRevision(product001.productId()).nameCn());
        assertEquals(List.of(product001.productId(), product002.productId()),
                contentService.getHall(hall.hallId()).productMappings().stream()
                        .map(ShowroomHallProductMapping::productId)
                        .toList());
    }

    @Test
    void importProductExcelShouldFailOnOwnerCompanyMismatch() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-MISMATCH", "导入前不匹配产品", "Mismatch Product",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED")))
                .revisionId(), 904L);

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("IMPORT-MISMATCH", "导入后不匹配产品", "Mismatch Product", "创新展柜",
                        "其他公司", "已注册", "心内介入BU", "中国", "适应症",
                        "型号", "注册证信息", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.successCount());
        assertEquals(1, response.failureCount());
        assertEquals("IMPORT-MISMATCH", response.failures().get(0).productCode());
        assertTrue(response.failures().get(0).reason().contains("持证公司"));
        assertTrue(response.failures().get(0).reason().contains("其他公司"));
        assertTrue(response.failures().get(0).reason().contains("瑛泰医疗"));
        assertEquals(1, contentService.requireCurrentProductRevision(product.productId()).revisionNo());
    }

    @Test
    void importProductExcelShouldResolveOwnerCompanyFromFormalRowWhenCurrentOwnerIdIsStale() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        contentService.createHall("formal_hall_01", "心内介植入展柜", "Cardiac Intervention Implant Showcase", "", "");
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "product_001", "三通旋塞-OFF", "Manifold for Single use-OFF",
                Map.of(
                        "owner_company_id", "124",
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "core_selling_points", "旧在售国家",
                        "registration_certificate", "旧注册证")))
                .revisionId(), 905L);
        mockProductPublishNarrations(product.productId(), product.revisionId());

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("product_001", "一次性使用三通旋塞", "Manifold for Single use-OFF", "心内介植入展柜",
                        "瑛泰", "已注册", "神经血管BU / Neurovascular BU", "中国.", "用于介入手术中压力监测管路中的连接、输液和通路切换。\nON-阀门开关所指方向开放",
                        "19", "注册证名称：一次性使用三通旋塞\n注册证号：沪械注准20242030122\n生效时间：2024.5.30",
                        "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("product_001"), response.successProductCodes());
        var current = contentService.requireCurrentProductRevision(product.productId());
        assertEquals("一次性使用三通旋塞", current.nameCn());
        assertEquals(String.valueOf(ownerCompany.companyId()), current.fields().get("owner_company_id"));
        assertEquals("YINGTAI", current.fields().get("product_owner_type"));
    }

    @Test
    void importProductExcelShouldParseRegisteredAndInDevelopment() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var registered = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-REGISTERED", "注册产品", "Registered Product",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "R_AND_D")))
                .revisionId(), 905L);
        mockProductPublishNarrations(registered.productId(), registered.revisionId());
        var inDevelopment = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-RD", "研发产品", "R&D Product",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED")))
                .revisionId(), 906L);
        mockProductPublishNarrations(inDevelopment.productId(), inDevelopment.revisionId());

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("IMPORT-REGISTERED", "注册产品更新", "Registered Product", "创新展柜",
                        "瑛泰医疗", "已注册", "结构BU", "中国", "适应症A", "型号A",
                        "注册证A", "", ""),
                replacementRow("IMPORT-RD", "研发产品更新", "R&D Product", "创新展柜",
                        "瑛泰医疗", "研发中", "研发BU", "美国", "适应症B", "型号B",
                        "注册证B", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(2, response.successCount());
        assertEquals("REGISTERED", contentService.requireCurrentProductRevision(registered.productId())
                .fields().get("lifecycle_stage"));
        assertEquals("R_AND_D", contentService.requireCurrentProductRevision(inDevelopment.productId())
                .fields().get("lifecycle_stage"));
    }

    @Test
    void importProductExcelShouldCreateInDevelopmentProductWithoutOwnerCompany() throws Exception {
        seedOwnerCompany();
        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("IMPORT-RD-NO-OWNER", "\u7814\u53d1\u4e2d\u7f3a\u5c11\u6301\u8bc1\u516c\u53f8\u4ea7\u54c1",
                        "R&D Product Without Owner", "\u521b\u65b0\u5c55\u67dc",
                        "", "\u7814\u53d1\u4e2d", "", "", "", "",
                        "", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount());
        assertEquals(0, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("IMPORT-RD-NO-OWNER"), response.successProductCodes());

        var created = contentService.listProducts().stream()
                .filter(item -> "IMPORT-RD-NO-OWNER".equals(item.productCode()))
                .findFirst()
                .orElseThrow();
        var createdCurrent = contentService.requireCurrentProductRevision(created.productId());
        assertEquals(1, createdCurrent.revisionNo());
        assertEquals("PUBLISHED", createdCurrent.status());
        assertEquals("\u7814\u53d1\u4e2d\u7f3a\u5c11\u6301\u8bc1\u516c\u53f8\u4ea7\u54c1", createdCurrent.nameCn());
        assertEquals("R&D Product Without Owner", createdCurrent.nameEn());
        assertEquals("", responseOrEmpty(createdCurrent.fields().get("owner_company_id")));
        assertEquals("", responseOrEmpty(createdCurrent.fields().get("product_owner_type")));
        assertEquals("R_AND_D", createdCurrent.fields().get("lifecycle_stage"));
        assertTrue(contentService.getProduct(created.productId()).incomplete());

        var importHall = contentService.listHalls().stream()
                .filter(item -> "\u521b\u65b0\u5c55\u67dc".equals(item.name()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(created.productId()),
                contentService.getHall(importHall.hallId()).productMappings().stream()
                        .map(ShowroomHallProductMapping::productId)
                        .toList());
    }

    @Test
    void importProductExcelShouldCreateMissingProductWithLegacyProductCode() throws Exception {
        seedOwnerCompany();
        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("INT-3", "Y\u578b\u8fde\u63a5\u5668", "Y-Connector", "\u521b\u65b0\u5c55\u67dc",
                        "\u745b\u6cf0\u533b\u7597", "\u5df2\u6ce8\u518c", "\u5fc3\u5185\u4ecb\u5165BU", "\u4e2d\u56fd", "\u9002\u5e94\u75c7",
                        "\u578b\u53f7", "\u6ce8\u518c\u8bc1\u4fe1\u606f", "", "")
        ), "product_003");

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        var created = contentService.listProducts().stream()
                .filter(item -> "INT-3".equals(item.productCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("product_003", created.legacyProductCode());
        assertEquals("Y\u578b\u8fde\u63a5\u5668", contentService.requireCurrentProductRevision(created.productId()).nameCn());
    }

    @Test
    void importProductExcelShouldReviveSoftDeletedProductWithSameCode() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var deletedProduct = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-DELETED-001", "已删除产品", "Deleted Product",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/product/deleted-cover.png")))
                .revisionId(), 904L);
        Long deletedProductId = deletedProduct.productId();
        contentService.deleteProduct(deletedProductId);

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("IMPORT-DELETED-001", "复活后的产品", "Revived Product", "创新展柜",
                        "瑛泰医疗", "已注册", "心内介入BU", "中国", "新适应症", "新型号",
                        "新注册证", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("IMPORT-DELETED-001"), response.successProductCodes());

        var revived = contentService.listProducts().stream()
                .filter(item -> "IMPORT-DELETED-001".equals(item.productCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(deletedProductId, revived.productId());
        var current = contentService.requireCurrentProductRevision(revived.productId());
        assertEquals("复活后的产品", current.nameCn());
        assertEquals("Revived Product", current.nameEn());
        assertEquals(2, current.revisionNo());
        assertEquals("REGISTERED", current.fields().get("lifecycle_stage"));
    }

    @Test
    void importProductExcelShouldOnlyReadProductListSheet() throws Exception {
        seedOwnerCompany();
        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("IMPORT-SHEET-ONLY", "只读产品列表", "Only Product Sheet", "创新展柜",
                        "瑛泰医疗", "已注册", "心内介入BU", "中国", "适应症", "型号",
                        "注册证", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.successCount(), response.failures().toString());
        assertEquals(0, response.failureCount());
        assertEquals(1, response.awardTotalRows());
        assertEquals(1, response.awardSuccessCount());
        assertEquals(0, response.awardFailureCount());
    }

    @Test
    void importProductExcelShouldPublishChangedRowsSkipUnchangedRowsAndPreserveMediaFields() throws Exception {
        ShowroomCompanySnapshot ownerCompany = seedOwnerCompany();
        var changed = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-001", "导入前产品一", "Import Product One",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "target_market", "旧市场",
                        "pipeline_layout", "旧布局",
                        "registration_certificate", "旧注册证",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/product/cover-1.png")))
                .revisionId(), 903L);
        mockProductPublishNarrations(changed.productId(), changed.revisionId());
        var unchanged = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, "IMPORT-002", "导入前产品二", "Import Product Two",
                Map.of(
                        "owner_company_id", String.valueOf(ownerCompany.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "target_market", "保持不变",
                        "registration_certificate", "保持不变注册证")))
                .revisionId(), 904L);

        byte[] excelBytes = buildReplacementHeaderImportExcel(List.of(
                replacementRow("IMPORT-001", "导入后产品一", "Import Product One", "创新展柜",
                        "瑛泰医疗", "已注册", "新BU", "中国", "新适应症", "新型号",
                        "新注册证", "", ""),
                replacementRow("IMPORT-002", "导入前产品二", "Import Product Two", "创新展柜",
                        "瑛泰医疗", "已注册", "", "", "", "", "保持不变注册证", "", ""),
                replacementRow("IMPORT-404", "不存在产品", "Missing Product", "创新展柜",
                        "瑛泰医疗", "已注册", "无", "", "", "", "", "", "")
        ));

        ShowroomAdminController.ShowroomProductImportRespVO response = importExcel(excelBytes);

        assertEquals(3, response.totalRows());
        assertEquals(2, response.successCount());
        assertEquals(1, response.skippedCount());
        assertEquals(0, response.failureCount());
        assertEquals(List.of("IMPORT-001", "IMPORT-404"), response.successProductCodes());
        assertEquals(List.of("IMPORT-002"), response.skippedProductCodes());

        var changedCurrent = contentService.requireCurrentProductRevision(changed.productId());
        assertEquals(2, changedCurrent.revisionNo());
        assertEquals("PUBLISHED", changedCurrent.status());
        assertEquals("导入后产品一", changedCurrent.nameCn());
        assertEquals("中国", responseOrEmpty(changedCurrent.fields().get("target_market")));
        assertEquals("新BU", changedCurrent.fields().get("pipeline_layout"));
        assertEquals("", responseOrEmpty(changedCurrent.fields().get("core_selling_points")));
        assertEquals("新注册证", changedCurrent.fields().get("registration_certificate"));
        assertEquals("/admin-api/infra/file/28/get/showroom/product/cover-1.png",
                changedCurrent.fields().get("cover_image"));

        var unchangedCurrent = contentService.requireCurrentProductRevision(unchanged.productId());
        assertEquals(1, unchangedCurrent.revisionNo());
        assertEquals("导入前产品二", unchangedCurrent.nameCn());

        var created = contentService.listProducts().stream()
                .filter(item -> "IMPORT-404".equals(item.productCode()))
                .findFirst()
                .orElseThrow();
        var createdCurrent = contentService.requireCurrentProductRevision(created.productId());
        assertEquals(1, createdCurrent.revisionNo());
        assertEquals("PUBLISHED", createdCurrent.status());
        assertEquals("不存在产品", createdCurrent.nameCn());
        assertEquals("Missing Product", createdCurrent.nameEn());
        assertEquals(String.valueOf(ownerCompany.companyId()), createdCurrent.fields().get("owner_company_id"));
        assertEquals("YINGTAI", createdCurrent.fields().get("product_owner_type"));
        assertEquals("REGISTERED", createdCurrent.fields().get("lifecycle_stage"));
        assertEquals("无", createdCurrent.fields().get("pipeline_layout"));
        var importHall = contentService.listHalls().stream()
                .filter(item -> "创新展柜".equals(item.name()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(changed.productId(), unchanged.productId(), created.productId()),
                contentService.getHall(importHall.hallId()).productMappings().stream()
                        .map(ShowroomHallProductMapping::productId)
                        .toList());

        verify(productNarrationCodexService, never()).generateSalesCountries(any(), any());
        verify(productNarrationCodexService, never()).translateZhToEn(anyString());
        verify(narrationService, never()).generateAudio(anyLong());
        verify(narrationService, times(2)).attachAudio(any(ShowroomNarrationAudioDraftCommand.class));
        verify(productCoverImageService, never()).generateCoverImage(anyString(), anyString());
        verify(imagePromptVersionService, never()).renderProductCoverPrompt(anyLong(), anyString(), anyString());
    }

    private ShowroomCompanySnapshot seedOwnerCompany() {
        var revision = contentService.publishCompanyRevision(contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "瑛泰医疗", "Yingtai Medical", Map.of())).revisionId(), 900L);
        contentService.createHall("import_hall", "创新展柜", "Innovation Showcase", "", "");
        return contentService.getCompany(revision.companyId());
    }

    private void mockProductPublishNarrations(Long productId, Long sourceRevisionId) {
        ShowroomNarrationKey zhKey = new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, productId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);
        ShowroomNarrationKey enKey = new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, productId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN);
        ShowroomNarrationVersion zh = registerNarration(liveNarration(zhKey, sourceRevisionId, "产品中文讲解"));
        ShowroomNarrationVersion en = registerNarration(liveNarration(enKey, sourceRevisionId,
                "English product narration"));
        when(narrationService.latest(zhKey)).thenReturn(Optional.of(zh));
        when(narrationService.latest(enKey)).thenReturn(Optional.of(en));
        when(narrationService.latest(zhKey, sourceRevisionId)).thenReturn(Optional.of(zh));
        when(narrationService.latest(enKey, sourceRevisionId)).thenReturn(Optional.of(en));
        when(narrationService.latestPublished(zhKey, sourceRevisionId)).thenReturn(Optional.of(zh));
        when(narrationService.latestPublished(enKey, sourceRevisionId)).thenReturn(Optional.of(en));
    }

    private void mockProductLatestDraftWithoutAudio(Long productId, Long sourceRevisionId) {
        ShowroomNarrationKey zhKey = new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, productId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);
        ShowroomNarrationKey enKey = new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, productId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN);
        ShowroomNarrationVersion zh = draftNarration(zhKey, sourceRevisionId, "产品中文讲解-未生成音频草稿");
        ShowroomNarrationVersion en = draftNarration(enKey, sourceRevisionId,
                "English product narration draft without audio");
        when(narrationService.latest(zhKey, sourceRevisionId)).thenReturn(Optional.of(zh));
        when(narrationService.latest(enKey, sourceRevisionId)).thenReturn(Optional.of(en));
    }

    private void mockAwardPublishNarrations(Long awardId, Long sourceRevisionId) {
        ShowroomNarrationKey zhKey = new ShowroomNarrationKey(
                ShowroomNarrationTargetType.AWARD, awardId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);
        ShowroomNarrationKey enKey = new ShowroomNarrationKey(
                ShowroomNarrationTargetType.AWARD, awardId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN);
        ShowroomNarrationVersion zh = registerNarration(liveNarration(zhKey, sourceRevisionId, "奖项中文讲解"));
        ShowroomNarrationVersion en = registerNarration(liveNarration(enKey, sourceRevisionId,
                "English award narration"));
        when(narrationService.latest(zhKey)).thenReturn(Optional.of(zh));
        when(narrationService.latest(enKey)).thenReturn(Optional.of(en));
        when(narrationService.latest(zhKey, sourceRevisionId)).thenReturn(Optional.of(zh));
        when(narrationService.latest(enKey, sourceRevisionId)).thenReturn(Optional.of(en));
        when(narrationService.latestPublished(zhKey, sourceRevisionId)).thenReturn(Optional.of(zh));
        when(narrationService.latestPublished(enKey, sourceRevisionId)).thenReturn(Optional.of(en));
    }

    private ShowroomNarrationVersion liveNarration(ShowroomNarrationKey key, Long sourceRevisionId, String scriptText) {
        return new ShowroomNarrationVersion(
                narrationIds.incrementAndGet(),
                key,
                sourceRevisionId,
                1,
                scriptText,
                narrationIds.incrementAndGet(),
                30,
                null,
                ShowroomNarrationGenerationStatus.AUDIO_GENERATED,
                ShowroomNarrationStatus.PUBLISHED,
                false,
                Instant.now(),
                Instant.now(),
                true
        );
    }

    private ShowroomNarrationVersion draftNarration(ShowroomNarrationKey key, Long sourceRevisionId,
                                                    String scriptText) {
        return new ShowroomNarrationVersion(
                narrationIds.incrementAndGet(),
                key,
                sourceRevisionId,
                2,
                scriptText,
                null,
                null,
                null,
                ShowroomNarrationGenerationStatus.SCRIPT_GENERATED,
                ShowroomNarrationStatus.DRAFT,
                false,
                Instant.now(),
                null,
                false
        );
    }

    private ShowroomNarrationVersion registerNarration(ShowroomNarrationVersion version) {
        narrationVersions.put(version.id(), version);
        if (version.audioFileId() != null) {
            FileDO file = new FileDO();
            file.setId(version.audioFileId());
            file.setConfigId(28L);
            file.setPath("showroom/narration/test-" + version.audioFileId() + ".wav");
            when(fileMapper.selectById(version.audioFileId())).thenReturn(file);
            try {
                when(fileService.getFileContent(file.getConfigId(), file.getPath())).thenReturn(TEST_WAV_BYTES);
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
        return version;
    }

    private ShowroomAdminController.ShowroomProductImportRespVO importExcel(byte[] excelBytes) throws Exception {
        return importExcel(excelBytes, "SKIP");
    }

    private ShowroomAdminController.ShowroomProductImportRespVO importExcel(byte[] excelBytes,
                                                                            String sameProductAction) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "showroom-product-import.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );
        return withLoginUser(300L, () -> adminController.importProductExcel(file, sameProductAction).getCheckedData());
    }

    private ShowroomAdminController.ShowroomProductImportRespVO importPackage(byte[] packageBytes,
                                                                              String sameProductAction) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "showroom-product-resource-package.zip",
                "application/zip",
                packageBytes
        );
        return withLoginUser(300L, () -> adminController.importProductExcel(file, sameProductAction).getCheckedData());
    }

    private ShowroomAdminController.ShowroomProductImportRespVO importBaseWorkbookExcel(byte[] excelBytes)
            throws Exception {
        return importBaseWorkbookExcel(excelBytes, "SKIP");
    }

    private ShowroomAdminController.ShowroomProductImportRespVO importBaseWorkbookExcel(byte[] excelBytes,
                                                                                        String sameProductAction)
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "showroom-base-workbook-import.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );
        return withLoginUser(300L,
                () -> adminController.importProductBaseWorkbook(file, sameProductAction).getCheckedData());
    }

    private String resolveShowroomImportProductCode(String workbookProductCode) {
        if (workbookProductCode == null) {
            return null;
        }
        return contentService.listProducts().stream()
                .filter(product -> workbookProductCode.equals(product.legacyProductCode()))
                .map(ShowroomProductSnapshot::productCode)
                .findFirst()
                .orElse(workbookProductCode);
    }

    private String resolveShowroomImportNameCn(String productCode, String workbookNameCn) {
        if (workbookNameCn != null && !workbookNameCn.isBlank() && !workbookNameCn.startsWith("主数据-")) {
            return workbookNameCn;
        }
        return contentService.listProducts().stream()
                .filter(product -> productCode.equals(product.productCode())
                        || productCode.equals(product.legacyProductCode()))
                .filter(product -> product.currentRevisionId().isPresent())
                .findFirst()
                .map(product -> contentService.requireCurrentProductRevision(product.productId()).nameCn())
                .orElse(workbookNameCn);
    }

    private String resolveShowroomImportNameEn(String productCode, String workbookNameEn) {
        if (workbookNameEn != null && !workbookNameEn.isBlank() && !workbookNameEn.startsWith("Master ")) {
            return workbookNameEn;
        }
        return contentService.listProducts().stream()
                .filter(product -> productCode.equals(product.productCode())
                        || productCode.equals(product.legacyProductCode()))
                .filter(product -> product.currentRevisionId().isPresent())
                .findFirst()
                .map(product -> contentService.requireCurrentProductRevision(product.productId()).nameEn())
                .orElse(workbookNameEn);
    }

    private static ReplacementProductRow replacementRow(String productCode, String nameCn, String nameEn,
                                                        String hallName, String ownerCompanyName,
                                                        String lifecycleStage, String pipelineLayout,
                                                        String coreSellingPoints, String indicationContent,
                                                        String modelSpecification, String registrationCertificate,
                                                        String awards, String rawMaterialSheet) {
        return new ReplacementProductRow(productCode, nameCn, nameEn, hallName, ownerCompanyName, lifecycleStage,
                pipelineLayout, coreSellingPoints, indicationContent, modelSpecification, registrationCertificate,
                awards, rawMaterialSheet);
    }

    private static byte[] buildReplacementHeaderImportExcel(List<ReplacementProductRow> rows) throws IOException {
        return buildReplacementHeaderImportExcel(rows, "");
    }

    private static byte[] buildReplacementHeaderImportExcel(List<ReplacementProductRow> rows,
                                                            String legacyProductCode) throws IOException {
        String[] headers = {
                "展品编码", "旧产品编号", "产品名-中文", "产品名-英文", "展柜名称", "持证公司", "在售/在研", "BU",
                "在售国家", "适应症", "型号规格", "注册证信息", "卖点文案", "产品图", "奖项", "原材料表单"
        };
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("产品列表");
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                ReplacementProductRow source = rows.get(rowIndex);
                org.apache.poi.ss.usermodel.Row excelRow = sheet.createRow(rowIndex + 1);
                String[] values = {
                        source.productCode(), legacyProductCode, source.nameCn(), source.nameEn(), source.hallName(),
                        source.ownerCompanyName(), source.lifecycleStage(), source.pipelineLayout(),
                        source.coreSellingPoints(), source.indicationContent(), source.modelSpecification(),
                        source.registrationCertificate(), "", "", source.awards(), source.rawMaterialSheet()
                };
                for (int cellIndex = 0; cellIndex < values.length; cellIndex++) {
                    excelRow.createCell(cellIndex).setCellValue(values[cellIndex] == null ? "" : values[cellIndex]);
                }
            }
            addProductMasterSheet(workbook, rows.stream()
                    .map(row -> productMasterSheetRow(row.productCode(), row.nameCn(), row.nameEn(),
                            row.modelSpecification(), row.ownerCompanyName()))
                    .toList());
            addDefaultAwardSheet(workbook);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildProductMaterialImportExcelWithProductColumn(boolean withImage, String productCode,
                                                                           String productName,
                                                                           String productNameEn) throws IOException {
        return buildProductMaterialImportExcelWithExtras(withImage, productCode, productName, productNameEn,
                "中国", "卖点文案");
    }

    private static byte[] buildProductMaterialImportExcelWithExtras(boolean withImage, String productCode,
                                                                    String productName, String productNameEn,
                                                                    String salesCountry, String sellingPointsCopy)
            throws IOException {
        return buildProductMaterialImportExcelWithValues(withImage, productCode, productName, productNameEn,
                "创新展柜", "瑛泰", "已注册", "新BU", salesCountry, "新适应症", "新型号",
                "新注册证", sellingPointsCopy);
    }

    private static ProductMaterialRow productMaterialRow(String productCode, String productName, String productNameEn,
                                                         String hallName, String ownerCompanyName,
                                                         String lifecycleStage, String pipelineLayout,
                                                         String salesCountry, String indicationContent,
                                                         String modelSpecification, String registrationCertificate,
                                                         String sellingPointsCopy) {
        return new ProductMaterialRow(productCode, productName, productNameEn, hallName, ownerCompanyName,
                lifecycleStage, pipelineLayout, salesCountry, indicationContent, modelSpecification,
                registrationCertificate, sellingPointsCopy);
    }

    private static byte[] buildProductMaterialImportExcelWithValues(boolean withImage, String productCode,
                                                                    String productName, String productNameEn,
                                                                    String hallName, String ownerCompanyName,
                                                                    String lifecycleStage, String pipelineLayout,
                                                                    String salesCountry, String indicationContent,
                                                                    String modelSpecification,
                                                                    String registrationCertificate,
                                                                    String sellingPointsCopy)
            throws IOException {
        return buildProductMaterialImportExcelWithRows(withImage, List.of(productMaterialRow(productCode, productName,
                productNameEn, hallName, ownerCompanyName, lifecycleStage, pipelineLayout, salesCountry,
                indicationContent, modelSpecification, registrationCertificate, sellingPointsCopy)));
    }

    private static byte[] buildProductMaterialImportExcelWithRows(boolean withImage, List<ProductMaterialRow> rows)
            throws IOException {
        String[] headers = {
                "展品编码", "旧产品编号", "产品名-中文", "产品名-英文", "展柜名称", "持证公司", "在售/在研", "BU",
                "在售国家", "适应症", "型号规格", "注册证信息", "卖点文案", "产品图", "奖项", "原材料表单"
        };
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("产品列表");
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                ProductMaterialRow source = rows.get(rowIndex);
                org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(rowIndex + 1);
                String[] values = {
                        source.productCode(), "", source.productName(), source.productNameEn(), source.hallName(),
                        source.ownerCompanyName(), source.lifecycleStage(), source.pipelineLayout(),
                        source.salesCountry(), source.indicationContent(), source.modelSpecification(),
                        source.registrationCertificate(), source.sellingPointsCopy(), "", "", ""
                };
                for (int index = 0; index < values.length; index++) {
                    dataRow.createCell(index).setCellValue(values[index] == null ? "" : values[index]);
                }
            }
            if (withImage && !rows.isEmpty()) {
                int imageIndex = workbook.addPicture(ONE_PIXEL_PNG_BYTES, org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG);
                org.apache.poi.ss.usermodel.Drawing<?> drawing = sheet.createDrawingPatriarch();
                org.apache.poi.ss.usermodel.ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
                anchor.setCol1(13);
                anchor.setRow1(1);
                anchor.setCol2(14);
                anchor.setRow2(2);
                anchor.setAnchorType(org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_AND_RESIZE);
                drawing.createPicture(anchor, imageIndex);
            }
            addProductMasterSheet(workbook, productMasterSheetRowsFromProductRows(rows));
            addDefaultAwardSheet(workbook);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildProductMaterialImportExcelWithLegacyProductColumn(boolean withImage, String productCode,
                                                                                String productName, String legacyProductName,
                                                                                String productNameEn,
                                                                                String salesCountry,
                                                                                String sellingPointsCopy)
            throws IOException {
        String[] headers = {
                "展品编码", "旧产品编号", "产品名-中文", "产品名-英文", "展柜名称", "持证公司", "在售/在研", "BU",
                "在售国家", "适应症", "型号规格", "注册证信息", "产品", "卖点文案",
                "产品图", "奖项", "原材料表单"
        };
        String[] values = {
                productCode, "", productName, productNameEn, "创新展柜", "瑛泰",
                "已注册", "新BU", salesCountry, "新适应症", "新型号", "新注册证",
                legacyProductName, sellingPointsCopy, "", "", ""
        };
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("产品列表");
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }
            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(1);
            for (int index = 0; index < values.length; index++) {
                dataRow.createCell(index).setCellValue(values[index]);
            }
            if (withImage) {
                int imageIndex = workbook.addPicture(ONE_PIXEL_PNG_BYTES, org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG);
                org.apache.poi.ss.usermodel.Drawing<?> drawing = sheet.createDrawingPatriarch();
                org.apache.poi.ss.usermodel.ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
                anchor.setCol1(14);
                anchor.setRow1(1);
                anchor.setCol2(15);
                anchor.setRow2(2);
                anchor.setAnchorType(org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_AND_RESIZE);
                drawing.createPicture(anchor, imageIndex);
            }
            addProductMasterSheet(workbook, List.of(productMasterSheetRow(productCode, productName, productNameEn,
                    "新型号", "瑛泰")));
            addDefaultAwardSheet(workbook);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildProductMaterialImportExcelWithOldChineseNameHeader(String productCode,
                                                                                  String productName,
                                                                                  String productNameEn)
            throws IOException {
        String[] headers = {
                "展品编码", "旧产品编号", "产品-中文", "产品名-英文", "展柜名称", "持证公司", "在售/在研", "BU",
                "在售国家", "适应症", "型号规格", "注册证信息", "卖点文案",
                "产品图", "奖项", "原材料表单"
        };
        String[] values = {
                productCode, "", productName, productNameEn, "创新展柜", "瑛泰",
                "已注册", "新BU", "中国", "新适应症", "新型号", "新注册证",
                "卖点文案", "", "", ""
        };
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("产品列表");
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }
            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(1);
            for (int index = 0; index < values.length; index++) {
                dataRow.createCell(index).setCellValue(values[index]);
            }
            addProductMasterSheet(workbook, List.of(productMasterSheetRow(productCode, productName, productNameEn,
                    "新型号", "瑛泰")));
            addDefaultAwardSheet(workbook);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildImportExcel(List<ShowroomProductExcelVO> rows) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            EasyExcel.write(outputStream, ShowroomProductExcelVO.class)
                    .sheet("产品列表")
                    .doWrite(rows);
            try (org.apache.poi.ss.usermodel.Workbook workbook =
                         org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
                 ByteArrayOutputStream finalOutput = new ByteArrayOutputStream()) {
                addProductMasterSheet(workbook, rows.stream()
                        .map(row -> productMasterSheetRow(row.getProductCode(), row.getNameCn(), row.getNameEn(),
                                row.getModelSpecification(), row.getOwnerCompanyName()))
                        .toList());
                addDefaultAwardSheet(workbook);
                workbook.write(finalOutput);
                return finalOutput.toByteArray();
            }
        }
    }

    private static byte[] buildBaseWorkbookImportExcel(List<ProductMaterialRow> productRows,
                                                       List<BaseAwardRow> awardRows,
                                                       boolean withAwardCoverImages) throws IOException {
        String[] headers = {
                "展品编码", "旧产品编号", "产品名-中文", "产品名-英文", "展柜名称", "持证公司", "在售/在研", "BU",
                "在售国家", "适应症", "型号规格", "注册证信息", "卖点文案", "产品图", "奖项", "原材料表单"
        };
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet productSheet = workbook.createSheet("产品列表");
            org.apache.poi.ss.usermodel.Row headerRow = productSheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }
            for (int rowIndex = 0; rowIndex < productRows.size(); rowIndex++) {
                ProductMaterialRow source = productRows.get(rowIndex);
                org.apache.poi.ss.usermodel.Row dataRow = productSheet.createRow(rowIndex + 1);
                String[] values = {
                        source.productCode(), "", source.productName(), source.productNameEn(), source.hallName(),
                        source.ownerCompanyName(), source.lifecycleStage(), source.pipelineLayout(),
                        source.salesCountry(), source.indicationContent(), source.modelSpecification(),
                        source.registrationCertificate(), source.sellingPointsCopy(), "", "", ""
                };
                for (int index = 0; index < values.length; index++) {
                    dataRow.createCell(index).setCellValue(values[index] == null ? "" : values[index]);
                }
            }
            addProductMasterSheet(workbook, productMasterSheetRowsFromProductRows(productRows));
            addAwardSheet(workbook, awardRows, withAwardCoverImages);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] addDefaultAwardSheet(byte[] bytes) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            addDefaultAwardSheet(workbook);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildResourcePackage(byte[] workbookBytes) throws IOException {
        return buildResourcePackage(workbookBytes, """
                {"schemaVersion":"showroom-product-resource-package.v1","workbookPath":"product-data.xlsx","narrations":[]}
                """, Map.of());
    }

    private static byte[] buildResourcePackage(byte[] workbookBytes,
                                               String manifestJson,
                                               Map<String, byte[]> assets) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifestJson.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("product-data.xlsx"));
            zip.write(workbookBytes);
            zip.closeEntry();
            for (Map.Entry<String, byte[]> asset : assets.entrySet()) {
                zip.putNextEntry(new ZipEntry(asset.getKey()));
                zip.write(asset.getValue());
                zip.closeEntry();
            }
            zip.finish();
            return outputStream.toByteArray();
        }
    }

    private static byte[] addNarrationAndKeywordSheets(byte[] bytes,
                                                       List<NarrationSheetRow> narrationRows,
                                                       List<KeywordSheetRow> keywordRows) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            addNarrationSheet(workbook, narrationRows);
            addKeywordSheet(workbook, keywordRows);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static void addDefaultAwardSheet(org.apache.poi.ss.usermodel.Workbook workbook) {
        if (workbook.getSheet("奖项") != null) {
            addNarrationSheet(workbook, List.of());
            addKeywordSheet(workbook, List.of());
            return;
        }
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("奖项");
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(0);
        row.createCell(0).setCellValue(1);
        row.createCell(1).setCellValue("创新奖");
        row.createCell(2).setCellValue("2026");
        row.createCell(3).setCellValue("颁发单位");
        int imageIndex = workbook.addPicture(ONE_PIXEL_PNG_BYTES, org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG);
        org.apache.poi.ss.usermodel.Drawing<?> drawing = sheet.createDrawingPatriarch();
        org.apache.poi.ss.usermodel.ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setCol1(4);
        anchor.setRow1(0);
        anchor.setCol2(5);
        anchor.setRow2(1);
        anchor.setAnchorType(org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_AND_RESIZE);
        drawing.createPicture(anchor, imageIndex);
        addNarrationSheet(workbook, List.of());
        addKeywordSheet(workbook, List.of());
    }

    private static List<ProductMasterSheetRow> productMasterSheetRowsFromProductRows(List<ProductMaterialRow> rows) {
        return rows.stream()
                .map(row -> productMasterSheetRow(row.productCode(), row.productName(), row.productNameEn(),
                        row.modelSpecification(), row.ownerCompanyName()))
                .toList();
    }

    private static ProductMasterSheetRow productMasterSheetRow(String productCode, String nameCn, String nameEn,
                                                               String modelSpecification, String category) {
        return new ProductMasterSheetRow(productCode, nameCn, nameEn, modelSpecification, category);
    }

    private static void addProductMasterSheet(org.apache.poi.ss.usermodel.Workbook workbook,
                                              List<ProductMasterSheetRow> productRows) {
        org.apache.poi.ss.usermodel.Sheet existing = workbook.getSheet("产品主数据");
        if (existing != null) {
            workbook.removeSheetAt(workbook.getSheetIndex(existing));
        }
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("产品主数据");
        String[] headers = {"产品编码", "DCC产品编号", "中文名称", "英文名称", "型号规格", "产品分类"};
        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            header.createCell(index).setCellValue(headers[index]);
        }
        for (int rowIndex = 0; rowIndex < productRows.size(); rowIndex++) {
            ProductMasterSheetRow product = productRows.get(rowIndex);
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex + 1);
            row.createCell(0).setCellValue(product.productCode() == null ? "" : product.productCode());
            row.createCell(1).setCellValue("");
            row.createCell(2).setCellValue(product.nameCn() == null ? "" : product.nameCn());
            row.createCell(3).setCellValue(product.nameEn() == null ? "" : product.nameEn());
            row.createCell(4).setCellValue(product.modelSpecification() == null ? "" : product.modelSpecification());
            row.createCell(5).setCellValue(product.category() == null ? "" : product.category());
        }
    }

    private static void addAwardSheet(org.apache.poi.ss.usermodel.Workbook workbook,
                                      List<BaseAwardRow> awardRows,
                                      boolean withAwardCoverImages) {
        org.apache.poi.ss.usermodel.Sheet existingSheet = workbook.getSheet("奖项");
        if (existingSheet != null) {
            int existingIndex = workbook.getSheetIndex(existingSheet);
            workbook.removeSheetAt(existingIndex);
        }
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("奖项");
        org.apache.poi.ss.usermodel.Drawing<?> drawing = withAwardCoverImages
                ? sheet.createDrawingPatriarch()
                : null;
        for (int rowIndex = 0; rowIndex < awardRows.size(); rowIndex++) {
            BaseAwardRow awardRow = awardRows.get(rowIndex);
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex);
            row.createCell(0).setCellValue(awardRow.sequence());
            row.createCell(1).setCellValue(awardRow.nameCn());
            row.createCell(2).setCellValue(awardRow.awardDateText());
            row.createCell(3).setCellValue(awardRow.issuer());
            if (withAwardCoverImages) {
                int imageIndex = workbook.addPicture(ONE_PIXEL_PNG_BYTES,
                        org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG);
                org.apache.poi.ss.usermodel.ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
                anchor.setCol1(4);
                anchor.setRow1(rowIndex);
                anchor.setCol2(5);
                anchor.setRow2(rowIndex + 1);
                anchor.setAnchorType(org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_AND_RESIZE);
                drawing.createPicture(anchor, imageIndex);
            }
        }
    }

    private static void addNarrationSheet(org.apache.poi.ss.usermodel.Workbook workbook,
                                          List<NarrationSheetRow> rows) {
        org.apache.poi.ss.usermodel.Sheet existing = workbook.getSheet("讲解音频");
        if (existing != null) {
            workbook.removeSheetAt(workbook.getSheetIndex(existing));
        }
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("讲解音频");
        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("目标类型");
        header.createCell(1).setCellValue("目标编码");
        header.createCell(2).setCellValue("目标名称");
        header.createCell(3).setCellValue("语言");
        header.createCell(4).setCellValue("讲解稿");
        header.createCell(5).setCellValue("音频文件ID");
        header.createCell(6).setCellValue("音频地址");
        header.createCell(7).setCellValue("音频时长(秒)");
        header.createCell(8).setCellValue("音色");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            NarrationSheetRow source = rows.get(rowIndex);
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex + 1);
            row.createCell(0).setCellValue(source.targetType());
            row.createCell(1).setCellValue(source.targetCode());
            row.createCell(2).setCellValue(source.targetName());
            row.createCell(3).setCellValue(source.language());
            row.createCell(4).setCellValue(source.scriptText());
            row.createCell(5).setCellValue(source.audioFileId() == null ? "" : String.valueOf(source.audioFileId()));
            row.createCell(6).setCellValue(source.audioUrl());
            row.createCell(7).setCellValue(source.audioDurationSeconds() == null
                    ? ""
                    : String.valueOf(source.audioDurationSeconds()));
            row.createCell(8).setCellValue(source.voice());
        }
    }

    private static void addKeywordSheet(org.apache.poi.ss.usermodel.Workbook workbook,
                                        List<KeywordSheetRow> rows) {
        org.apache.poi.ss.usermodel.Sheet existing = workbook.getSheet("关键词中英对照");
        if (existing != null) {
            workbook.removeSheetAt(workbook.getSheetIndex(existing));
        }
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("关键词中英对照");
        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("中文关键词");
        header.createCell(1).setCellValue("English Keyword");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            KeywordSheetRow source = rows.get(rowIndex);
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex + 1);
            row.createCell(0).setCellValue(source.nameZh());
            row.createCell(1).setCellValue(source.nameEn());
        }
    }

    private static List<String> readHeader(byte[] bytes) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Row row = workbook.getSheetAt(0).getRow(0);
            java.util.List<String> headers = new java.util.ArrayList<>();
            for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                headers.add(row.getCell(cellIndex).getStringCellValue());
            }
            return headers;
        }
    }

    private static Map<String, String> readFirstDataRowByHeader(byte[] bytes) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(0);
            org.apache.poi.ss.usermodel.Row dataRow = sheet.getRow(1);
            org.apache.poi.ss.usermodel.DataFormatter dataFormatter = new org.apache.poi.ss.usermodel.DataFormatter();
            java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
            for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
                String header = dataFormatter.formatCellValue(headerRow.getCell(cellIndex));
                String value = dataRow == null ? "" : dataFormatter.formatCellValue(dataRow.getCell(cellIndex));
                values.put(header, value);
            }
            return values;
        }
    }

    private static List<String> readSheetNames(byte[] bytes) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            java.util.List<String> sheetNames = new java.util.ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                sheetNames.add(workbook.getSheetName(sheetIndex));
            }
            return sheetNames;
        }
    }

    private static int countEmbeddedPictures(byte[] bytes) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (workbook instanceof org.apache.poi.xssf.usermodel.XSSFWorkbook xssfWorkbook) {
                return xssfWorkbook.getAllPictures().size();
            }
            return 0;
        }
    }

    private static byte[] productWorkbookBytes(MockHttpServletResponse response) throws IOException {
        byte[] content = response.getContentAsByteArray();
        if (!"application/zip".equals(response.getContentType())) {
            return content;
        }
        byte[] workbookBytes = unzipPackage(content).get(ShowroomProductResourcePackage.WORKBOOK_PATH);
        if (workbookBytes == null) {
            throw new IllegalStateException("product-data.xlsx is missing from resource package");
        }
        return workbookBytes;
    }

    private static Map<String, byte[]> unzipPackage(byte[] zipBytes) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    result.put(entry.getName(), zip.readAllBytes());
                }
            }
        }
        return result;
    }

    private static List<ShowroomProductExcelVO> readProductRows(byte[] bytes) {
        return FastExcelFactory.read(new ByteArrayInputStream(bytes), ShowroomProductExcelVO.class, null)
                .autoCloseStream(false)
                .sheet("产品列表")
                .doReadSync();
    }

    private static List<Map<String, String>> readSheetRowsByHeader(byte[] bytes, String sheetName) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                return List.of();
            }
            org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return List.of();
            }
            org.apache.poi.ss.usermodel.DataFormatter dataFormatter = new org.apache.poi.ss.usermodel.DataFormatter();
            List<String> headers = new java.util.ArrayList<>();
            for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
                headers.add(dataFormatter.formatCellValue(headerRow.getCell(cellIndex)));
            }
            List<Map<String, String>> rows = new java.util.ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new java.util.LinkedHashMap<>();
                boolean nonBlank = false;
                for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                    String value = dataFormatter.formatCellValue(row.getCell(cellIndex));
                    if (!value.isBlank()) {
                        nonBlank = true;
                    }
                    values.put(headers.get(cellIndex), value);
                }
                if (nonBlank) {
                    rows.add(values);
                }
            }
            return rows;
        }
    }

    private static boolean isCellWrapped(byte[] bytes, String header, int rowIndex) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(0);
            org.apache.poi.ss.usermodel.DataFormatter dataFormatter = new org.apache.poi.ss.usermodel.DataFormatter();
            int columnIndex = -1;
            for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
                if (header.equals(dataFormatter.formatCellValue(headerRow.getCell(cellIndex)))) {
                    columnIndex = cellIndex;
                    break;
                }
            }
            if (columnIndex < 0) {
                return false;
            }
            org.apache.poi.ss.usermodel.Row dataRow = sheet.getRow(rowIndex);
            if (dataRow == null || dataRow.getCell(columnIndex) == null) {
                return false;
            }
            return dataRow.getCell(columnIndex).getCellStyle().getWrapText();
        }
    }

    private static String responseOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void seedPublishedAward(String awardCode, String nameCn, String awardDateText, String issuer,
                                    String coverImage) {
        var draft = seedAwardDraft(awardCode, nameCn, awardDateText, issuer, coverImage);
        contentService.publishAwardRevision(draft.revisionId(), 300L);
    }

    private void seedKeyword(String nameZh, String nameEn) {
        cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO keyword =
                new cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO();
        keyword.setTenantId(1L);
        keyword.setNameZh(nameZh);
        keyword.setNameEn(nameEn);
        keywordMapper.insert(keyword);
    }

    private cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardRevision seedAwardDraft(
            String awardCode, String nameCn, String awardDateText, String issuer, String coverImage) {
        return contentService.saveAwardDraft(new ShowroomAwardDraft(null, awardCode, nameCn, "",
                "", "", issuer, awardDateText, coverImage));
    }

    private record ReplacementProductRow(String productCode, String nameCn, String nameEn, String hallName,
                                         String ownerCompanyName, String lifecycleStage, String pipelineLayout,
                                         String coreSellingPoints, String indicationContent,
                                         String modelSpecification, String registrationCertificate,
                                         String awards, String rawMaterialSheet) {
    }

    private record ProductMaterialRow(String productCode, String productName, String productNameEn, String hallName,
                                      String ownerCompanyName, String lifecycleStage, String pipelineLayout,
                                      String salesCountry, String indicationContent, String modelSpecification,
                                      String registrationCertificate, String sellingPointsCopy) {
    }

    private record ProductMasterSheetRow(String productCode, String nameCn, String nameEn,
                                         String modelSpecification, String category) {
    }

    private static BaseAwardRow baseAwardRow(int sequence, String nameCn, String awardDateText, String issuer) {
        return new BaseAwardRow(sequence, nameCn, awardDateText, issuer);
    }

    private record BaseAwardRow(int sequence, String nameCn, String awardDateText, String issuer) {
    }

    private record NarrationSheetRow(String targetType,
                                     String targetCode,
                                     String targetName,
                                     String language,
                                     String scriptText,
                                     Long audioFileId,
                                     String audioUrl,
                                     Integer audioDurationSeconds,
                                     String voice) {
    }

    private record KeywordSheetRow(String nameZh, String nameEn) {
    }

    private <T> T withLoginUser(Long userId, CheckedSupplier<T> supplier) throws Exception {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            return supplier.get();
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
