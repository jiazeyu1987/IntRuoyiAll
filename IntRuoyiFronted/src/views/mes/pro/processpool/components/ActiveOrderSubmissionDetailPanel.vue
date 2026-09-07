<template>
  <div v-loading="loading" class="team-leader-workbench__active-order-detail">
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
      show-icon
      data-team-leader-active-order-detail-error
    >
      <template #default>
        <el-button link type="primary" @click="$emit('retry')">重新加载</el-button>
      </template>
    </el-alert>
    <template v-else-if="detail">
      <div class="team-leader-workbench__active-order-detail-summary">
        <div>
          <span>生产订单</span>
          <strong>{{ activeOrderWorkOrderDisplay.workOrderCode }}</strong>
        </div>
        <div>
          <span>工艺路线</span>
          <strong>{{ detail.routeName }}</strong>
        </div>
        <div>
          <span>工序数</span>
          <strong>{{ detail.processes.length }}</strong>
        </div>
      </div>

      <el-tabs
        v-model="activeTab"
        data-team-leader-active-order-detail-main-tabs
        class="team-leader-workbench__active-order-detail-tabs"
      >
        <el-tab-pane label="生产提交" name="productionSubmissions">
          <el-tabs
            v-model="productionActiveTab"
            data-team-leader-active-order-detail-production-process-tabs
            class="team-leader-workbench__active-order-detail-inner-tabs"
          >
            <el-tab-pane
              v-for="(process, processIndex) in detail.processes"
              :key="`${process.routeProcessId}-${process.processId}`"
              :name="activeOrderDetailProcessTabName(process, processIndex)"
            >
              <template #label>
                <span
                  data-team-leader-active-order-detail-production-process-tab
                  :title="process.processName"
                >
                  {{ processIndex + 1 }}. {{ process.processName }}
                </span>
              </template>
              <section
                class="team-leader-workbench__active-order-process-detail"
                :class="{ 'is-quantity-conflict': process.quantityConflict }"
              >
                <div class="team-leader-workbench__active-order-process-header">
                  <div class="team-leader-workbench__active-order-process-title">
                    <strong>{{ process.processName }}</strong>
                    <span v-if="process.processCode">{{ process.processCode }}</span>
                  </div>
                  <div class="team-leader-workbench__active-order-process-metrics">
                    <div>
                      <span>应提数量</span>
                      <strong>{{ formatTraceQuantity(process.requiredQuantity) }}</strong>
                    </div>
                    <div>
                      <span>已提交</span>
                      <strong>{{ formatTraceQuantity(process.submittedQuantity) }}</strong>
                    </div>
                    <div>
                      <span>提交记录</span>
                      <strong>{{ process.submissionCount }}</strong>
                    </div>
                    <div v-if="process.quantityConflict">
                      <span>超出数量</span>
                      <strong class="team-leader-workbench__quantity-conflict-text">
                        {{ formatTraceQuantity(process.overageQuantity) }}
                      </strong>
                    </div>
                  </div>
                  <div class="team-leader-workbench__active-order-process-actions">
                    <el-button
                      size="small"
                      type="primary"
                      plain
                      data-active-order-production-record-form-button
                      @click="openProductionRecordForm(process)"
                    >
                      表单
                    </el-button>
                  </div>
                </div>

                <div
                  class="team-leader-workbench__active-order-detail-table-shell"
                  data-active-order-production-record-input-materials
                >
                  <div class="team-leader-workbench__production-record-section-title">
                    输入物料批次号
                  </div>
                  <el-table
                    v-if="process.inputMaterials?.length"
                    :data="process.inputMaterials"
                    size="small"
                    border
                    :fit="true"
                    table-layout="fixed"
                    row-key="materialCode"
                    class="team-leader-workbench__active-order-submission-table"
                  >
                    <el-table-column label="输入物料编码" width="135">
                      <template #default="{ row }">{{ row.materialCode || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="输入物料名称" width="150">
                      <template #default="{ row }">{{ row.materialName || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="规格型号" width="135">
                      <template #default="{ row }">{{ row.materialSpecification || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="批号" width="170">
                      <template #default="{ row }">{{ formatActiveOrderBatchCodes(row.batchCodes) }}</template>
                    </el-table-column>
                    <el-table-column label="领料单号" width="190">
                      <template #default="{ row }">
                        {{ formatProductionInputMaterialPickListEvidence(row) }}
                      </template>
                    </el-table-column>
                    <el-table-column label="实发数量" width="105">
                      <template #default="{ row }">{{ formatTraceQuantity(row.actualQuantity) }}</template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-else :image-size="48" description="暂无输入物料批次号" />
                </div>

                <div
                  v-if="process.submissions?.length"
                  class="team-leader-workbench__active-order-detail-table-shell"
                >
                  <el-table
                    :data="buildActiveOrderProductionSubmissionRows(process)"
                    size="small"
                    border
                    :fit="true"
                    table-layout="fixed"
                    row-key="key"
                    :row-class-name="resolveActiveOrderSubmissionRowClassName"
                    class="team-leader-workbench__active-order-submission-table"
                  >
                    <el-table-column label="输出物料" width="150">
                      <template #default="{ row }">
                        {{ row.materialName || '-' }}
                      </template>
                    </el-table-column>
                    <el-table-column label="物料编码" width="140">
                      <template #default="{ row }">
                        {{ row.materialCode || '-' }}
                      </template>
                    </el-table-column>
                    <el-table-column label="规格型号" width="140">
                      <template #default="{ row }">
                        {{ row.materialSpecification || '-' }}
                      </template>
                    </el-table-column>
                    <el-table-column label="完成数量" width="120">
                      <template #default="{ row }">
                        {{ formatTraceQuantity(row.outputQuantity ?? row.submittedQuantity) }}
                      </template>
                    </el-table-column>
                    <el-table-column label="损耗数量" width="120">
                      <template #default="{ row }">
                        {{ formatTraceQuantity(row.lossQuantity) }}
                      </template>
                    </el-table-column>
                    <el-table-column label="设备" width="180">
                      <template #default="{ row }">
                        {{ formatActiveOrderSubmissionDevices(row.devices) }}
                      </template>
                    </el-table-column>
                    <el-table-column label="提交人" prop="submitterName" width="110" />
                    <el-table-column label="审核人" width="140">
                      <template #default="{ row }">
                        <span :class="{ 'is-pending': !row.reviewerName }">
                          {{ row.reviewerName || '未审核' }}
                        </span>
                      </template>
                    </el-table-column>
                    <el-table-column label="提交时间" width="160">
                      <template #default="{ row }">
                        {{ formatDateTime(row.submittedAt) }}
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
                <el-empty v-else :image-size="56" description="暂无一线生产提交" />
              </section>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>
        <el-tab-pane label="PQC提交" name="pqcSubmissions">
          <div
            v-if="pqcProcessGroups.length"
            class="team-leader-workbench__active-order-pqc-form-actions"
          >
            <el-button
              size="small"
              type="primary"
              plain
              data-active-order-pqc-inspection-record-form-button
              @click="openPqcInspectionRecordForm"
            >
              表单
            </el-button>
          </div>
          <el-tabs
            v-if="pqcProcessGroups.length"
            v-model="pqcActiveTab"
            data-team-leader-active-order-detail-pqc-process-tabs
            class="team-leader-workbench__active-order-detail-inner-tabs"
          >
            <el-tab-pane
              v-for="(pqcProcess, pqcProcessIndex) in pqcProcessGroups"
              :key="pqcProcess.key"
              :name="resolveActiveOrderPqcProcessTabName(pqcProcess, pqcProcessIndex)"
            >
              <template #label>
                <span
                  data-team-leader-active-order-detail-pqc-process-tab
                  :title="pqcProcess.qaProcessName"
                >
                  {{ pqcProcessIndex + 1 }}. {{ pqcProcess.qaProcessName }}
                </span>
              </template>
              <section class="team-leader-workbench__active-order-process-detail">
                <div class="team-leader-workbench__active-order-process-header">
                  <div class="team-leader-workbench__active-order-process-title">
                    <strong>{{ pqcProcess.qaProcessName }}</strong>
                    <span v-if="pqcProcess.qaProcessCode">{{ pqcProcess.qaProcessCode }}</span>
                  </div>
                  <div class="team-leader-workbench__active-order-process-metrics">
                    <div>
                      <span>PQC提交</span>
                      <strong>{{ pqcProcess.submissions.length }}</strong>
                    </div>
                  </div>
                </div>
                <div
                  v-for="pqcSubmission in pqcProcess.submissions"
                  :key="pqcSubmission.pqcTaskId"
                  class="team-leader-workbench__active-order-pqc-card"
                >
                  <div class="team-leader-workbench__active-order-pqc-title">
                    <strong>
                      {{ resolvePqcInspectionTypeText(pqcSubmission) }}
                      <template v-if="shouldShowPqcRoundNo(pqcSubmission)">
                        / 第 {{ pqcSubmission.roundNo }} 轮
                      </template>
                    </strong>
                    <span>实检 {{ pqcSubmission.actualInspectionQuantity ?? '-' }} 件</span>
                    <span v-if="formatActiveOrderPqcEventIds(pqcSubmission) !== '-'">
                      事件 {{ formatActiveOrderPqcEventIds(pqcSubmission) }}
                    </span>
                  </div>
                  <div class="team-leader-workbench__active-order-pqc-parties">
                    <div>
                      <span>提交人</span>
                      <strong>{{ pqcSubmission.submitterName || '-' }}</strong>
                    </div>
                    <div>
                      <span>审核人</span>
                      <strong :class="{ 'is-pending': !pqcSubmission.reviewerName }">
                        {{ pqcSubmission.reviewerName || '未审核' }}
                      </strong>
                    </div>
                  </div>
                  <div
                    v-if="pqcSubmission.items?.length"
                    class="team-leader-workbench__active-order-detail-table-shell"
                  >
                    <el-table
                      :data="buildActiveOrderPqcItemRows(pqcSubmission)"
                      size="small"
                      border
                      :fit="true"
                      table-layout="fixed"
                      class="team-leader-workbench__active-order-submission-table"
                    >
                      <el-table-column label="检验项" width="180">
                        <template #default="{ row: item }">{{ item.itemNameText }}</template>
                      </el-table-column>
                      <el-table-column label="样本" width="180">
                        <template #default="{ row: item }">{{ item.sampleSummaryText }}</template>
                      </el-table-column>
                      <el-table-column label="结果汇总">
                        <template #default="{ row: item }">{{ item.resultSummaryText }}</template>
                      </el-table-column>
                      <el-table-column label="判定" width="120">
                        <template #default="{ row: item }">{{ item.judgementSummaryText }}</template>
                      </el-table-column>
                      <el-table-column label="设备" width="180">
                        <template #default="{ row: item }">{{ item.equipmentSummaryText }}</template>
                      </el-table-column>
                    </el-table>
                  </div>
                  <el-empty v-else :image-size="56" description="暂无PQC检验明细" />
                </div>
              </section>
            </el-tab-pane>
          </el-tabs>
          <el-empty v-else :image-size="56" description="暂无一线PQC提交" />
        </el-tab-pane>
        <el-tab-pane
          label="领料单"
          name="materials"
          data-team-leader-active-order-detail-material-tab
        >
          <div
            v-if="pickListMaterials.length"
            class="team-leader-workbench__active-order-detail-table-shell"
          >
            <el-table
              :data="pickListMaterials"
              size="small"
              border
              :fit="true"
              table-layout="fixed"
              class="team-leader-workbench__active-order-submission-table"
            >
              <el-table-column label="领料单" width="180">
                <template #default="{ row: material }">
                  <template
                    v-for="link in renderActiveOrderDocumentLinks(material.sourcePickListNos, material.sourcePickListIds, 'pick')"
                    :key="link.key"
                  >
                    <el-button link type="primary" @click="openActiveOrderDocument(link)">
                      {{ link.no }}
                    </el-button>
                  </template>
                  <span v-if="!renderActiveOrderDocumentLinks(material.sourcePickListNos, material.sourcePickListIds, 'pick').length">
                    -
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="物料编码" prop="materialCode" width="140" />
              <el-table-column label="物料名称" prop="materialName" />
              <el-table-column label="规格型号" width="150">
                <template #default="{ row: material }">
                  {{ material.materialSpecification || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="批号" width="180">
                <template #default="{ row: material }">
                  {{ formatActiveOrderBatchCodes(material.batchCodes) }}
                </template>
              </el-table-column>
              <el-table-column label="实发数量" width="120">
                <template #default="{ row: material }">
                  {{ formatTraceQuantity(material.actualQuantity) }}
                </template>
              </el-table-column>
              <el-table-column label="对应工序" width="160">
                <template #default="{ row: material }">
                  {{ material.sourceProcessNames.join('、') || '-' }}
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else :image-size="56" description="暂无领料单物料批号" />
        </el-tab-pane>
        <el-tab-pane
          label="补料单"
          name="replenishmentMaterials"
          data-team-leader-active-order-detail-replenishment-tab
        >
          <div
            v-if="replenishmentMaterials.length"
            class="team-leader-workbench__active-order-detail-table-shell"
          >
            <el-table
              :data="replenishmentMaterials"
              size="small"
              border
              :fit="true"
              table-layout="fixed"
              class="team-leader-workbench__active-order-submission-table"
            >
              <el-table-column label="补料单" width="180">
                <template #default="{ row: material }">
                  <template
                    v-for="link in renderActiveOrderDocumentLinks(material.sourceReplenishmentListNos, material.sourceReplenishmentListIds, 'replenishment')"
                    :key="link.key"
                  >
                    <el-button link type="primary" @click="openActiveOrderDocument(link)">
                      {{ link.no }}
                    </el-button>
                  </template>
                  <span v-if="!renderActiveOrderDocumentLinks(material.sourceReplenishmentListNos, material.sourceReplenishmentListIds, 'replenishment').length">
                    -
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="物料编码" prop="materialCode" width="140" />
              <el-table-column label="物料名称" prop="materialName" />
              <el-table-column label="规格型号" width="150">
                <template #default="{ row: material }">
                  {{ material.materialSpecification || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="批号" width="180">
                <template #default="{ row: material }">
                  {{ formatActiveOrderBatchCodes(material.batchCodes) }}
                </template>
              </el-table-column>
              <el-table-column label="申请数量" width="120">
                <template #default="{ row: material }">
                  {{ formatTraceQuantity(material.requestedQuantity) }}
                </template>
              </el-table-column>
              <el-table-column label="实发数量" width="120">
                <template #default="{ row: material }">
                  {{ formatTraceQuantity(material.actualQuantity) }}
                </template>
              </el-table-column>
              <el-table-column label="对应工序" width="160">
                <template #default="{ row: material }">
                  {{ material.sourceProcessNames.join('、') || '-' }}
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else :image-size="56" description="暂无补料单物料批号" />
        </el-tab-pane>
        <el-tab-pane
          label="生产工单"
          name="workOrder"
          data-team-leader-active-order-detail-work-order-tab
        >
          <div
            class="team-leader-workbench__work-order-highlight-grid"
            data-active-order-work-order-highlight
          >
            <div class="team-leader-workbench__work-order-highlight-card is-primary">
              <span>工单编号</span>
              <el-button
                link
                type="primary"
                class="team-leader-workbench__work-order-highlight-value"
                data-active-order-work-order-link
                @click="openWorkOrderList(activeOrderWorkOrderDisplay.workOrderCode)"
              >
                {{ activeOrderWorkOrderDisplay.workOrderCode }}
              </el-button>
            </div>
            <div class="team-leader-workbench__work-order-highlight-card is-primary">
              <span>批次号</span>
              <strong class="team-leader-workbench__work-order-highlight-value">
                {{ activeOrderWorkOrderDisplay.batchCode }}
              </strong>
            </div>
            <div class="team-leader-workbench__work-order-highlight-card is-primary">
              <span>数量</span>
              <strong class="team-leader-workbench__work-order-highlight-value">
                {{ activeOrderWorkOrderDisplay.quantity }}
              </strong>
            </div>
            <div class="team-leader-workbench__work-order-highlight-card">
              <span>规格型号</span>
              <strong>{{ activeOrderWorkOrderDisplay.productSpecification }}</strong>
            </div>
            <div class="team-leader-workbench__work-order-highlight-card">
              <span>产品代码</span>
              <strong>{{ activeOrderWorkOrderDisplay.productCode }}</strong>
            </div>
            <div class="team-leader-workbench__work-order-highlight-card">
              <span>产品名称</span>
              <strong>{{ activeOrderWorkOrderDisplay.productName }}</strong>
            </div>
            <div class="team-leader-workbench__work-order-highlight-card">
              <span>创建时间</span>
              <strong>{{ activeOrderWorkOrderDisplay.createTime }}</strong>
            </div>
          </div>
          <div
            class="team-leader-workbench__active-order-detail-table-shell team-leader-workbench__work-order-list-shell"
            data-active-order-work-order-standard-list
          >
            <el-table
              :data="[activeOrderWorkOrderDisplay]"
              size="small"
              border
              :fit="true"
              table-layout="fixed"
              class="team-leader-workbench__active-order-submission-table"
            >
              <el-table-column label="批次号">
                <template #default="{ row }">
                  <strong class="team-leader-workbench__work-order-list-emphasis">
                    {{ row.batchCode }}
                  </strong>
                </template>
              </el-table-column>
              <el-table-column label="工单编号">
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    class="team-leader-workbench__work-order-list-link"
                    data-active-order-work-order-list-link
                    @click="openWorkOrderList(row.workOrderCode)"
                  >
                    {{ row.workOrderCode }}
                  </el-button>
                </template>
              </el-table-column>
              <el-table-column label="数量">
                <template #default="{ row }">
                  <strong class="team-leader-workbench__work-order-list-emphasis">
                    {{ row.quantity }}
                  </strong>
                </template>
              </el-table-column>
              <el-table-column label="规格型号">
                <template #default="{ row }">{{ row.productSpecification }}</template>
              </el-table-column>
              <el-table-column label="产品代码">
                <template #default="{ row }">{{ row.productCode }}</template>
              </el-table-column>
              <el-table-column label="产品名称">
                <template #default="{ row }">{{ row.productName }}</template>
              </el-table-column>
              <el-table-column label="创建时间">
                <template #default="{ row }">{{ row.createTime }}</template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
      <el-dialog
        v-model="productionRecordFormVisible"
        width="1180px"
        class="team-leader-workbench__production-record-form-dialog"
        data-active-order-production-record-form-dialog
        :title="`${selectedProductionRecordProcess?.processName || ''}生产记录表单`"
      >
        <section
          v-if="selectedProductionRecordProcess"
          class="team-leader-workbench__production-record-form"
        >
          <div class="team-leader-workbench__production-record-form-title">
            <h3>生产记录表单</h3>
            <span>{{ selectedProductionRecordProcess.processName }}</span>
          </div>
          <div class="team-leader-workbench__production-record-meta">
            <div>
              <span>工序名称</span>
              <strong>{{ selectedProductionRecordProcess.processName }}</strong>
            </div>
            <div>
              <span>生产批号</span>
              <strong>{{ detail.batchCode || '未记录' }}</strong>
            </div>
            <div>
              <span>产品规格</span>
              <strong>{{ detail.productSpecification || '未记录' }}</strong>
            </div>
            <div>
              <span>生产订单</span>
              <strong>{{ detail.workOrderCode }}</strong>
            </div>
          </div>
          <div
            class="team-leader-workbench__production-record-input-materials"
            data-active-order-production-record-input-materials
          >
            <div class="team-leader-workbench__production-record-section-title">
              输入物料批次号
            </div>
            <el-table
              v-if="selectedProductionInputMaterials.length"
              :data="selectedProductionInputMaterials"
              size="small"
              border
              :fit="true"
              table-layout="fixed"
              row-key="materialCode"
              class="team-leader-workbench__production-record-table"
            >
              <el-table-column label="输入物料编码" width="135">
                <template #default="{ row }">{{ row.materialCode || '未记录' }}</template>
              </el-table-column>
              <el-table-column label="输入物料名称" width="150">
                <template #default="{ row }">{{ row.materialName || '未记录' }}</template>
              </el-table-column>
              <el-table-column label="规格型号" width="135">
                <template #default="{ row }">{{ row.materialSpecification || '未记录' }}</template>
              </el-table-column>
              <el-table-column label="批号" width="170">
                <template #default="{ row }">{{ formatActiveOrderBatchCodes(row.batchCodes) }}</template>
              </el-table-column>
              <el-table-column label="领料单号" width="190">
                <template #default="{ row }">
                  {{ formatProductionInputMaterialPickListEvidence(row) }}
                </template>
              </el-table-column>
              <el-table-column label="实发数量" width="105">
                <template #default="{ row }">{{ formatTraceQuantity(row.actualQuantity) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else :image-size="48" description="暂无输入物料批次号" />
          </div>
          <el-table
            v-if="selectedProductionRecordRows.length"
            :data="selectedProductionRecordRows"
            size="small"
            border
            :fit="true"
            table-layout="fixed"
            row-key="key"
            class="team-leader-workbench__production-record-table"
          >
            <el-table-column label="提交时间" width="150">
              <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
            </el-table-column>
            <el-table-column label="提交人" prop="submitterName" width="100" />
            <el-table-column label="复核人" width="100">
              <template #default="{ row }">{{ row.reviewerName || '未审核' }}</template>
            </el-table-column>
            <el-table-column label="输出物料编码" width="135">
              <template #default="{ row }">{{ row.materialCode || '未记录' }}</template>
            </el-table-column>
            <el-table-column label="输出物料名称" width="130">
              <template #default="{ row }">{{ row.materialName || '未记录' }}</template>
            </el-table-column>
            <el-table-column label="规格型号" width="135">
              <template #default="{ row }">{{ row.materialSpecification || '未记录' }}</template>
            </el-table-column>
            <el-table-column label="生产数量" width="105">
              <template #default="{ row }">{{ formatTraceQuantity(row.outputQuantity) }}</template>
            </el-table-column>
            <el-table-column label="损耗数量" width="105">
              <template #default="{ row }">{{ formatTraceQuantity(row.lossQuantity) }}</template>
            </el-table-column>
            <el-table-column label="总数量" width="105">
              <template #default="{ row }">{{ formatTraceQuantity(row.totalQuantity) }}</template>
            </el-table-column>
            <el-table-column label="清场确认" width="95">
              <template #default="{ row }">
                {{ formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'workplace') }}
              </template>
            </el-table-column>
            <el-table-column label="物料确认" width="95">
              <template #default="{ row }">
                {{ formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'material') }}
              </template>
            </el-table-column>
            <el-table-column label="清洁确认" width="95">
              <template #default="{ row }">
                {{ formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'cleaning') }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else :image-size="56" description="暂无可生成的生产提交记录" />
          <div
            v-if="selectedProductionRecordMaterialDeviceGroups.length"
            class="team-leader-workbench__production-record-section-title"
          >
            物料使用设备 / 设备参数
          </div>
          <div
            v-if="selectedProductionRecordMaterialDeviceGroups.length"
            class="team-leader-workbench__production-record-device-groups"
          >
            <section
              v-for="materialDeviceGroup in selectedProductionRecordMaterialDeviceGroups"
              :key="materialDeviceGroup.key"
              class="team-leader-workbench__production-record-device-group"
              data-active-order-production-record-material-device-group
            >
              <div class="team-leader-workbench__production-record-device-group-title">
                <span>输出物料</span>
                <strong>{{ materialDeviceGroup.materialText }}</strong>
              </div>
              <el-table
                :data="materialDeviceGroup.devices"
                size="small"
                border
                :fit="true"
                table-layout="fixed"
                row-key="key"
                class="team-leader-workbench__production-record-table"
                data-active-order-production-record-device-table
              >
                <el-table-column label="设备名称" width="150">
                  <template #default="{ row }">{{ row.deviceNameText }}</template>
                </el-table-column>
                <el-table-column label="设备编号" width="130">
                  <template #default="{ row }">{{ row.deviceCodeText }}</template>
                </el-table-column>
                <el-table-column label="计量状态" width="130">
                  <template #default="{ row }">
                    <span
                      :class="{ 'team-leader-workbench__production-record-warning': row.meteringOutOfPeriod }"
                    >
                      {{ row.meteringValidityText }}
                    </span>
                  </template>
                </el-table-column>
              </el-table>
              <div
                v-for="deviceGroup in materialDeviceGroup.devices"
                :key="`${materialDeviceGroup.key}-${deviceGroup.key}`"
                class="team-leader-workbench__production-record-parameter-group"
              >
                <div class="team-leader-workbench__production-record-parameter-title">
                  设备参数：{{ deviceGroup.deviceNameText }} / {{ deviceGroup.deviceCodeText }}
                </div>
                <el-table
                  :data="deviceGroup.parameters"
                  size="small"
                  border
                  :fit="true"
                  table-layout="fixed"
                  row-key="key"
                  class="team-leader-workbench__production-record-table"
                  data-active-order-production-record-device-parameter-table
                >
                  <el-table-column label="参数名称" width="160">
                    <template #default="{ row: parameter }">
                      {{ parameter.parameterNameText }}
                    </template>
                  </el-table-column>
                  <el-table-column label="参数范围" width="150">
                    <template #default="{ row: parameter }">
                      {{ parameter.parameterRangeText }}
                    </template>
                  </el-table-column>
                  <el-table-column label="提交值" width="130">
                    <template #default="{ row: parameter }">
                      <span
                        :class="{ 'team-leader-workbench__production-record-warning': parameter.outOfRange }"
                        :data-parameter-status="parameter.parameterStatus"
                        data-active-order-production-record-parameter-value
                      >
                        {{ parameter.parameterValueText }}
                      </span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </section>
          </div>
          <div class="team-leader-workbench__production-record-totals">
            <span>生产数量合计：{{ formatTraceQuantity(productionRecordTotals.outputQuantity) }}</span>
            <span>损耗数量合计：{{ formatTraceQuantity(productionRecordTotals.lossQuantity) }}</span>
            <strong>总数量合计：{{ formatTraceQuantity(productionRecordTotals.totalQuantity) }}</strong>
          </div>
        </section>
      </el-dialog>
      <el-dialog
        v-model="pqcInspectionRecordFormVisible"
        width="96vw"
        class="team-leader-workbench__pqc-inspection-record-form-dialog"
        data-active-order-pqc-inspection-record-form-dialog
        title="过程检验记录"
      >
        <section class="team-leader-workbench__pqc-inspection-record-form">
          <div class="team-leader-workbench__production-record-form-title">
            <h3>过程检验记录</h3>
            <span>{{ detail.routeName }}</span>
          </div>
          <div class="team-leader-workbench__production-record-meta">
            <div>
              <span>生产订单</span>
              <strong>{{ detail.workOrderCode }}</strong>
            </div>
            <div>
              <span>生产批号</span>
              <strong>{{ detail.batchCode || '未记录' }}</strong>
            </div>
            <div>
              <span>产品规格</span>
              <strong>{{ detail.productSpecification || '未记录' }}</strong>
            </div>
            <div>
              <span>检验数量</span>
              <strong>{{ pqcInspectionRecordSummary.inspectionQuantityText }}</strong>
            </div>
            <div>
              <span>PQC提交</span>
              <strong>{{ pqcInspectionRecordSummary.submissionCount }} 次</strong>
            </div>
            <div>
              <span>检验项目</span>
              <strong>{{ pqcInspectionRecordSummary.itemCount }} 项</strong>
            </div>
            <div>
              <span>检验结论</span>
              <strong>{{ pqcInspectionRecordSummary.judgementText }}</strong>
            </div>
            <div>
              <span>检验设备</span>
              <strong>{{ pqcInspectionRecordSummary.equipmentText }}</strong>
            </div>
          </div>
          <table
            v-if="pqcInspectionRecordRows.length"
            class="team-leader-workbench__pqc-inspection-record-table"
            data-pqc-inspection-record-form-table
            aria-label="过程检验记录"
          >
            <colgroup>
              <col class="team-leader-workbench__pqc-inspection-record-col-sequence" />
              <col class="team-leader-workbench__pqc-inspection-record-col-date" />
              <col class="team-leader-workbench__pqc-inspection-record-col-project" />
              <col class="team-leader-workbench__pqc-inspection-record-col-item" />
              <col class="team-leader-workbench__pqc-inspection-record-col-method" />
              <col class="team-leader-workbench__pqc-inspection-record-col-standard" />
              <col class="team-leader-workbench__pqc-inspection-record-col-type" />
              <col class="team-leader-workbench__pqc-inspection-record-col-quantity" />
              <col class="team-leader-workbench__pqc-inspection-record-col-result" />
              <col class="team-leader-workbench__pqc-inspection-record-col-equipment" />
              <col class="team-leader-workbench__pqc-inspection-record-col-judgement" />
              <col class="team-leader-workbench__pqc-inspection-record-col-person" />
              <col class="team-leader-workbench__pqc-inspection-record-col-person" />
            </colgroup>
            <thead>
              <tr>
                <th>序号</th>
                <th>检验日期</th>
                <th colspan="4">检验项目</th>
                <th>检验类型</th>
                <th>检测数量 pcs</th>
                <th>检测结果</th>
                <th>检验设备</th>
                <th>判定</th>
                <th>检验人/日期</th>
                <th>复核人/日期</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in pqcInspectionRecordRows" :key="row.key">
                <td
                  v-if="row.isSequenceFirstRow"
                  :rowspan="row.sequenceRowSpan"
                  class="team-leader-workbench__pqc-inspection-record-center-cell"
                >
                  {{ row.sequenceText }}
                </td>
                <td
                  v-if="row.isInspectionDateFirstRow"
                  :rowspan="row.inspectionDateRowSpan"
                  class="team-leader-workbench__pqc-inspection-record-center-cell"
                >
                  {{ row.inspectionDateText }}
                </td>
                <td
                  v-if="row.isQaProcessFirstRow"
                  :rowspan="row.qaProcessRowSpan"
                  class="team-leader-workbench__pqc-inspection-record-common-cell"
                  data-pqc-inspection-record-common-cell
                  data-pqc-inspection-record-common-process
                >
                  {{ row.qaProcessName }}
                </td>
                <td
                  v-if="row.isItemFirstRow"
                  :rowspan="row.itemRowSpan"
                  class="team-leader-workbench__pqc-inspection-record-common-cell"
                  data-pqc-inspection-record-common-cell
                  data-pqc-inspection-record-common-item
                >
                  {{ row.itemNameText }}
                </td>
                <td
                  v-if="row.isMethodFirstRow"
                  :rowspan="row.methodRowSpan"
                  class="team-leader-workbench__pqc-inspection-record-common-cell"
                  data-pqc-inspection-record-common-cell
                  data-pqc-inspection-record-common-method
                >
                  {{ row.inspectionMethodText }}
                </td>
                <td
                  v-if="row.isStandardFirstRow"
                  :rowspan="row.standardRowSpan"
                  class="team-leader-workbench__pqc-inspection-record-common-cell"
                  data-pqc-inspection-record-common-cell
                  data-pqc-inspection-record-common-standard
                >
                  {{ row.standardText }}
                </td>
                <td class="team-leader-workbench__pqc-inspection-record-type-cell">
                  {{ row.inspectionTypeText }}
                </td>
                <td class="team-leader-workbench__pqc-inspection-record-center-cell">
                  {{ row.inspectionQuantityText }}
                </td>
                <td>{{ row.resultText }}</td>
                <td
                  v-if="row.isEquipmentFirstRow"
                  :rowspan="row.equipmentRowSpan"
                  class="team-leader-workbench__pqc-inspection-record-center-cell"
                  data-pqc-inspection-record-equipment-cell
                >
                  {{ row.equipmentText }}
                </td>
                <td class="team-leader-workbench__pqc-inspection-record-judgement-cell">
                  {{ row.judgementText }}
                </td>
                <td>{{ row.inspectorText }}</td>
                <td>{{ row.reviewerText }}</td>
              </tr>
            </tbody>
          </table>
          <el-empty v-else :image-size="56" description="暂无PQC检验记录" />
        </section>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type {
  TeamLeaderActiveOrderDetailRespVO,
  TeamLeaderActiveOrderInputMaterialDetailRespVO,
  TeamLeaderActiveOrderPqcSubmissionDetailRespVO,
  TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO,
  TeamLeaderActiveOrderProcessDetailRespVO,
  TeamLeaderActiveOrderClearanceConfirmationRespVO,
  TeamLeaderActiveOrderSubmissionDetailRespVO,
  TeamLeaderActiveOrderSubmissionDeviceDetailRespVO,
  TeamLeaderActiveOrderSubmissionDeviceParameterRespVO,
  TeamLeaderActiveOrderSubmissionMaterialDetailRespVO,
  TeamLeaderActiveOrderSupplementMaterialDetailRespVO
} from '@/api/mes/pro/processpool/teamLeader'
import type { ProWorkOrderVO } from '@/api/mes/pro/workorder'
import { formatDateTimeValue } from '@/utils/formatTime'

const props = defineProps<{
  detail?: TeamLeaderActiveOrderDetailRespVO
  sourceWorkOrder?: ProWorkOrderVO
  loading?: boolean
  error?: string
}>()

defineEmits<{
  retry: []
}>()

const activeTab = ref('')
const productionActiveTab = ref('')
const pqcActiveTab = ref('')
const productionRecordFormVisible = ref(false)
const selectedProductionRecordProcess = ref<TeamLeaderActiveOrderProcessDetailRespVO>()
const pqcInspectionRecordFormVisible = ref(false)
const router = useRouter()

const formatDateTime = (value?: string | number | Date) => formatDateTimeValue(value)

const formatIntegerQuantity = (value: number | string | undefined) => {
  if (value === undefined || value === null || value === '') return '-'
  const parsed = Number(value)
  return Number.isFinite(parsed) ? String(Math.round(parsed)) : String(value)
}

const formatTraceQuantity = (value: number | string | undefined) => {
  return formatIntegerQuantity(value)
}

const formatActiveOrderBatchCodes = (batchCodes?: string[]) => {
  const normalized = (batchCodes ?? []).map((code) => String(code).trim()).filter(Boolean)
  return normalized.length ? normalized.join('、') : '-'
}

type ActiveOrderDocumentType = 'pick' | 'replenishment'

interface ActiveOrderDocumentLink {
  key: string
  id?: number
  no: string
  type: ActiveOrderDocumentType
}

const renderActiveOrderDocumentLinks = (
  sourceNos?: string[],
  sourceIds?: number[],
  type?: ActiveOrderDocumentType
): ActiveOrderDocumentLink[] => {
  return (sourceNos ?? [])
    .map((sourceNo, index) => ({
      key: `${type || 'document'}-${sourceIds?.[index] || index}-${sourceNo}`,
      id: sourceIds?.[index],
      no: String(sourceNo).trim(),
      type: type || 'pick'
    }))
    .filter((link) => link.no)
}

const openActiveOrderDocument = (link: ActiveOrderDocumentLink) => {
  if (link.type === 'pick') {
    router.push({
      path: '/erp/production/pick-list',
      query: {
        sourceBillNo: link.no
      }
    })
    return
  }
  router.push({
    path: '/erp/production/replenishment-list',
    query: {
      sourceBillNo: link.no
    }
  })
}

const openWorkOrderList = (workOrderCode: string) => {
  router.push({
    path: '/mes/pro/work-order',
    query: { code: workOrderCode }
  })
}

const activeOrderWorkOrderDisplay = computed(() => {
  const source = props.sourceWorkOrder
  const detail = props.detail
  if (source) {
    return {
      batchCode: source.batchCode || '-',
      workOrderCode: source.code || '-',
      quantity: formatTraceQuantity(source.quantity),
      productSpecification: source.productSpecification || '-',
      productCode: source.productCode || '-',
      productName: source.productName || '-',
      createTime: formatDateTime(source.createTime)
    }
  }
  return {
    batchCode: detail?.batchCode || '-',
    workOrderCode: detail?.workOrderCode || '-',
    quantity: formatTraceQuantity(detail?.workOrderQuantity),
    productSpecification: detail?.productSpecification || '-',
    productCode: detail?.productCode || '-',
    productName: detail?.productName || '-',
    createTime: formatDateTime(detail?.workOrderCreateTime)
  }
})

const formatActiveOrderSubmissionDevices = (
  devices?: TeamLeaderActiveOrderSubmissionDeviceDetailRespVO[]
) => {
  const normalized = (devices ?? [])
    .map((device) =>
      [device.deviceName, device.deviceCode]
        .map((value) => String(value || '').trim())
        .filter(Boolean)
        .join(' / ')
    )
    .filter(Boolean)
  return normalized.length ? normalized.join('、') : '-'
}

const formatActiveOrderPqcEventIds = (
  pqcSubmission: TeamLeaderActiveOrderPqcSubmissionDetailRespVO
) => {
  const ids = (pqcSubmission.submittedEventIds?.length
    ? pqcSubmission.submittedEventIds
    : pqcSubmission.submittedEventId
      ? [pqcSubmission.submittedEventId]
      : []
  )
    .map((id) => Number(id))
    .filter((id) => Number.isFinite(id) && id > 0)
    .sort((left, right) => left - right)
  const uniqueIds = Array.from(new Set(ids))
  return uniqueIds.length ? uniqueIds.join('、') : '-'
}

interface ActiveOrderPqcItemAggregateRow {
  key: string
  itemIdentityKey: string
  itemNameText: string
  sampleSummaryText: string
  resultSummaryText: string
  judgementSummaryText: string
  equipmentSummaryText: string
}

interface ActiveOrderPqcInspectionRecordRow {
  key: string
  sequenceText: string
  sequenceRowSpan: number
  isSequenceFirstRow: boolean
  inspectionDateText: string
  inspectionDateRowSpan: number
  isInspectionDateFirstRow: boolean
  qaProcessRowSpan: number
  isQaProcessFirstRow: boolean
  itemRowSpan: number
  isItemFirstRow: boolean
  methodRowSpan: number
  isMethodFirstRow: boolean
  standardRowSpan: number
  isStandardFirstRow: boolean
  equipmentRowSpan: number
  isEquipmentFirstRow: boolean
  qaProcessName: string
  inspectionTypeText: string
  itemIdentityKey: string
  itemNameText: string
  inspectionMethodText: string
  standardText: string
  inspectionQuantityText: string
  resultText: string
  sampleSummaryText: string
  measuredValuesText: string
  judgementText: string
  equipmentText: string
  submitterName: string
  reviewerName: string
  inspectorText: string
  reviewerText: string
}

interface ActiveOrderProductionSubmissionRow {
  key: string
  eventId: number
  materialId?: number
  materialCode?: string
  materialName?: string
  materialSpecification?: string
  submittedQuantity: number | string
  outputQuantity?: number | string
  lossQuantity?: number | string
  submitterName: string
  reviewerName?: string
  submittedAt: string | number
  quantityConflict?: boolean
  devices: TeamLeaderActiveOrderSubmissionDeviceDetailRespVO[]
}

interface ActiveOrderProductionRecordRow extends ActiveOrderProductionSubmissionRow {
  deviceParameters: TeamLeaderActiveOrderSubmissionDeviceParameterRespVO[]
  clearanceConfirmations: TeamLeaderActiveOrderClearanceConfirmationRespVO[]
  totalQuantity?: number | string
}

interface ActiveOrderProductionRecordParameterRow {
  key: string
  parameterNameText: string
  parameterRangeText: string
  parameterValueText: string
  parameterStatus: string
  outOfRange: boolean
}

interface ActiveOrderProductionRecordDeviceGroup {
  key: string
  deviceNameText: string
  deviceCodeText: string
  meteringValidityText: string
  meteringOutOfPeriod: boolean
  parameters: ActiveOrderProductionRecordParameterRow[]
}

interface ActiveOrderProductionRecordMaterialDeviceGroup {
  key: string
  materialText: string
  devices: ActiveOrderProductionRecordDeviceGroup[]
}

const buildActiveOrderProductionSubmissionRows = (
  process: TeamLeaderActiveOrderProcessDetailRespVO
): ActiveOrderProductionSubmissionRow[] => {
  const rows: ActiveOrderProductionSubmissionRow[] = []
  for (const submission of process.submissions ?? []) {
    for (const [materialIndex, material] of (submission.materials ?? []).entries()) {
      rows.push(toActiveOrderProductionMaterialSubmissionRow(submission, material, materialIndex))
    }
    if (!submission.materials?.length) {
      rows.push(toActiveOrderProductionEventSubmissionRow(submission))
    }
  }
  return rows
}

const toActiveOrderProductionMaterialSubmissionRow = (
  submission: TeamLeaderActiveOrderSubmissionDetailRespVO,
  material: TeamLeaderActiveOrderSubmissionMaterialDetailRespVO,
  materialIndex: number
): ActiveOrderProductionSubmissionRow => ({
  key: `${submission.eventId}-${materialIndex}-${material.materialId}-${material.materialCode}`,
  eventId: submission.eventId,
  materialId: material.materialId,
  materialCode: material.materialCode,
  materialName: material.materialName,
  materialSpecification: material.materialSpecification,
  submittedQuantity: submission.submittedQuantity,
  outputQuantity: material.outputQuantity,
  lossQuantity: material.lossQuantity,
  submitterName: submission.submitterName,
  reviewerName: submission.reviewerName,
  submittedAt: submission.submittedAt,
  quantityConflict: submission.quantityConflict,
  devices: material.devices?.length ? material.devices : submission.devices
})

const toActiveOrderProductionEventSubmissionRow = (
  submission: TeamLeaderActiveOrderSubmissionDetailRespVO
): ActiveOrderProductionSubmissionRow => ({
  key: `${submission.eventId}`,
  eventId: submission.eventId,
  submittedQuantity: submission.submittedQuantity,
  submitterName: submission.submitterName,
  reviewerName: submission.reviewerName,
  submittedAt: submission.submittedAt,
  quantityConflict: submission.quantityConflict,
  devices: submission.devices
})

const openProductionRecordForm = (process: TeamLeaderActiveOrderProcessDetailRespVO) => {
  selectedProductionRecordProcess.value = process
  productionRecordFormVisible.value = true
}

const openPqcInspectionRecordForm = () => {
  pqcInspectionRecordFormVisible.value = true
}

const toNumberOrUndefined = (value?: number | string) => {
  if (value === undefined || value === null || value === '') return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const normalizeProductionRecordLossQuantity = (value?: number | string) => {
  const parsed = toNumberOrUndefined(value)
  if (parsed === undefined) return 0
  return parsed
}

const sumProductionRecordQuantities = (...values: Array<number | string | undefined>) =>
  values.reduce<number>((total, value) => total + (toNumberOrUndefined(value) ?? 0), 0)

const buildProductionRecordRows = (
  process?: TeamLeaderActiveOrderProcessDetailRespVO
): ActiveOrderProductionRecordRow[] => {
  if (!process) return []
  const rows: ActiveOrderProductionRecordRow[] = []
  for (const submission of process.submissions ?? []) {
    const submissionDeviceParameters = submission.deviceParameters ?? []
    if (submission.materials?.length) {
      for (const [materialIndex, material] of submission.materials.entries()) {
        const outputQuantity = material.outputQuantity ?? submission.submittedQuantity
        const lossQuantity = normalizeProductionRecordLossQuantity(material.lossQuantity)
        const deviceParameters = material.deviceParameters?.length
          ? material.deviceParameters
          : submissionDeviceParameters
        rows.push({
          ...toActiveOrderProductionMaterialSubmissionRow(submission, material, materialIndex),
          outputQuantity,
          lossQuantity,
          totalQuantity: sumProductionRecordQuantities(outputQuantity, lossQuantity),
          deviceParameters,
          clearanceConfirmations: material.clearanceConfirmations?.length
            ? material.clearanceConfirmations
            : submission.clearanceConfirmations ?? []
        })
      }
      continue
    }
    rows.push({
      ...toActiveOrderProductionEventSubmissionRow(submission),
      outputQuantity: submission.submittedQuantity,
      lossQuantity: 0,
      totalQuantity: sumProductionRecordQuantities(submission.submittedQuantity, 0),
      deviceParameters: submissionDeviceParameters,
      clearanceConfirmations: submission.clearanceConfirmations ?? []
    })
  }
  return rows
}

const selectedProductionRecordRows = computed(() =>
  buildProductionRecordRows(selectedProductionRecordProcess.value)
)

const selectedProductionRecordMaterialDeviceGroups = computed(() =>
  buildProductionRecordMaterialDeviceGroups(selectedProductionRecordRows.value)
)

const selectedProductionInputMaterials = computed(
  () => selectedProductionRecordProcess.value?.inputMaterials ?? []
)

const productionRecordTotals = computed(() => {
  const rows = selectedProductionRecordRows.value
  const sum = (field: keyof Pick<ActiveOrderProductionRecordRow, 'outputQuantity' | 'lossQuantity' | 'totalQuantity'>) =>
    rows.reduce((total, row) => total + (toNumberOrUndefined(row[field]) ?? 0), 0)
  return {
    outputQuantity: sum('outputQuantity'),
    lossQuantity: sum('lossQuantity'),
    totalQuantity: sum('totalQuantity')
  }
})

const formatProductionRecordParameterValue = (
  parameter: TeamLeaderActiveOrderSubmissionDeviceParameterRespVO
) => {
  const value = parameter.textValue || parameter.value
  if (value === undefined || value === null || value === '') return '未记录'
  return `${value}${parameter.unit || ''}`
}

const formatProductionRecordMaterialText = (row: ActiveOrderProductionRecordRow) => {
  const name = String(row.materialName || '').trim()
  const code = String(row.materialCode || '').trim()
  return [name, code].filter(Boolean).join(' / ') || '未记录'
}

const formatProductionRecordDeviceNameText = (
  device?: TeamLeaderActiveOrderSubmissionDeviceDetailRespVO
) => String(device?.deviceName || '').trim() || '未记录'

const formatProductionRecordDeviceCodeText = (
  device?: TeamLeaderActiveOrderSubmissionDeviceDetailRespVO
) => String(device?.deviceCode || (device?.deviceId ? `#${device.deviceId}` : '')).trim() || '未记录'

const formatProductionRecordDeviceMeteringValidityText = (value?: boolean) => {
  if (value === true) return '计量有效'
  if (value === false) return '计量超期'
  return '计量状态未记录'
}

const formatProductionRecordLimitText = (value?: number | string, unit = '') => {
  if (value === undefined || value === null || value === '') return ''
  return `${value}${unit}`
}

const formatProductionRecordParameterRange = (
  parameter?: TeamLeaderActiveOrderSubmissionDeviceParameterRespVO
) => {
  const unit = parameter?.unit || ''
  const lower = formatProductionRecordLimitText(parameter?.lowerLimit, unit)
  const upper = formatProductionRecordLimitText(parameter?.upperLimit, unit)
  if (lower && upper) return `${lower} ~ ${upper}`
  if (lower) return `≥ ${lower}`
  if (upper) return `≤ ${upper}`
  return '无'
}

const isProductionRecordParameterOutOfRange = (
  parameter?: TeamLeaderActiveOrderSubmissionDeviceParameterRespVO
) => {
  if (!parameter) return false
  if (parameter.parameterStatus && parameter.parameterStatus !== 'NORMAL') return true
  const value = toNumberOrUndefined(parameter.value)
  if (value === undefined) return false
  const lowerLimit = toNumberOrUndefined(parameter.lowerLimit)
  const upperLimit = toNumberOrUndefined(parameter.upperLimit)
  return (
    (lowerLimit !== undefined && value < lowerLimit) ||
    (upperLimit !== undefined && value > upperLimit)
  )
}

const resolveProductionRecordDeviceKey = (
  device?: TeamLeaderActiveOrderSubmissionDeviceDetailRespVO,
  index = 0
) => String(device?.deviceId || device?.deviceCode || device?.deviceName || `device-${index}`)

const hasProductionRecordDeviceIdentity = (
  device?: Pick<TeamLeaderActiveOrderSubmissionDeviceDetailRespVO, 'deviceId' | 'deviceCode' | 'deviceName'>
) => Boolean(device?.deviceId || device?.deviceCode || device?.deviceName)

const isSameProductionRecordDevice = (
  parameter: TeamLeaderActiveOrderSubmissionDeviceParameterRespVO,
  device: TeamLeaderActiveOrderSubmissionDeviceDetailRespVO,
  deviceCount: number
) => {
  if (parameter.deviceId && device.deviceId) return parameter.deviceId === device.deviceId
  if (parameter.deviceCode && device.deviceCode) return parameter.deviceCode === device.deviceCode
  if (parameter.deviceName && device.deviceName) return parameter.deviceName === device.deviceName
  return deviceCount === 1 && !parameter.deviceId && !parameter.deviceCode && !parameter.deviceName
}

const toProductionRecordParameterRow = (
  parameter: TeamLeaderActiveOrderSubmissionDeviceParameterRespVO | undefined,
  key: string
): ActiveOrderProductionRecordParameterRow => {
  const outOfRange = isProductionRecordParameterOutOfRange(parameter)
  const parameterStatus = parameter?.parameterStatus || (outOfRange ? 'ABNORMAL' : 'NORMAL')
  return {
    key,
    parameterNameText: parameter?.parameterName || parameter?.parameterCode || '暂无设备参数',
    parameterRangeText: parameter ? formatProductionRecordParameterRange(parameter) : '-',
    parameterValueText: parameter ? formatProductionRecordParameterValue(parameter) : '-',
    parameterStatus,
    outOfRange
  }
}

const toProductionRecordDeviceGroup = (
  device: TeamLeaderActiveOrderSubmissionDeviceDetailRespVO | undefined,
  parameters: TeamLeaderActiveOrderSubmissionDeviceParameterRespVO[],
  key: string
): ActiveOrderProductionRecordDeviceGroup => ({
  key,
  deviceNameText: formatProductionRecordDeviceNameText(device),
  deviceCodeText: formatProductionRecordDeviceCodeText(device),
  meteringValidityText: formatProductionRecordDeviceMeteringValidityText(
    device?.inMeteringValidityPeriod
  ),
  meteringOutOfPeriod: device?.inMeteringValidityPeriod === false,
  parameters: parameters.length
    ? parameters.map((parameter, parameterIndex) =>
        toProductionRecordParameterRow(parameter, `${key}-${parameter.parameterCode || parameterIndex}`)
      )
    : [toProductionRecordParameterRow(undefined, `${key}-empty-parameter`)]
})

const buildProductionRecordMaterialDeviceGroups = (
  rows: ActiveOrderProductionRecordRow[]
): ActiveOrderProductionRecordMaterialDeviceGroup[] => {
  const result: ActiveOrderProductionRecordMaterialDeviceGroup[] = []
  for (const recordRow of rows) {
    const deviceMap = new Map<string, TeamLeaderActiveOrderSubmissionDeviceDetailRespVO>()
    ;(recordRow.devices ?? []).forEach((device, index) => {
      deviceMap.set(resolveProductionRecordDeviceKey(device, index), device)
    })
    ;(recordRow.deviceParameters ?? []).forEach((parameter, index) => {
      const device = {
        deviceId: parameter.deviceId,
        deviceCode: parameter.deviceCode,
        deviceName: parameter.deviceName
      }
      const key = resolveProductionRecordDeviceKey(device, index)
      if (!deviceMap.has(key) && hasProductionRecordDeviceIdentity(parameter)) {
        deviceMap.set(key, device)
      }
    })
    const devices = Array.from(deviceMap.entries())
    if (!devices.length) {
      continue
    }
    result.push({
      key: `${recordRow.key}-device-group`,
      materialText: formatProductionRecordMaterialText(recordRow),
      devices: devices.map(([deviceKey, device]) => {
        const parameters = (recordRow.deviceParameters ?? []).filter((parameter) =>
          isSameProductionRecordDevice(parameter, device, devices.length)
        )
        return toProductionRecordDeviceGroup(device, parameters, `${recordRow.key}-${deviceKey}`)
      })
    })
  }
  return result
}

const formatProductionInputMaterialPickListEvidence = (
  material: TeamLeaderActiveOrderInputMaterialDetailRespVO
) => {
  const sourcePickListNos = (material.sourcePickListNos ?? [])
    .map((sourceNo) => String(sourceNo).trim())
    .filter(Boolean)
  return sourcePickListNos.length ? sourcePickListNos.join('、') : '-'
}

const formatProductionRecordClearanceConfirmation = (
  confirmations: TeamLeaderActiveOrderClearanceConfirmationRespVO[] | undefined,
  key: 'workplace' | 'material' | 'cleaning'
) => {
  const confirmation = (confirmations ?? []).find((item) => item.key === key)
  if (!confirmation) return '未记录'
  return confirmation.confirmed ? '是' : '否'
}

const normalizeActiveOrderPqcText = (value?: string | number | null) => {
  if (value === undefined || value === null) return ''
  return String(value).trim()
}

const formatActiveOrderPqcItemCountSummary = (values: string[]) => {
  const counts = new Map<string, number>()
  for (const value of values.map((item) => item.trim()).filter(Boolean)) {
    counts.set(value, (counts.get(value) ?? 0) + 1)
  }
  const parts = Array.from(counts.entries())
    .sort(([left], [right]) => left.localeCompare(right, 'zh-Hans-CN'))
    .map(([value, count]) => (count > 1 ? `${value} × ${count}` : value))
  return parts.length ? parts.join('；') : '-'
}

const formatActiveOrderPqcItemUniqueSummary = (values: string[]) => {
  const uniqueValues = Array.from(new Set(values.map((item) => item.trim()).filter(Boolean))).sort(
    (left, right) => left.localeCompare(right, 'zh-Hans-CN')
  )
  return uniqueValues.length ? uniqueValues.join('；') : '-'
}

const formatActiveOrderPqcItemSampleSummary = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) => {
  const sampleNos = Array.from(
    new Set(
      items
        .map((item) => Number(item.sampleNo))
        .filter((sampleNo) => Number.isFinite(sampleNo) && sampleNo > 0)
    )
  ).sort((left, right) => left - right)
  if (!sampleNos.length) return `${items.length} 件`
  const visibleSamples = sampleNos.slice(0, 8).join('、')
  const suffix = sampleNos.length > 8 ? `…共 ${sampleNos.length} 件` : `共 ${sampleNos.length} 件`
  return `样本 ${visibleSamples}，${suffix}`
}

const formatActiveOrderPqcItemResultSummary = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) =>
  formatActiveOrderPqcItemCountSummary(
    items.map((item) => normalizeActiveOrderPqcText(item.measuredValue || item.itemResult))
  )

const formatActiveOrderPqcItemJudgementSummary = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) => formatActiveOrderPqcItemCountSummary(items.map((item) => normalizeActiveOrderPqcText(item.judgement)))

