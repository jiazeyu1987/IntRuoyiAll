import { h, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import SignatureCredentialInput from '@/components/SignatureCredentialInput/SignatureCredentialInput.vue'

export const promptSignatureCredential = async (title: string) => {
  const value = ref('')
  await ElMessageBox({
    title,
    message: () => h(SignatureCredentialInput, {
      modelValue: value.value,
      'onUpdate:modelValue': (credential: string) => { value.value = credential }
    }),
    showCancelButton: true,
    confirmButtonText: '确认签名',
    cancelButtonText: '取消',
    closeOnClickModal: false,
    beforeClose: (action, instance, done) => {
      if (action === 'confirm' && !value.value) {
        instance.confirmButtonText = '请先完整输入签名凭据'
        return
      }
      done()
    }
  })
  return { value: value.value }
}
