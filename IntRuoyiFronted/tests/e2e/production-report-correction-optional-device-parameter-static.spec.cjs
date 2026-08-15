const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const workspaceRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(workspaceRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(workspaceRoot, 'src/api/mes/pro/processpool/eventRevision.ts'),
  'utf8'
)

const between = (source, start, end) => {
  const startIndex = source.indexOf(start)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(startIndex, -1, `missing start marker: ${start}`)
  assert.notEqual(endIndex, -1, `missing end marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

test('production correction keeps missing device parameter values optional', () => {
  const openCorrection = between(
    pageSource,
    'const openProductionCorrection =',
    'const openPqcCorrection ='
  )
  const buildRequest = between(
    pageSource,
    'const buildProductionCorrectionRequest =',
    'const buildPqcCorrectionRequest ='
  )

  assert.doesNotMatch(openCorrection, /const value = Number\(item\.value\)/)
  assert.doesNotMatch(openCorrection, /报工设备参数快照不完整，不能修改/)
  assert.match(pageSource, /const optionalCorrectionNumber = \(value: unknown\): number \| undefined =>/)
  assert.match(pageSource, /value === null \|\| value === undefined \|\| value === ''/)
  assert.match(openCorrection, /optionalCorrectionNumber\(item\.value\)/)
  assert.match(buildRequest, /if \(value === undefined\) return \[\]/)
  assert.match(buildRequest, /deviceParameterReadings: correctionForm\.deviceParameterReadings\.flatMap/)
  assert.match(apiSource, /interface ProcessPoolProductionReportCorrectionParameterReqVO[\s\S]*value\?: number/)
})
