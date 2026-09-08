<template>
  <div
    v-loading="loading"
    class="team-leader-workbench__active-order-detail"
    :class="{
      'is-embedded': embedded,
      'is-single-mode': displayMode !== 'full'
    }"
  >
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
      <div
        v-if="!embedded"
        class="team-leader-workbench__active-order-detail-summary"
      >
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
        <el-tab-pane
          v-if="showSummaryTab"
          label="总表"
          name="summary"
          data-active-order-summary-tab
        >
          <section class="team-leader-workbench__active-order-summary">
            <table
              class="team-leader-workbench__active-order-summary-table"
              data-active-order-summary-product-table
            >
              <tbody>
                <tr>
                  <th colspan="4" class="team-leader-workbench__active-order-summary-title">
                    产品信息
                  </th>
                </tr>
                <tr>
                  <th>产品名称</th>
                  <td>{{ activeOrderWorkOrderDisplay.productName }}</td>
                  <th>型号规格</th>
                  <td>{{ activeOrderWorkOrderDisplay.productSpecification }}</td>
                </tr>
                <tr>
                  <th>生产批号</th>
                  <td>{{ activeOrderWorkOrderDisplay.batchCode }}</td>
                  <th>生产数量</th>
                  <td>{{ activeOrderWorkOrderDisplay.quantity }}</td>
                </tr>
                <tr>
                  <th>生产指令</th>
                  <td>{{ blankSummaryField }}</td>
                  <th>生产订单号</th>
                  <td>{{ activeOrderWorkOrderDisplay.workOrderCode }}</td>
                </tr>
                <tr>
                  <th>图号</th>
                  <td>{{ activeOrderWorkOrderDisplay.drawingNumber }}</td>
                  <th>生产周期</th>
                  <td>{{ summaryProductionCycleText }}</td>
                </tr>
              </tbody>
            </table>

            <table
              class="team-leader-workbench__active-order-summary-table"
              data-active-order-summary-material-batches-table
            >
              <tbody>
                <tr>
                  <th colspan="6" class="team-leader-workbench__active-order-summary-title">
                    生产零配件批号信息
                  </th>
                </tr>
                <tr>
                  <th>物料编码</th>
                  <th>物料名称</th>
                  <th>物料批号</th>
                  <th>物料编码</th>
                  <th>物料名称</th>
                  <th>物料批号</th>
                </tr>
                <tr
                  v-for="pair in summaryPickListMaterialPairs"
                  :key="pair.key"
                >
                  <td>{{ pair.left?.materialCode || blankSummaryField }}</td>
                  <td>{{ pair.left?.materialName || blankSummaryField }}</td>
                  <td>{{ formatSummaryMaterialBatchCodes(pair.left) }}</td>
                  <td>{{ pair.right?.materialCode || blankSummaryField }}</td>
                  <td>{{ pair.right?.materialName || blankSummaryField }}</td>
                  <td>{{ formatSummaryMaterialBatchCodes(pair.right) }}</td>
                </tr>
                <tr v-if="!summaryPickListMaterialPairs.length">
                  <td colspan="6">{{ blankSummaryField }}</td>
                </tr>
              </tbody>
            </table>

            <table
              class="team-leader-workbench__active-order-summary-table"
              data-active-order-summary-process-personnel-table
            >
              <tbody>
                <tr>
                  <th>工序名称</th>
                  <th>操作人员</th>
                  <th>装配日期</th>
                  <th>工序名称</th>
                  <th>操作人员</th>
                  <th>装配日期</th>
                </tr>
                <tr
                  v-for="pair in summaryProcessPersonnelPairs"
                  :key="pair.key"
                >
                  <td>{{ pair.left?.processName || blankSummaryField }}</td>
                  <td>{{ pair.left?.operatorText || blankSummaryField }}</td>
                  <td>{{ pair.left?.assemblyDateText || blankSummaryField }}</td>
                  <td>{{ pair.right?.processName || blankSummaryField }}</td>
                  <td>{{ pair.right?.operatorText || blankSummaryField }}</td>
                  <td>{{ pair.right?.assemblyDateText || blankSummaryField }}</td>
                </tr>
                <tr v-if="!summaryProcessPersonnelPairs.length">
                  <td colspan="6">{{ blankSummaryField }}</td>
                </tr>
              </tbody>
            </table>
          </section>
        </el-tab-pane>
        <el-tab-pane
          v-if="showSummaryTab"
          label="生产过程损耗报告单"
          name="pqcLossReport"
          data-active-order-pqc-loss-report-tab
        >
          <section class="team-leader-workbench__pqc-loss-report">
            <table
              class="team-leader-workbench__active-order-summary-table team-leader-workbench__pqc-loss-report-table"
              data-active-order-pqc-loss-report-table
            >
              <tbody>
                <tr>
                  <th colspan="8" class="team-leader-workbench__active-order-summary-title">
                    生产过程损耗报告单
                  </th>
                </tr>
                <tr>
                  <th>产品名称</th>
                  <td>{{ activeOrderWorkOrderDisplay.productName }}</td>
                  <th>型号规格</th>
                  <td>{{ activeOrderWorkOrderDisplay.productSpecification }}</td>
                  <th>批号</th>
                  <td>{{ activeOrderWorkOrderDisplay.batchCode }}</td>
                  <th>生产数量</th>
                  <td>{{ activeOrderWorkOrderDisplay.quantity }}</td>
                </tr>
                <tr>
                  <th colspan="8" class="team-leader-workbench__pqc-loss-description">
                    损耗描述：
                  </th>
                </tr>
                <tr>
                  <th>不合格日期</th>
                  <th>工序名称</th>
                  <th>不合格数量</th>
                  <th>不合格原因</th>
                  <th>处置方式</th>
                  <th>生产人员/日期</th>
                  <th>检验人员确认日期</th>
                  <th>批准人/日期</th>
                </tr>
                <tr
                  v-for="row in pqcLossReportRows"
                  :key="row.key"
                >
                  <td>{{ row.unqualifiedDateText }}</td>
                  <td>{{ row.processNameText }}</td>
                  <td>{{ row.unqualifiedQuantityText }}</td>
                  <td>{{ row.unqualifiedReasonText }}</td>
                  <td>{{ row.disposalMethodText }}</td>
                  <td>{{ row.productionOperatorDateText }}</td>
                  <td>{{ row.inspectorConfirmationDateText }}</td>
                  <td>{{ row.approverDateText }}</td>
                </tr>
                <tr v-if="!pqcLossReportRows.length">
                  <td colspan="8">暂无生产过程损耗记录</td>
                </tr>
                <tr>
                  <th colspan="2">批准人/日期：</th>
                  <td colspan="6">{{ pqcLossReportApprovalText }}</td>
                </tr>
              </tbody>
            </table>
          </section>
        </el-tab-pane>
        <el-tab-pane
          v-if="showProductionSubmissionTab"
          label="生产提交"
          name="productionSubmissions"
        >
          <el-tabs
            v-model="productionActiveTab"
            data-team-leader-active-order-detail-production-process-tabs
            class="team-leader-workbench__active-order-detail-inner-tabs"
          >
            <el-tab-pane
              v-for="(process, processIndex) in visibleProductionProcesses"
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
              <section class="team-leader-workbench__production-record-form">
                <div class="team-leader-workbench__production-record-form-title">
                  <h3 class="team-leader-workbench__production-record-process-title">
                    {{ process.processName }}
                  </h3>
                  <span
                    class="team-leader-workbench__production-record-key-flag"
                    :class="{
                      'is-key': process.keyFlag,
                      'is-non-key': !process.keyFlag
                    }"
                    data-active-order-production-record-key-flag
                  >
                    {{ process.keyFlag ? '关键工序' : '非关键工序' }}
                  </span>
                  <el-button
                    v-if="!embedded"
                    size="small"
                    type="primary"
                    plain
                    data-active-order-open-batch-execution-production-form
                    @click="emit('open-batch-execution-production-form', process)"
                  >
                    打开批次执行生产表单
                  </el-button>
                </div>
                <div class="team-leader-workbench__production-record-meta">
                  <div>
                    <span>工序名称</span>
                    <strong>{{ process.processName }}</strong>
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
                  v-if="process.inputMaterials?.length"
                  class="team-leader-workbench__production-record-input-materials"
                  data-active-order-production-record-input-materials
                >
                  <div class="team-leader-workbench__production-record-section-title">
                    输入物料批次号
                  </div>
                  <el-table
                    :data="process.inputMaterials"
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
                </div>
                <table
                  v-if="buildProductionRecordRows(process).length"
                  class="team-leader-workbench__production-record-table team-leader-workbench__production-record-two-line-table"
                  data-active-order-production-record-two-line-table
                >
                  <thead>
                    <tr>
                      <th>提交时间</th>
                      <th>输出物料编码</th>
                      <th>输出物料名称</th>
                      <th>规格型号</th>
                      <th>生产数量</th>
                      <th>损耗数量</th>
                      <th>总数量</th>
                      <th>清场确认</th>
                      <th>物料确认</th>
                      <th>清洁确认</th>
                      <th>操作人</th>
                      <th>复核人</th>
                    </tr>
                  </thead>
                  <tbody>
                    <template
                      v-for="row in buildProductionRecordRows(process)"
                      :key="row.key"
                    >
                      <tr data-active-order-production-record-main-row>
                        <td>{{ formatDateTime(row.submittedAt) }}</td>
                        <td>{{ row.materialCode || '未记录' }}</td>
                        <td>{{ row.materialName || '未记录' }}</td>
                        <td>{{ row.materialSpecification || '未记录' }}</td>
                        <td>{{ formatTraceQuantity(row.outputQuantity) }}</td>
                        <td>{{ formatTraceQuantity(row.lossQuantity) }}</td>
                        <td>{{ formatTraceQuantity(row.totalQuantity) }}</td>
                        <td>{{ formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'workplace') }}</td>
                        <td>{{ formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'material') }}</td>
                        <td>{{ formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'cleaning') }}</td>
                        <td>
                          <button
                            type="button"
                            class="team-leader-workbench__signature-link"
                            data-active-order-production-record-submitter-signature
                            :disabled="!row.submitterSignature?.signatureId"
                            @click="openActiveOrderSignatureRecord(row.submitterSignature)"
                          >
                            {{ formatActiveOrderSignatureCellText(row.submitterSignature) }}
                          </button>
                        </td>
                        <td>
                          <button
                            type="button"
                            class="team-leader-workbench__signature-link"
                            data-active-order-production-record-reviewer-signature
                            :disabled="!row.reviewerSignature?.signatureId"
                            @click="openActiveOrderSignatureRecord(row.reviewerSignature)"
                          >
                            {{ formatActiveOrderSignatureCellText(row.reviewerSignature) }}
                          </button>
                        </td>
                      </tr>
                      <tr data-active-order-production-record-device-row>
                        <td colspan="12">
                          <div
                            v-if="buildProductionRecordRowDeviceGroups(row).length"
                            class="team-leader-workbench__production-record-device-row-content"
                          >
                            <section
                              v-for="deviceGroup in buildProductionRecordRowDeviceGroups(row)"
                              :key="`${row.key}-${deviceGroup.key}`"
                              class="team-leader-workbench__production-record-device-card"
                            >
                              <div class="team-leader-workbench__production-record-device-card-title">
                                <span>设备名称：{{ deviceGroup.deviceNameText }}</span>
                                <span>设备编号：{{ deviceGroup.deviceCodeText }}</span>
                                <span
                                  :class="{ 'team-leader-workbench__production-record-warning': deviceGroup.meteringOutOfPeriod }"
                                >
                                  计量状态：{{ deviceGroup.meteringValidityText }}
                                </span>
                              </div>
                              <table
                                class="team-leader-workbench__production-record-parameter-mini-table"
                                data-active-order-production-record-parameter-horizontal-table
                              >
                                <thead>
                                  <tr>
                                    <th
                                      v-for="parameter in deviceGroup.parameters"
                                      :key="`${parameter.key}-name`"
                                      :colspan="2"
                                    >
                                      {{ parameter.parameterNameText }}
                                    </th>
                                  </tr>
                                  <tr>
                                    <template
                                      v-for="parameter in deviceGroup.parameters"
                                      :key="`${parameter.key}-headers`"
                                    >
                                      <th>参考值</th>
                                      <th>实际值</th>
                                    </template>
                                  </tr>
                                </thead>
                                <tbody>
                                  <tr>
                                    <template
                                      v-for="parameter in deviceGroup.parameters"
                                      :key="parameter.key"
                                    >
                                    <td data-active-order-production-record-parameter-reference>
                                      {{ parameter.parameterRangeText }}
                                    </td>
                                    <td
                                      :class="{ 'team-leader-workbench__production-record-warning': parameter.outOfRange }"
                                      data-active-order-production-record-parameter-actual
                                    >
                                      {{ parameter.parameterValueText }}
                                    </td>
                                    </template>
                                  </tr>
                                </tbody>
                              </table>
                            </section>
                          </div>
                          <span v-else>暂无设备信息</span>
                        </td>
                      </tr>
                    </template>
                  </tbody>
                </table>
                <el-empty v-else :image-size="56" description="暂无可生成的生产提交记录" />
                <div class="team-leader-workbench__production-record-totals">
                  <span>
                    生产数量合计：{{ formatTraceQuantity(buildProductionRecordTotals(buildProductionRecordRows(process)).outputQuantity) }}
                  </span>
                  <span>
                    损耗数量合计：{{ formatTraceQuantity(buildProductionRecordTotals(buildProductionRecordRows(process)).lossQuantity) }}
                  </span>
                  <strong>
                    总数量合计：{{ formatTraceQuantity(buildProductionRecordTotals(buildProductionRecordRows(process)).totalQuantity) }}
                  </strong>
                </div>
              </section>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>
        <el-tab-pane
          v-if="showPqcSubmissionTab"
          label="PQC提交"
          name="pqcSubmissions"
        >
          <section
            v-if="pqcProcessGroups.length"
            class="team-leader-workbench__pqc-inspection-record-form"
          >
            <div class="team-leader-workbench__production-record-form-title">
              <h3>过程检验记录</h3>
              <span>{{ detail.routeName }}</span>
              <el-button
                v-if="!embedded"
                size="small"
                type="primary"
                plain
                data-active-order-open-batch-execution-pqc-form
                @click="emit('open-batch-execution-pqc-form')"
              >
                打开批次执行过程检验记录
              </el-button>
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
                <span>批次数量</span>
                <strong>{{ pqcInspectionRecordSummary.batchQuantityText }}</strong>
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
                  <td data-pqc-inspection-record-result-cell>{{ row.resultText }}</td>
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
                  <td>
                    <div class="team-leader-workbench__signature-stack">
                      <button
                        v-for="signature in row.submitterSignatures"
                        :key="`pqc-submitter-${row.key}-${signature.signatureId}`"
                        type="button"
                        class="team-leader-workbench__signature-link"
                        data-active-order-pqc-inspection-record-inspector-signature
                        :disabled="!signature.signatureId"
                        @click="openActiveOrderSignatureRecord(signature)"
                      >
                        {{ formatActiveOrderSignatureCellText(signature) }}
                      </button>
                      <span v-if="!row.submitterSignatures.length">未签名</span>
                    </div>
                  </td>
                  <td>
                    <div class="team-leader-workbench__signature-stack">
                      <button
                        v-for="signature in row.reviewerSignatures"
                        :key="`pqc-reviewer-${row.key}-${signature.signatureId}`"
                        type="button"
                        class="team-leader-workbench__signature-link"
                        data-active-order-pqc-inspection-record-reviewer-signature
                        :disabled="!signature.signatureId"
                        @click="openActiveOrderSignatureRecord(signature)"
                      >
                        {{ formatActiveOrderSignatureCellText(signature) }}
                      </button>
                      <span v-if="!row.reviewerSignatures.length">未签名</span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <el-empty v-else :image-size="56" description="暂无PQC检验记录" />
          </section>
          <el-empty v-else :image-size="56" description="暂无一线PQC提交" />
        </el-tab-pane>
        <el-tab-pane
          v-if="displayMode === 'full'"
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
          v-if="displayMode === 'full'"
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
          label="生产用料清单"
          name="productionMaterialLists"
          data-team-leader-active-order-detail-production-material-list-tab
        >
          <el-alert
            v-if="productionMaterialListError"
            :title="productionMaterialListError"
            type="error"
            :closable="false"
            show-icon
            class="team-leader-workbench__production-material-list-error"
          />
          <div
            v-else-if="productionMaterialListDocuments.length"
            v-loading="productionMaterialListLoading"
            class="team-leader-workbench__production-material-list-documents"
            data-active-order-production-material-list-documents
          >
            <section
              v-for="document in productionMaterialListDocuments"
              :key="document.key"
              class="team-leader-workbench__production-material-list-document"
              data-active-order-production-material-list-document
            >
              <h3>生产用料清单</h3>
              <div class="team-leader-workbench__production-material-list-head">
                <div>
                  <span>生产订单号：</span>
                  <strong>{{ document.productionOrderNo }}</strong>
                </div>
                <div>
                  <span>生产车间：</span>
                  <strong>{{ document.workshopName }}</strong>
                </div>
                <div>
                  <span>单据编号：</span>
                  <strong>{{ document.sourceBillNo }}</strong>
                </div>
                <div>
                  <span>产品代码：</span>
                  <strong>{{ document.productCode }}</strong>
                </div>
                <div>
                  <span>产品名称：</span>
                  <strong>{{ document.productName }}</strong>
                </div>
                <div>
                  <span>规格型号：</span>
                  <strong>{{ document.productSpecification }}</strong>
                </div>
                <div>
                  <span>单位：</span>
                  <strong>{{ document.unitName }}</strong>
                </div>
                <div>
                  <span>生产数量：</span>
                  <strong>{{ document.productionQuantity }}</strong>
                </div>
                <div>
                  <span>生产组织：</span>
                  <strong>{{ document.productionOrganization }}</strong>
                </div>
              </div>
              <table class="team-leader-workbench__production-material-list-table">
                <thead>
                  <tr>
                    <th>序号</th>
                    <th>物料编码</th>
                    <th>物料名称</th>
                    <th>规格型号</th>
                    <th>单位</th>
                    <th>图号</th>
                    <th>应发数量</th>
                    <th>实际用量</th>
                    <th>需求日期</th>
                    <th>仓库</th>
                    <th>发料方式</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, rowIndex) in document.rows" :key="row.id || row.sourceEntryId || rowIndex">
                    <td>{{ rowIndex + 1 }}</td>
                    <td>{{ row.childMaterialCode || '' }}</td>
                    <td>{{ row.childMaterialName || '' }}</td>
                    <td>{{ row.childMaterialSpecification || '' }}</td>
                    <td>{{ row.childUnitName || '' }}</td>
                    <td>{{ resolveProductionMaterialListDrawingNo(row) }}</td>
                    <td>{{ formatTraceQuantity(row.requiredQuantity) }}</td>
                    <td>{{ resolveProductionMaterialListActualUsage(row) }}</td>
                    <td>{{ formatDate(row.demandTime) }}</td>
                    <td>{{ resolveProductionMaterialListWarehouse(row) }}</td>
                    <td>{{ row.issueMethod || '' }}</td>
                  </tr>
                </tbody>
              </table>
              <div class="team-leader-workbench__production-material-list-footer">
                <div>
                  <span>创建人：</span>
                  <strong>{{ document.creatorName }}</strong>
                </div>
                <div>
                  <span>创建日期：</span>
                  <strong>{{ document.createDate }}</strong>
                </div>
                <div>
                  <span>审核人：</span>
                  <strong>{{ document.auditorName }}</strong>
                </div>
                <div>
                  <span>审核日期：</span>
                  <strong>{{ document.auditDate }}</strong>
                </div>
              </div>
            </section>
          </div>
          <el-empty
            v-else
            v-loading="productionMaterialListLoading"
            :image-size="56"
            description="暂无生产用料清单"
          />
        </el-tab-pane>
        <el-tab-pane
          v-if="displayMode === 'full'"
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
            <h3 class="team-leader-workbench__production-record-process-title">
              {{ selectedProductionRecordProcess.processName }}
            </h3>
            <span
              class="team-leader-workbench__production-record-key-flag"
              :class="{
                'is-key': selectedProductionRecordProcess.keyFlag,
                'is-non-key': !selectedProductionRecordProcess.keyFlag
              }"
              data-active-order-production-record-key-flag
            >
              {{ selectedProductionRecordProcess.keyFlag ? '关键工序' : '非关键工序' }}
            </span>
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
            v-if="selectedProductionInputMaterials.length"
            class="team-leader-workbench__production-record-input-materials"
            data-active-order-production-record-input-materials
          >
            <div class="team-leader-workbench__production-record-section-title">
              输入物料批次号
            </div>
            <el-table
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
          </div>
          <table
            v-if="selectedProductionRecordRows.length"
            class="team-leader-workbench__production-record-table team-leader-workbench__production-record-two-line-table"
            data-active-order-production-record-two-line-table
          >
            <thead>
              <tr>
                <th>提交时间</th>
                <th>输出物料编码</th>
                <th>输出物料名称</th>
                <th>规格型号</th>
                <th>生产数量</th>
                <th>损耗数量</th>
                <th>总数量</th>
                <th>清场确认</th>
                <th>物料确认</th>
                <th>清洁确认</th>
                <th>操作人</th>
                <th>复核人</th>
              </tr>
            </thead>
            <tbody>
              <template
                v-for="row in selectedProductionRecordRows"
                :key="row.key"
              >
                <tr data-active-order-production-record-main-row>
                  <td>{{ formatDateTime(row.submittedAt) }}</td>
                  <td>{{ row.materialCode || '未记录' }}</td>
                  <td>{{ row.materialName || '未记录' }}</td>
                  <td>{{ row.materialSpecification || '未记录' }}</td>
                  <td>{{ formatTraceQuantity(row.outputQuantity) }}</td>
                  <td>{{ formatTraceQuantity(row.lossQuantity) }}</td>
                  <td>{{ formatTraceQuantity(row.totalQuantity) }}</td>
                  <td>{{ formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'workplace') }}</td>
                  <td>{{ formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'material') }}</td>
                  <td>{{ formatProductionRecordClearanceConfirmation(row.clearanceConfirmations, 'cleaning') }}</td>
                  <td>
                    <button
                      type="button"
                      class="team-leader-workbench__signature-link"
                      data-active-order-production-record-submitter-signature
                      :disabled="!row.submitterSignature?.signatureId"
                      @click="openActiveOrderSignatureRecord(row.submitterSignature)"
                    >
                      {{ formatActiveOrderSignatureCellText(row.submitterSignature) }}
                    </button>
                  </td>
                  <td>
                    <button
                      type="button"
                      class="team-leader-workbench__signature-link"
                      data-active-order-production-record-reviewer-signature
                      :disabled="!row.reviewerSignature?.signatureId"
                      @click="openActiveOrderSignatureRecord(row.reviewerSignature)"
                    >
                      {{ formatActiveOrderSignatureCellText(row.reviewerSignature) }}
                    </button>
                  </td>
                </tr>
                <tr data-active-order-production-record-device-row>
                  <td colspan="12">
                    <div
                      v-if="buildProductionRecordRowDeviceGroups(row).length"
                      class="team-leader-workbench__production-record-device-row-content"
                    >
                      <section
                        v-for="deviceGroup in buildProductionRecordRowDeviceGroups(row)"
                        :key="`${row.key}-${deviceGroup.key}`"
                        class="team-leader-workbench__production-record-device-card"
                      >
                        <div class="team-leader-workbench__production-record-device-card-title">
                          <span>设备名称：{{ deviceGroup.deviceNameText }}</span>
                          <span>设备编号：{{ deviceGroup.deviceCodeText }}</span>
                          <span
                            :class="{ 'team-leader-workbench__production-record-warning': deviceGroup.meteringOutOfPeriod }"
                          >
                            计量状态：{{ deviceGroup.meteringValidityText }}
                          </span>
                        </div>
                        <table
                          class="team-leader-workbench__production-record-parameter-mini-table"
                          data-active-order-production-record-parameter-horizontal-table
                        >
                          <thead>
                            <tr>
                              <th
                                v-for="parameter in deviceGroup.parameters"
                                :key="`${parameter.key}-name`"
                                :colspan="2"
                              >
                                {{ parameter.parameterNameText }}
                              </th>
                            </tr>
                            <tr>
                              <template
                                v-for="parameter in deviceGroup.parameters"
                                :key="`${parameter.key}-headers`"
                              >
                                <th>参考值</th>
                                <th>实际值</th>
                              </template>
                            </tr>
                          </thead>
                          <tbody>
                            <tr>
                              <template
                                v-for="parameter in deviceGroup.parameters"
                                :key="parameter.key"
                              >
                              <td data-active-order-production-record-parameter-reference>
                                {{ parameter.parameterRangeText }}
                              </td>
                              <td
                                :class="{ 'team-leader-workbench__production-record-warning': parameter.outOfRange }"
                                data-active-order-production-record-parameter-actual
                              >
                                {{ parameter.parameterValueText }}
                              </td>
                              </template>
                            </tr>
                          </tbody>
                        </table>
                      </section>
                    </div>
                    <span v-else>暂无设备信息</span>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
          <el-empty v-else :image-size="56" description="暂无可生成的生产提交记录" />
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
              <span>批次数量</span>
              <strong>{{ pqcInspectionRecordSummary.batchQuantityText }}</strong>
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
                <td data-pqc-inspection-record-result-cell>{{ row.resultText }}</td>
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
                <td>
                  <div class="team-leader-workbench__signature-stack">
                    <button
                      v-for="signature in row.submitterSignatures"
                      :key="`pqc-submitter-${row.key}-${signature.signatureId}`"
                      type="button"
                      class="team-leader-workbench__signature-link"
                      data-active-order-pqc-inspection-record-inspector-signature
                      :disabled="!signature.signatureId"
                      @click="openActiveOrderSignatureRecord(signature)"
                    >
                      {{ formatActiveOrderSignatureCellText(signature) }}
                    </button>
                    <span v-if="!row.submitterSignatures.length">未签名</span>
                  </div>
                </td>
                <td>
                  <div class="team-leader-workbench__signature-stack">
                    <button
                      v-for="signature in row.reviewerSignatures"
                      :key="`pqc-reviewer-${row.key}-${signature.signatureId}`"
                      type="button"
                      class="team-leader-workbench__signature-link"
                      data-active-order-pqc-inspection-record-reviewer-signature
                      :disabled="!signature.signatureId"
                      @click="openActiveOrderSignatureRecord(signature)"
                    >
                      {{ formatActiveOrderSignatureCellText(signature) }}
                    </button>
                    <span v-if="!row.reviewerSignatures.length">未签名</span>
                  </div>
                </td>
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
  TeamLeaderActiveOrderSignatureDetailRespVO,
  TeamLeaderActiveOrderSupplementMaterialDetailRespVO
} from '@/api/mes/pro/processpool/teamLeader'
import type { ProWorkOrderVO } from '@/api/mes/pro/workorder'
import type { ErpProductionMaterialListVO } from '@/api/erp/production/material-list'
import { formatDateTimeValue } from '@/utils/formatTime'