const formatActiveOrderPqcEquipmentSummary = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) => {
  const equipments = Array.from(
    new Set(
      items
        .map((item) =>
          normalizeActiveOrderPqcText(item.selectedEquipmentNumber || item.selectedEquipmentName)
        )
        .filter(Boolean)
    )
  )
  return equipments.length ? equipments.join('、') : '-'
}

const buildActiveOrderPqcItemRows = (
  pqcSubmission: TeamLeaderActiveOrderPqcSubmissionDetailRespVO
): ActiveOrderPqcItemAggregateRow[] => {
  const rowsByItem = new Map<
    string,
    {
      itemNameText: string
      itemIdentityKey: string
      items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
    }
  >()
  for (const item of pqcSubmission.items ?? []) {
    const itemCode = normalizeActiveOrderPqcText(item.itemCode)
    const itemName = normalizeActiveOrderPqcText(item.itemName)
    const key = resolveActiveOrderPqcItemIdentityKey(item)
    const itemNameText = itemName || itemCode || '-'
    const existed = rowsByItem.get(key)
    if (existed) {
      existed.items.push(item)
      continue
    }
    rowsByItem.set(key, { itemNameText, itemIdentityKey: key, items: [item] })
  }
  return Array.from(rowsByItem.entries()).map(([key, row]) => ({
    key,
    itemIdentityKey: row.itemIdentityKey,
    itemNameText: row.itemNameText,
    sampleSummaryText: formatActiveOrderPqcItemSampleSummary(row.items),
    resultSummaryText: formatActiveOrderPqcItemResultSummary(row.items),
    judgementSummaryText: formatActiveOrderPqcItemJudgementSummary(row.items),
    equipmentSummaryText: formatActiveOrderPqcEquipmentSummary(row.items)
  }))
}

