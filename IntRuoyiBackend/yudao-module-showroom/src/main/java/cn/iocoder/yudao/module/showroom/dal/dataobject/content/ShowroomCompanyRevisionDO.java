package cn.iocoder.yudao.module.showroom.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("showroom_company_revision")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomCompanyRevisionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long companyId;

    private Integer revisionNo;

    private String status;

    private String developmentHistory;

    private String developmentHistoryEn;

    private String parkIntroduction;

    private String parkIntroductionEn;

    private String incubationPlatform;

    private String incubationPlatformEn;

    private String subsidiaryOverview;

    private String subsidiaryOverviewEn;

    private String stockInfo;

    private String stockInfoEn;

    private String coverImage;

    private String coreManufacturingCapability;

    private String coreManufacturingCapabilityEn;

    private String honorsAwards;

    private String honorsAwardsEn;

    private String displayNameSnapshot;

    private String displayNameEnSnapshot;

    private String companyTypeSnapshot;

    private Long submittedBy;

    private Long approvedBy;

    private LocalDateTime publishedAt;

}
