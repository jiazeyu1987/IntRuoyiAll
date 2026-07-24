package cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - SRM NAS定位黑名单 Response VO")
@Data
public class SrmNasLocatorBlacklistRespVO {

    @Schema(description = "黑名单文件名模式列表", example = "[\"*.pyc\",\"*MO13*.pdf\"]")
    private List<String> patterns;
}
