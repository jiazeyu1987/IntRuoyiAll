package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("dcc_controlled_file_source_ownership")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileSourceOwnershipDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Long controlledFileId;
    private Long sourceFileId;
    private Long originSourceFileId;
    private String sourceSha256;
    private String ownershipType;
    private Long claimedBy;
    private LocalDateTime claimedTime;

}
