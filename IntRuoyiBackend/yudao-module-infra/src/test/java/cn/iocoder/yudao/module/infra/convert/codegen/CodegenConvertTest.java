package cn.iocoder.yudao.module.infra.convert.codegen;

import cn.iocoder.yudao.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CodegenConvertTest {

    @Test
    public void testConvertTableInfo_onlyMapsGenerationSourceFields() {
        TableInfo tableInfo = mock(TableInfo.class);
        when(tableInfo.getName()).thenReturn("infra_config");
        when(tableInfo.getComment()).thenReturn("config");

        CodegenTableDO result = CodegenConvert.INSTANCE.convert(tableInfo);

        assertEquals("infra_config", result.getTableName());
        assertEquals("config", result.getTableComment());
        assertNull(result.getDataSourceConfigId());
        assertNull(result.getScene());
        assertNull(result.getAuthor());
    }

    @Test
    public void testConvertTableField_onlyMapsSchemaFields() {
        TableField field = mock(TableField.class);
        TableField.MetaInfo metaInfo = mock(TableField.MetaInfo.class);
        when(field.getName()).thenReturn("nick_name");
        when(field.getComment()).thenReturn("nick");
        when(field.getMetaInfo()).thenReturn(metaInfo);
        when(metaInfo.getJdbcType()).thenReturn(JdbcType.VARCHAR);
        when(metaInfo.isNullable()).thenReturn(false);
        when(field.isKeyFlag()).thenReturn(true);
        when(field.getColumnType()).thenReturn(DbColumnType.STRING);
        when(field.getPropertyName()).thenReturn("nickName");

        CodegenColumnDO result = CodegenConvert.INSTANCE.convert(field);

        assertEquals("nick_name", result.getColumnName());
        assertEquals("VARCHAR", result.getDataType());
        assertEquals("nick", result.getColumnComment());
        assertFalse(result.getNullable());
        assertTrue(result.getPrimaryKey());
        assertEquals("String", result.getJavaType());
        assertEquals("nickName", result.getJavaField());
        assertNull(result.getTableId());
        assertNull(result.getHtmlType());
    }

}
