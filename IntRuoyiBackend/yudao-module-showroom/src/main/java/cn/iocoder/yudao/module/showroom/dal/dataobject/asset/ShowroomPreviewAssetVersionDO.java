package cn.iocoder.yudao.module.showroom.dal.dataobject.asset;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
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

@TableName("showroom_preview_asset_version")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomPreviewAssetVersionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String targetType;

    private Long targetId;

    private Long sourceRevisionId;

    private Integer versionNo;

    private Long imageFileId;

    private String status;

    private Boolean generatedByAi;

    private LocalDateTime generatedAt;

    private LocalDateTime publishedAt;

}
