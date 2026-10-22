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
        when(dossiers.selectList(any(Wrapper.class))).thenReturn(List.of(row));
        when(files.getFile(9L)).thenReturn(FileDO.builder().id(9L).configId(1L).path("proof").size(2L).url("http://file").build());
        when(files.getFileContent(1L,"proof")).thenReturn(bytes);when(files.presignGetUrl("http://file",900)).thenReturn("http://file");
        var first=service.read(1L,3L,4L,5L,false);assertEquals(1,first.attachments().size());
        assertEquals(first.manifest(),service.read(1L,3L,4L,5L,true).manifest());
        order.setDossierRevision(2L);assertNotEquals(first.manifest(),service.read(1L,3L,4L,5L,false).manifest());
        when(files.getFileContent(1L,"proof")).thenReturn(new byte[]{3,4});assertThrows(IllegalStateException.class,()->service.read(1L,3L,4L,5L,true));
    }
    @Test void emptyListIsExplicitlySignedAndCrossTenantOrderFails() {
        var orders=mock(MesProcessPoolActiveOrderMapper.class);var dossiers=mock(MesProcessPoolActiveOrderDossierFileMapper.class);var files=mock(FileService.class);
        var order=MesProcessPoolActiveOrderDO.builder().id(3L).dossierRevision(0L).build();order.setTenantId(1L);
        when(orders.selectById(3L)).thenReturn(order);when(dossiers.selectList(any(Wrapper.class))).thenReturn(List.of());
        var service=new EdhrActiveOrderDossierManifest(orders,dossiers,files,mock(cn.iocoder.yudao.module.infra.service.file.FileConfigService.class));
        assertEquals(0,service.read(1L,3L,4L,5L,false).manifest().path("files").size());
        assertThrows(IllegalStateException.class,()->service.read(2L,3L,4L,5L,false));verifyNoInteractions(files);
    }
}
