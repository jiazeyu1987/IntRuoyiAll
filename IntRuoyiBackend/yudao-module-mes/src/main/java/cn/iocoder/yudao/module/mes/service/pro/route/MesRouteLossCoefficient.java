package cn.iocoder.yudao.module.mes.service.pro.route;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.math.BigDecimal;

/** 流程级损耗系数；未设置时按业务默认值1.1执行，与排产系数独立。 */
public final class MesRouteLossCoefficient {
    public static final BigDecimal DEFAULT = new BigDecimal("1.1");
    private MesRouteLossCoefficient() {}

    public static BigDecimal read(JSONObject config) {
        if (config == null || !config.containsKey("lossCoefficient")) return DEFAULT;
        BigDecimal value = config.getBigDecimal("lossCoefficient");
        if (value == null || value.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("损耗系数必须大于等于1");
        }
        return value;
    }

    public static BigDecimal frozen(String productionConfigJson) {
        return read(productionConfigJson == null ? null : JSON.parseObject(productionConfigJson));
    }
}
