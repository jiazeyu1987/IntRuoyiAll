<template>
  <component :is="fieldAuditPageShell" :class="{ 'edhr-field-audit--embedded': isEmbedded }">
    <div class="edhr-field-audit">
      <el-tabs v-model="activeView" class="edhr-field-audit__view-tabs">
        <el-tab-pane label="当前责任汇总" name="responsibility">
          <el-form :inline="true" :model="responsibilityQueryParams" class="edhr-field-audit__toolbar">
            <el-form-item label="执行ID">
              <el-input v-model.number="responsibilityQueryParams.executionId" clearable class="!w-120px" />
            </el-form-item>
            <el-form-item label="字段">
              <el-input
                v-model="responsibilityQueryParams.fieldKeyword"
                clearable
                placeholder="字段名称 / 标识"
                class="!w-200px"
              />
            </el-form-item>
            <el-form-item label="证据状态">
              <el-select v-model="responsibilityQueryParams.evidenceStatus" clearable class="!w-150px">
                <el-option label="证据完整" value="COMPLETE" />
                <el-option label="证据缺失" value="EVIDENCE_MISSING" />
                <el-option label="证据阻断" value="BLOCKED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleResponsibilityQuery">查询</el-button>
              <el-button @click="resetResponsibilityQuery">重置</el-button>
              <el-button
                v-hasPermi="['mes:pro-batch-record-execution:field-audit-export']"
                :loading="responsibilityExportLoading"
                @click="handleResponsibilityExport"
              >
                责任证明导出
              </el-button>
            </el-form-item>
            <el-form-item class="edhr-field-audit__advanced">
              <el-collapse v-model="responsibilityAdvancedFilterNames">
                <el-collapse-item title="责任筛选" name="responsibility-summary">
                  <div class="edhr-field-audit__advanced-grid">
                    <el-form-item label="值来源">
                      <el-select v-model="responsibilityQueryParams.valueOrigin" clearable class="!w-150px">
                        <el-option label="人工填写" value="HUMAN" />
                        <el-option label="系统基线" value="SYSTEM_BASELINE" />
                        <el-option label="空值未填" value="EMPTY_UNTOUCHED" />
                        <el-option label="来源未知" value="UNKNOWN" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="操作人ID">
                      <el-input v-model.number="responsibilityQueryParams.actorId" clearable class="!w-140px" />
                    </el-form-item>
                    <div class="edhr-field-audit__hint">
                      责任视图仅展示实际证据，不通过创建人、更新人、候选人或管理员推断实际填写人。
                    </div>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="responsibilityError"
            :title="responsibilityError"
            type="error"
            :closable="false"
            show-icon
          />
          <el-alert
            v-if="responsibilitySummary"
            :title="`执行 ${responsibilitySummary.executionCode || responsibilitySummary.executionId}：${resolveResponsibilityEvidenceStatusLabel(responsibilitySummary.overallEvidenceStatus)}，字段 ${responsibilitySummary.total} 个${responsibilitySummary.contextWarnings?.length ? '，上下文提示：' + resolveResponsibilityContextWarningsText(responsibilitySummary.contextWarnings) : ''}`"
            :type="resolveResponsibilityEvidenceStatusType(responsibilitySummary.overallEvidenceStatus)"
            :closable="false"
            show-icon
          />

          <div class="edhr-field-audit__table edhr-field-audit__responsibility-summary responsibility-export">
            <el-table
              v-loading="responsibilityLoading"
              :data="responsibilityList"
              stripe
              :show-overflow-tooltip="true"
              empty-text="暂无字段责任记录，请输入执行ID或调整筛选条件"
            >
              <el-table-column type="expand" width="44">
                <template #default="{ row }">
                  <div class="edhr-field-audit__evidence">
                    <div class="edhr-field-audit__evidence-header">
                      <div>
                        <div class="edhr-field-audit__evidence-title">责任证据</div>
                        <div class="edhr-field-audit__muted">按业务视角展示当前填写、责任人和证据状态</div>
                      </div>
                      <el-tag :type="resolveResponsibilityEvidenceStatusType(row.evidenceStatus)">
                        {{ resolveResponsibilityEvidenceStatusLabel(row.evidenceStatus) }}
                      </el-tag>
                    </div>
                    <div class="edhr-field-audit__evidence-summary">
                      <div class="edhr-field-audit__evidence-card edhr-field-audit__evidence-card--wide">
                        <div class="edhr-field-audit__label">字段名称</div>
                        <div class="edhr-field-audit__value">{{ row.fieldLabel || row.fieldKey || '--' }}</div>
                        <div class="edhr-field-audit__muted">{{ row.fieldPath || '未提供字段路径' }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-card">
                        <div class="edhr-field-audit__label">当前填写</div>
                        <div class="edhr-field-audit__value">{{ row.currentValueDisplay || '空值未填' }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-card">
                        <div class="edhr-field-audit__label">填写状态</div>
                        <div class="edhr-field-audit__value">{{ resolveResponsibilityValueOriginLabel(row.valueOrigin) }}</div>
                        <div class="edhr-field-audit__muted">
                          {{ row.currentValueChangedAt ? '最后更新 ' + row.currentValueChangedAt : '暂无更新时间' }}
                        </div>
                      </div>
                      <div class="edhr-field-audit__evidence-card">
                        <div class="edhr-field-audit__label">填写责任</div>
                        <div class="edhr-field-audit__value">{{ row.firstHumanActorName || '--' }}</div>
                        <div class="edhr-field-audit__muted">{{ row.firstHumanChangedAt || '暂无有效填写人' }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-card">
                        <div class="edhr-field-audit__label">操作记录</div>
                        <div class="edhr-field-audit__value">
                          {{ row.currentValueActorName || row.firstHumanActorName || '--' }}
                        </div>
                        <div class="edhr-field-audit__muted">
                          历史 {{ row.historyCount || 0 }} 条{{
                            row.currentValueChangedAt ? ' · ' + row.currentValueChangedAt : ''
                          }}
                        </div>
                      </div>
                    </div>
                    <div
                      v-if="row.reasonCodes?.length || responsibilitySummary?.contextWarnings?.length"
                      class="edhr-field-audit__evidence-notes"
                    >
                      <span v-if="row.reasonCodes?.length" class="edhr-field-audit__evidence-note-label">
                        证据提示
                      </span>
                      <el-tag
                        v-for="reasonCode in row.reasonCodes"
                        :key="reasonCode"
                        type="warning"
                        effect="plain"
                      >
                        {{ resolveResponsibilityReasonCodeLabel(reasonCode) }}
                      </el-tag>
                      <span
                        v-if="responsibilitySummary?.contextWarnings?.length"
                        class="edhr-field-audit__evidence-note-label"
                      >
                        上下文提示
                      </span>
                      <el-tag
                        v-for="warningCode in responsibilitySummary?.contextWarnings"
                        :key="warningCode"
                        type="info"
                        effect="plain"
                      >
                        {{ resolveResponsibilityContextWarningLabel(warningCode) }}
                      </el-tag>
                    </div>
                    <details class="edhr-field-audit__technical-details">
                      <summary>技术详情</summary>
                      <div class="edhr-field-audit__evidence-grid">
                        <div class="edhr-field-audit__evidence-item">
                          <div class="edhr-field-audit__label">字段路径</div>
                          <div class="edhr-field-audit__value">{{ row.fieldPath || '--' }}</div>
                        </div>
                        <div class="edhr-field-audit__evidence-item">
                          <div class="edhr-field-audit__label">字段标识</div>
                          <div class="edhr-field-audit__value">{{ row.fieldKey || '--' }}</div>
                        </div>
                        <div class="edhr-field-audit__evidence-item">
                          <div class="edhr-field-audit__label">字段坐标</div>
                          <div class="edhr-field-audit__value">
                            第 {{ row.rowIndex ?? '--' }} 行 / 第 {{ row.columnIndex ?? '--' }} 列
                          </div>
                        </div>
                        <div class="edhr-field-audit__evidence-item">
                          <div class="edhr-field-audit__label">当前值原文</div>
                          <div class="edhr-field-audit__value">{{ row.currentValueJson || '--' }}</div>
                        </div>
                        <div class="edhr-field-audit__evidence-item">
                          <div class="edhr-field-audit__label">当前值校验码</div>
                          <div class="edhr-field-audit__value">{{ row.currentValueHash || '--' }}</div>
                        </div>
                        <div class="edhr-field-audit__evidence-item">
                          <div class="edhr-field-audit__label">最新审计明细</div>
                          <div class="edhr-field-audit__value">{{ row.latestAuditItemId || '--' }}</div>
                        </div>
                      </div>
                    </details>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="字段" min-width="220">
                <template #default="{ row }">
                  <div class="edhr-field-audit__strong">{{ row.fieldLabel || row.fieldKey }}</div>
                  <div class="edhr-field-audit__muted">{{ row.fieldPath || '--' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="当前值 / 来源" min-width="220">
                <template #default="{ row }">
                  <div class="edhr-field-audit__change-value">{{ row.currentValueDisplay || '--' }}</div>
                  <div class="edhr-field-audit__muted">{{ resolveResponsibilityValueOriginLabel(row.valueOrigin) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="首次有效填写人" min-width="160">
                <template #default="{ row }">
                  <div class="edhr-field-audit__strong">{{ row.firstHumanActorName || '--' }}</div>
                  <div class="edhr-field-audit__muted">{{ row.firstHumanChangedAt || '--' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="当前值最后操作人" min-width="170">
                <template #default="{ row }">
                  <div class="edhr-field-audit__strong">{{ row.currentValueActorName || '--' }}</div>
                  <div class="edhr-field-audit__muted">{{ row.currentValueChangedAt || '--' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="证据状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="resolveResponsibilityEvidenceStatusType(row.evidenceStatus)">
                    {{ resolveResponsibilityEvidenceStatusLabel(row.evidenceStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
                            <el-table-column label="历史" width="120">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openResponsibilityHistory(row)">
                    查看历史 {{ row.historyCount || 0 }}
                  </el-button>
                </template>
              </el-table-column>
              <el-table-column label="审计" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openResponsibilityAuditDetail(row)">
                    查看审计
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <Pagination
              :total="responsibilityTotal"
              v-model:page="responsibilityQueryParams.pageNo"
              v-model:limit="responsibilityQueryParams.pageSize"
              @pagination="getResponsibilitySummary"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="变更流水" name="audit">
          <el-form :inline="true" :model="queryParams" class="edhr-field-audit__toolbar">
            <el-form-item label="执行ID">
              <el-input v-model.number="queryParams.executionId" clearable class="!w-120px" />
            </el-form-item>
            <el-form-item label="原因">
              <el-select v-model="queryParams.reasonCategory" clearable class="!w-180px">
                <el-option
                  v-for="option in EDHR_FIELD_CHANGE_REASON_OPTIONS"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="修改时间">
              <el-date-picker
                v-model="changedTimeRange"
                type="datetimerange"
                value-format="YYYY-MM-DD HH:mm:ss"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                class="!w-360px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleQuery">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
              <el-button
                v-hasPermi="['mes:pro-batch-record-execution:field-audit-verify']"
                :loading="verifyLoading"
                :disabled="!listLoaded"
                @click="handleVerify"
              >
                校验当前筛选结果
              </el-button>
              <el-button
                v-hasPermi="['mes:pro-batch-record-execution:field-audit-export']"
                :loading="exportLoading"
                :disabled="!listLoaded"
                @click="handleExport"
              >
                导出审计链
              </el-button>
            </el-form-item>
            <el-form-item class="edhr-field-audit__advanced">
              <el-collapse v-model="fieldAuditAdvancedFilterNames">
                <el-collapse-item title="高级筛选" name="advanced">
                  <div class="edhr-field-audit__advanced-grid">
                    <el-form-item label="审计批次">
                      <el-input v-model="queryParams.auditBatchId" clearable class="!w-160px" />
                    </el-form-item>
                    <el-form-item label="字段路径">
                      <el-input v-model="queryParams.fieldPath" clearable class="!w-220px" />
                    </el-form-item>
                    <el-form-item label="字段标识">
                      <el-input v-model="queryParams.fieldKey" clearable class="!w-150px" />
                    </el-form-item>
                    <el-form-item label="修改人">
                      <el-input v-model="queryParams.actorName" clearable class="!w-140px" />
                    </el-form-item>
                    <el-form-item label="原因关键字">
                      <el-input v-model="queryParams.reasonKeyword" clearable class="!w-180px" />
                    </el-form-item>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </el-form-item>
          </el-form>

          <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
          <el-alert
            v-if="verifyResult"
            :title="resolveVerifyResultTitle(verifyResult)"
            :type="verifyResult.hashVerification.status === 'VALID' ? 'success' : 'error'"
            :closable="false"
            show-icon
          />

          <div class="edhr-field-audit__table edhr-field-audit__responsibility-history">
            <el-table
              v-loading="loading"
              :data="list"
              stripe
              :show-overflow-tooltip="true"
              empty-text="暂无字段审计记录，请输入执行ID或调整筛选条件"
            >
              <el-table-column type="expand" width="44">
                <template #default="{ row }">
                  <div class="edhr-field-audit__evidence">
                    <div class="edhr-field-audit__evidence-title">审计证据</div>
                    <div class="edhr-field-audit__evidence-grid">
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">字段路径</div>
                        <div class="edhr-field-audit__value">{{ row.fieldPath || '--' }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">字段标识</div>
                        <div class="edhr-field-audit__value">{{ row.fieldKey || '--' }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">组件</div>
                        <div class="edhr-field-audit__value">{{ row.component || '--' }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">定位</div>
                        <div class="edhr-field-audit__value">
                          rowIndex={{ row.rowIndex }} / columnIndex={{ row.columnIndex }}
                        </div>
                      </div>
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">旧值 JSON</div>
                        <div class="edhr-field-audit__value">{{ formatJson(row.oldValueJson) }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">旧值 hash</div>
                        <div class="edhr-field-audit__value">{{ row.oldValueHash || '--' }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">新值 JSON</div>
                        <div class="edhr-field-audit__value">{{ formatJson(row.newValueJson) }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">新值 hash</div>
                        <div class="edhr-field-audit__value">{{ row.newValueHash || '--' }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">审计哈希</div>
                        <div class="edhr-field-audit__value">{{ row.auditHash || '--' }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">前序哈希</div>
                        <div class="edhr-field-audit__value">{{ row.previousHash || '--' }}</div>
                      </div>
                      <div class="edhr-field-audit__evidence-item">
                        <div class="edhr-field-audit__label">签名</div>
                        <div class="edhr-field-audit__value">{{ row.signatureId || '--' }}</div>
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="审计序号" prop="fieldAuditRevision" width="96" />
              <el-table-column label="执行编号" min-width="150">
                <template #default="{ row }">
                  <el-button link type="primary" class="edhr-field-audit__execution-link" @click="openExecution(row)">
                    {{ row.executionCode || `#${row.executionId}` }}
                  </el-button>
                </template>
              </el-table-column>
              <el-table-column label="字段" min-width="210">
                <template #default="{ row }">
                  <div class="edhr-field-audit__strong">{{ row.fieldLabel || row.fieldKey }}</div>
                  <div class="edhr-field-audit__muted">{{ row.fieldPath }}</div>
                </template>
              </el-table-column>
              <el-table-column label="变更值" min-width="210">
                <template #default="{ row }">
                  <div class="edhr-field-audit__change-value">{{ row.oldValueDisplay || '--' }}</div>
                  <div class="edhr-field-audit__change-arrow">→</div>
                  <div class="edhr-field-audit__change-value">{{ row.newValueDisplay || '--' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="原因" min-width="180">
                <template #default="{ row }">
                  <div>{{ row.reasonCategory || '--' }}</div>
                  <div class="edhr-field-audit__muted">{{ row.reasonText || '--' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="修改人 / 时间" min-width="150">
                <template #default="{ row }">
                  <div class="edhr-field-audit__strong">{{ row.actorName || '--' }}</div>
                  <div class="edhr-field-audit__muted">{{ row.changedAt || '--' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="hash 状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="resolveHashStatusType(row.hashVerification?.status)">
                    {{ resolveHashStatusLabel(row.hashVerification?.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="76" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
            <Pagination
              :total="total"
              v-model:page="queryParams.pageNo"
              v-model:limit="queryParams.pageSize"
              @pagination="getList"
            />
          </div>
        </el-tab-pane>
      </el-tabs>

      <el-dialog
        v-model="responsibilityHistoryDialogVisible"
        title="字段责任历史"
        width="980px"
        class="edhr-field-audit__history-dialog"
      >
        <div class="edhr-field-audit__history-title">
          {{ selectedResponsibilityField?.fieldLabel || selectedResponsibilityField?.fieldKey || '--' }}
        </div>
        <el-table
          v-loading="responsibilityHistoryLoading"
          :data="responsibilityHistory?.list || []"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无字段责任历史"
        >
          <el-table-column label="审计序号" prop="fieldAuditRevision" width="96" />
          <el-table-column label="变更值" min-width="220">
            <template #default="{ row }">
              <div class="edhr-field-audit__change-value">{{ row.oldValueDisplay || '--' }}</div>
              <div class="edhr-field-audit__change-arrow">→</div>
              <div class="edhr-field-audit__change-value">{{ row.newValueDisplay || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作人 / 时间" min-width="160">
            <template #default="{ row }">
              <div class="edhr-field-audit__strong">{{ row.actorName || '--' }}</div>
              <div class="edhr-field-audit__muted">{{ row.changedAt || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="签名" min-width="140">
            <template #default="{ row }">
              <div>{{ row.signatureId || '--' }}</div>
              <div class="edhr-field-audit__muted">{{ row.signatureActorNicknameSnapshot || row.signatureActorUsernameSnapshot || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="证据状态" width="120">
            <template #default="{ row }">
              <el-tag :type="resolveResponsibilityEvidenceStatusType(row.evidenceStatus)">
                {{ resolveResponsibilityEvidenceStatusLabel(row.evidenceStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原因" min-width="180">
            <template #default="{ row }">
              <div>{{ row.reasonText || '--' }}</div>
              <div class="edhr-field-audit__muted">
                {{ resolveResponsibilityReasonCodesText(row.reasonCodes) }}
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>
    </div>
  </component>
</template>

<script setup lang="ts">
import {
  EDHR_FIELD_CHANGE_REASON_OPTIONS,
  EDHR_HASH_STATUS_LABEL_MAP,
  EDHR_HASH_STATUS_TAG_TYPE_MAP,
  exportEdhrFieldAudit,
  exportEdhrFieldResponsibility,
  getEdhrFieldAuditPage,
  getEdhrFieldResponsibilityHistory,
  getEdhrFieldResponsibilitySummary,
  verifyEdhrFieldAuditChain,
  type EdhrFieldAuditEntryVO,
  type EdhrFieldAuditExportRespVO,
  type EdhrFieldAuditPageReqVO,
  type EdhrFieldAuditVerifyRespVO,
  type EdhrFieldResponsibilityContextWarning,
  type EdhrFieldResponsibilityEvidenceStatus,
  type EdhrFieldResponsibilityExportRespVO,
  type EdhrFieldResponsibilityHistoryRespVO,
  type EdhrFieldResponsibilityItemRespVO,
  type EdhrFieldResponsibilityReasonCode,
  type EdhrFieldResponsibilitySummaryReqVO,
  type EdhrFieldResponsibilitySummaryRespVO,
  type EdhrFieldResponsibilityValueOrigin
} from '@/api/mes/pro/edhr/fieldAudit'
import { hasPermission } from '@/directives/permission/hasPermi'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'MesProFeedbackEdhrFieldAudit' })

const props = withDefaults(
  defineProps<{
    embedded?: boolean
    initialExecutionId?: string | number
    initialView?: 'responsibility' | 'audit'
  }>(),
  {
    embedded: false
  }
)

const FIELD_AUDIT_QUERY_PERMISSION = 'mes:pro-batch-record-execution:field-audit-query'
const FIELD_AUDIT_EXPORT_PERMISSION = 'mes:pro-batch-record-execution:field-audit-export'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const isEmbedded = computed(() => props.embedded)
const fieldAuditPageShell = computed(() => (isEmbedded.value ? 'div' : 'ContentWrap'))
const loading = ref(false)
const verifyLoading = ref(false)
const exportLoading = ref(false)
const responsibilityLoading = ref(false)
const responsibilityHistoryLoading = ref(false)
const responsibilityExportLoading = ref(false)
const loadError = ref('')
const responsibilityError = ref('')
const list = ref<EdhrFieldAuditEntryVO[]>([])
const responsibilityList = ref<EdhrFieldResponsibilityItemRespVO[]>([])
const total = ref(0)
const responsibilityTotal = ref(0)
const listLoaded = ref(false)
const changedTimeRange = ref<string[]>([])
const verifyResult = ref<EdhrFieldAuditVerifyRespVO>()
const responsibilitySummary = ref<EdhrFieldResponsibilitySummaryRespVO>()
const responsibilityHistory = ref<EdhrFieldResponsibilityHistoryRespVO>()
const selectedResponsibilityField = ref<EdhrFieldResponsibilityItemRespVO>()
const responsibilityHistoryDialogVisible = ref(false)
const fieldAuditAdvancedFilterNames = ref<string[]>([])
const responsibilityAdvancedFilterNames = ref<string[]>([])
const resolveInitialExecutionId = () => {
  return (
    parsePositiveRouteQueryId(props.initialExecutionId) ||
    parsePositiveRouteQueryId(route.query.executionId) ||
    undefined
  )
}
const resolveInitialView = () => {
  if (props.initialView === 'responsibility' || props.initialView === 'audit') return props.initialView
  return route.query.view === 'responsibility' ? 'responsibility' : 'audit'
}
const activeView = ref<'responsibility' | 'audit'>(resolveInitialView())
const queryParams = reactive<EdhrFieldAuditPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  executionId: resolveInitialExecutionId(),
  auditBatchId: parsePositiveRouteQueryId(route.query.auditBatchId) || undefined,
  fieldPath: '',
  fieldKey: '',
  actorName: '',
  reasonKeyword: '',
  reasonCategory: undefined,
})
const responsibilityQueryParams = reactive<EdhrFieldResponsibilitySummaryReqVO>({
  pageNo: 1,
  pageSize: 50,
  executionId: resolveInitialExecutionId() || 0,
  fieldKeyword: '',
  evidenceStatus: undefined,
  valueOrigin: undefined,
  actorId: undefined
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const resolveHashStatusLabel = (status?: string) => {
  return status && status in EDHR_HASH_STATUS_LABEL_MAP
    ? EDHR_HASH_STATUS_LABEL_MAP[status as keyof typeof EDHR_HASH_STATUS_LABEL_MAP]
    : status || '--'
}

const resolveHashStatusType = (status?: string) => {
  return status && status in EDHR_HASH_STATUS_TAG_TYPE_MAP
    ? EDHR_HASH_STATUS_TAG_TYPE_MAP[status as keyof typeof EDHR_HASH_STATUS_TAG_TYPE_MAP]
    : 'info'
}

const responsibilityEvidenceStatusLabelMap: Record<EdhrFieldResponsibilityEvidenceStatus, string> = {
  COMPLETE: '证据完整',
  EVIDENCE_MISSING: '证据缺失',
  BLOCKED: '证据阻断'
}

const responsibilityEvidenceStatusTypeMap: Record<EdhrFieldResponsibilityEvidenceStatus, string> = {
  COMPLETE: 'success',
  EVIDENCE_MISSING: 'warning',
  BLOCKED: 'danger'
}

const responsibilityValueOriginLabelMap: Record<EdhrFieldResponsibilityValueOrigin, string> = {
  HUMAN: '人工填写',
  SYSTEM_BASELINE: '系统基线',
  EMPTY_UNTOUCHED: '空值未填',
  UNKNOWN: '来源未知'
}

const responsibilityReasonCodeLabelMap: Record<EdhrFieldResponsibilityReasonCode, string> = {
  EXECUTION_SNAPSHOT_MISSING: '执行快照缺失',
  FIELD_DEFINITION_MISSING: '字段定义缺失',
  BASELINE_MISSING: '审计基线缺失',
  FIELD_AUDIT_MISSING: '缺少字段审计记录',
  SIGNATURE_MISSING: '缺少签名记录',
  SIGNATURE_INVALID: '签名校验未通过',
  CHAIN_INVALID: '审计链校验未通过',
  CURRENT_VALUE_MISMATCH: '当前值与审计记录不一致',
  FIELD_IDENTITY_AMBIGUOUS: '字段无法唯一匹配',
  CROSS_TENANT_ASSOCIATION: '跨租户关联异常',
  CROSS_EXECUTION_ASSOCIATION: '跨执行记录关联异常'
}

const responsibilityContextWarningLabelMap: Record<EdhrFieldResponsibilityContextWarning, string> = {
  VERSION_CONTEXT_MISSING: '版本上下文缺失'
}

const resolveResponsibilityEvidenceStatusLabel = (status?: string) => {
  return status && status in responsibilityEvidenceStatusLabelMap
    ? responsibilityEvidenceStatusLabelMap[status as EdhrFieldResponsibilityEvidenceStatus]
    : status || '--'
}

const resolveResponsibilityEvidenceStatusType = (status?: string) => {
  return status && status in responsibilityEvidenceStatusTypeMap
    ? responsibilityEvidenceStatusTypeMap[status as EdhrFieldResponsibilityEvidenceStatus]
    : 'info'
}

const resolveResponsibilityValueOriginLabel = (origin?: string) => {
  return origin && origin in responsibilityValueOriginLabelMap
    ? responsibilityValueOriginLabelMap[origin as EdhrFieldResponsibilityValueOrigin]
    : origin || '--'
}

const resolveResponsibilityReasonCodeLabel = (code?: string) => {
  return code && code in responsibilityReasonCodeLabelMap
    ? responsibilityReasonCodeLabelMap[code as EdhrFieldResponsibilityReasonCode]
    : code || '--'
}

const resolveResponsibilityReasonCodesText = (codes?: string[]) => {
  return codes?.length ? codes.map(resolveResponsibilityReasonCodeLabel).join('、') : '--'
}

const resolveResponsibilityContextWarningLabel = (code?: string) => {
  return code && code in responsibilityContextWarningLabelMap
    ? responsibilityContextWarningLabelMap[code as EdhrFieldResponsibilityContextWarning]
    : code || '--'
}

const resolveResponsibilityContextWarningsText = (codes?: string[]) => {
  return codes?.length ? codes.map(resolveResponsibilityContextWarningLabel).join('、') : '--'
}

const resolveVerifyResultTitle = (result: EdhrFieldAuditVerifyRespVO) => {
  const hashStatus = result.hashVerification?.status
  const verifiedCount = result.verifiedCount ?? 0
  if (hashStatus === 'VALID') {
    return `字段审计链校验通过，共校验 ${verifiedCount} 条。`
  }
  return `字段审计链校验未通过：${resolveHashStatusLabel(hashStatus)}，共校验 ${verifiedCount} 条。`
}

const formatJson = (value: unknown) => JSON.stringify(value)

const decodeEdhrFieldAuditExportContent = (exportPayload: EdhrFieldAuditExportRespVO) => {
  const { content } = exportPayload
  if (Array.isArray(content)) {
    if (!content.length) throw new Error('字段审计导出响应 content 为空，无法下载。')
    return Uint8Array.from(content)
  }
  if (typeof content === 'string' && content.trim()) {
    const base64Content = content.includes(',') ? content.slice(content.indexOf(',') + 1) : content
    const binary = window.atob(base64Content)
    if (!binary.length) throw new Error('字段审计导出响应 content 为空，无法下载。')
    const bytes = new Uint8Array(binary.length)
    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index)
    }
    return bytes
  }
  throw new Error('字段审计导出响应缺少 content，无法下载。')
}

const downloadEdhrFieldAuditExport = (exportPayload: EdhrFieldAuditExportRespVO) => {
  if (!exportPayload.fileName?.trim()) throw new Error('字段审计导出响应缺少 fileName，无法下载。')
  if (!exportPayload.contentType?.trim()) throw new Error('字段审计导出响应缺少 contentType，无法下载。')
  const contentBytes = decodeEdhrFieldAuditExportContent(exportPayload)
  const blob = new Blob([contentBytes], { type: exportPayload.contentType })
  const href = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = href
  link.download = exportPayload.fileName
  link.click()
  URL.revokeObjectURL(href)
}

const downloadEdhrFieldResponsibilityExport = (exportPayload: EdhrFieldResponsibilityExportRespVO) => {
  if (!exportPayload.fileName?.trim()) throw new Error('字段责任导出响应缺少 fileName，无法下载。')
  if (!exportPayload.contentType?.trim()) throw new Error('字段责任导出响应缺少 contentType，无法下载。')
  if (!exportPayload.contentBase64?.trim()) throw new Error('字段责任导出响应缺少 contentBase64，无法下载。')
  const binary = window.atob(exportPayload.contentBase64)
  if (!binary.length) throw new Error('字段责任导出响应缺少 contentBase64，无法下载。')
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  const blob = new Blob([bytes], { type: exportPayload.contentType })
  const href = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = href
  link.download = exportPayload.fileName
  link.click()
  URL.revokeObjectURL(href)
}

const buildQuery = (): EdhrFieldAuditPageReqVO => ({
  ...queryParams,
  executionId: parsePositiveRouteQueryId(queryParams.executionId) || undefined,
  auditBatchId: queryParams.auditBatchId?.trim() || undefined,
  fieldPath: queryParams.fieldPath?.trim() || undefined,
  fieldKey: queryParams.fieldKey?.trim() || undefined,
  actorName: queryParams.actorName?.trim() || undefined,
  reasonKeyword: queryParams.reasonKeyword?.trim() || undefined,
  changedAtStart: changedTimeRange.value[0] || undefined,
  changedAtEnd: changedTimeRange.value[1] || undefined
})

const buildResponsibilityQuery = (): EdhrFieldResponsibilitySummaryReqVO => ({
  ...responsibilityQueryParams,
  executionId: parsePositiveRouteQueryId(responsibilityQueryParams.executionId) || 0,
  fieldKeyword: responsibilityQueryParams.fieldKeyword?.trim() || undefined,
  evidenceStatus: responsibilityQueryParams.evidenceStatus || undefined,
  valueOrigin: responsibilityQueryParams.valueOrigin || undefined,
  actorId: Number.isFinite(responsibilityQueryParams.actorId)
    ? responsibilityQueryParams.actorId
    : undefined
})

const getResponsibilitySummary = async () => {
  if (!hasPermission([FIELD_AUDIT_QUERY_PERMISSION])) {
    responsibilityList.value = []
    responsibilityTotal.value = 0
    responsibilitySummary.value = undefined
    responsibilityError.value = '当前账号没有字段审计查询权限。'
    return
  }
  const summaryQuery = buildResponsibilityQuery()
  if (!summaryQuery.executionId) {
    responsibilityList.value = []
    responsibilityTotal.value = 0
    responsibilitySummary.value = undefined
    responsibilityError.value = '缺少执行ID，无法加载字段责任汇总。'
    return
  }
  responsibilityLoading.value = true
  responsibilityError.value = ''
  try {
    const summary = await getEdhrFieldResponsibilitySummary(summaryQuery)
    responsibilitySummary.value = summary
    responsibilityList.value = summary.list || []
    responsibilityTotal.value = summary.total || 0
  } catch (error) {
    responsibilityList.value = []
    responsibilityTotal.value = 0
    responsibilitySummary.value = undefined
    responsibilityError.value = resolveErrorMessage(error, '字段责任汇总加载失败，请联系管理员。')
  } finally {
    responsibilityLoading.value = false
  }
}

const handleResponsibilityQuery = () => {
  responsibilityQueryParams.pageNo = 1
  getResponsibilitySummary()
}

const resetResponsibilityQuery = () => {
  responsibilityQueryParams.pageNo = 1
  responsibilityQueryParams.executionId = queryParams.executionId || 0
  responsibilityQueryParams.fieldKeyword = ''
  responsibilityQueryParams.evidenceStatus = undefined
  responsibilityQueryParams.valueOrigin = undefined
  responsibilityQueryParams.actorId = undefined
  getResponsibilitySummary()
}

const openResponsibilityHistory = async (row: EdhrFieldResponsibilityItemRespVO) => {
  selectedResponsibilityField.value = row
  responsibilityHistory.value = undefined
  responsibilityHistoryDialogVisible.value = true
  responsibilityHistoryLoading.value = true
  responsibilityError.value = ''
  try {
    responsibilityHistory.value = await getEdhrFieldResponsibilityHistory({
      executionId: responsibilityQueryParams.executionId,
      fieldPath: row.fieldPath,
      fieldKey: row.fieldKey,
      rowIndex: row.rowIndex,
      columnIndex: row.columnIndex,
      pageSize: 50
    })
  } catch (error) {
    responsibilityError.value = resolveErrorMessage(error, '字段责任历史加载失败，请联系管理员。')
  } finally {
    responsibilityHistoryLoading.value = false
  }
}

const openResponsibilityAuditDetail = async (row: EdhrFieldResponsibilityItemRespVO) => {
  if (!row.latestAuditItemId) {
    message.error('当前责任行缺少字段审计明细，无法查看审计证据。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-field-audit/detail',
    query: {
      executionId: String(responsibilityQueryParams.executionId),
      auditItemId: String(row.latestAuditItemId)
    }
  })
}

const handleResponsibilityExport = async () => {
  if (!hasPermission([FIELD_AUDIT_EXPORT_PERMISSION])) {
    responsibilityError.value = '当前账号没有字段责任导出权限。'
    message.error(responsibilityError.value)
    return
  }
  const summaryQuery = buildResponsibilityQuery()
  if (!summaryQuery.executionId) {
    responsibilityError.value = '缺少执行ID，无法导出责任证明。'
    message.error(responsibilityError.value)
    return
  }
  responsibilityExportLoading.value = true
  responsibilityError.value = ''
  try {
    const exportPayload = await exportEdhrFieldResponsibility({
      executionId: summaryQuery.executionId,
      format: 'XLSX'
    })
    downloadEdhrFieldResponsibilityExport(exportPayload)
    message.success('责任证明导出已开始')
  } catch (error) {
    responsibilityError.value = resolveErrorMessage(error, '字段责任导出失败，请联系管理员。')
  } finally {
    responsibilityExportLoading.value = false
  }
}
const getList = async () => {
  if (!hasPermission([FIELD_AUDIT_QUERY_PERMISSION])) {
    list.value = []
    total.value = 0
    listLoaded.value = false
    loadError.value = '当前账号没有字段审计查询权限。'
    return
  }
  loading.value = true
  loadError.value = ''
  verifyResult.value = undefined
  try {
    const pageData = await getEdhrFieldAuditPage(buildQuery())
    list.value = pageData.list || []
    total.value = pageData.total || 0
    listLoaded.value = true
  } catch (error) {
    list.value = []
    total.value = 0
    listLoaded.value = false
    loadError.value = resolveErrorMessage(error, '字段审计链加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  responsibilityQueryParams.executionId = queryParams.executionId || 0
  if (activeView.value === 'responsibility') {
    getResponsibilitySummary()
    return
  }
  getList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.executionId = undefined
  queryParams.auditBatchId = undefined
  queryParams.fieldPath = ''
  queryParams.fieldKey = ''
  queryParams.actorName = ''
  queryParams.reasonKeyword = ''
  queryParams.reasonCategory = undefined
  changedTimeRange.value = []
  getList()
}

const handleVerify = async () => {
  const verifyQuery = buildQuery()
  if (!verifyQuery.executionId) {
    verifyResult.value = undefined
    loadError.value = '缺少执行ID，无法校验字段审计链。'
    message.error(loadError.value)
    return
  }
  verifyLoading.value = true
  loadError.value = ''
  try {
    verifyResult.value = await verifyEdhrFieldAuditChain({
      executionId: verifyQuery.executionId,
      includeBrokenItem: true
    })
    if (verifyResult.value.hashVerification.status !== 'VALID') {
      loadError.value = `字段审计链校验未通过：${resolveHashStatusLabel(verifyResult.value.hashVerification.status)}`
      return
    }
    message.success('字段审计链校验通过')
  } catch (error) {
    verifyResult.value = undefined
    loadError.value = resolveErrorMessage(error, '字段审计链校验失败，请联系管理员。')
  } finally {
    verifyLoading.value = false
  }
}

const handleExport = async () => {
  const exportQuery = buildQuery()
  if (!exportQuery.executionId) {
    loadError.value = '缺少执行ID，无法导出字段审计链。'
    message.error(loadError.value)
    return
  }
  exportLoading.value = true
  loadError.value = ''
  try {
    const exportPayload = await exportEdhrFieldAudit({
      ...exportQuery,
      executionId: exportQuery.executionId,
      format: 'XLSX'
    })
    downloadEdhrFieldAuditExport(exportPayload)
    message.success('字段审计链导出已开始')
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '字段审计链导出失败，请联系管理员。')
  } finally {
    exportLoading.value = false
  }
}

const openDetail = async (row: EdhrFieldAuditEntryVO) => {
  await router.push({
    path: '/mes/pro/feedback/edhr-field-audit/detail',
    query: {
      executionId: String(row.executionId),
      auditBatchId: row.auditBatchId ? String(row.auditBatchId) : undefined,
      auditItemId: String(row.id)
    }
  })
}

const openExecution = async (row: EdhrFieldAuditEntryVO) => {
  if (!row.executionId) {
    message.error('当前字段审计记录缺少执行ID，无法打开执行表单。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-execution/form',
    query: { id: String(row.executionId) }
  })
}

watch(activeView, (view) => {
  if (view === 'responsibility') {
    responsibilityQueryParams.executionId = queryParams.executionId || 0
    getResponsibilitySummary()
    return
  }
  getList()
})

watch(
  () => [props.initialExecutionId, props.initialView],
  () => {
    const executionId = resolveInitialExecutionId()
    queryParams.executionId = executionId
    responsibilityQueryParams.executionId = executionId || 0
    activeView.value = resolveInitialView()
    if (activeView.value === 'responsibility') {
      getResponsibilitySummary()
      return
    }
    getList()
  }
)

onMounted(() => {
  if (activeView.value === 'responsibility') {
    getResponsibilitySummary()
    return
  }
  getList()
})
</script>

<style scoped>
.edhr-field-audit--embedded {
  margin: 0;
}

.edhr-field-audit__toolbar,
.edhr-field-audit__table {
  padding: 16px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-field-audit__toolbar {
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  padding-bottom: 0;
}

.edhr-field-audit__table {
  border-radius: 0 0 8px 8px;
}

.edhr-field-audit__advanced {
  display: block;
  width: 100%;
  margin-right: 0;
}

.edhr-field-audit__advanced :deep(.el-form-item__content) {
  width: 100%;
}

.edhr-field-audit__advanced :deep(.el-collapse) {
  width: 100%;
  border-top: 1px solid #edf1f6;
  border-bottom: 0;
}

.edhr-field-audit__advanced :deep(.el-collapse-item__header) {
  min-height: 40px;
  color: #172033;
  font-weight: 600;
}

.edhr-field-audit__advanced-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, max-content));
  gap: 0 12px;
}

.edhr-field-audit__evidence {
  padding: 12px 16px;
  background: #fafcff;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
}

.edhr-field-audit__evidence-title {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-field-audit__evidence-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.edhr-field-audit__evidence-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.edhr-field-audit__evidence-card {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #edf1f6;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-field-audit__evidence-card--wide {
  grid-column: span 2;
}

.edhr-field-audit__evidence-notes {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #edf1f6;
}

.edhr-field-audit__evidence-note-label {
  color: #4b5563;
  font-size: 12px;
  font-weight: 600;
}

.edhr-field-audit__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.edhr-field-audit__technical-details {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #edf1f6;
}

.edhr-field-audit__technical-details > summary {
  color: #2b5f9f;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.edhr-field-audit__technical-details[open] > summary {
  margin-bottom: 10px;
}

.edhr-field-audit__evidence-item {
  min-width: 0;
}

.edhr-field-audit__label {
  color: #4b5563;
  font-size: 12px;
}

.edhr-field-audit__value {
  margin-top: 4px;
  color: #172033;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.edhr-field-audit__table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-field-audit__table :deep(.el-table__row) {
  height: 52px;
}

.edhr-field-audit__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-field-audit__execution-link {
  padding: 0;
  font-weight: 600;
}

.edhr-field-audit__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-field-audit__change-value {
  color: #172033;
  font-weight: 600;
}

.edhr-field-audit__change-arrow {
  margin: 2px 0;
  color: #4b5563;
  font-size: 12px;
}

@media (max-width: 720px) {
  .edhr-field-audit__evidence-card--wide {
    grid-column: span 1;
  }
}
</style>
