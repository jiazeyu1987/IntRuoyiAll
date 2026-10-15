package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DccNativeSupervisorPersistenceOrderTest {
 DccBoundWorkflowContractResolver resolver;DccControlledFileDO file;Map<String,Object> original;
 @BeforeEach void setup(){resolver=new DccBoundWorkflowContractResolver();file=DccControlledFileDO.builder().id(20L).build();original=new LinkedHashMap<>();original.put("payload",Map.of("actor","99","departments",List.of("51","52")));original.put("sourceDigest","same-digest");original.put("sourceToken","same-authenticated-token");var rows=mock(DccControlledFileRouteSnapshotMapper.class);ReflectionTestUtils.setField(resolver,"routes",rows);when(rows.selectListByControlledFileId(20L)).thenReturn(java.util.stream.IntStream.range(0,4).mapToObj(i->DccControlledFileRouteSnapshotDO.builder().runtimeApprovalNodesJson(JsonUtils.toJsonString(Map.of("nativeSupervisorSource",original))).build()).toList());}
 void require(Map<String,Object> frozen){ReflectionTestUtils.invokeMethod(resolver,"requireNativeSupervisorBpmSource",file,Map.of("DCC_NATIVE_SUPERVISOR_SOURCE",frozen));}
 @Test void restartedImmutableMapOrderDoesNotInvalidateIdenticalPersistedProof(){var reordered=new LinkedHashMap<String,Object>();reordered.put("sourceToken",original.get("sourceToken"));reordered.put("sourceDigest",original.get("sourceDigest"));reordered.put("payload",original.get("payload"));assertDoesNotThrow(()->require(reordered));}
 @Test void differentTokenStillRejects(){var changed=new LinkedHashMap<>(original);changed.put("sourceToken","other-token");assertThrows(IllegalStateException.class,()->require(changed));}
 @Test void differentActorTypeStillRejects(){var changed=new LinkedHashMap<>(original);changed.put("payload",Map.of("actor",99,"departments",List.of("51","52")));assertThrows(IllegalStateException.class,()->require(changed));}
 @Test void differentDepartmentOrderingStillRejects(){var changed=new LinkedHashMap<>(original);changed.put("payload",Map.of("actor","99","departments",List.of("52","51")));assertThrows(IllegalStateException.class,()->require(changed));}
}
