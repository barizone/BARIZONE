import { lazy, Suspense, useLayoutEffect } from "react";
import "@/App.css";
import { BrowserRouter, Routes, Route, useLocation, useNavigate, Navigate, MemoryRouter } from "react-router-dom";
import { AuthProvider } from "@/context/AuthContext";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import MobileNav from "@/components/MobileNav";
import Home from "@/pages/Home";
import { Toaster } from "sonner";

// Le pagine diverse dalla Home si scaricano solo quando servono: primo caricamento più leggero su rete mobile.
const Contacts = lazy(() => import("@/pages/Contacts"));
const Notifications = lazy(() => import("@/pages/Notifications"));
const LiveGroups = lazy(() => import("@/pages/LiveGroups"));
const Subscribe = lazy(() => import("@/pages/Subscribe"));
const NewsList = lazy(() => import("@/pages/NewsList"));
const ArticleDetail = lazy(() => import("@/pages/ArticleDetail"));
const EventsPage = lazy(() => import("@/pages/EventsPage"));
const Pharmacies = lazy(() => import("@/pages/Pharmacies"));
const NeighborhoodNews = lazy(() => import("@/pages/NeighborhoodNews"));
const QuartieriPage = lazy(() => import("@/pages/QuartieriPage"));
const MerchantDetail = lazy(() => import("@/pages/MerchantDetail"));
const PromuoviPage = lazy(() => import("@/pages/PromuoviPage"));
const AdminPage = lazy(() => import("@/pages/AdminPage"));
const LoginPage = lazy(() => import("@/pages/AuthPages").then(m => ({ default: m.LoginPage })));
// Accesso di utenti e attività
const AccountLoginPage = lazy(() => import("@/pages/AccountAccess").then(m => ({ default: m.AccountLoginPage })));
const VerifyEmailPage = lazy(() => import("@/pages/AccountAccess").then(m => ({ default: m.VerifyEmailPage })));
const ForgotPasswordPage = lazy(() => import("@/pages/AccountAccess").then(m => ({ default: m.ForgotPasswordPage })));
const ResetPasswordPage = lazy(() => import("@/pages/AccountAccess").then(m => ({ default: m.ResetPasswordPage })));
const RegisterUserPage = lazy(() => import("@/pages/AccountRegister").then(m => ({ default: m.RegisterUserPage })));
const RegisterBusinessPage = lazy(() => import("@/pages/AccountRegister").then(m => ({ default: m.RegisterBusinessPage })));
const ProfilePage = lazy(() => import("@/pages/AccountAreas").then(m => ({ default: m.ProfilePage })));
const BusinessAreaPage = lazy(() => import("@/pages/AccountAreas").then(m => ({ default: m.BusinessAreaPage })));

/** Segnaposto durante il caricamento di una pagina: stessa struttura, nessuno spostamento del layout. */
function PageSkeleton() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6" role="status" aria-live="polite">
      <span className="sr-only">Caricamento…</span>
      <div className="skeleton h-8 w-2/3 mb-4" />
      <div className="skeleton h-4 w-full mb-2" />
      <div className="skeleton h-4 w-5/6 mb-6" />
      <div className="skeleton h-48 w-full" />
    </div>
  );
}

function AppRouter() {
  const location = useLocation();
  const navigate = useNavigate();
  useLayoutEffect(() => { window.scrollTo({ top: 0, left: 0, behavior: "instant" }); }, [location.pathname, location.search]);

  return (
    <>
      <a href="#contenuto" className="skip-link">Vai al contenuto</a>
      <Header />
      <main id="contenuto" tabIndex={-1}>
        {location.pathname !== '/' && <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-3"><button type="button" onClick={() => window.history.state?.idx > 0 ? navigate(-1) : navigate('/', {replace:true})} className="inline-flex items-center gap-2 min-h-11 px-4 py-2 rounded-xl border border-[#0B2545] text-[#0B2545] bg-white hover:bg-[#F8F5EE] focus-visible:outline-2 focus-visible:outline-[#D9531E]"><span aria-hidden="true">←</span> Indietro</button></div>}
        <Suspense fallback={<PageSkeleton />}>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/iscriviti-news" element={<Subscribe />} />
            <Route path="/notizie" element={<NewsList key="current" />} />
            <Route path="/archivio" element={<NewsList key="archive" archive />} />
            <Route path="/notizie/:id" element={<ArticleDetail />} />
            <Route path="/eventi" element={<EventsPage />} />
            <Route path="/farmacie" element={<Pharmacies />} />
            <Route path="/ristoranti" element={<Navigate to="/aziende?categoria=Ristoranti" replace />} />
            <Route path="/aziende" element={<QuartieriPage />} />
            <Route path="/quartieri" element={<NeighborhoodNews />} />
            <Route path="/aziende/:id" element={<MerchantDetail />} />
            <Route path="/promuovi" element={<PromuoviPage />} />
            <Route path="/simulazione" element={<Navigate to="/gestione-clienti" replace />} />
            <Route path="/gestione-clienti" element={<LiveGroups />} />
            <Route path="/esercente" element={<LiveGroups />} />
            <Route path="/adesioni" element={<LiveGroups />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/accedi" element={<AccountLoginPage />} />
            <Route path="/registrati" element={<RegisterUserPage />} />
            <Route path="/registra-attivita" element={<RegisterBusinessPage />} />
            <Route path="/verifica-email" element={<VerifyEmailPage />} />
            <Route path="/password-dimenticata" element={<ForgotPasswordPage />} />
            <Route path="/reimposta-password" element={<ResetPasswordPage />} />
            <Route path="/profilo" element={<ProfilePage />} />
            <Route path="/area-attivita" element={<BusinessAreaPage />} />
            <Route path="/register" element={<Navigate to="/" replace />} />
            <Route path="/contatti" element={<Contacts />} />
            <Route path="/notifiche" element={<Notifications />} />
            <Route path="/admin" element={<AdminPage />} />
          </Routes>
        </Suspense>
      </main>
      <Footer />
      <MobileNav />
    </>
  );
}

function App() {
  const Router = window.location.protocol === "file:" ? MemoryRouter : BrowserRouter;
  return (
    <div className="App">
      <Router>
        <AuthProvider>
          <AppRouter />
          {/* Su mobile i messaggi compaiono sopra la barra di navigazione inferiore. */}
          <Toaster position="bottom-center" mobileOffset={{ bottom: 76 }} />
        </AuthProvider>
      </Router>
    </div>
  );
}

export default App;
