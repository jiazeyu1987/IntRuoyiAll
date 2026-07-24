const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()

const read = (relativePath) => {
  const absolutePath = path.resolve(root, relativePath)
  assert(fs.existsSync(absolutePath), `${relativePath} must exist`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertIncludes = (source, needle, message) => {
  assert(source.includes(needle), message)
}

const api = read('src/api/mes/pro/edhr/initBatch.ts')
for (const fragment of [
  "const EDHR_INIT_BATCH_BASE_URL = '/mes/pro/edhr-init-batch'",
  'getPage',
  'getDetail',
  'create',
  'uploadManifest',
  'runPrecheck',
  'getIssuePage',
  'manifestHash',
  'sourceFileName',
  'responsibleName'
]) {
  assertIncludes(api, fragment, `init batch API must keep ${fragment}`)
}

for (const forbidden of ['commitImport', 'rollbackRequest', 'signoff']) {
  assert(!api.includes(forbidden), `first slice must not expose ${forbidden}`)
}

const page = read('src/views/mes/pro/edhr-init-batch/InitBatchPage.vue')
for (const fragment of [
  'EdhrInitBatchApi.getPage',
  'EdhrInitBatchApi.create',
  'EdhrInitBatchApi.uploadManifest',
  'EdhrInitBatchApi.runPrecheck',
  'EdhrInitBatchApi.getIssuePage',
  '初始化批次',
  '创建批次',
  '上传 manifest',
  '执行预检',
  '预检问题',
  'manifestHash',
  '源文件',
  '行号',
  '字段',
  '责任人',
  '下一步动作',
  "v-hasPermi=\"['mes:pro-edhr-init-batch:create']\"",
  "v-hasPermi=\"['mes:pro-edhr-init-batch:precheck']\""
]) {
  assertIncludes(page, fragment, `init batch page must keep ${fragment}`)
}

for (const forbidden of [
  'catch {}',
  'catch (e) {}',
  'commitImport',
  'rollbackRequest',
  '标记完成'
]) {
  assert(!page.includes(forbidden), `first slice must not hide errors or expose ${forbidden}`)
}

console.log('PASS: eDHR init batch first slice static contract')
