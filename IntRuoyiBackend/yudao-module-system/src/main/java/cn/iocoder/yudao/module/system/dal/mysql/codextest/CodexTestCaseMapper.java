package cn.iocoder.yudao.module.system.dal.mysql.codextest;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCasePageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCaseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CodexTestCaseMapper extends BaseMapperX<CodexTestCaseDO> {

    default PageResult<CodexTestCaseDO> selectPage(CodexTestCasePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CodexTestCaseDO>()
                .likeIfPresent(CodexTestCaseDO::getName, reqVO.getName())
                .eqIfPresent(CodexTestCaseDO::getProject, reqVO.getProject())
                .eqIfPresent(CodexTestCaseDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CodexTestCaseDO::getDefaultExecutionMode, reqVO.getExecutionMode())
                .orderByAsc(CodexTestCaseDO::getSort)
                .orderByDesc(BaseDO::getCreateTime));
    }

    default List<CodexTestCaseDO> selectListByIds(Collection<Long> ids) {
        return selectList(CodexTestCaseDO::getId, ids);
    }

}
