<template>
  <div class="edhr-release-dossier-requirement-setting">
    <el-card shadow="never">
      <template #header>
        <div class="edhr-release-dossier-requirement-setting__header">
          <div>
            <div class="edhr-release-dossier-requirement-setting__title">eDHR 放行资料限制</div>
            <div class="edhr-release-dossier-requirement-setting__subtitle">
              仅金手指用户可配置；开启后，对应特殊节点必须完成并保存 ADD 附件后才可放行。
            </div>
          </div>
          <el-tag type="info" effect="plain">默认关闭</el-tag>
        </div>
      </template>

      <el-alert
        v-if="loadError"
        :title="loadError"
        type="error"
        :closable="false"
        show-icon
      />

      <div v-else class="edhr-release-dossier-requirement-setting__grid">
        <div
          v-for="item in switchItems"
          :key="item.field"
          class="edhr-release-dossier-requirement-setting__item"
        >
          <div>
            <div class="edhr-release-dossier-requirement-setting__item-title">{{ item.label }}</div>
            <div class="edhr-release-dossier-requirement-setting__item-desc">
              {{ item.description }}
            </div>
          </div>
          <el-switch
            :model-value="setting[item.field]"
            :loading="loading || savingField === item.field"
            :disabled="loading || savingField !== ''"
            active-text="需要"
            inactive-text="不需要"
            @change="(value) => requestSwitchChange(item.field, value === true)"
          />
        </div>
      </div>

      <div v-if="configHash" class="edhr-release-dossier-requirement-setting__meta">
        当前配置 hash：{{ configHash }}
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import {
  type EdhrReleaseDossierRequirementSettingRespVO,
  type EdhrReleaseDossierRequirementSettingUpdateReqVO,
  getEdhrReleaseDossierRequirementSetting,
  updateEdhrReleaseDossierRequirementSetting
} from '@/api/mes/pro/edhr/releaseDossierRequirementSetting'

type DossierRequirementField = keyof EdhrReleaseDossierRequirementSettingUpdateReqVO

const message = useMessage()

const defaultSetting = (): EdhrReleaseDossierRequirementSettingUpdateReqVO => ({
  incomingInspectionReportRequired: false,
  sterilizationReportRequired: false,
  finishedProductInspectionReportRequired: false,
  finishedProductInspectionRecordRequired: false
})

const switchItems: Array<{
  field: DossierRequirementField
  label: string
  description: string
}> = [
  {
    field: 'incomingInspectionReportRequired',
    label: '来料检报告',
    description: '打开后，来料检报告特殊节点必须完成，并至少保存 1 个 ADD 附件。'
  },
  {
    field: 'sterilizationReportRequired',
    label: '灭菌报告',
    description: '打开后，灭菌报告特殊节点必须完成，并至少保存 1 个 ADD 附件。'
  },
  {
    field: 'finishedProductInspectionReportRequired',
    label: '成品检报告',
    description: '打开后，成品检报告特殊节点必须完成，并至少保存 1 个 ADD 附件。'
  },
  {
    field: 'finishedProductInspectionRecordRequired',
    label: '成品检记录限制',
    description: '打开后，成品检记录特殊节点必须完成，并至少保存 1 个 ADD 附件。'
  }
]

const loading = ref(false)
const savingField = ref<DossierRequirementField | ''>('')
const loadError = ref('')
const setting = ref<EdhrReleaseDossierRequirementSettingUpdateReqVO>(defaultSetting())
const configHash = ref('')

const cloneSetting = (source: EdhrReleaseDossierRequirementSettingUpdateReqVO) => ({ ...source })

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message.trim()) return error.message
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  return typeof responseMessage === 'string' && responseMessage.trim() ? responseMessage : defaultMessage
}

const isCancelAction = (error: unknown) => error === 'cancel' || error === 'close'

const applyLoadedSetting = (result: EdhrReleaseDossierRequirementSettingRespVO) => {
  setting.value = {
    incomingInspectionReportRequired: result.incomingInspectionReportRequired === true,
    sterilizationReportRequired: result.sterilizationReportRequired === true,
    finishedProductInspectionReportRequired: result.finishedProductInspectionReportRequired === true,
    finishedProductInspectionRecordRequired: result.finishedProductInspectionRecordRequired === true
  }
  configHash.value = result.configHash || ''
}

const loadSetting = async () => {
  loading.value = true
  loadError.value = ''
  try {
    applyLoadedSetting(await getEdhrReleaseDossierRequirementSetting())
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '放行资料限制开关加载失败。')
  } finally {
    loading.value = false
  }
}

const buildFullPayload = (): EdhrReleaseDossierRequirementSettingUpdateReqVO => ({
  incomingInspectionReportRequired: setting.value.incomingInspectionReportRequired,
  sterilizationReportRequired: setting.value.sterilizationReportRequired,
  finishedProductInspectionReportRequired: setting.value.finishedProductInspectionReportRequired,
  finishedProductInspectionRecordRequired: setting.value.finishedProductInspectionRecordRequired
})

const requestSwitchChange = async (field: DossierRequirementField, nextValue: boolean) => {
  if (loading.value || savingField.value) return
  const previousSetting = cloneSetting(setting.value)
  setting.value = { ...setting.value, [field]: nextValue }
  const item = switchItems.find((candidate) => candidate.field === field)
  const actionText = nextValue ? '打开' : '关闭'

  try {
    await ElMessageBox.confirm(
      `确认${actionText}${item?.label || '资料限制'}？${nextValue ? '打开后，缺少对应资料的批次将无法放行。' : '关闭后，放行不再强制要求该资料。'}`,
      `${actionText}放行资料限制`,
      { type: nextValue ? 'warning' : 'info' }
    )
  } catch (error) {
    setting.value = previousSetting
    if (!isCancelAction(error)) {
      message.error(resolveErrorMessage(error, '放行资料限制开关确认失败。'))
    }
    return
  }

  savingField.value = field
  try {
    const result = await updateEdhrReleaseDossierRequirementSetting(buildFullPayload())
    applyLoadedSetting(result)
    message.success(`${item?.label || '资料限制'}已${nextValue ? '打开' : '关闭'}`)
  } catch (error) {
    setting.value = previousSetting
    message.error(resolveErrorMessage(error, '放行资料限制开关接口保存失败。'))
  } finally {
    savingField.value = ''
  }
}

onMounted(loadSetting)
</script>

<style scoped>
.edhr-release-dossier-requirement-setting {
  max-width: 860px;
}

.edhr-release-dossier-requirement-setting__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.edhr-release-dossier-requirement-setting__title {
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 600;
}

.edhr-release-dossier-requirement-setting__subtitle {
  margin-top: 4px;
  color: #6b778c;
  font-size: 13px;
}

.edhr-release-dossier-requirement-setting__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.edhr-release-dossier-requirement-setting__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 76px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.edhr-release-dossier-requirement-setting__item-title {
  color: #1f2d3d;
  font-size: 14px;
  font-weight: 600;
}

.edhr-release-dossier-requirement-setting__item-desc {
  margin-top: 4px;
  color: #6b778c;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-release-dossier-requirement-setting__meta {
  margin-top: 12px;
  color: #8a96a8;
  font-size: 12px;
  word-break: break-all;
}

@media (max-width: 768px) {
  .edhr-release-dossier-requirement-setting__grid {
    grid-template-columns: 1fr;
  }
}
</style>
