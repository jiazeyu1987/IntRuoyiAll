package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrEvidencePackagePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrEvidencePackageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrEvidencePackageMapper extends BaseMapperX<MesProEdhrEvidencePackageDO> {

    default PageResult<MesProEdhrEvidencePackageDO> selectPage(MesProEdhrEvidencePackagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrEvidencePackageDO>()
                .eq(MesProEdhrEvidencePackageDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(MesProEdhrEvidencePackageDO::getPackageStatus, reqVO.getPackageStatus())
                .eqIfPresent(MesProEdhrEvidencePackageDO::getEvidenceStatus, reqVO.getEvidenceStatus())
                .orderByAsc(MesProEdhrEvidencePackageDO::getId));
    }

    default List<MesProEdhrEvidencePackageDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrEvidencePackageDO>()
                .eq(MesProEdhrEvidencePackageDO::getProjectId, projectId)
                .orderByAsc(MesProEdhrEvidencePackageDO::getId));
    }
}
