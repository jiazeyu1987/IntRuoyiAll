const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const { test } = require('node:test')
const ts = require('typescript')

const page = fs.readFileSync(path.resolve(__dirname, '../../src/views/approval-center/index.vue'), 'utf8')
const script = page.split('<script lang="ts" setup>')[1].split('</script>')[0]
const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true)
const statement = ast.statements.find(node => ts.isVariableStatement(node)
  && node.declarationList.declarations.some(d => d.name.getText(ast) === 'resolveApprovalRemark'))
assert.ok(statement, 'the actual page must declare its remark resolver')
const compiled = ts.transpileModule(statement.getText(ast), {
  compilerOptions: { target: ts.ScriptTarget.ES2020 }
}).outputText
const resolveRemark = vm.runInNewContext(`${compiled}\nresolveApprovalRemark`)

test('DONE remarks display the formal opinion for approval and rejection without changing its text', () => {
  for (const approvalResult of ['APPROVE', 'REJECT']) {
    const approvalRemark = 'RUN20261105115100A7K2项目产品资料核对通过\n第二行：保留原文。'
    assert.equal(resolveRemark({ moduleCode: 'DCC', approvalResult, approvalRemark }), approvalRemark)
  }
})

test('DONE remarks never borrow a creation, write or rejection reason when the formal opinion is absent', () => {
  for (const approvalResult of ['APPROVE', 'REJECT', undefined]) {
    for (const approvalRemark of [undefined, null, '']) {
      assert.equal(resolveRemark({ approvalResult, approvalRemark,
        creationReason: '申请原因', writeReason: '重试写入原因', rejectReason: '其他节点驳回原因',
        sourceRemark: '来源说明' }), '--')
    }
  }
})
