package cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - MES 工序只读目录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProMesProcessPageReqVO extends PageParam {

    @Schema(description = "关键词，匹配产品、工序、设备编码、设备名称或批记录工序")
    private String keyword;
}
