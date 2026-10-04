<template>
  <ContentWrap class="scheme-d-basic-data-page scheme-d-basic-data-page--dcc-product-catalog">
    <div class="mb-16px flex items-center gap-8px">
      <span class="text-18px font-600 text-[var(--el-text-color-primary)]">基础数据 / DCC产品目录</span>
    </div>
    <UnifiedListTemplate
      class="dcc-product-catalog-list-template"
      table-key="dcc.productCatalog.main"
      :query-model="queryParams"
      label-width="76px"
      :filter-definitions="productCatalogQuickFilterDefinitions"
      :quick-filter-state="productCatalogQuickFilter.state"
      :selected-filter-definition="productCatalogQuickFilter.selectedDefinition.value"
      :operator-options="productCatalogQuickFilter.operatorOptions.value"
      :columns="productCatalogColumns"
      :column-saving="productCatalogColumnSaving"
      :total="total"
      single-line-toolbar
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      v-model:sort-state="productCatalogSortState"
      @update:quick-filter-state="productCatalogQuickFilter.updateState"
      @quick-filter-query="productCatalogQuickFilter.applyQuickFilter"
      @column-change="saveProductCatalogColumnConfig"
      @column-reset="resetProductCatalogColumnConfig"
      @sort-change="handleProductCatalogSortChange"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          plain
          data-testid="dcc-project-product-create-open"
          @click="openProjectProductDialog('create')"
          v-hasPermi="['dcc:project-code:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新建项目代码及产品
        </el-button>
        <el-button data-testid="dcc-project-product-records-open" v-hasPermi="['dcc:project-code:query']" @click="openProjectProductDialog('records')">
          项目申请与审批
        </el-button>
        <ProjectReviewerConfiguration @changed="reviewerConfiguration = $event" />
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <div class="dcc-product-catalog-split-layout">
          <aside class="dcc-product-catalog-tree-panel">
            <div class="dcc-product-catalog-tree-title">产品分类</div>
            <el-tree
              v-loading="loading"
              class="dcc-product-catalog-tree"
              :data="treeList"
              :props="{ label: 'label', children: 'children' }"
              node-key="treeNodeId"
              :current-node-key="selectedProductCatalogTreeNode.treeNodeId"
              default-expand-all
              highlight-current
              @node-click="handleProductCatalogTreeNodeClick"
            >
              <template #default="{ data }">
                <span class="dcc-product-catalog-tree-node">
                  <span class="dcc-product-catalog-tree-node__label">{{ data.label }}</span>
                  <span class="dcc-product-catalog-tree-node__count">{{ data.detailCount }}</span>
                </span>
              </template>
            </el-tree>
          </aside>

          <section class="dcc-product-catalog-detail-panel">
            <div class="dcc-product-catalog-detail-summary">
              <span class="dcc-product-catalog-detail-summary__title">
                {{ selectedProductCatalogTreeNode.label }}
              </span>
              <span class="dcc-product-catalog-detail-summary__count">
                {{ selectedProductCatalogRows.length }} 条产品明细
              </span>
            </div>
            <div class="dcc-product-catalog-table-shell">
              <el-table
                v-loading="loading"
                class="dcc-product-catalog-resizable-table"
                data-user-table-column-explicit
                data-user-table-key="dcc.productCatalog.main"
                :data="selectedProductCatalogRows"
                height="100%"
                scrollbar-always-on
                border
                :allow-drag-last-column="true"
                :stripe="true"
                :show-overflow-tooltip="true"
                @header-dragend="handleProductCatalogHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
              <el-table-column
                v-if="isProductCatalogColumnVisible('categoryLevel1')"
                label="产品类别 I"
                prop="categoryLevel1"
                :width="getProductCatalogColumnWidthString('categoryLevel1')"
                :min-width="getProductCatalogColumnMinWidthString('categoryLevel1', 180)"
                v-bind="sortColumnAttrs('categoryLevel1')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('categoryLevel2')"
                label="产品类别 II"
                prop="categoryLevel2"
                :width="getProductCatalogColumnWidthString('categoryLevel2')"
                :min-width="getProductCatalogColumnMinWidthString('categoryLevel2', 180)"
                v-bind="sortColumnAttrs('categoryLevel2')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('productSequence')"
                label="产品序号"
                prop="productSequence"
                :width="getProductCatalogColumnWidthString('productSequence')"
                :min-width="getProductCatalogColumnMinWidthString('productSequence', 100)"
                v-bind="sortColumnAttrs('productSequence')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('product')"
                label="产品"
                prop="product"
                :width="getProductCatalogColumnWidthString('product')"
                :min-width="getProductCatalogColumnMinWidthString('product', 220)"
                v-bind="sortColumnAttrs('product')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('dataSource')"
                label="数据来源"
                prop="dataSource"
                :width="getProductCatalogColumnWidthString('dataSource')"
                :min-width="getProductCatalogColumnMinWidthString('dataSource', 120)"
                v-bind="sortColumnAttrs('dataSource')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('productCode')"
                label="产品编码"
                prop="productCode"
                :width="getProductCatalogColumnWidthString('productCode')"
                :min-width="getProductCatalogColumnMinWidthString('productCode', 120)"
                v-bind="sortColumnAttrs('productCode')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('projectName')"
                label="项目名称"
                prop="projectName"
                :width="getProductCatalogColumnWidthString('projectName')"
                :min-width="getProductCatalogColumnMinWidthString('projectName', 180)"
                v-bind="sortColumnAttrs('projectName')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('projectCode')"
                label="项目代码"
                prop="projectCode"
                :width="getProductCatalogColumnWidthString('projectCode')"
                :min-width="getProductCatalogColumnMinWidthString('projectCode', 120)"
                v-bind="sortColumnAttrs('projectCode')"
              >
                <template #default="{ row }">
                  <el-button
                    v-if="row.projectCode && row.projectCodeId"
                    link
                    class="scheme-d-row-action scheme-d-row-action--primary"
                    type="primary"
                    data-testid="dcc-product-catalog-project-code-link"
                    @click="openLinkedProjectCode(row)"
                  >
                    {{ row.projectCode }}
                  </el-button>
                  <span v-else>{{ row.projectCode || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isProductCatalogColumnVisible('batchRecordTotalRecognitionJson')"
                label="批记录识别JSON"
                prop="batchRecordTotalRecognitionJson"
                :width="getProductCatalogColumnWidthString('batchRecordTotalRecognitionJson', 140)"
              >
                <template #default="{ row }">
                  <el-button
                    v-if="row.batchRecordTotalRecognitionJson"
                    link
                    class="scheme-d-row-action scheme-d-row-action--primary"
                    type="primary"
                    data-testid="dcc-product-catalog-copy-recognition-json"
                    @click="copyProductCatalogBatchRecordTotalRecognitionJson(row)"
                  >
                    复制JSON
                  </el-button>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isProductCatalogColumnVisible('classification')"
                label="分类"
                prop="classification"
                :width="getProductCatalogColumnWidthString('classification')"
                :min-width="getProductCatalogColumnMinWidthString('classification', 120)"
                v-bind="sortColumnAttrs('classification')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('registrationCertificateName')"
                label="注册证名称"
                prop="registrationCertificateName"
                :width="getProductCatalogColumnWidthString('registrationCertificateName')"
                :min-width="
                  getProductCatalogColumnMinWidthString('registrationCertificateName', 220)
                "
                v-bind="sortColumnAttrs('registrationCertificateName')"
              >
                <template #default="{ row }">
                  <el-button
                    v-if="row.registrationCertificateName && row.registrationCertificateId"
                    link
                    class="scheme-d-row-action scheme-d-row-action--primary"
                    type="primary"
                    data-testid="dcc-product-catalog-registration-certificate-link"
                    @click="openLinkedRegistrationCertificate(row)"
                  >
                    {{ row.registrationCertificateName }}
                  </el-button>
                  <span v-else>{{ row.registrationCertificateName || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isProductCatalogColumnVisible('registrationCertificateNumber')"
                label="注册证号"
                prop="registrationCertificateNumber"
                :width="getProductCatalogColumnWidthString('registrationCertificateNumber')"
                :min-width="
                  getProductCatalogColumnMinWidthString('registrationCertificateNumber', 180)
                "
                v-bind="sortColumnAttrs('registrationCertificateNumber')"
              >
                <template #default="{ row }">
                  <el-button
                    v-if="row.registrationCertificateNumber && row.registrationCertificateId"
                    link
                    class="scheme-d-row-action scheme-d-row-action--primary"
                    type="primary"
                    data-testid="dcc-product-catalog-registration-certificate-number-link"
                    @click="openLinkedRegistrationCertificate(row)"
                  >
                    {{ row.registrationCertificateNumber }}
                  </el-button>
                  <span v-else>{{ row.registrationCertificateNumber || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isProductCatalogColumnVisible('certificateHolder')"
                label="持证人"
                prop="certificateHolder"
                :width="getProductCatalogColumnWidthString('certificateHolder')"
                :min-width="getProductCatalogColumnMinWidthString('certificateHolder', 160)"
                v-bind="sortColumnAttrs('certificateHolder')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('registrationPlace')"
                label="注册地"
                prop="registrationPlace"
                :width="getProductCatalogColumnWidthString('registrationPlace')"
                :min-width="getProductCatalogColumnMinWidthString('registrationPlace', 120)"
                v-bind="sortColumnAttrs('registrationPlace')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('effectiveDate')"
                label="生效日期"
                prop="effectiveDate"
                :width="getProductCatalogColumnWidthString('effectiveDate', 120)"
                v-bind="sortColumnAttrs('effectiveDate')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('expiryDate')"
                label="有效期至"
                prop="expiryDate"
                :width="getProductCatalogColumnWidthString('expiryDate', 120)"
                v-bind="sortColumnAttrs('expiryDate')"
              />
              <el-table-column
                v-if="isProductCatalogColumnVisible('productStatus')"
                label="产品状态"
                prop="productStatus"
                :width="getProductCatalogColumnWidthString('productStatus', 120)"
                v-bind="sortColumnAttrs('productStatus')"
              >
                <template #default="{ row }">{{ formatProductStatus(row.productStatus) }}</template>
              </el-table-column>
              <el-table-column
                v-if="isProductCatalogColumnVisible('registrationInfoLink')"
                label="注册证信息链接"
                prop="registrationInfoLink"
                :width="getProductCatalogColumnWidthString('registrationInfoLink')"
                :min-width="getProductCatalogColumnMinWidthString('registrationInfoLink', 150)"
                v-bind="sortColumnAttrs('registrationInfoLink')"
              >
                <template #default="{ row }">
                  <el-link
                    v-if="row.registrationInfoLink"
                    :href="row.registrationInfoLink"
                    target="_blank"
                    type="primary"
                  >
                    查看链接
                  </el-link>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isProductCatalogColumnVisible('remark')"
                label="备注"
                prop="remark"
                :width="getProductCatalogColumnWidthString('remark')"
                :min-width="getProductCatalogColumnMinWidthString('remark', 220)"
                v-bind="sortColumnAttrs('remark')"
              >
                <template #default="{ row }">{{ row.remark || '-' }}</template>
              </el-table-column>
              <el-table-column
                v-if="isProductCatalogColumnVisible('actions')"
                label="操作"
                prop="actions"
                fixed="right"
                :width="getProductCatalogActionColumnWidthString()"
              >
                <template #default="{ row }">
                  <el-button
                    link
                    class="scheme-d-row-action scheme-d-row-action--primary"
                    type="primary"
                    data-testid="dcc-product-catalog-bind-registration"
                    @click="openBinding(row)"
                    v-hasPermi="['dcc:project-code:update']"
                  >
                    绑定
                  </el-button>
                  <el-button
                    link
                    class="scheme-d-row-action scheme-d-row-action--primary"
                    type="primary"
                    @click="openForm('update', row)"
                    v-hasPermi="['dcc:project-code:update']"
                  >
                    编辑
                  </el-button>
                  <el-button
                    link
                    class="scheme-d-row-action scheme-d-row-action--danger"
                    type="danger"
                    @click="handleDelete(row)"
                    v-hasPermi="['dcc:project-code:delete']"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
              </el-table>
            </div>
          </section>
        </div>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <Dialog
    v-model="projectProductDialogVisible"
    class="scheme-d-form-control"
    :title="projectProductMode === 'records' ? '项目申请与审批' : projectProductResubmitRequestId ? '修改驳回申请后重提' : '新建项目代码及产品'"
    width="980px"
  >
    <el-alert v-if="projectProductError" :title="projectProductError" type="error" :closable="false" class="mb-12px" />
    <el-form
      v-if="projectProductMode === 'create'"
      ref="projectProductFormRef"
      v-loading="projectProductLoading"
      :model="projectProductForm"
      :rules="projectProductRules"
      label-width="96px"
    >
      <el-row :gutter="18">
        <el-col :span="12">
          <el-form-item label="项目名称" prop="projectName">
            <el-input v-model="projectProductForm.projectName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目代码" prop="projectCode">
            <el-input v-model="projectProductForm.projectCode" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目负责人" prop="projectLeaderUserId">
            <el-select v-model="projectProductForm.projectLeaderUserId" filterable placeholder="选择系统账号">
              <el-option v-for="user in projectLeaderUsers" :key="user.id" :value="user.id" :label="user.nickname + '（' + user.username + '）'" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="产品编码" prop="productCode">
            <el-input v-model="projectProductForm.productCode" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="产品名称" prop="productName">
            <el-input v-model="projectProductForm.productName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分类" prop="classification">
            <el-select v-model="projectProductForm.classification" class="!w-100%">
              <el-option label="一类" value="一类" />
              <el-option label="二类" value="二类" />
              <el-option label="三类" value="三类" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="目录模板" prop="folderTemplateId">
            <el-select v-model="projectProductForm.folderTemplateId" placeholder="选择启用模板">
              <el-option v-for="item in folderTemplates.filter(row => row.active)" :key="item.id" :value="item.id" :label="item.name" />
            </el-select>
            <el-button v-hasPermi="['dcc:project-code:update']" link type="primary" @click="folderLibraryRef?.open()">文件夹模板库</el-button>
          </el-form-item>
          <el-tree v-if="selectedFolderPreview.length" :data="selectedFolderPreview" :props="{ label: 'name' }" default-expand-all class="mb-12px" />
          <el-alert v-if="projectProductRejectReason" :title="'原申请驳回原因：' + projectProductRejectReason" type="warning" :closable="false" class="mb-12px" />
          <ProjectAttributesFields v-model="projectProductForm.defaultAttributes" />
          <el-form-item label="本次审核人">
            <span v-if="reviewerConfiguration?.configured">{{ reviewerConfiguration.reviewerNickname }}（{{ reviewerConfiguration.reviewerUsername }}）{{ reviewerConfiguration.enabled ? '' : '，账号不可用' }}</span>
            <span v-else>尚未配置，请先由文控配置审核人员</span>
          </el-form-item>
          <el-form-item v-if="!projectProductResubmitRequestId" label="新建申请原因" prop="creationReason" required>
            <el-input v-model="projectProductForm.creationReason" type="textarea" maxlength="500" placeholder="请填写本次新建项目及产品的原因" />
          </el-form-item>
          <el-form-item v-if="projectProductResubmitRequestId" label="重提说明" prop="resubmissionReason" required>
            <el-input v-model="projectProductForm.resubmissionReason" type="textarea" maxlength="500" placeholder="请说明已按驳回意见修改的内容" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="projectProductForm.remark" type="textarea" :rows="2" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <el-divider />
    <div class="mb-8px flex items-center justify-between">
      <span class="font-600">申请与审批记录</span>
      <el-button link type="primary" :loading="projectProductRequestsLoading" @click="loadProjectProductRequests">
        刷新
      </el-button>
    </div>
    <el-table
      v-loading="projectProductRequestsLoading"
      :data="projectProductRequests"
      max-height="260"
      border
      size="small"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目负责人">{{ row.projectLeader }}（账号 {{ row.projectLeaderUserId || '历史未记录' }}）</el-descriptions-item>
            <el-descriptions-item label="目录模板ID">{{ row.folderTemplateId || '历史未记录' }}</el-descriptions-item>
            <el-descriptions-item label="驳回原因">{{ row.rejectReason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交时审核人">{{ row.configuredReviewerNickname || '历史未记录' }}<span v-if="row.configuredReviewerUsername">（{{ row.configuredReviewerUsername }}）</span></el-descriptions-item>
            <el-descriptions-item label="新建原因">{{ row.creationReason || '历史未记录' }}</el-descriptions-item>
            <el-descriptions-item label="本次写入原因">{{ row.writeReason || '尚未写入' }}</el-descriptions-item>
            <el-descriptions-item label="申请链">{{ row.previousRequestId ? '由申请 ' + row.previousRequestId + ' 重提' : '首次申请' }}{{ row.resubmittedRequestId ? '；已重提为 ' + row.resubmittedRequestId : '' }}</el-descriptions-item>
          </el-descriptions>
          <el-alert v-if="requestAttributeResult(row).error" :title="requestAttributeResult(row).error" type="error" :closable="false" />
          <el-form v-else label-width="140px" class="mt-12px">
            <ProjectAttributesFields :model-value="requestAttributeResult(row).attributes || emptyAttributes()" readonly :historical-missing="!row.defaultAttributesJson" />
          </el-form>
          <el-tree v-if="row.folderTemplateSnapshotJson" :data="requestFolderPreview(row)" :props="{ label: 'name' }" default-expand-all />
        </template>
      </el-table-column>
      <el-table-column prop="projectCode" label="项目代码" min-width="120" />
      <el-table-column prop="productCode" label="产品编码" min-width="120" />
      <el-table-column prop="productName" label="产品名称" min-width="150" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" fixed="right" width="180">
        <template #default="{ row }">
          <el-button v-if="canResubmitProjectProductRequest(row, userStore.getUser.id)" v-hasPermi="['dcc:project-code:create']" link type="primary" @click="editRejectedProjectProduct(row)">
            修改后重提
          </el-button>
          <el-button
            v-if="canReviewProjectProduct(row, userStore.getUser.id)"
            v-hasPermi="['dcc:project-code:update']"
            link
            type="primary"
            @click="handleProjectProductAction(row, 'review', true)"
          >
            审核通过
          </el-button>
          <el-button
            v-if="canReviewProjectProduct(row, userStore.getUser.id)"
            v-hasPermi="['dcc:project-code:update']"
            link
            type="danger"
            @click="handleProjectProductAction(row, 'review', false)"
          >
            审核驳回
          </el-button>
          <el-button
            v-if="row.status === 'PENDING_APPROVAL'"
            link
            type="primary"
            @click="handleProjectProductAction(row, 'approve', true)"
          >
            批准通过
          </el-button>
          <el-button
            v-if="row.status === 'PENDING_APPROVAL'"
            link
            type="danger"
            @click="handleProjectProductAction(row, 'approve', false)"
          >
            批准驳回
          </el-button>
          <el-button
            v-if="row.status === 'WRITE_FAILED'"
            link
            type="warning"
            @click="handleProjectProductRetry(row)"
          >
            重试写入
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button v-if="projectProductMode === 'create'" type="primary" :loading="projectProductLoading" @click="submitProjectProductRequest">
          {{ projectProductResubmitRequestId ? '确认重提信息' : '确认申请信息' }}
        </el-button>
        <el-button :disabled="projectProductLoading" @click="projectProductDialogVisible = false">
          取消
        </el-button>
      </div>
    </template>
  </Dialog>

  <FolderTemplateLibraryEditor ref="folderLibraryRef" @saved="reloadFolderTemplates" />
  <Dialog v-model="formVisible" class="scheme-d-form-control" title="产品目录维护" width="820px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="126px"
    >
      <el-form-item label="数据来源" prop="dataSource">
        <el-select
          v-model="formData.dataSource"
          class="!w-100%"
          :disabled="formType === 'update'"
          placeholder="请选择数据来源"
        >
          <el-option
            v-for="item in dataSourceOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="产品类别 I" prop="categoryLevel1">
        <el-input v-model="formData.categoryLevel1" placeholder="请输入产品类别 I" />
      </el-form-item>
      <el-form-item label="产品类别 II" prop="categoryLevel2">
        <el-input v-model="formData.categoryLevel2" placeholder="请输入产品类别 II" />
      </el-form-item>
      <el-form-item label="产品序号" prop="productSequence">
        <el-input v-model="formData.productSequence" placeholder="请输入产品序号" />
      </el-form-item>
      <el-form-item label="产品" prop="product">
        <el-input v-model="formData.product" placeholder="请输入产品" />
      </el-form-item>
      <el-form-item label="产品编码" prop="productCode">
        <el-input v-model="formData.productCode" placeholder="请输入产品编码" />
      </el-form-item>
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="formData.projectName" placeholder="请输入项目名称" />
      </el-form-item>
      <el-form-item label="项目代码" prop="projectCode">
        <el-input v-model="formData.projectCode" placeholder="请输入项目代码" />
      </el-form-item>
      <el-form-item label="注册证名称" prop="registrationCertificateName">
        <el-input v-model="formData.registrationCertificateName" placeholder="请输入注册证名称" />
      </el-form-item>
      <el-form-item label="注册证号" prop="registrationCertificateNumber">
        <el-input v-model="formData.registrationCertificateNumber" placeholder="请输入注册证号" />
      </el-form-item>
      <el-form-item label="持证人" prop="certificateHolder">
        <el-input v-model="formData.certificateHolder" placeholder="请输入持证人" />
      </el-form-item>
      <el-form-item label="注册地" prop="registrationPlace">
        <el-input v-model="formData.registrationPlace" placeholder="请输入注册地" />
      </el-form-item>
      <el-form-item label="生效日期" prop="effectiveDate">
        <el-input v-model="formData.effectiveDate" placeholder="例如 2026-07-03" />
      </el-form-item>
      <el-form-item label="有效期至" prop="expiryDate">
        <el-input v-model="formData.expiryDate" placeholder="例如 2031-07-03" />
      </el-form-item>
      <el-form-item label="分类" prop="classification">
        <el-input v-model="formData.classification" placeholder="请输入分类" />
      </el-form-item>
      <el-form-item label="注册证信息链接" prop="registrationInfoLink">
        <el-input v-model="formData.registrationInfoLink" placeholder="请输入注册证信息链接" />
      </el-form-item>
      <el-form-item label="产品状态" prop="productStatus">
        <el-select v-model="formData.productStatus" class="!w-100%" clearable placeholder="请选择产品状态">
          <el-option
            v-for="item in productStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" :rows="3" type="textarea" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          :disabled="formLoading"
          @click="submitForm"
        >
          确定
        </el-button>
        <el-button class="scheme-d-btn scheme-d-btn--neutral" @click="formVisible = false">取消</el-button>
      </div>
    </template>
  </Dialog>

  <Dialog v-model="bindingVisible" class="scheme-d-form-control" title="绑定项目代码和注册证" width="620px">
    <el-form ref="bindingFormRef" v-loading="bindingLoading" :model="bindingFormData" :rules="bindingFormRules" label-width="126px">
      <el-form-item label="产品目录">
        <el-input :model-value="bindingRow?.product || bindingRow?.productCode || '-'" disabled />
      </el-form-item>
      <el-form-item label="DCC项目代码" prop="projectCodeId">
        <el-select v-model="bindingFormData.projectCodeId" class="!w-100%" filterable placeholder="请选择DCC项目代码" @change="handleBindingProjectCodeChange">
          <el-option v-for="item in bindingProjectCodeOptions" :key="item.id" :label="`${item.projectCode} / ${item.projectName}`" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="注册证" prop="registrationCertificateId">
        <el-select v-model="bindingFormData.registrationCertificateId" class="!w-100%" filterable :disabled="!bindingFormData.projectCodeId || bindingCertificateLoading" placeholder="请先选择DCC项目代码">
          <el-option v-for="item in bindingCertificateOptions" :key="item.certificateId" :label="`${item.certificateNo} / ${item.productName}`" :value="item.certificateId" />
        </el-select>
      </el-form-item>
      <el-form-item label="绑定备注" prop="relationRemark">
        <el-input v-model="bindingFormData.relationRemark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="请输入绑定备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button class="scheme-d-btn scheme-d-btn--success" type="primary" :loading="bindingLoading" @click="submitBinding">确定绑定</el-button>
        <el-button class="scheme-d-btn scheme-d-btn--neutral" :disabled="bindingLoading" @click="bindingVisible = false">取消</el-button>
      </div>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormRules } from 'element-plus'
import { confirmProjectProductApplication } from './project-product-confirmation'
import { useUserStore } from '@/store/modules/user'
import { canResubmitProjectProductRequest, restoreRejectedProjectProductForm } from './project-product-resubmit'
import ProjectAttributesFields from '../../project-attributes/ProjectAttributesFields.vue'
import FolderTemplateLibraryEditor from './FolderTemplateLibraryEditor.vue'
import ProjectReviewerConfiguration from './ProjectReviewerConfiguration.vue'
import { canReviewProjectProduct, requireConfiguredReviewer } from './project-reviewer'
import { emptyAttributes, validateAttributes, type ProjectAttributes } from '../../project-attributes/state'
import { getFolderTemplates, type FolderTemplate, type FolderNode } from '@/api/dcc/controlledFile/projectAttributes'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { useClipboard } from '@vueuse/core'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import type {
  DccProductCatalogPageReqVO,
  DccProductCatalogRespVO,
  DccProductCatalogSaveReqVO,
  DccProductCatalogUpdateReqVO
} from '@/api/dcc/controlledFile/productCatalog'
import {
  createProductCatalog,
  deleteProductCatalog,
  getProductCatalogPage,
  updateProductCatalog
} from '@/api/dcc/controlledFile/productCatalog'
import {
  approveDccProjectProductRequest,
  createDccProjectProductRequest,
  resubmitDccProjectProductRequest,
  getDccProjectProductRequests,
  getDccProjectReviewerConfiguration,
  retryDccProjectProductRequestWrite,
  reviewDccProjectProductRequest,
  type DccProjectProductCreateReqVO,
  type DccProjectProductCreateRespVO,
  type DccProjectReviewerConfiguration,
  type DccProjectRequestId
} from '@/api/dcc/controlledFile/projectProductRequests'
import { getProjectCodePage, type DccProjectCodeRespVO } from '@/api/dcc/controlledFile/projectCodes'
import { createDccDataRelation } from '@/api/dcc/dataRelations'
import {
  getRegistrationCertificatePage,
  type DccRegistrationCertificatePageItemVO
} from '@/api/dcc/registrationCertificate'

defineOptions({ name: 'ProductCatalogTabPanel' })

const message = useMessage()
const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const total = ref(0)
const formRef = ref()
const bindingFormRef = ref()
const bindingVisible = ref(false)
const bindingLoading = ref(false)
const bindingCertificateLoading = ref(false)
const bindingRow = ref<DccProductCatalogRespVO>()
const bindingProjectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const bindingCertificateOptions = ref<DccRegistrationCertificatePageItemVO[]>([])
const bindingFormData = reactive({
  projectCodeId: undefined as number | string | undefined,
  registrationCertificateId: undefined as number | string | undefined,
  relationRemark: ''
})
const bindingFormRules: FormRules = {
  projectCodeId: [{ required: true, message: '请选择DCC项目代码', trigger: 'change' }],
  registrationCertificateId: [{ required: true, message: '请选择注册证', trigger: 'change' }]
}
const projectProductDialogVisible = ref(false)
const projectProductMode = ref<'create' | 'records'>('create')
const projectProductLoading = ref(false)
const projectProductRequestsLoading = ref(false)
const projectProductFormRef = ref()
const projectProductRequests = ref<DccProjectProductCreateRespVO[]>([])
const projectLeaderUsers = ref<UserVO[]>([])
const folderTemplates = ref<FolderTemplate[]>([])
const folderLibraryRef = ref<InstanceType<typeof FolderTemplateLibraryEditor>>()
const projectProductError = ref('')
const projectProductResubmitRequestId = ref<DccProjectRequestId>()
const reviewerConfiguration = ref<DccProjectReviewerConfiguration>()
const projectProductRejectReason = ref('')
const reloadFolderTemplates = async () => {
  try { folderTemplates.value = await getFolderTemplates() }
  catch (cause) { projectProductError.value = cause instanceof Error ? cause.message : String(cause) }
}
type FolderPreviewNode = FolderNode & { children: FolderPreviewNode[] }
const buildFolderPreview = (json: string): FolderPreviewNode[] => {
  const structure = JSON.parse(json) as { nodes: FolderNode[] }
  if (!Array.isArray(structure.nodes)) throw new Error('模板目录结构缺失')
  const build = (parentKey: string | null): FolderPreviewNode[] => structure.nodes
    .filter(node => (node.parentKey || null) === parentKey)
    .map(node => ({ ...node, children: build(node.key) }))
  return build(null)
}
const requestFolderPreview = (row: DccProjectProductCreateRespVO) => buildFolderPreview(row.folderTemplateSnapshotJson!)
const requestAttributeResult = (row: DccProjectProductCreateRespVO): { attributes?: ProjectAttributes; error?: string } => {
  if (!row.defaultAttributesJson) return {}
  try { return { attributes: validateAttributes(JSON.parse(row.defaultAttributesJson)) } }
  catch (cause) { return { error: cause instanceof Error ? cause.message : String(cause) } }
}
const selectedFolderPreview = computed(() => {
  const row = folderTemplates.value.find(item => item.id === projectProductForm.folderTemplateId)
  if (!row) return []
  return buildFolderPreview(row.structureJson)
})
const projectProductForm = reactive<DccProjectProductCreateReqVO>({
  projectName: '',
  projectCode: '',
  projectLeader: '',
  projectLeaderUserId: undefined,
  folderTemplateId: undefined,
  defaultAttributes: emptyAttributes(),
  productCode: '',
  productName: '',
  classification: '一类',
  creationReason: '',
  remark: ''
})
const projectProductRules: FormRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectCode: [{ required: true, message: '请输入项目代码', trigger: 'blur' }],
  projectLeaderUserId: [{ required: true, message: '请选择正式负责人账号', trigger: 'change' }],
  folderTemplateId: [{ required: true, message: '请选择目录模板', trigger: 'change' }],
  productCode: [{ required: true, message: '请输入产品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  classification: [{ required: true, message: '请选择分类', trigger: 'change' }],
  creationReason: [{ validator: (_rule, value, callback) => {
    if (!projectProductResubmitRequestId.value && !String(value || '').trim()) callback(new Error('请填写新建申请原因'))
    else callback()
  }, trigger: 'blur' }],
  resubmissionReason: [{ validator: (_rule, value, callback) => {
    if (projectProductResubmitRequestId.value && !String(value || '').trim()) callback(new Error('请填写重提说明'))
    else callback()
  }, trigger: 'blur' }]
}

const productStatusOptions = [
  { label: '在研(N)', value: 'N' },
  { label: '在售(S)', value: 'S' },
  { label: '已取消(C)', value: 'C' }
]

const dataSourceOptions = [
  { label: '瑛泰产品', value: '瑛泰产品' }
]

const productCatalogQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'keyword',
    label: '关键词',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '产品、编码、持证人'
  },
  {
    key: 'categoryLevel1',
    label: '产品类别 I',
    type: 'text',
    queryParamKey: 'categoryLevel1',
    placeholder: '产品类别 I'
  },
  {
    key: 'categoryLevel2',
    label: '产品类别 II',
    type: 'text',
    queryParamKey: 'categoryLevel2',
    placeholder: '产品类别 II'
  },
  {
    key: 'productSequence',
    label: '产品序号',
    type: 'text',
    queryParamKey: 'productSequence',
    placeholder: '产品序号'
  },
  {
    key: 'product',
    label: '产品',
    type: 'text',
    queryParamKey: 'product',
    placeholder: '产品'
  },
  {
    key: 'productStatus',
    label: '产品状态',
    type: 'select',
    queryParamKey: 'productStatus',
    options: productStatusOptions
  },
  {
    key: 'dataSource',
    label: '数据来源',
    type: 'select',
    queryParamKey: 'dataSource',
    options: dataSourceOptions
  },
  {
    key: 'productCode',
    label: '产品编码',
    type: 'text',
    queryParamKey: 'productCode',
    placeholder: '产品编码'
  },
  {
    key: 'projectName',
    label: '项目名称',
    type: 'text',
    queryParamKey: 'projectName',
    placeholder: '项目名称'
  },
  {
    key: 'projectCode',
    label: '项目代码',
    type: 'text',
    queryParamKey: 'projectCode',
    placeholder: '项目代码'
  },
  {
    key: 'registrationCertificateName',
    label: '注册证名称',
    type: 'text',
    queryParamKey: 'registrationCertificateName',
    placeholder: '注册证名称'
  },
  {
    key: 'registrationCertificateNumber',
    label: '注册证号',
    type: 'text',
    queryParamKey: 'registrationCertificateNumber',
    placeholder: '注册证号'
  },
  {
    key: 'certificateHolder',
    label: '持证人',
    type: 'text',
    queryParamKey: 'certificateHolder',
    placeholder: '持证人'
  },
  {
    key: 'registrationPlace',
    label: '注册地',
    type: 'text',
    queryParamKey: 'registrationPlace',
    placeholder: '注册地'
  },
  {
    key: 'effectiveDate',
    label: '生效日期',
    type: 'text',
    queryParamKey: 'effectiveDate',
    placeholder: '生效日期'
  },
  {
    key: 'expiryDate',
    label: '有效期至',
    type: 'text',
    queryParamKey: 'expiryDate',
    placeholder: '有效期至'
  },
  {
    key: 'classification',
    label: '分类',
    type: 'text',
    queryParamKey: 'classification',
    placeholder: '分类'
  },
  {
    key: 'registrationInfoLink',
    label: '注册证信息链接',
    type: 'text',
    queryParamKey: 'registrationInfoLink',
    placeholder: '注册证信息链接'
  },
  {
    key: 'remark',
    label: '备注',
    type: 'text',
    queryParamKey: 'remark',
    placeholder: '备注'
  }
]

const PRODUCT_CATALOG_ACTION_PANEL_WIDTH = 188

const productCatalogDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'categoryLevel1', label: '产品类别 I', minWidth: 180 },
  { key: 'categoryLevel2', label: '产品类别 II', minWidth: 180 },
  { key: 'productSequence', label: '产品序号', minWidth: 100 },
  { key: 'product', label: '产品', minWidth: 220 },
  { key: 'dataSource', label: '数据来源', minWidth: 120 },
  { key: 'productCode', label: '产品编码', minWidth: 120 },
  { key: 'projectName', label: '项目名称', minWidth: 180, sortable: 'custom' },
  { key: 'projectCode', label: '项目代码', minWidth: 120, sortable: 'custom' },
  { key: 'batchRecordTotalRecognitionJson', label: '批记录识别JSON', width: 140 },
  { key: 'classification', label: '分类', minWidth: 120 },
  { key: 'registrationCertificateName', label: '注册证名称', minWidth: 220 },
  { key: 'registrationCertificateNumber', label: '注册证号', minWidth: 180 },
  { key: 'certificateHolder', label: '持证人', minWidth: 160 },
  { key: 'registrationPlace', label: '注册地', minWidth: 120 },
  { key: 'effectiveDate', label: '生效日期', width: 120 },
  { key: 'expiryDate', label: '有效期至', width: 120 },
  { key: 'productStatus', label: '产品状态', width: 120 },
  { key: 'registrationInfoLink', label: '注册证信息链接', minWidth: 150 },
  { key: 'remark', label: '备注', minWidth: 220 },
  {
    key: 'actions',
    label: '操作',
    width: PRODUCT_CATALOG_ACTION_PANEL_WIDTH,
    hideable: false,
    business: false
  }
]

const {
  columns: productCatalogColumns,
  saving: productCatalogColumnSaving,
  isColumnVisible: isProductCatalogColumnVisible,
  getColumnWidthString: getProductCatalogColumnWidthString,
  getColumnMinWidthString: getProductCatalogColumnMinWidthString,
  handleHeaderDragend: handleProductCatalogHeaderDragend,
  saveConfig: saveProductCatalogColumnConfig,
  resetConfig: resetProductCatalogColumnConfig
} = useUserTableColumns('dcc.productCatalog.main', productCatalogDefaultColumns)

const getProductCatalogActionColumnWidthString = () => {
  return String(PRODUCT_CATALOG_ACTION_PANEL_WIDTH)
}

type DccProductCatalogPageQuery = DccProductCatalogPageReqVO & {
  pageNo: number
  pageSize: number
}

type ProductCatalogSortChange = {
  prop?: string
  order?: 'ascending' | 'descending' | null
}

const productCatalogSortState = ref<ProductCatalogSortChange>({})
const PRODUCT_CATALOG_SERVER_SORT_FIELDS = new Set(['projectName', 'projectCode'])

const queryParams = reactive<DccProductCatalogPageQuery>({
  pageNo: 1,
  pageSize: 10,
  keyword: undefined,
  categoryLevel1: undefined,
  categoryLevel2: undefined,
  productSequence: undefined,
  product: undefined,
  productStatus: undefined,
  dataSource: undefined,
  productCode: undefined,
  projectName: undefined,
  projectCode: undefined,
  registrationCertificateName: undefined,
  registrationCertificateNumber: undefined,
  certificateHolder: undefined,
  registrationPlace: undefined,
  effectiveDate: undefined,
  expiryDate: undefined,
  classification: undefined,
  registrationInfoLink: undefined,
  remark: undefined,
  projectCodeNotBlank: undefined,
  sortField: undefined,
  sortOrder: undefined
})

const formData = ref<DccProductCatalogUpdateReqVO>({
  dataSource: '瑛泰产品',
  originalRowNo: 0,
  categoryLevel1: '',
  categoryLevel2: '',
  productSequence: '',
  product: '',
  productCode: '',
  projectName: '',
  projectCode: '',
  registrationCertificateName: '',
  registrationCertificateNumber: '',
  certificateHolder: '',
  registrationPlace: '',
  effectiveDate: '',
  expiryDate: '',
  classification: '',
  registrationInfoLink: '',
  productStatus: '',
  remark: ''
})

const formRules = reactive<FormRules>({
  dataSource: [{ required: true, message: '数据来源不能为空', trigger: 'change' }],
  product: [{ required: true, message: '产品不能为空', trigger: 'blur' }]
})

const resetFormData = () => {
  formData.value = {
    dataSource: '瑛泰产品',
    originalRowNo: 0,
    categoryLevel1: '',
    categoryLevel2: '',
    productSequence: '',
    product: '',
    productCode: '',
    projectName: '',
    projectCode: '',
    registrationCertificateName: '',
    registrationCertificateNumber: '',
    certificateHolder: '',
    registrationPlace: '',
    effectiveDate: '',
    expiryDate: '',
    classification: '',
    registrationInfoLink: '',
    productStatus: '',
    remark: ''
  }
  formRef.value?.resetFields()
}

const resetProjectProductForm = () => {
  projectProductResubmitRequestId.value = undefined
  projectProductRejectReason.value = ''
  projectProductForm.projectName = ''
  projectProductForm.projectCode = ''
  projectProductForm.projectLeader = ''
  projectProductForm.projectLeaderUserId = undefined
  projectProductForm.folderTemplateId = undefined
  projectProductForm.defaultAttributes = emptyAttributes()
  projectProductError.value = ''
  projectProductForm.productCode = ''
  projectProductForm.productName = ''
  projectProductForm.classification = '一类'
  projectProductForm.remark = ''
  projectProductForm.resubmissionReason = ''
  projectProductForm.creationReason = ''
  projectProductFormRef.value?.resetFields()
}

const loadProjectProductRequests = async () => {
  projectProductRequestsLoading.value = true
  try {
    projectProductRequests.value = await getDccProjectProductRequests()
  } finally {
    projectProductRequestsLoading.value = false
  }
}

const openProjectProductDialog = async (mode: 'create' | 'records' = 'create') => {
  resetProjectProductForm()
  projectProductMode.value = mode
  projectProductDialogVisible.value = true
  projectProductLoading.value = true
  reviewerConfiguration.value = undefined
  try {
    if (mode === 'records') {
      await loadProjectProductRequests()
      return
    }
    const results = await Promise.all([getSimpleUserList(), getFolderTemplates(), loadProjectProductRequests(), getDccProjectReviewerConfiguration()])
    projectLeaderUsers.value = results[0]
    folderTemplates.value = results[1]
    reviewerConfiguration.value = results[3]
  } catch (cause) { projectProductError.value = cause instanceof Error ? cause.message : String(cause) }
  finally { projectProductLoading.value = false }
}

const editRejectedProjectProduct = async (row: DccProjectProductCreateRespVO) => {
  try {
    const restored = restoreRejectedProjectProductForm(row, userStore.getUser.id)
    if (projectProductMode.value === 'records') {
      await openProjectProductDialog('create')
      if (projectProductError.value) return
    }
    Object.assign(projectProductForm, restored, { resubmissionReason: '', creationReason: '' })
    projectProductResubmitRequestId.value = row.id
    projectProductRejectReason.value = row.rejectReason || '历史未记录'
    projectProductError.value = ''
    projectProductFormRef.value?.clearValidate()
  } catch (cause) { projectProductError.value = cause instanceof Error ? cause.message : String(cause) }
}

const submitProjectProductRequest = async () => {
  if (projectProductLoading.value) return
  const valid = await projectProductFormRef.value?.validate()
  if (!valid || projectProductLoading.value) return
  projectProductLoading.value = true
  projectProductError.value = ''
  try {
    validateAttributes(projectProductForm.defaultAttributes)
    const reason = projectProductResubmitRequestId.value ? projectProductForm.resubmissionReason : projectProductForm.creationReason
    if (!reason?.trim() || reason.length > 500) throw new Error(projectProductResubmitRequestId.value ? '请填写有效重提说明' : '请填写有效新建申请原因')
    const rejectedRequestId = projectProductResubmitRequestId.value
    const dialogVisible = projectProductDialogVisible.value
    const mode = projectProductMode.value
    const payload = JSON.parse(JSON.stringify(projectProductForm))
    const selectedLeader = () => projectLeaderUsers.value.filter(row => String(row.id) === String(payload.projectLeaderUserId))
    const selectedTemplate = () => folderTemplates.value.filter(row => String(row.id) === String(payload.folderTemplateId))
    const selectedDirectoryContext = JSON.stringify([selectedLeader(), selectedTemplate()])
    const sameContext = () => rejectedRequestId === projectProductResubmitRequestId.value &&
      dialogVisible === projectProductDialogVisible.value && mode === projectProductMode.value &&
      selectedDirectoryContext === JSON.stringify([selectedLeader(), selectedTemplate()]) &&
      JSON.stringify(payload) === JSON.stringify(projectProductForm)
    reviewerConfiguration.value = requireConfiguredReviewer(await getDccProjectReviewerConfiguration())
    if (!sameContext()) throw new Error('申请内容或所属申请已变化，请重新确认提交')
    const confirmedReviewer = { ...reviewerConfiguration.value }
    await confirmProjectProductApplication(payload, projectLeaderUsers.value, folderTemplates.value,
      confirmedReviewer, Boolean(rejectedRequestId))
    if (!sameContext()) throw new Error('申请内容或所属申请已变化，请重新确认提交')
    reviewerConfiguration.value = requireConfiguredReviewer(await getDccProjectReviewerConfiguration())
    if (!sameContext()) throw new Error('申请内容或所属申请已变化，请重新确认提交')
    if (reviewerConfiguration.value.reviewerUserId !== confirmedReviewer.reviewerUserId ||
        reviewerConfiguration.value.reviewerUsername !== confirmedReviewer.reviewerUsername ||
        reviewerConfiguration.value.reviewerNickname !== confirmedReviewer.reviewerNickname)
      throw new Error('审核人配置已变化，请重新核对并确认申请')
    if (rejectedRequestId) {
      await resubmitDccProjectProductRequest(rejectedRequestId, payload)
    } else {
      await createDccProjectProductRequest(payload)
    }
    message.success('申请已提交，重新进入审核')
    if (sameContext()) resetProjectProductForm()
    await loadProjectProductRequests()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close')
      projectProductError.value = cause instanceof Error ? cause.message : String(cause)
  }
  finally {
    projectProductLoading.value = false
  }
}

const handleProjectProductAction = async (
  row: DccProjectProductCreateRespVO,
  node: 'review' | 'approve',
  approve: boolean
) => {
  if (node === 'review' && !canReviewProjectProduct(row, userStore.getUser.id)) {
    projectProductError.value = '仅本次申请提交时指定的审核人可审核'
    return
  }
  const reason = window.prompt(approve ? '请输入通过意见' : '请输入驳回原因', '')
  if (reason === null || !reason.trim()) {
    message.warning('审批意见不能为空')
    return
  }
  projectProductRequestsLoading.value = true
  try {
    if (node === 'review') {
      await reviewDccProjectProductRequest(row.id, { reason: reason.trim() }, approve)
    } else {
      await approveDccProjectProductRequest(row.id, { reason: reason.trim() }, approve)
    }
    message.success('审批处理成功')
    await loadProjectProductRequests()
    await getList()
  } finally {
    projectProductRequestsLoading.value = false
  }
}

const handleProjectProductRetry = async (row: DccProjectProductCreateRespVO) => {
  const reason = window.prompt('请输入本次重试写入原因', '')
  if (reason === null) return
  if (!reason.trim() || reason.length > 500) { projectProductError.value = '请填写有效重试写入原因'; return }
  projectProductRequestsLoading.value = true
  projectProductError.value = ''
  try {
    await retryDccProjectProductRequestWrite(row.id, { reason: reason.trim() })
    message.success('已重新写入')
    await loadProjectProductRequests()
    await getList()
  } catch (cause) { projectProductError.value = cause instanceof Error ? cause.message : String(cause) }
  finally {
    projectProductRequestsLoading.value = false
  }
}

const PRODUCT_CATALOG_TREE_PAGE_SIZE = 200
const PRODUCT_CATALOG_UNCLASSIFIED = '未分类'
const PRODUCT_CATALOG_UNNAMED = '未命名产品'

type DccProductCatalogTreeOption = {
  treeNodeId: string
  nodeType: 'all' | 'categoryLevel1' | 'categoryLevel2' | 'product'
  label: string
  detailCount: number
  categoryLevel1?: string | null
  categoryLevel2?: string | null
  product?: string | null
  children: DccProductCatalogTreeOption[]
}

const createProductCatalogAllTreeNode = (
  detailCount = 0,
  children: DccProductCatalogTreeOption[] = []
): DccProductCatalogTreeOption => ({
  treeNodeId: 'all',
  nodeType: 'all',
  label: '全部产品',
  detailCount,
  children
})

const productCatalogFlatRows = ref<DccProductCatalogRespVO[]>([])
const treeList = ref<DccProductCatalogTreeOption[]>([createProductCatalogAllTreeNode()])
const selectedProductCatalogTreeNode = ref<DccProductCatalogTreeOption>(
  createProductCatalogAllTreeNode()
)

const selectedProductCatalogRows = computed<DccProductCatalogRespVO[]>(() =>
  productCatalogFlatRows.value.filter((row) =>
    matchesProductCatalogTreeSelection(row, selectedProductCatalogTreeNode.value)
  )
)

const getList = async () => {
  loading.value = true
  try {
    const rows = await getAllProductCatalogRows()
    productCatalogFlatRows.value = rows
    const nextTreeList = buildProductCatalogTree(rows)
    treeList.value = nextTreeList
    selectedProductCatalogTreeNode.value =
      findProductCatalogTreeNode(nextTreeList, selectedProductCatalogTreeNode.value.treeNodeId) ||
      nextTreeList[0] ||
      createProductCatalogAllTreeNode()
    total.value = rows.length
  } finally {
    loading.value = false
  }
}

const getAllProductCatalogRows = async (): Promise<DccProductCatalogRespVO[]> => {
  const baseQuery = buildProductCatalogTreeQuery()
  const firstPage = await getProductCatalogPage(baseQuery)
  const rows = [...firstPage.list]
  const totalRows = firstPage.total || rows.length
  let pageNo = 2
  while (rows.length < totalRows) {
    const nextPage = await getProductCatalogPage({ ...baseQuery, pageNo })
    if (nextPage.list.length === 0) {
      throw new Error('产品目录分页返回数量与总数不一致，无法构建完整树')
    }
    rows.push(...nextPage.list)
    pageNo += 1
  }
  return rows
}

const buildProductCatalogTreeQuery = (): DccProductCatalogPageReqVO => ({
  ...queryParams,
  pageNo: 1,
  pageSize: PRODUCT_CATALOG_TREE_PAGE_SIZE
})

const buildProductCatalogTree = (
  rows: DccProductCatalogRespVO[]
): DccProductCatalogTreeOption[] => {
  const level1Nodes: DccProductCatalogTreeOption[] = []
  const nodeMap = new Map<string, DccProductCatalogTreeOption>()

  for (const row of rows) {
    const categoryLevel1 = normalizeTreeLabel(row.categoryLevel1, PRODUCT_CATALOG_UNCLASSIFIED)
    const categoryLevel2 = normalizeTreeLabel(row.categoryLevel2, PRODUCT_CATALOG_UNCLASSIFIED)
    const product = normalizeTreeLabel(row.product, PRODUCT_CATALOG_UNNAMED)
    const level1Key = `categoryLevel1:${row.dataSource}:${categoryLevel1}`
    const level2Key = `categoryLevel2:${row.dataSource}:${categoryLevel1}:${categoryLevel2}`
    const productKey = `product:${row.dataSource}:${categoryLevel1}:${categoryLevel2}:${product}`

    const level1Node = ensureProductCatalogTreeNode(nodeMap, level1Nodes, {
      treeNodeId: level1Key,
      nodeType: 'categoryLevel1',
      label: categoryLevel1,
      categoryLevel1
    })
    const level2Node = ensureProductCatalogTreeNode(nodeMap, level1Node.children || [], {
      treeNodeId: level2Key,
      nodeType: 'categoryLevel2',
      label: categoryLevel2,
      categoryLevel1,
      categoryLevel2
    })
    const productNode = ensureProductCatalogTreeNode(nodeMap, level2Node.children || [], {
      treeNodeId: productKey,
      nodeType: 'product',
      label: product,
      categoryLevel1,
      categoryLevel2,
      product
    })
    level1Node.detailCount += 1
    level2Node.detailCount += 1
    productNode.detailCount += 1
  }

  return [createProductCatalogAllTreeNode(rows.length, level1Nodes)]
}

type ProductCatalogBranchNodeInput = Omit<DccProductCatalogTreeOption, 'detailCount' | 'children'>

const ensureProductCatalogTreeNode = (
  nodeMap: Map<string, DccProductCatalogTreeOption>,
  siblings: DccProductCatalogTreeOption[],
  input: ProductCatalogBranchNodeInput
): DccProductCatalogTreeOption => {
  const existing = nodeMap.get(input.treeNodeId)
  if (existing) {
    return existing
  }
  const node: DccProductCatalogTreeOption = {
    treeNodeId: input.treeNodeId,
    nodeType: input.nodeType,
    label: input.label,
    detailCount: 0,
    categoryLevel1: input.categoryLevel1 || null,
    categoryLevel2: input.categoryLevel2 || null,
    product: input.product || null,
    children: []
  }
  nodeMap.set(input.treeNodeId, node)
  siblings.push(node)
  return node
}

const findProductCatalogTreeNode = (
  nodes: DccProductCatalogTreeOption[],
  treeNodeId: string
): DccProductCatalogTreeOption | undefined => {
  for (const node of nodes) {
    if (node.treeNodeId === treeNodeId) {
      return node
    }
    const childNode = findProductCatalogTreeNode(node.children || [], treeNodeId)
    if (childNode) {
      return childNode
    }
  }
  return undefined
}

const handleProductCatalogTreeNodeClick = (node: DccProductCatalogTreeOption) => {
  selectedProductCatalogTreeNode.value = node
}

const normalizeTreeLabel = (value: string | null | undefined, defaultValue: string) => {
  const normalized = value?.trim()
  return normalized || defaultValue
}

const matchesProductCatalogTreeSelection = (
  row: DccProductCatalogRespVO,
  selectedNode: DccProductCatalogTreeOption
) => {
  if (selectedNode.nodeType === 'all') {
    return true
  }
  const rowCategoryLevel1 = normalizeTreeLabel(row.categoryLevel1, PRODUCT_CATALOG_UNCLASSIFIED)
  if (rowCategoryLevel1 !== selectedNode.categoryLevel1) {
    return false
  }
  if (selectedNode.nodeType === 'categoryLevel1') {
    return true
  }
  const rowCategoryLevel2 = normalizeTreeLabel(row.categoryLevel2, PRODUCT_CATALOG_UNCLASSIFIED)
  if (rowCategoryLevel2 !== selectedNode.categoryLevel2) {
    return false
  }
  if (selectedNode.nodeType === 'categoryLevel2') {
    return true
  }
  return normalizeTreeLabel(row.product, PRODUCT_CATALOG_UNNAMED) === selectedNode.product
}

const productCatalogQuickFilter = useTableQuickFilter(
  'dcc.productCatalog.main',
  productCatalogQuickFilterDefinitions,
  queryParams,
  getList
)

const handleProductCatalogSortChange = ({ prop, order }: ProductCatalogSortChange) => {
  queryParams.pageNo = 1
  const sortField = prop || ''
  if (!order || !PRODUCT_CATALOG_SERVER_SORT_FIELDS.has(sortField)) {
    queryParams.sortField = undefined
    queryParams.sortOrder = undefined
    getList()
    return
  }
  queryParams.sortField = sortField
  queryParams.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  getList()
}

const copyProductCatalogBatchRecordTotalRecognitionJson = async (row: DccProductCatalogRespVO) => {
  const { copy, copied, isSupported } = useClipboard({
    legacy: true,
    source: row.batchRecordTotalRecognitionJson || ''
  })
  if (!isSupported) {
    message.error('当前浏览器不支持复制')
    return
  }
  await copy()
  if (unref(copied)) {
    message.success('批记录识别 JSON 已复制')
  }
}

const resetBindingForm = () => {
  bindingFormData.projectCodeId = undefined
  bindingFormData.registrationCertificateId = undefined
  bindingFormData.relationRemark = ''
  bindingCertificateOptions.value = []
  bindingFormRef.value?.resetFields()
}

const openBinding = async (row: DccProductCatalogRespVO) => {
  bindingRow.value = row
  resetBindingForm()
  bindingVisible.value = true
  bindingLoading.value = true
  try {
    const page = await getProjectCodePage({ pageNo: 1, pageSize: 200, status: 'ENABLE' })
    bindingProjectCodeOptions.value = page.list
  } finally {
    bindingLoading.value = false
  }
}

const handleBindingProjectCodeChange = async (projectCodeId: number | string) => {
  bindingFormData.registrationCertificateId = undefined
  bindingCertificateOptions.value = []
  if (!projectCodeId) return
  bindingCertificateLoading.value = true
  try {
    const page = await getRegistrationCertificatePage({ pageNo: 1, pageSize: 200, projectCodeId, status: 'CURRENT' })
    bindingCertificateOptions.value = page.list
  } finally {
    bindingCertificateLoading.value = false
  }
}

const submitBinding = async () => {
  const valid = await bindingFormRef.value?.validate()
  if (!valid || !bindingRow.value) return
  bindingLoading.value = true
  try {
    await createDccDataRelation({
      productCatalogId: bindingRow.value.id,
      projectCodeId: bindingFormData.projectCodeId!,
      registrationCertificateId: bindingFormData.registrationCertificateId!,
      relationRemark: bindingFormData.relationRemark.trim() || undefined
    })
    message.success('绑定成功，产品目录已同步注册证数据')
    bindingVisible.value = false
    await getList()
  } finally {
    bindingLoading.value = false
  }
}

const openLinkedProjectCode = (row: DccProductCatalogRespVO) => {
  if (!row.projectCodeId) {
    message.error('当前产品目录缺少绑定的 DCC 项目代码，无法跳转')
    return
  }
  router.push({
    path: '/mdm/project-code',
    query: { projectCodeId: String(row.projectCodeId) }
  })
}

const openLinkedRegistrationCertificate = (row: DccProductCatalogRespVO) => {
  if (!row.registrationCertificateId) {
    message.error('当前产品目录缺少绑定的注册证，无法跳转')
    return
  }
  router.push({
    path: '/mdm/registration-certificate/detail/' + String(row.registrationCertificateId),
    query: {
      projectCodeId: row.projectCodeId ? String(row.projectCodeId) : undefined
    }
  })
}

const openForm = (type: 'create' | 'update', row?: DccProductCatalogRespVO) => {
  formVisible.value = true
  formType.value = type
  resetFormData()
  if (type === 'update' && row) {
    formData.value = {
      dataSource: row.dataSource,
      originalRowNo: row.originalRowNo,
      categoryLevel1: row.categoryLevel1 || '',
      categoryLevel2: row.categoryLevel2 || '',
      productSequence: row.productSequence || '',
      product: row.product || '',
      productCode: row.productCode || '',
      projectName: row.projectName || '',
      projectCode: row.projectCode || '',
      registrationCertificateName: row.registrationCertificateName || '',
      registrationCertificateNumber: row.registrationCertificateNumber || '',
      certificateHolder: row.certificateHolder || '',
      registrationPlace: row.registrationPlace || '',
      effectiveDate: row.effectiveDate || '',
      expiryDate: row.expiryDate || '',
      classification: row.classification || '',
      registrationInfoLink: row.registrationInfoLink || '',
      productStatus: row.productStatus || '',
      remark: row.remark || ''
    }
  }
}

const buildSavePayload = (): DccProductCatalogSaveReqVO => ({
  dataSource: formData.value.dataSource,
  categoryLevel1: formData.value.categoryLevel1,
  categoryLevel2: formData.value.categoryLevel2,
  productSequence: formData.value.productSequence,
  product: formData.value.product,
  productCode: formData.value.productCode,
  projectName: formData.value.projectName,
  projectCode: formData.value.projectCode,
  registrationCertificateName: formData.value.registrationCertificateName,
  registrationCertificateNumber: formData.value.registrationCertificateNumber,
  certificateHolder: formData.value.certificateHolder,
  registrationPlace: formData.value.registrationPlace,
  effectiveDate: formData.value.effectiveDate,
  expiryDate: formData.value.expiryDate,
  classification: formData.value.classification,
  registrationInfoLink: formData.value.registrationInfoLink,
  productStatus: formData.value.productStatus,
  remark: formData.value.remark
})

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await createProductCatalog(buildSavePayload())
      message.success('新增产品目录成功')
    } else {
      await updateProductCatalog({
        ...buildSavePayload(),
        originalRowNo: formData.value.originalRowNo
      })
      message.success('编辑产品目录成功')
    }
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (row: DccProductCatalogRespVO) => {
  try {
    await message.delConfirm(`确认删除产品目录“${row.product || row.productCode || row.originalRowNo}”吗？`)
  } catch {
    return
  }
  loading.value = true
  try {
    await deleteProductCatalog(row.dataSource, row.originalRowNo)
    message.success('删除产品目录成功')
    await getList()
  } finally {
    loading.value = false
  }
}

const formatProductStatus = (status?: string | null) => {
  const normalized = status?.trim()
  const labels: Record<string, string> = {
    N: '在研(N)',
    S: '在售(S)',
    C: '已取消(C)'
  }
  if (!normalized) {
    return '-'
  }
  return labels[normalized] || normalized
}

onMounted(async () => {
  await getList()
})
</script>

<style scoped>
@media (min-width: 1181px) {
  .dcc-product-catalog-list-template.unified-list-template--single-line-toolbar
    :deep(.unified-list-template__query-form) {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .dcc-product-catalog-list-template.unified-list-template--single-line-toolbar
    :deep(.unified-list-template__multi-filter) {
    min-width: 0;
  }

  .dcc-product-catalog-list-template.unified-list-template--single-line-toolbar
    :deep(.table-multi-filter),
  .dcc-product-catalog-list-template.unified-list-template--single-line-toolbar
    :deep(.table-multi-filter__tabs-empty) {
    min-width: 0;
  }
}

.dcc-product-catalog-split-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 12px;
  height: clamp(420px, calc(100vh - 258px), 680px);
  min-height: 0;
}

.dcc-product-catalog-tree-panel {
  min-width: 0;
  padding: 10px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}

.dcc-product-catalog-tree-title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.dcc-product-catalog-tree {
  height: 100%;
  overflow: auto;
}

.dcc-product-catalog-detail-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.dcc-product-catalog-detail-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  margin-bottom: 8px;
}

.dcc-product-catalog-detail-summary__title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.dcc-product-catalog-detail-summary__count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.dcc-product-catalog-table-shell {
  flex: 1 1 auto;
  min-height: 0;
}

:deep(.dcc-product-catalog-tree-node) {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 0;
  gap: 8px;
}

:deep(.dcc-product-catalog-tree-node__label) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.dcc-product-catalog-tree-node__count) {
  flex: 0 0 auto;
  padding: 0 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background-color: var(--el-fill-color-light);
  border-radius: 999px;
}

@media (max-width: 1180px) {
  .dcc-product-catalog-split-layout {
    grid-template-columns: 1fr;
  }

  .dcc-product-catalog-tree {
    max-height: 260px;
  }
}

:deep(.dcc-product-catalog-resizable-table .el-table__header-wrapper th.el-table__cell) {
  position: relative;
}

:deep(.dcc-product-catalog-resizable-table .el-table__header-wrapper th.el-table__cell::after) {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  width: 8px;
  height: 100%;
  content: '';
  cursor: col-resize;
  border-right: 2px solid transparent;
}

:deep(.dcc-product-catalog-resizable-table .el-table__header-wrapper th.el-table__cell:hover::after) {
  border-right-color: #1677ff;
}

</style>
