package cn.iocoder.yudao.module.mes.service.pro.frontline;

public record MesFrontlineDefectReasonOption(Long reasonId,
                                             String reasonType,
                                             String reasonCode,
                                             String reasonName) {

    // System-owned reason: never persisted in a tenant's loss catalog or route bindings.
    public static final long OTHER_LOSS_ID = -1L;
    public static final MesFrontlineDefectReasonOption OTHER_LOSS =
            new MesFrontlineDefectReasonOption(OTHER_LOSS_ID, "LOSS", "SYSTEM_OTHER_LOSS", "其他损耗");

    public static boolean isValidLossReasonId(Long reasonId) {
        return reasonId != null && (reasonId > 0 || reasonId == OTHER_LOSS_ID);
    }
}
