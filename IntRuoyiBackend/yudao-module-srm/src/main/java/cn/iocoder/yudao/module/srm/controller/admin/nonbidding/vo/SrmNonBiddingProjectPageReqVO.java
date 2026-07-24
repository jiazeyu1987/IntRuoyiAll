package cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmNonBiddingProjectPageReqVO extends PageParam {

    private String projectNo;

    private String projectTitle;

    private String projectStatus;

    private Long supplierId;
}
