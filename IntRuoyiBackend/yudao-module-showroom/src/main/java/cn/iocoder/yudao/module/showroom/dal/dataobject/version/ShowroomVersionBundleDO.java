package cn.iocoder.yudao.module.showroom.dal.dataobject.version;

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

@TableName("showroom_version_bundle")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomVersionBundleDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String targetType;

    private Long targetId;

    private Long revisionId;

    private Integer revisionNo;

    private Long releasePreviewAssetVersionId;

    private Long narrationZhVersionId;

    private Long narrationEnVersionId;

    private Long copiedFromRevisionId;

    private Long publishedBy;

    private LocalDateTime publishedAt;
}
