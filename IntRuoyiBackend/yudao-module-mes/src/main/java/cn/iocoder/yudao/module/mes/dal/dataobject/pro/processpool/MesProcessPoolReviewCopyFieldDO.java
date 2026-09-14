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

@TableName("mes_pro_process_pool_review_copy_field")
@KeySequence("mes_pro_process_pool_review_copy_field_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolReviewCopyFieldDO extends TenantBaseDO {

    public static final String RULE_CLAMP_TO_MAX = "CLAMP_TO_MAX";
    public static final String RULE_CLAMP_TO_MIN = "CLAMP_TO_MIN";
    public static final String RULE_UNCHANGED_IN_RANGE = "UNCHANGED_IN_RANGE";

    @TableId
    private Long id;

    private Long reviewCopyId;
    private Long eventId;
    private Long sourceQuantityFragmentId;
    private String fieldCode;
    private String fieldName;
    private String rawValue;
    private String correctedValue;
    private String ruleType;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private String valueType;
    private Boolean affectsAllocation;
    private String feedbackSourceType;
    private Long feedbackSourceId;
    private String recordbookSourceType;
    private Long recordbookSourceId;
    private String templateFieldMetadataJson;
}
