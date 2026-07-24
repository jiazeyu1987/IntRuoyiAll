<template>
  <Dialog v-model="dialogVisible" :max-height="560" :scroll="true" title="消息详情">
    <div v-loading="detailLoading" class="notify-message-detail">
      <section class="notify-message-detail__header">
        <div>
          <div class="notify-message-detail__eyebrow">站内信</div>
          <h3 class="notify-message-detail__title">
            {{ detailData.templateNickname || '系统通知' }}
          </h3>
        </div>
        <dict-tag :type="DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE" :value="detailData.templateType" />
      </section>

      <section class="notify-message-detail__meta-grid">
        <div class="notify-message-detail__meta-item">
          <span>发送人</span>
          <strong>{{ detailData.templateNickname || '-' }}</strong>
        </div>
        <div class="notify-message-detail__meta-item">
          <span>发送时间</span>
          <strong>{{ formatDate(detailData.createTime) || '-' }}</strong>
        </div>
        <div class="notify-message-detail__meta-item">
          <span>阅读状态</span>
          <dict-tag :type="DICT_TYPE.INFRA_BOOLEAN_STRING" :value="detailData.readStatus" />
        </div>
        <div v-if="detailData.readStatus" class="notify-message-detail__meta-item">
          <span>阅读时间</span>
          <strong>{{ formatDate(detailData.readTime) || '-' }}</strong>
        </div>
      </section>

      <section class="notify-message-detail__content">
        <div class="notify-message-detail__section-title">消息内容</div>
        <button
          v-if="hasDetailTarget"
          type="button"
          class="notify-message-detail__content-button"
          @click="handleContentClick"
        >
          {{ detailData.templateContent || '-' }}
        </button>
        <p v-else>{{ detailData.templateContent || '-' }}</p>
      </section>

      <section v-if="templateParamEntries.length" class="notify-message-detail__params">
        <div class="notify-message-detail__section-title">业务信息</div>
        <div class="notify-message-detail__param-grid">
          <div
            v-for="item in templateParamEntries"
            :key="item.key"
            class="notify-message-detail__param-item"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </section>

      <section v-if="hasDetailActions" class="notify-message-detail__actions">
        <div class="notify-message-detail__section-title">快捷操作</div>
        <div class="notify-message-detail__action-buttons">
          <el-button v-if="showroomProductNavigation" type="primary" @click="navigateToShowroomProduct">
            查看关联产品
          </el-button>
          <el-button v-if="bpmApprovalNavigation" type="primary" @click="navigateToBpmApproval">
            去审批
          </el-button>
          <el-button v-if="edhrWorkTaskNavigation" type="primary" @click="navigateToEdhrWorkTask">
            处理批记录任务
          </el-button>
        </div>
      </section>
    </div>
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as NotifyMessageApi from '@/api/system/notify/message'
import {
  getNotifyMessageTarget,
  getNotifyMessageTargets,
  navigateToNotifyMessageTarget,
  NOTIFY_MESSAGE_NAVIGATION_PARAM_KEYS,
  type BpmApprovalNotifyTarget,
  type EdhrWorkTaskNotifyTarget,
  type ShowroomProductNotifyTarget
} from '@/utils/notifyMessageNavigation'

defineOptions({ name: 'MyNotifyMessageDetailDetail' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref({} as NotifyMessageApi.NotifyMessageVO)
const router = useRouter()

const resetDialogState = () => {
  dialogVisible.value = false
  detailLoading.value = false
  detailData.value = {} as NotifyMessageApi.NotifyMessageVO
}

const normalizeTemplateParams = (value: unknown) => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return null
  }
  return value as Record<string, unknown>
}

const notifyMessageTargets = computed(() => getNotifyMessageTargets(detailData.value))
const primaryDetailTarget = computed(() => getNotifyMessageTarget(detailData.value))
const hasDetailTarget = computed(() => Boolean(primaryDetailTarget.value))
const showroomProductNavigation = computed(
  () =>
    notifyMessageTargets.value.find(
      (target): target is ShowroomProductNotifyTarget => target.type === 'showroomProduct'
    ) ?? null
)
const bpmApprovalNavigation = computed(
  () =>
    notifyMessageTargets.value.find(
      (target): target is BpmApprovalNotifyTarget => target.type === 'bpmApproval'
    ) ?? null
)
const edhrWorkTaskNavigation = computed(
  () =>
    notifyMessageTargets.value.find(
      (target): target is EdhrWorkTaskNotifyTarget => target.type === 'edhrWorkTask'
    ) ?? null
)

