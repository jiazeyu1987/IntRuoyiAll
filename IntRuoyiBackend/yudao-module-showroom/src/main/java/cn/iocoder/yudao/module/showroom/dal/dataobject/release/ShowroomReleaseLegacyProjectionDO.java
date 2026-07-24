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

@TableName("showroom_release_legacy_projection")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomReleaseLegacyProjectionDO extends BaseDO {

    @TableId
    private Long id;
    private String releaseId;
    private String siteKey;
    private String stage;
    private String projectionHash;
    private LocalDateTime publishedAt;
    private String rootDocumentId;
    private String status;
    private String payloadJson;
    private Long tenantId;
}
