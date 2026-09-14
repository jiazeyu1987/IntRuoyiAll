const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const nasPage = fs.readFileSync(path.join(root, 'src/views/system/nas/index.vue'), 'utf8')
const nasApi = fs.readFileSync(path.join(root, 'src/api/system/nas/index.ts'), 'utf8')

assert.match(nasPage, /统计未受控文件/, 'NAS 管理页必须新增独立的“统计未受控文件”按钮。')
assert.match(
  nasPage,
  /checkPermi\(\['infra:nas:query'\]\)[\s\S]*checkPermi\(\['dcc:controlled-file:query'\]\)/,
  '统计按钮必须同时要求 NAS 查询权限和受控文件查询权限。'
)
assert.match(
  nasPage,
  /1\. QMS documents[\s\S]*2\.DHF[\s\S]*3\.DMR/,
  '确认文案必须明确固定扫描三个 NAS 根目录。'
)
assert.match(
  nasPage,
  /无权限子目录[\s\S]*跳过/,
  '确认文案必须说明无权限子目录会跳过。'
)
assert.match(nasPage, /auditDialog|controlAudit/, '页面必须维护 NAS 受控统计任务状态。')
assert.match(nasPage, /currentPath/, '页面轮询必须展示当前扫描目录。')
assert.match(nasPage, /scannedFileCount/, '页面轮询必须展示已扫描文件数。')
assert.match(nasPage, /skippedDirectoryCount/, '页面轮询必须展示已跳过目录数。')
assert.match(nasPage, /重新下载报告/, '统计成功后必须保留重新下载报告入口。')
assert.match(nasPage, /downloadNasControlAuditReport/, '统计成功后必须调用专用下载 API。')
assert.match(
  nasApi,
  /nas-control-audit\/start[\s\S]*nas-control-audit\/\$\{taskId\}[\s\S]*nas-control-audit\/\$\{taskId\}\/download/,
  'NAS API wrapper 必须包含 start/get/download 三个专用接口。'
)
assert.match(nasApi, /request\.download/, '报告下载必须使用二进制下载请求。')
