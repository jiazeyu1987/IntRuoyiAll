<template>
  <div v-bind="attrs" class="w-full" @click="open">
    <el-input
      :model-value="label"
      :placeholder="placeholder"
      :disabled="effectiveDisabled"
      readonly
      @keydown.enter.prevent="open"
      @keydown.space.prevent="open"
    >
      <template #suffix
        ><el-icon
          v-if="clearable && !effectiveDisabled && (ids.length || (allowRelationships && relationships?.length)) && !min"
          class="cursor-pointer"
          aria-label="清空人员"
          @click.stop="clear"
          ><CircleClose /></el-icon
        ><el-icon v-else><Search /></el-icon
      ></template>
    </el-input>
    <div v-if="displayError" class="text-12px text-[var(--el-color-danger)]" role="alert">{{
      displayError
    }}</div>
  </div>
  <UserSelectDialogV2
    ref="dialog"
    v-bind="pickerProps"
    @selected="onSelected"
    @visible-change="onVisibilityChange"
  />
</template>
<script setup lang="ts">
import { useFormDisabled } from 'element-plus'
import { CircleClose, Search } from '@element-plus/icons-vue'
import * as UserApi from '@/api/system/user'
import { useUserStoreWithOut } from '@/store/modules/user'
import UserSelectDialogV2 from './UserSelectDialogV2.vue'
import { normalizeOptions, USER_RELATIONSHIPS, type UserId, type UserOption, type UserPickerProps, type UserRelationship, type UserPickerSelection } from './userPicker'
defineOptions({ name: 'UserSelectV2', inheritAttrs: false })
const props = withDefaults(
  defineProps<
    UserPickerProps & {
      modelValue?: UserId | UserId[]
      defaultCurrentUser?: boolean
      disabled?: boolean
      clearable?: boolean
      placeholder?: string
      hideSelectedLabel?: boolean
    }
  >(),
  { clearable: true, placeholder: '请选择人员', multiple: false }
)
const emit = defineEmits<{
  'update:modelValue': [value: any]
  change: [value: any]
  'visible-change': [visible: boolean]
  'update:relationships': [value: UserRelationship[]]
  'selection-change': [value: UserPickerSelection]
}>()
const effectiveDisabled = useFormDisabled()
const attrs = useAttrs()
const dialog = ref<InstanceType<typeof UserSelectDialogV2>>()
const selected = ref<UserOption[]>([])
const displayError = ref('')
const ids = computed<UserId[]>(() =>
  props.modelValue == null || props.modelValue === ''
    ? []
    : Array.isArray(props.modelValue)
      ? props.modelValue
      : [props.modelValue]
)
const pickerProps = computed(() => ({
  allowRelationships: props.allowRelationships,
  relationships: props.relationships,
  multiple: props.multiple,
  deptId: props.deptId,
  scopeRoleId: props.scopeRoleId,
  scopePostId: props.scopePostId,
  max: props.max,
  disabledIds: props.disabledIds,
  excludeCurrentUser: props.excludeCurrentUser,
  allowInactive: props.allowInactive,
  userOptions: props.userOptions,
  loadUserOptions: props.loadUserOptions,
  optionMapper: props.optionMapper,
  candidateLoading: props.candidateLoading,
  candidateError: props.candidateError,
  departmentOptions: props.departmentOptions,
  roleOptions: props.roleOptions,
  postOptions: props.postOptions,
  min: props.clearable === false ? Math.max(props.min ?? 0, 1) : props.min,
  selectedUsers: [...(props.selectedUsers || []), ...selected.value]
}))
const label = computed(() =>
  props.hideSelectedLabel
    ? ''
    : ids.value
        .map((id) => {
          const row = selected.value.find((item) => String(item.id) === String(id))
          return row?.nickname || row?.username || `人员 ${id}`
        })
        .concat(props.allowRelationships ? (props.relationships || []).map(value => USER_RELATIONSHIPS.find(option => option.value === value)?.label || '未识别关系') : [])
        .join('、')
)
let resolveVersion = 0
const resolve = async () => {
  const version = ++resolveVersion
  displayError.value = ''
  if (!ids.value.length) {
    selected.value = []
    return
  }
  try {
    const businessMode = props.userOptions !== undefined || props.loadUserOptions !== undefined
    let known = [...(props.selectedUsers || []), ...selected.value]
    if (props.userOptions !== undefined)
      known = [...normalizeOptions(props.userOptions, props.optionMapper), ...known]
    const missing = ids.value.filter((id) => !known.some((row) => String(row.id) === String(id)))
    if (missing.length && !businessMode)
      known = [...known, ...(await UserApi.getSelectionList(missing))]
    if (version !== resolveVersion) return
    selected.value = ids.value.flatMap((id) => {
      const row = known.find((item) => String(item.id) === String(id))
      return row ? [{ ...row, id }] : []
    })
    if (selected.value.length !== ids.value.length)
      displayError.value = '部分已选人员信息尚未提供，请打开选择器重新选择'
  } catch (cause) {
    if (version === resolveVersion)
      displayError.value = cause instanceof Error ? cause.message : '已选人员信息加载失败'
  }
}
watch(() => [props.modelValue, props.userOptions, props.selectedUsers], resolve, {
  deep: true,
  immediate: true
})
const onSelected = (rows: UserOption[], _activityId?: unknown, relationships: UserRelationship[] = []) => {
  const rules = props.allowRelationships ? relationships : []
  if (effectiveDisabled.value || (!(rows.length + rules.length) && (props.clearable === false || (props.min ?? 0) > 0))) return
  selected.value = rows
  displayError.value = ''
  emit('update:modelValue', props.multiple ? rows.map((row) => row.id) : rows[0]?.id)
  emit('change', props.multiple ? rows : rows[0])
  emit('update:relationships', rules)
  emit('selection-change', { users: rows, relationships: rules })
}
const onVisibilityChange = (value: boolean) => {
  if (!value) emit('visible-change', false)
}
const clear = () => { if (!effectiveDisabled.value && props.clearable !== false && !props.min) onSelected([]) }
const open = async () => {
  if (effectiveDisabled.value) return
  emit('visible-change', true)
  await nextTick()
  await dialog.value?.open(ids.value, props.disabledIds, undefined, selected.value)
}
onMounted(() => {
  if (!props.defaultCurrentUser || ids.value.length || props.relationships?.length || props.excludeCurrentUser) return
  const id = useUserStoreWithOut().getUser.id
  if (!id || props.disabledIds?.some((value) => String(value) === String(id))) return
  const businessMode = props.userOptions !== undefined || props.loadUserOptions !== undefined
  if (
    businessMode &&
    !props.userOptions?.some(
      (row) => String((props.optionMapper ? props.optionMapper(row) : row).id) === String(id)
    )
  )
    return
  emit('update:modelValue', props.multiple ? [id] : id)
})
defineExpose({ open })
</script>
