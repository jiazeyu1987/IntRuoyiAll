package cn.iocoder.yudao.module.srm.service.coderule;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRulePageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.coderule.SrmCodeRuleCounterDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.coderule.SrmCodeRuleDO;
import cn.iocoder.yudao.module.srm.dal.mysql.coderule.SrmCodeRuleCounterMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.coderule.SrmCodeRuleMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.*;

/**
 * SRM 编码规则 Service 实现类。
 */
@Service
@Validated
public class SrmCodeRuleServiceImpl implements SrmCodeRuleService {

    private static final String GLOBAL_PERIOD_KEY = "GLOBAL";

    @Resource
    private SrmCodeRuleMapper codeRuleMapper;
    @Resource
    private SrmCodeRuleCounterMapper codeRuleCounterMapper;

    @Override
    public Long createCodeRule(SrmCodeRuleSaveReqVO createReqVO) {
        validateCodeRuleTargetForm(createReqVO.getTargetForm());
        validateSerialConfiguration(createReqVO);
        validateRuleCodeUnique(null, createReqVO.getRuleCode());
        validateTargetFormUnique(null, createReqVO.getTargetForm());

        SrmCodeRuleDO codeRule = BeanUtils.toBean(createReqVO, SrmCodeRuleDO.class);
        codeRuleMapper.insert(codeRule);
        return codeRule.getId();
    }

