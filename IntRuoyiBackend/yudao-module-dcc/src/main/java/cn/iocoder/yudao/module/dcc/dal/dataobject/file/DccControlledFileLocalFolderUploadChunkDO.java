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

@TableName("dcc_controlled_file_local_folder_upload_chunk")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileLocalFolderUploadChunkDO extends BaseDO {

    @TableId
    private Long id;

    private Long taskId;

    private String relativePath;

    private String fileName;

    private Long fileSize;

    private Integer chunkIndex;

    private Integer totalChunks;

    private Long chunkSize;

    private String chunkSha256;

    private String chunkTempPath;

    private String status;
}
