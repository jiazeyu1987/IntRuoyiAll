<template>
  <el-dialog
    v-model="visible"
    title="生产订单详情"
    width="92vw"
    class="order-detail-dialog"
    :close-on-click-modal="!saving"
    :close-on-press-escape="!saving"
    :show-close="!saving"
    append-to-body
  >
    <div v-loading="loading" class="order-detail-content">
      <template v-if="detail">
        <div class="order-hero">
          <div class="order-hero-code">
            <span class="hero-label">生产订单号</span>
            <strong>{{ detail.code || '—' }}</strong>
          </div>
          <div class="order-hero-status">
            <span>业务状态</span>
            <b>{{ detail.businessStatus || '—' }}</b>
          </div>
        </div>

        <div class="order-fields">
          <div class="order-field"
            ><span>订单名称</span><strong>{{ detail.name || '—' }}</strong></div
          >
          <div class="order-field"
            ><span>产品名称</span><strong>{{ detail.productName || '—' }}</strong></div
          >
          <div class="order-field"
            ><span>产品编码</span><strong>{{ detail.productCode || '—' }}</strong></div
          >
          <div class="order-field"
            ><span>ERP生产数量</span><strong>{{ detail.quantity ?? '—' }}</strong></div
          >
          <div class="order-field"
            ><span>生产车间</span><strong>{{ detail.workshopName || '—' }}</strong></div
          >
          <div class="order-field"
            ><span>来源单号</span><strong>{{ detail.orderSourceCode || '—' }}</strong></div
          >
          <div class="order-field order-field-wide"
            ><span>BOM版本</span><strong>{{ detail.bomVersion || '—' }}</strong></div
          >
          <div class="order-field"
            ><span>计划开始时间</span
            ><strong>{{ formatDate(detail.plannedStartTime) || '—' }}</strong></div
          >
          <div class="order-field"
            ><span>计划结束时间</span
            ><strong>{{ formatDate(detail.plannedEndTime) || '—' }}</strong></div
          >
        </div>

        <section class="order-section">
          <h3>规格型号</h3>
          <div class="specification">{{ detail.productSpecification || '—' }}</div>
        </section>
        <div class="order-field order-field-wide single-line"
          ><span>备注</span><strong>{{ detail.remark || '—' }}</strong></div
        >
        <section class="order-section batch-section">
          <h3>批次号</h3>
          <el-input
            v-if="canFill"
            v-model="batchCode"
            class="batch-input"
            aria-label="批次号"
            placeholder="请输入批次号"
            maxlength="64"
            show-word-limit
            :disabled="saving"
          />
          <div v-else class="batch-readonly">{{ detail.batchCode || '未填写' }}</div>
        </section>
      </template>
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
    </div>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">关闭</el-button>
      <el-button
        v-if="canFill"
        type="primary"
        :loading="saving"
        :disabled="loading || !batchCode.trim()"
        @click="save"
        >保存批次号</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import request from '@/config/axios'
import type { ProWorkOrderVO } from '@/api/mes/pro/workorder'
import { checkPermi } from '@/utils/permission'
import { formatDate } from '@/utils/formatTime'

