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

@TableName("showroom_product_revision")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomProductRevisionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long productId;

    private Integer revisionNo;

    private String status;

    private String nameCn;

    private String nameEn;

    private Long ownerCompanyId;

    private String productOwnerType;

    private String lifecycleStage;

    private String targetMarket;

    private String targetMarketEn;

    private String pipelineLayout;

    private String pipelineLayoutEn;

    private String registrationCertificate;

    private String registrationCertificateEn;

    private String indicationContent;

    private String indicationContentEn;

    private String coreSellingPoints;

    private String coreSellingPointsEn;

    private String modelSpecification;

    private String modelSpecificationEn;

    private String coverImage;

    private String clinicalEffect;

    private String clinicalEffectEn;

    private String fimStatus;

    private String fimStatusEn;

    private Long submittedBy;

    private Long approvedBy;

    private LocalDateTime publishedAt;

}
