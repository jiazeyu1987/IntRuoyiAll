<template>
  <el-dialog
    v-model="dialogVisible"
    :title="detail ? `产品详情 · ${detail.productCode}` : '产品详情'"
    class="showroom-product-detail-dialog"
    destroy-on-close
    width="1120px"
  >
    <div v-loading="loading" class="showroom-product-detail-dialog__body">
      <el-alert
        v-if="loadError"
        :closable="false"
        show-icon
        type="error"
        :title="loadError"
      />

      <template v-else-if="detail && form">
        <div class="showroom-product-detail-dialog__summary">
          <div class="showroom-product-detail-dialog__meta">
            <div>
              <span class="showroom-product-detail-dialog__meta-label">当前版本</span>
              <strong>V{{ detail.revisionNo }}</strong>
            </div>
            <div v-if="!approvalMode">
              <span class="showroom-product-detail-dialog__meta-label">版本中心</span>
              <el-button link type="primary" @click="handleOpenVersionCenter">进入版本中心</el-button>
            </div>
            <div>
              <span class="showroom-product-detail-dialog__meta-label">Revision ID</span>
              <strong>{{ detail.currentRevisionId }}</strong>
            </div>
            <div>
              <span class="showroom-product-detail-dialog__meta-label">旧产品编号</span>
              <strong>{{ detail.legacyProductCode || '未配置' }}</strong>
            </div>
            <div>
              <span class="showroom-product-detail-dialog__meta-label">资料状态</span>
              <el-tag :type="detail.incomplete ? 'warning' : 'success'">
                {{ detail.incomplete ? '资料未完善' : '资料完整' }}
              </el-tag>
            </div>
            <div>
              <span class="showroom-product-detail-dialog__meta-label">审批状态</span>
              <el-tag :type="resolveProductStatusTagType(detail.status)">
                {{ resolveProductStatusText(detail.status) }}
              </el-tag>
            </div>
            <div>
              <span class="showroom-product-detail-dialog__meta-label">编辑能力</span>
              <el-tag :type="readonly ? 'info' : 'success'">
                {{ readonly ? '只读查看' : '可编辑' }}
              </el-tag>
            </div>
          </div>
        </div>

        <div v-if="approvalMode" class="showroom-product-detail-dialog__approval-panel">
          <div class="showroom-product-detail-dialog__approval-header">
            <div>
              <div class="showroom-product-detail-dialog__section-title">待审批变更</div>
              <div v-if="approvalDetail" class="showroom-product-detail-dialog__approval-meta">
                变更单 #{{ approvalDetail.changeRequest.changeRequestId }} /
                {{ resolveApprovalStatusText(approvalDetail.changeRequest.status) }}
              </div>
            </div>
          </div>

          <el-alert
            v-if="approvalLoadError"
            :closable="false"
            show-icon
            type="warning"
            :title="approvalLoadError"
          />

          <template v-if="approvalDetail">
            <el-table :data="approvalDetail.fieldDiffs" row-key="fieldCode">
              <el-table-column label="字段" min-width="140" prop="fieldCode" />
              <el-table-column label="旧值" min-width="220" prop="oldValueJson" show-overflow-tooltip />
              <el-table-column label="新值" min-width="220" prop="newValueJson" show-overflow-tooltip />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="resolveApprovalStatusTagType(row.approvalStatus)">
                    {{ resolveApprovalStatusText(row.approvalStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <div class="showroom-product-detail-dialog__section-title mt-16px">签名留痕</div>
            <el-table :data="approvalDetail.signatureRecords" row-key="id" empty-text="暂无签名记录">
              <el-table-column label="阶段" width="120" prop="approvalStage" />
              <el-table-column label="动作" width="120" prop="actionType" />
              <el-table-column label="签名人" width="120" prop="actorId" />
              <el-table-column label="签名方式" width="120" prop="signatureMode" />
              <el-table-column label="签名意见" min-width="220" prop="comment" show-overflow-tooltip />
              <el-table-column label="签名时间" min-width="180" prop="signedAt" />
            </el-table>
          </template>
        </div>

        <el-form class="showroom-product-detail-dialog__form" label-position="top">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="当前产品编号">
                <el-input v-model="form.productCode" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="旧底表产品编号">
                <el-input
                  v-model="form.legacyProductCode"
                  :disabled="readonly"
                  placeholder="例如 product_012"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-tabs v-model="activeLanguageTab">
            <el-tab-pane label="中文" name="zh">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="注册证">
                    <el-input v-model="form.fields.registration_certificate" :disabled="readonly" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="FIM状态">
                    <el-input v-model="form.fields.fim_status" :disabled="readonly" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="临床效果">
                <el-input
                  v-model="form.fields.clinical_effect"
                  :disabled="readonly"
                  :rows="6"
                  type="textarea"
                />
              </el-form-item>
            </el-tab-pane>

            <el-tab-pane label="English" name="en">
              <div class="showroom-product-detail-dialog__toolbar">
                <div>
                  <h4>English Advanced Fields</h4>
                  <p>Translate the current Chinese advanced fields first, then continue editing them manually.</p>
                </div>
                <el-button
                  :disabled="readonly || !canTranslateEnglishFields"
                  :loading="translatingEnglishFields"
                  plain
                  type="primary"
                  @click="handleTranslateEnglishFields"
                >
                  AI Translate
                </el-button>
              </div>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="Registration Certificate">
                    <el-input
                      v-model="form.fields.registration_certificate_en"
                      :disabled="readonly"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="FIM Status">
                    <el-input v-model="form.fields.fim_status_en" :disabled="readonly" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="Clinical Effect">
                <el-input
                  v-model="form.fields.clinical_effect_en"
                  :disabled="readonly"
                  :rows="6"
                  type="textarea"
                />
              </el-form-item>
            </el-tab-pane>
          </el-tabs>
        </el-form>
      </template>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">
        {{ activeLanguageTab === 'en' ? 'Close' : '关闭' }}
      </el-button>
      <template v-if="approvalMode">
        <el-button
          type="danger"
          :disabled="!approvalDetail"
          :loading="approvalSubmitting"
          @click="openSignatureDialog('reject')"
        >
          {{ activeLanguageTab === 'en' ? 'Reject' : '驳回' }}
        </el-button>
        <el-button
          type="primary"
          :disabled="!approvalDetail"
          :loading="approvalSubmitting"
          @click="openSignatureDialog('approve')"
        >
          {{
            activeLanguageTab === 'en'
              ? approvalDetail?.changeRequest.status === 'PENDING_GAOXIN_APPROVAL'
                ? 'Approve and Publish'
                : 'Approve'
              : approvalButtonText
          }}
        </el-button>
      </template>
      <template v-else>
        <template v-if="!readonly && isShowroomPublicity">
          <el-button type="primary" :disabled="!detail || !form" :loading="saving" @click="handleSaveDraft">
            {{ activeLanguageTab === 'en' ? 'Save' : '保存' }}
          </el-button>
        </template>
        <template v-else-if="!readonly">
          <el-button :disabled="!detail || !form" :loading="saving" @click="handleSaveDraft">
            {{ activeLanguageTab === 'en' ? 'Save Draft' : '保存草稿' }}
          </el-button>
          <el-button
            type="primary"
            :disabled="!canPrimaryAction"
            :loading="submitting"
            @click="handleSubmit"
          >
            {{ activeLanguageTab === 'en' ? 'Submit for Approval' : '提交审批' }}
          </el-button>
        </template>
      </template>
    </template>

    <ShowroomApprovalSignatureDialog
      v-model="signatureDialogVisible"
      :loading="approvalSubmitting"
      :mode="signatureDialogMode"
      :title="approvalDialogTitle"
      @confirm="handleApprovalSignatureConfirm"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ShowroomAdminApi } from '@/api/showroom-admin'
import { useUserStore } from '@/store/modules/user'
import ShowroomApprovalSignatureDialog from '@/views/showroom-admin/approval/ShowroomApprovalSignatureDialog.vue'
import {
  normalizeApprovalDetail,
  resolveApprovalStatusTagType,
  resolveApprovalStatusText,
  type ShowroomApprovalDetailRecord
} from '@/views/showroom-admin/approval/contracts'
import {
  buildProductDraftPayload,
  createProductDraftForm,
  normalizeProductDetail,
  productAdvancedFieldDefinitions,
  resolveChangedFieldCodes,
  resolveProductEnglishFieldKey,
  type ShowroomApprovalRoutePreview,
  resolveProductStatusTagType,
  resolveProductStatusText,
  type ProductDraftForm,
  type ShowroomProductDetail
} from './contracts'

defineOptions({ name: 'ShowroomProductDetailDialog' })

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    productId?: number | null
    initialRevisionId?: number | null
    submitRoutePreview?: ShowroomApprovalRoutePreview | null
    approvalChangeRequestId?: number | null
  }>(),
  {
    productId: undefined,
    initialRevisionId: null,
    submitRoutePreview: null,
    approvalChangeRequestId: null
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [detail: ShowroomProductDetail]
  submitted: [payload: { productId: number; fieldCodes: string[] }]
  'approval-completed': []
  'open-version-center': [payload: { productId: number; revisionId: number }]
}>()

const message = useMessage()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.getUser.id)
const isShowroomPublicity = computed(() => userStore.getRoles.includes('showroom_publicity'))

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const approvalSubmitting = ref(false)
const loadError = ref('')
const approvalLoadError = ref('')
const activeLanguageTab = ref<'zh' | 'en'>('zh')
const translatingEnglishFields = ref(false)
const detail = ref<ShowroomProductDetail | null>(null)
const form = ref<ProductDraftForm | null>(null)
const approvalDetail = ref<ShowroomApprovalDetailRecord | null>(null)
const signatureDialogVisible = ref(false)
const signatureDialogMode = ref<'approve' | 'reject'>('approve')
const approvalComment = ref('')

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const approvalMode = computed(() => Number.isFinite(props.approvalChangeRequestId))

