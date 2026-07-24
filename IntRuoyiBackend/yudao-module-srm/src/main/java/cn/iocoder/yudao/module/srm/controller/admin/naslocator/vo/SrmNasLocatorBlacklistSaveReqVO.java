package cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - SRM NAS定位黑名单保存 Request VO")
@Data
public class SrmNasLocatorBlacklistSaveReqVO {

    @Schema(description = "黑名单文件名模式列表", example = "[\"*.pyc\",\"*MO13*.pdf\"]")
    private List<String> patterns;
}
