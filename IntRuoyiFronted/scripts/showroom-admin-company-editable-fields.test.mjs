import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom admin company page exposes manufacturing capability and honors fields for display, editing, and translation', () => {
  const contractsSource = readText('src/views/showroom-admin/company/contracts.ts')
  const profileFormSource = readText('src/views/showroom-admin/company/CompanyProfileForm.vue')
  const workbenchSource = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')

  assert.match(contractsSource, /companyFieldDefinitions/)
  assert.match(contractsSource, /core_manufacturing_capability/)
  assert.match(contractsSource, /honors_awards/)
  assert.doesNotMatch(contractsSource, /visibleCompanyFieldDefinitions/)
  assert.doesNotMatch(contractsSource, /definition\.key !== 'core_manufacturing_capability'/)
  assert.doesNotMatch(contractsSource, /definition\.key !== 'honors_awards'/)
  assert.match(contractsSource, /buildCompanyDraftPayload/)
  assert.match(contractsSource, /companyFieldDefinitions\.(map|flatMap)/)

  assert.match(profileFormSource, /companyFieldDefinitions/)
  assert.match(profileFormSource, /v-for="definition in companyFieldDefinitions"/)
  assert.match(profileFormSource, /resolveCompanyEnglishFieldKey\(definition\.key\)/)
  assert.match(workbenchSource, /v-for="definition in companyFieldDefinitions"/)
  assert.match(workbenchSource, /fieldCodes: companyFieldDefinitions\.map\(\(definition\) => definition\.key\)/)
  assert.match(workbenchSource, /buildCompanyTranslationSourceFields/)

  assert.match(contractsSource, /label: '核心制造能力'/)
  assert.match(contractsSource, /label: '荣誉资质'/)
  assert.match(contractsSource, /labelEn: 'Core Manufacturing Capability'/)
  assert.match(contractsSource, /labelEn: 'Honors and Awards'/)
})
