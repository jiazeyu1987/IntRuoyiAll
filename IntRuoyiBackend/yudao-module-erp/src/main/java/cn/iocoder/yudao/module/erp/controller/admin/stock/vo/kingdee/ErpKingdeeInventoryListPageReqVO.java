package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 即时库存分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpKingdeeInventoryListPageReqVO extends PageParam {

    private String materialNumber;
    private String materialName;
    private String warehouseName;
    private String lotNumber;

}
