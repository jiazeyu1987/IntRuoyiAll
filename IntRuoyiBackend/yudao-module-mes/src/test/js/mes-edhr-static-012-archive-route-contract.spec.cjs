const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '../../..')
const javaRoot = path.join(moduleRoot, 'src/main/java/cn/iocoder/yudao/module/mes')
const testRoot = path.join(moduleRoot, 'src/test/java/cn/iocoder/yudao/module/mes')

const read = (base, file) => fs.readFileSync(path.join(base, file), 'utf8')
const sliceMethod = (source, signature) => {
  const startAt = source.indexOf(signature)
  assert.notEqual(startAt, -1, `missing method anchor: ${signature}`)
  const bodyStart = source.indexOf('{', startAt)
  assert.notEqual(bodyStart, -1, `missing method body: ${signature}`)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    if (source[i] === '{') {
      depth += 1
    } else if (source[i] === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(startAt, i + 1)
      }
    }
  }
  assert.fail(`unterminated method body: ${signature}`)
}

const service = read(javaRoot, 'service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java')
const openOrCreateFromProductionRelease = sliceMethod(service, 'public Long openOrCreateFromProductionRelease(')
assert(openOrCreateFromProductionRelease.includes('requireFrozenRouteIdentity(frozenRouteVersion, command.getRouteId())'),
  'EDHR-STATIC-012: production-release batch creation must derive route code/name from the frozen route snapshot')
assert(!openOrCreateFromProductionRelease.includes('.setRouteCode(route.getCode())')
  && !openOrCreateFromProductionRelease.includes('.setRouteName(route.getName())'),
  'EDHR-STATIC-012: production-release batch creation must not persist current route master code/name')
assert(openOrCreateFromProductionRelease.includes('.setRouteCode(frozenRouteIdentity.routeCode())')
  && openOrCreateFromProductionRelease.includes('.setRouteName(frozenRouteIdentity.routeName())'),
  'EDHR-STATIC-012: production-release batch creation must persist frozen route code/name')

const buildArchiveManifest = sliceMethod(service, 'private String buildArchiveManifest(')
assert(buildArchiveManifest.includes('requireArchiveFrozenRouteIdentity(batch)'),
  'EDHR-STATIC-012: archive manifest must validate frozen batch route identity before writing manifest')
assert(!buildArchiveManifest.includes('routeMapper.selectById(')
  && !buildArchiveManifest.includes('route.getCode()')
  && !buildArchiveManifest.includes('route.getName()'),
  'EDHR-STATIC-012: archive manifest must not read current route master code/name')
assert(buildArchiveManifest.includes('manifest.put("routeCode", batch.getRouteCode())')
  && buildArchiveManifest.includes('manifest.put("routeName", batch.getRouteName())'),
  'EDHR-STATIC-012: archive manifest must write batch-saved route code/name')
assert(buildArchiveManifest.includes('manifest.put("routeVersionId", batch.getRouteVersionId())')
  && buildArchiveManifest.includes('manifest.put("routeVersionNo", batch.getRouteVersionNo())'),
  'EDHR-STATIC-012: archive manifest must include the batch frozen route version')

const frozenGuard = sliceMethod(service, 'private void requireArchiveFrozenRouteIdentity(')
assert(frozenGuard.includes('StrUtil.isBlank(batch.getRouteSnapshotJson())')
  && frozenGuard.includes('JSON.parseObject(batch.getRouteSnapshotJson())'),
  'EDHR-STATIC-012: archive must fail fast when the frozen route snapshot JSON is missing or invalid')
assert(frozenGuard.includes('routeSnapshot.getLong("routeId")')
  && frozenGuard.includes('routeSnapshot.getString("routeCode")')
  && frozenGuard.includes('routeSnapshot.getString("routeName")'),
  'EDHR-STATIC-012: archive must validate route id/code/name against the frozen snapshot')
assert(frozenGuard.includes('PRO_EDHR_BATCH_EXECUTION_ROUTE_SNAPSHOT_REQUIRED'),
  'EDHR-STATIC-012: archive guard must expose a stable route snapshot blocker')

const serviceTest = read(testRoot, 'service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java')
assert(serviceTest.includes('openOrCreateFromProductionRelease_usesFrozenRouteSnapshotIdentityAfterRouteRenameBeforeBatchCreation'),
  'EDHR-STATIC-012: Java regression must cover route rename between active-order freeze and batch creation')
assert(serviceTest.includes('generateArchive_usesFrozenBatchRouteIdentityAfterCurrentRouteRenameAndDelete')
  && serviceTest.includes('currentRoute.setName("')
  && serviceTest.includes('routeMapper.deleteById(fixture.routeId())')
  && serviceTest.includes('assertEquals(frozenRouteCode,')
  && serviceTest.includes('assertEquals(frozenRouteName,'),
  'EDHR-STATIC-012: Java regression must cover current route rename and delete without manifest drift')
assert(serviceTest.includes('generateArchive_requiresFrozenRouteIdentityAndSnapshot')
  && serviceTest.includes('PRO_EDHR_BATCH_EXECUTION_ROUTE_SNAPSHOT_REQUIRED'),
  'EDHR-STATIC-012: Java regression must cover missing or inconsistent frozen route snapshot blocker')

console.log('PASS: EDHR-STATIC-012 archive route identity contract')
