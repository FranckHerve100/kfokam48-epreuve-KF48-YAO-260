import { useState, type FormEvent } from 'react'
import { api, messageErreur } from '../api/client'
import type { StatutExercice } from '../api/types'
import { Alerte } from '../components/Alerte'
import { Carte } from '../components/Carte'
import { ChoixListe } from '../components/ChoixListe'
import { useChargement } from '../hooks/useChargement'
import { formatDateHeure } from '../utils/format'

type Retour = { genre: 'succes' | 'erreur'; texte: string } | null

const libelleStatut: Record<StatutExercice, string> = {
  DEPOSE: 'déposé — un relecteur sera tiré dès qu’un autre étudiant sera présent',
  EN_ATTENTE_RELECTURE: 'en attente de relecture — un pair a été tiré au sort',
  RELU: 'relu',
}

/** Écran étudiant : se choisir dans la liste (Q1), marquer sa présence (EF2), déposer son exercice (EF3). */
export function Etudiant() {
  const [promotionId, setPromotionId] = useState<number | null>(null)
  const [etudiantId, setEtudiantId] = useState<number | null>(null)

  function choisirPromotion(id: number | null) {
    setPromotionId(id)
    setEtudiantId(null)
  }

  return (
    <>
      <h1>Espace étudiant</h1>
      <div className="grille">
        <Carte titre="Qui êtes-vous ?" sousTitre="Choisissez votre promotion puis votre nom.">
          <form onSubmit={(e) => e.preventDefault()}>
            <ChoixListe libelle="Promotion" charger={api.promotions} dependances={[]} valeur={promotionId} onChange={choisirPromotion} />
            <ChoixListe
              libelle="Votre nom"
              charger={promotionId ? () => api.etudiants(promotionId) : null}
              dependances={[promotionId]}
              valeur={etudiantId}
              onChange={setEtudiantId}
              indication={promotionId ? 'Choisir votre nom…' : 'Choisissez d’abord une promotion'}
            />
          </form>
        </Carte>
        <Pointage etudiantId={etudiantId} />
        {promotionId && etudiantId && <Depot promotionId={promotionId} etudiantId={etudiantId} />}
      </div>
    </>
  )
}

function Pointage({ etudiantId }: { etudiantId: number | null }) {
  const [code, setCode] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [retour, setRetour] = useState<Retour>(null)

  async function pointer(evenement: FormEvent) {
    evenement.preventDefault()
    if (!etudiantId) return
    setEnvoi(true)
    setRetour(null)
    try {
      await api.marquerPresence(code, etudiantId)
      setRetour({ genre: 'succes', texte: 'Présence enregistrée. Bon cours !' })
      setCode('')
    } catch (e) {
      setRetour({ genre: 'erreur', texte: messageErreur(e) })
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <Carte titre="Marquer ma présence" sousTitre="Saisissez le code projeté par le formateur.">
      <form onSubmit={pointer}>
        <label className="champ">
          <span>Code de la session</span>
          <input
            className="code-saisie"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            maxLength={6}
            autoComplete="off"
            autoCapitalize="characters"
            inputMode="text"
            placeholder="······"
            required
          />
        </label>
        <button type="submit" disabled={!etudiantId || code.trim().length === 0 || envoi}>
          {envoi ? 'Envoi…' : 'Je suis présent'}
        </button>
        {!etudiantId && <p className="vide">Choisissez d’abord votre nom.</p>}
        {retour && <Alerte genre={retour.genre}>{retour.texte}</Alerte>}
      </form>
    </Carte>
  )
}

function Depot({ promotionId, etudiantId }: { promotionId: number; etudiantId: number }) {
  const sessions = useChargement(() => api.sessions(promotionId), [promotionId])
  const [sessionId, setSessionId] = useState<number | null>(null)
  const [lien, setLien] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [retour, setRetour] = useState<Retour>(null)
  const ouvertes = sessions.donnees?.filter((session) => !session.clotureAt) ?? []

  async function deposer(evenement: FormEvent) {
    evenement.preventDefault()
    if (!sessionId) return
    setEnvoi(true)
    setRetour(null)
    try {
      const exercice = await api.deposerExercice(sessionId, etudiantId, lien.trim())
      setRetour({ genre: 'succes', texte: `Exercice ${libelleStatut[exercice.statut]}.` })
      setLien('')
    } catch (e) {
      setRetour({ genre: 'erreur', texte: messageErreur(e) })
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <Carte titre="Déposer mon exercice" sousTitre="Possible jusqu’à la clôture de la session, même après l’expiration du code.">
      <form onSubmit={deposer}>
        <label className="champ">
          <span>Session</span>
          <select value={sessionId ?? ''} onChange={(e) => setSessionId(e.target.value ? Number(e.target.value) : null)} disabled={sessions.chargement}>
            <option value="">{sessions.chargement ? 'Chargement…' : 'Choisir la session…'}</option>
            {ouvertes.map((session) => (
              <option key={session.id} value={session.id}>
                {session.titre} — {formatDateHeure(session.ouvertureAt)}
              </option>
            ))}
          </select>
        </label>
        {sessions.erreur && <Alerte genre="erreur">{sessions.erreur}</Alerte>}
        <label className="champ">
          <span>Lien de l’exercice</span>
          <input type="text" inputMode="url" value={lien} onChange={(e) => setLien(e.target.value)} placeholder="https://github.com/…" required />
        </label>
        <button type="submit" disabled={!sessionId || !lien.trim() || envoi}>
          {envoi ? 'Dépôt…' : 'Déposer'}
        </button>
        {retour && <Alerte genre={retour.genre}>{retour.texte}</Alerte>}
      </form>
    </Carte>
  )
}
