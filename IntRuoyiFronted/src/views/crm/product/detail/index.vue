<template>
  <ProductDetailsHeader :loading="loading" :product="product" @refresh="handleRefresh" />
  <el-col>
    <el-tabs>
      <el-tab-pane label="详细资料">
        <ProductDetailsInfo :product="product" />
      </el-tab-pane>
      
    </el-tabs>
  </el-col>
</template>
<script lang="ts" setup>
import { useTagsViewStore } from '@/store/modules/tagsView'

import * as ProductApi from '@/api/crm/product'
import ProductDetailsHeader from '@/views/crm/product/detail/ProductDetailsHeader.vue'
import ProductDetailsInfo from '@/views/crm/product/detail/ProductDetailsInfo.vue'



defineOptions({ name: 'CrmProductDetail' })

const route = useRoute()
const message = useMessage()
const id = route.params.id // 编号
const loading = ref(true) // 加载中
const product = ref<ProductApi.ProductVO>({} as ProductApi.ProductVO) // 详情

/** 获取详情 */
const productId = Number(Array.isArray(id) ? id[0] : id)

const getProductData = async (id: number) => {
  loading.value = true
  try {
    product.value = await ProductApi.getProduct(id)
    
  } finally {
    loading.value = false
  }
}

/** 获取操作日志 */
 // 操作日志列表


/** 初始化 */
const { delView } = useTagsViewStore() // 视图操作
const { currentRoute } = useRouter() // 路由
const handleRefresh = async () => {
  if (!Number.isFinite(productId) || productId <= 0) {
    return
  }
  await getProductData(productId)
}

onMounted(async () => {
  if (!id) {
    message.warning('参数错误，产品不能为空！')
    delView(unref(currentRoute))
    return
  }
  await getProductData(productId)
})
</script>
