package cn.iocoder.yudao.module.showroom.content.service;

import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardSnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallCanvasLayoutPolicy;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallItemMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallItemOption;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductOption;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductAttachmentPolicy;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomPublishContract;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomVersionAudit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ShowroomContentService implements ShowroomContentOperations {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String ACTION_PUBLISH = "PUBLISH";

    private long companyIdSequence = 1L;
    private long companyRevisionIdSequence = 100L;
    private long productIdSequence = 1L;
    private long productRevisionIdSequence = 1000L;
    private long awardIdSequence = 1L;
    private long awardRevisionIdSequence = 2000L;
    private long hallIdSequence = 1L;

    private final Map<Long, CompanyAggregate> companies = new LinkedHashMap<>();
    private final Map<Long, Long> companyRevisionOwners = new LinkedHashMap<>();
    private final Map<Long, ProductAggregate> products = new LinkedHashMap<>();
    private final Map<Long, Long> productRevisionOwners = new LinkedHashMap<>();
    private final Map<Long, AwardAggregate> awards = new LinkedHashMap<>();
    private final Map<Long, Long> awardRevisionOwners = new LinkedHashMap<>();
    private final Map<Long, ShowroomHall> halls = new LinkedHashMap<>();
    private final Map<String, List<ShowroomVersionAudit>> versionAudits = new LinkedHashMap<>();

    public ShowroomCompanyRevision saveCompanyDraft(ShowroomCompanyDraft draft) {
        requireNonNull(draft, "SHOWROOM_REQUIRED_FIELD_MISSING: company draft is required");
        Map<String, String> fields = copyFields(draft.fields());
        CompanyAggregate company = draft.companyId() == null
                ? createCompany(draft.companyType(), draft.displayName(), draft.displayNameEn())
                : requireCompany(draft.companyId());
        company.companyType = requireText(draft.companyType(), "SHOWROOM_REQUIRED_FIELD_MISSING: company type is required");
        company.displayName = requireText(draft.displayName(), "SHOWROOM_REQUIRED_FIELD_MISSING: company display name is required");
        company.displayNameEn = requireText(draft.displayNameEn(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: company display name_en is required");
        ShowroomCompanyRevision revision = new ShowroomCompanyRevision(company.companyId,
                companyRevisionIdSequence++, ++company.revisionNo, STATUS_DRAFT, fields);
        company.revisions.put(revision.revisionId(), revision);
        companyRevisionOwners.put(revision.revisionId(), company.companyId);
        return revision;
    }

    public ShowroomCompanySnapshot getCompany(Long companyId) {
        CompanyAggregate company = requireCompany(companyId);
        return new ShowroomCompanySnapshot(company.companyId, company.companyType, company.displayName, company.displayNameEn,
                Optional.ofNullable(company.currentRevisionId), company.currentRevisionId != null);
    }

    public List<ShowroomCompanySnapshot> listCompanies() {
        return companies.values().stream()
                .map(company -> new ShowroomCompanySnapshot(company.companyId, company.companyType,
                        company.displayName, company.displayNameEn, Optional.ofNullable(company.currentRevisionId),
                        company.currentRevisionId != null))
                .toList();
    }

    public ShowroomCompanyRevision getCompanyRevision(Long revisionId) {
        CompanyAggregate company = requireCompanyByRevision(revisionId);
        ShowroomCompanyRevision revision = company.revisions.get(revisionId);
        if (revision == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company revision not found");
        }
        return revision;
    }

    public ShowroomCompanyRevision requireCurrentCompanyRevision() {
        return companies.values().stream()
                .filter(company -> company.currentRevisionId != null)
                .findFirst()
                .map(company -> company.revisions.get(company.currentRevisionId))
                .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live company revision not found"));
    }

    public Optional<ShowroomCompanyRevision> findCurrentOrLatestCompanyRevision() {
        Optional<ShowroomCompanyRevision> current = companies.values().stream()
                .filter(company -> company.currentRevisionId != null)
                .findFirst()
                .map(company -> company.revisions.get(company.currentRevisionId));
        if (current.isPresent()) {
            return current;
        }
        return companies.values().stream()
                .flatMap(company -> company.revisions.values().stream())
                .max(Comparator.comparingLong(ShowroomCompanyRevision::revisionId));
    }

    public ShowroomCompanyRevision publishCompanyRevision(Long revisionId, Long operatorId) {
        requireNonNull(operatorId, "SHOWROOM_ROLE_BINDING_MISSING: publish operator is required");
        CompanyAggregate company = requireCompanyByRevision(revisionId);
        ShowroomCompanyRevision revision = company.revisions.get(revisionId);
        ShowroomCompanyRevision published = new ShowroomCompanyRevision(revision.companyId(), revision.revisionId(),
                revision.revisionNo(), STATUS_PUBLISHED, revision.fields());
        company.revisions.put(revisionId, published);
        company.currentRevisionId = revisionId;
        appendAudits("COMPANY", company.companyId, revisionId, revision.fields(), operatorId, ACTION_PUBLISH);
        return published;
    }

    public ShowroomProductRevision saveProductDraft(ShowroomProductDraft draft) {
        requireNonNull(draft, "SHOWROOM_REQUIRED_FIELD_MISSING: product draft is required");
        Map<String, String> fields = copyFields(draft.fields());
        var attachments = ShowroomProductAttachmentPolicy.normalizedCopy(draft.attachments());
        ProductAggregate product = draft.productId() == null
                ? createProduct(draft.productMasterId(), draft.productCode())
                : requireProduct(draft.productId());
        if (draft.productMasterId() != null) {
            product.productMasterId = draft.productMasterId();
        }
        product.productCode = requireText(draft.productCode(), "SHOWROOM_REQUIRED_FIELD_MISSING: product code is required");
        String ownerCompanyId = fields.get("owner_company_id");
        String productOwnerType = fields.get("product_owner_type");
        String lifecycleStage = fields.get("lifecycle_stage");
        boolean incomplete = isProductIncomplete(
                draft.nameCn(), draft.nameEn(), ownerCompanyId, productOwnerType, lifecycleStage);
        ShowroomProductRevision revision = new ShowroomProductRevision(product.productId,
                productRevisionIdSequence++, ++product.revisionNo, STATUS_DRAFT, draft.nameCn(), draft.nameEn(),
                incomplete, fields, attachments);
        product.revisions.put(revision.revisionId(), revision);
        productRevisionOwners.put(revision.revisionId(), product.productId);
        product.incomplete = incomplete;
        return revision;
    }

    public ShowroomProductSnapshot getProduct(Long productId) {
        ProductAggregate product = requireProduct(productId);
        return new ShowroomProductSnapshot(product.productId, product.productMasterId, product.productCode,
                Optional.ofNullable(product.currentRevisionId), product.incomplete,
                product.currentRevisionId != null);
    }

    public ShowroomProductRevision getProductRevision(Long revisionId) {
        ProductAggregate product = requireProductByRevision(revisionId);
        ShowroomProductRevision revision = product.revisions.get(revisionId);
        if (revision == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found");
        }
        return revision;
    }

    @Override
    public ShowroomProductRevision getLatestProductRevision(Long productId) {
        ProductAggregate product = requireProduct(productId);
        return product.revisions.values().stream()
                .max(Comparator.comparingInt(ShowroomProductRevision::revisionNo)
                        .thenComparingLong(ShowroomProductRevision::revisionId))
                .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found"));
    }

    public ShowroomProductRevision requireCurrentProductRevision(Long productId) {
        ProductAggregate product = requireProduct(productId);
        if (product.currentRevisionId == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live product revision not found");
        }
        return product.revisions.get(product.currentRevisionId);
    }

    @Override
    public ShowroomProductRevision getCurrentOrLatestProductRevision(Long productId) {
        ProductAggregate product = requireProduct(productId);
        if (product.currentRevisionId != null) {
            return product.revisions.get(product.currentRevisionId);
        }
        return product.revisions.values().stream()
                .max(Comparator.comparingInt(ShowroomProductRevision::revisionNo)
                        .thenComparingLong(ShowroomProductRevision::revisionId))
                .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found"));
    }

    public List<ShowroomProductSnapshot> listProducts() {
        return products.values().stream()
                .map(product -> new ShowroomProductSnapshot(product.productId, product.productMasterId, product.productCode,
                        null, Optional.ofNullable(product.currentRevisionId), product.incomplete,
                        product.currentRevisionId != null))
                .toList();
    }

    @Override
    public List<ShowroomProductSnapshot> listProducts(String keyword, Integer pageNo, Integer pageSize) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return page(listProducts().stream()
                .filter(snapshot -> matchesProduct(snapshot, normalizedKeyword))
                .toList(), pageNo, pageSize);
    }

    @Override
    public List<ShowroomHallProductOption> listHallProductOptions() {
        Map<Long, List<Long>> hallIdsByProductId = new LinkedHashMap<>();
        for (ShowroomHall hall : halls.values()) {
            for (ShowroomHallProductMapping mapping : hall.productMappings()) {
                hallIdsByProductId.computeIfAbsent(mapping.productId(), ignored -> new ArrayList<>())
                        .add(hall.hallId());
            }
        }
        return listProducts().stream()
                .map(snapshot -> {
                    ShowroomProductRevision revision = getCurrentOrLatestProductRevision(snapshot.productId());
                    return new ShowroomHallProductOption(snapshot.productId(), snapshot.productMasterId(),
                            requireText(snapshot.productCode(),
                                    "SHOWROOM_REQUIRED_FIELD_MISSING: hall product option code is required"),
                            revision.nameCn() == null ? "" : revision.nameCn(),
                            revision.revisionNo(), snapshot.incomplete(),
                            revision.fields().get("cover_image") == null ? "" : revision.fields().get("cover_image"),
                            hallIdsByProductId.getOrDefault(snapshot.productId(), List.of()));
                })
                .toList();
    }

    @Override
    public List<ShowroomHallItemOption> listHallItemOptions() {
        List<ShowroomHallItemOption> options = new ArrayList<>();
        for (ShowroomHallProductOption product : listHallProductOptions()) {
            options.add(new ShowroomHallItemOption(ShowroomHallItemMapping.TYPE_PRODUCT, product.productId(),
                    product.productCode(), product.nameCn(), "", product.revisionNo(), product.incomplete(),
                    product.previewImageUrl(), product.hallIds()));
        }
        Map<Long, List<Long>> hallIdsByAwardId = new LinkedHashMap<>();
        for (ShowroomHall hall : halls.values()) {
            for (ShowroomHallItemMapping mapping : hall.itemMappings()) {
                if (ShowroomHallItemMapping.TYPE_AWARD.equals(mapping.itemType())) {
                    hallIdsByAwardId.computeIfAbsent(mapping.itemId(), ignored -> new ArrayList<>())
                            .add(hall.hallId());
                }
            }
        }
        for (ShowroomAwardSnapshot award : listAwards()) {
            ShowroomAwardRevision revision = getCurrentOrLatestAwardRevision(award.awardId());
            options.add(new ShowroomHallItemOption(ShowroomHallItemMapping.TYPE_AWARD, award.awardId(),
                    award.awardCode(), nullToEmpty(revision.nameCn()), nullToEmpty(revision.nameEn()),
                    revision.revisionNo(), award.incomplete(), nullToEmpty(revision.fields().get("cover_image")),
                    hallIdsByAwardId.getOrDefault(award.awardId(), List.of())));
        }
        return List.copyOf(options);
    }

    @Override
    public void deleteProduct(Long productId) {
        ProductAggregate product = requireProduct(productId);
        products.remove(product.productId);
        product.revisions.keySet().forEach(productRevisionOwners::remove);
        halls.replaceAll((hallId, hall) -> new ShowroomHall(hall.hallId(), hall.hallCode(), hall.name(),
                hall.nameEn(),
                hall.description(),
                hall.descriptionEn(),
                hall.canvasBackgroundImageUrl(),
                hall.productMappings().stream()
                .filter(mapping -> !product.productId.equals(mapping.productId()))
                .toList(),
                hall.itemMappings().stream()
                        .filter(mapping -> !(ShowroomHallItemMapping.TYPE_PRODUCT.equals(mapping.itemType())
                                && product.productId.equals(mapping.itemId())))
                        .toList()));
    }

    public ShowroomProductRevision publishProductRevision(Long revisionId, Long operatorId) {
        requireNonNull(operatorId, "SHOWROOM_ROLE_BINDING_MISSING: publish operator is required");
        ProductAggregate product = requireProductByRevision(revisionId);
        ShowroomProductRevision revision = product.revisions.get(revisionId);
        if (isProductPublishBlocked(revision.nameCn(), revision.nameEn())) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: product publish requires name_en");
        }
        boolean incomplete = isProductIncomplete(
                revision.nameCn(),
                revision.nameEn(),
                revision.fields().get("owner_company_id"),
                revision.fields().get("product_owner_type"),
                revision.fields().get("lifecycle_stage"));
        ShowroomProductRevision published = new ShowroomProductRevision(revision.productId(), revision.revisionId(),
                revision.revisionNo(), STATUS_PUBLISHED, revision.nameCn(), revision.nameEn(), incomplete,
                revision.fields(), revision.attachments());
        product.revisions.put(revisionId, published);
        product.currentRevisionId = revisionId;
        product.incomplete = incomplete;
        appendAudits("PRODUCT", product.productId, revisionId, productAuditFields(revision), operatorId, ACTION_PUBLISH);
        return published;
    }

    @Override
    public ShowroomAwardRevision saveAwardDraft(ShowroomAwardDraft draft) {
        requireNonNull(draft, "SHOWROOM_REQUIRED_FIELD_MISSING: award draft is required");
        AwardAggregate award = draft.awardId() == null ? createAward(draft.awardCode()) : requireAward(draft.awardId());
        award.awardCode = requireText(draft.awardCode(), "SHOWROOM_REQUIRED_FIELD_MISSING: award code is required");
        boolean incomplete = isAwardIncomplete(draft.nameCn(), draft.nameEn(), draft.coverImage());
        Map<String, String> fields = awardFields(draft.descriptionZh(), draft.descriptionEn(), draft.issuer(),
                draft.awardDateText(), draft.coverImage());
        ShowroomAwardRevision revision = new ShowroomAwardRevision(award.awardId, awardRevisionIdSequence++,
                ++award.revisionNo, STATUS_DRAFT, award.awardCode, draft.nameCn(), draft.nameEn(), incomplete,
                fields);
        award.revisions.put(revision.revisionId(), revision);
        awardRevisionOwners.put(revision.revisionId(), award.awardId);
        award.incomplete = incomplete;
        return revision;
    }

    @Override
    public ShowroomAwardSnapshot getAward(Long awardId) {
        AwardAggregate award = requireAward(awardId);
        return new ShowroomAwardSnapshot(award.awardId, award.awardCode, Optional.ofNullable(award.currentRevisionId),
                award.incomplete, award.currentRevisionId != null);
    }

    @Override
    public ShowroomAwardRevision getAwardRevision(Long revisionId) {
        AwardAggregate award = requireAwardByRevision(revisionId);
        ShowroomAwardRevision revision = award.revisions.get(revisionId);
        if (revision == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: award revision not found");
        }
        return revision;
    }

    @Override
    public ShowroomAwardRevision getLatestAwardRevision(Long awardId) {
        AwardAggregate award = requireAward(awardId);
        return award.revisions.values().stream()
                .max(Comparator.comparingInt(ShowroomAwardRevision::revisionNo)
                        .thenComparingLong(ShowroomAwardRevision::revisionId))
                .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: award revision not found"));
    }

    @Override
    public ShowroomAwardRevision getCurrentOrLatestAwardRevision(Long awardId) {
        AwardAggregate award = requireAward(awardId);
        if (award.currentRevisionId != null) {
            return award.revisions.get(award.currentRevisionId);
        }
        return getLatestAwardRevision(awardId);
    }

    @Override
    public ShowroomAwardRevision requireCurrentAwardRevision(Long awardId) {
        AwardAggregate award = requireAward(awardId);
        if (award.currentRevisionId == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live award revision not found");
        }
        return award.revisions.get(award.currentRevisionId);
    }

    @Override
    public List<ShowroomAwardSnapshot> listAwards() {
        return awards.values().stream()
                .map(award -> new ShowroomAwardSnapshot(award.awardId, award.awardCode,
                        Optional.ofNullable(award.currentRevisionId), award.incomplete,
                        award.currentRevisionId != null))
                .toList();
    }

    @Override
    public void deleteAward(Long awardId) {
        AwardAggregate award = requireAward(awardId);
        awards.remove(award.awardId);
        award.revisions.keySet().forEach(awardRevisionOwners::remove);
        halls.replaceAll((hallId, hall) -> toHallWithItems(hall,
                hall.itemMappings().stream()
                        .filter(mapping -> !(ShowroomHallItemMapping.TYPE_AWARD.equals(mapping.itemType())
                                && award.awardId.equals(mapping.itemId())))
                        .toList()));
    }

    @Override
    public ShowroomAwardRevision publishAwardRevision(Long revisionId, Long operatorId) {
        requireNonNull(operatorId, "SHOWROOM_ROLE_BINDING_MISSING: publish operator is required");
        AwardAggregate award = requireAwardByRevision(revisionId);
        ShowroomAwardRevision revision = award.revisions.get(revisionId);
        if (!hasText(revision.nameCn())) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: award publish requires name_cn");
        }
        if (!hasText(revision.fields().get("cover_image"))) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: award publish requires cover_image");
        }
        boolean incomplete = isAwardIncomplete(revision.nameCn(), revision.nameEn(), revision.fields().get("cover_image"));
        ShowroomAwardRevision published = new ShowroomAwardRevision(revision.awardId(), revision.revisionId(),
                revision.revisionNo(), STATUS_PUBLISHED, revision.awardCode(), revision.nameCn(), revision.nameEn(),
                incomplete, revision.fields());
        award.revisions.put(revisionId, published);
        award.currentRevisionId = revisionId;
        award.incomplete = incomplete;
        ensureCompanyHonorHallAwardMappings();
        appendAudits("AWARD", award.awardId, revisionId, revision.fields(), operatorId, ACTION_PUBLISH);
        return published;
    }

    public ShowroomHall createHall(String hallCode, String name, String nameEn, String description, String descriptionEn) {
        ShowroomHall hall = new ShowroomHall(hallIdSequence++,
                requireText(hallCode, "SHOWROOM_REQUIRED_FIELD_MISSING: hall code is required"),
                requireText(name, "SHOWROOM_REQUIRED_FIELD_MISSING: hall name is required"),
                requireText(nameEn, "SHOWROOM_REQUIRED_FIELD_MISSING: hall name_en is required"),
                description, descriptionEn, List.of());
        halls.put(hall.hallId(), hall);
        return hall;
    }

    public ShowroomHall updateHall(Long hallId, String name, String nameEn, String description, String descriptionEn) {
        ShowroomHall hall = requireHall(hallId);
        ShowroomHall updated = new ShowroomHall(hall.hallId(), hall.hallCode(),
                requireText(name, "SHOWROOM_REQUIRED_FIELD_MISSING: hall name is required"),
                requireText(nameEn, "SHOWROOM_REQUIRED_FIELD_MISSING: hall name_en is required"),
                description, descriptionEn, hall.canvasBackgroundImageUrl(),
                hall.productMappings(), hall.itemMappings());
        halls.put(hallId, updated);
        return updated;
    }

    @Override
    public ShowroomHall updateHallCanvasBackground(Long hallId, String canvasBackgroundImageUrl) {
        ShowroomHall hall = requireHall(hallId);
        ShowroomHall updated = new ShowroomHall(hall.hallId(), hall.hallCode(), hall.name(), hall.nameEn(),
                hall.description(), hall.descriptionEn(), nullableTrimmedText(canvasBackgroundImageUrl),
                hall.productMappings(), hall.itemMappings());
        halls.put(hallId, updated);
        return updated;
    }

    public ShowroomHall replaceHallProductMappings(Long hallId, List<ShowroomHallProductMapping> mappings) {
        requireNonNull(mappings, "SHOWROOM_REQUIRED_FIELD_MISSING: hall product mappings are required");
        return replaceHallItemMappings(hallId, mappings.stream()
                .map(mapping -> new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_PRODUCT,
                        mapping.productId(), mapping.displayOrder(), mapping.layoutX(), mapping.layoutY(),
                        mapping.layoutWidth(), mapping.layoutHeight()))
                .toList());
    }

    @Override
    public ShowroomHall replaceHallCanvasLayout(Long hallId, List<ShowroomHallProductMapping> mappings) {
        requireNonNull(mappings, "SHOWROOM_REQUIRED_FIELD_MISSING: hall product mappings are required");
        return replaceHallItemCanvasLayout(hallId, mappings.stream()
                .map(mapping -> new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_PRODUCT,
                        mapping.productId(), mapping.displayOrder(), mapping.layoutX(), mapping.layoutY(),
                        mapping.layoutWidth(), mapping.layoutHeight()))
                .toList());
    }

    @Override
    public ShowroomHall replaceHallItemMappings(Long hallId, List<ShowroomHallItemMapping> mappings) {
        ShowroomHall hall = requireHall(hallId);
        List<ShowroomHallItemMapping> ordered = normalizeHallItemMappings(mappings);
        requireHallItemPlacementAllowed(hall, ordered);
        ShowroomHall updated = toHallWithItems(hall, ordered);
        halls.put(hallId, updated);
        return updated;
    }

    @Override
    public ShowroomHall replaceHallItemCanvasLayout(Long hallId, List<ShowroomHallItemMapping> mappings) {
        ShowroomHall hall = requireHall(hallId);
        List<ShowroomHallItemMapping> ordered = normalizeHallItemMappings(mappings);
        requireHallItemPlacementAllowed(hall, ordered);
        List<ShowroomHallItemMapping> resolved = requireItemCanvasLayout(ordered);
        ShowroomHall updated = toHallWithItems(hall, resolved);
        halls.put(hallId, updated);
        return updated;
    }

    private List<ShowroomHallItemMapping> normalizeHallItemMappings(List<ShowroomHallItemMapping> mappings) {
        requireNonNull(mappings, "SHOWROOM_REQUIRED_FIELD_MISSING: hall item mappings are required");
        if (mappings.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: hall must keep at least one item mapping");
        }
        Set<String> itemKeys = new HashSet<>();
        List<ShowroomHallItemMapping> ordered = new ArrayList<>();
        for (ShowroomHallItemMapping mapping : mappings) {
            requireNonNull(mapping.itemId(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall item_id is required");
            requireNonNull(mapping.displayOrder(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall display_order is required");
            String itemKey = mapping.itemType() + ":" + mapping.itemId();
            if (!itemKeys.add(itemKey)) {
                throw new IllegalStateException("SHOWROOM_DUPLICATE_ITEM: duplicate hall item mapping is invalid");
            }
            if (ShowroomHallItemMapping.TYPE_PRODUCT.equals(mapping.itemType())) {
                requireProduct(mapping.itemId());
            } else {
                requireAward(mapping.itemId());
            }
            ordered.add(mapping);
        }
        ordered.sort(Comparator.comparing(ShowroomHallItemMapping::displayOrder));
        return ordered;
    }

    private void requireHallItemPlacementAllowed(ShowroomHall hall, List<ShowroomHallItemMapping> orderedMappings) {
        boolean companyHonorHall = isEnterpriseHonorHall(hall);
        for (ShowroomHallItemMapping mapping : orderedMappings) {
            if (ShowroomHallItemMapping.TYPE_AWARD.equals(mapping.itemType()) && !companyHonorHall) {
                throw new IllegalStateException(
                        "SHOWROOM_AWARD_HALL_FORBIDDEN: awards must be placed in enterprise honor halls");
            }
            if (ShowroomHallItemMapping.TYPE_PRODUCT.equals(mapping.itemType()) && companyHonorHall) {
                throw new IllegalStateException(
                        "SHOWROOM_COMPANY_HONOR_HALL_PRODUCT_FORBIDDEN: enterprise honor halls only accept awards");
            }
        }
    }

    private void ensureCompanyHonorHallAwardMappings() {
        ensureCompanyHonorHalls();
        List<AwardAggregate> publishedAwards = awards.values().stream()
                .filter(award -> award.currentRevisionId != null)
                .sorted(Comparator.comparing((AwardAggregate award) -> nullToEmpty(award.awardCode))
                        .thenComparing(award -> award.awardId))
                .toList();
        Map<String, List<ShowroomHallItemMapping>> mappingsByHallCode = splitCompanyHonorAwardMappings(publishedAwards);
        halls.replaceAll((hallId, hall) -> {
            if (isEnterpriseHonorHall(hall)) {
                List<ShowroomHallItemMapping> mappings = mappingsByHallCode.getOrDefault(hall.hallCode(), List.of());
                return toHallWithItems(hall, mappings.isEmpty() ? List.of() : withDefaultItemLayout(mappings));
            }
            return toHallWithItems(hall, hall.itemMappings().stream()
                    .filter(mapping -> !ShowroomHallItemMapping.TYPE_AWARD.equals(mapping.itemType()))
                    .toList());
        });
    }

    private List<ShowroomHall> ensureCompanyHonorHalls() {
        halls.entrySet().removeIf(entry ->
                ShowroomEnterpriseHonorHalls.LEGACY_HALL_CODE.equals(entry.getValue().hallCode()));
        List<ShowroomHall> result = new ArrayList<>();
        for (ShowroomEnterpriseHonorHalls.Definition definition : ShowroomEnterpriseHonorHalls.DEFINITIONS) {
            ShowroomHall hall = halls.values().stream()
                    .filter(existing -> definition.hallCode().equals(existing.hallCode()))
                    .findFirst()
                    .map(existing -> new ShowroomHall(existing.hallId(), definition.hallCode(), definition.name(),
                            definition.nameEn(), definition.description(), definition.descriptionEn(),
                            existing.canvasBackgroundImageUrl(), existing.productMappings(), existing.itemMappings()))
                    .orElseGet(() -> new ShowroomHall(hallIdSequence++, definition.hallCode(), definition.name(),
                            definition.nameEn(), definition.description(), definition.descriptionEn(), List.of()));
            halls.put(hall.hallId(), hall);
            result.add(hall);
        }
        return result;
    }

    private static Map<String, List<ShowroomHallItemMapping>> splitCompanyHonorAwardMappings(
            List<AwardAggregate> publishedAwards) {
        Map<String, List<ShowroomHallItemMapping>> mappingsByHallCode = new LinkedHashMap<>();
        for (ShowroomEnterpriseHonorHalls.Definition definition : ShowroomEnterpriseHonorHalls.DEFINITIONS) {
            mappingsByHallCode.put(definition.hallCode(), new ArrayList<>());
        }
        int firstHalfCount = (publishedAwards.size() + 1) / 2;
        for (int index = 0; index < publishedAwards.size(); index++) {
            ShowroomEnterpriseHonorHalls.Definition definition = index < firstHalfCount
                    ? ShowroomEnterpriseHonorHalls.DEFINITIONS.get(0)
                    : ShowroomEnterpriseHonorHalls.DEFINITIONS.get(1);
            List<ShowroomHallItemMapping> mappings = mappingsByHallCode.get(definition.hallCode());
            mappings.add(new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_AWARD,
                    publishedAwards.get(index).awardId, mappings.size() + 1));
        }
        return mappingsByHallCode;
    }

    private static List<ShowroomHallItemMapping> withDefaultItemLayout(
            List<ShowroomHallItemMapping> orderedMappings) {
        List<ShowroomHallProductMapping> layoutCarriers =
                ShowroomHallCanvasLayoutPolicy.withDefaultLayoutIfMissing(
                        toLayoutOnlyProductMappings(orderedMappings));
        return applyProductLayoutToItems(orderedMappings, layoutCarriers);
    }

    private static boolean isEnterpriseHonorHall(ShowroomHall hall) {
        return hall != null && ShowroomEnterpriseHonorHalls.isEnterpriseHonorHallCode(hall.hallCode());
    }

    public ShowroomHall getHall(Long hallId) {
        return requireHall(hallId);
    }

    public List<ShowroomHall> listHalls() {
        return List.copyOf(halls.values());
    }

    @Override
    public List<ShowroomHall> listHalls(String keyword, Integer pageNo, Integer pageSize) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return page(listHalls().stream()
                .filter(hall -> matchesHall(hall, normalizedKeyword))
                .toList(), pageNo, pageSize);
    }

    @Override
    public void deleteHall(Long hallId) {
        requireHall(hallId);
        halls.remove(hallId);
    }

    public List<ShowroomVersionAudit> versionAudits(String targetType, Long targetId) {
        return List.copyOf(versionAudits.getOrDefault(targetKey(targetType, targetId), List.of()));
    }

    private CompanyAggregate createCompany(String companyType, String displayName, String displayNameEn) {
        CompanyAggregate company = new CompanyAggregate(companyIdSequence++,
                requireText(companyType, "SHOWROOM_REQUIRED_FIELD_MISSING: company type is required"),
                requireText(displayName, "SHOWROOM_REQUIRED_FIELD_MISSING: company display name is required"),
                requireText(displayNameEn, "SHOWROOM_REQUIRED_FIELD_MISSING: company display name_en is required"));
        companies.put(company.companyId, company);
        return company;
    }

    private ProductAggregate createProduct(Long productMasterId, String productCode) {
        ProductAggregate product = new ProductAggregate(productIdSequence++, productMasterId,
                requireText(productCode, "SHOWROOM_REQUIRED_FIELD_MISSING: product code is required"));
        products.put(product.productId, product);
        return product;
    }

    private AwardAggregate createAward(String awardCode) {
        AwardAggregate award = new AwardAggregate(awardIdSequence++,
                requireText(awardCode, "SHOWROOM_REQUIRED_FIELD_MISSING: award code is required"));
        awards.put(award.awardId, award);
        return award;
    }

    private CompanyAggregate requireCompany(Long companyId) {
        requireNonNull(companyId, "SHOWROOM_TARGET_NOT_FOUND: company id is required");
        CompanyAggregate company = companies.get(companyId);
        if (company == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company not found");
        }
        return company;
    }

    private CompanyAggregate requireCompanyByRevision(Long revisionId) {
        requireNonNull(revisionId, "SHOWROOM_TARGET_NOT_FOUND: company revision id is required");
        Long companyId = companyRevisionOwners.get(revisionId);
        if (companyId == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company revision not found");
        }
        return requireCompany(companyId);
    }

    private ProductAggregate requireProduct(Long productId) {
        requireNonNull(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        ProductAggregate product = products.get(productId);
        if (product == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product not found");
        }
        return product;
    }

    private ProductAggregate requireProductByRevision(Long revisionId) {
        requireNonNull(revisionId, "SHOWROOM_TARGET_NOT_FOUND: product revision id is required");
        Long productId = productRevisionOwners.get(revisionId);
        if (productId == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found");
        }
        return requireProduct(productId);
    }

    private AwardAggregate requireAward(Long awardId) {
        requireNonNull(awardId, "SHOWROOM_TARGET_NOT_FOUND: award id is required");
        AwardAggregate award = awards.get(awardId);
        if (award == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: award not found");
        }
        return award;
    }

    private AwardAggregate requireAwardByRevision(Long revisionId) {
        requireNonNull(revisionId, "SHOWROOM_TARGET_NOT_FOUND: award revision id is required");
        Long awardId = awardRevisionOwners.get(revisionId);
        if (awardId == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: award revision not found");
        }
        return requireAward(awardId);
    }

    private ShowroomHall requireHall(Long hallId) {
        requireNonNull(hallId, "SHOWROOM_TARGET_NOT_FOUND: hall id is required");
        ShowroomHall hall = halls.get(hallId);
        if (hall == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: hall not found");
        }
        return hall;
    }

    private boolean matchesProduct(ShowroomProductSnapshot snapshot, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        if (containsIgnoreCase(snapshot.productCode(), keyword)) {
            return true;
        }
        if (containsIgnoreCase(snapshot.legacyProductCode(), keyword)) {
            return true;
        }
        ShowroomProductRevision revision = getCurrentOrLatestProductRevision(snapshot.productId());
        return containsIgnoreCase(revision.nameCn(), keyword) || containsIgnoreCase(revision.nameEn(), keyword);
    }

    private static boolean matchesHall(ShowroomHall hall, String keyword) {
        return !hasText(keyword)
                || containsIgnoreCase(hall.hallCode(), keyword)
                || containsIgnoreCase(hall.name(), keyword)
                || containsIgnoreCase(hall.nameEn(), keyword)
                || containsIgnoreCase(hall.description(), keyword)
                || containsIgnoreCase(hall.descriptionEn(), keyword);
    }

    private static <T> List<T> page(List<T> values, Integer pageNo, Integer pageSize) {
        int resolvedPageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int resolvedPageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 20);
        int fromIndex = Math.min((resolvedPageNo - 1) * resolvedPageSize, values.size());
        int toIndex = Math.min(fromIndex + resolvedPageSize, values.size());
        return values.subList(fromIndex, toIndex);
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private static boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private static boolean isProductPublishBlocked(String nameCn, String nameEn) {
        for (String requiredField : ShowroomPublishContract.requiredProductPublishFields()) {
            if ("name_cn".equals(requiredField) && !hasText(nameCn)) {
                return true;
            }
            if ("name_en".equals(requiredField) && !hasText(nameEn)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProductIncomplete(String nameCn, String nameEn, String ownerCompanyId,
                                               String productOwnerType, String lifecycleStage) {
        for (String requiredField : ShowroomPublishContract.requiredProductCompletenessFields()) {
            if ("name_cn".equals(requiredField) && !hasText(nameCn)) {
                return true;
            }
            if ("name_en".equals(requiredField) && !hasText(nameEn)) {
                return true;
            }
            if ("owner_company_id".equals(requiredField) && !hasText(ownerCompanyId)) {
                return true;
            }
            if ("product_owner_type".equals(requiredField) && !hasText(productOwnerType)) {
                return true;
            }
            if ("lifecycle_stage".equals(requiredField) && !hasText(lifecycleStage)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAwardIncomplete(String nameCn, String nameEn, String coverImage) {
        return !hasText(nameCn) || !hasText(nameEn) || !hasText(coverImage);
    }

    private static Map<String, String> awardFields(String descriptionZh, String descriptionEn, String issuer,
                                                   String awardDateText, String coverImage) {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfHasText(fields, "description_zh", descriptionZh);
        putIfHasText(fields, "description_en", descriptionEn);
        putIfHasText(fields, "issuer", issuer);
        putIfHasText(fields, "award_date_text", awardDateText);
        putIfHasText(fields, "cover_image", coverImage);
        return Collections.unmodifiableMap(fields);
    }

    private static void putIfHasText(Map<String, String> fields, String key, String value) {
        if (hasText(value)) {
            fields.put(key, value);
        }
    }

    private static ShowroomHall toHallWithItems(ShowroomHall hall, List<ShowroomHallItemMapping> itemMappings) {
        List<ShowroomHallProductMapping> products = itemMappings.stream()
                .filter(mapping -> ShowroomHallItemMapping.TYPE_PRODUCT.equals(mapping.itemType()))
                .map(ShowroomHallItemMapping::asProductMapping)
                .toList();
        return new ShowroomHall(hall.hallId(), hall.hallCode(), hall.name(), hall.nameEn(), hall.description(),
                hall.descriptionEn(), hall.canvasBackgroundImageUrl(), products, itemMappings);
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

    private static Map<String, String> productAuditFields(ShowroomProductRevision revision) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name_cn", revision.nameCn());
        values.put("name_en", revision.nameEn());
        values.putAll(revision.fields());
        return values;
    }

    private void appendAudits(String targetType, Long targetId, Long revisionId, Map<String, String> fields,
                              Long operatorId, String operatorAction) {
        List<ShowroomVersionAudit> audits = versionAudits.computeIfAbsent(targetKey(targetType, targetId),
                key -> new ArrayList<>());
        fields.forEach((fieldCode, newValue) -> audits.add(new ShowroomVersionAudit(targetType, targetId, revisionId,
                fieldCode, null, newValue, operatorId, operatorAction, Instant.now())));
    }

    private static Map<String, String> copyFields(Map<String, String> fields) {
        requireNonNull(fields, "SHOWROOM_REQUIRED_FIELD_MISSING: draft fields are required");
        return Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    private static String targetKey(String targetType, Long targetId) {
        return targetType + ":" + targetId;
    }

    private static void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    private static String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String nullableTrimmedText(String value) {
        return hasText(value) ? value.trim() : "";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class CompanyAggregate {
        private final Long companyId;
        private String companyType;
        private String displayName;
        private String displayNameEn;
        private Long currentRevisionId;
        private int revisionNo;
        private final Map<Long, ShowroomCompanyRevision> revisions = new LinkedHashMap<>();

        private CompanyAggregate(Long companyId, String companyType, String displayName, String displayNameEn) {
            this.companyId = companyId;
            this.companyType = companyType;
            this.displayName = displayName;
            this.displayNameEn = displayNameEn;
        }
    }

    private static final class ProductAggregate {
        private final Long productId;
        private Long productMasterId;
        private String productCode;
        private Long currentRevisionId;
        private boolean incomplete;
        private int revisionNo;
        private final Map<Long, ShowroomProductRevision> revisions = new LinkedHashMap<>();

        private ProductAggregate(Long productId, Long productMasterId, String productCode) {
            this.productId = productId;
            this.productMasterId = productMasterId;
            this.productCode = productCode;
        }
    }

    private static final class AwardAggregate {
        private final Long awardId;
        private String awardCode;
        private Long currentRevisionId;
        private boolean incomplete = true;
        private int revisionNo;
        private final Map<Long, ShowroomAwardRevision> revisions = new LinkedHashMap<>();

        private AwardAggregate(Long awardId, String awardCode) {
            this.awardId = awardId;
            this.awardCode = awardCode;
        }
    }

}
