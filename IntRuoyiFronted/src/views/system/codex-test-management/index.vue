<template>
  <ContentWrap>
    <el-form class="-mb-15px codex-test-toolbar" :inline="true" label-width="90px">
      <el-form-item label="测试租户">
        <el-select v-model="selectedTenantId" class="!w-240px" placeholder="请选择测试租户">
          <el-option
            v-for="tenant in tenantOptions"
            :key="tenant.id"
            :label="tenant.name"
            :value="tenant.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="测试项">
        <el-input
          v-model="queryParams.name"
          class="!w-220px"
          clearable
          placeholder="输入测试项名称"
          @keyup.enter="getCaseList"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" class="!w-150px" clearable placeholder="全部">
          <el-option label="启用" value="ENABLE" />
          <el-option label="禁用" value="DISABLE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :loading="caseLoading" type="primary" @click="getCaseList">
          <Icon class="mr-5px" icon="ep:search" />
          查询
        </el-button>
        <el-button v-hasPermi="['system:codex-test:create']" plain type="primary" @click="openCreate">
          <Icon class="mr-5px" icon="ep:plus" />
          新增测试项
        </el-button>
        <el-button
          v-hasPermi="['system:codex-test:execute']"
          :disabled="selectedCaseIds.length === 0 || !selectedTenantId"
          :loading="executeLoading"
          plain
          type="success"
          @click="startExecution('SEQUENTIAL')"
        >
          顺序执行
        </el-button>
        <el-button
          v-hasPermi="['system:codex-test:execute']"
          :disabled="selectedCaseIds.length === 0 || !selectedTenantId"
          :loading="executeLoading"
          plain
          type="warning"
          @click="startExecution('PARALLEL')"
        >
          并行执行
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table
      v-loading="caseLoading"
      :data="caseTableRows"
      :span-method="caseRowSpanMethod"
      row-key="displayRowKey"
      stripe
      @selection-change="handleCaseSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column label="测试项" min-width="180" prop="name" />
      <el-table-column label="测试方法项" min-width="300">
        <template #default="{ row }">
          <span class="codex-test-item-line">{{ row.displayMethodItem }}</span>
        </template>
      </el-table-column>
      <el-table-column label="测试目标项" min-width="320">
        <template #default="{ row }">
          <span class="codex-test-item-line">{{ row.displayTargetItem }}</span>
        </template>
      </el-table-column>
      <el-table-column label="检查点" prop="checkpointCount" width="90" />
      <el-table-column label="默认方法" prop="defaultExecutionMode" width="110" />
      <el-table-column label="并行安全" width="100">
        <template #default="{ row }">
          <el-tag :type="row.parallelSafe ? 'success' : 'info'" effect="plain">
            {{ row.parallelSafe ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'" effect="plain">
            {{ row.status === 'ENABLE' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="180">
        <template #default="{ row }">
          <el-button
            v-hasPermi="['system:codex-test:update']"
            link
            type="primary"
            @click="openEdit(row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['system:codex-test:delete']"
            link
            type="danger"
            @click="deleteCase(row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="caseTotal"
      @pagination="getCaseList"
    />
  </ContentWrap>

  <ContentWrap>
    <div class="codex-test-section-title">
      <span>执行记录</span>
      <el-button :loading="executionLoading" link type="primary" @click="getExecutionList">刷新</el-button>
    </div>
    <el-table v-loading="executionLoading" :data="executionList" stripe>
      <el-table-column label="批次" prop="id" width="100" />
      <el-table-column label="测试租户" prop="targetTenantId" width="120" />
      <el-table-column label="方法" prop="executionMode" width="110" />
      <el-table-column label="结果" width="120">
        <template #default="{ row }">
          <el-tag :type="executionTagType(row.status)" effect="plain">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" prop="startedAt" min-width="170" />
      <el-table-column label="完成时间" prop="finishedAt" min-width="170" />
      <el-table-column fixed="right" label="操作" width="180">
        <template #default="{ row }">
          <el-button
            v-hasPermi="['system:codex-test:artifact']"
            link
            type="primary"
            @click="openExecution(row.id)"
          >
            查看结果
          </el-button>
          <el-button
            v-if="['PENDING', 'RUNNING'].includes(row.status)"
            v-hasPermi="['system:codex-test:cancel']"
            link
            type="danger"
            @click="cancelExecution(row.id)"
          >
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <el-dialog v-model="caseDialogVisible" :title="caseForm.id ? '修改测试项' : '新增测试项'" width="860px">
    <el-form ref="caseFormRef" :model="caseForm" :rules="caseRules" label-width="120px">
      <el-form-item label="测试项名称" prop="name">
        <el-input v-model="caseForm.name" placeholder="例如：排产手动重排工单校验" />
      </el-form-item>
      <el-form-item label="测试方法项" prop="methodText">
        <el-input
          v-model="caseForm.methodText"
          :rows="5"
          placeholder="按行录入测试方法，例如：a. 打开排产工单页"
          type="textarea"
        />
      </el-form-item>
      <el-form-item label="测试数据">
        <el-input
          v-model="caseForm.testDataText"
          :rows="3"
          placeholder="用户手写数据，例如：来源生产工单号=881MO093613,881MO093615"
          type="textarea"
        />
      </el-form-item>
      <el-form-item label="默认方法">
        <el-radio-group v-model="caseForm.defaultExecutionMode">
          <el-radio-button label="SEQUENTIAL">顺序执行</el-radio-button>
          <el-radio-button label="PARALLEL">并行执行</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="执行控制">
        <el-switch v-model="caseForm.parallelSafe" active-text="允许并行" inactive-text="不允许并行" />
        <el-switch
          v-model="caseForm.status"
          active-text="启用"
          active-value="ENABLE"
          class="ml-24px"
          inactive-text="禁用"
          inactive-value="DISABLE"
        />
      </el-form-item>
      <el-form-item label="测试目标项">
        <div class="codex-test-checkpoints">
          <div
            v-for="(checkpoint, index) in caseForm.checkpoints"
            :key="index"
            class="codex-test-checkpoint"
          >
            <el-input-number v-model="checkpoint.sort" :min="1" controls-position="right" />
            <el-input v-model="checkpoint.name" placeholder="目标项名称" />
            <el-input
              v-model="checkpoint.expectedText"
              placeholder="按行录入测试目标，例如：a. 两个排产工单被筛选出"
              type="textarea"
            />
            <el-button
              :disabled="caseForm.checkpoints.length === 1"
              link
              type="danger"
              @click="removeCheckpoint(index)"
            >
              删除
            </el-button>
          </div>
          <el-button plain type="primary" @click="addCheckpoint">新增目标项</el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="caseDialogVisible = false">取消</el-button>
      <el-button v-hasPermi="['system:codex-test:create', 'system:codex-test:update']" type="primary" @click="saveCase">
        保存
      </el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="executionDrawerVisible" size="70%" title="执行结果">
    <template v-if="executionDetail">
      <el-alert
        :closable="false"
        :title="`批次 ${executionDetail.id}：${statusText(executionDetail.status)}`"
        class="mb-12px"
        show-icon
        :type="executionTagType(executionDetail.status)"
      />
      <el-collapse>
        <el-collapse-item
          v-for="caseResult in executionDetail.cases || []"
          :key="caseResult.id"
          :title="`${caseResult.caseNameSnapshot} - ${statusText(caseResult.status)}`"
        >
          <el-descriptions :column="1" border>
            <el-descriptions-item label="测试方法项">
              {{ caseResult.methodTextSnapshot }}
            </el-descriptions-item>
            <el-descriptions-item label="测试数据">
              {{ caseResult.testDataTextSnapshot || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="失败描述">
              {{ caseResult.failureReason || '-' }}
            </el-descriptions-item>
          </el-descriptions>
          <el-table :data="caseResult.checkpointResults" class="mt-12px" stripe>
            <el-table-column label="检查点" min-width="180" prop="checkpointNameSnapshot" />
            <el-table-column label="期待结果" min-width="220" prop="expectedTextSnapshot" />
            <el-table-column label="实际结果" min-width="220" prop="actualText" />
            <el-table-column label="判定" width="110">
              <template #default="{ row }">
                <el-tag :type="checkpointTagType(row.status)" effect="plain">
                  {{ row.status === 'PASS' ? '绿色勾通过' : row.status === 'FAIL' ? '红色叉失败' : statusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="为什么不同" min-width="220" prop="mismatchDescription" />
            <el-table-column label="失败截图" width="120">
              <template #default="{ row }">
                <el-button
                  v-if="row.screenshotArtifactId"
                  v-hasPermi="['system:codex-test:artifact']"
                  link
                  type="primary"
                  @click="previewArtifact(row.screenshotArtifactId)"
                >
                  查看
                </el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
      <el-image v-if="artifactPreviewUrl" class="codex-test-artifact" :src="artifactPreviewUrl" fit="contain" />
    </template>
  </el-drawer>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import * as CodexTestApi from '@/api/system/codexTestManagement'
import * as TenantApi from '@/api/system/tenant'

defineOptions({ name: 'SystemCodexTestManagement' })

const message = useMessage()

const caseLoading = ref(false)
const executionLoading = ref(false)
const executeLoading = ref(false)
const caseDialogVisible = ref(false)
const executionDrawerVisible = ref(false)
const artifactPreviewUrl = ref('')
const caseFormRef = ref<FormInstance>()
const tenantOptions = ref<TenantApi.TenantVO[]>([])
const selectedTenantId = ref<number>()
const selectedCaseIds = ref<number[]>([])
const caseList = ref<CodexTestApi.CodexTestCaseVO[]>([])
const caseTotal = ref(0)
const executionList = ref<CodexTestApi.CodexTestExecutionVO[]>([])
const executionDetail = ref<CodexTestApi.CodexTestExecutionVO>()

type CodexTestCaseTableRow = CodexTestApi.CodexTestCaseVO & {
  displayRowKey: string
  displayRowIndex: number
  displayRowCount: number
  displayMethodItem: string
  displayTargetItem: string
}

const queryParams = reactive<CodexTestApi.CodexTestCasePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  name: '',
  status: undefined,
  executionMode: undefined
})

const defaultCaseForm = (): CodexTestApi.CodexTestCaseVO => ({
  name: '',
  methodText: '',
  testDataText: '',
  defaultExecutionMode: 'SEQUENTIAL',
  parallelSafe: false,
  status: 'ENABLE',
  sort: 0,
  checkpoints: [newCheckpoint(1)]
})

const caseForm = reactive<CodexTestApi.CodexTestCaseVO>(defaultCaseForm())

const caseRules: FormRules = {
  name: [{ required: true, message: '测试项名称不能为空', trigger: 'blur' }],
  methodText: [{ required: true, message: '测试方法项不能为空', trigger: 'blur' }]
}

function newCheckpoint(sort: number): CodexTestApi.CodexTestCheckpointVO {
  return {
    sort,
    name: `检查点 ${sort}`,
    expectedText: '',
    severity: 'MAJOR'
  }
}

function resetCaseForm() {
  Object.assign(caseForm, defaultCaseForm())
}

function splitDisplayItems(text?: string, fallback?: string) {
  const source = text?.trim() || fallback?.trim() || ''
  if (!source) return ['-']
  return source
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function formatMethodItems(methodText?: string) {
  return splitDisplayItems(methodText)
}

function formatTargetItems(checkpoints?: CodexTestApi.CodexTestCheckpointVO[]) {
  const targetItems = [...(checkpoints || [])]
    .sort((left, right) => (left.sort || 0) - (right.sort || 0))
    .flatMap((checkpoint) => splitDisplayItems(checkpoint.expectedText, checkpoint.name))
  return targetItems.length > 0 ? targetItems : ['-']
}

const caseTableRows = computed<CodexTestCaseTableRow[]>(() =>
  caseList.value.flatMap((testCase, caseIndex) => {
    const methodItems = formatMethodItems(testCase.methodText)
    const targetItems = formatTargetItems(testCase.checkpoints)
    const displayRowCount = Math.max(methodItems.length, targetItems.length)
    const caseKey = testCase.id ?? `new-${caseIndex}`

    return Array.from({ length: displayRowCount }, (_, displayRowIndex) => ({
      ...testCase,
      displayRowKey: `${caseKey}-${displayRowIndex}`,
      displayRowIndex,
      displayRowCount,
      displayMethodItem: methodItems[displayRowIndex] || '',
      displayTargetItem: targetItems[displayRowIndex] || ''
    }))
  })
)

function caseRowSpanMethod({
  row,
  columnIndex
}: {
  row: CodexTestCaseTableRow
  columnIndex: number
}) {
  if ([2, 3].includes(columnIndex)) {
    return { rowspan: 1, colspan: 1 }
  }
  if (row.displayRowIndex > 0) {
    return { rowspan: 0, colspan: 0 }
  }
  return { rowspan: row.displayRowCount, colspan: 1 }
}

function showRequestError(error: unknown, defaultMessage: string) {
  const text = error instanceof Error ? error.message : typeof error === 'string' ? error : defaultMessage
  message.error(text || defaultMessage)
}

async function getTenantOptions() {
  try {
    tenantOptions.value = await TenantApi.getTenantList()
    selectedTenantId.value = tenantOptions.value[0]?.id
  } catch (error) {
    showRequestError(error, '测试租户加载失败')
  }
}

async function getCaseList() {
  caseLoading.value = true
  try {
    const data = await CodexTestApi.getCodexTestCasePage(queryParams)
    caseList.value = data.list
    caseTotal.value = data.total
  } catch (error) {
    showRequestError(error, '测试项加载失败')
  } finally {
    caseLoading.value = false
  }
}

async function getExecutionList() {
  executionLoading.value = true
  try {
    const data = await CodexTestApi.getCodexTestExecutionPage({
      pageNo: 1,
      pageSize: 10,
      targetTenantId: selectedTenantId.value
    })
    executionList.value = data.list
  } catch (error) {
    showRequestError(error, '执行记录加载失败')
  } finally {
    executionLoading.value = false
  }
}

function handleCaseSelectionChange(rows: CodexTestCaseTableRow[]) {
  selectedCaseIds.value = Array.from(
    new Set(rows.map((row) => row.id).filter((id): id is number => Boolean(id)))
  )
}

function openCreate() {
  resetCaseForm()
  caseDialogVisible.value = true
}

async function openEdit(id?: number) {
  if (!id) return
  try {
    const data = await CodexTestApi.getCodexTestCase(id)
    Object.assign(caseForm, data)
    if (caseForm.checkpoints.length === 0) {
      caseForm.checkpoints = [newCheckpoint(1)]
    }
    caseDialogVisible.value = true
  } catch (error) {
    showRequestError(error, '测试项详情加载失败')
  }
}

function addCheckpoint() {
  caseForm.checkpoints.push(newCheckpoint(caseForm.checkpoints.length + 1))
}

function removeCheckpoint(index: number) {
  caseForm.checkpoints.splice(index, 1)
  caseForm.checkpoints.forEach((checkpoint, checkpointIndex) => {
    checkpoint.sort = checkpointIndex + 1
  })
}

async function saveCase() {
  await caseFormRef.value?.validate()
  if (caseForm.checkpoints.some((checkpoint) => !checkpoint.expectedText?.trim())) {
    message.error('测试目标项不能为空')
    return
  }
  try {
    if (caseForm.id) {
      await CodexTestApi.updateCodexTestCase(caseForm)
    } else {
      await CodexTestApi.createCodexTestCase(caseForm)
    }
    message.success('保存成功')
    caseDialogVisible.value = false
    await getCaseList()
  } catch (error) {
    showRequestError(error, '保存失败')
  }
}

async function deleteCase(id?: number) {
  if (!id) return
  try {
    await message.confirm('确认删除该测试项吗？')
    await CodexTestApi.deleteCodexTestCase(id)
    message.success('删除成功')
    await getCaseList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showRequestError(error, '删除失败')
    }
  }
}

async function startExecution(mode: 'SEQUENTIAL' | 'PARALLEL') {
  if (!selectedTenantId.value) {
    message.error('请选择测试租户')
    return
  }
  executeLoading.value = true
  try {
    const executionId = await CodexTestApi.startCodexTestExecution({
      targetTenantId: selectedTenantId.value,
      executionMode: mode,
      caseIds: selectedCaseIds.value
    })
    message.success(`已创建执行批次 ${executionId}`)
    await getExecutionList()
  } catch (error) {
    showRequestError(error, mode === 'PARALLEL' ? '并行执行失败' : '顺序执行失败')
  } finally {
    executeLoading.value = false
  }
}

async function cancelExecution(id: number) {
  try {
    await CodexTestApi.cancelCodexTestExecution(id)
    message.success('已取消执行')
    await getExecutionList()
  } catch (error) {
    showRequestError(error, '取消执行失败')
  }
}

async function openExecution(id: number) {
  try {
    executionDetail.value = await CodexTestApi.getCodexTestExecution(id)
    executionDrawerVisible.value = true
  } catch (error) {
    showRequestError(error, '执行详情加载失败')
  }
}

async function previewArtifact(id: number) {
  try {
    const data = await CodexTestApi.downloadCodexTestArtifact(id)
    if (artifactPreviewUrl.value) {
      URL.revokeObjectURL(artifactPreviewUrl.value)
    }
    artifactPreviewUrl.value = URL.createObjectURL(new Blob([data as BlobPart]))
  } catch (error) {
    showRequestError(error, '失败截图加载失败')
  }
}

function executionTagType(status?: string) {
  if (status === 'PASS') return 'success'
  if (status === 'FAIL' || status === 'TIMEOUT') return 'danger'
  if (status === 'BLOCKED' || status === 'CANCELED') return 'warning'
  return 'info'
}

function checkpointTagType(status?: string) {
  if (status === 'PASS') return 'success'
  if (status === 'FAIL') return 'danger'
  if (status === 'BLOCKED') return 'warning'
  return 'info'
}

function statusText(status?: string) {
  const labels: Record<string, string> = {
    PENDING: '待执行',
    CLAIMED: '已领取',
    RUNNING: '执行中',
    PASS: '通过',
    FAIL: '失败',
    BLOCKED: '阻塞',
    CANCELED: '已取消',
    TIMEOUT: '超时',
    NOT_RUN: '未执行'
  }
  return status ? labels[status] || status : '-'
}

onMounted(async () => {
  await getTenantOptions()
  await getCaseList()
  await getExecutionList()
})
</script>

<style lang="scss" scoped>
.codex-test-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
}

.codex-test-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1.5;
  white-space: normal;
  word-break: break-word;
}

.codex-test-item-line {
  color: var(--el-text-color-regular);
}

.codex-test-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
}

.codex-test-checkpoints {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 10px;
}

.codex-test-checkpoint {
  display: grid;
  grid-template-columns: 120px 180px 1fr 60px;
  gap: 10px;
  align-items: flex-start;
}

.codex-test-artifact {
  width: 100%;
  max-height: 480px;
  margin-top: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
}
</style>
