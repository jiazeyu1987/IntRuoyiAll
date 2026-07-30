package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolEventRevisionDiffContractTest {

    @Test
    void requiresFieldLevelDiff() {
        Set<String> fields = Arrays.stream(MesProProcessPoolEventRevisionDiffDO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        for (String required : Set.of(
                "fieldCode", "fieldName", "beforeValue", "afterValue",
                "affectsQuantityFragment", "sourceQuantityFragmentId",
                "originalFieldCode", "originalFieldName")) {
            assertTrue(fields.contains(required), "revision diff must contain " + required);
        }

        assertFalse(fields.contains("remarkOnly"), "F6 must not store only a remark instead of field diff");
        assertFalse(fields.contains("payloadOnly"), "F6 must not store only whole before/after payload");
    }
}
