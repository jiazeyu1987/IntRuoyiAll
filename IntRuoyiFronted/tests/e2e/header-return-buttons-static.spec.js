const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(frontendRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertStandardReturnButton = (source, file) => {
  assert.match(
    source,
    /<Icon\s+icon=["']ep:arrow-left["'][\s\S]*?>[\s\S]*?返回[\s\S]*?<\/el-button>/,
    `${file} must render header back controls as ep:arrow-left + standard “返回” label`
  )
}

const targets = [
  {
    file: 'src/views/form-center/template/index.vue',
    forbidden: [
      '返回表单模板',
      '{{ templateSimulationBackLabel }}'
    ]
  },
  {
    file: 'src/views/report/jmreport/index.vue',
    forbidden: ['返回报表列表']
  },
  {
    file: 'src/views/mes/pro/task/calendar/index.vue',
    forbidden: ['返回排产']
  },
  {
    file: 'src/views/mes/pro/route/RouteEditPage.vue',
    forbidden: ['返回列表']
  },
  {
    file: 'src/views/mes/pro/edhr/ApprovalDetailPage.vue',
    forbidden: ['返回审批列表']
  },
  {
    file: 'src/views/mes/pro/edhr/DomainTraceDetailPage.vue',
    forbidden: ['返回列表']
  },
  {
    file: 'src/views/mes/pro/edhr/FieldAuditDetailPage.vue',
    forbidden: ['返回列表']
  },
  {
    file: 'src/views/mes/pro/edhr/ExecutionPage.vue',
    forbidden: [
      '返回批次详情',
      '返回批次执行',
      '{{ backToBatchLabel }}'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue',
    forbidden: [
      '{{ backButtonLabel }}',
      'returnLabel.value',
      '返回模板说明',
      '返回批记录表单'
    ]
  },
  {
    file: 'src/views/mes/pro/edhr-batch/BatchExecutionTemplatePage.vue',
    forbidden: ['返回模板说明'],
    rendersReturnButton: false
  },
  {
    file: 'src/views/mes/pro/batchrecordformlist/index.vue',
    forbidden: ['返回批记录表单'],
    rendersReturnButton: false
  }
]

for (const target of targets) {
  const source = readSource(target.file)
  for (const label of target.forbidden) {
    assert.equal(
      source.includes(label),
      false,
      `${target.file} must not expose long header return label “${label}”; use standard “返回”`
    )
  }
  if (target.rendersReturnButton !== false) {
    assertStandardReturnButton(source, target.file)
  }
}

console.log('PASS: header return buttons use the standard return treatment')
