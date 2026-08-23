package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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

@TableName("mes_pro_edhr_batch_execution_trace_manifest")
@KeySequence("mes_pro_edhr_batch_execution_trace_manifest_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrBatchExecutionTraceManifestDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long batchExecutionId;
    private Integer manifestVersion;
    private String previousManifestHash;
    private String manifestJson;
    private String manifestHash;
    private String sealReason;
    private Long sealedBy;
    private LocalDateTime sealedAt;
}
