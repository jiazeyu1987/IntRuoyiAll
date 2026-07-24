package cn.iocoder.yudao.module.srm.controller.admin.tender.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;

@Data
public class SrmTenderProjectPageReqVO extends PageParam {

    private String projectNo;

    private String projectTitle;

    private String projectStatus;

    private Long supplierId;
}
