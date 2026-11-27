package cn.iocoder.yudao.module.mes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class MesProTaskLockReasonTest {
 @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","\t\n","　"})
 void rejectsBlankBeforePersistence(String reason) {
  ServiceException error=assertThrows(ServiceException.class,()->new MesProTaskServiceImpl().lockTask(1L,reason));
  assertEquals("锁定原因不能为空",error.getMessage());
 }
}
