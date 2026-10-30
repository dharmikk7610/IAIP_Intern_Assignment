import { createContext, useContext } from 'react'

export const ToastContext = createContext(null)

/** toast.success(message), toast.error(message) */
export function useToast() {
  return useContext(ToastContext)
}
