package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;

/**
 * Validates and protects the immutable UDI control-document reference captured by PQC approval.
 */
public final class MesPqcProductionReleaseUdiPolicy {

    public static final int MAX_LENGTH = 128;

    private MesPqcProductionReleaseUdiPolicy() {
    }

    public static String requireNormalized(String value) {
        String normalized = StrUtil.trim(value);
        if (StrUtil.isBlank(normalized)) {
            throw new IllegalArgumentException("UDI编号不能为空");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("UDI编号长度不能超过128个字符");
        }
        return normalized;
    }

    public static WriteDecision checkCompatibility(MesProcessPoolActiveOrderDO activeOrder,
                                                   String normalizedValue) {
        if (activeOrder == null) {
            throw new IllegalStateException("正式活跃订单来源不存在，无法保存UDI编号");
        }
        String existingValue = activeOrder.getUdiControlDocumentNo();
        if (existingValue == null) {
            return WriteDecision.WRITE;
        }
        if (existingValue.equals(normalizedValue)) {
            return WriteDecision.IDEMPOTENT;
        }
        throw new IllegalStateException("活跃订单已有不同的UDI编号，不能覆盖");
    }

    public enum WriteDecision {
        WRITE,
        IDEMPOTENT
    }
}
