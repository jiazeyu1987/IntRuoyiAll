package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitManifestDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrInitManifestMapper extends BaseMapperX<MesProEdhrInitManifestDO> {

    default MesProEdhrInitManifestDO selectByBatchAndHash(Long initBatchId, String manifestHash) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrInitManifestDO>()
                .eq(MesProEdhrInitManifestDO::getInitBatchId, initBatchId)
                .eq(MesProEdhrInitManifestDO::getManifestHash, manifestHash));
    }

    default List<MesProEdhrInitManifestDO> selectListByBatchId(Long initBatchId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrInitManifestDO>()
                .eq(MesProEdhrInitManifestDO::getInitBatchId, initBatchId)
                .orderByAsc(MesProEdhrInitManifestDO::getId));
    }
}
