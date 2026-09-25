export function Chargement({ texte = 'Chargement…' }: { texte?: string }) {
  return (
    <p className="chargement" aria-live="polite">
      <span className="rond" aria-hidden="true" />
      {texte}
    </p>
  )
}
