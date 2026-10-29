package cn.iocoder.yudao.module.mes.service.qa.regulation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcItemEquipmentConfigService;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
class MesSimplePqcPublishServiceTest {
 @Test void invalidRequestDoesNotPublish() {
  var r=mock(MesQaInspectionRegulationService.class); var e=mock(MesPqcItemEquipmentConfigService.class);
  var s=new MesSimplePqcPublishService(r,e,mock(MesDvMachineryMapper.class));
  assertThrows(RuntimeException.class,()->s.publish(new MesQaInspectionRegulationSaveReqVO()));
  verifyNoInteractions(r,e);
 }
 @Test void isolatedCodesAndEquipmentArePublishedTogether() {
  var r=mock(MesQaInspectionRegulationService.class); var e=mock(MesPqcItemEquipmentConfigService.class);
  var s=new MesSimplePqcPublishService(r,e,mock(MesDvMachineryMapper.class));
  var i=new MesQaInspectionRegulationSaveReqVO.InspectionItem();i.setItemName("visual");i.setItemCode("OLD");i.setEquipmentOptions(List.of());
  var p=new MesQaInspectionRegulationSaveReqVO.InspectionProcess();p.setItems(List.of(i));
  var q=new MesQaInspectionRegulationSaveReqVO();q.setProcesses(List.of(p));q.setDccProjectCodeId(129L);
  s.publish(q);assertNotEquals("OLD",i.getItemCode());verify(r).publish(q);verify(e).replaceItemConfigs(any());
 }
}
