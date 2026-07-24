package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportDefinitionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReportDefinitionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrReportDefinitionMapper extends BaseMapperX<MesProEdhrReportDefinitionDO> {

    default PageResult<MesProEdhrReportDefinitionDO> selectPage(MesProEdhrReportDefinitionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrReportDefinitionDO>()
                .likeIfPresent(MesProEdhrReportDefinitionDO::getReportCode, reqVO.getReportCode())
                .likeIfPresent(MesProEdhrReportDefinitionDO::getReportName, reqVO.getReportName())
                .eqIfPresent(MesProEdhrReportDefinitionDO::getReportType, reqVO.getReportType())
                .eqIfPresent(MesProEdhrReportDefinitionDO::getDatasetCode, reqVO.getDatasetCode())
                .eqIfPresent(MesProEdhrReportDefinitionDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesProEdhrReportDefinitionDO::getPublishedAt)
                .orderByDesc(MesProEdhrReportDefinitionDO::getId));
    }

    default MesProEdhrReportDefinitionDO selectPublishedByReportCode(String reportCode) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReportDefinitionDO>()
                .eq(MesProEdhrReportDefinitionDO::getReportCode, reportCode)
                .eq(MesProEdhrReportDefinitionDO::getStatus, "PUBLISHED")
                .orderByDesc(MesProEdhrReportDefinitionDO::getPublishedAt)
                .last("LIMIT 1"));
    }
}
