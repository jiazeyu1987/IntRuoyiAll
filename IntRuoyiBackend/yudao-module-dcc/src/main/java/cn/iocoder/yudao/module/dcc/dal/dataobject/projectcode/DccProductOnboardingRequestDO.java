package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_product_onboarding_request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProductOnboardingRequestDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long productMasterId;
    private String productCode;
    private String dccProductCode;
    private String productNameCn;
    private String productNameEn;
    private String modelSpecification;
    private String productCategory;
    private String docControlNo;
    private String projectName;
    private String projectCode;
    private String category;
    private String commissionedProduction;
    private String projectLeader;
    private String projectEngineer;
    private String storageLocation;
    private String priority;
    private String status;
    private Long applicantUserId;
    private Long approverUserId;
    private LocalDateTime approvedTime;
    private Long generatedProjectCodeId;
    private String rejectReason;
}
