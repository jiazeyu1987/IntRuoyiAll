package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentItemRespVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MesPqcItemEquipmentConfigService {

    List<MesPqcItemEquipmentItemRespVO> listConfigurableItems();

    MesPqcItemEquipmentConfigRespVO getItemConfig(String itemCode);

    MesPqcItemEquipmentConfigRespVO replaceItemConfig(MesPqcItemEquipmentConfigSaveReqVO reqVO);

    Map<String, List<MesPqcItemEquipmentOption>> listEnabledEquipmentOptionsByItemCodes(Collection<String> itemCodes);
}
