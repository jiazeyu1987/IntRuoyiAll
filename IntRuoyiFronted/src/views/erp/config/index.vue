<template>
  <doc-alert title="【ERP】金蝶连接与同步配置" url="https://doc.iocoder.cn/erp/" />

  <ContentWrap>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="160px"
      v-loading="formLoading"
    >
      <el-card shadow="never">
        <template #header>
          <div class="flex items-center justify-between">
            <CardTitle title="ERP 配置管理" />
            <el-button type="primary" @click="onSubmit" v-hasPermi="['erp:kingdee-config:save']">
              保存
            </el-button>
          </div>
        </template>

        <el-tabs>
          <el-tab-pane label="基础连接">
            <el-form-item label="基础地址" prop="baseUrl">
              <el-input v-model="formData.baseUrl" placeholder="请输入金蝶基础地址" />
            </el-form-item>
            <el-form-item label="账套 ID" prop="acctId">
              <el-input v-model="formData.acctId" placeholder="请输入账套 ID" />
            </el-form-item>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formData.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="formData.password"
                type="password"
                show-password
                placeholder="请输入密码"
              />
            </el-form-item>
            <el-form-item label="语言 LCID" prop="lcid">
              <el-input-number v-model="formData.lcid" :min="1" class="!w-260px" />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="产品同步">
            <el-form-item label="产品查询上限" prop="product.queryLimit">
              <el-input-number v-model="formData.product.queryLimit" :min="1" class="!w-260px" />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="生产同步">
            <el-form-item label="生产订单模板单号" prop="productionOrder.templateBillNo">
              <el-input
                v-model="formData.productionOrder.templateBillNo"
                placeholder="请输入生产订单模板单号"
              />
            </el-form-item>
            <el-form-item label="生产查询上限" prop="productionOrder.queryLimit">
              <el-input-number
                v-model="formData.productionOrder.queryLimit"
                :min="1"
                class="!w-260px"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="采购同步">
            <el-form-item label="采购组织编码" prop="purchaseOrder.purchaseOrgNumber">
              <el-input
                v-model="formData.purchaseOrder.purchaseOrgNumber"
                placeholder="请输入采购组织编码"
              />
            </el-form-item>
            <el-form-item label="采购查询天数" prop="purchaseOrder.queryDays">
              <el-input-number
                v-model="formData.purchaseOrder.queryDays"
                :min="1"
                class="!w-260px"
              />
            </el-form-item>
            <el-form-item label="采购查询上限" prop="purchaseOrder.queryLimit">
              <el-input-number
                v-model="formData.purchaseOrder.queryLimit"
                :min="1"
                class="!w-260px"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="销售同步">
            <el-form-item label="销售查询天数" prop="saleOrder.queryDays">
              <el-input-number
                v-model="formData.saleOrder.queryDays"
                :min="1"
                class="!w-260px"
              />
            </el-form-item>
            <el-form-item label="销售查询上限" prop="saleOrder.queryLimit">
              <el-input-number
                v-model="formData.saleOrder.queryLimit"
                :min="1"
                class="!w-260px"
              />
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </el-form>
  </ContentWrap>
</template>

<script setup lang="ts">
import { CardTitle } from '@/components/Card'
import { ErpKingdeeConfigApi, ErpKingdeeConfigVO } from '@/api/erp/config'

defineOptions({ name: 'ErpKingdeeConfig' })

const { t } = useI18n()
const message = useMessage()

const formLoading = ref(false)
const formRef = ref()
const formData = ref<ErpKingdeeConfigVO>({
  baseUrl: '',
  acctId: '',
  username: '',
  password: '',
  lcid: 2052,
  product: {
    queryLimit: 5000
  },
  productionOrder: {
    queryLimit: 1000,
    templateBillNo: ''
  },
  purchaseOrder: {
    purchaseOrgNumber: '',
    queryDays: 365,
    queryLimit: 1000
  },
  saleOrder: {
    queryDays: 365,
    queryLimit: 1000
  }
})

const formRules = reactive({
  baseUrl: [{ required: true, message: '基础地址不能为空', trigger: 'blur' }],
  acctId: [{ required: true, message: '账套 ID 不能为空', trigger: 'blur' }],
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  lcid: [{ required: true, message: '语言 LCID 不能为空', trigger: 'blur' }],
  'product.queryLimit': [{ required: true, message: '产品查询上限不能为空', trigger: 'blur' }],
  'productionOrder.queryLimit': [
    { required: true, message: '生产查询上限不能为空', trigger: 'blur' }
  ],
  'productionOrder.templateBillNo': [
    { required: true, message: '生产订单模板单号不能为空', trigger: 'blur' }
  ],
  'purchaseOrder.purchaseOrgNumber': [
    { required: true, message: '采购组织编码不能为空', trigger: 'blur' }
  ],
  'purchaseOrder.queryDays': [{ required: true, message: '采购查询天数不能为空', trigger: 'blur' }],
  'purchaseOrder.queryLimit': [{ required: true, message: '采购查询上限不能为空', trigger: 'blur' }],
  'saleOrder.queryDays': [{ required: true, message: '销售查询天数不能为空', trigger: 'blur' }],
  'saleOrder.queryLimit': [{ required: true, message: '销售查询上限不能为空', trigger: 'blur' }]
})

const getConfig = async () => {
  formLoading.value = true
  try {
    const data = await ErpKingdeeConfigApi.getConfig()
    if (data) {
      formData.value = data
    }
  } finally {
    formLoading.value = false
  }
}

const onSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    await ErpKingdeeConfigApi.saveConfig(formData.value)
    message.success(t('common.updateSuccess'))
    await getConfig()
  } finally {
    formLoading.value = false
  }
}

onMounted(() => {
  getConfig()
})
</script>
