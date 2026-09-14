const { assertApprovalCenterStandardListContract } = require('./approval-center-standard-list-template-contract-helper')

assertApprovalCenterStandardListContract({
  routePath: 'todo',
  routeName: 'ApprovalCenterTodo',
  routeTitle: '待办',
  viewType: 'TODO',
  tableKey: 'approval.center.todo.applicant.v1',
  expectsReviewAction: true
})

console.log('PASS: approval center TODO standard list template static contract')
