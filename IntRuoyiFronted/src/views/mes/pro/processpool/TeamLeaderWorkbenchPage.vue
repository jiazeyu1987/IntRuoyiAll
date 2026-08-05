<template>
  <ContentWrap v-if="!showPqcModuleTabs">
    <div class="team-leader-workbench__header">
      <div>
        <div class="team-leader-workbench__title">{{ pageTitle }}</div>
        <div class="team-leader-workbench__subtitle">
          {{ pageSubtitle }}
        </div>
      </div>
    </div>

    <el-tabs
      v-if="showLeaderTypeTabs"
      v-model="activeLeaderTab"
      data-team-leader-type-tabs
      @tab-change="handleLeaderTypeChange"
    >
      <el-tab-pane label="生产组长" name="PRODUCTION" />
      <el-tab-pane label="PQC 组长" name="PQC" />
    </el-tabs>

    <el-tabs
      v-if="showProductionModuleTabs"
      v-model="activeProductionModuleTab"
      class="team-leader-workbench__module-tabs"
      data-production-leader-module-tabs
    >
      <el-tab-pane label="人员管理" name="personnel" data-production-leader-module-tab-personnel />
      <el-tab-pane label="报工管理" name="report" data-production-leader-module-tab-report />
      <el-tab-pane label="损耗管理" name="loss" data-production-leader-module-tab-loss />
      <el-tab-pane label="班组配置" name="config" data-production-leader-module-tab-config />
    </el-tabs>
  </ContentWrap>

  <ContentWrap v-if="loadError">
      <el-alert :title="loadError" type="error" :closable="false" show-icon />
    </ContentWrap>

  <ContentWrap v-if="showProductionPersonnelModule" data-team-leader-production-personnel-tab>
    <el-tabs v-model="productionPersonnelActiveTab" class="team-leader-workbench__personnel-tabs">
      <el-tab-pane label="生产人员档案" name="productionPersonnel">
        <div class="team-leader-workbench__section-head">
          <div>
            <div class="team-leader-workbench__section-title">生产人员档案</div>
            <div class="team-leader-workbench__hint">
              只维护已关联当前生产组长的员工；正式工从受限姓名下拉选择，临时工手动录入姓名和签名密码。
            </div>
          </div>
          <el-button :loading="productionPersonnelLoading" @click="refreshProductionPersonnel">
            <Icon icon="ep:refresh" class="mr-5px" />
            刷新人员档案
          </el-button>
        </div>

        <div class="team-leader-workbench__personnel-actions">
          <el-card shadow="never">
            <template #header>搜索选择正式工</template>
            <el-form :model="formalEmployeeForm" label-width="108px">
              <el-form-item label="正式工姓名">
                <el-select
                  v-model="formalEmployeeForm.systemUserId"
                  filterable
                  remote
                  clearable
                  reserve-keyword
                  placeholder="输入姓名搜索"
                  :remote-method="searchFormalEmployeeCandidatesForSelect"
                  :loading="formalCandidateLoading"
                  class="team-leader-workbench__full-control"
                  data-team-leader-formal-employee-select
                >
                  <!-- static contract anchor: remote-method="searchFormalEmployeeCandidatesForSelect" -->
                  <el-option
                    v-for="candidate in formalEmployeeCandidateOptions"
                    :key="candidate.systemUserId"
                    :label="candidate.displayName"
                    :value="candidate.systemUserId"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="显示名">
                <el-input
                  v-model="formalEmployeeForm.displayName"
                  clearable
                  placeholder="可选；重名时请加后缀"
                />
              </el-form-item>
              <el-alert
                title="正式工电子签名密码继续使用原账号配置，本页不设置或重置。"
                type="info"
                :closable="false"
                show-icon
              />
              <el-form-item class="team-leader-workbench__form-actions">
                <el-button
                  type="primary"
                  :loading="productionPersonnelSubmitting"
                  @click="submitLinkFormalEmployee"
                >
                  关联正式工
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never">
            <template #header>手动录入临时工</template>
            <el-form
              :model="temporaryEmployeeForm"
              label-width="108px"
              data-team-leader-temporary-employee-form
            >
              <el-form-item label="显示名">
                <el-input
                  v-model="temporaryEmployeeForm.displayName"
                  clearable
                  placeholder="同组长有效员工不能重名，重名请加后缀"
                />
              </el-form-item>
              <el-form-item label="签名密码">
                <el-input
                  v-model="temporaryEmployeeForm.signaturePassword"
                  show-password
                  clearable
                  placeholder="用于统一电子签名流程"
                />
              </el-form-item>
              <el-alert
                title="临时工只创建生产人员档案，不创建系统登录账号。"
                type="info"
                :closable="false"
                show-icon
              />
              <el-form-item class="team-leader-workbench__form-actions">
                <el-button
                  type="primary"
                  :loading="productionPersonnelSubmitting"
                  @click="submitCreateTemporaryEmployee"
                >
                  新增临时工
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>

        <UnifiedListTemplate
          table-key="mes.processPool.teamLeader.productionPersonnel"
          :query-model="productionPersonnelQuery"
          :filter-definitions="productionPersonnelFilterDefinitions"
          :quick-filter-state="productionPersonnelQuickFilterState"
          :operator-options="productionPersonnelOperatorOptions"
          :columns="productionPersonnelColumns"
          :show-quick-filter="false"
          :show-column-settings="false"
          :total="productionPersonnelTotal"
          :page="productionPersonnelQuery.pageNo"
          :limit="productionPersonnelQuery.pageSize"
          @update:page="handleProductionPersonnelPageChange"
          @update:limit="handleProductionPersonnelPageSizeChange"
          @pagination="refreshProductionPersonnel"
        >
          <template #actions>
            <el-select
              v-model="productionPersonnelQuery.enabled"
              clearable
              placeholder="启用状态"
              class="!w-140px"
              @change="refreshProductionPersonnel"
            >
              <el-option label="未禁用" :value="true" />
              <el-option label="已禁用" :value="false" />
            </el-select>
          </template>
          <template #table>
            <el-table
              v-loading="productionPersonnelLoading"
              :data="pagedProductionPersonnelRows"
              border
              stripe
              data-team-leader-production-personnel-list
            >
              <el-table-column label="显示名" min-width="140">
                <template #default="{ row }">
                  <span>{{ row.displayName || row.employeeName || '--' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="来源" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.employeeType === 'TEMPORARY' ? 'warning' : 'success'" effect="plain">
                    {{ formatEmployeeType(row.employeeType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="员工编码" min-width="120">
                <template #default="{ row }">{{ row.employeeCode || '-' }}</template>
              </el-table-column>
              <el-table-column label="签名密码" min-width="140">
                <template #default="{ row }">
                  {{ formatSignaturePasswordManager(row) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.enabled === false ? 'danger' : 'success'" effect="plain">
                    {{ row.enabled === false ? '已禁用' : '可选择' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="260" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="updateEmployeeDisplayName(row)">
                    修改显示名
                  </el-button>
                  <el-button
                    link
                    :type="row.enabled === false ? 'success' : 'warning'"
                    @click="updateEmployeeStatus(row, row.enabled === false)"
                  >
                    {{ row.enabled === false ? '启用' : '禁用' }}
                  </el-button>
                  <el-button
                    v-if="row.employeeType === 'TEMPORARY'"
                    link
                    type="primary"
                    @click="resetTemporarySignaturePassword(row)"
                  >
                    重置签名密码
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </UnifiedListTemplate>

      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <ContentWrap
    v-if="showPqcManagementModule"
    :class="{ 'team-leader-workbench__pqc-module-card': showPqcModuleTabs }"
    data-team-leader-report-workbench
  >
      <div v-if="showPqcModuleTabs" class="team-leader-workbench__embedded-header">
        <div class="team-leader-workbench__title">{{ pageTitle }}</div>
        <div class="team-leader-workbench__subtitle">
          {{ pageSubtitle }}
        </div>
      </div>
      <el-tabs
        v-if="showPqcModuleTabs"
        v-model="activePqcModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
        data-pqc-leader-module-tabs
      >
        <el-tab-pane label="PQC管理" name="management" data-pqc-leader-module-tab-management />
        <el-tab-pane label="看板" name="dashboard" data-pqc-leader-module-tab-dashboard />
      </el-tabs>
      <div v-if="!showPqcModuleTabs" class="team-leader-workbench__section-head">
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
              v-if="canReviewSubmission(row)"
              link
              type="success"
              :data-team-leader-review-event-id="String(row.id)"
              @click="openReview(row)"
            >
              复核
            </el-button>
            <el-button
              v-if="canCorrectSubmission(row)"
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

    <ContentWrap
      v-if="showPqcDashboardModule"
      :class="{ 'team-leader-workbench__pqc-module-card': showPqcModuleTabs }"
      data-role-matrix-daily-close
    >
      <div v-if="showPqcModuleTabs" class="team-leader-workbench__embedded-header">
        <div class="team-leader-workbench__title">{{ pageTitle }}</div>
        <div class="team-leader-workbench__subtitle">
          {{ pageSubtitle }}
        </div>
      </div>
      <el-tabs
        v-if="showPqcModuleTabs"
        v-model="activePqcModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
        data-pqc-leader-module-tabs
      >
        <el-tab-pane label="PQC管理" name="management" data-pqc-leader-module-tab-management />
        <el-tab-pane label="看板" name="dashboard" data-pqc-leader-module-tab-dashboard />
      </el-tabs>
      <div v-if="!showPqcModuleTabs" class="team-leader-workbench__section-head">
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

    <ContentWrap v-if="showProductionReportModule" data-team-leader-abnormal-report>
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


    <ContentWrap v-if="showProductionLossModule" data-team-leader-loss-reason-tab>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">损耗原因维护</div>
          <div class="team-leader-workbench__hint">
            标准列表按“工序开始”授权展示工序，损耗原因绑定到工序设置列表下的路线工序并由多个生产组长共用。
          </div>
        </div>
        <el-button :loading="lossReasonLoading" @click="loadLossReasonRows">刷新</el-button>
      </div>
      <el-table
        v-loading="lossReasonLoading"
        :data="lossReasonRows"
        border
        stripe
        data-loss-reason-standard-list
      >
        <el-table-column label="工艺路线" min-width="180">
          <template #default="{ row }">
            {{ row.routeName || row.routeCode || row.routeId || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="工序" min-width="180">
          <template #default="{ row }">
            <span data-loss-reason-route-process-row>
              {{ formatLossReasonProcess(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="损耗原因" min-width="280" data-loss-reason-column>
          <template #default="{ row }">
            <div class="team-leader-workbench__loss-reasons">
              <el-tag
                v-for="reason in row.reasons"
                :key="reason.id"
                :type="reason.enabled ? 'success' : 'info'"
                effect="plain"
              >
                {{ reason.reasonCode }} / {{ reason.reasonName }}{{ reason.enabled ? '' : '（停用）' }}
              </el-tag>
              <span v-if="!row.reasons?.length" class="team-leader-workbench__hint">暂无损耗原因</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作面板" width="320" fixed="right">
          <template #default="{ row }">
            <div data-loss-reason-operation-panel>
              <el-button link type="primary" @click="openCreateLossReason(row)">新增损耗原因</el-button>
              <el-button
                v-for="reason in row.reasons"
                :key="`edit-${reason.id}`"
                link
                type="warning"
                @click="openEditLossReason(row, reason)"
              >
                修改损耗原因
              </el-button>
              <el-button
                v-for="reason in row.reasons"
                :key="`delete-${reason.id}`"
                link
                type="danger"
                @click="handleDeleteLossReason(reason)"
              >
                删除损耗原因
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>
    <ContentWrap v-if="showProductionConfigModule" data-team-leader-config-center>
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
          <template #header>生产人员工序绑定</template>
          <el-alert
            title="员工档案请在上方生产人员档案 tab 维护；这里仅把已关联当前组长的生产人员档案绑定到工序。"
            type="info"
            :closable="false"
            show-icon
          />
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

    <el-dialog
      v-model="lossReasonDialogVisible"
      :title="lossReasonDialogTitle"
      width="560px"
      destroy-on-close
      data-loss-reason-edit-dialog
    >
      <el-form :model="lossReasonForm" label-width="108px">
        <el-form-item label="工艺路线">
          <span>{{ lossReasonEditingRow?.routeName || lossReasonEditingRow?.routeCode || '--' }}</span>
        </el-form-item>
        <el-form-item label="工序">
          <span>{{ lossReasonEditingRow ? formatLossReasonProcess(lossReasonEditingRow) : '--' }}</span>
        </el-form-item>
        <el-form-item label="原因编码" required>
          <el-input
            v-model="lossReasonForm.reasonCode"
            :disabled="lossReasonDialogMode === 'edit'"
            maxlength="64"
            placeholder="请输入损耗原因编码"
          />
        </el-form-item>
        <el-form-item label="原因名称" required>
          <el-input
            v-model="lossReasonForm.reasonName"
            maxlength="255"
            placeholder="请输入损耗原因名称"
          />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch
            v-model="lossReasonForm.enabled"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item label="维护说明">
          <el-input
            v-model="lossReasonForm.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="记录新增、修改或删除原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lossReasonDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="lossReasonSubmitting"
          @click="submitLossReason"
        >
          保存损耗原因
        </el-button>
      </template>
    </el-dialog>

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
import { ElMessage, ElMessageBox } from 'element-plus'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  addTeamLeaderActiveOrder,
  confirmTeamLeaderReportAllocation,
  createTemporaryTeamEmployee,
  createTeamDevice,
  createTeamEmployeeProfile,
  createTeamLeaderLossReason,
  deleteTeamLeaderLossReason,
  getTeamLeaderLossReasonPage,
  getProductionPersonnelList,
  getTeamLeaderActiveOrderList,
  getTeamLeaderActiveOrderTransferTrace,
  getTeamLeaderSubmissionDetail,
  getTeamLeaderSubmissionPage,
  linkFormalTeamEmployee,
  markAndReportWorkOrderAbnormal,
  previewTeamLeaderReportFifoAllocation,
  removeTeamLeaderActiveOrder,
  resetTemporaryTeamEmployeeSignaturePassword,
  reviewTeamLeaderSubmission,
  saveTeamProcessDefectReason,
  saveTeamProcessDeviceBinding,
  saveTeamProcessEmployeeBinding,
  saveTeamRuntimeDeviceParameterRule,
  searchTeamFormalEmployeeCandidates,
  updateTeamLeaderLossReason,
  updateTeamDeviceStatus,
  updateTeamEmployeeDisplayName as updateTeamEmployeeDisplayNameRequest,
  updateTeamEmployeeStatus as updateTeamEmployeeStatusRequest,
  type TeamFormalEmployeeCandidateRespVO,
  type TeamLeaderActiveOrderRespVO,
  type TeamLeaderActiveOrderTransferTraceRespVO,
  type TeamLeaderLossReasonRowVO,
  type TeamLeaderLossReasonVO,
  type TeamLeaderReportAllocationLine,
  type TeamLeaderSubmissionPageReqVO,
  type TeamLeaderType,
  type TeamProductionEmployeeRespVO
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

type WorkbenchLeaderTab = TeamLeaderType

const props = withDefaults(
  defineProps<{
    leaderType?: TeamLeaderType
    showLeaderTypeTabs?: boolean
    showPqcModuleTabs?: boolean
    showProductionModuleTabs?: boolean
    title?: string
    subtitle?: string
  }>(),
  {
    leaderType: 'PRODUCTION',
    showLeaderTypeTabs: false,
    showPqcModuleTabs: false,
    showProductionModuleTabs: false,
    title: '工序池班组长工作台',
    subtitle: '负责生产报工确认、活跃订单分配、异常上报和班组配置中心维护'
  }
)

const queryFormRef = ref()
const abnormalFormRef = ref()
const activeLeaderTab = ref<WorkbenchLeaderTab>(props.leaderType)
const activePqcModuleTab = ref<'management' | 'dashboard'>('management')
const activeProductionModuleTab = ref<'personnel' | 'report' | 'loss' | 'config'>('personnel')
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
const lossReasonRows = ref<TeamLeaderLossReasonRowVO[]>([])
const lossReasonLoading = ref(false)
const lossReasonSubmitting = ref(false)
const lossReasonDialogVisible = ref(false)
const lossReasonDialogMode = ref<'create' | 'edit'>('create')
const lossReasonEditingRow = ref<TeamLeaderLossReasonRowVO>()
const lossReasonEditingReason = ref<TeamLeaderLossReasonVO>()
const allocationRows = ref<TeamLeaderReportAllocationLine[]>([])
const configuredDefectReasonOptions = ref<
  Array<{ reasonType: string; reasonCode: string; reasonName: string }>
>([])
const productionPersonnelActiveTab = ref('productionPersonnel')
const productionPersonnelLoading = ref(false)
const productionPersonnelSubmitting = ref(false)
const formalCandidateLoading = ref(false)
const productionPersonnelRows = ref<TeamProductionEmployeeRespVO[]>([])
const formalEmployeeCandidateOptions = ref<TeamFormalEmployeeCandidateRespVO[]>([])

const productionPersonnelQuery = reactive({
  enabled: true as boolean | undefined,
  pageNo: 1,
  pageSize: 10
})
const productionPersonnelFilterDefinitions: any[] = []
const productionPersonnelQuickFilterState = reactive({})
const productionPersonnelOperatorOptions: any[] = []
const productionPersonnelColumns: any[] = [
  { key: 'displayName', label: '显示名', visible: true },
  { key: 'employeeType', label: '来源', visible: true },
  { key: 'employeeCode', label: '员工编码', visible: true },
  { key: 'enabled', label: '状态', visible: true }
]

const showLeaderTypeTabs = computed(() => props.showLeaderTypeTabs)
const showPqcModuleTabs = computed(
  () => props.showPqcModuleTabs && activeLeaderTab.value === 'PQC'
)
const showProductionModuleTabs = computed(
  () => props.showProductionModuleTabs && activeLeaderTab.value === 'PRODUCTION'
)
const pageTitle = computed(() => props.title)
const pageSubtitle = computed(() => props.subtitle)
const isProductionLeader = computed(() => activeLeaderTab.value === 'PRODUCTION')
const showProductionPersonnelModule = computed(
  () => isProductionLeader.value && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'personnel')
)
const showProductionReportModule = computed(
  () => isProductionLeader.value && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'report')
)
const showProductionLossModule = computed(
  () => isProductionLeader.value && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'loss')
)
const showProductionConfigModule = computed(
  () => isProductionLeader.value && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'config')
)
const showPqcManagementModule = computed(
  () =>
    showProductionReportModule.value ||
    (activeLeaderTab.value === 'PQC' && (!showPqcModuleTabs.value || activePqcModuleTab.value === 'management'))
)
const showPqcDashboardModule = computed(
  () =>
    showProductionReportModule.value ||
    (activeLeaderTab.value === 'PQC' && (!showPqcModuleTabs.value || activePqcModuleTab.value === 'dashboard'))
)
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
const lossReasonDialogTitle = computed(() =>
  lossReasonDialogMode.value === 'create' ? '新增损耗原因' : '修改损耗原因'
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
const productionPersonnelTotal = computed(() => productionPersonnelRows.value.length)
const pagedProductionPersonnelRows = computed(() => {
  const pageNo = Math.max(1, Number(productionPersonnelQuery.pageNo) || 1)
  const pageSize = Math.max(1, Number(productionPersonnelQuery.pageSize) || 10)
  const start = (pageNo - 1) * pageSize
  return productionPersonnelRows.value.slice(start, start + pageSize)
})

const canReviewSubmission = (row: ProcessPoolTimelineEventVO) =>
  !row.submissionReviewStatus || row.submissionReviewStatus === 'PENDING'

const canCorrectSubmission = (row: ProcessPoolTimelineEventVO) =>
  row.submissionReviewStatus === 'REJECTED'

const queryParams = reactive<TeamLeaderSubmissionPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  leaderType: activeLeaderTab.value,
  submitDate: new Date().toISOString().slice(0, 10),
  employeeUserId: undefined,
  processId: undefined,
  deviceId: undefined,
  templateType: activeLeaderTab.value === 'PQC' ? 'PQC_SIMPLIFIED' : undefined,
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

const formalEmployeeForm = reactive({
  systemUserId: undefined as number | undefined,
  displayName: ''
})

const temporaryEmployeeForm = reactive({
  displayName: '',
  signaturePassword: ''
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
  reasonType: 'UNQUALIFIED',
  reasonCode: '',
  reasonName: ''
})

const lossReasonForm = reactive({
  reasonCode: '',
  reasonName: '',
  enabled: true,
  remark: ''
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

const formatEmployeeType = (employeeType?: string) => {
  if (employeeType === 'TEMPORARY') return '临时工'
  if (employeeType === 'FORMAL') return '正式工'
  return employeeType || '--'
}

const formatSignaturePasswordManager = (row: TeamProductionEmployeeRespVO) => {
  if (row.employeeType === 'TEMPORARY') return '临时工档案密码'
  if (row.employeeType === 'FORMAL') return '原账号电子签名密码'
  return row.signaturePasswordManagedBy || '--'
}

const refreshProductionPersonnel = async () => {
  productionPersonnelLoading.value = true
  try {
    productionPersonnelRows.value = await getProductionPersonnelList({
      enabled: productionPersonnelQuery.enabled
    })
    const maxPage = Math.max(1, Math.ceil(productionPersonnelRows.value.length / productionPersonnelQuery.pageSize))
    if (productionPersonnelQuery.pageNo > maxPage) {
      productionPersonnelQuery.pageNo = maxPage
    }
  } catch (error) {
    productionPersonnelRows.value = []
    ElMessage.error(resolveErrorMessage(error, '生产人员档案加载失败'))
  } finally {
    productionPersonnelLoading.value = false
  }
}

const handleProductionPersonnelPageChange = (page: number) => {
  productionPersonnelQuery.pageNo = page
}

const handleProductionPersonnelPageSizeChange = (limit: number) => {
  productionPersonnelQuery.pageSize = limit
  productionPersonnelQuery.pageNo = 1
}

const searchFormalEmployeeCandidatesForSelect = async (keyword: string) => {
  const searchText = keyword.trim()
  if (!searchText) {
    formalEmployeeCandidateOptions.value = []
    return
  }
  formalCandidateLoading.value = true
  try {
    formalEmployeeCandidateOptions.value = await searchTeamFormalEmployeeCandidates(searchText)
  } catch (error) {
    formalEmployeeCandidateOptions.value = []
    ElMessage.error(resolveErrorMessage(error, '正式工候选搜索失败'))
  } finally {
    formalCandidateLoading.value = false
  }
}

const submitLinkFormalEmployee = async () => {
  productionPersonnelSubmitting.value = true
  try {
    await linkFormalTeamEmployee({
      systemUserId: requirePositiveNumber(formalEmployeeForm.systemUserId, '请选择正式工'),
      displayName: formalEmployeeForm.displayName.trim() || undefined
    })
    formalEmployeeForm.systemUserId = undefined
    formalEmployeeForm.displayName = ''
    formalEmployeeCandidateOptions.value = []
    ElMessage.success('正式工已关联当前生产组长')
    await refreshProductionPersonnel()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '正式工关联失败，请确认是否重名并按提示加后缀'))
  } finally {
    productionPersonnelSubmitting.value = false
  }
}

const submitCreateTemporaryEmployee = async () => {
  productionPersonnelSubmitting.value = true
  try {
    await createTemporaryTeamEmployee({
      displayName: temporaryEmployeeForm.displayName.trim(),
      signaturePassword: temporaryEmployeeForm.signaturePassword
    })
    temporaryEmployeeForm.displayName = ''
    temporaryEmployeeForm.signaturePassword = ''
    ElMessage.success('临时工已新增并关联当前生产组长')
    await refreshProductionPersonnel()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '临时工新增失败，请确认是否重名并按提示加后缀'))
  } finally {
    productionPersonnelSubmitting.value = false
  }
}

const updateEmployeeDisplayName = async (row: TeamProductionEmployeeRespVO) => {
  try {
    const result = await ElMessageBox.prompt('请输入新的显示名；重名时请加后缀区分', '修改显示名', {
      inputValue: row.displayName || row.employeeName || '',
      inputPattern: /\S+/,
      inputErrorMessage: '显示名不能为空',
      confirmButtonText: '保存',
      cancelButtonText: '取消'
    })
    const displayName = String(result.value || '').trim()
    await updateTeamEmployeeDisplayNameRequest({
      employeeProfileId: requirePositiveNumber(row.id, '生产人员档案ID不能为空'),
      displayName
    })
    ElMessage.success('显示名已修改')
    await refreshProductionPersonnel()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, '显示名修改失败，请确认是否重名并按提示加后缀'))
  }
}

const updateEmployeeStatus = async (row: TeamProductionEmployeeRespVO, enabled: boolean) => {
  try {
    await ElMessageBox.confirm(
      enabled
        ? '启用后该员工可重新进入新报工选择。'
        : '禁用后该员工不再进入新报工选择，历史报工和签名继续保留姓名快照。',
      enabled ? '启用生产人员' : '禁用生产人员',
      {
        type: enabled ? 'success' : 'warning',
        confirmButtonText: enabled ? '启用' : '禁用',
        cancelButtonText: '取消'
      }
    )
    await updateTeamEmployeeStatusRequest({
      employeeProfileId: requirePositiveNumber(row.id, '生产人员档案ID不能为空'),
      enabled
    })
    ElMessage.success(enabled ? '员工已启用' : '员工已禁用')
    await refreshProductionPersonnel()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, enabled ? '员工启用失败' : '员工禁用失败'))
  }
}

const resetTemporarySignaturePassword = async (row: TeamProductionEmployeeRespVO) => {
  try {
    const result = await ElMessageBox.prompt('请输入新的临时工电子签名密码', '重置签名密码', {
      inputType: 'password',
      inputPattern: /\S+/,
      inputErrorMessage: '签名密码不能为空',
      confirmButtonText: '重置',
      cancelButtonText: '取消'
    })
    await resetTemporaryTeamEmployeeSignaturePassword({
      employeeProfileId: requirePositiveNumber(row.id, '生产人员档案ID不能为空'),
      signaturePassword: String(result.value || '')
    })
    ElMessage.success('临时工签名密码已重置')
    await refreshProductionPersonnel()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, '临时工签名密码重置失败'))
  }
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

const loadLossReasonRows = async () => {
  if (!isProductionLeader.value) {
    lossReasonRows.value = []
    return
  }
  lossReasonLoading.value = true
  try {
    lossReasonRows.value = await getTeamLeaderLossReasonPage()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '损耗原因标准列表加载失败'))
    throw error
  } finally {
    lossReasonLoading.value = false
  }
}

const formatLossReasonProcess = (row: TeamLeaderLossReasonRowVO) => {
  const sortText = Number.isFinite(Number(row.sort)) ? `${row.sort} - ` : ''
  const processText = row.processName || row.processCode || row.processId || '--'
  return `${sortText}${processText}`
}

const resetLossReasonForm = () => {
  lossReasonForm.reasonCode = ''
  lossReasonForm.reasonName = ''
  lossReasonForm.enabled = true
  lossReasonForm.remark = ''
}

const openCreateLossReason = (row: TeamLeaderLossReasonRowVO) => {
  lossReasonDialogMode.value = 'create'
  lossReasonEditingRow.value = row
  lossReasonEditingReason.value = undefined
  resetLossReasonForm()
  lossReasonDialogVisible.value = true
}

const openEditLossReason = (
  row: TeamLeaderLossReasonRowVO,
  reason: TeamLeaderLossReasonVO
) => {
  lossReasonDialogMode.value = 'edit'
  lossReasonEditingRow.value = row
  lossReasonEditingReason.value = reason
  lossReasonForm.reasonCode = reason.reasonCode
  lossReasonForm.reasonName = reason.reasonName
  lossReasonForm.enabled = reason.enabled
  lossReasonForm.remark = ''
  lossReasonDialogVisible.value = true
}

const submitLossReason = async () => {
  const row = lossReasonEditingRow.value
  if (!row) {
    ElMessage.error('请先选择工序')
    return
  }
  const reasonName = lossReasonForm.reasonName.trim()
  const reasonCode = lossReasonForm.reasonCode.trim()
  if (!reasonName || (lossReasonDialogMode.value === 'create' && !reasonCode)) {
    ElMessage.error('损耗原因编码和名称不能为空')
    return
  }
  lossReasonSubmitting.value = true
  try {
    if (lossReasonDialogMode.value === 'create') {
      await createTeamLeaderLossReason({
        routeProcessId: row.routeProcessId,
        reasonCode,
        reasonName,
        enabled: lossReasonForm.enabled,
        remark: lossReasonForm.remark.trim() || undefined
      })
    } else {
      const reasonId = requirePositiveNumber(
        lossReasonEditingReason.value?.id,
        '损耗原因编号不能为空'
      )
      await updateTeamLeaderLossReason(reasonId, {
        reasonName,
        enabled: lossReasonForm.enabled,
        remark: lossReasonForm.remark.trim() || undefined
      })
    }
    ElMessage.success('损耗原因已保存')
    lossReasonDialogVisible.value = false
    await loadLossReasonRows()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '损耗原因保存失败'))
  } finally {
    lossReasonSubmitting.value = false
  }
}

