package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;

@Data
public class DccCategoryViewMatrixPageReqVO {

    private String code;
    private String name;
    private Boolean active;
    private Boolean configured;

}
