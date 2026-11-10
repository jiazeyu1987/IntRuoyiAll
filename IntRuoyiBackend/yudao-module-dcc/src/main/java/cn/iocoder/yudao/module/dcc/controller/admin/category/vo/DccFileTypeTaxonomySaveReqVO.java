package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccFileTypeTaxonomySaveReqVO {

    private Long id;
    private Long parentId;

    private String code;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    @NotNull(message = "分类启用状态不能为空")
    private Boolean active;

    @NotNull(message = "分类排序不能为空")
    private Integer sort;

    private String remark;

    // 一级二级仅承载路径；三级及以下必须随类型保存明确审批来源。
    private Boolean pathNodeOnly;
    private String approvalSourceMode;
    private Long matrixSourceCategoryId;
    @jakarta.validation.Valid
    private DccCategoryDirectoryBindingSaveReqVO directoryBinding;
    @jakarta.validation.Valid
    private DccCategoryApprovalMatrixSaveReqVO approvalMatrix;
}
