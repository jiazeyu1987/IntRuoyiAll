const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const backend = path.resolve(__dirname, '../../../../')
const mes = path.join(backend, 'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes')
const testJava = path.join(backend, 'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes')

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

const reader = read(mes,
  'service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl.java')
const readMethod = sliceMethod(reader,
  'private InspectionSource readSource(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,')
assert(readMethod.includes('selectLockedQa(lockedDccQa, task)'),
  'EDHR-STATIC-005: reader must resolve QA identity per PQC task')
const selectLockedQa = sliceMethod(reader, 'private PublishedQa selectLockedQa(')
assert(selectLockedQa.includes('selectById(task == null ? null : task.getRegulationVersionId())'),
  'EDHR-STATIC-005: reader must load the version frozen on the PQC task')
assert(selectLockedQa.includes('OWNER_MODULE_MES_QA_COMMON'),
  'EDHR-STATIC-005: reader must accept common QA regulation owner module')
assert(!selectLockedQa.includes('lockedDccQa.qaRegulationVersionId')
  && !selectLockedQa.includes('lockedDccQa.qaRegulationId'),
  'EDHR-STATIC-005: reader must not validate every task against active-order dedicated QA fields')

const writer = read(mes,
  'service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java')
const validatePublishedQa = sliceMethod(writer, 'private boolean validatePublishedQa(')
assert(validatePublishedQa.includes('OWNER_MODULE_MES_QA_COMMON.equals(ownerModule)'),
  'EDHR-STATIC-005: writer must accept common QA tasks as formal process-inspection sources')
assert(validatePublishedQa.includes('Objects.equals(task.getRegulationVersionId(), version.getId())'),
  'EDHR-STATIC-005: writer must validate the published version against the task frozen version')
assert(!validatePublishedQa.includes('command.getQaRegulationVersionId')
  && !validatePublishedQa.includes('activeOrder.getQaRegulationVersionId'),
  'EDHR-STATIC-005: writer must not use the order special QA version for all tasks')

const provenance = read(mes,
  'service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImpl.java')
assert(provenance.includes('COMMON_QA_REGULATION_VERSION')
  && provenance.includes('OWNER_MODULE_MES_QA_COMMON.equals(regulation.getOwnerModule())'),
  'EDHR-STATIC-005: common QA task provenance must have an explicit accepted contract')

const readerTest = read(testJava,
  'service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest.java')
assert(readerTest.includes('readsDedicatedAndCommonQaTasksByEachTaskFrozenRegulationVersion')
  && readerTest.includes('COMMON_REGULATION_VERSION_ID')
  && readerTest.includes('OWNER_MODULE_MES_QA_COMMON'),
  'EDHR-STATIC-005: reader regression test must cover dedicated and common QA tasks together')

const writerTest = read(testJava,
  'service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest.java')
assert(writerTest.includes('commonQaTaskPlansAlongsideDedicatedQaTaskByItsFrozenTaskVersion')
  && writerTest.includes('COMMON_QA_REGULATION_VERSION'),
  'EDHR-STATIC-005: writer regression test must cover common QA planning with dedicated QA')

console.log('PASS: EDHR-STATIC-005 process inspection QA version contract')
