package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccFileTypeTaxonomyRespVO {

    private Long id;
    private Long parentId;
    private Integer levelNo;
    private String code;
    private String name;
    private Boolean active;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
}
