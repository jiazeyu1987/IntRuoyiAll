package cn.iocoder.yudao.module.srm.service.tender;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.*;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectDO;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmSourcingProjectStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleServiceImpl;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskServiceImpl;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierPortalApplicationServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@Import({
        SrmCodeRuleServiceImpl.class,
        SrmSupplierAccessRiskServiceImpl.class,
        SrmSupplierPortalApplicationServiceImpl.class,
        SrmTenderProcurementServiceImpl.class
})
class SrmTenderExpertCommitteeServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmCodeRuleServiceImpl codeRuleService;
    @Resource
    private SrmTenderProcurementServiceImpl tenderService;
    @Resource
    private SrmSourcingProjectMapper sourcingProjectMapper;

    @Test
    void formCommittee_shouldFailWhenExpertIsPendingDuplicateInsufficientOrSpecialtyMismatch() {
        seedExpertApplicationRule();
        Long projectId = insertPublishedTenderProject();
        Long pendingExpertId = tenderService.createExpert(buildExpertReq("待审专家", "MEDICAL"));
        Long approvedExpertId = createApprovedExpert("合格专家", "MEDICAL");
        Long wrongSpecialtyExpertId = createApprovedExpert("财务专家", "FINANCE");

        ServiceException pendingException = assertThrows(ServiceException.class,
                () -> tenderService.formCommittee(buildCommitteeReq(projectId, List.of(pendingExpertId), 1, "MEDICAL")));
        assertTrue(pendingException.getMessage().contains("待审核"));

        ServiceException duplicateException = assertThrows(ServiceException.class,
                () -> tenderService.formCommittee(buildCommitteeReq(projectId, List.of(approvedExpertId, approvedExpertId), 2, "MEDICAL")));
        assertTrue(duplicateException.getMessage().contains("不能重复"));

        ServiceException insufficientException = assertThrows(ServiceException.class,
                () -> tenderService.formCommittee(buildCommitteeReq(projectId, List.of(approvedExpertId), 2, "MEDICAL")));
        assertTrue(insufficientException.getMessage().contains("人数不足"));

        ServiceException mismatchException = assertThrows(ServiceException.class,
                () -> tenderService.formCommittee(buildCommitteeReq(projectId, List.of(wrongSpecialtyExpertId), 1, "MEDICAL")));
        assertTrue(mismatchException.getMessage().contains("专业类型"));

        assertEquals(SrmSourcingProjectStatusEnum.PUBLISHED.getStatus(),
                sourcingProjectMapper.selectById(projectId).getProjectStatus());
    }

    private Long insertPublishedTenderProject() {
        SrmSourcingProjectDO project = SrmSourcingProjectDO.builder()
                .projectNo("TP-UNIT-0001")
                .projectTitle("T4 专家门禁招标项目")
                .projectType(SrmProcurementMethodEnum.TENDER.getMethod())
                .projectStatus(SrmSourcingProjectStatusEnum.PUBLISHED.getStatus())
                .sourcePlanId(9001L)
                .sourcePlanNo("PP-UNIT-0001")
                .expectedAmount(new BigDecimal("1280.50"))
                .build();
        project.setTenantId(1L);
        sourcingProjectMapper.insert(project);
        return project.getId();
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

    private void seedExpertApplicationRule() {
        codeRuleService.createCodeRule(buildRule("T4_EXPERT_APP_RULE", SrmCodeRuleTargetFormEnum.EXPERT_DRAW_APPLICATION.getTargetForm(), "EA"));
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
