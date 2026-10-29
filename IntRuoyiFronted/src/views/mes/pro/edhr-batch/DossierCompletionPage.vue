<template>
  <ContentWrap title="资料补件">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div v-loading="loading">
      <el-descriptions v-if="task" :column="1" border>
        <el-descriptions-item label="工单">{{ task.workOrderCode }}</el-descriptions-item>
        <el-descriptions-item label="资料类别">{{ task.categoryLabel }}</el-descriptions-item>
        <el-descriptions-item label="补件要求">{{ task.reason }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ task.status === 'DONE' ? '已补齐' : task.status === 'CANCELED' ? '已撤销' : '待补件' }}</el-descriptions-item>
      </el-descriptions>
      <el-upload v-if="task?.writable" class="mt-16px" :show-file-list="false" :disabled="uploading" :http-request="upload" data-dossier-completion-upload>
        <el-button type="primary" :loading="uploading">上传补件资料</el-button>
      </el-upload>
      <el-alert v-if="task && !task.writable && task.status !== 'DONE'" class="mt-16px" title="当前任务或订单状态不可补件，请联系生产组长核查责任及订单状态。" type="warning" :closable="false" />
      <el-result v-if="task?.status === 'DONE'" icon="success" title="资料已补齐" sub-title="对应资料已保存，个人待办已完成。" />
    </div>
  </ContentWrap>
</template>
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { UploadRequestOptions } from 'element-plus'
import { getDossierCompletionTask, type DossierCompletionTask } from '@/api/mes/pro/edhr/dossierCompletion'
import { uploadActiveOrderDossierFile } from '@/api/mes/pro/processpool/teamLeader'
const route = useRoute()
const task = ref<DossierCompletionTask>()
const loading = ref(false)
const uploading = ref(false)
const error = ref('')
let generation = 0
const load = async () => {
  const token = ++generation
  task.value = undefined
  error.value = ''
  const id = route.query.workTaskId
  if (typeof id !== 'string' || !/^[1-9]\d*$/.test(id)) { error.value = '缺少有效资料待办编号'; return }
  loading.value = true
  try { const result = await getDossierCompletionTask(id); if (token === generation) task.value = result }
  catch (e) { if (token === generation) error.value = e instanceof Error ? e.message : String(e) }
  finally { if (token === generation) loading.value = false }
}
const upload = async (options: UploadRequestOptions) => {
  const selected = task.value
  const token = generation
  if (!selected?.writable) throw new Error('当前资料任务不可上传')
  uploading.value = true
  error.value = ''
  try {
    await uploadActiveOrderDossierFile({ activeOrderId: selected.activeOrderId, applicationId: selected.applicationId, categoryKey: selected.categoryKey, workTaskId: selected.workTaskId, file: options.file })
    options.onSuccess({})
    if (token === generation) await load()
  } catch (e) {
    if (token === generation) error.value = e instanceof Error ? e.message : String(e)
    options.onError(e as Parameters<typeof options.onError>[0])
  } finally { uploading.value = false }
}
onMounted(load)
watch(() => route.query.workTaskId, load)
</script>
