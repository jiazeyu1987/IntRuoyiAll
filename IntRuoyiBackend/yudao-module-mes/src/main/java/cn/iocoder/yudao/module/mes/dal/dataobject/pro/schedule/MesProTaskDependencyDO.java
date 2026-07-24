package cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("mes_pro_task_dependency")
@KeySequence("mes_pro_task_dependency_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProTaskDependencyDO extends BaseDO {

    @TableId
    private Long id;

    private Long sourceTaskId;

    private Long targetTaskId;

    private Long sourceProcessId;

    private Long targetProcessId;

    private String dependencyType;

    private Boolean enabled;

}
