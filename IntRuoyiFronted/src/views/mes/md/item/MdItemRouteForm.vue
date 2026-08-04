<template>
  <div v-loading="loading" class="md-item-route-form">
    <el-alert
      class="mb-12px"
      type="info"
      :closable="false"
      title="生产数量、生产用时仍在工艺路线关联产品中维护"
      description="这里仅维护产品当前选用的工艺路线，路线内的产品参数继续在工艺路线关联产品中管理。"
    />
    <el-form label-width="120px" :disabled="isDetail">
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
        <el-button type="primary" :loading="saving" @click="saveRoute">
          保存工艺路线
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ProRouteApi, type ProRouteVO } from '@/api/mes/pro/route'
import {
  ProRouteProductApi,
  type ProRouteProductVO
} from '@/api/mes/pro/route/product'

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
const isDetail = computed(() => props.formType === 'detail')

const formatRouteLabel = (route: ProRouteVO) => {
  const code = route.code ? `${route.code} / ` : ''
  const version = route.activeRouteVersionNo ? `（${route.activeRouteVersionNo}）` : ''
  return `${code}${route.name || route.id}${version}`
}

const loadData = async () => {
  if (!props.itemId) {
    return
  }
  loading.value = true
  try {
    const [routes, binding] = await Promise.all([
      ProRouteApi.getRouteSimpleList(),
      ProRouteProductApi.getRouteProductByItem(props.itemId)
    ])
    routeOptions.value = routes || []
    routeId.value = (binding as ProRouteProductVO | null)?.routeId
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
