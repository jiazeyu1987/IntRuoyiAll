const { assertApprovalCenterStandardListContract } = require('./approval-center-standard-list-template-contract-helper')

assertApprovalCenterStandardListContract({
  routePath: 'done',
  routeName: 'ApprovalCenterDone',
  routeTitle: '已办',
  viewType: 'DONE',
  tableKey: 'approval.center.done.applicant.v1'
})

console.log('PASS: approval center DONE standard list template static contract')
