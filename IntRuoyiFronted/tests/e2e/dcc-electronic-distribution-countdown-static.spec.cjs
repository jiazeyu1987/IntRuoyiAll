const fs = require('fs')
const path = require('path')
const assert = require('assert')

const sourcePath = path.join(
  process.cwd(),
  'src/views/dcc/controlled-file/detail/index.vue'
)
const source = fs.readFileSync(sourcePath, 'utf8')

const dialogState = source.slice(
  source.indexOf('const electronicReceiptDialog = reactive({'),
  source.indexOf('const distributionSignDialog = reactive({')
)
assert(
  dialogState.includes('confirmCountdownSeconds: 0'),
  'electronic receipt dialog state must store the confirm countdown seconds'
)

const openDialog = source.slice(
  source.indexOf('const openElectronicReceiptDialog = (distribution: ControlledFileDistributionStatusVO) => {'),
  source.indexOf('const closeElectronicReceiptDialog = () => {')
)
assert(
  openDialog.includes('startElectronicReceiptConfirmCountdown()'),
  'opening electronic receipt dialog must start the countdown'
)

const submitButton = source.slice(
  source.indexOf(':disabled="isElectronicReceiptConfirmDisabled"'),
  source.indexOf('@click="submitElectronicReceiptDialog"') + 80
)
assert(
  submitButton.includes(':disabled="isElectronicReceiptConfirmDisabled"'),
  'electronic receipt confirm button must stay disabled while countdown is active'
)

assert(
  source.includes('const ELECTRONIC_RECEIPT_CONFIRM_COUNTDOWN_SECONDS = 10'),
  'electronic receipt countdown must be 10 seconds'
)
assert(
  source.includes('确认签收（{{ electronicReceiptDialog.confirmCountdownSeconds }}s）'),
  'button text must show the remaining seconds while waiting'
)
assert(
  source.includes('window.setInterval') &&
    source.includes('clearElectronicReceiptConfirmCountdownTimer'),
  'countdown timer must be explicitly started and cleared'
)
