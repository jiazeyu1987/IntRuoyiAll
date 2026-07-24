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

@TableName("mes_pro_edhr_delivery_project")
@KeySequence("mes_pro_edhr_delivery_project_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrDeliveryProjectDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String projectCode;

    private String projectName;

    private String customerName;

    private String siteName;

    private String systemScope;

    private String validationScope;

    private String releaseTag;

    private String schemaVersion;

    private String targetEnvironment;

    private String projectStatus;

    private Boolean signoffAllowed;

    private String ownerName;

    private String ownerDepartment;

    private String blockedReason;

    private String gateSummaryJson;

    private String remark;
}
