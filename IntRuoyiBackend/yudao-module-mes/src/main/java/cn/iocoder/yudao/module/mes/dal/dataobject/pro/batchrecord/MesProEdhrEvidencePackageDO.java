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

@TableName("mes_pro_edhr_evidence_package")
@KeySequence("mes_pro_edhr_evidence_package_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrEvidencePackageDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long projectId;

    private String packageCode;

    private String packageName;

    private String packageType;

    private String packageStatus;

    private String evidenceStatus;

    private String ownerName;

    private String ownerDepartment;

    private String requiredEvidenceJson;

    private String availableEvidenceJson;

    private String missingEvidenceJson;

    private String signoffImpact;

    private String nextAction;

    private String remark;
}
