<template>
  <div v-hasPermi="['mes:pro-process-pool-team-leader:maintain']" data-dossier-responsibility-assignment>
    <el-button :disabled="disabled" @click="open">指派资料责任人</el-button>
    <el-dialog v-model="visible" title="资料补件责任交接" width="560px" append-to-body>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-descriptions v-if="current" :column="1" class="mb-16px">
        <el-descriptions-item label="当前责任人">{{ current.assigneeUserName }}</el-descriptions-item>
        <el-descriptions-item label="办理状态">{{ statusLabel(current.status) }}</el-descriptions-item>
        <el-descriptions-item label="补件要求">{{ current.reason }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px" v-loading="loading">
        <el-form-item label="资料类别">{{ categoryLabel }}</el-form-item>
        <el-form-item label="责任人" required>
          <UserSelectV2 v-model="assigneeUserId" placeholder="请选择具备资料上传权限的责任人" />
        </el-form-item>
        <el-form-item label="补件要求" required>
          <el-input v-model="reason" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <p>责任人将收到通知和个人待办，上传本类别的新资料后完成。转派将撤销原待办。</p>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="loading || !!error" @click="save">确认交接</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { assignDossierResponsibility, getDossierResponsibilities, type DossierId, type DossierResponsibility } from '@/api/mes/pro/edhr/dossierCompletion'
const props = defineProps<{ activeOrderId: DossierId; categoryKey: string; categoryLabel: string; disabled?: boolean }>()
const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const current = ref<DossierResponsibility>()
const assigneeUserId = ref<DossierId>()
const reason = ref('')
const statusLabels: Record<string, string> = { TODO: '待补件', DOING: '补件中', OVERDUE: '已超期', DONE: '已补齐', CANCELED: '已撤销' }
const statusLabel = (status: string) => statusLabels[status] || status
const open = async () => {
  visible.value = true
  loading.value = true
  error.value = ''
  current.value = undefined
  assigneeUserId.value = undefined
  reason.value = ''
  try {
    const rows = await getDossierResponsibilities(props.activeOrderId)
    current.value = rows.find(row => row.categoryKey === props.categoryKey)
    assigneeUserId.value = current.value?.assigneeUserId
  } catch (e) { error.value = e instanceof Error ? e.message : String(e) }
  finally { loading.value = false }
}
const save = async () => {
  if (!assigneeUserId.value || !reason.value.trim()) { ElMessage.warning('请选择责任人并填写补件要求'); return }
  saving.value = true
  try {
    await assignDossierResponsibility({ activeOrderId: props.activeOrderId, categoryKey: props.categoryKey, assigneeUserId: assigneeUserId.value, reason: reason.value.trim() })
    ElMessage.success('资料责任已交接，已发送通知并生成个人待办')
    visible.value = false
  } catch (e) { ElMessage.error(e instanceof Error ? e.message : String(e)) }
  finally { saving.value = false }
}
</script>
