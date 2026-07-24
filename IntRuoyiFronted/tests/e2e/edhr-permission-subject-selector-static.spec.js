const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr/PermissionMatrixPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const mustInclude = [
  {
    token: "import { getSimpleUserList, type UserVO } from '@/api/system/user'",
    reason: '权限主体用户选项必须来自真实系统用户 simple-list'
  },
  {
    token: "import { getSimpleRoleList, type RoleVO } from '@/api/system/role'",
    reason: '权限主体角色选项必须来自真实系统角色 simple-list'
  },
  {
    token: "import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'",
    reason: '权限主体部门选项必须来自真实系统部门 simple-list'
  },
  {
    token: 'const subjectOptionsLoading = ref(false)',
    reason: '主体选项加载状态必须显式呈现'
  },
  {
    token: 'const userOptions = ref<UserVO[]>([])',
    reason: '用户主体选项必须有独立状态'
  },
  {
    token: 'const roleOptions = ref<RoleVO[]>([])',
    reason: '角色主体选项必须有独立状态'
  },
  {
    token: 'const deptOptions = ref<DeptVO[]>([])',
    reason: '部门主体选项必须有独立状态'
  },
  {
    token: 'loadSubjectOptions',
    reason: '页面初始化必须加载真实主体选项'
  },
  {
    token: 'getSubjectOptions(row.subjectType)',
    reason: '主体下拉选项必须随主体类型切换'
  },
  {
    token: 'formatSubjectOption',
    reason: '下拉项必须展示可读名称而不是裸 ID'
  },
  {
    token: 'formatSubjectSummary(row)',
    reason: '规则行必须展示可读主体摘要'
  },
  {
    token: 'handleSubjectTypeChange(row)',
    reason: '主体类型切换时必须清空旧主体 ID'
  },
  {
    token: '@change="handleSubjectTypeChange(row)"',
    reason: '主体类型选择器必须绑定清空旧 ID 的事件'
  },
  {
    token: 'v-model="row.subjectId"',
    reason: '主体选择器仍必须写入权限规则 subjectId'
  },
  {
    token: 'filterable',
    reason: '主体选择器必须支持按名称搜索'
  },
  {
    token: ':loading="subjectOptionsLoading"',
    reason: '主体选择器必须暴露选项加载中状态'
  },
  {
    token: 'class="edhr-permission-matrix__subject-summary"',
    reason: '主体摘要必须有稳定样式类'
  }
]

const mustNotInclude = [
  {
    token: '<el-table-column label="主体ID" width="150">',
    reason: '权限规则主体不应继续要求管理员手填裸 ID'
  },
  {
    token: '<el-input-number v-model="row.subjectId"',
    reason: '主体 ID 不应再通过数字输入框维护'
  },
  {
    token: 'mock',
    reason: '主体选项不得使用 mock 数据'
  },
  {
    token: '静默',
    reason: '主体加载失败不得静默处理'
  },
  {
    token: '降级',
    reason: '主体加载失败不得降级为低可信录入路径'
  }
]

const failures = []

for (const { token, reason } of mustInclude) {
  if (!source.includes(token)) {
    failures.push(`缺少: ${reason} -> ${token}`)
  }
}

for (const { token, reason } of mustNotInclude) {
  if (source.includes(token)) {
    failures.push(`禁止: ${reason} -> ${token}`)
  }
}

if (failures.length) {
  throw new Error(`EDHR 权限矩阵主体选择器静态契约失败:\n${failures.join('\n')}`)
}

console.log('PASS edhr-permission-subject-selector-static')
