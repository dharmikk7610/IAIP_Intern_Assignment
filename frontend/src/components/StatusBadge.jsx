import { STATUS_OPTIONS, labelOf } from '../utils/format'

export default function StatusBadge({ status }) {
  return <span className={`badge badge-${status?.toLowerCase()}`}>{labelOf(STATUS_OPTIONS, status)}</span>
}
