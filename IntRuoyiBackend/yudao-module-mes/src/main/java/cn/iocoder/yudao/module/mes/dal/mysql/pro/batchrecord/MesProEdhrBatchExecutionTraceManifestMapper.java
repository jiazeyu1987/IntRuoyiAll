package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceManifestDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrBatchExecutionTraceManifestMapper extends BaseMapperX<MesProEdhrBatchExecutionTraceManifestDO> {

    default List<MesProEdhrBatchExecutionTraceManifestDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTraceManifestDO>()
                .eq(MesProEdhrBatchExecutionTraceManifestDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProEdhrBatchExecutionTraceManifestDO::getManifestVersion));
    }

    default MesProEdhrBatchExecutionTraceManifestDO selectLatestByBatchExecutionId(Long batchExecutionId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTraceManifestDO>()
                .eq(MesProEdhrBatchExecutionTraceManifestDO::getBatchExecutionId, batchExecutionId)
                .orderByDesc(MesProEdhrBatchExecutionTraceManifestDO::getManifestVersion)
                .last("LIMIT 1"));
    }
}
