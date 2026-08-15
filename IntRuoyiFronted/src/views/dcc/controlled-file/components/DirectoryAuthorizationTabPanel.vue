<template>
  <el-row :gutter="16">
    <el-col :span="7">
      <ContentWrap>
        <div class="mb-12px flex items-center gap-8px">
          <el-input
            v-model="directoryKeyword"
            clearable
            placeholder="筛选已绑定目录"
            class="flex-1"
          >
            <template #prefix>
              <Icon icon="ep:search" />
            </template>
          </el-input>
        </div>

        <div class="access-rule-bound-directory-list" data-testid="dcc-access-rule-bound-directory-list">
          <div
            v-for="directory in filteredBoundDirectories"
            :key="directory.id"
            class="access-rule-bound-directory-list__item"
            :class="{ 'is-active': directory.id === selectedDirectoryId && !isDraftDirectory }"
            data-testid="dcc-access-rule-bound-directory-item"
            @click="handleBoundDirectoryClick(directory)"
          >
            <div class="access-rule-bound-directory-list__content">
              <div class="access-rule-bound-directory-list__path" :title="directory.directoryPath">
                {{ directory.directoryPath }}
              </div>
            </div>
            <el-button
              link
              type="danger"
              class="access-rule-bound-directory-list__delete"
              @click.stop="deleteBoundDirectory(directory)"
            >
              删除
            </el-button>
          </div>

          <el-empty
            v-if="!filteredBoundDirectories.length"
            description="当前暂无已绑定目录，请使用新增目录开始维护"
            :image-size="52"
          />
        </div>
      </ContentWrap>
    </el-col>

    <el-col :span="17">
      <ContentWrap>
        <div v-if="showDirectoryPicker" class="access-rule-directory-picker">
          <el-tree-select
            v-model="draftSelectedDirectoryId"
            :data="directories"
            :props="defaultProps"
            check-strictly
            clearable
            default-expand-all
            filterable
            node-key="id"
            placeholder="请选择目录"
            :render-after-expand="false"
            @change="handleDirectoryPickerChange"
          />
          <div class="access-rule-directory-picker__hint">
            选择目录后直接进入维护；已绑定目录只切换，未绑定目录保存后加入左侧列表。
          </div>
        </div>

        <UnifiedListTemplate
          class="directory-authorization-list-template"
          table-key="dcc.controlledFile.permission.directoryAuthorization"
          :query-model="directoryAuthorizationQueryParams"
          label-width="76px"
          :filter-definitions="directoryAuthorizationQuickFilterDefinitions"
          :quick-filter-state="directoryAuthorizationQuickFilter.state"
          :selected-filter-definition="directoryAuthorizationQuickFilter.selectedDefinition.value"
          :operator-options="directoryAuthorizationQuickFilter.operatorOptions.value"
          :columns="directoryAuthorizationColumns"
          :column-saving="directoryAuthorizationColumnSaving"
          :show-column-reset="false"
          :total="directoryAuthorizationTotal"
          v-model:page="directoryAuthorizationQueryParams.pageNo"
          v-model:limit="directoryAuthorizationQueryParams.pageSize"
          @update:quick-filter-state="directoryAuthorizationQuickFilter.updateState"
          @quick-filter-query="directoryAuthorizationQuickFilter.applyQuickFilter"
          @column-change="saveDirectoryAuthorizationColumnConfig"
          @pagination="handleDirectoryAuthorizationPagination"
        >
          <template #actions>
            <el-button type="primary" plain @click="toggleDirectoryPicker">
              <Icon icon="ep:plus" class="mr-5px" />
              新增目录
            </el-button>
            <el-button
              type="primary"
              plain
              @click="addRule"
              :disabled="!selectedDirectoryId"
              v-hasPermi="['dcc:controlled-file:access-rule:manage']"
            >
              <Icon icon="ep:plus" class="mr-5px" />
              新增规则
            </el-button>
            <el-button
              type="primary"
              @click="saveRules"
              :disabled="!selectedDirectoryId"
              :loading="saveLoading"
              v-hasPermi="['dcc:controlled-file:access-rule:manage']"
            >
              <Icon icon="ep:check" class="mr-5px" />
              保存规则
            </el-button>
          </template>
          <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
            <el-table
              v-loading="loading"
              data-user-table-column-explicit
              data-user-table-key="dcc.controlledFile.permission.directoryAuthorization"
              :data="paginatedDirectoryAuthorizationRules"
              border
              :stripe="true"
              :show-overflow-tooltip="true"
              empty-text="当前目录暂无访问规则"
              row-key="subjectId"
              @header-dragend="handleDirectoryAuthorizationHeaderDragend"
              @sort-change="handleTemplateSortChange"
            >
              <el-table-column
                v-if="isDirectoryAuthorizationColumnVisible('subjectType')"
                label="主体类型"
                prop="subjectType"
                :width="getDirectoryAuthorizationColumnWidthString('subjectType', 150)"
                v-bind="sortColumnAttrs('subjectType')"
              >
                <template #default="{ row }">
                  <el-select
                    v-model="row.subjectType"
                    class="w-full"
                    @change="handleSubjectTypeChange(row)"
                  >
                    <el-option
                      v-for="item in ACCESS_SUBJECT_TYPE_OPTIONS"
                      :key="String(item.value)"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </template>
              </el-table-column>

              <el-table-column
                v-if="isDirectoryAuthorizationColumnVisible('subject')"
                label="授权对象"
                prop="subject"
                :width="getDirectoryAuthorizationColumnWidthString('subject')"
                :min-width="getDirectoryAuthorizationColumnMinWidthString('subject', 260)"
                v-bind="sortColumnAttrs('subject')"
              >
                <template #default="{ row }">
                  <el-select
                    v-model="row.subjectId"
                    class="w-full"
                    clearable
                    filterable
                    placeholder="请选择授权对象"
                  >
                    <el-option
                      v-for="item in getSubjectOptions(row.subjectType)"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </template>
              </el-table-column>

              <el-table-column
                v-if="isDirectoryAuthorizationColumnVisible('permissionSummary')"
                label="权限摘要"
                prop="permissionSummary"
                :width="getDirectoryAuthorizationColumnWidthString('permissionSummary')"
                :min-width="getDirectoryAuthorizationColumnMinWidthString('permissionSummary', 320)"
                v-bind="sortColumnAttrs('permissionSummary')"
              >
                <template #default="{ row }">
                  <div
                    class="access-rule-permission-summary access-rule-permission-summary--spread"
                    data-testid="dcc-access-rule-permission-summary"
                  >
                    <div class="access-rule-permission-summary__status">
                      <el-switch v-model="row.active" aria-label="启用状态" />
                      <el-tag :type="row.active ? 'success' : 'info'" size="small">
                        {{ row.active ? '已启用' : '已停用' }}
                      </el-tag>
                    </div>
                    <div class="access-rule-permission-summary__toggles">
                      <span class="access-rule-permission-summary__toggle">
                        <span class="access-rule-permission-summary__label">查看</span>
                        <el-switch
                          v-model="row.canQuery"
                          aria-label="查看权限"
                          @change="handleQueryPermissionChange(row)"
                        />
                      </span>
                      <span class="access-rule-permission-summary__toggle">
                        <span class="access-rule-permission-summary__label">下载</span>
                        <el-switch v-model="row.canDownload" aria-label="下载权限" />
                      </span>
                    </div>
                  </div>
                </template>
              </el-table-column>

              <el-table-column
                v-if="isDirectoryAuthorizationColumnVisible('actions')"
                label="操作"
                prop="actions"
                align="center"
                fixed="right"
                :width="getDirectoryAuthorizationColumnWidthString('actions', 88)"
              >
                <template #default="{ row }">
                  <el-button link type="danger" @click="removeRule(resolveRuleIndex(row))">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </UnifiedListTemplate>
      </ContentWrap>
    </el-col>
  </el-row>
