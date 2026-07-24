package cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Device-process detail rows synced from the final balloon Excel.
 */
@TableName("mes_dv_machinery_process")
@KeySequence("mes_dv_machinery_process_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesDvMachineryProcessDO extends BaseDO {

    @TableId
    private Long id;

    private Long machineryId;

    private Long processId;

    private String processCode;

    private String machineryCode;

    private String lineName;

    private String processName;

    private String deviceName;

    private BigDecimal deviceQuantity;

    private BigDecimal tenHalfHourDailyCapacity;

    private BigDecimal standardHourlyCapacity;

    private Integer sourceRowNo;

    private String remark;
}
