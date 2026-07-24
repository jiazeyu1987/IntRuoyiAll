package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccProjectCodeAssignmentFilePageReqVO extends PageParam {

    private String keyword;
    private Boolean changed;
    private Long categoryId;
    private String fileTypeLevel2;
    private String fileTypeLevel3;

}
