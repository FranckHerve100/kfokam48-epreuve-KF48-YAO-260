import type { ReactNode } from 'react'

export function Carte({ titre, sousTitre, children }: { titre: string; sousTitre?: string; children: ReactNode }) {
  return (
    <section className="carte">
      <header className="carte-entete">
        <h2>{titre}</h2>
        {sousTitre && <p>{sousTitre}</p>}
      </header>
      {children}
    </section>
  )
}