const readonly = computed(() => {
  return approvalMode.value || (detail.value ? !detail.value.editable : false)
})

const changedFieldCodes = computed(() => {
  if (!detail.value || !form.value) {
    return []
  }
  return resolveChangedFieldCodes(detail.value, form.value)
})

const canPrimaryAction = computed(() => {
  const status = detail.value?.status || ''
  return !readonly.value && (changedFieldCodes.value.length > 0 || status === 'DRAFT' || status === 'REJECTED')
})

const canTranslateEnglishFields = computed(() => {
  if (!form.value) {
    return false
  }
  return productAdvancedFieldDefinitions.some((definition) => form.value?.fields[definition.key]?.trim())
})

const approvalButtonText = computed(() => {
  const status = approvalDetail.value?.changeRequest.status || ''
  return status === 'PENDING_GAOXIN_APPROVAL' ? '企宣批准并发布' : '主管通过'
})

const approvalDialogTitle = computed(() => {
  const actionText = signatureDialogMode.value === 'reject' ? '驳回' : '签名'
  return `${approvalButtonText.value}${actionText}`
})

const assignForm = (productDetail: ShowroomProductDetail) => {
  detail.value = productDetail
  form.value = createProductDraftForm(productDetail)
}

const requireProductId = () => {
  if (!props.productId) {
    throw new Error('产品详情入口缺少 productId')
  }
  return props.productId
}

