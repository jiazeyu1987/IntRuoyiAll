package cn.iocoder.yudao.module.showroom.configpackage;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.framework.file.core.utils.FileTypeUtils;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPersistentPreviewAssetService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetDraftCommand;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetFiles;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetKey;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetVersion;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallItemMapping;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.hall.ShowroomHallConfigPackageImportRespVO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomAwardDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomHallDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomAwardMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomHallItemMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomHallMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomHallProductMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.keyword.ShowroomKeywordMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.narration.ShowroomNarrationVersionMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudioDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import cn.iocoder.yudao.module.showroom.narration.ShowroomPersistentNarrationService;
import cn.iocoder.yudao.module.showroom.release.ResolvedBinarySource;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseSourceFileReader;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;

@Service
public class ShowroomHallConfigPackageServiceImpl implements ShowroomHallConfigPackageService {

    static final String PACKAGE_FILE_NAME = "showroom-hall-config-package.zip";
    static final String MANIFEST_PATH = "manifest.json";
    static final String SCHEMA_VERSION = "showroom-hall-config-package.v1";
    private static final String HALL_ASSET_ROOT = "assets/halls/";
    private static final String BACKGROUND_DIRECTORY = "showroom/hall/background";
    private static final String PREVIEW_DIRECTORY = "showroom/hall/preview";
    private static final String NARRATION_DIRECTORY = "showroom/hall/narration";
    private static final DateTimeFormatter EXPORTED_AT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final ShowroomPersistentContentService contentService;
    private final ShowroomKeywordMapper keywordMapper;
    private final ShowroomHallMapper hallMapper;
    private final ShowroomHallItemMapper hallItemMapper;
    private final ShowroomHallProductMapper hallProductMapper;
    private final ShowroomProductMapper productMapper;
    private final ShowroomAwardMapper awardMapper;
    private final ShowroomPreviewAssetVersionMapper previewAssetVersionMapper;
    private final ShowroomNarrationVersionMapper narrationVersionMapper;
    private final ShowroomPersistentPreviewAssetService previewAssetService;
    private final ShowroomPersistentNarrationService narrationService;
    private final ShowroomReleaseSourceFileReader sourceFileReader;
    private final FileService fileService;
    private final TenantMapper tenantMapper;

    public ShowroomHallConfigPackageServiceImpl(ShowroomPersistentContentService contentService,
                                                ShowroomKeywordMapper keywordMapper,
                                                ShowroomHallMapper hallMapper,
                                                ShowroomHallItemMapper hallItemMapper,
                                                ShowroomHallProductMapper hallProductMapper,
                                                ShowroomProductMapper productMapper,
                                                ShowroomAwardMapper awardMapper,
                                                ShowroomPreviewAssetVersionMapper previewAssetVersionMapper,
                                                ShowroomNarrationVersionMapper narrationVersionMapper,
                                                ShowroomPersistentPreviewAssetService previewAssetService,
                                                ShowroomPersistentNarrationService narrationService,
                                                ShowroomReleaseSourceFileReader sourceFileReader,
                                                FileService fileService,
                                                TenantMapper tenantMapper) {
        this.contentService = contentService;
        this.keywordMapper = keywordMapper;
        this.hallMapper = hallMapper;
        this.hallItemMapper = hallItemMapper;
        this.hallProductMapper = hallProductMapper;
        this.productMapper = productMapper;
        this.awardMapper = awardMapper;
        this.previewAssetVersionMapper = previewAssetVersionMapper;
        this.narrationVersionMapper = narrationVersionMapper;
        this.previewAssetService = previewAssetService;
        this.narrationService = narrationService;
        this.sourceFileReader = sourceFileReader;
        this.fileService = fileService;
        this.tenantMapper = tenantMapper;
    }

