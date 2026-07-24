package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RuntimeOpsBusinessHealthCheckResult {

    private String code;
    private String name;
    private RuntimeOpsInspectionStatus status;
    private String evidence;
    private String reason;
    private LocalDateTime sampledAt;

    public static RuntimeOpsBusinessHealthCheckResult pass(String code, String name, String evidence,
                                                           LocalDateTime sampledAt) {
        return new RuntimeOpsBusinessHealthCheckResult(code, name, RuntimeOpsInspectionStatus.PASS,
                evidence, null, sampledAt);
    }

    public static RuntimeOpsBusinessHealthCheckResult blocked(String code, String name, String reason,
                                                              LocalDateTime sampledAt) {
        return new RuntimeOpsBusinessHealthCheckResult(code, name, RuntimeOpsInspectionStatus.BLOCKED,
                null, reason, sampledAt);
    }
}
