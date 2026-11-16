<template>
  <section class="ncr-workspace" data-edhr-ncr-review-panel>
    <header class="ncr-workspace__header">
      <div>
        <div class="ncr-workspace__eyebrow">不合格评审 · <span data-edhr-ncr-review-id>{{ review.id }}</span></div>
        <h2>{{ review.reviewCode || '未生成评审单号' }}</h2>
      </div>
      <div class="ncr-workspace__header-actions">
        <el-tag :type="pendingReview ? 'warning' : 'info'" effect="light">{{ statusLabel }}</el-tag>
        <el-button :disabled="busy" @click="emit('back')">返回列表</el-button>
      </div>
    </header>

    <div class="ncr-workspace__columns">
      <aside class="ncr-workspace__context">
        <section class="ncr-workspace__card">
          <h3>评审信息</h3>
          <dl class="ncr-workspace__facts">
            <div><dt>来源</dt><dd>{{ sourceLabel }}</dd></div>
            <div><dt>生产工单</dt><dd>{{ review.workOrderCode || '--' }}</dd></div>
            <div><dt>批号</dt><dd>{{ review.batchCode || '--' }}</dd></div>
            <div><dt>冻结时间</dt><dd>{{ frozenAt }}</dd></div>
          </dl>
        </section>
        <section class="ncr-workspace__reason">
          <h3>不合格原因</h3>
          <p>{{ review.nonconformanceReason || '--' }}</p>
        </section>
      </aside>

      <div class="ncr-workspace__body">
        <section v-if="canAssign" class="ncr-workspace__card">
          <h3>指派QA责任人</h3>
          <p class="ncr-workspace__hint">此评审尚未指定QA责任人，请明确指派后办理。</p>
          <div class="ncr-workspace__assignment">
            <QaReviewerSelect :model-value="reviewerId" :disabled="busy" placeholder="请选择QA评审责任人" @update:model-value="emit('update:reviewerId', $event)" />
            <el-button type="primary" :loading="assigningReviewer" :disabled="!reviewerId" @click="emit('assign')">指派QA</el-button>
          </div>
        </section>
        <el-alert v-if="readOnly" title="此交接仅供查看原轮次结果，不能再次处置。" type="info" :closable="false" show-icon />
        <el-alert v-else-if="pendingReview && !canDispose && review.qaReviewerUserId" title="仅本评审已指派的QA责任人可以上传材料并处置。" type="info" :closable="false" show-icon />

        <el-form v-if="editable" label-position="top" :disabled="busy" class="ncr-workspace__form" @submit.prevent>
          <section class="ncr-workspace__card">
            <div class="ncr-workspace__section-heading">
              <h3>评审材料 <span class="ncr-workspace__hint">（选填）</span></h3>
              <span class="ncr-workspace__hint">已上传 {{ materials.length }} / 5 份</span>
            </div>
            <el-form-item :error="errors.materials" data-edhr-ncr-review-material>
              <div class="ncr-workspace__upload">
                <label :for="materialInputId" class="ncr-workspace__upload-label">选择评审材料</label>
                <input
                  :id="materialInputId"
                  type="file"
                  multiple
                  class="ncr-workspace__file-input"
                  data-edhr-ncr-select-material
                  aria-label="选择文件"
                  :aria-describedby="`${materialInputId}-hint`"
                  :aria-invalid="Boolean(errors.materials)"
                  :disabled="busy || materials.length + uploadingCount >= 5"
                  @change="handleFilesChange"
                />
                <p :id="`${materialInputId}-hint`" class="ncr-workspace__hint">可不上传文件；选择后自动上传，最多5份。已选择文件须等待上传完成后提交评审。</p>
                <ul v-if="materials.length || attempts.length" class="ncr-workspace__files" aria-live="polite">
                  <li v-for="material in materials" :key="material.url" data-edhr-ncr-material-success>
                    <Icon icon="ep:document" class="ncr-workspace__file-icon" />
                    <span class="ncr-workspace__file-name">{{ material.fileName }}</span>
                    <span class="ncr-workspace__file-success">已上传</span>
                    <el-button link type="danger" :disabled="busy" :aria-label="`移除 ${material.fileName}`" @click="emit('removeMaterial', material.url)">移除</el-button>
                  </li>
                  <li v-for="attempt in attempts" :key="attempt.key" :data-edhr-ncr-material-state="attempt.status">
                    <Icon :icon="attempt.status === 'uploading' ? 'ep:loading' : 'ep:warning'" class="ncr-workspace__file-icon" />
                    <div class="ncr-workspace__file-name">
                      {{ attempt.fileName }}
                      <p v-if="attempt.error" class="ncr-workspace__file-error" role="alert">{{ attempt.error }}</p>
                    </div>
                    <span :class="{ 'ncr-workspace__file-error': attempt.status === 'failed' }">{{ attempt.status === 'uploading' ? '上传中' : '上传失败' }}</span>
                    <el-button v-if="attempt.status === 'failed'" link :disabled="busy" :aria-label="`移除失败记录 ${attempt.fileName}`" @click="emit('dismissAttempt', attempt.key)">移除</el-button>
                  </li>
                </ul>
              </div>
            </el-form-item>
          </section>

          <section class="ncr-workspace__card">
            <el-form-item label="评审意见" required :error="errors.opinion">
              <el-input :model-value="opinion" type="textarea" :rows="5" resize="vertical" placeholder="请输入评审意见" data-edhr-ncr-review-opinion @update:model-value="emit('update:opinion', $event)" />
            </el-form-item>
          </section>
          <section class="ncr-workspace__card">
            <h3>电子签名</h3>
            <el-form-item label="签名凭据" required :error="errors.signature">
              <SignatureCredentialInput :model-value="signature" :disabled="busy" data-edhr-ncr-signature-password @update:model-value="emit('update:signature', $event)" />
            </el-form-item>
          </section>
        </el-form>

        <template v-else>
          <section v-if="!pendingReview" class="ncr-workspace__card ncr-workspace__result" data-edhr-ncr-disposition-result>
            <el-tag type="success">{{ dispositionLabel }}</el-tag>
            <p>{{ dispositionNote }}</p>
            <dl class="ncr-workspace__facts">
              <div><dt>电子签名</dt><dd data-edhr-ncr-qa-signature>{{ review.qaSignature || '--' }}</dd></div>
              <div><dt>完成时间</dt><dd>{{ closedAt }}</dd></div>
            </dl>
          </section>
          <section class="ncr-workspace__card">
            <h3>评审材料</h3>
            <ul v-if="materials.length" class="ncr-workspace__readonly-files"><li v-for="material in materials" :key="material.url">{{ material.fileName }}</li></ul>
            <p v-else class="ncr-workspace__hint">暂无评审材料</p>
          </section>
          <section class="ncr-workspace__card"><h3>评审意见</h3><p class="ncr-workspace__opinion">{{ review.reviewOpinion || '暂无评审意见' }}</p></section>
        </template>
      </div>
    </div>

    <footer class="ncr-workspace__footer">
      <span class="ncr-workspace__hint" role="status">{{ uploadingCount ? `正在上传 ${uploadingCount} 份材料，请稍候` : editable ? '请核对评审材料与意见，并使用本人凭据签名。' : '当前评审仅供查看。' }}</span>
      <div v-if="editable" class="ncr-workspace__actions">
        <el-button type="primary" :disabled="busy || uploadingCount > 0" :loading="submitting" data-edhr-ncr-concession-release @click="emit('dispose', DISPOSITION_CONCESSION_RELEASE)">让步放行</el-button>
        <el-button type="warning" plain :disabled="busy || uploadingCount > 0" @click="emit('dispose', DISPOSITION_REWORK)">返工</el-button>
        <el-button type="danger" plain :disabled="busy || uploadingCount > 0" @click="emit('dispose', DISPOSITION_VOID)">作废</el-button>
      </div>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SignatureCredentialInput from '@/components/SignatureCredentialInput/SignatureCredentialInput.vue'
