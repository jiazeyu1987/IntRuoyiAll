const fs = require('fs')
const path = require('path')
const { parse } = require('@vue/compiler-sfc')

const filePath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/company/CompanyWorkbench.vue'
)
const source = fs.readFileSync(filePath, 'utf8')
const parseResult = parse(source, { filename: filePath })

if (parseResult.errors.length) {
  const details = parseResult.errors
    .map((error) => (error instanceof Error ? error.message : String(error)))
    .join('\n')
  throw new Error(`Vue SFC parse failed for ${filePath}\n${details}`)
}

if (/Click \\"Translate English Content\\" first/.test(source)) {
  throw new Error(
    `found raw escaped double quotes inside template attribute expression in ${filePath}`
  )
}

if (
  !/Click Translate English Content first, then adjust the English narration manually\./.test(
    source
  )
) {
  throw new Error(`missing expected English narration placeholder guidance in ${filePath}`)
}

console.log('PASS: CompanyWorkbench template is parseable and keeps the English guidance text')
