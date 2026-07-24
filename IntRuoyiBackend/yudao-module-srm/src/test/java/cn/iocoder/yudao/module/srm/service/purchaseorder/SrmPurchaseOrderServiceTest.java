package cn.iocoder.yudao.module.srm.service.purchaseorder;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderChangeReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderConfirmReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderCreateReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderRejectChangeReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderWithdrawChangeReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderChangeDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierAccessDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder.SrmPurchaseOrderChangeMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder.SrmPurchaseOrderLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder.SrmPurchaseOrderMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmErpSupplierMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierAccessMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierPortalApplicationMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmPurchaseOrderChangeStatusEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmPurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierAccessStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierOnboardingStageStatusEnum;
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
        SrmProcurementPlanServiceImpl.class,
        SrmSupplierPortalApplicationServiceImpl.class,
        SrmSupplierAccessRiskServiceImpl.class,
        SrmPurchaseOrderServiceImpl.class
})
class SrmPurchaseOrderServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmCodeRuleServiceImpl codeRuleService;
    @Resource
    private SrmProcurementPlanServiceImpl procurementPlanService;
    @Resource
    private SrmPurchaseOrderService purchaseOrderService;
    @Resource
    private SrmPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private SrmPurchaseOrderChangeMapper purchaseOrderChangeMapper;
    @Resource
    private SrmPurchaseOrderLineMapper purchaseOrderLineMapper;
    @Resource
    private SrmErpSupplierMapper erpSupplierMapper;
    @Resource
    private SrmSupplierAccessMapper supplierAccessMapper;
    @Resource
    private SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper;

    @Test
    void createFromPlan_shouldFailWhenPlanNotApproved() {
        seedCodeRules();
        seedEligibleSupplier(108L, 201L, "SRM Portal E2E 供应商");

        Long planId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            planId = procurementPlanService.createProcurementPlan(buildPlanSaveReq("T3 未审核计划"));
        }

        SrmPurchaseOrderCreateReqVO reqVO = new SrmPurchaseOrderCreateReqVO();
        reqVO.setSourcePlanId(planId);
        reqVO.setSupplierId(108L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> purchaseOrderService.createFromPlan(reqVO));
        assertTrue(exception.getMessage().contains("审核通过"));
    }

    @Test
    void createFromPlan_shouldCreatePendingOrderForEligibleSupplier() {
        seedCodeRules();
        seedEligibleSupplier(108L, 201L, "SRM Portal E2E 供应商");
        Long planId = seedApprovedPlan("T3 采购订单协同");

        SrmPurchaseOrderCreateReqVO reqVO = new SrmPurchaseOrderCreateReqVO();
        reqVO.setSourcePlanId(planId);
        reqVO.setSupplierId(108L);
        reqVO.setOrderRemark("请确认交期与数量");

        Long orderId = purchaseOrderService.createFromPlan(reqVO);
        SrmPurchaseOrderRespVO order = purchaseOrderService.getPurchaseOrder(orderId);

        assertEquals(SrmPurchaseOrderStatusEnum.PENDING_CONFIRM.getStatus(), order.getOrderStatus());
        assertEquals("SRM Portal E2E 供应商", order.getSupplierName());
        assertEquals(1, order.getLines().size());
        assertEquals(new BigDecimal("10.00"), order.getLines().get(0).getRequestedQuantity());
        assertNull(order.getLines().get(0).getConfirmedQuantity());
        assertNotNull(purchaseOrderMapper.selectById(orderId));
        assertEquals(1, purchaseOrderLineMapper.selectListByOrderId(orderId).size());
    }

    @Test
    void confirmMyPurchaseOrder_shouldPersistConfirmedLineAndStatus() {
        seedCodeRules();
        seedEligibleSupplier(108L, 201L, "SRM Portal E2E 供应商");
        Long planId = seedApprovedPlan("T3 供应商确认采购订单");

        SrmPurchaseOrderCreateReqVO createReqVO = new SrmPurchaseOrderCreateReqVO();
        createReqVO.setSourcePlanId(planId);
        createReqVO.setSupplierId(108L);
        Long orderId = purchaseOrderService.createFromPlan(createReqVO);
        Long orderLineId = purchaseOrderLineMapper.selectListByOrderId(orderId).get(0).getId();

        SrmPurchaseOrderConfirmReqVO confirmReqVO = new SrmPurchaseOrderConfirmReqVO();
        confirmReqVO.setId(orderId);
        confirmReqVO.setConfirmRemark("可按期交付");
        SrmPurchaseOrderConfirmReqVO.Line line = new SrmPurchaseOrderConfirmReqVO.Line();
        line.setOrderLineId(orderLineId);
        line.setConfirmedQuantity(new BigDecimal("9.50"));
        line.setConfirmedDeliveryDate(LocalDate.now().plusDays(8));
        line.setSupplierRemark("首批 5 件，剩余 4.5 件");
        confirmReqVO.setLines(List.of(line));

        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(201L, "portal-user")) {
            purchaseOrderService.confirmMyPurchaseOrder(confirmReqVO);
        }

        SrmPurchaseOrderRespVO order = purchaseOrderService.getPurchaseOrder(orderId);
        assertEquals(SrmPurchaseOrderStatusEnum.CONFIRMED.getStatus(), order.getOrderStatus());
        assertEquals("portal-user", order.getConfirmedName());
        assertEquals(new BigDecimal("9.50"), order.getLines().get(0).getConfirmedQuantity());
        assertEquals("首批 5 件，剩余 4.5 件", order.getLines().get(0).getSupplierRemark());
        assertNotNull(order.getLines().get(0).getConfirmedDeliveryDate());
    }

    @Test
    void submitOrderChange_shouldKeepOriginalConfirmedValueUntilSupplierReconfirms() {
        seedCodeRules();
        seedEligibleSupplier(108L, 201L, "SRM Portal E2E 供应商");
        Long orderId = seedConfirmedOrder();
        Long orderLineId = purchaseOrderLineMapper.selectListByOrderId(orderId).get(0).getId();

        Long changeId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            changeId = purchaseOrderService.submitOrderChange(buildOrderChangeReq(orderId, orderLineId,
                    new BigDecimal("8.00"), LocalDate.now().plusDays(10), "客户改期"));
        }

        SrmPurchaseOrderRespVO order = purchaseOrderService.getPurchaseOrder(orderId);
        assertEquals(SrmPurchaseOrderStatusEnum.CHANGE_PENDING.getStatus(), order.getOrderStatus());
        assertEquals(new BigDecimal("9.50"), order.getLines().get(0).getConfirmedQuantity());
        assertEquals(new BigDecimal("8.00"), order.getLines().get(0).getPendingChangedQuantity());
        assertEquals("客户改期", order.getLines().get(0).getPendingChangedRemark());
        assertNotNull(changeId);
    }

    @Test
    void rejectMyPurchaseOrderChange_shouldRollbackPendingValueAndKeepOrderConfirmed() {
        seedCodeRules();
        seedEligibleSupplier(108L, 201L, "SRM Portal E2E 供应商");
        Long orderId = seedConfirmedOrder();
        Long orderLineId = purchaseOrderLineMapper.selectListByOrderId(orderId).get(0).getId();
        Long changeId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            changeId = purchaseOrderService.submitOrderChange(buildOrderChangeReq(orderId, orderLineId,
                    new BigDecimal("7.50"), LocalDate.now().plusDays(11), "仓储窗口变更"));
        }

        SrmPurchaseOrderRejectChangeReqVO rejectReqVO = new SrmPurchaseOrderRejectChangeReqVO();
        rejectReqVO.setChangeId(changeId);
        rejectReqVO.setRejectRemark("当前产能无法接受该改动");
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(201L, "portal-user")) {
            purchaseOrderService.rejectMyPurchaseOrderChange(rejectReqVO);
        }

        SrmPurchaseOrderRespVO order = purchaseOrderService.getPurchaseOrder(orderId);
        assertEquals(SrmPurchaseOrderStatusEnum.CONFIRMED.getStatus(), order.getOrderStatus());
        assertNull(order.getLines().get(0).getPendingChangedQuantity());
        SrmPurchaseOrderChangeDO change = purchaseOrderChangeMapper.selectById(changeId);
        assertEquals(SrmPurchaseOrderChangeStatusEnum.REJECTED.getStatus(), change.getChangeStatus());
        assertEquals("当前产能无法接受该改动", change.getRejectRemark());
    }

    @Test
    void withdrawOrderChange_shouldClearPendingValueAndMarkWithdrawn() {
        seedCodeRules();
        seedEligibleSupplier(108L, 201L, "SRM Portal E2E 供应商");
        Long orderId = seedConfirmedOrder();
        Long orderLineId = purchaseOrderLineMapper.selectListByOrderId(orderId).get(0).getId();
        Long changeId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            changeId = purchaseOrderService.submitOrderChange(buildOrderChangeReq(orderId, orderLineId,
                    new BigDecimal("8.20"), LocalDate.now().plusDays(9), "排产计划微调"));
            SrmPurchaseOrderWithdrawChangeReqVO withdrawReqVO = new SrmPurchaseOrderWithdrawChangeReqVO();
            withdrawReqVO.setChangeId(changeId);
            withdrawReqVO.setWithdrawRemark("采购侧内部调整，暂不发起");
            purchaseOrderService.withdrawOrderChange(withdrawReqVO);
        }

        SrmPurchaseOrderRespVO order = purchaseOrderService.getPurchaseOrder(orderId);
        assertEquals(SrmPurchaseOrderStatusEnum.CONFIRMED.getStatus(), order.getOrderStatus());
        assertNull(order.getLines().get(0).getPendingChangedQuantity());
        SrmPurchaseOrderChangeDO change = purchaseOrderChangeMapper.selectById(changeId);
        assertEquals(SrmPurchaseOrderChangeStatusEnum.WITHDRAWN.getStatus(), change.getChangeStatus());
        assertEquals("采购侧内部调整，暂不发起", change.getWithdrawRemark());
    }

    private void seedCodeRules() {
        codeRuleService.createCodeRule(buildRule("PLAN_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PL"));
        codeRuleService.createCodeRule(buildRule("PLAN_LINE_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN_LINE.getTargetForm(), "PLI"));
        codeRuleService.createCodeRule(buildRule("PO_RULE", SrmCodeRuleTargetFormEnum.PURCHASE_ORDER.getTargetForm(), "PO"));
        codeRuleService.createCodeRule(buildRule("PO_LINE_RULE", SrmCodeRuleTargetFormEnum.PURCHASE_ORDER_LINE.getTargetForm(), "POL"));
        codeRuleService.createCodeRule(buildRule("PO_CHANGE_RULE", SrmCodeRuleTargetFormEnum.PURCHASE_ORDER_CHANGE.getTargetForm(), "POC"));
    }

    private Long seedConfirmedOrder() {
        Long planId = seedApprovedPlan("T3 订单变更协同");
        SrmPurchaseOrderCreateReqVO createReqVO = new SrmPurchaseOrderCreateReqVO();
        createReqVO.setSourcePlanId(planId);
        createReqVO.setSupplierId(108L);
        Long orderId = purchaseOrderService.createFromPlan(createReqVO);
        Long orderLineId = purchaseOrderLineMapper.selectListByOrderId(orderId).get(0).getId();
        SrmPurchaseOrderConfirmReqVO confirmReqVO = new SrmPurchaseOrderConfirmReqVO();
        confirmReqVO.setId(orderId);
        confirmReqVO.setConfirmRemark("原订单已确认");
        SrmPurchaseOrderConfirmReqVO.Line line = new SrmPurchaseOrderConfirmReqVO.Line();
        line.setOrderLineId(orderLineId);
        line.setConfirmedQuantity(new BigDecimal("9.50"));
        line.setConfirmedDeliveryDate(LocalDate.now().plusDays(8));
        line.setSupplierRemark("按原承诺排产");
        confirmReqVO.setLines(List.of(line));
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(201L, "portal-user")) {
            purchaseOrderService.confirmMyPurchaseOrder(confirmReqVO);
        }
        return orderId;
    }

    private static SrmPurchaseOrderChangeReqVO buildOrderChangeReq(Long orderId, Long orderLineId,
                                                                   BigDecimal changedQuantity, LocalDate changedDeliveryDate,
                                                                   String changedRemark) {
        SrmPurchaseOrderChangeReqVO reqVO = new SrmPurchaseOrderChangeReqVO();
        reqVO.setOrderId(orderId);
        reqVO.setChangeReason("客户交付窗口调整");
        reqVO.setChangeRemark("采购侧提出改期改量");
        SrmPurchaseOrderChangeReqVO.Line line = new SrmPurchaseOrderChangeReqVO.Line();
        line.setOrderLineId(orderLineId);
        line.setChangedQuantity(changedQuantity);
        line.setChangedDeliveryDate(changedDeliveryDate);
        line.setChangedSupplierRemark(changedRemark);
        reqVO.setLines(List.of(line));
        return reqVO;
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
        reqVO.setExpectedAmount(new BigDecimal("1280.50"));
        reqVO.setRemark("Phase 3 采购订单协同测试");
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

    private static SrmProcurementPlanAuditReqVO buildAuditReq(Long planId, String remark) {
        SrmProcurementPlanAuditReqVO reqVO = new SrmProcurementPlanAuditReqVO();
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
