import type { ReactNode } from 'react'

type Genre = 'succes' | 'erreur' | 'info'

export function Alerte({ genre, children }: { genre: Genre; children: ReactNode }) {
  return (
    <div className={`alerte alerte-${genre}`} role={genre === 'erreur' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}