const resolveActiveOrderPqcItemIdentityKey = (
  item: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO
) => {
  const itemCode = normalizeActiveOrderPqcText(item.itemCode)
  if (itemCode) return `item-code:${itemCode}`
  const aggregateDetailId = Number(item.aggregateDetailId)
  if (Number.isFinite(aggregateDetailId) && aggregateDetailId > 0) {
    return `aggregate-detail:${aggregateDetailId}`
  }
  throw new Error('PQC提交缺少正式检验项目身份，无法展示复合检验项目')
}

const normalizePqcInspectionJudgement = (
  item: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO
) => {
  const rawText = normalizeActiveOrderPqcText(item.judgement || item.itemResult || item.measuredValue)
  if (!rawText) return '未记录'
  const negativeKeywords = ['不合格', '不通过', '否', 'NG', 'FAIL', 'FALSE']
  if (negativeKeywords.some((keyword) => rawText.toUpperCase().includes(keyword))) return '不合格'
  const positiveKeywords = ['合格', '通过', '是', 'OK', 'PASS', 'TRUE']
  if (positiveKeywords.some((keyword) => rawText.toUpperCase().includes(keyword))) return '合格'
  return rawText
}

const formatPqcInspectionMeasuredValues = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) => {
  const values = items
    .map((item) => normalizeActiveOrderPqcText(item.measuredValue || item.itemResult))
    .filter(Boolean)
  return values.length ? values.join('，') : '-'
}

