package cn.iocoder.yudao.module.dcc.service.audit;

/** Immutable lifecycle action audit written for both successful and failed operations. */
public record DccLifecycleLogCreateCommand(Long fileId,
                                           String versionNo,
                                           Long userId,
                                           String actionType,
                                           String result,
                                           String failureCode,
                                           String reason) {
}
