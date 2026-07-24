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

@TableName("dcc_controlled_file_nas_transfer_task_item")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileNasTransferTaskItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long taskId;

    private Long parentItemId;

    private String itemType;

    private String nasPath;

    private String itemName;

    private Long sourceFileId;

    private String status;

    private Integer attemptCount;

    private String failureStage;

    private String lastError;

    private Long resolvedDirectoryId;

    private Long resolvedCategoryId;

    private String directoryOutcome;

    private String categoryOutcome;

    private Boolean previewDownloadOnly;

    private LocalDateTime lastAttemptAt;

    private LocalDateTime completedAt;
}
