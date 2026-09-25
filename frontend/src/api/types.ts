// Types des réponses de l'API (api/contrat.yaml)

export interface Reference {
  id: number
  nom: string
}

export interface Session {
  id: number
  titre: string
  code: string
  ouvertureAt: string
  expirationAt: string
  clotureAt: string | null
}

export interface SessionOuverte {
  id: number
  code: string
  ouvertureAt: string
  expirationAt: string
}

export interface Presence {
  id: number
  sessionId: number
  etudiantId: number
  source: 'ETUDIANT' | 'FORMATEUR'
}

export type StatutExercice = 'DEPOSE' | 'EN_ATTENTE_RELECTURE' | 'PARTIELLEMENT_RELU' | 'RELU'

export interface ExerciceDepose {
  id: number
  statut: StatutExercice
}

export interface RelectureAssignee {
  id: number
  exerciceId: number
  lien: string
  statut: 'EN_ATTENTE' | 'RENDUE'
}

export interface LigneTableau {
  etudiantId: number
  nom: string
  presences: number
  exercicesDeposes: number
  moyenne: number | null
  relecturesEnAttente: number
  /** Vrai si la moyenne compte une note qui ne repose encore que sur une relecture sur deux (RG15, H12) */
  moyenneProvisoire?: boolean
}
