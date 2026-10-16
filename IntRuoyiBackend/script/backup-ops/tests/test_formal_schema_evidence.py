"""Schema contract tests; doubles are not real source/restore evidence."""
import importlib
from pathlib import Path
import sys
import pytest

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))

def module():
    return importlib.import_module('formal_schema_evidence')

@pytest.mark.parametrize('raw,expected',[(b'a\tb\\tc\n',[['a','b\tc']]),
    (b'\\N\tNULL\t\\0\\n\\r\\Z\\\\\n',[[None,'NULL','\0\n\r\x1a\\']]),(b'',[])])
def test_private_escaped_transport(raw,expected):
    assert module().parse_rows(raw)==expected

@pytest.mark.parametrize('raw',[b'truncated',b'\\x\n',b'\\\n',b'\xff\n',b'a\x00\n',b'a\r\n'])
def test_ambiguous_transport_refuses(raw):
    with pytest.raises(Exception,match='FORMAL_SCHEMA_TRANSPORT_INVALID'):module().parse_rows(raw)

class Catalog:
    container='a'*64
    def __init__(self):self.statements=[];self.trigger_order='1';self.table='CREATE TABLE `probe` (`id` int)';self.changed=False
    def schema_query(self,sql,*,database,expected_uuid,max_bytes):
        rows=self._response(sql)
        if 'FROM information_schema.' in sql:
            return [['N' if value is None else 'V'+value.encode().hex().upper() for value in row] for row in rows]
        return rows
    def _response(self,sql):
        self.statements.append(sql)
        if sql=='SHOW CREATE DATABASE `owned_schema`;':return [['owned_schema','CREATE DATABASE `owned_schema`']]
        if sql=='SHOW FULL TABLES;':return [['probe','BASE TABLE'],['visible','VIEW']]
        if sql.startswith('DESCRIBE information_schema.'):return [['stub']]
        if 'FROM information_schema.ROUTINES' in sql:return [['owned_function','FUNCTION'],['owned_procedure','PROCEDURE']]
        if 'FROM information_schema.TRIGGERS' in sql:return [['owned_trigger','probe','INSERT','BEFORE',self.trigger_order]]
        if 'FROM information_schema.EVENTS' in sql:return [['owned_event']]
        if 'FROM information_schema.COLUMNS' in sql:return [['probe','id','1','int','int',None,None,'YES',None,'','']]
        if sql=='SHOW CREATE TABLE `probe`;':return [['probe',self.table]]
        if sql=='SHOW CREATE VIEW `visible`;':return [['visible','CREATE VIEW `visible` AS select `id` from `probe`','utf8mb4','utf8mb4_0900_ai_ci']]
        if sql.startswith('SHOW CREATE FUNCTION'):return [['owned_function','STRICT_TRANS_TABLES','CREATE FUNCTION `owned_function`() RETURNS INT RETURN 7','utf8mb4','utf8mb4_0900_ai_ci','utf8mb4_0900_ai_ci']]
        if sql.startswith('SHOW CREATE PROCEDURE'):return [['owned_procedure','','CREATE PROCEDURE `owned_procedure`() SELECT 7','utf8mb4','utf8mb4_0900_ai_ci','utf8mb4_0900_ai_ci']]
        if sql.startswith('SHOW CREATE TRIGGER'):return [['owned_trigger','','CREATE TRIGGER `owned_trigger` BEFORE INSERT ON `probe` FOR EACH ROW SET NEW.id=7','utf8mb4','utf8mb4_0900_ai_ci','utf8mb4_0900_ai_ci','2026-01-01 00:00:00.00']]
        if sql.startswith('SHOW CREATE EVENT'):return [['owned_event','','SYSTEM','CREATE EVENT `owned_event` ON SCHEDULE EVERY 1 DAY DISABLE DO SELECT 1','utf8mb4','utf8mb4_0900_ai_ci','utf8mb4_0900_ai_ci']]
        raise AssertionError(sql)

