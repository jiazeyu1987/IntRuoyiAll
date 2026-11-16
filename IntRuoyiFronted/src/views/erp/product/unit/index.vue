<template>
  <doc-alert title="【产品】产品信息、分类、单位" url="https://doc.iocoder.cn/erp/product/" />

  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="单位名字" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入单位名字"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="单位状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择单位状态"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['erp:product-unit:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="warning"
          plain
          @click="openImport"
          v-hasPermi="['erp:product-unit:create']"
        >
          <Icon icon="ep:upload" class="mr-5px" /> 导入
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['erp:product-unit:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="名字" align="center" prop="name" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180px"
      />
      <el-table-column label="操作" align="center">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['erp:product-unit:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['erp:product-unit:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <ProductUnitForm ref="formRef" @success="getList" />
  <Dialog
    v-model="importVisible"
    title="导入产品单位"
    width="420"
    :close-on-click-modal="!importLoading"
    :close-on-press-escape="!importLoading"
    :before-close="beforeImportClose"
  >
    <el-upload
      v-model:file-list="importFiles"
      :auto-upload="false"
      :limit="1"
      :disabled="importLoading"
      accept=".xls,.xlsx"
      :on-exceed="handleImportExceed"
      action="none"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          支持 .xls、.xlsx 文件。
          <el-button link type="primary" :loading="templateLoading" @click="downloadTemplate">
            下载导入模板
          </el-button>
        </div>
      </template>
    </el-upload>
    <template #footer>
      <el-button type="primary" :loading="importLoading" @click="handleImport">确 定</el-button>
      <el-button :disabled="importLoading" @click="importVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { ProductUnitApi, ProductUnitVO } from '@/api/erp/product/unit'
import ProductUnitForm from './ProductUnitForm.vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'

/** ERP 产品单位列表 */
defineOptions({ name: 'ErpProductUnit' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const list = ref<ProductUnitVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  status: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
const importVisible = ref(false)
const importLoading = ref(false)
const templateLoading = ref(false)
const importFiles = ref<import('element-plus').UploadUserFile[]>([])

const openImport = () => {
  importFiles.value = []
  importVisible.value = true
}

const beforeImportClose = (done: () => void) => {
  if (!importLoading.value) done()
}

const handleImportExceed = () => message.warning('每次只能导入一个 Excel 文件，请先移除已选文件')

const downloadTemplate = async () => {
  templateLoading.value = true
  try {
    const data = await ProductUnitApi.getImportTemplate()
    download.excel(data, '产品单位导入模板.xlsx')
  } finally {
    templateLoading.value = false
  }
}

const handleImport = async () => {
  if (importLoading.value) return
  const file = importFiles.value[0]?.raw
  if (!file || !/\.(xls|xlsx)$/i.test(file.name)) {
    message.warning('请选择 .xls 或 .xlsx 格式的 Excel 文件')
    return
  }
  importLoading.value = true
  try {
    const result = await ProductUnitApi.importProductUnits(file)
    message.success(`新增 ${result.importedCount} 条产品单位，跳过重复 ${result.skippedCount} 条`)
    importVisible.value = false
    importFiles.value = []
    queryParams.pageNo = 1
    await getList()
  } finally {
    importLoading.value = false
  }
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ProductUnitApi.getProductUnitPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery(true)
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await ProductUnitApi.deleteProductUnit(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  await message.exportConfirm()
  try {
    // 发起导出
    exportLoading.value = true
    const data = await ProductUnitApi.exportProductUnit(queryParams)
    download.excel(data, '产品单位.xlsx')
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
