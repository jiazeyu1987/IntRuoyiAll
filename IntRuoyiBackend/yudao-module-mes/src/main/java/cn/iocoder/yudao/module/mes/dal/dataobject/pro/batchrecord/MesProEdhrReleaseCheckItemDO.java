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

@TableName("mes_pro_edhr_release_check_item")
@KeySequence("mes_pro_edhr_release_check_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrReleaseCheckItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long releaseTransactionId;

    private String checkCode;

    private String checkCategory;

    private String checkName;

    private String checkResult;

    private String itemStatus;

    private String severity;

    private String responsibilityModule;

    private String sourceObjectType;

    private String sourceObjectId;

    private String sourceObjectCode;

    private String sourceRecordUrl;

    private String failureReason;

    private String remediationSuggestion;

    private String impactScopeJson;

    private String evidenceHash;

    private LocalDateTime checkedAt;
}
