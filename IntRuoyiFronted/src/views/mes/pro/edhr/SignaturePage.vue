<template>
  <component :is="signaturePageShell" :class="{ 'edhr-signature-page--embedded': isEmbedded }">
    <div class="edhr-query">
      <UnifiedListTemplate
        class="edhr-query__list-template"
        table-key="mes.pro.edhr.signature.history"
        :query-model="queryParams"
        :filter-definitions="signatureQuickFilterDefinitions"
        :show-quick-filter-label="false"
        :quick-filter-state="signatureQuickFilter.state"
        :selected-filter-definition="signatureQuickFilter.selectedDefinition.value"
        :operator-options="signatureQuickFilter.operatorOptions.value"
        :columns="signatureColumns"
        :column-saving="signatureColumnSaving"
        :show-column-reset="false"
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @update:quick-filter-state="signatureQuickFilter.updateState"
        @quick-filter-query="signatureQuickFilter.applyQuickFilter"
        @column-change="saveSignatureColumnConfig"
        @pagination="getList"
      >
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.edhr.signature.history"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无签名记录"
          @header-dragend="handleSignatureHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column v-if="isSignatureColumnVisible('evidenceExpand')" type="expand" :width="getSignatureColumnWidthString('evidenceExpand', 40)">
            <template #default="{ row }">
              <div class="edhr-signature-evidence">
                <div class="edhr-signature-evidence__header">
                  <div class="edhr-signature-evidence__title">签名审计证据</div>
                  <div class="edhr-signature-evidence__subtitle">
                    保留签名当时的身份、岗位角色、权限依据、记录版本和审计哈希。
                  </div>
                </div>
                <div class="edhr-signature-evidence__grid">
                  <div class="edhr-signature-evidence__item">
                    <span>签名编号</span>
                    <strong>{{ row.id || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>签名方式</span>
                    <strong>{{ formatSignatureMode(row.signatureMode) || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>密码校验</span>
                    <strong>{{ row.passwordVerified ? '通过' : '失败' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>账号快照</span>
                    <strong>{{ formatSnapshotValue(row.actorUsernameSnapshot) }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>部门快照</span>
                    <strong>{{ formatSnapshotValue(row.actorDeptNameSnapshot) }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>岗位快照</span>
                    <strong>{{ formatSnapshotValue(row.actorPostNamesSnapshot) }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>角色快照</span>
                    <strong>{{ formatSnapshotValue(row.actorRoleNamesSnapshot) }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>签名目的</span>
                    <strong>{{ formatSnapshotValue(row.signaturePurpose || row.meaningText) }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>认证方式</span>
                    <strong>{{ formatSnapshotValue(row.authenticationMethod || row.signatureMode) }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>流程任务</span>
                    <strong>{{ row.bpmTaskName || row.bpmTaskId || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>流程实例</span>
                    <strong>{{ row.processInstanceId || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>系统签名时间</span>
                    <strong>{{ formatSignatureSignedAt(row.signedAt) || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>选择签名时间</span>
                    <strong>{{ formatSignatureSignedAt(row.selectedSignedAt) || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>显示签名时间</span>
                    <strong>{{ formatSignatureSignedAt(row.signatureDisplayAt) || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>时间模式</span>
                    <strong>{{ formatSignatureTimeMode(row.signatureTimeMode) || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>时区</span>
                    <strong>{{ row.selectedTimeZone || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>时间策略版本</span>
                    <strong>{{ row.selectedTimePolicyVersion || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>记录版本</span>
                    <strong>{{ formatSnapshotValue(row.recordVersionSnapshot) }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>审计状态</span>
                    <strong>{{ formatSnapshotValue(row.snapshotStatus) }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item edhr-signature-evidence__item--wide">
                    <span>权限依据</span>
                    <strong>{{ formatSnapshotValue(row.authorizationBasis) }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item edhr-signature-evidence__item--wide">
                    <span>业务记录 Hash</span>
                    <strong class="edhr-signature-evidence__hash">
                      {{ formatSnapshotValue(row.recordHashSnapshot) }}
                    </strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>客户端 IP</span>
                    <strong>{{ row.clientIpSnapshot || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item">
                    <span>User-Agent</span>
                    <strong>{{ row.userAgentSnapshot || '--' }}</strong>
                  </div>
                  <div class="edhr-signature-evidence__item edhr-signature-evidence__item--wide">
                    <span>时间审计哈希</span>
                    <strong class="edhr-signature-evidence__hash">
                      {{ row.selectedTimeAuditHash || '--' }}
                    </strong>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="isSignatureColumnVisible('execution')" label="业务记录" prop="execution" :width="getSignatureColumnWidthString('execution', 170)" v-bind="sortColumnAttrs('execution')">
            <template #default="{ row }">
              <el-button
                v-if="row.executionId"
                link
                type="primary"
                class="edhr-signature__execution-link"
                @click="openExecution(row)"
              >
                {{ row.executionCode || `执行 #${row.executionId}` }}
              </el-button>
              <span v-else class="edhr-signature-muted">{{ row.executionCode || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="isSignatureColumnVisible('actionType')" label="动作结果" prop="actionType" :width="getSignatureColumnWidthString('actionType', 110)" v-bind="sortColumnAttrs('actionType')">
            <template #default="{ row }">{{ formatSignatureAction(row.actionType) }}</template>
          </el-table-column>
          <el-table-column v-if="isSignatureColumnVisible('meaningText')" label="签名含义" prop="meaningText" :min-width="getSignatureColumnMinWidthString('meaningText', 140)" v-bind="sortColumnAttrs('meaningText')" />
          <el-table-column v-if="isSignatureColumnVisible('actorName')" label="签名人" prop="actorName" :width="getSignatureColumnWidthString('actorName', 150)" v-bind="sortColumnAttrs('actorName')">
            <template #default="{ row }">
              {{ row.actorName || row.actorNicknameSnapshot || '--' }}
            </template>
          </el-table-column>
          <el-table-column v-if="isSignatureColumnVisible('actorUsernameSnapshot')" label="账号" prop="actorUsernameSnapshot" :min-width="getSignatureColumnMinWidthString('actorUsernameSnapshot', 130)" v-bind="sortColumnAttrs('actorUsernameSnapshot')">
            <template #default="{ row }">
              {{ row.actorUsernameSnapshot || '旧版证据未记录' }}
            </template>
          </el-table-column>
          <el-table-column v-if="isSignatureColumnVisible('actorDeptPost')" label="部门/岗位" prop="actorDeptPost" :min-width="getSignatureColumnMinWidthString('actorDeptPost', 170)" v-bind="sortColumnAttrs('actorDeptPost')">
            <template #default="{ row }">
              <div>{{ formatSnapshotValue(row.actorDeptNameSnapshot) }}</div>
              <div class="edhr-signature-muted">{{ formatSnapshotValue(row.actorPostNamesSnapshot) }}</div>
            </template>
          </el-table-column>
          <el-table-column v-if="isSignatureColumnVisible('actorRoleNamesSnapshot')" label="角色" prop="actorRoleNamesSnapshot" :min-width="getSignatureColumnMinWidthString('actorRoleNamesSnapshot', 150)" v-bind="sortColumnAttrs('actorRoleNamesSnapshot')">
            <template #default="{ row }">
              {{ formatSnapshotValue(row.actorRoleNamesSnapshot) }}
            </template>
          </el-table-column>
          <el-table-column v-if="isSignatureColumnVisible('signaturePurpose')" label="签名目的" prop="signaturePurpose" :min-width="getSignatureColumnMinWidthString('signaturePurpose', 140)" v-bind="sortColumnAttrs('signaturePurpose')">
            <template #default="{ row }">
              {{ formatSnapshotValue(row.signaturePurpose || row.meaningText) }}
            </template>
          </el-table-column>
          <el-table-column v-if="isSignatureColumnVisible('signatureConfirm')" label="签名确认" prop="signatureConfirm" :width="getSignatureColumnWidthString('signatureConfirm', 105)" v-bind="sortColumnAttrs('signatureConfirm')">
            <template #default="{ row }">
              <div>{{ formatSignatureMode(row.signatureMode) }}</div>
              <el-tag size="small" :type="row.passwordVerified ? 'success' : 'danger'" effect="plain">
                {{ row.passwordVerified ? '通过' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isSignatureColumnVisible('signedAt')" label="签名时间" prop="signedAt" :width="getSignatureColumnWidthString('signedAt', 190)" v-bind="sortColumnAttrs('signedAt')">
            <template #default="{ row }">
              <div>
                {{
                  formatSignatureSignedAt(
                    row.signatureDisplayAt || row.selectedSignedAt || row.signedAt
                  ) || '--'
                }}
              </div>
              <div
                v-if="row.signedAt && row.signatureDisplayAt && row.signedAt !== row.signatureDisplayAt"
                class="edhr-signature-muted"
              >
                系统：{{ formatSignatureSignedAt(row.signedAt) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="isSignatureColumnVisible('comment')" label="意见/原因" prop="comment" :min-width="getSignatureColumnMinWidthString('comment', 150)" v-bind="sortColumnAttrs('comment')">
            <template #default="{ row }">{{ row.reason || row.comment || '--' }}</template>
          </el-table-column>
        </el-table>
        </template>
      </UnifiedListTemplate>
    </div>
  </component>
</template>

<script setup lang="ts">
import { ContentWrap } from '@/components/ContentWrap'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import {
  getEdhrExecutionSignaturePage,
  type EdhrSignatureActionType,
  type EdhrSignatureSummaryVO
} from '@/api/mes/pro/edhr/signatures'
import { formatDate } from '@/utils/formatTime'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'MesProFeedbackEdhrSignatures' })

const props = withDefaults(
  defineProps<{
    embedded?: boolean
    initialExecutionId?: string | number
    initialExecutionCode?: string
  }>(),
  {
    embedded: false,
    initialExecutionCode: ''
  }
)
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrSignatureSummaryVO[]>([])
const total = ref(0)
const isEmbedded = computed(() => props.embedded)
const signaturePageShell = computed(() => (isEmbedded.value ? 'div' : ContentWrap))
const SIGNATURE_ACTION_LABELS: Record<EdhrSignatureActionType, string> = {
  FIELD_CHANGE: '字段变更',
  FORM_REVIEW: '表单复核',
  SUBMIT: '提交审批',
  REVIEW_APPROVE: '审核签名',
  APPROVE: '最终批准',
  REJECT: '审批驳回',
  ARCHIVE_SEAL: '归档封存'
}
const SIGNATURE_MODE_LABELS: Record<EdhrSignatureSummaryVO['signatureMode'], string> = {
  PASSWORD: '密码签名'
}
const SIGNATURE_TIME_MODE_LABELS: Record<
  NonNullable<EdhrSignatureSummaryVO['signatureTimeMode']>,
  string
> = {
  SERVER_TIME: '服务端时间',
  USER_SELECTED: '手动选择时间'
}
const signatureDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'evidenceExpand', label: '审计证据', width: 40, hideable: false, business: false },
  { key: 'execution', label: '业务记录', width: 170 },
  { key: 'actionType', label: '动作结果', width: 110 },
  { key: 'meaningText', label: '签名含义', minWidth: 140 },
  { key: 'actorName', label: '签名人', width: 150 },
  { key: 'actorUsernameSnapshot', label: '账号', minWidth: 130 },
  { key: 'actorDeptPost', label: '部门/岗位', minWidth: 170 },
  { key: 'actorRoleNamesSnapshot', label: '角色', minWidth: 150 },
  { key: 'signaturePurpose', label: '签名目的', minWidth: 140 },
  { key: 'signatureConfirm', label: '签名确认', width: 105 },
  { key: 'signedAt', label: '签名时间', width: 190 },
  { key: 'comment', label: '意见/原因', minWidth: 150 }
]
const {
  columns: signatureColumns,
  saving: signatureColumnSaving,
  isColumnVisible: isSignatureColumnVisible,
  getColumnWidthString: getSignatureColumnWidthString,
  getColumnMinWidthString: getSignatureColumnMinWidthString,
  handleHeaderDragend: handleSignatureHeaderDragend,
  saveConfig: saveSignatureColumnConfig
} = useUserTableColumns('mes.pro.edhr.signature.history', signatureDefaultColumns)
const signatureQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'executionCode', label: '执行编号', type: 'text', queryParamKey: 'executionCode', placeholder: '请输入执行编号' },
  { key: 'actorName', label: '签名人', type: 'text', queryParamKey: 'actorName', placeholder: '请输入签名人' },
  {
    key: 'actionType',
    label: '动作',
    type: 'select',
    queryParamKey: 'actionType',
    options: Object.entries(SIGNATURE_ACTION_LABELS).map(([value, label]) => ({ value, label }))
  },
  { key: 'processInstanceId', label: '流程实例', type: 'text', queryParamKey: 'processInstanceId', placeholder: '请输入流程实例' },
  { key: 'bpmTaskId', label: 'BPM任务', type: 'text', queryParamKey: 'bpmTaskId', placeholder: '请输入BPM任务' }
]
const resolveInitialExecutionId = () => {
  const propExecutionId = parsePositiveRouteQueryId(props.initialExecutionId)
  if (propExecutionId) return propExecutionId
  return parsePositiveRouteQueryId(route.query.executionId) || undefined
}

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  executionId: resolveInitialExecutionId() as string | number | undefined,
  executionCode: props.initialExecutionCode || (typeof route.query.executionCode === 'string' ? route.query.executionCode : ''),
  actorId: undefined as number | undefined,
  actorName: '',
  actionType: undefined as EdhrSignatureActionType | undefined,
  processInstanceId: '',
  bpmTaskId: '',
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const message = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof message === 'string' && message.trim()) return message
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const formatSignatureAction = (actionType?: EdhrSignatureActionType) => {
  if (!actionType) return ''
  const label = SIGNATURE_ACTION_LABELS[actionType]
  if (!label) {
    throw new Error(`未知签名动作：${String(actionType)}`)
  }
  return label
}

const formatSignatureMode = (signatureMode?: EdhrSignatureSummaryVO['signatureMode']) => {
  if (!signatureMode) return ''
  const label = SIGNATURE_MODE_LABELS[signatureMode]
  if (!label) {
    throw new Error(`未知签名方式：${String(signatureMode)}`)
  }
  return label
}

const formatSignatureTimeMode = (signatureTimeMode?: EdhrSignatureSummaryVO['signatureTimeMode']) => {
  if (!signatureTimeMode) return ''
  const label = SIGNATURE_TIME_MODE_LABELS[signatureTimeMode]
  if (!label) {
    throw new Error(`未知签名时间模式：${String(signatureTimeMode)}`)
  }
  return label
}

const formatSnapshotValue = (value?: string | number | null) => {
  if (value === undefined || value === null) return '旧版证据未记录'
  const text = String(value).trim()
  return text || '旧版证据未记录'
}

const formatSignatureSignedAt = (signedAt?: string | number | Date) => {
  if (!signedAt) return ''
  const parsedDate =
    typeof signedAt === 'number' || /^\d+$/.test(String(signedAt))
      ? new Date(Number(signedAt))
      : new Date(signedAt)
  if (Number.isNaN(parsedDate.getTime())) {
    throw new Error(`签名时间不可解析：${String(signedAt)}`)
  }
  return formatDate(parsedDate, 'YYYY年M月D日 HH:mm:ss')
}

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrExecutionSignaturePage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      executionId: parsePositiveRouteQueryId(queryParams.executionId) || undefined,
      executionCode: queryParams.executionCode?.trim() || undefined,
      actionType: queryParams.actionType,
      actorId: Number.isFinite(queryParams.actorId) ? queryParams.actorId : undefined,
      actorName: queryParams.actorName?.trim() || undefined,
      processInstanceId: queryParams.processInstanceId?.trim() || undefined,
      bpmTaskId: queryParams.bpmTaskId?.trim() || undefined
    })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'eDHR 签名记录加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const signatureQuickFilter = useTableQuickFilter(
  'mes.pro.edhr.signature.history',
  signatureQuickFilterDefinitions,
  queryParams,
  getList
)

const openExecution = async (row: EdhrSignatureSummaryVO) => {
  if (!row.executionId) return
  await router.push({
    path: '/mes/pro/feedback/edhr-execution/form',
    query: { id: String(row.executionId), viewMode: 'tracking' }
  })
}

onMounted(() => getList())
</script>

<style scoped>
.edhr-query__toolbar,
.edhr-query__table {
  padding: 16px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-signature-page--embedded {
  margin: 0;
}
.edhr-query__toolbar {
  border-radius: 8px 8px 0 0;
  border-bottom: 0;
  padding-bottom: 0;
}
.edhr-query__table {
  border-radius: 0 0 8px 8px;
}
.edhr-signature-evidence {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}
.edhr-signature-evidence__header {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  align-items: baseline;
  margin-bottom: 12px;
}
.edhr-signature-evidence__title {
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
}
.edhr-signature-evidence__subtitle,
.edhr-signature-muted {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}
.edhr-signature-evidence__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}
.edhr-signature-evidence__item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #ffffff;
}
.edhr-signature-evidence__item span {
  display: block;
  margin-bottom: 6px;
  color: #6b7280;
  font-size: 12px;
}
.edhr-signature-evidence__item strong {
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
.edhr-signature-evidence__item--wide {
  grid-column: 1 / -1;
}
.edhr-signature-evidence__hash {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
}
.edhr-signature__execution-link {
  padding: 0;
  font-weight: 600;
}
</style>
