<template>
  <ContentWrap>
    <el-form inline @submit.prevent>
      <el-form-item label="工序">
        <el-select
          v-model="filterProcessId"
          filterable
          clearable
          class="!w-240px"
          placeholder="全部工序"
        >
          <el-option
            v-for="p in processes"
            :key="p.id"
            :label="p.code + ' ' + p.name"
            :value="p.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="工艺"
        ><el-input v-model="keyword" clearable placeholder="工艺编号或名称"
      /></el-form-item>
      <el-form-item>
        <el-button v-hasPermi="['mes:pro-craft:create']" type="primary" @click="openEditor()"
          >新增工艺</el-button
        >
        <el-button
          v-hasPermi="['mes:pro-craft:initialize']"
          :loading="loading"
          @click="previewInitialization"
          >从现有流程初始化</el-button
        >
        <el-button :loading="loading" @click="load">刷新</el-button>
      </el-form-item>
    </el-form>
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
      show-icon
      class="mb-12px"
    />
    <el-table v-loading="loading" :data="filteredCrafts">
      <el-table-column label="工艺编号" prop="code" width="140" />
      <el-table-column label="工艺名称" prop="name" min-width="180" />
      <el-table-column label="所属工序" min-width="180"
        ><template #default="{ row }">{{ processLabel(row.processId) }}</template></el-table-column
      >
      <el-table-column label="参数数量" width="100"
        ><template #default="{ row }">{{
          JSON.parse(row.parametersJson).length
        }}</template></el-table-column
      >
      <el-table-column label="操作" width="150"
        ><template #default="{ row }">
          <el-button link type="primary" @click="openEditor(row, true)">查看</el-button>
          <el-button
            v-hasPermi="['mes:pro-craft:update']"
            link
            type="primary"
            @click="openEditor(row)"
            >修改</el-button
          >
        </template></el-table-column
      >
    </el-table>
  </ContentWrap>
  <ContentWrap title="工艺修改记录">
    <el-table :data="changes">
      <el-table-column label="时间" prop="createTime" :formatter="dateFormatter" min-width="180" />
      <el-table-column label="操作人" prop="creator" width="100" />
      <el-table-column label="操作" width="110"
        ><template #default="{ row }">{{ typeLabel(row.changeType) }}</template></el-table-column
      >
      <el-table-column label="状态" width="160"
        ><template #default="{ row }">{{ statusLabel(row.status) }}</template></el-table-column
      >
      <el-table-column label="原因" prop="reason" min-width="220" />
      <el-table-column label="操作" width="160"
        ><template #default="{ row }">
          <el-button link @click="showHistory(row)">查看变更</el-button>
          <el-button
            v-if="row.status === 'PENDING'"
            v-hasPermi="['mes:pro-craft:update']"
            link
            type="danger"
            :disabled="loading"
            @click="cancelChange(row)"
            >作废整次修改</el-button
          >
        </template></el-table-column
      >
    </el-table>
  </ContentWrap>
  <Dialog
    v-model="editorVisible"
    :title="readOnly ? '查看工艺' : form.id ? '修改工艺' : '新增工艺'"
    width="92%"
  >
    <el-form label-width="90px" :disabled="readOnly || saving">
      <el-form-item label="工艺编号"
        ><el-input :model-value="editorCode || '保存后自动生成 GY- 编号'" disabled
      /></el-form-item>
      <el-form-item label="工艺名称" required
        ><el-input v-model="form.name" maxlength="128"
      /></el-form-item>
      <el-form-item label="所属工序" required>
        <el-select
          v-model="form.processId"
          filterable
          :disabled="!!form.id"
          placeholder="请选择工序"
        >
          <el-option
            v-for="p in processes"
            :key="p.id"
            :label="p.code + ' ' + p.name"
            :value="p.id!"
          />
        </el-select>
      </el-form-item>
      <el-button v-if="!readOnly" class="mb-12px" @click="addParameter">新增参数</el-button>
      <el-table :data="form.parameters" border max-height="440">
        <el-table-column label="参数编码" width="145"
          ><template #default="{ row }"><el-input v-model="row.parameterCode" /></template
        ></el-table-column>
        <el-table-column label="参数名称" width="140"
          ><template #default="{ row }"><el-input v-model="row.parameterName" /></template
        ></el-table-column>
        <el-table-column label="类型" width="130"
          ><template #default="{ row }">
            <el-select v-model="row.valueType" @change="resetTypeFields(row)"
              ><el-option
                v-for="t in parameterTypes"
                :key="t.value"
                :label="t.label"
                :value="t.value"
            /></el-select> </template
        ></el-table-column>
        <el-table-column label="单位" width="90"
          ><template #default="{ row }"><el-input v-model="row.unit" /></template
        ></el-table-column>
        <el-table-column label="标准说明" min-width="170"
          ><template #default="{ row }"
            ><el-input v-model="row.standardText" type="textarea" /></template
        ></el-table-column>
        <el-table-column label="下限" width="120"
          ><template #default="{ row }"
            ><el-input-number
              v-if="numeric(row)"
              v-model="row.lowerLimit"
              controls-position="right"
              class="!w-100px" /></template
        ></el-table-column>
        <el-table-column label="上限" width="120"
          ><template #default="{ row }"
            ><el-input-number
              v-if="numeric(row)"
              v-model="row.upperLimit"
              controls-position="right"
              class="!w-100px" /></template
        ></el-table-column>
        <el-table-column label="默认值" width="140"
          ><template #default="{ row }">
            <el-select v-if="row.valueType === 'BOOLEAN'" v-model="row.defaultValue"
              ><el-option label="是" :value="1" /><el-option label="否" :value="0"
            /></el-select>
            <el-input-number
              v-else-if="numeric(row)"
              v-model="row.defaultValue"
              controls-position="right"
              class="!w-120px"
            />
            <el-input v-else-if="row.valueType === 'SELECT'" v-model="row.defaultText" /> </template
        ></el-table-column>
        <el-table-column label="下拉选项" width="150"
          ><template #default="{ row }">
            <el-input
              v-if="row.valueType === 'SELECT'"
              :model-value="optionsText(row)"
              type="textarea"
              placeholder="每行一个选项"
              @update:model-value="setOptions(row, $event)"
            /> </template
        ></el-table-column>
        <el-table-column label="小数位数" width="120"
          ><template #default="{ row }"
            ><el-input-number
              v-if="row.valueType === 'DECIMAL'"
              v-model="row.decimalScale"
              :min="0"
              :max="6"
              controls-position="right"
              class="!w-100px" /></template
        ></el-table-column>
        <el-table-column label="顺序" width="110"
          ><template #default="{ row }"
            ><el-input-number
              v-model="row.sort"
              :min="0"
              controls-position="right"
              class="!w-90px" /></template
        ></el-table-column>
        <el-table-column v-if="!readOnly" label="操作" width="75"
          ><template #default="{ $index }"
            ><el-button link type="danger" @click="form.parameters.splice($index, 1)"
              >删除</el-button
            ></template
          ></el-table-column
        >
      </el-table>
      <el-form-item v-if="!readOnly" label="修改原因" required class="mt-16px"
        ><el-input v-model="form.reason" type="textarea" maxlength="512"
      /></el-form-item>
    </el-form>
    <el-alert
      v-if="editorError"
      :title="editorError"
      type="error"
      :closable="false"
      class="mt-12px"
    />
    <template #footer>
      <el-button :disabled="saving" @click="discardEditor">{{
        readOnly ? '关闭' : '取消本次修改'
      }}</el-button>
      <el-button v-if="!readOnly" type="primary" :loading="saving" @click="saveOrPreview">{{
        form.id ? '预览全部影响' : '保存工艺'
      }}</el-button>
    </template>
  </Dialog>
  <Dialog
    v-model="impactVisible"
    :title="initializing ? '初始化工艺库' : '确认工艺修改及全部流程升级'"
    width="1000px"
    :close-on-click-modal="false"
  >
    <el-alert
      title="确认后全部关联流程统一升级；已有生产订单继续使用原参数快照。"
      type="info"
      :closable="false"
      show-icon
    />
    <template v-if="impact">
      <el-alert
        v-for="(blocker, index) in impact.blockers"
        :key="index"
        :title="blocker"
        type="error"
        :closable="false"
        class="mt-8px"
      />
      <template v-if="initializing">
        <p
          >可提取 {{ impact.sources?.length || 0 }} 个工序节点；无参数节点
          {{ impact.emptyNodeCount || 0 }} 个。</p
        >
        <el-table :data="impact.sources" max-height="380"
          ><el-table-column label="工艺流程" prop="routeName" /><el-table-column
            label="工序"
            prop="processName" /><el-table-column label="节点编号" prop="routeProcessId"
        /></el-table>
      </template>
      <template v-else>
        <p>工艺名称：{{ impact.beforeName }} → {{ impact.afterName }}</p>
        <el-table :data="parameterDiff" class="mt-12px" max-height="260">
          <el-table-column label="参数" prop="name" /><el-table-column
            label="修改前"
            prop="before"
          /><el-table-column label="修改后" prop="after" />
        </el-table>
        <p>受影响工艺流程：{{ impact.routes?.length || 0 }} 个</p>
        <el-table :data="impact.routes" max-height="260"
          ><el-table-column label="流程编号" prop="routeCode" /><el-table-column
            label="流程名称"
            prop="routeName" /><el-table-column label="当前版本" prop="versionNo"
        /></el-table>
      </template>
    </template>
    <el-alert
      v-if="impactError"
      :title="impactError"
      type="error"
      :closable="false"
      class="mt-12px"
    />
    <template #footer>
      <el-button :disabled="saving" @click="discardImpact">取消本次修改</el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="!impact || !!impact.blockers.length || (initializing && !impact.sources?.length)"
        @click="confirmAll"
        >{{ initializing ? '确认初始化并全部关联' : '确认修改并全部升级' }}</el-button
      >
    </template>
  </Dialog>
  <Dialog v-model="historyVisible" title="工艺变更内容" width="900px">
    <el-descriptions v-if="history" :column="1" border
      ><el-descriptions-item label="状态">{{ statusLabel(history.status) }}</el-descriptions-item
      ><el-descriptions-item label="原因">{{
        history.reason
      }}</el-descriptions-item></el-descriptions
    >
    <template v-if="history?.changeType === 'INITIALIZE'">
      <p>初始化来源</p>
      <el-table :data="historySources" max-height="400"
        ><el-table-column label="工艺流程" prop="routeName" /><el-table-column
          label="工序"
          prop="processName" /><el-table-column label="节点编号" prop="routeProcessId"
      /></el-table>
      <p>新增工艺：{{ historyCraftCodes }}</p>
    </template>
    <template v-else>
      <el-descriptions :column="1" border class="mt-12px"
        ><el-descriptions-item label="工艺编号">{{ historyAfter?.code }}</el-descriptions-item
        ><el-descriptions-item label="名称变化"
          >{{ historyBefore?.name || '无' }} → {{ historyAfter?.name }}</el-descriptions-item
        ></el-descriptions
      >
      <el-table :data="historyDiff" max-height="400"
        ><el-table-column label="参数" prop="name" /><el-table-column
          label="修改前"
          prop="before" /><el-table-column label="修改后" prop="after"
      /></el-table>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import {
  CraftApi,
  type Craft,
  type CraftParameter,
  type CraftSave,
  type CraftImpact,
  type CraftChange
} from '@/api/mes/pro/craft'
import { ProProcessApi, type ProProcessVO } from '@/api/mes/pro/process'
import { dateFormatter } from '@/utils/formatTime'
defineOptions({ name: 'MesProCraft' })
const message = useMessage()
const crafts = ref<Craft[]>([]),
  processes = ref<ProProcessVO[]>([]),
  changes = ref<CraftChange[]>([])
