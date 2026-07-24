package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardSnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallCanvasLayoutPolicy;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallItemMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductAttachment;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.asset.ShowroomPreviewAssetVersionDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import cn.iocoder.yudao.module.showroom.foundation.enums.ShowroomFieldTierEnum;
import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldCatalog;
import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldDisplaySupport;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import cn.iocoder.yudao.module.showroom.narration.ShowroomPersistentNarrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ShowroomReleaseAssembler {

    private static final Logger log = LoggerFactory.getLogger(ShowroomReleaseAssembler.class);

    private final ShowroomPersistentContentService contentService;
    private final ShowroomPreviewAssetVersionMapper previewAssetVersionMapper;
    private final ShowroomPersistentNarrationService narrationService;
    private final ShowroomReleaseSourceFileReader fileReader;

    public ShowroomReleaseAssembler(ShowroomPersistentContentService contentService,
                                    ShowroomPreviewAssetVersionMapper previewAssetVersionMapper,
                                    ShowroomPersistentNarrationService narrationService,
                                    ShowroomReleaseSourceFileReader fileReader) {
        this.contentService = contentService;
        this.previewAssetVersionMapper = previewAssetVersionMapper;
        this.narrationService = narrationService;
        this.fileReader = fileReader;
    }

    public ShowroomReleaseSourceSnapshot resolveSourceSnapshot() {
        ShowroomCompanyRevision companyRevision = contentService.requireCurrentCompanyRevision();
        ShowroomCompanySnapshot companySnapshot = contentService.getCompany(companyRevision.companyId());
        ResolvedBinarySource companyHomeImage;
        ShowroomReleaseSourceSnapshot.ResolvedNarrationPair companyNarrations;
        try {
            String companyCoverImage = requireText(companyRevision.fields().get("cover_image"),
                    "SHOWROOM_REQUIRED_FIELD_MISSING: company cover_image is required");
            companyHomeImage = fileReader.readByAdminUrl(
                    ShowroomReleaseConstants.OWNER_COMPANY_ASSET_ID,
                    ShowroomReleaseConstants.ASSET_TYPE_IMAGE,
                    companyCoverImage);
            companyNarrations = resolveNarrationPair(
                    ShowroomNarrationTargetType.COMPANY,
                    companyRevision.companyId(),
                    companyRevision.revisionId(),
                    ShowroomReleaseConstants.OWNER_COMPANY_AUDIO_ZH_ASSET_ID,
                    ShowroomReleaseConstants.OWNER_COMPANY_AUDIO_EN_ASSET_ID,
                    "company");
        } catch (RuntimeException exception) {
            throw new IllegalStateException("SHOWROOM_RELEASE_COMPANY_BLOCKED: companyId="
                    + companyRevision.companyId() + " revisionId=" + companyRevision.revisionId()
                    + " reason=" + summarizeFailureReason(exception), exception);
        }

        List<Long> previewAssetVersionIds = new ArrayList<>();
        List<Long> narrationVersionIds = new ArrayList<>();
        previewAssetVersionIds.add(requirePreviewVersion(ShowroomReleaseConstants.TARGET_COMPANY,
                companyRevision.companyId(), null).getId());
        narrationVersionIds.add(companyNarrations.zh().id());
        narrationVersionIds.add(companyNarrations.en().id());

        List<ShowroomHall> hallList = contentService.listHallsForReleaseSnapshot();
        if (hallList.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: hall snapshot is required for release");
        }
        List<ShowroomReleaseSourceSnapshot.ResolvedHall> resolvedHalls = new ArrayList<>();
        Map<Long, ShowroomReleaseSourceSnapshot.ResolvedProduct> productsById = new LinkedHashMap<>();
        Map<Long, ShowroomReleaseSourceSnapshot.ResolvedAward> awardsById = new LinkedHashMap<>();
        List<Long> productRevisionIds = new ArrayList<>();
        List<Long> awardRevisionIds = new ArrayList<>();
        for (ShowroomHall hall : hallList) {
            requireHallReleaseText(hall);
            if (hall.itemMappings().isEmpty()) {
                throw new IllegalStateException("SHOWROOM_RELEASE_HALL_BLOCKED: hallId=" + hall.hallId()
                        + " hallCode=" + hall.hallCode() + " reason=hall has no item mappings");
            }
            List<ShowroomHallItemMapping> releaseMappings;
            try {
                releaseMappings = requireItemCanvasLayout(hall.itemMappings());
            } catch (RuntimeException exception) {
                throw new IllegalStateException("SHOWROOM_RELEASE_HALL_BLOCKED: hallId=" + hall.hallId()
                        + " hallCode=" + hall.hallCode() + " reason=" + summarizeFailureReason(exception),
                        exception);
            }
            ShowroomPreviewAssetVersionDO hallPreview;
            ResolvedBinarySource hallPreviewImage;
            ShowroomReleaseSourceSnapshot.ResolvedNarrationPair hallNarrations;
            try {
                hallPreview = requirePreviewVersion(ShowroomReleaseConstants.TARGET_HALL, hall.hallId(), null);
                hallPreviewImage = fileReader.readFileById("hall-" + hall.hallId() + "-preview",
                        ShowroomReleaseConstants.ASSET_TYPE_IMAGE,
                        hallPreview.getImageFileId(), hallPreview.getId(), null);
                hallNarrations = resolveNarrationPair(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                        "hall-" + hall.hallId() + "-audio-zh",
                        "hall-" + hall.hallId() + "-audio-en",
                        "hall");
            } catch (RuntimeException exception) {
                throw new IllegalStateException("SHOWROOM_RELEASE_HALL_BLOCKED: hallId=" + hall.hallId()
                        + " hallCode=" + hall.hallCode() + " reason=" + summarizeFailureReason(exception),
                        exception);
            }
            List<ShowroomHallItemMapping> retainedMappings = new ArrayList<>();
            for (ShowroomHallItemMapping mapping : releaseMappings) {
                if (ShowroomHallItemMapping.TYPE_PRODUCT.equals(mapping.itemType())) {
                    Long productId = mapping.itemId();
                    if (productsById.containsKey(productId)) {
                        retainedMappings.add(mapping);
                        continue;
                    }
                    try {
                        productsById.put(productId, resolveProduct(productId,
                                productRevisionIds, previewAssetVersionIds, narrationVersionIds));
                        retainedMappings.add(mapping);
                    } catch (RuntimeException exception) {
                        throw new IllegalStateException("SHOWROOM_RELEASE_PRODUCT_BLOCKED: hallId=" + hall.hallId()
                                + " hallCode=" + hall.hallCode() + " productId=" + productId + " productCode="
                                + resolveProductCode(productId) + " reason=" + summarizeFailureReason(exception),
                                exception);
                    }
                    continue;
                }
                Long awardId = mapping.itemId();
                if (awardsById.containsKey(awardId)) {
                    retainedMappings.add(mapping);
                    continue;
                }
                try {
                    awardsById.put(awardId, resolveAward(awardId, awardRevisionIds, narrationVersionIds));
                    retainedMappings.add(mapping);
                } catch (RuntimeException exception) {
                    throw new IllegalStateException("SHOWROOM_RELEASE_AWARD_BLOCKED: hallId=" + hall.hallId()
                            + " hallCode=" + hall.hallCode() + " awardId=" + awardId + " awardCode="
                            + resolveAwardCode(awardId) + " reason=" + summarizeFailureReason(exception),
                            exception);
                }
            }
            if (retainedMappings.isEmpty()) {
                throw new IllegalStateException("SHOWROOM_RELEASE_HALL_BLOCKED: hallId=" + hall.hallId()
                        + " hallCode=" + hall.hallCode()
                        + " reason=hall has no publishable items after validation");
            }
            previewAssetVersionIds.add(hallPreview.getId());
            narrationVersionIds.add(hallNarrations.zh().id());
            narrationVersionIds.add(hallNarrations.en().id());
            resolvedHalls.add(new ShowroomReleaseSourceSnapshot.ResolvedHall(
                    retainHallItems(hall, retainedMappings), hallPreviewImage, hallNarrations));
        }
        if (resolvedHalls.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: no publishable halls available for release");
        }
        if (productsById.isEmpty() && awardsById.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: no publishable live items available for release");
        }

        String hallSnapshotHash = ShowroomReleaseHashSupport.sha256Hex(JsonUtils.toJsonString(
                resolvedHalls.stream().map(ShowroomReleaseSourceSnapshot.ResolvedHall::hall)
                        .map(this::toHallSnapshotHashEntry).toList()));
        String mappingHash = ShowroomReleaseHashSupport.sha256Hex(JsonUtils.toJsonString(
                resolvedHalls.stream().map(ShowroomReleaseSourceSnapshot.ResolvedHall::hall)
                        .flatMap(hall -> hall.itemMappings().stream()
                                .map(mapping -> Map.of("hallId", hall.hallId(), "itemType", mapping.itemType(),
                                        "itemId", mapping.itemId(),
                                        "displayOrder", mapping.displayOrder(),
                                        "layoutX", mapping.layoutX(),
                                        "layoutY", mapping.layoutY(),
                                        "layoutWidth", mapping.layoutWidth(),
                                        "layoutHeight", mapping.layoutHeight())))
                        .toList()));
        return new ShowroomReleaseSourceSnapshot(companyRevision.revisionId(), hallSnapshotHash, mappingHash,
                Instant.now(), companySnapshot, companyRevision, companyHomeImage, companyNarrations,
                List.copyOf(resolvedHalls), Map.copyOf(productsById), Map.copyOf(awardsById),
                List.copyOf(productRevisionIds), List.copyOf(awardRevisionIds),
                List.copyOf(previewAssetVersionIds), List.copyOf(narrationVersionIds));
    }

    public ShowroomMaterializedRelease materializeRelease(ShowroomReleaseSourceSnapshot snapshot, Instant publishedAt) {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    public ShowroomMaterializedRelease materializeRelease(ShowroomReleaseScope scope,
                                                          ShowroomReleaseSourceSnapshot snapshot,
                                                          Instant publishedAt) {
        Objects.requireNonNull(scope, "scope");
        String releaseId = buildReleaseId(scope, publishedAt, snapshot);
        Instant materializedAt = publishedAt.minusSeconds(20);
        List<ShowroomMaterializedRelease.MaterializedAsset> assets = new ArrayList<>();
        List<ShowroomMaterializedRelease.MaterializedAssetRef> assetRefs = new ArrayList<>();
        Map<String, ShowroomMaterializedRelease.MaterializedAsset> assetByKey = new LinkedHashMap<>();

        Map<String, Object> companyDocument = new LinkedHashMap<>();
        List<Map<String, Object>> hallPayloads = new ArrayList<>();
        companyDocument.put("companyId", String.valueOf(snapshot.companyRevision().companyId()));
        companyDocument.put("name", requireText(snapshot.companySnapshot().displayName(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: company display name is required"));
        companyDocument.put("nameEn", requireText(snapshot.companySnapshot().displayNameEn(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: company display name_en is required"));
        companyDocument.put("homeImage", descriptor(assetByKey, assets, assetRefs, ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX,
                snapshot.companyHomeImage(), "company-home-image", materializedAt));
        companyDocument.put("subtitleZh", requireText(snapshot.companyNarrations().zh().scriptText(),
                "SHOWROOM_SCRIPT_MISSING: company ZH narration text is required"));
        companyDocument.put("subtitleEn", requireText(snapshot.companyNarrations().en().scriptText(),
                "SHOWROOM_SCRIPT_MISSING: company EN narration text is required"));
        companyDocument.put("audioZh", descriptor(assetByKey, assets, assetRefs, ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX,
                snapshot.companyNarrations().zhAudio(), "company-audio-zh", materializedAt));
        companyDocument.put("audioEn", descriptor(assetByKey, assets, assetRefs, ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX,
                snapshot.companyNarrations().enAudio(), "company-audio-en", materializedAt));
        companyDocument.put("bilingualPublicFields", companyBilingualFields(snapshot.companyRevision().fields()));

        for (ShowroomReleaseSourceSnapshot.ResolvedHall hall : snapshot.halls()) {
            Map<String, Object> hallMap = new LinkedHashMap<>();
            hallMap.put("hallId", String.valueOf(hall.hall().hallId()));
            hallMap.put("hallCode", requireText(hall.hall().hallCode(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall code is required"));
            hallMap.put("name", requireText(hall.hall().name(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall name is required"));
            hallMap.put("nameEn", requireText(hall.hall().nameEn(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall name_en is required"));
            hallMap.put("description", nullToEmpty(hall.hall().description()));
            hallMap.put("descriptionEn", nullToEmpty(hall.hall().descriptionEn()));
            hallMap.put("previewImage", descriptor(assetByKey, assets, assetRefs, ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX,
                    hall.previewImage(), "hall-preview-image", materializedAt));
            hallMap.put("audioZh", descriptor(assetByKey, assets, assetRefs, ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX,
                    hall.narrations().zhAudio(), "hall-audio-zh", materializedAt));
            hallMap.put("audioEn", descriptor(assetByKey, assets, assetRefs, ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX,
                    hall.narrations().enAudio(), "hall-audio-en", materializedAt));
            List<Map<String, Object>> itemCards = new ArrayList<>();
            List<Map<String, Object>> productCards = new ArrayList<>();
            for (ShowroomHallItemMapping mapping : hall.hall().itemMappings()) {
                Map<String, Object> card = new LinkedHashMap<>();
                card.put("itemType", mapping.itemType());
                card.put("itemId", String.valueOf(mapping.itemId()));
                if (ShowroomHallItemMapping.TYPE_PRODUCT.equals(mapping.itemType())) {
                    ShowroomReleaseSourceSnapshot.ResolvedProduct product = snapshot.productsById().get(mapping.itemId());
                    card.put("productId", String.valueOf(product.snapshot().productId()));
                    card.put("productCode", requireText(product.snapshot().productCode(),
                            "SHOWROOM_REQUIRED_FIELD_MISSING: product code is required"));
                    card.put("itemCode", product.snapshot().productCode());
                    card.put("nameCn", nullToEmpty(product.revision().nameCn()));
                    card.put("nameEn", requireText(product.revision().nameEn(),
                            "SHOWROOM_REQUIRED_FIELD_MISSING: product name_en is required"));
                    card.put("incompleteFlag", product.revision().incomplete());
                    card.put("previewImage", descriptor(assetByKey, assets, assetRefs, ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX,
                            product.previewImage(), "product-preview-image", materializedAt));
                    card.put("detailDocumentId", productDetailDocumentId(product.snapshot().productId()));
                    putLayoutFields(card, mapping);
                    productCards.add(new LinkedHashMap<>(card));
                } else {
                    ShowroomReleaseSourceSnapshot.ResolvedAward award = snapshot.awardsById().get(mapping.itemId());
                    card.put("awardId", String.valueOf(award.snapshot().awardId()));
                    card.put("awardCode", requireText(award.snapshot().awardCode(),
                            "SHOWROOM_REQUIRED_FIELD_MISSING: award code is required"));
                    card.put("itemCode", award.snapshot().awardCode());
                    card.put("nameCn", requireText(award.revision().nameCn(),
                            "SHOWROOM_REQUIRED_FIELD_MISSING: award name_cn is required"));
                    card.put("nameEn", nullToEmpty(award.revision().nameEn()));
                    card.put("incompleteFlag", award.revision().incomplete());
                    card.put("previewImage", descriptor(assetByKey, assets, assetRefs, ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX,
                            award.previewImage(), "award-preview-image", materializedAt));
                    card.put("detailDocumentId", awardDetailDocumentId(award.snapshot().awardId()));
                    putLayoutFields(card, mapping);
                }
                itemCards.add(card);
            }
            hallMap.put("items", itemCards);
            hallMap.put("products", productCards);
            hallPayloads.add(hallMap);
        }

        List<ShowroomMaterializedRelease.MaterializedDocument> documents = new ArrayList<>();
        String websiteIndexJson = JsonUtils.toJsonString(new LinkedHashMap<>(Map.of(
                "documentId", ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX,
                "kind", ShowroomReleaseConstants.DOCUMENT_KIND_WEBSITE_INDEX,
                "releaseId", releaseId
        )));
        Map<String, Object> websiteIndexPayload = new LinkedHashMap<>();
        websiteIndexPayload.put("documentId", ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX);
        websiteIndexPayload.put("kind", ShowroomReleaseConstants.DOCUMENT_KIND_WEBSITE_INDEX);
        websiteIndexPayload.put("releaseId", releaseId);
        websiteIndexPayload.put("company", companyDocument);
        websiteIndexPayload.put("showrooms", hallPayloads);
        String websiteIndexHash = hashJsonWithoutField(websiteIndexPayload, "contentHash");
        websiteIndexPayload.put("contentHash", websiteIndexHash);
        String websiteIndexBody = JsonUtils.toJsonString(websiteIndexPayload);
        documents.add(new ShowroomMaterializedRelease.MaterializedDocument(
                ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX, ShowroomReleaseConstants.DOCUMENT_KIND_WEBSITE_INDEX,
                null, websiteIndexHash, ShowroomReleaseHttpSupport.utf8Bytes(websiteIndexBody), materializedAt,
                websiteIndexBody));

        for (ShowroomReleaseSourceSnapshot.ResolvedProduct product : snapshot.productsById().values().stream()
                .sorted(Comparator.comparing(value -> value.snapshot().productId()))
                .toList()) {
            Map<String, Object> detailPayload = new LinkedHashMap<>();
            String documentId = productDetailDocumentId(product.snapshot().productId());
            detailPayload.put("documentId", documentId);
            detailPayload.put("kind", ShowroomReleaseConstants.DOCUMENT_KIND_PRODUCT_DETAIL);
            detailPayload.put("releaseId", releaseId);
            detailPayload.put("productId", String.valueOf(product.snapshot().productId()));
            detailPayload.put("subtitleZh", requireText(product.narrations().zh().scriptText(),
                    "SHOWROOM_SCRIPT_MISSING: product ZH narration text is required"));
            detailPayload.put("subtitleEn", requireText(product.narrations().en().scriptText(),
                    "SHOWROOM_SCRIPT_MISSING: product EN narration text is required"));
            detailPayload.put("audioZh", descriptor(assetByKey, assets, assetRefs, documentId,
                    product.narrations().zhAudio(), "product-audio-zh", materializedAt));
            detailPayload.put("audioEn", descriptor(assetByKey, assets, assetRefs, documentId,
                    product.narrations().enAudio(), "product-audio-en", materializedAt));
            detailPayload.put("bilingualPublicFields", productBilingualFields(product.revision()));
            detailPayload.put("attachments", productAttachments(assetByKey, assets, assetRefs, documentId,
                    product, materializedAt));
            String detailHash = hashJsonWithoutField(detailPayload, "contentHash");
            detailPayload.put("contentHash", detailHash);
            String detailBody = JsonUtils.toJsonString(detailPayload);
            documents.add(new ShowroomMaterializedRelease.MaterializedDocument(documentId,
                    ShowroomReleaseConstants.DOCUMENT_KIND_PRODUCT_DETAIL, product.snapshot().productId(), detailHash,
                    ShowroomReleaseHttpSupport.utf8Bytes(detailBody), materializedAt, detailBody));
        }

        for (ShowroomReleaseSourceSnapshot.ResolvedAward award : snapshot.awardsById().values().stream()
                .sorted(Comparator.comparing(value -> value.snapshot().awardId()))
                .toList()) {
            Map<String, Object> detailPayload = new LinkedHashMap<>();
            String documentId = awardDetailDocumentId(award.snapshot().awardId());
            detailPayload.put("documentId", documentId);
            detailPayload.put("kind", ShowroomReleaseConstants.DOCUMENT_KIND_AWARD_DETAIL);
            detailPayload.put("releaseId", releaseId);
            detailPayload.put("awardId", String.valueOf(award.snapshot().awardId()));
            detailPayload.put("awardCode", requireText(award.snapshot().awardCode(),
                    "SHOWROOM_REQUIRED_FIELD_MISSING: award code is required"));
            detailPayload.put("nameCn", requireText(award.revision().nameCn(),
                    "SHOWROOM_REQUIRED_FIELD_MISSING: award name_cn is required"));
            detailPayload.put("nameEn", nullToEmpty(award.revision().nameEn()));
            detailPayload.put("descriptionZh", requireText(award.revision().fields().get("description_zh"),
                    "SHOWROOM_SCRIPT_MISSING: award ZH narration text is required"));
            detailPayload.put("descriptionEn", requireText(award.revision().fields().get("description_en"),
                    "SHOWROOM_SCRIPT_MISSING: award EN narration text is required"));
            detailPayload.put("issuer", nullToEmpty(award.revision().fields().get("issuer")));
            detailPayload.put("awardDateText", nullToEmpty(award.revision().fields().get("award_date_text")));
            detailPayload.put("subtitleZh", requireText(award.narrations().zh().scriptText(),
                    "SHOWROOM_SCRIPT_MISSING: award ZH narration text is required"));
            detailPayload.put("subtitleEn", requireText(award.narrations().en().scriptText(),
                    "SHOWROOM_SCRIPT_MISSING: award EN narration text is required"));
            detailPayload.put("audioZh", descriptor(assetByKey, assets, assetRefs, documentId,
                    award.narrations().zhAudio(), "award-audio-zh", materializedAt));
            detailPayload.put("audioEn", descriptor(assetByKey, assets, assetRefs, documentId,
                    award.narrations().enAudio(), "award-audio-en", materializedAt));
            detailPayload.put("bilingualPublicFields", awardBilingualFields(award.revision()));
            detailPayload.put("attachments", List.of());
            String detailHash = hashAwardDetailDocument(detailPayload);
            detailPayload.put("contentHash", detailHash);
            String detailBody = JsonUtils.toJsonString(detailPayload);
            documents.add(new ShowroomMaterializedRelease.MaterializedDocument(documentId,
                    ShowroomReleaseConstants.DOCUMENT_KIND_AWARD_DETAIL, award.snapshot().awardId(), detailHash,
                    ShowroomReleaseHttpSupport.utf8Bytes(detailBody), materializedAt, detailBody));
        }

        String legacyProjectionJson = buildLegacyProjectionJson(scope, snapshot, documents, assets);
        String legacyProjectionHash = ShowroomReleaseHashSupport.sha256Hex(legacyProjectionJson);

        List<Map<String, Object>> manifestDocuments = documents.stream()
                .sorted(Comparator.comparing(ShowroomMaterializedRelease.MaterializedDocument::documentId))
                .map(document -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("documentId", document.documentId());
                    map.put("kind", document.kind());
                    if (ShowroomReleaseConstants.DOCUMENT_KIND_AWARD_DETAIL.equals(document.kind())
                            && document.productId() != null) {
                        map.put("awardId", String.valueOf(document.productId()));
                    } else if (document.productId() != null) {
                        map.put("productId", String.valueOf(document.productId()));
                    }
                    map.put("contentHash", document.contentHash());
                    map.put("bytes", document.bytes());
                    return map;
                })
                .toList();
        List<Map<String, Object>> manifestAssets = assets.stream()
                .sorted(Comparator.comparing(ShowroomMaterializedRelease.MaterializedAsset::assetId))
                .map(asset -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("assetId", asset.assetId());
                    map.put("contentHash", asset.contentHash());
                    map.put("assetType", asset.assetType());
                    map.put("mimeType", asset.mimeType());
                    map.put("bytes", asset.bytes());
                    map.put("referencedBy", assetRefs.stream()
                            .filter(ref -> ref.assetId().equals(asset.assetId()) && ref.contentHash().equals(asset.contentHash()))
                            .map(ShowroomMaterializedRelease.MaterializedAssetRef::documentId)
                            .distinct()
                            .toList());
                    return map;
                })
                .toList();
        Map<String, Object> manifestPayload = new LinkedHashMap<>();
        manifestPayload.put("releaseId", releaseId);
        manifestPayload.put("schemaVersion", ShowroomReleaseConstants.SCHEMA_VERSION);
        manifestPayload.put("publishedAt", DateTimeFormatter.ISO_INSTANT.format(publishedAt));
        manifestPayload.put("rootDocumentId", ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX);
        manifestPayload.put("documents", manifestDocuments);
        manifestPayload.put("assets", manifestAssets);
        String manifestHash = hashJsonWithoutField(manifestPayload, "manifestHash");
        manifestPayload.put("manifestHash", manifestHash);
        String manifestJson = JsonUtils.toJsonString(manifestPayload);
        long manifestBytes = ShowroomReleaseHttpSupport.utf8Bytes(manifestJson);
        long installBytes = manifestBytes
                + documents.stream().mapToLong(ShowroomMaterializedRelease.MaterializedDocument::bytes).sum()
                + assets.stream().mapToLong(ShowroomMaterializedRelease.MaterializedAsset::bytes).sum();
        return new ShowroomMaterializedRelease(releaseId, publishedAt, manifestHash, manifestJson,
                ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX, documents.size(), assets.size(), installBytes,
                snapshot, List.copyOf(documents), List.copyOf(assets), List.copyOf(assetRefs),
                legacyProjectionJson, legacyProjectionHash);
    }

    private ShowroomReleaseSourceSnapshot.ResolvedProduct resolveProduct(Long productId, List<Long> productRevisionIds,
                                                                         List<Long> previewAssetVersionIds,
                                                                         List<Long> narrationVersionIds) {
        ShowroomProductSnapshot snapshot = contentService.getProduct(productId);
        ShowroomProductRevision revision = contentService.requireCurrentProductRevision(productId);
        ShowroomReleaseSourceSnapshot.ResolvedNarrationPair narrations = resolveNarrationPair(
                ShowroomNarrationTargetType.PRODUCT,
                productId,
                revision.revisionId(),
                "product-" + productId + "-audio-zh",
                "product-" + productId + "-audio-en",
                "product");
        productRevisionIds.add(revision.revisionId());
        narrationVersionIds.add(narrations.zh().id());
        narrationVersionIds.add(narrations.en().id());
        return new ShowroomReleaseSourceSnapshot.ResolvedProduct(snapshot, revision,
                resolveProductImage(productId, revision, previewAssetVersionIds),
                narrations, resolveProductAttachments(productId, revision));
    }

    private ShowroomReleaseSourceSnapshot.ResolvedAward resolveAward(Long awardId, List<Long> awardRevisionIds,
                                                                     List<Long> narrationVersionIds) {
        ShowroomAwardSnapshot snapshot = contentService.getAward(awardId);
        ShowroomAwardRevision revision = contentService.requireCurrentAwardRevision(awardId);
        ShowroomReleaseSourceSnapshot.ResolvedNarrationPair narrations = resolveNarrationPair(
                ShowroomNarrationTargetType.AWARD,
                awardId,
                revision.revisionId(),
                "award-" + awardId + "-audio-zh",
                "award-" + awardId + "-audio-en",
                "award");
        awardRevisionIds.add(revision.revisionId());
        narrationVersionIds.add(narrations.zh().id());
        narrationVersionIds.add(narrations.en().id());
        return new ShowroomReleaseSourceSnapshot.ResolvedAward(snapshot, revision,
                resolveAwardImage(awardId, revision), narrations);
    }

    private List<ShowroomReleaseSourceSnapshot.ResolvedProductAttachment> resolveProductAttachments(
            Long productId, ShowroomProductRevision revision) {
        if (revision.attachments().isEmpty()) {
            return List.of();
        }
        List<ShowroomReleaseSourceSnapshot.ResolvedProductAttachment> attachments = new ArrayList<>();
        for (ShowroomProductAttachment attachment : revision.attachments()) {
            attachments.add(new ShowroomReleaseSourceSnapshot.ResolvedProductAttachment(attachment,
                    fileReader.requireAdminFileUrlById(attachment.fileId())));
        }
        return attachments;
    }

    private ResolvedBinarySource resolveProductImage(Long productId, ShowroomProductRevision revision,
                                                     List<Long> previewAssetVersionIds) {
        String coverImage = requireText(revision.fields().get("cover_image"),
                "SHOWROOM_REQUIRED_FIELD_MISSING: product cover_image is required for PRODUCT:" + productId);
        return fileReader.readByAdminUrl("product-" + productId + "-preview",
                ShowroomReleaseConstants.ASSET_TYPE_IMAGE, coverImage);
    }

    private ResolvedBinarySource resolveAwardImage(Long awardId, ShowroomAwardRevision revision) {
        String coverImage = requireText(revision.fields().get("cover_image"),
                "SHOWROOM_REQUIRED_FIELD_MISSING: award cover_image is required for AWARD:" + awardId);
        return fileReader.readByAdminUrl("award-" + awardId + "-preview",
                ShowroomReleaseConstants.ASSET_TYPE_IMAGE, coverImage);
    }

    private ShowroomReleaseSourceSnapshot.ResolvedNarrationPair resolveNarrationPair(
            ShowroomNarrationTargetType targetType, Long targetId, Long sourceRevisionId,
            String zhAssetId, String enAssetId, String label) {
        ShowroomNarrationVersion zh = requireNarration(targetType, targetId, sourceRevisionId,
                ShowroomNarrationLanguage.ZH, label);
        ShowroomNarrationVersion en = requireNarration(targetType, targetId, sourceRevisionId,
                ShowroomNarrationLanguage.EN, label);
        return new ShowroomReleaseSourceSnapshot.ResolvedNarrationPair(
                zh, en,
                fileReader.readFileById(zhAssetId, ShowroomReleaseConstants.ASSET_TYPE_AUDIO, zh.audioFileId(), null,
                        zh.id()),
                fileReader.readFileById(enAssetId, ShowroomReleaseConstants.ASSET_TYPE_AUDIO, en.audioFileId(), null,
                        en.id()));
    }

    private ShowroomNarrationVersion requireNarration(ShowroomNarrationTargetType targetType, Long targetId,
                                                      Long sourceRevisionId, ShowroomNarrationLanguage language,
                                                      String label) {
        ShowroomNarrationVersion version = narrationService.live(new ShowroomNarrationKey(targetType, targetId,
                        ShowroomNarrationAudienceType.PUBLIC, language))
                .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live "
                        + label + " " + language.name() + " narration not found"));
        if (!sourceRevisionId.equals(version.sourceRevisionId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live " + label + " "
                    + language.name() + " narration source revision mismatch");
        }
        if (version.audioFileId() == null) {
            throw new IllegalStateException("SHOWROOM_AUDIO_GENERATION_FAILED: live " + label + " "
                    + language.name() + " narration audio is required");
        }
        return version;
    }

    private ShowroomPreviewAssetVersionDO requirePreviewVersion(String targetType, Long targetId, Long sourceRevisionId) {
        ShowroomPreviewAssetVersionDO version = previewAssetVersionMapper.selectLatestPublishedByKey(targetType, targetId);
        if (version == null || version.getImageFileId() == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live preview asset is required for " + targetType + ":" + targetId);
        }
        if (sourceRevisionId != null && !sourceRevisionId.equals(version.getSourceRevisionId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live preview asset source revision mismatch for "
                    + targetType + ":" + targetId);
        }
        return version;
    }

    private Map<String, Object> toHallSnapshotHashEntry(ShowroomHall hall) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("hallId", hall.hallId());
        map.put("hallCode", hall.hallCode());
        map.put("name", hall.name());
        map.put("nameEn", hall.nameEn());
        map.put("description", nullToEmpty(hall.description()));
        map.put("descriptionEn", nullToEmpty(hall.descriptionEn()));
        map.put("items", hall.itemMappings().stream().map(mapping -> Map.of(
                "itemType", mapping.itemType(),
                "itemId", mapping.itemId(),
                "displayOrder", mapping.displayOrder(),
                "layoutX", mapping.layoutX(),
                "layoutY", mapping.layoutY(),
                "layoutWidth", mapping.layoutWidth(),
                "layoutHeight", mapping.layoutHeight())).toList());
        return map;
    }

    private static void putLayoutFields(Map<String, Object> target, ShowroomHallProductMapping mapping) {
        target.put("layoutX", requireLayoutValue(mapping.layoutX(), "layoutX"));
        target.put("layoutY", requireLayoutValue(mapping.layoutY(), "layoutY"));
        target.put("layoutWidth", requireLayoutValue(mapping.layoutWidth(), "layoutWidth"));
        target.put("layoutHeight", requireLayoutValue(mapping.layoutHeight(), "layoutHeight"));
    }

    private static void putLayoutFields(Map<String, Object> target, ShowroomHallItemMapping mapping) {
        target.put("layoutX", requireLayoutValue(mapping.layoutX(), "layoutX"));
        target.put("layoutY", requireLayoutValue(mapping.layoutY(), "layoutY"));
        target.put("layoutWidth", requireLayoutValue(mapping.layoutWidth(), "layoutWidth"));
        target.put("layoutHeight", requireLayoutValue(mapping.layoutHeight(), "layoutHeight"));
    }

    private static void copyLayoutFields(Map<String, Object> target, Map<String, Object> source) {
        target.put("layoutX", source.get("layoutX"));
        target.put("layoutY", source.get("layoutY"));
        target.put("layoutWidth", source.get("layoutWidth"));
        target.put("layoutHeight", source.get("layoutHeight"));
    }

    private static BigDecimal requireLayoutValue(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas " + fieldName + " is required");
        }
        return value;
    }

    private List<Map<String, Object>> productAttachments(
            Map<String, ShowroomMaterializedRelease.MaterializedAsset> assetByKey,
            List<ShowroomMaterializedRelease.MaterializedAsset> assets,
            List<ShowroomMaterializedRelease.MaterializedAssetRef> refs,
            String documentId,
            ShowroomReleaseSourceSnapshot.ResolvedProduct product,
            Instant materializedAt) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ShowroomReleaseSourceSnapshot.ResolvedProductAttachment resolved : product.attachments()) {
            ShowroomProductAttachment attachment = resolved.attachment();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("assetType", attachment.assetType());
            item.put("fileId", String.valueOf(attachment.fileId()));
            item.put("originalName", attachment.originalName());
            item.put("mimeType", attachment.mimeType());
            item.put("bytes", attachment.fileSize());
            item.put("displayOrder", attachment.displayOrder());
            item.put("url", resolved.url());
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> descriptor(Map<String, ShowroomMaterializedRelease.MaterializedAsset> assetByKey,
                                           List<ShowroomMaterializedRelease.MaterializedAsset> assets,
                                           List<ShowroomMaterializedRelease.MaterializedAssetRef> refs,
                                           String documentId,
                                           ResolvedBinarySource source,
                                           String usageCode,
                                           Instant materializedAt) {
        String contentHash = ShowroomReleaseHashSupport.sha256Hex(source.bytes());
        String key = source.assetId() + ":" + contentHash;
        ShowroomMaterializedRelease.MaterializedAsset asset = assetByKey.computeIfAbsent(key, ignored -> {
            ShowroomMaterializedRelease.MaterializedAsset created = new ShowroomMaterializedRelease.MaterializedAsset(
                    source.assetId(), source.assetType(), contentHash, source.mimeType(), source.bytes().length,
                    "release/" + source.assetId() + "/" + contentHash, materializedAt, source.bytes());
            assets.add(created);
            return created;
        });
        refs.add(new ShowroomMaterializedRelease.MaterializedAssetRef(documentId, asset.assetId(), asset.contentHash(),
                usageCode));
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("assetId", asset.assetId());
        descriptor.put("contentHash", asset.contentHash());
        descriptor.put("mimeType", asset.mimeType());
        descriptor.put("bytes", asset.bytes());
        return descriptor;
    }

    private String buildLegacyProjectionJson(ShowroomReleaseScope scope,
                                             ShowroomReleaseSourceSnapshot snapshot,
                                             List<ShowroomMaterializedRelease.MaterializedDocument> documents,
                                             List<ShowroomMaterializedRelease.MaterializedAsset> assets) {
        Map<String, Object> detailById = new LinkedHashMap<>();
        for (ShowroomMaterializedRelease.MaterializedDocument document : documents) {
            if (!ShowroomReleaseConstants.DOCUMENT_KIND_PRODUCT_DETAIL.equals(document.kind())
                    && !ShowroomReleaseConstants.DOCUMENT_KIND_AWARD_DETAIL.equals(document.kind())) {
                continue;
            }
            detailById.put(document.documentId(), JsonUtils.parseObject(document.payloadJson(), Map.class));
        }
        Map<String, Object> websiteIndex = JsonUtils.parseObject(documents.stream()
                .filter(document -> ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX.equals(document.documentId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("SHOWROOM_RELEASE_BROKEN: website-index document missing"))
                .payloadJson(), Map.class);
        Map<String, Object> company = castMap(websiteIndex.get("company"));
        List<Map<String, Object>> showrooms = castListMap(websiteIndex.get("showrooms"));
        Map<String, Object> legacyCompany = new LinkedHashMap<>();
        legacyCompany.put("companyId", company.get("companyId"));
        legacyCompany.put("name", company.get("name"));
        legacyCompany.put("nameEn", company.get("nameEn"));
        legacyCompany.put("homeImageUrl", assetUrl(scope, castMap(company.get("homeImage"))));
        legacyCompany.put("subtitleZh", company.get("subtitleZh"));
        legacyCompany.put("subtitleEn", company.get("subtitleEn"));
        legacyCompany.put("audioZhUrl", assetUrl(scope, castMap(company.get("audioZh"))));
        legacyCompany.put("audioEnUrl", assetUrl(scope, castMap(company.get("audioEn"))));
        List<Map<String, Object>> companyBilingual = castListMap(company.get("bilingualPublicFields"));
        legacyCompany.put("publicFields", companyBilingual.stream().map(field -> Map.of(
                "label", field.get("labelZh"),
                "value", field.get("valueZh"))).toList());
        legacyCompany.put("bilingualPublicFields", companyBilingual);

        List<Map<String, Object>> legacyShowrooms = new ArrayList<>();
        for (Map<String, Object> showroom : showrooms) {
            Map<String, Object> legacyShowroom = new LinkedHashMap<>();
            legacyShowroom.put("hallId", showroom.get("hallId"));
            legacyShowroom.put("hallCode", showroom.get("hallCode"));
            legacyShowroom.put("name", showroom.get("name"));
            legacyShowroom.put("nameEn", showroom.get("nameEn"));
            legacyShowroom.put("description", showroom.get("description"));
            legacyShowroom.put("descriptionEn", showroom.get("descriptionEn"));
            legacyShowroom.put("previewImageUrl", assetUrl(scope, castMap(showroom.get("previewImage"))));
            legacyShowroom.put("audioZhUrl", assetUrl(scope, castMap(showroom.get("audioZh"))));
            legacyShowroom.put("audioEnUrl", assetUrl(scope, castMap(showroom.get("audioEn"))));
            List<Map<String, Object>> legacyItems = new ArrayList<>();
            for (Map<String, Object> item : castListMap(showroom.get("items"))) {
                String detailDocumentId = String.valueOf(item.get("detailDocumentId"));
                Map<String, Object> detail = castMap(detailById.get(detailDocumentId));
                if (detail == null) {
                    throw new IllegalStateException("SHOWROOM_RELEASE_BROKEN: missing item detail " + detailDocumentId);
                }
                List<Map<String, Object>> bilingualFields = castListMap(detail.get("bilingualPublicFields"));
                Map<String, Object> legacyItem = new LinkedHashMap<>();
                legacyItem.put("itemType", item.get("itemType"));
                legacyItem.put("itemId", item.get("itemId"));
                legacyItem.put("itemCode", item.get("itemCode"));
                legacyItem.put("productId", item.get("productId"));
                legacyItem.put("productCode", item.get("productCode"));
                legacyItem.put("awardId", item.get("awardId"));
                legacyItem.put("awardCode", item.get("awardCode"));
                legacyItem.put("nameCn", item.get("nameCn"));
                legacyItem.put("nameEn", item.get("nameEn"));
                legacyItem.put("incompleteFlag", item.get("incompleteFlag"));
                legacyItem.put("previewImageUrl", assetUrl(scope, castMap(item.get("previewImage"))));
                legacyItem.put("detailDocumentId", detailDocumentId);
                copyLayoutFields(legacyItem, item);
                legacyItem.put("subtitleZh", detail.get("subtitleZh"));
                legacyItem.put("subtitleEn", detail.get("subtitleEn"));
                legacyItem.put("audioZhUrl", assetUrl(scope, castMap(detail.get("audioZh"))));
                legacyItem.put("audioEnUrl", assetUrl(scope, castMap(detail.get("audioEn"))));
                legacyItem.put("attachments", legacyProductAttachments(scope,
                        castListMap(detail.get("attachments"))));
                legacyItem.put("publicFields", bilingualFields.stream().map(field -> Map.of(
                        "label", field.get("labelZh"),
                        "value", field.get("valueZh"))).toList());
                legacyItem.put("bilingualPublicFields", bilingualFields);
                legacyItems.add(legacyItem);
            }
            List<Map<String, Object>> legacyProducts = new ArrayList<>();
            for (Map<String, Object> product : castListMap(showroom.get("products"))) {
                String detailDocumentId = String.valueOf(product.get("detailDocumentId"));
                Map<String, Object> detail = castMap(detailById.get(detailDocumentId));
                if (detail == null) {
                    throw new IllegalStateException("SHOWROOM_RELEASE_BROKEN: missing product detail " + detailDocumentId);
                }
                List<Map<String, Object>> bilingualFields = castListMap(detail.get("bilingualPublicFields"));
                Map<String, Object> legacyProduct = new LinkedHashMap<>();
                legacyProduct.put("productId", product.get("productId"));
                legacyProduct.put("productCode", product.get("productCode"));
                legacyProduct.put("nameCn", product.get("nameCn"));
                legacyProduct.put("nameEn", product.get("nameEn"));
                legacyProduct.put("incompleteFlag", product.get("incompleteFlag"));
                legacyProduct.put("previewImageUrl", assetUrl(scope, castMap(product.get("previewImage"))));
                copyLayoutFields(legacyProduct, product);
                legacyProduct.put("subtitleZh", detail.get("subtitleZh"));
                legacyProduct.put("subtitleEn", detail.get("subtitleEn"));
                legacyProduct.put("audioZhUrl", assetUrl(scope, castMap(detail.get("audioZh"))));
                legacyProduct.put("audioEnUrl", assetUrl(scope, castMap(detail.get("audioEn"))));
                legacyProduct.put("attachments", legacyProductAttachments(scope,
                        castListMap(detail.get("attachments"))));
                legacyProduct.put("publicFields", bilingualFields.stream().map(field -> Map.of(
                        "label", field.get("labelZh"),
                        "value", field.get("valueZh"))).toList());
                legacyProduct.put("bilingualPublicFields", bilingualFields);
                legacyProducts.add(legacyProduct);
            }
            legacyShowroom.put("items", legacyItems);
            legacyShowroom.put("products", legacyProducts);
            legacyShowrooms.add(legacyShowroom);
        }
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("code", 0);
        wrapper.put("msg", "");
        wrapper.put("data", Map.of("company", legacyCompany, "showrooms", legacyShowrooms));
        return JsonUtils.toJsonString(wrapper);
    }

    private List<Map<String, Object>> legacyProductAttachments(ShowroomReleaseScope scope,
                                                               List<Map<String, Object>> attachments) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> attachment : attachments) {
            Map<String, Object> item = new LinkedHashMap<>(attachment);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> companyBilingualFields(Map<String, String> fields) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String fieldCode : ShowroomReleaseConstants.COMPANY_WEBSITE_FIELD_ORDER) {
            String valueZh = nullToEmpty(fields.get(fieldCode));
            String valueEn = nullToEmpty(fields.get(fieldCode + "_en"));
            if (hasText(valueZh) || hasText(valueEn)) {
                result.add(Map.of(
                        "fieldCode", fieldCode,
                        "labelZh", ShowroomFieldDisplaySupport.fieldLabel(ShowroomReleaseConstants.TARGET_COMPANY, fieldCode),
                        "labelEn", ShowroomFieldDisplaySupport.fieldLabelEn(ShowroomReleaseConstants.TARGET_COMPANY, fieldCode),
                        "valueZh", valueZh,
                        "valueEn", valueEn
                ));
            }
        }
        return result;
    }

    private List<Map<String, Object>> productBilingualFields(ShowroomProductRevision revision) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(Map.of(
                "fieldCode", "name",
                "labelZh", "产品名称",
                "labelEn", "Product Name",
                "valueZh", nullToEmpty(revision.nameCn()),
                "valueEn", nullToEmpty(revision.nameEn())
        ));
        for (String fieldCode : ShowroomReleaseConstants.PRODUCT_FIELD_ORDER) {
            if (ShowroomFieldCatalog.productField(fieldCode).tier() != ShowroomFieldTierEnum.BASIC) {
                continue;
            }
            String valueZhRaw = revision.fields().get(fieldCode);
            String valueEnRaw = productEnglishRawValue(revision, fieldCode, valueZhRaw);
            String valueZh = hasText(valueZhRaw)
                    ? ShowroomFieldDisplaySupport.formatStoredFieldValue(ShowroomReleaseConstants.TARGET_PRODUCT,
                    fieldCode, valueZhRaw, contentService)
                    : "";
            String valueEn = hasText(valueEnRaw)
                    ? ShowroomFieldDisplaySupport.formatStoredFieldValue(ShowroomReleaseConstants.TARGET_PRODUCT,
                    fieldCode, valueEnRaw, contentService, true)
                    : "";
            if (hasText(valueZh) || hasText(valueEn)) {
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("fieldCode", fieldCode);
                field.put("labelZh", ShowroomFieldDisplaySupport.fieldLabel(ShowroomReleaseConstants.TARGET_PRODUCT,
                        fieldCode));
                field.put("labelEn", ShowroomFieldDisplaySupport.fieldLabelEn(ShowroomReleaseConstants.TARGET_PRODUCT,
                        fieldCode));
                field.put("valueZh", valueZh);
                field.put("valueEn", valueEn);
                result.add(field);
            }
        }
        return result;
    }

    private List<Map<String, Object>> awardBilingualFields(ShowroomAwardRevision revision) {
        List<Map<String, Object>> result = new ArrayList<>();
        addAwardBilingualField(result, "name", "奖项名称", "Award Name",
                revision.nameCn(), revision.nameEn());
        addAwardBilingualField(result, "award_date_text", "日期/期限", "Date / Period",
                revision.fields().get("award_date_text"), revision.fields().get("award_date_text"));
        addAwardBilingualField(result, "issuer", "颁发单位", "Issuer",
                revision.fields().get("issuer"), revision.fields().get("issuer"));
        addAwardBilingualField(result, "description", "中文讲解", "English Description",
                revision.fields().get("description_zh"), revision.fields().get("description_en"));
        return result;
    }

    private static void addAwardBilingualField(List<Map<String, Object>> result, String fieldCode,
                                               String labelZh, String labelEn, String valueZh, String valueEn) {
        if (hasText(valueZh) || hasText(valueEn)) {
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("fieldCode", fieldCode);
            field.put("labelZh", labelZh);
            field.put("labelEn", labelEn);
            field.put("valueZh", nullToEmpty(valueZh));
            field.put("valueEn", nullToEmpty(valueEn));
            result.add(field);
        }
    }

    private String hashJsonWithoutField(Map<String, Object> payload, String hashFieldName) {
        Map<String, Object> copy = new LinkedHashMap<>(payload);
        copy.remove(hashFieldName);
        return ShowroomReleaseHashSupport.sha256Hex(JsonUtils.toJsonString(copy));
    }

    private String hashAwardDetailDocument(Map<String, Object> payload) {
        Map<String, Object> integrityPayload = new LinkedHashMap<>();
        integrityPayload.put("documentId", payload.get("documentId"));
        integrityPayload.put("kind", payload.get("kind"));
        integrityPayload.put("releaseId", payload.get("releaseId"));
        integrityPayload.put("awardId", payload.get("awardId"));
        integrityPayload.put("nameCn", payload.get("nameCn"));
        integrityPayload.put("nameEn", payload.get("nameEn"));
        integrityPayload.put("descriptionZh", payload.get("descriptionZh"));
        integrityPayload.put("descriptionEn", payload.get("descriptionEn"));
        integrityPayload.put("issuer", payload.get("issuer"));
        integrityPayload.put("awardDateText", payload.get("awardDateText"));
        integrityPayload.put("audioZh", payload.get("audioZh"));
        integrityPayload.put("audioEn", payload.get("audioEn"));
        return ShowroomReleaseHashSupport.sha256Hex(JsonUtils.toJsonString(integrityPayload));
    }

    private String buildReleaseId(ShowroomReleaseScope scope, Instant publishedAt,
                                  ShowroomReleaseSourceSnapshot snapshot) {
        String base = ShowroomReleaseConstants.RELEASE_ID_TIME_FORMATTER.withZone(ZoneOffset.UTC).format(publishedAt);
        String scopeHash = ShowroomReleaseHashSupport.sha256Hex(
                scope.tenantId() + "|" + scope.siteKey() + "|" + scope.stage()).substring(0, 12);
        return base + "-" + scopeHash + "-" + snapshot.hallSnapshotHash().substring(0, 12);
    }

    private static String productDetailDocumentId(Long productId) {
        return "product-detail-" + productId;
    }

    private static String awardDetailDocumentId(Long awardId) {
        return "award-detail-" + awardId;
    }

    private static String assetUrl(ShowroomReleaseScope scope, Map<String, Object> descriptor) {
        return "/showroom/sites/" + scope.siteKey() + "/stages/" + scope.stage() + "/assets/"
                + descriptor.get("assetId") + "/" + descriptor.get("contentHash");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value == null ? null : (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castListMap(Object value) {
        return value == null ? List.of() : (List<Map<String, Object>>) value;
    }

    private static String productEnglishRawValue(ShowroomProductRevision revision, String fieldCode,
                                                 String valueZhRaw) {
        if (!hasText(valueZhRaw)) {
            return "";
        }
        if (ShowroomReleaseConstants.PRODUCT_TRANSLATABLE_FIELD_KEYS.contains(fieldCode)) {
            return nullToEmpty(revision.fields().get(fieldCode + "_en"));
        }
        return valueZhRaw;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static List<ShowroomHallItemMapping> requireItemCanvasLayout(List<ShowroomHallItemMapping> orderedMappings) {
        List<ShowroomHallProductMapping> validated = ShowroomHallCanvasLayoutPolicy.requireCanvasLayout(
                toLayoutOnlyProductMappings(orderedMappings));
        return applyProductLayoutToItems(orderedMappings, validated);
    }

    private static List<ShowroomHallProductMapping> toLayoutOnlyProductMappings(
            List<ShowroomHallItemMapping> orderedMappings) {
        List<ShowroomHallProductMapping> converted = new ArrayList<>();
        for (int index = 0; index < orderedMappings.size(); index++) {
            ShowroomHallItemMapping mapping = orderedMappings.get(index);
            converted.add(new ShowroomHallProductMapping((long) index + 1, mapping.displayOrder(),
                    mapping.layoutX(), mapping.layoutY(), mapping.layoutWidth(), mapping.layoutHeight()));
        }
        return converted;
    }

    private static List<ShowroomHallItemMapping> applyProductLayoutToItems(
            List<ShowroomHallItemMapping> orderedMappings,
            List<ShowroomHallProductMapping> layoutMappings) {
        List<ShowroomHallItemMapping> result = new ArrayList<>();
        for (int index = 0; index < orderedMappings.size(); index++) {
            ShowroomHallItemMapping item = orderedMappings.get(index);
            ShowroomHallProductMapping layout = layoutMappings.get(index);
            result.add(new ShowroomHallItemMapping(item.itemType(), item.itemId(), item.displayOrder(),
                    layout.layoutX(), layout.layoutY(), layout.layoutWidth(), layout.layoutHeight()));
        }
        return List.copyOf(result);
    }

    private String resolveAwardCode(Long awardId) {
        try {
            return nullToEmpty(contentService.getAward(awardId).awardCode());
        } catch (RuntimeException exception) {
            return "award-" + awardId;
        }
    }

    private ShowroomHall retainHallItems(ShowroomHall hall, List<ShowroomHallItemMapping> mappings) {
        List<ShowroomHallProductMapping> productMappings = mappings.stream()
                .filter(mapping -> ShowroomHallItemMapping.TYPE_PRODUCT.equals(mapping.itemType()))
                .map(ShowroomHallItemMapping::asProductMapping)
                .toList();
        return new ShowroomHall(hall.hallId(), hall.hallCode(), hall.name(), hall.nameEn(), hall.description(),
                hall.descriptionEn(), hall.canvasBackgroundImageUrl(), productMappings, mappings);
    }

    private String resolveProductCode(Long productId) {
        try {
            return nullToEmpty(contentService.getProduct(productId).productCode());
        } catch (RuntimeException exception) {
            return "product-" + productId;
        }
    }

    private static String summarizeFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        return hasText(message) ? message.trim() : exception.getClass().getSimpleName();
    }

    private static String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private static void requireHallReleaseText(ShowroomHall hall) {
        requireHallText(hall.description(), hall, "hall description");
        requireHallText(hall.descriptionEn(), hall, "hall description_en");
    }

    private static void requireHallText(String value, ShowroomHall hall, String fieldLabel) {
        if (hasText(value)) {
            return;
        }
        throw new IllegalStateException("SHOWROOM_RELEASE_HALL_BLOCKED: hallId=" + hall.hallId()
                + " hallCode=" + hall.hallCode()
                + " reason=SHOWROOM_REQUIRED_FIELD_MISSING: " + fieldLabel + " is required");
    }
}
