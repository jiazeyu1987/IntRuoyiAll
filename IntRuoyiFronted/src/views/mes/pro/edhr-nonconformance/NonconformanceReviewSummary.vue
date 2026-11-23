<template>
  <section class="ncr-summary" aria-label="评审摘要" data-edhr-ncr-summary>
    <h2>评审摘要 · {{ summary.review.reviewCode }}</h2>
    <dl class="ncr-summary__facts">
      <div><dt>评审状态</dt><dd>{{ statusLabel }}</dd></div>
      <div><dt>指定QA责任人</dt><dd>{{ summary.qaReviewerName || '未指派' }}</dd></div>
      <div><dt>实际处置人</dt><dd>{{ summary.qaUserName || '尚未处置' }}</dd></div>
      <div><dt>原周期QA交接任务</dt><dd>{{ summary.qaTaskId || '未生成' }}</dd></div>
      <div><dt>任务状态</dt><dd>{{ summary.qaTaskStatus ? taskLabels[summary.qaTaskStatus] : '未生成' }}</dd></div>
      <div><dt>处置结论</dt><dd>{{ dispositionLabel }}</dd></div>
      <div class="ncr-summary__wide"><dt>不合格原因</dt><dd>{{ summary.review.nonconformanceReason || '未记录' }}</dd></div>
      <div class="ncr-summary__wide"><dt>评审意见</dt><dd>{{ summary.review.reviewOpinion || '尚未填写' }}</dd></div>
    </dl>
    <p>材料、签名和操作过程请查看下方“操作记录”。</p>
  </section>
</template>
<script setup lang="ts">
import type { EdhrNonconformanceReviewSummary } from '@/api/mes/pro/edhr/nonconformanceReview'
defineProps<{ summary: EdhrNonconformanceReviewSummary; statusLabel: string; dispositionLabel: string }>()
const taskLabels = { TODO: '待处理', DONE: '已完成', CANCELED: '已取消（原周期已关闭）' }
</script>
<style scoped>
.ncr-summary { padding: 16px; margin: 12px 0; border: 1px solid var(--el-border-color); border-radius: 4px; }
.ncr-summary h2 { margin: 0 0 12px; font-size: 16px; }
.ncr-summary__facts { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px 20px; margin: 0; }
.ncr-summary dt, .ncr-summary p { color: var(--el-text-color-secondary); font-size: 13px; }
.ncr-summary dd { margin: 4px 0 0; white-space: pre-wrap; overflow-wrap: anywhere; }
.ncr-summary__wide { grid-column: 1 / -1; }
.ncr-summary p { margin: 12px 0 0; }
@media (max-width: 768px) { .ncr-summary__facts { grid-template-columns: minmax(0, 1fr); } }
</style>