const props = defineProps<{
  detail?: TeamLeaderActiveOrderDetailRespVO
  sourceWorkOrder?: ProWorkOrderVO
  productionMaterialLists?: ErpProductionMaterialListVO[]
  productionMaterialListLoading?: boolean
  productionMaterialListError?: string
  loading?: boolean
  error?: string
  embedded?: boolean
  displayMode?: 'full' | 'production' | 'pqc'
  productionRouteProcessId?: number | string
}>()

const emit = defineEmits<{
  retry: []
  'open-batch-execution-production-form': [process: TeamLeaderActiveOrderProcessDetailRespVO]
  'open-batch-execution-pqc-form': []
}>()

const activeTab = ref('')
const productionActiveTab = ref('')
const pqcActiveTab = ref('')
const productionRecordFormVisible = ref(false)
const selectedProductionRecordProcess = ref<TeamLeaderActiveOrderProcessDetailRespVO>()
const pqcInspectionRecordFormVisible = ref(false)
const router = useRouter()

const displayMode = computed(() => props.displayMode || 'full')
const embedded = computed(() => Boolean(props.embedded))
const showProductionSubmissionTab = computed(
  () => displayMode.value === 'full' || displayMode.value === 'production'
)
const showPqcSubmissionTab = computed(
  () => displayMode.value === 'full' || displayMode.value === 'pqc'
)
const showSummaryTab = computed(() => !embedded.value)
const blankSummaryField = ''

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
      drawingNumber: source.drawingNumber || '-',
      productSpecification: source.productSpecification || '-',
      productCode: source.productCode || '-',
      productName: source.productName || '-',
      unitName: source.unitMeasureName || '',
      workshopName: source.workshopName || '',
      createTime: formatDateTime(source.createTime)
    }
  }
  return {
    batchCode: detail?.batchCode || '-',
    workOrderCode: detail?.workOrderCode || '-',
    quantity: formatTraceQuantity(detail?.workOrderQuantity),
    drawingNumber: detail?.drawingNumber || '-',
    productSpecification: detail?.productSpecification || '-',
    productCode: detail?.productCode || '-',
    productName: detail?.productName || '-',
    unitName: '',
    workshopName: '',
    createTime: formatDateTime(detail?.workOrderCreateTime)
  }
})

