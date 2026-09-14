const assert = require('assert')
const fs = require('fs')
const os = require('os')
const path = require('path')
const { pathToFileURL } = require('url')

async function main() {
  const root = path.resolve(__dirname, '..', '..')
  const {
    collectCodeReadonlyEvidence,
    resolveCodeReadonlySearchRoots,
    resolveCodeReadonlySearchTerms
  } = await import(
    pathToFileURL(path.join(root, 'scripts/codex-test-readonly-evidence.mjs')).href
  )
  const fixtureRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'codex-readonly-evidence-'))
  try {
    const files = {
      'IntRuoyiFronted/src/views/ProductionPersonnel.vue': '生产人员管理：维护正式员工和临时工。',
      'IntRuoyiFronted/src/router/modules/remaining.ts': 'const component = "TeamLeaderWorkbench"',
      'IntRuoyiFronted/src/api/a/DistractorApi.ts': 'export const unrelated = "API"',
      'IntRuoyiFronted/src/api/b/DistractorApi.ts': 'export const unrelated = "API"',
      'IntRuoyiFronted/src/api/c/DistractorApi.ts': 'export const unrelated = "API"',
      'IntRuoyiFronted/tests/e2e/production-personnel.spec.cjs': 'assert.ok("新增临时工并启用员工")',
      'IntRuoyiFronted/tests/e2e/production-personnel-000-static.spec.cjs': 'assert.ok("temporaryEmployee")',
      'IntRuoyiFronted/tests/e2e/production-personnel-001-static.spec.cjs': 'assert.ok("temporaryEmployee")',
      'IntRuoyiFronted/tests/e2e/production-personnel-002-static.spec.cjs': 'assert.ok("temporaryEmployee")',
      'IntRuoyiFronted/tests/e2e/production-personnel-management-real.e2e.js':
        'assert.ok("temporaryEmployee real flow")',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/example/service/PersonnelService.java':
        'class PersonnelService { String rule = "修改临时工密码"; }',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/example/dal/dataobject/MesProcessPoolTeamEmployeeProfileDO.java':
        'class MesProcessPoolTeamEmployeeProfileDO { boolean enabled; }',
      'IntRuoyiBackend/yudao-module-mes/src/test/java/example/PersonnelServiceTest.java':
        'class PersonnelServiceTest { String expected = "禁用员工"; }',
      'doc/tasks/unrelated/evidence.md': '生产人员管理：这条任务文档不得进入证据。',
      'IntRuoyiFronted/node_modules/example/index.js': '生产人员管理：依赖不得进入证据。'
    }
    for (let index = 0; index < 6; index += 1) {
      files[`IntRuoyiFronted/src/views/a/EmployeeContext${index}.vue`] = 'const unrelated = "employeeProfile"'
    }
    for (let index = 0; index < 10; index += 1) {
      files[
        `IntRuoyiBackend/yudao-module-mes/src/main/java/example/controller/vo/EmployeeProfileDistractor${index}.java`
      ] = 'class EmployeeProfileDistractor {}'
    }
    files[
      'IntRuoyiBackend/yudao-module-mes/src/main/java/example/controller/PersonnelController.java'
    ] = `${Array.from({ length: 30 }, (_, index) => `MesProcessPoolTeamLeaderNoise${index}`).join('\n')}\nvoid updateEmployeeStatus() { /* late business method */ }`
    files['IntRuoyiFronted/src/api/teamLeader.ts'] = `${Array.from(
      { length: 20 },
      (_, index) => `const createTemporaryTeamEmployee${index} = true`
    ).join('\n')}\nconst lateResetUrl = 'employee-profile/temp-signature-password/reset'`
    for (const [relativePath, content] of Object.entries(files)) {
      const filePath = path.join(fixtureRoot, relativePath)
      fs.mkdirSync(path.dirname(filePath), { recursive: true })
      fs.writeFileSync(filePath, content, 'utf8')
    }
    const task = {
      caseName: '批记录测试-生产组长-03-生产人员管理',
      methodText: '只读扫描当前代码，分析是否支持生产人员管理',
      testDataText: '维护正式员工和临时工，可新增临时工、修改临时工密码、启用或禁用员工。',
      checkpoints: [{
        sort: 1,
        name: '生产人员管理',
        expectedText: '当前代码、路由、API、权限、数据模型和测试能够满足生产人员管理：维护正式员工和临时工'
      }]
    }

    const terms = resolveCodeReadonlySearchTerms(task)
    const searchRoots = resolveCodeReadonlySearchRoots(fixtureRoot)
    const evidence = collectCodeReadonlyEvidence(task, fixtureRoot)

    assert.ok(terms.includes('生产人员管理'))
    assert.ok(terms.includes('TeamLeaderWorkbench'))
    assert.ok(terms.includes('MesProcessPoolTeamEmployeeProfile'))
    assert.ok(terms.includes('updateTeamEmployeeStatus'))
    assert.ok(terms.includes('employee-profile/status/update'))
    assert.ok(terms.includes('employee_profile'))
    assert.ok(!terms.includes('API'))
    assert.ok(searchRoots.includes('IntRuoyiBackend/yudao-module-mes/src/main'))
    assert.ok(searchRoots.includes('IntRuoyiBackend/yudao-module-mes/src/test'))
    assert.ok(
      searchRoots.every(
        (searchRoot) => searchRoot !== 'IntRuoyiBackend'
          && !searchRoot.includes('target')
      )
    )
    assert.match(evidence, /ProductionPersonnel\.vue/)
    assert.match(evidence, /remaining\.ts/)
    assert.match(evidence, /PersonnelService\.java/)
    assert.match(evidence, /MesProcessPoolTeamEmployeeProfileDO\.java/)
    assert.match(evidence, /late business method/)
    assert.match(evidence, /lateResetUrl/)
    assert.match(evidence, /PersonnelServiceTest\.java/)
    assert.match(evidence, /production-personnel-management-real\.e2e\.js/)
    assert.doesNotMatch(evidence, /DistractorApi|unrelated\/evidence\.md|node_modules/)
  } finally {
    fs.rmSync(fixtureRoot, { recursive: true, force: true })
  }
  console.log('codex-runner-readonly-evidence PASS')
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
