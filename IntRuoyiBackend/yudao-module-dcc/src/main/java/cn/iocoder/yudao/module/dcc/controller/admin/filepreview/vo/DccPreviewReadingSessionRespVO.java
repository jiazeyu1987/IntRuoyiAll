package cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo;

/** Server timestamps are UTC ISO-8601; duration is confirmed server heartbeat time only. */
public record DccPreviewReadingSessionRespVO(Long sessionId, String openedAt, String startedAt,
        String lastHeartbeatAt, String endedAt, int accumulatedSeconds, int heartbeatIntervalSeconds,
        int maxHeartbeatGapSeconds) {}
