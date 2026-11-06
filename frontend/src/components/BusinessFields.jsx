import React from 'react';
import { neighborhoods } from '@/pages/NeighborhoodNews';
import { Field, TextField, inputClass } from '@/components/FormParts';

export const EMPTY_BUSINESS = {
  business_name: '', legal_name: '', owner_name: '', vat_number: '', tax_code: '',
  legal_address: '', postal_code: '', city: 'Bari', province: 'BA', pec: '', email: '', phone: '',
  category: '', quartiere: '', address: '', description: '', slogan: '', promotion: '', maps_url: '', cover_image: '', entity_type: '',
};

/** Campi dell'attività: tutti obbligatori tranne forma giuridica, slogan, promozione, mappa e foto. */
export default function BusinessFields({ value, onChange }) {
  const set = (name, v) => onChange({ ...value, [name]: v });
  return (
    <>
      <fieldset className="mb-6">
        <legend className="font-serif-display text-xl font-bold text-[#0B2545] mb-3">Dati legali</legend>
        <TextField label="Ragione sociale" name="legal_name" value={value.legal_name} onChange={set} maxLength={200} autoComplete="organization" />
        <TextField label="Partita IVA" name="vat_number" value={value.vat_number} onChange={set} inputMode="numeric" maxLength={11} pattern="\d{11}" hint="11 cifre, senza prefisso IT." />
        <TextField label="Codice fiscale" name="tax_code" value={value.tax_code} onChange={set} maxLength={16} hint="11 cifre per le società, 16 caratteri per ditte individuali e professionisti." />
        <TextField label="Forma giuridica" name="entity_type" value={value.entity_type} onChange={set} required={false} maxLength={100} hint="Es. S.r.l., S.n.c., ditta individuale." />
        <TextField label="Nome e cognome del titolare o legale rappresentante" name="owner_name" value={value.owner_name} onChange={set} maxLength={120} />
        <TextField label="Indirizzo sede legale" name="legal_address" value={value.legal_address} onChange={set} maxLength={200} autoComplete="street-address" />
        <div className="grid grid-cols-2 gap-3">
          <TextField label="CAP" name="postal_code" value={value.postal_code} onChange={set} inputMode="numeric" maxLength={5} pattern="\d{5}" autoComplete="postal-code" />
          <TextField label="Provincia" name="province" value={value.province} onChange={set} maxLength={2} hint="Sigla, es. BA." />
        </div>
        <TextField label="Comune" name="city" value={value.city} onChange={set} maxLength={100} />
        <TextField label="PEC" name="pec" value={value.pec} onChange={set} type="email" inputMode="email" maxLength={254} />
      </fieldset>

      <fieldset className="mb-6">
        <legend className="font-serif-display text-xl font-bold text-[#0B2545] mb-3">L’attività</legend>
        <TextField label="Nome dell’attività" name="business_name" value={value.business_name} onChange={set} maxLength={160} hint="Il nome con cui è conosciuta, come comparirà sul sito." />
        <TextField label="Categoria" name="category" value={value.category} onChange={set} maxLength={100} hint="Es. Bar e caffetteria, Alimentari, Parrucchiere." />
        <Field label="Quartiere">
          <select required value={value.quartiere} onChange={e => set('quartiere', e.target.value)} className={inputClass}>
            <option value="" disabled>Seleziona il quartiere</option>
            {neighborhoods.filter(n => n !== 'Tutta Bari').map(n => <option key={n} value={n}>{n}</option>)}
          </select>
        </Field>
        <TextField label="Indirizzo dell’attività" name="address" value={value.address} onChange={set} maxLength={200} hint="Dove ricevi i clienti." />
        <Field label="Descrizione" hint="Almeno 20 caratteri.">
          <textarea required minLength={20} maxLength={3000} rows={5} value={value.description} onChange={e => set('description', e.target.value)} className={inputClass} />
        </Field>
        <TextField label="Slogan" name="slogan" value={value.slogan} onChange={set} required={false} maxLength={200} />
        <TextField label="Promozione" name="promotion" value={value.promotion} onChange={set} required={false} maxLength={500} />
      </fieldset>

      <fieldset className="mb-2">
        <legend className="font-serif-display text-xl font-bold text-[#0B2545] mb-3">Contatti pubblici</legend>
        <TextField label="Email di contatto" name="email" value={value.email} onChange={set} type="email" inputMode="email" maxLength={254} />
        <TextField label="Telefono" name="phone" value={value.phone} onChange={set} type="tel" inputMode="tel" maxLength={25} autoComplete="tel" />
        <TextField label="Collegamento Google Maps" name="maps_url" value={value.maps_url} onChange={set} required={false} type="url" inputMode="url" maxLength={500} />
        <TextField label="Foto (collegamento https)" name="cover_image" value={value.cover_image} onChange={set} required={false} type="url" inputMode="url" maxLength={500} hint="Indirizzo di un’immagine già online. Dopo l’approvazione la redazione può caricare le foto." />
      </fieldset>
    </>
  );
}
