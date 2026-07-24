<template>
  <ContentWrap v-loading="adminLoading">
    <el-alert
      v-if="adminLoadError"
      :closable="false"
      class="mb-12px"
      show-icon
      type="error"
      :title="adminLoadError"
    />

    <CompanyWorkbench v-if="activeSection === 'company'" />
    <CompanyVersionWorkbench v-else-if="activeSection === 'companyVersion'" />
    <KeywordWorkbench v-else-if="activeSection === 'keyword'" />
    <PromptWorkbench v-else-if="activeSection === 'prompt'" />
    <div v-else-if="activeSection === 'product'" class="showroom-admin-product-tabs">
      <el-tabs v-model="productManageTab" @tab-change="handleProductManageTabChange">
        <el-tab-pane label="产品" name="product">
          <ProductListTable
            :batch-audio-auto-check-enabled="batchProductAudioAutoCheckState?.enabled"
            :batch-audio-auto-check-label="batchProductAudioAutoCheckLabel"
            :batch-generating-sales-countries="batchGeneratingProductSalesCountries"
            :batch-generating-narration-script="batchGeneratingProductNarrationScript"
            :batch-publishing="batchPublishingProduct"
            :batch-generating-audio="batchGeneratingProductAudio"
            :batch-generating-cover="batchGeneratingProductCover"
            :batch-translating-publishing="batchTranslatingPublishingProduct"
            :batch-translate-publish-task-status="productTranslatePublishTaskStatus"
            :can-batch-generate-published-media="isShowroomPublicity"
            :cover-task-summary="latestProductCoverTaskSummary"
            :exporting-excel="exportingProductExcel"
            :filters="productFilters"
            :importing-excel="importingProductExcel"
            :loading="adminLoading"
            :manage-products="isShowroomPublicity"
            :narration-script-task-active="productNarrationScriptTaskStatus?.active"
            :narration-script-task-status="productNarrationScriptTaskStatus"
            :page-no="productPageNo"
            :page-size="productPageSize"
            :page-total="productPageTotal"
            :publishing-product-id="publishingProductId"
            :products="productRows"
            :user-options="showroomUserOptions"
            @create="openProductCreate"
            @assign="openProductWholeAssignment"
            @batch-publish="handleBatchPublishProducts"
            @batch-generate-sales-countries="handleBatchGenerateProductSalesCountries"
            @batch-generate-narration-script="handleStartBatchGenerateNarrationScriptTask"
            @batch-translate-publish="handleStartBatchTranslatePublishTask"
            @batch-generate-audio="handleBatchGenerateProductNarrationAudio"
            @batch-generate-cover="handleBatchGenerateProductCoverImage"
            @delete="handleDeleteProduct"
            @detail="openProductDetail"
            @edit="openProductEdit"
            @export-excel="handleExportProductExcel"
            @open-audio-dialog="openProductAudioDialog"
            @import-excel="openProductImportForm"
            @import-base-workbook="openProductBaseWorkbookImportForm"
            @page-change="handleProductPageChange"
            @publish="handlePublishListProduct"
            @search="handleProductSearch"
            @version-center="openProductVersionCenter"
          />
        </el-tab-pane>
        <el-tab-pane label="奖项" name="award">
          <AwardListTable
            :awards="awardRows"
            :generating-cover-award-id="generatingCoverAwardId"
            :keyword="awardKeyword"
            :loading="adminLoading"
            :page-no="awardPageNo"
            :page-size="awardPageSize"
            :page-total="awardPageTotal"
            @delete="handleDeleteAward"
            @edit="openAwardEdit"
            @generate-cover="handleGenerateAwardCoverImage"
            @page-change="handleAwardPageChange"
            @search="handleAwardSearch"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
    <HallListTable
      v-else-if="activeSection === 'hall'"
      :batch-generating-audio="batchGeneratingHallAudio"
      :can-generate-audio="isShowroomPublicity"
      :exporting-config-package="exportingHallConfigPackage"
      :generating-audio-hall-id="generatingAudioHallId"
      :halls="hallRows"
      :importing-config-package="importingHallConfigPackage"
      :loading="adminLoading"
      :manage-config-package="canManageHallConfigPackage"
      :publishing-preview-hall-id="publishingHallPreviewAssetId"
      @batch-generate-audio="handleBatchGenerateHallNarrationAudio"
      @create="openHallCreate"
      @delete="handleDeleteHall"
      @edit="openHallEdit"
      @export-config-package="handleExportHallConfigPackage"
      @generate-audio="handleGenerateHallNarrationAudio"
      @import-config-package="handleImportHallConfigPackage"
      @open-canvas="openHallCanvasLayout"
      @open-mapping="openHallMapping"
      @publish-preview-asset="handlePublishHallPreviewAsset"
      @search="handleHallSearch"
    />
    <ApprovalTaskPanel v-else-if="activeSection === 'approval'" />
    <CompanyHistoryWorkbench v-else-if="activeSection === 'history'" />
    <AssignmentWorkbench
      v-else-if="activeSection === 'assignment'"
      :company-current="companyCurrent"
      :products="productRows"
    />
    <DiscussionWorkbench v-else-if="activeSection === 'discussion'" :products="productRows" />
    <NarrationWorkspace
      v-else-if="activeSection === 'narration'"
      :company-current="companyCurrent"
      :halls="hallRows"
      :products="productRows"
    />
    <el-table v-else :data="activeRows" row-key="code" class="showroom-admin-table">
      <el-table-column label="模块" min-width="150" prop="name" />
      <el-table-column label="当前状态" width="140">
        <template #default="{ row }">
          <el-tag :type="row.tagType">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关键说明" min-width="260" prop="description" show-overflow-tooltip />
      <el-table-column label="下一步动作" width="180" prop="action" />
    </el-table>
  </ContentWrap>

  <el-dialog
    v-model="productDialogVisible"
    :title="productForm.productId ? '编辑产品' : '新增产品'"
    width="1120px"
  >
    <el-form :model="productForm" label-width="120px">
      <el-tabs v-model="productDialogActiveLanguageTab">
        <el-tab-pane label="中文" name="zh">
          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="主数据产品" required>
                <el-select
                  v-model="productForm.productMasterId"
                  class="!w-full"
                  clearable
                  filterable
                  remote
                  reserve-keyword
                  :loading="productMasterOptionsLoading"
                  :remote-method="loadProductMasterOptions"
                  placeholder="请选择产品主数据"
                  @change="handleProductMasterChange"
                >
                  <el-option
                    v-for="product in productMasterOptions"
                    :key="product.id"
                    :label="formatProductMasterOptionLabel(product)"
                    :value="product.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="产品编码" required>
                <el-input v-model="productForm.productCode" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="旧产品编号">
                <el-input
                  v-model="productForm.legacyProductCode"
                  clearable
                  placeholder="例如 product_012"
                  :disabled="!productDialogEditable"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="中文名称" required>
                <el-input v-model="productForm.nameCn" :disabled="Boolean(productForm.productMasterId)" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="生命周期">
                <el-select v-model="productForm.lifecycleStage">
                  <el-option label="已注册" value="REGISTERED" />
                  <el-option label="研发中" value="R_AND_D" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="产品归属/类型" required>
                <el-input model-value="瑛泰医疗" disabled />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="在售国家">
                <el-input v-model="productForm.targetMarket" :rows="3" type="textarea" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="BU">
                <el-select
                  v-model="productForm.pipelineLayout"
                  clearable
                  filterable
                  placeholder="请选择BU"
                >
                  <el-option
                    v-for="item in SHOWROOM_PRODUCT_BU_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="适应症">
                <el-input v-model="productForm.indicationContent" :rows="3" type="textarea" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="卖点文案">
                <el-input v-model="productForm.coreSellingPoints" :rows="3" type="textarea" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="型号规格">
            <el-input v-model="productForm.modelSpecification" :rows="3" type="textarea" />
          </el-form-item>

          <el-form-item label="封面">
            <div class="showroom-admin-product-dialog__cover-row">
              <UploadImg
                v-model="productForm.coverImage"
                :limit="1"
                :is-show-tip="false"
                height="120px"
                width="120px"
              />
              <div v-if="!isShowroomScopedEditor" class="showroom-admin-product-dialog__cover-actions">
                <el-button
                  :loading="productCoverActionLoading"
                  :plain="!canGenerateProductAiCover"
                  :type="canGenerateProductAiCover ? 'primary' : 'default'"
                  @click="handleGenerateProductCoverImage"
                >
                  AI生成
                </el-button>
                <p class="showroom-admin-product-dialog__cover-tip">
                  仅产品基础信息审核通过后可生成 AI 封面
                </p>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="附件资料">
            <div class="showroom-admin-product-dialog__attachments">
              <div class="showroom-admin-product-dialog__attachment-toolbar">
                <el-radio-group
                  v-model="productAttachmentUploadType"
                  :disabled="!canEditProductDialog"
                  size="small"
                >
                  <el-radio-button label="image">图片</el-radio-button>
                  <el-radio-button label="video">视频</el-radio-button>
                  <el-radio-button label="text">文本</el-radio-button>
                </el-radio-group>
                <el-upload
                  :accept="productAttachmentAccept"
                  :disabled="!canEditProductDialog || productForm.attachments.length >= 20"
                  :http-request="handleProductAttachmentUpload"
                  :show-file-list="false"
                >
                  <el-button
                    :disabled="!canEditProductDialog || productForm.attachments.length >= 20"
                    :loading="productAttachmentUploading"
                    type="primary"
                  >
                    上传附件
                  </el-button>
                </el-upload>
                <span class="showroom-admin-product-dialog__attachment-tip">
                  图片 JPG/PNG/WEBP/GIF，视频 MP4/WEBM/MOV/AVI，文本 TXT/MD/PDF/DOC/DOCX，最多20个
                </span>
              </div>
              <el-table
                :data="productForm.attachments"
                border
                class="showroom-admin-product-dialog__attachment-table"
                empty-text="暂无附件"
                size="small"
              >
                <el-table-column label="类型" width="90">
                  <template #default="{ row }">
                    <el-tag size="small">{{ resolveProductAttachmentTypeLabel(row.assetType) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="文件名" min-width="260" prop="originalName" show-overflow-tooltip>
                  <template #default="{ row }">
                    <el-button
                      class="showroom-admin-product-dialog__attachment-link"
                      link
                      type="primary"
                      @click="handlePreviewProductAttachment(row)"
                    >
                      {{ row.originalName }}
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="大小" width="110">
                  <template #default="{ row }">{{ formatProductAttachmentSize(row.size) }}</template>
                </el-table-column>
                <el-table-column label="排序" width="100">
                  <template #default="{ $index }">{{ $index + 1 }}</template>
                </el-table-column>
                <el-table-column v-if="canEditProductDialog" label="操作" width="160" fixed="right">
                  <template #default="{ $index }">
                    <el-button
                      :disabled="$index === 0"
                      link
                      type="primary"
                      @click="handleMoveProductAttachment($index, -1)"
                    >
                      上移
                    </el-button>
                    <el-button
                      :disabled="$index === productForm.attachments.length - 1"
                      link
                      type="primary"
                      @click="handleMoveProductAttachment($index, 1)"
                    >
                      下移
                    </el-button>
                    <el-button link type="danger" @click="handleRemoveProductAttachment($index)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-form-item>

          <el-form-item label="中文讲解稿">
            <el-input
              v-model="productNarrationDraft.zhScriptText"
              :autosize="{ minRows: 6, maxRows: 10 }"
              :disabled="productNarrationLoading"
              placeholder="可直接编辑中文讲解稿，或点击生成讲解稿基于当前产品资料自动生成"
              type="textarea"
            />
          </el-form-item>

          <el-alert
            v-if="productNarrationLoadError"
            :closable="false"
            class="showroom-admin-product-dialog__narration-alert"
            show-icon
            type="warning"
            :title="productNarrationLoadError"
          />

          <div v-if="!isShowroomScopedEditor" class="showroom-admin-product-dialog__narration-toolbar">
            <el-button
              :loading="productNarrationActionLoading"
              @click="handleGenerateProductNarrationScript"
            >
              生成讲解稿
            </el-button>
          </div>

          <div class="showroom-admin-product-dialog__audio-grid">
            <div class="showroom-admin-product-dialog__audio-item">
              <span class="showroom-admin-product-dialog__audio-label">中文音频</span>
              <audio
                v-if="draftProductZhAudioUrl"
                :src="draftProductZhAudioUrl"
                class="showroom-admin-product-dialog__audio"
                controls
                preload="none"
              ></audio>
              <span v-else class="showroom-admin-product-dialog__audio-empty">未生成</span>
            </div>
          </div>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="部门主管">
                <el-input
                  :model-value="productApprovalRoutePreview?.supervisorName || ''"
                  disabled
                  placeholder="未解析到主管审批人"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="审批人" required>
                <el-input model-value="企宣角色" disabled />
              </el-form-item>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="English" name="en">
          <div class="showroom-admin-product-dialog__tab-toolbar">
            <div class="showroom-admin-product-dialog__tab-copy">
              <h4>English Content</h4>
              <p>
                Translate the current Chinese draft into English first, then refine the
                English description and narration manually.
              </p>
            </div>
            <el-button
              :disabled="!canTranslateProductEnglishFields"
              :loading="translatingProductEnglishFields"
              plain
              type="primary"
              @click="handleTranslateProductFieldsToEn"
            >
              AI Translate
            </el-button>
          </div>

          <el-form-item label="English Name" required>
            <el-input v-model="productForm.nameEn" :disabled="Boolean(productForm.productMasterId)" />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="Countries on Sale">
                <el-input v-model="productForm.targetMarketEn" :rows="3" type="textarea" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="BU">
                <el-input v-model="productForm.pipelineLayoutEn" :rows="3" type="textarea" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="Indication">
                <el-input v-model="productForm.indicationContentEn" :rows="3" type="textarea" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Selling Points Copy">
                <el-input v-model="productForm.coreSellingPointsEn" :rows="3" type="textarea" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="Model Specification">
            <el-input v-model="productForm.modelSpecificationEn" :rows="3" type="textarea" />
          </el-form-item>

          <el-form-item label="English Narration">
            <el-input
              v-model="productNarrationDraft.enScriptText"
              :autosize="{ minRows: 6, maxRows: 10 }"
              :disabled="productNarrationLoading"
              placeholder="Click AI Translate to prefill the English narration, then edit it before generating audio."
              type="textarea"
            />
          </el-form-item>

          <el-alert
            v-if="productNarrationDraftStale"
            :closable="false"
            class="showroom-admin-product-dialog__narration-alert"
            show-icon
            type="warning"
            title="The Chinese or English narration changed. Please generate the audio again."
          />

          <div class="showroom-admin-product-dialog__audio-grid">
            <div class="showroom-admin-product-dialog__audio-item">
              <span class="showroom-admin-product-dialog__audio-label">English Audio</span>
              <audio
                v-if="draftProductEnAudioUrl"
                :src="draftProductEnAudioUrl"
                class="showroom-admin-product-dialog__audio"
                controls
                preload="none"
              ></audio>
              <span v-else class="showroom-admin-product-dialog__audio-empty">Not generated</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-form>
    <template #footer>
      <el-button @click="productDialogVisible = false">
        {{ productDialogActiveLanguageTab === 'en' ? 'Cancel' : '取消' }}
      </el-button>
      <template v-if="isShowroomPublicity">
        <el-button type="primary" :loading="savingProduct" @click="handleSaveProduct">
          {{ productDialogActiveLanguageTab === 'en' ? 'Save' : '保存' }}
        </el-button>
      </template>
      <template v-else>
        <el-button :loading="savingProduct" @click="handleSaveProduct">
          {{ productDialogActiveLanguageTab === 'en' ? 'Save Draft' : '保存草稿' }}
        </el-button>
        <el-button
          type="primary"
          :disabled="!canSubmitProduct"
          :loading="submittingProduct"
          @click="handleSubmitProduct"
        >
          {{ productDialogActiveLanguageTab === 'en' ? 'Submit for Approval' : '提交审批' }}
        </el-button>
      </template>
    </template>
  </el-dialog>

  <ProductDetailDialog
    v-model="productDetailDialogVisible"
    :product-id="activeProductDetailId"
    :initial-revision-id="activeProductDetailRevisionId"
    :approval-change-request-id="productDetailApprovalChangeRequestId"
    :submit-route-preview="productApprovalRoutePreview"
    @open-version-center="handleOpenProductVersionCenterFromDetail"
    @saved="handleProductDetailSaved"
    @submitted="handleProductDetailSubmitted"
    @approval-completed="handleProductApprovalCompleted"
  />

  <el-dialog
    v-model="hallDialogVisible"
    :title="hallForm.hallId ? '编辑展柜' : '新增展柜'"
    width="560px"
  >
    <el-form :model="hallForm" label-width="100px">
      <el-form-item label="展柜编码" required>
        <el-input v-model="hallForm.hallCode" :disabled="Boolean(hallForm.hallId)" />
      </el-form-item>
      <el-form-item label="展柜名称" required>
        <el-input v-model="hallForm.name" />
      </el-form-item>
      <el-form-item label="英文名称" required>
        <el-input v-model="hallForm.nameEn" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="hallForm.description" :rows="4" type="textarea" />
      </el-form-item>
      <el-form-item label="英文描述">
        <el-input v-model="hallForm.descriptionEn" :rows="4" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="hallDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="savingHall" @click="handleSaveHall">保存</el-button>
    </template>
  </el-dialog>

  <HallProductMappingDialog
    v-model="hallMappingDialogVisible"
    :hall="activeHallMappingRecord"
    :products="productRows"
    @saved="handleHallMappingSaved"
  />

  <HallCanvasLayoutDialog
    v-model="hallCanvasDialogVisible"
    :hall="activeHallCanvasRecord"
    :products="productRows"
    @saved="handleHallCanvasSaved"
  />

  <ProductWholeAssignmentDialog
    v-model="productAssignmentDialogVisible"
    :product="activeProductAssignmentTarget"
    :user-options="showroomUserOptions"
    @saved="handleProductWholeAssignmentSaved"
  />

  <ProductAudioDialog
    v-model="productAudioDialogVisible"
    :generate-handler="handleGenerateProductNarrationAudioFromRow"
    :product-code="activeProductAudioTarget?.productCode"
    :product-id="activeProductAudioTarget?.productId"
    :product-name="activeProductAudioTarget?.nameCn"
    :source-revision-id="activeProductAudioTarget?.sourceRevisionId"
    @generated="handleProductAudioDialogGenerated"
  />

  <ShowroomProductImportForm ref="productImportFormRef" @success="handleProductImportSuccess" />

  <el-dialog v-model="awardDialogVisible" title="编辑奖项" width="860px">
    <el-form :model="awardForm" label-width="120px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="奖项编码">
            <el-input v-model="awardForm.awardCode" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="中文名称" required>
            <el-input v-model="awardForm.nameCn" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="英文名称">
            <el-input v-model="awardForm.nameEn" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="颁发单位">
            <el-input v-model="awardForm.issuer" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="日期/期限">
        <el-input v-model="awardForm.awardDateText" />
      </el-form-item>
      <el-form-item label="中文讲解">
        <el-input v-model="awardForm.descriptionZh" :rows="4" type="textarea" />
      </el-form-item>
      <el-form-item label="英文讲解">
        <el-input v-model="awardForm.descriptionEn" :rows="4" type="textarea" />
      </el-form-item>
      <el-form-item label="封面">
        <UploadImg
          v-model="awardForm.coverImage"
          :limit="1"
          :is-show-tip="false"
          height="120px"
          width="120px"
        />
      </el-form-item>
      <el-alert
        v-if="awardNarrationDraftStale"
        :closable="false"
        class="showroom-admin-product-dialog__narration-alert"
        show-icon
        title="奖项讲解内容已变更，请重新生成中英文语音。"
        type="warning"
      />
      <div class="showroom-admin-product-dialog__audio-grid">
        <div class="showroom-admin-product-dialog__audio-item">
          <span class="showroom-admin-product-dialog__audio-label">中文语音</span>
          <audio
            v-if="awardNarrationDraft.zhAudioUrl"
            :src="awardNarrationDraft.zhAudioUrl"
            class="showroom-admin-product-dialog__audio"
            controls
          ></audio>
          <span v-else-if="awardNarrationDraft.zhAudioFileId" class="showroom-admin-product-dialog__audio-ready">
            已生成
          </span>
          <span v-else class="showroom-admin-product-dialog__audio-empty">未生成</span>
        </div>
        <div class="showroom-admin-product-dialog__audio-item">
          <span class="showroom-admin-product-dialog__audio-label">英文语音</span>
          <audio
            v-if="awardNarrationDraft.enAudioUrl"
            :src="awardNarrationDraft.enAudioUrl"
            class="showroom-admin-product-dialog__audio"
            controls
          ></audio>
          <span v-else-if="awardNarrationDraft.enAudioFileId" class="showroom-admin-product-dialog__audio-ready">
            已生成
          </span>
          <span v-else class="showroom-admin-product-dialog__audio-empty">未生成</span>
        </div>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="awardDialogVisible = false">取消</el-button>
      <el-button :loading="savingAward" @click="handleSaveAwardDraft">保存草稿</el-button>
      <el-button :loading="generatingAwardAudio" @click="handleGenerateAwardNarrationAudio">
        生成中英文语音
      </el-button>
      <el-button type="primary" :loading="savingAward" @click="handlePublishAward">保存并发布</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="batchMediaSummaryDialogVisible" :title="batchMediaSummaryTitle" width="780px">
    <template v-if="batchMediaSummaryResult">
      <el-descriptions :column="2" border class="showroom-admin-batch-summary__stats">
        <el-descriptions-item label="命中产品">{{ batchMediaSummaryResult.matchedCount }}</el-descriptions-item>
        <el-descriptions-item :label="resolveBatchSummaryPublishedLabel(batchMediaSummaryKind)">
          {{ batchMediaSummaryResult.publishedCount }}
        </el-descriptions-item>
        <el-descriptions-item :label="resolveBatchSummarySkippedLabel(batchMediaSummaryKind)">
          {{ batchMediaSummaryResult.skippedUnpublishedCount }}
        </el-descriptions-item>
        <el-descriptions-item v-if="batchMediaSummaryKind === 'COVER'" label="跳过已有封面">
          {{ batchMediaSummaryResult.skippedExistingCount }}
        </el-descriptions-item>
        <el-descriptions-item v-if="batchMediaSummaryKind === 'AUDIO'" label="跳过已有语音">
          {{ batchMediaSummaryResult.skippedExistingCount }}
        </el-descriptions-item>
        <el-descriptions-item v-if="batchMediaSummaryKind === 'AUDIO'" label="跳过缺讲解稿">
          {{ batchMediaSummaryResult.skippedMissingScriptCount }}
        </el-descriptions-item>
        <el-descriptions-item :label="resolveBatchSummarySucceededLabel(batchMediaSummaryKind)">
          {{ batchMediaSummaryResult.succeededCount }}
        </el-descriptions-item>
        <el-descriptions-item label="失败数量">{{ batchMediaSummaryResult.failedCount }}</el-descriptions-item>
        <el-descriptions-item v-if="batchMediaSummaryKind === 'AUDIO'" label="定时检查">
          {{ batchMediaSummaryResult.autoCheckEnabled ? '定时检查中' : '已停止' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="batchMediaSummaryKind === 'AUDIO'" label="剩余待处理">
          {{ batchMediaSummaryResult.remainingActionableCount }}
        </el-descriptions-item>
        <el-descriptions-item v-if="batchMediaSummaryKind === 'COVER' && batchMediaSummaryResult.taskId" label="任务编号">
          {{ batchMediaSummaryResult.taskId }}
        </el-descriptions-item>
        <el-descriptions-item v-if="batchMediaSummaryKind === 'COVER' && batchMediaSummaryResult.taskId" label="任务状态">
          {{ resolveBatchCoverTaskStatusText(batchMediaSummaryResult.taskStatus) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="batchMediaSummaryKind === 'COVER' && batchMediaSummaryResult.taskId" label="剩余未完成">
          {{ batchMediaSummaryResult.remainingPendingCount }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="batchMediaSummaryKind === 'COVER' && batchMediaSummaryResult.taskId"
          label="下一次检查时间"
        >
          {{ batchMediaSummaryResult.nextCheckAt || '待计算' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ resolveBatchSummaryStatusText(batchMediaSummaryKind, batchMediaSummaryResult) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="batchMediaSummaryKind === 'COVER' && shouldShowBatchCoverAutoResumeHint(batchMediaSummaryResult)"
        :closable="false"
        class="showroom-admin-batch-summary__alert"
        show-icon
        title="已开启后台定时检查，每 10 分钟自动续跑，全部完成后自动停止"
        type="warning"
      />

      <el-alert
        v-else-if="batchMediaSummaryResult.failedCount === 0"
        :closable="false"
        class="showroom-admin-batch-summary__alert"
        show-icon
        :title="resolveBatchSummarySuccessTitle(batchMediaSummaryKind, batchMediaSummaryResult)"
        type="success"
      />

      <template v-else>
        <el-alert
          :closable="false"
          class="showroom-admin-batch-summary__alert"
          show-icon
          :title="resolveBatchSummaryFailureTitle(batchMediaSummaryKind)"
          type="warning"
        />
        <el-table :data="batchMediaSummaryResult.failures" border class="showroom-admin-batch-summary__table">
          <el-table-column label="产品ID" prop="productId" width="100" />
          <el-table-column label="产品编码" prop="productCode" min-width="160" show-overflow-tooltip />
          <el-table-column label="中文名称" prop="nameCn" min-width="180" show-overflow-tooltip />
          <el-table-column label="失败原因" prop="reason" min-width="260" show-overflow-tooltip />
        </el-table>
      </template>
    </template>
    <template #footer>
      <el-button type="primary" @click="batchMediaSummaryDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {
  ShowroomAdminApi,
  type ShowroomProductCoverGenerationMode,
  type ShowroomProductBatchGenerateRespVO,
  type ShowroomProductSalesCountriesBatchGenerateRespVO,
  type ShowroomProductCoverBatchTaskStateRespVO,
  type ShowroomProductTranslatePublishBatchTaskRespVO,
  type ShowroomProductNarrationScriptTaskCurrentProductRespVO,
  type ShowroomProductNarrationScriptTaskRespVO,
  type ShowroomProductBatchGenerateStateRespVO,
  type ShowroomProductAttachment,
  type ShowroomProductAttachmentAssetType,
  type ShowroomAwardPageRowRespVO
} from '@/api/showroom-admin'
import {
  getProductSimpleList,
  MDM_PRODUCT_STATUS_ENABLE,
  type MdmProductSimpleRespVO
} from '@/api/mdm/product'
import * as DeptApi from '@/api/system/dept'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
import download from '@/utils/download'
import { ElMessageBox } from 'element-plus'
import ApprovalTaskPanel from '@/views/showroom-admin/approval/ApprovalTaskPanel.vue'
import AssignmentWorkbench from '@/views/showroom-admin/assignment/AssignmentWorkbench.vue'
import { CompanyWorkbench } from '@/views/showroom-admin/company'
import { CompanyVersionWorkbench } from '@/views/showroom-admin/company-version'
import HallProductMappingDialog from '@/views/showroom-admin/components/HallProductMappingDialog.vue'
import HallCanvasLayoutDialog from '@/views/showroom-admin/components/HallCanvasLayoutDialog.vue'
import HallListTable from '@/views/showroom-admin/components/HallListTable.vue'
import DiscussionWorkbench from '@/views/showroom-admin/discussion/DiscussionWorkbench.vue'
import { CompanyHistoryWorkbench } from '@/views/showroom-admin/history'
import NarrationWorkspace from '@/views/showroom-admin/narration/NarrationWorkspace.vue'
import { PromptWorkbench } from '@/views/showroom-admin/prompt'
import KeywordWorkbench from './keyword/KeywordWorkbench.vue'
import {
  normalizeNarrationVersion,
  type ShowroomNarrationVersionRecord
} from '@/views/showroom-admin/narration/contracts'
import ProductListTable from '@/views/showroom-admin/components/ProductListTable.vue'
import AwardListTable from '@/views/showroom-admin/components/AwardListTable.vue'
import ProductAudioDialog from '@/views/showroom-admin/product/ProductAudioDialog.vue'
import ProductDetailDialog from '@/views/showroom-admin/product/ProductDetailDialog.vue'
import ShowroomProductImportForm from '@/views/showroom-admin/product/ShowroomProductImportForm.vue'
import ProductWholeAssignmentDialog from '@/views/showroom-admin/product/ProductWholeAssignmentDialog.vue'
import {
  buildShowroomCompanyOptions,
  normalizeProductDetail,
  SHOWROOM_PRODUCT_BU_OPTIONS,
  productTranslatableFieldDefinitions,
  resolveShowroomApprovalRoutePreview,
  SHOWROOM_PRODUCT_OWNER_LABEL,
  type ProductAssignmentUserOption,
  type ShowroomApprovalRoutePreview,
  type ShowroomCompanyOption,
  type ShowroomProductDetail
} from '@/views/showroom-admin/product/contracts'
import { formatShowroomStructuredError } from '@/views/showroom-admin/shared/structuredError'

defineOptions({ name: 'ShowroomAdminWorkspace' })

interface ShowroomAdminRow {
  code: string
  name: string
  status: string
  tagType: 'success' | 'warning' | 'info' | 'danger'
  description: string
  action: string
}

interface ProductForm {
  productId?: number
  productMasterId: number | null
  productCode: string
  legacyProductCode: string
  nameCn: string
  nameEn: string
  ownerCompanyId: number | null
  productOwnerType: 'YINGTAI' | 'SUBSIDIARY'
  lifecycleStage: 'REGISTERED' | 'R_AND_D'
  targetMarket: string
  targetMarketEn: string
  pipelineLayout: string
  pipelineLayoutEn: string
  indicationContent: string
  indicationContentEn: string
  coreSellingPoints: string
  coreSellingPointsEn: string
  modelSpecification: string
  modelSpecificationEn: string
  registrationCertificate: string
  registrationCertificateEn: string
  clinicalEffect: string
  clinicalEffectEn: string
  fimStatus: string
  fimStatusEn: string
  coverImage: string
  attachments: ShowroomProductAttachment[]
}

interface HallForm {
  hallId?: number
  hallCode: string
  name: string
  nameEn: string
  description: string
  descriptionEn: string
}

interface ProductSubmitBaseline {
  status: string
  productMasterId: number | null
  productCode: string
  nameCn: string
  nameEn: string
  fields: Record<string, string>
  attachments: ShowroomProductAttachment[]
}

interface ProductListFilters {
  productId?: number | null
  keyword: string
  lifecycleStage: string
  incompleteStatus: string
  approvalStatus: string
}

interface BatchMediaSummaryFailure {
  productId: number
  productCode: string
  nameCn: string
  reason: string
}

interface BatchMediaSummaryResult extends ShowroomProductBatchGenerateRespVO {
  failures: BatchMediaSummaryFailure[]
}

interface ProductSalesCountriesBatchGenerateResult extends ShowroomProductSalesCountriesBatchGenerateRespVO {
  failures: BatchMediaSummaryFailure[]
}

interface CoverTaskBannerSummary extends ShowroomProductCoverBatchTaskStateRespVO {
  failures: BatchMediaSummaryFailure[]
  updatedAt: number
}

type BatchMediaSummaryKind = 'AUDIO' | 'COVER' | 'PUBLISH'

const message = useMessage()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const SHOWROOM_NOTIFY_PRODUCT_TARGET_KEY = 'showroomNotifyProductTarget'
const SHOWROOM_EDITOR_ROLE_CODE = 'EDITOR'
const SHOWROOM_DEPARTMENT_SUPERVISOR_ROLE_CODE = 'DEPARTMENT_SUPERVISOR'
const SHOWROOM_PUBLICITY_ROLE_CODE = 'showroom_publicity'
const SUPER_ADMIN_ROLE_CODE = 'super_admin'
const PRODUCT_AI_COVER_APPROVED_STATUSES = new Set(['APPROVED', 'PUBLISHED'])
const PRODUCT_DIRECT_PUBLISHABLE_STATUSES = new Set(['DRAFT', 'REJECTED'])
const PRODUCT_AI_COVER_GATE_MESSAGE = '需要产品基础信息经过审核之后才可以AI生成封面'
const PRODUCT_AI_COVER_PENDING_MESSAGE = 'AI封面生成中，请稍候'
const PRODUCT_AI_COVER_LOADING_MESSAGE = 'AI封面仍在生成中，请稍候'
const adminLoading = ref(false)
const adminLoadError = ref('')
const companyCurrent = ref<Record<string, unknown> | null>(null)
const productCompanyOptions = ref<ShowroomCompanyOption[]>([])
const productMasterOptions = ref<MdmProductSimpleRespVO[]>([])
const productMasterOptionsLoading = ref(false)
const productApprovalRoutePreview = ref<ShowroomApprovalRoutePreview | null>(null)
const productSubmitBaseline = ref<ProductSubmitBaseline | null>(null)
const productFilters = ref<ProductListFilters>({
  productId: null,
  keyword: '',
  lifecycleStage: '',
  incompleteStatus: '',
  approvalStatus: ''
})
const productPageNo = ref(1)
const productPageSize = ref(20)
const productPageTotal = ref(0)
const productRows = ref<unknown[]>([])
const productManageTab = ref<'product' | 'award'>('product')
const awardKeyword = ref('')
const awardPageNo = ref(1)
const awardPageSize = ref(20)
const awardPageTotal = ref(0)
const awardRows = ref<ShowroomAwardPageRowRespVO[]>([])
const awardDialogVisible = ref(false)
const savingAward = ref(false)
const generatingCoverAwardId = ref<number | null>(null)
const generatingAwardAudio = ref(false)
const awardForm = reactive({
  awardId: 0,
  awardCode: '',
  nameCn: '',
  nameEn: '',
  descriptionZh: '',
  descriptionEn: '',
  issuer: '',
  awardDateText: '',
  coverImage: ''
})
const awardNarrationBaselineZhScript = ref('')
const awardNarrationBaselineEnScript = ref('')
const awardNarrationDraft = reactive({
  sourceRevisionId: null as number | null,
  zhVersionId: null as number | null,
  enVersionId: null as number | null,
  zhScriptText: '',
  enScriptText: '',
  zhAudioFileId: null as number | null,
  enAudioFileId: null as number | null,
  zhAudioUrl: '',
  enAudioUrl: '',
  zhAudioDurationSeconds: null as number | null,
  enAudioDurationSeconds: null as number | null,
  voice: ''
})
const showroomUserOptions = ref<ProductAssignmentUserOption[]>([])
const hallRows = ref<unknown[]>([])
const rowsBySection = ref<Record<string, ShowroomAdminRow[]>>({})
const hallKeyword = ref('')
const productDialogVisible = ref(false)
const productDialogEditable = ref(true)
const productDetailDialogVisible = ref(false)
const productAssignmentDialogVisible = ref(false)
const productAudioDialogVisible = ref(false)
const activeProductDetailId = ref<number | null>(null)
const activeProductDetailRevisionId = ref<number | null>(null)
const productDetailApprovalChangeRequestId = ref<number | null>(null)
const activeProductAssignmentTarget = ref<{
  productId: number
  productCode: string
  nameCn: string
} | null>(null)
const activeProductAudioTarget = ref<{
  productId: number
  productCode: string
  nameCn: string
  sourceRevisionId: number
} | null>(null)
const hallDialogVisible = ref(false)
const hallMappingDialogVisible = ref(false)
const hallCanvasDialogVisible = ref(false)
const activeHallMappingRecord = ref<unknown | null>(null)
const activeHallCanvasRecord = ref<unknown | null>(null)
const publishingProductId = ref<number | null>(null)
const batchGeneratingProductSalesCountries = ref(false)
const batchGeneratingProductNarrationScript = ref(false)
const batchPublishingProduct = ref(false)
const batchGeneratingProductAudio = ref(false)
const batchGeneratingHallAudio = ref(false)
const exportingHallConfigPackage = ref(false)
const importingHallConfigPackage = ref(false)
const batchProductAudioAutoCheckState = ref<ShowroomProductBatchGenerateStateRespVO | null>(null)
const productNarrationScriptTaskStatus = ref<ShowroomProductNarrationScriptTaskRespVO | null>(null)
let productNarrationScriptTaskPollingTimer: number | undefined
const batchGeneratingProductCover = ref(false)
const latestProductCoverTaskSummary = ref<CoverTaskBannerSummary | null>(null)
let productCoverTaskPollingTimer: number | undefined
const batchTranslatingPublishingProduct = ref(false)
const productTranslatePublishTaskStatus = ref<ShowroomProductTranslatePublishBatchTaskRespVO | null>(null)
let productTranslatePublishTaskPollingTimer: number | undefined
const exportingProductExcel = ref(false)
const importingProductExcel = ref(false)
const batchMediaSummaryDialogVisible = ref(false)
const batchMediaSummaryTitle = ref('')
const batchMediaSummaryResult = ref<BatchMediaSummaryResult | null>(null)
const batchMediaSummaryKind = ref<BatchMediaSummaryKind>('AUDIO')
const savingProduct = ref(false)
const submittingProduct = ref(false)
const savingHall = ref(false)
const productNarrationLoading = ref(false)
const productNarrationActionLoading = ref(false)
const generatingAudioHallId = ref<number | null>(null)
const publishingHallPreviewAssetId = ref<number | null>(null)
const productCoverActionLoading = ref(false)
const translatingProductEnglishFields = ref(false)
const productNarrationLoadError = ref('')
const productNarrationBaselineZhScript = ref('')
const productNarrationBaselineEnScript = ref('')
const consumedNotifyProductRouteKey = ref('')
const productImportFormRef = ref()
const productDialogActiveLanguageTab = ref<'zh' | 'en'>('zh')
const productAttachmentUploadType = ref<ShowroomProductAttachmentAssetType>('image')
const productAttachmentUploading = ref(false)
const isShowroomPublicity = computed(() => {
  return userStore.getRoles.includes(SHOWROOM_PUBLICITY_ROLE_CODE)
})
const canManageHallConfigPackage = computed(() => {
  const roles = userStore.getRoles
  return roles.includes(SHOWROOM_PUBLICITY_ROLE_CODE) || roles.includes(SUPER_ADMIN_ROLE_CODE)
})
const canEditProductDialog = computed(() => productDialogEditable.value)
const productAttachmentAccept = computed(() => {
  if (productAttachmentUploadType.value === 'image') {
    return '.jpg,.jpeg,.png,.webp,.gif'
  }
  if (productAttachmentUploadType.value === 'video') {
    return '.mp4,.webm,.mov,.avi'
  }
  return '.txt,.md,.pdf,.doc,.docx'
})

const resolveProductAttachmentTypeLabel = (assetType: string) => {
  if (assetType === 'image') {
    return '图片'
  }
  if (assetType === 'video') {
    return '视频'
  }
  return '文本'
}

const formatProductAttachmentSize = (size: number) => {
  if (size >= 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(1)} MB`
  }
  if (size >= 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${size} B`
}

const resolveProductAttachmentFileUrl = (attachment: ShowroomProductAttachment) => {
  const url = resolveStringValue(attachment.url).trim()
  if (!url) {
    throw new Error('附件访问地址缺失，无法预览文件')
  }
  return url
}

const handlePreviewProductAttachment = (attachment: ShowroomProductAttachment) => {
  try {
    const url = resolveProductAttachmentFileUrl(attachment)
    window.open(url, '_blank', 'noopener')
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  }
}

const handleProductAttachmentUpload = async (options: {
  file: File
  onSuccess?: (response: unknown) => void
  onError?: (error: Error) => void
}) => {
  if (!canEditProductDialog.value) {
    const error = new Error('当前产品不可编辑附件')
    options.onError?.(error)
    message.error(error.message)
    throw error
  }
  if (productForm.attachments.length >= 20) {
    const error = new Error('单个产品最多添加20个附件')
    options.onError?.(error)
    message.error(error.message)
    throw error
  }
  productAttachmentUploading.value = true
  try {
    const formData = new FormData()
    if (productForm.productId) {
      formData.append('productId', String(productForm.productId))
    }
    formData.append('assetType', productAttachmentUploadType.value)
    formData.append('file', options.file)
    const uploaded = await ShowroomAdminApi.uploadProductAttachment(formData)
    productForm.attachments.push({
      assetType: uploaded.assetType,
      fileId: uploaded.fileId,
      url: uploaded.url,
      originalName: uploaded.originalName,
      mimeType: uploaded.mimeType,
      size: uploaded.size,
      displayOrder: productForm.attachments.length + 1
    })
    options.onSuccess?.(uploaded)
    message.success('附件已上传')
  } catch (error) {
    const resolved = resolveError(error)
    options.onError?.(resolved)
    message.error(resolved.message)
    throw resolved
  } finally {
    productAttachmentUploading.value = false
  }
}

const handleMoveProductAttachment = (index: number, offset: number) => {
  const nextIndex = index + offset
  if (nextIndex < 0 || nextIndex >= productForm.attachments.length) {
    return
  }
  const next = [...productForm.attachments]
  const current = next[index]
  next[index] = next[nextIndex]
  next[nextIndex] = current
  productForm.attachments = next.map((attachment, orderIndex) => ({
    ...attachment,
    displayOrder: orderIndex + 1
  }))
}

const handleRemoveProductAttachment = (index: number) => {
  productForm.attachments = productForm.attachments
    .filter((_, currentIndex) => currentIndex !== index)
    .map((attachment, orderIndex) => ({ ...attachment, displayOrder: orderIndex + 1 }))
}
const isShowroomScopedEditor = computed(() => {
  const roles = userStore.getRoles
  return (
    roles.includes(SHOWROOM_EDITOR_ROLE_CODE) &&
    !roles.includes(SHOWROOM_DEPARTMENT_SUPERVISOR_ROLE_CODE) &&
    !roles.includes(SHOWROOM_PUBLICITY_ROLE_CODE)
  )
})
const batchProductAudioAutoCheckLabel = computed(() => {
  const state = batchProductAudioAutoCheckState.value
  if (!state) {
    return ''
  }
  const hasHistory =
    Boolean(state.lastRunAt) ||
    state.matchedCount > 0 ||
    Boolean(state.keyword || state.lifecycleStage || state.incompleteStatus || state.approvalStatus)
  if (!hasHistory) {
    return ''
  }
  if (state.enabled) {
    return state.remainingActionableCount > 0
      ? `定时检查中（剩${state.remainingActionableCount}）`
      : '定时检查中'
  }
  return '定时检查已停止'
})

const createEmptyProductForm = (): ProductForm => ({
  productMasterId: null,
  productCode: '',
  legacyProductCode: '',
  nameCn: '',
  nameEn: '',
  ownerCompanyId: null,
  productOwnerType: 'YINGTAI',
  lifecycleStage: 'REGISTERED',
  targetMarket: '',
  targetMarketEn: '',
  pipelineLayout: '',
  pipelineLayoutEn: '',
  indicationContent: '',
  indicationContentEn: '',
  coreSellingPoints: '',
  coreSellingPointsEn: '',
  modelSpecification: '',
  modelSpecificationEn: '',
  registrationCertificate: '',
  registrationCertificateEn: '',
  clinicalEffect: '',
  clinicalEffectEn: '',
  fimStatus: '',
  fimStatusEn: '',
  coverImage: '',
  attachments: []
})

const createEmptyHallForm = (): HallForm => ({
  hallCode: '',
  name: '',
  nameEn: '',
  description: '',
  descriptionEn: ''
})

const productForm = reactive<ProductForm>(createEmptyProductForm())
const hallForm = reactive<HallForm>(createEmptyHallForm())
const productNarrationDraft = reactive<{
  sourceRevisionId: number | null
  zhVersionId: number | null
  enVersionId: number | null
  zhVersionNo: number | null
  enVersionNo: number | null
  zhScriptText: string
  enScriptText: string
  zhStatus: string
  enStatus: string
  zhGenerationStatus: string
  enGenerationStatus: string
  zhGeneratedByAi: boolean
  enGeneratedByAi: boolean
  zhAudioFileId: number | null
  enAudioFileId: number | null
  zhAudioUrl: string
  enAudioUrl: string
  zhAudioDurationSeconds: number | null
  enAudioDurationSeconds: number | null
  voice: string
}>({
  sourceRevisionId: null,
  zhVersionId: null,
  enVersionId: null,
  zhVersionNo: null,
  enVersionNo: null,
  zhScriptText: '',
  enScriptText: '',
  zhStatus: 'DRAFT',
  enStatus: 'DRAFT',
  zhGenerationStatus: 'NOT_GENERATED',
  enGenerationStatus: 'NOT_GENERATED',
  zhGeneratedByAi: false,
  enGeneratedByAi: false,
  zhAudioFileId: null,
  enAudioFileId: null,
  zhAudioUrl: '',
  enAudioUrl: '',
  zhAudioDurationSeconds: null,
  enAudioDurationSeconds: null,
  voice: ''
})
const handlingNotifyProductRoute = ref(false)

const showroomAdminSections = [
  { name: 'company', routeName: 'ShowroomAdminCompany' },
  { name: 'companyVersion', routeName: 'ShowroomAdminCompanyVersion' },
  { name: 'product', routeName: 'ShowroomAdminProduct' },
  { name: 'keyword', routeName: 'ShowroomAdminKeyword' },
  { name: 'prompt', routeName: 'ShowroomAdminPrompt' },
  { name: 'hall', routeName: 'ShowroomAdminHall' },
  { name: 'approval', routeName: 'ShowroomAdminApproval' },
  { name: 'history', routeName: 'ShowroomAdminHistory' },
  { name: 'assignment', routeName: 'ShowroomAdminAssignment' },
  { name: 'discussion', routeName: 'ShowroomAdminDiscussion' },
  { name: 'narration', routeName: 'ShowroomAdminNarration' }
] as const

const activeSection = computed(() => {
  const routeName = String(route.name || '')
  return showroomAdminSections.find((section) => section.routeName === routeName)?.name || ''
})

const shouldLoadProductRows = computed(() => {
  return ['product', 'assignment', 'discussion', 'narration'].includes(activeSection.value)
})

const shouldLoadHallRows = computed(() => {
  return ['hall', 'narration'].includes(activeSection.value)
})

const shouldLoadCompanyCurrent = computed(() => {
  return ['product', 'assignment', 'narration'].includes(activeSection.value)
})

const activeRows = computed(() => rowsBySection.value[activeSection.value] || [])

const normalizeArray = (value: unknown, fieldName: string): unknown[] => {
  if (!Array.isArray(value)) {
    throw new Error(`展柜接口缺少列表字段：${fieldName}`)
  }
  return value
}

const normalizeObject = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`展柜接口缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const normalizeId = (value: unknown, fieldName: string): number => {
  const id = typeof value === 'string' ? Number(value) : value
  if (typeof id !== 'number' || !Number.isFinite(id)) {
    throw new Error(`展柜接口缺少数值字段：${fieldName}`)
  }
  return id
}

const normalizeTotal = (value: unknown, fieldName: string): number => {
  const total = typeof value === 'string' ? Number(value) : value
  if (typeof total !== 'number' || !Number.isFinite(total) || total < 0) {
    throw new Error(`展柜接口缺少总数字段：${fieldName}`)
  }
  return total
}

const normalizeOptionalTotal = (value: unknown, fieldName: string): number => {
  if (value === undefined || value === null || value === '') {
    return 0
  }
  return normalizeTotal(value, fieldName)
}

const normalizeOptionalBoolean = (value: unknown): boolean => {
  if (value === undefined || value === null || value === '') {
    return false
  }
  if (value === true || value === 'true' || value === 1 || value === '1') {
    return true
  }
  if (value === false || value === 'false' || value === 0 || value === '0') {
    return false
  }
  throw new Error('展柜接口缺少布尔字段')
}

const normalizeAwardRows = (rows: unknown[]): ShowroomAwardPageRowRespVO[] => {
  return rows.map((item, index) => {
    const award = normalizeObject(item, `awardPage.list[${index}]`)
    const revisionSource =
      award.displayRevision && typeof award.displayRevision === 'object'
        ? award.displayRevision
        : award.revision && typeof award.revision === 'object'
          ? award.revision
          : award
    const revision = normalizeObject(revisionSource, `awardPage.list[${index}].displayRevision`)
    return {
      awardId: normalizeId(award.awardId, `awardPage.list[${index}].awardId`),
      awardCode: resolveStringValue(award.awardCode).trim(),
      nameCn: resolveStringValue(revision.nameCn).trim(),
      nameEn: resolveStringValue(revision.nameEn).trim(),
      issuer: resolveStringValue(revision.issuer).trim(),
      awardDateText: resolveStringValue(revision.awardDateText).trim(),
      coverImageUrl: resolveStringValue(
        revision.coverImageUrl ?? revision.coverImage ?? revision.cover_image
      ).trim(),
      incomplete: normalizeOptionalBoolean(award.incomplete ?? revision.incomplete),
      revisionNo: normalizeTotal(revision.revisionNo, `awardPage.list[${index}].revisionNo`)
    }
  })
}

const normalizeBatchMediaSummaryResult = (value: unknown, fieldName: string): BatchMediaSummaryResult => {
  const result = normalizeObject(value, fieldName)
  const failures = normalizeArray(result.failures, `${fieldName}.failures`).map((item, index) => {
    const failure = normalizeObject(item, `${fieldName}.failures[${index}]`)
    return {
      productId: normalizeId(failure.productId, `${fieldName}.failures[${index}].productId`),
      productCode: resolveStringValue(failure.productCode).trim(),
      nameCn: resolveStringValue(failure.nameCn).trim(),
      reason: resolveStringValue(failure.reason).trim() || '未知失败'
    }
  })
  return {
    matchedCount: normalizeTotal(result.matchedCount, `${fieldName}.matchedCount`),
    publishedCount: normalizeTotal(result.publishedCount, `${fieldName}.publishedCount`),
    skippedUnpublishedCount: normalizeTotal(
      result.skippedUnpublishedCount,
      `${fieldName}.skippedUnpublishedCount`
    ),
    skippedExistingCount: normalizeOptionalTotal(
      result.skippedExistingCount,
      `${fieldName}.skippedExistingCount`
    ),
    skippedMissingScriptCount: normalizeOptionalTotal(
      result.skippedMissingScriptCount,
      `${fieldName}.skippedMissingScriptCount`
    ),
    succeededCount: normalizeTotal(result.succeededCount, `${fieldName}.succeededCount`),
    failedCount: normalizeTotal(result.failedCount, `${fieldName}.failedCount`),
    autoCheckEnabled: normalizeOptionalBoolean(result.autoCheckEnabled),
    remainingActionableCount: normalizeOptionalTotal(
      result.remainingActionableCount,
      `${fieldName}.remainingActionableCount`
    ),
    taskId:
      result.taskId === undefined || result.taskId === null || result.taskId === ''
        ? null
        : normalizeId(result.taskId, `${fieldName}.taskId`),
    taskStatus: resolveStringValue(result.taskStatus).trim(),
    remainingPendingCount: normalizeOptionalTotal(
      result.remainingPendingCount,
      `${fieldName}.remainingPendingCount`
    ),
    nextCheckAt: resolveStringValue(result.nextCheckAt).trim() || null,
    failures
  }
}

const normalizeHallNarrationBatchGenerateResult = (value: unknown, fieldName: string) => {
  const result = normalizeObject(value, fieldName)
  const failures = normalizeArray(result.failures, `${fieldName}.failures`).map((item, index) => {
    const failure = normalizeObject(item, `${fieldName}.failures[${index}]`)
    return {
      hallId: normalizeId(failure.hallId, `${fieldName}.failures[${index}].hallId`),
      hallCode: resolveStringValue(failure.hallCode).trim(),
      name: resolveStringValue(failure.name).trim(),
      reason: resolveStringValue(failure.reason).trim() || '未知失败'
    }
  })
  return {
    matchedCount: normalizeOptionalTotal(result.matchedCount, `${fieldName}.matchedCount`),
    succeededCount: normalizeOptionalTotal(result.succeededCount, `${fieldName}.succeededCount`),
    failedCount: normalizeOptionalTotal(result.failedCount, `${fieldName}.failedCount`),
    failures
  }
}

const normalizeProductSalesCountriesBatchGenerateResult = (
  value: unknown,
  fieldName: string
): ProductSalesCountriesBatchGenerateResult => {
  const result = normalizeObject(value, fieldName)
  const failures = normalizeArray(result.failures, `${fieldName}.failures`).map((item, index) => {
    const failure = normalizeObject(item, `${fieldName}.failures[${index}]`)
    return {
      productId: normalizeId(failure.productId, `${fieldName}.failures[${index}].productId`),
      productCode: resolveStringValue(failure.productCode).trim(),
      nameCn: resolveStringValue(failure.nameCn).trim(),
      reason: resolveStringValue(failure.reason).trim() || '未知失败'
    }
  })
  return {
    matchedCount: normalizeTotal(result.matchedCount, `${fieldName}.matchedCount`),
    skippedCompletedCount: normalizeTotal(
      result.skippedCompletedCount,
      `${fieldName}.skippedCompletedCount`
    ),
    updatedProductCount: normalizeTotal(result.updatedProductCount, `${fieldName}.updatedProductCount`),
    generatedLanguageCount: normalizeTotal(
      result.generatedLanguageCount,
      `${fieldName}.generatedLanguageCount`
    ),
    failedCount: normalizeTotal(result.failedCount, `${fieldName}.failedCount`),
    failures
  }
}

const resolveBatchCoverTaskStatusText = (status: string | null | undefined) => {
  const normalized = resolveStringValue(status).trim().toUpperCase()
  if (normalized === 'WAITING') {
    return '等待下一轮检查'
  }
  if (normalized === 'RUNNING') {
    return '执行中'
  }
  if (normalized === 'COMPLETED') {
    return '已完成'
  }
  return normalized || '未返回'
}

const shouldShowBatchCoverAutoResumeHint = (result: BatchMediaSummaryResult) => {
  return (
    resolveStringValue(result.taskStatus).trim().toUpperCase() === 'WAITING' &&
    (result.remainingPendingCount || 0) > 0
  )
}

const resolveBatchSummaryPublishedLabel = (kind: BatchMediaSummaryKind) => {
  return kind === 'PUBLISH' ? '可直接发布' : '已发布产品'
}

const resolveBatchSummarySkippedLabel = (kind: BatchMediaSummaryKind) => {
  return kind === 'PUBLISH' ? '自动跳过不可直发' : '自动跳过未发布'
}

const resolveBatchSummarySucceededLabel = (kind: BatchMediaSummaryKind) => {
  return kind === 'PUBLISH' ? '成功发布' : '成功生成'
}

const resolveBatchSummaryStatusText = (
  kind: BatchMediaSummaryKind,
  result: BatchMediaSummaryResult
) => {
  if (kind === 'COVER' && shouldShowBatchCoverAutoResumeHint(result)) {
    return '后台续跑中'
  }
  if (result.failedCount > 0) {
    return '部分失败'
  }
  if (kind === 'AUDIO' && result.autoCheckEnabled) {
    return '已开启续跑'
  }
  return '全部完成'
}

const resolveBatchSummarySuccessTitle = (
  kind: BatchMediaSummaryKind,
  result: BatchMediaSummaryResult
) => {
  if (kind === 'AUDIO' && result.autoCheckEnabled) {
    return '本次首轮批量处理已完成，系统将每 10 分钟继续检查未补齐语音的产品。'
  }
  if (kind === 'PUBLISH' && result.matchedCount === 0) {
    return '当前筛选范围没有命中产品。'
  }
  return kind === 'PUBLISH' ? '本次批量发布已完成，未发现失败产品。' : '本次批量处理已完成，未发现失败产品。'
}

const resolveBatchSummaryFailureTitle = (kind: BatchMediaSummaryKind) => {
  return kind === 'PUBLISH' ? '以下产品发布失败，其余成功项已继续执行。' : '以下已发布产品处理失败，其余成功项已继续执行。'
}

const normalizeBatchProductAudioAutoCheckState = (
  value: unknown,
  fieldName: string
): ShowroomProductBatchGenerateStateRespVO => {
  const state = normalizeObject(value, fieldName)
  return {
    enabled: normalizeOptionalBoolean(state.enabled),
    keyword: resolveStringValue(state.keyword).trim(),
    lifecycleStage: resolveStringValue(state.lifecycleStage).trim(),
    incompleteStatus: resolveStringValue(state.incompleteStatus).trim(),
    approvalStatus: resolveStringValue(state.approvalStatus).trim(),
    matchedCount: normalizeOptionalTotal(state.matchedCount, `${fieldName}.matchedCount`),
    publishedCount: normalizeOptionalTotal(state.publishedCount, `${fieldName}.publishedCount`),
    skippedUnpublishedCount: normalizeOptionalTotal(
      state.skippedUnpublishedCount,
      `${fieldName}.skippedUnpublishedCount`
    ),
    skippedExistingCount: normalizeOptionalTotal(
      state.skippedExistingCount,
      `${fieldName}.skippedExistingCount`
    ),
    skippedMissingScriptCount: normalizeOptionalTotal(
      state.skippedMissingScriptCount,
      `${fieldName}.skippedMissingScriptCount`
    ),
    succeededCount: normalizeOptionalTotal(state.succeededCount, `${fieldName}.succeededCount`),
    failedCount: normalizeOptionalTotal(state.failedCount, `${fieldName}.failedCount`),
    remainingActionableCount: normalizeOptionalTotal(
      state.remainingActionableCount,
      `${fieldName}.remainingActionableCount`
    ),
    lastRunAt:
      state.lastRunAt === undefined || state.lastRunAt === null || state.lastRunAt === ''
        ? null
        : normalizeTotal(state.lastRunAt, `${fieldName}.lastRunAt`),
    lastFailureMessage: resolveStringValue(state.lastFailureMessage).trim(),
    lastFailureAt:
      state.lastFailureAt === undefined || state.lastFailureAt === null || state.lastFailureAt === ''
        ? null
        : normalizeTotal(state.lastFailureAt, `${fieldName}.lastFailureAt`)
  }
}

const normalizeProductNarrationScriptTaskStatus = (
  value: unknown,
  fieldName: string
): ShowroomProductNarrationScriptTaskRespVO => {
  const state = normalizeObject(value, fieldName)
  const rawCurrentProduct = state.currentProduct
  const currentProduct =
    rawCurrentProduct === undefined || rawCurrentProduct === null
      ? null
      : (() => {
          const product = normalizeObject(rawCurrentProduct, `${fieldName}.currentProduct`)
          return {
            productId: normalizeId(product.productId, `${fieldName}.currentProduct.productId`),
            productCode: resolveStringValue(product.productCode).trim(),
            nameCn: resolveStringValue(product.nameCn).trim()
          } as ShowroomProductNarrationScriptTaskCurrentProductRespVO
        })()
  const rawFailure = state.lastFailure
  const lastFailure =
    rawFailure === undefined || rawFailure === null
      ? null
      : (() => {
          const failure = normalizeObject(rawFailure, `${fieldName}.lastFailure`)
          return {
            productId: normalizeId(failure.productId, `${fieldName}.lastFailure.productId`),
            productCode: resolveStringValue(failure.productCode).trim(),
            nameCn: resolveStringValue(failure.nameCn).trim(),
            reason: resolveStringValue(failure.reason).trim()
          }
        })()
  return {
    active: normalizeOptionalBoolean(state.active),
    running: normalizeOptionalBoolean(state.running),
    keyword: resolveStringValue(state.keyword).trim(),
    lifecycleStage: resolveStringValue(state.lifecycleStage).trim(),
    incompleteStatus: resolveStringValue(state.incompleteStatus).trim(),
    approvalStatus: resolveStringValue(state.approvalStatus).trim(),
    matchedCount: normalizeOptionalTotal(state.matchedCount, `${fieldName}.matchedCount`),
    skippedCompletedCount: normalizeOptionalTotal(
      state.skippedCompletedCount,
      `${fieldName}.skippedCompletedCount`
    ),
    generatedLanguageCount: normalizeOptionalTotal(
      state.generatedLanguageCount,
      `${fieldName}.generatedLanguageCount`
    ),
    failedCount: normalizeOptionalTotal(state.failedCount, `${fieldName}.failedCount`),
    remainingCount: normalizeOptionalTotal(state.remainingCount, `${fieldName}.remainingCount`),
    startedAt:
      state.startedAt === undefined || state.startedAt === null || state.startedAt === ''
        ? null
        : normalizeTotal(state.startedAt, `${fieldName}.startedAt`),
    lastRunAt:
      state.lastRunAt === undefined || state.lastRunAt === null || state.lastRunAt === ''
        ? null
        : normalizeTotal(state.lastRunAt, `${fieldName}.lastRunAt`),
    completedAt:
      state.completedAt === undefined || state.completedAt === null || state.completedAt === ''
        ? null
        : normalizeTotal(state.completedAt, `${fieldName}.completedAt`),
    currentProduct,
    lastFailure,
    lastFailureAt:
      state.lastFailureAt === undefined || state.lastFailureAt === null || state.lastFailureAt === ''
        ? null
        : normalizeTotal(state.lastFailureAt, `${fieldName}.lastFailureAt`)
  }
}

const normalizeProductCoverTaskState = (
  value: unknown,
  fieldName: string
): CoverTaskBannerSummary => {
  const state = normalizeObject(value, fieldName)
  const rawCurrentProduct = state.currentProduct
  const currentProduct =
    rawCurrentProduct === undefined || rawCurrentProduct === null
      ? null
      : (() => {
          const product = normalizeObject(rawCurrentProduct, `${fieldName}.currentProduct`)
          return {
            productId: normalizeId(product.productId, `${fieldName}.currentProduct.productId`),
            productCode: resolveStringValue(product.productCode).trim(),
            nameCn: resolveStringValue(product.nameCn).trim()
          }
        })()
  return {
    startAllowed: normalizeOptionalBoolean(state.startAllowed),
    active: normalizeOptionalBoolean(state.active),
    running: normalizeOptionalBoolean(state.running),
    keyword: resolveStringValue(state.keyword).trim(),
    lifecycleStage: resolveStringValue(state.lifecycleStage).trim(),
    incompleteStatus: resolveStringValue(state.incompleteStatus).trim(),
    approvalStatus: resolveStringValue(state.approvalStatus).trim(),
    matchedCount: normalizeOptionalTotal(state.matchedCount, `${fieldName}.matchedCount`),
    publishedCount: normalizeOptionalTotal(state.publishedCount, `${fieldName}.publishedCount`),
    skippedUnpublishedCount: normalizeOptionalTotal(
      state.skippedUnpublishedCount,
      `${fieldName}.skippedUnpublishedCount`
    ),
    skippedExistingCount: normalizeOptionalTotal(
      state.skippedExistingCount,
      `${fieldName}.skippedExistingCount`
    ),
    succeededCount: normalizeOptionalTotal(state.succeededCount, `${fieldName}.succeededCount`),
    failedCount: normalizeOptionalTotal(state.failedCount, `${fieldName}.failedCount`),
    remainingPendingCount: normalizeOptionalTotal(
      state.remainingPendingCount,
      `${fieldName}.remainingPendingCount`
    ),
    taskId:
      state.taskId === undefined || state.taskId === null || state.taskId === ''
        ? null
        : normalizeId(state.taskId, `${fieldName}.taskId`),
    taskStatus: resolveStringValue(state.taskStatus).trim(),
    nextCheckAt: resolveStringValue(state.nextCheckAt).trim() || null,
    lastRunAt:
      state.lastRunAt === undefined || state.lastRunAt === null || state.lastRunAt === ''
        ? null
        : normalizeTotal(state.lastRunAt, `${fieldName}.lastRunAt`),
    completedAt:
      state.completedAt === undefined || state.completedAt === null || state.completedAt === ''
        ? null
        : normalizeTotal(state.completedAt, `${fieldName}.completedAt`),
    lastFailureMessage: resolveStringValue(state.lastFailureMessage).trim() || null,
    currentProduct,
    failures: [],
    updatedAt: Date.now()
  }
}

const normalizeProductTranslatePublishTaskStatus = (
  value: unknown,
  fieldName: string
): ShowroomProductTranslatePublishBatchTaskRespVO => {
  const state = normalizeObject(value, fieldName)
  const currentProductValue = state.currentProduct
  const currentProduct = currentProductValue
    ? normalizeObject(currentProductValue, `${fieldName}.currentProduct`)
    : null
  const lastFailureValue = state.lastFailure
  const lastFailure = lastFailureValue
    ? normalizeObject(lastFailureValue, `${fieldName}.lastFailure`)
    : null
  return {
    active: normalizeOptionalBoolean(state.active),
    running: normalizeOptionalBoolean(state.running),
    keyword: resolveStringValue(state.keyword).trim(),
    lifecycleStage: resolveStringValue(state.lifecycleStage).trim(),
    incompleteStatus: resolveStringValue(state.incompleteStatus).trim(),
    approvalStatus: resolveStringValue(state.approvalStatus).trim(),
    matchedCount: normalizeTotal(state.matchedCount, `${fieldName}.matchedCount`),
    succeededCount: normalizeTotal(state.succeededCount, `${fieldName}.succeededCount`),
    failedCount: normalizeTotal(state.failedCount, `${fieldName}.failedCount`),
    remainingCount: normalizeTotal(state.remainingCount, `${fieldName}.remainingCount`),
    startedAt: state.startedAt ? normalizeId(state.startedAt, `${fieldName}.startedAt`) : null,
    lastRunAt: state.lastRunAt ? normalizeId(state.lastRunAt, `${fieldName}.lastRunAt`) : null,
    completedAt: state.completedAt ? normalizeId(state.completedAt, `${fieldName}.completedAt`) : null,
    currentProduct: currentProduct
      ? {
          productId: normalizeId(currentProduct.productId, `${fieldName}.currentProduct.productId`),
          productCode: resolveStringValue(currentProduct.productCode).trim(),
          nameCn: resolveStringValue(currentProduct.nameCn).trim()
        }
      : null,
    lastFailure: lastFailure
      ? {
          productId: normalizeId(lastFailure.productId, `${fieldName}.lastFailure.productId`),
          productCode: resolveStringValue(lastFailure.productCode).trim(),
          nameCn: resolveStringValue(lastFailure.nameCn).trim(),
          reason: resolveStringValue(lastFailure.reason).trim() || '未知失败'
        }
      : null,
    lastFailureAt: state.lastFailureAt
      ? normalizeId(state.lastFailureAt, `${fieldName}.lastFailureAt`)
      : null,
    failures: normalizeArray(state.failures, `${fieldName}.failures`).map((item, index) => {
      const failure = normalizeObject(item, `${fieldName}.failures[${index}]`)
      return {
        productId: normalizeId(failure.productId, `${fieldName}.failures[${index}].productId`),
        productCode: resolveStringValue(failure.productCode).trim(),
        nameCn: resolveStringValue(failure.nameCn).trim(),
        reason: resolveStringValue(failure.reason).trim() || '未知失败'
      }
    })
  }
}

const buildProductPageParams = (pageNo: number, pageSize: number) => {
  return {
    pageNo,
    pageSize,
    productId: productFilters.value.productId || undefined,
    keyword: productFilters.value.keyword,
    lifecycleStage: productFilters.value.lifecycleStage,
    incompleteStatus: productFilters.value.incompleteStatus,
    approvalStatus: productFilters.value.approvalStatus
  } as PageParam & { keyword?: string }
}

const buildProductBatchGeneratePayload = (coverGenerationMode?: ShowroomProductCoverGenerationMode) => {
  return {
    keyword: productFilters.value.keyword.trim() || undefined,
    lifecycleStage: productFilters.value.lifecycleStage || undefined,
    incompleteStatus: productFilters.value.incompleteStatus || undefined,
    approvalStatus: productFilters.value.approvalStatus || undefined,
    coverGenerationMode
  }
}

const resolveStringValue = (value: unknown) => {
  return value === undefined || value === null ? '' : String(value)
}

const resolveTranslatedProductText = (
  sourceText: string,
  translatedValue: unknown,
  fieldLabel: string
): string | null => {
  const normalizedSource = sourceText.trim()
  if (!normalizedSource) {
    return null
  }
  const normalizedTranslated = resolveStringValue(translatedValue).trim()
  if (!normalizedTranslated) {
    throw new Error(`产品翻译失败：${fieldLabel}缺少英文结果`)
  }
  return normalizedTranslated
}

const resolveError = (error: unknown): Error => {
  return error instanceof Error ? error : new Error(String(error))
}

const isUserCanceledAction = (error: unknown) => {
  return error === 'cancel' || error === 'close'
}

const toOptionalRouteNumber = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  if (rawValue === undefined || rawValue === null || rawValue === '') {
    return null
  }
  const parsed = Number(rawValue)
  return Number.isFinite(parsed) ? parsed : null
}

const resolveNotifyProductRouteRequest = () => {
  const targetType = String(
    (Array.isArray(route.query.notifyTargetType) ? route.query.notifyTargetType[0] : route.query.notifyTargetType) ||
      ''
  ).toUpperCase()
  const targetId = toOptionalRouteNumber(route.query.notifyTargetId)
  if (!targetId) {
    const stored = sessionStorage.getItem(SHOWROOM_NOTIFY_PRODUCT_TARGET_KEY)
    if (!stored) {
      if (targetType !== 'PRODUCT') {
        return null
      }
      return null
    }
    try {
      const parsed = JSON.parse(stored) as {
        targetId?: number
        changeRequestId?: number
        notifyOpen?: string
      }
      if (!parsed?.targetId) {
        return null
      }
      return {
        targetId: Number(parsed.targetId),
        changeRequestId: toOptionalRouteNumber(parsed.changeRequestId),
        notifyOpen:
          String(parsed.notifyOpen || '').toLowerCase() === 'approval'
            ? 'approval'
            : String(parsed.notifyOpen || '').toLowerCase() === 'edit'
              ? 'edit'
              : 'detail',
        requestKey: `stored:${Number(parsed.targetId)}:${toOptionalRouteNumber(parsed.changeRequestId) ?? ''}:${String(parsed.notifyOpen || '').toLowerCase() === 'approval' ? 'approval' : String(parsed.notifyOpen || '').toLowerCase() === 'edit' ? 'edit' : 'detail'}`
      } as const
    } catch {
      return null
    }
  }
  if (targetType !== 'PRODUCT') {
    return null
  }
  const notifyChangeRequestId = toOptionalRouteNumber(route.query.notifyChangeRequestId)
  const rawNotifyOpen = String(
    (Array.isArray(route.query.notifyOpen) ? route.query.notifyOpen[0] : route.query.notifyOpen) || ''
  ).toLowerCase()
  return {
    targetId,
    changeRequestId: notifyChangeRequestId,
    notifyOpen:
      rawNotifyOpen === 'approval' ? 'approval' : rawNotifyOpen === 'edit' ? 'edit' : 'detail',
    requestKey: `query:${targetId}:${notifyChangeRequestId ?? ''}:${rawNotifyOpen === 'approval' ? 'approval' : rawNotifyOpen === 'edit' ? 'edit' : 'detail'}`
  } as const
}

const clearNotifyProductRouteQuery = async () => {
  const hasNotifyQuery =
    route.query.notifyTargetType !== undefined ||
    route.query.notifyTargetId !== undefined ||
    route.query.notifyChangeRequestId !== undefined ||
    route.query.notifyOpen !== undefined
  const nextQuery = { ...route.query }
  delete nextQuery.notifyTargetType
  delete nextQuery.notifyTargetId
  delete nextQuery.notifyChangeRequestId
  delete nextQuery.notifyOpen
  if (!hasNotifyQuery) {
    sessionStorage.removeItem(SHOWROOM_NOTIFY_PRODUCT_TARGET_KEY)
    return
  }
  const search = new URLSearchParams(
    Object.entries(nextQuery).reduce<Record<string, string>>((accumulator, [key, value]) => {
      if (value === undefined || value === null) {
        return accumulator
      }
      accumulator[key] = Array.isArray(value) ? String(value[0]) : String(value)
      return accumulator
    }, {})
  ).toString()
  const nextUrl = search ? `${route.path}?${search}` : route.path
  window.history.replaceState(window.history.state, '', nextUrl)
  sessionStorage.removeItem(SHOWROOM_NOTIFY_PRODUCT_TARGET_KEY)
}

const resetProductNarrationDraft = (sourceRevisionId: number | null) => {
  productNarrationDraft.sourceRevisionId = sourceRevisionId
  productNarrationDraft.zhVersionId = null
  productNarrationDraft.enVersionId = null
  productNarrationDraft.zhVersionNo = null
  productNarrationDraft.enVersionNo = null
  productNarrationDraft.zhScriptText = ''
  productNarrationDraft.enScriptText = ''
  productNarrationDraft.zhStatus = 'DRAFT'
  productNarrationDraft.enStatus = 'DRAFT'
  productNarrationDraft.zhGenerationStatus = 'NOT_GENERATED'
  productNarrationDraft.enGenerationStatus = 'NOT_GENERATED'
  productNarrationDraft.zhGeneratedByAi = false
  productNarrationDraft.enGeneratedByAi = false
  productNarrationDraft.zhAudioFileId = null
  productNarrationDraft.enAudioFileId = null
  productNarrationDraft.zhAudioUrl = ''
  productNarrationDraft.enAudioUrl = ''
  productNarrationDraft.zhAudioDurationSeconds = null
  productNarrationDraft.enAudioDurationSeconds = null
  productNarrationDraft.voice = ''
  productNarrationBaselineZhScript.value = ''
  productNarrationBaselineEnScript.value = ''
}

const applyProductNarrationVersion = (version: ShowroomNarrationVersionRecord) => {
  productNarrationDraft.sourceRevisionId = version.sourceRevisionId
  if (version.key.language === 'EN') {
    productNarrationDraft.enVersionId = version.id
    productNarrationDraft.enVersionNo = version.versionNo
    productNarrationDraft.enScriptText = version.scriptText
    productNarrationDraft.enStatus = version.status
    productNarrationDraft.enGenerationStatus = version.generationStatus
    productNarrationDraft.enGeneratedByAi = version.generatedByAi
    productNarrationDraft.enAudioFileId = version.audioFileId || null
    productNarrationDraft.enAudioUrl = version.audioUrl || ''
    productNarrationDraft.enAudioDurationSeconds = version.audioDurationSeconds || null
    productNarrationBaselineEnScript.value = version.scriptText.trim()
  } else {
    productNarrationDraft.zhVersionId = version.id
    productNarrationDraft.zhVersionNo = version.versionNo
    productNarrationDraft.zhScriptText = version.scriptText
    productNarrationDraft.zhStatus = version.status
    productNarrationDraft.zhGenerationStatus = version.generationStatus
    productNarrationDraft.zhGeneratedByAi = version.generatedByAi
    productNarrationDraft.zhAudioFileId = version.audioFileId || null
    productNarrationDraft.zhAudioUrl = version.audioUrl || ''
    productNarrationDraft.zhAudioDurationSeconds = version.audioDurationSeconds || null
    productNarrationBaselineZhScript.value = version.scriptText.trim()
  }
  if (version.voice) {
    productNarrationDraft.voice = version.voice
  }
  productNarrationLoadError.value = ''
}

const isProductNarrationNotFoundError = (message: string) => {
  return message.includes('SHOWROOM_TARGET_NOT_FOUND: narration not found')
}

const isDirectPublishableProductStatus = (status: string) => {
  return PRODUCT_DIRECT_PUBLISHABLE_STATUSES.has(status)
}

const buildProductFieldSnapshot = (form: ProductForm) => {
  return {
    owner_company_id: form.ownerCompanyId === null ? '' : String(form.ownerCompanyId),
    product_owner_type: form.productOwnerType,
    lifecycle_stage: form.lifecycleStage,
    target_market: form.targetMarket.trim(),
    target_market_en: form.targetMarketEn.trim(),
    pipeline_layout: form.pipelineLayout.trim(),
    pipeline_layout_en: form.pipelineLayoutEn.trim(),
    indication_content: form.indicationContent.trim(),
    indication_content_en: form.indicationContentEn.trim(),
    core_selling_points: form.coreSellingPoints.trim(),
    core_selling_points_en: form.coreSellingPointsEn.trim(),
    model_specification: form.modelSpecification.trim(),
    model_specification_en: form.modelSpecificationEn.trim(),
    registration_certificate: form.registrationCertificate.trim(),
    registration_certificate_en: form.registrationCertificateEn.trim(),
    clinical_effect: form.clinicalEffect.trim(),
    clinical_effect_en: form.clinicalEffectEn.trim(),
    fim_status: form.fimStatus.trim(),
    fim_status_en: form.fimStatusEn.trim(),
    cover_image: form.coverImage.trim()
  }
}

const buildProductAttachmentPayload = (): ShowroomProductAttachment[] => {
  return productForm.attachments.map(normalizeProductAttachmentForPayload)
}

const normalizeProductAttachmentForPayload = (
  attachment: ShowroomProductAttachment,
  index: number
): ShowroomProductAttachment => {
  const originalName = resolveStringValue(attachment.originalName).trim()
  const mimeType = resolveStringValue(attachment.mimeType).trim()
  if (!['image', 'video', 'text'].includes(attachment.assetType)) {
    throw new Error('附件类型不支持，无法保存产品')
  }
  if (!Number.isFinite(attachment.fileId)) {
    throw new Error('附件文件ID缺失，无法保存产品')
  }
  if (!originalName) {
    throw new Error('附件文件名缺失，无法保存产品')
  }
  if (!mimeType) {
    throw new Error('附件MIME类型缺失，无法保存产品')
  }
  if (!Number.isFinite(attachment.size) || attachment.size < 0) {
    throw new Error('附件大小缺失，无法保存产品')
  }
  return {
    id: attachment.id ?? null,
    assetType: attachment.assetType,
    fileId: attachment.fileId,
    originalName,
    mimeType,
    size: attachment.size,
    displayOrder: index + 1
  }
}

const productAttachmentSignature = (attachments: ShowroomProductAttachment[]) => {
  return JSON.stringify(
    attachments.map((attachment, index) => normalizeProductAttachmentForPayload(attachment, index))
  )
}

const syncProductSubmitBaseline = (status: string) => {
  productSubmitBaseline.value = {
    status,
    productMasterId: productForm.productMasterId,
    productCode: productForm.productCode.trim(),
    nameCn: productForm.nameCn.trim(),
    nameEn: productForm.nameEn.trim(),
    fields: buildProductFieldSnapshot(productForm),
    attachments: buildProductAttachmentPayload()
  }
}

const hasProductDraftChanges = computed(() => {
  const baseline = productSubmitBaseline.value
  if (!baseline) {
    return false
  }
  if (baseline.productCode !== productForm.productCode.trim()) {
    return true
  }
  if (baseline.productMasterId !== productForm.productMasterId) {
    return true
  }
  if (baseline.nameCn !== productForm.nameCn.trim()) {
    return true
  }
  if (baseline.nameEn !== productForm.nameEn.trim()) {
    return true
  }
  const currentFields = buildProductFieldSnapshot(productForm)
  if (Object.keys(currentFields).some((key) => baseline.fields[key] !== currentFields[key])) {
    return true
  }
  return productAttachmentSignature(baseline.attachments) !==
    productAttachmentSignature(buildProductAttachmentPayload())
})

const canSubmitProduct = computed(() => {
  const status = productSubmitBaseline.value?.status || ''
  return hasProductDraftChanges.value || status === 'DRAFT' || status === 'REJECTED'
})

const canGenerateProductAiCover = computed(() => {
  const status = productSubmitBaseline.value?.status || ''
  return Boolean(productForm.productId) && PRODUCT_AI_COVER_APPROVED_STATUSES.has(status)
})

const canTranslateProductEnglishFields = computed(() => {
  const fields = buildProductFieldSnapshot(productForm)
  return Boolean(
    productForm.nameCn.trim() ||
      productTranslatableFieldDefinitions.some((definition) => fields[definition.key].trim()) ||
      productNarrationDraft.zhScriptText.trim()
  )
})

const draftProductZhAudioUrl = computed(() => productNarrationDraft.zhAudioUrl)
const draftProductEnAudioUrl = computed(() => productNarrationDraft.enAudioUrl)

const awardNarrationDraftStale = computed(() => {
  return Boolean(
    (awardNarrationDraft.zhAudioFileId || awardNarrationDraft.enAudioFileId) &&
      (awardForm.descriptionZh.trim() !== awardNarrationBaselineZhScript.value ||
        awardForm.descriptionEn.trim() !== awardNarrationBaselineEnScript.value)
  )
})

const hasProductNarrationChanges = computed(() => {
  return (
    productNarrationDraft.zhScriptText.trim() !== productNarrationBaselineZhScript.value ||
    productNarrationDraft.enScriptText.trim() !== productNarrationBaselineEnScript.value
  )
})

const productNarrationDraftStale = computed(() => {
  return Boolean(
    (productNarrationDraft.zhAudioFileId || productNarrationDraft.enAudioFileId) &&
      hasProductNarrationChanges.value
  )
})

const loadProductNarrationDraft = async (productId: number, sourceRevisionId: number | null) => {
  productNarrationLoading.value = true
  productNarrationLoadError.value = ''
  resetProductNarrationDraft(sourceRevisionId)
  try {
    await Promise.all(
      (['ZH', 'EN'] as const).map(async (language) => {
        try {
          const version = normalizeNarrationVersion(
            await ShowroomAdminApi.getNarration({
              targetType: 'PRODUCT',
              targetId: productId,
              audienceType: 'PUBLIC',
              language
            })
          )
          applyProductNarrationVersion(version)
        } catch (error) {
          const resolved = resolveError(error)
          if (!isProductNarrationNotFoundError(resolved.message)) {
            throw resolved
          }
        }
      })
    )
  } catch (error) {
    const resolved = resolveError(error)
    productNarrationLoadError.value = resolved.message
  } finally {
    productNarrationLoading.value = false
  }
}

const loadProductRows = async () => {
  const productPage = normalizeObject(
    await ShowroomAdminApi.getProductPage(buildProductPageParams(productPageNo.value, productPageSize.value)),
    'productPage'
  )
  const nextTotal = normalizeTotal(productPage.total, 'productPage.total')
  const totalPages = nextTotal > 0 ? Math.ceil(nextTotal / productPageSize.value) : 0
  if (totalPages > 0 && productPageNo.value > totalPages) {
    productPageNo.value = totalPages
    await loadProductRows()
    return
  }
  productPageTotal.value = nextTotal
  productRows.value = normalizeArray(productPage.list, 'productPage.list')
}

const loadAwardRows = async () => {
  const awardPage = normalizeObject(
    await ShowroomAdminApi.getAwardPage({
      pageNo: awardPageNo.value,
      pageSize: awardPageSize.value,
      keyword: awardKeyword.value.trim() || undefined
    }),
    'awardPage'
  )
  const nextTotal = normalizeTotal(awardPage.total, 'awardPage.total')
  const totalPages = nextTotal > 0 ? Math.ceil(nextTotal / awardPageSize.value) : 0
  if (totalPages > 0 && awardPageNo.value > totalPages) {
    awardPageNo.value = totalPages
    await loadAwardRows()
    return
  }
  awardPageTotal.value = nextTotal
  awardRows.value = normalizeAwardRows(normalizeArray(awardPage.list, 'awardPage.list'))
}

const loadBatchProductAudioAutoCheckState = async () => {
  if (!isShowroomPublicity.value || activeSection.value !== 'product') {
    batchProductAudioAutoCheckState.value = null
    return
  }
  batchProductAudioAutoCheckState.value = normalizeBatchProductAudioAutoCheckState(
    await ShowroomAdminApi.getProductBatchGenerateNarrationAudioState(),
    'productBatchGenerateNarrationAudioState'
  )
}

const loadProductNarrationScriptTaskStatus = async () => {
  if (!isShowroomPublicity.value || activeSection.value !== 'product') {
    productNarrationScriptTaskStatus.value = null
    return
  }
  productNarrationScriptTaskStatus.value = normalizeProductNarrationScriptTaskStatus(
    await ShowroomAdminApi.getBatchGenerateNarrationScriptTaskStatus(),
      'productNarrationScriptTaskStatus'
    )
}

const loadProductCoverTaskState = async () => {
  if (!isShowroomPublicity.value || activeSection.value !== 'product') {
    latestProductCoverTaskSummary.value = null
    return
  }
  latestProductCoverTaskSummary.value = normalizeProductCoverTaskState(
    await ShowroomAdminApi.getProductBatchGenerateCoverImageState(),
    'productBatchGenerateCoverImageState'
  )
}

const loadProductTranslatePublishTaskStatus = async () => {
  if (!isShowroomPublicity.value || activeSection.value !== 'product') {
    productTranslatePublishTaskStatus.value = null
    return
  }
  productTranslatePublishTaskStatus.value = normalizeProductTranslatePublishTaskStatus(
    await ShowroomAdminApi.getBatchTranslatePublishTaskStatus(),
    'productTranslatePublishTaskStatus'
  )
}

const clearProductNarrationScriptTaskPolling = () => {
  if (productNarrationScriptTaskPollingTimer) {
    window.clearInterval(productNarrationScriptTaskPollingTimer)
    productNarrationScriptTaskPollingTimer = undefined
  }
}

const clearProductCoverTaskPolling = () => {
  if (productCoverTaskPollingTimer) {
    window.clearInterval(productCoverTaskPollingTimer)
    productCoverTaskPollingTimer = undefined
  }
}

const clearProductTranslatePublishTaskPolling = () => {
  if (productTranslatePublishTaskPollingTimer) {
    window.clearInterval(productTranslatePublishTaskPollingTimer)
    productTranslatePublishTaskPollingTimer = undefined
  }
}

const syncProductNarrationScriptTaskPolling = () => {
  const shouldPoll =
    activeSection.value === 'product' &&
    isShowroomPublicity.value &&
    (productNarrationScriptTaskStatus.value?.active || productNarrationScriptTaskStatus.value?.running)
  if (!shouldPoll) {
    clearProductNarrationScriptTaskPolling()
    return
  }
  if (productNarrationScriptTaskPollingTimer) {
    return
  }
  productNarrationScriptTaskPollingTimer = window.setInterval(async () => {
    const stillActive =
      activeSection.value === 'product' &&
      isShowroomPublicity.value &&
      (productNarrationScriptTaskStatus.value?.active || productNarrationScriptTaskStatus.value?.running)
    if (!stillActive) {
      clearProductNarrationScriptTaskPolling()
      return
    }
    try {
      await loadProductNarrationScriptTaskStatus()
    } catch (error) {
      clearProductNarrationScriptTaskPolling()
      message.error(`刷新一键讲解任务状态失败：${resolveError(error).message}`)
    }
  }, 2000)
}

const syncProductCoverTaskPolling = () => {
  const shouldPoll =
    activeSection.value === 'product' &&
    isShowroomPublicity.value &&
    Boolean(
      latestProductCoverTaskSummary.value?.active || latestProductCoverTaskSummary.value?.running
    )
  if (!shouldPoll) {
    clearProductCoverTaskPolling()
    return
  }
  if (productCoverTaskPollingTimer) {
    return
  }
  productCoverTaskPollingTimer = window.setInterval(async () => {
    const stillActive =
      activeSection.value === 'product' &&
      isShowroomPublicity.value &&
      Boolean(
        latestProductCoverTaskSummary.value?.active || latestProductCoverTaskSummary.value?.running
      )
    if (!stillActive) {
      clearProductCoverTaskPolling()
      return
    }
    try {
      await loadProductCoverTaskState()
    } catch (error) {
      clearProductCoverTaskPolling()
      message.error(`刷新一键封面任务状态失败：${resolveError(error).message}`)
    }
  }, 2000)
}

const syncProductTranslatePublishTaskPolling = () => {
  const shouldPoll =
    activeSection.value === 'product' &&
    isShowroomPublicity.value &&
    Boolean(productTranslatePublishTaskStatus.value?.active || productTranslatePublishTaskStatus.value?.running)
  if (!shouldPoll) {
    clearProductTranslatePublishTaskPolling()
    return
  }
  if (productTranslatePublishTaskPollingTimer) {
    return
  }
  productTranslatePublishTaskPollingTimer = window.setInterval(async () => {
    const wasActive = Boolean(
      productTranslatePublishTaskStatus.value?.active || productTranslatePublishTaskStatus.value?.running
    )
    const stillAllowed = activeSection.value === 'product' && isShowroomPublicity.value
    if (!stillAllowed || !wasActive) {
      clearProductTranslatePublishTaskPolling()
      return
    }
    try {
      await loadProductTranslatePublishTaskStatus()
      const state = productTranslatePublishTaskStatus.value
      if (state && !state.active && !state.running) {
        clearProductTranslatePublishTaskPolling()
        await loadProductRows()
        openBatchMediaSummary('一键翻译发布结果', 'PUBLISH', {
          matchedCount: state.matchedCount,
          publishedCount: state.succeededCount,
          skippedUnpublishedCount: 0,
          skippedExistingCount: 0,
          skippedMissingScriptCount: 0,
          succeededCount: state.succeededCount,
          failedCount: state.failedCount,
          autoCheckEnabled: false,
          remainingActionableCount: state.remainingCount,
          failures: state.failures
        })
      }
    } catch (error) {
      clearProductTranslatePublishTaskPolling()
      message.error(`刷新一键翻译任务状态失败：${resolveError(error).message}`)
    }
  }, 2000)
}

const loadHallRows = async () => {
  const params = {
    pageNo: 1,
    pageSize: 20,
    keyword: hallKeyword.value
  } as PageParam & { keyword?: string }
  hallRows.value = normalizeArray(await ShowroomAdminApi.getHallPage(params), 'hallPage')
}

const loadProductCompanyOptions = async () => {
  const [deptList, userList] = await Promise.all([DeptApi.getSimpleDeptList(), getSimpleUserList()])
  showroomUserOptions.value = userList as UserVO[]
  productCompanyOptions.value = buildShowroomCompanyOptions(deptList)
  productApprovalRoutePreview.value = resolveShowroomApprovalRoutePreview(
    userStore.getUser.id || null,
    userStore.getUser.deptId || null,
    deptList,
    userList
  )
}

const formatProductMasterOptionLabel = (product: MdmProductSimpleRespVO) =>
  `${product.productCode} · ${product.nameCn}`

const loadProductMasterOptions = async (keyword = '') => {
  productMasterOptionsLoading.value = true
  try {
    productMasterOptions.value = await getProductSimpleList({
      status: MDM_PRODUCT_STATUS_ENABLE,
      keyword: keyword.trim() || undefined
    })
  } finally {
    productMasterOptionsLoading.value = false
  }
}

const handleProductMasterChange = (productId: number | null | undefined) => {
  const product = productMasterOptions.value.find((item) => item.id === productId)
  productForm.productCode = product?.productCode || ''
  productForm.nameCn = product?.nameCn || ''
  productForm.nameEn = product?.nameEn || ''
  if (product?.modelSpecification) {
    productForm.modelSpecification = product.modelSpecification
  }
}

const ensureProductCompanyOptions = async () => {
  if (productCompanyOptions.value.length > 0 && productApprovalRoutePreview.value) {
    return
  }
  await loadProductCompanyOptions()
}

const ensureProductMasterOptions = async () => {
  if (productMasterOptions.value.length > 0) {
    return
  }
  await loadProductMasterOptions()
}

const loadShowroomAdminData = async () => {
  adminLoading.value = true
  adminLoadError.value = ''
  try {
    const notifyRequest = activeSection.value === 'product' ? resolveNotifyProductRouteRequest() : null
    if (notifyRequest && notifyRequest.requestKey !== consumedNotifyProductRouteKey.value) {
      productPageNo.value = 1
      productFilters.value = {
        ...productFilters.value,
        productId: notifyRequest.targetId,
        keyword: '',
        lifecycleStage: '',
        incompleteStatus: '',
        approvalStatus: ''
      }
    }
    companyCurrent.value = shouldLoadCompanyCurrent.value
      ? normalizeObject(await ShowroomAdminApi.getCompanyCurrent(), 'companyCurrent')
      : null
    const dataLoaders: Promise<unknown>[] = []
    if (shouldLoadProductRows.value) {
      dataLoaders.push(loadProductRows())
      dataLoaders.push(loadAwardRows())
    }
    if (shouldLoadHallRows.value) {
      dataLoaders.push(loadHallRows())
    }
    await Promise.all(dataLoaders)
    await loadBatchProductAudioAutoCheckState()
    await loadProductNarrationScriptTaskStatus()
    await loadProductCoverTaskState()
    await loadProductTranslatePublishTaskStatus()

    rowsBySection.value = {
      history: [
        {
          code: 'version-audit',
          name: '版本历史',
          status: '可追溯',
          tagType: 'success',
          description: '按字段保留旧值、新值、操作人和发布时间',
          action: '查看记录'
        }
      ]
    }
  } catch (error) {
    const resolved = resolveError(error)
    adminLoadError.value = `加载展柜数据失败：${resolved.message}`
    message.error(adminLoadError.value)
    throw resolved
  } finally {
    adminLoading.value = false
  }
}

const assignProductForm = (form: ProductForm) => {
  Object.assign(productForm, form)
}

const assignHallForm = (form: HallForm) => {
  Object.assign(hallForm, form)
}

const openProductCreate = async () => {
  try {
    await Promise.all([ensureProductCompanyOptions(), ensureProductMasterOptions()])
    assignProductForm(createEmptyProductForm())
    productDialogEditable.value = true
    applyFixedProductOwner()
    productDialogActiveLanguageTab.value = 'zh'
    syncProductSubmitBaseline('NEW')
    productNarrationLoadError.value = ''
    resetProductNarrationDraft(null)
    productDialogVisible.value = true
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  }
}

const openProductImportForm = () => {
  importingProductExcel.value = false
  productImportFormRef.value.open('STANDARD')
}

const openProductBaseWorkbookImportForm = () => {
  importingProductExcel.value = false
  productImportFormRef.value.open('BASE_WORKBOOK')
}

const resolveOptionalProductOwnerCompanyId = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return null
  }
  return normalizeId(value, 'owner_company_id')
}

const buildFallbackProductCompanyOption = (): ShowroomCompanyOption => {
  const current = companyCurrent.value
  if (!current) {
    throw new Error('展柜公司信息缺失，无法设置产品归属')
  }
  const companyId = normalizeId(current.companyId, 'companyCurrent.companyId')
  return {
    id: companyId,
    name: SHOWROOM_PRODUCT_OWNER_LABEL,
    ownerType: 'YINGTAI'
  }
}

const requireYingtaiProductCompany = () => {
  const companyOption = productCompanyOptions.value[0]
  if (companyOption) {
    return companyOption
  }
  return buildFallbackProductCompanyOption()
}

const applyFixedProductOwner = () => {
  const companyOption = requireYingtaiProductCompany()
  productForm.ownerCompanyId = companyOption.id
  productForm.productOwnerType = companyOption.ownerType
}

const closeProductDetailDialog = () => {
  productDetailDialogVisible.value = false
  activeProductDetailId.value = null
  activeProductDetailRevisionId.value = null
  productDetailApprovalChangeRequestId.value = null
}

const resolveProductDetailRevisionId = (product: Record<string, unknown>) => {
  const latestRevision =
    product.revision && typeof product.revision === 'object'
      ? (product.revision as Record<string, unknown>)
      : null
  if (latestRevision?.revisionId !== undefined && latestRevision.revisionId !== null) {
    return normalizeId(latestRevision.revisionId, 'product.revision.revisionId')
  }
  const displayRevision =
    product.displayRevision && typeof product.displayRevision === 'object'
      ? (product.displayRevision as Record<string, unknown>)
      : null
  if (displayRevision?.revisionId !== undefined && displayRevision.revisionId !== null) {
    return normalizeId(displayRevision.revisionId, 'product.displayRevision.revisionId')
  }
  return normalizeId(product.currentRevisionId, 'product.currentRevisionId')
}

const resolveProductAudioSourceRevisionId = (product: Record<string, unknown>) => {
  const resolveNestedRevisionId = (revisionValue: unknown, fieldLabel: string) => {
    if (!revisionValue || typeof revisionValue !== 'object' || Array.isArray(revisionValue)) {
      return null
    }
    const revision = revisionValue as Record<string, unknown>
    if (revision.revisionId === undefined || revision.revisionId === null) {
      return null
    }
    return normalizeId(revision.revisionId, fieldLabel)
  }

  const draftRevisionId = resolveNestedRevisionId(product.revision, 'product.revision.revisionId')
  if (draftRevisionId !== null) {
    return draftRevisionId
  }
  const displayRevisionId = resolveNestedRevisionId(
    product.displayRevision,
    'product.displayRevision.revisionId'
  )
  if (displayRevisionId !== null) {
    return displayRevisionId
  }
  if (product.displayRevisionId !== undefined && product.displayRevisionId !== null) {
    return normalizeId(product.displayRevisionId, 'product.displayRevisionId')
  }
  if (product.currentRevisionId !== undefined && product.currentRevisionId !== null) {
    return normalizeId(product.currentRevisionId, 'product.currentRevisionId')
  }
  throw new Error('产品缺少来源版本，无法打开语音弹框')
}

const openProductDetail = (product: Record<string, unknown>) => {
  activeProductDetailId.value = normalizeId(product.productId, 'product.productId')
  activeProductDetailRevisionId.value = resolveProductDetailRevisionId(product)
  productDetailDialogVisible.value = true
}

const openProductDetailById = (productId: number) => {
  activeProductDetailId.value = productId
  activeProductDetailRevisionId.value = null
  productDetailDialogVisible.value = true
}

const openProductVersionCenter = async (payload: {
  productId: string
  displayRevisionId: string
}) => {
  try {
    await router.push({
      name: 'ShowroomAdminProductVersionCenter',
      params: { productId: normalizeId(payload.productId, 'payload.productId') },
      query: { revisionId: String(normalizeId(payload.displayRevisionId, 'payload.displayRevisionId')) }
    })
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  }
}

const handleOpenProductVersionCenterFromDetail = async (payload: {
  productId: number
  revisionId: number
}) => {
  closeProductDetailDialog()
  try {
    await router.push({
      name: 'ShowroomAdminProductVersionCenter',
      params: { productId: payload.productId },
      query: { revisionId: String(payload.revisionId) }
    })
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  }
}

const openProductWholeAssignment = async (product: Record<string, unknown>) => {
  await ensureProductCompanyOptions()
  const revision =
    product.revision && typeof product.revision === 'object'
      ? (product.revision as Record<string, unknown>)
      : {}
  activeProductAssignmentTarget.value = {
    productId: normalizeId(product.productId, 'product.productId'),
    productCode: resolveStringValue(product.productCode),
    nameCn: resolveStringValue(revision.nameCn)
  }
  productAssignmentDialogVisible.value = true
}

const openProductAudioDialog = async (product: Record<string, unknown>) => {
  activeProductAudioTarget.value = {
    productId: normalizeId(product.productId, 'product.productId'),
    productCode: resolveStringValue(product.productCode),
    nameCn:
      resolveStringValue(
        product.displayRevision && typeof product.displayRevision === 'object'
          ? (product.displayRevision as Record<string, unknown>).nameCn
          : ''
      ) || '未命名',
    sourceRevisionId: resolveProductAudioSourceRevisionId(product)
  }
  productAudioDialogVisible.value = true
}

const handleGenerateProductNarrationAudioFromRow = async (product: Record<string, unknown>) => {
  const productId = normalizeId(product.productId, 'product.productId')
  const sourceRevisionId =
    product.sourceRevisionId !== undefined && product.sourceRevisionId !== null
      ? normalizeId(product.sourceRevisionId, 'product.sourceRevisionId')
      : resolveProductAudioSourceRevisionId(product)
  try {
    await ShowroomAdminApi.generateProductNarrationAudio({
      productId,
      sourceRevisionId
    })
    await loadProductRows()
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '产品音频生成')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  }
}

const openProductEdit = async (product: Record<string, unknown>) => {
  try {
    await Promise.all([ensureProductCompanyOptions(), ensureProductMasterOptions()])
    const productId = normalizeId(product.productId, 'product.productId')
    const revision = normalizeProductDetail(await ShowroomAdminApi.getProduct(productId))
    const fields = revision.fields
    const ownerCompanyId = resolveOptionalProductOwnerCompanyId(
      fields.owner_company_id ?? fields.ownerCompanyId
    )
    assignProductForm({
      productId,
      productMasterId: revision.productMasterId,
      productCode: resolveStringValue(product.productCode),
      legacyProductCode: resolveStringValue(revision.legacyProductCode),
      nameCn: resolveStringValue(revision.nameCn),
      nameEn: resolveStringValue(revision.nameEn),
      ownerCompanyId,
      productOwnerType:
        resolveStringValue(fields.product_owner_type ?? fields.productOwnerType) === 'SUBSIDIARY'
          ? 'SUBSIDIARY'
          : 'YINGTAI',
      lifecycleStage:
        resolveStringValue(fields.lifecycle_stage ?? fields.lifecycleStage) === 'R_AND_D'
          ? 'R_AND_D'
          : 'REGISTERED',
      targetMarket: resolveStringValue(fields.target_market ?? fields.targetMarket),
      targetMarketEn: resolveStringValue(fields.target_market_en ?? fields.targetMarketEn),
      pipelineLayout: resolveStringValue(fields.pipeline_layout ?? fields.pipelineLayout),
      pipelineLayoutEn: resolveStringValue(fields.pipeline_layout_en ?? fields.pipelineLayoutEn),
      indicationContent: resolveStringValue(fields.indication_content ?? fields.indicationContent),
      indicationContentEn: resolveStringValue(
        fields.indication_content_en ?? fields.indicationContentEn
      ),
      coreSellingPoints: resolveStringValue(
        fields.core_selling_points ?? fields.coreSellingPoints
      ),
      coreSellingPointsEn: resolveStringValue(
        fields.core_selling_points_en ?? fields.coreSellingPointsEn
      ),
      modelSpecification: resolveStringValue(
        fields.model_specification ?? fields.modelSpecification
      ),
      modelSpecificationEn: resolveStringValue(
        fields.model_specification_en ?? fields.modelSpecificationEn
      ),
      registrationCertificate: resolveStringValue(
        fields.registration_certificate ?? fields.registrationCertificate
      ),
      registrationCertificateEn: resolveStringValue(
        fields.registration_certificate_en ?? fields.registrationCertificateEn
      ),
      clinicalEffect: resolveStringValue(fields.clinical_effect ?? fields.clinicalEffect),
      clinicalEffectEn: resolveStringValue(fields.clinical_effect_en ?? fields.clinicalEffectEn),
      fimStatus: resolveStringValue(fields.fim_status ?? fields.fimStatus),
      fimStatusEn: resolveStringValue(fields.fim_status_en ?? fields.fimStatusEn),
      coverImage: resolveStringValue(fields.cover_image ?? fields.coverImage),
      attachments: revision.attachments.map((attachment, index) => ({
        ...attachment,
        displayOrder: index + 1
      }))
    })
    productDialogEditable.value = Boolean(revision.editable)
    applyFixedProductOwner()
    productDialogActiveLanguageTab.value = 'zh'
    syncProductSubmitBaseline(resolveStringValue(revision.status) || 'DRAFT')
    await loadProductNarrationDraft(productId, normalizeId(revision.revisionId, 'product.revisionId'))
    productDialogVisible.value = true
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  }
}

const buildProductPayload = () => {
  const fields = buildProductFieldSnapshot(productForm)
  const payload = {
    productMasterId: productForm.productMasterId ?? undefined,
    productCode: productForm.productCode.trim(),
    legacyProductCode: productForm.legacyProductCode.trim(),
    nameCn: productForm.nameCn.trim(),
    nameEn: productForm.nameEn.trim(),
    fields,
    attachments: buildProductAttachmentPayload()
  }
  if (!payload.productCode || !payload.nameCn || !payload.nameEn) {
    throw new Error('产品编码、中文名称、英文名称为必填项')
  }
  if (!payload.productMasterId) {
    throw new Error('请选择产品主数据')
  }
  if (!payload.fields.owner_company_id) {
    throw new Error('产品归属/类型为必填项')
  }
  return productForm.productId ? { productId: productForm.productId, ...payload } : payload
}

const buildProductCoverGeneratePayload = () => {
  if (!productForm.productId) {
    throw new Error(PRODUCT_AI_COVER_GATE_MESSAGE)
  }
  const productCode = productForm.productCode.trim()
  const nameCn = productForm.nameCn.trim()
  const nameEn = productForm.nameEn.trim()
  const fields = buildProductFieldSnapshot(productForm)
  if (!productCode || !nameCn || !nameEn) {
    throw new Error('产品编码、中文名称、英文名称为必填项')
  }
  if (!fields.owner_company_id) {
    throw new Error('产品归属/类型为必填项')
  }
  return {
    productId: productForm.productId,
    productCode,
    nameCn,
    nameEn,
    fields
  }
}

const requireProductApprovalRoutePreview = () => {
  if (!productApprovalRoutePreview.value) {
    throw new Error('当前登录用户审批路线未解析完成，无法提交产品审批')
  }
  return productApprovalRoutePreview.value
}

const saveCurrentProductDraft = async () => {
  const payload = buildProductPayload()
  const savedRevision = normalizeObject(
    productForm.productId
      ? await ShowroomAdminApi.saveProductDraft(payload)
      : await ShowroomAdminApi.createProduct(payload),
    'savedProductRevision'
  )
  productForm.productId = normalizeId(savedRevision.productId, 'savedProductRevision.productId')
  syncProductSubmitBaseline('DRAFT')
  return savedRevision
}

const saveProductNarrationDraft = async (savedRevision: Record<string, unknown>) => {
  const productId = normalizeId(savedRevision.productId, 'savedProductRevision.productId')
  const sourceRevisionId = normalizeId(savedRevision.revisionId, 'savedProductRevision.revisionId')
  productNarrationDraft.sourceRevisionId = sourceRevisionId
  const zhScriptText = productNarrationDraft.zhScriptText.trim()
  const enScriptText = productNarrationDraft.enScriptText.trim()
  if (!zhScriptText && !enScriptText) {
    if (hasProductNarrationChanges.value || productNarrationDraft.zhVersionId || productNarrationDraft.enVersionId) {
      throw new Error('中英文讲解稿都不能为空')
    }
    resetProductNarrationDraft(sourceRevisionId)
    return null
  }
  if (!zhScriptText || !enScriptText) {
    throw new Error('中英文讲解稿都不能为空')
  }
  const zhVersion = normalizeNarrationVersion(
    await ShowroomAdminApi.saveNarrationDraft({
      targetType: 'PRODUCT',
      targetId: productId,
      sourceRevisionId,
      audienceType: 'PUBLIC',
      language: 'ZH',
      scriptText: zhScriptText,
      audioFileId: productNarrationDraft.zhAudioFileId,
      audioDurationSeconds: productNarrationDraft.zhAudioDurationSeconds,
      generatedByAi: productNarrationDraft.zhGeneratedByAi
    })
  )
  applyProductNarrationVersion(zhVersion)
  const enVersion = normalizeNarrationVersion(
    await ShowroomAdminApi.saveNarrationDraft({
      targetType: 'PRODUCT',
      targetId: productId,
      sourceRevisionId,
      audienceType: 'PUBLIC',
      language: 'EN',
      scriptText: enScriptText,
      audioFileId: productNarrationDraft.enAudioFileId,
      audioDurationSeconds: productNarrationDraft.enAudioDurationSeconds,
      generatedByAi: productNarrationDraft.enGeneratedByAi
    })
  )
  applyProductNarrationVersion(enVersion)
  return { zhVersion, enVersion }
}

const buildProductSubmitPayload = (savedRevision: Record<string, unknown>) => {
  const approvalRoute = requireProductApprovalRoutePreview()
  return {
    targetId: normalizeId(savedRevision.productId, 'savedProductRevision.productId'),
    targetRevisionId: normalizeId(savedRevision.revisionId, 'savedProductRevision.revisionId'),
    fieldCodes: [],
    moduleCode: 'product',
    submittedBy: approvalRoute.submitterUserId,
    submitterDeptId: approvalRoute.submitterDeptId,
    supervisorUserId: approvalRoute.supervisorUserId
  }
}

const buildDirectPublishPayload = (productDetail: ShowroomProductDetail) => {
  return {
    productId: productDetail.productId,
    productMasterId: productDetail.productMasterId ?? undefined,
    productCode: productDetail.productCode.trim(),
    nameCn: productDetail.nameCn.trim(),
    nameEn: productDetail.nameEn.trim(),
    fields: { ...productDetail.fields },
    sourceRevisionId: productDetail.revisionId,
    narrationScriptText: null,
    narrationGeneratedByAi: false,
    attachments: productDetail.attachments
  }
}

const handleSaveProduct = async () => {
  savingProduct.value = true
  try {
    const savedRevision = await saveCurrentProductDraft()
    await saveProductNarrationDraft(savedRevision)
    productDialogVisible.value = false
    message.success('产品草稿已保存')
    await loadProductRows()
  } catch (error) {
    message.error(resolveError(error).message)
    throw error
  } finally {
    savingProduct.value = false
  }
}

const handlePublishListProduct = async (product: Record<string, unknown>) => {
  const productId = normalizeId(product.productId, 'product.productId')
  publishingProductId.value = productId
  try {
    const productDetail = normalizeProductDetail(await ShowroomAdminApi.getProduct(productId))
    if (!isDirectPublishableProductStatus(productDetail.status)) {
      throw new Error('当前产品状态不支持直接发布')
    }
    const publishResult = await ShowroomAdminApi.publishProduct(buildDirectPublishPayload(productDetail))
    if (publishResult.materialBlockers?.length) {
      message.warning(
        `产品字段已发布，版本中心存在 ${publishResult.materialBlockers.length} 个物料 blocker`
      )
    } else {
      message.success('产品新版本已发布')
    }
    await loadProductRows()
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '产品发布')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    publishingProductId.value = null
  }
}

const handleSubmitProduct = async () => {
  submittingProduct.value = true
  try {
    if (!canSubmitProduct.value) {
      throw new Error('当前没有可提交的产品草稿变更')
    }
    const savedRevision = await saveCurrentProductDraft()
    await saveProductNarrationDraft(savedRevision)
    await ShowroomAdminApi.submitProduct(buildProductSubmitPayload(savedRevision))
    productDialogVisible.value = false
    message.success('产品变更已提交审批')
    await loadProductRows()
  } catch (error) {
    message.error(resolveError(error).message)
    throw error
  } finally {
    submittingProduct.value = false
  }
}

const handleDeleteProduct = async (product: Record<string, unknown>) => {
  const productId = normalizeId(product.productId, 'product.productId')
  await message.confirm('确认删除该产品吗？')
  await ShowroomAdminApi.deleteProduct(productId)
  message.success('产品已删除')
  await loadProductRows()
}

const handleExportProductExcel = async () => {
  exportingProductExcel.value = true
  try {
    await message.exportConfirm()
    const data = await ShowroomAdminApi.exportProductExcel(
      buildProductPageParams(productPageNo.value, productPageSize.value)
    )
    download.zip(data, 'showroom-product-resource-package.zip')
  } catch (error) {
    if (!isUserCanceledAction(error)) {
      message.error(resolveError(error).message)
      throw error
    }
  } finally {
    exportingProductExcel.value = false
  }
}

const handleProductSearch = async (filters: ProductListFilters) => {
  productFilters.value = {
    productId: null,
    keyword: filters.keyword.trim(),
    lifecycleStage: filters.lifecycleStage,
    incompleteStatus: filters.incompleteStatus,
    approvalStatus: filters.approvalStatus
  }
  productPageNo.value = 1
  await loadProductRows()
}

const handleProductPageChange = async (pagination: { pageNo: number; pageSize: number }) => {
  productPageNo.value = pagination.pageNo
  productPageSize.value = pagination.pageSize
  await loadProductRows()
}

const handleProductManageTabChange = async (tabName: string | number) => {
  if (tabName === 'award') {
    await loadAwardRows()
  }
}

const handleAwardSearch = async (keyword: string) => {
  awardKeyword.value = keyword
  awardPageNo.value = 1
  await loadAwardRows()
}

const handleAwardPageChange = async (pagination: { pageNo: number; pageSize: number }) => {
  awardPageNo.value = pagination.pageNo
  awardPageSize.value = pagination.pageSize
  await loadAwardRows()
}

const resetAwardNarrationDraft = (sourceRevisionId: number | null) => {
  awardNarrationDraft.sourceRevisionId = sourceRevisionId
  awardNarrationDraft.zhVersionId = null
  awardNarrationDraft.enVersionId = null
  awardNarrationDraft.zhScriptText = awardForm.descriptionZh.trim()
  awardNarrationDraft.enScriptText = awardForm.descriptionEn.trim()
  awardNarrationDraft.zhAudioFileId = null
  awardNarrationDraft.enAudioFileId = null
  awardNarrationDraft.zhAudioUrl = ''
  awardNarrationDraft.enAudioUrl = ''
  awardNarrationDraft.zhAudioDurationSeconds = null
  awardNarrationDraft.enAudioDurationSeconds = null
  awardNarrationDraft.voice = ''
  awardNarrationBaselineZhScript.value = awardNarrationDraft.zhScriptText
  awardNarrationBaselineEnScript.value = awardNarrationDraft.enScriptText
}

const applyAwardNarrationVersion = (version: ShowroomNarrationVersionRecord) => {
  awardNarrationDraft.sourceRevisionId = version.sourceRevisionId
  if (version.key.language === 'EN') {
    awardNarrationDraft.enVersionId = version.id
    awardNarrationDraft.enScriptText = version.scriptText
    awardNarrationDraft.enAudioFileId = version.audioFileId || null
    awardNarrationDraft.enAudioUrl = version.audioUrl || ''
    awardNarrationDraft.enAudioDurationSeconds = version.audioDurationSeconds || null
    awardNarrationBaselineEnScript.value = version.scriptText.trim()
  } else {
    awardNarrationDraft.zhVersionId = version.id
    awardNarrationDraft.zhScriptText = version.scriptText
    awardNarrationDraft.zhAudioFileId = version.audioFileId || null
    awardNarrationDraft.zhAudioUrl = version.audioUrl || ''
    awardNarrationDraft.zhAudioDurationSeconds = version.audioDurationSeconds || null
    awardNarrationBaselineZhScript.value = version.scriptText.trim()
  }
  if (version.voice) {
    awardNarrationDraft.voice = version.voice
  }
}

const loadAwardNarrationDraft = async (awardId: number, sourceRevisionId: number | null) => {
  resetAwardNarrationDraft(sourceRevisionId)
  await Promise.all(
    (['ZH', 'EN'] as const).map(async (language) => {
      try {
        const version = normalizeNarrationVersion(
          await ShowroomAdminApi.getNarration({
            targetType: 'AWARD',
            targetId: awardId,
            audienceType: 'PUBLIC',
            language
          })
        )
        applyAwardNarrationVersion(version)
      } catch (error) {
        const resolved = resolveError(error)
        if (!isProductNarrationNotFoundError(resolved.message)) {
          throw resolved
        }
      }
    })
  )
}

const openAwardEdit = async (award: ShowroomAwardPageRowRespVO) => {
  const detail = normalizeObject(await ShowroomAdminApi.getAward(award.awardId), 'awardDetail')
  awardForm.awardId = normalizeId(detail.awardId, 'awardDetail.awardId')
  awardForm.awardCode = String(detail.awardCode || '')
  awardForm.nameCn = String(detail.nameCn || '')
  awardForm.nameEn = String(detail.nameEn || '')
  awardForm.descriptionZh = String(detail.descriptionZh || '')
  awardForm.descriptionEn = String(detail.descriptionEn || '')
  awardForm.issuer = String(detail.issuer || '')
  awardForm.awardDateText = String(detail.awardDateText || '')
  awardForm.coverImage = String(detail.coverImageUrl || detail.coverImage || '')
  const sourceRevisionId = normalizeId(detail.currentRevisionId ?? detail.revisionId, 'awardDetail.revisionId')
  await loadAwardNarrationDraft(awardForm.awardId, sourceRevisionId)
  awardDialogVisible.value = true
}

const buildAwardDraftPayload = () => {
  if (!awardForm.awardId || !awardForm.awardCode || !awardForm.nameCn.trim()) {
    throw new Error('奖项编码和中文名称为必填项')
  }
  return {
    awardId: awardForm.awardId,
    awardCode: awardForm.awardCode.trim(),
    nameCn: awardForm.nameCn.trim(),
    nameEn: awardForm.nameEn.trim(),
    descriptionZh: awardForm.descriptionZh.trim(),
    descriptionEn: awardForm.descriptionEn.trim(),
    issuer: awardForm.issuer.trim(),
    awardDateText: awardForm.awardDateText.trim(),
    coverImage: awardForm.coverImage.trim()
  }
}

const saveAwardDraftAndRefreshNarrationSource = async () => {
  const saved = normalizeObject(await ShowroomAdminApi.saveAwardDraft(buildAwardDraftPayload()), 'awardDraft')
  const sourceRevisionId = normalizeId(saved.revisionId ?? saved.currentRevisionId, 'awardDraft.revisionId')
  awardNarrationDraft.sourceRevisionId = sourceRevisionId
  return {
    awardId: normalizeId(saved.awardId ?? awardForm.awardId, 'awardDraft.awardId'),
    sourceRevisionId
  }
}

const saveAwardNarrationDraft = async (language: 'ZH' | 'EN') => {
  if (!awardNarrationDraft.sourceRevisionId) {
    throw new Error('奖项讲解缺少来源版本，无法生成语音')
  }
  const scriptText = language === 'ZH' ? awardForm.descriptionZh.trim() : awardForm.descriptionEn.trim()
  if (!scriptText) {
    throw new Error(language === 'ZH' ? '中文讲解不能为空' : '英文讲解不能为空')
  }
  const version = normalizeNarrationVersion(
    await ShowroomAdminApi.generateNarrationScript({
      targetType: 'AWARD',
      targetId: awardForm.awardId,
      sourceRevisionId: awardNarrationDraft.sourceRevisionId,
      audienceType: 'PUBLIC',
      language,
      scriptText,
      generatedByAi: false
    })
  )
  applyAwardNarrationVersion(version)
  return version
}

const handleGenerateAwardNarrationAudio = async () => {
  generatingAwardAudio.value = true
  try {
    await saveAwardDraftAndRefreshNarrationSource()
    const zhDraft = await saveAwardNarrationDraft('ZH')
    const enDraft = await saveAwardNarrationDraft('EN')
    const zhAudio = normalizeNarrationVersion(
      await ShowroomAdminApi.generateNarrationAudio({ narrationVersionId: zhDraft.id })
    )
    applyAwardNarrationVersion(zhAudio)
    const enAudio = normalizeNarrationVersion(
      await ShowroomAdminApi.generateNarrationAudio({ narrationVersionId: enDraft.id })
    )
    applyAwardNarrationVersion(enAudio)
    message.success('奖项中英文语音已生成')
  } catch (error) {
    message.error(resolveError(error).message)
    throw error
  } finally {
    generatingAwardAudio.value = false
  }
}

const buildAwardPublishPayload = () => {
  if (!awardNarrationDraft.sourceRevisionId) {
    throw new Error('奖项讲解缺少来源版本，无法发布奖项')
  }
  return {
    ...buildAwardDraftPayload(),
    revisionId: awardNarrationDraft.sourceRevisionId
  }
}

const handleSaveAwardDraft = async () => {
  savingAward.value = true
  try {
    await saveAwardDraftAndRefreshNarrationSource()
    message.success('奖项草稿已保存')
    awardDialogVisible.value = false
    await loadAwardRows()
  } catch (error) {
    message.error(resolveError(error).message)
    throw error
  } finally {
    savingAward.value = false
  }
}

const handlePublishAward = async () => {
  savingAward.value = true
  try {
    if (awardNarrationDraftStale.value) {
      throw new Error('奖项讲解内容已变更，请先重新生成中英文语音')
    }
    if (!awardNarrationDraft.zhAudioFileId) {
      throw new Error('中文语音未生成，无法发布奖项')
    }
    if (!awardNarrationDraft.enAudioFileId) {
      throw new Error('英文语音未生成，无法发布奖项')
    }
    await ShowroomAdminApi.publishAward(buildAwardPublishPayload())
    message.success('奖项已发布')
    awardDialogVisible.value = false
    await loadAwardRows()
  } catch (error) {
    message.error(resolveError(error).message)
    throw error
  } finally {
    savingAward.value = false
  }
}

const handleDeleteAward = async (award: ShowroomAwardPageRowRespVO) => {
  await message.confirm(`确认删除奖项 ${award.awardCode} 吗？`)
  await ShowroomAdminApi.deleteAward(award.awardId)
  message.success('奖项已删除')
  await loadAwardRows()
}

const handleGenerateAwardCoverImage = async (award: ShowroomAwardPageRowRespVO) => {
  generatingCoverAwardId.value = award.awardId
  try {
    await ShowroomAdminApi.generateAwardCoverImage({ awardId: award.awardId })
    if (awardDialogVisible.value && awardForm.awardId === award.awardId) {
      awardDialogVisible.value = false
    }
    await loadAwardRows()
    message.success('奖项封面已生成并发布新版本')
  } catch (error) {
    message.error(resolveError(error).message)
    throw error
  } finally {
    generatingCoverAwardId.value = null
  }
}

const openBatchMediaSummary = (
  title: string,
  kind: BatchMediaSummaryKind,
  result: BatchMediaSummaryResult
) => {
  batchMediaSummaryTitle.value = title
  batchMediaSummaryKind.value = kind
  batchMediaSummaryResult.value = result
  batchMediaSummaryDialogVisible.value = true
}

const selectBatchCoverGenerationMode = async (): Promise<ShowroomProductCoverGenerationMode | null> => {
  try {
    await ElMessageBox.confirm(
      '请先选择本次批量封面的生成范围。未发布产品仍会自动跳过。',
      '批量生成封面',
      {
        confirmButtonText: '重新生成所有',
        cancelButtonText: '只生成未上传的',
        distinguishCancelAndClose: true,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        type: 'warning'
      }
    )
    return 'ALL'
  } catch (error) {
    if (error === 'cancel') {
      return 'MISSING_ONLY'
    }
    if (error === 'close') {
      return null
    }
    throw error
  }
}

const handleStartBatchGenerateNarrationScriptTask = async () => {
  try {
    await message.confirm(
      '确认按当前筛选条件批量补齐产品当前版本的中英文讲解稿吗？已存在的语言会自动跳过。'
    )
  } catch (error) {
    if (isUserCanceledAction(error)) {
      return
    }
    throw error
  }

  batchGeneratingProductNarrationScript.value = true
  try {
    const taskStatus = normalizeProductNarrationScriptTaskStatus(
      await ShowroomAdminApi.startBatchGenerateNarrationScriptTask(buildProductBatchGeneratePayload()),
      'productNarrationScriptTaskStatus'
    )
    productNarrationScriptTaskStatus.value = taskStatus
    await loadProductRows()
    await loadProductNarrationScriptTaskStatus()
    if (taskStatus.active) {
      message.success('一键讲解任务已启动，后台会立即执行并每 10 分钟自动续跑')
    } else if (taskStatus.matchedCount === 0) {
      message.success('当前筛选范围没有命中产品')
    } else {
      message.success('当前筛选产品的中英文讲解稿已全部齐全')
    }
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  } finally {
    batchGeneratingProductNarrationScript.value = false
  }
}

const handleBatchGenerateProductSalesCountries = async () => {
  try {
    await message.confirm(
      '确认按当前筛选条件批量补齐产品最新版本的中英文在售国家吗？已存在的语言会自动跳过。'
    )
  } catch (error) {
    if (isUserCanceledAction(error)) {
      return
    }
    throw error
  }

  batchGeneratingProductSalesCountries.value = true
  try {
    const result = normalizeProductSalesCountriesBatchGenerateResult(
      await ShowroomAdminApi.batchGenerateProductSalesCountries(buildProductBatchGeneratePayload()),
      'batchGenerateProductSalesCountriesResult'
    )
    await loadProductRows()
    if (result.failedCount > 0) {
      message.warning('一键在售国家已完成，部分产品处理失败')
    } else if (result.matchedCount === 0) {
      message.success('当前筛选范围没有命中产品')
    } else if (result.updatedProductCount === 0) {
      message.success('当前筛选产品的中英文在售国家已全部齐全')
    } else {
      message.success(`一键在售国家已完成，本轮补齐 ${result.generatedLanguageCount} 个语言在售国家`)
    }
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  } finally {
    batchGeneratingProductSalesCountries.value = false
  }
}

const handleBatchPublishProducts = async () => {
  try {
    await message.confirm(
      '确认按当前筛选条件批量发布全部可直发产品吗？不可直发的产品会自动跳过，失败原因会在结果中显示。'
    )
  } catch (error) {
    if (isUserCanceledAction(error)) {
      return
    }
    throw error
  }

  batchPublishingProduct.value = true
  try {
    const result = normalizeBatchMediaSummaryResult(
      await ShowroomAdminApi.batchPublishProducts(buildProductBatchGeneratePayload()),
      'batchPublishProductsResult'
    )
    await loadProductRows()
    openBatchMediaSummary('批量发布结果', 'PUBLISH', result)
    if (result.failedCount > 0) {
      message.warning('批量发布已完成，部分产品发布失败')
    } else if (result.matchedCount === 0) {
      message.success('当前筛选范围没有命中产品')
    } else if (result.publishedCount === 0) {
      message.success('当前筛选产品里没有可直接发布的产品')
    } else {
      message.success('批量发布已完成')
    }
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  } finally {
    batchPublishingProduct.value = false
  }
}

const handleStartBatchTranslatePublishTask = async () => {
  try {
    await message.confirm(
      '确认按当前筛选条件一键翻译并发布产品当前版本吗？已有英文内容会被本次关键词翻译结果覆盖，并生成新的发布版本。'
    )
  } catch (error) {
    if (isUserCanceledAction(error)) {
      return
    }
    throw error
  }

  batchTranslatingPublishingProduct.value = true
  try {
    productTranslatePublishTaskStatus.value = normalizeProductTranslatePublishTaskStatus(
      await ShowroomAdminApi.startBatchTranslatePublishTask(buildProductBatchGeneratePayload()),
      'productTranslatePublishTaskStatus'
    )
    syncProductTranslatePublishTaskPolling()
    if (productTranslatePublishTaskStatus.value.active || productTranslatePublishTaskStatus.value.running) {
      message.success('一键翻译任务已启动，后台将逐个翻译、保存并发布')
    } else if (productTranslatePublishTaskStatus.value.matchedCount === 0) {
      message.success('当前筛选范围没有命中产品')
    } else {
      message.success('一键翻译任务已完成')
    }
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  } finally {
    batchTranslatingPublishingProduct.value = false
  }
}

const handleBatchGenerateProductNarrationAudio = async () => {
  try {
    await message.confirm('确认按当前筛选条件为所有已发布产品批量生成中英文语音吗？未发布产品将自动跳过。')
  } catch (error) {
    if (isUserCanceledAction(error)) {
      return
    }
    throw error
  }

  batchGeneratingProductAudio.value = true
  try {
    const result = normalizeBatchMediaSummaryResult(
      await ShowroomAdminApi.batchGenerateProductNarrationAudio(buildProductBatchGeneratePayload()),
      'batchGenerateProductNarrationAudioResult'
    )
    await loadProductRows()
    await loadBatchProductAudioAutoCheckState()
    openBatchMediaSummary('批量生成中英文语音结果', 'AUDIO', result)
    if (result.failedCount > 0) {
      message.warning('批量生成中英文语音已完成，部分产品处理失败')
    } else if (result.autoCheckEnabled) {
      message.success('批量生成中英文语音首轮已完成，已开启定时检查')
    } else {
      message.success('批量生成中英文语音已完成')
    }
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  } finally {
    batchGeneratingProductAudio.value = false
  }
}

const handleBatchGenerateProductCoverImage = async () => {
  const coverGenerationMode = await selectBatchCoverGenerationMode()
  if (!coverGenerationMode) {
    return
  }

  batchGeneratingProductCover.value = true
  try {
    const result = normalizeBatchMediaSummaryResult(
      await ShowroomAdminApi.batchGenerateProductCoverImage(
        buildProductBatchGeneratePayload(coverGenerationMode)
      ),
      'batchGenerateProductCoverImageResult'
    )
    await loadProductCoverTaskState()
    await loadProductRows()
    if (shouldShowBatchCoverAutoResumeHint(result)) {
      message.warning('已开启后台定时检查，每 10 分钟自动续跑，全部完成后自动停止')
    } else if (result.failedCount > 0) {
      message.warning('批量生成封面已完成，部分产品处理失败')
    } else {
      message.success('批量生成封面已完成')
    }
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  } finally {
    batchGeneratingProductCover.value = false
  }
}

const handleTranslateProductFieldsToEn = async () => {
  if (!canTranslateProductEnglishFields.value) {
    throw new Error('请先填写中文名称、中文字段或中文讲解稿后再翻译')
  }
  translatingProductEnglishFields.value = true
  try {
    const sourceFields = buildProductFieldSnapshot(productForm)
    const nameCn = productForm.nameCn.trim()
    const narrationScriptZh = productNarrationDraft.zhScriptText.trim()
    if (!productForm.productId) {
      const savedRevision = await saveCurrentProductDraft()
      productForm.productId = normalizeId(savedRevision.productId, 'savedProductRevision.productId')
    }
    const translation = normalizeObject(
      await ShowroomAdminApi.translateProductFieldsToEn({
        productId: productForm.productId as number,
        nameCn,
        fields: sourceFields,
        narrationScriptZh: narrationScriptZh || undefined
      }),
      'translatedProductFields'
    )
    const translatedFields = normalizeObject(
      translation.translatedFields,
      'translatedProductFields.translatedFields'
    )
    const translatedNameEn = resolveTranslatedProductText(nameCn, translation.nameEn, '英文名称')
    const translatedTargetMarketEn = resolveTranslatedProductText(
      sourceFields.target_market,
      translatedFields.target_market_en,
      '在售国家'
    )
    const translatedPipelineLayoutEn = resolveTranslatedProductText(
      sourceFields.pipeline_layout,
      translatedFields.pipeline_layout_en,
      'BU'
    )
    const translatedIndicationContentEn = resolveTranslatedProductText(
      sourceFields.indication_content,
      translatedFields.indication_content_en,
      '适应症'
    )
    const translatedCoreSellingPointsEn = resolveTranslatedProductText(
      sourceFields.core_selling_points,
      translatedFields.core_selling_points_en,
      '卖点文案'
    )
    const translatedModelSpecificationEn = resolveTranslatedProductText(
      sourceFields.model_specification,
      translatedFields.model_specification_en,
      '型号规格'
    )
    const translatedRegistrationCertificateEn = resolveTranslatedProductText(
      sourceFields.registration_certificate,
      translatedFields.registration_certificate_en,
      '注册证'
    )
    const translatedClinicalEffectEn = resolveTranslatedProductText(
      sourceFields.clinical_effect,
      translatedFields.clinical_effect_en,
      '临床效果'
    )
    const translatedFimStatusEn = resolveTranslatedProductText(
      sourceFields.fim_status,
      translatedFields.fim_status_en,
      'FIM状态'
    )
    const translatedNarrationScriptEn = resolveTranslatedProductText(
      narrationScriptZh,
      translation.narrationScriptEn,
      '英文讲解稿'
    )

    if (translatedNameEn !== null) {
      productForm.nameEn = translatedNameEn
    }
    if (translatedTargetMarketEn !== null) {
      productForm.targetMarketEn = translatedTargetMarketEn
    }
    if (translatedPipelineLayoutEn !== null) {
      productForm.pipelineLayoutEn = translatedPipelineLayoutEn
    }
    if (translatedIndicationContentEn !== null) {
      productForm.indicationContentEn = translatedIndicationContentEn
    }
    if (translatedCoreSellingPointsEn !== null) {
      productForm.coreSellingPointsEn = translatedCoreSellingPointsEn
    }
    if (translatedModelSpecificationEn !== null) {
      productForm.modelSpecificationEn = translatedModelSpecificationEn
    }
    if (translatedRegistrationCertificateEn !== null) {
      productForm.registrationCertificateEn = translatedRegistrationCertificateEn
    }
    if (translatedClinicalEffectEn !== null) {
      productForm.clinicalEffectEn = translatedClinicalEffectEn
    }
    if (translatedFimStatusEn !== null) {
      productForm.fimStatusEn = translatedFimStatusEn
    }
    if (translatedNarrationScriptEn !== null) {
      productNarrationDraft.enScriptText = translatedNarrationScriptEn
      productNarrationDraft.enGeneratedByAi = true
    }
    productDialogActiveLanguageTab.value = 'en'
    message.success('英文内容已翻译，可继续微调后再生成语音')
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '产品翻译')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    translatingProductEnglishFields.value = false
  }
}

const handleProductAudioDialogGenerated = async () => {
  await loadProductRows()
}

const handleGenerateHallNarrationAudio = async (hall: { hallId: number }) => {
  const hallId = normalizeId(hall.hallId, 'hall.hallId')
  generatingAudioHallId.value = hallId
  try {
    await ShowroomAdminApi.generateHallNarrationAudio({ hallId })
    await loadHallRows()
    message.success('展柜中英文语音已生成')
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '展柜语音生成')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    generatingAudioHallId.value = null
  }
}

const handleBatchGenerateHallNarrationAudio = async () => {
  try {
    await message.confirm('确认为全部展柜生成中文和英文语音吗？已有语音将生成新版本并作为当前可发布版本。')
  } catch (error) {
    if (isUserCanceledAction(error)) {
      return
    }
    throw error
  }

  batchGeneratingHallAudio.value = true
  try {
    const result = normalizeHallNarrationBatchGenerateResult(
      await ShowroomAdminApi.batchGenerateHallNarrationAudio(),
      'batchGenerateHallNarrationAudioResult'
    )
    await loadHallRows()
    if (result.failedCount > 0) {
      const firstFailure = result.failures[0]
      message.warning(
        `一键语音完成：成功 ${result.succeededCount} 个，失败 ${result.failedCount} 个。首个失败：${firstFailure?.hallCode || firstFailure?.name || '未知展柜'} ${firstFailure?.reason || ''}`
      )
    } else {
      message.success(`一键语音已完成：共生成 ${result.succeededCount} 个展柜`)
    }
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '一键展柜语音')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    batchGeneratingHallAudio.value = false
  }
}

const hallPreviewAssetPromptOptions = {
  confirmButtonText: '发布',
  cancelButtonText: '取消',
  inputPattern: /^[1-9]\d*$/,
  inputErrorMessage: '文件ID必须为正整数'
}

const handlePublishHallPreviewAsset = async (hall: { hallId: number }) => {
  const hallId = normalizeId(hall.hallId, 'hall.hallId')
  let promptResult
  try {
    promptResult = await ElMessageBox.prompt(
      '请输入已上传图片文件ID',
      '发布展柜预览图',
      hallPreviewAssetPromptOptions
    )
  } catch (error) {
    if (isUserCanceledAction(error)) {
      return
    }
    throw error
  }
  const imageFileId = normalizeId(promptResult.value, 'hall.previewImageFileId')
  publishingHallPreviewAssetId.value = hallId
  try {
    await ShowroomAdminApi.publishHallPreviewAsset({ hallId, imageFileId })
    await loadHallRows()
    message.success('展柜预览图已发布')
  } catch (error) {
    message.error(resolveError(error).message)
    throw error
  } finally {
    publishingHallPreviewAssetId.value = null
  }
}

const summarizeHallConfigPackageImport = (summary: {
  hallCount: number
  keywordCount: number
  previewAssetCount: number
  narrationCount: number
  backgroundAssetCount: number
  removedHallCount: number
  removedKeywordCount: number
  validatedProductCount: number
  validatedAwardCount: number
}) => {
  return [
    `展柜 ${summary.hallCount} 个`,
    `关键词 ${summary.keywordCount} 个`,
    `预览图 ${summary.previewAssetCount} 个`,
    `语音 ${summary.narrationCount} 个`,
    `背景图 ${summary.backgroundAssetCount} 个`,
    `删除旧展柜 ${summary.removedHallCount} 个`,
    `删除旧关键词 ${summary.removedKeywordCount} 个`,
    `校验产品 ${summary.validatedProductCount} 个`,
    `校验奖项 ${summary.validatedAwardCount} 个`
  ].join('，')
}

const handleExportHallConfigPackage = async () => {
  exportingHallConfigPackage.value = true
  try {
    const data = await ShowroomAdminApi.exportHallConfigPackage()
    download.zip(data, 'showroom-hall-config-package.zip')
    message.success('展柜数据包已导出')
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '导出展柜数据包')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    exportingHallConfigPackage.value = false
  }
}

const handleImportHallConfigPackage = async (file: File) => {
  if (!/\.zip$/i.test(file.name)) {
    const error = new Error('仅支持导入 zip 数据包')
    message.error(error.message)
    throw error
  }
  importingHallConfigPackage.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const summary = await ShowroomAdminApi.importHallConfigPackage(formData)
    await loadHallRows()
    message.success(`展柜数据包导入完成：${summarizeHallConfigPackageImport(summary)}`)
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '导入展柜数据包')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    importingHallConfigPackage.value = false
  }
}

const handleGenerateProductNarrationScript = async () => {
  productNarrationActionLoading.value = true
  try {
    const savedRevision = await saveCurrentProductDraft()
    const productId = normalizeId(savedRevision.productId, 'savedProductRevision.productId')
    const sourceRevisionId = normalizeId(savedRevision.revisionId, 'savedProductRevision.revisionId')
    const version = normalizeNarrationVersion(await ShowroomAdminApi.generateProductNarrationScript({ productId }))
    applyProductNarrationVersion(version)
    await loadProductNarrationDraft(productId, sourceRevisionId)
    message.success('讲解稿已生成')
  } catch (error) {
    const resolved = resolveError(error)
    message.error(resolved.message)
    throw resolved
  } finally {
    productNarrationActionLoading.value = false
  }
}

const requireGeneratedCoverImageUrl = (coverImage: unknown) => {
  const coverUrl = resolveStringValue(coverImage).trim()
  if (!coverUrl) {
    throw new Error('AI封面生成失败：后端未返回真实 coverImage URL')
  }
  if (/\/admin-api\/infra\/file\/get\?id=/.test(coverUrl)) {
    throw new Error('AI封面生成失败：后端返回的是文件元数据 URL，不是可读取内容 URL')
  }
  return coverUrl
}

const handleGenerateProductCoverImage = async () => {
  if (productCoverActionLoading.value) {
    message.info(PRODUCT_AI_COVER_LOADING_MESSAGE)
    return
  }
  if (!canGenerateProductAiCover.value) {
    message.warning(PRODUCT_AI_COVER_GATE_MESSAGE)
    return
  }
  productCoverActionLoading.value = true
  message.info(PRODUCT_AI_COVER_PENDING_MESSAGE)
  try {
    const generated = normalizeObject(
      await ShowroomAdminApi.generateProductCoverImage(buildProductCoverGeneratePayload()),
      'generatedProductCover'
    )
    productForm.coverImage = requireGeneratedCoverImageUrl(generated.coverImage)
    message.success('AI封面已生成，已回填表单，尚未保存草稿或发布')
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '产品AI封面')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    productCoverActionLoading.value = false
  }
}

const handleProductDetailSaved = async () => {
  closeProductDetailDialog()
  await loadProductRows()
}

const handleProductDetailSubmitted = async () => {
  closeProductDetailDialog()
  await loadProductRows()
}

const handleProductApprovalCompleted = async () => {
  closeProductDetailDialog()
  await loadProductRows()
}

const handleProductWholeAssignmentSaved = async () => {
  productAssignmentDialogVisible.value = false
  await loadProductRows()
}

const handleProductImportSuccess = async () => {
  importingProductExcel.value = false
  await loadProductRows()
}

const openHallCreate = () => {
  assignHallForm(createEmptyHallForm())
  hallDialogVisible.value = true
}

const openHallEdit = (value: unknown) => {
  const hall = normalizeObject(value, 'hall')
  assignHallForm({
    hallId: normalizeId(hall.hallId, 'hall.hallId'),
    hallCode: resolveStringValue(hall.hallCode),
    name: resolveStringValue(hall.name),
    nameEn: resolveStringValue(hall.nameEn),
    description: resolveStringValue(hall.description),
    descriptionEn: resolveStringValue(hall.descriptionEn)
  })
  hallDialogVisible.value = true
}

const openHallMapping = (hall: unknown) => {
  activeHallMappingRecord.value = hall
  hallMappingDialogVisible.value = true
}

const openHallCanvasLayout = (hall: unknown) => {
  activeHallCanvasRecord.value = hall
  hallCanvasDialogVisible.value = true
}

const buildHallPayload = () => {
  const payload = {
    hallCode: hallForm.hallCode.trim(),
    name: hallForm.name.trim(),
    nameEn: hallForm.nameEn.trim(),
    description: hallForm.description.trim(),
    descriptionEn: hallForm.descriptionEn.trim()
  }
  if (!payload.hallCode || !payload.name || !payload.nameEn) {
    throw new Error('展柜编码、展柜名称、英文名称为必填项')
  }
  return hallForm.hallId
    ? {
        hallId: hallForm.hallId,
        name: payload.name,
        nameEn: payload.nameEn,
        description: payload.description,
        descriptionEn: payload.descriptionEn
      }
    : payload
}

const handleSaveHall = async () => {
  savingHall.value = true
  try {
    const payload = buildHallPayload()
    if (hallForm.hallId) {
      await ShowroomAdminApi.updateHall(payload)
    } else {
      await ShowroomAdminApi.createHall(payload)
    }
    hallDialogVisible.value = false
    message.success('展柜已保存')
    await loadHallRows()
  } catch (error) {
    message.error(resolveError(error).message)
    throw error
  } finally {
    savingHall.value = false
  }
}

const handleDeleteHall = async (value: unknown) => {
  const hall = normalizeObject(value, 'hall')
  const hallId = normalizeId(hall.hallId, 'hall.hallId')
  await message.confirm('确认删除该展柜吗？')
  await ShowroomAdminApi.deleteHall(hallId)
  message.success('展柜已删除')
  await loadHallRows()
}

const handleHallSearch = async (filters: { keyword: string }) => {
  hallKeyword.value = filters.keyword
  await loadHallRows()
}

const handleHallMappingSaved = async () => {
  await loadHallRows()
}

const handleHallCanvasSaved = async () => {
  await loadHallRows()
}

const handleNotifyProductRoute = async () => {
  const request = resolveNotifyProductRouteRequest()
  if (
    !request ||
    request.requestKey === consumedNotifyProductRouteKey.value ||
    activeSection.value !== 'product' ||
    adminLoading.value ||
    handlingNotifyProductRoute.value
  ) {
    return
  }
  handlingNotifyProductRoute.value = true
  try {
    productPageNo.value = 1
    productFilters.value = {
      ...productFilters.value,
      productId: request.targetId,
      keyword: '',
      lifecycleStage: '',
      incompleteStatus: '',
      approvalStatus: ''
    }
    await loadProductRows()
    const matched = productRows.value.some((product) => {
      if (!product || typeof product !== 'object' || Array.isArray(product)) {
        return false
      }
      const record = product as Record<string, unknown>
      const rawId = record.productId
      const normalizedId = typeof rawId === 'string' ? Number(rawId) : rawId
      return typeof normalizedId === 'number' && Number.isFinite(normalizedId) && normalizedId === request.targetId
    })
    if (!matched) {
      throw new Error('站内信关联的产品不存在或当前用户无权查看')
    }
    consumedNotifyProductRouteKey.value = request.requestKey
    if (request.notifyOpen === 'approval') {
      productDetailApprovalChangeRequestId.value = request.changeRequestId ?? null
      openProductDetailById(request.targetId)
    } else if (request.notifyOpen === 'edit') {
      productDetailApprovalChangeRequestId.value = null
      await openProductEdit({ productId: request.targetId })
    } else {
      productDetailApprovalChangeRequestId.value = null
      openProductDetailById(request.targetId)
    }
    await clearNotifyProductRouteQuery()
  } catch (error) {
    message.error(resolveError(error).message)
  } finally {
    handlingNotifyProductRoute.value = false
  }
}

watch(
  () =>
    [
      activeSection.value,
      isShowroomPublicity.value,
      productNarrationScriptTaskStatus.value?.active,
      productNarrationScriptTaskStatus.value?.running
    ] as const,
  () => {
    syncProductNarrationScriptTaskPolling()
  },
  { immediate: true }
)

watch(
  () =>
    [
      activeSection.value,
      isShowroomPublicity.value,
      latestProductCoverTaskSummary.value?.active,
      latestProductCoverTaskSummary.value?.running
    ] as const,
  () => {
    syncProductCoverTaskPolling()
  },
  { immediate: true }
)

watch(
  () =>
    [
      activeSection.value,
      isShowroomPublicity.value,
      productTranslatePublishTaskStatus.value?.active,
      productTranslatePublishTaskStatus.value?.running
    ] as const,
  () => {
    syncProductTranslatePublishTaskPolling()
  },
  { immediate: true }
)

onUnmounted(() => {
  clearProductNarrationScriptTaskPolling()
  clearProductCoverTaskPolling()
  clearProductTranslatePublishTaskPolling()
})

watch(
  () => activeSection.value,
  (section) => {
    if (section === 'company') {
      adminLoading.value = false
      adminLoadError.value = ''
      return
    }
    void loadShowroomAdminData()
  },
  { immediate: true }
)

watch(
  () =>
    [
      activeSection.value,
      adminLoading.value,
      route.query.notifyTargetType,
      route.query.notifyTargetId,
      route.query.notifyOpen
    ] as const,
  () => {
    void handleNotifyProductRoute()
  },
  { immediate: true }
)
</script>

<style scoped>
.showroom-admin-product-dialog__cover-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 14px;
}

.showroom-admin-product-dialog__cover-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 4px;
}

.showroom-admin-product-dialog__cover-tip {
  margin: 0;
  max-width: 260px;
  color: #4b5563;
  font-size: 0.85rem;
  line-height: 1.5;
}

.showroom-admin-product-dialog__tab-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.showroom-admin-product-dialog__tab-copy h4 {
  margin: 0;
  color: #172033;
}

.showroom-admin-product-dialog__tab-copy p {
  margin: 6px 0 0;
  color: #4b5563;
  font-size: 0.88rem;
  line-height: 1.6;
}

.showroom-admin-product-dialog__narration-alert {
  margin-bottom: 12px;
}

.showroom-admin-product-dialog__narration-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.showroom-admin-product-dialog__attachments {
  width: 100%;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-admin-product-dialog__attachment-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.showroom-admin-product-dialog__attachment-tip {
  color: #4b5563;
  font-size: 0.84rem;
  line-height: 1.5;
}

.showroom-admin-product-dialog__attachment-table :deep(.el-table__header th) {
  background: #f7f9fc;
}

.showroom-admin-product-dialog__attachment-link {
  max-width: 100%;
  padding: 0;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  vertical-align: baseline;
}

.showroom-admin-product-dialog__audio-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.showroom-admin-product-dialog__audio-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.showroom-admin-product-dialog__audio-label {
  color: #172033;
  font-size: 0.88rem;
  font-weight: 600;
}

.showroom-admin-product-dialog__audio {
  width: 100%;
}

.showroom-admin-product-dialog__audio-empty {
  color: #4b5563;
  font-size: 0.88rem;
}

.showroom-admin-batch-summary__stats {
  margin-bottom: 16px;
}

.showroom-admin-batch-summary__alert {
  margin-bottom: 16px;
}

.showroom-admin-batch-summary__table :deep(.el-table__header th) {
  background: #f7f9fc;
}

.showroom-admin-table :deep(.el-table__header th) {
  background: #f7f9fc;
}

@media (max-width: 960px) {
  .showroom-admin-product-dialog__tab-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .showroom-admin-product-dialog__audio-grid {
    grid-template-columns: 1fr;
  }
}
</style>
