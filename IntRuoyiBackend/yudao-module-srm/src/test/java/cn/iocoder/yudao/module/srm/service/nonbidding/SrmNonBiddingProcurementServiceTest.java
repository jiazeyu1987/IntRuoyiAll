package cn.iocoder.yudao.module.srm.service.nonbidding;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo.SrmNonBiddingDealReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo.SrmNonBiddingProjectPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo.SrmNonBiddingProjectRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo.SrmNonBiddingPublishReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo.SrmNonBiddingQuoteReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanGenerateReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierAccessAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierAccessSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import cn.iocoder.yudao.module.srm.dal.mysql.nonbidding.SrmNonBiddingQuoteMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.nonbidding.SrmNonBiddingSupplierScopeMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmErpSupplierMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierPortalApplicationMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmSourcingProjectStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierPortalApplicationStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleServiceImpl;
import cn.iocoder.yudao.module.srm.service.procurement.SrmProcurementPlanServiceImpl;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskServiceImpl;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierPortalApplicationServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@Import({
        SrmCodeRuleServiceImpl.class,
        SrmSupplierPortalApplicationServiceImpl.class,
        SrmSupplierAccessRiskServiceImpl.class,
        SrmProcurementPlanServiceImpl.class,
        SrmNonBiddingProcurementServiceImpl.class
})
class SrmNonBiddingProcurementServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmCodeRuleServiceImpl codeRuleService;
    @Resource
    private SrmSupplierAccessRiskServiceImpl supplierAccessRiskService;
    @Resource
    private SrmProcurementPlanServiceImpl procurementPlanService;
    @Resource
    private SrmNonBiddingProcurementServiceImpl nonBiddingService;
    @Resource
    private SrmErpSupplierMapper erpSupplierMapper;
    @Resource
    private SrmSourcingProjectMapper sourcingProjectMapper;
    @Resource
    private SrmSourcingProjectLineMapper sourcingProjectLineMapper;
    @Resource
    private SrmNonBiddingSupplierScopeMapper supplierScopeMapper;
    @Resource
    private SrmNonBiddingQuoteMapper quoteMapper;
    @Resource
    private SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper;

    @Test
    void publishQuoteAndDeal_shouldExposeContractableSourceWithoutCreatingContract() {
        seedCodeRules();
        approveSupplier(130L, "非招标合格供应商");
        Long projectId = createApprovedNonBiddingProject("T3 非招标完整链路");

        SrmNonBiddingProjectRespVO published;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(41L, "non-bidding-owner")) {
            published = nonBiddingService.publishProject(buildPublishReq(projectId, List.of(130L)));
        }

        assertEquals(SrmSourcingProjectStatusEnum.PUBLISHED.getStatus(), published.getProjectStatus());
        assertEquals(1, published.getSupplierScopes().size());
        assertEquals(1, supplierScopeMapper.selectListByProjectId(projectId).size());

        SrmNonBiddingProjectRespVO quoted;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(42L, "quote-owner")) {
            quoted = nonBiddingService.submitQuote(buildQuoteReq(projectId, 130L, firstProjectLineId(projectId)));
        }
        assertEquals(1, quoted.getQuotes().size());
        assertEquals(1, quoteMapper.selectListByProjectId(projectId).size());

        Long quoteId = quoted.getQuotes().get(0).getId();
        SrmNonBiddingProjectRespVO dealt;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(43L, "deal-owner")) {
            dealt = nonBiddingService.confirmDeal(buildDealReq(projectId, quoteId));
        }
        assertEquals(SrmSourcingProjectStatusEnum.DEAL_CONFIRMED.getStatus(), dealt.getProjectStatus());
        assertEquals(130L, dealt.getDealSupplierId());
        assertNull(dealt.getContractId());

        SrmNonBiddingProjectPageReqVO pageReqVO = new SrmNonBiddingProjectPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        PageResult<SrmNonBiddingProjectRespVO> contractablePage = nonBiddingService.getContractableProjectPage(pageReqVO);
        assertEquals(1, contractablePage.getTotal());
        assertEquals(projectId, contractablePage.getList().get(0).getId());
    }

    @Test
    void publishProject_shouldFailWhenWindowScopeOrAttachmentIsMissing() {
        seedCodeRules();
        approveSupplier(131L, "非招标发布校验供应商");

        Long missingAttachmentProjectId = createApprovedNonBiddingProject("T3 缺附件");
        SrmNonBiddingPublishReqVO missingAttachmentReqVO = buildPublishReq(missingAttachmentProjectId, List.of(131L));
        missingAttachmentReqVO.setAttachmentUrl(" ");
        ServiceException attachmentException = assertThrows(ServiceException.class,
                () -> nonBiddingService.publishProject(missingAttachmentReqVO));
        assertTrue(attachmentException.getMessage().contains("附件"));

        Long missingScopeProjectId = createApprovedNonBiddingProject("T3 缺供应商范围");
        SrmNonBiddingPublishReqVO missingScopeReqVO = buildPublishReq(missingScopeProjectId, List.of());
        ServiceException scopeException = assertThrows(ServiceException.class,
                () -> nonBiddingService.publishProject(missingScopeReqVO));
        assertTrue(scopeException.getMessage().contains("供应商"));

        Long invalidWindowProjectId = createApprovedNonBiddingProject("T3 报价时间无效");
        SrmNonBiddingPublishReqVO invalidWindowReqVO = buildPublishReq(invalidWindowProjectId, List.of(131L));
        invalidWindowReqVO.setQuoteEndTime(invalidWindowReqVO.getQuoteStartTime().minusMinutes(1));
        ServiceException windowException = assertThrows(ServiceException.class,
                () -> nonBiddingService.publishProject(invalidWindowReqVO));
        assertTrue(windowException.getMessage().contains("报价时间"));

        assertEquals(SrmSourcingProjectStatusEnum.DRAFT.getStatus(),
                sourcingProjectMapper.selectById(missingAttachmentProjectId).getProjectStatus());
        assertEquals(0, supplierScopeMapper.selectListByProjectId(missingScopeProjectId).size());
    }

    @Test
    void publishProject_shouldFailWhenQuoteModeIsInvalid() {
        seedCodeRules();
        approveSupplier(136L, "非招标询价模式校验供应商");
        Long projectId = createApprovedNonBiddingProject("T3 询价模式无效");

        SrmNonBiddingPublishReqVO reqVO = buildPublishReq(projectId, List.of(136L));
        reqVO.setQuoteMode("DIRECT");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> nonBiddingService.publishProject(reqVO));
        assertTrue(exception.getMessage().contains("询价模式"));
    }

    @Test
    void submitQuote_shouldFailWhenSupplierNotEligibleUninvitedExpiredOrDuplicate() {
        seedCodeRules();
        approveSupplier(132L, "非招标受邀供应商");
        approveSupplier(133L, "非招标未受邀供应商");
        erpSupplierMapper.insert(SrmErpSupplierDO.builder()
                .id(134L)
                .name("非招标未准入供应商")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .tenantId(1L)
                .build());
        Long projectId = createApprovedNonBiddingProject("T3 报价门禁");
        nonBiddingService.publishProject(buildPublishReq(projectId, List.of(132L)));

        ServiceException notEligibleException = assertThrows(ServiceException.class,
                () -> nonBiddingService.submitQuote(buildQuoteReq(projectId, 134L, firstProjectLineId(projectId))));
        assertTrue(notEligibleException.getMessage().contains("供应商资格"));

        ServiceException uninvitedException = assertThrows(ServiceException.class,
                () -> nonBiddingService.submitQuote(buildQuoteReq(projectId, 133L, firstProjectLineId(projectId))));
        assertTrue(uninvitedException.getMessage().contains("受邀"));

        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(42L, "quote-owner")) {
            nonBiddingService.submitQuote(buildQuoteReq(projectId, 132L, firstProjectLineId(projectId)));
        }
        ServiceException duplicateException = assertThrows(ServiceException.class,
                () -> nonBiddingService.submitQuote(buildQuoteReq(projectId, 132L, firstProjectLineId(projectId))));
        assertTrue(duplicateException.getMessage().contains("重复"));

        Long expiredProjectId = createApprovedNonBiddingProject("T3 报价过期");
        nonBiddingService.publishProject(buildPublishReq(expiredProjectId, List.of(132L)));
        SrmSourcingProjectDO expiredProject = sourcingProjectMapper.selectById(expiredProjectId);
        expiredProject.setQuoteEndTime(LocalDateTime.now().minusMinutes(1));
        sourcingProjectMapper.updateById(expiredProject);

        ServiceException expiredException = assertThrows(ServiceException.class,
                () -> nonBiddingService.submitQuote(buildQuoteReq(expiredProjectId, 132L, firstProjectLineId(expiredProjectId))));
        assertTrue(expiredException.getMessage().contains("报价时间"));
    }

    @Test
    void submitQuote_shouldAllowEligibleSupplierWhenProjectIsPublic() {
        seedCodeRules();
        approveSupplier(137L, "非招标公开询价供应商");
        Long projectId = createApprovedNonBiddingProject("T3 公开询价报价");

        SrmNonBiddingPublishReqVO publishReqVO = buildPublishReq(projectId, List.of());
        publishReqVO.setQuoteMode("PUBLIC");
        nonBiddingService.publishProject(publishReqVO);

        SrmNonBiddingProjectRespVO quoted;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(46L, "public-quote-owner")) {
            quoted = nonBiddingService.submitQuote(buildQuoteReq(projectId, 137L, firstProjectLineId(projectId)));
        }

        assertEquals("PUBLIC", sourcingProjectMapper.selectById(projectId).getQuoteMode());
        assertTrue(quoted.getSupplierScopes().isEmpty());
        assertEquals(1, quoted.getQuotes().size());
        assertEquals(137L, quoted.getQuotes().get(0).getSupplierId());
    }

    @Test
    void getProject_shouldBuildQuoteComparisonSummaryFromRealQuotes() {
        seedCodeRules();
        approveSupplier(138L, "非招标比价供应商A");
        approveSupplier(139L, "非招标比价供应商B");
        Long projectId = createApprovedNonBiddingProject("T3 比价汇总");
        nonBiddingService.publishProject(buildPublishReq(projectId, List.of(138L, 139L)));

        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(47L, "compare-quote-a")) {
            nonBiddingService.submitQuote(buildQuoteReq(projectId, 138L, firstProjectLineId(projectId)));
        }
        SrmNonBiddingQuoteReqVO lowerQuoteReqVO = buildQuoteReq(projectId, 139L, firstProjectLineId(projectId));
        lowerQuoteReqVO.setQuoteAmount(new BigDecimal("980.00"));
        lowerQuoteReqVO.getLines().get(0).setUnitPrice(new BigDecimal("98.00"));
        lowerQuoteReqVO.getLines().get(0).setLineAmount(new BigDecimal("980.00"));
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(48L, "compare-quote-b")) {
            nonBiddingService.submitQuote(lowerQuoteReqVO);
        }

        SrmNonBiddingProjectRespVO project = nonBiddingService.getProject(projectId);
        assertNotNull(project.getComparisonSummary());
        assertEquals(2, project.getComparisonSummary().getSupplierQuoteCount());
        assertEquals(new BigDecimal("980.00"), project.getComparisonSummary().getLowestQuoteAmount());
        assertEquals(139L, project.getComparisonSummary().getLowestQuoteSupplierId());
        assertEquals(138L, project.getComparisonSummary().getQuoteRankings().get(1).getSupplierId());
        assertEquals(Integer.valueOf(1), project.getComparisonSummary().getQuoteRankings().get(0).getRankNo());
    }

    @Test
    void getProject_shouldExposePriceTrendFromHistoricalQuotes() {
        seedCodeRules();
        approveSupplier(140L, "非招标趋势供应商A");
        approveSupplier(141L, "非招标趋势供应商B");
        Long historicalProjectId = createApprovedNonBiddingProject("T3 历史价格项目");
        nonBiddingService.publishProject(buildPublishReq(historicalProjectId, List.of(140L)));
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(49L, "history-quote-a")) {
            nonBiddingService.submitQuote(buildQuoteReq(historicalProjectId, 140L, firstProjectLineId(historicalProjectId)));
        }

        Long currentProjectId = createApprovedNonBiddingProject("T3 当前价格项目");
        nonBiddingService.publishProject(buildPublishReq(currentProjectId, List.of(141L)));
        SrmNonBiddingQuoteReqVO currentQuoteReqVO = buildQuoteReq(currentProjectId, 141L, firstProjectLineId(currentProjectId));
        currentQuoteReqVO.setQuoteAmount(new BigDecimal("1280.00"));
        currentQuoteReqVO.getLines().get(0).setUnitPrice(new BigDecimal("128.00"));
        currentQuoteReqVO.getLines().get(0).setLineAmount(new BigDecimal("1280.00"));
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(50L, "history-quote-b")) {
            nonBiddingService.submitQuote(currentQuoteReqVO);
        }

        SrmNonBiddingProjectRespVO project = nonBiddingService.getProject(currentProjectId);
        assertFalse(project.getPriceTrends().isEmpty());
        SrmNonBiddingProjectRespVO.PriceTrend trend = project.getPriceTrends().get(0);
        assertEquals("MAT-5001", trend.getMaterialCode());
        assertEquals(2, trend.getPoints().size());
        assertEquals(historicalProjectId, trend.getPoints().get(0).getProjectId());
        assertEquals(new BigDecimal("118.00"), trend.getPoints().get(0).getUnitPrice());
        assertEquals(currentProjectId, trend.getPoints().get(1).getProjectId());
        assertEquals(new BigDecimal("128.00"), trend.getPoints().get(1).getUnitPrice());
    }

    @Test
    void submitQuote_shouldFailWhenQuoteAmountDoesNotMatchLineTotal() {
        seedCodeRules();
        approveSupplier(135L, "非招标报价金额校验供应商");
        Long projectId = createApprovedNonBiddingProject("T3 报价金额合计校验");
        nonBiddingService.publishProject(buildPublishReq(projectId, List.of(135L)));

        SrmNonBiddingQuoteReqVO reqVO = buildQuoteReq(projectId, 135L, firstProjectLineId(projectId));
        reqVO.setQuoteAmount(new BigDecimal("1181.00"));
        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(45L, "quote-reviewer")) {
            exception = assertThrows(ServiceException.class,
                    () -> nonBiddingService.submitQuote(reqVO));
        }
        assertTrue(exception.getMessage().contains("合计"));
    }

    private Long createApprovedNonBiddingProject(String title) {
        Long planId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            planId = procurementPlanService.createProcurementPlan(buildPlanSaveReq(title));
            procurementPlanService.submitProcurementPlan(planId);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(22L, "approver")) {
            SrmProcurementPlanAuditReqVO auditReqVO = new SrmProcurementPlanAuditReqVO();
            auditReqVO.setId(planId);
            auditReqVO.setAuditRemark("同意非招标采购");
            procurementPlanService.approveProcurementPlan(auditReqVO);
        }
        SrmProcurementPlanGenerateReqVO generateReqVO = new SrmProcurementPlanGenerateReqVO();
        generateReqVO.setId(planId);
        generateReqVO.setProjectType(SrmProcurementMethodEnum.NON_BIDDING.getMethod());
        return procurementPlanService.generateSourcingProject(generateReqVO).getId();
    }

    private void approveSupplier(Long supplierId, String supplierName) {
        erpSupplierMapper.insert(SrmErpSupplierDO.builder()
                .id(supplierId)
                .name(supplierName)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .tenantId(1L)
                .build());
        insertApprovedPortalApplication(supplierId, supplierId, supplierName + "-portal");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "supplier-owner")) {
            SrmSupplierAccessSaveReqVO reqVO = new SrmSupplierAccessSaveReqVO();
            reqVO.setSupplierId(supplierId);
            reqVO.setAccessRemark("非招标供应商准入");
            reqVO.setPortalContactName("联系人-" + supplierId);
            reqVO.setPortalContactPhone("1380013" + String.format("%04d", supplierId % 10000));
            reqVO.setQualificationExpireDate(LocalDate.now().plusDays(60));
            accessId = supplierAccessRiskService.createSupplierAccess(reqVO);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(12L, "sample-auditor")) {
            SrmSupplierAccessAuditReqVO reqVO = new SrmSupplierAccessAuditReqVO();
            reqVO.setId(accessId);
            reqVO.setAuditRemark("样品测试通过");
            supplierAccessRiskService.approveSampleTest(reqVO);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(13L, "trial-auditor")) {
            SrmSupplierAccessAuditReqVO reqVO = new SrmSupplierAccessAuditReqVO();
            reqVO.setId(accessId);
            reqVO.setAuditRemark("小批试用通过");
            supplierAccessRiskService.approveTrialOrder(reqVO);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "supplier-auditor")) {
            SrmSupplierAccessAuditReqVO reqVO = new SrmSupplierAccessAuditReqVO();
            reqVO.setId(accessId);
            reqVO.setAuditRemark("准入通过");
            supplierAccessRiskService.approveSupplierAccess(reqVO);
        }
    }

    private void insertApprovedPortalApplication(Long supplierId, Long userId, String submitterName) {
        supplierPortalApplicationMapper.insert(SrmSupplierPortalApplicationDO.builder()
                .tenantId(1L)
                .userId(userId)
                .supplierId(supplierId)
                .companyName("门户申请-" + supplierId)
                .unifiedSocialCreditCode("USCC-" + supplierId)
                .contactName("联系人-" + supplierId)
                .contactPhone("1380013" + String.format("%04d", supplierId % 10000))
                .contactEmail("portal" + supplierId + "@example.com")
                .qualificationAttachmentUrls("http://files.local/" + supplierId + ".pdf")
                .qualificationExpireDate(LocalDate.now().plusDays(60))
                .bankName("招商银行")
                .bankAccount("622202" + supplierId)
                .bankAddress("深圳")
                .applicationStatus(SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus())
                .submitterName(submitterName)
                .submittedTime(LocalDateTime.now().minusDays(1))
                .auditBy(99L)
                .auditName("portal-auditor")
                .auditTime(LocalDateTime.now())
                .auditRemark("通过")
                .build());
    }

    private void seedCodeRules() {
        codeRuleService.createCodeRule(buildRule("T3_PLAN_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PP"));
        codeRuleService.createCodeRule(buildRule("T3_PLAN_LINE_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN_LINE.getTargetForm(), "PPL"));
        codeRuleService.createCodeRule(buildRule("T3_NON_BIDDING_RULE", SrmCodeRuleTargetFormEnum.NON_TENDER_PROJECT.getTargetForm(), "NB"));
    }

    private static SrmNonBiddingPublishReqVO buildPublishReq(Long projectId, List<Long> supplierIds) {
        SrmNonBiddingPublishReqVO reqVO = new SrmNonBiddingPublishReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setQuoteMode("INVITE");
        reqVO.setQuoteStartTime(LocalDateTime.now().minusMinutes(5));
        reqVO.setQuoteEndTime(LocalDateTime.now().plusDays(2));
        reqVO.setAttachmentUrl("http://127.0.0.1:9000/yudao/srm/non-bidding/t3-publish.pdf");
        reqVO.setSupplierIds(supplierIds);
        return reqVO;
    }

    private Long firstProjectLineId(Long projectId) {
        return sourcingProjectLineMapper.selectListByProjectId(projectId).get(0).getId();
    }

    private static SrmNonBiddingQuoteReqVO buildQuoteReq(Long projectId, Long supplierId, Long projectLineId) {
        SrmNonBiddingQuoteReqVO reqVO = new SrmNonBiddingQuoteReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setSupplierId(supplierId);
        reqVO.setQuoteAmount(new BigDecimal("1180.00"));
        reqVO.setAttachmentUrl("http://127.0.0.1:9000/yudao/srm/non-bidding/t3-quote.pdf");
        SrmNonBiddingQuoteReqVO.Line line = new SrmNonBiddingQuoteReqVO.Line();
        line.setProjectLineId(projectLineId);
        line.setUnitPrice(new BigDecimal("118.00"));
        line.setLineAmount(new BigDecimal("1180.00"));
        reqVO.setLines(List.of(line));
        return reqVO;
    }

    private static SrmNonBiddingDealReqVO buildDealReq(Long projectId, Long quoteId) {
        SrmNonBiddingDealReqVO reqVO = new SrmNonBiddingDealReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setQuoteId(quoteId);
        reqVO.setDealRemark("确认成交");
        return reqVO;
    }

    private static SrmProcurementPlanSaveReqVO buildPlanSaveReq(String title) {
        SrmProcurementPlanSaveReqVO reqVO = new SrmProcurementPlanSaveReqVO();
        reqVO.setPlanTitle(title);
        reqVO.setProcurementMethod(SrmProcurementMethodEnum.NON_BIDDING.getMethod());
        reqVO.setExpectedAmount(new BigDecimal("1280.50"));
        SrmProcurementPlanSaveReqVO.Line line = new SrmProcurementPlanSaveReqVO.Line();
        line.setMaterialId(5001L);
        line.setMaterialCode("MAT-5001");
        line.setMaterialName("一次性使用耗材");
        line.setQuantity(new BigDecimal("10.00"));
        line.setUnit("盒");
        line.setRequiredDate(LocalDate.now().plusDays(7));
        reqVO.setLines(List.of(line));
        return reqVO;
    }

    private static SrmCodeRuleSaveReqVO buildRule(String ruleCode, String targetForm, String prefix) {
        SrmCodeRuleSaveReqVO reqVO = new SrmCodeRuleSaveReqVO();
        reqVO.setRuleCode(ruleCode);
        reqVO.setRuleName(ruleCode);
        reqVO.setTargetForm(targetForm);
        reqVO.setPrefix(prefix);
        reqVO.setDateSegmentEnabled(true);
        reqVO.setDatePattern("yyyyMMdd");
        reqVO.setSerialWidth(4);
        reqVO.setStep(1);
        reqVO.setMinSerial(1L);
        reqVO.setMaxSerial(9999L);
        reqVO.setSeparator("-");
        reqVO.setEnabled(true);
        return reqVO;
    }

    private MockedStatic<SecurityFrameworkUtils> mockLoginUser(Long userId, String nickname) {
        MockedStatic<SecurityFrameworkUtils> mocked = mockStatic(SecurityFrameworkUtils.class);
        mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
        mocked.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn(nickname);
        return mocked;
    }
}
