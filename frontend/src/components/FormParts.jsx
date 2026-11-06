import React from 'react';

/** Elementi di modulo condivisi: etichetta sempre visibile, campi da 44 px, errori accessibili. */
export const inputClass = 'block w-full mt-1 min-h-11 px-3 py-2 border border-[#CFC6B4] rounded-md bg-white text-base text-[#0B2545] focus-visible:outline-2 focus-visible:outline-[#0B2545]';
export const buttonClass = 'inline-flex items-center justify-center min-h-12 px-5 rounded-md bg-[#0B2545] text-white font-ui font-semibold disabled:opacity-50';
export const linkClass = 'underline text-[#0B2545] inline-flex items-center min-h-11';

export function Field({ label, hint, children }) {
  return (
    <label className="block font-ui text-sm text-[#0B2545] mb-3">
      <span className="font-semibold">{label}</span>
      {children}
      {hint && <span className="block text-xs text-slate-600 mt-1">{hint}</span>}
    </label>
  );
}

export function TextField({ label, hint, name, value, onChange, required = true, ...rest }) {
  return (
    <Field label={label + (required ? '' : ' (facoltativo)')} hint={hint}>
      <input name={name} value={value} required={required} onChange={e => onChange(name, e.target.value)} className={inputClass} {...rest} />
    </Field>
  );
}

export function Alert({ children }) {
  return children ? <p role="alert" className="p-3 my-3 rounded-md bg-red-50 text-red-800 text-sm">{children}</p> : null;
}

export function Notice({ children }) {
  return children ? <p role="status" className="p-3 my-3 rounded-md bg-green-50 text-green-900 text-sm">{children}</p> : null;
}

/** Contenitore centrato per le pagine di accesso e registrazione. */
export function Page({ title, subtitle, wide = false, children }) {
  return (
    <div className={`${wide ? 'max-w-3xl' : 'max-w-md'} mx-auto px-4 py-6 sm:py-8`}>
      <h1 className="font-serif-display text-3xl font-bold text-[#0B2545]">{title}</h1>
      {subtitle && <p className="mt-2 text-slate-700 font-body">{subtitle}</p>}
      <div className="mt-6">{children}</div>
    </div>
  );
}

/** Messaggio d'errore del server (campo `detail`) o testo di riserva. */
export const errorText = (e, fallback = 'Operazione non riuscita. Riprova.') => e?.response?.data?.detail || fallback;