const loadProductDetail = async (productId: number, revisionId?: number | null) => {
  return normalizeProductDetail(await ShowroomAdminApi.getProduct(productId, revisionId))
}

const loadApprovalDetail = async () => {
  if (!props.approvalChangeRequestId) {
    approvalDetail.value = null
    approvalLoadError.value = ''
    return
  }
  try {
    approvalDetail.value = normalizeApprovalDetail(
      await ShowroomAdminApi.getApproval(props.approvalChangeRequestId)
    )
    approvalLoadError.value = ''
  } catch (error) {
    approvalDetail.value = null
    const resolved = error instanceof Error ? error : new Error(String(error))
    approvalLoadError.value = resolved.message
  }
}

const resolveInitialRevisionId = (productDetail: ShowroomProductDetail) => {
  if (approvalMode.value) {
    return productDetail.revisionId
  }
  return props.initialRevisionId || productDetail.currentRevisionId
}

const loadDialogState = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const productId = requireProductId()
    const baseDetail = await loadProductDetail(productId)
    const targetRevisionId = resolveInitialRevisionId(baseDetail)
    const productDetail =
      targetRevisionId === baseDetail.revisionId
        ? baseDetail
        : await loadProductDetail(productId, targetRevisionId)
    activeLanguageTab.value = 'zh'
    assignForm(productDetail)
    await loadApprovalDetail()
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    detail.value = null
    form.value = null
    approvalDetail.value = null
    approvalLoadError.value = ''
    loadError.value = resolved.message
  } finally {
    loading.value = false
  }
}

