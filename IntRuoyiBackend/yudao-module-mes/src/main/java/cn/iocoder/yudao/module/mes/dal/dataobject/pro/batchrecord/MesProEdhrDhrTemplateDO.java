package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_pro_edhr_dhr_template")
@KeySequence("mes_pro_edhr_dhr_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MesProEdhrDhrTemplateDO extends BaseDO {

    @TableId
    private Long id;

    private Long catalogId;

    private String templateCode;

    private String templateName;

    private String currentVersion;

    private String status;

    private String reviewStatus;

    private String signoffStatus;

    private Integer bindingCount;

    private Integer integrityIssueCount;

    private String integrityIssueJson;

    private String signoffEvidenceHash;

    private LocalDateTime effectiveAt;

    private LocalDateTime retiredAt;

    private LocalDateTime voidedAt;

    private String remark;
}
