import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { parse } from '@vue/compiler-sfc'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

test('showroom route exposes a visible prompt tab after product management', () => {
  const source = readText('src/router/modules/showroom.ts')
  const productIndex = source.indexOf("name: 'ShowroomAdminProduct'")
  const promptIndex = source.indexOf("name: 'ShowroomAdminPrompt'")
  const hallIndex = source.indexOf("name: 'ShowroomAdminHall'")

  assert.notEqual(productIndex, -1, 'product route must exist')
  assert.notEqual(promptIndex, -1, 'prompt route must exist')
  assert.notEqual(hallIndex, -1, 'hall route must exist')
  assert.ok(productIndex < promptIndex && promptIndex < hallIndex)
  assert.match(source, /title: '提示管理'/)
})

test('showroom admin shell maps the prompt section to the dedicated workbench', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /PromptWorkbench/)
  assert.match(source, /activeSection === 'prompt'/)
  assert.match(source, /routeName: 'ShowroomAdminPrompt'/)

  const productIndex = source.indexOf("{ name: 'product', routeName: 'ShowroomAdminProduct' }")
  const promptIndex = source.indexOf("{ name: 'prompt', routeName: 'ShowroomAdminPrompt' }")
  const hallIndex = source.indexOf("{ name: 'hall', routeName: 'ShowroomAdminHall' }")
  assert.ok(productIndex !== -1 && promptIndex !== -1 && hallIndex !== -1)
  assert.ok(productIndex < promptIndex && promptIndex < hallIndex)
})

test('prompt workbench exposes current version, save form, placeholders, and history preview', () => {
  assert.ok(exists('src/views/showroom-admin/prompt/PromptWorkbench.vue'))
  assert.ok(exists('src/views/showroom-admin/prompt/index.ts'))

  const source = readText('src/views/showroom-admin/prompt/PromptWorkbench.vue')

  for (const token of [
    '当前生效版本',
    '保存新版本',
    '历史版本',
    '查看内容',
    'product_name_cn',
    'product_name_en',
    '当前用户无权访问提示管理',
    'ShowroomAdminApi.getImagePromptCurrent',
    'ShowroomAdminApi.getImagePromptHistory',
    'ShowroomAdminApi.saveImagePromptVersion'
  ]) {
    assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('prompt workbench template remains parseable and avoids raw nested moustache interpolation', () => {
  const filePath = path.join(root, 'src/views/showroom-admin/prompt/PromptWorkbench.vue')
  const source = fs.readFileSync(filePath, 'utf8')
  const parseResult = parse(source, { filename: filePath })

  if (parseResult.errors.length) {
    const details = parseResult.errors
      .map((error) => (error instanceof Error ? error.message : String(error)))
      .join('\n')
    throw new Error(`Vue SFC parse failed for ${filePath}\n${details}`)
  }

  assert.doesNotMatch(source, /\{\{\s*`?\{\{/)
  assert.match(source, /formatPlaceholder\(placeholderCode\)/)
})
