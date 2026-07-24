package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("mes_pro_batch_record_version_migration_item")
@KeySequence("mes_pro_batch_record_version_migration_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordVersionMigrationItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long definitionId;

    private Long versionId;

    private Long sourceVersionId;

    private String itemType;

    private String diffGroup;

    private String diffType;

    private String sourceLogicalKey;

    private String targetLogicalKey;

    private BigDecimal matchConfidence;

    private String matchEvidenceJson;

    private String riskLevel;

    private String ruleType;

    private String businessOwnerType;

    private Boolean confirmed;

    private Long confirmedBy;

    private LocalDateTime confirmedAt;

    private String confirmComment;

    private String confirmIdempotencyKey;

    private String message;
}
