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

@TableName("mes_pro_edhr_dataset_definition")
@KeySequence("mes_pro_edhr_dataset_definition_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrDatasetDefinitionDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String datasetCode;

    private String datasetName;

    private String datasetVersion;

    private String status;

    private String sourceType;

    private String sourceObject;

    private String sourceOwner;

    private String fieldSchemaJson;

    private String joinKeyJson;

    private String sensitiveFieldJson;

    private String permissionPolicyJson;

    private String caliberVersion;

    private String dataSourceStatus;

    private String failureReason;

    private LocalDateTime publishedAt;

    private String remark;
}