interface ProductionMaterialListDocument {
  key: string
  sourceBillNo: string
  productionOrderNo: string
  productCode: string
  productName: string
  productSpecification: string
  unitName: string
  productionQuantity: string
  workshopName: string
  productionOrganization: string
  creatorName: string
  createDate: string
  auditorName: string
  auditDate: string
  rows: ErpProductionMaterialListVO[]
}

const readProductionMaterialListText = (
  row: ErpProductionMaterialListVO | undefined,
  keys: string[]
) => {
  if (!row) return ''
  const record = row as unknown as Record<string, unknown>
  for (const key of keys) {
    const value = record[key]
    if (value !== undefined && value !== null && String(value).trim()) {
      return String(value).trim()
    }
  }
  return ''
}

const formatProductionMaterialListQuantity = (value: number | string | undefined) => {
  if (value === undefined || value === null || value === '') return ''
  const parsed = Number(value)
  return Number.isFinite(parsed) ? String(Math.round(parsed)) : String(value)
}

const formatDate = (value?: string | number | Date) => {
  const text = formatDateTime(value)
  if (!text || text === '-') return ''
  return text.split(' ')[0]
}

const resolveProductionMaterialListDrawingNo = (row: ErpProductionMaterialListVO) =>
  readProductionMaterialListText(row, ['drawingNumber', 'drawingNo', 'blueprintNo', 'figureNo'])

