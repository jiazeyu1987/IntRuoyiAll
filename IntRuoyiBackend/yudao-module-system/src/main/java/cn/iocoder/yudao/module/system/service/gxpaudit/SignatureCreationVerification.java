package cn.iocoder.yudao.module.system.service.gxpaudit;

import java.util.List;

public record SignatureCreationVerification(String auditBindingStatus, List<String> reasonCodes, Long auditEventId) {
    public SignatureCreationVerification {
        reasonCodes = List.copyOf(reasonCodes);
    }
}
