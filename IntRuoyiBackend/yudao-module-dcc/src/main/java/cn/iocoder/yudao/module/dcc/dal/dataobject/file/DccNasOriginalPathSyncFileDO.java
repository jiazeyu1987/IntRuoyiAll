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

@TableName("dcc_nas_original_path_sync_file")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccNasOriginalPathSyncFileDO extends BaseDO {

    @TableId
    private Long id;

    private Long auditTaskId;

    private Long auditFileId;

    private Long transferTaskId;

    private Long transferTaskItemId;

    private Long sourceFileId;

    private String nasShareName;

    private String rootPath;

    private String normalizedRelativePath;

    private String pathHash;

    private String fileName;

    private Long fileSize;

    private LocalDateTime modifiedAt;

    private String sourceSignature;

    private String syncStatus;

    private Long syncedByUserId;

    private LocalDateTime syncedAt;

    private Long deletedByUserId;

    private LocalDateTime deletedAt;

    private Long tenantId;
}
