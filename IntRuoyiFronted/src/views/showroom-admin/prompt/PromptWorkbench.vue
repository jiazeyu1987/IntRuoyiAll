<template>
  <div class="showroom-prompt-workbench" v-loading="loading">
    <el-alert
      v-if="!isShowroomPublicity"
      :closable="false"
      show-icon
      type="error"
      title="当前用户无权访问提示管理"
      description="只有企宣角色可以查看和保存产品封面提示词版本。"
    />

    <template v-else>
      <el-alert
        v-if="loadError"
        :closable="false"
        show-icon
        type="error"
        :title="loadError"
      />

      <template v-else-if="currentVersion">
        <div class="showroom-prompt-workbench__grid">
          <section class="showroom-prompt-workbench__panel">
            <div class="showroom-prompt-workbench__panel-header">
              <div>
                <h3>当前生效版本</h3>
                <p class="showroom-prompt-workbench__panel-tip">
                  当前产品封面生成默认使用此版本提示词。
                </p>
              </div>
              <el-tag type="success">PRODUCT_COVER</el-tag>
            </div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="版本号">
                V{{ currentVersion.versionNo }}
              </el-descriptions-item>
              <el-descriptions-item label="创建人">
                {{ resolveCreator(currentVersion.creator) }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatDateTime(currentVersion.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="使用次数">
                {{ currentVersion.useCount }}
              </el-descriptions-item>
              <el-descriptions-item label="最近使用时间">
                {{ formatDateTime(currentVersion.lastUsedAt, '尚未使用') }}
              </el-descriptions-item>
              <el-descriptions-item label="版本说明">
                {{ currentVersion.changeNote || '未填写版本说明' }}
              </el-descriptions-item>
              <el-descriptions-item label="支持占位符" :span="2">
                <div class="showroom-prompt-workbench__placeholder-tags">
                  <el-tag
                    v-for="placeholderCode in currentVersion.placeholderCodes"
                    :key="`current-${placeholderCode}`"
                    type="info"
                  >
                    {{ formatPlaceholder(placeholderCode) }}
                  </el-tag>
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="showroom-prompt-workbench__panel">
            <div class="showroom-prompt-workbench__panel-header">
              <div>
                <h3>保存新版本</h3>
                <p class="showroom-prompt-workbench__panel-tip">
                  每次保存都会生成新版本，不会覆盖旧版本。
                </p>
              </div>
            </div>

            <div class="showroom-prompt-workbench__placeholder-guide">
              <span class="showroom-prompt-workbench__guide-label">可用占位符</span>
              <el-tag
                v-for="placeholderCode in supportedPlaceholderCodes"
                :key="`guide-${placeholderCode}`"
                type="info"
              >
                {{ formatPlaceholder(placeholderCode) }}
              </el-tag>
            </div>
            <p class="showroom-prompt-workbench__guide-copy">
              模板至少包含一个产品名占位符；未知占位符会直接保存失败。
            </p>

            <el-form label-position="top">
              <el-form-item label="提示词模板">
                <el-input
                  v-model="draft.templateText"
                  :autosize="{ minRows: 12, maxRows: 20 }"
                  placeholder="请输入新的产品封面提示词模板"
                  type="textarea"
                />
              </el-form-item>
              <el-form-item label="版本说明">
                <el-input
                  v-model="draft.changeNote"
                  maxlength="255"
                  placeholder="说明这次提示词修改的目的或差异"
                  show-word-limit
                />
              </el-form-item>
              <div class="showroom-prompt-workbench__form-actions">
                <el-button @click="resetDraftFromCurrent">恢复当前版本内容</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">
                  保存新版本
                </el-button>
              </div>
            </el-form>
          </section>
        </div>

        <section class="showroom-prompt-workbench__panel">
          <div class="showroom-prompt-workbench__panel-header">
            <div>
              <h3>历史版本</h3>
              <p class="showroom-prompt-workbench__panel-tip">
                这里只读查看历史版本，不支持直接用旧版本重新生成图片。
              </p>
            </div>
          </div>

          <el-table :data="historyRows" row-key="promptVersionId" empty-text="暂无提示词历史版本">
            <el-table-column label="版本" width="100">
              <template #default="{ row }">
                <div class="showroom-prompt-workbench__version-cell">
                  <strong>V{{ row.versionNo }}</strong>
                  <el-tag v-if="row.current" size="small" type="success">当前</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="版本说明" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.changeNote || '未填写版本说明' }}
              </template>
            </el-table-column>
            <el-table-column label="占位符" min-width="180">
              <template #default="{ row }">
                <div class="showroom-prompt-workbench__placeholder-tags">
                  <el-tag
                    v-for="placeholderCode in row.placeholderCodes"
                    :key="`${row.promptVersionId}-${placeholderCode}`"
                    type="info"
                  >
                    {{ formatPlaceholder(placeholderCode) }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="使用次数" width="100" prop="useCount" />
            <el-table-column label="最近使用时间" min-width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.lastUsedAt, '尚未使用') }}
              </template>
            </el-table-column>
            <el-table-column label="创建人" width="140">
              <template #default="{ row }">
                {{ resolveCreator(row.creator) }}
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openHistoryPreview(row)">
                  查看内容
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <el-dialog
          v-model="historyPreviewVisible"
          title="提示词版本详情"
          width="980px"
          destroy-on-close
        >
          <template v-if="activeHistoryItem">
            <el-descriptions :column="2" border class="showroom-prompt-workbench__dialog-meta">
              <el-descriptions-item label="版本号">
                V{{ activeHistoryItem.versionNo }}
              </el-descriptions-item>
              <el-descriptions-item label="创建人">
                {{ resolveCreator(activeHistoryItem.creator) }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatDateTime(activeHistoryItem.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="使用次数">
                {{ activeHistoryItem.useCount }}
              </el-descriptions-item>
              <el-descriptions-item label="最近使用时间">
                {{ formatDateTime(activeHistoryItem.lastUsedAt, '尚未使用') }}
              </el-descriptions-item>
              <el-descriptions-item label="版本说明">
                {{ activeHistoryItem.changeNote || '未填写版本说明' }}
              </el-descriptions-item>
            </el-descriptions>
            <el-input
              :model-value="activeHistoryItem.templateText"
              :autosize="{ minRows: 14, maxRows: 22 }"
              readonly
              type="textarea"
            />
          </template>
          <template #footer>
            <el-button @click="historyPreviewVisible = false">关闭</el-button>
          </template>
        </el-dialog>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import {
  ShowroomAdminApi,
  type ShowroomImagePromptCurrentRespVO,
  type ShowroomImagePromptHistoryItemRespVO
} from '@/api/showroom-admin'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'ShowroomPromptWorkbench' })