</template>

<script lang="ts" setup>
import { defaultProps } from '@/utils/tree'
import {
  deleteDirectoryAccessRules,
  getAccessRuleDirectories,
  getDirectoryAccessRules,
  getDirectoryTree,
  saveDirectoryAccessRules,
  type ControlledFileDirectoryAccessRuleDirectoryVO,
  type ControlledFileDirectoryAccessRuleVO,
  type ControlledFileDirectoryVO
} from '@/api/dcc/controlledFile/directories'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { getSimpleRoleList, type RoleVO } from '@/api/system/role'
import { getSimplePostList, type PostVO } from '@/api/system/post'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import { parsePositiveRouteQueryId, sameRouteQueryId } from '@/utils/routeQueryId'
import { ACCESS_SUBJECT_TYPE_OPTIONS } from '../shared/options'
import { flattenTree } from '../shared/utils'

defineOptions({ name: 'DccControlledFileDirectoryAuthorizationTabPanel' })

interface SubjectOption {
  label: string
  value: number
}

const props = withDefaults(
  defineProps<{
    initialDirectoryId?: string
    active?: boolean
    categoryRevision?: number
  }>(),
  {
    initialDirectoryId: undefined,
    active: true,
    categoryRevision: 0
  }
)

const DEFAULT_DRAFT_RULE_DEPT_NAME = 'QA'

