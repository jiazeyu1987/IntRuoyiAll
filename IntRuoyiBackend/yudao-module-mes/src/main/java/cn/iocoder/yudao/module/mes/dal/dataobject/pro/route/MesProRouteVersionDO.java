package cn.iocoder.yudao.module.mes.dal.dataobject.pro.route;

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

import java.time.LocalDateTime;

/**
 * MES 工艺路线版本 DO
 */
@TableName("mes_pro_route_version")
@KeySequence("mes_pro_route_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProRouteVersionDO extends BaseDO {

    @TableId
    private Long id;

    private Long routeId;

    private String versionNo;

    private Boolean active;

    private String lifecycleStatus;

    private Integer activeUniqueFlag;

    private Long sourceRouteVersionId;

    private String routeSnapshotJson;

    private String changeSummaryJson;

    private String validationResultJson;

    private Long submittedBy;

    private LocalDateTime submittedTime;

    private String approvalProcessInstanceId;

    private Long publishedBy;

    private LocalDateTime publishedTime;

    private String remark;

}