const SCENE_CODE = 'PRODUCT_COVER'
const SHOWROOM_PUBLICITY_ROLE_CODE = 'showroom_publicity'
const PLACEHOLDER_PATTERN = /\{\{\s*([a-zA-Z0-9_]+)\s*}}/g
const supportedPlaceholderCodes = ['product_name_cn', 'product_name_en'] as const

const message = useMessage()
const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const currentVersion = ref<ShowroomImagePromptCurrentRespVO | null>(null)
const historyRows = ref<ShowroomImagePromptHistoryItemRespVO[]>([])
const historyPreviewVisible = ref(false)
const activeHistoryItem = ref<ShowroomImagePromptHistoryItemRespVO | null>(null)
const draft = reactive({
  templateText: '',
  changeNote: ''
})

const isShowroomPublicity = computed(() => {
  return userStore.getRoles.includes(SHOWROOM_PUBLICITY_ROLE_CODE)
})

const formatDateTime = (value?: number | null, emptyText = '未记录') => {
  if (!value) {
    return emptyText
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const resolveCreator = (creator: string) => {
  return creator?.trim() ? creator : '系统'
}

const formatPlaceholder = (placeholderCode: string) => {
  return `{{${placeholderCode}}}`
}

const resetDraftFromCurrent = () => {
  draft.templateText = currentVersion.value?.templateText || ''
  draft.changeNote = ''
}

const validateDraft = () => {
  const templateText = draft.templateText.trim()
  if (!templateText) {
    return '提示词模板不能为空'
  }
  const placeholderCodes = new Set<string>()
  for (const match of templateText.matchAll(PLACEHOLDER_PATTERN)) {
    const placeholderCode = String(match[1] || '').trim()
    if (!supportedPlaceholderCodes.includes(placeholderCode as (typeof supportedPlaceholderCodes)[number])) {
      return `存在不支持的占位符：{{${placeholderCode}}}`
    }
    placeholderCodes.add(placeholderCode)
  }
  if (
    !placeholderCodes.has('product_name_cn') &&
    !placeholderCodes.has('product_name_en')
  ) {
    return '模板至少包含一个产品名占位符'
  }
  return ''
}

const loadData = async () => {
  if (!isShowroomPublicity.value) {
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const [current, history] = await Promise.all([
      ShowroomAdminApi.getImagePromptCurrent(SCENE_CODE),
      ShowroomAdminApi.getImagePromptHistory(SCENE_CODE)
    ])
    currentVersion.value = current
    historyRows.value = history
    resetDraftFromCurrent()
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    loadError.value = resolved.message
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  const validationMessage = validateDraft()
  if (validationMessage) {
    message.warning(validationMessage)
    return
  }
  saving.value = true
  try {
    await ShowroomAdminApi.saveImagePromptVersion({
      sceneCode: SCENE_CODE,
      templateText: draft.templateText.trim(),
      changeNote: draft.changeNote.trim()
    })
    message.success('提示词版本已保存')
    await loadData()
  } finally {
    saving.value = false
  }
}

const openHistoryPreview = (row: ShowroomImagePromptHistoryItemRespVO) => {
  activeHistoryItem.value = row
  historyPreviewVisible.value = true
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.showroom-prompt-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showroom-prompt-workbench__grid {
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(0, 1.05fr);
  gap: 16px;
}

.showroom-prompt-workbench__panel {
  padding: 20px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.showroom-prompt-workbench__panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.showroom-prompt-workbench__panel-header h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.showroom-prompt-workbench__panel-tip {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.showroom-prompt-workbench__placeholder-guide {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.showroom-prompt-workbench__guide-label {
  font-size: 13px;
  color: #475569;
}

.showroom-prompt-workbench__guide-copy {
  margin: 0 0 16px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.showroom-prompt-workbench__form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.showroom-prompt-workbench__placeholder-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.showroom-prompt-workbench__version-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.showroom-prompt-workbench__dialog-meta {
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .showroom-prompt-workbench__grid {
    grid-template-columns: 1fr;
  }
}
</style>
