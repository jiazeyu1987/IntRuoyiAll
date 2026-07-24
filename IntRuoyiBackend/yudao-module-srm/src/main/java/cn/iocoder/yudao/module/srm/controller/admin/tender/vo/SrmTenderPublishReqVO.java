package cn.iocoder.yudao.module.srm.controller.admin.tender.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SrmTenderPublishReqVO {

    @NotNull(message = "招标项目编号不能为空")
    private Long projectId;

    @NotBlank(message = "公告标题不能为空")
    private String noticeTitle;

    @NotBlank(message = "公告附件不能为空")
    private String noticeAttachmentUrl;

    @NotBlank(message = "标书名称不能为空")
    private String documentName;

    @NotBlank(message = "标书附件不能为空")
    private String documentAttachmentUrl;

    @NotNull(message = "投标开始时间不能为空")
    private LocalDateTime submissionStartTime;

    @NotNull(message = "投标截止时间不能为空")
    private LocalDateTime submissionEndTime;
}