const formatPqcInspectionRecordEquipmentText = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) => formatActiveOrderPqcEquipmentSummary(items)

const summarizePqcInspectionJudgements = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) => {
  const judgements = Array.from(new Set(items.map(normalizePqcInspectionJudgement).filter(Boolean)))
  if (!judgements.length) return '未记录'
  if (judgements.includes('不合格')) return '不合格'
  if (judgements.length === 1 && judgements[0] === '合格') return '合格'
  return judgements.join('；')
}

type PqcInspectionRecordSpanNumberKey =
  | 'sequenceRowSpan'
  | 'inspectionDateRowSpan'
  | 'qaProcessRowSpan'
  | 'itemRowSpan'
  | 'methodRowSpan'
  | 'standardRowSpan'
  | 'equipmentRowSpan'

type PqcInspectionRecordSpanBooleanKey =
  | 'isSequenceFirstRow'
  | 'isInspectionDateFirstRow'
  | 'isQaProcessFirstRow'
  | 'isItemFirstRow'
  | 'isMethodFirstRow'
  | 'isStandardFirstRow'
  | 'isEquipmentFirstRow'

const formatPqcInspectionRecordResultText = (
  standardText: string,
  measuredValuesText: string,
  sampleSummaryText: string
) => {
  const standardPart = standardText && standardText !== '-' ? `标准：${standardText}` : ''
  const measuredPart =
    measuredValuesText && measuredValuesText !== '-' ? `实测：${measuredValuesText}` : ''
  const samplePart = sampleSummaryText && sampleSummaryText !== '-' ? `样本：${sampleSummaryText}` : ''
  const parts = [standardPart, samplePart, measuredPart].filter(Boolean)
  return parts.length ? parts.join('；') : '□符合要求　□不符合要求______'
}

