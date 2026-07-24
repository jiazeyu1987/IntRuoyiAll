package cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 展厅关键词分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class KeywordPageReqVO extends PageParam {

    @Schema(description = "关键词，同时匹配中文和英文", example = "INT")
    private String keyword;

}
