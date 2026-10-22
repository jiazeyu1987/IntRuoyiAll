package cn.iocoder.yudao.module.dcc.service.projectcode.attributes;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.UUID;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;

/** 使用当前基线正式统一账本端口；操作策略未登记时正式端口必须阻断，绝不退回领域历史表。 */
@Service
public class DccProjectConfigurationAuditService {
    @Resource private GxpAuditService auditService;
    public void validateReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 500) throw fail(INVALID);
    }
    public void append(String operation, Long id, String version, String reason, String before, String after) {
        validateReason(reason);
        auditService.append(GxpAuditCommand.builder().operationId(operation)
                .subjectId("DCC_PROJECT_CONFIGURATION:" + id).subjectVersion(version).reason(reason.trim())
                .beforeState(GxpAuditStateEnvelope.builder().state(before == null ? "ABSENT" : "PRESENT")
                        .objectVersion(version).canonicalJson(before == null ? "{}" : before).build())
                .afterState(GxpAuditStateEnvelope.builder().state(after == null ? "VOIDED" : "PRESENT")
                        .objectVersion(version).canonicalJson(after == null ? "{}" : after).build())
                .idempotencyKey("DCC:B:" + UUID.randomUUID()).source(operation).build());
    }
}
