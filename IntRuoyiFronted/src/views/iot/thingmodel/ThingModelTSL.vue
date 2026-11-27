<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <ReadableData title="物模型定义" :source="JSON.stringify(thingModelTSL)" />
  </Dialog>
</template>

<script setup lang="ts">
import ReadableData from '@/components/BusinessData/ReadableData.vue'
import { ThingModelApi } from '@/api/iot/thingmodel'
import { ProductVO } from '@/api/iot/product/product'
import { IOT_PROVIDE_KEY } from '@/views/iot/utils/constants'

defineOptions({ name: 'ThingModelTSL' })

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('物模型 TSL') // 弹窗的标题
const product = inject<Ref<ProductVO>>(IOT_PROVIDE_KEY.PRODUCT) // 注入产品信息

/** 打开弹窗 */
const open = () => {
  dialogVisible.value = true
}
defineExpose({ open })

/** 获取 TSL */
const thingModelTSL = ref({})
const getTsl = async () => {
  thingModelTSL.value = await ThingModelApi.getThingModelTSLByProductId(product?.value?.id || 0)
}

/** 初始化 **/
onMounted(async () => {
  await getTsl()
})
</script>
