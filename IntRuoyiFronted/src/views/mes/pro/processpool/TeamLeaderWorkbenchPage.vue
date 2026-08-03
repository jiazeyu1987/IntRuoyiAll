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
    </el-tabs>
  </ContentWrap>

  <ContentWrap v-if="loadError">
      <el-alert :title="loadError" type="error" :closable="false" show-icon />
    </ContentWrap>

    <ContentWrap data-team-leader-report-workbench>
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
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="success" @click="openReview(row)">复核</el-button>
            <el-button link type="warning" @click="openCorrection(row)">修正</el-button>
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

    <ContentWrap data-role-matrix-daily-close>
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
            <el-descriptions-item label="原始提交内容">
              <pre class="team-leader-workbench__payload">{{
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

const queryFormRef = ref()
const abnormalFormRef = ref()
const activeLeaderTab = ref<TeamLeaderType>('PRODUCTION')
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
const allocationRows = ref<TeamLeaderReportAllocationLine[]>([])
const configuredDefectReasonOptions = ref<
  Array<{ reasonType: string; reasonCode: string; reasonName: string }>
>([])

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
  reviewRemark: ''
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
  routeVersionId: undefined as number | undefined
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

const formatActiveOrderOption = (order: TeamLeaderActiveOrderRespVO) => {
  return `订单 ${order.workOrderId} / 活跃池 ${order.id}`
}

const resetReviewAllocation = () => {
  reviewForm.allocationMode = 'FIFO'
  allocationRows.value = []
}

const loadActiveOrders = async () => {
  activeOrderOptions.value = await getTeamLeaderActiveOrderList()
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
  const leaderType = String(value) as TeamLeaderType
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
    if (isProductionLeader.value && reviewForm.reviewStatus === 'APPROVED') {
      await confirmTeamLeaderReportAllocation({
        eventId,
        leaderType,
        allocationMode: reviewForm.allocationMode,
        reviewRemark,
        allocations: buildAllocationSubmitLines()
      })
    } else {
      await reviewTeamLeaderSubmission({
        leaderType,
        eventId,
        reviewStatus: reviewForm.reviewStatus,
        reviewRemark
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
      routeVersionId: requirePositiveNumber(activeOrderForm.routeVersionId, '路线版本ID不能为空')
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

onMounted(() => getSubmissionList())
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
  .team-leader-workbench__maintenance-grid,
  .team-leader-workbench__daily-close-grid {
    grid-template-columns: 1fr;
  }
}
</style>
