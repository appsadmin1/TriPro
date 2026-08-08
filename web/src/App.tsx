import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect, useState, useMemo } from 'react';
import { User } from 'firebase/auth';
import { authService } from './services/authService';
import { CircularProgress, Box, ThemeProvider, createTheme } from '@mui/material';
import { themeOptions } from './theme';
import { useTranslation } from 'react-i18next';
import { CacheProvider } from '@emotion/react';
import createCache from '@emotion/cache';
import { prefixer } from 'stylis';
import rtlPlugin from 'stylis-plugin-rtl';

// Pages
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import TripDetailPage from './pages/TripDetailPage';
import DayDetailPage from './pages/DayDetailPage';
import CreateTripPage from './pages/CreateTripPage';
import TripDocsPage from './pages/TripDocsPage';
import ProfilePage from './pages/ProfilePage';
import SettingsPage from './pages/SettingsPage';
import PastAdventuresPage from './pages/PastAdventuresPage';
import CollaboratorsPage from './pages/CollaboratorsPage';
import AlertsPage from './pages/AlertsPage';
import SharedTripsPage from './pages/SharedTripsPage';

// Create rtl cache
const cacheRtl = createCache({
  key: 'muirtl',
  stylisPlugins: [prefixer, rtlPlugin],
});

const cacheLtr = createCache({
  key: 'mui',
});

interface ProtectedRouteProps {
  user: User | null;
  children: React.ReactElement;
}

const ProtectedRoute = ({ user, children }: ProtectedRouteProps) => {
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const { i18n } = useTranslation();

  const direction = i18n.language.startsWith('he') ? 'rtl' : 'ltr';

  useEffect(() => {
    document.body.dir = direction;
  }, [direction]);

  const currentTheme = useMemo(() => createTheme({
    ...themeOptions,
    direction,
  }), [direction]);

  useEffect(() => {
    const unsubscribe = authService.subscribeToAuthChanges((u) => {
      setUser(u);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="100vh" bgcolor="#F8F9FF">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <CacheProvider value={direction === 'rtl' ? cacheRtl : cacheLtr}>
      <ThemeProvider theme={currentTheme}>
        <Router>
          <Routes>
            <Route path="/login" element={!user ? <LoginPage /> : <Navigate to="/" />} />

            <Route path="/" element={
              <ProtectedRoute user={user}>
                <DashboardPage />
              </ProtectedRoute>
            } />

            <Route path="/trip/:tripId" element={
              <ProtectedRoute user={user}>
                <TripDetailPage />
              </ProtectedRoute>
            } />

            <Route path="/trip/:tripId/docs" element={
              <ProtectedRoute user={user}>
                <TripDocsPage />
              </ProtectedRoute>
            } />

            <Route path="/trip/:tripId/day/:date" element={
              <ProtectedRoute user={user}>
                <DayDetailPage />
              </ProtectedRoute>
            } />

            <Route path="/create-trip" element={
              <ProtectedRoute user={user}>
                <CreateTripPage />
              </ProtectedRoute>
            } />

            <Route path="/profile" element={
              <ProtectedRoute user={user}>
                <ProfilePage />
              </ProtectedRoute>
            } />

            <Route path="/settings" element={
              <ProtectedRoute user={user}>
                <SettingsPage />
              </ProtectedRoute>
            } />

            <Route path="/past" element={
              <ProtectedRoute user={user}>
                <PastAdventuresPage />
              </ProtectedRoute>
            } />

            <Route path="/trip/:tripId/members" element={
              <ProtectedRoute user={user}>
                <CollaboratorsPage />
              </ProtectedRoute>
            } />

            <Route path="/alerts" element={
              <ProtectedRoute user={user}>
                <AlertsPage />
              </ProtectedRoute>
            } />

            <Route path="/shared-trips-view/:uid" element={
              <ProtectedRoute user={user}>
                <SharedTripsPage />
              </ProtectedRoute>
            } />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </Router>
      </ThemeProvider>
    </CacheProvider>
  );
}

export default App;
