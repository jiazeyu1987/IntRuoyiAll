package cn.iocoder.yudao.module.srm.dal.dataobject.naslocator;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("srm_nas_locator_entry")
@KeySequence("srm_nas_locator_entry_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmNasLocatorEntryDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long refreshTaskId;

    private String entryType;

    private String name;

    private String path;

    private String pathHash;

    private String parentPath;

    private Long size;

    private Long modifiedAt;
}
