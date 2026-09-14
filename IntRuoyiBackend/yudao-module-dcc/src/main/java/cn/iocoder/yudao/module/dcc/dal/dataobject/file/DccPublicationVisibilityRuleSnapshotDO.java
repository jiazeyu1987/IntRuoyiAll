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

@TableName("dcc_publication_visibility_rule_snapshot")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccPublicationVisibilityRuleSnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long batchId;
    private String sourceType;
    private Long sourceRuleId;
    private String sourceScope;
    private String subjectType;
    private Long subjectId;
    private Long dccProjectCodeId;
    private Long categoryId;
    private Long directoryId;
    private String sourceSummary;
    private String resolutionStatus;
    private String resolutionMessage;
}
