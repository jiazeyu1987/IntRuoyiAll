package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - Codex 测试项分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CodexTestCasePageReqVO extends PageParam {

    @Schema(description = "测试项名称")
    private String name;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "执行方式")
    private String executionMode;

}
