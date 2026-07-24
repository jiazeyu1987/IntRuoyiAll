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

@TableName("dcc_controlled_file_metadata_change")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileMetadataChangeDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long assignmentId;
    private Long projectCodeId;
    private Long controlledFileId;
    private Long masterId;
    private Long operatorUserId;
    private String source;
    private String requestId;
    private String changeReason;
    private Integer changedFieldCount;
    private String beforeSnapshotJson;
    private String afterSnapshotJson;
    private LocalDateTime changedTime;

}
