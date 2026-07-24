package cn.iocoder.yudao.module.srm.service.coderule;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.coderule.SrmCodeRuleCounterDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.coderule.SrmCodeRuleDO;
import cn.iocoder.yudao.module.srm.dal.mysql.coderule.SrmCodeRuleCounterMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.coderule.SrmCodeRuleMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.annotation.Resource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link SrmCodeRuleServiceImpl} 的 D7-1 编码规则契约测试。
 */
@Import(SrmCodeRuleServiceImpl.class)
class SrmCodeRuleServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmCodeRuleServiceImpl codeRuleService;
    @Resource
    private SrmCodeRuleMapper codeRuleMapper;
    @Resource
    private SrmCodeRuleCounterMapper codeRuleCounterMapper;

    @AfterEach
    void clearInlineMocks() {
        clearAllCaches();
    }

    @Test
    void generateCode_shouldCreateTenantScopedCounterAndReturnDeterministicCode() {
        Long ruleId = codeRuleService.createCodeRule(buildRule("PLAN_RULE",
                SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PL"));

        String firstCode = codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm());
        String secondCode = codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm());

        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        assertEquals("PL-" + today + "-0001", firstCode);
        assertEquals("PL-" + today + "-0002", secondCode);

        SrmCodeRuleCounterDO counter = codeRuleCounterMapper.selectByRuleIdAndPeriodKey(ruleId, today);
        assertNotNull(counter);
        assertEquals(2L, counter.getCurrentSerial());
        assertEquals(secondCode, counter.getLastCode());
    }

    @Test
    void generateCode_shouldLockRuleAndCounterBeforeUpdatingExistingCounter() {
        SrmCodeRuleServiceImpl service = new SrmCodeRuleServiceImpl();
        SrmCodeRuleMapper lockedCodeRuleMapper = mock(SrmCodeRuleMapper.class);
        SrmCodeRuleCounterMapper lockedCounterMapper = mock(SrmCodeRuleCounterMapper.class);
        ReflectionTestUtils.setField(service, "codeRuleMapper", lockedCodeRuleMapper);
        ReflectionTestUtils.setField(service, "codeRuleCounterMapper", lockedCounterMapper);

        String targetForm = SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm();
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        SrmCodeRuleDO codeRule = SrmCodeRuleDO.builder()
                .id(100L)
                .ruleCode("CONCURRENT_PLAN")
                .targetForm(targetForm)
                .prefix("PL")
                .datePattern("yyyyMMdd")
                .dateSegmentEnabled(true)
                .serialWidth(4)
                .step(1)
                .minSerial(1L)
                .maxSerial(9999L)
                .separator("-")
                .enabled(true)
                .build();
        SrmCodeRuleCounterDO counter = SrmCodeRuleCounterDO.builder()
                .id(200L)
                .ruleId(100L)
                .targetForm(targetForm)
                .periodKey(today)
                .currentSerial(1L)
                .lastCode("PL-" + today + "-0001")
                .lastGeneratedAt(LocalDateTime.now())
                .version(0)
                .build();
        when(lockedCodeRuleMapper.selectByTargetFormForUpdate(targetForm)).thenReturn(codeRule);
        when(lockedCounterMapper.selectByRuleIdAndPeriodKeyForUpdate(100L, today)).thenReturn(counter);

        String code = service.generateCode(targetForm);

        assertEquals("PL-" + today + "-0002", code);
        InOrder inOrder = inOrder(lockedCodeRuleMapper, lockedCounterMapper);
        inOrder.verify(lockedCodeRuleMapper).selectByTargetFormForUpdate(targetForm);
        inOrder.verify(lockedCounterMapper).selectByRuleIdAndPeriodKeyForUpdate(100L, today);
        verify(lockedCodeRuleMapper, never()).selectByTargetForm(targetForm);
        verify(lockedCounterMapper, never()).selectByRuleIdAndPeriodKey(100L, today);
        ArgumentCaptor<SrmCodeRuleCounterDO> counterCaptor = ArgumentCaptor.forClass(SrmCodeRuleCounterDO.class);
        verify(lockedCounterMapper).updateById(counterCaptor.capture());
        assertEquals(2L, counterCaptor.getValue().getCurrentSerial());
        assertEquals("PL-" + today + "-0002", counterCaptor.getValue().getLastCode());
        assertEquals(1, counterCaptor.getValue().getVersion());
    }

    @Test
    void generateCode_shouldFailFastWhenTargetFormRuleIsMissing() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.TENDER_PROJECT.getTargetForm()));

        assertTrue(exception.getMessage().contains("编码规则"));
        assertTrue(exception.getMessage().contains(SrmCodeRuleTargetFormEnum.TENDER_PROJECT.getTargetForm()));
    }

    @Test
    void generateCode_shouldFailFastWhenRuleIsDisabled() {
        Long ruleId = codeRuleService.createCodeRule(buildRule("DISABLED_PLAN",
                SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PL"));
        codeRuleService.enableCodeRule(ruleId, false);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm()));

        assertTrue(exception.getMessage().contains("已禁用"));
        assertEquals(0L, codeRuleCounterMapper.selectCount());
    }

    @Test
    void generateCode_shouldFailFastWhenSerialExceedsMax() {
        SrmCodeRuleSaveReqVO reqVO = buildRule("LIMIT_PLAN",
                SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PL");
        reqVO.setMaxSerial(1L);
        codeRuleService.createCodeRule(reqVO);

        assertEquals("PL-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-0001",
                codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm()));

        assertTrue(exception.getMessage().contains("最大流水"));
    }

    @Test
    void createCodeRule_shouldRejectDuplicateTargetFormInSameTenant() {
        codeRuleService.createCodeRule(buildRule("PLAN_A",
                SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PA"));

        ServiceException exception = assertThrows(ServiceException.class, () ->
                codeRuleService.createCodeRule(buildRule("PLAN_B",
                        SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm(), "PB")));

        assertTrue(exception.getMessage().contains("目标表单"));
        assertEquals(1L, codeRuleMapper.selectCount());
    }

    @Test
    void updateCodeRule_shouldRejectInvalidSerialConfiguration() {
        Long ruleId = codeRuleService.createCodeRule(buildRule("CONTRACT_RULE",
                SrmCodeRuleTargetFormEnum.PROCUREMENT_CONTRACT.getTargetForm(), "CT"));
        SrmCodeRuleSaveReqVO updateReqVO = buildRule("CONTRACT_RULE",
                SrmCodeRuleTargetFormEnum.PROCUREMENT_CONTRACT.getTargetForm(), "CT");
        updateReqVO.setId(ruleId);
        updateReqVO.setSerialWidth(0);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> codeRuleService.updateCodeRule(updateReqVO));

        assertTrue(exception.getMessage().contains("流水宽度"));
        SrmCodeRuleDO rule = codeRuleMapper.selectById(ruleId);
        assertEquals(4, rule.getSerialWidth());
    }

    private static SrmCodeRuleSaveReqVO buildRule(String ruleCode, String targetForm, String prefix) {
        SrmCodeRuleSaveReqVO reqVO = new SrmCodeRuleSaveReqVO();
        reqVO.setRuleCode(ruleCode);
        reqVO.setTargetForm(targetForm);
        reqVO.setPrefix(prefix);
        reqVO.setDatePattern("yyyyMMdd");
        reqVO.setDateSegmentEnabled(true);
        reqVO.setSerialWidth(4);
        reqVO.setStep(1);
        reqVO.setMinSerial(1L);
        reqVO.setMaxSerial(9999L);
        reqVO.setSeparator("-");
        reqVO.setEnabled(true);
        reqVO.setRemark("D7-1 RED contract");
        return reqVO;
    }

}