const formatPqcInspectionRecordPersonText = (name: string, dateText: string) =>
  dateText && dateText !== '-' ? `${name} / ${dateText}` : name

const applyPqcInspectionRecordRowSpan = (
  rows: ActiveOrderPqcInspectionRecordRow[],
  resolveKey: (row: ActiveOrderPqcInspectionRecordRow) => string,
  spanKey: PqcInspectionRecordSpanNumberKey,
  firstKey: PqcInspectionRecordSpanBooleanKey
) => {
  let index = 0
  while (index < rows.length) {
    const key = resolveKey(rows[index])
    let end = index + 1
    while (end < rows.length && resolveKey(rows[end]) === key) {
      end += 1
    }
    rows[index][firstKey] = true
    rows[index][spanKey] = end - index
    for (let cursor = index + 1; cursor < end; cursor += 1) {
      rows[cursor][firstKey] = false
      rows[cursor][spanKey] = 0
    }
    index = end
  }
}

const buildPqcInspectionRecordRows = (): ActiveOrderPqcInspectionRecordRow[] => {
  const rows: ActiveOrderPqcInspectionRecordRow[] = []
  for (const [pqcProcessIndex, pqcProcess] of pqcProcessGroups.value.entries()) {
    const groupedItemsByIdentity = new Map<string, ActiveOrderPqcItemAggregateRow>()
    for (const submission of pqcProcess.submissions) {
      for (const groupedItem of buildActiveOrderPqcItemRows(submission)) {
        if (!groupedItemsByIdentity.has(groupedItem.itemIdentityKey)) {
          groupedItemsByIdentity.set(groupedItem.itemIdentityKey, groupedItem)
        }
      }
    }
    const groupedItems = Array.from(groupedItemsByIdentity.values())
    for (const groupedItem of groupedItems) {
      for (const submission of pqcProcess.submissions) {
        const sourceItems = (submission.items ?? []).filter((item) => {
          const key = resolveActiveOrderPqcItemIdentityKey(item)
          return key === groupedItem.key
        })
        if (!sourceItems.length) continue
        const inspectionMethodText = formatActiveOrderPqcItemUniqueSummary(
          sourceItems.map((item) => normalizeActiveOrderPqcText(item.inspectionMethod))
        )
        const standardText = formatActiveOrderPqcItemUniqueSummary(
          sourceItems.map((item) => normalizeActiveOrderPqcText(item.standardText))
        )
        const sampleSummaryText = formatActiveOrderPqcItemSampleSummary(sourceItems)
        const measuredValuesText = formatPqcInspectionMeasuredValues(sourceItems)
        const judgementText = summarizePqcInspectionJudgements(sourceItems)
        const equipmentText = formatPqcInspectionRecordEquipmentText(sourceItems)
        const inspectionDateText = normalizeActiveOrderPqcText(submission.businessDate) || '-'
        const submitterName = submission.submitterName || '-'
        const reviewerName = submission.reviewerName || '未审核'
        rows.push({
          key: `${pqcProcess.key}-${submission.pqcTaskId}-${groupedItem.key}`,
          sequenceText: String(pqcProcessIndex + 1),
          sequenceRowSpan: 1,
          isSequenceFirstRow: true,
          inspectionDateText,
          inspectionDateRowSpan: 1,
          isInspectionDateFirstRow: true,
          qaProcessRowSpan: 1,
          isQaProcessFirstRow: true,
          itemRowSpan: 1,
          isItemFirstRow: true,
          methodRowSpan: 1,
          isMethodFirstRow: true,
          standardRowSpan: 1,
          isStandardFirstRow: true,
          equipmentRowSpan: 1,
          isEquipmentFirstRow: true,
          qaProcessName: pqcProcess.qaProcessName,
          inspectionTypeText: resolvePqcInspectionTypeText(submission),
          itemIdentityKey: groupedItem.itemIdentityKey,
          itemNameText: groupedItem.itemNameText,
          inspectionMethodText,
          standardText,
          inspectionQuantityText:
            submission.actualInspectionQuantity === undefined ||
            submission.actualInspectionQuantity === null
              ? '-'
              : String(submission.actualInspectionQuantity),
          resultText: formatPqcInspectionRecordResultText(
            standardText,
            measuredValuesText,
            sampleSummaryText
          ),
          sampleSummaryText,
          measuredValuesText,
          judgementText,
          equipmentText,
          submitterName,
          reviewerName,
          inspectorText: formatPqcInspectionRecordPersonText(submitterName, inspectionDateText),
          reviewerText: formatPqcInspectionRecordPersonText(reviewerName, inspectionDateText)
        })
      }
    }
  }
  applyPqcInspectionRecordRowSpan(
    rows,
    (row) => row.sequenceText,
    'sequenceRowSpan',
    'isSequenceFirstRow'
  )
  applyPqcInspectionRecordRowSpan(
    rows,
    (row) => `${row.sequenceText}::${row.inspectionDateText}`,
    'inspectionDateRowSpan',
    'isInspectionDateFirstRow'
  )
  applyPqcInspectionRecordRowSpan(
    rows,
    (row) => `${row.sequenceText}::${row.inspectionDateText}::${row.qaProcessName}`,
    'qaProcessRowSpan',
    'isQaProcessFirstRow'
  )
  applyPqcInspectionRecordRowSpan(
    rows,
    (row) =>
      `${row.sequenceText}::${row.inspectionDateText}::${row.qaProcessName}::${row.itemIdentityKey}`,
    'itemRowSpan',
    'isItemFirstRow'
  )
  applyPqcInspectionRecordRowSpan(
    rows,
    (row) =>
      `${row.sequenceText}::${row.inspectionDateText}::${row.qaProcessName}::${row.itemIdentityKey}::${row.inspectionMethodText}`,
    'methodRowSpan',
    'isMethodFirstRow'
  )
  applyPqcInspectionRecordRowSpan(
    rows,
    (row) =>
      `${row.sequenceText}::${row.inspectionDateText}::${row.qaProcessName}::${row.itemIdentityKey}::${row.inspectionMethodText}::${row.standardText}`,
    'standardRowSpan',
    'isStandardFirstRow'
  )
  applyPqcInspectionRecordRowSpan(
    rows,
    (row) =>
      `${row.sequenceText}::${row.inspectionDateText}::${row.qaProcessName}::${row.itemIdentityKey}::${row.inspectionMethodText}::${row.standardText}::${row.equipmentText}`,
    'equipmentRowSpan',
    'isEquipmentFirstRow'
  )
  return rows
}