const message = useMessage()
const loading = ref(false)
const saveLoading = ref(false)
const directories = ref<ControlledFileDirectoryVO[]>([])
const boundDirectories = ref<ControlledFileDirectoryAccessRuleDirectoryVO[]>([])
const rules = ref<ControlledFileDirectoryAccessRuleVO[]>([])
const directoryKeyword = ref('')
const selectedDirectoryId = ref<number>()
const draftSelectedDirectoryId = ref<number>()
const showDirectoryPicker = ref(false)
const directoryAuthorizationQueryParams = reactive<{
  subjectKeyword?: string
  subjectType?: string
  active?: boolean
  pageNo: number
  pageSize: number
  quickFilter?: TableQuickFilterValue
}>({
  subjectKeyword: '',
  subjectType: undefined,
  active: undefined,
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined
})

const users = ref<UserVO[]>([])
const depts = ref<DeptVO[]>([])
const roles = ref<RoleVO[]>([])
const posts = ref<PostVO[]>([])

const directoryAuthorizationQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'subjectKeyword',
    label: '授权对象',
    type: 'text',
    queryParamKey: 'subjectKeyword',
    placeholder: '请输入授权对象'
  },
  {
    key: 'subjectType',
    label: '主体类型',
    type: 'select',
    queryParamKey: 'subjectType',
    options: ACCESS_SUBJECT_TYPE_OPTIONS.map((item) => ({
      label: item.label,
      value: String(item.value)
    }))
  },
  {
    key: 'active',
    label: '启用状态',
    type: 'select',
    queryParamKey: 'active',
    options: [
      { label: '已启用', value: true },
      { label: '已停用', value: false }
    ]
  }
]

const directoryAuthorizationDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'subjectType', label: '主体类型', width: 150, hideable: false },
  { key: 'subject', label: '授权对象', minWidth: 260 },
  { key: 'permissionSummary', label: '权限摘要', minWidth: 320 },
  { key: 'actions', label: '操作', width: 88, hideable: false }
]

const {
  columns: directoryAuthorizationColumns,
  saving: directoryAuthorizationColumnSaving,
  isColumnVisible: isDirectoryAuthorizationColumnVisible,
  getColumnWidthString: getDirectoryAuthorizationColumnWidthString,
  getColumnMinWidthString: getDirectoryAuthorizationColumnMinWidthString,
  handleHeaderDragend: handleDirectoryAuthorizationHeaderDragend,
  saveConfig: saveDirectoryAuthorizationColumnConfig
} = useUserTableColumns(
  'dcc.controlledFile.permission.directoryAuthorization',
  directoryAuthorizationDefaultColumns
)

function refreshDirectoryAuthorizationFilters() {
  directoryAuthorizationQueryParams.pageNo = 1
}

const directoryAuthorizationQuickFilter = useTableQuickFilter(
  'dcc.controlledFile.permission.directoryAuthorization',
  directoryAuthorizationQuickFilterDefinitions,
  directoryAuthorizationQueryParams,
  refreshDirectoryAuthorizationFilters
)

const flatDirectories = computed(() => flattenTree(directories.value))
const boundDirectoryMap = computed(
  () => new Map(boundDirectories.value.map((item) => [parsePositiveRouteQueryId(item.id), item]))
)
const isDraftDirectory = computed(
  () =>
    !!selectedDirectoryId.value &&
    draftSelectedDirectoryId.value === selectedDirectoryId.value &&
    !boundDirectoryMap.value.has(parsePositiveRouteQueryId(selectedDirectoryId.value))
)
const filteredBoundDirectories = computed(() => {
  const keyword = directoryKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return boundDirectories.value
  }
  return boundDirectories.value.filter((directory) => {
    return (
      directory.directoryPath.toLowerCase().includes(keyword) ||
      directory.name.toLowerCase().includes(keyword)
    )
  })
})

