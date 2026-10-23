package cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo;

/** Ready/open facts and confirmed duration for one authenticated user's exact file version. */
public record DccPreviewReadingSummaryRespVO(String firstOpenedAt, String lastOpenedAt,
        long accumulatedSeconds, long openedSessionCount) {}