const filterProcessId = ref<number>(),
  keyword = ref(''),
  loading = ref(false),
  saving = ref(false)
const error = ref(''),
  editorError = ref(''),
  impactError = ref('')
const editorVisible = ref(false),
  impactVisible = ref(false),
  readOnly = ref(false),
  initializing = ref(false)
const editorCode = ref(''),
  impact = ref<CraftImpact>(),
  historyVisible = ref(false),
  history = ref<CraftChange>()
const form = reactive<CraftSave>({ name: '', processId: undefined, parameters: [], reason: '' })
const filteredCrafts = computed(() =>
  crafts.value.filter(
    (c) =>
      (!filterProcessId.value || c.processId === filterProcessId.value) &&
      (!keyword.value || (c.code + c.name).includes(keyword.value))
  )
)
const processLabel = (id: number) => {
  const p = processes.value.find((p) => p.id === id)
  return p ? p.code + ' ' + p.name : String(id)
}
const parameterTypes = [
  { value: 'INTEGER', label: '整数' },
  { value: 'DECIMAL', label: '小数' },
  { value: 'BOOLEAN', label: '是否' },
  { value: 'SELECT', label: '下拉选项' },
  { value: 'TEXT_STANDARD', label: '文本标准' }
]
const numeric = (p: CraftParameter) => ['INTEGER', 'DECIMAL'].includes(p.valueType)
const optionsText = (p: CraftParameter) =>
  p.optionValuesJson ? (JSON.parse(p.optionValuesJson) as string[]).join('\n') : ''
