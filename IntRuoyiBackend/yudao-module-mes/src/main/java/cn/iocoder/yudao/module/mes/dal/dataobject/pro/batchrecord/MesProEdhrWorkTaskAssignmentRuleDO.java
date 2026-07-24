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

@TableName("mes_pro_edhr_work_task_assignment_rule")
@KeySequence("mes_pro_edhr_work_task_assignment_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrWorkTaskAssignmentRuleDO extends BaseDO {

    @TableId
    private Long id;

    private Long routeProcessId;

    private String scopeType;

    private Long scopeId;

    private String taskType;

    private Long assigneeUserId;

    private Long reviewUserId;

    private String candidateSourceType;

    private Long candidateSourceId;

    private Integer dueMinutes;

    private Boolean enabled;

    private String remark;
}
