package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

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
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_pro_edhr_permission_rule")
@KeySequence("mes_pro_edhr_permission_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrPermissionRuleDO extends BaseDO {

    @TableId
    private Long id;

    private Long scopeId;

    private String subjectType;

    private Long subjectId;

    private String ability;

    private String decision;

    private Integer priority;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private String status;

    private Integer version;

    private Long createUserId;

    private Long updateUserId;
}