const resolveProductionMaterialListActualUsage = (row: ErpProductionMaterialListVO) =>
  formatProductionMaterialListQuantity(
    readProductionMaterialListText(row, ['actualQuantity', 'actualUsageQuantity', 'actualUsage'])
  )

const resolveProductionMaterialListWarehouse = (row: ErpProductionMaterialListVO) =>
  readProductionMaterialListText(row, ['warehouseName', 'stockName', 'warehouse'])

const productionMaterialListDocuments = computed<ProductionMaterialListDocument[]>(() => {
  const grouped = new Map<string, ErpProductionMaterialListVO[]>()
  for (const row of props.productionMaterialLists ?? []) {
    const sourceBillNo = String(row.sourceBillNo || '').trim()
    if (!sourceBillNo) continue
    if (!grouped.has(sourceBillNo)) {
      grouped.set(sourceBillNo, [])
    }
    grouped.get(sourceBillNo)?.push(row)
  }
  return Array.from(grouped.entries()).map(([sourceBillNo, rows]) => {
    const first = rows[0]
    return {
      key: sourceBillNo,
      sourceBillNo,
      productionOrderNo: first.productionOrderNo || activeOrderWorkOrderDisplay.value.workOrderCode || '',
      productCode: first.productCode || activeOrderWorkOrderDisplay.value.productCode || '',
      productName: activeOrderWorkOrderDisplay.value.productName === '-' ? '' : activeOrderWorkOrderDisplay.value.productName,
      productSpecification:
        activeOrderWorkOrderDisplay.value.productSpecification === '-'
          ? ''
          : activeOrderWorkOrderDisplay.value.productSpecification,
      unitName: activeOrderWorkOrderDisplay.value.unitName,
      productionQuantity:
        activeOrderWorkOrderDisplay.value.quantity === '-' ? '' : activeOrderWorkOrderDisplay.value.quantity,
      workshopName: activeOrderWorkOrderDisplay.value.workshopName,
      productionOrganization: readProductionMaterialListText(first, ['productionOrganization', 'organizationName']),
      creatorName: readProductionMaterialListText(first, ['creatorName', 'creator', 'createUserName']),
      createDate: formatDate(first.createTime),
      auditorName: readProductionMaterialListText(first, ['auditorName', 'auditUserName', 'reviewerName']),
      auditDate: formatDate(readProductionMaterialListText(first, ['auditTime', 'auditDate', 'reviewTime'])),
      rows
    }
  })
})

