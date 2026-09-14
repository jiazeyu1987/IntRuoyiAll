package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import lombok.Data;

@Data
public class DccProjectFileTemplateItemRespVO {

    private Long id;
    private Long projectCodeId;
    private Long fileTypeTaxonomyId;
    private Long stageTaxonomyId;
    private String stageName;
    private Long fileTypeNodeId;
    private String fileTypeName;
    private String taxonomyPath;
    private String fileName;
    private Integer sortOrder;
}
