package cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation;

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

@TableName("mes_qa_inspection_regulation_version")
@KeySequence("mes_qa_inspection_regulation_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesQaInspectionRegulationVersionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long regulationId;
    private String versionNo;
    private String lifecycleStatus;
    private LocalDateTime publishedAt;
    private LocalDateTime retiredAt;
    private String snapshotJson;
}