const setOptions = (p: CraftParameter, text: string) => {
  p.optionValuesJson = JSON.stringify(
    text
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean)
  )
}
const resetTypeFields = (p: CraftParameter) => {
  p.lowerLimit = null
  p.upperLimit = null
  p.defaultValue = null
  p.defaultText = null
  p.optionValuesJson = null
  p.decimalScale = null
}
const typeLabel = (s: string) =>
  (({ CREATE: '新增', UPDATE: '修改', INITIALIZE: '初始化' }) as Record<string, string>)[s] || s
const statusLabel = (s: string) =>
  (
    ({
      PREPARING: '准备中',
      PENDING: '等待全部流程审批',
      APPLYING: '统一生效中',
      APPLIED: '已生效',
      CANCELLED: '已作废'
    }) as Record<string, string>
  )[s] || s
const explain = (e: unknown) =>
  e instanceof Error
    ? e.message
    : typeof e === 'object' && e && 'msg' in e
      ? String(e.msg)
      : String(e)
const parameterText = (p?: CraftParameter) =>
  p
    ? [
        p.parameterName,
        parameterTypes.find((t) => t.value === p.valueType)?.label,
        p.standardText,
        p.unit ? '单位 ' + p.unit : '',
        p.lowerLimit != null ? '下限 ' + p.lowerLimit : '',
        p.upperLimit != null ? '上限 ' + p.upperLimit : '',
        p.defaultValue != null
          ? '默认 ' + p.defaultValue
          : p.defaultText
            ? '默认 ' + p.defaultText
            : '',
        p.valueType === 'SELECT' ? '选项 ' + optionsText(p) : '',
        p.decimalScale != null ? '小数位 ' + p.decimalScale : '',
        p.sort != null ? '顺序 ' + p.sort : ''
      ]
        .filter(Boolean)
        .join('；')
    : '无'
