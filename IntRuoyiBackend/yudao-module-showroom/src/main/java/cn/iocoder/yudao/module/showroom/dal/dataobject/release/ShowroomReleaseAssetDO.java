package cn.iocoder.yudao.module.showroom.dal.dataobject.release;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("showroom_release_asset")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomReleaseAssetDO extends BaseDO {

    @TableId
    private Long id;
    private String siteKey;
    private String stage;
    private String assetId;
    private String assetType;
    private String contentHash;
    private String mimeType;
    private Long bytes;
    private String storageKey;
    private LocalDateTime materializedAt;
    private String status;
    private byte[] binaryContent;
    private Long tenantId;
}
