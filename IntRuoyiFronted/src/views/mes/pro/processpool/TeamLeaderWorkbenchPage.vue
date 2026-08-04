<template>
  <ContentWrap>
    <div class="team-leader-workbench__header">
      <div>
        <div class="team-leader-workbench__title">工序池班组长工作台</div>
        <div class="team-leader-workbench__subtitle">
          负责报工确认、活跃订单分配、异常上报和班组配置中心维护
        </div>
      </div>
    </div>

    <el-tabs
      v-model="activeLeaderTab"
      data-team-leader-type-tabs
      @tab-change="handleLeaderTypeChange"
    >
      <el-tab-pane label="生产组长" name="PRODUCTION" />
      <el-tab-pane label="PQC 组长" name="PQC" />
      <el-tab-pane label="QA 规程" name="QA" />
    </el-tabs>
  </ContentWrap>

  <ContentWrap v-if="activeLeaderTab !== 'QA' && loadError">
      <el-alert :title="loadError" type="error" :closable="false" show-icon />
    </ContentWrap>

    <ContentWrap v-if="activeLeaderTab === 'QA'" data-qa-regulation-tab>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">QA 规程配置</div>
          <div class="team-leader-workbench__hint">
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

      <div class="team-leader-workbench__qa-layout">
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
          <el-form :model="qaRegulationDraft" label-width="112px" class="team-leader-workbench__form">
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

      <el-card shadow="never" class="team-leader-workbench__qa-card" data-qa-regulation-inspection-rules>
        <template #header>
          <div class="team-leader-workbench__qa-card-head">
            <span>检验类型规则</span>
            <div class="team-leader-workbench__qa-rule-tags" data-qa-regulation-rule-types>
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
              <div class="team-leader-workbench__qa-rule-name">{{ row.label }}</div>
              <div class="team-leader-workbench__hint">{{ row.roundLabel }}</div>
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
        <div class="team-leader-workbench__hint mt-8px">
          巡检示例：{{ qaRegulationDraft.sampleOrderQuantity }} × 5% =
          {{ Math.ceil(qaRegulationDraft.sampleOrderQuantity * 0.05) }}，按向上取整生成 PQC 任务。
        </div>
      </el-card>

      <el-card shadow="never" class="team-leader-workbench__qa-card" data-qa-regulation-items>
        <template #header>
          <div class="team-leader-workbench__qa-card-head">
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
              <div class="team-leader-workbench__qa-source" data-qa-regulation-original-excerpt>
                <div class="team-leader-workbench__qa-source-meta">
                  <el-tag size="small" type="info" effect="plain">
                    PDF 第 {{ row.sourceOriginalPage || '待补充' }} 页
                  </el-tag>
                  <span>{{ row.sourceOriginalItem || '待补充原文项目' }}</span>
                </div>
                <div class="team-leader-workbench__qa-source-label">接受标准原文</div>
                <div class="team-leader-workbench__qa-source-text">
                  {{ row.sourceOriginalExcerpt || 'QA 手工新增项目需补充对应 PDF/规程原文摘录。' }}
                </div>
                <template v-if="row.sourceOriginalMethod">
                  <div class="team-leader-workbench__qa-source-label">检验方法原文</div>
                  <div class="team-leader-workbench__qa-source-text">
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

      <div class="team-leader-workbench__qa-layout">
        <el-card shadow="never" data-qa-regulation-completeness>
          <template #header>发布完整性检查</template>
          <div class="team-leader-workbench__qa-check-list">
            <div
              v-for="check in qaRegulationCompletenessChecks"
              :key="check.key"
              class="team-leader-workbench__qa-check"
              :class="{ 'is-passed': check.passed }"
            >
              <el-tag :type="check.passed ? 'success' : 'danger'" effect="plain">
                {{ check.passed ? '已满足' : '需补齐' }}
              </el-tag>
              <div>
                <div class="team-leader-workbench__qa-check-title">{{ check.label }}</div>
                <div class="team-leader-workbench__hint">{{ check.detail }}</div>
              </div>
            </div>
          </div>
          <div class="team-leader-workbench__qa-actions">
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

    <ContentWrap v-if="activeLeaderTab !== 'QA'" data-team-leader-report-workbench>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">报工确认工作台</div>
          <div class="team-leader-workbench__hint">
            查看员工结构化报工，确认后按 FIFO 或手动分配到活跃订单。
          </div>
        </div>
      </div>
      <el-form
        ref="queryFormRef"
        class="team-leader-workbench__query"
        :model="queryParams"
        :inline="true"
        label-width="88px"
      >
        <el-form-item label="提交日期" prop="submitDate">
          <el-date-picker
            v-model="queryParams.submitDate"
            value-format="YYYY-MM-DD"
            type="date"
            placeholder="请选择提交日期"
            class="!w-180px"
          />
        </el-form-item>
        <el-form-item :label="employeeFilterLabel" prop="employeeUserId">
          <el-input-number
            v-model="queryParams.employeeUserId"
            :min="1"
            :controls="false"
            placeholder="员工编号"
            class="!w-180px"
          />
        </el-form-item>
        <el-form-item label="工序" prop="processId">
          <el-input-number
            v-model="queryParams.processId"
            :min="1"
            :controls="false"
            placeholder="工序编号"
            class="!w-180px"
          />
        </el-form-item>
        <el-form-item label="模板类型" prop="templateType">
          <el-select
            v-model="queryParams.templateType"
            clearable
            filterable
            placeholder="请选择模板"
            class="!w-190px"
          >
            <el-option label="生产简化模板" value="PRODUCTION_SIMPLIFIED" />
            <el-option label="PQC 简化模板" value="PQC_SIMPLIFIED" />
          </el-select>
        </el-form-item>
        <el-form-item label="生产工单" prop="workOrderCode">
          <el-input
            v-model="queryParams.workOrderCode"
            clearable
            placeholder="工单编码"
            class="!w-220px"
          />
        </el-form-item>
        <template v-if="activeLeaderTab === 'PQC'">
          <el-form-item label="产品" prop="productKeyword">
            <el-input
              v-model="queryParams.productKeyword"
              clearable
              placeholder="产品编码/名称"
              class="!w-220px"
              data-pqc-leader-filter-product
            />
          </el-form-item>
          <el-form-item label="检验类型" prop="inspectionType">
            <el-select
              v-model="queryParams.inspectionType"
              clearable
              placeholder="检验类型"
              class="!w-160px"
              data-pqc-leader-filter-inspection-type
            >
              <el-option label="首检" value="FIRST" />
              <el-option label="巡检" value="PATROL" />
              <el-option label="末检" value="FINAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="轮次" prop="roundNo">
            <el-input-number
              v-model="queryParams.roundNo"
              :min="1"
              :controls="false"
              placeholder="轮次"
              class="!w-140px"
              data-pqc-leader-filter-round
            />
          </el-form-item>
          <el-form-item label="复核状态" prop="submissionReviewStatus">
            <el-select
              v-model="queryParams.submissionReviewStatus"
              clearable
              placeholder="复核状态"
              class="!w-160px"
              data-pqc-leader-filter-review-status
            >
              <el-option label="待判定" value="PENDING" />
              <el-option label="正确" value="APPROVED" />
              <el-option label="不正确" value="REJECTED" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-5px" />
            搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-5px" />
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="submissionList" border stripe>
        <el-table-column label="提交时间" prop="submittedAt" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column :label="employeeColumnLabel" min-width="140">
          <template #default="{ row }">
            {{ row.actualEmployeeUserName || row.actualEmployeeUserId || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="工序" min-width="150">
          <template #default="{ row }">{{ row.processName || row.processCode || '--' }}</template>
        </el-table-column>
        <el-table-column label="生产工单" min-width="160">
          <template #default="{ row }">{{ row.workOrderCode || '--' }}</template>
        </el-table-column>
        <el-table-column v-if="activeLeaderTab === 'PQC'" label="产品" min-width="180">
          <template #default="{ row }">
            <span data-pqc-leader-submission-product>
              {{ row.productCode || row.productName || '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column v-if="activeLeaderTab === 'PQC'" label="检验类型/轮次" min-width="150">
          <template #default="{ row }">
            <span data-pqc-leader-submission-task>
              {{ resolvePqcInspectionTypeText(row.inspectionType) }} / 第 {{ row.roundNo || '--' }} 轮
            </span>
          </template>
        </el-table-column>
        <el-table-column label="PQC" min-width="130">
          <template #default="{ row }">
            <el-tag :type="resolvePqcTagType(row.pqcResult)" effect="plain">
              {{ row.pqcSummary || row.pqcResult || '--' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交内容" min-width="220">
          <template #default="{ row }">
            <div
              v-if="isPqcSubmissionRow(row)"
              class="team-leader-workbench__pqc-content"
              data-pqc-leader-submission-content
            >
              <div
                v-for="item in resolvePqcSubmissionContentItems(row)"
                :key="item.key"
                class="team-leader-workbench__pqc-content-item"
                :data-pqc-leader-submission-entry="item.key"
              >
                <span class="team-leader-workbench__pqc-content-label">{{ item.label }}</span>
                <span class="team-leader-workbench__pqc-content-value">{{ item.valueText }}</span>
              </div>
            </div>
            <template v-else>{{ resolveProductionSubmissionSummary(row) }}</template>
          </template>
        </el-table-column>
        <el-table-column label="审核副本" min-width="130">
          <template #default="{ row }">{{ row.auditCopyStatus || '--' }}</template>
        </el-table-column>
        <el-table-column v-if="activeLeaderTab === 'PQC'" label="过程检验汇集" min-width="180">
          <template #default="{ row }">
            <div
              class="team-leader-workbench__review-log"
              data-pqc-process-inspection-aggregation
              :data-pqc-process-inspection-event-id="String(row.id)"
            >
              <el-tag
                :type="resolveProcessInspectionAggregationTagType(row.processInspectionAggregationStatus)"
                effect="plain"
              >
                {{ resolveProcessInspectionAggregationStatusText(row.processInspectionAggregationStatus) }}
              </el-tag>
              <span
                v-if="row.processInspectionReviewId"
                class="team-leader-workbench__review-meta"
              >
                复核 {{ row.processInspectionReviewId }} ·
                {{ formatDateTime(row.processInspectionAggregatedAt) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="复核判定" min-width="190">
          <template #default="{ row }">
            <div class="team-leader-workbench__review-log" data-team-leader-review-log>
              <el-tag :type="resolveSubmissionReviewTagType(row.submissionReviewStatus)" effect="plain">
                {{ resolveSubmissionReviewStatusText(row.submissionReviewStatus) }}
              </el-tag>
              <span v-if="row.submissionReviewRemark" class="team-leader-workbench__review-text">
                {{ row.submissionReviewRemark }}
              </span>
              <span v-if="row.submissionReviewedAt" class="team-leader-workbench__review-meta">
                复核人 {{ row.submissionReviewLeaderUserId || '--' }} ·
                {{ formatDateTime(row.submissionReviewedAt) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :data-team-leader-detail-event-id="String(row.id)"
              @click="openDetail(row)"
            >
              详情
            </el-button>
            <el-button
              link
              type="success"
              :data-team-leader-review-event-id="String(row.id)"
              @click="openReview(row)"
            >
              复核
            </el-button>
            <el-button
              link
              type="warning"
              :data-team-leader-correction-event-id="String(row.id)"
              @click="openCorrection(row)"
            >
              修正
            </el-button>
            <el-button v-if="isProductionLeader" link type="warning" @click="prefillAbnormal(row)">
              标记异常
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        :total="submissionTotal"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getSubmissionList"
      />
    </ContentWrap>

    <ContentWrap v-if="activeLeaderTab !== 'QA'" data-role-matrix-daily-close>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">日结待处理看板</div>
          <div class="team-leader-workbench__hint">
            汇总当前筛选范围内真实报工、复核和活跃订单状态，日结前未关闭项必须先处理。
          </div>
        </div>
        <el-tag :type="dailyCloseStatusType" effect="dark" data-role-matrix-daily-close-status>
          {{ dailyCloseStatusText }}
        </el-tag>
      </div>
      <div class="team-leader-workbench__daily-close-grid" data-role-matrix-daily-close-summary>
        <el-card
          v-for="item in dailyCloseSummaryCards"
          :key="item.key"
          shadow="never"
          class="team-leader-workbench__daily-close-card"
          :data-role-matrix-daily-close-card="item.key"
        >
          <div class="team-leader-workbench__daily-close-label">{{ item.label }}</div>
          <div class="team-leader-workbench__daily-close-value">{{ item.value }}</div>
          <div class="team-leader-workbench__daily-close-hint">{{ item.hint }}</div>
        </el-card>
      </div>
      <el-alert
        v-if="loadError"
        :title="`日结阻塞：${loadError}`"
        type="error"
        :closable="false"
        show-icon
      />
      <el-alert
        v-else-if="dailyCloseOpenItemCount > 0"
        :title="`日结前仍有 ${dailyCloseOpenItemCount} 项待处理，请先完成复核或异常闭环。`"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-alert
        v-else
        title="当前筛选范围没有未关闭项，可进入后续日结核对。"
        type="success"
        :closable="false"
        show-icon
      />
    </ContentWrap>

    <ContentWrap v-if="isProductionLeader" data-team-leader-abnormal-report>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">订单异常上报</div>
          <div class="team-leader-workbench__hint">
            异常订单来自活跃订单池，异常原因来自当前工序配置。
          </div>
        </div>
      </div>
      <el-form
        ref="abnormalFormRef"
        :model="abnormalForm"
        :rules="abnormalRules"
        label-width="120px"
        class="team-leader-workbench__form"
      >
        <el-form-item label="活跃订单" prop="activeOrderId" data-team-leader-active-order-select>
          <el-select
            v-model="abnormalForm.activeOrderId"
            filterable
            placeholder="请选择活跃订单"
            @change="handleAbnormalActiveOrderChange"
          >
            <el-option
              v-for="order in activeOrderOptions"
              :key="order.id"
              :label="formatActiveOrderOption(order)"
              :value="order.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="工序ID" prop="processId">
          <el-input-number v-model="abnormalForm.processId" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item
          label="异常原因"
          prop="abnormalReasonCode"
          data-team-leader-defect-reason-select
        >
          <el-select
            v-model="abnormalForm.abnormalReasonCode"
            filterable
            allow-create
            placeholder="请选择当前工序允许的异常原因"
          >
            <el-option
              v-for="reason in configuredDefectReasonOptions"
              :key="reason.reasonCode"
              :label="reason.reasonName"
              :value="reason.reasonCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="异常说明" prop="abnormalDescription">
          <el-input
            v-model="abnormalForm.abnormalDescription"
            type="textarea"
            :rows="4"
            placeholder="请输入异常说明"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="abnormalSubmitting" @click="submitAbnormal">
            <Icon icon="ep:warning-filled" class="mr-5px" />
            标记并上报
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap v-if="isProductionLeader" data-team-leader-config-center>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">班组配置中心</div>
          <div class="team-leader-workbench__hint">
            维护员工、设备、参数、活跃订单和工序关系，员工端填报从这里读取配置。
          </div>
        </div>
      </div>
      <div class="team-leader-workbench__maintenance-grid">
        <el-card shadow="never" data-team-leader-active-order-config>
          <template #header>活跃订单池</template>
          <el-form :model="activeOrderForm" label-width="98px">
            <el-form-item label="生产订单ID">
              <el-input-number v-model="activeOrderForm.workOrderId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="路线ID" data-team-leader-active-order-route-id>
              <el-input-number v-model="activeOrderForm.routeId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="路线版本ID" data-team-leader-active-order-route-version-id>
              <el-input-number v-model="activeOrderForm.routeVersionId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="调拨单ID列表" data-team-leader-active-order-transfer-ids>
              <el-input
                v-model="activeOrderForm.transferIdsText"
                clearable
                placeholder="多个 ID 用逗号或空格分隔"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitAddActiveOrder">
                加入活跃订单
              </el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <el-form :model="activeOrderRemoveForm" label-width="98px">
            <el-form-item label="活跃记录ID">
              <el-input-number
                v-model="activeOrderRemoveForm.activeOrderId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="danger"
                plain
                :loading="maintenanceSubmitting"
                @click="submitRemoveActiveOrder"
              >
                移出活跃订单
              </el-button>
            </el-form-item>
          </el-form>
          <div class="team-leader-workbench__hint">
            当前活跃订单：{{ activeOrderOptions.length }} 个
          </div>
          <el-divider>调拨库存追溯</el-divider>
          <el-alert
            v-if="activeOrderTransferTraceError"
            :title="activeOrderTransferTraceError"
            type="error"
            :closable="false"
            show-icon
            data-team-leader-active-order-transfer-trace-error
          />
          <el-table
            v-else
            :data="activeOrderTransferTraceRows"
            v-loading="activeOrderTransferTraceLoading"
            size="small"
            border
            class="team-leader-workbench__transfer-trace"
            empty-text="暂无正式调拨/发货/补料/退料追溯"
            data-team-leader-active-order-transfer-trace
          >
            <el-table-column label="活跃池" width="76">
              <template #default="{ row }">
                <span data-transfer-trace-active-order-id>{{ row.activeOrderId }}</span>
              </template>
            </el-table-column>
            <el-table-column label="来源类型" min-width="92">
              <template #default="{ row }">
                <span data-transfer-trace-source-type>{{ row.sourceType }}</span>
              </template>
            </el-table-column>
            <el-table-column label="来源单号" min-width="116">
              <template #default="{ row }">
                <span data-transfer-trace-source-object-code>
                  {{ row.sourceObjectCode || row.sourceObjectId || '-' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="88">
              <template #default="{ row }">
                <span data-transfer-trace-source-status>{{ row.sourceStatus || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="数量" min-width="82">
              <template #default="{ row }">
                <span data-transfer-trace-quantity>{{ formatTraceQuantity(row.quantity) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="库存ID" min-width="86">
              <template #default="{ row }">
                <span data-transfer-trace-material-stock-id>{{ row.materialStockId || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="批次ID" min-width="86">
              <template #default="{ row }">
                <span data-transfer-trace-batch-id>{{ row.batchId || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="幂等键" min-width="160">
              <template #default="{ row }">
                <span data-transfer-trace-idempotency-key>{{ row.idempotencyKey }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" data-team-leader-employee-config>
          <template #header>员工档案与工序员工</template>
          <el-form :model="employeeProfileForm" label-width="108px">
            <el-form-item label="员工编号">
              <el-input v-model="employeeProfileForm.employeeCode" />
            </el-form-item>
            <el-form-item label="员工姓名">
              <el-input v-model="employeeProfileForm.employeeName" />
            </el-form-item>
            <el-form-item label="员工类型">
              <el-select v-model="employeeProfileForm.employeeType">
                <el-option label="正式员工" value="FORMAL" />
                <el-option label="临时工" value="TEMPORARY" />
              </el-select>
            </el-form-item>
            <el-form-item label="系统用户ID">
              <el-input-number v-model="employeeProfileForm.systemUserId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitEmployeeProfile">
                新增员工
              </el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <el-form :model="processEmployeeBindingForm" label-width="108px">
            <el-form-item label="工序ID">
              <el-input-number
                v-model="processEmployeeBindingForm.processId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item label="员工档案ID">
              <el-input-number
                v-model="processEmployeeBindingForm.employeeProfileId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="maintenanceSubmitting"
                @click="submitProcessEmployeeBinding"
              >
                绑定工序员工
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" data-team-leader-device-config>
          <template #header>设备档案与状态</template>
          <el-form :model="teamDeviceForm" label-width="98px">
            <el-form-item label="设备编号">
              <el-input v-model="teamDeviceForm.deviceCode" />
            </el-form-item>
            <el-form-item label="设备名称">
              <el-input v-model="teamDeviceForm.deviceName" />
            </el-form-item>
            <el-form-item label="设备状态">
              <el-select v-model="teamDeviceForm.deviceStatus">
                <el-option label="启用" value="ENABLED" />
                <el-option label="报修" value="REPAIRING" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitTeamDevice">
                新增设备
              </el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <el-form :model="teamDeviceStatusForm" label-width="98px">
            <el-form-item label="设备ID">
              <el-input-number v-model="teamDeviceStatusForm.deviceId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="teamDeviceStatusForm.deviceStatus">
                <el-option label="启用" value="ENABLED" />
                <el-option label="报修" value="REPAIRING" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="maintenanceSubmitting" @click="submitTeamDeviceStatus">
                更新状态
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" data-team-leader-process-relation-config>
          <template #header>工序设备与异常关系</template>
          <el-form :model="processDeviceBindingForm" label-width="108px">
            <el-form-item label="工序ID">
              <el-input-number
                v-model="processDeviceBindingForm.processId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item label="设备ID">
              <el-input-number
                v-model="processDeviceBindingForm.deviceId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitProcessDeviceBinding">
                绑定工序设备
              </el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <el-form :model="defectReasonForm" label-width="108px">
            <el-form-item label="工序ID">
              <el-input-number v-model="defectReasonForm.processId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="原因类型">
              <el-select v-model="defectReasonForm.reasonType">
                <el-option label="损耗" value="LOSS" />
                <el-option label="不合格" value="UNQUALIFIED" />
                <el-option label="PQC 失败" value="PQC_FAILURE" />
              </el-select>
            </el-form-item>
            <el-form-item label="原因编码">
              <el-input v-model="defectReasonForm.reasonCode" />
            </el-form-item>
            <el-form-item label="原因名称">
              <el-input v-model="defectReasonForm.reasonName" />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="maintenanceSubmitting"
                @click="submitProcessDefectReason"
              >
                保存工序异常原因
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" data-team-leader-parameter-config>
          <template #header>设备参数维护</template>
          <el-form :model="deviceRuleForm" label-width="98px">
            <el-form-item label="工序ID">
              <el-input-number v-model="deviceRuleForm.processId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="设备ID">
              <el-input-number v-model="deviceRuleForm.deviceId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="参数编码">
              <el-input v-model="deviceRuleForm.parameterCode" />
            </el-form-item>
            <el-form-item label="参数名称">
              <el-input v-model="deviceRuleForm.parameterName" />
            </el-form-item>
            <el-form-item label="单位">
              <el-input v-model="deviceRuleForm.unit" />
            </el-form-item>
            <el-form-item label="下限">
              <el-input-number v-model="deviceRuleForm.lowerLimit" :controls="false" />
            </el-form-item>
            <el-form-item label="上限">
              <el-input-number v-model="deviceRuleForm.upperLimit" :controls="false" />
            </el-form-item>
            <el-form-item label="默认值">
              <el-input-number v-model="deviceRuleForm.defaultValue" :controls="false" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitRuntimeDeviceRule">
                保存参数
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </ContentWrap>

    <el-drawer v-model="detailVisible" :title="detailDrawerTitle" size="620px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="1" border data-team-leader-structured-detail>
          <el-descriptions-item label="服务端提交时间">
            {{ formatDateTime(detail.submittedAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="employeeDetailLabel">
            {{ detail.actualEmployeeUserName || detail.actualEmployeeUserId || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="工序">
            {{ detail.processName || detail.processCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="生产工单">
            {{ detail.workOrderCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="提交摘要">
            {{ detail.submittedSummary || '--' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.pqcResult || detail.pqcSummary" label="PQC检验内容">
            <el-tag :type="resolvePqcTagType(detail.pqcResult)" effect="plain">
              {{ detail.pqcSummary || detail.pqcResult }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail && isPqcSubmissionRow(detail)" label="PQC项目明细">
            <el-table
              :data="resolvePqcItemSnapshotDetails(detail)"
              border
              size="small"
              data-pqc-leader-item-snapshot-table
              empty-text="PQC提交内容缺少正式项目明细"
            >
              <el-table-column label="检验项目" min-width="120">
                <template #default="{ row }">{{ row.itemName || row.itemCode || '--' }}</template>
              </el-table-column>
              <el-table-column label="检验设备" min-width="140">
                <template #default="{ row }">
                  {{ row.selectedEquipmentName || row.selectedEquipmentCode || '--' }}
                </template>
              </el-table-column>
              <el-table-column label="设备编号" prop="selectedEquipmentNumber" min-width="130" />
              <el-table-column label="接收标准" min-width="180">
                <template #default="{ row }">{{ formatPqcSnapshotStandard(row) }}</template>
              </el-table-column>
              <el-table-column label="检验方法" prop="inspectionMethod" min-width="180" />
              <el-table-column label="样本值" min-width="180">
                <template #default="{ row }">{{ formatPqcSnapshotSampleValues(row) }}</template>
              </el-table-column>
              <el-table-column label="判定" min-width="100">
                <template #default="{ row }">{{ row.judgement || row.itemResult || '--' }}</template>
              </el-table-column>
            </el-table>
          </el-descriptions-item>
          <el-descriptions-item label="结构化报工内容">
            <el-table
              :data="resolveStructuredPayloadItems(detail.originalPayloadJson)"
              border
              size="small"
              empty-text="暂无结构化字段"
            >
              <el-table-column label="字段" prop="field" min-width="160" />
              <el-table-column label="值" prop="value" min-width="220" />
            </el-table>
          </el-descriptions-item>
        </el-descriptions>
        <div
          v-if="detail && isPqcSubmissionRow(detail)"
          class="team-leader-workbench__submission-log"
          data-pqc-submission-log
        >
          <div class="team-leader-workbench__submission-log-title">PQC提交日志</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="提交事件编号">
              {{ detail.id || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="PQC检验员">
              {{ detail.actualEmployeeUserName || detail.actualEmployeeUserId || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="服务端提交时间">
              {{ formatDateTime(detail.submittedAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="签名编号">
              <span data-pqc-submission-signature-id>
                {{ detail.electronicSignatureId || '--' }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="原始提交内容">
              <pre class="team-leader-workbench__payload" data-pqc-submission-original-payload>{{
                detail.originalPayloadJson || '--'
              }}</pre>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="reviewVisible" title="复核员工提交" width="760px">
      <el-form :model="reviewForm" label-width="92px">
        <el-form-item label="判定结果">
          <el-select v-model="reviewForm.reviewStatus">
            <el-option label="正确" value="APPROVED" />
            <el-option label="不正确" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="复核说明">
          <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="复核签名ID" data-team-leader-review-signature>
          <el-input-number
            v-model="reviewForm.reviewSignatureId"
            :min="1"
            :controls="false"
            class="team-leader-workbench__number"
          />
        </el-form-item>
        <el-form-item label="签名员工ID">
          <el-input-number
            v-model="reviewForm.reviewSignatureEmployeeUserId"
            :min="1"
            :controls="false"
            class="team-leader-workbench__number"
          />
        </el-form-item>
        <el-form-item label="签名快照">
          <el-input
            v-model="reviewForm.reviewSignatureSnapshotJson"
            type="textarea"
            :rows="3"
            resize="vertical"
            placeholder="请输入电子签名快照 JSON 或签名服务返回引用"
          />
        </el-form-item>
      </el-form>
      <div
        v-if="isProductionLeader && reviewForm.reviewStatus === 'APPROVED'"
        class="team-leader-workbench__allocation"
      >
        <div class="team-leader-workbench__allocation-toolbar">
          <div>
            <div class="team-leader-workbench__section-title">活跃订单分配</div>
            <div class="team-leader-workbench__hint">
              可先按 FIFO 自动分配，再根据现场情况手动调整。
            </div>
          </div>
          <div>
            <el-button
              data-team-leader-fifo-allocation
              type="primary"
              plain
              :loading="allocationPreviewLoading"
              @click="previewFifoAllocation"
            >
              FIFO 自动分配
            </el-button>
            <el-button @click="addAllocationLine">新增分配行</el-button>
          </div>
        </div>
        <el-table
          data-team-leader-allocation-table
          :data="allocationRows"
          border
          size="small"
          empty-text="请点击 FIFO 自动分配或手动新增分配行"
        >
          <el-table-column label="活跃订单" min-width="220">
            <template #default="{ row }">
              <el-select
                v-model="row.activeOrderId"
                filterable
                placeholder="请选择活跃订单"
                @change="markManualAllocation"
              >
                <el-option
                  v-for="order in activeOrderOptions"
                  :key="order.id"
                  :label="formatActiveOrderOption(order)"
                  :value="order.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="分配数量" width="180">
            <template #default="{ row }">
              <el-input-number
                v-model="row.allocatedQuantity"
                :min="0"
                :precision="3"
                :controls="false"
                class="!w-140px"
                @change="markManualAllocation"
              />
            </template>
          </el-table-column>
          <el-table-column label="FIFO 剩余" width="140">
            <template #default="{ row }">
              {{ row.remainingQuantityBeforeAllocation ?? '--' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeAllocationLine($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="team-leader-workbench__hint mt-8px">
          当前分配模式：{{ reviewForm.allocationMode }}
        </div>
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview"
          >提交复核</el-button
        >
      </template>
    </el-dialog>

    <el-dialog v-model="correctionVisible" title="修正不正确内容" width="760px" destroy-on-close>
      <el-alert
        title="修正将调用原始记录修改接口，系统会记录修改前、修改后、原因、修改人、签名和字段差异日志。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form class="team-leader-workbench__correction-form" :model="correctionForm" label-width="150px">
        <el-form-item label="提交事件编号">
          <el-input-number
            v-model="correctionForm.eventId"
            :min="1"
            :controls="false"
            disabled
            class="team-leader-workbench__number"
          />
        </el-form-item>
        <el-form-item label="修改原因">
          <el-input v-model="correctionForm.changeReason" maxlength="500" show-word-limit />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :md="8">
            <el-form-item label="修改人用户ID">
              <el-input-number
                v-model="correctionForm.modifiedByUserId"
                :min="1"
                :controls="false"
                class="team-leader-workbench__number"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="修正签名ID">
              <el-input-number
                v-model="correctionForm.revisionSignatureId"
                :min="1"
                :controls="false"
                class="team-leader-workbench__number"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="签名用户ID">
              <el-input-number
                v-model="correctionForm.revisionSignatureUserId"
                :min="1"
                :controls="false"
                class="team-leader-workbench__number"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="修改后payload JSON">
          <el-input v-model="correctionForm.afterPayloadJson" type="textarea" :rows="8" resize="vertical" />
        </el-form-item>
        <el-form-item label="修正签名快照JSON">
          <el-input
            v-model="correctionForm.revisionSignatureSnapshotJson"
            type="textarea"
            :rows="4"
            resize="vertical"
          />
        </el-form-item>
        <el-form-item label="字段变更JSON">
          <el-input
            v-model="correctionForm.changedFieldsJson"
            type="textarea"
            :rows="8"
            resize="vertical"
            placeholder="请输入非空数组，逐项记录 fieldCode/fieldName/beforeValue/afterValue/affectsQuantityFragment/originalField"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="correctionVisible = false">取消</el-button>
        <el-button type="primary" :loading="correctionSubmitting" @click="submitCorrection">
          提交修正并记录日志
        </el-button>
      </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  addTeamLeaderActiveOrder,
  confirmTeamLeaderReportAllocation,
  createTeamDevice,
  createTeamEmployeeProfile,
  getTeamLeaderActiveOrderList,
  getTeamLeaderActiveOrderTransferTrace,
  getTeamLeaderSubmissionDetail,
  getTeamLeaderSubmissionPage,
  markAndReportWorkOrderAbnormal,
  previewTeamLeaderReportFifoAllocation,
  removeTeamLeaderActiveOrder,
  reviewTeamLeaderSubmission,
  saveTeamProcessDefectReason,
  saveTeamProcessDeviceBinding,
  saveTeamProcessEmployeeBinding,
  saveTeamRuntimeDeviceParameterRule,
  updateTeamDeviceStatus,
  type TeamLeaderActiveOrderRespVO,
  type TeamLeaderActiveOrderTransferTraceRespVO,
  type TeamLeaderReportAllocationLine,
  type TeamLeaderSubmissionPageReqVO,
  type TeamLeaderType
} from '@/api/mes/pro/processpool/teamLeader'
import type {
  ProcessPoolTimelineDetailVO,
  ProcessPoolTimelineEventVO
} from '@/api/mes/pro/processpool'
import {
  updateProcessPoolOriginalRecord,
  type ProcessPoolEventRevisionFieldChangeVO
} from '@/api/mes/pro/processpool/eventRevision'
import { formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'MesProProcessPoolTeamLeaderWorkbench' })

type WorkbenchLeaderTab = TeamLeaderType | 'QA'
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

const queryFormRef = ref()
const abnormalFormRef = ref()
const activeLeaderTab = ref<WorkbenchLeaderTab>('PRODUCTION')
const loading = ref(false)
const detailLoading = ref(false)
const reviewSubmitting = ref(false)
const allocationPreviewLoading = ref(false)
const abnormalSubmitting = ref(false)
const maintenanceSubmitting = ref(false)
const correctionSubmitting = ref(false)
const detailVisible = ref(false)
const reviewVisible = ref(false)
const correctionVisible = ref(false)
const loadError = ref('')
const submissionTotal = ref(0)
const submissionList = ref<ProcessPoolTimelineEventVO[]>([])
const detail = ref<ProcessPoolTimelineDetailVO>()
const reviewEvent = ref<ProcessPoolTimelineEventVO>()
const correctionEvent = ref<ProcessPoolTimelineEventVO>()
const activeOrderOptions = ref<TeamLeaderActiveOrderRespVO[]>([])
const activeOrderTransferTraceRows = ref<TeamLeaderActiveOrderTransferTraceRespVO[]>([])
const activeOrderTransferTraceLoading = ref(false)
const activeOrderTransferTraceError = ref('')
const allocationRows = ref<TeamLeaderReportAllocationLine[]>([])
const configuredDefectReasonOptions = ref<
  Array<{ reasonType: string; reasonCode: string; reasonName: string }>
>([])

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

const isProductionLeader = computed(() => activeLeaderTab.value === 'PRODUCTION')
const employeeFilterLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员' : '员工'
)
const employeeColumnLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员' : '员工'
)
const employeeDetailLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员' : '实际员工'
)
const detailDrawerTitle = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员提交详情' : '员工提交详情'
)
const dailyClosePendingReviewCount = computed(
  () =>
    submissionList.value.filter(
      (row) => !row.submissionReviewStatus || row.submissionReviewStatus === 'PENDING'
    ).length
)
const dailyCloseRejectedCount = computed(
  () => submissionList.value.filter((row) => row.submissionReviewStatus === 'REJECTED').length
)
const dailyCloseOpenItemCount = computed(
  () => dailyClosePendingReviewCount.value + dailyCloseRejectedCount.value + (loadError.value ? 1 : 0)
)
const dailyCloseStatusType = computed(() =>
  loadError.value || dailyCloseOpenItemCount.value > 0 ? 'warning' : 'success'
)
const dailyCloseStatusText = computed(() => {
  if (loadError.value) return '加载阻塞'
  return dailyCloseOpenItemCount.value > 0 ? '待处理' : '可日结'
})
const dailyCloseSummaryCards = computed(() => [
  {
    key: 'pending-review',
    label: '待复核提交',
    value: dailyClosePendingReviewCount.value,
    hint: '来自当前筛选提交列表，未判定记录不得日结'
  },
  {
    key: 'rejected-review',
    label: '复核不正确',
    value: dailyCloseRejectedCount.value,
    hint: '复核退回后需先修正或重新确认'
  },
  {
    key: 'active-orders',
    label: '活跃订单',
    value: activeOrderOptions.value.length,
    hint: '来自活跃订单池，日结前需确认分配与异常状态'
  },
  {
    key: 'load-blocker',
    label: '加载阻塞',
    value: loadError.value ? 1 : 0,
    hint: loadError.value || '当前看板数据已加载'
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

const queryParams = reactive<TeamLeaderSubmissionPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  leaderType: 'PRODUCTION',
  submitDate: new Date().toISOString().slice(0, 10),
  employeeUserId: undefined,
  processId: undefined,
  deviceId: undefined,
  templateType: undefined,
  workOrderId: undefined,
  workOrderCode: undefined,
  productId: undefined,
  productKeyword: undefined,
  inspectionType: undefined,
  roundNo: undefined,
  submissionReviewStatus: undefined
})

const reviewForm = reactive({
  reviewStatus: 'APPROVED' as 'APPROVED' | 'REJECTED',
  allocationMode: 'FIFO' as 'FIFO' | 'MANUAL',
  reviewRemark: '',
  reviewSignatureId: undefined as number | undefined,
  reviewSignatureEmployeeUserId: undefined as number | undefined,
  reviewSignatureSnapshotJson: ''
})

const correctionForm = reactive({
  eventId: undefined as number | undefined,
  modifiedByUserId: undefined as number | undefined,
  revisionSignatureId: undefined as number | undefined,
  revisionSignatureUserId: undefined as number | undefined,
  changeReason: '',
  afterPayloadJson: '',
  revisionSignatureSnapshotJson: '',
  changedFieldsJson: ''
})

const abnormalForm = reactive({
  activeOrderId: undefined as number | undefined,
  workOrderId: undefined as number | undefined,
  routeProcessId: undefined as number | undefined,
  processId: undefined as number | undefined,
  sourceEventId: undefined as number | undefined,
  abnormalReasonCode: '',
  abnormalDescription: ''
})

const activeOrderForm = reactive({
  workOrderId: undefined as number | undefined,
  routeId: undefined as number | undefined,
  routeVersionId: undefined as number | undefined,
  transferIdsText: ''
})

const activeOrderRemoveForm = reactive({
  activeOrderId: undefined as number | undefined
})

const employeeProfileForm = reactive({
  systemUserId: undefined as number | undefined,
  employeeCode: '',
  employeeName: '',
  employeeType: 'TEMPORARY'
})

const processEmployeeBindingForm = reactive({
  processId: undefined as number | undefined,
  employeeProfileId: undefined as number | undefined
})

const teamDeviceForm = reactive({
  deviceCode: '',
  deviceName: '',
  deviceStatus: 'ENABLED' as 'ENABLED' | 'REPAIRING' | 'DISABLED'
})

const teamDeviceStatusForm = reactive({
  deviceId: undefined as number | undefined,
  deviceStatus: 'REPAIRING' as 'ENABLED' | 'REPAIRING' | 'DISABLED'
})

const processDeviceBindingForm = reactive({
  processId: undefined as number | undefined,
  deviceId: undefined as number | undefined
})

const defectReasonForm = reactive({
  processId: undefined as number | undefined,
  reasonType: 'LOSS',
  reasonCode: '',
  reasonName: ''
})

const deviceRuleForm = reactive({
  processId: undefined as number | undefined,
  deviceId: undefined as number | undefined,
  parameterCode: '',
  parameterName: '',
  unit: '',
  lowerLimit: undefined as number | undefined,
  upperLimit: undefined as number | undefined,
  defaultValue: undefined as number | undefined,
  valueType: 'DECIMAL'
})

const abnormalRules = {
  activeOrderId: [{ required: true, message: '活跃订单不能为空', trigger: 'change' }],
  abnormalReasonCode: [{ required: true, message: '异常原因不能为空', trigger: 'blur' }],
  abnormalDescription: [{ required: true, message: '异常说明不能为空', trigger: 'blur' }]
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage =
    (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const normalizePositiveNumber = (value?: number) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const requirePositiveNumber = (value: unknown, message: string) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(message)
  }
  return parsed
}

const normalizeFiniteNumber = (value?: number) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const requireFiniteNumber = (value: unknown, message: string) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    throw new Error(message)
  }
  return parsed
}

const parsePositiveIntegerList = (value: string, label: string) => {
  const text = value.trim()
  if (!text) return []
  return text.split(/[,\s，]+/).filter(Boolean).map((item) => {
    const parsed = Number(item)
    if (!Number.isInteger(parsed) || parsed <= 0) {
      throw new Error(`${label}只能包含大于 0 的整数 ID`)
    }
    return parsed
  })
}

const formatActiveOrderOption = (order: TeamLeaderActiveOrderRespVO) => {
  return `订单 ${order.workOrderId} / 活跃池 ${order.id}`
}

const formatTraceQuantity = (value: number | string | undefined) => {
  if (value === undefined || value === null || value === '') return '-'
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed.toFixed(3) : String(value)
}

const resetReviewAllocation = () => {
  reviewForm.allocationMode = 'FIFO'
  allocationRows.value = []
}

const loadActiveOrderTransferTraces = async () => {
  activeOrderTransferTraceError.value = ''
  activeOrderTransferTraceRows.value = []
  const activeOrders = activeOrderOptions.value.filter((order) => normalizePositiveNumber(order.id))
  if (activeOrders.length === 0) {
    return
  }
  activeOrderTransferTraceLoading.value = true
  try {
    const traceGroups = await Promise.all(
      activeOrderOptions.value.map((order) => getTeamLeaderActiveOrderTransferTrace(order.id))
    )
    activeOrderTransferTraceRows.value = traceGroups.flat()
  } catch (error) {
    activeOrderTransferTraceError.value = resolveErrorMessage(error, '活跃订单调拨库存追溯加载失败')
    activeOrderTransferTraceRows.value = []
    throw error
  } finally {
    activeOrderTransferTraceLoading.value = false
  }
}

const loadActiveOrders = async () => {
  activeOrderOptions.value = await getTeamLeaderActiveOrderList()
  await loadActiveOrderTransferTraces()
}

const markManualAllocation = () => {
  reviewForm.allocationMode = 'MANUAL'
}

const addAllocationLine = () => {
  reviewForm.allocationMode = 'MANUAL'
  allocationRows.value.push({
    activeOrderId: activeOrderOptions.value[0]?.id ?? 0,
    allocatedQuantity: 0
  })
}

const removeAllocationLine = (index: number) => {
  reviewForm.allocationMode = 'MANUAL'
  allocationRows.value.splice(index, 1)
}

const previewFifoAllocation = async () => {
  const eventId = requirePositiveNumber(reviewEvent.value?.id, '工序池提交事件编号不能为空')
  allocationPreviewLoading.value = true
  try {
    const preview = await previewTeamLeaderReportFifoAllocation({
      eventId,
      leaderType: queryParams.leaderType as TeamLeaderType
    })
    reviewForm.allocationMode = 'FIFO'
    allocationRows.value = (preview.lines || []).map((line) => ({
      activeOrderId: line.activeOrderId,
      workOrderId: line.workOrderId,
      workOrderCode: line.workOrderCode,
      allocatedQuantity: line.allocatedQuantity,
      remainingQuantityBeforeAllocation: line.remainingQuantityBeforeAllocation
    }))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'FIFO 自动分配失败'))
  } finally {
    allocationPreviewLoading.value = false
  }
}

const buildAllocationSubmitLines = (): TeamLeaderReportAllocationLine[] => {
  const lines = allocationRows.value.map((line) => ({
    activeOrderId: requirePositiveNumber(line.activeOrderId, '活跃订单不能为空'),
    allocatedQuantity: requirePositiveNumber(line.allocatedQuantity, '分配数量必须大于 0')
  }))
  if (lines.length === 0) {
    throw new Error('生产组长确认报工前必须分配到活跃订单')
  }
  return lines
}

const buildReviewSignaturePayload = () => ({
  reviewSignatureId: requirePositiveNumber(reviewForm.reviewSignatureId, '复核电子签名不能为空'),
  reviewSignatureEmployeeUserId: requirePositiveNumber(
    reviewForm.reviewSignatureEmployeeUserId,
    '复核签名员工不能为空'
  ),
  reviewSignatureSnapshotJson: reviewForm.reviewSignatureSnapshotJson.trim() || undefined
})

function parseJsonField<T>(value: string, label: string): T {
  if (!value || !value.trim()) {
    throw new Error(`${label}不能为空`)
  }
  try {
    return JSON.parse(value) as T
  } catch (error) {
    throw new Error(`${label}必须是合法 JSON`)
  }
}

const normalizePayloadJsonForCorrection = (payloadJson?: string) => {
  const text = payloadJson?.trim()
  if (!text) {
    throw new Error('原始payload缺失，不能发起修正')
  }
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch (error) {
    throw new Error('原始payload不是合法 JSON，不能发起修正')
  }
}

type PqcSubmissionContentItemKey = string

interface PqcSubmissionContentDefinition {
  key: PqcSubmissionContentItemKey
  label: string
  unit?: string
}

interface PqcSubmissionContentItem extends PqcSubmissionContentDefinition {
  valueText: string
}

type PqcSubmissionPayloadRecord = Record<string, unknown>

interface PqcItemSnapshotDetail {
  itemCode?: string
  itemName?: string
  selectedEquipmentId?: number
  selectedEquipmentCode?: string
  selectedEquipmentName?: string
  selectedEquipmentNumber?: string
  standardText?: string
  standardLowerLimit?: number | string
  standardUpperLimit?: number | string
  standardUnit?: string
  standardPrecision?: number
  inspectionMethod?: string
  resultType?: string
  sampleValues?: string[]
  itemResult?: string
  judgement?: string
}

const PQC_SUBMISSION_CONTENT_MISSING_ITEMS: PqcSubmissionContentItem[] = [
  {
    key: 'missing',
    label: 'PQC明细',
    valueText: 'PQC提交内容缺少正式项目明细'
  }
]

const isRecord = (value: unknown): value is PqcSubmissionPayloadRecord =>
  Boolean(value) && typeof value === 'object' && !Array.isArray(value)

const parsePqcOriginalPayload = (payloadJson?: string) => {
  const text = payloadJson?.trim()
  if (!text) {
    return undefined
  }
  try {
    const parsed = JSON.parse(text)
    return isRecord(parsed) ? parsed : undefined
  } catch (error) {
    console.warn('PQC提交原始payload解析失败', error)
    return undefined
  }
}

const isPqcSubmissionRow = (row: ProcessPoolTimelineEventVO) =>
  String(row.templateType || '').includes('PQC') || activeLeaderTab.value === 'PQC'

const readPqcPayloadField = (payload: PqcSubmissionPayloadRecord, key: string) => {
  const draft = isRecord(payload.pqcDraft) ? payload.pqcDraft : undefined
  return draft?.[key] ?? payload[key]
}

const normalizePqcSubmittedValues = (value: unknown): string[] => {
  if (Array.isArray(value)) {
    return value.map((item) => String(item ?? '').trim()).filter(Boolean)
  }
  if (isRecord(value)) {
    for (const nestedKey of ['values', 'pieceValues', 'results', 'value']) {
      const nestedValues = normalizePqcSubmittedValues(value[nestedKey])
      if (nestedValues.length) {
        return nestedValues
      }
    }
    return []
  }
  if (value === undefined || value === null) {
    return []
  }
  const text = String(value).trim()
  return text ? [text] : []
}

const toPqcItemSnapshotDetail = (value: unknown): PqcItemSnapshotDetail | undefined => {
  if (!isRecord(value)) {
    return undefined
  }
  const detail: PqcItemSnapshotDetail = {
    itemCode: String(value.itemCode ?? '').trim() || undefined,
    itemName: String(value.itemName ?? '').trim() || undefined,
    selectedEquipmentId: Number(value.selectedEquipmentId) || undefined,
    selectedEquipmentCode: String(value.selectedEquipmentCode ?? '').trim() || undefined,
    selectedEquipmentName: String(value.selectedEquipmentName ?? '').trim() || undefined,
    selectedEquipmentNumber: String(value.selectedEquipmentNumber ?? '').trim() || undefined,
    standardText: String(value.standardText ?? '').trim() || undefined,
    standardLowerLimit: value.standardLowerLimit as number | string | undefined,
    standardUpperLimit: value.standardUpperLimit as number | string | undefined,
    standardUnit: String(value.standardUnit ?? '').trim() || undefined,
    standardPrecision: Number(value.standardPrecision) || undefined,
    inspectionMethod: String(value.inspectionMethod ?? '').trim() || undefined,
    resultType: String(value.resultType ?? '').trim() || undefined,
    sampleValues: normalizePqcSubmittedValues(
      value.sampleValues ?? value.samples ?? value.values ?? value.measuredValue
    ),
    itemResult: String(value.itemResult ?? '').trim() || undefined,
    judgement: String(value.judgement ?? '').trim() || undefined
  }
  return detail.itemCode || detail.itemName ? detail : undefined
}

const normalizePqcItemSnapshotDetails = (value: unknown): PqcItemSnapshotDetail[] => {
  const sourceItems = Array.isArray(value)
    ? value
    : isRecord(value)
      ? Object.values(value)
      : []
  return sourceItems
    .map(toPqcItemSnapshotDetail)
    .filter((item): item is PqcItemSnapshotDetail => Boolean(item))
}

const resolvePqcPayloadPair = (row: ProcessPoolTimelineEventVO) => {
  const payload = parsePqcOriginalPayload(row.originalPayloadJson)
  const rootPayload = payload && isRecord(payload.rawPayload) ? payload.rawPayload : payload
  return { payload, rootPayload }
}

const resolvePqcItemSnapshotDetails = (row: ProcessPoolTimelineEventVO) => {
  const { payload, rootPayload } = resolvePqcPayloadPair(row)
  const sources = [
    rootPayload?.pqcItemDetails,
    payload?.pqcItemDetails,
    rootPayload?.itemResults,
    payload?.itemResults
  ]
  for (const source of sources) {
    const details = normalizePqcItemSnapshotDetails(source)
    if (details.length) {
      return details
    }
  }
  return []
}

const formatPqcSnapshotSampleValues = (detail: PqcItemSnapshotDetail) =>
  detail.sampleValues?.length ? detail.sampleValues.join('、') : '未填写'

const formatPqcSnapshotStandard = (detail: PqcItemSnapshotDetail) => {
  const lower = detail.standardLowerLimit
  const upper = detail.standardUpperLimit
  const unit = detail.standardUnit || ''
  const range = lower !== undefined || upper !== undefined
    ? `${lower ?? '--'} ~ ${upper ?? '--'}${unit}`
    : ''
  return [detail.standardText, range].filter(Boolean).join('；') || '未配置'
}

const resolvePqcInspectionTypeText = (value: unknown) => {
  if (value === 'FIRST') return '首检'
  if (value === 'PATROL') return '巡检'
  if (value === 'FINAL') return '末检'
  return String(value ?? '').trim()
}

const resolvePqcSubmissionOverviewItem = (
  payload: PqcSubmissionPayloadRecord
): PqcSubmissionContentItem | undefined => {
  const inspectionType = resolvePqcInspectionTypeText(readPqcPayloadField(payload, 'inspectionType'))
  const patrolRound = readPqcPayloadField(payload, 'patrolRound')
  const inspectionQuantity = readPqcPayloadField(payload, 'inspectionQuantity')
  const scrapQuantity = readPqcPayloadField(payload, 'scrapQuantity')
  const parts = [
    inspectionType,
    patrolRound ? `第${patrolRound}轮` : '',
    inspectionQuantity ? `检验${inspectionQuantity}件` : '',
    scrapQuantity ? `报废${scrapQuantity}件` : ''
  ].filter(Boolean)
  if (!parts.length) {
    return undefined
  }
  return {
    key: 'inspectionOverview',
    label: '检验信息',
    valueText: parts.join('，')
  }
}

const resolvePqcSubmissionContentItems = (
  row: ProcessPoolTimelineEventVO
): PqcSubmissionContentItem[] => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  const details = resolvePqcItemSnapshotDetails(row)
  if (!rootPayload || !details.length) {
    return PQC_SUBMISSION_CONTENT_MISSING_ITEMS
  }
  const contentItems = details.map((detail, index) => ({
    key: detail.itemCode || `pqc-item-${index}`,
    label: detail.itemName || detail.itemCode || '检验项目',
    valueText: [
      detail.selectedEquipmentNumber ? `设备编号：${detail.selectedEquipmentNumber}` : '',
      `样本：${formatPqcSnapshotSampleValues(detail)}`,
      detail.judgement ? `判定：${detail.judgement}` : ''
    ].filter(Boolean).join('；')
  }))
  const overviewItem = resolvePqcSubmissionOverviewItem(rootPayload)
  return overviewItem ? [overviewItem, ...contentItems] : contentItems
}

const resolveProductionSubmissionSummary = (row: ProcessPoolTimelineEventVO) =>
  row.submittedSummary || row.pqcSummary || '--'

const resolveSubmissionReviewStatusText = (status?: string) => {
  if (status === 'APPROVED') return '正确'
  if (status === 'REJECTED') return '不正确'
  return '待判定'
}

const resolveSubmissionReviewTagType = (status?: string) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

const resolveProcessInspectionAggregationStatusText = (status?: string) => {
  if (status === 'AGGREGATED') return '已汇集'
  if (status === 'FAILED') return '汇集失败'
  return '待汇集'
}

const resolveProcessInspectionAggregationTagType = (status?: string) => {
  if (status === 'AGGREGATED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

const buildSubmissionParams = (): TeamLeaderSubmissionPageReqVO => {
  if (!queryParams.submitDate) {
    throw new Error('提交日期不能为空')
  }
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    leaderType: queryParams.leaderType,
    submitDate: queryParams.submitDate,
    employeeUserId: normalizePositiveNumber(queryParams.employeeUserId),
    processId: normalizePositiveNumber(queryParams.processId),
    deviceId: normalizePositiveNumber(queryParams.deviceId),
    templateType: queryParams.templateType || undefined,
    workOrderId: normalizePositiveNumber(queryParams.workOrderId),
    workOrderCode: queryParams.workOrderCode?.trim() || undefined,
    productId: normalizePositiveNumber(queryParams.productId),
    productKeyword: queryParams.productKeyword?.trim() || undefined,
    inspectionType: queryParams.inspectionType || undefined,
    roundNo: normalizePositiveNumber(queryParams.roundNo),
    submissionReviewStatus: queryParams.submissionReviewStatus || undefined
  }
}

const getSubmissionList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getTeamLeaderSubmissionPage(buildSubmissionParams())
    submissionList.value = data.list || []
    submissionTotal.value = data.total || 0
  } catch (error) {
    submissionList.value = []
    submissionTotal.value = 0
    loadError.value = resolveErrorMessage(error, '班组长提交看板加载失败')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  if (activeLeaderTab.value === 'QA') {
    return
  }
  queryParams.pageNo = 1
  getSubmissionList()
}

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

const handleLeaderTypeChange = (value: string | number) => {
  const selectedTab = String(value) as WorkbenchLeaderTab
  if (selectedTab === 'QA') {
    loading.value = false
    loadError.value = ''
    return
  }
  const leaderType = selectedTab as TeamLeaderType
  queryParams.leaderType = leaderType
  if (leaderType === 'PQC') {
    queryParams.templateType = 'PQC_SIMPLIFIED'
  } else if (queryParams.templateType === 'PQC_SIMPLIFIED') {
    queryParams.templateType = undefined
    queryParams.productId = undefined
    queryParams.productKeyword = undefined
    queryParams.inspectionType = undefined
    queryParams.roundNo = undefined
    queryParams.submissionReviewStatus = undefined
  }
  if (leaderType === 'PRODUCTION') {
    loadActiveOrders().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '活跃订单加载失败'))
    })
  }
  handleQuery()
}

const resetQuery = () => {
  const leaderType = activeLeaderTab.value
  if (leaderType === 'QA') {
    return
  }
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.leaderType = leaderType
  queryParams.submitDate = new Date().toISOString().slice(0, 10)
  queryParams.templateType = leaderType === 'PQC' ? 'PQC_SIMPLIFIED' : undefined
  queryParams.productId = undefined
  queryParams.productKeyword = undefined
  queryParams.inspectionType = undefined
  queryParams.roundNo = undefined
  queryParams.submissionReviewStatus = undefined
  getSubmissionList()
}

const openDetail = async (event: ProcessPoolTimelineEventVO) => {
  const eventId = requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
  detailVisible.value = true
  detailLoading.value = true
  detail.value = undefined
  try {
    detail.value = await getTeamLeaderSubmissionDetail(
      eventId,
      queryParams.leaderType as TeamLeaderType
    )
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '员工提交详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

const openReview = async (event: ProcessPoolTimelineEventVO) => {
  requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
  reviewEvent.value = event
  reviewForm.reviewStatus = 'APPROVED'
  resetReviewAllocation()
  reviewForm.reviewRemark = ''
  reviewForm.reviewSignatureId = undefined
  reviewForm.reviewSignatureEmployeeUserId = undefined
  reviewForm.reviewSignatureSnapshotJson = ''
  reviewVisible.value = true
  if (isProductionLeader.value) {
    try {
      await loadActiveOrders()
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, '活跃订单加载失败'))
    }
  }
}

const submitReview = async () => {
  const eventId = requirePositiveNumber(reviewEvent.value?.id, '工序池提交事件编号不能为空')
  reviewSubmitting.value = true
  try {
    const leaderType = queryParams.leaderType as TeamLeaderType
    const reviewRemark = reviewForm.reviewRemark.trim() || undefined
    const reviewSignaturePayload = buildReviewSignaturePayload()
    if (isProductionLeader.value && reviewForm.reviewStatus === 'APPROVED') {
      await confirmTeamLeaderReportAllocation({
        eventId,
        leaderType,
        allocationMode: reviewForm.allocationMode,
        reviewRemark,
        ...reviewSignaturePayload,
        allocations: buildAllocationSubmitLines()
      })
    } else {
      await reviewTeamLeaderSubmission({
        leaderType,
        eventId,
        reviewStatus: reviewForm.reviewStatus,
        reviewRemark,
        ...reviewSignaturePayload
      })
    }
    ElMessage.success('复核已提交')
    reviewVisible.value = false
    await getSubmissionList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '复核提交失败'))
  } finally {
    reviewSubmitting.value = false
  }
}

const openCorrection = (event: ProcessPoolTimelineEventVO) => {
  try {
    const eventId = requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
    correctionEvent.value = event
    correctionForm.eventId = eventId
    correctionForm.modifiedByUserId = undefined
    correctionForm.revisionSignatureId = undefined
    correctionForm.revisionSignatureUserId = undefined
    correctionForm.changeReason = ''
    correctionForm.afterPayloadJson = normalizePayloadJsonForCorrection(event.originalPayloadJson)
    correctionForm.revisionSignatureSnapshotJson = ''
    correctionForm.changedFieldsJson = ''
    correctionVisible.value = true
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '原始记录修正入口打开失败'))
  }
}

const buildCorrectionRequest = () => {
  parseJsonField<Record<string, unknown>>(correctionForm.afterPayloadJson, '修改后payload JSON')
  parseJsonField<Record<string, unknown>>(
    correctionForm.revisionSignatureSnapshotJson,
    '修正签名快照JSON'
  )
  const changedFields = parseJsonField<ProcessPoolEventRevisionFieldChangeVO[]>(
    correctionForm.changedFieldsJson,
    '字段变更JSON'
  )
  if (!Array.isArray(changedFields) || changedFields.length === 0) {
    throw new Error('字段变更JSON必须是非空数组')
  }
  if (changedFields.some((item) => typeof item.affectsQuantityFragment !== 'boolean')) {
    throw new Error('字段变更JSON中 affectsQuantityFragment 必须是 true 或 false')
  }
  if (!correctionForm.changeReason.trim()) {
    throw new Error('修改原因不能为空')
  }
  return {
    eventId: requirePositiveNumber(correctionForm.eventId, '工序池提交事件编号不能为空'),
    afterPayload: correctionForm.afterPayloadJson.trim(),
    changeReason: correctionForm.changeReason.trim(),
    revisionSignatureId: requirePositiveNumber(correctionForm.revisionSignatureId, '修正签名ID不能为空'),
    revisionSignatureUserId: requirePositiveNumber(
      correctionForm.revisionSignatureUserId,
      '签名用户ID不能为空'
    ),
    revisionSignatureSnapshot: correctionForm.revisionSignatureSnapshotJson.trim(),
    modifiedByUserId: requirePositiveNumber(correctionForm.modifiedByUserId, '修改人用户ID不能为空'),
    changedFields
  }
}

const submitCorrection = async () => {
  requirePositiveNumber(correctionEvent.value?.id, '工序池提交事件编号不能为空')
  correctionSubmitting.value = true
  try {
    await updateProcessPoolOriginalRecord(buildCorrectionRequest())
    ElMessage.success('修正已提交，修改日志已记录')
    correctionVisible.value = false
    await getSubmissionList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '原始记录修正失败'))
  } finally {
    correctionSubmitting.value = false
  }
}

const prefillAbnormal = (event: ProcessPoolTimelineEventVO) => {
  abnormalForm.workOrderId = normalizePositiveNumber(event.workOrderId)
  const matchedActiveOrder = activeOrderOptions.value.find(
    (order) => order.workOrderId === abnormalForm.workOrderId
  )
  abnormalForm.activeOrderId = matchedActiveOrder?.id
  abnormalForm.routeProcessId = normalizePositiveNumber(event.routeProcessId)
  abnormalForm.processId = normalizePositiveNumber(event.processId)
  abnormalForm.sourceEventId = normalizePositiveNumber(event.id)
}

const handleAbnormalActiveOrderChange = (activeOrderId?: number) => {
  const activeOrder = activeOrderOptions.value.find((order) => order.id === activeOrderId)
  abnormalForm.workOrderId = activeOrder?.workOrderId
}

const requireSelectedActiveOrderWorkOrderId = () => {
  const activeOrderId = requirePositiveNumber(abnormalForm.activeOrderId, '活跃订单不能为空')
  const activeOrder = activeOrderOptions.value.find((order) => order.id === activeOrderId)
  if (!activeOrder) {
    throw new Error('活跃订单不存在或已移出')
  }
  return activeOrder.workOrderId
}

const resolveStructuredPayloadItems = (rawPayload?: string) => {
  if (!rawPayload?.trim()) return []
  try {
    const parsed = JSON.parse(rawPayload)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return [{ field: 'payload', value: String(parsed) }]
    }
    return Object.entries(parsed).map(([field, value]) => ({
      field,
      value: typeof value === 'object' ? JSON.stringify(value) : String(value)
    }))
  } catch {
    return [{ field: 'payload', value: rawPayload }]
  }
}

const submitAbnormal = async () => {
  const valid = await abnormalFormRef.value?.validate?.()
  if (valid === false) return
  abnormalSubmitting.value = true
  try {
    await markAndReportWorkOrderAbnormal({
      workOrderId: requireSelectedActiveOrderWorkOrderId(),
      routeProcessId: normalizePositiveNumber(abnormalForm.routeProcessId),
      processId: normalizePositiveNumber(abnormalForm.processId),
      sourceEventId: normalizePositiveNumber(abnormalForm.sourceEventId),
      abnormalReasonCode: abnormalForm.abnormalReasonCode.trim(),
      abnormalDescription: abnormalForm.abnormalDescription.trim()
    })
    ElMessage.success('异常已上报')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '异常上报失败'))
  } finally {
    abnormalSubmitting.value = false
  }
}

const submitAddActiveOrder = async () => {
  maintenanceSubmitting.value = true
  try {
    await addTeamLeaderActiveOrder({
      workOrderId: requirePositiveNumber(activeOrderForm.workOrderId, '生产订单ID不能为空'),
      routeId: requirePositiveNumber(activeOrderForm.routeId, '路线ID不能为空'),
      routeVersionId: requirePositiveNumber(activeOrderForm.routeVersionId, '路线版本ID不能为空'),
      transferIds: parsePositiveIntegerList(activeOrderForm.transferIdsText, '调拨单ID列表')
    })
    ElMessage.success('活跃订单已加入')
    await loadActiveOrders()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '活跃订单加入失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitRemoveActiveOrder = async () => {
  maintenanceSubmitting.value = true
  try {
    await removeTeamLeaderActiveOrder({
      activeOrderId: requirePositiveNumber(activeOrderRemoveForm.activeOrderId, '活跃订单记录ID不能为空')
    })
    ElMessage.success('活跃订单已移出')
    await loadActiveOrders()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '活跃订单移出失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitEmployeeProfile = async () => {
  maintenanceSubmitting.value = true
  try {
    await createTeamEmployeeProfile({
      systemUserId: normalizePositiveNumber(employeeProfileForm.systemUserId),
      employeeCode: employeeProfileForm.employeeCode.trim(),
      employeeName: employeeProfileForm.employeeName.trim(),
      employeeType: employeeProfileForm.employeeType
    })
    ElMessage.success('员工档案已新增')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '员工档案新增失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitProcessEmployeeBinding = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamProcessEmployeeBinding({
      processId: requirePositiveNumber(processEmployeeBindingForm.processId, '工序ID不能为空'),
      employeeProfileId: requirePositiveNumber(
        processEmployeeBindingForm.employeeProfileId,
        '员工档案ID不能为空'
      )
    })
    ElMessage.success('工序员工关系已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '工序员工关系保存失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitTeamDevice = async () => {
  maintenanceSubmitting.value = true
  try {
    await createTeamDevice({
      deviceCode: teamDeviceForm.deviceCode.trim(),
      deviceName: teamDeviceForm.deviceName.trim(),
      deviceStatus: teamDeviceForm.deviceStatus
    })
    ElMessage.success('设备已新增')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '设备新增失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitTeamDeviceStatus = async () => {
  maintenanceSubmitting.value = true
  try {
    await updateTeamDeviceStatus({
      deviceId: requirePositiveNumber(teamDeviceStatusForm.deviceId, '设备ID不能为空'),
      deviceStatus: teamDeviceStatusForm.deviceStatus
    })
    ElMessage.success('设备状态已更新')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '设备状态更新失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitProcessDeviceBinding = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamProcessDeviceBinding({
      processId: requirePositiveNumber(processDeviceBindingForm.processId, '工序ID不能为空'),
      deviceId: requirePositiveNumber(processDeviceBindingForm.deviceId, '设备ID不能为空')
    })
    ElMessage.success('工序设备关系已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '工序设备关系保存失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitProcessDefectReason = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamProcessDefectReason({
      processId: requirePositiveNumber(defectReasonForm.processId, '工序ID不能为空'),
      reasonType: defectReasonForm.reasonType,
      reasonCode: defectReasonForm.reasonCode.trim(),
      reasonName: defectReasonForm.reasonName.trim()
    })
    const nextReason = {
      reasonType: defectReasonForm.reasonType,
      reasonCode: defectReasonForm.reasonCode.trim(),
      reasonName: defectReasonForm.reasonName.trim()
    }
    configuredDefectReasonOptions.value = [
      ...configuredDefectReasonOptions.value.filter(
        (reason) => reason.reasonCode !== nextReason.reasonCode
      ),
      nextReason
    ]
    ElMessage.success('工序异常原因已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '工序异常原因保存失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitRuntimeDeviceRule = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamRuntimeDeviceParameterRule({
      processId: requirePositiveNumber(deviceRuleForm.processId, '工序ID不能为空'),
      deviceId: requirePositiveNumber(deviceRuleForm.deviceId, '设备ID不能为空'),
      parameterCode: deviceRuleForm.parameterCode.trim(),
      parameterName: deviceRuleForm.parameterName.trim() || undefined,
      unit: deviceRuleForm.unit.trim() || undefined,
      lowerLimit: requireFiniteNumber(deviceRuleForm.lowerLimit, '参数下限不能为空'),
      upperLimit: requireFiniteNumber(deviceRuleForm.upperLimit, '参数上限不能为空'),
      defaultValue: normalizeFiniteNumber(deviceRuleForm.defaultValue),
      valueType: deviceRuleForm.valueType
    })
    ElMessage.success('设备参数已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '设备参数保存失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const formatDateTime = (value?: string | number | Date) => formatDateTimeValue(value, '--')

const resolvePqcTagType = (pqcResult?: string) => {
  if (pqcResult === 'SUCCESS' || pqcResult === 'PASS') return 'success'
  if (pqcResult === 'FAILURE' || pqcResult === 'FAIL') return 'danger'
  return 'info'
}

onMounted(() => {
  getSubmissionList()
  loadActiveOrders().catch((error) => {
    ElMessage.error(resolveErrorMessage(error, '活跃订单调拨库存追溯加载失败'))
  })
})
</script>

<style scoped>
.team-leader-workbench__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.team-leader-workbench__title {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.team-leader-workbench__subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.team-leader-workbench__query {
  margin-bottom: -15px;
}

.team-leader-workbench__form {
  max-width: 760px;
}

.team-leader-workbench__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.team-leader-workbench__section-title {
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.team-leader-workbench__hint {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.team-leader-workbench__qa-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 16px;
  margin-top: 16px;
}

.team-leader-workbench__qa-card {
  margin-top: 16px;
}

.team-leader-workbench__qa-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.team-leader-workbench__qa-rule-name {
  color: #172033;
  font-weight: 700;
}

.team-leader-workbench__qa-source {
  display: grid;
  gap: 6px;
  padding: 8px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.team-leader-workbench__qa-source-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  color: #172033;
  font-size: 12px;
  font-weight: 700;
}

.team-leader-workbench__qa-source-label {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.team-leader-workbench__qa-source-text {
  color: #172033;
  font-size: 12px;
  line-height: 1.55;
  white-space: normal;
}

.team-leader-workbench__qa-check-list {
  display: grid;
  gap: 10px;
}

.team-leader-workbench__qa-check {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
  padding: 10px;
  border: 1px solid #f2c6c6;
  border-radius: 8px;
  background: #fff7f7;
}

.team-leader-workbench__qa-check.is-passed {
  border-color: #b7e1c0;
  background: #f5fff7;
}

.team-leader-workbench__qa-check-title {
  color: #172033;
  font-weight: 700;
}

.team-leader-workbench__qa-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.team-leader-workbench__maintenance-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.team-leader-workbench__daily-close-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.team-leader-workbench__daily-close-card {
  border-color: #d9e2f1;
}

.team-leader-workbench__daily-close-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.team-leader-workbench__daily-close-value {
  margin-top: 6px;
  color: #172033;
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.team-leader-workbench__daily-close-hint {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.team-leader-workbench__transfer-trace {
  width: 100%;
  margin-top: 8px;
}

.team-leader-workbench__payload {
  max-height: 260px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.team-leader-workbench__review-log {
  display: grid;
  gap: 4px;
  color: #334155;
  font-size: 12px;
  line-height: 1.5;
}

.team-leader-workbench__review-text,
.team-leader-workbench__review-meta {
  word-break: break-word;
}

.team-leader-workbench__review-meta {
  color: #64748b;
}

.team-leader-workbench__submission-log {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.team-leader-workbench__submission-log-title {
  color: #172033;
  font-size: 14px;
  font-weight: 700;
}

.team-leader-workbench__correction-form {
  margin-top: 16px;
}

.team-leader-workbench__number {
  width: 100%;
}

.team-leader-workbench__pqc-content {
  display: grid;
  gap: 4px;
  color: #334155;
  font-size: 12px;
  line-height: 1.5;
}

.team-leader-workbench__pqc-content-item {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 8px;
}

.team-leader-workbench__pqc-content-label {
  color: #0f172a;
  font-weight: 600;
}

.team-leader-workbench__pqc-content-value {
  word-break: break-word;
}

@media (max-width: 1180px) {
  .team-leader-workbench__qa-layout,
  .team-leader-workbench__maintenance-grid,
  .team-leader-workbench__daily-close-grid {
    grid-template-columns: 1fr;
  }
}
</style>
