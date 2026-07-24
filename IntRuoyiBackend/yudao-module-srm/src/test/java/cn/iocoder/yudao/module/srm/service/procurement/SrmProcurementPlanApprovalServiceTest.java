package cn.iocoder.yudao.module.srm.service.procurement;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmProcurementApprovalRecordDO;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmProcurementApprovalRecordMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementApprovalActionEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementPlanStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@Import({SrmCodeRuleServiceImpl.class, SrmProcurementPlanServiceImpl.class})
class SrmProcurementPlanApprovalServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmCodeRuleServiceImpl codeRuleService;
    @Resource
    private SrmProcurementPlanServiceImpl procurementPlanService;
    @Resource
    private SrmProcurementApprovalRecordMapper approvalRecordMapper;

    @Test
    void submitAndApprovePlan_shouldPersistStatusAndApprovalRecords() {
        seedPlanCodeRules();

        Long planId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            planId = procurementPlanService.createProcurementPlan(buildPlanSaveReq("T2 采购计划审批"));
            procurementPlanService.submitProcurementPlan(planId);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(22L, "approver")) {
            procurementPlanService.approveProcurementPlan(buildAuditReq(planId, "同意采购"));
        }

        SrmProcurementPlanRespVO plan = procurementPlanService.getProcurementPlan(planId);
        List<SrmProcurementApprovalRecordDO> records = approvalRecordMapper.selectListByBiz("PROCUREMENT_PLAN", planId);

        assertEquals(SrmProcurementPlanStatusEnum.APPROVED.getStatus(), plan.getPlanStatus());
        assertNotNull(plan.getPlanNo());
        assertEquals(1, plan.getLines().size());
        assertEquals(2, records.size());
        assertEquals(SrmProcurementApprovalActionEnum.SUBMIT.getAction(), records.get(0).getAction());
        assertEquals(SrmProcurementApprovalActionEnum.APPROVE.getAction(), records.get(1).getAction());
        assertEquals("approver", records.get(1).getOperatorName());
    }

    @Test
    void rejectPlan_shouldRequireAuditRemarkAndKeepSubmittedStatusWhenMissing() {
        seedPlanCodeRules();
        Long planId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            planId = procurementPlanService.createProcurementPlan(buildPlanSaveReq("T2 采购计划驳回"));
            procurementPlanService.submitProcurementPlan(planId);
        }

        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(22L, "approver")) {
            exception = assertThrows(ServiceException.class,
                    () -> procurementPlanService.rejectProcurementPlan(buildAuditReq(planId, " ")));
        }

        assertTrue(exception.getMessage().contains("审核意见"));
        assertEquals(SrmProcurementPlanStatusEnum.SUBMITTED.getStatus(),
                procurementPlanService.getProcurementPlan(planId).getPlanStatus());
    }

    @Test
    void createProcurementPlan_shouldFailWhenAmountOrQuantityIsNonPositive() {
        seedPlanCodeRules();

        SrmProcurementPlanSaveReqVO amountReqVO = buildPlanSaveReq("T2 采购计划负金额");
        amountReqVO.setExpectedAmount(new BigDecimal("-1.00"));
        ServiceException amountException = assertThrows(ServiceException.class,
                () -> procurementPlanService.createProcurementPlan(amountReqVO));
        assertTrue(amountException.getMessage().contains("预计金额"));

        SrmProcurementPlanSaveReqVO quantityReqVO = buildPlanSaveReq("T2 采购计划零数量");
        quantityReqVO.getLines().get(0).setQuantity(BigDecimal.ZERO);
        ServiceException quantityException = assertThrows(ServiceException.class,
                () -> procurementPlanService.createProcurementPlan(quantityReqVO));
        assertTrue(quantityException.getMessage().contains("数量"));
    }

    private void seedPlanCodeRules() {
        codeRuleService.createCodeRule(buildRule("PLAN_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PL"));
        codeRuleService.createCodeRule(buildRule("PLAN_LINE_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN_LINE.getTargetForm(), "PLI"));
    }

    private static SrmProcurementPlanSaveReqVO buildPlanSaveReq(String title) {
        SrmProcurementPlanSaveReqVO reqVO = new SrmProcurementPlanSaveReqVO();
        reqVO.setPlanTitle(title);
        reqVO.setProcurementMethod(SrmProcurementMethodEnum.NON_BIDDING.getMethod());
        reqVO.setExpectedAmount(new BigDecimal("1280.50"));
        reqVO.setRemark("真实采购计划审批测试");
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