const diff = (before: CraftParameter[], after: CraftParameter[]) =>
  [...new Set([...before, ...after].map((p) => p.parameterCode))].map((code) => {
    const a = before.find((p) => p.parameterCode === code),
      b = after.find((p) => p.parameterCode === code)
    return {
      name: (b?.parameterName || a?.parameterName || code) + '（' + code + '）',
      before: parameterText(a),
      after: parameterText(b)
    }
  })
const parameterDiff = computed(() => diff(impact.value?.before || [], impact.value?.after || []))
const historyBefore = computed<Craft | undefined>(() =>
  history.value?.changeType !== 'INITIALIZE' && history.value?.beforeJson
    ? JSON.parse(history.value.beforeJson)
    : undefined
)
const historyAfter = computed<Craft | undefined>(() =>
  history.value?.changeType !== 'INITIALIZE' && history.value?.afterJson
    ? JSON.parse(history.value.afterJson)
    : undefined
)
const historyDiff = computed(() =>
  diff(
    historyBefore.value ? JSON.parse(historyBefore.value.parametersJson) : [],
    historyAfter.value ? JSON.parse(historyAfter.value.parametersJson) : []
  )
)
const historySources = computed(() =>
  history.value?.changeType === 'INITIALIZE' && history.value.beforeJson
    ? JSON.parse(history.value.beforeJson)
    : []
)
const historyCraftCodes = computed(() =>
  history.value?.changeType === 'INITIALIZE' && history.value.afterJson
    ? (JSON.parse(history.value.afterJson) as number[])
        .map((id) => crafts.value.find((c) => c.id === id)?.code || '已作废工艺 #' + id)
        .join('、')
    : ''
)
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [a, b, c] = await Promise.all([
      CraftApi.list(),
      ProProcessApi.getProcessSimpleList(),
      CraftApi.changes()
    ])
    crafts.value = a
    processes.value = b
    changes.value = c
  } catch (e) {
    error.value = explain(e)
  } finally {
    loading.value = false
  }
}
function openEditor(craft?: Craft, detail = false) {
  Object.assign(form, {
    id: craft?.id,
    name: craft?.name || '',
    processId: craft?.processId,
    parameters: craft ? JSON.parse(craft.parametersJson) : [],
    expectedFingerprint: craft?.fingerprint,
    impactToken: undefined,
    reason: ''
  })
  editorCode.value = craft?.code || ''
  readOnly.value = detail
  editorError.value = ''
  editorVisible.value = true
}
function addParameter() {
  form.parameters.push({
    parameterCode: '',
    parameterName: '',
    valueType: 'DECIMAL',
    standardText: '',
    sort: form.parameters.length + 1
  })
}
function discardEditor() {
  editorVisible.value = false
  form.parameters = []
}
function discardImpact() {
  impactVisible.value = false
  impact.value = undefined
  if (!initializing.value) discardEditor()
}
async function saveOrPreview() {
  editorError.value = ''
  if (!form.name.trim() || !form.processId || !form.reason.trim() || !form.parameters.length) {
    editorError.value = '请填写工艺名称、所属工序、参数和修改原因'
    return
  }
  saving.value = true
  try {
    if (!form.id) {
      await CraftApi.create(form)
      editorVisible.value = false
      message.success('工艺已新增')
      await load()
    } else {
      impact.value = await CraftApi.preview(form)
      form.impactToken = impact.value.impactToken
      initializing.value = false
      impactError.value = ''
      impactVisible.value = true
    }
  } catch (e) {
    editorError.value = explain(e)
  } finally {
    saving.value = false
  }
}
async function previewInitialization() {
  loading.value = true
  error.value = ''
  try {
    impact.value = await CraftApi.initializationPreview()
    initializing.value = true
    impactError.value = ''
    impactVisible.value = true
  } catch (e) {
    error.value = explain(e)
  } finally {
    loading.value = false
  }
}
async function confirmAll() {
  if (!impact.value) return
  saving.value = true
  impactError.value = ''
  try {
    const result = initializing.value
      ? await CraftApi.initialize(impact.value.impactToken)
      : await CraftApi.updateAll({ ...form, impactToken: impact.value.impactToken })
    impactVisible.value = false
    editorVisible.value = false
    message.success(
      result.status === 'APPLIED'
        ? '工艺及全部关联流程已统一生效'
        : '已提交，全部关联流程审批完成后统一生效'
    )
    await load()
  } catch (e) {
    impactError.value = explain(e)
  } finally {
    saving.value = false
  }
}
async function cancelChange(change: CraftChange) {
  await message.confirm('作废此次工艺修改及全部关联流程升级？原正式配置将保持不变。')
  loading.value = true
  error.value = ''
  try {
    await CraftApi.cancelChange(change.id)
    await load()
  } catch (e) {
    error.value = explain(e)
  } finally {
    loading.value = false
  }
}
function showHistory(change: CraftChange) {
  history.value = change
  historyVisible.value = true
}
onMounted(load)
</script>
<style scoped>
.craft-history {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  max-height: 300px;
  overflow: auto;
  background: var(--el-fill-color-light);
  padding: 12px;
}
</style>
