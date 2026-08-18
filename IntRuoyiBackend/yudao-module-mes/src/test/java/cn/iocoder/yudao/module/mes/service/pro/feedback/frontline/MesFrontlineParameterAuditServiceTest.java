package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineProcessPoolContextReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotCodec;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineParameterAuditServiceTest {

    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;

    private MesFrontlineParameterAuditServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesFrontlineParameterAuditServiceImpl(snapshotMapper, parameterRuleMapper);
    }

    @Test
    void frozenSnapshotRemainsAuthoritativeAndOverwritesClientStandard() {
        stubFrozenSnapshot(frozenRule(501L, "pressure", "10"));
        MesProFrontlineFeedbackSubmitReqVO req = request(reading(501L, "pressure"));
        req.getFeedbackPayload().getDeviceParameterReadings().get(0)
                .setUnit("client-unit")
                .setLowerLimit(new BigDecimal("0"))
                .setUpperLimit(new BigDecimal("999"))
                .setParameterStatus("NORMAL")
                .setValue(new BigDecimal("11"));

        MesFrontlineParameterAuditResult result = service.resolveAndApply(req);

        assertEquals("RESOLVED", result.getParameterAuditStatus());
        assertEquals(1, result.getResolvedCount());
        MesFrontlineParameterAuditItem item = result.getAuditItems().get(0);
        assertEquals("FROZEN", item.getSnapshotSource());
        assertEquals("MPa", item.getUnit());
        assertEquals(new BigDecimal("10"), item.getUpperLimit());
        assertEquals("ABOVE_UPPER", item.getParameterStatus());
        verify(parameterRuleMapper, never()).selectList(any());
    }

    @Test
    void allNineApprovedIdentityAndSnapshotAnomaliesReturnStableUnresolvedReasons() {
        assertReason(request(reading(null, "pressure")), "DEVICE_ID_MISSING", validSnapshot());
        assertReason(request(reading(501L, null)), "PARAMETER_CODE_MISSING", validSnapshot());

        MesProFrontlineFeedbackSubmitReqVO selectedMissing = request(reading(501L, "pressure"));
        selectedMissing.getFeedbackPayload().setSelectedDevice(
                new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO());
        assertReason(selectedMissing, "SELECTED_DEVICE_ID_MISSING", validSnapshot());

        assertReason(request(reading(502L, "pressure")), "DEVICE_MISMATCH", validSnapshot());

        MesProFrontlineFeedbackSubmitReqVO duplicate = request(
                reading(501L, "pressure"), reading(501L, "PRESSURE"));
        stubFrozenSnapshot(frozenRule(501L, "pressure", "10"));
        MesFrontlineParameterAuditResult duplicateResult = service.resolveAndApply(duplicate);
        assertEquals(List.of("DUPLICATE_PARAMETER", "DUPLICATE_PARAMETER"), duplicateResult.getAuditItems()
                .stream().map(MesFrontlineParameterAuditItem::getReasonCode).toList());
        assertEquals(List.of(0, 1), duplicateResult.getAuditItems().stream()
                .map(MesFrontlineParameterAuditItem::getReadingIndex).toList());

        assertReason(request(reading(501L, "unknown")), "RULE_NOT_FOUND", validSnapshot());

        MesProcessPoolActiveOrderProcessSnapshotDO wrongContext = validSnapshot().setRouteId(999L);
        assertReason(request(reading(501L, "pressure")), "CONTEXT_MISMATCH", wrongContext);

        MesProFrontlineFeedbackSubmitReqVO legacy = request(reading(501L, "pressure"));
        legacy.getFeedbackPayload().setActiveOrderProcessSnapshotId(null);
        MesFrontlineParameterAuditResult legacyResult = service.resolveAndApply(legacy);
        assertEquals("SNAPSHOT_MISSING_LEGACY", legacyResult.getAuditItems().get(0).getReasonCode());
        assertEquals("MISSING_LEGACY", legacyResult.getAuditItems().get(0).getSnapshotSource());

        MesProcessPoolActiveOrderProcessSnapshotDO badHash = validSnapshot().setParameterSnapshotSha256("bad");
        assertReason(request(reading(501L, "pressure")), "SNAPSHOT_HASH_MISMATCH", badHash);

        verify(parameterRuleMapper, never()).selectList(any());
    }

    @Test
    void noWorkOrderUsesCurrentRouteProcessSnapshotAtSubmit() {
        MesProFrontlineFeedbackSubmitReqVO req = request(reading(501L, "pressure"));
        req.getFeedbackPayload().setWorkOrderId(null).setActiveOrderProcessSnapshotId(null);
        req.getProcessPoolContext().setWorkOrderId(null).setActiveOrderId(null);
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                MesProcessPoolDeviceParameterRuleDO.builder()
                        .id(11L)
                        .routeProcessId(71L)
                        .processId(31L)
                        .deviceId(501L)
                        .parameterCode("pressure")
                        .parameterName("压力")
                        .unit("MPa")
                        .upperLimit(new BigDecimal("12"))
                        .enabled(Boolean.TRUE)
                        .build()));

        MesFrontlineParameterAuditResult result = service.resolveAndApply(req);

        assertEquals("CURRENT_ROUTE_PROCESS_AT_SUBMIT", result.getAuditItems().get(0).getSnapshotSource());
        assertEquals(new BigDecimal("12"), result.getAuditItems().get(0).getUpperLimit());
        verify(snapshotMapper, never()).selectById(any());
    }

    private void assertReason(MesProFrontlineFeedbackSubmitReqVO req, String reason,
                              MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        when(snapshotMapper.selectById(5101L)).thenReturn(snapshot);
        MesFrontlineParameterAuditResult result = service.resolveAndApply(req);
        assertEquals("UNRESOLVED", result.getParameterAuditStatus());
        assertEquals(reason, result.getAuditItems().get(0).getReasonCode());
    }

    private void stubFrozenSnapshot(MesDeviceParameterSnapshotRule rule) {
        String json = JsonUtils.toJsonString(List.of(rule));
        when(snapshotMapper.selectById(5101L)).thenReturn(validSnapshot()
                .setParameterSnapshotJson(json)
                .setParameterSnapshotSha256(MesDeviceParameterSnapshotCodec.sha256(json)));
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO validSnapshot() {
        MesDeviceParameterSnapshotRule rule = frozenRule(501L, "pressure", "10");
        String json = JsonUtils.toJsonString(List.of(rule));
        return new MesProcessPoolActiveOrderProcessSnapshotDO()
                .setId(5101L)
                .setActiveOrderId(8101L)
                .setWorkOrderId(41L)
                .setRouteId(21L)
                .setRouteProcessId(71L)
                .setProcessId(31L)
                .setParameterSnapshotState("FROZEN")
                .setParameterSnapshotJson(json)
                .setParameterSnapshotSha256(MesDeviceParameterSnapshotCodec.sha256(json));
    }

    private static MesDeviceParameterSnapshotRule frozenRule(Long deviceId, String code, String upperLimit) {
        return MesDeviceParameterSnapshotRule.builder()
                .routeProcessId(71L)
                .processId(31L)
                .deviceId(deviceId)
                .parameterCode(code)
                .parameterName("压力")
                .unit("MPa")
                .lowerLimit(BigDecimal.ZERO)
                .upperLimit(new BigDecimal(upperLimit))
                .valueType("DECIMAL")
                .build();
    }

    private static MesProFrontlineFeedbackSubmitReqVO request(
            MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO... readings) {
        return new MesProFrontlineFeedbackSubmitReqVO()
                .setFeedbackPayload(new MesProFrontlineFeedbackPayloadReqVO()
                        .setActiveOrderProcessSnapshotId(5101L)
                        .setWorkOrderId(41L)
                        .setRouteId(21L)
                        .setProcessId(31L)
                        .setSelectedDevice(new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO()
                                .setDeviceId(501L))
                        .setDeviceParameterReadings(List.of(readings)))
                .setProcessPoolContext(new MesProFrontlineProcessPoolContextReqVO()
                        .setActiveOrderId(8101L)
                        .setWorkOrderId(41L)
                        .setRouteId(21L)
                        .setRouteProcessId(71L)
                        .setProcessId(31L));
    }

    private static MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading(
            Long deviceId, String code) {
        return new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                .setDeviceId(deviceId)
                .setParameterCode(code)
                .setValue(new BigDecimal("5"));
    }
}
