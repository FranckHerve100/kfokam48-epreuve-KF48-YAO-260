import type { Reference } from '../api/types'
import { useChargement } from '../hooks/useChargement'
import { Alerte } from './Alerte'

interface Props {
  libelle: string
  charger: (() => Promise<Reference[]>) | null
  dependances: unknown[]
  valeur: number | null
  onChange: (id: number | null) => void
  indication?: string
}

/** Liste déroulante alimentée par l'API (promotions, étudiants), avec états de chargement et d'erreur. */
export function ChoixListe({ libelle, charger, dependances, valeur, onChange, indication = 'Choisir…' }: Props) {
  const { donnees, chargement, erreur } = useChargement(charger, dependances)
  return (
    <label className="champ">
      <span>{libelle}</span>
      <select
        value={valeur ?? ''}
        disabled={!charger || chargement}
        onChange={(e) => onChange(e.target.value ? Number(e.target.value) : null)}
      >
        <option value="">{chargement ? 'Chargement…' : indication}</option>
        {donnees?.map((element) => (
          <option key={element.id} value={element.id}>
            {element.nom}
          </option>
        ))}
      </select>
      {erreur && <Alerte genre="erreur">{erreur}</Alerte>}
    </label>
  )
}