const props = defineProps<{ readonly?: boolean }>()
const emit = defineEmits<{ saved: [] }>()
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const detail = ref<ProWorkOrderVO>()
const activeOrderId = ref<number | string>()
const batchCode = ref('')
const error = ref('')
let visit = 0
const endpoint = '/mes/pro/process-pool/team-leader/active-order/work-order'
const canFill = computed(
  () =>
    !!detail.value &&
    !detail.value.batchCode?.trim() &&
    !props.readonly &&
    checkPermi(['mes:pro-process-pool-team-leader:maintain'])
)
const open = async (id: number | string) => {
  const current = ++visit
  visible.value = true
  loading.value = true
  detail.value = undefined
  batchCode.value = ''
  error.value = ''
  activeOrderId.value = id
  try {
    const result = await request.get<ProWorkOrderVO>({
      url: endpoint,
      params: { activeOrderId: id }
    })
    if (current !== visit) return
    if (!result) throw new Error('未获取到生产订单详情')
    detail.value = result
  } catch (cause) {
    if (current === visit)
      error.value = cause instanceof Error ? cause.message : '订单详情加载失败，请关闭后重试'
  } finally {
    if (current === visit) loading.value = false
  }
}
const save = async () => {
  if (saving.value || !canFill.value || !batchCode.value.trim()) return
  saving.value = true
  error.value = ''
  const value = batchCode.value.trim()
  try {
    await request.put({
      url: `${endpoint}/batch-code`,
      data: { activeOrderId: activeOrderId.value, batchCode: value }
    })
    detail.value!.batchCode = value
    message.success('批次号保存成功')
    emit('saved')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '批次号保存失败，请核实后重试'
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>

<style scoped>
:global(.order-detail-dialog.el-dialog) {
  max-width: 1360px;
  margin-top: 3vh !important;
  border-radius: 14px;
  overflow: hidden;
  padding: 0;
}

:global(.order-detail-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 25px 30px 21px;
  border-bottom: 1px solid #e6edf1;
}

:global(.order-detail-dialog .el-dialog__title) {
  color: #172331;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

:global(.order-detail-dialog .el-dialog__headerbtn) {
  top: 25px;
  right: 26px;
  width: 34px;
  height: 34px;
}
:global(.order-detail-dialog .el-dialog__close) {
  color: #6b7785;
  font-size: 26px;
}
:global(.order-detail-dialog .el-dialog__body) {
  max-height: calc(94vh - 180px);
  overflow-y: auto;
  padding: 25px 28px 12px;
}
:global(.order-detail-dialog .el-dialog__footer) {
  padding: 20px 28px 24px;
  border-top: 1px solid #e6edf1;
}

.order-detail-content {
  color: #1e2936;
}
.order-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 84px;
  padding: 0 30px;
  border-radius: 10px;
  background: linear-gradient(100deg, #e8f7f7 0%, #f0faf9 100%);
  gap: 24px;
}
.order-hero-code,
.order-hero-status {
  display: flex;
  align-items: center;
  gap: 27px;
}
.order-hero-code .hero-label {
  padding-right: 27px;
  border-right: 1px solid #b9d5d8;
  color: #4c5b6c;
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
  white-space: nowrap;
}
.order-hero-code {
  min-width: 0;
}
.order-hero-code strong {
  color: #08777c;
  font-size: 30px;
  letter-spacing: 0.03em;
  overflow-wrap: anywhere;
  min-width: 0;
}
.order-hero-status {
  gap: 18px;
  color: #4c5b6c;
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
  white-space: nowrap;
}
.order-hero-status b {
  display: inline-flex;
  min-width: 53px;
  height: 44px;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #d9dcde;
  color: #303a45;
  font-size: 20px;
}
.order-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 54px;
}
.order-field {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  align-items: center;
  min-height: 59px;
  border-bottom: 1px solid #e3e9ed;
  font-size: 18px;
  padding: 12px 0;
}
.order-field > span,
.order-section h3 {
  color: #5f6d7d;
  font-weight: 700;
}
.order-field > strong {
  overflow-wrap: anywhere;
  font-weight: 500;
}
.order-field-wide {
  grid-column: 1 / -1;
}
.single-line {
  margin-top: 1px;
}
.order-section {
  margin-top: 17px;
}
.order-section h3 {
  margin: 0 0 9px;
  font-size: 18px;
}
.specification {
  min-height: 88px;
  padding: 15px 24px;
  border-radius: 9px;
  background: #f2f7f8;
  color: #273441;
  font-size: 17px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.batch-section {
  margin-top: 19px;
}
.batch-input :deep(.el-input__wrapper) {
  min-height: 54px;
  border: 2px solid #10a5a8;
  border-radius: 9px;
  box-shadow: none;
}
.batch-input :deep(.el-input__inner) {
  font-size: 18px;
}
.batch-readonly {
  min-height: 54px;
  padding: 14px 18px;
  border: 1px solid #dce5e9;
  border-radius: 9px;
  background: #f8fafb;
  color: #293744;
  font-size: 18px;
  overflow-wrap: anywhere;
}
:global(.order-detail-dialog .el-dialog__footer .el-button) {
  min-width: 116px;
  height: 48px;
  border-radius: 8px;
  font-size: 18px;
}
:global(.order-detail-dialog .el-dialog__footer .el-button--primary) {
  min-width: 174px;
  background: #08a5a8;
  border-color: #08a5a8;
}

@media (max-width: 760px) {
  :global(.order-detail-dialog .el-dialog__header) {
    padding: 20px;
  }
  :global(.order-detail-dialog .el-dialog__title) {
    font-size: 23px;
  }
  :global(.order-detail-dialog .el-dialog__body) {
    max-height: calc(94vh - 180px);
    overflow-y: auto;
    padding: 18px 16px 8px;
  }
  .order-hero {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
    padding: 18px;
  }
  .order-hero-code strong {
    font-size: 23px;
  }
  .order-hero-code {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    width: 100%;
  }
  .order-hero-code .hero-label {
    border-right: 0;
    padding-right: 0;
    font-size: 16px;
  }
  .order-hero-status {
    font-size: 16px;
  }
  .order-fields {
    display: block;
  }
  .order-field {
    grid-template-columns: 120px minmax(0, 1fr);
    min-height: 54px;
    font-size: 16px;
  }
  :global(.order-detail-dialog .el-dialog__footer) {
    padding: 15px 16px 18px;
  }
}
</style>
