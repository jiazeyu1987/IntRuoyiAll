package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool;

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

@TableName("mes_pro_process_pool_review_copy_rule")
@KeySequence("mes_pro_process_pool_review_copy_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolReviewCopyRuleDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long processId;
    private Long deviceId;
    private String templateType;
    private String fieldCode;
    private String fieldName;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private String valueType;
    private Boolean affectsAllocation;
    private String allocationField;
    private String sourceQuantityType;
    private String templateFieldMetadataJson;
    private Boolean enabled;
}
