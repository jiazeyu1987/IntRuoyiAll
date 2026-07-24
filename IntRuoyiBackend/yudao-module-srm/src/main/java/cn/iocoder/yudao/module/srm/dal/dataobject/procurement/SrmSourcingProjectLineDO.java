package cn.iocoder.yudao.module.srm.dal.dataobject.procurement;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("srm_sourcing_project_line")
@KeySequence("srm_sourcing_project_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSourcingProjectLineDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long projectId;

    private Long sourcePlanId;

    private Long sourcePlanLineId;

    private String lineNo;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private BigDecimal quantity;

    private String unit;

    private LocalDate requiredDate;
}