    @Override
    public void updateCodeRule(SrmCodeRuleSaveReqVO updateReqVO) {
        validateCodeRuleExists(updateReqVO.getId());
        validateCodeRuleTargetForm(updateReqVO.getTargetForm());
        validateSerialConfiguration(updateReqVO);
        validateRuleCodeUnique(updateReqVO.getId(), updateReqVO.getRuleCode());
        validateTargetFormUnique(updateReqVO.getId(), updateReqVO.getTargetForm());

        SrmCodeRuleDO updateObj = BeanUtils.toBean(updateReqVO, SrmCodeRuleDO.class);
        codeRuleMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableCodeRule(Long id, Boolean enabled) {
        validateCodeRuleExists(id);
        codeRuleMapper.updateById(SrmCodeRuleDO.builder().id(id).enabled(enabled).build());
    }

    @Override
    public SrmCodeRuleDO getCodeRule(Long id) {
        return codeRuleMapper.selectById(id);
    }

    @Override
    public PageResult<SrmCodeRuleDO> getCodeRulePage(SrmCodeRulePageReqVO pageReqVO) {
        return codeRuleMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateCode(String targetForm) {
        validateCodeRuleTargetForm(targetForm);
        SrmCodeRuleDO codeRule = codeRuleMapper.selectByTargetFormForUpdate(targetForm);
        if (codeRule == null) {
            throw exception(CODE_RULE_TARGET_FORM_NOT_EXISTS, targetForm);
        }
        if (Boolean.FALSE.equals(codeRule.getEnabled())) {
            throw exception(CODE_RULE_DISABLED, targetForm);
        }
        validateSerialConfiguration(codeRule);

        String periodKey = buildPeriodKey(codeRule);
        SrmCodeRuleCounterDO counter = codeRuleCounterMapper.selectByRuleIdAndPeriodKeyForUpdate(codeRule.getId(), periodKey);
        long nextSerial = counter == null
                ? codeRule.getMinSerial()
                : counter.getCurrentSerial() + codeRule.getStep();
        if (nextSerial > codeRule.getMaxSerial()) {
            throw exception(CODE_RULE_SERIAL_EXCEED_MAX);
        }

        String code = buildCode(codeRule, periodKey, nextSerial);
        LocalDateTime now = LocalDateTime.now();
        if (counter == null) {
            codeRuleCounterMapper.insert(SrmCodeRuleCounterDO.builder()
                    .ruleId(codeRule.getId())
                    .targetForm(codeRule.getTargetForm())
                    .periodKey(periodKey)
                    .currentSerial(nextSerial)
                    .lastCode(code)
                    .lastGeneratedAt(now)
                    .version(0)
                    .build());
        } else {
            counter.setCurrentSerial(nextSerial);
            counter.setLastCode(code);
            counter.setLastGeneratedAt(now);
            counter.setVersion(counter.getVersion() == null ? 1 : counter.getVersion() + 1);
            codeRuleCounterMapper.updateById(counter);
        }
        return code;
    }

    private void validateCodeRuleExists(Long id) {
        if (codeRuleMapper.selectById(id) == null) {
            throw exception(CODE_RULE_NOT_EXISTS);
        }
    }

    private void validateRuleCodeUnique(Long id, String ruleCode) {
        SrmCodeRuleDO codeRule = codeRuleMapper.selectByRuleCode(ruleCode);
        if (codeRule != null && !Objects.equals(id, codeRule.getId())) {
            throw exception(CODE_RULE_RULE_CODE_DUPLICATE);
        }
    }

    private void validateTargetFormUnique(Long id, String targetForm) {
        SrmCodeRuleDO codeRule = codeRuleMapper.selectByTargetForm(targetForm);
        if (codeRule != null && !Objects.equals(id, codeRule.getId())) {
            throw exception(CODE_RULE_TARGET_FORM_DUPLICATE);
        }
    }

    private void validateCodeRuleTargetForm(String targetForm) {
        if (!SrmCodeRuleTargetFormEnum.contains(targetForm)) {
            throw exception(CODE_RULE_TARGET_FORM_INVALID, targetForm);
        }
    }

    private void validateSerialConfiguration(SrmCodeRuleSaveReqVO reqVO) {
        validateSerialConfiguration(BeanUtils.toBean(reqVO, SrmCodeRuleDO.class));
    }

    private void validateSerialConfiguration(SrmCodeRuleDO codeRule) {
        if (codeRule.getSerialWidth() == null || codeRule.getSerialWidth() < 1 || codeRule.getSerialWidth() > 18) {
            throw exception(CODE_RULE_SERIAL_CONFIG_INVALID, "流水宽度必须在 1 到 18 之间");
        }
        if (codeRule.getStep() == null || codeRule.getStep() < 1) {
            throw exception(CODE_RULE_SERIAL_CONFIG_INVALID, "流水步长必须大于 0");
        }
        if (codeRule.getMinSerial() == null || codeRule.getMinSerial() < 0) {
            throw exception(CODE_RULE_SERIAL_CONFIG_INVALID, "最小流水不能小于 0");
        }
        if (codeRule.getMaxSerial() == null || codeRule.getMaxSerial() < codeRule.getMinSerial()) {
            throw exception(CODE_RULE_SERIAL_CONFIG_INVALID, "最大流水不能小于最小流水");
        }
        long maxSerialByWidth = calculateMaxSerialByWidth(codeRule.getSerialWidth());
        if (codeRule.getMaxSerial() > maxSerialByWidth) {
            throw exception(CODE_RULE_SERIAL_CONFIG_INVALID, "最大流水不能超过流水宽度上限");
        }
        if (Boolean.TRUE.equals(codeRule.getDateSegmentEnabled()) && StrUtil.isBlank(codeRule.getDatePattern())) {
            throw exception(CODE_RULE_DATE_PATTERN_INVALID, codeRule.getDatePattern());
        }
    }

    private String buildPeriodKey(SrmCodeRuleDO codeRule) {
        if (!Boolean.TRUE.equals(codeRule.getDateSegmentEnabled())) {
            return GLOBAL_PERIOD_KEY;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(codeRule.getDatePattern());
            return LocalDate.now().format(formatter);
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw exception(CODE_RULE_DATE_PATTERN_INVALID, codeRule.getDatePattern());
        }
    }

    private String buildCode(SrmCodeRuleDO codeRule, String periodKey, long serial) {
        String separator = StrUtil.nullToDefault(codeRule.getSeparator(), "");
        String serialText = String.format("%0" + codeRule.getSerialWidth() + "d", serial);
        if (Boolean.TRUE.equals(codeRule.getDateSegmentEnabled())) {
            return codeRule.getPrefix() + separator + periodKey + separator + serialText;
        }
        return codeRule.getPrefix() + separator + serialText;
    }

    private long calculateMaxSerialByWidth(Integer serialWidth) {
        long base = 1L;
        for (int i = 0; i < serialWidth; i++) {
            base *= 10L;
        }
        return base - 1L;
    }

}
