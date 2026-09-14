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

@TableName("dcc_controlled_file_related_file")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileRelatedFileDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long controlledFileId;
    private Long relatedControlledFileId;
    private Long projectCodeId;
    private Long relatedMasterId;
    private String relatedFileNumberSnapshot;
    private String relatedFileNameSnapshot;
    private String relatedVersionNoSnapshot;
    private String relationSource;

}
