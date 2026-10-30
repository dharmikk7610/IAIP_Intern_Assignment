import { useCallback, useMemo, useRef, useState } from 'react'
import { ToastContext } from './toastContext'

const DISMISS_AFTER_MS = 4500

/** Small notification system: messages stack in the corner and disappear on their own. */
export default function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const nextId = useRef(1)

  const dismiss = useCallback((id) => setToasts((list) => list.filter((t) => t.id !== id)), [])

  const show = useCallback(
    (type, message) => {
      const id = nextId.current++
      // A success makes earlier error messages obsolete
      setToasts((list) => [...(type === 'success' ? list.filter((t) => t.type !== 'error') : list), { id, type, message }])
      setTimeout(() => dismiss(id), DISMISS_AFTER_MS)
    },
    [dismiss],
  )

  const api = useMemo(
    () => ({ success: (m) => show('success', m), error: (m) => show('error', m) }),
    [show],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toast-stack" role="status" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.type}`}>
            <span>{t.message}</span>
            <button type="button" className="toast-close" onClick={() => dismiss(t.id)} aria-label="Dismiss">
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
