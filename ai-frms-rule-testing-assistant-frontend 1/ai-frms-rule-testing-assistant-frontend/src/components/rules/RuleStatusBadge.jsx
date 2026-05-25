import React from 'react'
import Badge from '../common/Badge'
import { STATUSES } from '../../data/statuses'

const RuleStatusBadge = ({ status, size = 'sm' }) => {
  const s = STATUSES.RULE_STATUS[status] ?? STATUSES.RULE_STATUS.DRAFT
  return (
    <Badge bgColor={s.bgColor} color={s.color} size={size}>
      {s.label}
    </Badge>
  )
}

export default RuleStatusBadge
