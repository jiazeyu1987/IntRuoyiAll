package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MesProEdhrDhrCatalogCreateReqVO {

    @NotBlank(message = "DHR目录编码不能为空")
    private String catalogCode;

    @NotBlank(message = "DHR目录名称不能为空")
    private String catalogName;

    private Long parentCatalogId;

    private String remark;
}