const getUserSubjectLabel = (item: UserVO) => {
  return item.deptName ? `${item.nickname}（${item.deptName}）` : item.nickname
}

const loadDirectories = async () => {
  directories.value = await getDirectoryTree()
}

const loadBoundDirectories = async () => {
  boundDirectories.value = await getAccessRuleDirectories()
}

const loadSubjectData = async () => {
  const [userList, deptList, roleList, postList] = await Promise.all([
    getSimpleUserList(),
    getSimpleDeptList(),
    getSimpleRoleList(),
    getSimplePostList()
  ])
  users.value = userList
  depts.value = deptList
  roles.value = roleList
  posts.value = postList
}

const mergeRuleReadPermission = (
  rule: ControlledFileDirectoryAccessRuleVO
): ControlledFileDirectoryAccessRuleVO => {
  const mergedReadAllowed = Boolean(rule.canQuery) || Boolean(rule.canPreview)
  return {
    ...rule,
    canQuery: mergedReadAllowed,
    canPreview: mergedReadAllowed
  }
}

const loadRules = async (directoryId?: number) => {
  if (!directoryId) {
    rules.value = []
    return []
  }
  loading.value = true
  try {
    const result = await getDirectoryAccessRules(directoryId)
    rules.value = result.map(mergeRuleReadPermission)
    return result
  } finally {
    loading.value = false
  }
}

const createDefaultDraftRule = (): ControlledFileDirectoryAccessRuleVO | undefined => {
  if (!selectedDirectoryId.value) {
    return undefined
  }
  const defaultDept = depts.value.find((item) => item.name === DEFAULT_DRAFT_RULE_DEPT_NAME)
  if (!defaultDept) {
    message.error(`缺少默认部门 ${DEFAULT_DRAFT_RULE_DEPT_NAME}，无法初始化未保存目录规则`)
    return undefined
  }
  return {
    directoryId: selectedDirectoryId.value,
    subjectType: 'DEPT',
    subjectId: defaultDept.id,
    canQuery: true,
    canPreview: true,
    canDownload: false,
    active: true,
    changeReason: ''
  }
}

const loadDraftRules = async (directoryId: number) => {
  selectedDirectoryId.value = directoryId
  const defaultRule = createDefaultDraftRule()
  rules.value = defaultRule ? [defaultRule] : []
  return rules.value
}

const clearSelection = () => {
  selectedDirectoryId.value = undefined
  draftSelectedDirectoryId.value = undefined
  rules.value = []
}

const selectDirectory = async (directoryId: number, draft: boolean) => {
  directoryAuthorizationQueryParams.pageNo = 1
  selectedDirectoryId.value = directoryId
  draftSelectedDirectoryId.value = draft ? directoryId : undefined
  if (draft) {
    await loadDraftRules(directoryId)
    return
  }
  await loadRules(directoryId)
}

const findDirectoryByIdText = (directoryIdText: string) =>
  flatDirectories.value.find((item) => sameRouteQueryId(item.id, directoryIdText))

const findBoundDirectoryByIdText = (directoryIdText: string) =>
  boundDirectories.value.find((item) => sameRouteQueryId(item.id, directoryIdText))

const initializeSelection = async () => {
  const queryDirectoryId = parsePositiveRouteQueryId(props.initialDirectoryId)
  if (queryDirectoryId) {
    const boundDirectory = findBoundDirectoryByIdText(queryDirectoryId)
    if (boundDirectory) {
      await selectDirectory(boundDirectory.id, false)
      return
    }
    const directory = findDirectoryByIdText(queryDirectoryId)
    if (directory?.id) {
      await selectDirectory(directory.id, true)
      return
    }
  }
  if (boundDirectories.value[0]?.id) {
    await selectDirectory(boundDirectories.value[0].id, false)
    return
  }
  clearSelection()
}

