const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../../../..')
const mapperPath = path.join(
  repoRoot,
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/process/MesProProcessMapper.java'
)
const mapper = fs.readFileSync(mapperPath, 'utf8')

const methodBody = (methodName) => {
  const marker = `default ${methodName}`
  const start = mapper.indexOf(marker)
  assert.notStrictEqual(start, -1, `缺少 ${methodName} 方法`)
  const nextDefault = mapper.indexOf('\n    default ', start + marker.length)
  return mapper.slice(start, nextDefault === -1 ? mapper.length : nextDefault)
}

for (const methodName of ['PageResult<MesProProcessDO> selectPage', 'List<MesProProcessDO> selectList']) {
  const body = methodBody(methodName)
  assert.match(
    body,
    /\.eq\(MesProProcessDO::getDeleted,\s*false\)/,
    `${methodName} 必须显式排除 deleted 工序，避免旧工序进入分页并触发路线工序身份歧义。`
  )
}

console.log('mes-pro-process-mapper-deleted-filter-static PASS')