def capture(db,guard=lambda:None):
    return module().observe_schema(db,'owned_schema','11111111-1111-1111-1111-111111111111',guard=guard)

def test_every_schema_object_and_column_and_trigger_order_is_bound():
    db=Catalog();guards=[];proof=capture(db,lambda:guards.append(True))
    assert proof['objectCounts']=={'DATABASE':1,'TABLE':1,'VIEW':1,'FUNCTION':1,'PROCEDURE':1,'TRIGGER':1,'EVENT':1}
    assert proof['fullDataEquivalenceVerified'] is False
    assert proof['definitions']['TRIGGER']['owned_trigger']==Catalog().schema_query('SHOW CREATE TRIGGER',database='',expected_uuid='',max_bytes=1)[0][:-1]
    assert proof['excludedHistoricalFields']==['SHOW CREATE TRIGGER.Created']
    assert len(guards)>=len(db.statements)*2
    module().compare_schema(proof,capture(Catalog()))

@pytest.mark.parametrize('kind',['table','trigger_order'])
def test_changed_definition_or_trigger_order_refuses(kind):
    before=capture(Catalog());db=Catalog()
    setattr(db,kind,'2' if kind=='trigger_order' else 'CREATE TABLE `probe` (`id` bigint)')
    with pytest.raises(Exception,match='FORMAL_SCHEMA_MISMATCH'):module().compare_schema(before,capture(db))

def test_guard_failure_stops_next_query():
    db=Catalog()
    def guard():raise RuntimeError('CALLER_GUARD_LOST')
    with pytest.raises(RuntimeError,match='CALLER_GUARD_LOST'):capture(db,guard)
    assert db.statements==[]

def test_catalog_changes_during_observation_refuse():
    db=Catalog();original=db.schema_query;calls=0
    def query(sql,**kw):
        nonlocal calls
        if sql=='SHOW FULL TABLES;':
            calls+=1
            if calls==2:return [['probe','BASE TABLE']]
        return original(sql,**kw)
    db.schema_query=query
    with pytest.raises(Exception,match='FORMAL_SCHEMA_CATALOG_CHANGED'):capture(db)

@pytest.mark.parametrize('kind',['UNKNOWN','BASE TABLE\n'])
def test_unsupported_table_category_refuses(kind):
    db=Catalog();original=db.schema_query
    db.schema_query=lambda sql,**kw:[['probe',kind]] if sql=='SHOW FULL TABLES;' else original(sql,**kw)
    with pytest.raises(Exception,match='FORMAL_SCHEMA_CATALOG_INVALID'):capture(db)

def test_tampered_observation_refuses_before_comparison():
    before=capture(Catalog());after=capture(Catalog());after['definitions']['TABLE']['probe'][1]='tampered'
    with pytest.raises(Exception,match='FORMAL_SCHEMA_PROOF_CHANGED'):module().compare_schema(before,after)

def test_unicode_line_separator_is_preserved_in_ddl():
    assert module().parse_rows('a\tcomment\u2028value\n'.encode())==[['a','comment\u2028value']]

def test_real_private_process_output_limit_and_no_stderr_exposure():
    m=module()
    with pytest.raises(Exception,match='FORMAL_SCHEMA_STREAM_LIMIT_OR_IO'):
        m._bounded_process([sys.executable,'-I','-c','import sys; sys.stdout.buffer.write(b"x"*1000000)'],b'',64,5)
    with pytest.raises(Exception,match='FORMAL_SCHEMA_QUERY_FAILED_OR_WARNING') as failure:
        m._bounded_process([sys.executable,'-I','-c','import sys; sys.stderr.write("secret-sentinel")'],b'',64,5)
    assert 'secret-sentinel' not in str(failure.value)

def test_real_private_process_timeout_covers_blocked_stdin():
    import time
    started=time.monotonic()
    with pytest.raises(Exception,match='FORMAL_SCHEMA_QUERY_TIMEOUT'):
        module()._bounded_process([sys.executable,'-I','-c','import time; time.sleep(20)'],b'x'*1000000,64,1)
    assert time.monotonic()-started<7

