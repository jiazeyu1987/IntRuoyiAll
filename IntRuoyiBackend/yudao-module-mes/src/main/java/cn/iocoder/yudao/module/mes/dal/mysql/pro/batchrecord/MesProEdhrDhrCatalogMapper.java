package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDhrCatalogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrDhrCatalogMapper extends BaseMapperX<MesProEdhrDhrCatalogDO> {

    default PageResult<MesProEdhrDhrCatalogDO> selectPage(MesProEdhrDhrCatalogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrDhrCatalogDO>()
                .likeIfPresent(MesProEdhrDhrCatalogDO::getCatalogCode, reqVO.getCatalogCode())
                .likeIfPresent(MesProEdhrDhrCatalogDO::getCatalogName, reqVO.getCatalogName())
                .eqIfPresent(MesProEdhrDhrCatalogDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesProEdhrDhrCatalogDO::getId));
    }

    default MesProEdhrDhrCatalogDO selectByCatalogCode(String catalogCode) {
        return selectOne(MesProEdhrDhrCatalogDO::getCatalogCode, catalogCode);
    }
}
