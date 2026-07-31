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

import java.time.LocalDateTime;

@TableName("dcc_nas_control_audit_task")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccNasControlAuditTaskDO extends BaseDO {

    @TableId
    private Long id;

    private Long operatorUserId;

    private String nasShareName;

    private String scanRootsJson;

    private String status;

    private String currentPath;

    private Long scannedFileCount;

    private Long controlledFileCount;

    private Long notControlledFileCount;

    private Long ambiguousFileCount;

    private Long sourceMissingCount;

    private Long skippedDirectoryCount;

    private Long reportFileId;

    private String reportFileName;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String failureReason;

    private Long tenantId;
}
