// Mise en forme d'affichage uniquement : aucune règle métier (F3).

const heure = new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' })
const dateHeure = new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })
const nombre = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export const formatHeure = (iso: string) => heure.format(new Date(iso))
export const formatDateHeure = (iso: string) => dateHeure.format(new Date(iso))

/** Moyenne calculée par l'API (RG15) ; « — » quand elle vaut null (H10). */
export const formatMoyenne = (moyenne: number | null) => (moyenne === null ? '—' : nombre.format(moyenne))
