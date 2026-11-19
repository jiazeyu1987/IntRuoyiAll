package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.content.MesProProcessContentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessContentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessContentMapper;
import cn.iocoder.yudao.module.mes.service.common.MesSharedMasterDataChangeGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MesProProcessContentSharedChangeTest {
    private final MesProProcessContentServiceImpl service = new MesProProcessContentServiceImpl();
    private final MesProProcessContentMapper mapper = mock(MesProProcessContentMapper.class);
    private final MesSharedMasterDataChangeGuard guard = mock(MesSharedMasterDataChangeGuard.class);

    MesProProcessContentSharedChangeTest() {
        ReflectionTestUtils.setField(service, "processContentMapper", mapper);
        ReflectionTestUtils.setField(service, "sharedChangeGuard", guard);
        ReflectionTestUtils.setField(service, "processService", mock(MesProProcessService.class));
        when(mapper.selectById(7L)).thenReturn(MesProProcessContentDO.builder().id(7L).processId(10L).sort(1).build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "update", "delete", "deleteAll"})
    void directStepWritesCannotBypassReferencedProcessConfirmation(String action) {
        var request = new MesProProcessContentSaveReqVO();
        request.setId(7L); request.setProcessId(10L); request.setSort(1);
        doThrow(new IllegalStateException("需要影响确认")).when(guard).assertDirectUpdateAllowed("PROCESS", 10L);
        assertThrows(IllegalStateException.class, () -> {
            switch (action) {
                case "create" -> service.createProcessContent(request);
                case "update" -> service.updateProcessContent(request);
                case "delete" -> service.deleteProcessContent(7L);
                case "deleteAll" -> service.deleteProcessContentByProcessId(10L);
            }
        });
        verify(mapper, never()).insert(any(MesProProcessContentDO.class));
        verify(mapper, never()).updateById(any(MesProProcessContentDO.class));
        verify(mapper, never()).deleteById(any(Long.class));
        verify(mapper, never()).deleteByProcessId(anyLong());
    }

    @Test void updateCannotMoveStepToAnotherProcess() {
        var request = new MesProProcessContentSaveReqVO();
        request.setId(7L); request.setProcessId(20L); request.setSort(1);
        assertThrows(RuntimeException.class, () -> service.updateProcessContent(request));
        verify(mapper, never()).updateById(any(MesProProcessContentDO.class));
    }
}
