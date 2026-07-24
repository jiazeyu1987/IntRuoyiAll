package cn.iocoder.yudao.module.srm.dal.dataobject.coderule;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SRM 编码规则 DO。
 */
@TableName("srm_code_rule")
@KeySequence("srm_code_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmCodeRuleDO extends BaseDO {

    /**
     * 规则编号。
     */
    @TableId
    private Long id;
    /**
     * 规则编码。
     */
    private String ruleCode;
    /**
     * 规则名称。
     */
    private String ruleName;
    /**
     * 目标表单。
     */
    private String targetForm;
    /**
     * 编码前缀。
     */
    private String prefix;
    /**
     * 日期格式。
     */
    private String datePattern;
    /**
     * 是否启用日期段。
     */
    private Boolean dateSegmentEnabled;
    /**
     * 流水宽度。
     */
    private Integer serialWidth;
    /**
     * 流水步长。
     */
    private Integer step;
    /**
     * 最小流水。
     */
    private Long minSerial;
    /**
     * 最大流水。
     */
    private Long maxSerial;
    /**
     * 分隔符。
     */
    @TableField("`separator`")
    private String separator;
    /**
     * 是否启用。
     */
    private Boolean enabled;
    /**
     * 备注。
     */
    private String remark;

}
