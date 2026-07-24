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
 * DCC category view matrix rule.
 */
@TableName("dcc_category_view_matrix_rule")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccCategoryViewMatrixRuleDO extends BaseDO {

    @TableId
    private Long id;
    private Long categoryId;
    private String excelFileName;
    private Integer excelRowNo;
    private String excelColumnLetter;
    private String subjectLabel;
    private String subjectTopHeader;
    private String subjectSubHeader;
    private String marker;
    private String scopeType;
    private String subjectType;
    private Long subjectId;
    private Boolean active;
    private String remark;

}
