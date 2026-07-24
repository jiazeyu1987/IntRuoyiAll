package cn.iocoder.yudao.module.srm.service.procurement;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanGenerateReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmSourcingProjectRespVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectLineDO;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementPlanStatusEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmSourcingProjectStatusEnum;
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
class SrmProcurementPlanSourcingServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmCodeRuleServiceImpl codeRuleService;
    @Resource
    private SrmProcurementPlanServiceImpl procurementPlanService;
    @Resource
    private SrmSourcingProjectMapper sourcingProjectMapper;
    @Resource
    private SrmSourcingProjectLineMapper sourcingProjectLineMapper;

    @Test
    void generateSourcingProject_shouldCopyApprovedPlanLinesAndBlockDuplicateGeneration() {
        seedPlanAndProjectCodeRules();
        Long planId = createApprovedPlan();

        SrmProcurementPlanGenerateReqVO generateReqVO = new SrmProcurementPlanGenerateReqVO();
        generateReqVO.setId(planId);
        generateReqVO.setProjectType(SrmProcurementMethodEnum.NON_BIDDING.getMethod());

        SrmSourcingProjectRespVO project;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(23L, "sourcing-owner")) {
            project = procurementPlanService.generateSourcingProject(generateReqVO);
        }

        SrmSourcingProjectDO persistedProject = sourcingProjectMapper.selectById(project.getId());
        List<SrmSourcingProjectLineDO> projectLines = sourcingProjectLineMapper.selectListByProjectId(project.getId());

        assertEquals(planId, persistedProject.getSourcePlanId());
        assertEquals(SrmProcurementMethodEnum.NON_BIDDING.getMethod(), persistedProject.getProjectType());
        assertEquals(SrmSourcingProjectStatusEnum.DRAFT.getStatus(), persistedProject.getProjectStatus());
        assertEquals(1, projectLines.size());
        assertNotNull(projectLines.get(0).getSourcePlanLineId());
        assertEquals(SrmProcurementPlanStatusEnum.GENERATED.getStatus(),
                procurementPlanService.getProcurementPlan(planId).getPlanStatus());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> procurementPlanService.generateSourcingProject(generateReqVO));
        assertTrue(exception.getMessage().contains("重复"));
    }

    @Test
    void generateSourcingProject_shouldFailWhenPlanIsNotApproved() {
        seedPlanAndProjectCodeRules();
        Long planId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            planId = procurementPlanService.createProcurementPlan(buildPlanSaveReq("未审核计划"));
        }

        SrmProcurementPlanGenerateReqVO generateReqVO = new SrmProcurementPlanGenerateReqVO();
        generateReqVO.setId(planId);
        generateReqVO.setProjectType(SrmProcurementMethodEnum.TENDER.getMethod());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> procurementPlanService.generateSourcingProject(generateReqVO));
        assertTrue(exception.getMessage().contains("审核通过"));
        assertNull(sourcingProjectMapper.selectBySourcePlanId(planId));
    }

    private Long createApprovedPlan() {
        Long planId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(21L, "planner")) {
            planId = procurementPlanService.createProcurementPlan(buildPlanSaveReq("已审核可转寻源"));
            procurementPlanService.submitProcurementPlan(planId);
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(22L, "approver")) {
            procurementPlanService.approveProcurementPlan(buildAuditReq(planId, "同意转寻源"));
        }
        return planId;
    }

    private void seedPlanAndProjectCodeRules() {
        codeRuleService.createCodeRule(buildRule("PLAN_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PL"));
        codeRuleService.createCodeRule(buildRule("PLAN_LINE_RULE", SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN_LINE.getTargetForm(), "PLI"));
        codeRuleService.createCodeRule(buildRule("NON_TENDER_RULE", SrmCodeRuleTargetFormEnum.NON_TENDER_PROJECT.getTargetForm(), "NB"));
        codeRuleService.createCodeRule(buildRule("TENDER_RULE", SrmCodeRuleTargetFormEnum.TENDER_PROJECT.getTargetForm(), "TD"));
    }

    private static SrmProcurementPlanSaveReqVO buildPlanSaveReq(String title) {
        SrmProcurementPlanSaveReqVO reqVO = new SrmProcurementPlanSaveReqVO();
        reqVO.setPlanTitle(title);
        reqVO.setProcurementMethod(SrmProcurementMethodEnum.NON_BIDDING.getMethod());
        reqVO.setExpectedAmount(new BigDecimal("3860.00"));
        SrmProcurementPlanSaveReqVO.Line line = new SrmProcurementPlanSaveReqVO.Line();
        line.setMaterialId(6001L);
        line.setMaterialCode("MAT-6001");
        line.setMaterialName("诊断试剂盒");
        line.setQuantity(new BigDecimal("20.00"));
        line.setUnit("盒");
        line.setRequiredDate(LocalDate.now().plusDays(10));
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