import QaReviewerSelect from './QaReviewerSelect.vue'
import { DISPOSITION_CONCESSION_RELEASE, DISPOSITION_REWORK, DISPOSITION_VOID, REVIEW_STATUS_PENDING_REVIEW, type EdhrNonconformanceReviewDisposition, type EdhrNonconformanceReviewRespVO } from '@/api/mes/pro/edhr/nonconformanceReview'
import type { ReviewUploadAttempt, ReviewValidationErrors, ReviewWorkspaceMaterial } from './reviewWorkspace'

const props = defineProps<{
  review: EdhrNonconformanceReviewRespVO
  sourceLabel: string
  statusLabel: string
  frozenAt: string
  closedAt: string
  dispositionLabel: string
  dispositionNote: string
  canDispose: boolean
  readOnly: boolean
  canAssign: boolean
  reviewerId?: string | number
  assigningReviewer: boolean
  submitting: boolean
  materials: ReviewWorkspaceMaterial[]
  attempts: ReviewUploadAttempt[]
  uploadingCount: number
  opinion: string
  signature: string
  errors: ReviewValidationErrors
}>()
const emit = defineEmits<{
  back: []
  assign: []
  dispose: [disposition: EdhrNonconformanceReviewDisposition]
  upload: [files: File[]]
  removeMaterial: [url: string]
  dismissAttempt: [key: number]
  'update:opinion': [value: string]
  'update:signature': [value: string]
  'update:reviewerId': [value: string | number | undefined]
}>()
const pendingReview = computed(() => props.review.reviewStatus === REVIEW_STATUS_PENDING_REVIEW)
const editable = computed(() => props.canDispose && !props.readOnly)
const busy = computed(() => props.submitting || props.assigningReviewer)
const materialInputId = computed(() => `ncr-material-${props.review.id}`)
const handleFilesChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (files.length && editable.value && !busy.value) emit('upload', files)
}
</script>

