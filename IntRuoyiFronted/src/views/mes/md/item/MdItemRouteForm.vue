<template>
  <div v-loading="loading" class="md-item-route-form">
    <el-alert
      class="mb-12px"
      type="info"
      :closable="false"
      title="工艺路线关联产品只维护产品编号"
      description="这里维护产品当前选用的工艺路线，工艺路线内关联产品时只需要录入产品编号。"
    />
    <el-alert
      v-if="isCurrentRouteLocked && !isDetail"
      class="mb-12px"
      type="warning"
      :closable="false"
      title="当前工艺路线已启用，不能在产品侧变更或解除"
    />
    <el-alert
      v-if="!persistedRouteId"
      class="mb-12px"
      type="info"
      :closable="false"
      title="未绑定产品可以新增绑定已启用工艺路线；生产数量、生产用时仍在工艺路线关联产品中维护"
    />
    <el-form label-width="120px" :disabled="isDetail || isCurrentRouteLocked">
      <el-form-item label="工艺路线">
        <el-select
          v-model="routeId"
          class="!w-420px"
          filterable
          clearable
          placeholder="请选择工艺路线"
        >
          <el-option
            v-for="route in routeOptions"
            :key="route.id"
            :label="formatRouteLabel(route)"
            :value="route.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!isDetail">
        <el-button
          type="primary"
          :loading="saving"
          :disabled="isCurrentRouteLocked"
          @click="saveRoute"
        >
          保存工艺路线
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ProRouteApi, type ProRouteVO } from '@/api/mes/pro/route'
import { ProRouteProductApi, type ProRouteProductVO } from '@/api/mes/pro/route/product'
import { CommonStatusEnum } from '@/utils/constants'

defineOptions({ name: 'MdItemRouteForm' })

const props = defineProps<{
  itemId: number
  formType: string
}>()

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const routeOptions = ref<ProRouteVO[]>([])
const routeId = ref<number | undefined>()
const persistedRouteId = ref<number | undefined>()
const isDetail = computed(() => props.formType === 'detail')
const persistedRoute = computed(() =>
  routeOptions.value.find((route) => route.id === persistedRouteId.value)
)
const isCurrentRouteLocked = computed(
  () => Boolean(persistedRouteId.value) && persistedRoute.value?.status === CommonStatusEnum.ENABLE
)

const formatRouteLabel = (route: ProRouteVO) => {
  const code = route.code ? `${route.code} / ` : ''
  const version = route.activeRouteVersionNo ? `（${route.activeRouteVersionNo}）` : ''
  const status = route.status === CommonStatusEnum.ENABLE ? '（已启用，可新增产品）' : ''
  return `${code}${route.name || route.id}${version}${status}`
}

const loadData = async () => {
  if (!props.itemId) {
    return
  }
  loading.value = true
  try {
    const [routes, binding] = await Promise.all([
      ProRouteApi.getRouteItemBindingList(),
      ProRouteProductApi.getRouteProductByItem(props.itemId)
    ])
    routeOptions.value = routes || []
    persistedRouteId.value = (binding as ProRouteProductVO | null)?.routeId
    routeId.value = persistedRouteId.value
  } finally {
    loading.value = false
  }
}

const saveRoute = async () => {
  saving.value = true
  try {
    await ProRouteProductApi.saveRouteProductByItem({
      itemId: props.itemId,
      routeId: routeId.value ?? null
    })
    message.success('工艺路线保存成功')
    await loadData()
  } finally {
    saving.value = false
  }
}

watch(
  () => props.itemId,
  () => {
    loadData()
  },
  { immediate: true }
)
</script>
