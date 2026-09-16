const assert = require('node:assert/strict')
const { STAGES, stageResults } = require('./edhr-ai-loop/stages.cjs')
assert.deepEqual(STAGES.map((stage) => stage.id), ['S01', 'S02', 'S03', 'S04', 'S05', 'S06', 'S07', 'S08'])
assert.deepEqual(
  STAGES.map((stage) => stage.name),
  [
    '复制固定母单生成活跃测试订单',
    '一线生产及生产复核',
    '一线PQC及PQC复核',
    '领料晚到及无补料确认完成',
    'PQC生产放行及正式表单衔接',
    '四份资料齐套',
    '负责人最终放行',
    '归档及历史追溯'
  ]
)
assert.deepEqual(stageResults('S04').map((stage) => stage.status), ['PASS', 'PASS', 'PASS', 'FAIL', 'BLOCKED', 'BLOCKED', 'BLOCKED', 'BLOCKED'])
assert.deepEqual(stageResults('S01', 'BLOCKED').map((stage) => stage.status), ['BLOCKED', 'BLOCKED', 'BLOCKED', 'BLOCKED', 'BLOCKED', 'BLOCKED', 'BLOCKED', 'BLOCKED'])
console.log('PASS: eDHR AI loop stage result contract')
