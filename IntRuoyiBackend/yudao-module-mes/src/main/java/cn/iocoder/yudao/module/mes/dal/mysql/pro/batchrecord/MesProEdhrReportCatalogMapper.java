package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportCatalogPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReportCatalogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrReportCatalogMapper extends BaseMapperX<MesProEdhrReportCatalogDO> {

    default PageResult<MesProEdhrReportCatalogDO> selectPage(MesProEdhrReportCatalogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrReportCatalogDO>()
                .likeIfPresent(MesProEdhrReportCatalogDO::getReportCode, reqVO.getReportCode())
                .likeIfPresent(MesProEdhrReportCatalogDO::getReportName, reqVO.getReportName())
                .eqIfPresent(MesProEdhrReportCatalogDO::getReportCategory, reqVO.getReportCategory())
                .eqIfPresent(MesProEdhrReportCatalogDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProEdhrReportCatalogDO::getAcceptanceStatus, reqVO.getAcceptanceStatus())
                .orderByAsc(MesProEdhrReportCatalogDO::getSort)
                .orderByAsc(MesProEdhrReportCatalogDO::getId));
    }
}
