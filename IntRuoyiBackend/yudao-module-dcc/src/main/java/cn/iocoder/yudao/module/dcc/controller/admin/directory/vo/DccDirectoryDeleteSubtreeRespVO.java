package cn.iocoder.yudao.module.dcc.controller.admin.directory.vo;

import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryDeleteSubtreeResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - DCC 删除目录子树 Response VO")
@Data
public class DccDirectoryDeleteSubtreeRespVO {

    @Schema(description = "删除目录数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer directoryCount;

    @Schema(description = "删除受控文件数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "12")
    private Integer controlledFileCount;

    @Schema(description = "删除文件主链数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    private Integer masterCount;

    @Schema(description = "删除底层上传文件数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    private Integer infraFileCount;

    public static DccDirectoryDeleteSubtreeRespVO of(DccDirectoryDeleteSubtreeResult result) {
        DccDirectoryDeleteSubtreeRespVO respVO = new DccDirectoryDeleteSubtreeRespVO();
        respVO.setDirectoryCount(result.getDirectoryCount());
        respVO.setControlledFileCount(result.getControlledFileCount());
        respVO.setMasterCount(result.getMasterCount());
        respVO.setInfraFileCount(result.getInfraFileCount());
        return respVO;
    }

}