const handleDeleteLossReason = async (reason: TeamLeaderLossReasonVO) => {
  try {
    await ElMessageBox.confirm(
      `确认删除损耗原因「${reason.reasonCode} / ${reason.reasonName}」？删除后不能用于新报工，历史报工快照不受影响。`,
      '删除损耗原因',
      { type: 'warning' }
    )
    lossReasonSubmitting.value = true
    await deleteTeamLeaderLossReason(reason.id)
    ElMessage.success('损耗原因已删除')
    await loadLossReasonRows()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(resolveErrorMessage(error, '损耗原因删除失败'))
    }
  } finally {
    lossReasonSubmitting.value = false
  }
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
  queryParams.pageNo = 1
  getSubmissionList()
}

const handleLeaderTypeChange = (value: string | number) => {
  const selectedTab = String(value) as WorkbenchLeaderTab
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
    refreshProductionPersonnel()
    loadActiveOrders().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '活跃订单加载失败'))
    })
    loadLossReasonRows().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '损耗原因标准列表加载失败'))
    })
  }
  handleQuery()
}

const resetQuery = () => {
  const leaderType = activeLeaderTab.value
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
  if (!canReviewSubmission(event)) {
    ElMessage.error('已完成复核的提交不能重复复核')
    return
  }
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
  if (reviewForm.reviewStatus === 'REJECTED' && !reviewForm.reviewRemark.trim()) {
    ElMessage.error('退回复核必须填写复核说明')
    return
  }
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
    if (!canCorrectSubmission(event)) {
      ElMessage.error('只有复核不正确的提交可以修正')
      return
    }
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
  if (isProductionLeader.value) {
    refreshProductionPersonnel()
    loadActiveOrders().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '活跃订单调拨库存追溯加载失败'))
    })
    loadLossReasonRows().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '损耗原因标准列表加载失败'))
    })
  }
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

.team-leader-workbench__embedded-header {
  margin-bottom: 14px;
}

.team-leader-workbench__pqc-module-card :deep(.el-card__body) {
  padding-top: 12px;
}

.team-leader-workbench__module-tabs--flat :deep(.el-tabs__header) {
  margin: 0 0 12px;
}

.team-leader-workbench__module-tabs--flat :deep(.el-tabs__item) {
  color: #172033;
  font-weight: 600;
}

.team-leader-workbench__module-tabs--flat :deep(.el-tabs__item.is-active) {
  color: #00a896;
}

.team-leader-workbench__module-tabs--flat :deep(.el-tabs__active-bar) {
  background-color: #00a896;
}

.team-leader-workbench__query {
  margin-bottom: -15px;
}

.team-leader-workbench__form {
  max-width: 760px;
}

.team-leader-workbench__personnel-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.team-leader-workbench__full-control {
  width: 100%;
}

.team-leader-workbench__form-actions {
  margin-top: 14px;
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

.team-leader-workbench__loss-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
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