const handleTranslateEnglishFields = async () => {
  const state = requireFormState()
  translatingEnglishFields.value = true
  try {
    const translation = await ShowroomAdminApi.translateProductFieldsToEn({
      productId: state.detail.productId,
      nameCn: state.form.nameCn,
      fields: state.form.fields
    })
    if (translation.nameEn?.trim()) {
      state.form.nameEn = translation.nameEn.trim()
    }
    for (const definition of productAdvancedFieldDefinitions) {
      const englishFieldKey = resolveProductEnglishFieldKey(definition.key)
      if (translation.translatedFields?.[englishFieldKey] !== undefined) {
        state.form.fields[englishFieldKey] = String(translation.translatedFields[englishFieldKey] || '')
      }
    }
    activeLanguageTab.value = 'en'
    message.success('英文高级字段已翻译')
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
    throw resolved
  } finally {
    translatingEnglishFields.value = false
  }
}

const handleOpenVersionCenter = () => {
  if (!detail.value) {
    throw new Error('产品详情尚未加载完成')
  }
  emit('open-version-center', {
    productId: detail.value.productId,
    revisionId: detail.value.revisionId
  })
}

const requireFormState = () => {
  if (!detail.value || !form.value) {
    throw new Error('产品详情尚未加载完成')
  }
  return { detail: detail.value, form: form.value }
}

const reloadAndNotify = async (successMessage: string) => {
  await loadDialogState()
  if (!detail.value) {
    throw new Error('产品详情刷新失败')
  }
  message.success(successMessage)
  return detail.value
}

const handleSaveDraft = async () => {
  saving.value = true
  try {
    const state = requireFormState()
    const payload = buildProductDraftPayload(state.detail, state.form)
    await ShowroomAdminApi.saveProductDraft(payload)
    const refreshedDetail = await reloadAndNotify('产品草稿已保存')
    emit('saved', refreshedDetail)
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
    throw resolved
  } finally {
    saving.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const state = requireFormState()
    if (!props.submitRoutePreview) {
      throw new Error('当前登录用户审批路线未解析完成，无法提交产品审批')
    }
    let targetRevisionId = state.detail.revisionId
    let fieldCodes = changedFieldCodes.value
    if (fieldCodes.length > 0) {
      const payload = buildProductDraftPayload(state.detail, state.form)
      await ShowroomAdminApi.saveProductDraft(payload)
      await loadDialogState()
      const refreshedState = requireFormState()
      targetRevisionId = refreshedState.detail.revisionId
      fieldCodes = resolveChangedFieldCodes(refreshedState.detail, refreshedState.form)
    }
    await ShowroomAdminApi.submitProduct({
      targetId: state.detail.productId,
      targetRevisionId,
      fieldCodes: [],
      moduleCode: 'product-detail',
      submittedBy: props.submitRoutePreview.submitterUserId,
      submitterDeptId: props.submitRoutePreview.submitterDeptId,
      supervisorUserId: props.submitRoutePreview.supervisorUserId
    })
    message.success('产品变更已提交审批')
    emit('submitted', { productId: state.detail.productId, fieldCodes })
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
    throw resolved
  } finally {
    submitting.value = false
  }
}

