package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("dcc_electronic_signature_policy")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccElectronicSignaturePolicyDO extends BaseDO {

    @TableId
    private Long id;
    private Integer passwordFailureWindowMinutes;
    private Integer passwordFailureThreshold;
    private Integer lockMinutes;
    private String evidencePayloadVersion;
    private String hashAlgorithm;
    private Integer status;

}