const visibleProductionProcesses = computed(() => {
  const processes = props.detail?.processes ?? []
  if (displayMode.value !== 'production' || props.productionRouteProcessId == null) {
    return processes
  }
  const routeProcessId = String(props.productionRouteProcessId)
  return processes.filter((process) => String(process.routeProcessId) === routeProcessId)
})

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
  submitterSignatures: TeamLeaderActiveOrderSignatureDetailRespVO[]
  reviewerSignatures: TeamLeaderActiveOrderSignatureDetailRespVO[]
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
  submitterSignature?: TeamLeaderActiveOrderSignatureDetailRespVO
  reviewerSignature?: TeamLeaderActiveOrderSignatureDetailRespVO
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
  submitterSignature: submission.submitterSignature,
  reviewerSignature: submission.reviewerSignature,
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
  submitterSignature: submission.submitterSignature,
  reviewerSignature: submission.reviewerSignature,
  quantityConflict: submission.quantityConflict,
  devices: submission.devices
})

const formatActiveOrderSignatureCellText = (
  signature?: TeamLeaderActiveOrderSignatureDetailRespVO
) => {
  if (!signature?.signatureId) return '未签名'
  const signerName = String(signature.signerName || '').trim() || '签名人未记录'
  const signedAt = formatDateTime(signature.signedAt)
  const signedAtText = signedAt && signedAt !== '-' ? signedAt : '签名时间未记录'
  return `${signerName}（${signedAtText}）`
}

