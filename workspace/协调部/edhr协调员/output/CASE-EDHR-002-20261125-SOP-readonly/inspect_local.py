import hashlib
import io
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

import pymysql
import yaml

ROOT = Path(r'D:\ProjectPackage\IntRuoyi\IntRuoyiAll')
OUTPUT = Path(__file__).resolve().parent
JAR = ROOT / 'output/runtime/int_main/backend-3d6b93f81efc46abb8a63cf626468830/yudao-server-exec.jar'
ORDER_ID = 1009200644
ORDER_CODE = 'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def write_json(name, data):
    (OUTPUT / name).write_text(json.dumps(data, ensure_ascii=False, indent=2, default=str), encoding='utf-8')


with zipfile.ZipFile(JAR) as archive:
    masters = []
    for document in yaml.safe_load_all(archive.read('BOOT-INF/classes/application-local.yaml').decode('utf-8')):
        if isinstance(document, dict) and 'datasource' in document.get('spring', {}):
            masters.append(document['spring']['datasource']['dynamic']['datasource']['master'])
    assert len(masters) == 1, 'local datasource must be unique'
    master = masters[0]
    modules = [name for name in archive.namelist() if re.fullmatch(r'BOOT-INF/lib/yudao-module-mes-[0-9].*\.jar', name)]
    assert len(modules) == 1, 'MES runtime module must be unique'
    with zipfile.ZipFile(io.BytesIO(archive.read(modules[0]))) as module:
        classes = ['service/pro/frontline/MesFrontlineActiveOrderProcessServiceImpl', 'controller/admin/pro/feedback/MesFrontlineDeviceAccountController', 'service/pro/route/MesProRouteVersionSnapshotIdentityWriter', 'service/pro/route/MesProRouteVersionSnapshotValidator', 'service/pro/route/MesProRouteServiceImpl', 'service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl', 'service/pro/processpool/team/MesFixedTestOrderInitializer']
        evidence = []
        for name in classes:
            relative = 'cn/iocoder/yudao/module/mes/' + name + '.class'
            data = module.read(relative)
            destination = OUTPUT / 'runtime-classes' / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(data)
            compiled = ROOT / 'IntRuoyiBackend/yudao-module-mes/target/classes' / relative
            evidence.append({'class': name, 'runtime_sha256': digest(data), 'target_sha256': digest(compiled.read_bytes()) if compiled.exists() else None})
        write_json('runtime-classes.json', {'jar': str(JAR), 'jar_mtime': datetime.fromtimestamp(JAR.stat().st_mtime).isoformat(), 'module': modules[0], 'classes': evidence})

match = re.fullmatch(r'jdbc:mysql://(127\.0\.0\.1):(23306)/([^?]+)\?.*', master['url'])
assert match, 'not the authorized local datasource'
connection = pymysql.connect(host=match[1], port=int(match[2]), database=match[3], user=master['username'], password=str(master['password']), charset='utf8mb4', autocommit=False, cursorclass=pymysql.cursors.DictCursor)
try:
    with connection.cursor() as cursor:
        cursor.execute('START TRANSACTION READ ONLY')

        def select(sql, parameters=()):
            assert sql.lstrip().upper().startswith('SELECT '), 'only SELECT allowed'
            cursor.execute(sql, parameters)
            return cursor.fetchall()

        orders = select('SELECT id,tenant_id,leader_user_id,work_order_id,route_id,route_version_id,current_route_version_id,active_status,business_status,joined_at,create_time,update_time FROM mes_pro_process_pool_active_order WHERE id=%s AND deleted=0', (ORDER_ID,))
        assert len(orders) == 1, 'authorized instance must exist uniquely'
        order = orders[0]
        tenant = order['tenant_id']
        tenants = select('SELECT id,name FROM system_tenant WHERE id=%s AND deleted=0', (tenant,))
        assert len(tenants) == 1 and tenants[0]['name'] == '测试1', 'tenant mismatch'
        work_orders = select('SELECT id,code,batch_code,product_id,quantity,quantity_produced,status FROM mes_pro_work_order WHERE tenant_id=%s AND id=%s AND code=%s AND deleted=0', (tenant, order['work_order_id'], ORDER_CODE))
        assert len(work_orders) == 1, 'work order mismatch'
        routes = select('SELECT id,code,name,status,update_time FROM mes_pro_route WHERE tenant_id=%s AND id=%s AND deleted=0', (tenant, order['route_id']))
        versions = select("SELECT id,route_id,version_no,active,lifecycle_status,route_snapshot_json,route_snapshot_sha256,route_snapshot_format_version,create_time,update_time,published_time FROM mes_pro_route_version WHERE tenant_id=%s AND route_id=%s AND deleted=0 AND (id=%s OR (active=1 AND lifecycle_status='ACTIVE')) ORDER BY id", (tenant, order['route_id'], order['route_version_id']))
        version_evidence = []
        for version in versions:
            snapshot = json.loads(version.pop('route_snapshot_json'))
            nodes = snapshot['configSnapshots']['flowGraph']['nodes']
            version['flow_node_evidence'] = [{'routeProcessId': node.get('routeProcessId'), 'processId': node.get('processId'), 'processCode': node.get('processCode'), 'processName': node.get('processName'), 'keys': sorted(node.keys()), 'sop_field_present': 'sopFileUrl' in node, 'sop_state': 'MISSING' if 'sopFileUrl' not in node else ('NULL' if node['sopFileUrl'] is None else ('EMPTY' if node['sopFileUrl'] == '' else 'BOUND'))} for node in nodes]
            version['flow_node_count'] = len(nodes)
            version['missing_sop_field_count'] = sum('sopFileUrl' not in node for node in nodes)
            version_evidence.append(version)
        current_processes = select("SELECT rp.id route_process_id,rp.process_id,rp.sort,p.code,p.name,p.sop_file_url IS NULL sop_is_null,p.sop_file_url='' sop_is_empty,p.update_time process_update_time FROM mes_pro_route_process rp JOIN mes_pro_process p ON p.id=rp.process_id AND p.tenant_id=rp.tenant_id AND p.deleted=0 WHERE rp.tenant_id=%s AND rp.route_id=%s AND rp.deleted=0 ORDER BY rp.sort,rp.id", (tenant, order['route_id']))
        targets = select('SELECT id,route_id,route_version_id,route_process_id,process_id,process_code_snapshot,process_name_snapshot,planned_quantity_snapshot,create_time FROM mes_pro_process_pool_active_order_process_snapshot WHERE tenant_id=%s AND active_order_id=%s AND deleted=0 ORDER BY id', (tenant, ORDER_ID))
        write_json('local-target-snapshot-evidence.json', {'observed_at': datetime.now().isoformat(), 'connection_scope': '127.0.0.1:23306, read-only transaction', 'tenant': tenants[0], 'active_order': order, 'work_order': work_orders[0], 'route': routes, 'route_versions': version_evidence, 'current_route_processes': current_processes, 'active_order_process_targets': targets})
        print(json.dumps({'tenant': tenants[0], 'active_order': order, 'route': routes, 'versions': [{key: value for key, value in version.items() if key != 'flow_node_evidence'} for version in version_evidence], 'current_route_process_count': len(current_processes), 'current_route_unbound_sop_count': sum(bool(process['sop_is_null'] or process['sop_is_empty']) for process in current_processes), 'active_order_process_target_count': len(targets)}, ensure_ascii=False, indent=2, default=str))
finally:
    connection.rollback()
    connection.close()
