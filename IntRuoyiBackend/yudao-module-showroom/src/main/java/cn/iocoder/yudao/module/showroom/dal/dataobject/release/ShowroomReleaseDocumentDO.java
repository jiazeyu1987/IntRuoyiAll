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

@TableName("showroom_release_document")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomReleaseDocumentDO extends BaseDO {

    @TableId
    private Long id;
    private String releaseId;
    private String siteKey;
    private String stage;
    private String documentId;
    private String kind;
    private Long productId;
    private String contentHash;
    private Long bytes;
    private LocalDateTime materializedAt;
    private String payloadJson;
    private Long tenantId;
}
