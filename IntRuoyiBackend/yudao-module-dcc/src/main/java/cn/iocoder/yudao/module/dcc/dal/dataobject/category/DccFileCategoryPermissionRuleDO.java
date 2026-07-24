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

@TableName("dcc_file_category_permission_rule")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccFileCategoryPermissionRuleDO extends BaseDO {

    @TableId
    private Long id;
    private Long categoryId;
    private String actionType;
    private String subjectType;
    private Long subjectId;
    private String scopeType;
    private Boolean active;
    private String remark;

}
