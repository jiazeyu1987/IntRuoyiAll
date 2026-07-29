package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProProcessPoolEventMapper extends BaseMapperX<MesProProcessPoolEventDO> {

    default MesProProcessPoolEventDO selectBySignatureId(Long signatureId) {
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolEventDO>()
                .eq(MesProProcessPoolEventDO::getSignatureId, signatureId));
    }
}
