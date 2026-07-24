const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const component = read('src/views/mes/pro/route/RouteProcessList.vue')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

assertIncludes(component, 'hasDictValue', 'route process dict value guard')
assertIncludes(component, 'formatEmptyDictValue', 'route process empty dict display')
assertIncludes(
  component,
  'v-if="hasDictValue(scope.row.keyFlag)"',
  'key flag dict tag must guard null values'
)
assertIncludes(
  component,
  'v-if="hasDictValue(scope.row.checkFlag)"',
  'check flag dict tag must guard null values'
)
assertIncludes(component, '{{ formatEmptyDictValue(scope.row.keyFlag) }}', 'key flag empty display')
assertIncludes(component, '{{ formatEmptyDictValue(scope.row.checkFlag) }}', 'check flag empty display')
assertNotIncludes(
  component,
  'scope.row.linkType',
  'legacy link type field'
)
assertNotIncludes(
  component,
  'DICT_TYPE.MES_PRO_LINK_TYPE',
  'legacy link type dictionary'
)
assertNotIncludes(
  component,
  '<dict-tag :type="DICT_TYPE.INFRA_BOOLEAN_STRING" :value="scope.row.keyFlag" />',
  'unguarded key flag dict tag'
)
assertNotIncludes(
  component,
  '<dict-tag :type="DICT_TYPE.INFRA_BOOLEAN_STRING" :value="scope.row.checkFlag" />',
  'unguarded check flag dict tag'
)

console.log('mes-route-process-null-dict-tag-static PASS')
