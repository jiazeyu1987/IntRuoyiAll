import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('BDD: 独立表单录入实时校验 -> 抽屉字段按模板规则提示并阻断保存提交', () => {
  const source = readText('src/views/mes/pro/edhr-form/FormPage.vue')

  assert.match(source, /import type \{ FormInstance, FormRules \} from 'element-plus'/)
  assert.match(source, /ref<FormInstance>\(\)/)
  assert.match(source, /:model="detailValues"/)
  assert.match(source, /:rules="detailValueRules"/)
  assert.match(source, /:prop="field\.key"/)
  assert.match(source, /validateDetailValues\(\)/)
  assert.match(source, /await detailValueFormRef\.value\.validate\(\)/)
  assert.match(
    source,
    /validateDetailValues\(\)\s*\n\s*const data = await saveEdhrFormInstanceDraft/
  )
  assert.match(source, /validateDetailValues\(\)\s*\n\s*const data = await submitEdhrFormInstance/)
  assert.match(source, /buildDetailValueRules/)
  assert.match(source, /validateEdhrFormFieldValue/)
  assert.match(source, /请输入\$\{field\.label\}/)
  assert.match(source, /不能小于/)
  assert.match(source, /不能大于/)
  assert.match(source, /请选择\$\{field\.label\}/)
  assert.match(source, /日期格式必须为 YYYY-MM-DD/)
  assert.match(source, /字段值不能超过 1000 个字符/)
})

test('BDD: 独立表单模板约束 -> 数字字段最大值不得小于最小值', () => {
  const source = readText('src/views/mes/pro/edhr-form/FormPage.vue')

  assert.match(source, /field\.min != null && field\.max != null && field\.min > field\.max/)
})
