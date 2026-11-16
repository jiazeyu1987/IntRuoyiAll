<template>
      <Dialog v-model="archiveRuleDialogVisible" :title="ruleDialogTitle" width="560px">
        <el-form
          ref="archiveRuleFormRef"
          v-loading="archiveRuleLoading"
          :model="archiveRuleForm"
          :rules="archiveRuleRules"
          label-width="110px"
        >
          <el-form-item label="工艺路线" prop="routeId">
            <el-select
              v-model="archiveRuleForm.routeId"
              class="!w-100%"
              filterable
              @change="loadArchiveRuleByRoute"
            >
              <el-option
                v-for="routeOption in routeOptions"
                :key="routeOption.id"
                :label="`${routeOption.code} ${routeOption.name}`"
                :value="routeOption.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="ruleAssigneeLabel" prop="assigneeUserId">
            <UserSelectV2
              v-model="archiveRuleForm.assigneeUserId"
              :user-options="userOptions"
              placeholder="请选择人员"
              class="!w-1/1"
            />
          </el-form-item>
          <el-form-item label="实际派发源">
            <div class="edhr-work-task-page__dispatch-source">
              <el-tag :type="isArchiveRuleDispatchAligned ? 'success' : 'warning'">
                {{ resolveArchiveRuleDispatchSourceLabel() }}
              </el-tag>
              <span class="edhr-work-task-page__muted">{{ resolveArchiveRuleDispatchHint() }}</span>
            </div>
          </el-form-item>
          <el-form-item v-if="ruleDialogMode !== 'RELEASE_APPROVE'" label="处理时限（分钟）" prop="dueMinutes">
            <el-input-number
              v-model="archiveRuleForm.dueMinutes"
              class="!w-100%"
              :min="1"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="启用" prop="enabled">
            <el-switch v-model="archiveRuleForm.enabled" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="archiveRuleForm.remark"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button :disabled="archiveRuleSaving" @click="archiveRuleDialogVisible = false"
            >取消</el-button
          >
          <el-button type="primary" :loading="archiveRuleSaving" :disabled="archiveRuleLoading || ruleReadFailed" @click="submitArchiveRule"
            >保存</el-button
          >
        </template>
      </Dialog>
</template>
<script setup lang="ts">
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { computed, reactive, ref, watch, onBeforeUnmount } from 'vue'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
import { ProRouteApi, type ProRouteVO } from '@/api/mes/pro/route'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import type { FormInstance, FormRules } from 'element-plus'
import {
 getEdhrRouteArchiveRule, getEdhrRouteReleaseApprovalRule,
 saveEdhrRouteArchiveRule, saveEdhrRouteReleaseApprovalRule,
 type EdhrWorkTaskArchiveRuleReqVO,
 type EdhrWorkTaskReleaseApprovalRuleReqVO, type EdhrWorkTaskAssignmentRuleRespVO
} from '@/api/mes/pro/edhr/workTask'
const message = useMessage()
const session = useApprovalCenterInvalidationStore()
let ruleReadGeneration = 0
const ruleReadFailed = ref(false)
const ruleContext = () => JSON.stringify([session.syncSession().key, ruleDialogMode.value, archiveRuleForm.routeId, archiveRuleDialogVisible.value])
const archiveRuleDialogVisible = ref(false)
const archiveRuleLoading = ref(false)
const archiveRuleSaving = ref(false)
const ruleDialogMode = ref<'ARCHIVE' | 'RELEASE_APPROVE'>('ARCHIVE')
const archiveRuleFormRef = ref<FormInstance>()
const routeOptions = ref<ProRouteVO[]>([])
const userOptions = ref<UserVO[]>([])
const currentArchiveRule = ref<EdhrWorkTaskAssignmentRuleRespVO | null>(null)
const archiveRuleForm = reactive<
  Partial<
    EdhrWorkTaskArchiveRuleReqVO & EdhrWorkTaskReleaseApprovalRuleReqVO
  >