const openActiveOrderSignatureRecord = (
  signature?: TeamLeaderActiveOrderSignatureDetailRespVO
) => {
  if (!signature?.signatureId) return
  router.push({
    path: '/signature-governance/signature-records',
    query: {
      quickFilterField: 'keyword',
      quickFilterValue: String(signature.signatureId)
    }
  })
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

const selectedProductionInputMaterials = computed(
  () => selectedProductionRecordProcess.value?.inputMaterials ?? []
)

const buildProductionRecordTotals = (rows: ActiveOrderProductionRecordRow[]) => {
  const sum = (field: keyof Pick<ActiveOrderProductionRecordRow, 'outputQuantity' | 'lossQuantity' | 'totalQuantity'>) =>
    rows.reduce((total, row) => total + (toNumberOrUndefined(row[field]) ?? 0), 0)
  return {
    outputQuantity: sum('outputQuantity'),
    lossQuantity: sum('lossQuantity'),
    totalQuantity: sum('totalQuantity')
  }
}

const productionRecordTotals = computed(() => buildProductionRecordTotals(selectedProductionRecordRows.value))

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

const buildProductionRecordRowDeviceGroups = (row: ActiveOrderProductionRecordRow) =>
  buildProductionRecordMaterialDeviceGroups([row])[0]?.devices ?? []

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

const formatActiveOrderPqcItemCountSummary = (values: Array<string | number | null | undefined>) => {
  const counts = new Map<string, number>()
  for (const value of values.map((item) => normalizeActiveOrderPqcText(item)).filter(Boolean)) {
    counts.set(value, (counts.get(value) ?? 0) + 1)
  }
  const parts = Array.from(counts.entries())
    .sort(([left], [right]) => left.localeCompare(right, 'zh-Hans-CN'))
    .map(([value, count]) => (count > 1 ? `${value} × ${count}` : value))
  return parts.length ? parts.join('；') : '-'
}

const formatActiveOrderPqcItemUniqueSummary = (values: Array<string | number | null | undefined>) => {
  const uniqueValues = Array.from(
    new Set(values.map((item) => normalizeActiveOrderPqcText(item)).filter(Boolean))
  ).sort(
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
        .map((item) => {
          const equipmentName = normalizeActiveOrderPqcText(item.selectedEquipmentName)
          const equipmentNumber = normalizeActiveOrderPqcText(item.selectedEquipmentNumber)
          if (equipmentName && equipmentNumber) return `${equipmentName}(${equipmentNumber})`
          return equipmentName || equipmentNumber
        })
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
  return normalizePqcPassFailText(item.judgement || item.itemResult || item.measuredValue) || '未记录'
}

const normalizePqcPassFailText = (value?: string) => {
  const rawText = normalizeActiveOrderPqcText(value)
  if (!rawText) return ''
  const upperText = rawText.toUpperCase()
  const negativeKeywords = ['不合格', '不通过', '否', 'NG', 'FAIL', 'FAILED', 'FAILURE', 'FALSE', 'ERROR']
  if (negativeKeywords.some((keyword) => upperText.includes(keyword))) return '不通过'
  const positiveKeywords = ['合格', '通过', '是', 'OK', 'PASS', 'SUCCESS', 'TRUE']
  if (positiveKeywords.some((keyword) => upperText.includes(keyword))) return '通过'
  return rawText
}

const formatPqcInspectionMeasuredValues = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) => {
  const values = items
    .map((item) => normalizePqcPassFailText(item.measuredValue || item.itemResult))
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
  if (judgements.includes('不通过')) return '不通过'
  if (judgements.length === 1 && judgements[0] === '通过') return '通过'
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
  measuredValuesText: string,
  _sampleSummaryText: string
) => {
  return measuredValuesText && measuredValuesText !== '-' ? measuredValuesText : '-'
}

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
            measuredValuesText,
            sampleSummaryText
          ),
          sampleSummaryText,
          measuredValuesText,
          judgementText,
          equipmentText,
          submitterName,
          reviewerName,
          submitterSignatures: submission.submitterSignatures ?? [],
          reviewerSignatures: submission.reviewerSignatures ?? []
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
  const batchQuantity = formatTraceQuantity(props.detail?.workOrderQuantity)
  return {
    submissionCount: submissions.length,
    itemCount: pqcInspectionRecordRows.value.length,
    inspectionQuantityText: inspectionQuantities.length
      ? `${inspectionQuantities.reduce((total, value) => total + value, 0)} 件`
      : '-',
    batchQuantityText: batchQuantity === '-' ? '-' : `${batchQuantity} 件`,
    judgementText: summarizePqcInspectionJudgements(
      submissions.flatMap((submission) => submission.items ?? [])
    )
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
      const qaProcessName = normalizeActiveOrderPqcText(submission.qaProcessName)
      if (!Number.isFinite(qaProcessId) || qaProcessId <= 0 || !qaProcessName) {
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
        qaProcessName,
        submissions: [submission]
      })
    }
  }
  return Array.from(groupsByQaProcessId.values())
})

type ActiveOrderDetailPickListMaterialRow = TeamLeaderActiveOrderInputMaterialDetailRespVO & {
  sourceProcessNames: string[]
}

interface ActiveOrderSummaryPair<T> {
  key: string
  left?: T
  right?: T
}

interface ActiveOrderSummaryProcessPersonnelRow {
  key: string
  processName: string
  operatorText: string
  assemblyDateText: string
}

interface ActiveOrderPqcLossReportRow {
  key: string
  unqualifiedDateText: string
  processNameText: string
  unqualifiedQuantityText: string
  unqualifiedReasonText: string
  disposalMethodText: string
  productionOperatorDateText: string
  inspectorConfirmationDateText: string
  approverDateText: string
}

