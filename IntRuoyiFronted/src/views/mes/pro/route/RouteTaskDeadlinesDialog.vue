<template>
  <Dialog v-model="visible" title="业务处理时限" width="720px">
    <el-alert title="仅配置已有业务约定的时限；未配置或停用表示无约定期限。保存仅影响新派单，原任务期限不变。冻结期间生产执行任务不催办，质量处置任务仍按原约定提醒。" type="info" :closable="false" />
    <el-form label-width="120px" class="mt-4" v-loading="loading" :disabled="saving">
      <el-form-item label="工艺路线">
        <el-select v-model="routeId" @change="load" filterable>
          <el-option v-for="route in routes" :key="route.id" :value="route.id!" :label="`${route.code} ${route.name}`" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务节点">
        <el-select v-model="taskType" @change="fill">
          <el-option v-for="(label, type) in types" :key="type" :value="type" :label="label" />
        </el-select>
      </el-form-item>
      <el-form-item label="约定时限（分钟）"><el-input-number v-model="minutes" :min="1" :precision="0" /></el-form-item>
      <el-form-item label="启用"><el-switch v-model="enabled" /></el-form-item>
      <el-form-item label="时限业务依据"><el-input v-model="reason" type="textarea" :rows="3" maxlength="500" placeholder="填写正式约定、制度或批准依据" /></el-form-item>
      <el-form-item label="当前配置"><span>{{ configured ? (configured.enabled ? '已启用' : '已停用') : '未配置约定期限' }}</span></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="saving" :disabled="loading || failed || !routeId || !taskType || !minutes || !reason.trim()" @click="save">保存</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import request from '@/config/axios'
import { ProRouteApi, type ProRouteVO } from '@/api/mes/pro/route'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
const message = useMessage()
const session = useApprovalCenterInvalidationStore()
const visible = ref(false), loading = ref(false), saving = ref(false), failed = ref(false)
const routes = ref<ProRouteVO[]>([]), routeId = ref<number>(), taskType = ref('')
type Rule = { taskType: string; dueMinutes: number; enabled: boolean; remark: string }
const rules = ref<Rule[]>([]), minutes = ref<number>(), enabled = ref(false), reason = ref('')
const types: Record<string, string> = {
  DEVIATION_RECEIVE: '偏差接收', DEVIATION_CORRECT: '偏差纠正', DEVIATION_SIGN: '偏差执行签署',
  DEVIATION_APPROVE: '偏差质量审核', DEVIATION_CLOSE: '偏差关闭', RECORD_CHANGE_APPROVE: '记录变更审批',
  RECORD_CHANGE_REWORK: '记录变更批准后修改', RECORD_CHANGE_CORRECTION: '记录变更退回补正', DOSSIER_COMPLETE: '资料补齐', PQC_RELEASE_CORRECTION: 'PQC放行补正',
  RELEASE_APPROVE: '上市放行', QA_REVIEW: '不合格QA评审', QA_DECISION_HANDOFF: 'QA处置结果接手'
}
const configured = computed(() => rules.value.find(rule => rule.taskType === taskType.value))
const fill = () => { minutes.value = configured.value?.dueMinutes; enabled.value = configured.value?.enabled ?? false; reason.value = configured.value?.remark ?? '' }
let generation = 0
const load = async () => {
  const current = ++generation, tenant = session.syncSession().key, route = routeId.value
  rules.value = []; fill(); failed.value = true
  if (!route) return
  loading.value = true
  try {
    const result = await request.get({ url: '/mes/pro/edhr-work-task/route-deadline', params: { routeId: route } })
    if (current !== generation || tenant !== session.syncSession().key || !visible.value) return
    rules.value = result; failed.value = false; fill()
  } finally { if (current === generation) loading.value = false }
}
const open = async () => {
  const current = ++generation, tenant = session.syncSession().key
  visible.value = true; routeId.value = undefined; taskType.value = ''; rules.value = []; fill(); failed.value = true
  routes.value = []
  loading.value = true
  try {
    const result = await ProRouteApi.getRouteSimpleList()
    if (current !== generation || tenant !== session.syncSession().key || !visible.value) return
    routes.value = result
  } finally { if (current === generation) loading.value = false }
}
const save = async () => {
  if (saving.value || failed.value || loading.value) return
  saving.value = true
  const tenant = session.syncSession().key
  try {
    await request.post({ url: '/mes/pro/edhr-work-task/route-deadline', data: { routeId: routeId.value, taskType: taskType.value, dueMinutes: minutes.value, enabled: enabled.value, reason: reason.value.trim() } })
    if (tenant !== session.syncSession().key || !visible.value) return
    message.success('业务处理时限已保存，仅用于新派单'); await load()
  } finally { saving.value = false }
}
watch(visible, value => { if (!value) generation++ })
watch(() => session.syncSession().key, () => { visible.value = false })
defineExpose({ open })
</script>
