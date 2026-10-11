package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/** Eleven-column append-only binding; deliberately has no BaseDO or logical deletion. */
@Data
@Accessors(chain = true)
@TableName("mes_edhr_source_creation_binding")
public class MesEdhrSourceCreationBindingDO {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    private String ownerProtocol;
    private Long sourceRowId;
    private String sourceReceiptId;
    private String sourceVersion;
    private String sourceHash;
    private String requestHashVersion;
    private String requestHash;
    private Long creationEventId;
    private String creationEventHash;
}