const buildActiveOrderSummaryPairs = <T,>(
  items: T[],
  resolveKey: (item: T, index: number) => string
): ActiveOrderSummaryPair<T>[] => {
  const pairs: ActiveOrderSummaryPair<T>[] = []
  for (let index = 0; index < items.length; index += 2) {
    const left = items[index]
    const right = items[index + 1]
    pairs.push({
      key: [
        left ? resolveKey(left, index) : 'blank',
        right ? resolveKey(right, index + 1) : 'blank'
      ].join('__'),
      left,
      right
    })
  }
  return pairs
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

const formatSummaryMaterialBatchCodes = (
  material?: ActiveOrderDetailPickListMaterialRow
) => (material ? formatActiveOrderBatchCodes(material.batchCodes) : blankSummaryField)

const summaryPickListMaterialPairs = computed(() =>
  buildActiveOrderSummaryPairs(
    pickListMaterials.value,
    (material, index) =>
      [
        index,
        material.materialCode,
        material.materialName,
        formatActiveOrderBatchCodes(material.batchCodes)
      ].join('::')
  )
)

const formatSummaryDate = (value?: string | number | Date) => {
  const formatted = formatDateTime(value)
  if (!formatted || formatted === '-') return blankSummaryField
  return formatted.split(' ')[0] || blankSummaryField
}

const formatSummaryCycleDate = (value?: string | number | Date) => {
  const dateText = formatSummaryDate(value)
  return dateText ? dateText.replaceAll('-', '.') : blankSummaryField
}

const summaryProductionCycleText = computed(() => {
  const submitTimes = (props.detail?.processes ?? [])
    .flatMap((process) => process.submissions ?? [])
    .map((submission) => submission.submittedAt)
    .filter((submittedAt): submittedAt is string | number =>
      submittedAt !== undefined && submittedAt !== null && submittedAt !== ''
    )
    .map((submittedAt) => ({
      submittedAt,
      timestamp: new Date(submittedAt).getTime()
    }))
    .filter((item) => Number.isFinite(item.timestamp))
    .sort((left, right) => left.timestamp - right.timestamp)
  if (!submitTimes.length) return blankSummaryField
  const startDate = formatSummaryCycleDate(submitTimes[0].submittedAt)
  const endDate = formatSummaryCycleDate(submitTimes[submitTimes.length - 1].submittedAt)
  if (!startDate || !endDate) return blankSummaryField
  return startDate === endDate ? startDate : `${startDate}~${endDate}`
})

const resolveSummarySignatureTime = (
  signature?: TeamLeaderActiveOrderSignatureDetailRespVO
) => {
  if (!signature?.signatureId || signature.signedAt === undefined || signature.signedAt === null) {
    return Number.NEGATIVE_INFINITY
  }
  const parsed = new Date(signature.signedAt).getTime()
  return Number.isFinite(parsed) ? parsed : Number.NEGATIVE_INFINITY
}

const resolveLatestProductionSubmitterSignature = (
  submissions: TeamLeaderActiveOrderSubmissionDetailRespVO[] = []
) => {
  return submissions.reduce<TeamLeaderActiveOrderSignatureDetailRespVO | undefined>(
    (latestSignature, submission) => {
      const currentSignature = submission.submitterSignature
      if (!currentSignature?.signatureId) return latestSignature
      if (!latestSignature) return currentSignature
      return resolveSummarySignatureTime(currentSignature) >=
        resolveSummarySignatureTime(latestSignature)
        ? currentSignature
        : latestSignature
    },
    undefined
  )
}

const formatSummarySignatureOperator = (
  signature?: TeamLeaderActiveOrderSignatureDetailRespVO
) => {
  if (!signature?.signatureId) return blankSummaryField
  return String(signature.signerName || '').trim() || blankSummaryField
}

const formatSummarySignatureDate = (
  signature?: TeamLeaderActiveOrderSignatureDetailRespVO
) => {
  if (!signature?.signatureId) return blankSummaryField
  return formatSummaryDate(signature.signedAt)
}

const summaryProcessPersonnelRows = computed<ActiveOrderSummaryProcessPersonnelRow[]>(() => {
  return (props.detail?.processes ?? []).map((process, index) => {
    const latestSubmitterSignature = resolveLatestProductionSubmitterSignature(process.submissions)
    return {
      key: [
        process.routeProcessId,
        process.processId,
        process.processName,
        index
      ].join('::'),
      processName: process.processName || blankSummaryField,
      operatorText: formatSummarySignatureOperator(latestSubmitterSignature),
      assemblyDateText: formatSummarySignatureDate(latestSubmitterSignature)
    }
  })
})

const summaryProcessPersonnelPairs = computed(() =>
  buildActiveOrderSummaryPairs(
    summaryProcessPersonnelRows.value,
    (process, index) => `${index}::${process.key}`
  )
)

const resolveLatestSignature = (
  signatures: TeamLeaderActiveOrderSignatureDetailRespVO[] = []
) => {
  return signatures.reduce<TeamLeaderActiveOrderSignatureDetailRespVO | undefined>(
    (latestSignature, signature) => {
      if (!signature?.signatureId) return latestSignature
      if (!latestSignature) return signature
      return resolveSummarySignatureTime(signature) >= resolveSummarySignatureTime(latestSignature)
        ? signature
        : latestSignature
    },
    undefined
  )
}

const formatLossReportSignatureDateText = (
  signature?: TeamLeaderActiveOrderSignatureDetailRespVO
) => {
  if (!signature?.signatureId) return blankSummaryField
  const operator = formatSummarySignatureOperator(signature)
  const date = formatSummarySignatureDate(signature)
  return [operator, date].filter(Boolean).join(' / ')
}

const formatPqcLossReason = (
  submission: TeamLeaderActiveOrderPqcSubmissionDetailRespVO
) => {
  const negativeItems = (submission.items ?? []).filter(
    (item) => normalizePqcPassFailText(item.judgement || item.itemResult || item.measuredValue) === '不通过'
  )
  const sourceItems = negativeItems.length ? negativeItems : submission.items ?? []
  const reasons = sourceItems
    .map((item) => {
      const itemName = normalizeActiveOrderPqcText(item.itemName || item.itemCode)
      const measuredValue = normalizeActiveOrderPqcText(item.measuredValue || item.itemResult)
      const judgement = normalizeActiveOrderPqcText(item.judgement)
      return [itemName, measuredValue, judgement].filter(Boolean).join(' / ')
    })
    .filter(Boolean)
  return reasons.length ? formatActiveOrderPqcItemUniqueSummary(reasons) : blankSummaryField
}

const pqcLossReportRows = computed<ActiveOrderPqcLossReportRow[]>(() => {
  const rows: ActiveOrderPqcLossReportRow[] = []
  for (const process of props.detail?.processes ?? []) {
    const latestProductionSignature = resolveLatestProductionSubmitterSignature(process.submissions)
    const productionOperatorDateText = formatLossReportSignatureDateText(latestProductionSignature)
    for (const submission of process.pqcSubmissions ?? []) {
      const scrapQuantity = Number(submission.scrapQuantity)
      if (!Number.isFinite(scrapQuantity) || scrapQuantity <= 0) {
        continue
      }
      const latestPqcSubmitterSignature = resolveLatestSignature(submission.submitterSignatures)
      const latestPqcReviewerSignature = resolveLatestSignature(submission.reviewerSignatures)
      rows.push({
        key: [
          process.routeProcessId,
          process.processId,
          submission.pqcTaskId,
          submission.submittedEventId,
          rows.length
        ].join('::'),
        unqualifiedDateText: formatSummaryCycleDate(submission.businessDate) || blankSummaryField,
        processNameText: submission.qaProcessName || process.processName || blankSummaryField,
        unqualifiedQuantityText: formatTraceQuantity(scrapQuantity),
        unqualifiedReasonText: formatPqcLossReason(submission),
        disposalMethodText: '☑ 报废　☐ 其他：',
        productionOperatorDateText,
        inspectorConfirmationDateText: formatLossReportSignatureDateText(latestPqcSubmitterSignature),
        approverDateText: formatLossReportSignatureDateText(latestPqcReviewerSignature)
      })
    }
  }
  return rows
})

const pqcLossReportApprovalText = computed(() => {
  const approvalTexts = Array.from(
    new Set(pqcLossReportRows.value.map((row) => row.approverDateText).filter(Boolean))
  )
  return approvalTexts.length ? approvalTexts.join('；') : blankSummaryField
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

const resetTabs = async () => {
  await nextTick()
  const firstProcess = visibleProductionProcesses.value[0]
  activeTab.value = showSummaryTab.value
    ? 'summary'
    : displayMode.value === 'pqc'
      ? 'pqcSubmissions'
      : 'productionSubmissions'
  productionActiveTab.value = firstProcess ? activeOrderDetailProcessTabName(firstProcess, 0) : ''
  const firstPqcProcess = pqcProcessGroups.value[0]
  pqcActiveTab.value = firstPqcProcess
    ? resolveActiveOrderPqcProcessTabName(firstPqcProcess, 0)
    : ''
}

watch(
  () => [props.detail, props.displayMode, props.productionRouteProcessId],
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

.team-leader-workbench__active-order-summary {
  display: grid;
  gap: 14px;
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
}

.team-leader-workbench__active-order-summary-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  background: var(--el-bg-color);
  font-size: 13px;
}

.team-leader-workbench__active-order-summary-table th,
.team-leader-workbench__active-order-summary-table td {
  min-height: 36px;
  padding: 9px 10px;
  border: 1px solid var(--el-border-color);
  color: var(--el-text-color-primary);
  text-align: center;
  vertical-align: middle;
  word-break: break-word;
}

.team-leader-workbench__active-order-summary-table th {
  font-weight: 700;
  background: var(--el-fill-color-light);
}

.team-leader-workbench__active-order-summary-title {
  font-size: 16px;
  letter-spacing: 0.04em;
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

.team-leader-workbench__production-material-list-error {
  margin-bottom: 12px;
}

.team-leader-workbench__production-material-list-documents {
  display: grid;
  gap: 18px;
  max-width: 100%;
  overflow-x: hidden;
}

.team-leader-workbench__production-material-list-document {
  display: grid;
  gap: 0;
  max-width: 100%;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
}

.team-leader-workbench__production-material-list-document h3 {
  margin: 0;
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-fill-color-lighter);
  text-align: center;
  font-size: 17px;
  font-weight: 700;
}

.team-leader-workbench__production-material-list-head {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-bottom: 1px solid var(--el-border-color);
}

.team-leader-workbench__production-material-list-head > div,
.team-leader-workbench__production-material-list-footer > div {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 36px;
  padding: 6px 10px;
  border-right: 1px solid var(--el-border-color);
  border-bottom: 1px solid var(--el-border-color);
}

.team-leader-workbench__production-material-list-head > div:nth-child(3n),
.team-leader-workbench__production-material-list-footer > div:nth-child(4n) {
  border-right: 0;
}

.team-leader-workbench__production-material-list-head > div:nth-last-child(-n + 3),
.team-leader-workbench__production-material-list-footer > div {
  border-bottom: 0;
}

.team-leader-workbench__production-material-list-head span,
.team-leader-workbench__production-material-list-footer span {
  flex: 0 0 auto;
  font-weight: 700;
}

.team-leader-workbench__production-material-list-head strong,
.team-leader-workbench__production-material-list-footer strong {
  min-width: 0;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.team-leader-workbench__production-material-list-table {
  width: 100%;
  max-width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 12px;
}

.team-leader-workbench__production-material-list-table th,
.team-leader-workbench__production-material-list-table td {
  min-height: 32px;
  padding: 7px 6px;
  border-right: 1px solid var(--el-border-color);
  border-bottom: 1px solid var(--el-border-color);
  text-align: center;
  vertical-align: middle;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.team-leader-workbench__production-material-list-table th {
  background: var(--el-fill-color-light);
  font-weight: 700;
}

.team-leader-workbench__production-material-list-table th:last-child,
.team-leader-workbench__production-material-list-table td:last-child {
  border-right: 0;
}

.team-leader-workbench__production-material-list-footer {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.team-leader-workbench__production-record-form-title
  .team-leader-workbench__production-record-process-title {
  flex: 0 1 auto;
  min-width: 0;
  text-align: center;
  font-size: 24px;
  font-weight: 700;
}

.team-leader-workbench__production-record-form-title span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.team-leader-workbench__production-record-key-flag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.team-leader-workbench__production-record-key-flag.is-key {
  border-color: var(--el-color-warning-light-5);
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning-dark-2);
}

.team-leader-workbench__production-record-key-flag.is-non-key {
  border-color: var(--el-border-color-light);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}

.team-leader-workbench__signature-link {
  max-width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: #008f83;
  cursor: pointer;
  font: inherit;
  line-height: 1.5;
  text-align: left;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.team-leader-workbench__signature-link:disabled {
  color: var(--el-text-color-secondary);
  cursor: default;
  text-decoration: none;
}

.team-leader-workbench__signature-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
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

.team-leader-workbench__production-record-two-line-table {
  table-layout: fixed;
  border-collapse: collapse;
  border: 1px solid var(--el-border-color);
  color: var(--el-text-color-primary);
  font-size: 12px;
}

.team-leader-workbench__production-record-two-line-table th,
.team-leader-workbench__production-record-two-line-table td {
  padding: 7px 8px;
  border: 1px solid var(--el-border-color);
  vertical-align: top;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.team-leader-workbench__production-record-two-line-table th {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-weight: 600;
  text-align: center;
}

.team-leader-workbench__production-record-two-line-table
  [data-active-order-production-record-main-row],
.team-leader-workbench__production-record-two-line-table
  [data-active-order-production-record-device-row] {
  page-break-inside: avoid;
  break-inside: avoid;
}

.team-leader-workbench__production-record-device-row-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 8px;
}

.team-leader-workbench__production-record-device-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 8px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: var(--el-fill-color-extra-light);
  page-break-inside: avoid;
  break-inside: avoid;
}

.team-leader-workbench__production-record-device-card-title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.team-leader-workbench__production-record-parameter-mini-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  background: var(--el-bg-color);
}

.team-leader-workbench__production-record-parameter-mini-table th,
.team-leader-workbench__production-record-parameter-mini-table td {
  padding: 5px 6px;
  border: 1px solid var(--el-border-color-lighter);
}

@media print {
  .team-leader-workbench__production-record-two-line-table,
  .team-leader-workbench__production-record-parameter-mini-table {
    font-size: 10px;
  }

  .team-leader-workbench__production-record-two-line-table
    [data-active-order-production-record-main-row],
  .team-leader-workbench__production-record-two-line-table
    [data-active-order-production-record-device-row],
  .team-leader-workbench__production-record-device-card {
    page-break-inside: avoid;
    break-inside: avoid;
  }
}

.team-leader-workbench__production-record-device-table {
  width: 410px;
  max-width: 100%;
}

.team-leader-workbench__production-record-device-parameter-table {
  width: 440px;
  max-width: 100%;
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
