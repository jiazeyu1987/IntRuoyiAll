package cn.iocoder.yudao.module.srm.service.paymentexecution;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo.SrmOutsourceExecutionCreateReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo.SrmOutsourceExecutionInspectReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo.SrmOutsourceExecutionIssueReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo.SrmOutsourceExecutionProgressReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo.SrmOutsourceExecutionReceiveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo.SrmOutsourceExecutionReconcileReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo.SrmPaymentExecutionApproveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo.SrmPaymentExecutionCreateReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo.SrmPaymentExecutionRejectReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo.SrmPaymentExecutionRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo.SrmPaymentExecutionSubmitReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderConfirmReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderCreateReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractPaymentDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution.SrmOutsourceExecutionDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.paymentexecution.SrmPaymentExecutionDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderLineDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierAccessDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import cn.iocoder.yudao.module.srm.dal.mysql.contract.SrmProcurementContractMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.contract.SrmProcurementContractPaymentMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.outsourceexecution.SrmOutsourceExecutionMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.paymentexecution.SrmPaymentExecutionMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder.SrmPurchaseOrderLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmErpSupplierMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierAccessMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierPortalApplicationMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.contract.SrmProcurementContractStatusEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierAccessStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierOnboardingStageStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleServiceImpl;
import cn.iocoder.yudao.module.srm.service.outsourceexecution.SrmOutsourceExecutionService;
import cn.iocoder.yudao.module.srm.service.outsourceexecution.SrmOutsourceExecutionServiceImpl;
import cn.iocoder.yudao.module.srm.service.procurement.SrmProcurementPlanServiceImpl;
import cn.iocoder.yudao.module.srm.service.purchaseorder.SrmPurchaseOrderServiceImpl;
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
        SrmProcurementPlanServiceImpl.class,
        SrmSupplierPortalApplicationServiceImpl.class,
        SrmSupplierAccessRiskServiceImpl.class,
        SrmPurchaseOrderServiceImpl.class,
        SrmOutsourceExecutionServiceImpl.class,
        SrmPaymentExecutionServiceImpl.class
})
class SrmPaymentExecutionServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmCodeRuleServiceImpl codeRuleService;
    @Resource
    private SrmProcurementPlanServiceImpl procurementPlanService;
    @Resource
    private SrmPurchaseOrderServiceImpl purchaseOrderService;
    @Resource
    private SrmOutsourceExecutionService outsourceExecutionService;
    @Resource
    private SrmPaymentExecutionService paymentExecutionService;
    @Resource
    private SrmPurchaseOrderLineMapper purchaseOrderLineMapper;
    @Resource
    private SrmPaymentExecutionMapper paymentExecutionMapper;
    @Resource
    private SrmOutsourceExecutionMapper outsourceExecutionMapper;
    @Resource
    private SrmProcurementContractMapper contractMapper;
    @Resource
    private SrmProcurementContractPaymentMapper contractPaymentMapper;
    @Resource
    private SrmErpSupplierMapper erpSupplierMapper;
    @Resource
    private SrmSupplierAccessMapper supplierAccessMapper;
    @Resource
    private SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper;

    @Test
    void createFromReconciliation_shouldRequireEffectiveContract() {
        seedBaseData();
        Long executionId = seedReconciledExecution();

        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            exception = assertThrows(ServiceException.class,
                    () -> paymentExecutionService.createFromReconciliation(buildCreateReq(executionId, 9999L)));
        }
        assertTrue(exception.getMessage().contains("采购合同"));
    }

    @Test
    void lifecycle_shouldCarryContractPaymentTermsAndPushStates() {
        seedBaseData();
        Long executionId = seedReconciledExecution();
        Long contractId = seedEffectiveContract(108L);

        Long paymentId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            paymentId = paymentExecutionService.createFromReconciliation(buildCreateReq(executionId, contractId));
            paymentExecutionService.submit(buildSubmitReq(paymentId));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(22L, "approver")) {
            paymentExecutionService.approve(buildApproveReq(paymentId));
        }

        SrmPaymentExecutionRespVO respVO = paymentExecutionService.getPaymentExecution(paymentId);
        assertEquals("APPROVED", respVO.getPaymentStatus());
        assertEquals(new BigDecimal("60.00"), respVO.getPaymentRatio());
        assertEquals(new BigDecimal("600.00"), respVO.getApplyAmount());
        assertTrue(respVO.getPaymentTermSummary().contains("首付款"));

        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(23L, "finance-user")) {
            paymentExecutionService.financePush(buildRejectReq(paymentId));
        }
        SrmPaymentExecutionDO payment = paymentExecutionMapper.selectById(paymentId);
        assertEquals("PUSH_FAILED", payment.getPaymentStatus());
    }

    private void seedBaseData() {
        seedCodeRules();
        seedEligibleSupplier(108L, 201L, "SRM Phase 5 供应商");
    }

    private Long seedReconciledExecution() {
        Long planId = seedApprovedPlan("Phase 5 对账前置订单");
        Long orderId = purchaseOrderService.createFromPlan(buildPurchaseOrderCreateReq(planId));
        Long orderLineId = purchaseOrderLineMapper.selectListByOrderId(orderId).get(0).getId();
        SrmPurchaseOrderConfirmReqVO confirmReqVO = new SrmPurchaseOrderConfirmReqVO();
        confirmReqVO.setId(orderId);
        confirmReqVO.setConfirmRemark("用于付款执行");
        SrmPurchaseOrderConfirmReqVO.Line line = new SrmPurchaseOrderConfirmReqVO.Line();
        line.setOrderLineId(orderLineId);
        line.setConfirmedQuantity(new BigDecimal("10.00"));
        line.setConfirmedDeliveryDate(LocalDate.now().plusDays(7));
        line.setSupplierRemark("按期交付");
        confirmReqVO.setLines(List.of(line));
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(201L, "portal-user")) {
            purchaseOrderService.confirmMyPurchaseOrder(confirmReqVO);
        }

        Long executionId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            executionId = outsourceExecutionService.createFromPurchaseOrder(buildCreateReq(orderId));
            outsourceExecutionService.issue(buildIssueReq(executionId));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(201L, "portal-user")) {
            outsourceExecutionService.updateProgress(buildProgressReq(executionId));
            outsourceExecutionService.receive(buildReceiveReq(executionId));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(22L, "quality-user")) {
            outsourceExecutionService.inspect(buildInspectReq(executionId));
            outsourceExecutionService.reconcile(buildReconcileReq(executionId));
        }
        return executionId;
    }

    private Long seedEffectiveContract(Long supplierId) {
        SrmProcurementContractDO contract = SrmProcurementContractDO.builder()
                .contractNo("CT-20260621-0001")
                .contractTitle("Phase 5 付款执行合同")
                .sourceType("NON_BIDDING")
                .sourceId(7001L)
                .sourceNo("NP-7001")
                .supplierId(supplierId)
                .supplierName("SRM Phase 5 供应商")
                .contractAmount(new BigDecimal("1000.00"))
                .currency("CNY")
                .effectiveDate(LocalDate.now().minusDays(1))
                .expireDate(LocalDate.now().plusDays(90))
                .contractStatus(SrmProcurementContractStatusEnum.EFFECTIVE.getStatus())
                .createdBy(21L)
                .createdName("planner")
                .createdTime(LocalDateTime.now().minusDays(1))
                .build();
        contract.setTenantId(1L);
        contractMapper.insert(contract);

        SrmProcurementContractPaymentDO payment = SrmProcurementContractPaymentDO.builder()
                .contractId(contract.getId())
                .paymentStage("首付款")
                .paymentRatio(new BigDecimal("60.00"))
                .paymentAmount(new BigDecimal("600.00"))
                .dueDate(LocalDate.now().plusDays(15))
                .paymentRemark("结算后支付")
                .build();
        payment.setTenantId(1L);
        contractPaymentMapper.insert(payment);
        return contract.getId();
    }

    private SrmPaymentExecutionCreateReqVO buildCreateReq(Long executionId, Long contractId) {
        SrmPaymentExecutionCreateReqVO reqVO = new SrmPaymentExecutionCreateReqVO();
        reqVO.setReconciliationId(executionId);
        reqVO.setContractId(contractId);
        reqVO.setPaymentRemark("测试租户受控模拟链路");
        return reqVO;
    }

    private SrmPaymentExecutionSubmitReqVO buildSubmitReq(Long paymentId) {
        SrmPaymentExecutionSubmitReqVO reqVO = new SrmPaymentExecutionSubmitReqVO();
        reqVO.setId(paymentId);
        reqVO.setSubmitRemark("提交付款申请");
        return reqVO;
    }

    private SrmPaymentExecutionApproveReqVO buildApproveReq(Long paymentId) {
        SrmPaymentExecutionApproveReqVO reqVO = new SrmPaymentExecutionApproveReqVO();
        reqVO.setId(paymentId);
        reqVO.setApproveRemark("审批通过");
        return reqVO;
    }

    private SrmPaymentExecutionRejectReqVO buildRejectReq(Long paymentId) {
        SrmPaymentExecutionRejectReqVO reqVO = new SrmPaymentExecutionRejectReqVO();
        reqVO.setId(paymentId);
        reqVO.setPushRemark("模拟财务回执");
        reqVO.setPushSuccess(false);
        return reqVO;
    }

    private void seedCodeRules() {
        codeRuleService.createCodeRule(buildRule("PLAN_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PL"));
        codeRuleService.createCodeRule(buildRule("PLAN_LINE_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN_LINE.getTargetForm(), "PLI"));
        codeRuleService.createCodeRule(buildRule("PO_RULE", SrmCodeRuleTargetFormEnum.PURCHASE_ORDER.getTargetForm(), "PO"));
        codeRuleService.createCodeRule(buildRule("PO_LINE_RULE", SrmCodeRuleTargetFormEnum.PURCHASE_ORDER_LINE.getTargetForm(), "POL"));
        codeRuleService.createCodeRule(buildRule("OUTSOURCE_EXECUTION_RULE", "OUTSOURCE_EXECUTION", "OE"));
        codeRuleService.createCodeRule(buildRule("OUTSOURCE_EVENT_RULE", "OUTSOURCE_EXECUTION_EVENT", "OEV"));
        codeRuleService.createCodeRule(buildRule("RECONCILIATION_RULE", "OUTSOURCE_RECONCILIATION", "OR"));
        codeRuleService.createCodeRule(buildRule("PAYMENT_EXECUTION_RULE", "PAYMENT_EXECUTION", "PE"));
        codeRuleService.createCodeRule(buildRule("PAYMENT_EVENT_RULE", "PAYMENT_EXECUTION_EVENT", "PEV"));
    }

    private Long seedApprovedPlan(String title) {
        Long planId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            planId = procurementPlanService.createProcurementPlan(buildPlanSaveReq(title));
            procurementPlanService.submitProcurementPlan(planId);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(22L, "approver")) {
            procurementPlanService.approveProcurementPlan(buildAuditReq(planId, "同意"));
        }
        return planId;
    }

    private void seedEligibleSupplier(Long supplierId, Long portalUserId, String supplierName) {
        erpSupplierMapper.insert(SrmErpSupplierDO.builder()
                .id(supplierId)
                .tenantId(1L)
                .name(supplierName)
                .contact("张三")
                .mobile("13800138000")
                .email("portal@example.com")
                .taxNo("TAX-" + supplierId)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
        SrmSupplierAccessDO supplierAccess = SrmSupplierAccessDO.builder()
                .id(supplierId)
                .supplierId(supplierId)
                .accessStatus(SrmSupplierAccessStatusEnum.APPROVED.getStatus())
                .enabled(true)
                .portalContactName("张三")
                .portalContactPhone("13800138000")
                .qualificationExpireDate(LocalDate.now().plusDays(30))
                .sampleTestStatus(SrmSupplierOnboardingStageStatusEnum.PASSED.getStatus())
                .trialOrderStatus(SrmSupplierOnboardingStageStatusEnum.PASSED.getStatus())
                .submittedBy(portalUserId)
                .submittedName("portal-user")
                .build();
        supplierAccess.setTenantId(1L);
        supplierAccessMapper.insert(supplierAccess);
        supplierPortalApplicationMapper.insert(SrmSupplierPortalApplicationDO.builder()
                .id(supplierId)
                .tenantId(1L)
                .userId(portalUserId)
                .supplierId(supplierId)
                .companyName(supplierName)
                .contactName("张三")
                .contactPhone("13800138000")
                .contactEmail("portal@example.com")
                .qualificationAttachmentUrls("http://files.local/portal.pdf")
                .qualificationExpireDate(LocalDate.now().plusDays(30))
                .bankName("招商银行")
                .bankAccount("6222021234567890")
                .applicationStatus("APPROVED")
                .submitterName("portal-user")
                .submittedTime(LocalDateTime.now().minusDays(1))
                .auditName("auditor")
                .auditTime(LocalDateTime.now().minusHours(1))
                .build());
    }

    private static SrmProcurementPlanSaveReqVO buildPlanSaveReq(String title) {
        SrmProcurementPlanSaveReqVO reqVO = new SrmProcurementPlanSaveReqVO();
        reqVO.setPlanTitle(title);
        reqVO.setProcurementMethod(SrmProcurementMethodEnum.NON_BIDDING.getMethod());
        reqVO.setExpectedAmount(new BigDecimal("1000.00"));
        reqVO.setRemark("Phase 5 测试");
        SrmProcurementPlanSaveReqVO.Line line = new SrmProcurementPlanSaveReqVO.Line();
        line.setMaterialId(5001L);
        line.setMaterialCode("MAT-5001");
        line.setMaterialName("付款执行测试物料");
        line.setQuantity(new BigDecimal("10.00"));
        line.setUnit("件");
        line.setRequiredDate(LocalDate.now().plusDays(7));
        reqVO.setLines(List.of(line));
        return reqVO;
    }

    private static SrmProcurementPlanAuditReqVO buildAuditReq(Long planId, String remark) {
        SrmProcurementPlanAuditReqVO reqVO = new SrmProcurementPlanAuditReqVO();
        reqVO.setId(planId);
        reqVO.setAuditRemark(remark);
        return reqVO;
    }

    private static SrmPurchaseOrderCreateReqVO buildPurchaseOrderCreateReq(Long planId) {
        SrmPurchaseOrderCreateReqVO reqVO = new SrmPurchaseOrderCreateReqVO();
        reqVO.setSourcePlanId(planId);
        reqVO.setSupplierId(108L);
        reqVO.setOrderRemark("Phase 5 委外付款测试");
        return reqVO;
    }

    private static SrmOutsourceExecutionCreateReqVO buildCreateReq(Long purchaseOrderId) {
        SrmOutsourceExecutionCreateReqVO reqVO = new SrmOutsourceExecutionCreateReqVO();
        reqVO.setPurchaseOrderId(purchaseOrderId);
        reqVO.setSimulationRemark("测试租户受控模拟链路");
        return reqVO;
    }

    private static SrmOutsourceExecutionIssueReqVO buildIssueReq(Long executionId) {
        SrmOutsourceExecutionIssueReqVO reqVO = new SrmOutsourceExecutionIssueReqVO();
        reqVO.setId(executionId);
        reqVO.setIssueRemark("模拟 PDA 发料");
        reqVO.setIssueQuantity(new BigDecimal("10.00"));
        return reqVO;
    }

    private static SrmOutsourceExecutionProgressReqVO buildProgressReq(Long executionId) {
        SrmOutsourceExecutionProgressReqVO reqVO = new SrmOutsourceExecutionProgressReqVO();
        reqVO.setId(executionId);
        reqVO.setProgressPercent(new BigDecimal("55"));
        reqVO.setProgressStage("加工中");
        reqVO.setProgressRemark("模拟进度回传");
        return reqVO;
    }

    private static SrmOutsourceExecutionReceiveReqVO buildReceiveReq(Long executionId) {
        SrmOutsourceExecutionReceiveReqVO reqVO = new SrmOutsourceExecutionReceiveReqVO();
        reqVO.setId(executionId);
        reqVO.setReceivedQuantity(new BigDecimal("10.00"));
        reqVO.setReceiveRemark("模拟收货回传");
        return reqVO;
    }

    private static SrmOutsourceExecutionInspectReqVO buildInspectReq(Long executionId) {
        SrmOutsourceExecutionInspectReqVO reqVO = new SrmOutsourceExecutionInspectReqVO();
        reqVO.setId(executionId);
        reqVO.setQualifiedQuantity(new BigDecimal("10.00"));
        reqVO.setInspectRemark("模拟检验合格");
        return reqVO;
    }

    private static SrmOutsourceExecutionReconcileReqVO buildReconcileReq(Long executionId) {
        SrmOutsourceExecutionReconcileReqVO reqVO = new SrmOutsourceExecutionReconcileReqVO();
        reqVO.setId(executionId);
        reqVO.setConfirmRemark("模拟对账确认");
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
