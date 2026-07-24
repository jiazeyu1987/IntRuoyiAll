<template>
  <ProductDetailsHeader :loading="loading" :product="product" @refresh="handleRefresh" />
  <el-col>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="产品信息" name="info">
        <ProductDetailsInfo v-if="activeTab === 'info'" :product="product" />
      </el-tab-pane>
      <el-tab-pane label="物模型（功能定义）" lazy name="thingModel">
        <IoTProductThingModel ref="thingModelRef" />
      </el-tab-pane>
    </el-tabs>
  </el-col>
</template>
<script lang="ts" setup>
import { ProductApi, ProductVO } from '@/api/iot/product/product'
import { DeviceApi } from '@/api/iot/device/device'
import ProductDetailsHeader from './ProductDetailsHeader.vue'
import ProductDetailsInfo from './ProductDetailsInfo.vue'
import IoTProductThingModel from '@/views/iot/thingmodel/index.vue'
import { useTagsViewStore } from '@/store/modules/tagsView'
import { useRouter } from 'vue-router'
import { IOT_PROVIDE_KEY } from '@/views/iot/utils/constants'

defineOptions({ name: 'IoTProductDetail' })

const { delView } = useTagsViewStore()
const { currentRoute } = useRouter()

const route = useRoute()
const message = useMessage()
const resolveRouteNumber = (value: string | string[] | undefined) => {
  const routeValue = Array.isArray(value) ? value[0] : value
  if (!routeValue) {
    return undefined
  }
  const parsedValue = Number(routeValue)
  return Number.isNaN(parsedValue) ? undefined : parsedValue
}
const id = resolveRouteNumber(route.params.id)
const loading = ref(true)
const product = ref<ProductVO>({} as ProductVO)
const activeTab = ref('info')

provide(IOT_PROVIDE_KEY.PRODUCT, product)

const getProductData = async (id: number) => {
  loading.value = true
  try {
    product.value = await ProductApi.getProduct(id)
  } finally {
    loading.value = false
  }
}

const handleRefresh = async () => {
  if (id === undefined) {
    return
  }
  await getProductData(id)
}

const getDeviceCount = async (productId: number) => {
  try {
    return await DeviceApi.getDeviceCount(productId)
  } catch (error) {
    console.error('Error fetching device count:', error, 'productId:', productId)
    return 0
  }
}

onMounted(async () => {
  if (id === undefined) {
    message.warning('参数错误，产品不能为空！')
    delView(unref(currentRoute))
    return
  }
  await getProductData(id)
  const routeTab = Array.isArray(route.query.tab) ? route.query.tab[0] : route.query.tab
  if (typeof routeTab === 'string' && routeTab) {
    activeTab.value = routeTab
  }
  if (product.value.id) {
    product.value.deviceCount = await getDeviceCount(product.value.id)
  }
})
</script>
