package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

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
 * DCC controlled file stamp job state.
 */
@TableName("dcc_controlled_file_stamp")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileStampDO extends BaseDO {

    @TableId
    private Long id;
    private Long controlledFileId;
    private String stampType;
    private String templateId;
    private String rendererType;
    private String stampText;
    private String outputFormat;
    private String pagePositionsJson;
    private Long sourceFileId;
    private Long outputFileId;
    private String status;
    private String errorMessage;

}
