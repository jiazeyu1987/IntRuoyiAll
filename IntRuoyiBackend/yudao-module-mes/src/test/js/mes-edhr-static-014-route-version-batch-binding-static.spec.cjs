const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const backend = path.resolve(__dirname, '../../../../')
const mes = path.join(backend, 'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes')

const read = (base, file) => fs.readFileSync(path.join(base, file), 'utf8')
const sliceMethod = (source, signature) => {
  const startAt = source.indexOf(signature)
  assert.notEqual(startAt, -1, `missing method anchor: ${signature}`)
  const bodyStart = source.indexOf('{', startAt)
  assert.notEqual(bodyStart, -1, `missing method body: ${signature}`)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    const char = source[i]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(startAt, i + 1)
      }
    }
  }
  assert.fail(`unterminated method body: ${signature}`)
}

const routeProjection = read(mes, 'service/pro/route/MesProRouteVersionPublishProjectionServiceImpl.java')
const projectUseConfigs = sliceMethod(
  routeProjection,
  'private void projectUseConfigs(Long routeVersionId, Long routeId, String useType, JSONArray configs,'
)
assert(!projectUseConfigs.includes('routeFlowProcessBatchRecordMapper.deleteByRouteIdAndUseType'),
  'EDHR-STATIC-014: route V2 publish must not physically delete V1 batch-record bindings')
assert(projectUseConfigs.includes('Historical route-version batch-record bindings stay queryable by ID'),
  'EDHR-STATIC-014: publish code must document the frozen-order binding retention contract')
assert(!routeProjection.includes('routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()')
  && routeProjection.includes('projected form binding identity is required')
  && routeProjection.includes('projected batch record binding identity is required'),
  'EDHR-STATIC-014: route publish must insert projected bindings through identity-checked variables')
assert(routeProjection.includes('binding.put("routeBindingId", projectedBinding.getId())')
  && routeProjection.includes('report.put("routeBindingId", projectedBinding.getId())'),
  'EDHR-STATIC-014: rewritten route snapshots must carry newly projected routeBindingId values')

const routeService = read(mes, 'service/pro/route/MesProRouteServiceImpl.java')
const batchRecordReportSnapshot = sliceMethod(routeService, 'private JSONObject buildBatchRecordReportSnapshot(')
assert(batchRecordReportSnapshot.includes('report.put("routeBindingId", record.getId())'),
  'EDHR-STATIC-014: frozen route batchRecordReports snapshots must retain original routeBindingId')

const batchRecordMapper = read(mes, 'dal/mysql/pro/route/MesProRouteFlowProcessBatchRecordMapper.java')
assert(batchRecordMapper.includes('selectCurrentProjectionListByRouteIdAndUseType')
  && batchRecordMapper.includes('INNER JOIN mes_pro_route_flow_process_config pc')
  && batchRecordMapper.includes('pc.id = br.route_flow_process_config_id')
  && batchRecordMapper.includes('pc.deleted = FALSE'),
  'EDHR-STATIC-014: route-level current binding reads must exclude retained historical projection rows')

const batchRecordWriter = read(mes, 'service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.java')
assert(batchRecordWriter.includes('selectVersionedProductionReportBindings')
  && batchRecordWriter.includes('routeVersionSnapshotResolver.resolveVersion(command.getRouteVersionId())')
  && !batchRecordWriter.includes('routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType'),
  'EDHR-STATIC-014: release writer must resolve batch-record bindings from the frozen order route version')

const backfillCommand = read(mes, 'service/pro/processpool/team/MesTeamLeaderBatchRecordBackfillCommand.java')
const backfillService = read(mes, 'service/pro/processpool/team/MesTeamLeaderBatchRecordBackfillServiceImpl.java')
assert(backfillCommand.includes('private MesProRouteFlowProcessBatchRecordDO routeBinding')
  && backfillService.includes('requireVersionedFormalBinding(command)')
  && backfillService.includes('command.getRouteBinding() != null'),
  'EDHR-STATIC-014: downstream backfill must consume the frozen route binding instead of current master data')

console.log('PASS: EDHR-STATIC-014 route-version batch-record binding contract')
