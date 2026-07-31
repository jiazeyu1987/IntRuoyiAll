<template>
  <ContentWrap>
    <div class="edhr-label-print-page">
      <el-tabs v-model="activeTab" class="edhr-label-print-page__tabs" @tab-change="handleTabChange">
        <el-tab-pane label="标签模板" name="template" />
        <el-tab-pane label="标签实例" name="label" />
        <el-tab-pane label="打印任务" name="printTask" />
        <el-tab-pane label="打印策略" name="printPolicy" />
      </el-tabs>

      <section v-show="activeTab === 'template'" class="edhr-label-print-page__section">
        <el-form :inline="true" :model="templateQuery" class="edhr-label-print-page__toolbar">
          <el-form-item label="模板编码">
            <el-input v-model="templateQuery.templateCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="模板名称">
            <el-input v-model="templateQuery.templateName" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="业务对象">
            <el-input v-model="templateQuery.businessObjectType" clearable class="!w-150px" />
          </el-form-item>
          <el-form-item label="模板状态">
            <el-select v-model="templateQuery.status" clearable class="!w-130px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已启用" value="ACTIVE" />
              <el-option label="已停用" value="DISABLED" />
              <el-option label="已作废" value="VOID" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleTemplateQuery">查询</el-button>
            <el-button @click="resetTemplateQuery">重置</el-button>
            <el-button
              v-hasPermi="['mes:pro-edhr-label-template:create']"
              type="success"
              @click="openTemplateDialog"
            >
              创建模板
            </el-button>
          </el-form-item>
        </el-form>

        <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
        <el-alert v-if="labelError" :title="labelError" type="error" :closable="false" show-icon />

        <el-table
          v-loading="templateLoading"
          :data="labelTemplateList"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无标签模板"
        >
          <el-table-column label="模板编码" prop="templateCode" min-width="170" />
          <el-table-column label="模板名称" prop="templateName" min-width="180" />
          <el-table-column label="模板版本" prop="templateVersion" width="120" />
          <el-table-column label="业务对象" prop="businessObjectType" width="130" />
          <el-table-column label="字段模型" prop="fieldModelJson" min-width="220" />
          <el-table-column label="解析版本" prop="parserVersion" width="120" />
          <el-table-column label="水印" prop="watermarkTemplate" min-width="160" />
          <el-table-column label="模板状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ resolveTemplateStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button
                v-hasPermi="['mes:pro-edhr-label-template:activate']"
                link
                type="primary"
                :disabled="row.status === 'ACTIVE'"
                @click="activateTemplate(row)"
              >
                启用
              </el-button>
              <el-button
                v-hasPermi="['mes:pro-edhr-label:preview']"
                link
                type="primary"
                @click="openPreviewDialog(row)"
              >
                预览
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="templateTotal"
          v-model:page="templateQuery.pageNo"
          v-model:limit="templateQuery.pageSize"
          @pagination="loadLabelTemplateList"
        />
      </section>

      <section v-show="activeTab === 'label'" class="edhr-label-print-page__section">
        <el-form :inline="true" :model="labelQuery" class="edhr-label-print-page__toolbar">
          <el-form-item label="标签编码">
            <el-input v-model="labelQuery.labelCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="业务对象">
            <el-input v-model="labelQuery.businessObjectCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="打印状态">
            <el-select v-model="labelQuery.printStatus" clearable class="!w-160px">
              <el-option label="未打印" value="NOT_PRINTED" />
              <el-option label="等待打印" value="WAITING" />
              <el-option label="待确认" value="PENDING_CONFIRM" />
              <el-option label="确认成功" value="SUCCESS_CONFIRMED" />
              <el-option label="打印失败" value="FAILED" />
              <el-option label="作废受限" value="VOID_RESTRICTED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleLabelQuery">查询</el-button>
            <el-button @click="resetLabelQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

        <el-table
          v-loading="labelLoading"
          :data="labelInstanceList"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无标签实例"
        >
          <el-table-column label="标签编码" prop="labelCode" min-width="180" />
          <el-table-column label="模板" min-width="170">
            <template #default="{ row }">
              <div class="edhr-label-print-page__strong">{{ row.templateCode || '--' }}</div>
              <div class="edhr-label-print-page__muted">版本：{{ row.templateVersion || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="业务对象" min-width="220">
            <template #default="{ row }">
              <div class="edhr-label-print-page__strong">{{ row.businessObjectCode || '--' }}</div>
              <div class="edhr-label-print-page__muted">{{ row.businessType || '--' }} / {{ row.businessObjectId || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="解析版本" prop="parserVersion" width="120" />
          <el-table-column label="渲染快照" prop="renderSnapshotJson" min-width="260" />
          <el-table-column label="打印状态" width="120">
            <template #default="{ row }">
              <el-tag :type="resolvePrintStatusType(row.printStatus)">
                {{ resolvePrintStatusLabel(row.printStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="生成时间" prop="generatedAt" width="180" :formatter="edhrDateTimeFormatter" />
        </el-table>
        <Pagination
          :total="labelTotal"
          v-model:page="labelQuery.pageNo"
          v-model:limit="labelQuery.pageSize"
          @pagination="loadLabelInstanceList"
        />
      </section>

      <section v-show="activeTab === 'printTask'" class="edhr-label-print-page__section">
        <el-form :inline="true" :model="printTaskQuery" class="edhr-label-print-page__toolbar">
          <el-form-item label="任务编码">
            <el-input v-model="printTaskQuery.taskCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="业务对象">
            <el-input v-model="printTaskQuery.sourceObjectCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="打印状态">
            <el-select v-model="printTaskQuery.status" clearable class="!w-160px">
              <el-option label="待打印" value="WAITING" />
              <el-option label="打印中" value="PRINTING" />
              <el-option label="待确认" value="PENDING_CONFIRM" />
              <el-option label="确认成功" value="SUCCESS_CONFIRMED" />
              <el-option label="打印失败" value="FAILED" />
              <el-option label="作废受限" value="VOID_RESTRICTED" />
            </el-select>
          </el-form-item>
          <el-form-item label="补打">
            <el-select v-model="printTaskQuery.isReprint" clearable class="!w-110px">
              <el-option label="是" :value="true" />
              <el-option label="否" :value="false" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handlePrintTaskQuery">查询</el-button>
            <el-button @click="resetPrintTaskQuery">重置</el-button>
            <el-button
              v-hasPermi="['mes:pro-edhr-print-task:create']"
              type="success"
              @click="openPrintTaskDialog"
            >
              创建打印任务
            </el-button>
            <el-button
              v-hasPermi="['mes:pro-edhr-print-task:export']"
              type="warning"
              @click="openExportDialog"
            >
              导出历史
            </el-button>
          </el-form-item>
        </el-form>

        <el-alert v-if="printTaskError" :title="printTaskError" type="error" :closable="false" show-icon />
        <el-alert v-if="reprintError" :title="reprintError" type="error" :closable="false" show-icon />
        <el-alert v-if="historyCopyError" :title="historyCopyError" type="error" :closable="false" show-icon />
        <el-alert v-if="exportError" :title="exportError" type="error" :closable="false" show-icon />

        <el-table
          v-loading="printTaskLoading"
          :data="printTaskList"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无打印任务"
        >
          <el-table-column label="打印任务" prop="taskCode" min-width="180" />
          <el-table-column label="业务对象" min-width="220">
            <template #default="{ row }">
              <div class="edhr-label-print-page__strong">{{ row.sourceObjectCode || '--' }}</div>
              <div class="edhr-label-print-page__muted">{{ row.sourceType || '--' }} / {{ row.sourceObjectId || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="模板" min-width="160">
            <template #default="{ row }">
              <div>{{ row.templateCode || '--' }}</div>
              <div class="edhr-label-print-page__muted">{{ row.templateType || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="打印状态" width="120">
            <template #default="{ row }">
              <el-tag :type="resolvePrintStatusType(row.status)">
                {{ resolvePrintStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="确认状态" prop="printConfirmStatus" width="130" />
          <el-table-column label="补打原因" prop="reprintReason" min-width="180" />
          <el-table-column label="失败原因" prop="failureReason" min-width="180" />
          <el-table-column label="扣减次数" width="100">
            <template #default="{ row }">
              <el-tag :type="row.printCountDeducted ? 'danger' : 'info'">
                {{ row.printCountDeducted ? '已扣减' : '未扣减' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发起时间" prop="requestedAt" width="180" :formatter="edhrDateTimeFormatter" />
          <el-table-column label="操作" width="330" fixed="right">
            <template #default="{ row }">
              <el-button
                v-hasPermi="['mes:pro-edhr-print-task:reprint']"
                link
                type="primary"
                @click="openReprintDialog(row)"
              >
                补打申请
              </el-button>
              <el-button
                v-hasPermi="['mes:pro-edhr-print-task:history-copy']"
                link
                type="warning"
                @click="openHistoryCopyDialog(row)"
              >
                作废历史副本
              </el-button>
              <el-button
                v-hasPermi="['mes:pro-edhr-print-task:mark-failed']"
                link
                type="danger"
                @click="openMarkFailedDialog(row)"
              >
                标记失败
              </el-button>
              <el-button
                v-hasPermi="['mes:pro-edhr-print-task:confirm']"
                link
                type="primary"
                @click="openConfirmDialog(row)"
              >
                确认成功
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="printTaskTotal"
          v-model:page="printTaskQuery.pageNo"
          v-model:limit="printTaskQuery.pageSize"
          @pagination="loadPrintTaskList"
        />
      </section>

      <section v-show="activeTab === 'printPolicy'" class="edhr-label-print-page__section">
        <el-form :inline="true" :model="printPolicyQuery" class="edhr-label-print-page__toolbar">
          <el-form-item label="策略编码">
            <el-input v-model="printPolicyQuery.policyCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="策略名称">
            <el-input v-model="printPolicyQuery.policyName" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="业务类型">
            <el-input v-model="printPolicyQuery.businessType" clearable class="!w-140px" />
          </el-form-item>
          <el-form-item label="模板类型">
            <el-select v-model="printPolicyQuery.templateType" clearable class="!w-160px">
              <el-option label="标签模板" value="LABEL_TEMPLATE" />
              <el-option label="流转单模板" value="TRAVELER_TEMPLATE" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="printPolicyQuery.status" clearable class="!w-120px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已启用" value="ACTIVE" />
              <el-option label="已停用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handlePrintPolicyQuery">查询</el-button>
            <el-button @click="resetPrintPolicyQuery">重置</el-button>
            <el-button
              v-hasPermi="['mes:pro-edhr-print-policy:create']"
              type="success"
              @click="openPrintPolicyDialog"
            >
              创建策略
            </el-button>
          </el-form-item>
        </el-form>

        <el-alert v-if="printPolicyError" :title="printPolicyError" type="error" :closable="false" show-icon />

        <el-table
          v-loading="printPolicyLoading"
          :data="printPolicyList"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无打印策略"
        >
          <el-table-column label="打印策略" min-width="190">
            <template #default="{ row }">
              <div class="edhr-label-print-page__strong">{{ row.policyName || '--' }}</div>
              <div class="edhr-label-print-page__muted">{{ row.businessType || '--' }} / {{ row.templateType || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="策略编码" prop="policyCode" min-width="170" />
          <el-table-column label="首次次数" prop="firstPrintLimit" width="100" />
          <el-table-column label="补打上限" prop="reprintLimit" width="100" />
          <el-table-column label="原因字典" prop="reasonDictJson" min-width="220" />
          <el-table-column label="水印模板" prop="watermarkTemplate" min-width="180" />
          <el-table-column label="作废水印" prop="voidCopyWatermark" min-width="200" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ resolvePrintPolicyStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                v-hasPermi="['mes:pro-edhr-print-policy:activate']"
                link
                type="primary"
                :disabled="row.status === 'ACTIVE'"
                @click="activatePrintPolicy(row)"
              >
                启用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="printPolicyTotal"
          v-model:page="printPolicyQuery.pageNo"
          v-model:limit="printPolicyQuery.pageSize"
          @pagination="loadPrintPolicyList"
        />
      </section>
    </div>

    <Dialog title="创建标签模板" v-model="templateDialogVisible" width="760px">
      <el-alert v-if="labelError" :title="labelError" type="error" :closable="false" show-icon />
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="templateForm.templateCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板版本" prop="templateVersion">
              <el-input v-model="templateForm.templateVersion" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="templateForm.templateName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务对象" prop="businessObjectType">
              <el-input v-model="templateForm.businessObjectType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="解析版本" prop="parserVersion">
              <el-input v-model="templateForm.parserVersion" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="水印模板">
              <el-input v-model="templateForm.watermarkTemplate" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="字段模型" prop="fieldModelJson">
              <el-input v-model="templateForm.fieldModelJson" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="布局" prop="layoutJson">
              <el-input v-model="templateForm.layoutJson" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="templateSubmitting" @click="submitTemplate">保存</el-button>
      </template>
    </Dialog>

    <Dialog title="标签真实数据预览" v-model="previewDialogVisible" width="680px">
      <el-alert v-if="labelError" :title="labelError" type="error" :closable="false" show-icon />
      <el-form ref="previewFormRef" :model="previewForm" :rules="previewRules" label-width="130px">
        <el-form-item label="模板ID" prop="templateId">
          <el-input-number v-model="previewForm.templateId" :min="1" :controls="false" class="!w-100%" />
        </el-form-item>
        <el-form-item label="业务对象" prop="businessType">
          <el-input v-model="previewForm.businessType" />
        </el-form-item>
        <el-form-item label="业务对象ID" prop="businessObjectId">
          <el-input-number v-model="previewForm.businessObjectId" :min="1" :controls="false" class="!w-100%" />
        </el-form-item>
        <el-form-item label="业务对象编码" prop="businessObjectCode">
          <el-input v-model="previewForm.businessObjectCode" />
        </el-form-item>
        <el-form-item label="业务字段快照" prop="businessObjectPayloadJson">
          <el-input v-model="previewForm.businessObjectPayloadJson" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <pre v-if="previewSnapshot" class="edhr-label-print-page__snapshot">{{ previewSnapshot }}</pre>
      <template #footer>
        <el-button @click="previewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="previewSubmitting" @click="submitPreview">预览</el-button>
      </template>
    </Dialog>

    <Dialog title="创建打印任务" v-model="printTaskDialogVisible" width="720px">
      <el-alert v-if="printTaskError" :title="printTaskError" type="error" :closable="false" show-icon />
      <el-form ref="printTaskFormRef" :model="printTaskForm" :rules="printTaskRules" label-width="130px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="来源类型" prop="sourceType">
              <el-select v-model="printTaskForm.sourceType" class="!w-100%">
                <el-option label="标签" value="LABEL" />
                <el-option label="流转单" value="TRAVELER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源对象ID" prop="sourceObjectId">
              <el-input-number v-model="printTaskForm.sourceObjectId" :min="1" :controls="false" class="!w-100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源对象编码" prop="sourceObjectCode">
              <el-input v-model="printTaskForm.sourceObjectCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板类型" prop="templateType">
              <el-select v-model="printTaskForm.templateType" class="!w-100%">
                <el-option label="标签模板" value="LABEL_TEMPLATE" />
                <el-option label="流转单模板" value="TRAVELER_TEMPLATE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板ID" prop="templateId">
              <el-input-number v-model="printTaskForm.templateId" :min="1" :controls="false" class="!w-100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="printTaskForm.templateCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="补打">
              <el-switch v-model="printTaskForm.isReprint" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原打印任务ID" prop="originalPrintTaskId">
              <el-input-number
                v-model="printTaskForm.originalPrintTaskId"
                :min="1"
                :controls="false"
                placeholder="请输入原打印任务ID"
                class="!w-100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="补打原因" prop="reprintReason">
              <el-input v-model="printTaskForm.reprintReason" type="textarea" :rows="2" placeholder="请输入补打原因" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="幂等键" prop="idempotencyKey">
              <el-input v-model="printTaskForm.idempotencyKey" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="printTaskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="printTaskSubmitting" @click="submitPrintTask">保存</el-button>
      </template>
    </Dialog>

    <Dialog title="标记打印失败" v-model="failureDialogVisible" width="520px">
      <el-alert v-if="failureError" :title="failureError" type="error" :closable="false" show-icon />
      <el-form ref="failureFormRef" :model="failureForm" :rules="failureRules" label-width="110px">
        <el-form-item label="任务ID">
          <el-input-number v-model="failureForm.id" disabled class="!w-100%" />
        </el-form-item>
        <el-form-item label="失败原因" prop="failureReason">
          <el-input v-model="failureForm.failureReason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="failureDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="failureSubmitting" @click="submitMarkFailed">标记失败</el-button>
      </template>
    </Dialog>

    <Dialog title="确认打印成功" v-model="confirmDialogVisible" width="520px">
      <el-alert v-if="confirmError" :title="confirmError" type="error" :closable="false" show-icon />
      <el-form ref="confirmFormRef" :model="confirmForm" :rules="confirmRules" label-width="130px">
        <el-form-item label="任务ID">
          <el-input-number v-model="confirmForm.id" disabled class="!w-100%" />
        </el-form-item>
        <el-form-item label="确认凭证" prop="confirmationEvidenceHash">
          <el-input v-model="confirmForm.confirmationEvidenceHash" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirmSubmitting" @click="submitConfirmPrintTask">确认成功</el-button>
      </template>
    </Dialog>

    <Dialog title="创建打印策略" v-model="printPolicyDialogVisible" width="760px">
      <el-alert v-if="printPolicyError" :title="printPolicyError" type="error" :closable="false" show-icon />
      <el-form ref="printPolicyFormRef" :model="printPolicyForm" :rules="printPolicyRules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="策略编码" prop="policyCode">
              <el-input v-model="printPolicyForm.policyCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="策略名称" prop="policyName">
              <el-input v-model="printPolicyForm.policyName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务类型" prop="businessType">
              <el-input v-model="printPolicyForm.businessType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板类型" prop="templateType">
              <el-select v-model="printPolicyForm.templateType" class="!w-100%">
                <el-option label="标签模板" value="LABEL_TEMPLATE" />
                <el-option label="流转单模板" value="TRAVELER_TEMPLATE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="首次次数" prop="firstPrintLimit">
              <el-input-number v-model="printPolicyForm.firstPrintLimit" :min="1" :controls="false" class="!w-100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="补打上限" prop="reprintLimit">
              <el-input-number v-model="printPolicyForm.reprintLimit" :min="0" :controls="false" class="!w-100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="原因字典" prop="reasonDictJson">
              <el-input v-model="printPolicyForm.reasonDictJson" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="水印模板" prop="watermarkTemplate">
              <el-input v-model="printPolicyForm.watermarkTemplate" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="作废水印" prop="voidCopyWatermark">
              <el-input v-model="printPolicyForm.voidCopyWatermark" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="printPolicyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="printPolicySubmitting" @click="submitPrintPolicy">保存</el-button>
      </template>
    </Dialog>

    <Dialog title="补打申请" v-model="reprintDialogVisible" width="620px">
      <el-alert v-if="reprintError" :title="reprintError" type="error" :closable="false" show-icon />
      <el-form ref="reprintFormRef" :model="reprintForm" :rules="reprintRules" label-width="120px">
        <el-form-item label="原打印任务ID" prop="originalPrintTaskId">
          <el-input-number v-model="reprintForm.originalPrintTaskId" :min="1" :controls="false" class="!w-100%" />
        </el-form-item>
        <el-form-item label="原因编码" prop="reprintReasonCode">
          <el-input v-model="reprintForm.reprintReasonCode" />
        </el-form-item>
        <el-form-item label="补打原因" prop="reprintReason">
          <el-input v-model="reprintForm.reprintReason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="已用次数">
          <el-input v-model="reprintUsageSummary" disabled />
        </el-form-item>
        <el-form-item label="幂等键" prop="idempotencyKey">
          <el-input v-model="reprintForm.idempotencyKey" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reprintDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reprintSubmitting" @click="submitReprint">提交</el-button>
      </template>
    </Dialog>

    <Dialog title="作废历史副本" v-model="historyCopyDialogVisible" width="640px">
      <el-alert
        title="仅历史追溯，不可用于生产流转。"
        type="warning"
        :closable="false"
        show-icon
        class="mb-12px"
      />
      <el-alert v-if="historyCopyError" :title="historyCopyError" type="error" :closable="false" show-icon />
      <el-form ref="historyCopyFormRef" :model="historyCopyForm" :rules="historyCopyRules" label-width="130px">
        <el-form-item label="来源打印任务ID" prop="sourcePrintTaskId">
          <el-input-number v-model="historyCopyForm.sourcePrintTaskId" :min="1" :controls="false" class="!w-100%" />
        </el-form-item>
        <el-form-item label="来源对象类型" prop="sourceObjectType">
          <el-input v-model="historyCopyForm.sourceObjectType" />
        </el-form-item>
        <el-form-item label="来源对象编码" prop="sourceObjectCode">
          <el-input v-model="historyCopyForm.sourceObjectCode" />
        </el-form-item>
        <el-form-item label="副本原因" prop="copyReason">
          <el-input v-model="historyCopyForm.copyReason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="幂等键" prop="idempotencyKey">
          <el-input v-model="historyCopyForm.idempotencyKey" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="historyCopyDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="historyCopySubmitting" @click="submitHistoryCopy">生成副本</el-button>
      </template>
    </Dialog>

    <Dialog title="导出历史" v-model="exportDialogVisible" width="680px">
      <el-alert v-if="exportError" :title="exportError" type="error" :closable="false" show-icon />
      <el-form ref="exportFormRef" :model="exportForm" :rules="exportRules" label-width="120px">
        <el-form-item label="筛选快照" prop="filterSnapshotJson">
          <el-input v-model="exportForm.filterSnapshotJson" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="证据Hash">
          <el-input v-model="exportAuditEvidenceHash" disabled />
        </el-form-item>
        <el-form-item label="幂等键" prop="idempotencyKey">
          <el-input v-model="exportForm.idempotencyKey" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="exportSubmitting" @click="submitExportHistory">记录导出</el-button>
      </template>
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import {
  activateEdhrPrintPolicy,
  activateEdhrLabelTemplate,
  applyReprint,
  confirmPrintTask,
  createEdhrLabelTemplate,
  createEdhrPrintPolicy,
  createPrintTask,
  createVoidHistoryCopy,
  exportPrintHistory,
  getEdhrLabelPage,
  getEdhrLabelTemplatePage,
  getEdhrPrintPolicyPage,
  getEdhrPrintTaskPage,
  markPrintTaskFailed,
  previewLabel,
  type EdhrLabelInstancePageReqVO,
  type EdhrLabelInstanceRespVO,
  type EdhrLabelPreviewReqVO,
  type EdhrLabelTemplateCreateReqVO,
  type EdhrLabelTemplatePageReqVO,
  type EdhrLabelTemplateRespVO,
  type EdhrPrintHistoryCopyReqVO,
  type EdhrPrintHistoryExportReqVO,
  type EdhrPrintPolicyCreateReqVO,
  type EdhrPrintPolicyPageReqVO,
  type EdhrPrintPolicyRespVO,
  type EdhrPrintTaskCreateReqVO,
  type EdhrPrintTaskRespVO,
  type EdhrReprintApplyReqVO
} from '@/api/mes/pro/edhr/labelPrint'
import { edhrDateTimeFormatter } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProFeedbackEdhrLabelPrint' })

const message = useMessage()

const activeTab = ref<'template' | 'label' | 'printTask' | 'printPolicy'>('template')
const loadError = ref('')
const labelError = ref('')
const printTaskError = ref('')
const printPolicyError = ref('')
const failureError = ref('')
const confirmError = ref('')
const reprintError = ref('')
const historyCopyError = ref('')
const exportError = ref('')

const templateLoading = ref(false)
const labelTemplateList = ref<EdhrLabelTemplateRespVO[]>([])
const templateTotal = ref(0)
const templateQuery = reactive<EdhrLabelTemplatePageReqVO>({
  pageNo: 1,
  pageSize: 10
})

const labelLoading = ref(false)
const labelInstanceList = ref<EdhrLabelInstanceRespVO[]>([])
const labelTotal = ref(0)
const labelQuery = reactive<EdhrLabelInstancePageReqVO>({
  pageNo: 1,
  pageSize: 10
})

const printTaskLoading = ref(false)
const printTaskList = ref<EdhrPrintTaskRespVO[]>([])
const printTaskTotal = ref(0)
const printTaskQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  taskCode: undefined as string | undefined,
  sourceType: undefined as string | undefined,
  sourceObjectId: undefined as number | undefined,
  sourceObjectCode: undefined as string | undefined,
  templateType: undefined as string | undefined,
  status: undefined as string | undefined,
  printConfirmStatus: undefined as string | undefined,
  isReprint: undefined as boolean | undefined
})

const printPolicyLoading = ref(false)
const printPolicyList = ref<EdhrPrintPolicyRespVO[]>([])
const printPolicyTotal = ref(0)
const printPolicyQuery = reactive<EdhrPrintPolicyPageReqVO>({
  pageNo: 1,
  pageSize: 10
})

const templateDialogVisible = ref(false)
const templateSubmitting = ref(false)
const templateFormRef = ref<FormInstance>()
const templateForm = reactive<EdhrLabelTemplateCreateReqVO>({
  templateCode: '',
  templateName: '',
  templateVersion: '',
  businessObjectType: '',
  fieldModelJson: '',
  layoutJson: '',
  parserVersion: '',
  watermarkTemplate: ''
})
const templateRules: FormRules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateVersion: [{ required: true, message: '请输入模板版本', trigger: 'blur' }],
  businessObjectType: [{ required: true, message: '请输入业务对象', trigger: 'blur' }],
  fieldModelJson: [{ required: true, message: '请输入字段模型', trigger: 'blur' }],
  layoutJson: [{ required: true, message: '请输入布局', trigger: 'blur' }],
  parserVersion: [{ required: true, message: '请输入解析版本', trigger: 'blur' }]
}

const previewDialogVisible = ref(false)
const previewSubmitting = ref(false)
const previewFormRef = ref<FormInstance>()
const previewSnapshot = ref('')
const previewForm = reactive<EdhrLabelPreviewReqVO>({
  templateId: 0,
  businessType: '',
  businessObjectId: 0,
  businessObjectCode: '',
  businessObjectPayloadJson: ''
})
const previewRules: FormRules = {
  templateId: [{ required: true, message: '请输入模板ID', trigger: 'blur' }],
  businessType: [{ required: true, message: '请输入业务对象', trigger: 'blur' }],
  businessObjectId: [{ required: true, message: '请输入业务对象ID', trigger: 'blur' }],
  businessObjectCode: [{ required: true, message: '请输入业务对象编码', trigger: 'blur' }],
  businessObjectPayloadJson: [{ required: true, message: '请输入业务字段快照', trigger: 'blur' }]
}

const printTaskDialogVisible = ref(false)
const printTaskSubmitting = ref(false)
const printTaskFormRef = ref<FormInstance>()
const printTaskForm = reactive<EdhrPrintTaskCreateReqVO>({
  sourceType: 'LABEL',
  sourceObjectId: 0,
  sourceObjectCode: '',
  templateType: 'LABEL_TEMPLATE',
  templateId: 0,
  templateCode: '',
  isReprint: false,
  idempotencyKey: ''
})

const validateReprintReason = (_rule: unknown, value: string | undefined, callback: (error?: Error) => void) => {
  if (printTaskForm.isReprint && !String(value || '').trim()) {
    callback(new Error('补打时必须填写补打原因'))
    return
  }
  callback()
}

const validateOriginalPrintTask = (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
  if (printTaskForm.isReprint && !value) {
    callback(new Error('补打时必须选择原打印任务'))
    return
  }
  callback()
}

const printTaskRules: FormRules = {
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  sourceObjectId: [{ required: true, message: '请输入来源对象ID', trigger: 'blur' }],
  sourceObjectCode: [{ required: true, message: '请输入来源对象编码', trigger: 'blur' }],
  templateType: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
  templateId: [{ required: true, message: '请输入模板ID', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  originalPrintTaskId: [{ validator: validateOriginalPrintTask, trigger: 'blur' }],
  reprintReason: [{ validator: validateReprintReason, trigger: 'blur' }],
  idempotencyKey: [{ required: true, message: '请输入幂等键', trigger: 'blur' }]
}

const failureDialogVisible = ref(false)
const failureSubmitting = ref(false)
const failureFormRef = ref<FormInstance>()
const failureForm = reactive({
  id: 0,
  failureReason: ''
})
const failureRules: FormRules = {
  failureReason: [{ required: true, message: '请输入失败原因', trigger: 'blur' }]
}

const confirmDialogVisible = ref(false)
const confirmSubmitting = ref(false)
const confirmFormRef = ref<FormInstance>()
const confirmForm = reactive({
  id: 0,
  confirmationEvidenceHash: ''
})
const confirmRules: FormRules = {
  confirmationEvidenceHash: [{ required: true, message: '请输入确认凭证', trigger: 'blur' }]
}

const printPolicyDialogVisible = ref(false)
const printPolicySubmitting = ref(false)
const printPolicyFormRef = ref<FormInstance>()
const printPolicyForm = reactive<EdhrPrintPolicyCreateReqVO>({
  policyCode: '',
  policyName: '',
  businessType: '',
  templateType: 'LABEL_TEMPLATE',
  firstPrintLimit: 1,
  reprintLimit: 0,
  reasonDictJson: '[{"reasonCode":"QUALITY","reasonName":"质量复核"}]',
  watermarkTemplate: '受控打印 ${taskCode}',
  voidCopyWatermark: '作废历史副本，仅历史追溯，不可用于生产流转'
})
const printPolicyRules: FormRules = {
  policyCode: [{ required: true, message: '请输入策略编码', trigger: 'blur' }],
  policyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  businessType: [{ required: true, message: '请输入业务类型', trigger: 'blur' }],
  templateType: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
  firstPrintLimit: [{ required: true, message: '请输入首次次数', trigger: 'blur' }],
  reprintLimit: [{ required: true, message: '请输入补打上限', trigger: 'blur' }],
  reasonDictJson: [{ required: true, message: '请输入原因字典', trigger: 'blur' }],
  watermarkTemplate: [{ required: true, message: '请输入水印模板', trigger: 'blur' }],
  voidCopyWatermark: [{ required: true, message: '请输入作废水印', trigger: 'blur' }]
}

const reprintDialogVisible = ref(false)
const reprintSubmitting = ref(false)
const reprintFormRef = ref<FormInstance>()
const reprintUsageSummary = ref('')
const reprintForm = reactive<EdhrReprintApplyReqVO>({
  originalPrintTaskId: 0,
  reprintReasonCode: '',
  reprintReason: '',
  idempotencyKey: ''
})
const validateReprintPolicy = (_rule: unknown, value: string | undefined, callback: (error?: Error) => void) => {
  if (!String(value || '').trim()) {
    callback(new Error('请输入受控原因编码'))
    return
  }
  callback()
}
const reprintRules: FormRules = {
  originalPrintTaskId: [{ required: true, message: '请输入原打印任务ID', trigger: 'blur' }],
  reprintReasonCode: [{ validator: validateReprintPolicy, trigger: 'blur' }],
  reprintReason: [{ required: true, message: '请输入补打原因', trigger: 'blur' }],
  idempotencyKey: [{ required: true, message: '请输入幂等键', trigger: 'blur' }]
}

const historyCopyDialogVisible = ref(false)
const historyCopySubmitting = ref(false)
const historyCopyFormRef = ref<FormInstance>()
const historyCopyForm = reactive<EdhrPrintHistoryCopyReqVO>({
  sourcePrintTaskId: 0,
  sourceObjectType: '',
  sourceObjectCode: '',
  copyReason: '',
  idempotencyKey: ''
})
const historyCopyRules: FormRules = {
  sourcePrintTaskId: [{ required: true, message: '请输入来源打印任务ID', trigger: 'blur' }],
  sourceObjectType: [{ required: true, message: '请输入来源对象类型', trigger: 'blur' }],
  sourceObjectCode: [{ required: true, message: '请输入来源对象编码', trigger: 'blur' }],
  copyReason: [{ required: true, message: '请输入副本原因', trigger: 'blur' }],
  idempotencyKey: [{ required: true, message: '请输入幂等键', trigger: 'blur' }]
}

const exportDialogVisible = ref(false)
const exportSubmitting = ref(false)
const exportFormRef = ref<FormInstance>()
const exportAuditEvidenceHash = ref('')
const exportForm = reactive<EdhrPrintHistoryExportReqVO>({
  filterSnapshotJson: '',
  idempotencyKey: ''
})
const exportRules: FormRules = {
  filterSnapshotJson: [{ required: true, message: '请输入筛选快照', trigger: 'blur' }],
  idempotencyKey: [{ required: true, message: '请输入幂等键', trigger: 'blur' }]
}

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error && typeof error === 'object') {
    const maybeError = error as { msg?: string; message?: string; data?: { msg?: string; message?: string } }
    return maybeError.data?.msg || maybeError.data?.message || maybeError.msg || maybeError.message || defaultMessage
  }
  return defaultMessage
}

const loadLabelTemplateList = async () => {
  templateLoading.value = true
  loadError.value = ''
  try {
    const res = await getEdhrLabelTemplatePage(templateQuery)
    labelTemplateList.value = res.list || []
    templateTotal.value = res.total || 0
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '标签模板列表加载失败。')
    message.error(resolveErrorMessage(error, '标签模板列表加载失败。'))
  } finally {
    templateLoading.value = false
  }
}

const loadLabelInstanceList = async () => {
  labelLoading.value = true
  loadError.value = ''
  try {
    const res = await getEdhrLabelPage(labelQuery)
    labelInstanceList.value = res.list || []
    labelTotal.value = res.total || 0
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '标签实例列表加载失败。')
    message.error(resolveErrorMessage(error, '标签实例列表加载失败。'))
  } finally {
    labelLoading.value = false
  }
}

const loadPrintTaskList = async () => {
  printTaskLoading.value = true
  printTaskError.value = ''
  try {
    const res = await getEdhrPrintTaskPage(printTaskQuery)
    printTaskList.value = res.list || []
    printTaskTotal.value = res.total || 0
  } catch (error) {
    printTaskError.value = resolveErrorMessage(error, '打印任务列表加载失败。')
    message.error(resolveErrorMessage(error, '打印任务列表加载失败。'))
  } finally {
    printTaskLoading.value = false
  }
}

const loadPrintPolicyList = async () => {
  printPolicyLoading.value = true
  printPolicyError.value = ''
  try {
    const res = await getEdhrPrintPolicyPage(printPolicyQuery)
    printPolicyList.value = res.list || []
    printPolicyTotal.value = res.total || 0
  } catch (error) {
    printPolicyError.value = resolveErrorMessage(error, '打印策略列表加载失败。')
    message.error(resolveErrorMessage(error, '打印策略列表加载失败。'))
  } finally {
    printPolicyLoading.value = false
  }
}

const handleTabChange = () => {
  if (activeTab.value === 'template') loadLabelTemplateList()
  if (activeTab.value === 'label') loadLabelInstanceList()
  if (activeTab.value === 'printTask') loadPrintTaskList()
  if (activeTab.value === 'printPolicy') loadPrintPolicyList()
}

const handleTemplateQuery = () => {
  templateQuery.pageNo = 1
  loadLabelTemplateList()
}

const resetTemplateQuery = () => {
  Object.assign(templateQuery, { pageNo: 1, pageSize: 10, templateCode: undefined, templateName: undefined, businessObjectType: undefined, status: undefined })
  loadLabelTemplateList()
}

const handleLabelQuery = () => {
  labelQuery.pageNo = 1
  loadLabelInstanceList()
}

const resetLabelQuery = () => {
  Object.assign(labelQuery, { pageNo: 1, pageSize: 10, labelCode: undefined, businessObjectCode: undefined, printStatus: undefined })
  loadLabelInstanceList()
}

const handlePrintTaskQuery = () => {
  printTaskQuery.pageNo = 1
  loadPrintTaskList()
}

const resetPrintTaskQuery = () => {
  Object.assign(printTaskQuery, {
    pageNo: 1,
    pageSize: 10,
    taskCode: undefined,
    sourceObjectCode: undefined,
    status: undefined,
    isReprint: undefined
  })
  loadPrintTaskList()
}

const handlePrintPolicyQuery = () => {
  printPolicyQuery.pageNo = 1
  loadPrintPolicyList()
}

const resetPrintPolicyQuery = () => {
  Object.assign(printPolicyQuery, {
    pageNo: 1,
    pageSize: 10,
    policyCode: undefined,
    policyName: undefined,
    businessType: undefined,
    templateType: undefined,
    status: undefined
  })
  loadPrintPolicyList()
}

const openTemplateDialog = () => {
  labelError.value = ''
  Object.assign(templateForm, {
    templateCode: '',
    templateName: '',
    templateVersion: '',
    businessObjectType: '',
    fieldModelJson: '',
    layoutJson: '',
    parserVersion: '',
    watermarkTemplate: ''
  })
  templateDialogVisible.value = true
}

const submitTemplate = async () => {
  const valid = await templateFormRef.value?.validate()
  if (!valid) return
  templateSubmitting.value = true
  labelError.value = ''
  try {
    await createEdhrLabelTemplate(templateForm)
    message.success('标签模板已创建。')
    templateDialogVisible.value = false
    loadLabelTemplateList()
  } catch (error) {
    labelError.value = resolveErrorMessage(error, '标签模板创建失败。')
    message.error(resolveErrorMessage(error, '标签模板创建失败。'))
  } finally {
    templateSubmitting.value = false
  }
}

const activateTemplate = async (row: EdhrLabelTemplateRespVO) => {
  if (!row.id) return
  labelError.value = ''
  try {
    await activateEdhrLabelTemplate(row.id)
    message.success('标签模板已启用。')
    loadLabelTemplateList()
  } catch (error) {
    labelError.value = resolveErrorMessage(error, '标签模板启用失败。')
    message.error(resolveErrorMessage(error, '标签模板启用失败。'))
  }
}

const openPreviewDialog = (row: EdhrLabelTemplateRespVO) => {
  labelError.value = ''
  previewSnapshot.value = ''
  Object.assign(previewForm, {
    templateId: row.id || 0,
    businessType: row.businessObjectType || '',
    businessObjectId: 0,
    businessObjectCode: '',
    businessObjectPayloadJson: ''
  })
  previewDialogVisible.value = true
}

const submitPreview = async () => {
  const valid = await previewFormRef.value?.validate()
  if (!valid) return
  previewSubmitting.value = true
  labelError.value = ''
  try {
    const res = await previewLabel(previewForm)
    previewSnapshot.value = res.renderSnapshotJson || ''
  } catch (error) {
    labelError.value = resolveErrorMessage(error, '标签预览失败。')
    message.error(resolveErrorMessage(error, '标签预览失败。'))
  } finally {
    previewSubmitting.value = false
  }
}

const openPrintTaskDialog = () => {
  printTaskError.value = ''
  Object.assign(printTaskForm, {
    sourceType: 'LABEL',
    sourceObjectId: 0,
    sourceObjectCode: '',
    templateType: 'LABEL_TEMPLATE',
    templateId: 0,
    templateCode: '',
    labelInstanceId: undefined,
    travelerId: undefined,
    isReprint: false,
    originalPrintTaskId: undefined,
    reprintReason: '',
    watermarkText: '',
    idempotencyKey: `PRINT-${Date.now()}`
  })
  printTaskDialogVisible.value = true
}

const submitPrintTask = async () => {
  const valid = await printTaskFormRef.value?.validate()
  if (!valid) return
  printTaskSubmitting.value = true
  printTaskError.value = ''
  try {
    await createPrintTask(printTaskForm)
    message.success('打印任务已创建，等待打印确认。')
    printTaskDialogVisible.value = false
    loadPrintTaskList()
  } catch (error) {
    printTaskError.value = resolveErrorMessage(error, '打印任务创建失败。')
    message.error(resolveErrorMessage(error, '打印任务创建失败。'))
  } finally {
    printTaskSubmitting.value = false
  }
}

const openMarkFailedDialog = (row: EdhrPrintTaskRespVO) => {
  failureError.value = ''
  Object.assign(failureForm, { id: row.id, failureReason: row.failureReason || '' })
  failureDialogVisible.value = true
}

const submitMarkFailed = async () => {
  const valid = await failureFormRef.value?.validate()
  if (!valid) return
  failureSubmitting.value = true
  failureError.value = ''
  try {
    await markPrintTaskFailed(failureForm)
    message.success('打印任务已标记失败。')
    failureDialogVisible.value = false
    loadPrintTaskList()
  } catch (error) {
    failureError.value = resolveErrorMessage(error, '标记打印失败失败。')
    message.error(resolveErrorMessage(error, '标记打印失败失败。'))
  } finally {
    failureSubmitting.value = false
  }
}

const openConfirmDialog = (row: EdhrPrintTaskRespVO) => {
  confirmError.value = ''
  Object.assign(confirmForm, { id: row.id, confirmationEvidenceHash: row.confirmationEvidenceHash || '' })
  confirmDialogVisible.value = true
}

const submitConfirmPrintTask = async () => {
  const valid = await confirmFormRef.value?.validate()
  if (!valid) return
  confirmSubmitting.value = true
  confirmError.value = ''
  try {
    await confirmPrintTask(confirmForm)
    message.success('打印任务已确认成功。')
    confirmDialogVisible.value = false
    loadPrintTaskList()
  } catch (error) {
    confirmError.value = resolveErrorMessage(error, '确认打印成功失败。')
    message.error(resolveErrorMessage(error, '确认打印成功失败。'))
  } finally {
    confirmSubmitting.value = false
  }
}

const openPrintPolicyDialog = () => {
  printPolicyError.value = ''
  Object.assign(printPolicyForm, {
    policyCode: '',
    policyName: '',
    businessType: '',
    templateType: 'LABEL_TEMPLATE',
    firstPrintLimit: 1,
    reprintLimit: 0,
    reasonDictJson: '[{"reasonCode":"QUALITY","reasonName":"质量复核"}]',
    watermarkTemplate: '受控打印 ${taskCode}',
    voidCopyWatermark: '作废历史副本，仅历史追溯，不可用于生产流转'
  })
  printPolicyDialogVisible.value = true
}

const submitPrintPolicy = async () => {
  const valid = await printPolicyFormRef.value?.validate()
  if (!valid) return
  printPolicySubmitting.value = true
  printPolicyError.value = ''
  try {
    await createEdhrPrintPolicy(printPolicyForm)
    message.success('打印策略已创建。')
    printPolicyDialogVisible.value = false
    loadPrintPolicyList()
  } catch (error) {
    printPolicyError.value = resolveErrorMessage(error, '打印策略创建失败。')
    message.error(resolveErrorMessage(error, '打印策略创建失败。'))
  } finally {
    printPolicySubmitting.value = false
  }
}

const activatePrintPolicy = async (row: EdhrPrintPolicyRespVO) => {
  if (!row.id) return
  printPolicyError.value = ''
  try {
    await activateEdhrPrintPolicy(row.id)
    message.success('打印策略已启用。')
    loadPrintPolicyList()
  } catch (error) {
    printPolicyError.value = resolveErrorMessage(error, '打印策略启用失败。')
    message.error(resolveErrorMessage(error, '打印策略启用失败。'))
  }
}

const openReprintDialog = (row: EdhrPrintTaskRespVO) => {
  reprintError.value = ''
  reprintUsageSummary.value = '提交后由后端校验并返回已用次数'
  Object.assign(reprintForm, {
    originalPrintTaskId: row.id || 0,
    reprintReasonCode: '',
    reprintReason: row.reprintReason || '',
    idempotencyKey: `REPRINT-${row.id}-${Date.now()}`
  })
  reprintDialogVisible.value = true
}

const submitReprint = async () => {
  const valid = await reprintFormRef.value?.validate()
  if (!valid) return
  reprintSubmitting.value = true
  reprintError.value = ''
  try {
    const res = await applyReprint(reprintForm)
    reprintUsageSummary.value = `${res.usedReprintCount ?? '--'} / ${res.reprintLimit ?? '--'}`
    message.success('补打申请已提交。')
    reprintDialogVisible.value = false
    loadPrintTaskList()
  } catch (error) {
    reprintError.value = resolveErrorMessage(error, '补打申请失败。')
    message.error(resolveErrorMessage(error, '补打申请失败。'))
  } finally {
    reprintSubmitting.value = false
  }
}

const openHistoryCopyDialog = (row: EdhrPrintTaskRespVO) => {
  historyCopyError.value = ''
  Object.assign(historyCopyForm, {
    sourcePrintTaskId: row.id || 0,
    sourceObjectType: row.sourceType || '',
    sourceObjectCode: row.sourceObjectCode || '',
    copyReason: row.failureReason || '历史追溯副本',
    idempotencyKey: `VOID-COPY-${row.id}-${Date.now()}`
  })
  historyCopyDialogVisible.value = true
}

const submitHistoryCopy = async () => {
  const valid = await historyCopyFormRef.value?.validate()
  if (!valid) return
  historyCopySubmitting.value = true
  historyCopyError.value = ''
  try {
    await createVoidHistoryCopy(historyCopyForm)
    message.success('历史副本已生成。')
    historyCopyDialogVisible.value = false
    loadPrintTaskList()
  } catch (error) {
    historyCopyError.value = resolveErrorMessage(error, '作废历史副本生成失败。')
    message.error(resolveErrorMessage(error, '作废历史副本生成失败。'))
  } finally {
    historyCopySubmitting.value = false
  }
}

const openExportDialog = () => {
  exportError.value = ''
  exportAuditEvidenceHash.value = ''
  Object.assign(exportForm, {
    filterSnapshotJson: JSON.stringify({ ...printTaskQuery }),
    idempotencyKey: `EXPORT-${Date.now()}`
  })
  exportDialogVisible.value = true
}

const submitExportHistory = async () => {
  const valid = await exportFormRef.value?.validate()
  if (!valid) return
  exportSubmitting.value = true
  exportError.value = ''
  try {
    const res = await exportPrintHistory(exportForm)
    exportAuditEvidenceHash.value = res.evidenceHash || ''
    message.success('导出历史已记录。')
    exportDialogVisible.value = false
  } catch (error) {
    exportError.value = resolveErrorMessage(error, '导出历史记录失败。')
    message.error(resolveErrorMessage(error, '导出历史记录失败。'))
  } finally {
    exportSubmitting.value = false
  }
}

const resolveTemplateStatusLabel = (status?: string) =>
  ({ DRAFT: '草稿', ACTIVE: '已启用', DISABLED: '已停用', VOID: '已作废' })[status || ''] || status || '--'

const resolvePrintPolicyStatusLabel = (status?: string) =>
  ({ DRAFT: '草稿', ACTIVE: '已启用', DISABLED: '已停用' })[status || ''] || status || '--'

const resolvePrintStatusLabel = (status?: string) =>
  ({
    NOT_PRINTED: '未打印',
    WAITING: '待打印',
    PRINTING: '打印中',
    PENDING_CONFIRM: '待确认',
    SUCCESS_CONFIRMED: '确认成功',
    FAILED: '打印失败',
    VOID_RESTRICTED: '作废受限'
  })[status || ''] || status || '--'

const resolvePrintStatusType = (status?: string) => {
  if (status === 'SUCCESS_CONFIRMED') return 'success'
  if (status === 'FAILED' || status === 'VOID_RESTRICTED') return 'danger'
  if (status === 'PENDING_CONFIRM' || status === 'PRINTING') return 'warning'
  return 'info'
}

onMounted(() => {
  loadLabelTemplateList()
})
</script>

<style scoped>
.edhr-label-print-page {
  color: #172033;
}

.edhr-label-print-page__tabs {
  margin-bottom: 12px;
}

.edhr-label-print-page__section {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.edhr-label-print-page__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  padding: 12px 14px 4px;
  border-bottom: 1px solid #dbe3ef;
  background: #fff;
}

.edhr-label-print-page__section :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
  color: #263247;
}

.edhr-label-print-page__section :deep(.el-table__row) {
  height: 52px;
}

.edhr-label-print-page__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-label-print-page__muted {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.edhr-label-print-page__snapshot {
  max-height: 220px;
  margin: 12px 0 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  color: #263247;
  font-size: 12px;
  line-height: 1.55;
}
</style>
