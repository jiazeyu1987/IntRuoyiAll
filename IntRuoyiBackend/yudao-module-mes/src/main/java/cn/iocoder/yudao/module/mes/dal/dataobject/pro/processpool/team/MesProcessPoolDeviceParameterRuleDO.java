package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@TableName("mes_pro_process_pool_device_parameter_rule")
@KeySequence("mes_pro_process_pool_device_parameter_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolDeviceParameterRuleDO extends TenantBaseDO {

    public static final String VALUE_TYPE_INTEGER = "INTEGER";
    public static final String VALUE_TYPE_DECIMAL = "DECIMAL";
    public static final String VALUE_TYPE_TEXT_STANDARD = "TEXT_STANDARD";
    public static final String VALUE_TYPE_SELECT = "SELECT";

    @TableId
    private Long id;

    private Long leaderUserId;
    private Long routeProcessId;
    private Long processId;
    private Long deviceId;
    private String parameterCode;
    private String parameterName;
    private String unit;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private BigDecimal defaultValue;
    private String valueType;
    private String standardText;
    private String optionValuesJson;
    private String defaultText;
    private Integer decimalScale;
    private Boolean enabled;
}