@pytest.mark.parametrize('warning,end_uuid',[('1','11111111-1111-1111-1111-111111111111'),
    ('0','22222222-2222-2222-2222-222222222222')])
def test_mysql_warning_or_uuid_change_never_returns_schema(monkeypatch,warning,end_uuid):
    m=module();identity='11111111-1111-1111-1111-111111111111'
    def command(arguments,input_bytes,limit,seconds):
        assert 'secret-sentinel' not in repr(arguments)
        assert input_bytes.startswith(b'secret-sentinel\n')
        assert '--raw' not in arguments and '--no-defaults' in arguments
        assert '--protocol=SOCKET' in arguments and 'timeout --signal=TERM' in arguments[6]
        return (identity+'\nprobe\tCREATE TABLE probe(id int)\n'+warning+'\n'+end_uuid+'\n').encode()
    monkeypatch.setattr(m,'_bounded_process',command)
    with pytest.raises(Exception,match='FORMAL_SCHEMA_IDENTITY_OR_WARNING'):
        m.SchemaMySql('a'*64,'secret-sentinel').schema_query('SHOW CREATE TABLE `probe`;',database='owned_schema',expected_uuid=identity)

def test_null_marker_cannot_collapse_to_literal_null():
    assert module()._decode_metadata([['N','V4E554C4C','V']])==[[None,'NULL','']]

def test_identity_tamper_refuses_even_if_schema_is_equal():
    source=capture(Catalog());target=capture(Catalog())
    target['observationIdentity']['containerId']='b'*64
    with pytest.raises(Exception,match='FORMAL_SCHEMA_PROOF_CHANGED'):module().compare_schema(source,target)


class NativeCharacterCatalog(Catalog):
    def __init__(self, *, explicit=False, name='id', comment='actual Chinese 注释'):
        super().__init__()
        self.name=name
        quoted=module().identifier(name)
        clause='CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci' if explicit else 'COLLATE utf8mb4_unicode_ci'
        self.table=('CREATE TABLE `probe` (\n  '+quoted+' varchar(32) '+clause+
            " DEFAULT 'literal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci' COMMENT '"+
            comment.replace("'","''")+"',\n  KEY `owned_index` ("+quoted+
            ')\n) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci')
        self.charset='utf8mb4';self.collation='utf8mb4_unicode_ci'
    def _response(self,sql):
        if 'FROM information_schema.COLUMNS' in sql:
            self.statements.append(sql)
            return [['probe',self.name,'1','varchar','varchar(32)',self.charset,self.collation,'YES',
                'literal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci','','']]
        return super()._response(sql)


@pytest.mark.parametrize('name',['id','col`tick','COLUMN CHARACTER SET utf8mb4'])
def test_native_catalog_bound_charset_display_equivalence_preserves_originals(name):
    source=capture(NativeCharacterCatalog(name=name))
    target=capture(NativeCharacterCatalog(explicit=True,name=name))
    import copy
    originals=copy.deepcopy((source,target))
    result=module().compare_schema(source,target)
    assert (source,target)==originals
    assert source['schemaSha256']!=target['schemaSha256']
    assert result['sourceSchemaSha256']==source['schemaSha256']
    assert result['targetSchemaSha256']==target['schemaSha256']
    assert result['comparisonEncoding']=='mysql8-catalog-bound-ddl/v1'
    assert len(result['canonicalSchemaSha256'])==64


