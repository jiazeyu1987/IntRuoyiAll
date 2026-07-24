package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileLocalFolderImportChunkRespVO {

    private Long taskId;

    private String relativePath;

    private Integer uploadedChunkCount;

    private Integer totalChunks;

    private Boolean fileCompleted;

    private DccControlledFileNasTransferRespVO task;
}
