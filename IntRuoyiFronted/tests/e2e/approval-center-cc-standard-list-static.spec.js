const { assertApprovalCenterStandardListContract } = require('./approval-center-standard-list-template-contract-helper')

assertApprovalCenterStandardListContract({
  routePath: 'cc',
  routeName: 'ApprovalCenterCc',
  routeTitle: '抄送我的',
  viewType: 'CC',
  tableKey: 'approval.center.cc.applicant.v1'
})

console.log('PASS: approval center CC standard list template static contract')
