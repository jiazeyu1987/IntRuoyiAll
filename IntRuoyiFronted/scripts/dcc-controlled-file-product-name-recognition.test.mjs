import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')
const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')

const extractInterfaceBody = (source, interfaceName) => {
  const match = source.match(new RegExp(`export interface ${interfaceName} \\{([\\s\\S]*?)\\n\\}`))
  assert.ok(match, `${interfaceName} must be exported`)
  return match[1]
}

const extractProductNameItem = () => {
  const match = detailSource.match(
    /<el-descriptions-item label="产品名称">([\s\S]*?)<\/el-descriptions-item>/
  )
  assert.ok(match, 'product name description item must exist')
  return match[1]
}

const extractFunctionSource = (functionName) => {
  const marker = `const ${functionName} = async () => {`
  const start = detailSource.indexOf(marker)
  assert.notEqual(start, -1, `${functionName} must exist`)
  const nextFunction = detailSource.indexOf('\nconst ', start + marker.length)
  assert.notEqual(nextFunction, -1, `${functionName} must be followed by another top-level const`)
  return detailSource.slice(start, nextFunction)
}

test('BDD: product name recognition API -> Given doc_control clicks recognition, When frontend calls backend, Then it posts to the dedicated endpoint and expects the persisted name', () => {
  const respBody = extractInterfaceBody(workflowSource, 'ControlledFileProductNameRecognitionRespVO')
  assert.match(respBody, /controlledFileId:\s*number/)
  assert.match(respBody, /productName:\s*string/)
  assert.match(workflowSource, /export const recognizeControlledFileProductName = async/)
  assert.match(
    workflowSource,
    /request\.post\(\{\s*url:\s*`\/dcc\/controlled-files\/\$\{id\}\/recognize-product-name`\s*\}\)/
  )
})

test('BDD: product name recognition button -> Given detail page is visible to doc_control or super_admin, When product name row renders, Then recognition is next to the product name through the shared metadata editor gate', () => {
  const productNameItem = extractProductNameItem()
  assert.match(productNameItem, /fileDetail\?\.productName \|\| '-'/)
  assert.match(productNameItem, /识别/)
  assert.match(productNameItem, /v-if="canEditMetadata && fileDetail"/)
  assert.match(productNameItem, /:loading="productNameRecognitionLoading"/)
  assert.match(productNameItem, /@click="handleRecognizeProductName"/)
  assert.match(detailSource, /const SUPER_ADMIN_ROLE_CODE = 'super_admin'/)
  assert.match(detailSource, /roles\.includes\(DOC_CONTROL_ROLE_CODE\) \|\| roles\.includes\(SUPER_ADMIN_ROLE_CODE\)/)
})

test('BDD: product name recognition behavior -> Given backend returns a recognized name, When recognition succeeds, Then detail reloads from database instead of only mutating local display', () => {
  assert.match(detailSource, /recognizeControlledFileProductName/)
  assert.match(detailSource, /const productNameRecognitionLoading = ref\(false\)/)

  const handlerSource = extractFunctionSource('handleRecognizeProductName')
  assert.match(handlerSource, /productNameRecognitionLoading\.value = true/)
  assert.match(handlerSource, /await recognizeControlledFileProductName\(fileDetail\.value\.id\)/)
  assert.match(handlerSource, /message\.success\(`已识别产品名称：\$\{result\.productName\}`\)/)
  assert.match(handlerSource, /await reloadAll\(\)/)
  assert.match(handlerSource, /message\.error\(resolveReadSideErrorMessage\(error, '产品名称识别失败，请查看错误提示后重试。'\)\)/)
  assert.match(handlerSource, /productNameRecognitionLoading\.value = false/)
  assert.doesNotMatch(handlerSource, /fileDetail\.value\.productName\s*=/)
})