const pqcInspectionRecordRows = computed(() => buildPqcInspectionRecordRows())


const pqcInspectionRecordSummary = computed(() => {
  const submissions = pqcProcessGroups.value.flatMap((pqcProcess) => pqcProcess.submissions)
  const inspectionQuantities = submissions
    .map((submission) => Number(submission.actualInspectionQuantity))
    .filter((value) => Number.isFinite(value))
  const equipmentText = formatPqcInspectionRecordEquipmentText(
    submissions.flatMap((submission) => submission.items ?? [])
  )
  return {
    submissionCount: submissions.length,
    itemCount: pqcInspectionRecordRows.value.length,
    inspectionQuantityText: inspectionQuantities.length
      ? `${inspectionQuantities.reduce((total, value) => total + value, 0)} 件`
      : '-',
    judgementText: summarizePqcInspectionJudgements(
      submissions.flatMap((submission) => submission.items ?? [])
    ),
    equipmentText
  }
})

const activeOrderDetailProcessTabName = (
  process: TeamLeaderActiveOrderProcessDetailRespVO,
  index: number
) => `process-${process.routeProcessId}-${process.processId}-${index}`

interface ActiveOrderDetailPqcProcessGroup {
  key: string
  qaProcessId: number
  qaProcessCode?: string
  qaProcessName: string
  submissions: TeamLeaderActiveOrderPqcSubmissionDetailRespVO[]
}

