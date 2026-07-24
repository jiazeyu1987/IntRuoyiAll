package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccFileDirectoryServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccFileDirectoryServiceImpl service;

    @Mock
    private DccFileDirectoryMapper fileDirectoryMapper;

    @Test
    void listEnabledChildDirectories_parentMissing_throwsNotExists() {
        Long parentId = 99L;
        when(fileDirectoryMapper.selectById(parentId)).thenReturn(null);

        var ex = assertThrows(RuntimeException.class, () -> service.listEnabledChildDirectories(parentId));

        assertInstanceOf(cn.iocoder.yudao.framework.common.exception.ServiceException.class, ex);
        assertEquals(exception(FILE_DIRECTORY_NOT_EXISTS).getCode(),
                ((cn.iocoder.yudao.framework.common.exception.ServiceException) ex).getCode());
        verify(fileDirectoryMapper).selectById(parentId);
    }

    @Test
    void listEnabledChildDirectories_rootParent_returnsMapperResult() {
        List<DccFileDirectoryDO> expected = List.of(new DccFileDirectoryDO(), new DccFileDirectoryDO());
        when(fileDirectoryMapper.selectEnabledListByParentId(null)).thenReturn(expected);

        List<DccFileDirectoryDO> actual = service.listEnabledChildDirectories(null);

        assertSame(expected, actual);
        verify(fileDirectoryMapper).selectEnabledListByParentId(null);
    }

}
