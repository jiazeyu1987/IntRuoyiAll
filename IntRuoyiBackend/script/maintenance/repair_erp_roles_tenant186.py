"""Scoped local repair. Defaults to a complete transaction followed by ROLLBACK.

Requires pymysql, PyYAML and psutil. --apply commits only after all assertions.
Does not run the multi-tenant release migration or assign roles to users.
Recovery: the JSON backup contains the original bindings/package/revision.
Restore only those bindings and package after checking for subsequent changes;
never restore whole tables. Newly added menus may be shared after this repair.
"""
import argparse
import hashlib
import json
import re
import sys
from datetime import datetime
from pathlib import Path

import psutil
import pymysql
import yaml

TENANT = 186
SOURCE_PACKAGE = 116
DIRECTORY = 991511
MARKER = 'erp-tenant186-20261116'
PACKAGE_NAME = '测试1-ERP独立权限套餐'
LEGACY_CODES = ('tenant_admin', 'super_admin', 'mes_workshop_director', 'mes_team_leader', 'mes_scheduler')
DEPENDENCIES = {
    'erp_data_product_operator': ('erp:product-category:query', 'erp:product-unit:query'),
    'erp_data_stock_operator': ('erp:warehouse:query',),
    'erp_data_order_operator': ('mes:md-item:query',),
    'erp_data_config_operator': ('erp:kingdee-config:query', 'erp:kingdee-config:save'),
}


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def connect(root):
    listeners = {c.pid for c in psutil.net_connections(kind='tcp')
                 if c.laddr.port == 48081 and c.status == 'LISTEN'}
    require(len(listeners) == 1, 'Expected one local backend on 48081')
    command = psutil.Process(next(iter(listeners))).cmdline()
    require('--spring.profiles.active=local' in command, 'Expected local profile')
    documents = list(yaml.safe_load_all((root / 'IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml').read_text(encoding='utf-8')))
    configs = [d['spring']['datasource']['dynamic']['datasource']['master'] for d in documents
               if d and d.get('spring', {}).get('datasource', {}).get('dynamic')]
    require(len(configs) == 1, 'Expected one master datasource')
    config = dict(configs[0])
    for arg in command:
        for key in ('url', 'username', 'password'):
            prop = 'spring.datasource.dynamic.datasource.master.' + key
            if arg.startswith('--' + prop + '=') or arg.startswith('-D' + prop + '='):
                config[key] = arg.split('=', 1)[1]
    match = re.fullmatch(r'jdbc:mysql://([^:/]+):(\d+)/([^?]+)(?:\?.*)?', config['url'])
    require(match and match[1] == '127.0.0.2' and int(match[2]) == 23306 and match[3] == 'ruoyi-vue-pro', 'Unexpected runtime database')
    return pymysql.connect(host=match[1], port=int(match[2]), database=match[3],
                           user=str(config['username']), password=str(config['password']),
                           charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, ensure_ascii=False, default=str).encode()).hexdigest()


