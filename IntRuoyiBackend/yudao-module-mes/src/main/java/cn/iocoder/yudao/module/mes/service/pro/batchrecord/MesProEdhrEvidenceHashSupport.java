package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrContractErrorCodeConstants.PRO_EDHR_CONTRACT_EVIDENCE_HASH_INPUT_REQUIRED;

public final class MesProEdhrEvidenceHashSupport {

    private MesProEdhrEvidenceHashSupport() {
    }

    public static String sha256(String sourceModule, String sourceObjectId, String evidenceType, String payload) {
        String normalized = String.join("\n",
                requirePart(sourceModule, "sourceModule"),
                requirePart(sourceObjectId, "sourceObjectId"),
                requirePart(evidenceType, "evidenceType"),
                requirePart(payload, "payload"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 digest is unavailable", error);
        }
    }

    private static String requirePart(String value, String field) {
        if (StrUtil.isBlank(value)) {
            throw exception(PRO_EDHR_CONTRACT_EVIDENCE_HASH_INPUT_REQUIRED, field);
        }
        return value.trim();
    }
}
