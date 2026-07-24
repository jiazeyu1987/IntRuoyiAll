package cn.iocoder.yudao.module.showroom.content.service;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.mdm.enums.MdmProductStatusConstants;
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
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductAttachment;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductAttachmentPolicy;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomCompanyDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomCompanyRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomAwardDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomAwardRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomHallDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomHallItemDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomHallProductDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionAttachmentDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomVersionAuditDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomCompanyMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomCompanyRevisionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomAwardMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomAwardRevisionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomHallMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomHallItemMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomHallProductMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionAttachmentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomVersionAuditMapper;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomPublishContract;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseAutoPublishService;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomVersionAudit;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ShowroomPersistentContentService implements ShowroomContentOperations {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String MASTER_STATUS_DRAFT_ONLY = "DRAFT_ONLY";
    private static final String MASTER_STATUS_LIVE = "LIVE";
    private static final String MASTER_STATUS_INCOMPLETE = "INCOMPLETE";
    private static final String HALL_STATUS_ACTIVE = "ACTIVE";
    private static final String ACTION_PUBLISH = "PUBLISH";

    private final ShowroomCompanyMapper companyMapper;
    private final ShowroomCompanyRevisionMapper companyRevisionMapper;
    private final ShowroomProductMapper productMapper;
    private final ShowroomProductRevisionMapper productRevisionMapper;
    private final ShowroomProductRevisionAttachmentMapper productRevisionAttachmentMapper;
    private final ShowroomAwardMapper awardMapper;
    private final ShowroomAwardRevisionMapper awardRevisionMapper;
    private final ShowroomHallMapper hallMapper;
    private final ShowroomHallItemMapper hallItemMapper;
    private final ShowroomHallProductMapper hallProductMapper;
    private final ShowroomVersionAuditMapper versionAuditMapper;
    private final MdmProductApi productApi;
    private final ShowroomReleaseAutoPublishService releaseAutoPublishService;
    private final ObjectProvider<ShowroomReleaseAutoPublishService> releaseAutoPublishServiceProvider;

    @Autowired
    public ShowroomPersistentContentService(ShowroomCompanyMapper companyMapper,
                                            ShowroomCompanyRevisionMapper companyRevisionMapper,
                                            ShowroomProductMapper productMapper,
                                            ShowroomProductRevisionMapper productRevisionMapper,
                                            ShowroomProductRevisionAttachmentMapper productRevisionAttachmentMapper,
                                            ShowroomAwardMapper awardMapper,
                                            ShowroomAwardRevisionMapper awardRevisionMapper,
                                            ShowroomHallMapper hallMapper,
                                            ShowroomHallItemMapper hallItemMapper,
                                            ShowroomHallProductMapper hallProductMapper,
                                            ShowroomVersionAuditMapper versionAuditMapper,
                                            ObjectProvider<MdmProductApi> productApiProvider,
                                            ObjectProvider<ShowroomReleaseAutoPublishService> releaseAutoPublishServiceProvider) {
        this.companyMapper = companyMapper;
        this.companyRevisionMapper = companyRevisionMapper;
        this.productMapper = productMapper;
        this.productRevisionMapper = productRevisionMapper;
        this.productRevisionAttachmentMapper = productRevisionAttachmentMapper;
        this.awardMapper = awardMapper;
        this.awardRevisionMapper = awardRevisionMapper;
        this.hallMapper = hallMapper;
        this.hallItemMapper = hallItemMapper;
        this.hallProductMapper = hallProductMapper;
        this.versionAuditMapper = versionAuditMapper;
        this.productApi = productApiProvider.getIfAvailable();
        this.releaseAutoPublishService = null;
        this.releaseAutoPublishServiceProvider = releaseAutoPublishServiceProvider;
    }

    ShowroomPersistentContentService(ShowroomCompanyMapper companyMapper,
                                     ShowroomCompanyRevisionMapper companyRevisionMapper,
                                     ShowroomProductMapper productMapper,
                                     ShowroomProductRevisionMapper productRevisionMapper,
                                     ShowroomProductRevisionAttachmentMapper productRevisionAttachmentMapper,
                                     ShowroomAwardMapper awardMapper,
                                     ShowroomAwardRevisionMapper awardRevisionMapper,
                                     ShowroomHallMapper hallMapper,
                                     ShowroomHallItemMapper hallItemMapper,
                                     ShowroomHallProductMapper hallProductMapper,
                                     ShowroomVersionAuditMapper versionAuditMapper,
                                     ShowroomReleaseAutoPublishService releaseAutoPublishService) {
        this.companyMapper = companyMapper;
        this.companyRevisionMapper = companyRevisionMapper;
        this.productMapper = productMapper;
        this.productRevisionMapper = productRevisionMapper;
        this.productRevisionAttachmentMapper = productRevisionAttachmentMapper;
        this.awardMapper = awardMapper;
        this.awardRevisionMapper = awardRevisionMapper;
        this.hallMapper = hallMapper;
        this.hallItemMapper = hallItemMapper;
        this.hallProductMapper = hallProductMapper;
        this.versionAuditMapper = versionAuditMapper;
        this.productApi = null;
        this.releaseAutoPublishService = releaseAutoPublishService;
        this.releaseAutoPublishServiceProvider = null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomCompanyRevision saveCompanyDraft(ShowroomCompanyDraft draft) {
        requireNonNull(draft, "SHOWROOM_REQUIRED_FIELD_MISSING: company draft is required");
        Map<String, String> fields = copyFields(draft.fields());
        ShowroomCompanyDO company = draft.companyId() == null
                ? resolveOrCreateCompany(draft.companyType(), draft.displayName(), draft.displayNameEn())
                : requireCompany(draft.companyId());
        company.setCompanyType(requireText(draft.companyType(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: company type is required"));
        company.setDisplayName(requireText(draft.displayName(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: company display name is required"));
        company.setDisplayNameEn(requireText(draft.displayNameEn(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: company display name_en is required"));
        ShowroomCompanyRevisionDO latestRevision = companyRevisionMapper.selectLatestByCompanyId(company.getId());
        int nextRevisionNo = latestRevision == null ? 1 : latestRevision.getRevisionNo() + 1;
        ShowroomCompanyRevisionDO revision = ShowroomCompanyRevisionDO.builder()
                .companyId(company.getId())
                .revisionNo(nextRevisionNo)
                .status(STATUS_DRAFT)
                .developmentHistory(fields.get("development_history"))
                .developmentHistoryEn(fields.get("development_history_en"))
                .parkIntroduction(fields.get("park_introduction"))
                .parkIntroductionEn(fields.get("park_introduction_en"))
                .incubationPlatform(fields.get("incubation_platform"))
                .incubationPlatformEn(fields.get("incubation_platform_en"))
                .subsidiaryOverview(fields.get("subsidiary_overview"))
                .subsidiaryOverviewEn(fields.get("subsidiary_overview_en"))
                .stockInfo(fields.get("stock_info"))
                .stockInfoEn(fields.get("stock_info_en"))
                .coverImage(nullableText(fields.get("cover_image")))
                .coreManufacturingCapability(fields.get("core_manufacturing_capability"))
                .coreManufacturingCapabilityEn(fields.get("core_manufacturing_capability_en"))
                .honorsAwards(fields.get("honors_awards"))
                .honorsAwardsEn(fields.get("honors_awards_en"))
                .displayNameSnapshot(company.getDisplayName())
                .displayNameEnSnapshot(company.getDisplayNameEn())
                .companyTypeSnapshot(company.getCompanyType())
                .build();
        companyRevisionMapper.insert(assignTenant(revision));

        company.setCurrentRevisionNo(nextRevisionNo);
        company.setStatus(company.getCurrentRevisionId() == null ? MASTER_STATUS_DRAFT_ONLY : MASTER_STATUS_LIVE);
        company.setIncompleteFlag(Boolean.FALSE);
        companyMapper.updateById(company);
        return toCompanyRevision(revision);
    }

    @Override
    public ShowroomCompanySnapshot getCompany(Long companyId) {
        return toCompanySnapshot(requireCompany(companyId));
    }

    @Override
    public List<ShowroomCompanySnapshot> listCompanies() {
        return companyMapper.selectListOrdered().stream()
                .map(this::toCompanySnapshot)
                .toList();
    }

    @Override
    public ShowroomCompanyRevision getCompanyRevision(Long revisionId) {
        return toCompanyRevision(requireCompanyRevisionDO(revisionId));
    }

    @Override
    public ShowroomCompanyRevision requireCurrentCompanyRevision() {
        ShowroomCompanyDO company = companyMapper.selectMainCompany();
        if (company == null || company.getCurrentRevisionId() == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live company revision not found");
        }
        return getCompanyRevision(company.getCurrentRevisionId());
    }

    @Override
    public Optional<ShowroomCompanyRevision> findCurrentOrLatestCompanyRevision() {
        ShowroomCompanyDO company = companyMapper.selectMainCompany();
        if (company == null) {
            return Optional.empty();
        }
        if (company.getCurrentRevisionId() != null) {
            return Optional.of(getCompanyRevision(company.getCurrentRevisionId()));
        }
        ShowroomCompanyRevisionDO latest = companyRevisionMapper.selectLatestByCompanyId(company.getId());
        if (latest == null) {
            return Optional.empty();
        }
        requireTenant(latest.getTenantId(), "SHOWROOM_TARGET_NOT_FOUND: company revision not found");
        return Optional.of(toCompanyRevision(latest));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomCompanyRevision publishCompanyRevision(Long revisionId, Long operatorId) {
        requireNonNull(operatorId, "SHOWROOM_ROLE_BINDING_MISSING: publish operator is required");
        ShowroomCompanyRevisionDO revision = requireCompanyRevisionDO(revisionId);
        ShowroomCompanyDO company = requireCompany(revision.getCompanyId());
        revision.setStatus(STATUS_PUBLISHED);
        revision.setApprovedBy(operatorId);
        revision.setPublishedAt(LocalDateTime.now());
        companyRevisionMapper.updateById(revision);

        company.setCurrentRevisionId(revision.getId());
        company.setCurrentRevisionNo(revision.getRevisionNo());
        company.setStatus(MASTER_STATUS_LIVE);
        company.setIncompleteFlag(Boolean.FALSE);
        companyMapper.updateById(company);

        appendAudits("COMPANY", company.getId(), revision.getId(), toCompanyFieldMap(revision), operatorId, ACTION_PUBLISH);
        markReleaseDirty("COMPANY_REVISION_PUBLISHED", operatorId);
        return toCompanyRevision(revision);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomProductRevision saveProductDraft(ShowroomProductDraft draft) {
        requireNonNull(draft, "SHOWROOM_REQUIRED_FIELD_MISSING: product draft is required");
        Map<String, String> fields = copyFields(draft.fields());
        List<ShowroomProductAttachment> attachments =
                ShowroomProductAttachmentPolicy.normalizedCopy(draft.attachments());
        ShowroomProductDO product = draft.productId() == null ? null : requireProduct(draft.productId());
        Long effectiveProductMasterId = draft.productMasterId() != null
                ? draft.productMasterId()
                : product == null ? null : product.getProductMasterId();
        ResolvedProductMaster productMaster = resolveProductMaster(effectiveProductMasterId);
        String productCode = productMaster == null ? draft.productCode() : productMaster.productCode();
        String nameCn = productMaster == null ? draft.nameCn() : productMaster.nameCn();
        String nameEn = productMaster == null ? draft.nameEn() : productMaster.nameEn();
        product = product == null
                ? createProduct(productMaster == null ? null : productMaster.id(), requireText(productCode,
                "SHOWROOM_REQUIRED_FIELD_MISSING: product code is required"))
                : product;
        if (productMaster != null) {
            product.setProductMasterId(productMaster.id());
        }
        product.setProductCode(requireText(productCode,
                "SHOWROOM_REQUIRED_FIELD_MISSING: product code is required"));
        if (draft.legacyProductCode() != null) {
            product.setLegacyProductCode(nullableText(draft.legacyProductCode()));
        }
        Long ownerCompanyId = parseLongField(fields, "owner_company_id");
        String productOwnerType = nullableText(fields.get("product_owner_type"));
        String lifecycleStage = nullableText(fields.get("lifecycle_stage"));
        boolean incomplete = isProductIncomplete(
                nameCn, nameEn, ownerCompanyId, productOwnerType, lifecycleStage);
        ShowroomProductRevisionDO latestRevision = productRevisionMapper.selectLatestByProductId(product.getId());
        int baselineRevisionNo = product.getCurrentRevisionNo() == null ? 0 : product.getCurrentRevisionNo();
        int nextRevisionNo = latestRevision == null
                ? baselineRevisionNo + 1
                : Math.max(latestRevision.getRevisionNo(), baselineRevisionNo) + 1;
        ShowroomProductRevisionDO revision = ShowroomProductRevisionDO.builder()
                .productId(product.getId())
                .revisionNo(nextRevisionNo)
                .status(STATUS_DRAFT)
                .nameCn(nameCn)
                .nameEn(nameEn)
                .ownerCompanyId(ownerCompanyId)
                .productOwnerType(productOwnerType)
                .lifecycleStage(lifecycleStage)
                .targetMarket(nullableText(fields.get("target_market")))
                .targetMarketEn(nullableText(fields.get("target_market_en")))
                .pipelineLayout(nullableText(fields.get("pipeline_layout")))
                .pipelineLayoutEn(nullableText(fields.get("pipeline_layout_en")))
                .registrationCertificate(nullableText(fields.get("registration_certificate")))
                .registrationCertificateEn(nullableText(fields.get("registration_certificate_en")))
                .indicationContent(nullableText(fields.get("indication_content")))
                .indicationContentEn(nullableText(fields.get("indication_content_en")))
                .coreSellingPoints(nullableText(fields.get("core_selling_points")))
                .coreSellingPointsEn(nullableText(fields.get("core_selling_points_en")))
                .modelSpecification(nullableText(fields.get("model_specification")))
                .modelSpecificationEn(nullableText(fields.get("model_specification_en")))
                .coverImage(nullableText(fields.get("cover_image")))
                .clinicalEffect(nullableText(fields.get("clinical_effect")))
                .clinicalEffectEn(nullableText(fields.get("clinical_effect_en")))
                .fimStatus(nullableText(fields.get("fim_status")))
                .fimStatusEn(nullableText(fields.get("fim_status_en")))
                .build();
        productRevisionMapper.insert(assignTenant(revision));
        persistProductRevisionAttachments(product.getId(), revision.getId(), attachments);

        product.setCurrentRevisionNo(nextRevisionNo);
        product.setIncompleteFlag(incomplete);
        product.setStatus(resolveProductMasterStatus(product.getCurrentRevisionId(), incomplete));
        productMapper.updateById(product);
        return toProductRevision(revision);
    }

    @Override
    public ShowroomProductSnapshot getProduct(Long productId) {
        return toProductSnapshot(requireProduct(productId));
    }

    @Override
    public ShowroomProductRevision getProductRevision(Long revisionId) {
        return toProductRevision(requireProductRevisionDO(revisionId));
    }

    @Override
    public ShowroomProductRevision getLatestProductRevision(Long productId) {
        ShowroomProductDO product = requireProduct(productId);
        ShowroomProductRevisionDO latest = productRevisionMapper.selectLatestByProductId(product.getId());
        if (latest == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found");
        }
        return toProductRevision(latest);
    }

    @Override
    public ShowroomProductRevision getCurrentOrLatestProductRevision(Long productId) {
        ShowroomProductDO product = requireProduct(productId);
        if (product.getCurrentRevisionId() != null) {
            return getProductRevision(product.getCurrentRevisionId());
        }
        return getLatestProductRevision(productId);
    }

    @Override
    public ShowroomProductRevision requireCurrentProductRevision(Long productId) {
        ShowroomProductDO product = requireProduct(productId);
        if (product.getCurrentRevisionId() == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live product revision not found");
        }
        return getProductRevision(product.getCurrentRevisionId());
    }

    @Override
    public List<ShowroomProductSnapshot> listProducts() {
        return productMapper.selectListOrdered().stream()
                .map(this::toProductSnapshot)
                .toList();
    }

    public PageResult<ShowroomProductSnapshot> pageProducts(Integer pageNo, Integer pageSize) {
        PageParam pageParam = productPageParam(pageNo, pageSize);
        PageResult<ShowroomProductDO> page = productMapper.selectPageOrdered(pageParam);
        return new PageResult<>(page.getList().stream()
                .map(this::toProductSnapshot)
                .toList(), page.getTotal());
    }

    public Map<Long, ShowroomProductRevision> latestProductRevisions(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ShowroomProductRevision> revisionsByProductId = new LinkedHashMap<>();
        for (ShowroomProductRevisionDO revision : productRevisionMapper.selectListByProductIds(productIds)) {
            revisionsByProductId.putIfAbsent(revision.getProductId(), toProductRevision(revision));
        }
        return revisionsByProductId;
    }

    public Map<Long, ShowroomProductRevision> productRevisions(Collection<Long> revisionIds) {
        if (revisionIds == null || revisionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ShowroomProductRevision> revisionsById = new LinkedHashMap<>();
        for (ShowroomProductRevisionDO revision : productRevisionMapper.selectListByIds(revisionIds)) {
            revisionsById.put(revision.getId(), toProductRevision(revision));
        }
        return revisionsById;
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
        List<ShowroomProductDO> products = productMapper.selectListOrdered();
        if (products.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = products.stream()
                .map(ShowroomProductDO::getId)
                .toList();
        Map<Long, ShowroomProductRevisionDO> revisionsByProductId = resolveHallProductOptionRevisions(products, productIds);
        Map<Long, List<Long>> hallIdsByProductId = loadHallIdsByProductId(productIds);
        return products.stream()
                .map(product -> {
                    ShowroomProductRevisionDO revision = revisionsByProductId.get(product.getId());
                    if (revision == null) {
                        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found");
                    }
                    return new ShowroomHallProductOption(product.getId(), product.getProductMasterId(),
                            requireText(product.getProductCode(),
                                    "SHOWROOM_REQUIRED_FIELD_MISSING: hall product option code is required"),
                            revision.getNameCn() == null ? "" : revision.getNameCn(),
                            revision.getRevisionNo(), Boolean.TRUE.equals(product.getIncompleteFlag()),
                            nullableText(revision.getCoverImage()),
                            hallIdsByProductId.getOrDefault(product.getId(), List.of()));
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
        List<ShowroomAwardSnapshot> awards = listAwards();
        if (!awards.isEmpty()) {
            Map<Long, List<Long>> hallIdsByAwardId = loadHallIdsByItemId(ShowroomHallItemMapping.TYPE_AWARD,
                    awards.stream().map(ShowroomAwardSnapshot::awardId).toList());
            for (ShowroomAwardSnapshot award : awards) {
                ShowroomAwardRevision revision = getCurrentOrLatestAwardRevision(award.awardId());
                options.add(new ShowroomHallItemOption(ShowroomHallItemMapping.TYPE_AWARD, award.awardId(),
                        award.awardCode(), nullToEmpty(revision.nameCn()), nullToEmpty(revision.nameEn()),
                        revision.revisionNo(), award.incomplete(), nullToEmpty(revision.fields().get("cover_image")),
                        hallIdsByAwardId.getOrDefault(award.awardId(), List.of())));
            }
        }
        return List.copyOf(options);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        ShowroomProductDO product = requireProduct(productId);
        hallProductMapper.delete(new LambdaQueryWrapper<ShowroomHallProductDO>()
                .eq(ShowroomHallProductDO::getProductId, product.getId()));
        productRevisionAttachmentMapper.delete(new LambdaQueryWrapper<ShowroomProductRevisionAttachmentDO>()
                .eq(ShowroomProductRevisionAttachmentDO::getProductId, product.getId()));
        productRevisionMapper.delete(new LambdaQueryWrapper<ShowroomProductRevisionDO>()
                .eq(ShowroomProductRevisionDO::getProductId, product.getId()));
        productMapper.deleteById(product.getId());
        markReleaseDirty("PRODUCT_DELETED", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomProductRevision publishProductRevision(Long revisionId, Long operatorId) {
        requireNonNull(operatorId, "SHOWROOM_ROLE_BINDING_MISSING: publish operator is required");
        ShowroomProductRevisionDO revision = requireProductRevisionDO(revisionId);
        if (isProductPublishBlocked(revision.getNameCn(), revision.getNameEn())) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: product publish requires name_en");
        }
        boolean incomplete = isProductIncomplete(revision.getNameCn(), revision.getNameEn(),
                revision.getOwnerCompanyId(), revision.getProductOwnerType(), revision.getLifecycleStage());
        ShowroomProductDO product = requireProduct(revision.getProductId());
        revision.setStatus(STATUS_PUBLISHED);
        revision.setApprovedBy(operatorId);
        revision.setPublishedAt(LocalDateTime.now());
        productRevisionMapper.updateById(revision);

        product.setCurrentRevisionId(revision.getId());
        product.setCurrentRevisionNo(revision.getRevisionNo());
        product.setIncompleteFlag(incomplete);
        product.setStatus(resolveProductMasterStatus(product.getCurrentRevisionId(), incomplete));
        productMapper.updateById(product);

        appendAudits("PRODUCT", product.getId(), revision.getId(), productAuditFields(revision), operatorId, ACTION_PUBLISH);
        markReleaseDirty("PRODUCT_REVISION_PUBLISHED", operatorId);
        return toProductRevision(revision);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomAwardRevision saveAwardDraft(ShowroomAwardDraft draft) {
        requireNonNull(draft, "SHOWROOM_REQUIRED_FIELD_MISSING: award draft is required");
        ShowroomAwardDO award = draft.awardId() == null
                ? resolveOrCreateAward(requireText(draft.awardCode(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: award code is required"))
                : requireAward(draft.awardId());
        award.setAwardCode(requireText(draft.awardCode(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: award code is required"));
        boolean incomplete = isAwardIncomplete(draft.nameCn(), draft.nameEn(), draft.coverImage());
        ShowroomAwardRevisionDO latestRevision = awardRevisionMapper.selectLatestByAwardId(award.getId());
        int nextRevisionNo = latestRevision == null ? 1 : latestRevision.getRevisionNo() + 1;
        ShowroomAwardRevisionDO revision = ShowroomAwardRevisionDO.builder()
                .awardId(award.getId())
                .revisionNo(nextRevisionNo)
                .status(STATUS_DRAFT)
                .awardCodeSnapshot(award.getAwardCode())
                .nameCn(nullableText(draft.nameCn()))
                .nameEn(nullableText(draft.nameEn()))
                .descriptionZh(nullableText(draft.descriptionZh()))
                .descriptionEn(nullableText(draft.descriptionEn()))
                .issuer(nullableText(draft.issuer()))
                .awardDateText(nullableText(draft.awardDateText()))
                .coverImage(nullableText(draft.coverImage()))
                .build();
        awardRevisionMapper.insert(assignTenant(revision));

        award.setCurrentRevisionNo(nextRevisionNo);
        award.setIncompleteFlag(incomplete);
        award.setStatus(resolveProductMasterStatus(award.getCurrentRevisionId(), incomplete));
        awardMapper.updateById(award);
        return toAwardRevision(revision);
    }

    @Override
    public ShowroomAwardSnapshot getAward(Long awardId) {
        return toAwardSnapshot(requireAward(awardId));
    }

    @Override
    public ShowroomAwardRevision getAwardRevision(Long revisionId) {
        return toAwardRevision(requireAwardRevisionDO(revisionId));
    }

    @Override
    public ShowroomAwardRevision getLatestAwardRevision(Long awardId) {
        ShowroomAwardDO award = requireAward(awardId);
        ShowroomAwardRevisionDO latest = awardRevisionMapper.selectLatestByAwardId(award.getId());
        if (latest == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: award revision not found");
        }
        return toAwardRevision(latest);
    }

    @Override
    public ShowroomAwardRevision getCurrentOrLatestAwardRevision(Long awardId) {
        ShowroomAwardDO award = requireAward(awardId);
        if (award.getCurrentRevisionId() != null) {
            return getAwardRevision(award.getCurrentRevisionId());
        }
        return getLatestAwardRevision(awardId);
    }

    @Override
    public ShowroomAwardRevision requireCurrentAwardRevision(Long awardId) {
        ShowroomAwardDO award = requireAward(awardId);
        if (award.getCurrentRevisionId() == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live award revision not found");
        }
        return getAwardRevision(award.getCurrentRevisionId());
    }

    @Override
    public List<ShowroomAwardSnapshot> listAwards() {
        return awardMapper.selectListOrdered().stream()
                .map(this::toAwardSnapshot)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAward(Long awardId) {
        ShowroomAwardDO award = requireAward(awardId);
        hallItemMapper.delete(new LambdaQueryWrapper<ShowroomHallItemDO>()
                .eq(ShowroomHallItemDO::getItemType, ShowroomHallItemMapping.TYPE_AWARD)
                .eq(ShowroomHallItemDO::getItemId, award.getId()));
        awardRevisionMapper.delete(new LambdaQueryWrapper<ShowroomAwardRevisionDO>()
                .eq(ShowroomAwardRevisionDO::getAwardId, award.getId()));
        awardMapper.deleteById(award.getId());
        markReleaseDirty("AWARD_DELETED", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomAwardRevision publishAwardRevision(Long revisionId, Long operatorId) {
        requireNonNull(operatorId, "SHOWROOM_ROLE_BINDING_MISSING: publish operator is required");
        ShowroomAwardRevisionDO revision = requireAwardRevisionDO(revisionId);
        if (!hasText(revision.getNameCn())) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: award publish requires name_cn");
        }
        if (!hasText(revision.getCoverImage())) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: award publish requires cover_image");
        }
        boolean incomplete = isAwardIncomplete(revision.getNameCn(), revision.getNameEn(), revision.getCoverImage());
        ShowroomAwardDO award = requireAward(revision.getAwardId());
        revision.setStatus(STATUS_PUBLISHED);
        revision.setApprovedBy(operatorId);
        revision.setPublishedAt(LocalDateTime.now());
        awardRevisionMapper.updateById(revision);

        award.setCurrentRevisionId(revision.getId());
        award.setCurrentRevisionNo(revision.getRevisionNo());
        award.setIncompleteFlag(incomplete);
        award.setStatus(resolveProductMasterStatus(award.getCurrentRevisionId(), incomplete));
        awardMapper.updateById(award);

        ensureCompanyHonorHallAwardMappings();
        appendAudits("AWARD", award.getId(), revision.getId(), awardAuditFields(revision), operatorId, ACTION_PUBLISH);
        markReleaseDirty("AWARD_REVISION_PUBLISHED", operatorId);
        return toAwardRevision(revision);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomHall createHall(String hallCode, String name, String nameEn, String description, String descriptionEn) {
        ShowroomHallDO hall = ShowroomHallDO.builder()
                .hallCode(requireText(hallCode, "SHOWROOM_REQUIRED_FIELD_MISSING: hall code is required"))
                .name(requireText(name, "SHOWROOM_REQUIRED_FIELD_MISSING: hall name is required"))
                .nameEn(requireText(nameEn, "SHOWROOM_REQUIRED_FIELD_MISSING: hall name_en is required"))
                .description(description)
                .descriptionEn(descriptionEn)
                .displayOrder(nextHallDisplayOrder())
                .status(HALL_STATUS_ACTIVE)
                .build();
        hallMapper.insert(assignTenant(hall));
        markReleaseDirty("HALL_CREATED", null);
        return toHall(hall, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomHall updateHall(Long hallId, String name, String nameEn, String description, String descriptionEn) {
        ShowroomHallDO hall = requireHallDO(hallId);
        hall.setName(requireText(name, "SHOWROOM_REQUIRED_FIELD_MISSING: hall name is required"));
        hall.setNameEn(requireText(nameEn, "SHOWROOM_REQUIRED_FIELD_MISSING: hall name_en is required"));
        hall.setDescription(description);
        hall.setDescriptionEn(descriptionEn);
        hallMapper.updateById(hall);
        markReleaseDirty("HALL_UPDATED", null);
        return toHall(hall, loadItemMappings(List.of(hall.getId())).getOrDefault(hall.getId(), List.of()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomHall updateHallCanvasBackground(Long hallId, String canvasBackgroundImageUrl) {
        ShowroomHallDO hall = requireHallDO(hallId);
        String normalizedCanvasBackgroundImageUrl = nullableTrimmedText(canvasBackgroundImageUrl);
        hallMapper.update(null, new LambdaUpdateWrapper<ShowroomHallDO>()
                .eq(ShowroomHallDO::getId, hall.getId())
                .set(ShowroomHallDO::getCanvasBackgroundImageUrl, normalizedCanvasBackgroundImageUrl));
        hall.setCanvasBackgroundImageUrl(normalizedCanvasBackgroundImageUrl);
        return toHall(hall, loadItemMappings(List.of(hall.getId())).getOrDefault(hall.getId(), List.of()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomHall replaceHallProductMappings(Long hallId, List<ShowroomHallProductMapping> mappings) {
        requireNonNull(mappings, "SHOWROOM_REQUIRED_FIELD_MISSING: hall product mappings are required");
        List<ShowroomHallItemMapping> itemMappings = mappings.stream()
                .map(mapping -> new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_PRODUCT,
                        mapping.productId(), mapping.displayOrder(), mapping.layoutX(), mapping.layoutY(),
                        mapping.layoutWidth(), mapping.layoutHeight()))
                .toList();
        return replaceHallItemMappings(hallId, itemMappings);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomHall replaceHallCanvasLayout(Long hallId, List<ShowroomHallProductMapping> mappings) {
        requireNonNull(mappings, "SHOWROOM_REQUIRED_FIELD_MISSING: hall product mappings are required");
        List<ShowroomHallItemMapping> itemMappings = mappings.stream()
                .map(mapping -> new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_PRODUCT,
                        mapping.productId(), mapping.displayOrder(), mapping.layoutX(), mapping.layoutY(),
                        mapping.layoutWidth(), mapping.layoutHeight()))
                .toList();
        return replaceHallItemCanvasLayout(hallId, itemMappings);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomHall replaceHallItemMappings(Long hallId, List<ShowroomHallItemMapping> mappings) {
        ShowroomHallDO hall = requireHallDO(hallId);
        List<ShowroomHallItemMapping> orderedMappings = normalizeHallItemMappings(mappings);
        requireHallItemPlacementAllowed(hall, orderedMappings);
        replaceHallItemMappingRows(hall, orderedMappings);
        markReleaseDirty("HALL_ITEM_MAPPINGS_REPLACED", null);
        return toHall(hall, orderedMappings);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomHall replaceHallItemCanvasLayout(Long hallId, List<ShowroomHallItemMapping> mappings) {
        ShowroomHallDO hall = requireHallDO(hallId);
        List<ShowroomHallItemMapping> orderedMappings = normalizeHallItemMappings(mappings);
        requireHallItemPlacementAllowed(hall, orderedMappings);
        List<ShowroomHallItemMapping> resolvedMappings =
                requireItemCanvasLayout(orderedMappings);
        replaceHallItemMappingRows(hall, resolvedMappings);
        markReleaseDirty("HALL_ITEM_MAPPINGS_REPLACED", null);
        return toHall(hall, resolvedMappings);
    }

    private List<ShowroomHallItemMapping> normalizeHallItemMappings(List<ShowroomHallItemMapping> mappings) {
        requireNonNull(mappings, "SHOWROOM_REQUIRED_FIELD_MISSING: hall item mappings are required");
        if (mappings.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: hall must keep at least one item mapping");
        }
        LinkedHashSet<String> itemKeys = new LinkedHashSet<>();
        LinkedHashSet<Integer> displayOrders = new LinkedHashSet<>();
        List<ShowroomHallItemMapping> orderedMappings = new ArrayList<>();
        for (ShowroomHallItemMapping mapping : mappings) {
            requireNonNull(mapping.itemId(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall item_id is required");
            requireNonNull(mapping.displayOrder(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall display_order is required");
            String itemKey = mapping.itemType() + ":" + mapping.itemId();
            if (!itemKeys.add(itemKey)) {
                throw new IllegalStateException("SHOWROOM_DUPLICATE_ITEM: duplicate hall item mapping is invalid");
            }
            if (!displayOrders.add(mapping.displayOrder())) {
                throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: duplicate hall display_order is invalid");
            }
            if (ShowroomHallItemMapping.TYPE_PRODUCT.equals(mapping.itemType())) {
                requireProduct(mapping.itemId());
            } else {
                requireAward(mapping.itemId());
            }
            orderedMappings.add(mapping);
        }
        orderedMappings.sort(Comparator.comparing(ShowroomHallItemMapping::displayOrder));
        return orderedMappings;
    }

    private void requireHallItemPlacementAllowed(ShowroomHallDO hall, List<ShowroomHallItemMapping> orderedMappings) {
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
        List<ShowroomHallDO> honorHalls = ensureCompanyHonorHalls();
        List<ShowroomAwardDO> publishedAwards = awardMapper.selectCurrentListOrdered();
        Map<String, List<ShowroomHallItemMapping>> mappingsByHallCode =
                splitCompanyHonorAwardMappings(publishedAwards);
        hallItemMapper.deleteByItemTypeOutsideHallsForce(currentTenantId(), ShowroomHallItemMapping.TYPE_AWARD,
                honorHalls.stream().map(ShowroomHallDO::getId).toList());
        for (ShowroomHallDO honorHall : honorHalls) {
            List<ShowroomHallItemMapping> mappings = mappingsByHallCode.getOrDefault(honorHall.getHallCode(),
                    List.of());
            replaceHallItemMappingRows(honorHall, mappings.isEmpty() ? List.of() : withDefaultItemLayout(mappings));
        }
    }

    private List<ShowroomHallDO> ensureCompanyHonorHalls() {
        retireLegacyCompanyHonorHall();
        List<ShowroomHallDO> result = new ArrayList<>();
        for (ShowroomEnterpriseHonorHalls.Definition definition : ShowroomEnterpriseHonorHalls.DEFINITIONS) {
            ShowroomHallDO existing = hallMapper.selectByHallCode(definition.hallCode());
            if (existing != null) {
                existing.setName(definition.name());
                existing.setNameEn(definition.nameEn());
                existing.setDescription(definition.description());
                existing.setDescriptionEn(definition.descriptionEn());
                existing.setDisplayOrder(definition.displayOrder());
                existing.setStatus(HALL_STATUS_ACTIVE);
                hallMapper.updateById(existing);
                result.add(existing);
                continue;
            }
            ShowroomHallDO hall = ShowroomHallDO.builder()
                    .hallCode(definition.hallCode())
                    .name(definition.name())
                    .nameEn(definition.nameEn())
                    .description(definition.description())
                    .descriptionEn(definition.descriptionEn())
                    .displayOrder(definition.displayOrder())
                    .status(HALL_STATUS_ACTIVE)
                    .build();
            hallMapper.insert(assignTenant(hall));
            result.add(hall);
        }
        return result;
    }

    private void retireLegacyCompanyHonorHall() {
        ShowroomHallDO legacy = hallMapper.selectByHallCode(ShowroomEnterpriseHonorHalls.LEGACY_HALL_CODE);
        if (legacy == null) {
            return;
        }
        hallProductMapper.deleteByHallIdForce(currentTenantId(), legacy.getId());
        hallItemMapper.deleteByHallIdForce(currentTenantId(), legacy.getId());
        hallMapper.deleteById(legacy.getId());
    }

    private static Map<String, List<ShowroomHallItemMapping>> splitCompanyHonorAwardMappings(
            List<ShowroomAwardDO> publishedAwards) {
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
                    publishedAwards.get(index).getId(), mappings.size() + 1));
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

    private static boolean isEnterpriseHonorHall(ShowroomHallDO hall) {
        return hall != null && ShowroomEnterpriseHonorHalls.isEnterpriseHonorHallCode(hall.getHallCode());
    }

    private void replaceHallItemMappingRows(ShowroomHallDO hall, List<ShowroomHallItemMapping> orderedMappings) {
        // hall-product relation has a unique key on (hall_id, product_id), so replace must
        // physically clear the old rows instead of relying on BaseDO logical delete.
        hallProductMapper.deleteByHallIdForce(currentTenantId(), hall.getId());
        hallItemMapper.deleteByHallIdForce(currentTenantId(), hall.getId());
        for (ShowroomHallItemMapping mapping : orderedMappings) {
            hallItemMapper.insert(assignTenant(ShowroomHallItemDO.builder()
                    .hallId(hall.getId())
                    .itemType(mapping.itemType())
                    .itemId(mapping.itemId())
                    .displayOrder(mapping.displayOrder())
                    .layoutX(mapping.layoutX())
                    .layoutY(mapping.layoutY())
                    .layoutWidth(mapping.layoutWidth())
                    .layoutHeight(mapping.layoutHeight())
                    .build()));
            if (ShowroomHallItemMapping.TYPE_PRODUCT.equals(mapping.itemType())) {
                hallProductMapper.insert(assignTenant(ShowroomHallProductDO.builder()
                        .hallId(hall.getId())
                        .productId(mapping.itemId())
                        .displayOrder(mapping.displayOrder())
                        .layoutX(mapping.layoutX())
                        .layoutY(mapping.layoutY())
                        .layoutWidth(mapping.layoutWidth())
                        .layoutHeight(mapping.layoutHeight())
                        .build()));
            }
        }
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public ShowroomHall getHall(Long hallId) {
        ShowroomHallDO hall = requireHallDO(hallId);
        return toHall(hall, loadItemMappings(List.of(hall.getId())).getOrDefault(hall.getId(), List.of()));
    }

    @Override
    public List<ShowroomHall> listHalls() {
        List<ShowroomHallDO> halls = hallMapper.selectListOrdered();
        if (halls.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ShowroomHallItemMapping>> mappingsByHallId = loadItemMappings(halls.stream()
                .map(ShowroomHallDO::getId)
                .toList());
        return halls.stream()
                .map(hall -> toHall(hall, mappingsByHallId.getOrDefault(hall.getId(), List.of())))
                .toList();
    }

    public List<ShowroomHall> listHallsForReleaseSnapshot() {
        List<ShowroomHallDO> halls = hallMapper.selectListOrdered();
        if (halls.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ShowroomHallItemMapping>> mappingsByHallId = loadItemMappings(halls.stream()
                .map(ShowroomHallDO::getId)
                .toList());
        return halls.stream()
                .map(hall -> toHall(hall, mappingsByHallId.getOrDefault(hall.getId(), List.of())))
                .toList();
    }

    @Override
    public List<ShowroomHall> listHalls(String keyword, Integer pageNo, Integer pageSize) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return page(listHalls().stream()
                .filter(hall -> matchesHall(hall, normalizedKeyword))
                .toList(), pageNo, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHall(Long hallId) {
        ShowroomHallDO hall = requireHallDO(hallId);
        hallProductMapper.delete(new LambdaQueryWrapper<ShowroomHallProductDO>()
                .eq(ShowroomHallProductDO::getHallId, hall.getId()));
        hallMapper.deleteById(hall.getId());
        markReleaseDirty("HALL_DELETED", null);
    }

    @Override
    public List<ShowroomVersionAudit> versionAudits(String targetType, Long targetId) {
        return versionAuditMapper.selectListByTarget(targetType, targetId).stream()
                .map(this::toVersionAudit)
                .toList();
    }

    private ShowroomCompanyDO resolveOrCreateCompany(String companyType, String displayName, String displayNameEn) {
        String normalizedCompanyType = requireText(companyType,
                "SHOWROOM_REQUIRED_FIELD_MISSING: company type is required");
        String normalizedDisplayName = requireText(displayName,
                "SHOWROOM_REQUIRED_FIELD_MISSING: company display name is required");
        String normalizedDisplayNameEn = requireText(displayNameEn,
                "SHOWROOM_REQUIRED_FIELD_MISSING: company display name_en is required");
        if ("MAIN".equals(normalizedCompanyType)) {
            ShowroomCompanyDO mainCompany = companyMapper.selectMainCompany();
            if (mainCompany != null) {
                return mainCompany;
            }
        }
        String companyCode = generateCompanyCode(normalizedDisplayName);
        ShowroomCompanyDO existing = companyMapper.selectOne(new LambdaQueryWrapper<ShowroomCompanyDO>()
                .eq(ShowroomCompanyDO::getTenantId, currentTenantId())
                .eq(ShowroomCompanyDO::getCompanyCode, companyCode)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        ShowroomCompanyDO company = ShowroomCompanyDO.builder()
                .companyCode(companyCode)
                .displayName(normalizedDisplayName)
                .displayNameEn(normalizedDisplayNameEn)
                .companyType(normalizedCompanyType)
                .currentRevisionNo(0)
                .currentRevisionId(null)
                .incompleteFlag(Boolean.FALSE)
                .status(MASTER_STATUS_DRAFT_ONLY)
                .build();
        companyMapper.insert(assignTenant(company));
        return company;
    }

    private ShowroomProductDO createProduct(Long productMasterId, String productCode) {
        ShowroomProductDO deletedProduct = productMapper.selectAnyByTenantIdAndProductCode(currentTenantId(), productCode);
        if (deletedProduct != null) {
            return reviveDeletedProduct(deletedProduct, productMasterId, productCode);
        }
        ShowroomProductDO product = ShowroomProductDO.builder()
                .productMasterId(productMasterId)
                .productCode(productCode)
                .currentRevisionNo(0)
                .currentRevisionId(null)
                .incompleteFlag(Boolean.TRUE)
                .status(MASTER_STATUS_DRAFT_ONLY)
                .build();
        productMapper.insert(assignTenant(product));
        return product;
    }

    private ShowroomProductDO reviveDeletedProduct(ShowroomProductDO deletedProduct, Long productMasterId, String productCode) {
        if (!Boolean.TRUE.equals(deletedProduct.getDeleted())) {
            return deletedProduct;
        }
        deletedProduct.setDeleted(Boolean.FALSE);
        deletedProduct.setProductMasterId(productMasterId);
        deletedProduct.setProductCode(productCode);
        deletedProduct.setCurrentRevisionId(null);
        deletedProduct.setCurrentRevisionNo(deletedProduct.getCurrentRevisionNo() == null
                ? 0
                : deletedProduct.getCurrentRevisionNo());
        deletedProduct.setIncompleteFlag(Boolean.TRUE);
        deletedProduct.setStatus(MASTER_STATUS_DRAFT_ONLY);
        productMapper.reviveById(assignTenant(deletedProduct));
        return deletedProduct;
    }

    private ResolvedProductMaster resolveProductMaster(Long productMasterId) {
        if (productMasterId == null) {
            return null;
        }
        if (productApi == null) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: mdm product api is required");
        }
        MdmProductRespDTO product = productApi.getProduct(productMasterId);
        if (product == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product master not found");
        }
        if (!MdmProductStatusConstants.ENABLE.equals(product.getStatus())) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_MASTER_DISABLED: product master is disabled");
        }
        String productCode = requireText(product.getProductCode(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: product master code is required");
        String nameCn = requireText(product.getNameCn(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: product master name_cn is required");
        return new ResolvedProductMaster(product.getId(), productCode, nameCn, nullableText(product.getNameEn()));
    }

    private ShowroomCompanyDO requireCompany(Long companyId) {
        ShowroomCompanyDO company = companyMapper.selectById(requireId(companyId,
                "SHOWROOM_TARGET_NOT_FOUND: company id is required"));
        if (company == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company not found");
        }
        requireTenant(company.getTenantId(), "SHOWROOM_TARGET_NOT_FOUND: company not found");
        return company;
    }

    private ShowroomCompanyRevisionDO requireCompanyRevisionDO(Long revisionId) {
        ShowroomCompanyRevisionDO revision = companyRevisionMapper.selectById(requireId(revisionId,
                "SHOWROOM_TARGET_NOT_FOUND: company revision id is required"));
        if (revision == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company revision not found");
        }
        requireTenant(revision.getTenantId(), "SHOWROOM_TARGET_NOT_FOUND: company revision not found");
        return revision;
    }

    private ShowroomProductDO requireProduct(Long productId) {
        ShowroomProductDO product = productMapper.selectById(requireId(productId,
                "SHOWROOM_TARGET_NOT_FOUND: product id is required"));
        if (product == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product not found");
        }
        requireTenant(product.getTenantId(), "SHOWROOM_TARGET_NOT_FOUND: product not found");
        return product;
    }

    private ShowroomProductRevisionDO requireProductRevisionDO(Long revisionId) {
        ShowroomProductRevisionDO revision = productRevisionMapper.selectById(requireId(revisionId,
                "SHOWROOM_TARGET_NOT_FOUND: product revision id is required"));
        if (revision == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found");
        }
        requireTenant(revision.getTenantId(), "SHOWROOM_TARGET_NOT_FOUND: product revision not found");
        return revision;
    }

    private ShowroomAwardDO requireAward(Long awardId) {
        ShowroomAwardDO award = awardMapper.selectById(requireId(awardId,
                "SHOWROOM_TARGET_NOT_FOUND: award id is required"));
        if (award == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: award not found");
        }
        requireTenant(award.getTenantId(), "SHOWROOM_TARGET_NOT_FOUND: award not found");
        return award;
    }

    private ShowroomAwardRevisionDO requireAwardRevisionDO(Long revisionId) {
        ShowroomAwardRevisionDO revision = awardRevisionMapper.selectById(requireId(revisionId,
                "SHOWROOM_TARGET_NOT_FOUND: award revision id is required"));
        if (revision == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: award revision not found");
        }
        requireTenant(revision.getTenantId(), "SHOWROOM_TARGET_NOT_FOUND: award revision not found");
        return revision;
    }

    private ShowroomAwardDO resolveOrCreateAward(String awardCode) {
        ShowroomAwardDO existing = awardMapper.selectByAwardCode(awardCode);
        if (existing != null) {
            return existing;
        }
        ShowroomAwardDO award = ShowroomAwardDO.builder()
                .awardCode(awardCode)
                .currentRevisionNo(0)
                .currentRevisionId(null)
                .incompleteFlag(Boolean.TRUE)
                .status(MASTER_STATUS_DRAFT_ONLY)
                .build();
        awardMapper.insert(assignTenant(award));
        return award;
    }

    private ShowroomHallDO requireHallDO(Long hallId) {
        ShowroomHallDO hall = hallMapper.selectById(requireId(hallId, "SHOWROOM_TARGET_NOT_FOUND: hall id is required"));
        if (hall == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: hall not found");
        }
        requireTenant(hall.getTenantId(), "SHOWROOM_TARGET_NOT_FOUND: hall not found");
        return hall;
    }

    private Long currentTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private void requireTenant(Long rowTenantId, String notFoundMessage) {
        if (!currentTenantId().equals(rowTenantId)) {
            throw new IllegalStateException(notFoundMessage);
        }
    }

    private Map<Long, List<ShowroomHallProductMapping>> loadMappings(Collection<Long> hallIds) {
        return loadMappings(hallIds, true);
    }

    private Map<Long, List<ShowroomHallProductMapping>> loadMappings(Collection<Long> hallIds,
                                                                     boolean applyDefaultLayoutIfMissing) {
        List<ShowroomHallProductDO> rows = hallProductMapper.selectListByHallIds(hallIds);
        Map<Long, List<ShowroomHallProductMapping>> result = new LinkedHashMap<>();
        for (ShowroomHallProductDO row : rows) {
            result.computeIfAbsent(row.getHallId(), ignored -> new ArrayList<>())
                    .add(new ShowroomHallProductMapping(row.getProductId(), row.getDisplayOrder(),
                            row.getLayoutX(), row.getLayoutY(), row.getLayoutWidth(), row.getLayoutHeight()));
        }
        result.replaceAll((hallId, mappings) -> {
            List<ShowroomHallProductMapping> orderedMappings = mappings.stream()
                    .sorted(Comparator.comparing(ShowroomHallProductMapping::displayOrder))
                    .toList();
            if (applyDefaultLayoutIfMissing) {
                return ShowroomHallCanvasLayoutPolicy.withDefaultLayoutIfMissing(orderedMappings);
            }
            return orderedMappings;
        });
        return result;
    }

    private Map<Long, List<ShowroomHallItemMapping>> loadItemMappings(Collection<Long> hallIds) {
        List<ShowroomHallItemDO> rows = hallItemMapper.selectListByHallIds(hallIds);
        Map<Long, List<ShowroomHallItemMapping>> result = new LinkedHashMap<>();
        for (ShowroomHallItemDO row : rows) {
            result.computeIfAbsent(row.getHallId(), ignored -> new ArrayList<>())
                    .add(new ShowroomHallItemMapping(row.getItemType(), row.getItemId(), row.getDisplayOrder(),
                            row.getLayoutX(), row.getLayoutY(), row.getLayoutWidth(), row.getLayoutHeight()));
        }
        result.replaceAll((hallId, mappings) -> {
            List<ShowroomHallItemMapping> orderedMappings = mappings.stream()
                    .sorted(Comparator.comparing(ShowroomHallItemMapping::displayOrder))
                    .toList();
            return orderedMappings;
        });
        return result;
    }

    private Map<Long, ShowroomProductRevisionDO> resolveHallProductOptionRevisions(List<ShowroomProductDO> products,
                                                                                   Collection<Long> productIds) {
        Map<Long, ShowroomProductRevisionDO> result = new LinkedHashMap<>();
        Map<Long, ShowroomProductRevisionDO> revisionsById = new LinkedHashMap<>();
        for (ShowroomProductRevisionDO revision : productRevisionMapper.selectListByProductIds(productIds)) {
            revisionsById.put(revision.getId(), revision);
        }
        for (ShowroomProductDO product : products) {
            if (product.getCurrentRevisionId() != null) {
                ShowroomProductRevisionDO currentRevision = revisionsById.get(product.getCurrentRevisionId());
                if (currentRevision == null) {
                    throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: current product revision not found");
                }
                result.put(product.getId(), currentRevision);
            }
        }
        for (ShowroomProductRevisionDO revision : revisionsById.values()) {
            result.putIfAbsent(revision.getProductId(), revision);
        }
        return result;
    }

    private Map<Long, List<Long>> loadHallIdsByProductId(Collection<Long> productIds) {
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (ShowroomHallProductDO row : hallProductMapper.selectListByProductIds(productIds)) {
            result.computeIfAbsent(row.getProductId(), ignored -> new ArrayList<>())
                    .add(requireId(row.getHallId(),
                            "SHOWROOM_REQUIRED_FIELD_MISSING: hall product option hall id is required"));
        }
        return result;
    }

    private Map<Long, List<Long>> loadHallIdsByItemId(String itemType, Collection<Long> itemIds) {
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (ShowroomHallItemDO row : hallItemMapper.selectListByItems(itemType, itemIds)) {
            result.computeIfAbsent(row.getItemId(), ignored -> new ArrayList<>())
                    .add(requireId(row.getHallId(),
                            "SHOWROOM_REQUIRED_FIELD_MISSING: hall item option hall id is required"));
        }
        return result;
    }

    private int nextHallDisplayOrder() {
        return hallMapper.selectListOrdered().stream()
                .map(ShowroomHallDO::getDisplayOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void appendAudits(String targetType, Long targetId, Long revisionId, Map<String, String> fields,
                              Long operatorId, String operatorAction) {
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            versionAuditMapper.insert(assignTenant(ShowroomVersionAuditDO.builder()
                    .targetType(targetType)
                    .targetId(targetId)
                    .revisionId(revisionId)
                    .fieldCode(entry.getKey())
                    .oldValueJson(null)
                    .newValueJson(jsonAuditValue(entry.getValue()))
                    .operatorId(operatorId)
                    .operatorAction(operatorAction)
                    .createdAt(LocalDateTime.now())
                    .build()));
        }
    }

    private <T extends TenantBaseDO> T assignTenant(T entity) {
        entity.setTenantId(currentTenantId());
        return entity;
    }

    private static String jsonAuditValue(String value) {
        return JsonUtils.toJsonString(Map.of("value", value == null ? "" : value));
    }

    private ShowroomCompanySnapshot toCompanySnapshot(ShowroomCompanyDO company) {
        return new ShowroomCompanySnapshot(company.getId(), company.getCompanyType(), company.getDisplayName(),
                company.getDisplayNameEn(),
                Optional.ofNullable(company.getCurrentRevisionId()), company.getCurrentRevisionId() != null);
    }

    private ShowroomCompanyRevision toCompanyRevision(ShowroomCompanyRevisionDO revision) {
        return new ShowroomCompanyRevision(revision.getCompanyId(), revision.getId(), revision.getRevisionNo(),
                revision.getStatus(), toCompanyFieldMap(revision));
    }

    private ShowroomProductSnapshot toProductSnapshot(ShowroomProductDO product) {
        return new ShowroomProductSnapshot(product.getId(), product.getProductMasterId(), product.getProductCode(),
                nullableText(product.getLegacyProductCode()), Optional.ofNullable(product.getCurrentRevisionId()),
                Boolean.TRUE.equals(product.getIncompleteFlag()), product.getCurrentRevisionId() != null);
    }

    private ShowroomAwardSnapshot toAwardSnapshot(ShowroomAwardDO award) {
        return new ShowroomAwardSnapshot(award.getId(), award.getAwardCode(),
                Optional.ofNullable(award.getCurrentRevisionId()), Boolean.TRUE.equals(award.getIncompleteFlag()),
                award.getCurrentRevisionId() != null);
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

    private static PageParam productPageParam(Integer pageNo, Integer pageSize) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo == null || pageNo < 1 ? 1 : pageNo);
        pageParam.setPageSize(pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 20));
        return pageParam;
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private static boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private void markReleaseDirty(String reason, Long operatorId) {
        ShowroomReleaseAutoPublishService autoPublishService = releaseAutoPublishService != null
                ? releaseAutoPublishService
                : releaseAutoPublishServiceProvider == null ? null : releaseAutoPublishServiceProvider.getIfAvailable();
        if (autoPublishService == null) {
            return;
        }
        autoPublishService.markDirty(reason, operatorId);
    }

    private ShowroomProductRevision toProductRevision(ShowroomProductRevisionDO revision) {
        return new ShowroomProductRevision(revision.getProductId(), revision.getId(), revision.getRevisionNo(),
                revision.getStatus(), revision.getNameCn(), revision.getNameEn(),
                isProductIncomplete(revision.getNameCn(), revision.getNameEn(), revision.getOwnerCompanyId(),
                        revision.getProductOwnerType(), revision.getLifecycleStage()),
                toProductFieldMap(revision), toProductAttachments(revision.getId()));
    }

    private ShowroomAwardRevision toAwardRevision(ShowroomAwardRevisionDO revision) {
        return new ShowroomAwardRevision(revision.getAwardId(), revision.getId(), revision.getRevisionNo(),
                revision.getStatus(), revision.getAwardCodeSnapshot(), revision.getNameCn(), revision.getNameEn(),
                isAwardIncomplete(revision.getNameCn(), revision.getNameEn(), revision.getCoverImage()),
                toAwardFieldMap(revision));
    }

    private void persistProductRevisionAttachments(Long productId, Long productRevisionId,
                                                   List<ShowroomProductAttachment> attachments) {
        int index = 1;
        for (ShowroomProductAttachment attachment : attachments) {
            ShowroomProductRevisionAttachmentDO record = ShowroomProductRevisionAttachmentDO.builder()
                    .productId(productId)
                    .productRevisionId(productRevisionId)
                    .assetType(attachment.assetType())
                    .fileId(attachment.fileId())
                    .originalName(attachment.originalName())
                    .mimeType(attachment.mimeType())
                    .fileSize(attachment.fileSize())
                    .displayOrder(index++)
                    .build();
            productRevisionAttachmentMapper.insert(assignTenant(record));
        }
    }

    private List<ShowroomProductAttachment> toProductAttachments(Long revisionId) {
        return productRevisionAttachmentMapper.selectByRevisionId(revisionId).stream()
                .map(this::toProductAttachment)
                .toList();
    }

    private ShowroomProductAttachment toProductAttachment(ShowroomProductRevisionAttachmentDO attachment) {
        return new ShowroomProductAttachment(attachment.getId(), attachment.getProductId(),
                attachment.getProductRevisionId(), attachment.getAssetType(), attachment.getFileId(),
                attachment.getOriginalName(), attachment.getMimeType(), attachment.getFileSize(),
                attachment.getDisplayOrder() == null ? 0 : attachment.getDisplayOrder());
    }

    private ShowroomHall toHall(ShowroomHallDO hall, List<ShowroomHallItemMapping> itemMappings) {
        List<ShowroomHallProductMapping> productMappings = itemMappings == null ? List.of() : itemMappings.stream()
                .filter(mapping -> ShowroomHallItemMapping.TYPE_PRODUCT.equals(mapping.itemType()))
                .map(ShowroomHallItemMapping::asProductMapping)
                .toList();
        return new ShowroomHall(hall.getId(), hall.getHallCode(), hall.getName(), hall.getNameEn(),
                hall.getDescription(), hall.getDescriptionEn(), hall.getCanvasBackgroundImageUrl(), productMappings,
                itemMappings == null ? List.of() : List.copyOf(itemMappings));
    }

    private ShowroomVersionAudit toVersionAudit(ShowroomVersionAuditDO audit) {
        return new ShowroomVersionAudit(audit.getTargetType(), audit.getTargetId(), audit.getRevisionId(),
                audit.getFieldCode(), audit.getOldValueJson(), audit.getNewValueJson(), audit.getOperatorId(),
                audit.getOperatorAction(), audit.getCreatedAt().toInstant(ZoneOffset.UTC));
    }

    private static Map<String, String> toCompanyFieldMap(ShowroomCompanyRevisionDO revision) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        putIfNonNull(fields, "development_history", revision.getDevelopmentHistory());
        putIfNonNull(fields, "development_history_en", revision.getDevelopmentHistoryEn());
        putIfNonNull(fields, "park_introduction", revision.getParkIntroduction());
        putIfNonNull(fields, "park_introduction_en", revision.getParkIntroductionEn());
        putIfNonNull(fields, "incubation_platform", revision.getIncubationPlatform());
        putIfNonNull(fields, "incubation_platform_en", revision.getIncubationPlatformEn());
        putIfNonNull(fields, "subsidiary_overview", revision.getSubsidiaryOverview());
        putIfNonNull(fields, "subsidiary_overview_en", revision.getSubsidiaryOverviewEn());
        putIfNonNull(fields, "stock_info", revision.getStockInfo());
        putIfNonNull(fields, "stock_info_en", revision.getStockInfoEn());
        putIfNonNull(fields, "cover_image", revision.getCoverImage());
        putIfNonNull(fields, "core_manufacturing_capability", revision.getCoreManufacturingCapability());
        putIfNonNull(fields, "core_manufacturing_capability_en", revision.getCoreManufacturingCapabilityEn());
        putIfNonNull(fields, "honors_awards", revision.getHonorsAwards());
        putIfNonNull(fields, "honors_awards_en", revision.getHonorsAwardsEn());
        return Collections.unmodifiableMap(fields);
    }

    private static Map<String, String> toProductFieldMap(ShowroomProductRevisionDO revision) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        putIfNonNull(fields, "owner_company_id", revision.getOwnerCompanyId() == null ? null
                : String.valueOf(revision.getOwnerCompanyId()));
        putIfNonNull(fields, "product_owner_type", revision.getProductOwnerType());
        putIfNonNull(fields, "lifecycle_stage", revision.getLifecycleStage());
        putIfNonNull(fields, "target_market", revision.getTargetMarket());
        putIfNonNull(fields, "target_market_en", revision.getTargetMarketEn());
        putIfNonNull(fields, "pipeline_layout", revision.getPipelineLayout());
        putIfNonNull(fields, "pipeline_layout_en", revision.getPipelineLayoutEn());
        putIfNonNull(fields, "registration_certificate", revision.getRegistrationCertificate());
        putIfNonNull(fields, "registration_certificate_en", revision.getRegistrationCertificateEn());
        putIfNonNull(fields, "indication_content", revision.getIndicationContent());
        putIfNonNull(fields, "indication_content_en", revision.getIndicationContentEn());
        putIfNonNull(fields, "core_selling_points", revision.getCoreSellingPoints());
        putIfNonNull(fields, "core_selling_points_en", revision.getCoreSellingPointsEn());
        putIfNonNull(fields, "model_specification", revision.getModelSpecification());
        putIfNonNull(fields, "model_specification_en", revision.getModelSpecificationEn());
        putIfNonNull(fields, "cover_image", revision.getCoverImage());
        putIfNonNull(fields, "clinical_effect", revision.getClinicalEffect());
        putIfNonNull(fields, "clinical_effect_en", revision.getClinicalEffectEn());
        putIfNonNull(fields, "fim_status", revision.getFimStatus());
        putIfNonNull(fields, "fim_status_en", revision.getFimStatusEn());
        return Collections.unmodifiableMap(fields);
    }

    private static Map<String, String> toAwardFieldMap(ShowroomAwardRevisionDO revision) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        putIfNonNull(fields, "description_zh", revision.getDescriptionZh());
        putIfNonNull(fields, "description_en", revision.getDescriptionEn());
        putIfNonNull(fields, "issuer", revision.getIssuer());
        putIfNonNull(fields, "award_date_text", revision.getAwardDateText());
        putIfNonNull(fields, "cover_image", revision.getCoverImage());
        return Collections.unmodifiableMap(fields);
    }

    private static Map<String, String> productAuditFields(ShowroomProductRevisionDO revision) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        putIfNonNull(values, "name_cn", revision.getNameCn());
        putIfNonNull(values, "name_en", revision.getNameEn());
        values.putAll(toProductFieldMap(revision));
        return values;
    }

    private static Map<String, String> awardAuditFields(ShowroomAwardRevisionDO revision) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        putIfNonNull(values, "award_code", revision.getAwardCodeSnapshot());
        putIfNonNull(values, "name_cn", revision.getNameCn());
        putIfNonNull(values, "name_en", revision.getNameEn());
        values.putAll(toAwardFieldMap(revision));
        return values;
    }

    private static Map<String, String> copyFields(Map<String, String> fields) {
        requireNonNull(fields, "SHOWROOM_REQUIRED_FIELD_MISSING: draft fields are required");
        LinkedHashMap<String, String> copied = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            copied.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(copied);
    }

    private static void putIfNonNull(Map<String, String> target, String key, String value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static Long parseLongField(Map<String, String> fields, String fieldCode) {
        String value = fields.get(fieldCode);
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: invalid numeric field " + fieldCode);
        }
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

    private static boolean isProductIncomplete(String nameCn, String nameEn, Long ownerCompanyId,
                                               String productOwnerType, String lifecycleStage) {
        for (String requiredField : ShowroomPublishContract.requiredProductCompletenessFields()) {
            if ("name_cn".equals(requiredField) && !hasText(nameCn)) {
                return true;
            }
            if ("name_en".equals(requiredField) && !hasText(nameEn)) {
                return true;
            }
            if ("owner_company_id".equals(requiredField) && ownerCompanyId == null) {
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

    private static String resolveProductMasterStatus(Long currentRevisionId, boolean incomplete) {
        if (currentRevisionId == null) {
            return MASTER_STATUS_DRAFT_ONLY;
        }
        return incomplete ? MASTER_STATUS_INCOMPLETE : MASTER_STATUS_LIVE;
    }

    private static String generateCompanyCode(String displayName) {
        return "OWNER_" + DigestUtil.md5Hex(displayName).substring(0, 12).toUpperCase();
    }

    private static Long requireId(Long value, String message) {
        requireNonNull(value, message);
        return value;
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

    private static String nullableText(String value) {
        return hasText(value) ? value : null;
    }

    private static String nullableTrimmedText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ResolvedProductMaster(Long id, String productCode, String nameCn, String nameEn) {
    }

}
