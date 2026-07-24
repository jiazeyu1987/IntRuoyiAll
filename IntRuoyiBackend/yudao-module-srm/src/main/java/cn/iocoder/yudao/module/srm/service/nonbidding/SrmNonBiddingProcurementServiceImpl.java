package cn.iocoder.yudao.module.srm.service.nonbidding;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo.*;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierEligibilityRespVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.nonbidding.*;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.*;
import cn.iocoder.yudao.module.srm.dal.mysql.nonbidding.*;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.*;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmSourcingProjectStatusEnum;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SrmNonBiddingProcurementServiceImpl implements SrmNonBiddingProcurementService {

    private static final String QUOTE_STATUS_SUBMITTED = "SUBMITTED";
    private static final String QUOTE_MODE_INVITE = "INVITE";
    private static final String QUOTE_MODE_PUBLIC = "PUBLIC";
    private static final int MONEY_SCALE = 2;

    @Resource
    private SrmSupplierAccessRiskService supplierAccessRiskService;
    @Resource
    private SrmSourcingProjectMapper sourcingProjectMapper;
    @Resource
    private SrmSourcingProjectLineMapper sourcingProjectLineMapper;
    @Resource
    private SrmNonBiddingSupplierScopeMapper supplierScopeMapper;
    @Resource
    private SrmNonBiddingQuoteMapper quoteMapper;
    @Resource
    private SrmNonBiddingQuoteLineMapper quoteLineMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmNonBiddingProjectRespVO publishProject(SrmNonBiddingPublishReqVO publishReqVO) {
        SrmSourcingProjectDO project = validateDraftNonBiddingProject(publishReqVO.getProjectId());
        validatePublishRequest(publishReqVO);

        List<SrmSupplierEligibilityRespVO> eligibleSuppliers = new ArrayList<>();
        for (Long supplierId : distinctSupplierIds(publishReqVO.getSupplierIds())) {
            SrmSupplierEligibilityRespVO eligibility = supplierAccessRiskService.checkSupplierEligibility(supplierId);
            if (!Boolean.TRUE.equals(eligibility.getEligible())) {
                throw exception(SUPPLIER_ELIGIBILITY_BLOCKED, eligibility.getBlockedReason());
            }
            eligibleSuppliers.add(eligibility);
        }

        LocalDateTime now = LocalDateTime.now();
        project.setQuoteMode(normalizeQuoteMode(publishReqVO.getQuoteMode()));
        project.setQuoteStartTime(publishReqVO.getQuoteStartTime());
        project.setQuoteEndTime(publishReqVO.getQuoteEndTime());
        project.setPublishAttachmentUrl(publishReqVO.getAttachmentUrl());
        project.setPublishedTime(now);
        project.setProjectStatus(SrmSourcingProjectStatusEnum.PUBLISHED.getStatus());
        sourcingProjectMapper.updateById(project);

        for (SrmSupplierEligibilityRespVO supplier : eligibleSuppliers) {
            SrmNonBiddingSupplierScopeDO scope = SrmNonBiddingSupplierScopeDO.builder()
                    .projectId(project.getId())
                    .supplierId(supplier.getSupplierId())
                    .supplierName(supplier.getSupplierName())
                    .build();
            scope.setTenantId(getRequiredTenantId());
            supplierScopeMapper.insert(scope);
        }
        return buildProjectResp(project.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmNonBiddingProjectRespVO submitQuote(SrmNonBiddingQuoteReqVO quoteReqVO) {
        SrmSourcingProjectDO project = validatePublishedNonBiddingProject(quoteReqVO.getProjectId());
        validateQuoteWindow(project);
        validateQuoteRequest(quoteReqVO);

        SrmSupplierEligibilityRespVO eligibility = supplierAccessRiskService.checkSupplierEligibility(quoteReqVO.getSupplierId());
        if (!Boolean.TRUE.equals(eligibility.getEligible())) {
            throw exception(SUPPLIER_ELIGIBILITY_BLOCKED, eligibility.getBlockedReason());
        }
        if (requiresInvitedSupplier(project)
                && supplierScopeMapper.selectByProjectIdAndSupplierId(project.getId(), quoteReqVO.getSupplierId()) == null) {
            throw exception(NON_BIDDING_QUOTE_SUPPLIER_NOT_INVITED);
        }
        if (quoteMapper.selectByProjectIdAndSupplierId(project.getId(), quoteReqVO.getSupplierId()) != null) {
            throw exception(NON_BIDDING_QUOTE_DUPLICATE);
        }

        Map<Long, SrmSourcingProjectLineDO> projectLineMap = sourcingProjectLineMapper.selectListByProjectId(project.getId()).stream()
                .collect(Collectors.toMap(SrmSourcingProjectLineDO::getId, Function.identity()));
        validateQuoteAmountConsistency(quoteReqVO, projectLineMap);
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        LocalDateTime now = LocalDateTime.now();

        SrmNonBiddingQuoteDO quote = SrmNonBiddingQuoteDO.builder()
                .projectId(project.getId())
                .supplierId(eligibility.getSupplierId())
                .supplierName(eligibility.getSupplierName())
                .quoteAmount(quoteReqVO.getQuoteAmount())
                .quoteStatus(QUOTE_STATUS_SUBMITTED)
                .attachmentUrl(quoteReqVO.getAttachmentUrl())
                .quotedBy(userId)
                .quotedName(nickname)
                .quotedTime(now)
                .build();
        quote.setTenantId(getRequiredTenantId());
        quoteMapper.insert(quote);

        for (SrmNonBiddingQuoteReqVO.Line reqLine : quoteReqVO.getLines()) {
            SrmSourcingProjectLineDO projectLine = projectLineMap.get(reqLine.getProjectLineId());
            if (projectLine == null) {
                throw exception(NON_BIDDING_QUOTE_LINE_INVALID);
            }
            SrmNonBiddingQuoteLineDO line = SrmNonBiddingQuoteLineDO.builder()
                    .quoteId(quote.getId())
                    .projectId(project.getId())
                    .projectLineId(projectLine.getId())
                    .materialId(projectLine.getMaterialId())
                    .materialCode(projectLine.getMaterialCode())
                    .materialName(projectLine.getMaterialName())
                    .quantity(projectLine.getQuantity())
                    .unit(projectLine.getUnit())
                    .unitPrice(reqLine.getUnitPrice())
                    .lineAmount(reqLine.getLineAmount())
                    .build();
            line.setTenantId(getRequiredTenantId());
            quoteLineMapper.insert(line);
        }
        return buildProjectResp(project.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmNonBiddingProjectRespVO confirmDeal(SrmNonBiddingDealReqVO dealReqVO) {
        if (StrUtil.isBlank(dealReqVO.getDealRemark())) {
            throw exception(NON_BIDDING_DEAL_REMARK_REQUIRED);
        }
        SrmSourcingProjectDO project = validatePublishedNonBiddingProject(dealReqVO.getProjectId());
        SrmNonBiddingQuoteDO quote = quoteMapper.selectById(dealReqVO.getQuoteId());
        if (quote == null || !Objects.equals(quote.getProjectId(), project.getId())) {
            throw exception(NON_BIDDING_DEAL_QUOTE_NOT_EXISTS);
        }

        project.setProjectStatus(SrmSourcingProjectStatusEnum.DEAL_CONFIRMED.getStatus());
        project.setDealQuoteId(quote.getId());
        project.setDealSupplierId(quote.getSupplierId());
        project.setDealSupplierName(quote.getSupplierName());
        project.setDealAmount(quote.getQuoteAmount());
        project.setDealRemark(dealReqVO.getDealRemark());
        project.setDealTime(LocalDateTime.now());
        project.setContractId(null);
        sourcingProjectMapper.updateById(project);
        return buildProjectResp(project.getId());
    }

    @Override
    public SrmNonBiddingProjectRespVO getProject(Long id) {
        return buildProjectResp(validateNonBiddingProject(id).getId());
    }

    @Override
    public PageResult<SrmNonBiddingProjectRespVO> getProjectPage(SrmNonBiddingProjectPageReqVO pageReqVO) {
        PageResult<SrmSourcingProjectDO> pageResult = sourcingProjectMapper.selectNonBiddingPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream()
                .map(project -> buildProjectResp(project.getId()))
                .collect(Collectors.toList()), pageResult.getTotal());
    }

    @Override
    public PageResult<SrmNonBiddingProjectRespVO> getContractableProjectPage(SrmNonBiddingProjectPageReqVO pageReqVO) {
        PageResult<SrmSourcingProjectDO> pageResult = sourcingProjectMapper.selectContractableNonBiddingPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream()
                .map(project -> buildProjectResp(project.getId()))
                .collect(Collectors.toList()), pageResult.getTotal());
    }

    private SrmSourcingProjectDO validateNonBiddingProject(Long id) {
        SrmSourcingProjectDO project = sourcingProjectMapper.selectById(id);
        if (project == null || !Objects.equals(project.getTenantId(), getRequiredTenantId())) {
            throw exception(NON_BIDDING_PROJECT_NOT_EXISTS);
        }
        if (!SrmProcurementMethodEnum.NON_BIDDING.getMethod().equals(project.getProjectType())) {
            throw exception(NON_BIDDING_PROJECT_TYPE_INVALID);
        }
        return project;
    }

    private SrmSourcingProjectDO validateDraftNonBiddingProject(Long id) {
        SrmSourcingProjectDO project = validateNonBiddingProject(id);
        if (!SrmSourcingProjectStatusEnum.DRAFT.getStatus().equals(project.getProjectStatus())) {
            throw exception(NON_BIDDING_PROJECT_STATUS_INVALID, SrmSourcingProjectStatusEnum.getLabel(project.getProjectStatus()));
        }
        return project;
    }

    private SrmSourcingProjectDO validatePublishedNonBiddingProject(Long id) {
        SrmSourcingProjectDO project = validateNonBiddingProject(id);
        if (!SrmSourcingProjectStatusEnum.PUBLISHED.getStatus().equals(project.getProjectStatus())) {
            throw exception(NON_BIDDING_PROJECT_STATUS_INVALID, SrmSourcingProjectStatusEnum.getLabel(project.getProjectStatus()));
        }
        return project;
    }

    private void validatePublishRequest(SrmNonBiddingPublishReqVO reqVO) {
        if (reqVO.getQuoteStartTime() == null || reqVO.getQuoteEndTime() == null
                || !reqVO.getQuoteEndTime().isAfter(reqVO.getQuoteStartTime())) {
            throw exception(NON_BIDDING_PUBLISH_WINDOW_INVALID);
        }
        if (StrUtil.isBlank(reqVO.getAttachmentUrl())) {
            throw exception(NON_BIDDING_PUBLISH_ATTACHMENT_REQUIRED);
        }
        String quoteMode = normalizeQuoteMode(reqVO.getQuoteMode());
        if (QUOTE_MODE_INVITE.equals(quoteMode) && distinctSupplierIds(reqVO.getSupplierIds()).isEmpty()) {
            throw exception(NON_BIDDING_PUBLISH_SUPPLIER_REQUIRED);
        }
    }

    private void validateQuoteRequest(SrmNonBiddingQuoteReqVO reqVO) {
        if (reqVO.getQuoteAmount() == null || reqVO.getQuoteAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(NON_BIDDING_QUOTE_AMOUNT_INVALID);
        }
        if (StrUtil.isBlank(reqVO.getAttachmentUrl())) {
            throw exception(NON_BIDDING_QUOTE_ATTACHMENT_REQUIRED);
        }
        if (reqVO.getLines() == null || reqVO.getLines().isEmpty()) {
            throw exception(NON_BIDDING_QUOTE_LINE_REQUIRED);
        }
        for (SrmNonBiddingQuoteReqVO.Line line : reqVO.getLines()) {
            if (line.getProjectLineId() == null
                    || line.getUnitPrice() == null || line.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0
                    || line.getLineAmount() == null || line.getLineAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(NON_BIDDING_QUOTE_LINE_INVALID);
            }
        }
    }

    private void validateQuoteAmountConsistency(SrmNonBiddingQuoteReqVO reqVO,
                                                Map<Long, SrmSourcingProjectLineDO> projectLineMap) {
        BigDecimal lineTotal = BigDecimal.ZERO;
        for (SrmNonBiddingQuoteReqVO.Line reqLine : reqVO.getLines()) {
            SrmSourcingProjectLineDO projectLine = projectLineMap.get(reqLine.getProjectLineId());
            if (projectLine == null) {
                throw exception(NON_BIDDING_QUOTE_LINE_INVALID);
            }
            BigDecimal expectedLineAmount = toMoney(projectLine.getQuantity().multiply(reqLine.getUnitPrice()));
            if (expectedLineAmount.compareTo(toMoney(reqLine.getLineAmount())) != 0) {
                throw exception(NON_BIDDING_QUOTE_AMOUNT_MISMATCH);
            }
            lineTotal = lineTotal.add(reqLine.getLineAmount());
        }
        if (toMoney(lineTotal).compareTo(toMoney(reqVO.getQuoteAmount())) != 0) {
            throw exception(NON_BIDDING_QUOTE_AMOUNT_MISMATCH);
        }
    }

    private BigDecimal toMoney(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private void validateQuoteWindow(SrmSourcingProjectDO project) {
        LocalDateTime now = LocalDateTime.now();
        if (project.getQuoteStartTime() == null || project.getQuoteEndTime() == null
                || now.isBefore(project.getQuoteStartTime()) || now.isAfter(project.getQuoteEndTime())) {
            throw exception(NON_BIDDING_QUOTE_WINDOW_CLOSED);
        }
    }

    private List<Long> distinctSupplierIds(List<Long> supplierIds) {
        if (supplierIds == null) {
            return Collections.emptyList();
        }
        return supplierIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private SrmNonBiddingProjectRespVO buildProjectResp(Long projectId) {
        SrmSourcingProjectDO project = validateNonBiddingProject(projectId);
        SrmNonBiddingProjectRespVO respVO = new SrmNonBiddingProjectRespVO();
        respVO.setId(project.getId());
        respVO.setProjectNo(project.getProjectNo());
        respVO.setProjectTitle(project.getProjectTitle());
        respVO.setProjectType(project.getProjectType());
        respVO.setProjectTypeLabel(SrmProcurementMethodEnum.getLabel(project.getProjectType()));
        respVO.setProjectStatus(project.getProjectStatus());
        respVO.setProjectStatusLabel(SrmSourcingProjectStatusEnum.getLabel(project.getProjectStatus()));
        respVO.setSourcePlanId(project.getSourcePlanId());
        respVO.setSourcePlanNo(project.getSourcePlanNo());
        respVO.setExpectedAmount(project.getExpectedAmount());
        respVO.setQuoteMode(project.getQuoteMode());
        respVO.setQuoteModeLabel(buildQuoteModeLabel(project.getQuoteMode()));
        respVO.setQuoteStartTime(project.getQuoteStartTime());
        respVO.setQuoteEndTime(project.getQuoteEndTime());
        respVO.setPublishAttachmentUrl(project.getPublishAttachmentUrl());
        respVO.setPublishedTime(project.getPublishedTime());
        respVO.setDealQuoteId(project.getDealQuoteId());
        respVO.setDealSupplierId(project.getDealSupplierId());
        respVO.setDealSupplierName(project.getDealSupplierName());
        respVO.setDealAmount(project.getDealAmount());
        respVO.setDealRemark(project.getDealRemark());
        respVO.setDealTime(project.getDealTime());
        respVO.setContractId(project.getContractId());
        respVO.setCreateTime(project.getCreateTime());
        respVO.setLines(sourcingProjectLineMapper.selectListByProjectId(project.getId()).stream()
                .map(this::buildLineResp)
                .collect(Collectors.toList()));
        respVO.setSupplierScopes(supplierScopeMapper.selectListByProjectId(project.getId()).stream()
                .map(this::buildSupplierScopeResp)
                .collect(Collectors.toList()));
        List<SrmNonBiddingQuoteDO> quotes = quoteMapper.selectListByProjectId(project.getId());
        respVO.setQuotes(quotes.stream()
                .map(this::buildQuoteResp)
                .collect(Collectors.toList()));
        respVO.setComparisonSummary(buildComparisonSummary(quotes));
        respVO.setPriceTrends(buildPriceTrends(project, respVO.getLines()));
        return respVO;
    }

    private boolean requiresInvitedSupplier(SrmSourcingProjectDO project) {
        return !QUOTE_MODE_PUBLIC.equals(normalizeQuoteMode(project.getQuoteMode()));
    }

    private String normalizeQuoteMode(String quoteMode) {
        if (QUOTE_MODE_PUBLIC.equals(quoteMode) || QUOTE_MODE_INVITE.equals(quoteMode)) {
            return quoteMode;
        }
        throw exception(NON_BIDDING_QUOTE_MODE_INVALID);
    }

    private String buildQuoteModeLabel(String quoteMode) {
        return QUOTE_MODE_PUBLIC.equals(quoteMode) ? "公开询价" : "邀请询价";
    }

    private SrmNonBiddingProjectRespVO.Line buildLineResp(SrmSourcingProjectLineDO line) {
        SrmNonBiddingProjectRespVO.Line respVO = new SrmNonBiddingProjectRespVO.Line();
        respVO.setId(line.getId());
        respVO.setSourcePlanLineId(line.getSourcePlanLineId());
        respVO.setLineNo(line.getLineNo());
        respVO.setMaterialId(line.getMaterialId());
        respVO.setMaterialCode(line.getMaterialCode());
        respVO.setMaterialName(line.getMaterialName());
        respVO.setQuantity(line.getQuantity());
        respVO.setUnit(line.getUnit());
        respVO.setRequiredDate(line.getRequiredDate());
        return respVO;
    }

    private SrmNonBiddingProjectRespVO.SupplierScope buildSupplierScopeResp(SrmNonBiddingSupplierScopeDO scope) {
        SrmNonBiddingProjectRespVO.SupplierScope respVO = new SrmNonBiddingProjectRespVO.SupplierScope();
        respVO.setId(scope.getId());
        respVO.setSupplierId(scope.getSupplierId());
        respVO.setSupplierName(scope.getSupplierName());
        return respVO;
    }

    private SrmNonBiddingProjectRespVO.Quote buildQuoteResp(SrmNonBiddingQuoteDO quote) {
        SrmNonBiddingProjectRespVO.Quote respVO = new SrmNonBiddingProjectRespVO.Quote();
        respVO.setId(quote.getId());
        respVO.setSupplierId(quote.getSupplierId());
        respVO.setSupplierName(quote.getSupplierName());
        respVO.setQuoteAmount(quote.getQuoteAmount());
        respVO.setQuoteStatus(quote.getQuoteStatus());
        respVO.setAttachmentUrl(quote.getAttachmentUrl());
        respVO.setQuotedName(quote.getQuotedName());
        respVO.setQuotedTime(quote.getQuotedTime());
        respVO.setLines(quoteLineMapper.selectListByQuoteId(quote.getId()).stream()
                .map(this::buildQuoteLineResp)
                .collect(Collectors.toList()));
        return respVO;
    }

    private SrmNonBiddingProjectRespVO.QuoteLine buildQuoteLineResp(SrmNonBiddingQuoteLineDO line) {
        SrmNonBiddingProjectRespVO.QuoteLine respVO = new SrmNonBiddingProjectRespVO.QuoteLine();
        respVO.setId(line.getId());
        respVO.setProjectLineId(line.getProjectLineId());
        respVO.setMaterialId(line.getMaterialId());
        respVO.setMaterialCode(line.getMaterialCode());
        respVO.setMaterialName(line.getMaterialName());
        respVO.setQuantity(line.getQuantity());
        respVO.setUnit(line.getUnit());
        respVO.setUnitPrice(line.getUnitPrice());
        respVO.setLineAmount(line.getLineAmount());
        return respVO;
    }

    private SrmNonBiddingProjectRespVO.ComparisonSummary buildComparisonSummary(List<SrmNonBiddingQuoteDO> quotes) {
        if (quotes.isEmpty()) {
            return null;
        }
        List<SrmNonBiddingQuoteDO> sortedQuotes = quotes.stream()
                .sorted(Comparator.comparing(SrmNonBiddingQuoteDO::getQuoteAmount)
                        .thenComparing(SrmNonBiddingQuoteDO::getQuotedTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(SrmNonBiddingQuoteDO::getId))
                .collect(Collectors.toList());
        SrmNonBiddingProjectRespVO.ComparisonSummary summary = new SrmNonBiddingProjectRespVO.ComparisonSummary();
        summary.setSupplierQuoteCount(sortedQuotes.size());
        summary.setLowestQuoteAmount(sortedQuotes.get(0).getQuoteAmount());
        summary.setLowestQuoteSupplierId(sortedQuotes.get(0).getSupplierId());
        summary.setLowestQuoteSupplierName(sortedQuotes.get(0).getSupplierName());
        summary.setHighestQuoteAmount(sortedQuotes.get(sortedQuotes.size() - 1).getQuoteAmount());
        summary.setAverageQuoteAmount(toMoney(quotes.stream()
                .map(SrmNonBiddingQuoteDO::getQuoteAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(sortedQuotes.size()), MONEY_SCALE, RoundingMode.HALF_UP)));
        summary.setQuoteRankings(buildQuoteRankings(sortedQuotes));
        return summary;
    }

    private List<SrmNonBiddingProjectRespVO.QuoteRanking> buildQuoteRankings(List<SrmNonBiddingQuoteDO> sortedQuotes) {
        List<SrmNonBiddingProjectRespVO.QuoteRanking> rankings = new ArrayList<>();
        for (int i = 0; i < sortedQuotes.size(); i++) {
            SrmNonBiddingQuoteDO quote = sortedQuotes.get(i);
            SrmNonBiddingProjectRespVO.QuoteRanking ranking = new SrmNonBiddingProjectRespVO.QuoteRanking();
            ranking.setRankNo(i + 1);
            ranking.setQuoteId(quote.getId());
            ranking.setSupplierId(quote.getSupplierId());
            ranking.setSupplierName(quote.getSupplierName());
            ranking.setQuoteAmount(quote.getQuoteAmount());
            ranking.setQuotedTime(quote.getQuotedTime());
            rankings.add(ranking);
        }
        return rankings;
    }

    private List<SrmNonBiddingProjectRespVO.PriceTrend> buildPriceTrends(SrmSourcingProjectDO project,
                                                                         List<SrmNonBiddingProjectRespVO.Line> lines) {
        return lines.stream()
                .map(line -> buildPriceTrend(project, line))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private SrmNonBiddingProjectRespVO.PriceTrend buildPriceTrend(SrmSourcingProjectDO currentProject,
                                                                  SrmNonBiddingProjectRespVO.Line line) {
        List<SrmNonBiddingQuoteLineDO> historyLines = quoteLineMapper.selectListByMaterialId(line.getMaterialId());
        if (historyLines.isEmpty()) {
            return null;
        }
        Map<Long, SrmNonBiddingQuoteDO> quoteMap = quoteMapper.selectBatchIds(historyLines.stream()
                        .map(SrmNonBiddingQuoteLineDO::getQuoteId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(SrmNonBiddingQuoteDO::getId, Function.identity()));
        Map<Long, SrmSourcingProjectDO> projectMap = sourcingProjectMapper.selectBatchIds(historyLines.stream()
                        .map(SrmNonBiddingQuoteLineDO::getProjectId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(SrmSourcingProjectDO::getId, Function.identity()));

        List<SrmNonBiddingProjectRespVO.PriceTrendPoint> points = historyLines.stream()
                .map(historyLine -> buildPriceTrendPoint(historyLine, quoteMap, projectMap))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SrmNonBiddingProjectRespVO.PriceTrendPoint::getQuotedTime,
                        Comparator.nullsLast(LocalDateTime::compareTo)))
                .collect(Collectors.toList());
        if (points.isEmpty()) {
            return null;
        }
        SrmNonBiddingProjectRespVO.PriceTrend trend = new SrmNonBiddingProjectRespVO.PriceTrend();
        trend.setMaterialId(line.getMaterialId());
        trend.setMaterialCode(line.getMaterialCode());
        trend.setMaterialName(line.getMaterialName());
        trend.setPoints(points);
        return trend;
    }

    private SrmNonBiddingProjectRespVO.PriceTrendPoint buildPriceTrendPoint(SrmNonBiddingQuoteLineDO historyLine,
                                                                            Map<Long, SrmNonBiddingQuoteDO> quoteMap,
                                                                            Map<Long, SrmSourcingProjectDO> projectMap) {
        SrmNonBiddingQuoteDO quote = quoteMap.get(historyLine.getQuoteId());
        SrmSourcingProjectDO project = projectMap.get(historyLine.getProjectId());
        if (quote == null || project == null) {
            return null;
        }
        SrmNonBiddingProjectRespVO.PriceTrendPoint point = new SrmNonBiddingProjectRespVO.PriceTrendPoint();
        point.setProjectId(project.getId());
        point.setProjectNo(project.getProjectNo());
        point.setQuoteId(quote.getId());
        point.setSupplierId(quote.getSupplierId());
        point.setSupplierName(quote.getSupplierName());
        point.setUnitPrice(historyLine.getUnitPrice());
        point.setLineAmount(historyLine.getLineAmount());
        point.setQuotedTime(quote.getQuotedTime());
        return point;
    }

    private Long getRequiredLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(SUPPLIER_LOGIN_CONTEXT_MISSING);
        }
        return userId;
    }

    private String getRequiredLoginUserNickname() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StrUtil.isBlank(nickname)) {
            throw exception(SUPPLIER_LOGIN_CONTEXT_MISSING);
        }
        return nickname;
    }

    private Long getRequiredTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }
}