@pytest.mark.parametrize('change',[
    ('CHARACTER SET utf8mb4 COLLATE','CHARACTER SET latin1 COLLATE'),
    ('COLLATE utf8mb4_unicode_ci NOT_A_TOKEN','COLLATE utf8mb4_general_ci NOT_A_TOKEN'),
    ('varchar(32)','varchar(33)'),('AUTO_INCREMENT=9','AUTO_INCREMENT=10'),
    ('owned_index','other_index'),('ENGINE=InnoDB','ENGINE=MyISAM'),
    ('actual Chinese 注释','changed Chinese 注释'),
    ("DEFAULT 'literal CHARACTER SET", "DEFAULT 'changed CHARACTER SET"),
    ('utf8mb4_unicode_ci\' COMMENT','utf8mb4_general_ci\' COMMENT'),
])
def test_native_charset_display_never_ignores_actual_ddl_changes(change):
    source=capture(NativeCharacterCatalog())
    database=NativeCharacterCatalog(explicit=True)
    before,after=change
    if 'NOT_A_TOKEN' in before:
        before='COLLATE utf8mb4_unicode_ci';after='COLLATE utf8mb4_general_ci'
    assert before in database.table
    database.table=database.table.replace(before,after)
    with pytest.raises(Exception,match='FORMAL_SCHEMA_MISMATCH'):
        module().compare_schema(source,capture(database))


@pytest.mark.parametrize('field,value',[('charset','latin1'),('collation','utf8mb4_general_ci')])
def test_native_catalog_change_blocks_display_equivalence(field,value):
    source=capture(NativeCharacterCatalog());database=NativeCharacterCatalog(explicit=True)
    setattr(database,field,value)
    with pytest.raises(Exception,match='FORMAL_SCHEMA_MISMATCH'):
        module().compare_schema(source,capture(database))


def test_native_display_literal_comment_is_never_rewritten():
    source=capture(NativeCharacterCatalog(comment='only COLLATE utf8mb4_unicode_ci'))
    target=capture(NativeCharacterCatalog(explicit=True,comment='only CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'))
    with pytest.raises(Exception,match='FORMAL_SCHEMA_MISMATCH'):module().compare_schema(source,target)


@pytest.mark.parametrize('tail',[
    " DEFAULT ('CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci')",
    " GENERATED ALWAYS AS (concat('CHARACTER SET utf8mb4', 'COLLATE utf8mb4_unicode_ci')) STORED",
    " COMMENT 'escaped \\' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'",
    " COMMENT 'double '' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'",
    ' /*!80023 INVISIBLE */',
    " CHECK (length(`id`) > length('CHARACTER SET utf8mb4'))",
])
def test_native_charset_display_preserves_nested_literals_and_executable_comments(tail):
    left=NativeCharacterCatalog();right=NativeCharacterCatalog(explicit=True)
    for database in (left,right):
        database.table=database.table[:database.table.index(' DEFAULT ')]+tail+'\n) ENGINE=InnoDB'
    result=module().compare_schema(capture(left),capture(right))
    assert result['comparisonEncoding']=='mysql8-catalog-bound-ddl/v1'


@pytest.mark.parametrize('kind',['missing','duplicate','ordinal-zero','ordinal-gap'])
def test_native_charset_display_refuses_invalid_column_bindings(kind):
    db=NativeCharacterCatalog();original=db._response
    def response(sql):
        rows=original(sql)
        if 'FROM information_schema.COLUMNS' not in sql:return rows
        if kind=='missing':return []
        if kind=='duplicate':return rows+rows
        rows[0][2]='0' if kind=='ordinal-zero' else '2'
        return rows
    db._response=response
    proof=capture(db)
    with pytest.raises(Exception,match='FORMAL_SCHEMA_COLUMN_BINDING_INVALID'):
        module().compare_schema(proof,proof)


@pytest.mark.parametrize('bad',[" COMMENT 'unterminated",' /* unterminated', ' DEFAULT (broken'])
def test_native_charset_display_refuses_unknown_or_unbalanced_lexical_input(bad):
    database=NativeCharacterCatalog()
    database.table=database.table[:database.table.index(' DEFAULT ')]+bad
    proof=capture(database)
    with pytest.raises(Exception,match='FORMAL_SCHEMA_DDL_INVALID'):
        module().compare_schema(proof,proof)
