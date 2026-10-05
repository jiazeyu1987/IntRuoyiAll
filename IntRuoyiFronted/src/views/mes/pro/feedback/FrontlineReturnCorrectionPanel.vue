<template>
  <el-dialog
    v-model="visible"
    title="本人退回待修改"
    width="900px"
    :teleported="false"
    :close-on-click-modal="false"
    :close-on-press-escape="!busy"
    :show-close="!busy"
    @closed="clearSensitive"
  >
    <p v-if="errorText" role="alert" class="return-error">{{ errorText }}</p>
    <section v-if="result" role="status" aria-label="重提结果">
      <p>已签名重提，等待组长复核。原事件：{{ result.eventId }}，新修订：{{ result.revisionId }}</p>
      <el-table :data="result.changes">
        <el-table-column prop="fieldName" label="修改字段" />
        <el-table-column prop="beforeValue" label="修改前" />
        <el-table-column prop="afterValue" label="修改后" />
      </el-table>
    </section>
    <template v-else-if="draft">
      <p>{{ draft.row.workOrderCode }} · {{ draft.row.formName }}</p>
      <p>退回原因：{{ draft.row.rejectionReason }}</p>
      <p>退回轮次：{{ draft.row.rejectedReviewId }}。修改业务值后由本人签名，组长再次复核。</p>
      <fieldset :disabled="busy" class="return-fields">
        <template v-if="draft.production">
          <label
            >完成数量
            <input
              v-model.number="draft.production.outputQuantity"
              type="number"
              min="1"
              step="1"
              aria-label="更正完成数量"
            />
          </label>
          <label v-for="loss in draft.production.lossDetails" :key="loss.reasonId">
            不良项目 {{ loss.reasonId }} 数量
            <input v-model.number="loss.quantity" type="number" min="1" aria-label="更正不良数量" />
          </label>
          <article v-for="material in draft.production.materialDetails" :key="material.materialId">
            <h4>物料 {{ material.materialId }}</h4>
            <label
              >物料完成数量<input v-model.number="material.outputQuantity" type="number" min="0"
            /></label>
            <label
              >物料损耗数量<input v-model.number="material.lossQuantity" type="number" min="0"
            /></label>
            <label v-for="loss in material.lossDetails" :key="loss.reasonId">
              不良项目 {{ loss.reasonId }} 数量<input
                v-model.number="loss.quantity"
                type="number"
                min="1"
              />
            </label>
            <label
              v-for="reading in material.deviceParameterReadings"
              :key="`${reading.deviceId}:${reading.parameterCode}`"
            >
              物料设备 {{ reading.deviceId }} · {{ reading.parameterCode }}
              <input
                v-if="reading.value != null"
                v-model.number="reading.value"
                type="number"
                step="any"
                :aria-label="`物料参数 ${reading.parameterCode}`"
              />
              <input
                v-else
                v-model="reading.textValue"
                :aria-label="`物料参数 ${reading.parameterCode}`"
              />
            </label>
          </article>
          <label
            v-for="reading in draft.production.deviceParameterReadings"
            :key="`${reading.deviceId}:${reading.parameterCode}`"
          >
            设备 {{ reading.deviceId }} · {{ reading.parameterCode }}
            <input
              v-if="reading.value != null"
              v-model.number="reading.value"
              type="number"
              step="any"
              :aria-label="`更正参数 ${reading.parameterCode}`"
            />
            <input
              v-else
              v-model="reading.textValue"
              :aria-label="`更正参数 ${reading.parameterCode}`"
            />
          </label>
        </template>
        <template v-if="draft.pqc">
          <p>正式检验数量：{{ draft.pqc.actualInspectionQuantity }} 件</p>
          <label
            >损耗数量<input
              v-model.number="draft.pqc.scrapQuantity"
              type="number"
              min="0"
              :max="draft.pqc.actualInspectionQuantity"
              step="1"
              aria-label="更正PQC损耗数量"
          /></label>
          <article v-for="item in draft.pqc.items" :key="item.itemCode">
            <h4>{{ item.itemName }}（{{ item.itemCode }}）</h4>
            <p
              >接收标准：{{ item.standardText
              }}<template v-if="item.lowerLimit != null || item.upperLimit != null">
                （{{ item.lowerLimit }} ～ {{ item.upperLimit }} {{ item.unit }}）</template
              ></p
            >
            <p>原检验设备：{{ item.selectedEquipmentNumber }}</p>
            <div class="return-samples">
              <label v-for="(_, index) in item.sampleValues" :key="index">
                第 {{ index + 1 }} 件
                <select
                  v-if="isBoolean(item)"
                  v-model="item.sampleValues[index]"
                  :aria-label="`${item.itemName}第${index + 1}件`"
                >
                  <option value="合格">合格</option
                  ><option value="不合格">不合格</option>
                </select>
                <input
                  v-else
                  v-model="item.sampleValues[index]"
                  :type="item.resultType === 'NUMERIC' ? 'number' : 'text'"
                  step="any"
                  :aria-label="`${item.itemName}第${index + 1}件`"
                />
              </label>
            </div>
          </article>
        </template>
        <label
          >更正原因<textarea v-model="reason" maxlength="500" aria-label="更正原因"></textarea>
        </label>
        <label
          >本人签名密码<input
            v-model="password"
            type="password"
            autocomplete="new-password"
            aria-label="本人签名密码"
        /></label>
      </fieldset>
    </template>
    <el-table
      v-else-if="!errorText"
      v-loading="busy"
      :data="rows"
      empty-text="当前没有属于本人的有效退回待办"
    >
      <el-table-column prop="workOrderCode" label="订单" />
      <el-table-column prop="formName" label="表单" />
      <el-table-column prop="rejectionReason" label="退回原因" />
      <el-table-column prop="rejectedAt" label="退回时间" />
      <el-table-column label="操作" width="130"
        ><template #default="{ row }">
          <el-button :disabled="busy" @click="choose(row)">修改并重提</el-button>
        </template></el-table-column
      >
    </el-table>
    <template #footer>
      <el-button :disabled="busy" @click="close">关闭</el-button>
      <el-button v-if="draft && !result" :disabled="busy" @click="back">返回列表</el-button>
      <el-button
        v-if="draft && !result"
        v-hasPermi="['mes:pro-feedback:create']"
        type="primary"
        :loading="busy"
        @click="resubmit"
        >本人签名重提</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { checkPermi } from '@/utils/permission'
