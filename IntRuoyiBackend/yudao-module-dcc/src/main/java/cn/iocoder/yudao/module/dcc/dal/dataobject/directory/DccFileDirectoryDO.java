package cn.iocoder.yudao.module.dcc.dal.dataobject.directory;

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
 * DCC file directory.
 */
@TableName("dcc_file_directory")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccFileDirectoryDO extends BaseDO {

    @TableId
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Boolean active;
    private Integer sort;
    private String remark;
    private Boolean accessRuleManuallyBound;

}
