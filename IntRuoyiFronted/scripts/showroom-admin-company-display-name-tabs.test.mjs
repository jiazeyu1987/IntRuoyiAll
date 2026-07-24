import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('company workbench header display name follows the active language tab', () => {
  const source = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')

  assert.match(source, /data-company-display-name/)
  assert.match(source, /data-company-display-language/)
  assert.match(source, /const activeDisplayCompanyName = computed\(\(\) =>/)
  assert.match(source, /const activeDisplayCompanyNameClass = computed\(\(\) =>/)
  assert.match(source, /activeDisplayLanguageTab\.value === 'zh'/)
  assert.match(source, /current\.value\?\.displayName \|\| '未命名公司'/)
  assert.match(source, /current\.value\?\.displayNameEn \|\| 'English company name not filled'/)
  assert.doesNotMatch(
    source,
    /showroom-company-workbench__subtitle showroom-company-workbench__subtitle--en">\s*\{\{\s*current\.displayNameEn \|\| 'English company name not filled'/
  )
})
