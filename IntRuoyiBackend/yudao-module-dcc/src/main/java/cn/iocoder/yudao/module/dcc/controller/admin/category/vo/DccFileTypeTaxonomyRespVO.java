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
    /** Read-only category source: null for parents or a leaf with an explicit uploadBlocker. */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    private String approvalSourceMode;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    private String uploadBlocker;
    private Boolean active;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
}
