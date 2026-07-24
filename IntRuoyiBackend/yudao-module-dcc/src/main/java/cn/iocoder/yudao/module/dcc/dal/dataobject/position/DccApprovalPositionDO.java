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
 * DCC approval position.
 */
@TableName("dcc_approval_position")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccApprovalPositionDO extends BaseDO {

    @TableId
    private Long id;
    private String code;
    private String name;
    private Boolean active;
    private String source;
    private String remark;

}