const getSubjectOptions = (subjectType?: string): SubjectOption[] => {
  switch (subjectType) {
    case 'USER':
      return users.value.map((item) => ({ label: getUserSubjectLabel(item), value: item.id }))
    case 'DEPT':
      return depts.value.map((item) => ({ label: item.name, value: item.id }))
    case 'ROLE':
      return roles.value.map((item) => ({ label: item.name, value: item.id }))
    case 'POSITION':
      return posts.value.map((item) => ({ label: item.name, value: Number(item.id) }))
    default:
      return []
  }
}

const getSubjectTypeLabel = (subjectType?: string) => {
  return ACCESS_SUBJECT_TYPE_OPTIONS.find((item) => item.value === subjectType)?.label || ''
}

const getRuleSubjectLabel = (row: ControlledFileDirectoryAccessRuleVO) => {
  const subjectId = Number(row.subjectId)
  return getSubjectOptions(row.subjectType).find((item) => item.value === subjectId)?.label || ''
}

const filteredDirectoryAuthorizationRules = computed(() => {
  const keyword = String(directoryAuthorizationQueryParams.subjectKeyword ?? '').trim().toLowerCase()
  const subjectType = directoryAuthorizationQueryParams.subjectType
  const active = directoryAuthorizationQueryParams.active
  return rules.value.filter((row) => {
    const typeMatch = !subjectType || row.subjectType === subjectType
    const activeMatch = active === undefined || row.active === active
    const subjectText =
      `${getSubjectTypeLabel(row.subjectType)} ${getRuleSubjectLabel(row)}`.toLowerCase()
    const keywordMatch = !keyword || subjectText.includes(keyword)
    return typeMatch && activeMatch && keywordMatch
  })
})

const directoryAuthorizationTotal = computed(() => filteredDirectoryAuthorizationRules.value.length)
const paginatedDirectoryAuthorizationRules = computed(() => {
  const start =
    (directoryAuthorizationQueryParams.pageNo - 1) * directoryAuthorizationQueryParams.pageSize
  return filteredDirectoryAuthorizationRules.value.slice(
    start,
    start + directoryAuthorizationQueryParams.pageSize
  )
})

const handleDirectoryAuthorizationPagination = () => undefined

const resolveRuleIndex = (row: ControlledFileDirectoryAccessRuleVO) => rules.value.indexOf(row)

const handleSubjectTypeChange = (row: ControlledFileDirectoryAccessRuleVO) => {
  row.subjectId = 0
}

const handleQueryPermissionChange = (row: ControlledFileDirectoryAccessRuleVO) => {
  row.canPreview = Boolean(row.canQuery)
}

const findInvalidRuleIndex = () => {
  return rules.value.findIndex((item) => !item.subjectType || !item.subjectId)
}

const buildInvalidRuleMessage = (invalidRuleIndex: number) => {
  if (invalidRuleIndex < 0) {
    return ''
  }
  return `第 ${invalidRuleIndex + 1} 条规则未选择授权对象，请先选择授权对象或删除该规则后再保存`
}

const handleBoundDirectoryClick = async (
  directory: ControlledFileDirectoryAccessRuleDirectoryVO
) => {
  showDirectoryPicker.value = false
  await selectDirectory(directory.id, false)
}

const handleDirectoryPickerChange = async (value?: number | string) => {
  const directoryId = Number(value)
  if (!Number.isFinite(directoryId) || directoryId <= 0) {
    return
  }
  if (boundDirectoryMap.value.has(parsePositiveRouteQueryId(directoryId))) {
    await selectDirectory(directoryId, false)
    showDirectoryPicker.value = false
    return
  }
  await selectDirectory(directoryId, true)
}

const toggleDirectoryPicker = () => {
  showDirectoryPicker.value = !showDirectoryPicker.value
  if (!showDirectoryPicker.value && !isDraftDirectory.value) {
    draftSelectedDirectoryId.value = undefined
  }
}

const addRule = () => {
  if (!selectedDirectoryId.value) {
    message.warning('请先选择目录')
    return
  }
  rules.value.push({
    directoryId: selectedDirectoryId.value,
    subjectType: 'USER',
    subjectId: 0,
    canQuery: true,
    canPreview: true,
    canDownload: false,
    active: true,
    changeReason: ''
  })
  directoryAuthorizationQueryParams.pageNo = Math.max(
    1,
    Math.ceil(
      filteredDirectoryAuthorizationRules.value.length / directoryAuthorizationQueryParams.pageSize
    )
  )
}

