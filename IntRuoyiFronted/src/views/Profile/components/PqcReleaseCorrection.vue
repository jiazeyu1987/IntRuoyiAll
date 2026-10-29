<template>
  <el-card class="mb-16px" shadow="never" data-pqc-release-correction>
    <template #header
      ><div class="correction-header"
        ><strong>放行补正</strong><el-button @click="close">返回个人工作台</el-button></div
      ></template
    >
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
      show-icon
      class="mb-16px"
    />
    <div v-loading="loading">
      <el-descriptions v-if="task" :column="2" border>
        <el-descriptions-item label="工单">{{ task.workOrderCode }}</el-descriptions-item>
        <el-descriptions-item label="批次">{{ task.batchCode }}</el-descriptions-item>
        <el-descriptions-item label="补正责任人">原放行申请人（当前账号）</el-descriptions-item>
        <el-descriptions-item label="状态">{{
          task.taskStatus === 'DONE' ? '已重新提交' : '待补正'
        }}</el-descriptions-item>
        <el-descriptions-item label="补正原因" :span="2">{{
          task.rejectReason
        }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="task?.taskStatus === 'DONE'"
        class="mt-16px"
        type="success"
        :closable="false"
        title="补正已重新提交，等待PQC审查"
        :description="`新申请编号：${task.nextApplicationId}`"
        show-icon
      />
      <template v-if="task && task.taskStatus !== 'DONE'">
        <el-alert
          class="mt-16px"
          type="info"
          :closable="false"
          title="请在原决定后按补正原因完善正式资料，填写补正说明后重新提交PQC审查。"
          show-icon
        />
        <el-tabs class="mt-16px">
          <el-tab-pane v-for="category in categories" :key="category.key" :label="category.label">
            <el-upload
              v-if="task.writable"
              :show-file-list="false"
              :disabled="uploading || submitting"
              :http-request="(options) => upload(category.key, options)"
              data-pqc-correction-upload
            >
              <el-button type="primary" :loading="uploading" :disabled="submitting"
                >上传补正文件</el-button
              >
            </el-upload>
            <el-table :data="category.files" border class="mt-12px" empty-text="暂无正式资料">
              <el-table-column prop="fileName" label="文件名" min-width="200" />
              <el-table-column prop="operatorName" label="上传人" min-width="120" />
              <el-table-column label="上传时间" min-width="180"
                ><template #default="{ row }">{{
                  formatDate(row.operatedAt)
                }}</template></el-table-column
              >
            </el-table>
          </el-tab-pane>
        </el-tabs>
        <el-form class="mt-16px" label-position="top">
          <el-form-item label="补正说明" required
            ><el-input
              v-model="correctionNote"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              :disabled="!task.writable || submitting"
              placeholder="逐项说明已完成的补正及对应文件"
          /></el-form-item>
        </el-form>
        <el-alert
          v-if="!task.writable"
          title="当前账号已不具备该订单申请资格，请联系生产组长明确责任。"
          type="warning"
          :closable="false"
        />
        <el-button
          type="primary"
          :disabled="!canResubmit"
          :loading="submitting"
          @click="resubmit"
          data-pqc-correction-resubmit
          >补正后重新提交</el-button
        >
        <el-button :disabled="uploading || submitting" @click="load">刷新资料与状态</el-button>
      </template>
    </div>
  </el-card>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { UploadRequestOptions } from 'element-plus'
import { formatDate } from '@/utils/formatTime'
import { useProfileWorkbenchTodoBadgeStore } from '@/store/modules/profileWorkbenchTodoBadge'
import type { ActiveOrderDossierFileCategoryVO } from '@/api/mes/pro/processpool/teamLeader'
import {
  getPqcReleaseCorrection,
  getPqcReleaseCorrectionFiles,
  resubmitPqcReleaseCorrection,
  uploadPqcReleaseCorrectionFile,
  type PqcReleaseCorrectionTask
} from '@/api/mes/pro/productionRelease/correction'
const route = useRoute()
const router = useRouter()
const task = ref<PqcReleaseCorrectionTask>()
const categories = ref<ActiveOrderDossierFileCategoryVO[]>([])
const loading = ref(false)
const uploading = ref(false)
const submitting = ref(false)
const correctionNote = ref('')
const error = ref('')
const key = ref('')
let revision = 0
const message = (failure: unknown) => (failure instanceof Error ? failure.message : String(failure))
const canResubmit = computed(
  () =>
    task.value?.writable &&
    task.value.currentDossierRevision > task.value.rejectedDossierRevision &&
    correctionNote.value.trim().length > 0 &&
    !loading.value &&
    !uploading.value &&
    !submitting.value
)
const load = async () => {
  const current = ++revision
  task.value = undefined
  categories.value = []
  error.value = ''
  const id = route.query.releaseCorrectionTaskId
  if (typeof id !== 'string' || !/^[1-9]\d*$/.test(id)) {
    error.value = '缺少有效PQC补正待办编号'
    return
  }
  loading.value = true
  try {
    const detail = await getPqcReleaseCorrection(id)
    const files = await getPqcReleaseCorrectionFiles(id)
    if (current !== revision) return
    task.value = detail
    categories.value = files.categories
  } catch (failure) {
    if (current === revision) error.value = message(failure)
  } finally {
    if (current === revision) loading.value = false
  }
}
const upload = async (category: string, options: UploadRequestOptions) => {
  const selected = task.value
  const current = revision
  if (!selected?.writable || uploading.value || submitting.value)
    throw new Error('当前补正任务不可上传')
  uploading.value = true
  error.value = ''
  try {
    const receipt = await uploadPqcReleaseCorrectionFile(selected, category, options.file)
    options.onSuccess(receipt)
    if (current === revision) await load()
  } catch (failure) {
    if (current === revision) error.value = message(failure)
    options.onError(failure as Parameters<typeof options.onError>[0])
  } finally {
    uploading.value = false
  }
}
const resubmit = async () => {
  const selected = task.value
  const current = revision
  if (!selected || !canResubmit.value) return
  key.value ||= crypto.randomUUID()
  submitting.value = true
  error.value = ''
  try {
    await resubmitPqcReleaseCorrection({
      workTaskId: selected.workTaskId,
      expectedVersion: selected.expectedVersion,
      idempotencyKey: key.value,
      correctionNote: correctionNote.value.trim()
    })
    if (current !== revision) return
    await load()
    await useProfileWorkbenchTodoBadgeStore().refreshTodoTotal()
  } catch (failure) {
    if (current === revision || task.value?.workTaskId === selected.workTaskId)
      error.value = message(failure)
  } finally {
    submitting.value = false
  }
}
const close = () => {
  revision++
  const query = { ...route.query }
  delete query.releaseCorrectionTaskId
  void router.replace({ path: route.path, query })
}
watch(
  () => route.query.releaseCorrectionTaskId,
  () => {
    key.value = ''
    correctionNote.value = ''
    void load()
  },
  { immediate: true }
)
onBeforeUnmount(() => { revision++ })
</script>
<style scoped>
.correction-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