<style scoped>
.ncr-workspace { display: flex; flex: 1; flex-direction: column; min-height: 0; color: var(--el-text-color-primary); font-size: 14px; line-height: 1.6; }
.ncr-workspace__header { display: flex; flex-shrink: 0; align-items: center; justify-content: space-between; gap: 20px; padding: 4px 0 20px; }
.ncr-workspace__eyebrow { color: var(--el-text-color-secondary); font-size: 12px; margin-bottom: 4px; }
.ncr-workspace h2 { margin: 0; font-size: 20px; font-weight: 600; line-height: 1.5; overflow-wrap: anywhere; }
.ncr-workspace h3 { margin: 0 0 16px; font-size: 16px; font-weight: 600; line-height: 24px; }
.ncr-workspace p { margin: 0; }
.ncr-workspace__header-actions, .ncr-workspace__actions { display: flex; gap: 12px; align-items: center; flex-shrink: 0; }
.ncr-workspace__actions .el-button { margin: 0; min-width: 88px; }
.ncr-workspace__columns { display: grid; flex: 1; min-height: 0; overflow: auto; grid-template-columns: 320px minmax(0, 1fr); align-items: start; gap: 24px; padding: 0 4px 12px 0; scrollbar-gutter: stable; }
.ncr-workspace__context, .ncr-workspace__body, .ncr-workspace__form { display: grid; gap: 20px; min-width: 0; }
.ncr-workspace__card { padding: 20px; border: 1px solid var(--el-border-color-lighter); border-radius: 6px; background: var(--el-bg-color); min-width: 0; }
.ncr-workspace__facts { display: grid; gap: 16px; margin: 0; }
.ncr-workspace__facts dt { color: var(--el-text-color-secondary); font-size: 12px; margin-bottom: 4px; }
.ncr-workspace__facts dd { margin: 0; overflow-wrap: anywhere; font-variant-numeric: tabular-nums; }
.ncr-workspace__reason { padding: 20px; background: var(--el-color-warning-light-9); border-left: 3px solid var(--el-color-warning); border-radius: 4px; }
.ncr-workspace__reason h3 { margin-bottom: 8px; }
.ncr-workspace__reason p, .ncr-workspace__opinion { white-space: pre-wrap; overflow-wrap: anywhere; }
.ncr-workspace__hint { font-size: 12px; color: var(--el-text-color-secondary); }
.ncr-workspace__required, .ncr-workspace__file-error { color: var(--el-color-danger); }
.ncr-workspace__section-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.ncr-workspace__upload { width: 100%; }
.ncr-workspace__upload-label { display: block; margin-bottom: 8px; }
.ncr-workspace__file-input { display: block; width: 100%; max-width: 100%; box-sizing: border-box; padding: 12px; border: 1px dashed var(--el-border-color); border-radius: 4px; color: var(--el-text-color-secondary); background: var(--el-fill-color-blank); font: inherit; cursor: pointer; }
.ncr-workspace__file-input::file-selector-button { border: 1px solid var(--el-color-primary); border-radius: 4px; padding: 7px 14px; margin-right: 12px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); font: inherit; cursor: pointer; }
.ncr-workspace__file-input:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
.ncr-workspace__file-input:disabled { cursor: not-allowed; opacity: .6; }
.ncr-workspace__file-input:disabled::file-selector-button { cursor: not-allowed; }
.ncr-workspace__upload > .ncr-workspace__hint { margin-top: 8px; }
.ncr-workspace__files { list-style: none; padding: 0; margin: 16px 0 0; border-top: 1px solid var(--el-border-color-lighter); }
.ncr-workspace__files li { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.ncr-workspace__file-name { flex: 1; min-width: 0; overflow-wrap: anywhere; }
.ncr-workspace__file-icon { flex-shrink: 0; color: var(--el-text-color-secondary); }
.ncr-workspace__files li > span:not(.ncr-workspace__file-name) { white-space: nowrap; font-size: 12px; }
.ncr-workspace__file-success { color: var(--el-color-success); }
.ncr-workspace__file-name p { font-size: 12px; margin-top: 4px; }
.ncr-workspace__form .el-form-item { margin-bottom: 8px; }
.ncr-workspace__form :deep(.el-form-item__label) { font-size: 14px; }
.ncr-workspace__form :deep(.el-form-item__content) { min-width: 0; }
.ncr-workspace__assignment { display: flex; gap: 12px; margin-top: 12px; }
.ncr-workspace__assignment > :first-child { flex: 1; min-width: 0; }
.ncr-workspace__result > p { margin: 12px 0 20px; }
.ncr-workspace__readonly-files { padding-left: 20px; margin: 0; overflow-wrap: anywhere; }
.ncr-workspace__footer { position: sticky; bottom: 0; z-index: 2; display: flex; flex-shrink: 0; align-items: center; justify-content: space-between; gap: 16px; padding: 16px 0 6px; border-top: 1px solid var(--el-border-color-lighter); background: var(--el-bg-color); }
@media (width <= 1100px) {
  .ncr-workspace__columns { grid-template-columns: 1fr; gap: 20px; }
  .ncr-workspace__facts { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (width <= 600px) {
  .ncr-workspace__header, .ncr-workspace__footer { align-items: flex-start; flex-direction: column; gap: 12px; }
  .ncr-workspace__facts { grid-template-columns: 1fr; }
  .ncr-workspace__card, .ncr-workspace__reason { padding: 16px; }
  .ncr-workspace__actions { flex-wrap: wrap; }
  .ncr-workspace__files li { gap: 8px; }
}
</style>
