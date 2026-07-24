package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProScheduleOrderBusinessOptimizationContractTest {

    private static final Path SCHEDULE_ORDER_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderServiceImpl.java");
    private static final Path AUTO_SCHEDULE_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java");
    private static final Path AUTO_SCHEDULE_REQ = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/schedule/vo/MesProAutoSchedulePreviewReqVO.java");

    @Test
    void scheduleOrderSnapshot_shouldApplyScheduleRouteFlowEnabledStateAndExplainDisabledProcesses() throws Exception {
        String source = Files.readString(SCHEDULE_ORDER_SERVICE, StandardCharsets.UTF_8);

        assertTrue(source.contains("MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()"),
                "排产工单快照必须读取 SCHEDULE 用途工序配置，不能只按工艺路线工序默认全部启用");
        assertTrue(source.contains("resolveScheduleRouteFlowConfigMap"),
                "排产工单快照必须集中解析 SCHEDULE 用途配置，便于追溯启停口径");
        assertTrue(source.contains("resolveScheduleProcessEnabled"),
                "工序快照 enabled 必须由 SCHEDULE 用途启停状态决定");
        assertTrue(source.contains("BLOCKED_ROUTE_PROCESS_DISABLED_FOR_SCHEDULE"),
                "排产前检查必须明确提示工序因智能排产用途关闭而不可排");
    }

    @Test
    void autoScheduleApply_shouldRequireBusinessReasonWhileReplanApplyCanOmitItAndPersistAuditPayload() throws Exception {
        String serviceSource = Files.readString(AUTO_SCHEDULE_SERVICE, StandardCharsets.UTF_8);
        String reqSource = Files.readString(AUTO_SCHEDULE_REQ, StandardCharsets.UTF_8);

        assertTrue(reqSource.contains("private String reason"),
                "自动排产发布/重排请求仍应保留可选业务原因字段");
        assertTrue(serviceSource.contains("prepareApplyReason(reqVO, operationType)"),
                "自动排产发布与手动重排必须先按操作类型统一整理 reason 字段");
        assertTrue(serviceSource.contains("validateRequiredApplyReason(reqVO)"),
                "自动排产发布仍必须在写入前校验业务原因");
        assertTrue(serviceSource.contains("normalizeOptionalApplyReason(reqVO)"),
                "手动重排发布缺少业务原因时必须允许继续并把空白 reason 规范化");
        assertTrue(serviceSource.contains("payload.put(\"reason\", reqVO.getReason())"),
                "自动排产事件审计 payload 必须记录业务原因");
        assertTrue(serviceSource.contains(".reason(reqVO.getReason())"),
                "排产工单操作日志 reason 必须使用操作者填写的业务原因");
    }
}
