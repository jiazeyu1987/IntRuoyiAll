<template>
  <div class="qa-regulation-page" data-qa-regulation-page>
    <ContentWrap>
      <div class="qa-regulation-page__header">
        <div>
          <div class="qa-regulation-page__title">QA 规程配置</div>
          <div class="qa-regulation-page__subtitle">
            QA 负责制定 PQC 的首检、巡检、末检和检验项目规则；PQC 只按已发布规程执行。
          </div>
        </div>
        <el-tag type="warning" effect="plain">{{ qaRegulationDraft.lifecycleStatus }}</el-tag>
      </div>
      <el-alert
        title="正式保存/发布接口未接入，本页调整仅用于前端规则预览和发布前检查，未写入后台。"
        type="warning"
        :closable="false"
        show-icon
        data-qa-regulation-api-blocker
      />
    </ContentWrap>

    <ContentWrap>
      <div class="qa-regulation-page__layout">
        <el-card shadow="never" data-qa-regulation-pressure-pump-source>
          <template #header>压力泵规程来源</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="规程名称">
              按压式球囊扩充压力泵组装过程检验规程
            </el-descriptions-item>
            <el-descriptions-item label="规程编号">PQC-IDI-001</el-descriptions-item>
            <el-descriptions-item label="版本">B/0</el-descriptions-item>
            <el-descriptions-item label="生效日期">2026-01-04</el-descriptions-item>
            <el-descriptions-item label="QA 规程类型">过程检验规程</el-descriptions-item>
            <el-descriptions-item label="识别说明">
              由扫描版 PDF 文件名与封面元数据初始化，检验项目和标准需 QA 复核后发布。
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" data-qa-regulation-scope>
          <template #header>适用范围</template>
          <el-form :model="qaRegulationDraft" label-width="112px" class="qa-regulation-page__form">
            <el-form-item label="规程编号">
              <el-input v-model="qaRegulationDraft.regulationCode" />
            </el-form-item>
            <el-form-item label="规程名称">
              <el-input v-model="qaRegulationDraft.regulationName" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :xs="24" :md="12">
                <el-form-item label="版本">
                  <el-input v-model="qaRegulationDraft.versionNo" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="生效日期">
                  <el-date-picker
                    v-model="qaRegulationDraft.effectiveDate"
                    value-format="YYYY-MM-DD"
                    type="date"
                    class="!w-100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="产品">
              <el-input v-model="qaRegulationDraft.productName" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :xs="24" :md="12">
                <el-form-item label="路线版本">
                  <el-input v-model="qaRegulationDraft.routeVersionName" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="路线工序">
                  <el-input v-model="qaRegulationDraft.routeProcessName" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="SOP">
              <el-input v-model="qaRegulationDraft.sopName" placeholder="请输入正式 SOP 或作业指导书" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :xs="24" :md="12">
                <el-form-item label="生产系数">
                  <el-input-number
                    v-model="qaRegulationDraft.productionFactor"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    class="!w-100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="示例订单数">
                  <el-input-number
                    v-model="qaRegulationDraft.sampleOrderQuantity"
                    :min="1"
                    :controls="false"
                    class="!w-100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="批记录绑定">
              <el-input
                v-model="qaRegulationDraft.batchRecordBinding"
                placeholder="请输入当前工序正式批记录绑定"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </ContentWrap>

    <ContentWrap>
      <el-card shadow="never" data-qa-regulation-inspection-rules>
        <template #header>
          <div class="qa-regulation-page__card-head">
            <span>检验类型规则</span>
            <div class="qa-regulation-page__rule-tags" data-qa-regulation-rule-types>
              <el-tag effect="plain">首检</el-tag>
              <el-tag effect="plain">上午巡检</el-tag>
              <el-tag effect="plain">下午巡检</el-tag>
              <el-tag effect="plain">末检</el-tag>
            </div>
          </div>
        </template>
        <el-table :data="qaInspectionTypeRules" border size="small">
          <el-table-column label="规则" min-width="120">
            <template #default="{ row }">
              <div class="qa-regulation-page__rule-name">{{ row.label }}</div>
              <div class="qa-regulation-page__hint">{{ row.roundLabel }}</div>
            </template>
          </el-table-column>
          <el-table-column label="是否适用" width="110">
            <template #default="{ row }">
              <el-switch v-model="row.required" />
            </template>
          </el-table-column>
          <el-table-column label="固定数量" width="140">
            <template #default="{ row }">
              <el-input-number
                v-model="row.fixedQuantity"
                :disabled="!row.required || row.sampleRatio !== undefined"
                :min="0"
                :controls="false"
                class="!w-100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="抽样比例" width="140">
            <template #default="{ row }">
              <el-input-number
                v-model="row.sampleRatio"
                :disabled="!row.required || row.fixedQuantity !== undefined"
                :min="0"
                :max="100"
                :precision="1"
                :controls="false"
                class="!w-100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="PQC 计划数量" width="150">
            <template #default="{ row }">
              <el-tag effect="plain">{{ formatQaRulePlannedQuantity(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="任务生成规则" min-width="240" prop="taskRule" />
          <el-table-column label="发布门禁" min-width="240" prop="releaseGate" />
        </el-table>
        <div class="qa-regulation-page__hint mt-8px">
          巡检示例：{{ qaRegulationDraft.sampleOrderQuantity }} × 5% =
          {{ Math.ceil(qaRegulationDraft.sampleOrderQuantity * 0.05) }}，按向上取整生成 PQC 任务。
        </div>
      </el-card>
    </ContentWrap>

    <ContentWrap>
      <el-card shadow="never" data-qa-regulation-items>
        <template #header>
          <div class="qa-regulation-page__card-head">
            <span>检验项目与判定标准</span>
            <el-button type="primary" plain @click="addQaRegulationItem">新增项目</el-button>
          </div>
        </template>
        <el-table :data="qaRegulationItems" border size="small">
          <el-table-column label="项目编码" width="130">
            <template #default="{ row }">
              <el-input v-model="row.itemCode" />
            </template>
          </el-table-column>
          <el-table-column label="项目" min-width="170">
            <template #default="{ row }">
              <el-input v-model="row.itemName" />
            </template>
          </el-table-column>
          <el-table-column label="适用类型" min-width="210">
            <template #default="{ row }">
              <el-select v-model="row.applicableTypes" multiple collapse-tags collapse-tags-tooltip>
                <el-option
                  v-for="option in qaInspectionTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="方法" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.inspectionMethod" />
            </template>
          </el-table-column>
          <el-table-column label="工具" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.inspectionTool" />
            </template>
          </el-table-column>
          <el-table-column label="结果类型" width="130">
            <template #default="{ row }">
              <el-select v-model="row.resultType">
                <el-option label="合格/不合格" value="BOOLEAN" />
                <el-option label="数值" value="NUMERIC" />
                <el-option label="文本" value="TEXT" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="标准" min-width="240">
            <template #default="{ row }">
              <el-input v-model="row.standardText" />
            </template>
          </el-table-column>
          <el-table-column label="原文依据" min-width="420">
            <template #default="{ row }">
              <div class="qa-regulation-page__source" data-qa-regulation-original-excerpt>
                <div class="qa-regulation-page__source-meta">
                  <el-tag size="small" type="info" effect="plain">
                    PDF 第 {{ row.sourceOriginalPage || '待补充' }} 页
                  </el-tag>
                  <span>{{ row.sourceOriginalItem || '待补充原文项目' }}</span>
                </div>
                <div class="qa-regulation-page__source-label">接受标准原文</div>
                <div class="qa-regulation-page__source-text">
                  {{ row.sourceOriginalExcerpt || 'QA 手工新增项目需补充对应 PDF/规程原文摘录。' }}
                </div>
                <template v-if="row.sourceOriginalMethod">
                  <div class="qa-regulation-page__source-label">检验方法原文</div>
                  <div class="qa-regulation-page__source-text">
                    {{ row.sourceOriginalMethod }}
                  </div>
                </template>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="下限" width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.lowerLimit"
                :disabled="row.resultType !== 'NUMERIC'"
                :controls="false"
                class="!w-100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="上限" width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.upperLimit"
                :disabled="row.resultType !== 'NUMERIC'"
                :controls="false"
                class="!w-100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="关键项" width="100">
            <template #default="{ row }">
              <el-checkbox v-model="row.critical">关键</el-checkbox>
            </template>
          </el-table-column>
          <el-table-column label="失败规则" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row.failureRule" />
            </template>
          </el-table-column>
          <el-table-column label="来源说明" min-width="200" prop="sourceNote" />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeQaRegulationItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </ContentWrap>

    <ContentWrap>
      <div class="qa-regulation-page__layout">
        <el-card shadow="never" data-qa-regulation-completeness>
          <template #header>发布完整性检查</template>
          <div class="qa-regulation-page__check-list">
            <div
              v-for="check in qaRegulationCompletenessChecks"
              :key="check.key"
              class="qa-regulation-page__check"
              :class="{ 'is-passed': check.passed }"
            >
              <el-tag :type="check.passed ? 'success' : 'danger'" effect="plain">
                {{ check.passed ? '已满足' : '需补齐' }}
              </el-tag>
              <div>
                <div class="qa-regulation-page__check-title">{{ check.label }}</div>
                <div class="qa-regulation-page__hint">{{ check.detail }}</div>
              </div>
            </div>
          </div>
          <div class="qa-regulation-page__actions">
            <el-button @click="previewQaRegulationDraft">保存草稿预览</el-button>
            <el-button type="primary" @click="runQaPublishPrecheck">发布前检查</el-button>
          </div>
        </el-card>

        <el-card shadow="never" data-qa-pqc-task-preview>
          <template #header>PQC 任务预览</template>
          <el-table :data="qaPqcTaskPreviewRows" border size="small">
            <el-table-column label="检验类型" prop="inspectionTypeText" min-width="110" />
            <el-table-column label="轮次" prop="roundText" min-width="110" />
            <el-table-column label="计划数量" prop="plannedQuantityText" min-width="110" />
            <el-table-column label="规程版本" prop="regulationVersionNo" min-width="110" />
            <el-table-column label="任务身份" prop="taskIdentity" min-width="260" />
          </el-table>
          <el-alert
            class="mt-12px"
            title="PQC 任务必须来自 QA 发布规程快照；缺产品、路线、工序、规则或项目时阻塞生成。"
            type="info"
            :closable="false"
            show-icon
          />
        </el-card>
      </div>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'

defineOptions({ name: 'MesProProcessPoolQaRegulation' })

type QaInspectionTypeValue = 'FIRST' | 'PATROL_AM' | 'PATROL_PM' | 'FINAL'
type QaInspectionResultType = 'BOOLEAN' | 'NUMERIC' | 'TEXT'

interface QaInspectionTypeRule {
  key: QaInspectionTypeValue
  inspectionType: 'FIRST' | 'PATROL' | 'FINAL'
  label: string
  roundLabel: string
  required: boolean
  fixedQuantity?: number
  sampleRatio?: number
  taskRule: string
  releaseGate: string
}

interface QaRegulationItem {
  itemCode: string
  itemName: string
  applicableTypes: QaInspectionTypeValue[]
  inspectionMethod: string
  inspectionTool: string
  resultType: QaInspectionResultType
  standardText: string
  lowerLimit?: number
  upperLimit?: number
  critical: boolean
  failureRule: string
  sourceNote: string
  sourceOriginalPage?: number
  sourceOriginalItem?: string
  sourceOriginalExcerpt?: string
  sourceOriginalMethod?: string
}

const qaInspectionTypeOptions: Array<{ label: string; value: QaInspectionTypeValue }> = [
  { label: '首检', value: 'FIRST' },
  { label: '上午巡检', value: 'PATROL_AM' },
  { label: '下午巡检', value: 'PATROL_PM' },
  { label: '末检', value: 'FINAL' }
]

const qaRegulationDraft = reactive({
  regulationCode: 'PQC-IDI-001',
  regulationName: '按压式球囊扩充压力泵组装过程检验规程',
  versionNo: 'B/0',
  effectiveDate: '2026-01-04',
  lifecycleStatus: 'DRAFT',
  productName: '按压式球囊扩充压力泵',
  routeVersionName: '正式路线版本待选择',
  routeProcessName: '组装过程检验工序',
  sopName: '按压式球囊扩充压力泵组装 SOP',
  productionFactor: 1,
  sampleOrderQuantity: 301,
  batchRecordBinding: '当前工序正式批记录绑定待选择'
})

const qaInspectionTypeRules = reactive<QaInspectionTypeRule[]>([
  {
    key: 'FIRST',
    inspectionType: 'FIRST',
    label: '首检',
    roundLabel: '每个适用订单工序开始前',
    required: true,
    fixedQuantity: 5,
    taskRule: '按发布规程固定数量生成首检任务',
    releaseGate: '缺固定数量或项目时不能发布'
  },
  {
    key: 'PATROL_AM',
    inspectionType: 'PATROL',
    label: '上午巡检',
    roundLabel: '上午班次独立轮次',
    required: true,
    sampleRatio: 5,
    taskRule: '按订单数量 × 上午比例向上取整',
    releaseGate: '上午比例需独立配置'
  },
  {
    key: 'PATROL_PM',
    inspectionType: 'PATROL',
    label: '下午巡检',
    roundLabel: '下午班次独立轮次',
    required: true,
    sampleRatio: 5,
    taskRule: '按订单数量 × 下午比例向上取整',
    releaseGate: '下午比例需独立配置'
  },
  {
    key: 'FINAL',
    inspectionType: 'FINAL',
    label: '末检',
    roundLabel: '订单工序结束前',
    required: true,
    fixedQuantity: 3,
    taskRule: '需要末检时生成末检任务；不适用必须显式关闭',
    releaseGate: '需要/不适用必须明确保存'
  }
])

const qaRegulationItems = ref<QaRegulationItem[]>([
  {
    itemCode: 'PP-APP',
    itemName: '外观确认',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '目视检查',
    inspectionTool: '目视/照明',
    resultType: 'BOOLEAN',
    standardText: '外观完整，无明显污渍、破损、变形',
    critical: false,
    failureRule: '任一件不符合则记录不合格并进入复核',
    sourceNote: '由压力泵过程检验规程初始化，QA 可编辑确认',
    sourceOriginalPage: 6,
    sourceOriginalItem: '整体粘结 / 外套组件与套筒组件装配 / 外观',
    sourceOriginalExcerpt:
      '压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；压力泵内腔无异物、毛丝等活动异物；压力泵外套应有足够的透明度，能清晰地看到基准线；压力泵的第一条刻度线（泵体排空时）应与活塞重合。',
    sourceOriginalMethod:
      '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-ASM',
    itemName: '装配完整性',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '逐件核对',
    inspectionTool: '装配清单',
    resultType: 'BOOLEAN',
    standardText: '球囊、管路、接头和压力泵主体装配齐全',
    critical: true,
    failureRule: '关键部件缺失或装配错误时整件判定不合格',
    sourceNote: '由压力泵过程检验规程初始化，QA 可编辑确认',
    sourceOriginalPage: 6,
    sourceOriginalItem: '外套组件与套筒组件装配 / 配合',
    sourceOriginalExcerpt:
      '推杆组件推入外套，后盖与外套的卡槽扣到位，旋转后盖使得后盖与外套的缺口完全一致，不能偏掉；旋转螺杆检查扭力不应偏大，按下按钮推拉螺杆看应无干涉及推拉力偏大。',
    sourceOriginalMethod: '目测、手感。'
  },
  {
    itemCode: 'PP-SEAL',
    itemName: '密封/泄漏确认',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '保压观察',
    inspectionTool: '压力源/水槽',
    resultType: 'BOOLEAN',
    standardText: '连接处无可见泄漏，保压过程无异常下降',
    critical: true,
    failureRule: '发现泄漏即判定不合格并触发质量异常',
    sourceNote: '由压力泵过程检验规程初始化，QA 可编辑确认',
    sourceOriginalPage: 7,
    sourceOriginalItem: '整体粘结 / 气密性 / 负压检测',
    sourceOriginalExcerpt: '负压检测：抽负压-80±5kpa，不应有泄漏。',
    sourceOriginalMethod:
      '将粘接完成 12 小时后的压力泵接上气密性检测工装，抽负压-80±5kpa，观察有无泄漏。'
  },
  {
    itemCode: 'PP-PRESS',
    itemName: '压力显示/保压确认',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '读数比对',
    inspectionTool: '标准压力表',
    resultType: 'NUMERIC',
    standardText: '压力显示与标准表差异在 QA 设定范围内',
    lowerLimit: -0.05,
    upperLimit: 0.05,
    critical: true,
    failureRule: '超出上下限即判定不合格',
    sourceNote: '由压力泵过程检验规程初始化，QA 可编辑确认',
    sourceOriginalPage: 3,
    sourceOriginalItem: '组装螺杆八组件 / 无跳压',
    sourceOriginalExcerpt:
      '20atm 压力打至 20atm 应无跳压现象；30atm 压力打至 30atm 应无跳压现象；40atm 压力泵需打压至 40atm 无跳压现象。',
    sourceOriginalMethod:
      '将推杆装到检测专用的泵筒(吸入 10ML 水)上，将压力打至 20atm/30atm/40atm 应无跳压现象。'
  },
  {
    itemCode: 'PP-LABEL',
    itemName: '判定规则与记录确认',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '记录核对',
    inspectionTool: '过程检验记录',
    resultType: 'BOOLEAN',
    standardText: '每一个检验项目均应合格，并形成对应过程检验记录',
    critical: false,
    failureRule: '任一检验项目不合格时按判定规则处理，不得直接放行',
    sourceNote: '由压力泵过程检验规程初始化，QA 可编辑确认',
    sourceOriginalPage: 8,
    sourceOriginalItem: '5.2 判定规则 / 7. 相关记录',
    sourceOriginalExcerpt:
      '检验中，每一个检验项目均应合格。若出现不合格，则进行不合格品评审并按照不合格评审结果处理。',
    sourceOriginalMethod:
      '记录编号 RE-PQC-IDI-001-01，记录名称：按压式球囊扩充压力泵组装过程检验记录。'
  }
])

const resolveQaRulePlannedQuantity = (rule: QaInspectionTypeRule) => {
  if (!rule.required) return 0
  if (Number.isFinite(Number(rule.fixedQuantity)) && Number(rule.fixedQuantity) > 0) {
    return Number(rule.fixedQuantity)
  }
  if (Number.isFinite(Number(rule.sampleRatio)) && Number(rule.sampleRatio) > 0) {
    return Math.ceil((qaRegulationDraft.sampleOrderQuantity * Number(rule.sampleRatio)) / 100)
  }
  return 0
}

const formatQaRulePlannedQuantity = (rule: QaInspectionTypeRule) => {
  if (!rule.required) return '不适用'
  const quantity = resolveQaRulePlannedQuantity(rule)
  return quantity > 0 ? `${quantity} 件` : '需补齐'
}

const qaRegulationCompletenessChecks = computed(() => {
  const scopeReady = Boolean(
    qaRegulationDraft.productName.trim() &&
      qaRegulationDraft.routeVersionName.trim() &&
      qaRegulationDraft.routeProcessName.trim()
  )
  const versionReady = Boolean(
    qaRegulationDraft.regulationCode.trim() &&
      qaRegulationDraft.regulationName.trim() &&
      qaRegulationDraft.versionNo.trim() &&
      qaRegulationDraft.effectiveDate
  )
  const ruleReady = qaInspectionTypeRules.every(
    (rule) => !rule.required || resolveQaRulePlannedQuantity(rule) > 0
  )
  const itemReady =
    qaRegulationItems.value.length > 0 &&
    qaRegulationItems.value.every(
      (item) =>
        item.itemCode.trim() &&
        item.itemName.trim() &&
        item.applicableTypes.length > 0 &&
        item.inspectionMethod.trim() &&
        item.inspectionTool.trim() &&
        item.resultType &&
        item.standardText.trim() &&
        item.failureRule.trim()
    )
  const numericLimitReady = qaRegulationItems.value.every(
    (item) =>
      item.resultType !== 'NUMERIC' ||
      (Number.isFinite(Number(item.lowerLimit)) && Number.isFinite(Number(item.upperLimit)))
  )
  const sourceExcerptReady = qaRegulationItems.value.every(
    (item) =>
      Number.isFinite(Number(item.sourceOriginalPage)) &&
      Boolean(item.sourceOriginalItem?.trim()) &&
      Boolean(item.sourceOriginalExcerpt?.trim())
  )
  return [
    {
      key: 'scope',
      label: '产品/路线/工序范围',
      passed: scopeReady,
      detail: scopeReady ? '已指定适用产品、路线版本和路线工序' : '需补齐产品、路线版本和路线工序'
    },
    {
      key: 'version',
      label: '规程版本信息',
      passed: versionReady,
      detail: versionReady ? '编号、名称、版本和生效日期已填写' : '需补齐编号、名称、版本或生效日期'
    },
    {
      key: 'rules',
      label: '首检/巡检/末检规则',
      passed: ruleReady,
      detail: ruleReady ? '适用的检验类型均有数量或比例' : '适用检验类型缺少固定数量或抽样比例'
    },
    {
      key: 'items',
      label: '检验项目字段',
      passed: itemReady,
      detail: itemReady ? '项目、方法、工具、标准和失败规则齐全' : '需补齐检验项目、方法、工具、标准或失败规则'
    },
    {
      key: 'numeric-limits',
      label: '数值上下限',
      passed: numericLimitReady,
      detail: numericLimitReady ? '数值类项目已有上下限' : '数值类项目必须填写上下限'
    },
    {
      key: 'source-excerpts',
      label: '原文依据摘录',
      passed: sourceExcerptReady,
      detail: sourceExcerptReady
        ? '每个检验项目均已关联 PDF 页码和相关原文摘录'
        : '每个检验项目都必须补齐对应 PDF 页码、原文项目和相关原文摘录'
    }
  ]
})

const qaPublishBlockers = computed(() =>
  qaRegulationCompletenessChecks.value.filter((check) => !check.passed)
)

const qaPqcTaskPreviewRows = computed(() =>
  qaInspectionTypeRules.map((rule) => ({
    inspectionTypeText: rule.label.includes('巡检') ? '巡检' : rule.label,
    roundText: rule.roundLabel,
    plannedQuantityText: formatQaRulePlannedQuantity(rule),
    regulationVersionNo: qaRegulationDraft.versionNo || '--',
    taskIdentity: `${qaRegulationDraft.productName || '--'} / ${
      qaRegulationDraft.routeProcessName || '--'
    } / ${rule.key}`
  }))
)

const addQaRegulationItem = () => {
  const nextIndex = qaRegulationItems.value.length + 1
  qaRegulationItems.value.push({
    itemCode: `QA-ITEM-${String(nextIndex).padStart(2, '0')}`,
    itemName: '新增检验项目',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM'],
    inspectionMethod: '',
    inspectionTool: '',
    resultType: 'BOOLEAN',
    standardText: '',
    critical: false,
    failureRule: '',
    sourceNote: 'QA 手工新增，发布前需确认',
    sourceOriginalItem: '',
    sourceOriginalExcerpt: '',
    sourceOriginalMethod: ''
  })
}

const removeQaRegulationItem = (index: number) => {
  qaRegulationItems.value.splice(index, 1)
}

const previewQaRegulationDraft = () => {
  ElMessage.info('已更新前端草稿预览；正式保存/发布接口未接入，未写入后台。')
}

const runQaPublishPrecheck = () => {
  if (qaPublishBlockers.value.length > 0) {
    ElMessage.warning(`发布前仍有 ${qaPublishBlockers.value.length} 项规则需补齐`)
    return
  }
  ElMessage.info('发布前检查已通过；正式保存/发布接口未接入，未写入后台。')
}
</script>

<style scoped>
.qa-regulation-page {
  display: grid;
  gap: 16px;
}

.qa-regulation-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.qa-regulation-page__title {
  color: #172033;
  font-size: 20px;
  font-weight: 700;
}

.qa-regulation-page__subtitle,
.qa-regulation-page__hint {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.qa-regulation-page__layout {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 16px;
}

.qa-regulation-page__form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.qa-regulation-page__card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.qa-regulation-page__rule-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.qa-regulation-page__rule-name {
  color: #172033;
  font-weight: 700;
}

.qa-regulation-page__source {
  display: grid;
  gap: 6px;
  padding: 8px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.qa-regulation-page__source-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  color: #172033;
  font-size: 12px;
  font-weight: 700;
}

.qa-regulation-page__source-label {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.qa-regulation-page__source-text {
  color: #172033;
  font-size: 12px;
  line-height: 1.55;
  white-space: normal;
}

.qa-regulation-page__check-list {
  display: grid;
  gap: 10px;
}

.qa-regulation-page__check {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
  padding: 10px;
  border: 1px solid #f2c6c6;
  border-radius: 8px;
  background: #fff7f7;
}

.qa-regulation-page__check.is-passed {
  border-color: #b7e1c0;
  background: #f5fff7;
}

.qa-regulation-page__check-title {
  color: #172033;
  font-weight: 700;
}

.qa-regulation-page__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

@media (max-width: 1180px) {
  .qa-regulation-page__layout {
    grid-template-columns: 1fr;
  }
}
</style>
