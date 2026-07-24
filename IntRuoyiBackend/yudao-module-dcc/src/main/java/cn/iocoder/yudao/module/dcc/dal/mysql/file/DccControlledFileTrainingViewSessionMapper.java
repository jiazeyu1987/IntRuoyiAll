package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingViewSessionDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccControlledFileTrainingViewSessionMapper extends BaseMapperX<DccControlledFileTrainingViewSessionDO> {

    default DccControlledFileTrainingViewSessionDO selectActiveByProgressIdAndClientSessionId(Long progressId,
                                                                                               String clientSessionId) {
        return selectOne(new LambdaQueryWrapper<DccControlledFileTrainingViewSessionDO>()
                .eq(DccControlledFileTrainingViewSessionDO::getTrainingProgressId, progressId)
                .eq(DccControlledFileTrainingViewSessionDO::getClientSessionId, clientSessionId)
                .isNull(DccControlledFileTrainingViewSessionDO::getEndedAt));
    }

    default List<DccControlledFileTrainingViewSessionDO> selectActiveListByProgressId(Long progressId) {
        return selectList(new LambdaQueryWrapper<DccControlledFileTrainingViewSessionDO>()
                .eq(DccControlledFileTrainingViewSessionDO::getTrainingProgressId, progressId)
                .isNull(DccControlledFileTrainingViewSessionDO::getEndedAt));
    }
}
