package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool;

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

@TableName("mes_pro_process_pool_event_revision_diff")
@KeySequence("mes_pro_process_pool_event_revision_diff_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProProcessPoolEventRevisionDiffDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long revisionId;
    private Long eventId;
    private String fieldCode;
    private String fieldName;
    private String beforeValue;
    private String afterValue;
    private Boolean affectsQuantityFragment;
    private Long sourceQuantityFragmentId;
    private String originalFieldCode;
    private String originalFieldName;
}
