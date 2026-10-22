package cn.iocoder.yudao.module.dcc.service.file;

/** This application's two explicit actors. Null means a genuinely unfilled draft choice. */
public record DccApplicationCustomMatrix(Long approverUserId, Long documentControlUserId) {
    public enum ApprovalSourceMode { PUBLIC_MATRIX, APPLICATION_CUSTOM }
}
