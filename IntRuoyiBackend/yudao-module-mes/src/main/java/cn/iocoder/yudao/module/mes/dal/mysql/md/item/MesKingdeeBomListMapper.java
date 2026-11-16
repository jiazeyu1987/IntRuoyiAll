package cn.iocoder.yudao.module.mes.dal.mysql.md.item;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee.MesKingdeeBomListPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesKingdeeBomListDO;
import org.apache.ibatis.annotations.Mapper;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

@Mapper
public interface MesKingdeeBomListMapper extends BaseMapperX<MesKingdeeBomListDO> {

    default Long countBySourceFid(String fid) {
        return selectCount(MesKingdeeBomListDO::getSourceFid, fid);
    }

    @org.apache.ibatis.annotations.Delete("DELETE FROM mes_kingdee_bom_list WHERE source_fid = #{fid}")
    int deleteBySourceFid(@org.apache.ibatis.annotations.Param("fid") String fid);

    default PageResult<MesKingdeeBomListDO> selectPage(MesKingdeeBomListPageReqVO reqVO) {
        return selectPage(reqVO, apply(reqVO, new LambdaQueryWrapperX<MesKingdeeBomListDO>()
                .eqIfPresent(MesKingdeeBomListDO::getParentMaterialCode, reqVO.getSyncCode())
                .likeIfPresent(MesKingdeeBomListDO::getBomNumber, reqVO.getBomNumber())
                .likeIfPresent(MesKingdeeBomListDO::getParentMaterialCode, reqVO.getParentMaterialCode())
                .likeIfPresent(MesKingdeeBomListDO::getParentMaterialName, reqVO.getParentMaterialName())
                .likeIfPresent(MesKingdeeBomListDO::getChildMaterialCode, reqVO.getChildMaterialCode())
                .likeIfPresent(MesKingdeeBomListDO::getChildMaterialName, reqVO.getChildMaterialName())
                .orderByDesc(MesKingdeeBomListDO::getSourceModifyTime)
                .orderByDesc(MesKingdeeBomListDO::getId), listFilterFields()));
    }

    default MesKingdeeBomListDO selectBySourceLine(String sourceFid, String sourceLineKey) {
        return selectOne(new LambdaQueryWrapperX<MesKingdeeBomListDO>()
                .eq(MesKingdeeBomListDO::getSourceFid, sourceFid)
                .eq(MesKingdeeBomListDO::getSourceLineKey, sourceLineKey));
    }


    static java.util.Map<String, cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field> listFilterFields() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("id", number("mes_kingdee_bom_list.id")),
                java.util.Map.entry("sourceFormId", text("mes_kingdee_bom_list.source_form_id")),
                java.util.Map.entry("sourceFid", text("mes_kingdee_bom_list.source_fid")),
                java.util.Map.entry("sourceLineKey", text("mes_kingdee_bom_list.source_line_key")),
                java.util.Map.entry("bomNumber", text("mes_kingdee_bom_list.bom_number")),
                java.util.Map.entry("bomType", text("mes_kingdee_bom_list.bom_type")),
                java.util.Map.entry("documentStatus", text("mes_kingdee_bom_list.document_status")),
                java.util.Map.entry("parentMaterialCode", text("mes_kingdee_bom_list.parent_material_code")),
                java.util.Map.entry("parentMaterialName", text("mes_kingdee_bom_list.parent_material_name")),
                java.util.Map.entry("parentMaterialSpecification", text("mes_kingdee_bom_list.parent_material_specification")),
                java.util.Map.entry("parentQuantity", number("mes_kingdee_bom_list.parent_quantity")),
                java.util.Map.entry("lineNo", number("mes_kingdee_bom_list.line_no")),
                java.util.Map.entry("childMaterialCode", text("mes_kingdee_bom_list.child_material_code")),
                java.util.Map.entry("childMaterialName", text("mes_kingdee_bom_list.child_material_name")),
                java.util.Map.entry("childMaterialSpecification", text("mes_kingdee_bom_list.child_material_specification")),
                java.util.Map.entry("childUnitName", text("mes_kingdee_bom_list.child_unit_name")),
                java.util.Map.entry("numerator", number("mes_kingdee_bom_list.numerator")),
                java.util.Map.entry("denominator", number("mes_kingdee_bom_list.denominator")),
                java.util.Map.entry("sourceModifyTime", dateTime("mes_kingdee_bom_list.source_modify_time")),
                java.util.Map.entry("lastSyncTime", dateTime("mes_kingdee_bom_list.last_sync_time")),
                java.util.Map.entry("createTime", dateTime("mes_kingdee_bom_list.create_time")));
    }
}
