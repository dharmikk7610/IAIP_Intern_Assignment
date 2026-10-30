import { Children, cloneElement, isValidElement } from 'react'

/**
 * Label + input + hint/error. Wires id, aria-invalid and aria-describedby onto the child
 * input so screen readers announce the error with the field.
 */
export default function FormField({ label, name, error, hint, required, children, className = '' }) {
  const id = `field-${name}`
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined
  const control = Children.only(children)

  return (
    <div className={`field ${error ? 'field-invalid' : ''} ${className}`}>
      <label htmlFor={id}>
        {label}
        {required && <span className="required" aria-hidden="true"> *</span>}
      </label>
      {isValidElement(control)
        ? cloneElement(control, { id, name, 'aria-invalid': Boolean(error), 'aria-describedby': describedBy })
        : control}
      {error ? (
        <p id={`${id}-error`} className="field-error">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-hint`} className="field-hint">
            {hint}
          </p>
        )
      )}
    </div>
  )
}
