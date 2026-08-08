package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_DEVICE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_DEVICE_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED;

@Service
public class MesFrontlineDeviceParameterValidatorImpl implements MesFrontlineDeviceParameterValidator {

    private static final String DEVICE_STATUS_ENABLED = "ENABLED";
    private static final String PARAMETER_STATUS_NORMAL = "NORMAL";
    private static final String PARAMETER_STATUS_BELOW_LOWER = "BELOW_LOWER";
    private static final String PARAMETER_STATUS_ABOVE_UPPER = "ABOVE_UPPER";

    private final MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    private final MesProcessPoolTeamDeviceMapper deviceMapper;
    private final MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;

    public MesFrontlineDeviceParameterValidatorImpl(
            MesProcessPoolTeamProcessDeviceMapper processDeviceMapper,
            MesProcessPoolTeamDeviceMapper deviceMapper,
            MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper) {
        this.processDeviceMapper = processDeviceMapper;
        this.deviceMapper = deviceMapper;
        this.parameterRuleMapper = parameterRuleMapper;
    }

    @Override
    public void validateSelectedDeviceAndParameters(
            Long routeProcessId,
            Long processId,
            MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice,
            List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO> deviceParameterReadings) {
        if (routeProcessId == null || processId == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "routeProcessId/processId");
        }
        if (selectedDevice == null || selectedDevice.getDeviceId() == null) {
            if (deviceParameterReadings != null && !deviceParameterReadings.isEmpty()) {
                throw exception(PRO_FRONTLINE_FEEDBACK_DEVICE_INVALID, "selectedDevice");
            }
            return;
        }
        MesProcessPoolTeamDeviceDO device = requireEnabledDevice(selectedDevice.getDeviceId());
        Set<Long> leaderUserIds = requireProcessDeviceLeaders(processId, device);
        Map<String, MesProcessPoolDeviceParameterRuleDO> rulesByParameterCode =
                listEnabledParameterRules(routeProcessId, processId, device.getId(), leaderUserIds);
        Set<String> submittedParameterCodes = new LinkedHashSet<>();
        for (MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading
                : deviceParameterReadings == null ? List.<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO>of()
                : deviceParameterReadings) {
            validateReadingAgainstRule(device, reading, rulesByParameterCode);
            if (!submittedParameterCodes.add(reading.getParameterCode())) {
                throw exception(PRO_FRONTLINE_FEEDBACK_DEVICE_PARAMETER_INVALID,
                        "duplicate parameterCode=" + reading.getParameterCode());
            }
        }
        for (MesProcessPoolDeviceParameterRuleDO rule : rulesByParameterCode.values()) {
            if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD.equals(rule.getValueType())) {
                continue;
            }
            if (!submittedParameterCodes.contains(rule.getParameterCode())) {
                throw exception(PRO_FRONTLINE_FEEDBACK_DEVICE_PARAMETER_INVALID,
                        "missing parameterCode=" + rule.getParameterCode());
            }
        }
    }

    private MesProcessPoolTeamDeviceDO requireEnabledDevice(Long deviceId) {
        MesProcessPoolTeamDeviceDO device = deviceMapper.selectById(deviceId);
        if (device == null
                || !Boolean.TRUE.equals(device.getEnabled())
                || !DEVICE_STATUS_ENABLED.equals(device.getDeviceStatus())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_DEVICE_INVALID, deviceId);
        }
        return device;
    }

    private Set<Long> requireProcessDeviceLeaders(Long processId, MesProcessPoolTeamDeviceDO device) {
        List<MesProcessPoolTeamProcessDeviceDO> bindings = processDeviceMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolTeamProcessDeviceDO>()
                        .eq(MesProcessPoolTeamProcessDeviceDO::getProcessId, processId)
                        .eq(MesProcessPoolTeamProcessDeviceDO::getDeviceId, device.getId())
                        .eq(MesProcessPoolTeamProcessDeviceDO::getEnabled, Boolean.TRUE));
        Set<Long> leaderUserIds = bindings.stream()
                .filter(binding -> Objects.equals(binding.getLeaderUserId(), device.getLeaderUserId()))
                .map(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (leaderUserIds.isEmpty()) {
            throw exception(PRO_FRONTLINE_FEEDBACK_DEVICE_INVALID, device.getId());
        }
        return leaderUserIds;
    }

    private Map<String, MesProcessPoolDeviceParameterRuleDO> listEnabledParameterRules(
            Long routeProcessId, Long processId, Long deviceId, Set<Long> leaderUserIds) {
        return parameterRuleMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolDeviceParameterRuleDO>()
                        .eq(MesProcessPoolDeviceParameterRuleDO::getProcessId, processId)
                        .eq(MesProcessPoolDeviceParameterRuleDO::getDeviceId, deviceId)
                        .in(MesProcessPoolDeviceParameterRuleDO::getLeaderUserId, leaderUserIds)
                        .eq(MesProcessPoolDeviceParameterRuleDO::getEnabled, Boolean.TRUE))
                .stream()
                .filter(rule -> routeProcessMatches(rule.getRouteProcessId(), routeProcessId))
                .collect(Collectors.toMap(MesProcessPoolDeviceParameterRuleDO::getParameterCode,
                        Function.identity(), (left, ignored) -> left, LinkedHashMap::new));
    }

    private void validateReadingAgainstRule(
            MesProcessPoolTeamDeviceDO device,
            MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading,
            Map<String, MesProcessPoolDeviceParameterRuleDO> rulesByParameterCode) {
        if (reading == null
                || !Objects.equals(device.getId(), reading.getDeviceId())
                || StrUtil.isBlank(reading.getParameterCode())
                || reading.getValue() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_DEVICE_PARAMETER_INVALID, "parameterCode");
        }
        MesProcessPoolDeviceParameterRuleDO rule = rulesByParameterCode.get(reading.getParameterCode());
        if (rule == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_DEVICE_PARAMETER_INVALID, reading.getParameterCode());
        }
        if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD.equals(rule.getValueType())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_DEVICE_PARAMETER_INVALID, reading.getParameterCode());
        }
        reading.setDeviceCode(device.getDeviceCode())
                .setDeviceName(device.getDeviceName())
                .setParameterName(rule.getParameterName())
                .setUnit(rule.getUnit())
                .setLowerLimit(rule.getLowerLimit())
                .setUpperLimit(rule.getUpperLimit())
                .setParameterStatus(resolveParameterStatus(reading.getValue(),
                        rule.getLowerLimit(), rule.getUpperLimit()));
    }

    private static String resolveParameterStatus(BigDecimal value, BigDecimal lowerLimit, BigDecimal upperLimit) {
        if (lowerLimit != null && value.compareTo(lowerLimit) < 0) {
            return PARAMETER_STATUS_BELOW_LOWER;
        }
        if (upperLimit != null && value.compareTo(upperLimit) > 0) {
            return PARAMETER_STATUS_ABOVE_UPPER;
        }
        return PARAMETER_STATUS_NORMAL;
    }

    private static boolean routeProcessMatches(Long configuredRouteProcessId, Long routeProcessId) {
        return configuredRouteProcessId != null && Objects.equals(configuredRouteProcessId, routeProcessId);
    }
}
