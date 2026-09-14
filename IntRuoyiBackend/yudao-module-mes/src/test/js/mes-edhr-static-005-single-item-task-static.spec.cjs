const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const reader = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl.java')
const writer = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java')

assert.match(
  reader,
  /selectLockedQa\(task,\s*lockedDccQa\)/,
  'EDHR-STATIC-005 reader must resolve QA by each PQC task frozen regulationVersionId.'
)

assert.match(
  reader,
  /MesQaInspectionRegulationDO\.OWNER_MODULE_MES_QA_COMMON/,
  'EDHR-STATIC-005 reader must accept common MES_QA_COMMON frozen task versions.'
)

assert.match(
  writer,
  /taskScopedQaItems\(/,
  'EDHR-STATIC-005 writer must scope QA items to the task qaProcessId and qaItemCode.'
)

assert.doesNotMatch(
  writer,
  /&&\s*MesQaInspectionRegulationDO\.OWNER_MODULE_MES_QA\.equals\(regulation\.getOwnerModule\(\)\)/,
  'EDHR-STATIC-005 writer must not reject MES_QA_COMMON by hardcoding only MES_QA.'
)
