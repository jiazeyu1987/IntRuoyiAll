<template>
  <el-card class="mb-4" data-qa-handoff-assignment-config>
    <template #header>QA 评审负责人配置</template>
    <el-alert type="info" :closable="false" title="按正式路线配置 QA 评审负责人；已创建的评审保留原责任快照。" class="mb-4" />
    <el-form label-width="110px" @submit.prevent="save">
      <el-form-item label="工艺路线">
        <el-select v-model="routeId" filterable placeholder="选择正式路线">
          <el-option v-for="option in routes" :key="option.id" :label="option.label" :value="option.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="责任来源">
        <el-select v-model="sourceType" @change="changeSource">
          <el-option label="正式用户" value="USER" /><el-option label="正式角色候选" value="ROLE_GROUP" />
        </el-select>
      </el-form-item>
      <el-form-item label="QA 负责人">
        <el-select v-if="sourceType === 'USER'" v-model="sourceId" filterable remote :remote-method="searchUsers" :loading="searching" placeholder="输入负责人姓名查询正式账号">
          <el-option v-for="option in userOptions" :key="option.id" :label="option.label" :value="option.id" />
        </el-select>
        <el-select v-else v-model="sourceId" filterable placeholder="选择具有处置权限的正式角色">
          <el-option v-for="option in roles" :key="option.id" :label="option.label" :value="option.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用"><el-switch v-model="enabled" /></el-form-item>
      <el-form-item label="变更原因"><el-input v-model="reason" type="textarea" maxlength="1000" show-word-limit /></el-form-item>
      <el-form-item>
        <el-button :loading="loading" @click="loadRule">刷新配置</el-button>
        <el-button v-hasPermi="['mes:pro-edhr-work-task-rule:update']" type="primary" :loading="saving" :disabled="!readyToSave" @click="save">保存 QA 负责人</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-alert v-else-if="routeId && !ruleId" title="该路线尚未配置 QA 评审负责人，发起评审将被阻止。" type="warning" :closable="false" />
  </el-card>
</template>
<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useMessage } from '@/hooks/web/useMessage'
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
const loading = ref(false)
const initializing = ref(false)
const searching = ref(false)
const saving = ref(false)
const error = ref('')
let routeEpoch = 0
let searchEpoch = 0
const readyToSave = computed(() => !initializing.value && !loading.value && !saving.value && loadedRouteId.value === routeId.value && !!routeId.value)
const failure = (e: unknown) => { error.value = e instanceof Error ? e.message : String(e) }
const applyRule = (rule: api.QaAssignmentRule | null) => {
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
    const options = await api.qaUserOptions(keyword)
    if (epoch !== searchEpoch || generation !== routeEpoch || route !== routeId.value || sourceType.value !== 'USER') return
    userOptions.value = savedOwner.value && !options.some(o => o.id === savedOwner.value?.id) ? [savedOwner.value, ...options] : options
    error.value = ''
  } catch (e) { if (epoch === searchEpoch && generation === routeEpoch) failure(e) }
  finally { if (epoch === searchEpoch && generation === routeEpoch) searching.value = false }
}
const changeSource = () => { ++searchEpoch; searching.value = false; sourceId.value = undefined; savedOwner.value = undefined; userOptions.value = [] }
const loadRule = async () => {
  const epoch = ++routeEpoch, route = routeId.value
  ++searchEpoch; loadedRouteId.value = undefined; searching.value = false; saving.value = false
  applyRule(null); error.value = ''
  if (!route) { error.value = '请选择正式路线'; loading.value = false; return }
  loading.value = true
  try {
    const rule = await api.qaAssignment(route)
    if (epoch !== routeEpoch || route !== routeId.value) return
    applyRule(rule); loadedRouteId.value = route
  } catch (e) { if (epoch === routeEpoch && route === routeId.value) failure(e) }
  finally { if (epoch === routeEpoch && route === routeId.value) loading.value = false }
}
watch(routeId, loadRule)
const save = async () => {
  if (!readyToSave.value || !sourceId.value || !reason.value.trim()) { error.value = '请先成功读取该路线配置，选择负责人并填写变更原因'; return }
  const route = routeId.value!, epoch = routeEpoch
  const data = { routeId: route, candidateSourceType: sourceType.value, candidateSourceId: sourceId.value, enabled: enabled.value, reason: reason.value.trim(), expectedRuleId: ruleId.value }
  saving.value = true
  try {
    const rule = await api.saveQaAssignment(data)
    if (epoch !== routeEpoch || route !== routeId.value) return
    applyRule(rule); error.value = ''; message.success('QA 负责人已保存')
  } catch (e) { if (epoch === routeEpoch && route === routeId.value) failure(e) }
  finally { if (epoch === routeEpoch && route === routeId.value) saving.value = false }
}
onMounted(async () => {
  initializing.value = true
  try { [routes.value, roles.value] = await Promise.all([api.qaRouteOptions(), api.qaRoleOptions()]) }
  catch (e) { failure(e) }
  finally { initializing.value = false }
})
</script>
