"""Configuration adapter only. The existing common capture engine does the work.

No SQL dump, binlog replay, object copy, archive, restore, SSH or authorization is
implemented here. Callers supply actual observed/pinned inputs and retain the
existing claim/grant/lease, native quiescence, fences and independent readback.
"""
import re
from core import COLUMNS, VERSION, exact, need
from redis_cache import EXECUTOR_KEY

REFERENCE_COLUMNS = {
    'smallapp_tenants': ['tenant_id', 'name'],
    'smallapp_users': ['id', 'tenant_id', 'username', 'active'],
    # Binary session hashes/CSRF/password hashes stay in the protected SQL dump;
    # the common semantic inventory decodes UTF-8, so do not expose them here.
    'smallapp_sessions': ['tenant_id', 'user_id', 'expires_at'],
    'smallapp_records': ['id', 'tenant_id', 'owner_id', 'record_key', 'title', 'content', 'version'],
    'smallapp_objects': ['id', 'tenant_id', 'bucket', 'object_key', 'size_bytes', 'sha256', 'state'],
    'smallapp_attachments': ['tenant_id', 'record_id', 'object_id', 'filename'],
}


def pin(value):
    exact(value, ('path', 'bytes', 'sha256'))
    need(type(value['path']) is str and value['path'] and type(value['bytes']) is int and value['bytes'] > 0
         and type(value['sha256']) is str and re.fullmatch('[0-9a-f]{64}', value['sha256']), 'ACTUAL_PIN_REQUIRED')
    return value


def reference_recipe(source_identity, scope_pin):
    need(type(source_identity) is str and bool(source_identity), 'ACTUAL_SOURCE_IDENTITY_REQUIRED')
    pin(scope_pin)
    need(set(REFERENCE_COLUMNS) == set(COLUMNS)
         and all(set(columns) <= set(COLUMNS[table]) for table, columns in REFERENCE_COLUMNS.items()),
         'CAPTURE_SCHEMA_RECIPE_MISMATCH')
    return {'schemaVersion': 'backup-capture-reference-recipe/v1', 'sourceIdentity': source_identity,
            'referenceQueries': [{'table': table, 'columns': columns} for table, columns in REFERENCE_COLUMNS.items()],
            'objectBindings': [{'table': 'smallapp_objects', 'bucket': {'column': 'bucket'},
                'keyColumn': 'object_key', 'versionColumn': None, 'nullable': False,
                'keyEncoding': 'RAW_KEY', 'urlPrefix': None}],
            'maxRows': 500000, 'complete': True, 'coverageScopePin': scope_pin}


def derived_recipe(source_identity, *, redis_policy_config_pin, queue_policy_config_pin):
    need(type(source_identity) is str and bool(source_identity), 'ACTUAL_SOURCE_IDENTITY_REQUIRED')
    return {'schemaVersion': 'backup-capture-derived-recipe/v1', 'sourceIdentity': source_identity,
            'classes': {'redis': {'classification': 'REBUILD', 'requiredForBusiness': True,
                'executorKey': EXECUTOR_KEY, 'rebuildFrom': 'DATABASE',
                'configurationReferences': [pin(redis_policy_config_pin)]},
                'queue': {'classification': 'NOT_PRESENT', 'requiredForBusiness': False,
                'executorKey': None, 'rebuildFrom': None,
                'configurationReferences': [pin(queue_policy_config_pin)]}}}


def object_source(binding, point_type):
    exact(binding, ('sourceIdentity', 'endpoint', 'minioContainerId', 'containerPort', 'credentialReference',
        'bucket', 'sourceScopePin', 'versionPins', 'referenceRecipePin', 'derivedRecipePin', 'parentExportIndexPin'))
    need(point_type in ('FULL', 'INCREMENTAL') and (binding['parentExportIndexPin'] is None) == (point_type == 'FULL'),
         'AUTHENTIC_PARENT_EXPORT_REQUIRED')
    value = {'schemaVersion': 'backup-capture-object-source/v1',
             **{key: item for key, item in binding.items() if key != 'bucket'}, 'buckets': [binding['bucket']]}
    # Import the existing validator, not a sample-only validation implementation.
    from formal_capture_semantics import validate_source
    return validate_source(value, point_type)


def require_quiescence(receipt, source_ref):
    exact(receipt, ('schemaVersion', 'requestId', 'operation', 'state', 'pendingObjects', 'activeMutations',
                    'version', 'tenantId', 'logicalConfigRef', 'stopRequested', 'redis'))
    health = exact(receipt['redis'], ('state', 'serverVersion', 'mode', 'role', 'instanceId',
                                    'database', 'classification', 'rebuildFrom'))
    need(health['state'] == 'UP' and health['classification'] == 'REBUILD'
         and health['rebuildFrom'] == 'DATABASE' and health['mode'] == 'standalone'
         and health['role'] == 'master' and type(health['database']) is int and health['database'] == 0
         and type(health['instanceId']) is str
         and re.fullmatch(r'brlab-20261012-smallapp-source-[a-z0-9-]+', health['instanceId'])
         and type(health['serverVersion']) is str and re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', health['serverVersion'])
         and tuple(map(int, health['serverVersion'].split('.'))) >= (6, 2, 0), 'REDIS_QUIESCENCE_HEALTH_REQUIRED', 503)
    need(receipt['schemaVersion'] == 'SmallAppControlResult/v2' and receipt['version'] == VERSION
         and receipt['tenantId'] == 122 and receipt['logicalConfigRef'] == source_ref
         and source_ref == 'smallapp-20261014-source-r1'
         and type(receipt['requestId']) is str and re.fullmatch('[A-Za-z0-9_-]{1,80}', receipt['requestId'])
         and type(receipt['pendingObjects']) is int and receipt['pendingObjects'] == 0
         and type(receipt['activeMutations']) is int and receipt['activeMutations'] == 0
         and receipt['operation'] in ('QUIESCE', 'STOP')
         and receipt['state'] == ('QUIESCED' if receipt['operation'] == 'QUIESCE' else 'PROTECTED')
         and type(receipt['stopRequested']) is bool
         and receipt['stopRequested'] == (receipt['operation'] == 'STOP'), 'APP_QUIESCENCE_REQUIRED', 503)
    return receipt  # App barrier only; native ownership/stop/fences remain mandatory.
