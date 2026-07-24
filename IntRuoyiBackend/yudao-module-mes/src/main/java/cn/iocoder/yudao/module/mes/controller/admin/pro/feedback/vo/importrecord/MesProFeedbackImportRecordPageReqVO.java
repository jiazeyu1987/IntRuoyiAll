package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Schema(description = "管理后台 - MES 第三方报工待归属分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MesProFeedbackImportRecordPageReqVO extends PageParam {

    @Schema(description = "导入记录编号", example = "1")
    private Long id;

    @Schema(description = "本次导入记录编号列表", example = "1,2,3")
    private List<Long> importRecordIds;

    @Schema(description = "正式报工编号", example = "100")
    private Long feedbackId;

    @Schema(description = "归属状态", example = "PENDING")
    private String attributionStatus;
}
