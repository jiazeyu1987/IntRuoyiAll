package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Data
public class DccControlledFileAttachmentRespVO {

    @JsonFormat(shape=JsonFormat.Shape.STRING)
    private Long attachmentId;
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    private Long controlledFileId;
    private Boolean canDownload;
    private String fileName;
    private String contentType;
    private String previewKind;
    private Long fileSize;
    private Integer sortNo;
    private String attachmentPurpose;
    private String applicationProcessInstanceId;
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    private Long uploadedBy;
    private String uploadedUsernameSnapshot;
    private String uploadedNicknameSnapshot;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;

}
