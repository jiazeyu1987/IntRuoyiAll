package cn.iocoder.yudao.module.system.dal.mysql.codextest;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCheckpointDO;
import org.apache.ibatis.annotations.Delete;
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

    @Delete("DELETE FROM system_codex_test_checkpoint WHERE case_id = #{caseId}")
    int deleteByCaseId(Long caseId);

}
