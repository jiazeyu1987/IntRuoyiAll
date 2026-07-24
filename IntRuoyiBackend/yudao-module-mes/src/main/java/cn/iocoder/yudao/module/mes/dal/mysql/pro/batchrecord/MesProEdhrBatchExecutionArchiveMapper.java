package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrBatchExecutionArchiveMapper extends BaseMapperX<MesProEdhrBatchExecutionArchiveDO> {

    default List<MesProEdhrBatchExecutionArchiveDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionArchiveDO>()
                .eq(MesProEdhrBatchExecutionArchiveDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrBatchExecutionArchiveDO::getDeleted, false)
                .orderByDesc(MesProEdhrBatchExecutionArchiveDO::getArchiveVersion)
                .orderByDesc(MesProEdhrBatchExecutionArchiveDO::getId));
    }
}
