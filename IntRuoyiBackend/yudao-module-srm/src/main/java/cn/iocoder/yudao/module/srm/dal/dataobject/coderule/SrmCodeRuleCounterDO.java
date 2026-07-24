package cn.iocoder.yudao.module.srm.dal.dataobject.coderule;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SRM 编码规则计数器 DO。
 */
@TableName("srm_code_rule_counter")
@KeySequence("srm_code_rule_counter_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmCodeRuleCounterDO extends BaseDO {

    /**
     * 计数器编号。
     */
    @TableId
    private Long id;
    /**
     * 编码规则编号。
     */
    private Long ruleId;
    /**
     * 目标表单。
     */
    private String targetForm;
    /**
     * 周期键。
     */
    private String periodKey;
    /**
     * 当前流水。
     */
    private Long currentSerial;
    /**
     * 最近生成编号。
     */
    private String lastCode;
    /**
     * 最近生成时间。
     */
    private LocalDateTime lastGeneratedAt;
    /**
     * 乐观版本。
     */
    private Integer version;

}
