// Seul point d'appel HTTP du frontend (F3) : aucun fetch ailleurs.
import type {
  ExerciceDepose,
  LigneTableau,
  Presence,
  Reference,
  RelectureAssignee,
  Session,
  SessionOuverte,
} from './types'

/** Erreur de l'API au format { code, message } du contrat, prête à être affichée. */
export class ErreurApi extends Error {
  readonly code: string
  readonly statut: number

  constructor(code: string, message: string, statut: number) {
    super(message)
    this.name = 'ErreurApi'
    this.code = code
    this.statut = statut
  }
}

async function requete<T>(methode: string, chemin: string, corps?: unknown, entetes: Record<string, string> = {}): Promise<T> {
  let reponse: Response
  try {
    reponse = await fetch(chemin, {
      method: methode,
      headers: corps === undefined ? entetes : { 'Content-Type': 'application/json', ...entetes },
      body: corps === undefined ? undefined : JSON.stringify(corps),
    })
  } catch {
    throw new ErreurApi('RESEAU', 'Le serveur est injoignable. Vérifiez votre connexion puis réessayez.', 0)
  }
  const texte = await reponse.text()
  const donnees = texte ? JSON.parse(texte) : undefined
  if (!reponse.ok) {
    const erreur = donnees as { code?: string; message?: string } | undefined
    throw new ErreurApi(
      erreur?.code ?? 'ERREUR_INCONNUE',
      erreur?.message ?? `Erreur inattendue (${reponse.status}).`,
      reponse.status,
    )
  }
  return donnees as T
}

export const api = {
  promotions: () => requete<Reference[]>('GET', '/api/promotions'),
  etudiants: (promotionId: number) => requete<Reference[]>('GET', `/api/promotions/${promotionId}/etudiants`),
  sessions: (promotionId: number) => requete<Session[]>('GET', `/api/promotions/${promotionId}/sessions`),

  ouvrirSession: (titre: string, promotionId: number) =>
    requete<SessionOuverte>('POST', '/api/sessions', { titre, promotionId }),
  marquerPresence: (code: string, etudiantId: number) =>
    requete<Presence>('POST', '/api/presences', { code, etudiantId }),
  deposerExercice: (sessionId: number, etudiantId: number, lien: string) =>
    requete<ExerciceDepose>('POST', '/api/exercices', { sessionId, etudiantId, lien }),

  relecturesDe: (etudiantId: number) => requete<RelectureAssignee[]>('GET', `/api/etudiants/${etudiantId}/relectures`),
  rendreRelecture: (relectureId: number, note: number, commentaire: string, etudiantId: number) =>
    requete<void>('POST', `/api/relectures/${relectureId}`, { note, commentaire }, { 'X-Etudiant-Id': String(etudiantId) }),

  tableau: (promotionId: number) => requete<LigneTableau[]>('GET', `/api/tableau?promotionId=${promotionId}`),
}

/** Message affichable pour toute erreur, qu'elle vienne de l'API ou d'ailleurs. */
export function messageErreur(erreur: unknown): string {
  return erreur instanceof Error ? erreur.message : 'Une erreur inattendue est survenue.'
}