const openSignatureDialog = (mode: 'approve' | 'reject') => {
  approvalComment.value = ''
  signatureDialogMode.value = mode
  signatureDialogVisible.value = true
}

const handleApprovalSignatureConfirm = async (payload: { password: string; comment: string }) => {
  if (!approvalDetail.value || !currentUserId.value) {
    throw new Error('当前审批上下文缺失，无法执行审批动作')
  }
  if (!payload.password) {
    message.error('请输入登录密码完成电子签名')
    return
  }
  approvalComment.value = payload.comment
  if (signatureDialogMode.value === 'reject' && !approvalComment.value) {
    message.error('请输入驳回原因')
    return
  }
  approvalSubmitting.value = true
  try {
    const basePayload = {
      id: approvalDetail.value.changeRequest.changeRequestId,
      reviewerUserId: currentUserId.value,
      password: payload.password
    }
    const isPublicityStage = approvalDetail.value.changeRequest.status === 'PENDING_GAOXIN_APPROVAL'
    if (signatureDialogMode.value === 'reject') {
      if (isPublicityStage) {
        await ShowroomAdminApi.gaoxinReject({
          ...basePayload,
          reason: approvalComment.value
        })
      } else {
        await ShowroomAdminApi.supervisorReject({
          ...basePayload,
          reason: approvalComment.value
        })
      }
      message.success('已签名驳回并退回发起人')
    } else if (isPublicityStage) {
      await ShowroomAdminApi.gaoxinApprove({
        ...basePayload,
        comment: approvalComment.value || undefined
      })
      message.success('企宣签名审批已完成')
    } else {
      await ShowroomAdminApi.supervisorApprove({
        ...basePayload,
        comment: approvalComment.value || undefined
      })
      message.success('主管签名审批已完成')
    }
    signatureDialogVisible.value = false
    dialogVisible.value = false
    emit('approval-completed')
  } finally {
    approvalSubmitting.value = false
  }
}

watch(
  () => [props.modelValue, props.productId, props.initialRevisionId, props.approvalChangeRequestId] as const,
  ([visible]) => {
    if (!visible) {
      return
    }
    void loadDialogState()
  },
  { immediate: true }
)
</script>

<style scoped>
.showroom-product-detail-dialog__body {
  min-height: 320px;
}

.showroom-product-detail-dialog__summary {
  margin-bottom: 16px;
}

.showroom-product-detail-dialog__meta {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-product-detail-dialog__meta-label {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 0.85rem;
}

.showroom-product-detail-dialog__version-select {
  width: 100%;
}

.showroom-product-detail-dialog__approval-panel,
.showroom-product-detail-dialog__form {
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-product-detail-dialog__approval-panel {
  margin-bottom: 16px;
}

.showroom-product-detail-dialog__approval-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.showroom-product-detail-dialog__section-title {
  margin-bottom: 8px;
  color: #172033;
  font-size: 0.95rem;
  font-weight: 600;
}

.showroom-product-detail-dialog__approval-meta {
  color: #4b5563;
  font-size: 0.85rem;
}

.showroom-product-detail-dialog__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.showroom-product-detail-dialog__toolbar h4 {
  margin: 0;
  color: #172033;
}

.showroom-product-detail-dialog__toolbar p {
  margin: 6px 0 0;
  color: #4b5563;
  font-size: 0.88rem;
  line-height: 1.6;
}

@media (max-width: 1080px) {
  .showroom-product-detail-dialog__meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .showroom-product-detail-dialog__toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
