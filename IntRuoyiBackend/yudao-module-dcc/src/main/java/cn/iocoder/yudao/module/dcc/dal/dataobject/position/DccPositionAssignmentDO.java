package cn.iocoder.yudao.module.dcc.dal.dataobject.position;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DCC position assignment.
 */
@TableName("dcc_position_assignment")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccPositionAssignmentDO extends BaseDO {

    @TableId
    private Long id;
    private Long positionId;
    private String assignmentType;
    private Long systemPostId;
    private Long userId;
    private Boolean active;
    private String changeReason;

}
