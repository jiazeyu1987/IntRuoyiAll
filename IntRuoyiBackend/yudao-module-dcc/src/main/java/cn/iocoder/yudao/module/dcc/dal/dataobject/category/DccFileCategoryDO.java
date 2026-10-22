package cn.iocoder.yudao.module.dcc.dal.dataobject.category;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DCC file category.
 */
@TableName("dcc_file_category")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccFileCategoryDO extends BaseDO {

    @TableId
    private Long id;
    private String code;
    private String name;
    /** Formal category configuration; unknown historical values must be repaired explicitly. */
    private String approvalSourceMode;
    private Long parentId;
    private Boolean active;
    private Integer sort;
    private String source;
    private String remark;
    private String description;
    private String lifecycleStage;
    private Long fileTypeTaxonomyId;
    private Boolean distributionRequired;
    private Boolean trainingRequired;

    /** No runtime fallback: callers must obtain this from the formal category row. */
    public static String requireApprovalSourceMode(String value) {
        if (!"PUBLIC_MATRIX".equals(value) && !"APPLICATION_CUSTOM".equals(value)) {
            throw new IllegalArgumentException("CATEGORY_APPROVAL_SOURCE_INVALID：文件类别审批来源未配置或无效，请明确维护");
        }
        return value;
    }

}
