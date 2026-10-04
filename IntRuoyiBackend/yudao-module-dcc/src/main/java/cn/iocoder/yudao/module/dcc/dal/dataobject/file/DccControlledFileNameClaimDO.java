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
import java.time.LocalDateTime;

/**
 * A tenant-wide reservation for a controlled-file logical name.
 *
 * <p>Original source name and formal number stay reserved through the explicit
 * obsolete retention deadline. Release also requires no in-use version in the chain.</p>
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

    private String sourceOriginalFileName;
    private Long dccProjectCodeId;
    private Long fileTypeTaxonomyLeafId;
    private String normalizedFileNumber;
    private LocalDateTime obsoleteTime;
    private LocalDateTime retainUntil;

    private Long masterId;
}
