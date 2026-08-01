package cn.iocoder.yudao.module.mes.dal.mysql.pro.mesprocess;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess.MesProMesProcessCatalogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProMesProcessCatalogMapper extends BaseMapperX<MesProMesProcessCatalogDO> {

    default PageResult<MesProMesProcessCatalogDO> selectPage(MesProMesProcessPageReqVO reqVO) {
        LambdaQueryWrapperX<MesProMesProcessCatalogDO> wrapper = new LambdaQueryWrapperX<MesProMesProcessCatalogDO>()
                .eq(MesProMesProcessCatalogDO::getDeleted, false);
        String keyword = StrUtil.trim(reqVO.getKeyword());
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(query -> query
                    .like(MesProMesProcessCatalogDO::getProductName, keyword)
                    .or().like(MesProMesProcessCatalogDO::getMesProcessName, keyword)
                    .or().like(MesProMesProcessCatalogDO::getMesProcessCode, keyword)
                    .or().like(MesProMesProcessCatalogDO::getSourceMachineryCodes, keyword)
                    .or().like(MesProMesProcessCatalogDO::getSourceMachineryName, keyword)
                    .or().like(MesProMesProcessCatalogDO::getBatchRecordProcessName, keyword));
        }
        wrapper.orderByAsc(MesProMesProcessCatalogDO::getSortNo)
                .orderByAsc(MesProMesProcessCatalogDO::getSourceRowNo);
        return selectPage(reqVO, wrapper);
    }
}