const hiddenTemplateParamKeys = NOTIFY_MESSAGE_NAVIGATION_PARAM_KEYS

const templateParamLabels: Record<string, string> = {
  processInstanceName: '流程名称',
  taskName: '任务名称',
  startUserNickname: '申请人',
  reason: '原因',
  businessTitle: '业务标题',
  businessCode: '业务编号',
  moduleName: '来源模块',
  result: '处理结果'
}

const formatTemplateParamValue = (value: unknown): string => {
  if (Array.isArray(value)) {
    return value.map((item) => formatTemplateParamValue(item)).join('、')
  }
  if (value && typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

const templateParamEntries = computed(() => {
  const templateParams = normalizeTemplateParams(detailData.value.templateParams)
  if (!templateParams) {
    return []
  }
  return Object.entries(templateParams)
    .filter(([key, value]) => !hiddenTemplateParamKeys.has(key) && value !== undefined && value !== null && value !== '')
    .map(([key, value]) => ({
      key,
      label: templateParamLabels[key] || key,
      value: formatTemplateParamValue(value)
    }))
})

const hasDetailActions = computed(() => notifyMessageTargets.value.length > 0)

const open = async (data: NotifyMessageApi.NotifyMessageVO) => {
  dialogVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = data
  } finally {
    detailLoading.value = false
  }
}

const navigateToShowroomProduct = async () => {
  const navigation = showroomProductNavigation.value
  if (!navigation) {
    return
  }
  await navigateToNotifyMessageTarget(router, navigation, {
    beforeNavigate: async () => {
      resetDialogState()
      await nextTick()
    },
    delayMs: 350
  })
}

const navigateToBpmApproval = async () => {
  const navigation = bpmApprovalNavigation.value
  if (!navigation) {
    return
  }
  await navigateToNotifyMessageTarget(router, navigation, {
    beforeNavigate: async () => {
      resetDialogState()
      await nextTick()
    }
  })
}

const navigateToEdhrWorkTask = async () => {
  const navigation = edhrWorkTaskNavigation.value
  if (!navigation) {
    return
  }
  await navigateToNotifyMessageTarget(router, navigation, {
    beforeNavigate: async () => {
      resetDialogState()
      await nextTick()
    }
  })
}

const handleContentClick = async () => {
  const target = primaryDetailTarget.value
  if (!target) {
    return
  }
  await navigateToNotifyMessageTarget(router, target, {
    beforeNavigate: async () => {
      resetDialogState()
      await nextTick()
    },
    delayMs: target.type === 'showroomProduct' ? 350 : 0
  })
}

onBeforeRouteLeave(() => {
  resetDialogState()
})

onDeactivated(() => {
  resetDialogState()
})

defineExpose({ open })
</script>

<style scoped>
.notify-message-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
  color: #172033;
}

.notify-message-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f7f9fc;
}

.notify-message-detail__eyebrow {
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 12px;
}

.notify-message-detail__title {
  margin: 0;
  color: #172033;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}

.notify-message-detail__meta-grid,
.notify-message-detail__param-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.notify-message-detail__meta-item,
.notify-message-detail__param-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #ffffff;
}

.notify-message-detail__meta-item span,
.notify-message-detail__param-item span {
  display: block;
  margin-bottom: 5px;
  color: #4b5563;
  font-size: 12px;
}

.notify-message-detail__meta-item strong,
.notify-message-detail__param-item strong {
  display: block;
  overflow-wrap: anywhere;
  color: #263247;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.45;
}

.notify-message-detail__content,
.notify-message-detail__params,
.notify-message-detail__actions {
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.notify-message-detail__section-title {
  margin-bottom: 8px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.notify-message-detail__content p {
  margin: 0;
  color: #263247;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.notify-message-detail__content-button {
  display: block;
  width: 100%;
  padding: 0;
  color: #1677ff;
  font: inherit;
  font-size: 14px;
  line-height: 1.7;
  text-align: left;
  white-space: pre-wrap;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.notify-message-detail__content-button:hover,
.notify-message-detail__content-button:focus-visible {
  color: #0958d9;
  text-decoration: underline;
  outline: none;
}

.notify-message-detail__action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 640px) {
  .notify-message-detail__meta-grid,
  .notify-message-detail__param-grid {
    grid-template-columns: 1fr;
  }
}
</style>
