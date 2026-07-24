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

/**
 * DCC category approval route node.
 */
@TableName("dcc_category_approval_route_node")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccCategoryApprovalRouteNodeDO extends BaseDO {

    @TableId
    private Long id;
    private Long routeId;
    private Integer stageNo;
    private String stageCode;
    private String stageName;
    private Integer stageOrder;
    private String candidateSourceType;
    private Long candidateSourceId;
    private String candidateSourceIds;
    private String approveMethod;
    private Integer approveRatio;
    private Boolean requireAllApprovals;
    private Boolean required;
    private Integer sort;
    private String stageType;
    private String subjectLabel;
    private String marker;
    private String subjectType;
    private Long subjectId;
    private String subjectName;
    private String subjectDepartmentPath;
    private String ruleRemark;

}
