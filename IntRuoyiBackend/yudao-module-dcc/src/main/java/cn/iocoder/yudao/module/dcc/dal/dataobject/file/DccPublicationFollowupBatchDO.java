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

@TableName("dcc_publication_followup_batch")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccPublicationFollowupBatchDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long publishedControlledFileId;
    private Long publishedMasterId;
    private Long previousActiveControlledFileId;
    private Long dccProjectCodeId;
    private Long categoryId;
    private Long directoryId;
    private Long fileTypeTaxonomyLeafId;
    private String fileNumberSnapshot;
    private String fileNameSnapshot;
    private String versionNoSnapshot;
    private String status;
    private LocalDateTime publishedAt;
    private String creationToken;
}
