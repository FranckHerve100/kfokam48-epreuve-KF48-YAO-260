import { Link } from 'react-router-dom'

const ecrans = [
  { vers: '/formateur', titre: 'Formateur', texte: 'Ouvrir une session, projeter le code de présence et suivre le tableau de la promotion.' },
  { vers: '/etudiant', titre: 'Étudiant', texte: 'Pointer avec le code de la session et déposer le lien de son exercice.' },
  { vers: '/relecture', titre: 'Relecture', texte: 'Noter et commenter l’exercice d’un pair qui vous a été assigné.' },
]

export function Accueil() {
  return (
    <>
      <section className="introduction">
        <h1>Présence et relecture entre pairs</h1>
        <p>Choisissez votre écran. Aucun mot de passe : vous vous choisissez dans la liste de votre promotion.</p>
      </section>
      <div className="grille-ecrans">
        {ecrans.map((ecran) => (
          <Link key={ecran.vers} to={ecran.vers} className="tuile">
            <h2>{ecran.titre}</h2>
            <p>{ecran.texte}</p>
            <span className="fleche" aria-hidden="true">→</span>
          </Link>
        ))}
      </div>
    </>
  )
}
