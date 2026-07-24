package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 表单中心实例草稿保存 Request VO")
@Data
public class FormInstanceDraftReqVO {

    @Schema(description = "表单填写数据")
    private Map<String, Object> formData;

    @Schema(description = "附件编号列表")
    private String attachmentIds;

}
