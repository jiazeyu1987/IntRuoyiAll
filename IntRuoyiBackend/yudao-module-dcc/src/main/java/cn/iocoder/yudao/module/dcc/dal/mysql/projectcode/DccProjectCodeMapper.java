package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccProjectCodeMapper extends BaseMapperX<DccProjectCodeDO> {

    default PageResult<DccProjectCodeDO> selectPage(DccProjectCodePageReqVO reqVO) {
        LambdaQueryWrapperX<DccProjectCodeDO> wrapper = new LambdaQueryWrapperX<DccProjectCodeDO>()
                .likeIfPresent(DccProjectCodeDO::getProjectName, reqVO.getProjectName())
                .likeIfPresent(DccProjectCodeDO::getProjectCode, reqVO.getProjectCode())
                .eqIfPresent(DccProjectCodeDO::getCategory, reqVO.getCategory())
                .eqIfPresent(DccProjectCodeDO::getPriority, reqVO.getPriority())
                .eqIfPresent(DccProjectCodeDO::getStatus, reqVO.getStatus())
                .orderByDesc(DccProjectCodeDO::getId);
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        if (keyword != null) {
            wrapper.and(item -> item.like(DccProjectCodeDO::getDocControlNo, keyword)
                    .or().like(DccProjectCodeDO::getProjectName, keyword)
                    .or().like(DccProjectCodeDO::getProjectCode, keyword)
                    .or().like(DccProjectCodeDO::getCategory, keyword)
                    .or().like(DccProjectCodeDO::getProjectLeader, keyword)
                    .or().like(DccProjectCodeDO::getProjectEngineer, keyword)
                    .or().like(DccProjectCodeDO::getStorageLocation, keyword));
        }
        return selectPage(reqVO, wrapper);
    }

    default DccProjectCodeDO selectByProjectNameAndProjectCode(String projectName, String projectCode) {
        return selectOne(new LambdaQueryWrapperX<DccProjectCodeDO>()
                .eq(DccProjectCodeDO::getProjectName, projectName)
                .eq(DccProjectCodeDO::getProjectCode, projectCode));
    }

    default List<DccProjectCodeDO> selectEnabledListByProjectName(String projectName) {
        return selectList(new LambdaQueryWrapperX<DccProjectCodeDO>()
                .eq(DccProjectCodeDO::getProjectName, projectName)
                .eq(DccProjectCodeDO::getStatus, DccProjectCodeStatusConstants.ENABLE)
                .orderByAsc(DccProjectCodeDO::getId));
    }

    default DccProjectCodeDO selectByProjectNameAndProjectCodeExcludingId(String projectName, String projectCode,
                                                                           Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<DccProjectCodeDO>()
                .eq(DccProjectCodeDO::getProjectName, projectName)
                .eq(DccProjectCodeDO::getProjectCode, projectCode)
                .neIfPresent(DccProjectCodeDO::getId, excludeId));
    }

    default List<DccProjectCodeDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<DccProjectCodeDO>()
                .eq(DccProjectCodeDO::getStatus, DccProjectCodeStatusConstants.ENABLE)
                .orderByAsc(DccProjectCodeDO::getId));
    }
}
