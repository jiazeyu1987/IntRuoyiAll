const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const backend = path.resolve(__dirname, '../../../../')
const mes = path.join(backend, 'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes')

const read = (base, file) => fs.readFileSync(path.join(base, file), 'utf8')
const sliceMethod = (source, signature) => {
  const startAt = source.indexOf(signature)
  assert.notEqual(startAt, -1, `missing method anchor: ${signature}`)
  const bodyStart = source.indexOf('{', startAt)
  assert.notEqual(bodyStart, -1, `missing method body: ${signature}`)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    const char = source[i]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(startAt, i + 1)
      }
    }
  }
  assert.fail(`unterminated method body: ${signature}`)
}

const service = read(mes, 'service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java')
const correct = sliceMethod(service, 'public Long correct(MesProcessPoolPqcInspectionCorrectionCommand command)')

assert(service.includes('MesProEdhrNonconformanceReviewService'),
  'EDHR-STATIC-016: PQC correction service must depend on the formal nonconformance freeze gate')

const taskValidationAt = correct.indexOf('validateTask(event, task)')
const freezeAt = correct.indexOf('nonconformanceReviewService.ensureWorkOrderNotFrozen(task.getWorkOrderId(), "PQC更正")')
const signatureAt = correct.indexOf('recordCorrectionSignature(')
const revisionAt = correct.indexOf('revisionService.updatePqcInspectionRecord')
const formalWriteAt = correct.indexOf('updateFormalPqcTables(')

assert(taskValidationAt !== -1 && freezeAt > taskValidationAt,
  'EDHR-STATIC-016: PQC correction must validate the formal task, then check the frozen work order')
assert(signatureAt !== -1 && freezeAt < signatureAt,
  'EDHR-STATIC-016: frozen work-order check must happen before correction signature')
assert(revisionAt !== -1 && freezeAt < revisionAt,
  'EDHR-STATIC-016: frozen work-order check must happen before event revision')
assert(formalWriteAt !== -1 && freezeAt < formalWriteAt,
  'EDHR-STATIC-016: frozen work-order check must happen before formal PQC table mutation')

console.log('PASS: EDHR-STATIC-016 PQC correction nonconformance freeze contract')
