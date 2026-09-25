import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { MiseEnPage } from './components/MiseEnPage'
import { Accueil } from './pages/Accueil'
import { Etudiant } from './pages/Etudiant'
import { Formateur } from './pages/Formateur'
import { Relecture } from './pages/Relecture'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<MiseEnPage />}>
          <Route index element={<Accueil />} />
          <Route path="formateur" element={<Formateur />} />
          <Route path="etudiant" element={<Etudiant />} />
          <Route path="relecture" element={<Relecture />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
