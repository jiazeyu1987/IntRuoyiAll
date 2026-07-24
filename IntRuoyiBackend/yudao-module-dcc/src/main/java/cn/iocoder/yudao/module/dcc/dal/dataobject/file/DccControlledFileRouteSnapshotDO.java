package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

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
 * DCC controlled file route snapshot.
 */
@TableName("dcc_controlled_file_route_snapshot")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileRouteSnapshotDO extends BaseDO {

    @TableId
    private Long id;
    private Long controlledFileId;
    private Integer routeVersionNo;
    private Integer stageNo;
    private String stageCode;
    private String stageName;
    private Integer stageOrder;
    private String candidateSourceType;
    private Long candidateSourceId;
    private String candidateSourceIds;
    private String resolvedUserIds;
    private String approveMethod;
    private Integer approveRatio;
    private Boolean requireAllApprovals;

}
