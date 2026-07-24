const fs = require('fs')
const path = require('path')

const apiPath = path.resolve(__dirname, '../../src/api/showroom-admin/index.ts')
const servicePath = path.resolve(__dirname, '../../src/config/axios/service.ts')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const serviceSource = fs.readFileSync(servicePath, 'utf8')

const getNarrationBlockMatch = apiSource.match(
  /getNarration:\s*async\s*\([\s\S]*?\)\s*=>\s*\{[\s\S]*?request\.get\(\{[\s\S]*?url:\s*'\/showroom\/narration\/get'[\s\S]*?\}\)[\s\S]*?\}/
)

if (!getNarrationBlockMatch) {
  throw new Error(`missing getNarration request block in ${apiPath}`)
}

const getNarrationBlock = getNarrationBlockMatch[0]

if (!getNarrationBlock.includes('ignoreErrorMessage: true')) {
  throw new Error(`getNarration must opt into silent error handling in ${apiPath}`)
}

if (!serviceSource.includes('ignoreErrorMessage')) {
  throw new Error(`axios service is missing ignoreErrorMessage support in ${servicePath}`)
}

if (!/else if \(code === 500\) \{[\s\S]*?if \(!ignoreErrorMessage\) \{[\s\S]*?ElMessage\.error/.test(serviceSource)) {
  throw new Error(`axios service must suppress 500 toast when ignoreErrorMessage is enabled in ${servicePath}`)
}

console.log('PASS: showroom product basic info narration request can suppress global error toasts')