const resolveActiveOrderPqcProcessTabName = (
  pqcProcess: ActiveOrderDetailPqcProcessGroup,
  index: number
) => `pqc-process-${pqcProcess.qaProcessId}-${index}`

const pqcProcessGroups = computed<ActiveOrderDetailPqcProcessGroup[]>(() => {
  const detailResult = props.detail
  if (!detailResult?.processes?.length) return []
  const groupsByQaProcessId = new Map<number, ActiveOrderDetailPqcProcessGroup>()
  for (const process of detailResult.processes) {
    for (const submission of process.pqcSubmissions ?? []) {
      const qaProcessId = Number(submission.qaProcessId)
      if (!Number.isFinite(qaProcessId) || qaProcessId <= 0 || !submission.qaProcessName?.trim()) {
        throw new Error('PQC提交缺少正式检验工序身份，无法按PQC工序展示')
      }
      const existed = groupsByQaProcessId.get(qaProcessId)
      if (existed) {
        existed.submissions.push(submission)
        continue
      }
      groupsByQaProcessId.set(qaProcessId, {
        key: `pqc-process-${qaProcessId}`,
        qaProcessId,
        qaProcessCode: submission.qaProcessCode,
        qaProcessName: submission.qaProcessName.trim(),
        submissions: [submission]
      })
    }
  }
  return Array.from(groupsByQaProcessId.values())
})

type ActiveOrderDetailPickListMaterialRow = TeamLeaderActiveOrderInputMaterialDetailRespVO & {
  sourceProcessNames: string[]
}

const pickListMaterials = computed<ActiveOrderDetailPickListMaterialRow[]>(() => {
  const detailResult = props.detail
  if (!detailResult?.processes?.length) return []
  const rowsByKey = new Map<string, ActiveOrderDetailPickListMaterialRow>()
  for (const process of detailResult.processes) {
    for (const material of process.inputMaterials ?? []) {
      const sourcePickListNos = (material.sourcePickListNos ?? [])
        .map((sourceNo) => String(sourceNo).trim())
        .filter(Boolean)
        .sort()
      const batchCodes = (material.batchCodes ?? [])
        .map((batchCode) => String(batchCode).trim())
        .filter(Boolean)
        .sort()
      const rowKey = [
        sourcePickListNos.join('|'),
        material.materialCode,
        batchCodes.join('|'),
        material.actualQuantity ?? '',
        material.baseActualQuantity ?? ''
      ].join('::')
      const existed = rowsByKey.get(rowKey)
      if (existed) {
        if (!existed.sourceProcessNames.includes(process.processName)) {
          existed.sourceProcessNames.push(process.processName)
        }
        continue
      }
      rowsByKey.set(rowKey, {
        ...material,
        sourcePickListNos,
        batchCodes,
        sourceProcessNames: [process.processName]
      })
    }
  }
  return Array.from(rowsByKey.values())
})

type ActiveOrderDetailReplenishmentMaterialRow =
  TeamLeaderActiveOrderSupplementMaterialDetailRespVO & {
    sourceProcessNames: string[]
  }

const replenishmentMaterials = computed<ActiveOrderDetailReplenishmentMaterialRow[]>(() => {
  const detailResult = props.detail
  if (!detailResult?.processes?.length) return []
  const rowsByKey = new Map<string, ActiveOrderDetailReplenishmentMaterialRow>()
  for (const process of detailResult.processes) {
    for (const material of process.supplementMaterials ?? []) {
      const sourceReplenishmentListNos = (material.sourceReplenishmentListNos ?? [])
        .map((sourceNo) => String(sourceNo).trim())
        .filter(Boolean)
        .sort()
      const batchCodes = (material.batchCodes ?? [])
        .map((batchCode) => String(batchCode).trim())
        .filter(Boolean)
        .sort()
      const rowKey = [
        sourceReplenishmentListNos.join('|'),
        material.materialCode,
        batchCodes.join('|'),
        material.actualQuantity ?? '',
        material.baseActualQuantity ?? ''
      ].join('::')
      const existed = rowsByKey.get(rowKey)
      if (existed) {
        if (!existed.sourceProcessNames.includes(process.processName)) {
          existed.sourceProcessNames.push(process.processName)
        }
        continue
      }
      rowsByKey.set(rowKey, {
        ...material,
        sourceReplenishmentListNos,
        batchCodes,
        sourceProcessNames: [process.processName]
      })
    }
  }
  return Array.from(rowsByKey.values())
})

const resolveActiveOrderSubmissionRowClassName = ({
  row
}: {
  row: { quantityConflict?: boolean }
}) => (row.quantityConflict ? 'team-leader-workbench__quantity-conflict-row' : '')

const resolvePqcInspectionTypeText = (pqcSubmission: TeamLeaderActiveOrderPqcSubmissionDetailRespVO) => {
  if (pqcSubmission.inspectionRuleKey === 'PATROL_AM') return '上午巡检'
  if (pqcSubmission.inspectionRuleKey === 'PATROL_PM') return '下午巡检'
  const inspectionType = pqcSubmission.inspectionType
  if (inspectionType === 'FIRST') return '首检'
  if (inspectionType === 'FINAL') return '末检'
  if (inspectionType === 'PATROL') return '巡检'
  if (inspectionType === 'PROCESS') return '巡检'
  return inspectionType || '检验'
}

