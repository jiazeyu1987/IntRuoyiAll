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

@TableName("mes_pro_edhr_process_form_permission_rule")
@KeySequence("mes_pro_edhr_process_form_permission_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrProcessFormPermissionRuleDO extends BaseDO {

    @TableId
    private Long id;

    private Long routeProcessId;

    private String batchRecordReportId;

    private Long batchRecordDefinitionId;

    private Long batchRecordVersionId;

    private String ruleType;

    private String signatureCellKey;

    private String signatureRole;

    private String candidateSourceType;

    private String candidateSourceIds;

    private String completionPolicy;

    private Integer dueMinutes;

    private Boolean enabled;

    private String remark;
}