def repair(db, rows, output, apply):
    with db.cursor() as c:
        def query(sql, params=()):
            c.execute(sql, params)
            return c.fetchall()

        def one(sql, params=()):
            result = query(sql, params)
            require(len(result) == 1, 'Expected exactly one row: ' + sql.split(' FROM ')[0])
            return result[0]

        def unchanged_state():
            # No credentials, tokens or user profiles are read or saved.
            return {
                'tenants': query('SELECT id,name,package_id,status,update_time,updater FROM system_tenant WHERE id<>186 ORDER BY id'),
                'packages': query('SELECT * FROM system_tenant_package WHERE remark<>%s OR remark IS NULL ORDER BY id', (MARKER,)),
                'roles': query('SELECT * FROM system_role WHERE tenant_id<>186 ORDER BY id'),
                'categories': query('SELECT * FROM system_role_category WHERE tenant_id<>186 ORDER BY id'),
                'revisions': query('SELECT * FROM system_approval_authority_revision WHERE tenant_id<>186 ORDER BY tenant_id'),
                'bindings': query('SELECT * FROM system_role_menu WHERE tenant_id<>186 ORDER BY id'),
                'menus': query('SELECT * FROM system_menu WHERE id NOT BETWEEN 605073600 AND 605073608 ORDER BY id'),
            }

        require(one('SELECT @@server_uuid uuid')['uuid'] == '2bad8012-4dd0-11f1-b747-a2c4d62de614', 'Unexpected MySQL server')
        for table in ('system_tenant', 'system_tenant_package', 'system_role', 'system_role_category', 'system_role_menu', 'system_menu', 'system_approval_authority_revision'):
            require(one('SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=%s', (table,))['ENGINE'] == 'InnoDB', 'Nontransactional table: ' + table)
        db.rollback()
        db.begin()
        revision = one('SELECT * FROM system_approval_authority_revision WHERE tenant_id=186 FOR UPDATE')
        tenant = one('SELECT id,name,package_id,status,updater,update_time FROM system_tenant WHERE id=186 AND deleted=0 FOR UPDATE')
        require(tenant['name'] == '测试1' and tenant['status'] == 0, 'Unexpected target tenant')
        package = one('SELECT * FROM system_tenant_package WHERE id=116 AND deleted=0 FOR UPDATE')
        require(package['status'] == 0, 'Source package disabled')
        source_ids = json.loads(package['menu_ids'])
        require(isinstance(source_ids, list) and all(type(n) is int for n in source_ids), 'Invalid source package JSON')
        protected = digest(unchanged_state())
        menus = query('SELECT *,deleted+0 is_deleted FROM system_menu ORDER BY id')
        by_id = {m['id']: m for m in menus}
        directory = by_id[DIRECTORY]
        require(directory['path'] == '/mes/edhr-data-sync' and directory['status'] == 0 and directory['is_deleted'] == 0, 'Directory changed')
        operation_ids = {r['permission_id'] for r in rows}
        scoped = {DIRECTORY, 991502} | operation_ids
        grants = {}
        for r in rows:
            page = by_id[r['page_id']]
            require(page['parent_id'] == DIRECTORY and page['type'] == 2 and page['status'] == 0 and page['is_deleted'] == 0, 'Page prerequisite changed')
            require(r['page_id'] in source_ids, 'Page missing from package')
            nodes = {r['page_id']}
            frontier = set(nodes)
            while frontier:
                children = {m['id'] for m in menus if m['parent_id'] in frontier and m['is_deleted'] == 0}
                require(not children.intersection(nodes), 'Menu tree cycle')
                nodes.update(children)
                frontier = children
            scoped.update(nodes)
            # 991502 is retired and still carries golden-finger globally. Never grant it.
            require(991502 not in nodes, 'Legacy config unexpectedly inside active page')
            grant = nodes | {DIRECTORY, r['permission_id']}
            for permission in DEPENDENCIES.get(r['code'], ()):
                matches = {m['id'] for m in menus if m['permission'] == permission and m['is_deleted'] == 0 and m['status'] == 0}
                require(bool(matches), 'Missing lookup dependency: ' + permission)
                grant.update(matches)
            require((grant - operation_ids).issubset(set(source_ids)), 'Required existing grants missing from package')
            require(not any(m['permission'] == 'mes:pro-batch-record-execution:golden-finger' for m in menus if m['id'] in grant), 'Unrelated batch-record authority in role')
            grants[r['code']] = grant
            matches = [m for m in menus if m['id'] == r['permission_id'] or m['permission'] == r['permission']]
            require(not matches or (len(matches) == 1 and matches[0]['id'] == r['permission_id'] and matches[0]['permission'] == r['permission'] and matches[0]['parent_id'] == r['page_id'] and matches[0]['type'] == 3 and matches[0]['status'] == 0 and matches[0]['is_deleted'] == 0), 'Operation menu conflict')
        existing_roles = query('SELECT * FROM system_role WHERE tenant_id=186 FOR UPDATE')
        legacy_ids = {r['id'] for r in existing_roles if r['code'] in LEGACY_CODES and r['deleted'] == b'\x00'}
        require(len(legacy_ids) == 5, 'Expected five legacy roles')
        before_bindings = query('SELECT * FROM system_role_menu WHERE tenant_id=186 ORDER BY id FOR UPDATE')
        revoked = [r for r in before_bindings if r['role_id'] in legacy_ids and r['menu_id'] in scoped]
        backup = {'server_uuid': '2bad8012-4dd0-11f1-b747-a2c4d62de614', 'tenant': tenant,
                  'source_package_id': SOURCE_PACKAGE, 'revision': revision, 'revoked_bindings': revoked,
                  'unchanged_sha256': protected, 'operation_ids': sorted(operation_ids)}
        output.mkdir(parents=True, exist_ok=True)
        backup_path = output / ('before-' + datetime.now().strftime('%Y%m%d-%H%M%S-%f') + '.json')
        backup_path.write_text(json.dumps(backup, ensure_ascii=False, indent=2, default=str), encoding='utf-8')
        category = query("SELECT * FROM system_role_category WHERE tenant_id=186 AND code='erp_data' FOR UPDATE")
        new_roles = [r for r in existing_roles if r['code'].startswith('erp_data_')]
        completed = bool(category or new_roles or tenant['package_id'] != SOURCE_PACKAGE)
        expected_package_ids = source_ids + [r['permission_id'] for r in rows if r['permission_id'] not in source_ids]
        if not completed:
            require(not query('SELECT id FROM system_tenant_package WHERE name=%s OR remark=%s', (PACKAGE_NAME, MARKER)), 'Dedicated package identity already exists')
            for r in rows:
                if r['permission_id'] not in by_id:
                    c.execute('INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater) VALUES(%s,%s,%s,3,90,%s,\'\',\'\',\'\',\'\',0,1,1,1,%s,%s)',
                              (r['permission_id'], r['name'] + '操作', r['permission'], r['page_id'], MARKER, MARKER))
                    require(c.rowcount == 1, 'Menu insert failed')
            c.execute('INSERT INTO system_tenant_package(name,status,remark,menu_ids,creator,updater) VALUES(%s,0,%s,%s,%s,%s)',
                      (PACKAGE_NAME, MARKER, json.dumps(expected_package_ids), MARKER, MARKER))
            require(c.rowcount == 1, 'Package insert failed')
            new_package_id = c.lastrowid
            c.execute('UPDATE system_tenant SET package_id=%s,updater=%s WHERE id=186 AND package_id=116 AND deleted=0', (new_package_id, MARKER))
            require(c.rowcount == 1, 'Tenant package assignment failed')
            c.execute("INSERT INTO system_role_category(name,code,sort,status,remark,tenant_id,creator,updater) VALUES('ERP数据','erp_data',56,0,'ERP数据独立页签操作角色',186,%s,%s)", (MARKER, MARKER))
            require(c.rowcount == 1, 'Category insert failed')
            category_id = c.lastrowid
            for r in rows:
                c.execute('INSERT INTO system_role(name,code,sort,category_id,data_scope,status,type,remark,tenant_id,creator,updater) VALUES(%s,%s,%s,%s,1,0,2,%s,186,%s,%s)',
                          (r['name'] + '操作', r['code'], r['sort'], category_id, 'ERP data operation roles 20261116', MARKER, MARKER))
                require(c.rowcount == 1, 'Role insert failed')
                role_id = c.lastrowid
                for menu_id in sorted(grants[r['code']]):
                    c.execute('INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater) VALUES(%s,%s,186,%s,%s)', (role_id, menu_id, MARKER, MARKER))
                    require(c.rowcount == 1, 'Role menu insert failed')
            for binding in revoked:
                c.execute('DELETE FROM system_role_menu WHERE id=%s AND tenant_id=186 AND role_id=%s AND menu_id=%s', (binding['id'], binding['role_id'], binding['menu_id']))
                require(c.rowcount == 1, 'Legacy revocation failed')
            c.execute('UPDATE system_approval_authority_revision SET revision=revision+1,updated_at=NOW() WHERE tenant_id=186 AND revision=%s', (revision['revision'],))
            require(c.rowcount == 1, 'Authority revision update failed')
        final_tenant = one('SELECT package_id FROM system_tenant WHERE id=186')
        dedicated = one('SELECT * FROM system_tenant_package WHERE id=%s', (final_tenant['package_id'],))
        require(dedicated['name'] == PACKAGE_NAME and dedicated['remark'] == MARKER and dedicated['status'] == 0 and dedicated['deleted'] == b'\x00', 'Dedicated package mismatch')
        require(json.loads(dedicated['menu_ids']) == expected_package_ids, 'Dedicated package grants mismatch')
        require(query('SELECT id FROM system_tenant WHERE package_id=%s AND deleted=0', (dedicated['id'],)) == [{'id': TENANT}], 'Dedicated package is shared')
        final_category = one("SELECT * FROM system_role_category WHERE tenant_id=186 AND code='erp_data' AND deleted=0")
        require(final_category['name'] == 'ERP数据' and final_category['status'] == 0, 'Category mismatch')
        final_roles = query("SELECT * FROM system_role WHERE tenant_id=186 AND code LIKE 'erp_data_%%' AND deleted=0")
        require({r['code'] for r in final_roles} == set(grants) and len(final_roles) == 9, 'Role set mismatch')
        for r in rows:
            menu = one('SELECT id,permission,parent_id,type,status,deleted+0 is_deleted FROM system_menu WHERE id=%s', (r['permission_id'],))
            require(menu == dict(id=r['permission_id'], permission=r['permission'], parent_id=r['page_id'], type=3, status=0, is_deleted=0), 'Final operation menu mismatch')
        for role in final_roles:
            require(role['category_id'] == final_category['id'] and role['status'] == 0 and role['remark'] == 'ERP data operation roles 20261116', 'Role identity mismatch')
            actual = query('SELECT menu_id FROM system_role_menu WHERE role_id=%s AND tenant_id=186 AND deleted=0', (role['id'],))
            require({m['menu_id'] for m in actual} == grants[role['code']], 'Role grants mismatch')
        after_bindings = query('SELECT * FROM system_role_menu WHERE tenant_id=186 ORDER BY id')
        require(not any(r['role_id'] in legacy_ids and r['menu_id'] in scoped for r in after_bindings), 'Legacy ERP grants remain')
        original_other = [r for r in before_bindings if r not in revoked]
        original_ids = {r['id'] for r in before_bindings}
        require([r for r in after_bindings if r['id'] in original_ids] == original_other, 'Unrelated target role bindings changed')
        require([r for r in query('SELECT * FROM system_role WHERE tenant_id=186 ORDER BY id') if r['id'] in {old['id'] for old in existing_roles}]
                == sorted(existing_roles, key=lambda r: r['id']), 'Existing role records changed')
        require(digest(unchanged_state()) == protected, 'Out-of-scope data changed')
        summary = {'mode': 'APPLIED' if apply and not completed else 'VERIFIED' if completed else 'DRY_RUN_ROLLED_BACK',
                   'tenant_id': TENANT, 'package_id': dedicated['id'], 'roles': len(final_roles),
                   'grants': {k: len(v) for k, v in grants.items()}, 'revoked_bindings': len(revoked),
                   'unchanged_sha256': protected, 'backup': str(backup_path)}
        if apply:
            db.commit()
        else:
            db.rollback()
        print(json.dumps(summary, ensure_ascii=False))


def main():
    sys.stdout.reconfigure(encoding='utf-8')
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runtime-root', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    sql = (Path(__file__).resolve().parents[2] / 'sql/mysql/20261116_erp_data_operation_roles.sql').read_text(encoding='utf-8')
    rows = [dict(page_id=int(a), permission_id=int(b), name=c, code=d, permission=e, sort=int(f))
            for a,b,c,d,e,f in re.findall(r"\((\d+),(\d+),'([^']+)','(erp_data_[^']+)','(erp:data-[^']+)',(\d+)\)", sql)]
    require(len(rows) == 9, 'Expected nine roles from authoritative migration')
    with connect(args.runtime_root) as db:
        try:
            repair(db, rows, args.output, args.apply)
        except Exception:
            db.rollback()
            raise


if __name__ == '__main__':
    main()
