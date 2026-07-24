<template>
  <el-button type="primary" link @click="handlePrint">
    {{ labelText }}
  </el-button>
  <BarcodeDetail ref="barcodeDetailRef" />
</template>
<script lang="ts" setup>
import { BarcodeBizTypeEnum } from '@/views/mes/utils/constants'
import BarcodeDetail from './BarcodeDetail.vue'

defineOptions({ name: 'PrinterLabel' })

const props = defineProps({
  bizId: { type: Number, default: undefined }, // 业务实体 ID
  bizCode: { type: String, default: undefined }, // 业务实体编码
  bizName: { type: String, default: undefined }, // 业务实体名称
  bizType: { type: String, default: undefined }, // 业务类型（如 ITEM、BATCH、PROCARD 等）
  labelText: { type: String, default: '标签打印' } // 按钮文字
})
const message = useMessage()
const barcodeDetailRef = ref<InstanceType<typeof BarcodeDetail>>()

const BARCODE_BIZ_TYPE_VALUE_MAP: Record<string, BarcodeBizTypeEnum> = {
  ITEM: BarcodeBizTypeEnum.ITEM,
  BATCH: BarcodeBizTypeEnum.BATCH,
  ITEM_BATCH: BarcodeBizTypeEnum.BATCH,
  PROCARD: BarcodeBizTypeEnum.PROCARD
}

const resolveBizTypeValue = () => {
  if (!props.bizType) {
    return undefined
  }
  return BARCODE_BIZ_TYPE_VALUE_MAP[props.bizType]
}

/** 打印 */
const handlePrint = async () => {
  if (!props.bizId) {
    message.warning('缺少业务编号，无法打开标签打印')
    return
  }
  const bizTypeValue = resolveBizTypeValue()
  if (!bizTypeValue) {
    message.warning(`暂不支持业务类型 ${props.bizType || '-'} 的标签打印`)
    return
  }
  await barcodeDetailRef.value?.openByBusiness(
    props.bizId,
    bizTypeValue,
    props.bizCode,
    props.bizName || props.bizCode
  )
}
</script>
