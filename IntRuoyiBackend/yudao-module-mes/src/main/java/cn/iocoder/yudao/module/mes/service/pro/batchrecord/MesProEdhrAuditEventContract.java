package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrContractErrorCodeConstants.PRO_EDHR_CONTRACT_AUDIT_EVENT_FIELD_REQUIRED;

@Data
@Accessors(chain = true)
public class MesProEdhrAuditEventContract {

    private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    private String sourceModule;

    private String sourceObjectId;

    private String action;

    private String permissionCode;

    private String result;

    private String reason;

    private String evidenceHash;

    private String idempotencyKey;

    public MesProEdhrAuditEventContract validateRequiredFields() {
        requireText(sourceModule, "sourceModule");
        requireText(sourceObjectId, "sourceObjectId");
        requireEnumName(action, MesProEdhrAuditAction.names(), "action");
        requireText(permissionCode, "permissionCode");
        requireEnumName(result, MesProEdhrAuditResult.names(), "result");
        requireText(reason, "reason");
        requireEvidenceHash(evidenceHash);
        idempotencyKey = MesProEdhrIdempotencySupport.requireIdempotencyKey(idempotencyKey, action);
        return this;
    }

    private static void requireText(String value, String field) {
        if (StrUtil.isBlank(value)) {
            throw exception(PRO_EDHR_CONTRACT_AUDIT_EVENT_FIELD_REQUIRED, field);
        }
    }

    private static void requireEnumName(String value, Iterable<String> allowedNames, String field) {
        requireText(value, field);
        for (String allowedName : allowedNames) {
            if (allowedName.equals(value)) {
                return;
            }
        }
        throw exception(PRO_EDHR_CONTRACT_AUDIT_EVENT_FIELD_REQUIRED, field);
    }

    private static void requireEvidenceHash(String value) {
        requireText(value, "evidenceHash");
        if (!SHA256_HEX_PATTERN.matcher(value).matches()) {
            throw exception(PRO_EDHR_CONTRACT_AUDIT_EVENT_FIELD_REQUIRED, "evidenceHash");
        }
    }
}
