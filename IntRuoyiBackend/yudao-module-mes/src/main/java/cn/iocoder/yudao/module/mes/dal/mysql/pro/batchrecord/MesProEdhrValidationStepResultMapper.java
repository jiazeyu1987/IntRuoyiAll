package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationStepResultDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrValidationStepResultMapper extends BaseMapperX<MesProEdhrValidationStepResultDO> {

    default List<MesProEdhrValidationStepResultDO> selectListByRunId(Long runId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrValidationStepResultDO>()
                .eq(MesProEdhrValidationStepResultDO::getRunId, runId)
                .orderByAsc(MesProEdhrValidationStepResultDO::getId));
    }
}
