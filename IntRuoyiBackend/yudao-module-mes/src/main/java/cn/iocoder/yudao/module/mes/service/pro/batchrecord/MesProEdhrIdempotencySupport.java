package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrContractErrorCodeConstants.PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrContractErrorCodeConstants.PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_REQUIRED;

public final class MesProEdhrIdempotencySupport {

    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{7,127}$");

    private MesProEdhrIdempotencySupport() {
    }

    public static String requireIdempotencyKey(String idempotencyKey, String action) {
        if (StrUtil.isBlank(idempotencyKey)) {
            throw exception(PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_REQUIRED, action);
        }
        String trimmed = idempotencyKey.trim();
        if (!IDEMPOTENCY_KEY_PATTERN.matcher(trimmed).matches()) {
            throw exception(PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_INVALID, action);
        }
        return trimmed;
    }
}
