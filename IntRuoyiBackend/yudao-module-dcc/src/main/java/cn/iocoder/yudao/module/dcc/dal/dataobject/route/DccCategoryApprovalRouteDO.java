package cn.iocoder.yudao.module.dcc.dal.dataobject.route;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * DCC category approval route.
 */
@TableName("dcc_category_approval_route")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccCategoryApprovalRouteDO extends BaseDO {

    @TableId
    private Long id;
    private Long categoryId;
    private Integer versionNo;
    private Boolean active;
    private LocalDateTime effectiveTime;
    private String remark;

}
