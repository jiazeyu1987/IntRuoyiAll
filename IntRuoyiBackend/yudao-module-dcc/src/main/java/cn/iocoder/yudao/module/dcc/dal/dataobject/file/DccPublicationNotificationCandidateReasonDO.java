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

@TableName("dcc_publication_notification_candidate_reason")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccPublicationNotificationCandidateReasonDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long batchId;
    private Long candidateId;
    private String reasonType;
    private Long sourceId;
    private Long relatedMasterId;
    private String reasonSummary;
}
