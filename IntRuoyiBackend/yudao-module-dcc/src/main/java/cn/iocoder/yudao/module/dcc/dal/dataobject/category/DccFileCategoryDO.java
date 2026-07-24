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

}
