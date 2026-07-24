package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MesProEdhrDhrCatalogRespVO {

    private Long id;

    private String catalogCode;

    private String catalogName;

    private Long parentCatalogId;

    private String status;

    private String remark;

    private LocalDateTime createTime;
}
