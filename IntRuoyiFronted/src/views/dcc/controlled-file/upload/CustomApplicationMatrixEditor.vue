<template>
  <section v-if="approvalSourceMode === 'APPLICATION_CUSTOM'" aria-label="本次自定义审批配置">
    <p class="mb-12px text-12px text-[var(--el-text-color-secondary)]">
      本次配置仅用于此申请。直接主管由组织关系确定；会签部门负责人签名指派本部门会签人。
    </p>
    <ElFormItem label="本次会签部门" required>
      <ElSelect
        data-testid="dcc-custom-signoff-departments" :model-value="displayedSelection.departments ?? []"
        multiple filterable class="w-full" placeholder="选择本次会签部门，可增加或删除"
        :disabled="editingDisabled" @update:model-value="updateDepartments"
      >
        <ElOption v-for="department in directory.departments" :key="department.id" :value="department.id" :label="department.name" />
        <ElOption v-for="id in unavailableDepartments" :key="`unavailable-${id}`" :value="id" :label="`部门ID ${id}（当前不可用）`" disabled />
      </ElSelect>
    </ElFormItem>
    <ElFormItem label="本次批准人" required>
      <ElSelect
        data-testid="dcc-custom-approver" :model-value="displayedSelection.matrix?.approverUserId ?? null"
        filterable clearable class="w-full" placeholder="选择本次批准人"
        :disabled="editingDisabled" @update:model-value="value => updateAccount('approverUserId', value)"
      >
        <ElOption v-for="user in directory.users" :key="user.id" :value="user.id" :label="accountLabel(user)" />
        <ElOption v-if="unavailableAccount('approverUserId')" :value="displayedSelection.matrix?.approverUserId" :label="`账号ID ${displayedSelection.matrix?.approverUserId}（当前不可用）`" disabled />
      </ElSelect>
    </ElFormItem>
    <ElFormItem label="本次文控审核人" required>
      <ElSelect
        data-testid="dcc-custom-document-control" :model-value="displayedSelection.matrix?.documentControlUserId ?? null"
        filterable clearable class="w-full" placeholder="选择本次文控审核人"
        :disabled="editingDisabled" @update:model-value="value => updateAccount('documentControlUserId', value)"
      >
        <ElOption v-for="user in directory.users" :key="user.id" :value="user.id" :label="accountLabel(user)" />
        <ElOption v-if="unavailableAccount('documentControlUserId')" :value="displayedSelection.matrix?.documentControlUserId" :label="`账号ID ${displayedSelection.matrix?.documentControlUserId}（当前不可用）`" disabled />
      </ElSelect>
    </ElFormItem>
    <p v-if="loading" class="text-12px">正在读取启用部门和账号目录...</p>
    <p v-if="visibleError" role="alert" class="text-12px text-[var(--el-color-danger)]">{{ visibleError }}</p>
  </section>
</template>

<script lang="ts">
export interface ApplicationCustomMatrixValue {
  approverUserId: string | null
  documentControlUserId: string | null
}
export interface CustomMatrixDirectoryRow {
  id: string | number
  name: string
  deptName?: string
  status?: number
}
</script>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElFormItem, ElOption, ElSelect } from 'element-plus'

const props = withDefaults(defineProps<{
  approvalSourceMode: 'PUBLIC_MATRIX' | 'APPLICATION_CUSTOM'
  modelValue: ApplicationCustomMatrixValue | null
  selectedSignoffDepartmentIds: string[] | null
  /** Parent passes the existing enabled directories; an explicit disabled status is never selectable. */
  departments: readonly CustomMatrixDirectoryRow[] | null
  users: readonly CustomMatrixDirectoryRow[] | null
  loading?: boolean
  directoryError?: string
  disabled?: boolean
}>(), { loading: false, directoryError: '', disabled: false })
const emit = defineEmits<{
  'update:modelValue': [value: ApplicationCustomMatrixValue]
  'update:selectedSignoffDepartmentIds': [value: string[]]
}>()

type AccountField = keyof ApplicationCustomMatrixValue
type Option = { id: string; name: string; deptName?: string }
const accountNames: Record<AccountField, string> = {
  approverUserId: '本次批准人', documentControlUserId: '本次文控审核人'
}
const localError = ref('')
const message = (cause: unknown): string => cause instanceof Error ? cause.message : String(cause)
const identity = (value: unknown, label: string): string => {
  if ((typeof value !== 'string' && typeof value !== 'number')
    || (typeof value === 'number' && !Number.isSafeInteger(value))
    || !/^[1-9][0-9]*$/.test(String(value)) || BigInt(value) > 9223372036854775807n)
    throw new Error(`${label}身份无效`)
  return String(value)
}
const readMatrix = (): ApplicationCustomMatrixValue | null => {
  const value = props.modelValue
  if (value === null) return null
  if (!value || typeof value !== 'object' || Array.isArray(value)
    || Object.keys(value).length !== 2 || !Object.hasOwn(value, 'approverUserId') || !Object.hasOwn(value, 'documentControlUserId'))
    throw new Error('本次自定义矩阵实际保存字段不完整或包含未知字段')
  return {
    approverUserId: value.approverUserId === null ? null : identity(value.approverUserId, accountNames.approverUserId),
    documentControlUserId: value.documentControlUserId === null ? null : identity(value.documentControlUserId, accountNames.documentControlUserId)
  }
}
const departmentIds = (value: unknown): string[] | null => {
  if (value === null) return null
  if (!Array.isArray(value)) throw new Error('本次会签部门实际选择缺失')
  const ids = value.map(id => identity(id, '会签部门'))
  if (new Set(ids).size !== ids.length) throw new Error('会签部门不能重复')
  return ids
}
const options = (rows: readonly CustomMatrixDirectoryRow[] | null, label: string): Option[] => {
  if (!Array.isArray(rows)) throw new Error(`${label}目录尚未读取`)
  const seen = new Set<string>(), result: Option[] = []
  for (const row of rows) {
    if (!row || typeof row.name !== 'string' || !row.name.trim()
      || (row.deptName != null && typeof row.deptName !== 'string')
      || (row.status != null && row.status !== 0 && row.status !== 1))
      throw new Error(`${label}目录包含无效记录`)
    const id = identity(row.id, label)
    if (seen.has(id)) throw new Error(`${label}目录身份重复`)
    seen.add(id)
    if (row.status == null || row.status === 0) result.push({ id, name: row.name, deptName: row.deptName })
  }
  return result
}
const directory = computed(() => {
  try {
    return { departments: options(props.departments, '部门'), users: options(props.users, '账号'), error: '' }
  } catch (cause) {
    return { departments: [] as Option[], users: [] as Option[], error: message(cause) }
  }
})
const displayedSelection = computed(() => {
  try {
    return { matrix: readMatrix(), departments: departmentIds(props.selectedSignoffDepartmentIds), error: '' }
  } catch (cause) {
    return { matrix: null, departments: null, error: message(cause) }
  }
})
const editingDisabled = computed(() => props.approvalSourceMode !== 'APPLICATION_CUSTOM' || props.disabled
  || props.loading || Boolean(props.directoryError) || Boolean(directory.value.error) || Boolean(displayedSelection.value.error))