import {
  listOwnReturns,
  getOwnReturnDetail,
  resubmitOwnProduction,
  resubmitOwnPqc,
  type OwnReturnRow,
  type OwnReturnDetail,
  type OwnReturnCorrectionResult,
  type OwnReturnPqcItem
} from '@/api/mes/pro/processpool/frontlineReturnCorrection'
const props = defineProps<{ mode: 'production' | 'pqc' }>()
const emit = defineEmits<{ corrected: [result: OwnReturnCorrectionResult] }>()
const leaderType = computed(() =>
  props.mode === 'pqc' ? ('PQC' as const) : ('PRODUCTION' as const)
)
const visible = ref(false),
  busy = ref(false),
  rows = ref<OwnReturnRow[]>([])
const draft = ref<OwnReturnDetail>(),
  result = ref<OwnReturnCorrectionResult>()
const reason = ref(''),
  password = ref(''),
  errorText = ref('')
let originalValues = ''
const errorMessage = (error: unknown) => (error instanceof Error ? error.message : String(error))
const businessValues = (detail: OwnReturnDetail) => JSON.stringify(detail.production ?? detail.pqc)
const isBoolean = (item: OwnReturnPqcItem) => item.resultType === 'BOOLEAN'
const clearSensitive = () => {
  password.value = ''
}
const close = () => {
  if (!busy.value) {
    password.value = ''
    visible.value = false
  }
}
const back = () => {
  if (!busy.value) {
    draft.value = undefined
    password.value = ''
    reason.value = ''
    errorText.value = ''
  }
}
const open = async () => {
  if (busy.value) return
  if (!checkPermi(['mes:pro-feedback:query'])) {
    errorText.value = '没有读取本人退回待办的权限'
    return
  }
  visible.value = true
  draft.value = undefined
  result.value = undefined
  password.value = ''
  reason.value = ''
  errorText.value = ''
  busy.value = true
  rows.value = []
  try {
    rows.value = await listOwnReturns(leaderType.value)
  } catch (error) {
    errorText.value = errorMessage(error)
  } finally {
    busy.value = false
  }
}
const choose = async (row: OwnReturnRow) => {
  if (busy.value) return
  if (!checkPermi(['mes:pro-feedback:query'])) {
    errorText.value = '没有读取本人退回表单的权限'
    return
  }
  busy.value = true
  draft.value = undefined
  result.value = undefined
  errorText.value = ''
  password.value = ''
  reason.value = ''
  try {
    const detail = await getOwnReturnDetail(row, leaderType.value)
    if (
      !detail?.row ||
      ['returnTaskId', 'eventId', 'activeOrderId', 'rejectedReviewId', 'expectedRevisionId'].some(
        (key) => detail.row[key as keyof OwnReturnRow] !== row[key as keyof OwnReturnRow]
      )
    )
      throw Error('退回轮次已变化，请重新打开待办')
    if (props.mode === 'production' ? !detail.production : !detail.pqc)
      throw Error('退回表单正式字段缺失')
    draft.value = JSON.parse(JSON.stringify(detail))
    originalValues = businessValues(detail)
  } catch (error) {
    errorText.value = errorMessage(error)
  } finally {
    busy.value = false
  }
}
const openTask = async (query: Record<string, unknown>) => {
  if (busy.value) return
  await open()
  if (!visible.value || errorText.value) return
  const queryId = (key: string) =>
    typeof query[key] === 'string' && /^[1-9]\d*$/.test(query[key] as string)
      ? (query[key] as string)
      : ''
  const taskId = queryId('returnTaskId')
  const cycleId = queryId('activeOrderId')
  const eventId = queryId('eventId')
  const reviewId = queryId('rejectedReviewId')
  if (
    !taskId ||
    !cycleId ||
    !eventId ||
    !reviewId ||
    queryId('handoffTaskId') !== taskId ||
    queryId('roundId') !== reviewId ||
    query.handoffType !== `${leaderType.value}_RETURN`
  ) {
    errorText.value = '退回通知上下文无效，请从本人待办重新打开'
    return
  }
  const exactId = (value: unknown) =>
    typeof value === 'string' && /^[1-9]\d*$/.test(value)
      ? value
      : typeof value === 'number' && Number.isSafeInteger(value) && value > 0
        ? String(value)
        : ''
  const matches = rows.value.filter(
    (row) =>
      row.returnTaskId === taskId &&
      exactId(row.activeOrderId) === cycleId &&
      exactId(row.eventId) === eventId &&
      exactId(row.rejectedReviewId) === reviewId
  )
  if (matches.length !== 1) {
    errorText.value = '该退回待办已失效或不属于本人当前周期，请重新查看待办'
    return
  }
  await choose(matches[0])
}
const resubmit = async () => {
  if (busy.value) return
  errorText.value = ''
  if (!checkPermi(['mes:pro-feedback:create'])) {
    errorText.value = '没有本人签名重提的权限'
    password.value = ''
    return
  }
  if (!draft.value) {
    errorText.value = '请先打开本人退回表单'
    password.value = ''
    return
  }
  if (businessValues(draft.value) === originalValues) {
    errorText.value = '请实际修改至少一个业务值'
    password.value = ''
    return
  }
  if (!reason.value.trim() || !password.value) {
    errorText.value = '请填写更正原因和本人签名密码'
    password.value = ''
    return
  }
  busy.value = true
  try {
    const detail = draft.value,
      context = {
        activeOrderId: detail.row.activeOrderId,
        rejectedReviewId: detail.row.rejectedReviewId,
        expectedRevisionId: detail.row.expectedRevisionId
      }
    const signed = {
      eventId: detail.row.eventId,
      changeReason: reason.value.trim(),
      signaturePassword: password.value
    }
    const saved =
      props.mode === 'production'
        ? await resubmitOwnProduction({
            ...context,
            correction: { ...JSON.parse(JSON.stringify(detail.production!)), ...signed }
          })
        : await resubmitOwnPqc({
            ...context,
            correction: {
              ...signed,
              actualInspectionQuantity: detail.pqc!.actualInspectionQuantity,
              scrapQuantity: detail.pqc!.scrapQuantity,
              itemResults: detail.pqc!.items.map((item) => ({
                itemCode: item.itemCode,
                selectedEquipmentId: item.selectedEquipmentId,
                selectedEquipmentNumber: item.selectedEquipmentNumber,
                sampleValues: [...item.sampleValues]
              }))
            }
          })
    if (
      !saved ||
      saved.eventId !== detail.row.eventId ||
      !(saved.revisionId > 0) ||
      !saved.changes?.length
    )
      throw Error('重提结果缺少正式修订或业务差异，请核对后操作')
    result.value = saved
    rows.value = rows.value.filter((row) => row.eventId !== saved.eventId)
    emit('corrected', saved)
  } catch (error) {
    errorText.value = errorMessage(error)
  } finally {
    password.value = ''
    busy.value = false
  }
}
defineExpose({ open, openTask })
</script>

<style scoped>
.return-error {
  color: #b42318;
  white-space: pre-wrap;
}
.return-fields {
  border: 0;
  padding: 0;
  display: grid;
  gap: 16px;
}
.return-fields label {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 8px 0;
}
.return-fields input,
.return-fields select,
.return-fields textarea {
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 8px;
  max-width: 100%;
}
.return-fields textarea {
  width: 65%;
  min-height: 72px;
}
.return-fields article {
  border-top: 1px solid #e2e8f0;
  padding-top: 12px;
}
.return-samples {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.return-samples input {
  width: 100px;
}
@media (max-width: 700px) {
  .return-samples {
    grid-template-columns: 1fr;
  }
}
</style>
