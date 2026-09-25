import { useCallback, useEffect, useState } from 'react'
import { messageErreur } from '../api/client'

/** États d'un appel à l'API : chargement, erreur affichable, données, relance (F3). */
export function useChargement<T>(charger: (() => Promise<T>) | null, dependances: unknown[]) {
  const [donnees, setDonnees] = useState<T | null>(null)
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!charger) {
      setDonnees(null)
      return
    }
    let actif = true
    setChargement(true)
    setErreur(null)
    charger()
      .then((resultat) => actif && setDonnees(resultat))
      .catch((e) => actif && setErreur(messageErreur(e)))
      .finally(() => actif && setChargement(false))
    return () => {
      actif = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependances, version])

  const recharger = useCallback(() => setVersion((v) => v + 1), [])
  return { donnees, chargement, erreur, recharger }
}
