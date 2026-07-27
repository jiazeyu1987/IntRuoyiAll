package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - Codex 测试节点串选项 Response VO")
@Data
public class CodexTestNodeChainOptionRespVO {

    @Schema(description = "节点串名称")
    private String name;

    @Schema(description = "所属项目")
    private String project;

    @Schema(description = "节点数量")
    private Integer nodeCount;

}
