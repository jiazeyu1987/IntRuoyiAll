package cn.iocoder.yudao.module.mes.service.qa.regulation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcItemEquipmentConfigService;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
class MesSimplePqcPublishServiceTest {
 private final MesQaInspectionRegulationService regulations = mock(MesQaInspectionRegulationService.class);
 private final MesPqcItemEquipmentConfigService equipment = mock(MesPqcItemEquipmentConfigService.class);
 private final MesQaInspectionRegulationMapper mapper = mock(MesQaInspectionRegulationMapper.class);
 private final MesDvMachineryMapper machinery = mock(MesDvMachineryMapper.class);
 private final MesSimplePqcPublishService service = new MesSimplePqcPublishService(regulations,equipment,machinery,mapper);
 @Test void invalidRequestDoesNotPublish() {
  var r=mock(MesQaInspectionRegulationService.class); var e=mock(MesPqcItemEquipmentConfigService.class);
  var s=new MesSimplePqcPublishService(r,e,mock(MesDvMachineryMapper.class),mapper);
  assertThrows(RuntimeException.class,()->s.publish(new MesQaInspectionRegulationSaveReqVO()));
  verifyNoInteractions(r,e);
 }
 @Test void isolatedCodesAndEquipmentArePublishedTogether() {
  var r=mock(MesQaInspectionRegulationService.class); var e=mock(MesPqcItemEquipmentConfigService.class);
  var s=new MesSimplePqcPublishService(r,e,mock(MesDvMachineryMapper.class),mapper);
  var i=new MesQaInspectionRegulationSaveReqVO.InspectionItem();i.setItemName("visual");i.setItemCode("OLD");i.setInspectionTool("目测");i.setEquipmentOptions(List.of());
  var p=new MesQaInspectionRegulationSaveReqVO.InspectionProcess();p.setItems(List.of(i));
  var q=new MesQaInspectionRegulationSaveReqVO();q.setProcesses(List.of(p));q.setDccProjectCodeId(129L);
  s.publish(q);assertNotEquals("OLD",i.getItemCode());verify(r).publish(q);verify(e).replaceItemConfigs(any());
 }
 @Test void nonSensoryToolRequiresExplicitReason() {
  var error=assertThrows(RuntimeException.class,()->service.publish(request("  ")));
  assertTrue(error.getMessage().contains("请填写器具未绑定原因"));
  verify(regulations,never()).publish(any()); verify(equipment,never()).replaceItemConfigs(any());
 }
 @Test void manuallyDisabledEquipmentKeepsOriginalToolWithoutPendingBinding() {
  var q=request(null);var item=q.getProcesses().get(0).getItems().get(0);
  item.setEquipmentRequired(false);
  service.publish(q);
  assertEquals("检测专用泵筒",item.getInspectionTool());
  assertNull(item.getEquipmentUnboundReason());
  verify(regulations).publish(q);
  verify(equipment).replaceItemConfigs(argThat(c->c.getEquipmentGroups().isEmpty()));
  verifyNoInteractions(machinery,mapper);
 }
 @Test void disabledEquipmentCannotRetainPendingBindingReason() {
  var q=request("待绑定");q.getProcesses().get(0).getItems().get(0).setEquipmentRequired(false);
  assertThrows(RuntimeException.class,()->service.publish(q));
  verifyNoInteractions(regulations,equipment,machinery);
 }
 @Test void explicitPendingBindingPreservesToolAndReasonWithoutInventingEquipment() {
  var q=request("真实器具尚无台账，待QA补齐"); service.publish(q);
  var i=q.getProcesses().get(0).getItems().get(0);
  assertEquals("检测专用泵筒",i.getInspectionTool());
  assertEquals("真实器具尚无台账，待QA补齐",i.getEquipmentUnboundReason());
  verify(regulations).publish(q);
  verify(equipment).replaceItemConfigs(argThat(c->c.getEquipmentGroups().isEmpty()));
  verifyNoInteractions(machinery);
 }
 @Test void boundEquipmentAndPendingReasonConflict() {
  var q=request("尚无台账");var option=new MesQaInspectionRegulationSaveReqVO.EquipmentOption();
  option.setEquipmentId(1L);option.setEquipmentNumber("REAL-001");
  q.getProcesses().get(0).getItems().get(0).setEquipmentOptions(List.of(option));
  assertTrue(assertThrows(RuntimeException.class,()->service.publish(q)).getMessage().contains("清空"));
  verify(regulations,never()).publish(any());verifyNoInteractions(machinery);
 }
 @Test void verifiedLegacyUnboundItemMayBeReused() {
  legacy(null,false);service.publish(request(null));verify(regulations).publish(any());
 }
 @Test void explicitPendingReasonCannotBeErasedThroughLegacyReuse() {
  legacy("尚无真实台账",false);assertThrows(RuntimeException.class,()->service.publish(request(null)));
  verify(regulations,never()).publish(any());
 }
 @Test void ambiguousLegacyItemCannotAuthorizeUnboundPublish() {
  legacy(null,true);assertThrows(RuntimeException.class,()->service.publish(request(null)));
  verify(regulations,never()).publish(any());
 }
 @Test void changingOriginalToolCannotReuseLegacyEmptyBinding() {
  legacy(null,false);var q=request(null);q.getProcesses().get(0).getItems().get(0).setInspectionTool("气密性检测工装");
  assertThrows(RuntimeException.class,()->service.publish(q));verify(regulations,never()).publish(any());
 }
 @Test void differentRegulationCannotAuthorizeLegacyReuse() {
  legacy(null,false);var other=regulations.getPublishedVersion(129L,null);other.setRegulationId(999L);
  assertThrows(RuntimeException.class,()->service.publish(request(null)));verify(regulations,never()).publish(any());
 }
 @Test void commonOwnerCannotAuthorizeProductLegacyReuse() {
  when(mapper.selectByDccProjectCodeId(129L,"MES_QA")).thenReturn(MesQaInspectionRegulationDO.builder()
    .id(1L).dccProjectCodeId(129L).ownerModule("MES_QA_COMMON").build());
  assertThrows(RuntimeException.class,()->service.publish(request(null)));
  verify(regulations,never()).getPublishedVersion(any(),any());verify(regulations,never()).publish(any());
 }
 private void legacy(String reason,boolean duplicate) {
  when(mapper.selectByDccProjectCodeId(129L,"MES_QA")).thenReturn(MesQaInspectionRegulationDO.builder()
    .id(1L).dccProjectCodeId(129L).ownerModule("MES_QA").build());
  var old=MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem.builder()
    .itemCode("OLD").itemName("牢固度").inspectionTool("检测专用泵筒").equipmentUnboundReason(reason).equipmentOptions(List.of()).build();
  when(regulations.getPublishedVersion(129L,null)).thenReturn(MesQaInspectionRegulationPublishedVersionRespVO.builder()
    .dccProjectCodeId(129L).regulationId(1L).publishedVersionId(2L).lifecycleStatus("PUBLISHED")
    .processes(List.of(MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess.builder()
      .processName("装配").items(duplicate?List.of(old,old):List.of(old)).build())).build());
  lenient().when(equipment.listEnabledEquipmentOptionsByProjectVersionAndItemCodes(129L,2L,List.of("OLD")))
    .thenReturn(java.util.Map.of());
 }
 private static MesQaInspectionRegulationSaveReqVO request(String reason) {
  var i=new MesQaInspectionRegulationSaveReqVO.InspectionItem();i.setItemName("牢固度");i.setItemCode("OLD");
  i.setInspectionTool("检测专用泵筒");i.setEquipmentUnboundReason(reason);i.setEquipmentOptions(List.of());
  var p=new MesQaInspectionRegulationSaveReqVO.InspectionProcess();p.setProcessName("装配");p.setItems(List.of(i));
  var q=new MesQaInspectionRegulationSaveReqVO();q.setDccProjectCodeId(129L);q.setProcesses(List.of(p));return q;
 }
}