const accountLabel = (row: Option): string => row.deptName ? `${row.name}（${row.deptName}）` : row.name
const requireAvailableAccount = (field: AccountField, id: string): void => {
  if (!directory.value.users.some(row => row.id === id)) throw new Error(`${accountNames[field]}（账号ID ${id}）当前不可用，请核对启用账号目录`)
}
const requireAvailableDepartments = (ids: string[]): void => {
  for (const id of ids) {
    if (!directory.value.departments.some(row => row.id === id)) throw new Error(`本次会签部门（部门ID ${id}）当前不可用，请核对启用部门目录`)
  }
}
const unavailableAccount = (field: AccountField): boolean => {
  const id = displayedSelection.value.matrix?.[field]
  return id != null && !directory.value.users.some(row => row.id === id)
}
const unavailableDepartments = computed(() => (displayedSelection.value.departments ?? [])
  .filter(id => !directory.value.departments.some(row => row.id === id)))
const savedSelectionError = computed(() => {
  try {
    const value = readMatrix(), ids = departmentIds(props.selectedSignoffDepartmentIds)
    if (props.loading || props.directoryError || directory.value.error) return ''
    if (ids) requireAvailableDepartments(ids)
    if (value?.approverUserId) requireAvailableAccount('approverUserId', value.approverUserId)
    if (value?.documentControlUserId) requireAvailableAccount('documentControlUserId', value.documentControlUserId)
    return ''
  } catch (cause) { return message(cause) }
})
const visibleError = computed(() => props.directoryError || displayedSelection.value.error || directory.value.error || savedSelectionError.value || localError.value)

const updateAccount = (field: AccountField, selected: unknown): void => {
  if (editingDisabled.value) return
  try {
    const value = readMatrix() ?? { approverUserId: null, documentControlUserId: null }
    const id = selected == null || selected === '' ? null : identity(selected, accountNames[field])
    if (id !== null) requireAvailableAccount(field, id)
    localError.value = ''
    emit('update:modelValue', { ...value, [field]: id })
  } catch (cause) { localError.value = message(cause) }
}
const updateDepartments = (selected: unknown): void => {
  if (editingDisabled.value) return
  try {
    const ids = departmentIds(selected)
    if (ids === null) throw new Error('会签部门控件未提供实际选择')
    requireAvailableDepartments(ids)
    localError.value = ''
    emit('update:selectedSignoffDepartmentIds', [...ids])
  } catch (cause) { localError.value = message(cause) }
}
/** Local fields/directories only. The caller must still perform formal server readiness before submission. */
const validate = (phase: 'SAVE' | 'SUBMIT'): {
  applicationCustomMatrix: ApplicationCustomMatrixValue | null
  selectedSignoffDepartmentIds: string[] | null
} => {
  try {
    if (props.approvalSourceMode !== 'APPLICATION_CUSTOM') throw new Error('当前文件类型不使用本次自定义矩阵')
    if (phase !== 'SAVE' && phase !== 'SUBMIT') throw new Error('本次自定义矩阵校验阶段无效')
    const value = readMatrix(), ids = departmentIds(props.selectedSignoffDepartmentIds)
    if (phase === 'SUBMIT') {
      if (!ids?.length) throw new Error('请至少选择一个会签部门')
      if (!value?.approverUserId) throw new Error('请选择本次批准人')
      if (!value.documentControlUserId) throw new Error('请选择本次文控审核人')
      if (props.loading) throw new Error('请等待启用部门和账号目录读取完成')
      if (props.directoryError) throw new Error(props.directoryError)
      if (directory.value.error) throw new Error(directory.value.error)
      requireAvailableDepartments(ids)
      requireAvailableAccount('approverUserId', value.approverUserId)
      requireAvailableAccount('documentControlUserId', value.documentControlUserId)
    }
    localError.value = ''
    return { applicationCustomMatrix: value, selectedSignoffDepartmentIds: ids }
  } catch (cause) {
    localError.value = message(cause)
    throw cause
  }
}
watch(() => [props.approvalSourceMode, props.modelValue, props.selectedSignoffDepartmentIds], () => {
  localError.value = ''
}, { deep: true, flush: 'sync' })
defineExpose({ validate })
</script>
