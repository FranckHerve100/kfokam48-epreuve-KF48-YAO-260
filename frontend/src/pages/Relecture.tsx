import { useState, type FormEvent } from 'react'
import { api, messageErreur } from '../api/client'
import type { RelectureAssignee } from '../api/types'
import { Alerte } from '../components/Alerte'
import { Carte } from '../components/Carte'
import { Chargement } from '../components/Chargement'
import { ChoixListe } from '../components/ChoixListe'
import { useChargement } from '../hooks/useChargement'

/** Écran relecteur : l'étudiant voit les exercices qui lui sont assignés et rend une note (EF5). */
export function Relecture() {
  const [promotionId, setPromotionId] = useState<number | null>(null)
  const [etudiantId, setEtudiantId] = useState<number | null>(null)

  return (
    <>
      <h1>Espace relecteur</h1>
      <div className="grille">
        <Carte titre="Qui êtes-vous ?" sousTitre="Les relectures vous sont assignées par tirage au sort.">
          <form onSubmit={(e) => e.preventDefault()}>
            <ChoixListe
              libelle="Promotion"
              charger={api.promotions}
              dependances={[]}
              valeur={promotionId}
              onChange={(id) => {
                setPromotionId(id)
                setEtudiantId(null)
              }}
            />
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
        {etudiantId && <MesRelectures key={etudiantId} etudiantId={etudiantId} />}
      </div>
    </>
  )
}

function MesRelectures({ etudiantId }: { etudiantId: number }) {
  const { donnees, chargement, erreur, recharger } = useChargement(() => api.relecturesDe(etudiantId), [etudiantId])
  const [choisie, setChoisie] = useState<RelectureAssignee | null>(null)
  const [succes, setSucces] = useState<string | null>(null)

  return (
    <>
      <Carte titre="Mes relectures" sousTitre="En attente d’abord. Ouvrez le lien, puis notez l’exercice.">
        {chargement && !donnees && <Chargement />}
        {erreur && <Alerte genre="erreur">{erreur}</Alerte>}
        {succes && <Alerte genre="succes">{succes}</Alerte>}
        {donnees && donnees.length === 0 && <p className="vide">Aucune relecture ne vous est assignée pour l’instant.</p>}
        {donnees && donnees.length > 0 && (
          <ul className="liste" style={{ marginTop: succes ? '0.75rem' : 0 }}>
            {donnees.map((relecture) => (
              <li key={relecture.id} className={choisie?.id === relecture.id ? 'selectionne' : undefined}>
                <span className="lien-exercice">
                  <strong>Exercice n° {relecture.exerciceId}</strong>
                  <br />
                  <a href={relecture.lien} target="_blank" rel="noreferrer">
                    {relecture.lien}
                  </a>
                </span>
                {relecture.statut === 'EN_ATTENTE' ? (
                  <button type="button" className="secondaire" onClick={() => { setChoisie(relecture); setSucces(null) }}>
                    Noter
                  </button>
                ) : (
                  <span className="pastille pastille-ok">Rendue</span>
                )}
              </li>
            ))}
          </ul>
        )}
      </Carte>
      {choisie && (
        <FormulaireNote
          relecture={choisie}
          etudiantId={etudiantId}
          onRendue={() => {
            setSucces(`Relecture de l’exercice n° ${choisie.exerciceId} enregistrée. La note est définitive.`)
            setChoisie(null)
            recharger()
          }}
        />
      )}
    </>
  )
}

function FormulaireNote({ relecture, etudiantId, onRendue }: { relecture: RelectureAssignee; etudiantId: number; onRendue: () => void }) {
  const [note, setNote] = useState('')
  const [commentaire, setCommentaire] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)

  async function envoyer(evenement: FormEvent) {
    evenement.preventDefault()
    setEnvoi(true)
    setErreur(null)
    try {
      await api.rendreRelecture(relecture.id, Number(note), commentaire, etudiantId)
      onRendue()
    } catch (e) {
      setErreur(messageErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <Carte titre={`Noter l’exercice n° ${relecture.exerciceId}`} sousTitre="Note entière sur 20. Une fois envoyée, elle ne peut plus être modifiée.">
      <form onSubmit={envoyer}>
        <label className="champ">
          <span>Note sur 20</span>
          <input inputMode="numeric" value={note} onChange={(e) => setNote(e.target.value)} placeholder="Ex. : 14" required />
        </label>
        <label className="champ">
          <span>Commentaire</span>
          <textarea value={commentaire} onChange={(e) => setCommentaire(e.target.value)} maxLength={2000} placeholder="Points forts, points à améliorer…" required />
        </label>
        <button type="submit" disabled={!note.trim() || !commentaire.trim() || envoi}>
          {envoi ? 'Envoi…' : 'Envoyer la relecture'}
        </button>
        {erreur && <Alerte genre="erreur">{erreur}</Alerte>}
      </form>
    </Carte>
  )
}
