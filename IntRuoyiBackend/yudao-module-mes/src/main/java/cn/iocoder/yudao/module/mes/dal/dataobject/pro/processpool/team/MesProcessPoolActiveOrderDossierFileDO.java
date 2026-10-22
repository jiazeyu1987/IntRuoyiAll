package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

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
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 活跃订单资料文件归属记录。资料文件不属于 P2 正式批次。
 */
@TableName("mes_pro_process_pool_active_order_dossier_file")
@KeySequence("mes_pro_process_pool_active_order_dossier_file_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolActiveOrderDossierFileDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long activeOrderId;
    private Long applicationId;
    private String categoryKey;
    private Long fileId;
    private String fileUrl;
    private Long storageConfigId;
    private String storagePath;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operatedAt;
}
