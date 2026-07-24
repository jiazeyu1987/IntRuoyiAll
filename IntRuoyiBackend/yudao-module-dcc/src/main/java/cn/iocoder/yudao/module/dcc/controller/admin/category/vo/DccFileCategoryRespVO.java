package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccFileCategoryRespVO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Long directoryId;
    private Boolean active;
    private Integer sort;
    private String source;
    private String remark;
    private String description;
    private String lifecycleStage;
    private Long fileTypeTaxonomyId;
    private Boolean distributionRequired;
    private Boolean trainingRequired;
    private List<Long> signoffPositionIds;
    private List<Long> approvalPositionIds;
    private LocalDateTime createTime;
}
