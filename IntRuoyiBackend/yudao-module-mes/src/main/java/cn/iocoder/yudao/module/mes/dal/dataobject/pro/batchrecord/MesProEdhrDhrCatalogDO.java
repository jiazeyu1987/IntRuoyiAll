package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@TableName("mes_pro_edhr_dhr_catalog")
@KeySequence("mes_pro_edhr_dhr_catalog_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MesProEdhrDhrCatalogDO extends BaseDO {

    @TableId
    private Long id;

    private String catalogCode;

    private String catalogName;

    private Long parentCatalogId;

    private String status;

    private String remark;
}
