package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DccControlledFileLocalFolderImportChunkReqVO {

    @NotBlank(message = "相对路径不能为空")
    private String relativePath;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 0, message = "文件大小不能小于 0")
    private Long fileSize;

    @NotNull(message = "分片序号不能为空")
    @Min(value = 0, message = "分片序号不能小于 0")
    private Integer chunkIndex;

    @NotNull(message = "分片总数不能为空")
    @Min(value = 1, message = "分片总数必须大于 0")
    private Integer totalChunks;

    @NotBlank(message = "分片校验值不能为空")
    private String chunkSha256;

    private String contentType;

    @NotNull(message = "分片文件不能为空")
    private MultipartFile chunk;
}
