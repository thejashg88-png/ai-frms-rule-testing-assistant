export const ACTIONS = [
  { value: 'ACCEPT',  label: 'Accept',  color: '#16a34a', bgColor: '#dcfce7' },
  { value: 'MONITOR', label: 'Monitor', color: '#ca8a04', bgColor: '#fef9c3' },
  { value: 'REJECT',  label: 'Reject',  color: '#dc2626', bgColor: '#fee2e2' },
]

export const ACTION_MAP = Object.fromEntries(ACTIONS.map((a) => [a.value, a]))

export default ACTIONS
