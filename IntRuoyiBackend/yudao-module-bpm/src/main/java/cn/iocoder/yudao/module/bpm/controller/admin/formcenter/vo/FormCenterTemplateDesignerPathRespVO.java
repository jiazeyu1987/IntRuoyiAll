package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 表单中心模板 Jimu 路径 Response VO")
@Data
public class FormCenterTemplateDesignerPathRespVO {

    @Schema(description = "Jimu 相对路径")
    private String path;

}
