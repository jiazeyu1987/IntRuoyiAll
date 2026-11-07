package cn.iocoder.yudao.module.mes.approval.content;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import org.junit.jupiter.api.Test;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class EdhrActiveOrderDossierManifestTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void correctionRoundRetainsOriginalUploadApplicationAndRejectsForeignProvenance(boolean locked) throws Exception {
        var orders=mock(MesProcessPoolActiveOrderMapper.class);
        var dossiers=mock(MesProcessPoolActiveOrderDossierFileMapper.class);
        var files=mock(FileService.class);
        var configurations=mock(cn.iocoder.yudao.module.infra.service.file.FileConfigService.class);
        var applications=mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var service=new EdhrActiveOrderDossierManifest(orders,dossiers,files,configurations);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"applications",applications);
        var order=MesProcessPoolActiveOrderDO.builder().id(3L).dossierRevision(2L).build();order.setTenantId(1L);
        when(orders.selectById(3L)).thenReturn(order);when(orders.selectByIdForUpdate(3L)).thenReturn(order);
        var original=MesProcessPoolActiveOrderReleaseApplicationDO.builder().id(2L).activeOrderId(3L).build();original.setTenantId(1L);
        when(applications.selectById(2L)).thenReturn(original);
        byte[] bytes={1,2};String hash=cn.hutool.crypto.digest.DigestUtil.sha256Hex(bytes);
        var rows=new java.util.ArrayList<MesProcessPoolActiveOrderDossierFileDO>();
        for(String category:List.of("INCOMING_INSPECTION_FILE","STERILIZATION_FILE","FINISHED_PRODUCT_FILE","OTHER_FILE")) {
            var row=MesProcessPoolActiveOrderDossierFileDO.builder().id(7L+rows.size()).activeOrderId(3L).applicationId(2L)
                    .fileId(9L).categoryKey(category).fileName("original.txt").fileSize(2L).contentType("text/plain")
                    .sha256(hash).storageConfigId(1L).storagePath("proof").build();row.setTenantId(1L);rows.add(row);
        }
        when(dossiers.selectList(any(Wrapper.class))).thenReturn(rows);
        when(files.getFile(9L)).thenReturn(FileDO.builder().id(9L).configId(1L).path("proof").size(2L).url("http://file").build());
        when(files.getFileContent(1L,"proof")).thenReturn(bytes);
        var client=mock(cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient.class);
        when(configurations.getFileClient(1L)).thenReturn(client);when(client.presignGetUrl("http://file",900)).thenReturn("http://file");
        var result=service.read(1L,3L,4L,5L,locked);
        assertEquals(4,result.attachments().size());
        assertEquals("4",result.manifest().path("applicationId").asText());
        assertEquals("2",result.manifest().path("files").get(0).path("applicationId").asText());
        verify(applications,times(1)).selectById(2L);
        original.setActiveOrderId(99L);
        assertThrows(IllegalStateException.class,()->service.read(1L,3L,4L,5L,locked));
        original.setActiveOrderId(3L);original.setTenantId(2L);
        assertThrows(IllegalStateException.class,()->service.read(1L,3L,4L,5L,locked));
        when(applications.selectById(2L)).thenReturn(null);
        assertThrows(IllegalStateException.class,()->service.read(1L,3L,4L,5L,locked));
        verify(dossiers,never()).updateById(any(MesProcessPoolActiveOrderDossierFileDO.class));
    }
    @Test void changeToAnyFileOrRevisionChangesSignedManifestAndMissingBytesFail() throws Exception {
        var orders=mock(MesProcessPoolActiveOrderMapper.class);var dossiers=mock(MesProcessPoolActiveOrderDossierFileMapper.class);var files=mock(FileService.class);
        var configurations=mock(cn.iocoder.yudao.module.infra.service.file.FileConfigService.class);
        var client=mock(cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient.class);
        when(configurations.getFileClient(1L)).thenReturn(client);when(client.presignGetUrl("http://file",900)).thenReturn("http://file");
        var service=new EdhrActiveOrderDossierManifest(orders,dossiers,files,configurations);
        var order=MesProcessPoolActiveOrderDO.builder().id(3L).dossierRevision(1L).build();order.setTenantId(1L);
        when(orders.selectById(3L)).thenReturn(order);when(orders.selectByIdForUpdate(3L)).thenReturn(order);
        byte[] bytes={1,2};String hash=cn.hutool.crypto.digest.DigestUtil.sha256Hex(bytes);
        var row=MesProcessPoolActiveOrderDossierFileDO.builder().id(7L).activeOrderId(3L).applicationId(4L).fileId(9L).categoryKey("OTHER_FILE")
                .fileName("evidence.txt").fileSize(2L).contentType("text/plain").sha256(hash).storageConfigId(1L).storagePath("proof").build();row.setTenantId(1L);
        var rows=new java.util.ArrayList<MesProcessPoolActiveOrderDossierFileDO>();
        for(String category:List.of("INCOMING_INSPECTION_FILE","STERILIZATION_FILE","FINISHED_PRODUCT_FILE","OTHER_FILE")) {
            var item=MesProcessPoolActiveOrderDossierFileDO.builder().id(7L+rows.size()).activeOrderId(3L).applicationId(4L)
                    .fileId(9L).categoryKey(category).fileName("evidence.txt").fileSize(2L).contentType("text/plain")
                    .sha256(hash).storageConfigId(1L).storagePath("proof").build();item.setTenantId(1L);rows.add(item);
        }
        when(dossiers.selectList(any(Wrapper.class))).thenReturn(rows);
        when(files.getFile(9L)).thenReturn(FileDO.builder().id(9L).configId(1L).path("proof").size(2L).url("http://file").build());
        when(files.getFileContent(1L,"proof")).thenReturn(bytes);when(files.presignGetUrl("http://file",900)).thenReturn("http://file");
        var first=service.read(1L,3L,4L,5L,false);assertEquals(4,first.attachments().size());
        assertEquals(first.manifest(),service.read(1L,3L,4L,5L,true).manifest());
        order.setDossierRevision(2L);assertNotEquals(first.manifest(),service.read(1L,3L,4L,5L,false).manifest());
        when(files.getFileContent(1L,"proof")).thenReturn(new byte[]{3,4});assertThrows(IllegalStateException.class,()->service.read(1L,3L,4L,5L,true));
    }
    @Test void emptyListBlocksPreviewAndNamesAllMissingCategoriesAndCrossTenantOrderFails() {
        var orders=mock(MesProcessPoolActiveOrderMapper.class);var dossiers=mock(MesProcessPoolActiveOrderDossierFileMapper.class);var files=mock(FileService.class);
        var order=MesProcessPoolActiveOrderDO.builder().id(3L).dossierRevision(0L).build();order.setTenantId(1L);
        when(orders.selectById(3L)).thenReturn(order);when(dossiers.selectList(any(Wrapper.class))).thenReturn(List.of());
        var service=new EdhrActiveOrderDossierManifest(orders,dossiers,files,mock(cn.iocoder.yudao.module.infra.service.file.FileConfigService.class));
        var error=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.read(1L,3L,4L,5L,false));
        for(String category:List.of("来料检文件","灭菌文件","成品检文件","其他文件（成品检记录）"))assertTrue(error.getMessage().contains(category));
        assertThrows(IllegalStateException.class,()->service.read(2L,3L,4L,5L,false));verifyNoInteractions(files);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"INCOMING_INSPECTION_FILE,来料检文件","STERILIZATION_FILE,灭菌文件","FINISHED_PRODUCT_FILE,成品检文件","OTHER_FILE,其他文件（成品检记录）"})
    void eachMissingCategoryBlocksEvenWithFourFilesInOtherCategories(String missing,String label) {
        var orders=mock(MesProcessPoolActiveOrderMapper.class);var dossiers=mock(MesProcessPoolActiveOrderDossierFileMapper.class);var files=mock(FileService.class);
        var order=MesProcessPoolActiveOrderDO.builder().id(3L).dossierRevision(4L).build();order.setTenantId(1L);
        when(orders.selectById(3L)).thenReturn(order);when(orders.selectByIdForUpdate(3L)).thenReturn(order);
        var rows=new java.util.ArrayList<MesProcessPoolActiveOrderDossierFileDO>();
        for(String category:List.of("INCOMING_INSPECTION_FILE","STERILIZATION_FILE","FINISHED_PRODUCT_FILE","OTHER_FILE"))if(!category.equals(missing))rows.add(MesProcessPoolActiveOrderDossierFileDO.builder().categoryKey(category).build());
        rows.add(rows.get(0));when(dossiers.selectList(any(Wrapper.class))).thenReturn(rows);
        var service=new EdhrActiveOrderDossierManifest(orders,dossiers,files,mock(cn.iocoder.yudao.module.infra.service.file.FileConfigService.class));
        for(boolean locked:List.of(false,true)) {
            var error=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.read(1L,3L,4L,5L,locked));
            assertEquals("上市放行资料未齐全，缺少："+label,error.getMessage());
        }
        verifyNoInteractions(files);
    }
}
