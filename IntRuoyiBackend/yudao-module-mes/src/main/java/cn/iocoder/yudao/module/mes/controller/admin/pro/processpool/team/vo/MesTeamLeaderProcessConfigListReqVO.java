package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长工序配置列表 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderProcessConfigListReqVO {

    @Schema(description = "工艺路线编码或名称关键字")
    @Size(max = 128, message = "工艺路线关键字长度不能超过 128 个字符")
    private String routeKeyword;

    @Schema(description = "工序编码或名称关键字")
    @Size(max = 128, message = "工序关键字长度不能超过 128 个字符")
    private String processKeyword;

    @Schema(description = "损耗原因描述关键字")
    @Size(max = 128, message = "损耗原因关键字长度不能超过 128 个字符")
    private String lossReasonKeyword;

    @Schema(description = "映射设备编码或名称关键字")
    @Size(max = 128, message = "映射设备关键字长度不能超过 128 个字符")
    private String deviceKeyword;

    @Schema(description = "设备参数编码或名称关键字")
    @Size(max = 128, message = "设备参数关键字长度不能超过 128 个字符")
    private String parameterKeyword;
}
