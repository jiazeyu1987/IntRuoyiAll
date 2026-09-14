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

@TableName("dcc_publication_relation_snapshot")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccPublicationRelationSnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long batchId;
    private Long relatedMasterId;
    private Long relatedActiveControlledFileId;
    private String relatedFileNumberSnapshot;
    private String relatedFileNameSnapshot;
    private String relatedVersionNoSnapshot;
    private Long responsibleUserIdSnapshot;
    private String responsibleUserNameSnapshot;
    private Integer responsibleUserStatusSnapshot;
    private String resolutionStatus;
    private LocalDateTime frozenAt;
}
