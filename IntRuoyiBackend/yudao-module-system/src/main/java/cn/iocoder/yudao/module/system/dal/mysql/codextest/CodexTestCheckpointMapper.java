package cn.iocoder.yudao.module.system.dal.mysql.codextest;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCheckpointDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CodexTestCheckpointMapper extends BaseMapperX<CodexTestCheckpointDO> {

    default List<CodexTestCheckpointDO> selectListByCaseId(Long caseId) {
        return selectList(new LambdaQueryWrapperX<CodexTestCheckpointDO>()
                .eq(CodexTestCheckpointDO::getCaseId, caseId)
                .orderByAsc(CodexTestCheckpointDO::getSort));
    }

    default List<CodexTestCheckpointDO> selectListByCaseIds(Collection<Long> caseIds) {
        return selectList(new LambdaQueryWrapperX<CodexTestCheckpointDO>()
                .in(CodexTestCheckpointDO::getCaseId, caseIds)
                .orderByAsc(CodexTestCheckpointDO::getCaseId)
                .orderByAsc(CodexTestCheckpointDO::getSort));
    }

    default int deleteByCaseId(Long caseId) {
        return delete(new LambdaUpdateWrapper<CodexTestCheckpointDO>()
                .eq(CodexTestCheckpointDO::getCaseId, caseId));
    }

}
