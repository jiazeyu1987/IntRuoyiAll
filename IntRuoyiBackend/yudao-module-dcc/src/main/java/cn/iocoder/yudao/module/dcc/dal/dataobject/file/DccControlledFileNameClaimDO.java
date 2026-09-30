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
 * A tenant-wide reservation for a controlled-file logical name.
 *
 * <p>The claim is released only after the owning logical file is approved
 * obsolete, so rejected and in-progress submissions continue to reserve the
 * name for their existing master.</p>
 */
@TableName("dcc_controlled_file_name_claim")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileNameClaimDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String normalizedName;

    private Long masterId;
}