    @Override
    public byte[] exportPackage() {
        try {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            TenantDO tenant = requireTenant(tenantId);
            List<ShowroomKeywordDO> keywords = keywordMapper.selectListOrdered();
            List<ShowroomHall> halls = contentService.listHalls();
            List<ExportedBinaryAsset> assets = new ArrayList<>();
            List<ManifestHall> manifestHalls = new ArrayList<>();
            for (ShowroomHall hall : halls) {
                manifestHalls.add(exportHall(hall, assets));
            }
            PackageManifest manifest = new PackageManifest(
                    SCHEMA_VERSION,
                    EXPORTED_AT_FORMATTER.format(Instant.now().atOffset(ZoneOffset.UTC)),
                    tenant.getId(),
                    tenant.getName(),
                    keywords.stream()
                            .map(keyword -> new ManifestKeyword(normalizeRequired(keyword.getNameZh(),
                                            "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: keyword zh is required"),
                                    normalizeRequired(keyword.getNameEn(),
                                            "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: keyword en is required")))
                            .toList(),
                    manifestHalls);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                 ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                writeZipEntry(zip, MANIFEST_PATH, JsonUtils.toJsonPrettyString(manifest).getBytes(StandardCharsets.UTF_8));
                for (ExportedBinaryAsset asset : assets) {
                    writeZipEntry(zip, asset.assetPath(), asset.bytes());
                }
                zip.finish();
                return output.toByteArray();
            } catch (IOException exception) {
                throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: failed to write zip package",
                        exception);
            }
        } catch (ServiceException exception) {
            throw exception;
        } catch (IllegalStateException exception) {
            throw exception0(INTERNAL_SERVER_ERROR.getCode(), exception.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomHallConfigPackageImportRespVO importPackage(byte[] content) {
        try {
            if (content == null || content.length == 0) {
                throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: package content is empty");
            }
            ParsedPackage parsedPackage = parsePackage(content);
            ValidatedPackage validatedPackage = validatePackage(parsedPackage);
            List<Long> uploadedFileIds = new ArrayList<>();
            try {
                return applyPackage(validatedPackage, uploadedFileIds);
            } catch (RuntimeException exception) {
                cleanupUploadedFiles(uploadedFileIds, exception);
                throw exception;
            }
        } catch (ServiceException exception) {
            throw exception;
        } catch (IllegalStateException exception) {
            throw exception0(BAD_REQUEST.getCode(), exception.getMessage());
        }
    }

    private ManifestHall exportHall(ShowroomHall hall, List<ExportedBinaryAsset> assets) {
        String hallCode = normalizeRequired(hall.hallCode(),
                "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: hallCode is required");
        ManifestBinaryRef canvasBackground = exportBackgroundAsset(hallCode, hall.canvasBackgroundImageUrl(), assets);
        ManifestBinaryRef previewAsset = exportPreviewAsset(hall, hallCode, assets);
        List<ManifestNarration> narrations = exportNarrations(hall, hallCode, assets);
        List<ManifestHallItemMapping> itemMappings = hall.itemMappings().stream()
                .sorted(Comparator.comparing(ShowroomHallItemMapping::displayOrder))
                .map(this::exportItemMapping)
                .toList();
        return new ManifestHall(
                hallCode,
                normalizeRequired(hall.name(),
                        "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: hall name is required for " + hallCode),
                normalizeRequired(hall.nameEn(),
                        "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: hall nameEn is required for " + hallCode),
                nullToEmpty(hall.description()),
                nullToEmpty(hall.descriptionEn()),
                canvasBackground,
                itemMappings,
                previewAsset,
                narrations);
    }

    private ManifestBinaryRef exportBackgroundAsset(String hallCode, String canvasBackgroundImageUrl,
                                                    List<ExportedBinaryAsset> assets) {
        if (!StrUtil.isNotBlank(canvasBackgroundImageUrl)) {
            return null;
        }
        ResolvedBinarySource binarySource = sourceFileReader.readByAdminUrl(
                hallCode + "-background", "background", canvasBackgroundImageUrl);
        String assetPath = hallAssetPath(hallCode, "background", binarySource.sourceKey(), binarySource.mimeType());
        assets.add(new ExportedBinaryAsset(assetPath, binarySource.bytes()));
        return new ManifestBinaryRef(assetPath);
    }

    private ManifestBinaryRef exportPreviewAsset(ShowroomHall hall, String hallCode, List<ExportedBinaryAsset> assets) {
        Optional<ShowroomPreviewAssetVersion> liveVersion = previewAssetService.live(
                new ShowroomPreviewAssetKey(ShowroomPreviewAssetTargetType.HALL, hall.hallId()));
        if (liveVersion.isEmpty()) {
            return null;
        }
        Long fileId = requireNonNull(liveVersion.get().files().desktopFileId(),
                "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: hall preview fileId is required for " + hallCode);
        ResolvedBinarySource binarySource = sourceFileReader.readFileById(
                hallCode + "-preview", "preview", fileId, liveVersion.get().id(), null);
        String assetPath = hallAssetPath(hallCode, "preview", binarySource.sourceKey(), binarySource.mimeType());
        assets.add(new ExportedBinaryAsset(assetPath, binarySource.bytes()));
        return new ManifestBinaryRef(assetPath);
    }

    private List<ManifestNarration> exportNarrations(ShowroomHall hall, String hallCode,
                                                     List<ExportedBinaryAsset> assets) {
        List<ManifestNarration> narrations = new ArrayList<>();
        for (ShowroomNarrationLanguage language : List.of(ShowroomNarrationLanguage.ZH, ShowroomNarrationLanguage.EN)) {
            Optional<ShowroomNarrationVersion> liveVersion = narrationService.live(new ShowroomNarrationKey(
                    ShowroomNarrationTargetType.HALL,
                    hall.hallId(),
                    ShowroomNarrationAudienceType.PUBLIC,
                    language));
            if (liveVersion.isEmpty()) {
                continue;
            }
            ShowroomNarrationVersion version = liveVersion.get();
            Long audioFileId = requireNonNull(version.audioFileId(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: hall narration audioFileId is required for "
                            + hallCode + "-" + language.name());
            Integer duration = requirePositive(version.audioDurationSeconds(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: hall narration duration is required for "
                            + hallCode + "-" + language.name());
            ResolvedBinarySource binarySource = sourceFileReader.readFileById(
                    hallCode + "-narration-" + language.name().toLowerCase(), "narration", audioFileId, null,
                    version.id());
            String assetPath = hallAssetPath(hallCode, "narration-" + language.name().toLowerCase(),
                    binarySource.sourceKey(), binarySource.mimeType());
            assets.add(new ExportedBinaryAsset(assetPath, binarySource.bytes()));
            narrations.add(new ManifestNarration(language.name(), normalizeRequired(version.scriptText(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: hall narration script is required for "
                            + hallCode + "-" + language.name()), nullToEmpty(version.voice()), duration,
                    new ManifestBinaryRef(assetPath)));
        }
        return narrations;
    }

    private ManifestHallItemMapping exportItemMapping(ShowroomHallItemMapping itemMapping) {
        String itemCode;
        if (ShowroomHallItemMapping.TYPE_PRODUCT.equals(itemMapping.itemType())) {
            ShowroomProductDO product = requireProduct(itemMapping.itemId());
            itemCode = normalizeRequired(product.getProductCode(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: productCode is required for hall mapping");
        } else if (ShowroomHallItemMapping.TYPE_AWARD.equals(itemMapping.itemType())) {
            ShowroomAwardDO award = requireAward(itemMapping.itemId());
            itemCode = normalizeRequired(award.getAwardCode(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: awardCode is required for hall mapping");
        } else {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: unsupported itemType "
                    + itemMapping.itemType());
        }
        return new ManifestHallItemMapping(itemMapping.itemType(), itemCode, itemMapping.displayOrder(),
                stripZeros(itemMapping.layoutX()), stripZeros(itemMapping.layoutY()),
                stripZeros(itemMapping.layoutWidth()), stripZeros(itemMapping.layoutHeight()));
    }

    private ParsedPackage parsePackage(byte[] content) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (entries.put(entry.getName(), zip.readAllBytes()) != null) {
                    throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: duplicate zip entry "
                            + entry.getName());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: package is not a valid zip",
                    exception);
        }
        byte[] manifestBytes = entries.remove(MANIFEST_PATH);
        if (manifestBytes == null || manifestBytes.length == 0) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: manifest.json is missing");
        }
        PackageManifest manifest;
        try {
            manifest = JsonUtils.parseObject(manifestBytes, PackageManifest.class);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: manifest.json is invalid",
                    exception);
        }
        return new ParsedPackage(manifest, entries);
    }

    private ValidatedPackage validatePackage(ParsedPackage parsedPackage) {
        PackageManifest manifest = Objects.requireNonNull(parsedPackage.manifest(),
                "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: manifest is required");
        if (!SCHEMA_VERSION.equals(manifest.schemaVersion())) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: unsupported schemaVersion "
                    + manifest.schemaVersion());
        }
        LinkedHashSet<String> keywordNames = new LinkedHashSet<>();
        for (ManifestKeyword keyword : defaultList(manifest.keywords())) {
            String nameZh = normalizeRequired(keyword.nameZh(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: keyword nameZh is required");
            normalizeRequired(keyword.nameEn(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: keyword nameEn is required");
            if (!keywordNames.add(nameZh)) {
                throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: duplicate keyword nameZh "
                        + nameZh);
            }
        }

        Map<String, byte[]> assets = parsedPackage.assets();
        LinkedHashSet<String> hallCodes = new LinkedHashSet<>();
        LinkedHashSet<String> productCodes = new LinkedHashSet<>();
        LinkedHashSet<String> awardCodes = new LinkedHashSet<>();
        List<ValidatedHall> halls = new ArrayList<>();
        for (ManifestHall hall : defaultList(manifest.halls())) {
            halls.add(validateHall(hall, assets, hallCodes, productCodes, awardCodes));
        }
        Map<String, ShowroomProductDO> productsByCode = loadProducts(productCodes);
        Map<String, ShowroomAwardDO> awardsByCode = loadAwards(awardCodes);
        validateReferencedCodes(productCodes, productsByCode.keySet(), "productCode");
        validateReferencedCodes(awardCodes, awardsByCode.keySet(), "awardCode");
        return new ValidatedPackage(manifest, halls, productsByCode, awardsByCode);
    }

    private ValidatedHall validateHall(ManifestHall hall,
                                       Map<String, byte[]> assets,
                                       Set<String> hallCodes,
                                       Set<String> productCodes,
                                       Set<String> awardCodes) {
        String hallCode = normalizeRequired(hall.hallCode(),
                "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: hallCode is required");
        if (!hallCodes.add(hallCode)) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: duplicate hallCode "
                    + hallCode);
        }
        normalizeRequired(hall.name(),
                "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: hall name is required for " + hallCode);
        normalizeRequired(hall.nameEn(),
                "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: hall nameEn is required for " + hallCode);
        ValidatedBinaryAsset canvasBackground = validateOptionalBinaryAsset(assets, hall.canvasBackground(),
                "hall canvasBackground", hallCode);
        ValidatedBinaryAsset previewAsset = validateOptionalBinaryAsset(assets, hall.previewAsset(),
                "hall previewAsset", hallCode);
        LinkedHashSet<String> itemKeys = new LinkedHashSet<>();
        LinkedHashSet<Integer> displayOrders = new LinkedHashSet<>();
        List<ValidatedHallItemMapping> itemMappings = new ArrayList<>();
        for (ManifestHallItemMapping itemMapping : defaultList(hall.itemMappings())) {
            String itemType = normalizeRequired(itemMapping.itemType(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: hall itemType is required for " + hallCode)
                    .toUpperCase();
            String itemCode = normalizeRequired(itemMapping.itemCode(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: hall itemCode is required for " + hallCode);
            Integer displayOrder = requirePositive(itemMapping.displayOrder(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: hall displayOrder is required for " + hallCode);
            if (!itemKeys.add(itemType + ":" + itemCode)) {
                throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: duplicate hall item "
                        + itemType + ":" + itemCode + " for " + hallCode);
            }
            if (!displayOrders.add(displayOrder)) {
                throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: duplicate displayOrder "
                        + displayOrder + " for " + hallCode);
            }
            if (ShowroomHallItemMapping.TYPE_PRODUCT.equals(itemType)) {
                productCodes.add(itemCode);
            } else if (ShowroomHallItemMapping.TYPE_AWARD.equals(itemType)) {
                awardCodes.add(itemCode);
            } else {
                throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: unsupported hall itemType "
                        + itemType);
            }
            itemMappings.add(new ValidatedHallItemMapping(itemType, itemCode, displayOrder,
                    itemMapping.layoutX(), itemMapping.layoutY(), itemMapping.layoutWidth(), itemMapping.layoutHeight()));
        }
        LinkedHashMap<String, ValidatedNarration> narrations = new LinkedHashMap<>();
        for (ManifestNarration narration : defaultList(hall.narrations())) {
            String language = normalizeRequired(narration.language(),
                    "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: hall narration language is required for " + hallCode)
                    .toUpperCase();
            if (!ShowroomNarrationLanguage.ZH.name().equals(language)
                    && !ShowroomNarrationLanguage.EN.name().equals(language)) {
                throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: unsupported narration language "
                        + language + " for " + hallCode);
            }
            if (narrations.containsKey(language)) {
                throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: duplicate narration language "
                        + language + " for " + hallCode);
            }
            ValidatedBinaryAsset audioAsset = validateRequiredBinaryAsset(assets, narration.audioAsset(),
                    "hall narration audio", hallCode + "-" + language);
            narrations.put(language, new ValidatedNarration(language,
                    normalizeRequired(narration.scriptText(),
                            "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: hall narration scriptText is required for "
                                    + hallCode + "-" + language),
                    nullToEmpty(narration.voice()),
                    requirePositive(narration.duration(),
                            "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: hall narration duration is required for "
                                    + hallCode + "-" + language),
                    audioAsset));
        }
        return new ValidatedHall(hallCode, hall.name(), hall.nameEn(), nullToEmpty(hall.description()),
                nullToEmpty(hall.descriptionEn()), canvasBackground, itemMappings, previewAsset, narrations);
    }

    private ValidatedBinaryAsset validateOptionalBinaryAsset(Map<String, byte[]> assets, ManifestBinaryRef binaryRef,
                                                             String label, String owner) {
        if (binaryRef == null || !StrUtil.isNotBlank(binaryRef.assetPath())) {
            return null;
        }
        return validateRequiredBinaryAsset(assets, binaryRef, label, owner);
    }

    private ValidatedBinaryAsset validateRequiredBinaryAsset(Map<String, byte[]> assets, ManifestBinaryRef binaryRef,
                                                             String label, String owner) {
        String assetPath = normalizeRequired(binaryRef.assetPath(),
                "SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: " + label + " assetPath is required for " + owner);
        byte[] bytes = assets.get(assetPath);
        if (bytes == null) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_ASSET_MISSING: " + assetPath);
        }
        if (bytes.length == 0) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_ASSET_INVALID: asset is empty " + assetPath);
        }
        String fileName = fileNameOf(assetPath);
        String mimeType = FileTypeUtils.getMineType(bytes, fileName);
        if (!StrUtil.isNotBlank(mimeType)) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_ASSET_INVALID: asset mimeType is invalid "
                    + assetPath);
        }
        return new ValidatedBinaryAsset(assetPath, fileName, bytes, mimeType);
    }

    private Map<String, ShowroomProductDO> loadProducts(Collection<String> productCodes) {
        Map<String, ShowroomProductDO> productsByCode = new LinkedHashMap<>();
        for (String productCode : productCodes) {
            ShowroomProductDO product = productMapper.selectByProductCode(productCode);
            if (product != null) {
                productsByCode.put(productCode, product);
            }
        }
        return productsByCode;
    }

    private Map<String, ShowroomAwardDO> loadAwards(Collection<String> awardCodes) {
        Map<String, ShowroomAwardDO> awardsByCode = new LinkedHashMap<>();
        for (String awardCode : awardCodes) {
            ShowroomAwardDO award = awardMapper.selectByAwardCode(awardCode);
            if (award != null) {
                awardsByCode.put(awardCode, award);
            }
        }
        return awardsByCode;
    }

    private void validateReferencedCodes(Set<String> expectedCodes, Set<String> existingCodes, String label) {
        LinkedHashSet<String> missingCodes = new LinkedHashSet<>(expectedCodes);
        missingCodes.removeAll(existingCodes);
        if (!missingCodes.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_REFERENCE_MISSING: missing " + label
                    + "(s): " + String.join(", ", missingCodes));
        }
    }

    private ShowroomHallConfigPackageImportRespVO applyPackage(ValidatedPackage validatedPackage,
                                                               List<Long> uploadedFileIds) {
        PackageManifest manifest = validatedPackage.manifest();
        List<ShowroomKeywordDO> existingKeywords = keywordMapper.selectListOrdered();
        List<ShowroomHallDO> existingHalls = hallMapper.selectListOrdered();
        Map<String, ShowroomHallDO> existingHallByCode = existingHalls.stream()
                .collect(LinkedHashMap::new,
                        (map, hall) -> map.put(hall.getHallCode(), hall),
                        LinkedHashMap::putAll);
        LinkedHashSet<String> packageKeywordNames = manifest.keywords().stream()
                .map(ManifestKeyword::nameZh)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        LinkedHashSet<String> packageHallCodes = validatedPackage.halls().stream()
                .map(ValidatedHall::hallCode)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        int removedKeywordCount = (int) existingKeywords.stream()
                .map(ShowroomKeywordDO::getNameZh)
                .filter(nameZh -> !packageKeywordNames.contains(nameZh))
                .count();
        int removedHallCount = (int) existingHalls.stream()
                .map(ShowroomHallDO::getHallCode)
                .filter(hallCode -> !packageHallCodes.contains(hallCode))
                .count();

        replaceKeywords(manifest.keywords());
        int previewAssetCount = 0;
        int narrationCount = 0;
        int backgroundAssetCount = 0;

        int displayOrder = 1;
        for (ValidatedHall validatedHall : validatedPackage.halls()) {
            ShowroomHall hall = upsertHall(validatedHall, existingHallByCode.get(validatedHall.hallCode()), displayOrder);
            Map<String, Long> productIds = validatedPackage.productsByCode().entrySet().stream()
                    .collect(LinkedHashMap::new,
                            (map, entry) -> map.put(entry.getKey(), entry.getValue().getId()),
                            LinkedHashMap::putAll);
            Map<String, Long> awardIds = validatedPackage.awardsByCode().entrySet().stream()
                    .collect(LinkedHashMap::new,
                            (map, entry) -> map.put(entry.getKey(), entry.getValue().getId()),
                            LinkedHashMap::putAll);
            replaceHallMappings(hall.hallId(), validatedHall.itemMappings(), productIds, awardIds);
            clearHallPreviewAssets(hall.hallId());
            clearHallNarrations(hall.hallId());
            if (validatedHall.canvasBackground() == null) {
                contentService.updateHallCanvasBackground(hall.hallId(), null);
            } else {
                String backgroundUrl = uploadAdminFile(validatedHall.canvasBackground(),
                        "hall-" + validatedHall.hallCode() + "-background", BACKGROUND_DIRECTORY, uploadedFileIds);
                contentService.updateHallCanvasBackground(hall.hallId(), backgroundUrl);
                backgroundAssetCount++;
            }
            if (validatedHall.previewAsset() != null) {
                Long imageFileId = uploadFile(validatedHall.previewAsset(),
                        "hall-" + validatedHall.hallCode() + "-preview", PREVIEW_DIRECTORY, uploadedFileIds);
                ShowroomPreviewAssetVersion previewVersion = previewAssetService.bindStaticPreviewAssets(
                        new ShowroomPreviewAssetDraftCommand(
                                ShowroomPreviewAssetTargetType.HALL,
                                hall.hallId(),
                                hall.hallId(),
                                new ShowroomPreviewAssetFiles(imageFileId, imageFileId, imageFileId)));
                previewAssetService.publishDirectly(previewVersion.id());
                previewAssetCount++;
            }
            for (ValidatedNarration narration : validatedHall.narrations().values()) {
                Long audioFileId = uploadFile(narration.audioAsset(),
                        "hall-" + validatedHall.hallCode() + "-narration-" + narration.language().toLowerCase(),
                        NARRATION_DIRECTORY, uploadedFileIds);
                ShowroomNarrationVersion draft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                        ShowroomNarrationTargetType.HALL,
                        hall.hallId(),
                        hall.hallId(),
                        ShowroomNarrationAudienceType.PUBLIC,
                        ShowroomNarrationLanguage.valueOf(narration.language()),
                        narration.scriptText(),
                        false));
                draft = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                        draft.id(),
                        audioFileId,
                        narration.duration(),
                        narration.voice()));
                narrationService.publishDirectly(draft.id());
                narrationCount++;
            }
            displayOrder++;
        }
        for (ShowroomHallDO hall : existingHalls) {
            if (packageHallCodes.contains(hall.getHallCode())) {
                continue;
            }
            clearHallPreviewAssets(hall.getId());
            clearHallNarrations(hall.getId());
            hallProductMapper.deleteByHallIdForce(currentTenantId(), hall.getId());
            hallItemMapper.deleteByHallIdForce(currentTenantId(), hall.getId());
            hallMapper.deleteById(hall.getId());
        }
        return new ShowroomHallConfigPackageImportRespVO(
                validatedPackage.halls().size(),
                manifest.keywords().size(),
                previewAssetCount,
                narrationCount,
                backgroundAssetCount,
                removedHallCount,
                removedKeywordCount,
                validatedPackage.productsByCode().size(),
                validatedPackage.awardsByCode().size());
    }

    private void replaceKeywords(List<ManifestKeyword> keywords) {
        keywordMapper.deleteByTenantId(currentTenantId());
        for (ManifestKeyword keyword : keywords) {
            ShowroomKeywordDO keywordDO = new ShowroomKeywordDO();
            keywordDO.setTenantId(currentTenantId());
            keywordDO.setNameZh(keyword.nameZh());
            keywordDO.setNameEn(keyword.nameEn());
            keywordMapper.insert(keywordDO);
        }
    }

    private ShowroomHall upsertHall(ValidatedHall validatedHall, ShowroomHallDO existing, int displayOrder) {
        ShowroomHall hall;
        if (existing == null) {
            hall = contentService.createHall(validatedHall.hallCode(), validatedHall.name(), validatedHall.nameEn(),
                    validatedHall.description(), validatedHall.descriptionEn());
            existing = hallMapper.selectByHallCode(validatedHall.hallCode());
            if (existing == null) {
                throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: hall was not created "
                        + validatedHall.hallCode());
            }
        } else {
            hall = contentService.updateHall(existing.getId(), validatedHall.name(), validatedHall.nameEn(),
                    validatedHall.description(), validatedHall.descriptionEn());
        }
        existing.setDisplayOrder(displayOrder);
        existing.setStatus("ACTIVE");
        existing.setName(validatedHall.name());
        existing.setNameEn(validatedHall.nameEn());
        existing.setDescription(validatedHall.description());
        existing.setDescriptionEn(validatedHall.descriptionEn());
        hallMapper.updateById(existing);
        return new ShowroomHall(existing.getId(), existing.getHallCode(), existing.getName(), existing.getNameEn(),
                existing.getDescription(), existing.getDescriptionEn(), existing.getCanvasBackgroundImageUrl(),
                List.of(), List.of());
    }

    private void replaceHallMappings(Long hallId,
                                     List<ValidatedHallItemMapping> itemMappings,
                                     Map<String, Long> productIds,
                                     Map<String, Long> awardIds) {
        List<ShowroomHallItemMapping> resolvedMappings = itemMappings.stream()
                .sorted(Comparator.comparing(ValidatedHallItemMapping::displayOrder))
                .map(itemMapping -> {
                    Long itemId;
                    if (ShowroomHallItemMapping.TYPE_PRODUCT.equals(itemMapping.itemType())) {
                        itemId = productIds.get(itemMapping.itemCode());
                    } else {
                        itemId = awardIds.get(itemMapping.itemCode());
                    }
                    if (itemId == null) {
                        throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_REFERENCE_MISSING: missing "
                                + itemMapping.itemType() + " itemCode " + itemMapping.itemCode());
                    }
                    return new ShowroomHallItemMapping(itemMapping.itemType(), itemId, itemMapping.displayOrder(),
                            itemMapping.layoutX(), itemMapping.layoutY(),
                            itemMapping.layoutWidth(), itemMapping.layoutHeight());
                })
                .toList();
        contentService.replaceHallItemMappings(hallId, resolvedMappings);
    }

    private void clearHallPreviewAssets(Long hallId) {
        previewAssetVersionMapper.deleteByTarget(currentTenantId(), ShowroomPreviewAssetTargetType.HALL.name(), hallId);
    }

    private void clearHallNarrations(Long hallId) {
        narrationVersionMapper.deleteByTarget(currentTenantId(), ShowroomNarrationTargetType.HALL.name(), hallId);
    }

    private String uploadAdminFile(ValidatedBinaryAsset asset,
                                   String fileNamePrefix,
                                   String directory,
                                   List<Long> uploadedFileIds) {
        Long fileId = uploadFile(asset, fileNamePrefix, directory, uploadedFileIds);
        FileDO file = fileService.getFile(fileId);
        if (file == null) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: uploaded file record not found "
                    + fileId);
        }
        if (file.getConfigId() == null || !StrUtil.isNotBlank(file.getPath())) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: uploaded file metadata is incomplete "
                    + fileId);
        }
        return "/admin-api/infra/file/" + file.getConfigId() + "/get/"
                + UriUtils.encodePath(file.getPath(), StandardCharsets.UTF_8);
    }

    private Long uploadFile(ValidatedBinaryAsset asset,
                            String fileNamePrefix,
                            String directory,
                            List<Long> uploadedFileIds) {
        String extension = extensionOf(asset.fileName());
        String fileName = extension == null ? fileNamePrefix : fileNamePrefix + "." + extension;
        Long fileId = fileService.createFileAndReturnId(asset.bytes(), fileName, directory, asset.mimeType());
        if (fileId == null) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_IMPORT_FAILED: uploaded file id is empty for "
                    + asset.assetPath());
        }
        uploadedFileIds.add(fileId);
        return fileId;
    }

    private void cleanupUploadedFiles(List<Long> uploadedFileIds, RuntimeException original) {
        for (Long fileId : uploadedFileIds) {
            if (fileId == null) {
                continue;
            }
            try {
                fileService.deleteFile(fileId);
            } catch (Exception cleanupException) {
                original.addSuppressed(cleanupException);
            }
        }
    }

    private TenantDO requireTenant(Long tenantId) {
        TenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: tenant not found " + tenantId);
        }
        return tenant;
    }

    private ShowroomProductDO requireProduct(Long productId) {
        ShowroomProductDO product = productMapper.selectById(productId);
        if (product == null || !Objects.equals(currentTenantId(), product.getTenantId())) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: product not found " + productId);
        }
        return product;
    }

    private ShowroomAwardDO requireAward(Long awardId) {
        ShowroomAwardDO award = awardMapper.selectById(awardId);
        if (award == null || !Objects.equals(currentTenantId(), award.getTenantId())) {
            throw new IllegalStateException("SHOWROOM_HALL_CONFIG_PACKAGE_EXPORT_FAILED: award not found " + awardId);
        }
        return award;
    }

    private Long currentTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private static void writeZipEntry(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(bytes);
        zip.closeEntry();
    }

    private static String hallAssetPath(String hallCode, String baseName, String sourceLocator, String mimeType) {
        String extension = extensionFromSource(sourceLocator);
        if (extension == null) {
            extension = extensionFromMimeType(mimeType);
        }
        return HALL_ASSET_ROOT + hallCode + "/" + baseName + (extension == null ? "" : "." + extension);
    }

    private static String extensionFromSource(String sourceLocator) {
        if (!StrUtil.isNotBlank(sourceLocator)) {
            return null;
        }
        String normalized = sourceLocator;
        int colonIndex = normalized.indexOf(':');
        if (colonIndex >= 0 && colonIndex + 1 < normalized.length()) {
            normalized = normalized.substring(colonIndex + 1);
        }
        return extensionOf(normalized);
    }

    private static String extensionOf(String path) {
        if (!StrUtil.isNotBlank(path)) {
            return null;
        }
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == path.length() - 1) {
            return null;
        }
        return path.substring(dotIndex + 1).toLowerCase();
    }

    private static String extensionFromMimeType(String mimeType) {
        if (!StrUtil.isNotBlank(mimeType)) {
            return null;
        }
        return switch (mimeType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "audio/mpeg" -> "mp3";
            case "audio/wav", "audio/x-wav" -> "wav";
            case "audio/mp4" -> "m4a";
            default -> null;
        };
    }

    private static String fileNameOf(String assetPath) {
        int slashIndex = assetPath.lastIndexOf('/');
        return slashIndex >= 0 ? assetPath.substring(slashIndex + 1) : assetPath;
    }

    private static String normalizeRequired(String value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException(message);
        }
        return normalized;
    }

    private static Integer requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static BigDecimal stripZeros(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

    private static <T> List<T> defaultList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record ExportedBinaryAsset(String assetPath, byte[] bytes) {
    }

    private record ParsedPackage(PackageManifest manifest, Map<String, byte[]> assets) {
    }

    private record ValidatedPackage(PackageManifest manifest,
                                    List<ValidatedHall> halls,
                                    Map<String, ShowroomProductDO> productsByCode,
                                    Map<String, ShowroomAwardDO> awardsByCode) {
    }

    private record ValidatedHall(String hallCode,
                                 String name,
                                 String nameEn,
                                 String description,
                                 String descriptionEn,
                                 ValidatedBinaryAsset canvasBackground,
                                 List<ValidatedHallItemMapping> itemMappings,
                                 ValidatedBinaryAsset previewAsset,
                                 Map<String, ValidatedNarration> narrations) {
    }

    private record ValidatedHallItemMapping(String itemType,
                                            String itemCode,
                                            Integer displayOrder,
                                            BigDecimal layoutX,
                                            BigDecimal layoutY,
                                            BigDecimal layoutWidth,
                                            BigDecimal layoutHeight) {
    }

    private record ValidatedNarration(String language,
                                      String scriptText,
                                      String voice,
                                      Integer duration,
                                      ValidatedBinaryAsset audioAsset) {
    }

    private record ValidatedBinaryAsset(String assetPath, String fileName, byte[] bytes, String mimeType) {
    }

    private record PackageManifest(String schemaVersion,
                                   String exportedAt,
                                   Long sourceTenantId,
                                   String sourceTenantName,
                                   List<ManifestKeyword> keywords,
                                   List<ManifestHall> halls) {
    }

    private record ManifestKeyword(String nameZh, String nameEn) {
    }

    private record ManifestHall(String hallCode,
                                String name,
                                String nameEn,
                                String description,
                                String descriptionEn,
                                ManifestBinaryRef canvasBackground,
                                List<ManifestHallItemMapping> itemMappings,
                                ManifestBinaryRef previewAsset,
                                List<ManifestNarration> narrations) {
    }

    private record ManifestBinaryRef(String assetPath) {
    }

    private record ManifestHallItemMapping(String itemType,
                                           String itemCode,
                                           Integer displayOrder,
                                           BigDecimal layoutX,
                                           BigDecimal layoutY,
                                           BigDecimal layoutWidth,
                                           BigDecimal layoutHeight) {
    }

    private record ManifestNarration(String language,
                                     String scriptText,
                                     String voice,
                                     Integer duration,
                                     ManifestBinaryRef audioAsset) {
    }

    private static final class CollUtil {
        private static boolean isEmpty(Collection<?> collection) {
            return collection == null || collection.isEmpty();
        }
    }

}
