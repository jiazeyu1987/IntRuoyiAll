package cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule;

import lombok.Data;

/** Transactional replan request receipt, retained for durable result replay. */
@Data
public class MesProReplanRequestDO {
    private String payloadHash;
    private String resultJson;
}
