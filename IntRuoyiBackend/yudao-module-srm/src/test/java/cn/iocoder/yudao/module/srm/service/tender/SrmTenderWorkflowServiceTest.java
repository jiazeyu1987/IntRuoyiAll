package cn.iocoder.yudao.module.srm.service.tender;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.*;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierAccessAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierAccessSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.*;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmErpSupplierMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmSourcingProjectStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleServiceImpl;
import cn.iocoder.yudao.module.srm.service.procurement.SrmProcurementPlanServiceImpl;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskServiceImpl;
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
        SrmSupplierAccessRiskServiceImpl.class,
        SrmProcurementPlanServiceImpl.class,
        SrmTenderProcurementServiceImpl.class
})
class SrmTenderWorkflowServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmCodeRuleServiceImpl codeRuleService;
    @Resource
    private SrmSupplierAccessRiskServiceImpl supplierAccessRiskService;
    @Resource
    private SrmProcurementPlanServiceImpl procurementPlanService;
    @Resource
    private SrmTenderProcurementServiceImpl tenderService;
    @Resource
    private SrmErpSupplierMapper erpSupplierMapper;
    @Resource
    private SrmSourcingProjectMapper sourcingProjectMapper;

    @Test
    void tenderToWinning_shouldPersistTraceableWinningResultWithoutCreatingContract() {
        seedCodeRules();
        approveSupplier(150L, "招标供应商一");
        approveSupplier(151L, "招标供应商二");
        Long projectId = createApprovedTenderProject("T4 招标完整链路");

        SrmTenderProjectRespVO published;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(41L, "tender-owner")) {
            published = tenderService.publishProject(buildPublishReq(projectId));
        }
        assertEquals(SrmSourcingProjectStatusEnum.PUBLISHED.getStatus(), published.getProjectStatus());
        assertNotNull(published.getNotice());
        assertNotNull(published.getDocument());

        SrmTenderProjectRespVO submitted;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(42L, "bid-owner")) {
            tenderService.submitBid(buildSubmissionReq(projectId, 151L, "1210.00"));
            submitted = tenderService.submitBid(buildSubmissionReq(projectId, 150L, "1180.00"));
        }
        assertEquals(2, submitted.getSubmissions().size());

        Long expertA = createApprovedExpert("招标专家A", "MEDICAL");
        Long expertB = createApprovedExpert("招标专家B", "MEDICAL");
        Long expertC = createApprovedExpert("招标专家C", "MEDICAL");
        SrmTenderProjectRespVO committee;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(43L, "committee-owner")) {
            committee = tenderService.formCommittee(buildCommitteeReq(projectId, List.of(expertA, expertB, expertC), 3, "MEDICAL"));
        }
        assertEquals(SrmSourcingProjectStatusEnum.COMMITTEE_CONFIRMED.getStatus(), committee.getProjectStatus());
        assertEquals(3, committee.getCommitteeMembers().size());

        List<Long> submissionIds = submitted.getSubmissions().stream()
                .map(SrmTenderProjectRespVO.Submission::getId)
                .toList();
        SrmTenderProjectRespVO candidates = tenderService.createCandidates(buildCandidateReq(projectId, submissionIds));
        assertEquals(SrmSourcingProjectStatusEnum.CANDIDATE_CONFIRMED.getStatus(), candidates.getProjectStatus());
        assertEquals(2, candidates.getCandidates().size());

        Long winningCandidateId = candidates.getCandidates().get(0).getId();
        SrmTenderProjectRespVO winning;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(44L, "winning-owner")) {
            winning = tenderService.confirmWinning(buildWinningReq(projectId, winningCandidateId));
        }
        assertEquals(SrmSourcingProjectStatusEnum.WINNING_CONFIRMED.getStatus(), winning.getProjectStatus());
        assertEquals(150L, winning.getDealSupplierId());
        assertEquals(new BigDecimal("1180.00"), winning.getDealAmount());
        assertNull(winning.getContractId());
        assertNotNull(winning.getWinningResult());

        PageResult<SrmTenderProjectRespVO> page = tenderService.getProjectPage(buildPageReq());
        assertEquals(1, page.getTotal());

        SrmSourcingProjectDO project = sourcingProjectMapper.selectById(projectId);
        assertNull(project.getContractId());
        assertEquals(150L, project.getDealSupplierId());
    }

    @Test
    void submitBid_shouldFailWhenSupplierDuplicateOrAmountInvalid() {
        seedCodeRules();
        approveSupplier(152L, "招标报价门禁供应商");
        Long projectId = createApprovedTenderProject("T4 投标门禁");
        tenderService.publishProject(buildPublishReq(projectId));

        ServiceException amountException = assertThrows(ServiceException.class,
                () -> tenderService.submitBid(buildSubmissionReq(projectId, 152L, "0.00")));
        assertTrue(amountException.getMessage().contains("投标金额"));

        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(42L, "bid-owner")) {
            tenderService.submitBid(buildSubmissionReq(projectId, 152L, "1180.00"));
        }
        ServiceException duplicateException = assertThrows(ServiceException.class,
                () -> tenderService.submitBid(buildSubmissionReq(projectId, 152L, "1190.00")));
        assertTrue(duplicateException.getMessage().contains("重复"));
    }

    @Test
    void createCandidates_shouldFailWhenSubmissionIdsAreEmpty() {
        seedCodeRules();
        approveSupplier(153L, "招标候选门禁供应商");
        Long projectId = createApprovedTenderProject("T4 候选空投标门禁");
        tenderService.publishProject(buildPublishReq(projectId));
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(42L, "bid-owner")) {
            tenderService.submitBid(buildSubmissionReq(projectId, 153L, "1180.00"));
        }
        Long expertA = createApprovedExpert("候选门禁专家A", "MEDICAL");
        Long expertB = createApprovedExpert("候选门禁专家B", "MEDICAL");
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(43L, "committee-owner")) {
            tenderService.formCommittee(buildCommitteeReq(projectId, List.of(expertA, expertB), 2, "MEDICAL"));
        }

        ServiceException exception = assertThrows(ServiceException.class,
                () -> tenderService.createCandidates(buildCandidateReq(projectId, List.of())));
        assertTrue(exception.getMessage().contains("投标记录"));
        assertEquals(SrmSourcingProjectStatusEnum.COMMITTEE_CONFIRMED.getStatus(),
                sourcingProjectMapper.selectById(projectId).getProjectStatus());
    }

    private Long createApprovedTenderProject(String title) {
        Long planId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            planId = procurementPlanService.createProcurementPlan(buildPlanSaveReq(title));
            procurementPlanService.submitProcurementPlan(planId);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(22L, "approver")) {
            SrmProcurementPlanAuditReqVO auditReqVO = new SrmProcurementPlanAuditReqVO();
            auditReqVO.setId(planId);
            auditReqVO.setAuditRemark("同意招标采购");
            procurementPlanService.approveProcurementPlan(auditReqVO);
        }
        SrmProcurementPlanGenerateReqVO generateReqVO = new SrmProcurementPlanGenerateReqVO();
        generateReqVO.setId(planId);
        generateReqVO.setProjectType(SrmProcurementMethodEnum.TENDER.getMethod());
        return procurementPlanService.generateSourcingProject(generateReqVO).getId();
    }

    private void approveSupplier(Long supplierId, String supplierName) {
        erpSupplierMapper.insert(SrmErpSupplierDO.builder()
                .id(supplierId)
                .name(supplierName)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .tenantId(1L)
                .build());
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "supplier-owner")) {
            SrmSupplierAccessSaveReqVO reqVO = new SrmSupplierAccessSaveReqVO();
            reqVO.setSupplierId(supplierId);
            reqVO.setAccessRemark("招标供应商准入");
            accessId = supplierAccessRiskService.createSupplierAccess(reqVO);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "supplier-auditor")) {
            SrmSupplierAccessAuditReqVO reqVO = new SrmSupplierAccessAuditReqVO();
            reqVO.setId(accessId);
            reqVO.setAuditRemark("准入通过");
            supplierAccessRiskService.approveSupplierAccess(reqVO);
        }
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
        codeRuleService.createCodeRule(buildRule("T4_PLAN_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PP"));
        codeRuleService.createCodeRule(buildRule("T4_PLAN_LINE_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN_LINE.getTargetForm(), "PPL"));
        codeRuleService.createCodeRule(buildRule("T4_TENDER_RULE", SrmCodeRuleTargetFormEnum.TENDER_PROJECT.getTargetForm(), "TP"));
        codeRuleService.createCodeRule(buildRule("T4_EXPERT_APP_RULE", SrmCodeRuleTargetFormEnum.EXPERT_DRAW_APPLICATION.getTargetForm(), "EA"));
    }

    private static SrmTenderPublishReqVO buildPublishReq(Long projectId) {
        SrmTenderPublishReqVO reqVO = new SrmTenderPublishReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setNoticeTitle("招标公告");
        reqVO.setNoticeAttachmentUrl("http://127.0.0.1:9000/yudao/srm/tender/notice.pdf");
        reqVO.setDocumentName("招标文件");
        reqVO.setDocumentAttachmentUrl("http://127.0.0.1:9000/yudao/srm/tender/document.pdf");
        reqVO.setSubmissionStartTime(LocalDateTime.now().minusMinutes(5));
        reqVO.setSubmissionEndTime(LocalDateTime.now().plusDays(2));
        return reqVO;
    }

    private static SrmTenderSubmissionReqVO buildSubmissionReq(Long projectId, Long supplierId, String amount) {
        SrmTenderSubmissionReqVO reqVO = new SrmTenderSubmissionReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setSupplierId(supplierId);
        reqVO.setBidAmount(new BigDecimal(amount));
        reqVO.setAttachmentUrl("http://127.0.0.1:9000/yudao/srm/tender/bid.pdf");
        return reqVO;
    }

    private static SrmTenderExpertSaveReqVO buildExpertReq(String expertName, String specialtyType) {
        SrmTenderExpertSaveReqVO reqVO = new SrmTenderExpertSaveReqVO();
        reqVO.setExpertName(expertName);
        reqVO.setSpecialtyType(specialtyType);
        return reqVO;
    }

    private static SrmTenderCommitteeReqVO buildCommitteeReq(Long projectId, List<Long> expertIds, int count, String specialtyType) {
        SrmTenderCommitteeReqVO reqVO = new SrmTenderCommitteeReqVO();
        reqVO.setProjectId(projectId);
        reqVO.setApplicationMethod("DESIGNATE");
        reqVO.setRequiredSpecialtyType(specialtyType);
        reqVO.setRequiredExpertCount(count);
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
        reqVO.setWinningRemark("确认中标");
        return reqVO;
    }

    private static SrmTenderProjectPageReqVO buildPageReq() {
        SrmTenderProjectPageReqVO reqVO = new SrmTenderProjectPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        return reqVO;
    }

    private static SrmProcurementPlanSaveReqVO buildPlanSaveReq(String title) {
        SrmProcurementPlanSaveReqVO reqVO = new SrmProcurementPlanSaveReqVO();
        reqVO.setPlanTitle(title);
        reqVO.setProcurementMethod(SrmProcurementMethodEnum.TENDER.getMethod());
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
