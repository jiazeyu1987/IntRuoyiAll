package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReprintRequestDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrReprintRequestMapper extends BaseMapperX<MesProEdhrReprintRequestDO> {

    default MesProEdhrReprintRequestDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(MesProEdhrReprintRequestDO::getIdempotencyKey, idempotencyKey);
    }

    default Long countByOriginalPrintTaskId(Long originalPrintTaskId) {
        return selectCount(MesProEdhrReprintRequestDO::getOriginalPrintTaskId, originalPrintTaskId);
    }
}
