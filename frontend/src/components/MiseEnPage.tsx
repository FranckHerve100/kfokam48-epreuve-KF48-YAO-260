import { NavLink, Outlet } from 'react-router-dom'

const liens = [
  { vers: '/formateur', texte: 'Formateur' },
  { vers: '/etudiant', texte: 'Étudiant' },
  { vers: '/relecture', texte: 'Relecture' },
]

export function MiseEnPage() {
  return (
    <div className="application">
      <header className="barre">
        <NavLink to="/" className="marque">
          <span className="logo" aria-hidden="true">K48</span>
          <span>
            Présence <em>&amp;</em> Relecture
          </span>
        </NavLink>
        <nav aria-label="Écrans">
          {liens.map((lien) => (
            <NavLink key={lien.vers} to={lien.vers} className={({ isActive }) => (isActive ? 'actif' : undefined)}>
              {lien.texte}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="contenu">
        <Outlet />
      </main>
      <footer className="pied">Formation KFOKAM48 · KF48-YAO-260</footer>
    </div>
  )
}
