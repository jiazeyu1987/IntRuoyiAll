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
 * DCC directory access rule.
 */
@TableName("dcc_directory_access_rule")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccDirectoryAccessRuleDO extends BaseDO {

    @TableId
    private Long id;
    private Long directoryId;
    private String subjectType;
    private Long subjectId;
    private Boolean canQuery;
    private Boolean canPreview;
    private Boolean canDownload;
    private Boolean active;
    private String changeReason;

}
