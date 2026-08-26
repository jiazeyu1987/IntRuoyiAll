package cn.iocoder.yudao.module.srm.service.framework;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.framework.vo.SrmFrameworkAgreementRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.framework.vo.SrmFrameworkPlanAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.framework.vo.SrmFrameworkPlanSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierAccessAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierAccessSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.framework.SrmFrameworkAgreementDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.framework.SrmFrameworkAgreementLineDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import cn.iocoder.yudao.module.srm.dal.mysql.framework.SrmFrameworkAgreementLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.framework.SrmFrameworkAgreementMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmErpSupplierMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierPortalApplicationMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.framework.SrmFrameworkAgreementStatusEnum;
import cn.iocoder.yudao.module.srm.enums.framework.SrmFrameworkPlanStatusEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierPortalApplicationStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleServiceImpl;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskServiceImpl;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierPortalApplicationServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@Import({SrmCodeRuleServiceImpl.class, SrmSupplierAccessRiskServiceImpl.class,
        SrmSupplierPortalApplicationServiceImpl.class, SrmFrameworkAgreementServiceImpl.class})
class SrmFrameworkAgreementServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmCodeRuleServiceImpl codeRuleService;
    @Resource
    private SrmSupplierAccessRiskServiceImpl supplierAccessRiskService;
    @Resource
    private SrmFrameworkAgreementServiceImpl frameworkAgreementService;
    @Resource
    private SrmErpSupplierMapper erpSupplierMapper;
    @Resource
    private SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper;
    @Resource
    private SrmFrameworkAgreementMapper frameworkAgreementMapper;
    @Resource
    private SrmFrameworkAgreementLineMapper frameworkAgreementLineMapper;

    @Test
    void createAgreement_shouldKeepSupplierMaterialValidityAndSourceTraceability() {
        seedFrameworkCodeRules();
        approveSupplier(120L, "框架合格供应商");
        Long frameworkPlanId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(31L, "framework-owner")) {
            frameworkPlanId = frameworkAgreementService.createFrameworkPlan(buildFrameworkPlanSaveReq(120L));
            frameworkAgreementService.submitFrameworkPlan(frameworkPlanId);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(32L, "framework-approver")) {
            frameworkAgreementService.approveFrameworkPlan(buildAuditReq(frameworkPlanId, "框架计划通过"));
        }

        SrmFrameworkAgreementRespVO agreement;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(33L, "agreement-owner")) {
            agreement = frameworkAgreementService.createAgreement(frameworkPlanId);
        }

        SrmFrameworkAgreementDO persistedAgreement = frameworkAgreementMapper.selectById(agreement.getId());
        List<SrmFrameworkAgreementLineDO> lines = frameworkAgreementLineMapper.selectListByAgreementId(agreement.getId());

        assertEquals(frameworkPlanId, persistedAgreement.getFrameworkPlanId());
        assertEquals(120L, persistedAgreement.getSupplierId());
        assertEquals(SrmFrameworkAgreementStatusEnum.EFFECTIVE.getStatus(), persistedAgreement.getAgreementStatus());
        assertEquals(SrmProcurementMethodEnum.NON_BIDDING.getMethod(), persistedAgreement.getProcurementMethod());
        assertEquals(1, lines.size());
        assertNotNull(lines.get(0).getFrameworkPlanLineId());
        assertEquals(SrmFrameworkPlanStatusEnum.AGREEMENT_CREATED.getStatus(),
                frameworkAgreementService.getFrameworkPlan(frameworkPlanId).getPlanStatus());
    }

    @Test
    void createFrameworkPlan_shouldFailWhenSupplierIsNotEligible() {
        seedFrameworkCodeRules();
        erpSupplierMapper.insert(SrmErpSupplierDO.builder()
                .id(121L)
                .name("未准入框架供应商")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .tenantId(1L)
                .build());

        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(31L, "framework-owner")) {
            exception = assertThrows(ServiceException.class,
                    () -> frameworkAgreementService.createFrameworkPlan(buildFrameworkPlanSaveReq(121L)));
        }

        assertTrue(exception.getMessage().contains("供应商资格校验未通过"));
    }

    @Test
    void createAgreement_shouldBlockDuplicateAgreementForSameFrameworkPlan() {
        seedFrameworkCodeRules();
        approveSupplier(122L, "框架重复协议供应商");
        Long frameworkPlanId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(31L, "framework-owner")) {
            frameworkPlanId = frameworkAgreementService.createFrameworkPlan(buildFrameworkPlanSaveReq(122L));
            frameworkAgreementService.submitFrameworkPlan(frameworkPlanId);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(32L, "framework-approver")) {
            frameworkAgreementService.approveFrameworkPlan(buildAuditReq(frameworkPlanId, "通过"));
        }
        frameworkAgreementService.createAgreement(frameworkPlanId);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> frameworkAgreementService.createAgreement(frameworkPlanId));
        assertTrue(exception.getMessage().contains("重复"));
    }

    @Test
    void createFrameworkPlan_shouldFailWhenBudgetOrQuantityIsNonPositive() {
        seedFrameworkCodeRules();
        approveSupplier(123L, "框架数值校验供应商");

        SrmFrameworkPlanSaveReqVO budgetReqVO = buildFrameworkPlanSaveReq(123L);
        budgetReqVO.setBudgetAmount(new BigDecimal("-1.00"));
        ServiceException budgetException = assertThrows(ServiceException.class,
                () -> frameworkAgreementService.createFrameworkPlan(budgetReqVO));
        assertTrue(budgetException.getMessage().contains("预算"));

        SrmFrameworkPlanSaveReqVO quantityReqVO = buildFrameworkPlanSaveReq(123L);
        quantityReqVO.getLines().get(0).setQuantity(BigDecimal.ZERO);
        ServiceException quantityException = assertThrows(ServiceException.class,
                () -> frameworkAgreementService.createFrameworkPlan(quantityReqVO));
        assertTrue(quantityException.getMessage().contains("数量"));
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
            reqVO.setAccessRemark("框架供应商准入");
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
                .submittedTime(java.time.LocalDateTime.now().minusDays(1))
                .auditBy(99L)
                .auditName("portal-auditor")
                .auditTime(java.time.LocalDateTime.now())
                .auditRemark("通过")
                .build());
    }

    private void seedFrameworkCodeRules() {
        codeRuleService.createCodeRule(buildRule("FRAMEWORK_PLAN_RULE", SrmCodeRuleTargetFormEnum.FRAMEWORK_PLAN.getTargetForm(), "FP"));
        codeRuleService.createCodeRule(buildRule("FRAMEWORK_AGREEMENT_RULE", SrmCodeRuleTargetFormEnum.FRAMEWORK_AGREEMENT.getTargetForm(), "FA"));
    }

    private static SrmFrameworkPlanSaveReqVO buildFrameworkPlanSaveReq(Long supplierId) {
        SrmFrameworkPlanSaveReqVO reqVO = new SrmFrameworkPlanSaveReqVO();
        reqVO.setPlanTitle("T2 框架计划");
        reqVO.setSupplierId(supplierId);
        reqVO.setProcurementMethod(SrmProcurementMethodEnum.NON_BIDDING.getMethod());
        reqVO.setBudgetAmount(new BigDecimal("20000.00"));
        reqVO.setValidStartDate(LocalDate.now().plusDays(1));
        reqVO.setValidEndDate(LocalDate.now().plusMonths(6));
        SrmFrameworkPlanSaveReqVO.Line line = new SrmFrameworkPlanSaveReqVO.Line();
        line.setMaterialId(7001L);
        line.setMaterialCode("MAT-7001");
        line.setMaterialName("框架耗材");
        line.setQuantity(new BigDecimal("100.00"));
        line.setUnit("箱");
        line.setBudgetAmount(new BigDecimal("20000.00"));
        reqVO.setLines(List.of(line));
        return reqVO;
    }

    private static SrmFrameworkPlanAuditReqVO buildAuditReq(Long planId, String remark) {
        SrmFrameworkPlanAuditReqVO reqVO = new SrmFrameworkPlanAuditReqVO();
        reqVO.setId(planId);
        reqVO.setAuditRemark(remark);
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
