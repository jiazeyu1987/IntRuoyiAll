const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const routeGenerationService = fs.readFileSync(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteGenerationServiceImpl.java'
  ),
  'utf8'
)

const candidateFlowGraphMethod = routeGenerationService.match(
  /private Map<String, Object> buildCandidateFlowGraphSnapshot[\s\S]*?(?=\n    private List<Map<String, Object>> copyProcessSnapshots)/
)
assert.ok(candidateFlowGraphMethod, '必须存在候选流转图快照生成方法。')

assert.match(
  candidateFlowGraphMethod[0],
  /flowGraph\.put\("nodes",\s*copyProcessSnapshots\(processSnapshots\)\)/,
  'Word 导入候选版本的 flowGraph.nodes 必须使用独立节点副本，避免 Fastjson 输出 $ref。'
)
assert.doesNotMatch(
  candidateFlowGraphMethod[0],
  /flowGraph\.put\("nodes",\s*processSnapshots\)/,
  'Word 导入候选版本不得让顶层 processes 与 flowGraph.nodes 共用同一对象。'
)

console.log('PASS: Word 导入候选版本快照节点使用独立对象，不会序列化为 $ref')