const removeRule = (index: number) => {
  if (index < 0) {
    return
  }
  rules.value.splice(index, 1)
  const maxPage = Math.max(
    1,
    Math.ceil(
      filteredDirectoryAuthorizationRules.value.length / directoryAuthorizationQueryParams.pageSize
    )
  )
  directoryAuthorizationQueryParams.pageNo = Math.min(directoryAuthorizationQueryParams.pageNo, maxPage)
}

const deleteBoundDirectory = async (directory: ControlledFileDirectoryAccessRuleDirectoryVO) => {
  try {
    await message.delConfirm(`确认删除目录“${directory.directoryPath}”的全部访问规则吗？`)
  } catch {
    return
  }
  const previousIndex = boundDirectories.value.findIndex((item) => item.id === directory.id)
  await deleteDirectoryAccessRules(directory.id)
  message.success('该目录访问规则已删除')
  await loadBoundDirectories()
  if (selectedDirectoryId.value === directory.id && !isDraftDirectory.value) {
    const nextDirectory =
      boundDirectories.value[previousIndex] || boundDirectories.value[previousIndex - 1]
    if (nextDirectory) {
      await selectDirectory(nextDirectory.id, false)
      return
    }
    clearSelection()
  }
}

const saveRules = async () => {
  if (!selectedDirectoryId.value) {
    message.warning('请先选择目录')
    return
  }
  if (!rules.value.length) {
    message.warning(
      isDraftDirectory.value
        ? '未保存目录至少新增一条规则后再保存'
        : '当前目录没有规则，请使用左侧删除或先新增规则'
    )
    return
  }
  const invalidRuleIndex = findInvalidRuleIndex()
  if (invalidRuleIndex >= 0) {
    message.warning(buildInvalidRuleMessage(invalidRuleIndex))
    return
  }
  saveLoading.value = true
  try {
    await saveDirectoryAccessRules(
      selectedDirectoryId.value,
      rules.value.map((item) => ({
        ...mergeRuleReadPermission(item),
        directoryId: selectedDirectoryId.value as number
      }))
    )
    message.success('访问规则已保存')
    await loadBoundDirectories()
    draftSelectedDirectoryId.value = undefined
    showDirectoryPicker.value = false
    await loadRules(selectedDirectoryId.value)
  } finally {
    saveLoading.value = false
  }
}

watch(
  () => props.initialDirectoryId,
  async (value) => {
    if (!parsePositiveRouteQueryId(value)) {
      return
    }
    if (!directories.value.length) {
      return
    }
    await initializeSelection()
  }
)

const loadDirectoryAuthorizationData = async () => {
  await Promise.all([loadDirectories(), loadBoundDirectories(), loadSubjectData()])
  await initializeSelection()
}

watch(
  () => [props.active, props.categoryRevision] as const,
  async ([active]) => {
    if (!active) {
      return
    }
    await loadDirectoryAuthorizationData()
  }
)

onMounted(async () => {
  if (props.active) {
    await loadDirectoryAuthorizationData()
  }
})
</script>

<style scoped>
.access-rule-bound-directory-list {
  display: grid;
  gap: 8px;
}

.access-rule-bound-directory-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.access-rule-bound-directory-list__item:hover {
  border-color: #bfd2ff;
  background: #fafcff;
}

.access-rule-bound-directory-list__item.is-active {
  border-color: #1677ff;
  background: #f5f9ff;
}

.access-rule-bound-directory-list__content {
  min-width: 0;
  flex: 1;
}

.access-rule-bound-directory-list__path {
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.access-rule-bound-directory-list__delete {
  flex: 0 0 auto;
}

.access-rule-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.access-rule-directory-picker {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.access-rule-directory-picker__hint {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.access-rule-permission-summary {
  min-width: 0;
}

.access-rule-permission-summary--spread {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  width: 100%;
}

.access-rule-permission-summary__status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.access-rule-permission-summary__toggles {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: center;
  column-gap: 18px;
  row-gap: 8px;
  flex: 1;
  min-width: 0;
}

.access-rule-permission-summary__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  min-width: 0;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.access-rule-permission-summary__label {
  min-width: 24px;
  color: #4b5563;
}

@media (max-width: 1366px) {
  .access-rule-permission-summary--spread {
    align-items: flex-start;
    gap: 10px;
  }

  .access-rule-permission-summary__toggles {
    column-gap: 12px;
  }
}
</style>
