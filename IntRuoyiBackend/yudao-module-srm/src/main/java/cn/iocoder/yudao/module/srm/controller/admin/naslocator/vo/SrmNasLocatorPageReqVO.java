package cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - SRM NAS定位文件分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmNasLocatorPageReqVO extends PageParam {

    @Schema(description = "文件名关键字", example = "手册")
    private String keyword;
}
