import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { User } from 'firebase/auth';
import { authService } from './services/authService';
import { CircularProgress, Box, ThemeProvider } from '@mui/material';
import { theme } from './theme';

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
    <ThemeProvider theme={theme}>
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

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App;
