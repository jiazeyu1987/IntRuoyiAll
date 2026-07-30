package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProProcessPoolEventRevisionMapper extends BaseMapperX<MesProProcessPoolEventRevisionDO> {

    default MesProProcessPoolEventRevisionDO selectBySignatureId(Long revisionSignatureId) {
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolEventRevisionDO>()
                .eq(MesProProcessPoolEventRevisionDO::getRevisionSignatureId, revisionSignatureId));
    }
}
