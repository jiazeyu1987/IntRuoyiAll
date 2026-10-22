package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("mes_pro_edhr_deviation_create_request")
public class MesProEdhrDeviationCreateRequestDO {

    private Long tenantId;
    private String idempotencyKey;
    private String payloadHash;
    private Long deviationId;
}
