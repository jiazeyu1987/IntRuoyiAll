package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_publication_impact_audit")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccPublicationImpactAuditDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long taskId;
    private Long batchId;
    private String actionType;
    private Long actorId;
    private String reason;
    private String statusBefore;
    private String statusAfter;
    private Long assigneeBefore;
    private Long assigneeAfter;
    private String decisionSnapshot;
    private Long linkedRevisionControlledFileId;
    private Integer rowVersionBefore;
    private Integer rowVersionAfter;
    private LocalDateTime occurredAt;
}
