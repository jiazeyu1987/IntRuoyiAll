package cn.iocoder.yudao.module.system.dal.mysql.codextest;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCasePageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCaseDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CodexTestCaseMapper extends BaseMapperX<CodexTestCaseDO> {

    default PageResult<CodexTestCaseDO> selectPage(CodexTestCasePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CodexTestCaseDO>()
                .likeIfPresent(CodexTestCaseDO::getName, reqVO.getName())
                .eqIfPresent(CodexTestCaseDO::getProject, reqVO.getProject())
                .eqIfPresent(CodexTestCaseDO::getNodeChainName, reqVO.getNodeChainName())
                .eqIfPresent(CodexTestCaseDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CodexTestCaseDO::getDefaultExecutionMode, reqVO.getExecutionMode())
                .orderByAsc(reqVO.getNodeChainName() != null, CodexTestCaseDO::getNodeChainSort)
                .orderByAsc(CodexTestCaseDO::getSort)
                .orderByDesc(BaseDO::getCreateTime));
    }

    default List<CodexTestCaseDO> selectListByIds(Collection<Long> ids) {
        return selectList(CodexTestCaseDO::getId, ids);
    }

    default Long selectCountByNodeChainNameAndSort(String nodeChainName, Integer nodeChainSort, Long excludeId) {
        return selectCount(new LambdaQueryWrapperX<CodexTestCaseDO>()
                .eq(CodexTestCaseDO::getNodeChainName, nodeChainName)
                .eq(CodexTestCaseDO::getNodeChainSort, nodeChainSort)
                .neIfPresent(CodexTestCaseDO::getId, excludeId));
    }

    default List<CodexTestCaseDO> selectListByNodeChainName(String nodeChainName) {
        return selectList(new LambdaQueryWrapperX<CodexTestCaseDO>()
                .eq(CodexTestCaseDO::getNodeChainName, nodeChainName)
                .orderByAsc(CodexTestCaseDO::getNodeChainSort));
    }

    default List<CodexTestCaseDO> selectNodeChainCases() {
        return selectList(new LambdaQueryWrapperX<CodexTestCaseDO>()
                .isNotNull(CodexTestCaseDO::getNodeChainName)
                .ne(CodexTestCaseDO::getNodeChainName, "")
                .orderByAsc(CodexTestCaseDO::getProject)
                .orderByAsc(CodexTestCaseDO::getNodeChainName)
                .orderByAsc(CodexTestCaseDO::getNodeChainSort));
    }

    @Delete("DELETE FROM system_codex_test_case WHERE id = #{id}")
    int deletePhysicalById(Long id);

}
