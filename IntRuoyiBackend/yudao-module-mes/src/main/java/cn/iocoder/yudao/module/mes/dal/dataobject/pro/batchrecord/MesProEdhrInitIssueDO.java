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

@TableName("mes_pro_edhr_init_issue")
@KeySequence("mes_pro_edhr_init_issue_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrInitIssueDO extends BaseDO {

    @TableId
    private Long id;

    private Long initBatchId;

    private Long initManifestId;

    private String issueCode;

    private String issueLevel;

    private String issueStatus;

    private String packageType;

    private String sourceFileName;

    private Integer sourceRowNo;

    private String sourceFieldName;

    private String objectType;

    private String objectKey;

    private Long responsibleUserId;

    private String responsibleName;

    private String issueMessage;

    private String remediationSuggestion;

    private String impactScopeJson;
}
