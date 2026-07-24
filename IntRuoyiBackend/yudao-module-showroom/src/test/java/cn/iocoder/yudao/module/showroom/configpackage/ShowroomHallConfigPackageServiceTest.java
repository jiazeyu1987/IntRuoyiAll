package cn.iocoder.yudao.module.showroom.configpackage;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetKey;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.hall.ShowroomHallConfigPackageImportRespVO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomAwardDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomHallDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomAwardMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomHallMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.keyword.ShowroomKeywordMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import cn.iocoder.yudao.module.showroom.release.AbstractShowroomReleaseDbTest;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Import(ShowroomHallConfigPackageServiceImpl.class)
class ShowroomHallConfigPackageServiceTest extends AbstractShowroomReleaseDbTest {

    private static final Long TEST_TENANT_ID = 122L;

    @Resource
    private ShowroomHallConfigPackageService service;
    @Resource
    private ShowroomKeywordMapper keywordMapper;
    @Resource
    private ShowroomHallMapper hallMapper;
    @Resource
    private ShowroomProductMapper productMapper;
    @Resource
    private ShowroomAwardMapper awardMapper;
    @MockBean
    private TenantMapper tenantMapper;

    private final AtomicLong importedFileIdSequence = new AtomicLong(9000L);

    @BeforeEach
    void setUpImportedFileMocks() {
        ensureTenant(DEFAULT_TENANT_ID, "芋道源码");
        ensureTenant(TEST_TENANT_ID, "测试租户");
        when(fileService.createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    byte[] bytes = invocation.getArgument(0);
                    String name = invocation.getArgument(1);
                    String directory = invocation.getArgument(2);
                    String mimeType = invocation.getArgument(3);
                    long fileId = importedFileIdSequence.incrementAndGet();
                    String path = directory + "/" + name;
                    FileDO file = FileDO.builder()
                            .id(fileId)
                            .configId(28L)
                            .name(name)
                            .path(path)
                            .type(mimeType)
                            .size((long) bytes.length)
                            .url("http://127.0.0.1:9000/yudao/" + path)
                            .build();
                    when(fileService.getFile(fileId)).thenReturn(file);
                    when(fileService.getFileContent(28L, path)).thenReturn(bytes);
                    return fileId;
                });
    }

    @Test
    void exportPackageShouldContainManifestKeywordsHallsAndAssets() throws Exception {
        seedSourceTenantFixture();

        byte[] content = TenantUtils.execute(DEFAULT_TENANT_ID, service::exportPackage);
        Map<String, byte[]> entries = unzip(content);

        assertTrue(entries.containsKey("manifest.json"));
        assertTrue(entries.keySet().stream().anyMatch(path -> path.startsWith("assets/halls/CARDIOLOGY/background.")));
        assertTrue(entries.keySet().stream().anyMatch(path -> path.startsWith("assets/halls/CARDIOLOGY/preview.")));
        assertTrue(entries.keySet().stream().anyMatch(path -> path.startsWith("assets/halls/CARDIOLOGY/narration-zh.")));
        assertTrue(entries.keySet().stream().anyMatch(path -> path.startsWith("assets/halls/CARDIOLOGY/narration-en.")));

        String manifestJson = new String(entries.get("manifest.json"), StandardCharsets.UTF_8);
        Map<?, ?> manifest = JsonUtils.parseObject(manifestJson, Map.class);
        assertEquals("showroom-hall-config-package.v1", manifest.get("schemaVersion"));
        assertEquals(1, ((Number) manifest.get("sourceTenantId")).intValue());
        assertEquals("芋道源码", manifest.get("sourceTenantName"));
        List<?> keywords = (List<?>) manifest.get("keywords");
        assertEquals("支架", ((Map<?, ?>) keywords.get(0)).get("nameZh"));
        List<?> halls = (List<?>) manifest.get("halls");
        Map<?, ?> hall = (Map<?, ?>) halls.get(0);
        assertEquals("CARDIOLOGY", hall.get("hallCode"));
        assertNotNull(hall.get("previewAsset"));
        assertNotNull(hall.get("canvasBackground"));
        List<?> narrations = (List<?>) hall.get("narrations");
        assertTrue(narrations.stream().anyMatch(item -> "ZH".equals(((Map<?, ?>) item).get("language"))));
        assertTrue(narrations.stream().anyMatch(item -> "EN".equals(((Map<?, ?>) item).get("language"))));
    }

    @Test
    void importPackageShouldFullyReplaceHallsKeywordsAndLiveAssets() throws Exception {
        seedSourceTenantFixture();
        seedTargetTenantFixture(true);
        byte[] exported = TenantUtils.execute(DEFAULT_TENANT_ID, service::exportPackage);

        ShowroomHallConfigPackageImportRespVO response =
                TenantUtils.execute(TEST_TENANT_ID, () -> service.importPackage(exported));

        assertEquals(1, response.hallCount());
        assertEquals(2, response.keywordCount());
        assertEquals(1, response.previewAssetCount());
        assertEquals(2, response.narrationCount());
        assertEquals(1, response.backgroundAssetCount());
        assertEquals(1, response.removedHallCount());
        assertEquals(1, response.removedKeywordCount());
        assertEquals(1, response.validatedProductCount());

        TenantUtils.execute(TEST_TENANT_ID, () -> {
            List<ShowroomKeywordDO> keywords = keywordMapper.selectListOrdered();
            assertEquals(List.of("支架", "导管"), keywords.stream().map(ShowroomKeywordDO::getNameZh).toList());

            List<ShowroomHall> halls = contentService.listHalls();
            assertEquals(1, halls.size());
            ShowroomHall hall = halls.get(0);
            assertEquals("CARDIOLOGY", hall.hallCode());
            assertEquals("心内介入展厅", hall.name());
            assertEquals("Cardiology Hall", hall.nameEn());
            assertEquals("源背景图", readAssetByAdminUrl(hall.canvasBackgroundImageUrl()));
            assertEquals(1, hall.itemMappings().size());
            assertEquals("P-101", productMapper.selectById(hall.itemMappings().get(0).itemId()).getProductCode());

            Long previewFileId = previewAssetService.live(
                    new ShowroomPreviewAssetKey(ShowroomPreviewAssetTargetType.HALL, hall.hallId()))
                    .orElseThrow()
                    .files()
                    .desktopFileId();
            assertEquals("hall-preview", readAssetByFileId(previewFileId));

            ShowroomNarrationVersion zhNarration = narrationService.live(new ShowroomNarrationKey(
                    ShowroomNarrationTargetType.HALL,
                    hall.hallId(),
                    ShowroomNarrationAudienceType.PUBLIC,
                    ShowroomNarrationLanguage.ZH)).orElseThrow();
            ShowroomNarrationVersion enNarration = narrationService.live(new ShowroomNarrationKey(
                    ShowroomNarrationTargetType.HALL,
                    hall.hallId(),
                    ShowroomNarrationAudienceType.PUBLIC,
                    ShowroomNarrationLanguage.EN)).orElseThrow();
            assertEquals("展厅简介", zhNarration.scriptText());
            assertEquals("Hall summary", enNarration.scriptText());
            assertEquals("ruoxi", zhNarration.voice());
            assertEquals("ruoxi", enNarration.voice());
            assertEquals(60, zhNarration.audioDurationSeconds());
            assertEquals(60, enNarration.audioDurationSeconds());
            assertEquals("hall-audio-zh", readAssetByFileId(zhNarration.audioFileId()));
            assertEquals("hall-audio-en", readAssetByFileId(enNarration.audioFileId()));
            assertFalse(hallMapper.selectListOrdered().stream().anyMatch(item -> "LEGACY".equals(item.getHallCode())));
            return null;
        });
    }

    @Test
    void importPackageShouldFailAndRollbackWhenProductCodeMissing() throws Exception {
        seedTargetTenantFixture(false);
        byte[] packageBytes = buildPackageBytes(
                List.of(new KeywordRow("产品词", "Product")),
                List.of(new HallRow("CARDIOLOGY", "心内介入展厅", "Cardiology Hall", "说明", "desc",
                        null,
                        List.of(new HallItemRow("PRODUCT", "P-404", 1, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ONE, BigDecimal.ONE)),
                        null,
                        List.of())));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> TenantUtils.execute(TEST_TENANT_ID, () -> service.importPackage(packageBytes)));
        ServiceException serviceException = unwrapServiceException(exception);

        assertEquals(400, serviceException.getCode());
        assertTrue(serviceException.getMessage().contains("missing productCode(s): P-404"));
        TenantUtils.execute(TEST_TENANT_ID, () -> {
            assertTrue(keywordMapper.selectListOrdered().stream().anyMatch(keyword -> "旧关键词".equals(keyword.getNameZh())));
            assertTrue(hallMapper.selectListOrdered().stream().anyMatch(hall -> "LEGACY".equals(hall.getHallCode())));
            return null;
        });
    }

    @Test
    void importPackageShouldFailAndRollbackWhenAwardCodeMissing() throws Exception {
        seedTargetTenantFixture(true);
        byte[] packageBytes = buildPackageBytes(
                List.of(new KeywordRow("奖项词", "Award")),
                List.of(new HallRow("HONOR-1", "企业荣誉", "Honor Hall", "说明", "desc",
                        null,
                        List.of(new HallItemRow("AWARD", "AWARD-404", 1, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ONE, BigDecimal.ONE)),
                        null,
                        List.of())));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> TenantUtils.execute(TEST_TENANT_ID, () -> service.importPackage(packageBytes)));
        ServiceException serviceException = unwrapServiceException(exception);

        assertEquals(400, serviceException.getCode());
        assertTrue(serviceException.getMessage().contains("missing awardCode(s): AWARD-404"));
        TenantUtils.execute(TEST_TENANT_ID, () -> {
            assertTrue(keywordMapper.selectListOrdered().stream().anyMatch(keyword -> "旧关键词".equals(keyword.getNameZh())));
            assertTrue(hallMapper.selectListOrdered().stream().anyMatch(hall -> "LEGACY".equals(hall.getHallCode())));
            return null;
        });
    }

    @Test
    void importPackageShouldFailWhenDeclaredAssetMissing() throws Exception {
        seedTargetTenantFixture(true);
        byte[] packageBytes = buildPackageBytes(
                List.of(new KeywordRow("背景词", "Background")),
                List.of(new HallRow("CARDIOLOGY", "心内介入展厅", "Cardiology Hall", "说明", "desc",
                        new BinaryRef("assets/halls/CARDIOLOGY/missing/background.png"),
                        List.of(new HallItemRow("PRODUCT", "P-101", 1, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ONE, BigDecimal.ONE)),
                        null,
                        List.of())));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> TenantUtils.execute(TEST_TENANT_ID, () -> service.importPackage(packageBytes)));
        ServiceException serviceException = unwrapServiceException(exception);

        assertEquals(400, serviceException.getCode());
        assertTrue(serviceException.getMessage().contains("SHOWROOM_HALL_CONFIG_PACKAGE_ASSET_MISSING"));
        TenantUtils.execute(TEST_TENANT_ID, () -> {
            assertTrue(keywordMapper.selectListOrdered().stream().anyMatch(keyword -> "旧关键词".equals(keyword.getNameZh())));
            assertTrue(hallMapper.selectListOrdered().stream().anyMatch(hall -> "LEGACY".equals(hall.getHallCode())));
            return null;
        });
    }

    @Test
    void importPackageShouldAllowReplacingTargetWithEmptyKeywordsAndHalls() throws Exception {
        seedTargetTenantFixture(true);
        byte[] packageBytes = buildPackageBytes(List.of(), List.of());

        ShowroomHallConfigPackageImportRespVO response =
                TenantUtils.execute(TEST_TENANT_ID, () -> service.importPackage(packageBytes));

        assertEquals(0, response.hallCount());
        assertEquals(0, response.keywordCount());
        assertEquals(0, response.previewAssetCount());
        assertEquals(0, response.narrationCount());
        assertEquals(0, response.backgroundAssetCount());
        assertEquals(1, response.removedHallCount());
        assertEquals(1, response.removedKeywordCount());
        assertEquals(0, response.validatedProductCount());
        assertEquals(0, response.validatedAwardCount());

        TenantUtils.execute(TEST_TENANT_ID, () -> {
            assertTrue(keywordMapper.selectListOrdered().isEmpty());
            assertTrue(contentService.listHalls().isEmpty());
            return null;
        });
    }

    private void seedSourceTenantFixture() throws Exception {
        TenantUtils.execute(DEFAULT_TENANT_ID, () -> {
            List<ShowroomHall> halls = contentService.listHalls();
            ShowroomHall hall = halls.isEmpty() ? seedPublishedFixtureAndReturnHall() : halls.get(0);
            if (keywordMapper.selectListOrdered().isEmpty()) {
                insertKeyword("支架", "Stent");
                insertKeyword("导管", "Catheter");
            }
            mockFile(110L, 11L, "showroom/hall/background-source.png", "image/png", "源背景图");
            contentService.updateHallCanvasBackground(hall.hallId(),
                    "/admin-api/infra/file/11/get/showroom/hall/background-source.png");
            return null;
        });
    }

    private ShowroomHall seedPublishedFixtureAndReturnHall() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        return contentService.getHall(fixture.hallId());
    }

    private void seedTargetTenantFixture(boolean includeAward) {
        TenantUtils.execute(TEST_TENANT_ID, () -> {
            insertKeyword("旧关键词", "Legacy Keyword");
            Long targetProductId = insertProduct("P-101");
            if (includeAward) {
                insertAward("AWARD-101");
            }
            ShowroomHall hall = contentService.createHall("LEGACY", "旧展厅", "Legacy Hall", "旧说明", "Legacy Desc");
            contentService.replaceHallItemMappings(hall.hallId(), List.of(
                    new cn.iocoder.yudao.module.showroom.content.model.ShowroomHallItemMapping(
                            "PRODUCT", targetProductId, 1, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ONE, BigDecimal.ONE)));
            return null;
        });
    }

    private void ensureTenant(Long tenantId, String tenantName) {
        TenantDO tenant = new TenantDO();
        tenant.setId(tenantId);
        tenant.setName(tenantName);
        tenant.setStatus(0);
        tenant.setPackageId(0L);
        tenant.setAccountCount(100);
        tenant.setExpireTime(LocalDateTime.now().plusYears(1));
        when(tenantMapper.selectById(tenantId)).thenReturn(tenant);
    }

    private void insertKeyword(String nameZh, String nameEn) {
        ShowroomKeywordDO keyword = new ShowroomKeywordDO();
        keyword.setTenantId(TenantContextHolder.getRequiredTenantId());
        keyword.setNameZh(nameZh);
        keyword.setNameEn(nameEn);
        keywordMapper.insert(keyword);
    }

    private Long insertProduct(String productCode) {
        ShowroomProductDO product = ShowroomProductDO.builder()
                .productCode(productCode)
                .currentRevisionNo(0)
                .incompleteFlag(Boolean.FALSE)
                .status("DRAFT_ONLY")
                .build();
        product.setTenantId(TenantContextHolder.getRequiredTenantId());
        productMapper.insert(product);
        return product.getId();
    }

    private Long insertAward(String awardCode) {
        ShowroomAwardDO award = ShowroomAwardDO.builder()
                .awardCode(awardCode)
                .currentRevisionNo(0)
                .incompleteFlag(Boolean.FALSE)
                .status("DRAFT_ONLY")
                .build();
        award.setTenantId(TenantContextHolder.getRequiredTenantId());
        awardMapper.insert(award);
        return award.getId();
    }

    private String readAssetByFileId(Long fileId) throws Exception {
        FileDO file = fileService.getFile(fileId);
        assertNotNull(file);
        return new String(fileService.getFileContent(file.getConfigId(), file.getPath()), StandardCharsets.UTF_8);
    }

    private String readAssetByAdminUrl(String adminUrl) throws Exception {
        int prefix = adminUrl.indexOf("/admin-api/infra/file/");
        String normalized = adminUrl.substring(prefix + "/admin-api/infra/file/".length());
        int slash = normalized.indexOf("/get/");
        Long configId = Long.valueOf(normalized.substring(0, slash));
        String path = normalized.substring(slash + "/get/".length());
        return new String(fileService.getFileContent(configId, path), StandardCharsets.UTF_8);
    }

    private Map<String, byte[]> unzip(byte[] zipBytes) throws Exception {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                result.put(entry.getName(), zip.readAllBytes());
            }
        }
        return result;
    }

    private byte[] buildPackageBytes(List<KeywordRow> keywords, List<HallRow> halls) throws Exception {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schemaVersion", "showroom-hall-config-package.v1");
        manifest.put("exportedAt", "2026-06-30T12:00:00Z");
        manifest.put("sourceTenantId", 1);
        manifest.put("sourceTenantName", "芋道源码");
        manifest.put("keywords", keywords.stream().map(keyword -> Map.of(
                "nameZh", keyword.nameZh(),
                "nameEn", keyword.nameEn())).toList());
        List<Map<String, Object>> hallRows = new ArrayList<>();
        Map<String, byte[]> assets = new LinkedHashMap<>();
        for (HallRow hall : halls) {
            List<Map<String, Object>> narrationRows = hall.narrations().stream().map(narration -> {
                assets.put(narration.audioAsset().assetPath(), narration.audioContent().getBytes(StandardCharsets.UTF_8));
                return Map.of(
                        "language", narration.language(),
                        "scriptText", narration.scriptText(),
                        "voice", narration.voice(),
                        "duration", narration.duration(),
                        "audioAsset", Map.of("assetPath", narration.audioAsset().assetPath()));
            }).toList();
            if (hall.canvasBackground() != null && hall.backgroundContent() != null) {
                assets.put(hall.canvasBackground().assetPath(), hall.backgroundContent().getBytes(StandardCharsets.UTF_8));
            }
            if (hall.previewAsset() != null && hall.previewContent() != null) {
                assets.put(hall.previewAsset().assetPath(), hall.previewContent().getBytes(StandardCharsets.UTF_8));
            }
            Map<String, Object> hallRow = new LinkedHashMap<>();
            hallRow.put("hallCode", hall.hallCode());
            hallRow.put("name", hall.name());
            hallRow.put("nameEn", hall.nameEn());
            hallRow.put("description", hall.description());
            hallRow.put("descriptionEn", hall.descriptionEn());
            hallRow.put("canvasBackground",
                    hall.canvasBackground() == null ? null : Map.of("assetPath", hall.canvasBackground().assetPath()));
            hallRow.put("itemMappings", hall.items().stream().map(item -> Map.of(
                    "itemType", item.itemType(),
                    "itemCode", item.itemCode(),
                    "displayOrder", item.displayOrder(),
                    "layoutX", item.layoutX(),
                    "layoutY", item.layoutY(),
                    "layoutWidth", item.layoutWidth(),
                    "layoutHeight", item.layoutHeight())).toList());
            hallRow.put("previewAsset",
                    hall.previewAsset() == null ? null : Map.of("assetPath", hall.previewAsset().assetPath()));
            hallRow.put("narrations", narrationRows);
            hallRows.add(hallRow);
        }
        manifest.put("halls", hallRows);
        try (java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(JsonUtils.toJsonPrettyString(manifest).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (Map.Entry<String, byte[]> entry : assets.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        }
    }

    private record KeywordRow(String nameZh, String nameEn) {
    }

    private record BinaryRef(String assetPath) {
    }

    private record NarrationRow(String language,
                                String scriptText,
                                String voice,
                                int duration,
                                BinaryRef audioAsset,
                                String audioContent) {
    }

    private record HallItemRow(String itemType,
                               String itemCode,
                               int displayOrder,
                               BigDecimal layoutX,
                               BigDecimal layoutY,
                               BigDecimal layoutWidth,
                               BigDecimal layoutHeight) {
    }

    private record HallRow(String hallCode,
                           String name,
                           String nameEn,
                           String description,
                           String descriptionEn,
                           BinaryRef canvasBackground,
                           List<HallItemRow> items,
                           BinaryRef previewAsset,
                           List<NarrationRow> narrations) {

        String backgroundContent() {
            if (canvasBackground == null) {
                return null;
            }
            return canvasBackground.assetPath().contains("/missing/") ? null : "background-bytes-" + hallCode;
        }

        String previewContent() {
            return previewAsset == null ? null : "preview-bytes-" + hallCode;
        }
    }

    private ServiceException unwrapServiceException(RuntimeException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ServiceException serviceException) {
                return serviceException;
            }
            current = current.getCause();
        }
        throw exception;
    }

}
