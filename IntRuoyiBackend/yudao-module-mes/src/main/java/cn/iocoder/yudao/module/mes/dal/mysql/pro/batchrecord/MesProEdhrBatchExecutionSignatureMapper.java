package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionSignatureDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrBatchExecutionSignatureMapper extends BaseMapperX<MesProEdhrBatchExecutionSignatureDO> {

    default List<MesProEdhrBatchExecutionSignatureDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionSignatureDO>()
                .eq(MesProEdhrBatchExecutionSignatureDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProEdhrBatchExecutionSignatureDO::getSignedAt)
                .orderByAsc(MesProEdhrBatchExecutionSignatureDO::getId));
    }
}