const shouldShowPqcRoundNo = (pqcSubmission: TeamLeaderActiveOrderPqcSubmissionDetailRespVO) =>
  !!pqcSubmission.roundNo &&
  pqcSubmission.inspectionRuleKey !== 'PATROL_AM' &&
  pqcSubmission.inspectionRuleKey !== 'PATROL_PM'

const resetTabs = async () => {
  await nextTick()
  const firstProcess = props.detail?.processes?.[0]
  activeTab.value = firstProcess ? 'productionSubmissions' : 'materials'
  productionActiveTab.value = firstProcess ? activeOrderDetailProcessTabName(firstProcess, 0) : ''
  const firstPqcProcess = pqcProcessGroups.value[0]
  pqcActiveTab.value = firstPqcProcess
    ? resolveActiveOrderPqcProcessTabName(firstPqcProcess, 0)
    : ''
}

watch(
  () => props.detail,
  () => {
    void resetTabs()
  },
  { immediate: true }
)
</script>
<style scoped>
.team-leader-workbench__active-order-detail {
  display: grid;
  gap: 16px;
  min-height: 180px;
  max-width: 100%;
  overflow-x: hidden;
}

.team-leader-workbench__active-order-detail-tabs,
.team-leader-workbench__active-order-detail-inner-tabs {
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
}

.team-leader-workbench__active-order-detail-tabs :deep(.el-tabs__header),
.team-leader-workbench__active-order-detail-inner-tabs :deep(.el-tabs__header) {
  max-width: 100%;
}

.team-leader-workbench__active-order-detail-tabs :deep(.el-tabs__nav-wrap),
.team-leader-workbench__active-order-detail-inner-tabs :deep(.el-tabs__nav-wrap) {
  max-width: 100%;
}

.team-leader-workbench__active-order-detail-tabs :deep(.el-tabs__nav-scroll),
.team-leader-workbench__active-order-detail-inner-tabs :deep(.el-tabs__nav-scroll) {
  max-width: 100%;
  overflow-x: auto;
}

.team-leader-workbench__active-order-detail-tabs :deep(.el-tabs__content),
.team-leader-workbench__active-order-detail-inner-tabs :deep(.el-tabs__content),
.team-leader-workbench__active-order-detail-tabs :deep(.el-tab-pane),
.team-leader-workbench__active-order-detail-inner-tabs :deep(.el-tab-pane) {
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
}

.team-leader-workbench__active-order-detail-summary {
  display: grid;
  grid-template-columns: minmax(180px, 0.9fr) minmax(280px, 1.5fr) 90px;
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-border-color-light);
}

.team-leader-workbench__active-order-detail-summary > div {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 12px 14px;
  background: var(--el-bg-color);
}

.team-leader-workbench__active-order-detail-summary span,
.team-leader-workbench__active-order-process-metrics span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__active-order-detail-summary strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__active-order-process-detail {
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  max-width: 100%;
}

.team-leader-workbench__active-order-process-detail.is-quantity-conflict {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}

.team-leader-workbench__quantity-conflict-text {
  color: var(--el-color-danger);
}

.team-leader-workbench__active-order-process-header {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 20px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-lighter);
}

.team-leader-workbench__active-order-process-title {
  display: flex;
  flex: 1 1 auto;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.team-leader-workbench__active-order-process-title strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__active-order-process-title span {
  flex: 0 0 auto;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__active-order-process-metrics {
  display: grid;
  flex: 0 0 360px;
  grid-template-columns: repeat(3, minmax(100px, 1fr));
  gap: 18px;
}

.team-leader-workbench__active-order-process-metrics > div {
  display: grid;
  gap: 3px;
}

.team-leader-workbench__active-order-process-metrics strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.team-leader-workbench__active-order-process-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
}

.team-leader-workbench__active-order-submission-table {
  width: 100%;
}

.team-leader-workbench__active-order-output-materials {
  display: grid;
  gap: 6px;
}

.team-leader-workbench__active-order-output-materials-title {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__active-order-output-material {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  align-items: center;
  color: var(--el-text-color-regular);
  line-height: 1.45;
}

.team-leader-workbench__active-order-output-material-name {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.team-leader-workbench__active-order-detail-table-shell {
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
}

.team-leader-workbench__active-order-detail-table-shell :deep(.el-table) {
  width: 100% !important;
}

.team-leader-workbench__active-order-detail-table-shell :deep(.el-table__inner-wrapper),
.team-leader-workbench__active-order-detail-table-shell :deep(.el-table__body-wrapper),
.team-leader-workbench__active-order-detail-table-shell :deep(.el-scrollbar__wrap) {
  max-width: 100%;
  overflow-x: hidden;
}

.team-leader-workbench__active-order-detail-table-shell :deep(.el-table__cell .cell) {
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.team-leader-workbench__work-order-highlight-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  max-width: 100%;
}

.team-leader-workbench__work-order-highlight-card {
  display: grid;
  gap: 7px;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.team-leader-workbench__work-order-highlight-card.is-primary {
  border-color: var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
}

.team-leader-workbench__work-order-highlight-card span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__work-order-highlight-card strong,
.team-leader-workbench__work-order-highlight-value {
  min-width: 0;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.team-leader-workbench__work-order-list-shell {
  margin-top: 12px;
}

.team-leader-workbench__work-order-list-emphasis,
.team-leader-workbench__work-order-list-link {
  font-weight: 700;
}

.team-leader-workbench__active-order-pqc-card {
  display: grid;
  gap: 8px;
  padding-bottom: 10px;
}

.team-leader-workbench__active-order-pqc-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__active-order-pqc-title strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.team-leader-workbench__active-order-pqc-parties {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 1px;
  margin: 0 14px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: var(--el-border-color-light);
}

.team-leader-workbench__active-order-pqc-parties > div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 8px 10px;
  background: var(--el-bg-color);
}

.team-leader-workbench__active-order-pqc-parties span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__active-order-pqc-parties strong {
  min-width: 0;
  color: var(--el-text-color-primary);
  font-size: 13px;
  word-break: break-word;
}

:deep(.team-leader-workbench__active-order-submission-row--quantity-conflict > td) {
  background: var(--el-color-danger-light-9) !important;
  color: var(--el-color-danger);
}

.team-leader-workbench__active-order-submission-table .is-pending {
  color: var(--el-color-warning);
}

.team-leader-workbench__production-record-form-dialog :deep(.el-dialog__body),
.team-leader-workbench__pqc-inspection-record-form-dialog :deep(.el-dialog__body) {
  max-height: calc(100vh - 180px);
  overflow: auto;
}

.team-leader-workbench__production-record-form {
  display: grid;
  gap: 12px;
}

.team-leader-workbench__production-record-form-title {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: center;
  gap: 8px;
  color: var(--el-text-color-primary);
}

.team-leader-workbench__production-record-form-title h3 {
  margin: 0;
  font-size: 20px;
}

.team-leader-workbench__production-record-form-title span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.team-leader-workbench__production-record-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: var(--el-border-color-light);
}

.team-leader-workbench__production-record-meta > div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 8px 10px;
  background: var(--el-bg-color);
}

.team-leader-workbench__production-record-meta span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__production-record-meta strong {
  min-width: 0;
  font-size: 13px;
  word-break: break-word;
}

.team-leader-workbench__production-record-table {
  width: 100%;
}

.team-leader-workbench__production-record-table :deep(.el-table__cell .cell) {
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.team-leader-workbench__production-record-device-groups {
  display: grid;
  gap: 12px;
}

.team-leader-workbench__production-record-device-group {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}

.team-leader-workbench__production-record-device-group-title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: baseline;
}

.team-leader-workbench__production-record-device-group-title span,
.team-leader-workbench__production-record-parameter-title {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__production-record-device-group-title strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.team-leader-workbench__production-record-parameter-group {
  display: grid;
  gap: 6px;
}

.team-leader-workbench__pqc-inspection-record-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  border: 1px solid var(--el-text-color-primary);
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.team-leader-workbench__pqc-inspection-record-table th,
.team-leader-workbench__pqc-inspection-record-table td {
  min-height: 44px;
  padding: 8px;
  border: 1px solid var(--el-text-color-primary);
  vertical-align: middle;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.team-leader-workbench__pqc-inspection-record-table th {
  text-align: center;
  font-weight: 600;
}

.team-leader-workbench__pqc-inspection-record-common-cell,
.team-leader-workbench__pqc-inspection-record-center-cell,
.team-leader-workbench__pqc-inspection-record-type-cell,
.team-leader-workbench__pqc-inspection-record-judgement-cell {
  text-align: center;
}

.team-leader-workbench__pqc-inspection-record-type-cell {
  font-weight: 600;
}

.team-leader-workbench__pqc-inspection-record-col-sequence {
  width: 54px;
}

.team-leader-workbench__pqc-inspection-record-col-date {
  width: 90px;
}

.team-leader-workbench__pqc-inspection-record-col-project {
  width: 86px;
}

.team-leader-workbench__pqc-inspection-record-col-item {
  width: 82px;
}

.team-leader-workbench__pqc-inspection-record-col-method {
  width: 120px;
}

.team-leader-workbench__pqc-inspection-record-col-standard {
  width: 180px;
}

.team-leader-workbench__pqc-inspection-record-col-type {
  width: 78px;
}

.team-leader-workbench__pqc-inspection-record-col-quantity {
  width: 80px;
}

.team-leader-workbench__pqc-inspection-record-col-result {
  width: 260px;
}

.team-leader-workbench__pqc-inspection-record-col-equipment {
  width: 110px;
}

.team-leader-workbench__pqc-inspection-record-col-judgement {
  width: 90px;
}

.team-leader-workbench__pqc-inspection-record-col-person {
  width: 120px;
}

.team-leader-workbench__production-record-section-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.team-leader-workbench__production-record-warning {
  color: var(--el-color-danger);
  font-weight: 600;
}

.team-leader-workbench__production-record-totals {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 14px;
  color: var(--el-text-color-regular);
}

@media (max-width: 760px) {
  .team-leader-workbench__active-order-detail-summary {
    grid-template-columns: 1fr;
  }

  .team-leader-workbench__active-order-process-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .team-leader-workbench__active-order-process-metrics {
    width: 100%;
    flex-basis: auto;
  }

  .team-leader-workbench__production-record-meta {
    grid-template-columns: 1fr;
  }
}
</style>