>({
  routeId: undefined,
  assigneeUserId: undefined,
  dueMinutes: undefined,
  enabled: true,
  remark: ''
})
const archiveRuleRules: FormRules<
  Partial<
    EdhrWorkTaskArchiveRuleReqVO & EdhrWorkTaskReleaseApprovalRuleReqVO
  >
> = {
  routeId: [{ required: true, message: '请选择工艺路线', trigger: 'change' }],
  assigneeUserId: [{ required: true, message: '请选择责任人', trigger: 'change' }],
  dueMinutes: [{ required: true, message: '请输入处理时限', trigger: 'blur' }],
  enabled: [{ required: true, message: '请选择启用状态', trigger: 'change' }]
}

const ruleDialogTitle = computed(() => {
  if (ruleDialogMode.value === 'RELEASE_APPROVE') return '放行规则'
  return '归档规则'
})
const ruleAssigneeLabel = computed(() => {
  if (ruleDialogMode.value === 'RELEASE_APPROVE') return '放行审批责任人'
  return '归档责任人'
})
const ruleActionLabel = computed(() => {
  if (ruleDialogMode.value === 'RELEASE_APPROVE') return '最终放行审批'
  return '最终归档'
})

const isArchiveRuleDispatchAligned = computed(() => {
  if (!archiveRuleForm.assigneeUserId || !currentArchiveRule.value?.candidateSourceId) return true
  return (
    Number(archiveRuleForm.assigneeUserId) === Number(currentArchiveRule.value.candidateSourceId)
  )
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage =
    (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const resetArchiveRuleForm = () => {
  currentArchiveRule.value = null
  archiveRuleForm.routeId = undefined
  archiveRuleForm.assigneeUserId = undefined
  archiveRuleForm.dueMinutes = undefined
  archiveRuleForm.enabled = true
  archiveRuleForm.remark = ''
  archiveRuleFormRef.value?.clearValidate()
}

const fillArchiveRuleForm = (rule: EdhrWorkTaskAssignmentRuleRespVO | null, routeId: number) => {
  currentArchiveRule.value = rule
  archiveRuleForm.routeId = routeId
  archiveRuleForm.assigneeUserId =
    ruleDialogMode.value === 'RELEASE_APPROVE' && rule?.candidateSourceType === 'USER'
      ? rule.candidateSourceId
      : rule?.assigneeUserId
  archiveRuleForm.dueMinutes = rule?.dueMinutes
  archiveRuleForm.enabled = typeof rule?.enabled === 'boolean' ? rule.enabled : true
  archiveRuleForm.remark = rule?.remark || ''
}

const resolveArchiveRuleDispatchSourceLabel = () => {
  const sourceType = currentArchiveRule.value?.candidateSourceType
  const sourceId = currentArchiveRule.value?.candidateSourceId
  if (!sourceType && !sourceId) return '保存后由责任人生成派发源'
  const labels: Record<string, string> = {
    USER: '用户',
    USER_GROUP: '用户组',
    ROLE_GROUP: '角色组',
    DEPT_GROUP: '部门组'
  }
  return `${labels[sourceType || ''] || sourceType || '未知来源'} ${sourceId || '--'}`
}

const resolveArchiveRuleDispatchHint = () => {
  if (!currentArchiveRule.value?.candidateSourceId) {
    return `新规则保存后将写入真实责任源，${ruleActionLabel.value}按该字段校验。`
  }
  return isArchiveRuleDispatchAligned.value
    ? `责任人与实际责任源一致，${ruleActionLabel.value}会由该责任人处理。`
    : '责任人与实际责任源不一致，请保存规则后重新加载确认。'
}

const openArchiveRuleDialog = async () => {
  ruleDialogMode.value = 'ARCHIVE'
  await openRuleDialog()
}

const openReleaseApprovalRuleDialog = async () => {
  ruleDialogMode.value = 'RELEASE_APPROVE'
  await openRuleDialog()
}

const openRuleDialog = async () => {
  ruleReadGeneration++
  ruleReadFailed.value = false
  resetArchiveRuleForm()
  archiveRuleDialogVisible.value = true
  archiveRuleLoading.value = true
  try {
    const [routes, users] = await Promise.all([
      ProRouteApi.getRouteSimpleList(),
      getSimpleUserList()
    ])
    routeOptions.value = routes || []
    userOptions.value = users || []
  } catch (error) {
    archiveRuleDialogVisible.value = false
    message.error(resolveErrorMessage(error, `${ruleDialogTitle.value}基础数据加载失败。`))
  } finally {
    archiveRuleLoading.value = false
  }
}

const loadArchiveRuleByRoute = async () => {
  if (!archiveRuleForm.routeId) return
  const generation = ++ruleReadGeneration
  const key = ruleContext()
  const current = () => generation === ruleReadGeneration && key === ruleContext()
  ruleReadFailed.value = false
  archiveRuleLoading.value = true
  try {
    const rule =
      ruleDialogMode.value === 'RELEASE_APPROVE'
          ? await getEdhrRouteReleaseApprovalRule(archiveRuleForm.routeId)
          : await getEdhrRouteArchiveRule(archiveRuleForm.routeId)
    if (!current()) return
    fillArchiveRuleForm(rule || null, archiveRuleForm.routeId)
  } catch (error) {
    if (!current()) return
    ruleReadFailed.value = true
    currentArchiveRule.value = null
    archiveRuleForm.assigneeUserId = undefined
    archiveRuleForm.dueMinutes = undefined
    archiveRuleForm.remark = ''
    message.error(resolveErrorMessage(error, `${ruleDialogTitle.value}加载失败。`))
  } finally {
    if (current()) archiveRuleLoading.value = false
  }
}

const submitArchiveRule = async () => {
  if (archiveRuleLoading.value || ruleReadFailed.value || archiveRuleSaving.value) return
  await archiveRuleFormRef.value?.validate()
  if (
    !archiveRuleForm.routeId ||
    !archiveRuleForm.assigneeUserId ||
    (ruleDialogMode.value !== 'RELEASE_APPROVE' && !archiveRuleForm.dueMinutes) ||
    typeof archiveRuleForm.enabled !== 'boolean'
  ) {
    throw new Error(`${ruleDialogTitle.value}表单未填写完整。`)
  }
  archiveRuleSaving.value = true
  try {
    const payload: EdhrWorkTaskArchiveRuleReqVO = {
      routeId: archiveRuleForm.routeId,
      assigneeUserId: archiveRuleForm.assigneeUserId,
      dueMinutes: archiveRuleForm.dueMinutes!,
      enabled: archiveRuleForm.enabled,
      remark: archiveRuleForm.remark?.trim() || undefined
    }
    const savedRule =
      ruleDialogMode.value === 'RELEASE_APPROVE'
          ? await saveEdhrRouteReleaseApprovalRule({
              routeId: archiveRuleForm.routeId,
              candidateSourceType: 'USER',
              candidateSourceId: archiveRuleForm.assigneeUserId,
              enabled: archiveRuleForm.enabled,
              remark: archiveRuleForm.remark?.trim() || undefined
            })
          : await saveEdhrRouteArchiveRule(payload)
    fillArchiveRuleForm(savedRule, archiveRuleForm.routeId)
    if (!isArchiveRuleDispatchAligned.value) {
      throw new Error(
        `${ruleDialogTitle.value}已保存，但责任人与实际责任源不一致，请重新检查路线配置。`
      )
    }
    message.success(`${ruleDialogTitle.value}已保存，责任人与实际责任源一致`)
    archiveRuleDialogVisible.value = false
  } catch (error) {
    message.error(resolveErrorMessage(error, `${ruleDialogTitle.value}保存失败。`))
  } finally {
    archiveRuleSaving.value = false
  }
}


watch(archiveRuleDialogVisible, (visible) => { if (!visible) ruleReadGeneration++ })
watch(() => session.syncSession().key, () => { archiveRuleDialogVisible.value = false })
onBeforeUnmount(() => { ruleReadGeneration++ })
defineExpose({ openArchiveRuleDialog, openReleaseApprovalRuleDialog })
</script>
