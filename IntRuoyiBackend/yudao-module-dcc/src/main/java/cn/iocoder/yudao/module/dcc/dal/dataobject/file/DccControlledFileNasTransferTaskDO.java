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

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("dcc_controlled_file_nas_transfer_task")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileNasTransferTaskDO extends BaseDO {

    @TableId
    private Long id;

    private Long auditTaskId;

    private Long operatorUserId;

    private Long templateCategoryId;

    private Long dccProjectCodeId;

    private Long productMasterId;

    private LocalDate effectiveDate;

    private String selectedNasPathsJson;

    private String sourceType;

    private String idempotencyKey;

    private String requestHash;

    private String status;

    private Long expectedFileCount;

    private Long expectedTotalBytes;

    private Long uploadedFileCount;

    private Long uploadedTotalBytes;

    private LocalDateTime uploadCompletedAt;

    private LocalDateTime nextCheckAt;

    private LocalDateTime lastRunAt;

    private LocalDateTime completedAt;

    private String lastFailureMessage;

    private String failureReportPath;

    private String failureReportGeneratedAt;

    private String failureReportError;
}
