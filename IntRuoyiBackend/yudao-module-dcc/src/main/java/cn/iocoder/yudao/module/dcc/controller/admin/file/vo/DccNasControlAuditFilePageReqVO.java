package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "Admin - DCC NAS control audit file page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DccNasControlAuditFilePageReqVO extends PageParam {

    @Schema(description = "Keyword for file name or NAS relative path")
    private String keyword;

    @Schema(description = "Classification status")
    private String classificationStatus;

    @Schema(description = "Download status")
    private String downloadStatus;

    @Schema(description = "Archive status")
    private String archiveStatus;
}