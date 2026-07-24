package cn.iocoder.yudao.module.mes.dal.mysql.md.item;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee.MesKingdeeBomListPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesKingdeeBomListDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesKingdeeBomListMapper extends BaseMapperX<MesKingdeeBomListDO> {

    default PageResult<MesKingdeeBomListDO> selectPage(MesKingdeeBomListPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesKingdeeBomListDO>()
                .likeIfPresent(MesKingdeeBomListDO::getBomNumber, reqVO.getBomNumber())
                .likeIfPresent(MesKingdeeBomListDO::getParentMaterialCode, reqVO.getParentMaterialCode())
                .likeIfPresent(MesKingdeeBomListDO::getParentMaterialName, reqVO.getParentMaterialName())
                .likeIfPresent(MesKingdeeBomListDO::getChildMaterialCode, reqVO.getChildMaterialCode())
                .likeIfPresent(MesKingdeeBomListDO::getChildMaterialName, reqVO.getChildMaterialName())
                .orderByDesc(MesKingdeeBomListDO::getSourceModifyTime)
                .orderByDesc(MesKingdeeBomListDO::getId));
    }

    default MesKingdeeBomListDO selectBySourceLine(String sourceFid, String sourceLineKey) {
        return selectOne(new LambdaQueryWrapperX<MesKingdeeBomListDO>()
                .eq(MesKingdeeBomListDO::getSourceFid, sourceFid)
                .eq(MesKingdeeBomListDO::getSourceLineKey, sourceLineKey));
    }

}
