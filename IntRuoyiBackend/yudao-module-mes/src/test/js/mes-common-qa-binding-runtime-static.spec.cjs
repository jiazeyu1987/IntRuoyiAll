const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..', '..', '..')
const readRepo = (...segments) => fs.readFileSync(path.join(repoRoot, ...segments), 'utf8')

const globalExceptionHandler = readRepo('yudao-framework', 'yudao-spring-boot-starter-web',
  'src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'framework', 'web', 'core', 'handler',
  'GlobalExceptionHandler.java')

assert(!/StrUtil\.containsAny\(message,\s*"product_",\s*"promotion_",\s*"trade_"\)/.test(globalExceptionHandler),
  'Missing MES table names containing product_ must not be classified as disabled Mall module tables.')
assert(/isMissingTableName\(message,\s*"product_"\)/.test(globalExceptionHandler)
  && /isMissingTableName\(message,\s*"promotion_"\)/.test(globalExceptionHandler)
  && /isMissingTableName\(message,\s*"trade_"\)/.test(globalExceptionHandler),
  'Mall disabled-module detection must match actual missing table names, not arbitrary substrings.')

console.log('PASS: common QA binding runtime table-missing classification static contract')
