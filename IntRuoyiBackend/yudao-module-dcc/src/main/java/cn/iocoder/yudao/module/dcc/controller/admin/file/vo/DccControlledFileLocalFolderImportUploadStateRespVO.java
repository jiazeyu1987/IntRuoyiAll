package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileLocalFolderImportUploadStateRespVO {

    private Long taskId;

    private String rootDirectoryName;

    private String status;

    private Long expectedFileCount;

    private Long expectedTotalBytes;

    private Long uploadedFileCount;

    private Long uploadedTotalBytes;

    private List<String> uploadedRelativePaths;

    private List<FileState> files;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileState {

        private String relativePath;

        private Long fileSize;

        private Integer totalChunks;

        private List<Integer> uploadedChunkIndexes;

        private Boolean completed;
    }
}
