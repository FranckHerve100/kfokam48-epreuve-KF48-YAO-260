import { useState, type FormEvent } from 'react'
import { api, messageErreur } from '../api/client'
import type { Session, SessionOuverte } from '../api/types'
import { Alerte } from '../components/Alerte'
import { Carte } from '../components/Carte'
import { Chargement } from '../components/Chargement'
import { ChoixListe } from '../components/ChoixListe'
import { useChargement } from '../hooks/useChargement'
import { formatDateHeure, formatHeure, formatMoyenne } from '../utils/format'

/** Écran formateur : ouvrir une session (EF1), suivre les sessions et le tableau de la promotion (EF6). */
export function Formateur() {
  const [promotionId, setPromotionId] = useState<number | null>(null)
  const [actualisation, setActualisation] = useState(0)

  return (
    <>
      <h1>Espace formateur</h1>
      <div className="grille">
        <Carte titre="Promotion" sousTitre="Choisissez la promotion que vous encadrez.">
          <ChoixListe libelle="Promotion" charger={api.promotions} dependances={[]} valeur={promotionId} onChange={setPromotionId} />
        </Carte>
        <OuvertureSession promotionId={promotionId} onOuverte={() => setActualisation((n) => n + 1)} />
        {promotionId && (
          <>
            <Sessions promotionId={promotionId} actualisation={actualisation} />
            <Tableau promotionId={promotionId} actualisation={actualisation} />
          </>
        )}
      </div>
    </>
  )
}

function OuvertureSession({ promotionId, onOuverte }: { promotionId: number | null; onOuverte: () => void }) {
  const [titre, setTitre] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)
  const [session, setSession] = useState<SessionOuverte | null>(null)

  async function ouvrir(evenement: FormEvent) {
    evenement.preventDefault()
    if (!promotionId) return
    setEnvoi(true)
    setErreur(null)
    try {
      setSession(await api.ouvrirSession(titre.trim(), promotionId))
      setTitre('')
      onOuverte()
    } catch (e) {
      setErreur(messageErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <Carte titre="Ouvrir une session" sousTitre="Le code est valable 15 minutes : projetez-le à la classe.">
      <form onSubmit={ouvrir}>
        <label className="champ">
          <span>Titre du cours</span>
          <input value={titre} onChange={(e) => setTitre(e.target.value)} placeholder="Ex. : Spring Boot, les tests" maxLength={200} required />
        </label>
        <button type="submit" disabled={!promotionId || !titre.trim() || envoi}>
          {envoi ? 'Ouverture…' : 'Ouvrir la session'}
        </button>
        {!promotionId && <p className="vide">Choisissez d'abord une promotion.</p>}
        {erreur && <Alerte genre="erreur">{erreur}</Alerte>}
      </form>
      {session && (
        <div className="code-projete" style={{ marginTop: '1rem' }} aria-live="polite">
          <p>Code de présence</p>
          <div className="code">{session.code}</div>
          <p>
            Valable jusqu'à <strong>{formatHeure(session.expirationAt)}</strong>
          </p>
        </div>
      )}
    </Carte>
  )
}

function statutSession(session: Session) {
  if (session.clotureAt) return <span className="pastille pastille-neutre">Clôturée</span>
  if (new Date(session.expirationAt) < new Date()) return <span className="pastille pastille-attente">Code expiré</span>
  return <span className="pastille pastille-ok">Code actif</span>
}

function Sessions({ promotionId, actualisation }: { promotionId: number; actualisation: number }) {
  const { donnees, chargement, erreur } = useChargement(() => api.sessions(promotionId), [promotionId, actualisation])
  return (
    <Carte titre="Sessions de la promotion" sousTitre="La plus récente en premier.">
      {chargement && <Chargement />}
      {erreur && <Alerte genre="erreur">{erreur}</Alerte>}
      {donnees && donnees.length === 0 && <p className="vide">Aucune session pour l'instant.</p>}
      {donnees && donnees.length > 0 && (
        <ul className="liste">
          {donnees.map((session) => (
            <li key={session.id}>
              <span>
                <strong>{session.titre}</strong>
                <br />
                <small>
                  {formatDateHeure(session.ouvertureAt)} · code <code>{session.code}</code>
                </small>
              </span>
              {statutSession(session)}
            </li>
          ))}
        </ul>
      )}
    </Carte>
  )
}

function Tableau({ promotionId, actualisation }: { promotionId: number; actualisation: number }) {
  const { donnees, chargement, erreur, recharger } = useChargement(() => api.tableau(promotionId), [promotionId, actualisation])
  return (
    <div className="pleine-largeur">
      <Carte
        titre="Tableau de la promotion"
        sousTitre="Moyenne des notes retenues, calculée par l'API, sur 20. Chaque exercice est noté par deux pairs ; une note encore seule est provisoire."
      >
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '0.75rem' }}>
          <button type="button" className="secondaire" onClick={recharger} disabled={chargement}>
            Actualiser
          </button>
        </div>
        {chargement && !donnees && <Chargement />}
        {erreur && <Alerte genre="erreur">{erreur}</Alerte>}
        {donnees && donnees.length === 0 && <p className="vide">Aucun étudiant dans cette promotion.</p>}
        {donnees && donnees.length > 0 && (
          <div className="table-defilante">
            <table>
              <thead>
                <tr>
                  <th>Étudiant</th>
                  <th className="nombre">Présences</th>
                  <th className="nombre">Exercices déposés</th>
                  <th className="nombre">Moyenne</th>
                  <th className="nombre">Relectures dues</th>
                </tr>
              </thead>
              <tbody>
                {donnees.map((ligne) => (
                  <tr key={ligne.etudiantId}>
                    <td>{ligne.nom}</td>
                    <td className="nombre">{ligne.presences}</td>
                    <td className="nombre">{ligne.exercicesDeposes}</td>
                    <td className="nombre">
                      {formatMoyenne(ligne.moyenne)}
                      {ligne.moyenneProvisoire && (
                        <span className="pastille pastille-attente provisoire" title="Une note ne repose encore que sur une relecture sur deux">
                          provisoire
                        </span>
                      )}
                    </td>
                    <td className="nombre">
                      {ligne.relecturesEnAttente > 0 ? (
                        <span className="pastille pastille-attente">{ligne.relecturesEnAttente} en attente</span>
                      ) : (
                        <span className="pastille pastille-ok">0</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Carte>
    </div>
  )
}
