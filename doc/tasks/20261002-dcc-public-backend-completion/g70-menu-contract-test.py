"""Offline SQL/proposal contract. No DB transport, no process, no runtime claims."""
from pathlib import Path
import copy
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]
SQL = ROOT / 'IntRuoyiBackend/sql/mysql/20261006_dcc_native_distribution_menu.sql'
PAYLOAD = dict(name='文控下发', permission='dcc:controlled-file:distribute', type=3,
               sort=99, parent_id=6800, path='', icon='', component='', component_name='',
               status=0, visible=1, keep_alive=0, always_show=0, deleted=0)


def proposal(menus, roles, bindings):
    """Explicit review oracle for permitted changes; not a SQL engine/success emulator."""
    parent = [m for m in menus if m['id'] == 6800 and m['type'] == 1 and m['status'] == 0 and m['deleted'] == 0]
    actual = [r for r in roles if r['tenant_id'] == 1 and r['code'] == 'doc_control' and r['status'] == 0 and r['deleted'] == 0]
    if len(parent) != 1 or len(actual) != 1 or actual[0]['id'] != 910233:
        raise ValueError('SOURCE_IDENTITY')
    if sum(b['tenant_id'] == 1 and b['role_id'] == 910233 and b['menu_id'] == 6800 and b['deleted'] == 0 for b in bindings) != 1:
        raise ValueError('ANCESTOR')
    found = [m for m in menus if m.get('permission', '').rstrip().casefold() == PAYLOAD['permission'].casefold()]
    if len(found) > 1 or found and any(found[0].get(k) != v for k, v in PAYLOAD.items()):
        raise ValueError('PAYLOAD')
    add_menu = None
    if not found:
        if any(m['id'] == 6839 for m in menus):
            raise ValueError('ID_CONFLICT')
        add_menu = dict(id=6839, **PAYLOAD)
        target = 6839
    else:
        target = found[0]['id']
        if not isinstance(target, int) or isinstance(target, bool) or target <= 0:
            raise ValueError('ID')
    matching = [b for b in bindings if b['role_id'] == 910233 and b['menu_id'] == target]
    if matching and (len(matching) != 1 or matching[0]['tenant_id'] != 1 or matching[0]['deleted'] != 0):
        raise ValueError('BINDING')
    return add_menu, None if matching else dict(tenant_id=1, role_id=910233, menu_id=target, deleted=0)


class Contract(unittest.TestCase):
    def seed(self):
        return ([dict(id=6800,type=1,status=0,deleted=0)],
                [dict(id=910233,tenant_id=1,code='doc_control',status=0,deleted=0)],
                [dict(tenant_id=1,role_id=910233,menu_id=6800,deleted=0)])

    def test_first_then_repeat_and_old_objects_unchanged(self):
        menus, roles, bindings = self.seed()
        before = copy.deepcopy((menus,roles,bindings))
        menu, grant = proposal(menus,roles,bindings)
        self.assertEqual(6839, menu['id']); self.assertEqual(6839, grant['menu_id'])
        self.assertEqual(before, (menus,roles,bindings))
        self.assertEqual((None,None),proposal(menus+[menu],roles,bindings+[grant]))

    def test_exact_ui_created_id_preserved(self):
        menus, roles, bindings=self.seed()
        self.assertEqual((None,dict(tenant_id=1,role_id=910233,menu_id=991235,deleted=0)),
                         proposal(menus+[dict(id=991235,**PAYLOAD)],roles,bindings))

    def test_payload_duplicates_casefold_and_reserved_collision_rejected(self):
        menus,roles,bindings=self.seed()
        bad=[dict(id=6839,**{**PAYLOAD,'name':'其它操作'}),
             dict(id=991235,**{**PAYLOAD,'permission':PAYLOAD['permission'].upper()}),
             dict(id=991235,**{**PAYLOAD,'status':1})]
        for row in bad:
            with self.assertRaises(ValueError):proposal(menus+[row],roles,bindings)
        with self.assertRaises(ValueError):proposal(menus+[dict(id=1,**PAYLOAD),dict(id=2,**PAYLOAD)],roles,bindings)

    def test_foreign_role_or_deleted_binding_never_granted(self):
        menus,roles,bindings=self.seed()
        with self.assertRaises(ValueError):proposal(menus,[dict(**{**roles[0],'tenant_id':2})],bindings)
        with self.assertRaises(ValueError):proposal(menus+[dict(id=6839,**PAYLOAD)],roles,bindings+[dict(tenant_id=1,role_id=910233,menu_id=6839,deleted=1)])

    def test_formal_sql_is_insert_only_scoped_atomic_and_conflict_closed(self):
        self.assertTrue(SQL.exists(),'required exact native distribution seed is missing')
        sql=SQL.read_text(encoding='utf-8')
        code=re.sub(r'(?m)^\s*--.*$', '', sql)
        self.assertNotRegex(code,r'(?i)\b(?:UPDATE|DELETE|REPLACE|TRUNCATE|ALTER|CREATE\s+TABLE)\s+(?:`?system_|`?dcc_)')
        self.assertNotRegex(code,r'(?i)INSERT\s+IGNORE|ON\s+DUPLICATE|DROP\s+PROCEDURE\s+IF\s+EXISTS')
        self.assertEqual(['system_menu','system_role_menu'],re.findall(r'(?i)INSERT\s+INTO\s+`?(\w+)',code))
        for token in ['BINARY permission','v_role_id = 910233','tenant_id = 1','parent_id = 6800','ROLLBACK','RESIGNAL','COMMIT','ROW_COUNT()','v_menu_id']:
            self.assertIn(token,code)
        self.assertRegex(sql,r'dependsOn=20260630_dcc_admin_full_config_menu,20260714_dcc_distribution_training_menu_retire')
        create=re.findall(r'CREATE PROCEDURE (\w+)',sql);call=re.findall(r'CALL (\w+)',sql);drop=re.findall(r'DROP PROCEDURE (\w+)',sql)
        self.assertEqual(create,call);self.assertEqual(create,drop);self.assertLessEqual(len(create[0]),64)
        self.assertGreaterEqual(code.count('SIGNAL SQLSTATE'),9)


if __name__ == '__main__':
    unittest.main()
