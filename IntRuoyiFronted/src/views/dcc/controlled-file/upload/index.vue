<template>
  <ContentWrap>
    <div class="upload-header">
      <div class="text-18px font-600">{{ pageTitle }}</div>
      <el-tag v-if="isExternalReview" type="warning">外来文件评审</el-tag>
    </div>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :disabled="uploadSubmitted"
      class="upload-form"
      label-width="86px"
      v-loading="submitLoading"
    >
      <div class="upload-workbench" data-testid="dcc-upload-single-page-workbench">
        <div class="upload-workbench__grid" data-testid="dcc-upload-single-page-grid">
          <div
            class="upload-workbench__column upload-workbench__column--main"
            data-testid="dcc-upload-left-column"
          >
            <section class="upload-section upload-section--scope" data-testid="dcc-upload-section-scope">
        <div class="upload-section__title">提交范围</div>
        <el-form-item v-if="!isExternalReview" label="DCC项目" prop="dccProjectCodeId">
          <el-select
            v-model="formData.dccProjectCodeId"
            class="!w-460px"
            clearable
            filterable
            remote
            reserve-keyword
            :loading="projectCodeOptionsLoading"
            :disabled="submitLoading || projectAttributesLoading"
            :remote-method="loadProjectCodeOptions"
            placeholder="请选择 DCC 项目"
            @visible-change="handleProjectCodeOptionsVisibleChange"
            @change="handleProjectCodeChange"
          >
            <el-option
              v-for="project in projectCodeOptions"
              :key="project.id"
              :label="formatProjectCodeOptionLabel(project)"
              :value="project.id"
            />
          </el-select>
          <el-alert
            v-if="projectCodeOptionsError"
            class="mt-8px !w-560px"
            type="warning"
            :closable="false"
            show-icon
            :title="projectCodeOptionsError"
          />
        </el-form-item>
        <el-form-item v-if="!isExternalReview" label="申请属性">
          <ProjectApplicationAttributes
            ref="projectAttributesPanel"
            :key="projectAttributesPanelKey"
            action="UPLOAD"
            :saved-snapshot="projectAttributesRestoreSnapshot"
            :readonly="submitLoading"
            @change="captureProjectAttributes"
            @restore-defaults="captureProjectAttributes"
          />
        </el-form-item>
        <el-form-item v-if="!isExternalReview" label="项目文件夹" prop="projectFolderId">
          <div class="w-full">
            <el-tree-select
              v-model="formData.projectFolderId" :data="projectFolderTree" node-key="id"
              :props="{ label: 'name', children: 'children', disabled: (node) => !node.active }"
              check-strictly :loading="projectFoldersLoading" :disabled="!formData.dccProjectCodeId || submitLoading"
              placeholder="请选择项目文件夹" class="!w-460px" />
            <el-button class="ml-8px" :loading="projectFoldersLoading"
              :disabled="!formData.dccProjectCodeId || submitLoading" @click="refreshUploadProjectFolders()">刷新目录</el-button>
            <el-alert v-if="projectFoldersError" :title="projectFoldersError" type="error" :closable="false" />
            <span v-else-if="formData.dccProjectCodeId && !projectFoldersLoading && !projectFolders.length">项目尚未生成独立目录</span>
          </div>
        </el-form-item>
        <el-form-item v-if="!isExternalReview" label="登记说明" prop="projectFolderChangeReason">
          <el-input
            v-model="formData.projectFolderChangeReason" maxlength="500" show-word-limit
            placeholder="选填：文件归入此项目文件夹的说明" :disabled="submitLoading" class="!w-560px" />
        </el-form-item>
        <el-form-item v-if="!isExternalReview" label="关联文件">
          <div class="w-full">
            <el-button
              :disabled="!formData.dccProjectCodeId || !formData.projectFolderId || !previewUpload || submitLoading"
              @click="openUploadRelations">关联</el-button>
            <span class="ml-8px">已选 {{ selectedUploadRelations.length }} 个文件</span>
            <div class="mt-8px"><el-tag v-for="file in selectedUploadRelations" :key="file.masterId" class="mr-6px">
              {{ file.fileName }} · {{ file.versionNo }}{{ file.pendingEffect ? ' · 待生效' : '' }}
            </el-tag></div>
          </div>
        </el-form-item>
        <el-form-item v-if="!isExternalReview" label="文件类型" prop="fileTypeTaxonomyId">
          <div class="w-full">
            <el-select
              v-model="formData.fileTypeTaxonomyId" class="!w-560px" clearable filterable
              :loading="fileTypeOptionsLoading || categoryOptionsLoading" :disabled="!formData.dccProjectCodeId || submitLoading || fileTypeOptionsLoading || categoryOptionsLoading || Boolean(categoryOptionsError)"
              placeholder="请选择启用的文件类型" @change="handleFileTypeTaxonomyChange">
              <el-option v-for="option in uploadFileTypeOptions" :key="option.id" :value="option.id" :label="option.names.join(' / ')" :disabled="Boolean(option.uploadBlocker) || option.approvalSourceMode === null" />
            </el-select>
            <el-alert v-if="fileTypeOptionsError" :title="fileTypeOptionsError" type="error" :closable="false" />
            <el-alert v-if="fileTypeCategoryError" :title="fileTypeCategoryError" type="error" :closable="false" />
          </div>
        </el-form-item>
        <el-form-item v-if="isExternalReview" label="文件类别" prop="categoryId">
          <div class="w-full">
            <el-select
              v-model="formData.categoryId"
              class="!w-360px"
              clearable
              filterable
              placeholder="请选择文件类别"
              @change="handleCategoryChange"
            >
              <el-option
                v-for="item in availableCategories"
                :key="item.id"
                :label="item.name"
                :value="item.id as number"
              />
              <template #empty>
                <div class="px-12px py-8px text-12px text-[var(--el-text-color-secondary)]">
                  {{ categorySelectEmptyText }}
                </div>
              </template>
            </el-select>
            <el-alert
              v-if="categoryPreflightMessage"
              class="mt-8px !w-560px"
              type="warning"
              :closable="false"
              show-icon
              :title="categoryPreflightMessage"
            />
          </div>
        </el-form-item>
        <el-form-item v-else label="文件类别" prop="categoryId">
          <div class="w-full">
            <div
              data-testid="dcc-upload-category-leaf-display"
              class="!w-360px rounded-6px border border-[#dbe3ef] bg-[#f8fafc] px-12px py-9px text-13px text-[#172033]"
            >
              {{ selectedFileTypeTaxonomyLeafName || '请先选择文件分类' }}
            </div>
          </div>
        </el-form-item>
        <CustomApplicationMatrixEditor v-if="!isExternalReview && isCustomApprovalSource"
          :model-value="formData.applicationCustomMatrix ?? null" :selected-signoff-department-ids="formData.selectedSignoffDepartmentIds?.map(String) ?? null"
          approval-source-mode="APPLICATION_CUSTOM" :departments="signoffDepartments" :users="approvalUsers"
          :disabled="submitLoading" :loading="signoffDepartmentsLoading || approvalUsersLoading"
          :directory-error="signoffDepartmentsError || approvalUsersError"
          @update:model-value="handleCustomMatrixChange" @update:selected-signoff-department-ids="handleCustomDepartmentsChange" />
        <el-form-item v-if="!isExternalReview && !isCustomApprovalSource" label="会签部门" required data-testid="dcc-upload-signoff-departments">
          <div class="w-full">
            <el-select
              v-model="formData.selectedSignoffDepartmentIds" multiple filterable class="!w-560px"
              :loading="signoffDepartmentsLoading" :disabled="!formData.categoryId || submitLoading || signoffDepartmentsLoading"
              placeholder="选择本次会签部门，可增加或删除" @change="refreshRouteReadiness">
              <el-option v-for="department in signoffDepartments" :key="department.id" :value="department.id" :label="department.name" />
            </el-select>
            <el-alert v-if="signoffDepartmentsError" :title="signoffDepartmentsError" type="error" :closable="false" class="mt-8px" />
            <el-button v-if="signoffDepartmentsError" link @click="loadSignoffDepartments">重新读取部门</el-button>
          </div>
        </el-form-item>
        <el-form-item
          v-if="!isCustomApprovalSource" label="批准人" data-testid="dcc-upload-matrix-approvers">
          <div class="w-full">
            <span v-if="approvalUsersLoading">正在读取启用账号目录...</span>
            <el-alert v-else-if="uploadApproverError" :title="uploadApproverError" type="error" :closable="false" />
            <template v-else-if="uploadApprovers">
              <div v-for="account in uploadApprovers.accounts" :key="account.id">
                {{ account.name }}{{ account.deptName ? `（${account.deptName}）` : '' }} · 账号ID {{ account.id }}
              </div>
              <div class="mt-6px text-12px">{{ uploadApprovers.rule }}；人员由本次文件类型的审批矩阵带出。</div>
              <div v-for="group in uploadApprovers.groups" :key="group.sourceNodeId" class="mt-6px text-12px" data-testid="dcc-upload-approval-rule-group">
                原批准组 {{ group.sourceNodeId }}：{{ group.summary }}
              </div>
              <div v-for="(limitation, index) in uploadApprovers.capabilityLimitations" :key="index" class="mt-6px text-12px">运行能力说明：{{ limitation }}</div>
            </template>
            <span v-else>请选择文件类型并等待正式审批路线预检</span>
            <el-button v-if="approvalUsersError" link @click="loadApprovalUsers">重新读取账号</el-button>
          </div>
        </el-form-item>
        <el-form-item v-if="!isNormalNewUpload && uploadDirectoryTree" label="提交目录" prop="directoryId">
          <div class="w-full">
            <template v-if="uploadDirectoryTree.leafBinding">
              <div class="rounded-6px border border-[#dbe3ef] bg-[#fafcff] px-12px py-9px text-13px text-[#172033]">
                {{ uploadDirectoryTree.bindingDirectoryPath }}
              </div>
              <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
                当前绑定目录已经是最后一层目录，将直接提交到该目录。
              </div>
            </template>
            <template v-else>
              <el-cascader
                v-model="formData.directoryId"
                class="!w-560px"
                :options="uploadDirectoryTree.children"
                :props="directoryCascaderProps"
                clearable
                filterable
                placeholder="请选择绑定目录下的最后一层子目录"
              />
              <div v-if="selectedUploadDirectoryPath" class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
                最终提交路径：{{ selectedUploadDirectoryPath }}
              </div>
              <div v-else class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
                请选择到最后一层叶子目录后再提交。
              </div>
            </template>
          </div>
        </el-form-item>
      </section>

          <section class="upload-section upload-section--file" data-testid="dcc-upload-section-file">
        <div class="upload-section__title">文件信息</div>
        <el-form-item v-if="isExternalReview" label="文件名称" prop="fileName">
          <el-input
            v-model="formData.fileName"
            class="!w-420px"
            clearable
            placeholder="请输入文件名称"
            @input="handleFileNameInput"
            @clear="handleFileNameClear"
          />
        </el-form-item>
        <el-form-item v-else label="文件名称" prop="fileName">
          <div class="w-full">
            <el-input v-model="formData.fileName" class="!w-420px" readonly placeholder="选择原件后显示完整文件名称" />
            <div v-if="projectTemplateFileOptions.length" class="mt-6px text-12px">项目模板参考名称：{{ projectTemplateFileOptions.map(item => item.value).join('、') }}（仅供参考，不限制上传）</div>
            <el-alert v-if="projectFileTemplateError" :title="projectFileTemplateError" type="warning" :closable="false" />
          </div>
        </el-form-item>
        <el-form-item v-if="!isExternalReview" label="下发部门" data-testid="dcc-upload-distribution-departments">
          <div class="w-full">
            <el-select v-model="formData.selectedDistributionDepartmentIds" multiple filterable class="!w-560px"
              :loading="signoffDepartmentsLoading" :disabled="!formData.categoryId || submitLoading || signoffDepartmentsLoading || formData.selectedDistributionDepartmentIds === undefined"
              placeholder="选择本次下发部门，可独立增加或删除">
              <el-option v-for="department in signoffDepartments" :key="department.id" :value="department.id" :label="department.name" />
            </el-select>
            <div class="mt-6px text-12px">首次从文件类型默认会签部门复制；两份名单独立调整。受控且已生效后向所选下发部门全部人员下发。</div>
          </div>
        </el-form-item>
        <el-form-item label="文件编号" prop="fileNumber">
          <el-input
            v-model="formData.fileNumber"
            class="!w-280px"
            placeholder="例如 SOP-001"
          />
        </el-form-item>
        <el-form-item v-if="formData.fileNumber" label="受控版本">
          <div
            data-testid="dcc-upload-current-version-panel"
            class="w-full rounded-8px border border-[var(--el-border-color-light)] bg-[#fafcff] px-12px py-10px text-13px"
          >
            <el-skeleton v-if="currentVersionLookupLoading" :rows="2" animated />
            <template v-else-if="currentVersionLookupError">
              <el-alert
                :closable="false"
                :title="currentVersionLookupError"
                type="error"
                show-icon
              />
            </template>
            <template v-else-if="currentVersionInfo?.matched">
              <div class="font-600 text-[var(--el-text-color-primary)]">
                {{ currentVersionInfo.fileName || '-' }} · {{ currentVersionInfo.currentVersionNo || '-' }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                文件编号：{{ currentVersionInfo.fileNumber }}；状态：{{ currentVersionInfo.status || '-' }}
              </div>
              <el-alert
                class="mt-8px" :closable="false" type="error" show-icon
                title="该逻辑文件已存在；请到文件浏览中检出后再检入新版本。"
              />
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                产品：{{ currentVersionInfo.productName || currentVersionInfo.productCode || '-' }}；
                修改中：{{ currentVersionInfo.modifying ? '是' : '否' }}
              </div>
              <el-alert
                v-if="currentVersionProjectionBlockReason"
                class="mt-8px"
                :closable="false"
                :title="currentVersionProjectionBlockReason"
                type="warning"
                show-icon
              />
              <el-alert
                v-if="isRequestedVersionDuplicate"
                class="mt-8px"
                :closable="false"
                :title="versionDuplicatePreflightMessage"
                type="error"
                show-icon
              />
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                原版本路径：{{ currentVersionInfo.originalFilePath || '-' }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                源文件路径：{{ currentVersionInfo.sourceFilePath || '-' }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                受控文件路径：{{ currentVersionInfo.stampedFilePath || currentVersionInfo.publishedFilePath || '-' }}
              </div>
            </template>
            <template v-else>
              未查询到同编号受控版本，将创建新的受控文件主档，并按新建规则校验。
            </template>
          </div>
        </el-form-item>
        <el-form-item label="产品编号" prop="productCode">
          <el-input
            v-model="formData.productCode"
            class="!w-420px"
            readonly
            placeholder="选择 DCC 项目后读取正式产品编码"
          />
          <div
            v-if="isProductRequiredForSelectedCategory"
            data-testid="dcc-upload-product-code-binding-hint"
            class="mt-6px text-12px"
            :class="productCodeBindingHintClass"
          >
            {{ productCodeBindingHintText }}
          </div>
          <div v-if="projectProduct" data-testid="dcc-upload-project-product-identity" class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
            产品：{{ projectProduct.productName || '未绑定正式产品' }}；来源：{{ formatProjectProductSource(projectProduct.source) }}
            <span v-if="projectProduct.productMasterId">；产品主数据ID：{{ projectProduct.productMasterId }}</span>
            <span v-if="projectProduct.productCatalogId">；产品目录ID：{{ projectProduct.productCatalogId }}；关系ID：{{ projectProduct.productRelationId }}；创建申请ID：{{ projectProduct.productCreateRequestId }}</span>
          </div>
          <div v-if="projectProductLoading" class="mt-6px text-12px">正在读取正式项目产品...</div>
          <div v-if="projectProductError" class="mt-6px text-12px text-[var(--el-color-danger)]">{{ projectProductError }}</div>
        </el-form-item>
        <el-form-item :label="isExternalReview ? '版本号' : '初始版本号'" prop="versionNo" :error="submitFieldErrors.versionNo">
          <el-input
            v-model="formData.versionNo"
            class="!w-220px"
            :placeholder="isExternalReview ? '例如 V1.0' : '例如 A/1、B/1'"
          />
          <div v-if="!isExternalReview" class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
            新文件须使用修订版/1格式；检入只生成工作小版本，正式局部变更或换版变更须单独提交审批。
          </div>
        </el-form-item>
        <el-form-item v-if="!isExternalReview" label="生效方式" prop="effectiveMode" data-testid="dcc-upload-effective-mode">
          <div>
            <el-radio-group v-model="formData.effectiveMode" @change="handleEffectiveModeChange">
              <el-radio value="AFTER_STANDARDIZATION_SIGNATURE" :disabled="isExternalReview">文控审核后立即生效</el-radio>
              <el-radio value="DELAYED">延迟生效</el-radio>
            </el-radio-group>
            <div v-if="formData.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE'" class="mt-6px text-12px">由本轮文控标准化审核的真实电子签名时间记录生效事实；受控且已生效后才日常可用并下发。</div>
            <div v-if="formData.effectiveMode === 'DELAYED'" class="mt-6px text-12px">文控审核及必要培训完成后，由原编制人选择生效时间并电子签名确认。</div>
          </div>
        </el-form-item>
        <el-form-item v-if="isExternalReview" label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="formData.effectiveDate"
            class="!w-220px"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择生效日期"
          />
        </el-form-item>
        <el-form-item label="提交备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            class="!w-560px"
            placeholder="请输入本次受控文件提交说明"
          />
        </el-form-item>
      </section>

      <section class="upload-section upload-section--preflight" data-testid="dcc-upload-preflight-panel">
        <div class="upload-section__title">提交前校验</div>
        <div class="upload-preflight-legend">文件编号/版本 · 文件类别 · 审批人链路 · 项目文件夹 · 浏览权限范围</div>
        <div data-testid="dcc-upload-route-readiness" class="mb-12px">
          <el-alert
            v-if="routeReadinessError"
            type="error"
            :closable="false"
            show-icon
            :title="routeReadinessError"
          />
          <el-alert
            v-else-if="routeReadiness && !routeReadiness.ready"
            type="error"
            :closable="false"
            show-icon
            title="审批路线尚未就绪"
          >
            <template #default>
              <ul class="upload-readiness-blockers">
                <li v-for="blocker in routeReadiness.blockers" :key="`${blocker.reasonCode}-${blocker.stageNo || 0}-${blocker.userId || 0}`">
                  <span>阶段：<template v-if="blocker.stageNo != null">#{{ blocker.stageNo }} · </template>{{ blocker.stageName || blocker.stageCode || '阶段未记录' }}</span>
                  <span>；人员：{{ blocker.userName || '姓名未记录' }}（账号ID：{{ blocker.userId ?? '未记录' }}）</span>
                  <span>；原因：{{ blocker.message }}</span>
                </li>
              </ul>
            </template>
          </el-alert>
          <el-alert
            v-else-if="routeReadiness?.ready && uploadApprovers && !uploadApproverError && !approvalUsersLoading"
            type="success"
            :closable="false"
            show-icon
            :title="`审批路线已就绪，共 ${routeReadiness.nodes.length} 个节点`"
          />
        </div>
        <div class="upload-preflight-grid">
          <div
            v-for="check in uploadPreflightChecks"
            :key="check.key"
            class="upload-preflight-card"
            :class="{ 'is-ok': check.ok, 'is-warning': check.warning, 'is-error': !check.ok && !check.warning }"
          >
            <div class="upload-preflight-card__header">
              <span>{{ check.label }}</span>
              <el-tag size="small" :type="check.ok ? 'success' : check.warning ? 'warning' : 'danger'">
                {{ check.status }}
              </el-tag>
            </div>
            <div class="upload-preflight-card__description">{{ check.description }}</div>
          </div>
        </div>
      </section>
          </div>

          <div
            class="upload-workbench__column upload-workbench__column--side"
            data-testid="dcc-upload-right-column"
          >
          <section class="upload-section upload-section--approval" data-testid="dcc-upload-section-approval">
        <div class="upload-section__title">审批要求</div>
        <el-form-item v-if="!isExternalReview" label="培训要求" prop="needTraining">
          <el-checkbox
            v-model="formData.needTraining"
            data-testid="dcc-upload-need-training"
          >
            需要培训
          </el-checkbox>
        </el-form-item>
      </section>

          <section class="upload-section upload-section--attachment" data-testid="dcc-upload-section-attachment">
        <div class="upload-section__title">附件上传</div>
        <el-form-item label="受控文件" prop="file">
          <div class="w-full">
            <el-upload
              ref="uploadRef"
              action="#"
              accept=".pdf,application/pdf"
              :auto-upload="false"
              :disabled="submitLoading || projectAttributesLoading"
              :limit="1"
              :file-list="fileList"
              :on-change="handleFileChange"
              :before-remove="handleBeforeFileRemove"
              :on-remove="handleFileRemove"
              :on-exceed="handleFileExceed"
            >
              <el-button type="primary" plain :loading="uploadPreviewLoading">
                <Icon icon="ep:upload" class="mr-5px" />
                选择文件
              </el-button>
              <template #tip>
                <div class="mt-8px text-12px text-[var(--el-text-color-secondary)]">
                  必须上传不可编辑 PDF，在线浏览固定使用该版本。
                </div>
              </template>
            </el-upload>
            <el-alert
              v-if="uploadPreviewError"
              data-testid="dcc-upload-preview-error"
              class="mt-12px"
              type="error"
              :closable="false"
              show-icon
              :title="uploadPreviewError"
            />
            <div
              v-if="previewUpload"
              class="mt-12px rounded-8px border border-[var(--el-border-color-light)] bg-[#fafcff] px-12px py-10px text-13px"
            >
              <div class="font-600 text-[var(--el-text-color-primary)]">
                预览文件：{{ previewUpload.fileName }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                {{ previewUpload.contentType }} · {{ formatPreviewFileSize(previewUpload.fileSize) }}
              </div>
            </div>
            <div v-if="previewFileBlob && previewUpload" class="upload-preview-panel mt-12px">
              <div class="mb-8px text-15px font-600">提交前预览</div>
              <ProtectedPdfViewer
                :preview-blob="previewFileBlob"
                :preview-kind="previewUpload.previewKind"
                :onlyoffice-base-url="previewUpload.onlyofficeBaseUrl"
                :onlyoffice-document-url="previewUpload.onlyofficeDocumentUrl"
                :preview-unavailable-reason="previewUpload.previewUnavailableReason"
                :title="previewUpload?.fileName || previewFileBlob?.name || '受控文件预览'"
                :watermark="previewUpload?.watermark || null"
              />
            </div>
          </div>
        </el-form-item>
        <el-form-item label="可编辑版本">
          <div class="w-full">
            <el-upload
              ref="editableUploadRef"
              action="#"
              :accept="EDITABLE_SOURCE_ACCEPT"
              :auto-upload="false"
              :limit="1"
              :file-list="editableFileList"
              :on-change="handleEditableFileChange"
              :before-remove="handleBeforeEditableFileRemove"
              :on-remove="handleEditableFileRemove"
              :on-exceed="handleFileExceed"
            >
              <el-button plain :loading="editableUploadLoading">
                <Icon icon="ep:edit" class="mr-5px" />
                选择可编辑文件
              </el-button>
              <template #tip>
                <div class="mt-8px text-12px text-[var(--el-text-color-secondary)]">
                  非必填；仅拥有可编辑版本下载权限的用户可获取。
                </div>
              </template>
            </el-upload>
            <div
              v-if="editableUpload"
              class="mt-12px rounded-8px border border-[var(--el-border-color-light)] bg-[#fafcff] px-12px py-10px text-13px"
            >
              <div class="font-600 text-[var(--el-text-color-primary)]">
                可编辑文件：{{ editableUpload.fileName }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                {{ editableUpload.contentType }} · {{ formatPreviewFileSize(editableUpload.fileSize) }}
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="图纸 PDF">
          <div class="w-full">
            <el-upload
              ref="drawingPdfUploadRef"
              action="#"
              accept=".pdf,application/pdf"
              :auto-upload="false"
              :disabled="submitLoading || projectAttributesLoading || uploadPreviewLoading || !previewUpload"
              :limit="1"
              :file-list="drawingPdfFileList"
              :on-change="handleDrawingPdfChange"
              :before-remove="handleBeforeDrawingPdfRemove"
              :on-remove="handleDrawingPdfRemove"
              :on-exceed="handleDrawingPdfExceed"
            >
              <el-button plain :loading="uploadDrawingPdfLoading">
                <Icon icon="ep:document" class="mr-5px" />
                选择 PDF
              </el-button>
              <template #tip>
                <div class="mt-8px text-12px text-[var(--el-text-color-secondary)]">
                  源文件为 DWG、SLDPRT、SLDASM、SLDDRW 时必填。
                </div>
              </template>
            </el-upload>
            <div
              v-if="drawingPdfUpload"
              class="mt-12px rounded-8px border border-[var(--el-border-color-light)] bg-[#fafcff] px-12px py-10px text-13px"
            >
              <div class="font-600 text-[var(--el-text-color-primary)]">
                图纸 PDF：{{ drawingPdfUpload.fileName }}
              </div>
              <div class="mt-4px text-[var(--el-text-color-secondary)]">
                {{ drawingPdfUpload.contentType }} · {{ formatPreviewFileSize(drawingPdfUpload.fileSize) }}
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="普通附件">
          <div class="w-full">
            <el-upload
              ref="attachmentUploadRef"
              action="#"
              multiple
              :auto-upload="false"
              :disabled="submitLoading || projectAttributesLoading || uploadPreviewLoading || !previewUpload"
              :file-list="attachmentFileList"
              :on-change="handleAttachmentFileChange"
              :before-remove="handleBeforeAttachmentRemove"
              :on-remove="handleAttachmentRemove"
              data-testid="dcc-upload-attachment-files"
            >
              <el-button plain :loading="uploadAttachmentLoading">
                <Icon icon="ep:paperclip" class="mr-5px" />
                选择附件
              </el-button>
              <template #tip>
                <div class="mt-8px text-12px text-[var(--el-text-color-secondary)]">
                  可一次选择多个普通附件，附件随本次受控文件一并提交，查看详情时可在线预览 PDF、图片等文件。
                </div>
              </template>
            </el-upload>
            <el-alert
              v-if="attachmentUploadBlockMessage"
              data-testid="dcc-upload-attachment-upload-state"
              class="mt-12px"
              type="warning"
              :closable="false"
              show-icon
              :title="attachmentUploadBlockMessage"
            />
            <div
              v-if="attachmentUploads.length"
              class="mt-12px rounded-8px border border-[var(--el-border-color-light)] bg-[#fafcff] px-12px py-10px text-13px"
            >
              <div class="font-600 text-[var(--el-text-color-primary)]">已上传附件</div>
              <div
                v-for="upload in attachmentUploads"
                :key="upload.uid"
                class="mt-6px flex flex-wrap items-center gap-8px text-[var(--el-text-color-secondary)]"
              >
                <Icon icon="ep:paperclip" />
                <span class="text-[var(--el-text-color-primary)]">{{ upload.fileName }}</span>
                <span>{{ upload.contentType || '-' }} · {{ formatPreviewFileSize(upload.fileSize) }}</span>
              </div>
            </div>
          </div>
        </el-form-item>
      </section>


          </div>
        </div>

        <section class="upload-submit-bar" data-testid="dcc-upload-section-submit">
          <el-form-item>
            <el-button v-if="!isExternalReview" :loading="submitLoading" :disabled="uploadSubmitted || hasUnreadyAttachmentUploads" @click="saveUploadDraft">保存申请草稿</el-button>
            <el-button
              type="primary"
              :loading="submitLoading"
              :disabled="submitBlockedByRouteReadiness || hasUnreadyAttachmentUploads"
              @click="submitForm"
            >
              <Icon icon="ep:promotion" class="mr-5px" />
              {{ submitButtonText }}
            </el-button>
          </el-form-item>
        </section>
      </div>
    </el-form>
    <DccFileSelector
      v-if="uploadRelationSource" v-model="uploadRelationsVisible" :source="uploadRelationSource"
      purpose="relations" :directories="uploadRelationDirectories" :selected="selectedUploadRelations"
      :load-page="loadUploadRelationPage" :persist="persistUploadRelations" :open-preview="previewUploadRelation" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { h, onActivated } from 'vue'
import { ElMessageBox } from 'element-plus'
import ProjectApplicationAttributes from '../project-attributes/ProjectApplicationAttributes.vue'
import DccFileSelector from '../relations/DccFileSelector.vue'
import type { SelectorSource, DirectoryNode } from '../relations/DccFileSelector.vue'
import type { FileCandidate, SelectorQuery } from '../relations/selector-state'
import { mapReferenceDirectoryNodes, referenceIdentity } from '../relations/project-reference-contract'
import { loadDccSelectorPage } from '@/api/dcc/controlledFile/applicationRead'
import { getProjectDiscoveryPage, type DccDiscoveredProject } from '@/api/dcc/controlledFile/projectDiscovery'
import { getProjectFolders, type ProjectFolder } from '@/api/dcc/controlledFile/projectAttributes'
import { getSimpleDeptList } from '@/api/system/dept'
import { getSimpleUserList } from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
import { signoffIdentity, normalizeSignoffDepartments, normalizeDistributionDepartments, defaultSignoffDepartments, initialUploadDepartmentSelection,
  signoffCategoryKey, signoffRequestKey, signoffDepartmentOptions, approvalUserOptions,
  resolvedUploadApprovers, resolvedUploadApplicationPeople, uploadApplicationActorKey, type UploadApprovalAccount } from './signoff-departments'
import { UploadApprovalMatrixState, isApprovalMatrixStaleError, matrixRuntimeBlockerMessage, refreshApprovalMatrixRejection } from './approval-matrix'
import CustomApplicationMatrixEditor from './CustomApplicationMatrixEditor.vue'
import type { ApplicationCustomMatrix } from '@/api/dcc/controlledFile/workflow'
import { buildProjectFolderTree } from '../basic-data/components/project-folder-tree'
import { readProjectProductIdentity, formatProjectProductSource } from './project-product-identity'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { buildControlledFileViewerPath } from '../view/presentation'
import { buildSubmittedApplicationRoute } from '../shared/submitted-application-navigation'
import type { AttributeSnapshot } from '../project-attributes/state'
import type { FormRules, UploadProps, UploadUserFile } from 'element-plus'
import { formatToDate } from '@/utils/dateUtil'
import type { ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import { getFileCategoryList } from '@/api/dcc/controlledFile/fileCategories'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCodeFileTemplate,
  type DccProjectFileTemplateItemRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import { getFileTypeTaxonomyUploadOptions, resolveFileTypeActiveCategory, type DccFileTypeTaxonomyUploadOption } from '@/api/dcc/controlledFile/fileTypeTaxonomies'
import { buildUploadFileTypePaths, uploadFileTypeIdentity } from './file-type-options'
import {
  cleanupControlledFileUploadSession,
  cleanupControlledFileUploadTicket,
  createControlledFileUploadSessionId,
  getControlledFileCurrentVersion,
  getControlledFileUploadDirectoryTree,
  getControlledFileUploadNameOptions,
  checkControlledFileRouteReadiness,
  previewControlledFileProjectProduct,
  submitControlledFile,
  createWorkingControlledFile,
  uploadControlledFilePreview,
  type ControlledFileCurrentVersionRespVO,
  type ControlledFileProjectProduct,
  type ControlledFileRouteReadinessVO,
  type ControlledFileUploadDirectoryNodeVO,
  type ControlledFileUploadDirectoryTreeVO,
  type ControlledFileUploadNameOptionVO,
  type ControlledFileUploadRespVO
} from '@/api/dcc/controlledFile/workflow'
import {
  DCC_ACTION_PROJECTION_MISSING_REASON,
  hasDccControlledFileActionProjection,
  resolveDccActionProjectionReadonlyReason
} from '../shared/lifecycle'
import {
  applySubmitFailureFeedback,
  buildSubmitFailureFeedback,
  clearSubmitFieldErrors,
  createUploadSubmitterService,
  EDITABLE_SOURCE_ACCEPT,
  buildUploadConfirmationSummary,
  EDITABLE_SOURCE_MESSAGE,
  formatPreviewFileSize,
  resolveUploadErrorMessage,
  resolveUploadPreviewErrorMessage,
  isFileNumberChainConflictMessage,
  isDccProductRequiredForCategoryCode,
  validateDrawingPdfUpload,
  validateControlledFileSelection,
  validateSingleUploadFileSelection,
  validateDccProjectProductCode,
  buildUploadEffectiveChoice,
  validateUploadTickets,
  validateUploadPreviewResponse,
  type UploadFormDraft,
  type UploadConfirmationRow
} from './submitter'

defineOptions({ name: 'DccControlledFileUpload' })

const ProtectedPdfViewer = defineAsyncComponent(() => import('../view/index.vue'))

const route = useRoute()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

interface UploadNameSuggestionItem {
  value: string
  templateItemId?: number
  fileTypeTaxonomyId?: number
  taxonomyPath?: string
  currentVersionNo?: string | null
  controlledFileId?: number | null
  fileNumber?: string | null
}

const formRef = ref()
const projectAttributesPanel = ref<{
  selectProject: (projectId: string | number) => Promise<boolean>
  getSnapshot: () => AttributeSnapshot
}>()
const projectAttributesPanelKey = ref(0)
const projectAttributesRestoreSnapshot = ref<AttributeSnapshot>()
const acceptedProjectCodeId = ref<number | string | null>(null)
const acceptedProjectCodeOption = ref<DccDiscoveredProject>()
const projectAttributesLoading = ref(false)
let projectAttributesSelectionSequence = 0
const uploadRef = ref()
const editableUploadRef = ref()
const drawingPdfUploadRef = ref()
const attachmentUploadRef = ref()
const categories = ref<ControlledFileCategoryVO[]>([])
const categoryOptionsLoading = ref(true)
const projectCodeOptions = ref<DccDiscoveredProject[]>([])
const projectFolders = ref<ProjectFolder[]>([])
const projectFoldersProjectId = ref<string>()
const projectFoldersLoading = ref(false)
const projectFoldersError = ref('')
let projectFolderRequestSequence = 0
const uploadRelationsVisible = ref(false)
const uploadRelationSource = ref<SelectorSource>()
const uploadRelationDirectories = ref<DirectoryNode[]>([])
const selectedUploadRelations = ref<FileCandidate[]>([])
const fileTypeTaxonomies = ref<DccFileTypeTaxonomyUploadOption[]>([])
const fileTypeOptionsLoading = ref(false)
const fileTypeOptionsError = ref('')
const fileTypeCategoryError = ref('')
let fileTypeOptionsRequestSequence = 0
let fileTypeCategoryRequestSequence = 0
let fileTypeChangeRequestSequence = 0
let projectTemplateRequestSequence = 0
const acceptedFileTypeTaxonomyId = ref<number | string | null>(null)
const projectFileTemplateItems = ref<DccProjectFileTemplateItemRespVO[]>([])
const uploadNameOptions = ref<ControlledFileUploadNameOptionVO[]>([])
const uploadDirectoryTree = ref<ControlledFileUploadDirectoryTreeVO>()
const currentVersionInfo = ref<ControlledFileCurrentVersionRespVO>()
const selectedHistoryVersion = ref<string | null>()
const fileList = ref<UploadUserFile[]>([])
const editableFileList = ref<UploadUserFile[]>([])
const drawingPdfFileList = ref<UploadUserFile[]>([])
const attachmentFileList = ref<UploadUserFile[]>([])
const previewUpload = ref<ControlledFileUploadRespVO>()
const editableUpload = ref<ControlledFileUploadRespVO>()
const drawingPdfUpload = ref<ControlledFileUploadRespVO>()
type AttachmentUploadState = ControlledFileUploadRespVO & { uid: string | number }
const attachmentUploads = ref<AttachmentUploadState[]>([])
type AttachmentUploadAttempt = {
  uid: string | number
  generation: number
  fileName: string
  status: 'UPLOADING' | 'READY' | 'FAILED'
}
const attachmentUploadAttempts = ref<AttachmentUploadAttempt[]>([])
const attachmentUploadGenerations = new Map<string, number>()
let sourceUploadRequestSeq = 0
let editableUploadRequestSeq = 0
let drawingPdfUploadRequestSeq = 0
let uploadDirectoryRequestSeq = 0
const previewFileBlob = ref<File | null>(null)
const uploadPreviewError = ref('')
const submitLoading = ref(false)
const projectProductLoading = ref(false)
const projectProductError = ref('')
const projectProductResolvedId = ref<number | string | null>(null)
const projectProduct = ref<ControlledFileProjectProduct>()
let projectProductRequestSequence = 0
let projectProductMounted = true
const uploadPreviewLoading = ref(false)
const editableUploadLoading = ref(false)
const uploadDrawingPdfLoading = ref(false)
const uploadAttachmentLoading = ref(false)
const uploadNameOptionsLoading = ref(false)
const uploadNameOptionsLoadedKey = ref('')
const currentVersionLookupLoading = ref(false)
const currentVersionLookupError = ref('')
const routeReadiness = ref<ControlledFileRouteReadinessVO>()
const approvalMatrixState = ref<UploadApprovalMatrixState>()
let approvalMatrixScopeKey = ''
const routeReadinessLoading = ref(false)
const routeReadinessError = ref('')
let routeReadinessRequestSeq = 0
const signoffDepartments = ref<Array<{ id: string; name: string }>>([])
const signoffDepartmentsLoading = ref(false)
const signoffDepartmentsError = ref('')
const routeReadinessSelectionKey = ref('')
let signoffSelectionCategoryKey = ''
let signoffDirectoryRequestSeq = 0
const approvalUsers = ref<UploadApprovalAccount[]>([])
const approvalUsersLoading = ref(false)
const approvalUsersError = ref('')
let approvalUsersRequestSeq = 0
const uploadApprovalProjection = computed(() => {
  if (approvalUsersError.value) return { error: approvalUsersError.value, result: undefined }
  if (approvalUsersLoading.value || !routeReadiness.value) return { error: '', result: undefined }
  try { return { result: resolvedUploadApprovers(routeReadiness.value, approvalUsers.value), error: '' } }
  catch (cause) { return { result: undefined, error: cause instanceof Error ? cause.message : String(cause) } }
})
const uploadApprovers = computed(() => uploadApprovalProjection.value.result)
const uploadApproverError = computed(() => uploadApprovalProjection.value.error)
const projectCodeOptionsLoading = ref(false)
const projectFileTemplateLoading = ref(false)
const projectCodeOptionsError = ref('')
const projectFileTemplateError = ref('')
const categoryOptionsError = ref('')
let uploadSessionId = createControlledFileUploadSessionId()
const uploadSessionBinding = ref<{ clientSessionId: string; scopedSessionId: string }>()
const attachmentUploadPurpose = { purpose: 'ATTACHMENT' as const }.purpose
const uploadSubmitted = ref(false)
const submitFieldErrors = reactive({
  versionNo: ''
})

const DEFAULT_MANUAL_VERSION_NO = 'V1.0'
const DEFAULT_CONTROLLED_INITIAL_VERSION_NO = 'A/1'
const VERSION_NO_FORMAT_MESSAGE = '版本号格式不正确，请使用 V1.0、V2.0 或 1.0 这类数字版本。'
const CONTROLLED_INITIAL_VERSION_MESSAGE = '初始版本号格式不正确，请使用 A/1、B/1 等“修订版/1”格式。'
const VERSION_NO_PATTERN = /^[Vv]?\d+(?:\.\d+)*$/
const WINDCHILL_VERSION_PATTERN = /^[A-Z]+(?:\/[1-9]\d*)+$/i
const resolveTodayDate = () => formatToDate(new Date())
const isVersionNoTextValid = (versionNo?: string | null) => VERSION_NO_PATTERN.test((versionNo || '').trim())
const resolveNextMajorVersionNo = (currentVersionNo: string | null | undefined) => {
  const matched = (currentVersionNo || '').trim().match(/^[Vv]?(\d+)(?:\.\d+)*$/)
  if (!matched) {
    return DEFAULT_MANUAL_VERSION_NO
  }
  const majorVersion = Number(matched[1])
  const nextMajorVersion = Number.isFinite(majorVersion) ? majorVersion + 1 : 1
  return `V${nextMajorVersion}.0`
}

const resolveProcessTypeByRoute = () =>
  route.path.includes('/external') ? 'EXTERNAL_REVIEW' : 'CONTROLLED_FILE'
const isExternalReview = computed(() => resolveProcessTypeByRoute() === 'EXTERNAL_REVIEW')
const pageTitle = computed(() => (isExternalReview.value ? '外来文件评审' : '受控文件提交'))
const submitButtonText = computed(() => (isExternalReview.value ? '提交评审' : '创建受控文件'))

const submitBlockedByRouteReadiness = computed(() =>
  Boolean(
    !effectiveSelectionValidation.value.valid ||
    (selectedCategory.value &&
      (routeReadinessLoading.value ||
        routeReadinessError.value ||
        routeReadiness.value?.ready === false ||
        approvalUsersLoading.value || approvalUsersError.value || uploadApproverError.value ||
        (!isExternalReview.value && (signoffDepartmentsLoading.value || signoffDepartmentsError.value))))
  )
)

const findAttachmentUploadAttempt = (uid: string | number) =>
  attachmentUploadAttempts.value.find((attempt) => String(attempt.uid) === String(uid))

const nextAttachmentUploadGeneration = (uid: string | number) => {
  const key = String(uid)
  const generation = (attachmentUploadGenerations.get(key) || 0) + 1
  attachmentUploadGenerations.set(key, generation)
  return generation
}

const isCurrentAttachmentUpload = (uid: string | number, generation: number) =>
  attachmentUploadGenerations.get(String(uid)) === generation &&
  findAttachmentUploadAttempt(uid)?.generation === generation

const syncAttachmentUploadLoading = () => {
  uploadAttachmentLoading.value = attachmentUploadAttempts.value.some(
    (attempt) => attempt.status === 'UPLOADING'
  )
}

const hasUnreadyAttachmentUploads = computed(() =>
  attachmentFileList.value.some((file) => file.uid === undefined || findAttachmentUploadAttempt(file.uid)?.status !== 'READY')
)

const attachmentUploadBlockMessage = computed(() => {
  if (attachmentUploadAttempts.value.some((attempt) => attempt.status === 'UPLOADING')) {
    return '普通附件仍在上传，请等待全部附件上传完成后再提交。'
  }
  if (attachmentUploadAttempts.value.some((attempt) => attempt.status === 'FAILED')) {
    return '普通附件上传失败，请移除失败附件后再提交。'
  }
  return ''
})

const resolveReadyAttachmentUploads = (): ControlledFileUploadRespVO[] | undefined => {
  const selectedUids = attachmentFileList.value.map((file) => String(file.uid))
  const readyUploads = selectedUids.map((uid) => {
    const attempt = attachmentUploadAttempts.value.find((candidate) => String(candidate.uid) === uid)
    return attempt?.status === 'READY'
      ? attachmentUploads.value.find((upload) => String(upload.uid) === uid)
      : undefined
  })
  if (readyUploads.some((upload) => !upload) || readyUploads.length !== selectedUids.length) {
    return undefined
  }
  return readyUploads.filter((upload): upload is AttachmentUploadState => Boolean(upload))
}

const canRetryWorkingDraftCreationAfterCurrentVersionConflictMessage = (
  errorMessage = currentVersionLookupError.value
) =>
  !isExternalReview.value &&
  formData.changeType === 'NEW' &&
  uploadSessionBinding.value?.clientSessionId === uploadSessionId &&
  uploadSessionBinding.value.scopedSessionId === previewUpload.value?.sessionId &&
  Boolean(previewUpload.value?.uploadTicket) &&
  isFileNumberChainConflictMessage(errorMessage)

const createInitialUploadDraft = (): UploadFormDraft => ({
  categoryId: null,
  directoryId: null,
  fileName: '',
  fileNumber: '',
  productMasterId: null,
  productCode: '',
  dccProjectCodeId: null,
  projectFolderId: null,
  projectFolderChangeReason: '',
  fileTypeTaxonomyId: null,
  revisionTargetControlledFileId: null,
  revisionSourceControlledFileId: null,
  relatedControlledFileIds: [],
  projectAttributes: undefined,
  selectedSignoffDepartmentIds: undefined,
  selectedDistributionDepartmentIds: undefined,
  approvalMatrixSelection: undefined,
  needTraining: false,
  processType: resolveProcessTypeByRoute(),
  changeType: 'NEW',
  versionNo: isExternalReview.value ? DEFAULT_MANUAL_VERSION_NO : DEFAULT_CONTROLLED_INITIAL_VERSION_NO,
  effectiveMode: null,
  effectiveDate: null,
  remark: ''
})
const formData = reactive<UploadFormDraft>(createInitialUploadDraft())

const fileTypeTaxonomyPathMap = computed(() => buildUploadFileTypePaths(fileTypeTaxonomies.value))
const uploadFileTypeOptions = computed(() => [...fileTypeTaxonomyPathMap.value.values()].filter(option => option.leaf))
const projectTemplateFileOptions = computed<UploadNameSuggestionItem[]>(() => projectFileTemplateItems.value
  .filter(item => String(item.fileTypeTaxonomyId) === String(formData.fileTypeTaxonomyId))
  .map(item => ({ value: item.fileName, fileTypeTaxonomyId: item.fileTypeTaxonomyId, taxonomyPath: item.taxonomyPath })))

const selectedFileTypeTaxonomyPath = computed(() => {
  if (!formData.fileTypeTaxonomyId) {
    return undefined
  }
  return fileTypeTaxonomyPathMap.value.get(String(formData.fileTypeTaxonomyId))
})

const selectedFileTypeTaxonomyLeafName = computed(() => {
  const names = selectedFileTypeTaxonomyPath.value?.names || []
  return names.length ? names[names.length - 1] : ''
})

const isFileTypeTaxonomyDepthValid = computed(
  () => Boolean(selectedFileTypeTaxonomyPath.value?.leaf && (selectedFileTypeTaxonomyPath.value.names.length || 0) >= 3)
)

const selectedCategory = computed(() =>
  categories.value.find((category) => category.id === formData.categoryId)
)
const selectedProjectCode = computed(() =>
  projectCodeOptions.value.find((project) => project.id === formData.dccProjectCodeId)
)
const isNormalNewUpload = computed(() => formData.processType === 'CONTROLLED_FILE' && formData.changeType === 'NEW')
const categorySelectEmptyText = computed(() => {
  return '当前没有可选文件类别'
})
const selectedFileTypeTaxonomyBoundCategories = computed(() => {
  if (isExternalReview.value) {
    return categories.value
  }
  if (!formData.fileTypeTaxonomyId || !isFileTypeTaxonomyDepthValid.value) {
    return []
  }
  return categories.value.filter(
    (category) =>
      category.active &&
      String(category.fileTypeTaxonomyId) === String(formData.fileTypeTaxonomyId)
  )
})
const availableCategories = computed(() =>
  selectedFileTypeTaxonomyBoundCategories.value.filter((category) => category.active)
)
const selectedFileTypeTaxonomyAutoCategory = computed(() =>
  !isExternalReview.value && availableCategories.value.length === 1
    ? availableCategories.value[0]
    : undefined
)
const categoryPreflightMessage = computed(() => {
  if (!isExternalReview.value && (fileTypeOptionsError.value || fileTypeCategoryError.value)) return fileTypeOptionsError.value || fileTypeCategoryError.value
  if (categoryOptionsError.value) {
    return categoryOptionsError.value
  }
  if (isExternalReview.value) {
    if (!categories.value.length || !availableCategories.value.length) {
      return '当前没有可选文件类别：请确认文件类别已启用。'
    }
    return ''
  }
  if (!formData.fileTypeTaxonomyId) {
    return '请先选择文件分类，文件类别将自动显示所选分类的最后一级。'
  }
  if (!isFileTypeTaxonomyDepthValid.value) {
    return '请先选择至少三级文件分类，文件类别将自动取最后一级。'
  }
  if (!selectedFileTypeTaxonomyBoundCategories.value.length) {
    return '当前文件分类暂无可选文件类别，请联系文控管理员配置该叶子节点的唯一正式 DCC 类别。'
  }
  if (selectedFileTypeTaxonomyBoundCategories.value.length > 1) {
    return '当前文件分类叶子节点绑定了多个正式 DCC 类别，请联系文控管理员保留唯一启用类别后再提交。'
  }
  const boundCategory = selectedFileTypeTaxonomyBoundCategories.value[0]
  if (!isNormalNewUpload.value && !boundCategory.directoryId) {
    return '该类别未配置正式默认目录，请联系文控管理员配置后再提交。'
  }
  return ''
})
const isProductRequiredForSelectedCategory = computed(() =>
  isDccProductRequiredForCategoryCode(selectedCategory.value?.code)
)
const isRequiredProjectCodeBound = computed(() => Boolean(projectProduct.value
  && projectProduct.value.source !== 'UNBOUND' && projectProduct.value.projectCodeId === String(formData.dccProjectCodeId)))
const productCodeBindingHintText = computed(() => {
  if (isRequiredProjectCodeBound.value) {
    return `已解析编号：${formData.productCode.trim()}`
  }
  return 'DHF/DMR 类别必须选择已绑定正式产品的 DCC 项目'
})
const productCodeBindingHintClass = computed(() =>
  isRequiredProjectCodeBound.value
    ? 'text-[var(--el-color-success)]'
    : 'text-[var(--el-color-danger)]'
)

const uploadSubmitterService = createUploadSubmitterService({
  uploadPreview: uploadControlledFilePreview,
  submit: submitControlledFile,
  createWorking: createWorkingControlledFile
})

const canLoadUploadNameOptions = computed(
  () =>
    Boolean(formData.dccProjectCodeId) &&
    Boolean(formData.fileTypeTaxonomyId) &&
    isFileTypeTaxonomyDepthValid.value
)

const formatProjectCodeOptionLabel = (project: DccDiscoveredProject) =>
  [project.projectName, project.projectCode].filter(Boolean).join(' · ')

const formRules = reactive<FormRules>({
  dccProjectCodeId: [{ required: true, message: '请选择 DCC 项目', trigger: 'change' }],
  fileTypeTaxonomyId: [
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        if (isExternalReview.value) {
          callback()
          return
        }
        if (!value) {
          callback(new Error('请选择启用的文件类型'))
          return
        }
        const path = fileTypeTaxonomyPathMap.value.get(String(value))
        if (!path || !path.leaf || path.names.length < 3) {
          callback(new Error('请选择至少三级文件分类'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  categoryId: [
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        if (!value) {
          callback(new Error(isExternalReview.value ? '请选择文件类别' : categoryPreflightMessage.value || '文件分类尚未自动匹配文件类别'))
          return
        }
        const category = categories.value.find((item) => String(item.id) === String(value))
        if (!isExternalReview.value && String(category?.fileTypeTaxonomyId) !== String(formData.fileTypeTaxonomyId)) {
          callback(new Error('文件类别必须来自当前文件分类叶子节点，请重新选择文件分类'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  directoryId: [{ validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
    if (!isNormalNewUpload.value && !value) { callback(new Error('请选择最终提交目录')); return }
    callback()
  }, trigger: 'change' }],
  fileName: [
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        const fileName = String(value || '')
        if (!fileName.trim()) { callback(new Error('请选择实际文件')); return }
        if (!isExternalReview.value && (!previewFileBlob.value || previewFileBlob.value.name !== fileName)) {
          callback(new Error('文件名称必须来自本次实际选择的原件')); return
        }
        callback()
      },
      trigger: ['change', 'blur']
    }
  ],
  fileNumber: [{ required: true, message: '请输入文件编号', trigger: 'blur' }],
  versionNo: [
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        const versionNo = String(value || '').trim()
        if (!versionNo) {
          callback(new Error(isExternalReview.value ? '请输入版本号' : '请输入初始版本号'))
          return
        }
        if (!isVersionNoFormatValid.value) {
          callback(new Error(isExternalReview.value ? VERSION_NO_FORMAT_MESSAGE : CONTROLLED_INITIAL_VERSION_MESSAGE))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  effectiveMode: [{ validator: (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
    if (isExternalReview.value) { callback(); return }
    try { buildUploadEffectiveChoice(formData.effectiveMode, formData.effectiveDate, resolveTodayDate()); callback() }
    catch (error) { callback(error instanceof Error ? error : new Error(String(error))) }
  }, trigger: 'change' }],
  effectiveDate: [{ validator: (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
    if (isExternalReview.value) { callback(formData.effectiveDate ? undefined : new Error('请选择生效日期')); return }
    try { buildUploadEffectiveChoice(formData.effectiveMode, formData.effectiveDate, resolveTodayDate()); callback() }
    catch (error) { callback(error instanceof Error ? error : new Error(String(error))) }
  }, trigger: 'change' }]
})

const directoryCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  emitPath: false,
  checkStrictly: false
} as const

const currentVersionProjectionBlockReason = computed(() => {
  const currentVersion = currentVersionInfo.value
  if (!currentVersion?.matched) {
    return ''
  }
  if (!hasDccControlledFileActionProjection(currentVersion)) {
    return DCC_ACTION_PROJECTION_MISSING_REASON
  }
  if (currentVersion.actionProjection?.actionLocked) {
    return resolveDccActionProjectionReadonlyReason(currentVersion)
  }
  return ''
})

let currentVersionLookupTimer: ReturnType<typeof setTimeout> | undefined
let currentVersionLookupSeq = 0

const clearCurrentVersionInfo = () => {
  currentVersionInfo.value = undefined
  selectedHistoryVersion.value = undefined
  currentVersionLookupError.value = ''
}

const resetUploadNameLinkage = (clearVersionNo: boolean) => {
  formData.changeType = 'NEW'
  formData.revisionTargetControlledFileId = null
  formData.revisionSourceControlledFileId = null
  if (clearVersionNo) {
    if (isExternalReview.value) {
      formData.versionNo = DEFAULT_MANUAL_VERSION_NO
    } else {
      formData.versionNo = DEFAULT_CONTROLLED_INITIAL_VERSION_NO
    }
  }
}

const resetUploadNameContext = (clearFileName: boolean) => {
  uploadNameOptions.value = []
  uploadNameOptionsLoading.value = false
  uploadNameOptionsLoadedKey.value = ''
  if (clearFileName) {
    formData.fileName = ''
  }
  resetUploadNameLinkage(true)
  clearCurrentVersionInfo()
}

const resetUploadDirectoryContext = () => {
  uploadDirectoryRequestSeq++
  uploadDirectoryTree.value = undefined
  formData.directoryId = null
}

const resetSelectedPreview = () => {
  resetEditableUpload()
  sourceUploadRequestSeq++
  uploadPreviewLoading.value = false
  uploadSessionBinding.value = undefined
  previewUpload.value = undefined
  fileList.value = []
  previewFileBlob.value = null
  uploadPreviewError.value = ''
}

const resetEditableUpload = () => {
  editableUploadRequestSeq++
  editableUploadLoading.value = false
  editableUpload.value = undefined
  editableFileList.value = []
}

const resetDrawingPdfUpload = () => {
  drawingPdfUploadRequestSeq++
  uploadDrawingPdfLoading.value = false
  drawingPdfUpload.value = undefined
  drawingPdfFileList.value = []
}

const resetAttachmentUploads = () => {
  attachmentUploadAttempts.value.forEach((attempt) => nextAttachmentUploadGeneration(attempt.uid))
  attachmentUploadAttempts.value = []
  attachmentUploads.value = []
  attachmentFileList.value = []
  syncAttachmentUploadLoading()
}

const resetCategorySelectionForFileTypeTaxonomyChange = () => {
  formData.applicationCustomMatrix = null
  formData.relatedControlledFileIds = []
  selectedUploadRelations.value = []
  uploadRelationsVisible.value = false
  uploadRelationSource.value = undefined
  formData.categoryId = null
  formData.selectedSignoffDepartmentIds = undefined
  formData.selectedDistributionDepartmentIds = undefined
  approvalMatrixState.value = undefined
  approvalMatrixScopeKey = ''
  formData.approvalMatrixSelection = undefined
  resetUploadDirectoryContext()
  resetSelectedPreview()
  resetDrawingPdfUpload()
  resetAttachmentUploads()
  clearSubmitFieldErrors(submitFieldErrors)
}

const syncAutoCategoryFromSelectedFileTypeTaxonomy = async () => {
  if (isExternalReview.value) return
  const sequence = ++fileTypeCategoryRequestSequence
  const typeId = formData.fileTypeTaxonomyId
  fileTypeCategoryError.value = ''
  if (typeId == null || !isFileTypeTaxonomyDepthValid.value) return
  try {
    const categoryId = await resolveFileTypeActiveCategory(uploadFileTypeIdentity(typeId))
    if (sequence !== fileTypeCategoryRequestSequence || String(formData.fileTypeTaxonomyId) !== String(typeId)) return
    const matches = availableCategories.value.filter(category => String(category.id) === uploadFileTypeIdentity(categoryId))
    if (availableCategories.value.length !== 1 || matches.length !== 1) throw new Error('文件类型没有唯一可用的正式分类')
    formData.categoryId = matches[0].id
    void applyDccProjectCodeProductNumber()
    if (!isNormalNewUpload.value) await loadUploadDirectoryTree(formData.categoryId)
  } catch (error) {
    if (sequence === fileTypeCategoryRequestSequence && String(formData.fileTypeTaxonomyId) === String(typeId)) {
      formData.categoryId = null
      fileTypeCategoryError.value = resolveUploadErrorMessage(error, '文件类型正式分类解析失败')
    }
  }
}

const hasTemporaryUploadState = () =>
  Boolean(
    previewUpload.value?.uploadTicket || editableUpload.value?.uploadTicket ||
      drawingPdfUpload.value?.uploadTicket ||
      attachmentUploads.value.some((upload) => upload.uploadTicket)
  )

const resolveCurrentUploadCleanupRequestId = () =>
  previewUpload.value?.requestId || editableUpload.value?.requestId || drawingPdfUpload.value?.requestId

const cleanupCurrentUploadSession = async (showSuccess = false) => {
  if (uploadSubmitted.value || !hasTemporaryUploadState()) {
    return true
  }
  const source = previewUpload.value || editableUpload.value || drawingPdfUpload.value
  const requestId = resolveCurrentUploadCleanupRequestId()
  const attachments = [...attachmentUploads.value]
  try {
    let cleanedCount = 0
    if (source?.uploadTicket) {
      const status = await cleanupControlledFileUploadSession(
        source.sessionId,
        requestId
      )
      cleanedCount += status.cleanedCount ?? 0
    }
    for (const upload of attachments) {
      if (upload.uploadTicket) {
        const status = await cleanupControlledFileUploadTicket(
          upload.sessionId,
          upload.uploadTicket,
          upload.requestId
        )
        cleanedCount += status.cleanedCount ?? 0
      }
    }
    if (showSuccess && cleanedCount > 0) {
      message.success('已清理本次上传临时文件')
    }
    return true
  } catch (error) {
    message.error(resolveUploadErrorMessage(error, '临时文件清理失败，请处理后再继续'))
    return false
  }
}

const cleanupTemporaryUploadTicket = async (
  upload: ControlledFileUploadRespVO | undefined,
  showSuccess = false
) => {
  if (uploadSubmitted.value || !upload?.uploadTicket) {
    return true
  }
  try {
    const status = await cleanupControlledFileUploadTicket(
      upload.sessionId,
      upload.uploadTicket,
      upload.requestId
    )
    if (showSuccess && (status.cleanedCount ?? 0) > 0) {
      message.success('已清理本次临时文件')
    }
    return true
  } catch (error) {
    message.error(resolveUploadErrorMessage(error, '图纸 PDF 临时文件清理失败，请处理后再继续'))
    return false
  }
}

const buildUploadPreviewContext = (sessionId = uploadSessionId) => {
  if (!formData.categoryId) {
    throw new Error(isExternalReview.value ? '请先选择文件类别' : categoryPreflightMessage.value || '文件分类尚未自动匹配文件类别')
  }
  return {
    categoryId: formData.categoryId,
    uploadContext: isExternalReview.value ? 'EXTERNAL_REVIEW' as const : 'NEW_UPLOAD' as const,
    dccProjectCodeId: formData.dccProjectCodeId ?? undefined,
    fileTypeTaxonomyId: formData.fileTypeTaxonomyId ?? undefined,
    fileName: formData.fileName,
    sessionId
  }
}

const cleanupStaleUploadResponse = async (
  upload: ControlledFileUploadRespVO,
  label: string
) => {
  try {
    await cleanupControlledFileUploadTicket(upload.sessionId, upload.uploadTicket, upload.requestId)
  } catch (error) {
    message.error(resolveUploadErrorMessage(error, `${label}临时文件清理失败，请联系管理员处理`))
  }
}

const loadProjectCodeOptions = async (keyword = '') => {
  projectCodeOptionsLoading.value = true
  projectCodeOptionsError.value = ''
  try {
    const page = await getProjectDiscoveryPage({
      pageNo: 1,
      pageSize: 50,
      status: DCC_PROJECT_CODE_STATUS_ENABLE,
      keyword: keyword.trim() || undefined
    })
    const acceptedProject = acceptedProjectCodeOption.value
    projectCodeOptions.value = acceptedProject && !page.list.some(project => project.id === acceptedProject.id)
      ? [acceptedProject, ...page.list]
      : page.list
  } catch (error) {
    projectCodeOptions.value = []
    const errorMessage = resolveUploadErrorMessage(error, 'DCC 项目候选加载失败，请确认项目代码权限或联系文控管理员。')
    projectCodeOptionsError.value = errorMessage.includes('DCC 项目候选加载失败')
      ? errorMessage
      : `DCC 项目候选加载失败：${errorMessage}`
    message.error(projectCodeOptionsError.value)
  } finally {
    projectCodeOptionsLoading.value = false
  }
}

const handleProjectCodeOptionsVisibleChange = async (visible: boolean) => {
  if (!visible || projectCodeOptions.value.length || projectCodeOptionsLoading.value) {
    return
  }
  await loadProjectCodeOptions()
}

const resetProjectFileTemplateSelection = (clearTemplate: boolean = true) => {
  projectFileTemplateLoading.value = false
  formData.fileTypeTaxonomyId = null
  acceptedFileTypeTaxonomyId.value = null
  fileTypeCategoryRequestSequence++
  projectTemplateRequestSequence++
  formData.fileNumber = ''
  resetUploadNameContext(true)
  resetCategorySelectionForFileTypeTaxonomyChange()
  if (clearTemplate) {
    projectFileTemplateItems.value = []
    projectFileTemplateError.value = ''
  }
}

const loadUploadFileTypes = async () => {
  const sequence = ++fileTypeOptionsRequestSequence
  fileTypeOptionsLoading.value = true; fileTypeOptionsError.value = ''
  try {
    const rows = await getFileTypeTaxonomyUploadOptions()
    buildUploadFileTypePaths(rows)
    if (sequence === fileTypeOptionsRequestSequence) fileTypeTaxonomies.value = rows
  } catch (error) {
    if (sequence === fileTypeOptionsRequestSequence) {
      fileTypeTaxonomies.value = []
      fileTypeOptionsError.value = resolveUploadErrorMessage(error, '正式文件类型读取失败')
    }
  } finally { if (sequence === fileTypeOptionsRequestSequence) fileTypeOptionsLoading.value = false }
}

const resetCompletedUploadDraft = () => {
  if (!uploadSubmitted.value || submitLoading.value) return
  projectAttributesSelectionSequence++
  projectFolderRequestSequence++
  projectProductRequestSequence++
  routeReadinessRequestSeq++
  signoffDirectoryRequestSeq++
  approvalUsersRequestSeq++
  currentVersionLookupSeq++
  if (currentVersionLookupTimer) clearTimeout(currentVersionLookupTimer)
  currentVersionLookupTimer = undefined
  resetProjectFileTemplateSelection()
  Object.assign(formData, createInitialUploadDraft())
  fileTypeCategoryError.value = ''
  acceptedProjectCodeId.value = null
  acceptedProjectCodeOption.value = undefined
  projectAttributesRestoreSnapshot.value = undefined
  projectAttributesPanelKey.value++
  projectAttributesLoading.value = false
  projectFolders.value = []
  projectFoldersProjectId.value = undefined
  projectFoldersError.value = ''
  projectFoldersLoading.value = false
  projectProduct.value = undefined
  projectProductResolvedId.value = null
  projectProductError.value = ''
  projectProductLoading.value = false
  selectedUploadRelations.value = []
  uploadRelationSource.value = undefined
  uploadRelationsVisible.value = false
  routeReadiness.value = undefined
  routeReadinessSelectionKey.value = ''
  routeReadinessError.value = ''
  routeReadinessLoading.value = false
  signoffSelectionCategoryKey = ''
  signoffDepartmentsError.value = ''
  signoffDepartmentsLoading.value = false
  approvalUsersError.value = ''
  approvalUsersLoading.value = false
  currentVersionLookupLoading.value = false
  uploadRef.value?.clearFiles()
  drawingPdfUploadRef.value?.clearFiles()
  attachmentUploadRef.value?.clearFiles()
  formRef.value?.clearValidate()
  uploadSessionId = createControlledFileUploadSessionId()
  uploadSubmitted.value = false
  void loadApprovalUsers()
  if (!isExternalReview.value) void loadSignoffDepartments()
}

onActivated(resetCompletedUploadDraft)

const loadProjectFileTemplate = async (projectCodeId: number | string) => {
  const sequence = ++projectTemplateRequestSequence
  projectFileTemplateLoading.value = true; projectFileTemplateError.value = ''
  try {
    const template = await getProjectCodeFileTemplate(projectCodeId)
    if (sequence !== projectTemplateRequestSequence || String(formData.dccProjectCodeId) !== String(projectCodeId)) return
    projectFileTemplateItems.value = template.items.filter(item => item.valid === true)
  } catch (error) {
    if (sequence !== projectTemplateRequestSequence || String(formData.dccProjectCodeId) !== String(projectCodeId)) return
    projectFileTemplateItems.value = []
    projectFileTemplateError.value = resolveUploadErrorMessage(error, '可选项目模板参考读取失败')
  } finally {
    if (sequence === projectTemplateRequestSequence && String(formData.dccProjectCodeId) === String(projectCodeId)) projectFileTemplateLoading.value = false
  }
}

const applyDccProjectCodeProductNumber = async () => {
  const sequence = ++projectProductRequestSequence
  const projectCodeId = formData.dccProjectCodeId
  projectProduct.value = undefined
  formData.productMasterId = null
  formData.productCode = ''
  projectProductError.value = ''
  projectProductResolvedId.value = null
  projectProductLoading.value = false
  if (isExternalReview.value || projectCodeId == null) return
  const current = () => projectProductMounted && sequence === projectProductRequestSequence
    && !isExternalReview.value && String(formData.dccProjectCodeId) === String(projectCodeId)
  projectProductLoading.value = true
  try {
    const result = await previewControlledFileProjectProduct(projectCodeId)
    if (!current()) return
    const product = readProjectProductIdentity(result, projectCodeId)
    projectProduct.value = product
    formData.productCode = product.productCode || ''
    projectProductResolvedId.value = projectCodeId
  } catch (error) {
    if (current()) projectProductError.value = resolveUploadErrorMessage(error, '正式项目产品读取失败')
  } finally {
    if (current()) projectProductLoading.value = false
  }
}

const loadBaseData = async () => {
  categoryOptionsLoading.value = true
  categoryOptionsError.value = ''
  try {
    const [categoryList] = await Promise.all([
      getFileCategoryList(),
      loadProjectCodeOptions()
    ])
    categories.value = categoryList.filter((item) => item.active)
  } catch (error) {
    categories.value = []
    const errorMessage = resolveUploadErrorMessage(error, '文件类别候选加载失败，请确认分类权限或联系文控管理员。')
    categoryOptionsError.value = errorMessage.includes('文件类别候选加载失败')
      ? errorMessage
      : `文件类别候选加载失败：${errorMessage}`
    message.error(categoryOptionsError.value)
  } finally {
    categoryOptionsLoading.value = false
  }
}

const buildUploadNameOptionsKey = () => {
  if (!canLoadUploadNameOptions.value || !formData.dccProjectCodeId || !formData.fileTypeTaxonomyId) {
    return ''
  }
  return `${formData.dccProjectCodeId}:${formData.fileTypeTaxonomyId}`
}

const loadUploadNameOptions = async (dccProjectCodeId: number | string, fileTypeTaxonomyId: number | string) => {
  const requestKey = `${dccProjectCodeId}:${fileTypeTaxonomyId}`
  uploadNameOptionsLoading.value = true
  try {
    const options = await getControlledFileUploadNameOptions({
      dccProjectCodeId,
      fileTypeTaxonomyId
    })
    if (buildUploadNameOptionsKey() !== requestKey) {
      return
    }
    uploadNameOptions.value = options
    uploadNameOptionsLoadedKey.value = requestKey
  } catch (error) {
    if (buildUploadNameOptionsKey() === requestKey) {
      uploadNameOptions.value = []
      uploadNameOptionsLoadedKey.value = ''
      message.error(resolveUploadErrorMessage(error, '历史文件名称加载失败，请查看错误提示后重试'))
    }
    throw error
  } finally {
    if (buildUploadNameOptionsKey() === requestKey) {
      uploadNameOptionsLoading.value = false
    }
  }
}

const ensureUploadNameOptionsLoaded = async () => {
  const optionsKey = buildUploadNameOptionsKey()
  if (!optionsKey) {
    return
  }
  if (uploadNameOptionsLoadedKey.value === optionsKey) {
    return
  }
  if (!canLoadUploadNameOptions.value || !formData.dccProjectCodeId || !formData.fileTypeTaxonomyId) {
    return
  }
  await loadUploadNameOptions(formData.dccProjectCodeId, formData.fileTypeTaxonomyId)
}

const uploadProjectAttributesEdited = ref(false)
const uploadProjectAttributesKey = (attributes: AttributeSnapshot['actual']) => JSON.stringify({
  targetMarkets: [...attributes.targetMarkets].sort(),
  licenseHolder: attributes.licenseHolder,
  actualManufacturer: attributes.actualManufacturer,
  documentTransfer: attributes.documentTransfer,
  otherMarket: attributes.otherMarket?.trim() ?? '',
  transferTo: attributes.transferTo?.trim() ?? ''
})
const captureProjectAttributes = (snapshot: AttributeSnapshot) => {
  if (snapshot.applicationType !== 'UPLOAD' || snapshot.projectId !== String(formData.dccProjectCodeId)) {
    return
  }
  formData.projectAttributes = JSON.parse(JSON.stringify(snapshot.actual))
  uploadProjectAttributesEdited.value = uploadProjectAttributesKey(snapshot.actual) !== uploadProjectAttributesKey(snapshot.defaultSource)
}

const hasUploadApplicationInput = () => Boolean(
  formData.applicationCustomMatrix?.approverUserId || formData.applicationCustomMatrix?.documentControlUserId
  ||
  formData.projectFolderId || formData.projectFolderChangeReason?.trim() || formData.fileName?.trim()
  || formData.fileNumber?.trim() || formData.remark?.trim() || formData.effectiveDate || formData.needTraining
  || formData.effectiveMode || formData.relatedControlledFileIds?.length || formData.selectedSignoffDepartmentIds?.length
  || formData.selectedDistributionDepartmentIds?.length
  || (formData.projectAttributes && uploadProjectAttributesEdited.value)
  || fileList.value.length || drawingPdfFileList.value.length || attachmentFileList.value.length
)

const handleProjectCodeChange = async () => {
  const sequence = ++projectAttributesSelectionSequence
  const requestedProjectId = formData.dccProjectCodeId
  const requestedProjectOption = projectCodeOptions.value.find(project => project.id === requestedProjectId)
  const previousProjectId = acceptedProjectCodeId.value
  projectAttributesLoading.value = true
  try {
    if (requestedProjectId !== previousProjectId && previousProjectId != null && (hasUploadApplicationInput() || requestedProjectId == null)) {
      await message.confirm('切换项目会清除已选文件夹、文件、编号、关联及本次部门选择，是否继续？', '确认切换项目')
      if (sequence !== projectAttributesSelectionSequence || formData.dccProjectCodeId !== requestedProjectId) return
    }
    const previousSnapshot = hasTemporaryUploadState() ? projectAttributesPanel.value?.getSnapshot() : undefined
    if (requestedProjectId == null) {
      if (sequence !== projectAttributesSelectionSequence || formData.dccProjectCodeId !== requestedProjectId) return
      delete formData.projectAttributes
      projectAttributesRestoreSnapshot.value = undefined
      projectAttributesPanelKey.value += 1
    } else {
      if (!projectAttributesPanel.value) throw new Error('项目属性组件尚未就绪')
      const accepted = await projectAttributesPanel.value.selectProject(requestedProjectId)
      if (sequence !== projectAttributesSelectionSequence || formData.dccProjectCodeId !== requestedProjectId) return
      if (!accepted) {
        formData.dccProjectCodeId = previousProjectId
        return
      }
    }
    if (!(await cleanupCurrentUploadSession())) {
      if (sequence !== projectAttributesSelectionSequence || formData.dccProjectCodeId !== requestedProjectId) return
      formData.dccProjectCodeId = previousProjectId
      if (previousSnapshot) {
        captureProjectAttributes(previousSnapshot)
        projectAttributesRestoreSnapshot.value = previousSnapshot
        projectAttributesPanelKey.value++
      }
      return
    }
    if (sequence !== projectAttributesSelectionSequence || formData.dccProjectCodeId !== requestedProjectId) return
    acceptedProjectCodeId.value = requestedProjectId
    acceptedProjectCodeOption.value = requestedProjectOption
  } catch (error) {
    if (sequence !== projectAttributesSelectionSequence || formData.dccProjectCodeId !== requestedProjectId) return
    formData.dccProjectCodeId = previousProjectId
    if (error !== 'cancel' && error !== 'close') {
      message.error(error instanceof Error ? error.message : String(error))
    }
    return
  } finally {
    if (sequence === projectAttributesSelectionSequence) projectAttributesLoading.value = false
  }
  projectFolderRequestSequence += 1
  projectFoldersLoading.value = false
  projectFoldersProjectId.value = undefined
  projectFolders.value = []
  projectFoldersError.value = ''
  formData.projectFolderId = null
  formData.projectFolderChangeReason = ''
  formData.relatedControlledFileIds = []
  selectedUploadRelations.value = []
  uploadRelationsVisible.value = false
  uploadRelationSource.value = undefined
  formData.applicationCustomMatrix = null
  formData.selectedSignoffDepartmentIds = undefined
  formData.selectedDistributionDepartmentIds = undefined
  formData.approvalMatrixSelection = undefined
  approvalMatrixState.value = undefined
  approvalMatrixScopeKey = ''
  routeReadinessRequestSeq++
  routeReadiness.value = undefined
  routeReadinessSelectionKey.value = ''
  signoffSelectionCategoryKey = ''
  resetProjectFileTemplateSelection()
  applyDccProjectCodeProductNumber()
  if (formData.dccProjectCodeId) {
    await Promise.all([
      loadUploadProjectFolders(formData.dccProjectCodeId),
      loadProjectFileTemplate(formData.dccProjectCodeId)
    ])
  }
}

const projectFolderTree = computed(() => projectFoldersProjectId.value
  && projectFoldersProjectId.value === String(formData.dccProjectCodeId)
  ? buildProjectFolderTree(projectFoldersProjectId.value, projectFolders.value) : [])
const selectedProjectFolder = computed(() => projectFolders.value.find(folder =>
  projectFoldersProjectId.value === String(formData.dccProjectCodeId)
  && String(folder.projectCodeId) === String(formData.dccProjectCodeId)
  && String(folder.id) === String(formData.projectFolderId) && folder.active))

const loadUploadProjectFolders = async (projectId: number | string) => {
  const sequence = ++projectFolderRequestSequence
  const selectedFolderId = formData.projectFolderId
  projectFoldersLoading.value = true
  projectFoldersError.value = ''
  projectFoldersProjectId.value = undefined
  projectFolders.value = []
  try {
    const rows = await getProjectFolders(projectId)
    if (sequence !== projectFolderRequestSequence || String(formData.dccProjectCodeId) !== String(projectId)) return
    buildProjectFolderTree(String(projectId), rows)
    projectFoldersProjectId.value = String(projectId)
    projectFolders.value = rows
    if (selectedFolderId != null && formData.projectFolderId === selectedFolderId
      && !rows.some(folder => String(folder.id) === String(selectedFolderId) && folder.active)) {
      formData.projectFolderId = null
      message.warning('原项目文件夹已删除或停用，请重新选择项目文件夹')
    }
  } catch (error) {
    if (sequence !== projectFolderRequestSequence || String(formData.dccProjectCodeId) !== String(projectId)) return
    projectFolders.value = []
    projectFoldersError.value = error instanceof Error ? error.message : String(error)
  } finally {
    if (sequence === projectFolderRequestSequence) projectFoldersLoading.value = false
  }
}

const refreshUploadProjectFolders = async (changedProjectId = formData.dccProjectCodeId) => {
  if (submitLoading.value || changedProjectId == null
    || String(changedProjectId) !== String(formData.dccProjectCodeId)) return
  await loadUploadProjectFolders(changedProjectId)
}

defineExpose({ refreshUploadProjectFolders })

const buildUploadRelationSource = (): SelectorSource => {
  if (!selectedProjectCode.value || !selectedProjectFolder.value || !previewUpload.value
    || projectFoldersLoading.value || projectFoldersError.value) throw new Error('请先选择项目文件夹并上传文件')
  const tenantId = referenceIdentity(getVisitTenantId() || getTenantId())
  const projectId = referenceIdentity(formData.dccProjectCodeId)
  const folderId = referenceIdentity(formData.projectFolderId)
  return {
    contextKey: JSON.stringify([tenantId, projectId, folderId, previewUpload.value.sessionId,
      previewUpload.value.fileName, formData.fileNumber, formData.versionNo]),
    tenantId, projectId, folderId, projectName: selectedProjectCode.value.projectName,
    folderName: selectedProjectFolder.value.name, fileName: previewUpload.value.fileName,
    fileNumber: formData.fileNumber, versionNo: formData.versionNo || '', unsubmitted: true
  }
}

const openUploadRelations = () => {
  try {
    uploadRelationSource.value = buildUploadRelationSource()
    uploadRelationDirectories.value = mapReferenceDirectoryNodes(uploadRelationSource.value.tenantId,
      uploadRelationSource.value.projectId, projectFolderTree.value)
    uploadRelationsVisible.value = true
  } catch (error) {
    message.error(error instanceof Error ? error.message : String(error))
  }
}

const loadUploadRelationPage = (query: SelectorQuery) => {
  if (!uploadRelationSource.value) throw new Error('关联窗口尚未打开')
  return loadDccSelectorPage(uploadRelationSource.value.tenantId, query)
}

const persistUploadRelations = async (rows: FileCandidate[]) => {
  const current = buildUploadRelationSource()
  if (current.contextKey !== uploadRelationSource.value?.contextKey) throw new Error('项目或上传文件已变化，请重新打开关联窗口')
  if (rows.some(row => row.tenantId !== current.tenantId)) throw new Error('关联文件租户不一致')
  const copy = rows.map(row => {
    if (row.projectId !== current.projectId) throw new Error('只能关联当前项目文件')
    if (!row.controlled || row.status !== 'ACTIVE' || row.pendingEffect || !row.executable)
      throw new Error('只能关联已生效的受控版本')
    if (!row.canPreview) throw new Error('没有关联文件正文查看权限')
    return { ...row, controlledFileId: referenceIdentity(row.controlledFileId), masterId: referenceIdentity(row.masterId) }
  })
  if (new Set(copy.map(row => row.masterId)).size !== copy.length) throw new Error('关联文件不能重复')
  selectedUploadRelations.value = copy
  formData.relatedControlledFileIds = copy.map(row => row.controlledFileId)
}

const previewUploadRelation = async (row: FileCandidate) => {
  if (!row.canPreview) throw new Error('没有该文件正文查看权限')
  const preview = window.open(buildControlledFileViewerPath(row.controlledFileId, 'browser', route.fullPath), '_blank')
  if (!preview) throw new Error('预览窗口未能打开，请允许此站点打开新窗口')
  preview.opener = null
}

const handleFileTypeTaxonomyChange = async () => {
  const chosen=formData.fileTypeTaxonomyId==null?undefined:fileTypeTaxonomyPathMap.value.get(String(formData.fileTypeTaxonomyId))
  if(chosen && (chosen.uploadBlocker || chosen.approvalSourceMode===null)) {
    message.error(chosen.uploadBlocker || '所选文件类型未配置正式审批来源');formData.fileTypeTaxonomyId=acceptedFileTypeTaxonomyId.value;return
  }
  const sequence = ++fileTypeChangeRequestSequence
  const requestedType = formData.fileTypeTaxonomyId
  const previousType = acceptedFileTypeTaxonomyId.value
  const current = () => sequence === fileTypeChangeRequestSequence && formData.fileTypeTaxonomyId === requestedType
  if (submitLoading.value || projectAttributesLoading.value) { formData.fileTypeTaxonomyId = previousType; return }
  try {
    if (requestedType !== previousType && hasUploadApplicationInput())
      await message.confirm('切换文件类型会清除本次文件、编号和部门选择，是否继续？', '确认切换文件类型')
    if (!current()) return
    if (!(await cleanupCurrentUploadSession())) { if (current()) formData.fileTypeTaxonomyId = previousType; return }
    if (!current()) return
  } catch (error) {
    if (!current()) return
    formData.fileTypeTaxonomyId = previousType
    if (error !== 'cancel' && error !== 'close') message.error(resolveUploadErrorMessage(error, '切换文件类型失败'))
    return
  }
  acceptedFileTypeTaxonomyId.value = requestedType
  fileTypeCategoryRequestSequence++
  formData.fileNumber = ''
  formData.selectedSignoffDepartmentIds = undefined
  resetUploadNameContext(true)
  resetCategorySelectionForFileTypeTaxonomyChange()
  await formRef.value?.validateField?.('fileTypeTaxonomyId').catch(() => undefined)
  await syncAutoCategoryFromSelectedFileTypeTaxonomy()
}

const loadUploadDirectoryTree = async (categoryId: number) => {
  const requestSequence = ++uploadDirectoryRequestSeq
  try {
    const tree = await getControlledFileUploadDirectoryTree(categoryId)
    if (requestSequence !== uploadDirectoryRequestSeq || formData.categoryId !== categoryId) {
      return
    }
    uploadDirectoryTree.value = tree
    formData.directoryId = tree.leafBinding ? tree.bindingDirectoryId : null
  } catch (error) {
    if (requestSequence !== uploadDirectoryRequestSeq || formData.categoryId !== categoryId) {
      return
    }
    uploadDirectoryTree.value = undefined
    formData.directoryId = null
    message.error(resolveUploadErrorMessage(error, '上传目录加载失败，请查看错误提示后重试'))
  }
}

const buildUploadDirectoryPathMap = (tree: ControlledFileUploadDirectoryTreeVO | undefined) => {
  const pathMap = new Map<number, string>()
  if (!tree) {
    return pathMap
  }
  const walk = (nodes: ControlledFileUploadDirectoryNodeVO[], parentPath: string) => {
    nodes.forEach((node) => {
      const currentPath = `${parentPath}/${node.name}`
      pathMap.set(node.id, currentPath)
      if (node.children?.length) {
        walk(node.children, currentPath)
      }
    })
  }
  if (tree.leafBinding) {
    pathMap.set(tree.bindingDirectoryId, tree.bindingDirectoryPath)
  }
  walk(tree.children || [], tree.bindingDirectoryPath)
  return pathMap
}

const uploadDirectoryPathMap = computed(() => buildUploadDirectoryPathMap(uploadDirectoryTree.value))

const selectedUploadDirectoryPath = computed(() => {
  if (!formData.directoryId) {
    return ''
  }
  return uploadDirectoryPathMap.value.get(formData.directoryId) || ''
})
const controlledBrowserPermissionScopeText = computed(() => {
  const categoryName = selectedCategory.value?.name || '未选择文件类别'
  if (isNormalNewUpload.value) {
    return `浏览权限范围：分类 ${categoryName}；项目 ${selectedProjectCode.value?.projectName || '未选择'}；项目文件夹 ${selectedProjectFolder.value?.name || '未选择'}。存储位置由系统在提交时按正式配置确定，正文访问继续校验实际 VIEW 权限。`
  }
  const directoryPath = selectedUploadDirectoryPath.value || '未选择受控浏览目录'
  const projectCode = selectedProjectCode.value?.projectCode || formData.productCode || '未选择项目代码'
  return `浏览权限范围：分类 ${categoryName}；目录 ${directoryPath}；项目代码 ${projectCode}；发布后按正式 VIEW 权限矩阵控制可见人员。`
})
interface UploadPreflightCheck {
  key: string
  label: string
  status: string
  description: string
  ok: boolean
  warning?: boolean
}

const normalizePreflightVersionNo = (value?: string | null) => String(value || '').trim().toUpperCase()

const isRequestedVersionDuplicate = computed(() => {
  if (!isExternalReview.value) {
    return false
  }
  const currentVersionNo = normalizePreflightVersionNo(currentVersionInfo.value?.currentVersionNo)
  const requestedVersionNo = normalizePreflightVersionNo(formData.versionNo)
  return Boolean(currentVersionInfo.value?.matched && currentVersionNo && requestedVersionNo && currentVersionNo === requestedVersionNo)
})
const isVersionNoFormatValid = computed(() => {
  const versionNo = normalizePreflightVersionNo(formData.versionNo)
  if (isExternalReview.value) return Boolean(versionNo && isVersionNoTextValid(versionNo))
  const match = versionNo.match(WINDCHILL_VERSION_PATTERN)
  return Boolean(match && versionNo.endsWith('/1'))
})
const versionFormatPreflightMessage = computed(() =>
  normalizePreflightVersionNo(formData.versionNo) && !isVersionNoFormatValid.value
    ? (isExternalReview.value ? VERSION_NO_FORMAT_MESSAGE : CONTROLLED_INITIAL_VERSION_MESSAGE)
    : ''
)
const versionDuplicatePreflightMessage = computed(() => {
  if (!isRequestedVersionDuplicate.value) {
    return ''
  }
  if (normalizePreflightVersionNo(formData.versionNo) === 'V1.0') {
    return '文件编号已存在，不能重复创建 V1.0 原版，请改用升版流程或更换文件编号。'
  }
  return `文件编号 ${formData.fileNumber.trim()} 的版本 ${normalizePreflightVersionNo(formData.versionNo)} 已存在，请调整升版版本号。`
})

const existingUploadIdentityBlockReason = computed(() =>
  !isExternalReview.value && currentVersionInfo.value?.matched
    ? '该项目、文件分类和文件编号对应的逻辑文件已存在；请到文件浏览中检出后再检入新版本。'
    : ''
)

const handleEffectiveModeChange = () => {
  if (formData.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE' || formData.effectiveMode === 'DELAYED') formData.effectiveDate = null
  formRef.value?.clearValidate?.(['effectiveMode', 'effectiveDate'])
}

const effectiveSelectionValidation = computed(() => {
  if (isExternalReview.value) return { valid: Boolean(formData.effectiveDate), message: formData.effectiveDate ? '' : '请选择生效日期' }
  try {
    buildUploadEffectiveChoice(formData.effectiveMode, formData.effectiveDate, resolveTodayDate())
    return { valid: true, message: '' }
  } catch (error) { return { valid: false, message: resolveUploadErrorMessage(error, '生效方式或日期无效') } }
})
const isCustomApprovalSource = computed(() => selectedFileTypeTaxonomyPath.value?.approvalSourceMode === 'APPLICATION_CUSTOM')
const handleCustomMatrixChange = (value: ApplicationCustomMatrix) => { formData.applicationCustomMatrix = value; void refreshRouteReadiness() }
const handleCustomDepartmentsChange = (ids: string[]) => {
  formData.selectedSignoffDepartmentIds = [...ids]
  if (formData.selectedDistributionDepartmentIds === undefined && ids.length) formData.selectedDistributionDepartmentIds = [...ids]
  void refreshRouteReadiness()
}
const effectiveDatePreflightText = computed(() => {
  if (isExternalReview.value) return formData.effectiveDate ? `生效日期 ${formData.effectiveDate} 已填写。` : '请选择生效日期。'
  if (!effectiveSelectionValidation.value.valid) return effectiveSelectionValidation.value.message
  return formData.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE'
    ? '文控标准化审核完成时按本轮真实电子签名时间生效；不填写预设日期。'
    : formData.effectiveMode === 'DELAYED' ? '延迟生效方式已选择；本次申请不填写具体时间，由原编制人在延迟生效确认任务中选择并电子签名。' : `历史预设生效日期 ${formData.effectiveDate} 已填写；最终按后台正式业务日期校验。`
})

const approvalChainPreflightText = computed(() => {
  if (!selectedCategory.value) {
    return isExternalReview.value ? '请选择文件类别后检查审批人链路' : '请先完成文件分类的文件类别自动匹配后检查审批人链路'
  }
  if (routeReadinessLoading.value) {
    return '正在校验全部审批人岗位、文控权限、电子签名授权和有效签名图片。'
  }
  if (approvalUsersLoading.value) return '正在读取启用账号并核对矩阵批准人。'
  if (approvalUsersError.value || uploadApproverError.value) return approvalUsersError.value || uploadApproverError.value
  if (routeReadinessError.value) {
    return routeReadinessError.value
  }
  if (!routeReadiness.value) {
    return '等待服务端审批路线校验。'
  }
  if (!routeReadiness.value.ready) {
    return `审批路线存在 ${routeReadiness.value.blockers.length} 项缺失，请逐项处理。`
  }
  return `审批路线 ${routeReadiness.value.nodes.length} 个节点已完成全部参与人配置校验。`
})

const uploadPreflightChecks = computed<UploadPreflightCheck[]>(() => {
  const hasApprovalChain = routeReadiness.value?.ready === true && Boolean(uploadApprovers.value)
    && !approvalUsersLoading.value && !approvalUsersError.value && !uploadApproverError.value
  const hasDirectoryLanding = isNormalNewUpload.value
    ? Boolean(selectedProjectFolder.value && !projectFoldersLoading.value && !projectFoldersError.value
      && String(selectedProjectFolder.value.projectCodeId) === String(formData.dccProjectCodeId))
    : Boolean(selectedUploadDirectoryPath.value)
  const versionReady = Boolean(
    formData.fileNumber.trim() &&
      isVersionNoFormatValid.value
  )
  const versionBlockingReason = versionFormatPreflightMessage.value ||
    currentVersionLookupError.value ||
    existingUploadIdentityBlockReason.value ||
    (isRequestedVersionDuplicate.value ? versionDuplicatePreflightMessage.value : '') ||
    currentVersionProjectionBlockReason.value ||
    (currentVersionInfo.value?.modifying ? '同编号文件已有未完成流程，当前不可重复提交。' : '')
  const versionDescription = currentVersionLookupLoading.value
    ? '正在校验文件编号和版本，请等待结果后再提交。'
    : versionBlockingReason
      ? versionBlockingReason
      : currentVersionInfo.value?.matched
        ? `受控版本 ${currentVersionInfo.value.currentVersionNo || '-'}，本次提交版本 ${formData.versionNo || '-'}。`
        : versionReady
          ? '未发现同编号受控版本，将按新建编号继续校验。'
          : '请输入文件编号和版本号后检查是否重复。'

  return [
    {
      key: 'file-version',
      label: '文件编号/版本',
      status: currentVersionLookupLoading.value
        ? '检查中'
        : versionBlockingReason
          ? '需处理'
        : versionReady
          ? '可提交'
          : '待填写',
      description: versionDescription,
      ok: versionReady && isVersionNoFormatValid.value && !currentVersionLookupLoading.value && !versionBlockingReason,
      warning: !versionReady || currentVersionLookupLoading.value
    },
    {
      key: 'effective-date',
      label: '生效方式及日期',
      status: effectiveSelectionValidation.value.valid ? '已填写' : '待核对',
      description: effectiveDatePreflightText.value,
      ok: effectiveSelectionValidation.value.valid,
      warning: !effectiveSelectionValidation.value.valid
    },
    {
      key: 'category-selection',
      label: '文件类别',
      status: selectedCategory.value ? '已匹配' : (isExternalReview.value ? '待选择' : '待匹配'),
      description: selectedCategory.value
        ? `当前文件类别：${selectedCategory.value.name}。`
        : isExternalReview.value ? '请选择文件类别。' : '选择文件分类后将自动匹配正式文件类别。',
      ok: Boolean(selectedCategory.value),
      warning: !selectedCategory.value
    },
    {
      key: 'approval-chain',
      label: '审批人链路',
      status: routeReadinessLoading.value
        ? '检查中'
        : selectedCategory.value
          ? (hasApprovalChain ? '已就绪' : '不完整')
          : (isExternalReview.value ? '待选择' : '待匹配'),
      description: approvalChainPreflightText.value,
      ok: hasApprovalChain,
      warning: !selectedCategory.value || routeReadinessLoading.value
    },
    {
      key: 'controlled-browser-directory',
      label: isNormalNewUpload.value ? '项目存储文件夹' : '受控浏览目录',
      status: isNormalNewUpload.value ? (hasDirectoryLanding ? '已选择' : '待选择') : (hasDirectoryLanding ? '可落位' : '待落位'),
      description: isNormalNewUpload.value
        ? `项目文件夹：${selectedProjectFolder.value?.name || '未选择'}。实际存储位置由系统提交时按正式配置确定。`
        : hasDirectoryLanding
        ? `最终受控浏览目录：${selectedUploadDirectoryPath.value}`
        : '请选择最终提交目录，确保发布后可以落位到受控浏览目录。',
      ok: hasDirectoryLanding,
      warning: !formData.categoryId
    },
    {
      key: 'controlled-browser-permission-scope',
      label: '浏览权限范围',
      status: selectedCategory.value && hasDirectoryLanding ? '按正式权限校验' : '待确认',
      description: controlledBrowserPermissionScopeText.value,
      ok: Boolean(selectedCategory.value && hasDirectoryLanding),
      warning: !selectedCategory.value || !hasDirectoryLanding
    }
  ]
})

const loadApprovalUsers = async () => {
  const token = ++approvalUsersRequestSeq
  const actorKey = uploadApplicationActorKey(userStore.getUser, getVisitTenantId() ?? getTenantId())
  approvalUsersLoading.value = true
  approvalUsersError.value = ''
  approvalUsers.value = []
  try {
    const accounts = approvalUserOptions(await getSimpleUserList())
    if (token !== approvalUsersRequestSeq) return
    if (actorKey !== uploadApplicationActorKey(userStore.getUser, getVisitTenantId() ?? getTenantId()))
      throw new Error('登录账号或租户已变化，请重新读取启用账号目录')
    approvalUsers.value = accounts
  } catch (cause) {
    if (token === approvalUsersRequestSeq)
      approvalUsersError.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (token === approvalUsersRequestSeq) approvalUsersLoading.value = false }
}

const loadSignoffDepartments = async () => {
  const token = ++signoffDirectoryRequestSeq
  signoffDepartmentsLoading.value = true
  signoffDepartmentsError.value = ''
  signoffDepartments.value = []
  try {
    const rows = signoffDepartmentOptions(await getSimpleDeptList())
    if (token === signoffDirectoryRequestSeq) signoffDepartments.value = rows
  } catch (cause) {
    if (token === signoffDirectoryRequestSeq)
      signoffDepartmentsError.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (token === signoffDirectoryRequestSeq) signoffDepartmentsLoading.value = false }
}

const ensureUploadApprovalMatrix = () => {
  const tenantId = referenceIdentity(getVisitTenantId() ?? getTenantId())
  const projectId = referenceIdentity(formData.dccProjectCodeId)
  const categoryId = signoffIdentity(formData.categoryId)
  const taxonomyId = uploadFileTypeIdentity(formData.fileTypeTaxonomyId)
  const key = JSON.stringify([tenantId, projectId, categoryId, taxonomyId, 'NEW',
    selectedFileTypeTaxonomyPath.value?.approvalSourceMode,
    uploadApplicationActorKey(userStore.getUser, getVisitTenantId() ?? getTenantId())])
  if (!approvalMatrixState.value || key !== approvalMatrixScopeKey) {
    approvalMatrixScopeKey = key
    approvalMatrixState.value = new UploadApprovalMatrixState({ tenantId, projectId, categoryId, taxonomyId,
      approvalSourceMode: selectedFileTypeTaxonomyPath.value?.approvalSourceMode ?? undefined,
      requesterId: referenceIdentity(userStore.getUser.id),
      actionType: 'NEW', applicationContextKey: createControlledFileUploadSessionId() })
    formData.approvalMatrixSelection = undefined
  }
  if (isCustomApprovalSource.value) approvalMatrixState.value.setApplicationCustomMatrix(formData.applicationCustomMatrix ?? null)
  return approvalMatrixState.value
}

const confirmUploadMatrixChange = async (phase: 'SUBMIT' | 'SAVE' = 'SUBMIT') => {
  const state = ensureUploadApprovalMatrix()
  const accepted = await state.confirmChange(async rows => {
    try {
      await ElMessageBox.confirm(h('div', { style: 'max-height:65vh;overflow:auto;white-space:pre-wrap' }, rows.map(row =>
        h('p', [h('strong', `${row.label}：`), h('span', row.value)]))), '公共审批配置已变化，请确认本次使用的配置',
        { confirmButtonText: '确认当前配置', cancelButtonText: '取消', type: 'warning' })
      return true
    } catch (cause) { if (cause === 'cancel' || cause === 'close') return false; throw cause }
  })
  if (!accepted || state !== approvalMatrixState.value) return false
  formData.approvalMatrixSelection = state.selection(phase)
  return true
}

const refreshRouteReadiness = async () => {
  const requestSeq = ++routeReadinessRequestSeq
  const actorKey = uploadApplicationActorKey(userStore.getUser, getVisitTenantId() ?? getTenantId())
  if (!formData.categoryId) {
    routeReadiness.value = undefined
    routeReadinessSelectionKey.value = ''
    signoffSelectionCategoryKey = ''
  formData.selectedSignoffDepartmentIds = undefined
  formData.selectedDistributionDepartmentIds = undefined
  formData.applicationCustomMatrix = null
    routeReadinessError.value = ''
    routeReadinessLoading.value = false
    return false
  }
  routeReadinessLoading.value = true
  routeReadinessError.value = ''
  routeReadiness.value = undefined
  routeReadinessSelectionKey.value = ''
  try {
    const categoryId = signoffIdentity(formData.categoryId)
    const categoryKey = signoffCategoryKey(categoryId, formData.processType)
    if (categoryKey !== signoffSelectionCategoryKey) {
      formData.selectedSignoffDepartmentIds = undefined
      formData.selectedDistributionDepartmentIds = undefined
      signoffSelectionCategoryKey = categoryKey
    }
    const selectedDepartments = isExternalReview.value ? undefined : isCustomApprovalSource.value ? [...(formData.selectedSignoffDepartmentIds ?? [])] : normalizeSignoffDepartments(formData.selectedSignoffDepartmentIds, true)
    const requestKey = isCustomApprovalSource.value ? JSON.stringify([categoryId, selectedDepartments, formData.applicationCustomMatrix]) : signoffRequestKey(categoryId, selectedDepartments, formData.processType)
    const matrix = isExternalReview.value ? undefined : ensureUploadApprovalMatrix()
    const matrixRequest = matrix?.begin(selectedDepartments)
    const readiness = await checkControlledFileRouteReadiness(matrixRequest?.request || { categoryId, actionType: 'NEW' })
    if (requestSeq !== routeReadinessRequestSeq) {
      return false
    }
    if (actorKey !== uploadApplicationActorKey(userStore.getUser, getVisitTenantId() ?? getTenantId()))
      throw new Error('登录账号或租户已变化，请重新校验审批路线')
    if (requestKey !== (isCustomApprovalSource.value ? JSON.stringify([signoffIdentity(formData.categoryId), [...(formData.selectedSignoffDepartmentIds ?? [])], formData.applicationCustomMatrix]) : signoffRequestKey(formData.categoryId,
      isExternalReview.value ? undefined : formData.selectedSignoffDepartmentIds, formData.processType)))
      throw new Error('会签部门或文件类别已变化，请重新校验')
    if (!isExternalReview.value) {
      if (!matrix || !matrixRequest || matrix !== ensureUploadApprovalMatrix() || !matrix.accept(matrixRequest, readiness)) return false
      if (isCustomApprovalSource.value) {
        routeReadinessSelectionKey.value = signoffRequestKey(formData.categoryId, selectedDepartments?.length ? selectedDepartments : undefined, formData.processType)
        routeReadiness.value = readiness
        formData.approvalMatrixSelection = matrix.selection('SAVE')
        return readiness.ready
      }
      if (!readiness.ready && !readiness.nodes.length) {
        routeReadinessSelectionKey.value = requestKey
        routeReadiness.value = readiness
        return false
      }
      const resolvedDepartments = defaultSignoffDepartments(readiness)
      if (formData.selectedDistributionDepartmentIds === undefined) {
        const defaultMatrix = selectedDepartments === undefined ? matrix : matrix.createDefaultPreview()
        const defaultRequest = selectedDepartments === undefined ? undefined : defaultMatrix.begin()
        const initialResponse = defaultRequest ? await checkControlledFileRouteReadiness(defaultRequest.request) : readiness
        if (requestSeq !== routeReadinessRequestSeq) return false
        if (actorKey !== uploadApplicationActorKey(userStore.getUser, getVisitTenantId() ?? getTenantId()))
          throw new Error('登录账号或租户已变化，请重新校验审批路线')
        if (JSON.stringify(readiness.nodes.filter(node => node.stageCode === 'SUPERVISOR_REVIEW'))
          !== JSON.stringify(initialResponse.nodes.filter(node => node.stageCode === 'SUPERVISOR_REVIEW')))
          throw new Error('直属主管正式预检来源已变化，请重新校验审批路线')
        if (requestKey !== signoffRequestKey(formData.categoryId, formData.selectedSignoffDepartmentIds, formData.processType))
          throw new Error('会签部门或文件类别已变化，请重新校验')
        if (JSON.stringify([initialResponse.matrixBefore, initialResponse.matrixAfter, initialResponse.preparedProcessDefinitionId,
          initialResponse.preparedDefinitionDigest, initialResponse.ready])
          !== JSON.stringify([readiness.matrixBefore, readiness.matrixAfter, readiness.preparedProcessDefinitionId,
            readiness.preparedDefinitionDigest, readiness.ready]))
          throw new Error(matrixRuntimeBlockerMessage(initialResponse) || '审批矩阵或流程准备来源已变化，请重新校验')
        if (matrix !== ensureUploadApprovalMatrix() || (defaultRequest && !defaultMatrix.accept(defaultRequest, initialResponse))) return false
        const initial = initialUploadDepartmentSelection(initialResponse)
        if (formData.selectedDistributionDepartmentIds === undefined)
          formData.selectedDistributionDepartmentIds = initial.selectedDistributionDepartmentIds
      }
      if (selectedDepartments === undefined) formData.selectedSignoffDepartmentIds = resolvedDepartments
      else if (JSON.stringify([...selectedDepartments].sort()) !== JSON.stringify([...resolvedDepartments].sort()))
        throw new Error('审批预检返回的会签部门与本次选择不一致')
    }
    routeReadinessSelectionKey.value = signoffRequestKey(formData.categoryId,
      isExternalReview.value ? undefined : formData.selectedSignoffDepartmentIds, formData.processType)
    routeReadiness.value = readiness
    return readiness.ready
  } catch (error) {
    if (requestSeq === routeReadinessRequestSeq) {
      routeReadiness.value = undefined
      routeReadinessError.value = resolveUploadErrorMessage(
        error,
        '审批路线校验失败，请处理配置或网络错误后重试'
      )
    }
    return false
  } finally {
    if (requestSeq === routeReadinessRequestSeq) {
      routeReadinessLoading.value = false
    }
  }
}

const loadCurrentVersionByFileNumber = async (options: {
  suppressWorkingDraftRetryConflictMessage?: boolean
} = {}) => {
  const fileNumber = formData.fileNumber.trim()
  const requestSeq = ++currentVersionLookupSeq
  if (!fileNumber) {
    clearCurrentVersionInfo()
    return
  }
  currentVersionLookupLoading.value = true
  currentVersionLookupError.value = ''
  try {
    const info = await getControlledFileCurrentVersion(
      fileNumber,
      formData.dccProjectCodeId,
      formData.fileTypeTaxonomyId
    )
    if (requestSeq !== currentVersionLookupSeq) {
      return
    }
    currentVersionInfo.value = info
    currentVersionLookupError.value = ''
    if (info.matched) {
      if (!isExternalReview.value) {
        formData.changeType = 'NEW'
        formData.revisionTargetControlledFileId = null
        formData.revisionSourceControlledFileId = null
      }
      if (isExternalReview.value && !formData.fileName && info.fileName) {
        formData.fileName = info.fileName
      }
      applyDccProjectCodeProductNumber()
    }
  } catch (error) {
    if (requestSeq === currentVersionLookupSeq) {
      const errorMessage = resolveUploadErrorMessage(error, '受控版本信息查询失败，请查看错误提示后重试')
      currentVersionInfo.value = undefined
      const recoverableRetryConflict =
        options.suppressWorkingDraftRetryConflictMessage === true &&
        canRetryWorkingDraftCreationAfterCurrentVersionConflictMessage(errorMessage)
      currentVersionLookupError.value = recoverableRetryConflict ? '' : errorMessage
      if (!recoverableRetryConflict) {
        message.error(errorMessage)
      }
    }
  } finally {
    if (requestSeq === currentVersionLookupSeq) {
      currentVersionLookupLoading.value = false
    }
  }
}

const handleCategoryChange = async () => {
  if (!(await cleanupCurrentUploadSession())) {
    return
  }
  clearSubmitFieldErrors(submitFieldErrors)
  resetUploadDirectoryContext()
  resetSelectedPreview()
  resetDrawingPdfUpload()
  resetAttachmentUploads()
  if (formData.categoryId) {
    applyDccProjectCodeProductNumber()
    if (!isNormalNewUpload.value) await loadUploadDirectoryTree(formData.categoryId)
  }
}

const handleFileNameInput = () => { resetUploadNameLinkage(false) }
const handleFileNameClear = () => {
  formData.fileName = ''
  resetUploadNameLinkage(true)
  clearSubmitFieldErrors(submitFieldErrors)
}

const refreshRejectedUploadMatrix = async (cause: unknown) => {
  formData.approvalMatrixSelection = undefined
  const state = approvalMatrixState.value
  return await refreshApprovalMatrixRejection(state, cause, async () => {
    await refreshRouteReadiness()
    if (state !== approvalMatrixState.value) throw new Error('申请范围已变化，请重新读取当前项目和类型')
    if (routeReadinessError.value) throw new Error(routeReadinessError.value)
    if (!routeReadiness.value) throw new Error('正式审批预检未返回结果')
    return matrixRuntimeBlockerMessage(routeReadiness.value)
  })
}

const handleFileExceed: UploadProps['onExceed'] = () => {
  message.error('只允许上传一个文件')
}

const captureUploadSelectionContext = () => JSON.stringify({
  project: formData.dccProjectCodeId, type: formData.fileTypeTaxonomyId,
  category: formData.categoryId, process: formData.processType
})

const handleFileChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  if (submitLoading.value || uploadSubmitted.value || projectAttributesLoading.value) return
  const requestSeq = ++sourceUploadRequestSeq
  const selectionContext = captureUploadSelectionContext()
  const selectedUid = file.uid
  const validation = validateSingleUploadFileSelection(uploadFiles.map((item) => ({
    name: item.name,
    type: item.raw?.type
  })))
  const isPdf = file.raw && (file.raw.type === 'application/pdf' || /\.pdf$/i.test(file.name))

  if (!validation.valid || (!isExternalReview.value && !isPdf)) {
    const errorMessage = validation.message || '文件校验失败'
    uploadRef.value?.clearFiles()
    resetSelectedPreview()
    uploadPreviewError.value = errorMessage
    message.error(errorMessage)
    return
  }
  if (!file.raw) {
    return
  }
  if (!(await cleanupCurrentUploadSession())) {
    if (requestSeq === sourceUploadRequestSeq) uploadRef.value?.clearFiles()
    return
  }
  if (requestSeq !== sourceUploadRequestSeq || selectionContext !== captureUploadSelectionContext()) return
  uploadSessionId = createControlledFileUploadSessionId()
  const clientSessionId = uploadSessionId
  resetEditableUpload()
  resetDrawingPdfUpload()
  resetAttachmentUploads()

  clearSubmitFieldErrors(submitFieldErrors)
  uploadPreviewError.value = ''
  fileList.value = uploadFiles.slice(-1)
  previewUpload.value = undefined
  previewFileBlob.value = file.raw as File
  if (!isExternalReview.value) formData.fileName = file.raw.name

  uploadPreviewLoading.value = true
  try {
    const uploaded = await uploadSubmitterService.uploadPreview(
      file.raw as File,
      isExternalReview.value ? 'SOURCE' : 'READ_ONLY_VIEW',
      buildUploadPreviewContext()
    )
    const isCurrentSelection =
      requestSeq === sourceUploadRequestSeq &&
      clientSessionId === uploadSessionId && selectionContext === captureUploadSelectionContext() &&
      fileList.value.some((selectedFile) => selectedFile.uid === selectedUid)
    if (!isCurrentSelection) {
      await cleanupStaleUploadResponse(uploaded, '受控文件')
      return
    }
    try { validateUploadPreviewResponse(uploaded, file.raw.name, true) }
    catch (invalidResponse) {
      await cleanupStaleUploadResponse(uploaded, '受控文件')
      throw invalidResponse
    }
    uploadSessionBinding.value = {
      clientSessionId,
      scopedSessionId: uploaded.sessionId
    }
    previewUpload.value = uploaded
  } catch (error) {
    if (requestSeq === sourceUploadRequestSeq) {
      previewUpload.value = undefined
      previewFileBlob.value = null
      uploadPreviewError.value = resolveUploadPreviewErrorMessage(error, '文件预览上传失败，请查看错误提示后重试')
      message.error(uploadPreviewError.value)
    }
  } finally {
    if (requestSeq === sourceUploadRequestSeq) {
      uploadPreviewLoading.value = false
    }
  }
}

const handleEditableFileChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  if (submitLoading.value || uploadSubmitted.value || projectAttributesLoading.value) return
  const requestSeq = ++editableUploadRequestSeq
  const session = uploadSessionId
  const context = captureUploadSelectionContext()
  const current = () => requestSeq === editableUploadRequestSeq && session === uploadSessionId && context === captureUploadSelectionContext()
  const validation = validateControlledFileSelection(uploadFiles.map((item) => ({
    name: item.name,
    type: item.raw?.type
  })))
  if (!validation.valid || !file.raw) {
    const errorMessage = validation.message || '可编辑文件校验失败'
    editableUploadRef.value?.clearFiles()
    resetEditableUpload()
    message.error(errorMessage)
    return
  }
  if (!(await cleanupTemporaryUploadTicket(editableUpload.value))) {
    editableUploadRef.value?.clearFiles()
    return
  }
  if (!current()) return
  editableFileList.value = uploadFiles.slice(-1)
  editableUpload.value = undefined
  editableUploadLoading.value = true
  try {
    const uploaded = await uploadSubmitterService.uploadPreview(
      file.raw as File,
      'EDITABLE_SOURCE',
      buildUploadPreviewContext()
    )
    if (!current()) { await cleanupStaleUploadResponse(uploaded, '可编辑文件'); return }
    validateUploadPreviewResponse(uploaded, file.raw.name, false)
    editableUpload.value = uploaded
  } catch (error) {
    if (!current()) return
    editableUpload.value = undefined
    message.error(resolveUploadPreviewErrorMessage(error, '可编辑文件上传失败，请查看错误提示后重试'))
  } finally {
    if (current()) editableUploadLoading.value = false
  }
}

const handleBeforeEditableFileRemove: UploadProps['beforeRemove'] = async () =>
  await cleanupTemporaryUploadTicket(editableUpload.value, true)

const handleEditableFileRemove: UploadProps['onRemove'] = () => {
  resetEditableUpload()
}

const handleBeforeFileRemove: UploadProps['beforeRemove'] = async () => {
  if (submitLoading.value || uploadSubmitted.value) return false
  const cleaned = await cleanupCurrentUploadSession(true)
  if (cleaned) {
    sourceUploadRequestSeq++
  }
  return cleaned
}

const handleFileRemove: UploadProps['onRemove'] = () => {
  resetSelectedPreview()
  resetDrawingPdfUpload()
  resetAttachmentUploads()
  clearSubmitFieldErrors(submitFieldErrors)
}

const handleDrawingPdfExceed: UploadProps['onExceed'] = () => {
  message.error('只允许上传一个 PDF 文件')
}

const handleDrawingPdfChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  if (submitLoading.value || uploadSubmitted.value || projectAttributesLoading.value || uploadPreviewLoading.value || !previewUpload.value) return
  const requestSeq = ++drawingPdfUploadRequestSeq
  const selectionContext = captureUploadSelectionContext()
  const clientSessionId = uploadSessionId
  const selectedUid = file.uid
  const validation = validateSingleUploadFileSelection(
    uploadFiles.map((item) => ({
      name: item.name,
      type: item.raw?.type
    }))
  )

  if (!validation.valid) {
    const errorMessage = validation.message || '图纸 PDF 校验失败'
    drawingPdfUploadRef.value?.clearFiles()
    resetDrawingPdfUpload()
    uploadPreviewError.value = errorMessage
    message.error(errorMessage)
    return
  }
  if (!file.raw) {
    return
  }
  const isPdf = file.raw.type === 'application/pdf' || /\.pdf$/i.test(file.name)
  if (!isPdf) {
    const errorMessage = '文件格式不受支持：图纸文件必须上传 PDF 格式'
    drawingPdfUploadRef.value?.clearFiles()
    resetDrawingPdfUpload()
    uploadPreviewError.value = errorMessage
    message.error(errorMessage)
    return
  }
  if (!(await cleanupTemporaryUploadTicket(drawingPdfUpload.value))) {
    drawingPdfUploadRef.value?.clearFiles()
    return
  }
  if (requestSeq !== drawingPdfUploadRequestSeq || clientSessionId !== uploadSessionId
    || selectionContext !== captureUploadSelectionContext()) return

  uploadPreviewError.value = ''
  drawingPdfFileList.value = uploadFiles.slice(-1)
  drawingPdfUpload.value = undefined
  uploadDrawingPdfLoading.value = true
  try {
    const uploaded = await uploadSubmitterService.uploadPreview(
      file.raw as File,
      'DRAWING_PDF',
      buildUploadPreviewContext()
    )
    const isCurrentSelection =
      requestSeq === drawingPdfUploadRequestSeq &&
      clientSessionId === uploadSessionId && selectionContext === captureUploadSelectionContext() &&
      drawingPdfFileList.value.some((selectedFile) => selectedFile.uid === selectedUid)
    if (!isCurrentSelection) {
      await cleanupStaleUploadResponse(uploaded, '图纸 PDF')
      return
    }
    try { validateUploadPreviewResponse(uploaded, file.raw.name, false) }
    catch (invalidResponse) { await cleanupStaleUploadResponse(uploaded, '图纸 PDF'); throw invalidResponse }
    drawingPdfUpload.value = uploaded
  } catch (error) {
    if (requestSeq === drawingPdfUploadRequestSeq) {
      drawingPdfUpload.value = undefined
      uploadPreviewError.value = resolveUploadPreviewErrorMessage(error, '图纸 PDF 上传失败，请查看错误提示后重试')
      message.error(uploadPreviewError.value)
    }
  } finally {
    if (requestSeq === drawingPdfUploadRequestSeq) {
      uploadDrawingPdfLoading.value = false
    }
  }
}

const handleBeforeDrawingPdfRemove: UploadProps['beforeRemove'] = async () => {
  if (submitLoading.value || uploadSubmitted.value) return false
  const cleaned = await cleanupTemporaryUploadTicket(drawingPdfUpload.value, true)
  if (cleaned) {
    drawingPdfUploadRequestSeq++
  }
  return cleaned
}

const handleDrawingPdfRemove: UploadProps['onRemove'] = () => {
  resetDrawingPdfUpload()
}

const handleAttachmentFileChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  if (submitLoading.value || uploadSubmitted.value || projectAttributesLoading.value || uploadPreviewLoading.value || !previewUpload.value) return
  if (!file.raw) {
    return
  }
  const generation = nextAttachmentUploadGeneration(file.uid)
  attachmentUploadAttempts.value = [
    ...attachmentUploadAttempts.value.filter((attempt) => String(attempt.uid) !== String(file.uid)),
    { uid: file.uid, generation, fileName: file.name, status: 'UPLOADING' }
  ]
  clearSubmitFieldErrors(submitFieldErrors)
  uploadPreviewError.value = ''
  attachmentFileList.value = uploadFiles
  syncAttachmentUploadLoading()
  const sessionId = createControlledFileUploadSessionId()
  try {
    const uploaded = await uploadSubmitterService.uploadPreview(
      file.raw as File,
      attachmentUploadPurpose,
      buildUploadPreviewContext(sessionId)
    )
    if (!isCurrentAttachmentUpload(file.uid, generation) ||
        !attachmentFileList.value.some((item) => String(item.uid) === String(file.uid))) {
      await cleanupStaleUploadResponse(uploaded, '普通附件')
      return
    }
    try { validateUploadPreviewResponse(uploaded, file.raw.name, false) }
    catch (invalidResponse) { await cleanupStaleUploadResponse(uploaded, '普通附件'); throw invalidResponse }
    attachmentUploads.value = [
      ...attachmentUploads.value.filter((upload) => upload.uid !== file.uid),
      {
        ...uploaded,
        uid: file.uid
      }
    ]
    const attempt = findAttachmentUploadAttempt(file.uid)
    if (attempt) {
      attempt.status = 'READY'
    }
  } catch (error) {
    if (isCurrentAttachmentUpload(file.uid, generation)) {
      const attempt = findAttachmentUploadAttempt(file.uid)
      if (attempt) {
        attempt.status = 'FAILED'
      }
      uploadPreviewError.value = resolveUploadPreviewErrorMessage(error, '普通附件上传失败，请移除失败附件后重试')
      message.error(uploadPreviewError.value)
    }
  } finally {
    syncAttachmentUploadLoading()
  }
}

const handleBeforeAttachmentRemove: UploadProps['beforeRemove'] = async (file) => {
  if (submitLoading.value || uploadSubmitted.value) return false
  const attempt = findAttachmentUploadAttempt(file.uid)
  if (attempt?.status === 'UPLOADING') {
    nextAttachmentUploadGeneration(file.uid)
    return true
  }
  const upload = attachmentUploads.value.find((item) => item.uid === file.uid)
  const cleaned = await cleanupTemporaryUploadTicket(upload, true)
  if (cleaned && attempt && isCurrentAttachmentUpload(file.uid, attempt.generation)) {
    nextAttachmentUploadGeneration(file.uid)
  }
  return cleaned
}

const handleAttachmentRemove: UploadProps['onRemove'] = (file, uploadFiles) => {
  nextAttachmentUploadGeneration(file.uid)
  attachmentUploads.value = attachmentUploads.value.filter((upload) => upload.uid !== file.uid)
  attachmentUploadAttempts.value = attachmentUploadAttempts.value.filter(
    (attempt) => String(attempt.uid) !== String(file.uid)
  )
  attachmentFileList.value = uploadFiles
  syncAttachmentUploadLoading()
  clearSubmitFieldErrors(submitFieldErrors)
}

const freezeUploadApplicationDraft = (savingDraft = false): UploadFormDraft => {
  const draft: UploadFormDraft = JSON.parse(JSON.stringify(formData))
  if (!isExternalReview.value && !savingDraft) Object.assign(draft, buildUploadEffectiveChoice(draft.effectiveMode, draft.effectiveDate, resolveTodayDate()))
  if (!isExternalReview.value) {
    const selectedType = selectedFileTypeTaxonomyPath.value
    if (selectedType?.uploadBlocker) throw new Error(selectedType.uploadBlocker)
    if (projectFoldersLoading.value || projectFoldersError.value || !selectedProjectFolder.value
      || String(selectedProjectFolder.value.projectCodeId) !== String(draft.dccProjectCodeId)) {
      throw new Error(projectFoldersError.value || '请选择当前项目的有效文件夹')
    }
    if ((draft.projectFolderChangeReason?.trim().length ?? 0) > 500) {
      throw new Error('文件夹登记说明不能超过500字')
    }
    if (projectAttributesLoading.value || !projectAttributesPanel.value) {
      throw new Error('请等待当前项目属性读取完成')
    }
    const snapshot = projectAttributesPanel.value.getSnapshot()
    if (snapshot.applicationType !== 'UPLOAD' || snapshot.projectId !== String(draft.dccProjectCodeId)) {
      throw new Error('本次申请属性与所选项目不一致，请重新选择项目')
    }
    draft.projectAttributes = JSON.parse(JSON.stringify(snapshot.actual))
    if (signoffDepartmentsLoading.value || signoffDepartmentsError.value)
      throw new Error(signoffDepartmentsError.value || '请等待启用部门目录读取完成')
    const selected = savingDraft && isCustomApprovalSource.value ? [...(draft.selectedSignoffDepartmentIds ?? [])] : normalizeSignoffDepartments(draft.selectedSignoffDepartmentIds, savingDraft)
    if (selected?.some(id => !signoffDepartments.value.some(department => department.id === id)))
      throw new Error('本次会签部门不在启用部门目录中，请重新选择')
    if (!savingDraft && routeReadinessSelectionKey.value !== signoffRequestKey(draft.categoryId, selected, draft.processType))
      throw new Error('本次会签部门尚未完成预检，请重新校验')
    draft.selectedSignoffDepartmentIds = selected
    const distribution = savingDraft && draft.selectedDistributionDepartmentIds === undefined ? undefined : normalizeDistributionDepartments(draft.selectedDistributionDepartmentIds)
    if (distribution?.some(id => !signoffDepartments.value.some(department => department.id === id)))
      throw new Error('本次下发部门不在启用部门目录中，请重新选择')
    draft.selectedDistributionDepartmentIds = distribution
  }
    if (savingDraft) return draft
  if (approvalUsersLoading.value || approvalUsersError.value || !routeReadiness.value?.ready)
    throw new Error(approvalUsersError.value || '请等待启用账号及审批路线校验完成')
  resolvedUploadApprovers(routeReadiness.value, approvalUsers.value)
  if (!isExternalReview.value) resolvedUploadApplicationPeople(routeReadiness.value, userStore.getUser, approvalUsers.value)
  return draft
}

const captureUploadSubmissionContext = () => JSON.stringify({
  actor: uploadApplicationActorKey(userStore.getUser, getVisitTenantId() ?? getTenantId()),
  form: Object.fromEntries(Object.entries(formData).filter(([key]) => key !== 'approvalMatrixSelection')), preview: previewUpload.value, editable: editableUpload.value, drawingPdf: drawingPdfUpload.value,
  attachments: resolveReadyAttachmentUploads(), relations: selectedUploadRelations.value,
  attributes: isExternalReview.value ? undefined : projectAttributesPanel.value?.getSnapshot(),
  project: selectedProjectCode.value, product: projectProduct.value, folder: selectedProjectFolder.value,
  fileType: selectedFileTypeTaxonomyLeafName.value, category: selectedCategory.value,
  users: approvalUsers.value, departments: signoffDepartments.value
})

const confirmUploadApplication = (rows: UploadConfirmationRow[]) => ElMessageBox.confirm(
  h('div', { 'data-testid': 'dcc-upload-submit-confirmation', style: 'max-height:65vh;overflow:auto' }, [
    ...rows.map(row => h('div', { style: 'margin-bottom:8px;overflow-wrap:anywhere' }, [
      h('strong', `${row.label}：`), h('span', row.value)
    ])),
    h('p', '生效与受控分别记录；日常可用及自动下发须同时满足受控且已生效。'),
    h('p', '再次确认后才提交审批；取消保留已填写内容。')
  ]), '确认提交', {
    confirmButtonText: '确认提交', cancelButtonText: '取消', type: 'warning',
    modalClass: 'app-confirm-message-box-overlay', customClass: 'dcc-upload-submit-confirmation'
  }
)

const saveUploadDraft = async () => {
  if (isExternalReview.value || submitLoading.value || uploadSubmitted.value || projectAttributesLoading.value
    || uploadPreviewLoading.value || editableUploadLoading.value || uploadDrawingPdfLoading.value || hasUnreadyAttachmentUploads.value) return
  submitLoading.value = true
  try {
    const clickedContext = captureUploadSubmissionContext()
    if (!previewUpload.value || !isVersionNoFormatValid.value || !formData.fileNumber.trim()
      || !formData.categoryId || !formData.fileTypeTaxonomyId) throw new Error('请完成正文上传、编号、初始版本和文件类型后保存草稿')
    if (projectProductLoading.value || projectProductError.value || projectProductResolvedId.value !== formData.dccProjectCodeId)
      throw new Error(projectProductError.value || '当前项目产品身份尚未读取完成')
    const product = validateDccProjectProductCode(formData.productCode, isProductRequiredForSelectedCategory.value)
    if (!product.valid) throw new Error(product.message)
    const drawing = validateDrawingPdfUpload(previewUpload.value, drawingPdfUpload.value)
    if (!drawing.valid) throw new Error(drawing.message)
    const attachments = resolveReadyAttachmentUploads()
    if (!attachments) throw new Error('普通附件尚未全部上传完成')
    await refreshRouteReadiness()
    if (captureUploadSubmissionContext() !== clickedContext) throw new Error('项目、文件或本次配置已变化，请核对后重新保存')
    if (routeReadinessError.value || !approvalMatrixState.value || (!isCustomApprovalSource.value && !approvalMatrixState.value.after))
      throw new Error(routeReadinessError.value || '正式审批矩阵尚未配置，不能保存申请工作稿')
    if (!(await confirmUploadMatrixChange('SAVE'))) return
    if (captureUploadSubmissionContext() !== clickedContext) throw new Error('项目、文件或本次配置已变化，请核对后重新保存')
    const draft = freezeUploadApplicationDraft(true)
    const id = await uploadSubmitterService.saveDraft(draft,
      JSON.parse(JSON.stringify(previewUpload.value)), drawingPdfUpload.value ? JSON.parse(JSON.stringify(drawingPdfUpload.value)) : undefined,
      JSON.parse(JSON.stringify(attachments)), editableUpload.value ? JSON.parse(JSON.stringify(editableUpload.value)) : undefined)
    uploadSubmitted.value = true
    const exactId = referenceIdentity(id)
    message.success('申请工作稿已保存')
    try {
      await router.push(buildSubmittedApplicationRoute(exactId, { management: '1', from: 'browser', returnTo: router.resolve({ name: 'DccControlledFileBrowser' }).path }))
    } catch (navigationError) { message.error(`工作稿已保存，但页面跳转失败：${resolveUploadErrorMessage(navigationError, '请从受控文件目录查看工作稿')}`) }
  } catch (cause) {
    if (isApprovalMatrixStaleError(cause)) {
      message.error(await refreshRejectedUploadMatrix(cause))
      return
    }
    message.error(resolveUploadErrorMessage(cause, '申请工作稿保存失败，请核对后重试'))
  }
  finally { submitLoading.value = false }
}

const submitForm = async () => {
  if (submitLoading.value || uploadSubmitted.value || projectAttributesLoading.value || uploadPreviewLoading.value || editableUploadLoading.value || uploadDrawingPdfLoading.value) {
    message.warning('请等待当前项目属性读取或提交完成')
    return
  }
  let clickedContext: string
  try { clickedContext = captureUploadSubmissionContext() }
  catch (cause) { message.error(cause instanceof Error ? cause.message : String(cause)); return }
  if (!isExternalReview.value && (projectProductLoading.value || projectProductError.value || projectProductResolvedId.value !== formData.dccProjectCodeId)) {
    message.warning(projectProductError.value || '请等待当前项目的产品编号解析完成')
    return
  }
  clearSubmitFieldErrors(submitFieldErrors)
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!isVersionNoFormatValid.value) {
    submitFieldErrors.versionNo = versionFormatPreflightMessage.value
    message.warning(versionFormatPreflightMessage.value)
    return
  }
  const initialAttachmentSnapshot = resolveReadyAttachmentUploads()
  if (hasUnreadyAttachmentUploads.value || !initialAttachmentSnapshot) {
    message.warning(attachmentUploadBlockMessage.value || '请等待全部普通附件上传完成后再提交')
    return
  }
  if (currentVersionLookupTimer) {
    clearTimeout(currentVersionLookupTimer)
    currentVersionLookupTimer = undefined
  }
  await loadCurrentVersionByFileNumber({ suppressWorkingDraftRetryConflictMessage: true })
  if (currentVersionLookupError.value) {
    if (!canRetryWorkingDraftCreationAfterCurrentVersionConflictMessage()) {
      submitFieldErrors.versionNo = currentVersionLookupError.value
      message.warning(currentVersionLookupError.value)
      return
    }
  }
  if (existingUploadIdentityBlockReason.value) {
    submitFieldErrors.versionNo = existingUploadIdentityBlockReason.value
    message.warning(existingUploadIdentityBlockReason.value)
    return
  }
  if (currentVersionProjectionBlockReason.value) {
    message.warning(currentVersionProjectionBlockReason.value)
    return
  }
  if (currentVersionInfo.value?.modifying) {
    message.warning('同编号文件已有未完成流程，当前不可重复提交')
    return
  }
  if (isRequestedVersionDuplicate.value) {
    submitFieldErrors.versionNo = versionDuplicatePreflightMessage.value
    message.warning(versionDuplicatePreflightMessage.value)
    return
  }
  if (!isNormalNewUpload.value && !uploadDirectoryTree.value) {
    message.warning(isExternalReview.value ? '请先选择文件类别并完成目录加载' : categoryPreflightMessage.value || '请先选择文件分类并完成目录加载')
    return
  }
  if (!isNormalNewUpload.value && !formData.directoryId) {
    message.warning(
      uploadDirectoryTree.value?.leafBinding
        ? '当前绑定目录尚未就绪，请刷新后重试'
        : '请选择绑定目录下的最后一层子目录'
    )
    return
  }
  if (!previewUpload.value) {
    message.warning('请先选择并完成不可编辑 PDF 上传')
    return
  }
  const productCodeValidation = validateDccProjectProductCode(
    formData.productCode,
    isProductRequiredForSelectedCategory.value
  )
  if (!productCodeValidation.valid) {
    message.warning(productCodeValidation.message || '产品编号校验失败')
    return
  }
  const drawingPdfValidation = validateDrawingPdfUpload(previewUpload.value, drawingPdfUpload.value)
  if (!drawingPdfValidation.valid) {
    message.warning(drawingPdfValidation.message || '图纸 PDF 校验失败')
    return
  }
  await refreshRouteReadiness()
  if (!routeReadiness.value?.ready) {
    const blockerMessage = routeReadiness.value?.blockers?.[0]?.message || routeReadinessError.value
    message.warning(blockerMessage || '审批路线尚未就绪，请处理配置后重试')
    return
  }
  if (approvalUsersLoading.value || approvalUsersError.value || uploadApproverError.value || !uploadApprovers.value) {
    message.warning(approvalUsersError.value || uploadApproverError.value || '请等待真实批准人读取完成')
    return
  }
  const attachmentSnapshot = resolveReadyAttachmentUploads()
  if (hasUnreadyAttachmentUploads.value || !attachmentSnapshot) {
    message.warning(attachmentUploadBlockMessage.value || '普通附件状态已变化，请等待全部附件上传完成后再提交')
    return
  }
  if (submitLoading.value || uploadSubmitted.value) return
  submitLoading.value = true
  try {
    if (captureUploadSubmissionContext() !== clickedContext) throw new Error('项目、文件或申请信息已变化，请核对后重新提交')
    if (!isExternalReview.value) resolvedUploadApplicationPeople(routeReadiness.value, userStore.getUser, approvalUsers.value)
    if (!isExternalReview.value) {
      const routeBeforeConfirmation = JSON.stringify({ route: routeReadiness.value, routeKey: routeReadinessSelectionKey.value })
      const matrixAccepted = await confirmUploadMatrixChange()
      if (captureUploadSubmissionContext() !== clickedContext || routeBeforeConfirmation
        !== JSON.stringify({ route: routeReadiness.value, routeKey: routeReadinessSelectionKey.value }))
        throw new Error('登录账号或审批路线及申请信息已变化，请核对后重新提交')
      if (!matrixAccepted) return
    }
    const draft = freezeUploadApplicationDraft()
    const preview = JSON.parse(JSON.stringify(previewUpload.value))
    const drawingPdf = drawingPdfUpload.value ? JSON.parse(JSON.stringify(drawingPdfUpload.value)) : undefined
    const editable = editableUpload.value ? JSON.parse(JSON.stringify(editableUpload.value)) : undefined
    const attachments = JSON.parse(JSON.stringify(attachmentSnapshot))
    validateUploadTickets(preview, drawingPdf, attachments, editable)
    const approvers = resolvedUploadApprovers(routeReadiness.value, approvalUsers.value)
    const people = isExternalReview.value ? undefined : resolvedUploadApplicationPeople(routeReadiness.value, userStore.getUser, approvalUsers.value)
    const summary = buildUploadConfirmationSummary(draft, preview, {
      projectName: selectedProjectCode.value?.projectName,
      folderName: selectedProjectFolder.value?.name,
      fileTypeName: isExternalReview.value ? selectedCategory.value?.name : selectedFileTypeTaxonomyLeafName.value,
      relatedFiles: JSON.parse(JSON.stringify(selectedUploadRelations.value)),
      signoffDepartments: draft.selectedSignoffDepartmentIds?.map(id => signoffDepartments.value.find(department => department.id === String(id))!),
      distributionDepartments: draft.selectedDistributionDepartmentIds?.map(id => signoffDepartments.value.find(department => department.id === String(id))!),
      approvers: approvers.accounts, approvalRule: approvers.rule, approvalGroups: approvers.groups,
      compiler: people?.compiler, supervisor: people?.supervisor,
      documentControl: isCustomApprovalSource.value
        ? approvalUsers.value.find(account => account.id === draft.applicationCustomMatrix?.documentControlUserId) : undefined,
      runtimeCapabilityLimitations: approvers.capabilityLimitations,
      drawingPdf, attachments, editable
    })
    const currentContext = () => JSON.stringify({ context: captureUploadSubmissionContext(),
      route: routeReadiness.value, routeKey: routeReadinessSelectionKey.value })
    const confirmedContext = currentContext()
    try {
      await confirmUploadApplication(summary)
    } catch (error) {
      if (error === 'cancel' || error === 'close') return
      throw error
    }
    if (projectAttributesLoading.value || approvalUsersLoading.value || routeReadinessLoading.value
      || currentContext() !== confirmedContext) {
      throw new Error('项目、文件或申请信息已变化，请核对后重新提交')
    }
    if (!isExternalReview.value) buildUploadEffectiveChoice(draft.effectiveMode, draft.effectiveDate, resolveTodayDate())
    if (!isExternalReview.value && JSON.stringify(draft.approvalMatrixSelection)
      !== JSON.stringify(ensureUploadApprovalMatrix().selection('SUBMIT')))
      throw new Error('直属主管来源或审批准备已变化，请重新核对并确认')
    validateUploadTickets(preview, drawingPdf, attachments, editable)
    await uploadSubmitterService.submit(
      draft,
      preview,
      drawingPdf,
      attachments,
      editable
    )
    uploadSubmitted.value = true
    message.success(isExternalReview.value ? '外来文件评审已提交审批' : '受控文件已提交审批')
    try { await router.push({ name: 'DccControlledFileBrowser' }) }
    catch (navigationError) {
      message.error(`申请已提交成功，但页面跳转失败：${resolveUploadErrorMessage(navigationError, '请从受控文件目录查看提交结果')}`)
    }
  } catch (error) {
    const feedback = buildSubmitFailureFeedback(
      error,
      '受控文件提交失败，请查看错误提示后重试',
      formData.processType
    )
    if (!isExternalReview.value && isApprovalMatrixStaleError(error)) {
      message.error(await refreshRejectedUploadMatrix(error))
      return
    }
    applySubmitFailureFeedback(submitFieldErrors, feedback)
    message.error(feedback.message)
  } finally {
    submitLoading.value = false
  }
}

watch(
  () => formData.versionNo,
  () => {
    if (submitFieldErrors.versionNo) {
      clearSubmitFieldErrors(submitFieldErrors)
    }
  }
)

watch(
  () => formData.categoryId,
  () => {
    void refreshRouteReadiness()
  }
)

watch(
  () => formData.fileNumber,
  () => {
    clearSubmitFieldErrors(submitFieldErrors)
    if (currentVersionLookupTimer) {
      clearTimeout(currentVersionLookupTimer)
    }
    currentVersionLookupTimer = setTimeout(loadCurrentVersionByFileNumber, 300)
  }
)

watch(
  () => route.fullPath,
  () => {
    formData.processType = resolveProcessTypeByRoute()
    void applyDccProjectCodeProductNumber()
    void refreshRouteReadiness()
    void loadApprovalUsers()
    if (!isExternalReview.value) void loadSignoffDepartments()
  }
)

watch(
  () => uploadApplicationActorKey(userStore.getUser, getVisitTenantId() ?? getTenantId()),
  () => {
    approvalUsersRequestSeq++; routeReadinessRequestSeq++
    approvalUsers.value = []; routeReadiness.value = undefined; routeReadinessSelectionKey.value = ''
    approvalMatrixState.value = undefined; approvalMatrixScopeKey = ''; formData.approvalMatrixSelection = undefined
    void loadApprovalUsers(); void refreshRouteReadiness()
    if (!isExternalReview.value) void loadSignoffDepartments()
  }, { flush: 'sync' }
)

onMounted(() => {
  loadBaseData()
  if (!isExternalReview.value) void loadUploadFileTypes()
  void loadApprovalUsers()
  if (!isExternalReview.value) void loadSignoffDepartments()
})

onBeforeRouteLeave(async () => {
  if (submitLoading.value && !uploadSubmitted.value) return false
  if (!(await cleanupCurrentUploadSession())) return false
  if (!uploadSubmitted.value) {
    resetSelectedPreview()
    resetDrawingPdfUpload()
    resetAttachmentUploads()
    uploadRef.value?.clearFiles()
    drawingPdfUploadRef.value?.clearFiles()
    attachmentUploadRef.value?.clearFiles()
    uploadSessionId = createControlledFileUploadSessionId()
  }
  return true
})

onBeforeUnmount(() => {
  fileTypeOptionsRequestSequence++
  fileTypeCategoryRequestSequence++
  projectTemplateRequestSequence++
  projectProductMounted = false
  projectProductRequestSequence++
  routeReadinessRequestSeq++
  signoffDirectoryRequestSeq++
  approvalUsersRequestSeq++
  if (currentVersionLookupTimer) {
    clearTimeout(currentVersionLookupTimer)
  }
  previewFileBlob.value = null
})
</script>

<style scoped>
:global(.dcc-upload-submit-confirmation) {
  width: min(720px, calc(100vw - 32px));
  max-width: calc(100vw - 32px);
}

.upload-readiness-blockers {
  margin: 8px 0 0;
  padding-left: 20px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.upload-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.upload-form {
  min-width: 0;
}

.upload-workbench {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.upload-workbench__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  min-width: 0;
}

.upload-workbench__column {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.upload-section {
  min-width: 0;
  padding: 14px 16px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}


.upload-section__title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.upload-submit-bar {
  display: flex;
  justify-content: flex-end;
  padding: 10px 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f7f9fc;
}

.upload-preflight-legend {
  margin-bottom: 10px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.upload-preflight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.upload-preflight-card {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #f1b8b8;
  border-radius: 8px;
  background: #fffafa;
}

.upload-preflight-card.is-ok {
  border-color: #b7dfc7;
  background: #f7fcf8;
}

.upload-preflight-card.is-warning {
  border-color: #f0d49a;
  background: #fffaf0;
}

.upload-preflight-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

.upload-preflight-card__description {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
  word-break: break-word;
}

.upload-preview-panel {
  max-height: 420px;
  overflow: auto;
}

.upload-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

.upload-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.upload-form :deep(.el-form-item__label) {
  min-height: 32px;
  color: #263247;
  font-size: 13px;
  line-height: 32px;
  padding-right: 10px;
}

.upload-form :deep(.el-form-item__content) {
  min-width: 0;
}

.upload-form :deep(.el-select),
.upload-form :deep(.el-cascader),
.upload-form :deep(.el-autocomplete),
.upload-form :deep(.el-input),
.upload-form :deep(.el-textarea),
.upload-form :deep(.el-date-editor) {
  width: 100% !important;
  max-width: 100%;
}

.upload-form :deep(.el-input__wrapper),
.upload-form :deep(.el-select__wrapper),
.upload-form :deep(.el-textarea__inner) {
  min-height: 32px;
  border-radius: 6px;
}

.upload-submit-bar :deep(.el-form-item),
.upload-submit-bar :deep(.el-form-item__content) {
  margin: 0 !important;
}

@media (max-width: 1280px) {
  .upload-workbench__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 720px) {
  .upload-preflight-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
