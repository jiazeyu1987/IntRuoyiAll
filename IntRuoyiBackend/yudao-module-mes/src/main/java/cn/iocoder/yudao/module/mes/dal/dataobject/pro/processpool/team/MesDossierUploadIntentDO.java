package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;
@TableName("mes_dossier_upload_intent")
@Data @EqualsAndHashCode(callSuper=true) @Builder @NoArgsConstructor @AllArgsConstructor
public class MesDossierUploadIntentDO extends TenantBaseDO {
    @TableId(type=IdType.AUTO) private Long id;
    private Long activeOrderId;
    private Long actorId;
    private Long configId;
    private String storagePath;
    private Long fileId;
    private String state;
    private LocalDateTime expiresAt;
    @TableField(updateStrategy=FieldStrategy.ALWAYS)
    private String lastError;
}
