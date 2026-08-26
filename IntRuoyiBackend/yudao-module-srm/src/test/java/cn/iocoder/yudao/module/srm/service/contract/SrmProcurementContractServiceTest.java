package cn.iocoder.yudao.module.srm.service.contract;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractCancelReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractSaveReqVO;
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
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.SrmTenderCandidateReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.SrmTenderCommitteeReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.SrmTenderExpertAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.SrmTenderExpertSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.SrmTenderProjectRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.SrmTenderPublishReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.SrmTenderSubmissionReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.SrmTenderWinningReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import cn.iocoder.yudao.module.srm.dal.mysql.contract.SrmProcurementContractMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.contract.SrmProcurementContractPaymentMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmErpSupplierMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierPortalApplicationMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.contract.SrmProcurementContractSourceTypeEnum;
import cn.iocoder.yudao.module.srm.enums.contract.SrmProcurementContractStatusEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmSourcingProjectStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierPortalApplicationStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleServiceImpl;
import cn.iocoder.yudao.module.srm.service.nonbidding.SrmNonBiddingProcurementServiceImpl;
import cn.iocoder.yudao.module.srm.service.procurement.SrmProcurementPlanServiceImpl;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskServiceImpl;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierPortalApplicationServiceImpl;
import cn.iocoder.yudao.module.srm.service.tender.SrmTenderProcurementServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@Import({
        SrmCodeRuleServiceImpl.class,
        SrmSupplierAccessRiskServiceImpl.class,
        SrmSupplierPortalApplicationServiceImpl.class,
        SrmProcurementPlanServiceImpl.class,
        SrmNonBiddingProcurementServiceImpl.class,
        SrmTenderProcurementServiceImpl.class,
        SrmProcurementContractServiceImpl.class
})
class SrmProcurementContractServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmCodeRuleServiceImpl codeRuleService;
    @Resource
    private SrmSupplierAccessRiskServiceImpl supplierAccessRiskService;
    @Resource
    private SrmProcurementPlanServiceImpl procurementPlanService;
    @Resource
    private SrmNonBiddingProcurementServiceImpl nonBiddingService;
    @Resource
    private SrmTenderProcurementServiceImpl tenderService;
    @Resource
    private SrmProcurementContractServiceImpl contractService;
    @Resource
    private SrmErpSupplierMapper erpSupplierMapper;
    @Resource
    private SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper;
    @Resource
    private SrmSourcingProjectMapper sourcingProjectMapper;
    @Resource
    private SrmSourcingProjectLineMapper sourcingProjectLineMapper;
    @Resource
    private SrmProcurementContractMapper contractMapper;
    @Resource
    private SrmProcurementContractPaymentMapper paymentMapper;

    @Test
    void createNonBiddingSourceContract_shouldSaveAtomicallyAndRemoveContractableSource() {
        seedCodeRules();
        approveSupplier(170L, "合同非招标供应商");
        Long projectId = createDealtNonBiddingProject(170L);

        SrmProcurementContractRespVO contract;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(61L, "contract-owner")) {
            contract = contractService.createContract(buildContractReq(
                    SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId, "NB合同"));
        }

        assertNotNull(contract.getId());
        assertEquals(SrmProcurementContractStatusEnum.EFFECTIVE.getStatus(), contract.getContractStatus());
        assertEquals(170L, contract.getSupplierId());
        assertEquals(1, contract.getPayments().size());
        assertEquals(1, contract.getSignings().size());
        assertEquals(1, contract.getAttachments().size());

        SrmSourcingProjectDO project = sourcingProjectMapper.selectById(projectId);
        assertEquals(contract.getId(), project.getContractId());
        assertEquals(SrmSourcingProjectStatusEnum.CONTRACT_CREATED.getStatus(), project.getProjectStatus());
        assertEquals(0, nonBiddingService.getContractableProjectPage(buildNonBiddingPageReq()).getTotal());
        assertEquals(1, paymentMapper.selectListByContractId(contract.getId()).size());
    }

    @Test
    void createTenderSourceContract_shouldWriteBackWinningProjectAndBlockDuplicate() {
        seedCodeRules();
        approveSupplier(171L, "合同招标供应商");
        Long projectId = createWinningTenderProject(171L);

        Long contractId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(62L, "tender-contract-owner")) {
            contractId = contractService.createContract(buildContractReq(
                    SrmProcurementContractSourceTypeEnum.TENDER.getSourceType(), projectId, "TP合同")).getId();
        }

        SrmSourcingProjectDO project = sourcingProjectMapper.selectById(projectId);
        assertEquals(contractId, project.getContractId());
        assertEquals(SrmSourcingProjectStatusEnum.CONTRACT_CREATED.getStatus(), project.getProjectStatus());

        ServiceException duplicateException = assertThrows(ServiceException.class,
                () -> contractService.createContract(buildContractReq(
                        SrmProcurementContractSourceTypeEnum.TENDER.getSourceType(), projectId, "TP重复合同")));
        assertTrue(duplicateException.getMessage().contains("已创建合同")
                || duplicateException.getMessage().contains("不允许创建合同"));
        assertEquals(1, contractMapper.selectListBySource(
                SrmProcurementContractSourceTypeEnum.TENDER.getSourceType(), projectId).size());
    }

    @Test
    void cancelAndDeleteContract_shouldRestoreSourceContractability() {
        seedCodeRules();
        approveSupplier(172L, "合同回写供应商");
        Long projectId = createDealtNonBiddingProject(172L);
        Long contractId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(61L, "contract-owner")) {
            contractId = contractService.createContract(buildContractReq(
                    SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId, "作废合同")).getId();
        }

        SrmProcurementContractCancelReqVO cancelReqVO = new SrmProcurementContractCancelReqVO();
        cancelReqVO.setId(contractId);
        cancelReqVO.setCancelReason("合同作废回写来源");
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(63L, "cancel-owner")) {
            contractService.cancelContract(cancelReqVO);
        }

        SrmProcurementContractDO cancelled = contractMapper.selectById(contractId);
        assertEquals(SrmProcurementContractStatusEnum.CANCELLED.getStatus(), cancelled.getContractStatus());
        SrmSourcingProjectDO restored = sourcingProjectMapper.selectById(projectId);
        assertNull(restored.getContractId());
        assertEquals(SrmSourcingProjectStatusEnum.DEAL_CONFIRMED.getStatus(), restored.getProjectStatus());
        assertEquals(1, nonBiddingService.getContractableProjectPage(buildNonBiddingPageReq()).getTotal());

        Long secondContractId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(64L, "second-contract-owner")) {
            secondContractId = contractService.createContract(buildContractReq(
                    SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId, "删除合同")).getId();
        }
        contractService.deleteContract(secondContractId);
        SrmSourcingProjectDO deletedRestored = sourcingProjectMapper.selectById(projectId);
        assertNull(deletedRestored.getContractId());
        assertEquals(SrmSourcingProjectStatusEnum.DEAL_CONFIRMED.getStatus(), deletedRestored.getProjectStatus());
        assertEquals(1, nonBiddingService.getContractableProjectPage(buildNonBiddingPageReq()).getTotal());
    }

    @Test
    void createContract_shouldFailWhenPaymentSigningOrAttachmentIsMissing() {
        seedCodeRules();
        approveSupplier(173L, "合同附件门禁供应商");
        Long projectId = createDealtNonBiddingProject(173L);

        SrmProcurementContractSaveReqVO missingPayment = buildContractReq(
                SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId, "缺付款合同");
        missingPayment.setPayments(List.of());
        ServiceException paymentException = assertThrows(ServiceException.class,
                () -> contractService.createContract(missingPayment));
        assertTrue(paymentException.getMessage().contains("付款"));

        SrmProcurementContractSaveReqVO missingSigning = buildContractReq(
                SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId, "缺签署合同");
        missingSigning.setSignings(List.of());
        ServiceException signingException = assertThrows(ServiceException.class,
                () -> contractService.createContract(missingSigning));
        assertTrue(signingException.getMessage().contains("签署"));

        SrmProcurementContractSaveReqVO missingAttachment = buildContractReq(
                SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId, "缺附件合同");
        missingAttachment.setAttachments(List.of());
        ServiceException attachmentException = assertThrows(ServiceException.class,
                () -> contractService.createContract(missingAttachment));
        assertTrue(attachmentException.getMessage().contains("附件"));

        assertNull(sourcingProjectMapper.selectById(projectId).getContractId());
        assertEquals(0, contractMapper.selectListBySource(
                SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId).size());
    }

    @Test
    void createContract_shouldFailWhenHeaderFieldsAreInvalid() {
        seedCodeRules();
        approveSupplier(175L, "合同基础门禁供应商");
        Long projectId = createDealtNonBiddingProject(175L);

        SrmProcurementContractSaveReqVO blankTitle = buildContractReq(
                SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId, "基础合同");
        blankTitle.setContractTitle(" ");
        ServiceException titleException = assertThrows(ServiceException.class,
                () -> contractService.createContract(blankTitle));
        assertTrue(titleException.getMessage().contains("基础信息"));

        SrmProcurementContractSaveReqVO blankCurrency = buildContractReq(
                SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId, "基础合同");
        blankCurrency.setCurrency(" ");
        ServiceException currencyException = assertThrows(ServiceException.class,
                () -> contractService.createContract(blankCurrency));
        assertTrue(currencyException.getMessage().contains("基础信息"));

        SrmProcurementContractSaveReqVO missingSourceId = buildContractReq(
                SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId, "基础合同");
        missingSourceId.setSourceId(null);
        ServiceException sourceException = assertThrows(ServiceException.class,
                () -> contractService.createContract(missingSourceId));
        assertTrue(sourceException.getMessage().contains("基础信息"));

        assertNull(sourcingProjectMapper.selectById(projectId).getContractId());
        assertEquals(0, contractMapper.selectListBySource(
                SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId).size());
    }

    @Test
    void getContractPage_shouldReturnCreatedContracts() {
        seedCodeRules();
        approveSupplier(174L, "合同分页供应商");
        Long projectId = createDealtNonBiddingProject(174L);
        Long contractId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(61L, "contract-owner")) {
            contractId = contractService.createContract(buildContractReq(
                    SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType(), projectId, "分页合同")).getId();
        }

        SrmProcurementContractPageReqVO pageReqVO = new SrmProcurementContractPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        PageResult<SrmProcurementContractRespVO> page = contractService.getContractPage(pageReqVO);

        assertEquals(1, page.getTotal());
        assertEquals(contractId, page.getList().get(0).getId());
    }

    private Long createDealtNonBiddingProject(Long supplierId) {
        Long projectId = createApprovedProject("T5 非招标合同来源", SrmProcurementMethodEnum.NON_BIDDING.getMethod());
        nonBiddingService.publishProject(buildPublishReq(projectId, List.of(supplierId)));
        SrmNonBiddingProjectRespVO quoted;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(52L, "quote-owner")) {
            quoted = nonBiddingService.submitQuote(buildQuoteReq(projectId, supplierId, firstProjectLineId(projectId)));
        }
        SrmNonBiddingDealReqVO dealReqVO = new SrmNonBiddingDealReqVO();
        dealReqVO.setProjectId(projectId);
        dealReqVO.setQuoteId(quoted.getQuotes().get(0).getId());
        dealReqVO.setDealRemark("T5 确认成交");
        nonBiddingService.confirmDeal(dealReqVO);
        return projectId;
    }

    private Long createWinningTenderProject(Long supplierId) {
        Long projectId = createApprovedProject("T5 招标合同来源", SrmProcurementMethodEnum.TENDER.getMethod());
        tenderService.publishProject(buildTenderPublishReq(projectId));
        SrmTenderProjectRespVO submitted;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(53L, "bid-owner")) {
            submitted = tenderService.submitBid(buildTenderSubmissionReq(projectId, supplierId));
        }
        Long expertA = createApprovedExpert("T5专家A", "MEDICAL");
        Long expertB = createApprovedExpert("T5专家B", "MEDICAL");
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(54L, "committee-owner")) {
            tenderService.formCommittee(buildCommitteeReq(projectId, List.of(expertA, expertB)));
        }
        SrmTenderProjectRespVO candidates = tenderService.createCandidates(buildCandidateReq(
                projectId,
                submitted.getSubmissions().stream().map(SrmTenderProjectRespVO.Submission::getId).toList()));
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(55L, "winning-owner")) {
            tenderService.confirmWinning(buildWinningReq(projectId, candidates.getCandidates().get(0).getId()));
        }
        return projectId;
    }

    private Long createApprovedProject(String title, String method) {
        Long planId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            planId = procurementPlanService.createProcurementPlan(buildPlanSaveReq(title, method));
            procurementPlanService.submitProcurementPlan(planId);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(22L, "approver")) {
            SrmProcurementPlanAuditReqVO auditReqVO = new SrmProcurementPlanAuditReqVO();
            auditReqVO.setId(planId);
            auditReqVO.setAuditRemark("同意采购");
            procurementPlanService.approveProcurementPlan(auditReqVO);
        }
        SrmProcurementPlanGenerateReqVO generateReqVO = new SrmProcurementPlanGenerateReqVO();
        generateReqVO.setId(planId);
        generateReqVO.setProjectType(method);
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
            reqVO.setAccessRemark("合同供应商准入");
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

    private Long createApprovedExpert(String expertName, String specialtyType) {
        Long expertId = tenderService.createExpert(buildExpertReq(expertName, specialtyType));
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(31L, "expert-auditor")) {
            SrmTenderExpertAuditReqVO reqVO = new SrmTenderExpertAuditReqVO();
            reqVO.setId(expertId);
            reqVO.setAuditRemark("专家资质通过");
            tenderService.approveExpert(reqVO);
        }
        return expertId;
    }

    private void seedCodeRules() {
        codeRuleService.createCodeRule(buildRule("T5_PLAN_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PP"));
        codeRuleService.createCodeRule(buildRule("T5_PLAN_LINE_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN_LINE.getTargetForm(), "PPL"));
        codeRuleService.createCodeRule(buildRule("T5_NON_BIDDING_RULE", SrmCodeRuleTargetFormEnum.NON_TENDER_PROJECT.getTargetForm(), "NB"));
        codeRuleService.createCodeRule(buildRule("T5_TENDER_RULE", SrmCodeRuleTargetFormEnum.TENDER_PROJECT.getTargetForm(), "TP"));
        codeRuleService.createCodeRule(buildRule("T5_CONTRACT_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_CONTRACT.getTargetForm(), "PC"));
        codeRuleService.createCodeRule(buildRule("T5_EXPERT_APP_RULE", SrmCodeRuleTargetFormEnum.EXPERT_DRAW_APPLICATION.getTargetForm(), "EA"));
    }

    private Long firstProjectLineId(Long projectId) {
        return sourcingProjectLineMapper.selectListByProjectId(projectId).get(0).getId();
    }

    private static SrmNonBiddingProjectPageReqVO buildNonBiddingPageReq() {
        SrmNonBiddingProjectPageReqVO reqVO = new SrmNonBiddingProjectPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        return reqVO;
    }

    private static SrmNonBiddingPublishReqVO buildPublishReq(Long projectId, List<Long> supplierIds) {
        SrmNonBiddingPublishReqVO reqVO = new SrmNonBiddingPublishReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setQuoteMode("INVITE");
        reqVO.setQuoteStartTime(LocalDateTime.now().minusMinutes(5));
        reqVO.setQuoteEndTime(LocalDateTime.now().plusDays(2));
        reqVO.setAttachmentUrl("http://127.0.0.1:9000/yudao/srm/contract/non-bidding-publish.pdf");
        reqVO.setSupplierIds(supplierIds);
        return reqVO;
    }

    private static SrmNonBiddingQuoteReqVO buildQuoteReq(Long projectId, Long supplierId, Long projectLineId) {
        SrmNonBiddingQuoteReqVO reqVO = new SrmNonBiddingQuoteReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setSupplierId(supplierId);
        reqVO.setQuoteAmount(new BigDecimal("1180.00"));
        reqVO.setAttachmentUrl("http://127.0.0.1:9000/yudao/srm/contract/non-bidding-quote.pdf");
        SrmNonBiddingQuoteReqVO.Line line = new SrmNonBiddingQuoteReqVO.Line();
        line.setProjectLineId(projectLineId);
        line.setUnitPrice(new BigDecimal("118.00"));
        line.setLineAmount(new BigDecimal("1180.00"));
        reqVO.setLines(List.of(line));
        return reqVO;
    }

    private static SrmTenderPublishReqVO buildTenderPublishReq(Long projectId) {
        SrmTenderPublishReqVO reqVO = new SrmTenderPublishReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setNoticeTitle("T5 招标公告");
        reqVO.setNoticeAttachmentUrl("http://127.0.0.1:9000/yudao/srm/contract/tender-notice.pdf");
        reqVO.setDocumentName("T5 招标文件");
        reqVO.setDocumentAttachmentUrl("http://127.0.0.1:9000/yudao/srm/contract/tender-document.pdf");
        reqVO.setSubmissionStartTime(LocalDateTime.now().minusMinutes(5));
        reqVO.setSubmissionEndTime(LocalDateTime.now().plusDays(2));
        return reqVO;
    }

    private static SrmTenderSubmissionReqVO buildTenderSubmissionReq(Long projectId, Long supplierId) {
        SrmTenderSubmissionReqVO reqVO = new SrmTenderSubmissionReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setSupplierId(supplierId);
        reqVO.setBidAmount(new BigDecimal("2180.00"));
        reqVO.setAttachmentUrl("http://127.0.0.1:9000/yudao/srm/contract/tender-bid.pdf");
        return reqVO;
    }

    private static SrmTenderExpertSaveReqVO buildExpertReq(String expertName, String specialtyType) {
        SrmTenderExpertSaveReqVO reqVO = new SrmTenderExpertSaveReqVO();
        reqVO.setExpertName(expertName);
        reqVO.setSpecialtyType(specialtyType);
        return reqVO;
    }

    private static SrmTenderCommitteeReqVO buildCommitteeReq(Long projectId, List<Long> expertIds) {
        SrmTenderCommitteeReqVO reqVO = new SrmTenderCommitteeReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setApplicationMethod("DIRECT");
        reqVO.setRequiredSpecialtyType("MEDICAL");
        reqVO.setRequiredExpertCount(expertIds.size());
        reqVO.setExpertIds(expertIds);
        return reqVO;
    }

    private static SrmTenderCandidateReqVO buildCandidateReq(Long projectId, List<Long> submissionIds) {
        SrmTenderCandidateReqVO reqVO = new SrmTenderCandidateReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setSubmissionIds(submissionIds);
        return reqVO;
    }

    private static SrmTenderWinningReqVO buildWinningReq(Long projectId, Long candidateId) {
        SrmTenderWinningReqVO reqVO = new SrmTenderWinningReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setCandidateId(candidateId);
        reqVO.setWinningRemark("T5 确认中标");
        return reqVO;
    }

    private static SrmProcurementContractSaveReqVO buildContractReq(String sourceType, Long sourceId, String title) {
        SrmProcurementContractSaveReqVO reqVO = new SrmProcurementContractSaveReqVO();
        reqVO.setSourceType(sourceType);
        reqVO.setSourceId(sourceId);
        reqVO.setContractTitle(title);
        reqVO.setContractAmount(new BigDecimal("1180.00"));
        reqVO.setCurrency("CNY");
        reqVO.setEffectiveDate(LocalDate.now());
        reqVO.setExpireDate(LocalDate.now().plusMonths(6));

        SrmProcurementContractSaveReqVO.Payment payment = new SrmProcurementContractSaveReqVO.Payment();
        payment.setPaymentStage("预付款");
        payment.setPaymentRatio(new BigDecimal("30.00"));
        payment.setPaymentAmount(new BigDecimal("354.00"));
        payment.setDueDate(LocalDate.now().plusDays(10));
        payment.setPaymentRemark("合同签署后支付");
        reqVO.setPayments(List.of(payment));

        SrmProcurementContractSaveReqVO.Signing signing = new SrmProcurementContractSaveReqVO.Signing();
        signing.setSigningParty("采购方");
        signing.setSignerName("采购负责人");
        signing.setSigningDate(LocalDate.now());
        signing.setSigningRemark("线下签署");
        reqVO.setSignings(List.of(signing));

        SrmProcurementContractSaveReqVO.Attachment attachment = new SrmProcurementContractSaveReqVO.Attachment();
        attachment.setAttachmentName("合同正文");
        attachment.setAttachmentUrl("http://127.0.0.1:9000/yudao/srm/contract/body.pdf");
        attachment.setAttachmentType("CONTRACT_FILE");
        reqVO.setAttachments(List.of(attachment));
        return reqVO;
    }

    private static SrmProcurementPlanSaveReqVO buildPlanSaveReq(String title, String method) {
        SrmProcurementPlanSaveReqVO reqVO = new SrmProcurementPlanSaveReqVO();
        reqVO.setPlanTitle(title);
        reqVO.setProcurementMethod(method);
        reqVO.setExpectedAmount(new BigDecimal("2280.50"));
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
