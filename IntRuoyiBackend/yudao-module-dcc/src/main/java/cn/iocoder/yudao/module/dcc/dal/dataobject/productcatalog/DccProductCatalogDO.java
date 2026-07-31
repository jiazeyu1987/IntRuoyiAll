package cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("dcc_product_catalog")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProductCatalogDO extends BaseDO {

    @TableId
    private Long id;
    private String dataSource;
    private Integer originalRowNo;
    private String categoryLevel1;
    private String categoryLevel2;
    private String productSequence;
    private String product;
    private String productCode;
    private String projectName;
    private String projectCode;
    private String registrationCertificateName;
    private String registrationCertificateNumber;
    private String certificateHolder;
    private String registrationPlace;
    private String effectiveDate;
    private String expiryDate;
    private String classification;
    private String registrationInfoLink;
    private String productStatus;
    private String remark;
}
