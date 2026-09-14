package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentBatchConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo.MesPqcItemEquipmentItemRespVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MesPqcItemEquipmentConfigService {

    List<MesPqcItemEquipmentItemRespVO> listConfigurableItems(Long dccProjectCodeId);

    MesPqcItemEquipmentConfigRespVO getItemConfig(String itemCode);

    MesPqcItemEquipmentConfigRespVO getItemConfig(Long dccProjectCodeId, Collection<String> itemCodes);

    MesPqcItemEquipmentConfigRespVO replaceItemConfig(MesPqcItemEquipmentConfigSaveReqVO reqVO);

    MesPqcItemEquipmentConfigRespVO replaceItemConfigs(MesPqcItemEquipmentBatchConfigSaveReqVO reqVO);

    Map<String, List<MesPqcItemEquipmentOption>> listEnabledEquipmentOptionsByItemCodes(Collection<String> itemCodes);

    Map<String, List<MesPqcItemEquipmentOption>> listEnabledEquipmentOptionsByProjectAndItemCodes(
            Long dccProjectCodeId, Collection<String> itemCodes);

    Map<String, List<MesPqcItemEquipmentOption>> listEnabledEquipmentOptionsByProjectVersionAndItemCodes(
            Long dccProjectCodeId, Long regulationVersionId, Collection<String> itemCodes);
}
