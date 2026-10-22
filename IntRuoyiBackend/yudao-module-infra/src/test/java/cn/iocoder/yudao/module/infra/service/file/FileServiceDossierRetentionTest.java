package cn.iocoder.yudao.module.infra.service.file;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FileServiceDossierRetentionTest {
    @Test void genericDeleteMoveAndUploadCannotMutateReservedDossierObjects() throws Exception {
        var files=mock(FileMapper.class);var clients=mock(FileConfigService.class);
        var service=new FileServiceImpl();ReflectionTestUtils.setField(service,"fileMapper",files);
        ReflectionTestUtils.setField(service,"fileConfigService",clients);
        when(files.selectById(1L)).thenReturn(new FileDO().setId(1L).setPath("mes/active-order-dossier/71/10/key"));
        assertThrows(RuntimeException.class,()->service.deleteFile(1L));
        assertThrows(RuntimeException.class,()->service.moveFile(1L,"elsewhere"));
        assertThrows(RuntimeException.class,()->service.presignPutUrl("report.pdf","mes/active-order-dossier/71/10"));
        when(files.selectById(2L)).thenReturn(new FileDO().setId(2L).setPath("ordinary/report.pdf"));
        assertThrows(RuntimeException.class,()->service.moveFile(2L,"ordinary/../mes/active-order-dossier/71/10/key"));
        assertThrows(RuntimeException.class,()->service.presignPutUrl("report.pdf","MES/ACTIVE-ORDER-DOSSIER/71/10"));
        verifyNoInteractions(clients);verify(files,never()).deleteById(anyLong());
    }
}
