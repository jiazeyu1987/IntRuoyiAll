package cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - ERP 表格自动同步立即执行 Response VO")
@Data
@Accessors(chain = true)
public class ErpKingdeeTableAutoSyncRunOnceRespVO {

    @Schema(description = "状态")
    private String status;

    @Schema(description = "总同步类型数")
    private Integer totalSyncCount;

    @Schema(description = "成功同步类型数")
    private Integer successSyncCount;

    @Schema(description = "失败信息")
    private String failureMessage;

    @Schema(description = "执行明细")
    private List<ErpKingdeeTableAutoSyncRunItemRespVO> items = new ArrayList<>();
}
