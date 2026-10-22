<template>
  <el-card class="mb-4" data-pqc-handoff-assignment-config>
    <template #header>PQC 检验接手人员配置</template>
    <el-alert
      type="info"
      :closable="false"
      title="按正式路线配置检验接手人员，PQC 组长由正式人员关系确定；已创建的任务保留原责任快照。"
      class="mb-4"
    />
    <el-form label-width="110px" :disabled="saving" @submit.prevent="save">
      <el-form-item label="工艺路线">
        <el-select v-model="routeId" filterable :loading="initializing" placeholder="选择正式路线">
          <el-option v-for="option in routes" :key="option.id" :label="option.label" :value="option.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="责任来源">
        <el-select v-model="sourceType" @change="changeSource">
          <el-option label="正式用户" value="USER" />
          <el-option label="正式角色候选" value="ROLE_GROUP" />
        </el-select>
      </el-form-item>
      <el-form-item label="PQC 接手人员">
        <el-select v-if="sourceType === 'USER'" v-model="sourceId" filterable remote :remote-method="searchUsers" :loading="searching" placeholder="输入检验员姓名查询正式账号">
          <el-option v-for="option in userOptions" :key="option.id" :label="option.label" :value="option.id" />
        </el-select>
        <el-select v-else v-model="sourceId" filterable placeholder="选择具有检验提交权限的正式角色">
          <el-option v-for="option in roles" :key="option.id" :label="option.label" :value="option.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用"><el-switch v-model="enabled" /></el-form-item>
      <el-form-item label="变更原因"><el-input v-model="reason" type="textarea" maxlength="1000" show-word-limit /></el-form-item>
      <el-form-item>
        <el-button :loading="loading" :disabled="saving || initializing" @click="loadRule">刷新配置</el-button>
        <el-button v-hasPermi="['mes:pro-edhr-work-task-rule:update']" type="primary" :loading="saving" :disabled="!readyToSave" data-pqc-handoff-assignment-save @click="save">保存 PQC 接手人员</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-alert v-else-if="routeId && loadedRouteId === routeId && !ruleId" title="该路线尚未配置 PQC 检验接手人员，检验接手通知将被阻止。" type="warning" :closable="false" />
    <el-alert v-else-if="routeId && loadedRouteId === routeId && ruleId && !enabled" title="该路线的 PQC 检验接手配置已禁用，检验接手通知将被阻止。" type="warning" :closable="false" />
  </el-card>
</template>
<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useMessage } from '@/hooks/web/useMessage'
import { hasPermission } from '@/directives/permission/hasPermi'
import * as api from '@/api/mes/pro/handoff'
const message = useMessage()
const routes = ref<api.AssignmentOption[]>([])
const roles = ref<api.AssignmentOption[]>([])
const userOptions = ref<api.AssignmentOption[]>([])
const savedOwner = ref<api.AssignmentOption>()
const routeId = ref<number>()
const loadedRouteId = ref<number>()
const sourceType = ref<'USER' | 'ROLE_GROUP'>('USER')
const sourceId = ref<number>()
const enabled = ref(true)
const reason = ref('')
const ruleId = ref<number | null>(null)
const optionsReady = ref(false)
const loading = ref(false)
const initializing = ref(false)
const searching = ref(false)
const saving = ref(false)
const error = ref('')
let initializationError = ''
let routeEpoch = 0
let searchEpoch = 0
const readyToSave = computed(() => optionsReady.value && !initializing.value && !loading.value && !saving.value && loadedRouteId.value === routeId.value && !!routeId.value)
const failure = (e: unknown) => { error.value = e instanceof Error ? e.message : String(e) }
const applyRule = (rule: api.PqcAssignmentRule | null) => {
  ruleId.value = rule?.id ?? null
  sourceType.value = rule?.candidateSourceType ?? 'USER'
  sourceId.value = rule?.candidateSourceId
  enabled.value = rule?.enabled ?? true
  reason.value = ''
  savedOwner.value = rule?.candidateSourceType === 'USER' ? { id: rule.candidateSourceId, label: rule.candidateLabel } : undefined
  userOptions.value = savedOwner.value ? [savedOwner.value] : []
}
const searchUsers = async (keyword: string) => {
  const epoch = ++searchEpoch, route = routeId.value, generation = routeEpoch
  userOptions.value = savedOwner.value ? [savedOwner.value] : []
  if (!keyword.trim()) { searching.value = false; return }
  searching.value = true
  try {
    const options = await api.pqcUserOptions(keyword)
    if (epoch !== searchEpoch || generation !== routeEpoch || route !== routeId.value || sourceType.value !== 'USER') return
    userOptions.value = savedOwner.value && !options.some(o => o.id === savedOwner.value?.id) ? [savedOwner.value, ...options] : options
    error.value = initializationError
  } catch (e) { if (epoch === searchEpoch && generation === routeEpoch) failure(e) }
  finally { if (epoch === searchEpoch && generation === routeEpoch) searching.value = false }
}
const changeSource = () => { ++searchEpoch; searching.value = false; sourceId.value = undefined; savedOwner.value = undefined; userOptions.value = [] }
const loadRule = async () => {
  const epoch = ++routeEpoch, route = routeId.value
  ++searchEpoch; loadedRouteId.value = undefined; searching.value = false
  applyRule(null); error.value = initializationError
  if (!route) { error.value = '请选择正式路线'; loading.value = false; return }
  loading.value = true
  try {
    const rule = await api.pqcAssignment(route)
    if (epoch !== routeEpoch || route !== routeId.value) return
    applyRule(rule); loadedRouteId.value = route
  } catch (e) { if (epoch === routeEpoch && route === routeId.value) failure(e) }
  finally { if (epoch === routeEpoch && route === routeId.value) loading.value = false }
}
watch(routeId, loadRule)
const save = async () => {
  if (saving.value) return
  if (!hasPermission(['mes:pro-edhr-work-task-rule:update'])) { error.value = '没有配置更新权限'; return }
  if (!readyToSave.value || !sourceId.value || !reason.value.trim()) { error.value = '请先成功读取该路线配置，选择检验接手人员并填写变更原因'; return }
  const route = routeId.value!, epoch = routeEpoch
  const data = { routeId: route, candidateSourceType: sourceType.value, candidateSourceId: sourceId.value, enabled: enabled.value, reason: reason.value.trim(), expectedRuleId: ruleId.value }
  saving.value = true
  try {
    const rule = await api.savePqcAssignment(data)
    if (epoch !== routeEpoch || route !== routeId.value) return
    applyRule(rule); error.value = ''; message.success('PQC 接手人员已保存')
  } catch (e) { if (epoch === routeEpoch && route === routeId.value) failure(e) }
  finally { saving.value = false }
}
onMounted(async () => {
  initializing.value = true
  try { [routes.value, roles.value] = await Promise.all([api.pqcRouteOptions(), api.pqcRoleOptions()]); optionsReady.value = true }
  catch (e) { failure(e); initializationError = error.value }
  finally { initializing.value = false }
})
</script>
