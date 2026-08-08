import React, { useState } from 'react';
import {
  Box,
  Button,
  Container,
  Typography,
  Paper,
  Stack,
  CircularProgress,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import { Explore, FlightTakeoff } from '@mui/icons-material';
import { authService } from '../services/authService';
import { useTranslation } from 'react-i18next';

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
  const { t } = useTranslation();

  const handleSignIn = async () => {
    setLoading(true);
    setError(null);
    try {
      await authService.signInWithGoogle();
    } catch (err: any) {
      setError(err.message || 'Failed to sign in');
      setLoading(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {/* Left Side: Form */}
      <Box
        sx={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: isDesktop ? 'flex-start' : 'center',
          px: isDesktop ? 10 : 3,
          bgcolor: 'background.paper',
        }}
      >
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 8 }}>
          <Explore color="primary" sx={{ fontSize: 40 }} />
          <Typography variant="h4" color="primary" sx={{ fontWeight: 'bold' }}>
            {t('app_name')}
          </Typography>
        </Stack>

        <Typography variant="h1" sx={{ fontWeight: 'bold', mb: 3, textAlign: isDesktop ? 'left' : 'center', lineHeight: 1.2 }}>
          {t('login_tagline').split(' ').slice(0, 2).join(' ')} <br /> {t('login_tagline').split(' ').slice(2).join(' ')}
        </Typography>

        <Typography variant="body1" color="text.secondary" sx={{ mb: 6, maxWidth: 400, textAlign: isDesktop ? 'left' : 'center' }}>
          {t('login_subtitle', { defaultValue: 'Plan, share, and experience travel like never before. TriPro meets the wonder of discovery.' })}
        </Typography>

        <Button
          variant="contained"
          size="large"
          onClick={handleSignIn}
          disabled={loading}
          sx={{
            width: '100%',
            maxWidth: 320,
            py: 1.5,
            bgcolor: 'secondary.main',
            '&:hover': { bgcolor: 'secondary.dark' },
            boxShadow: theme.shadows[4],
          }}
        >
          {loading ? <CircularProgress size={24} color="inherit" /> : t('login_sign_in_google')}
        </Button>

        {error && (
          <Typography color="error" variant="body2" sx={{ mt: 2 }}>
            {t('failed_signin', { defaultValue: error })}
          </Typography>
        )}
      </Box>

      {/* Right Side: Hero (Desktop only) */}
      {isDesktop && (
        <Box
          sx={{
            flex: 1,
            background: `linear-gradient(rgba(0,0,0,0.1), rgba(0,0,0,0.3)), url('https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&q=80&w=2070')`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            display: 'flex',
            alignItems: 'flex-end',
            justifyContent: 'flex-end',
            p: 6,
          }}
        >
          <Paper
            sx={{
              p: 4,
              maxWidth: 320,
              bgcolor: 'rgba(255, 255, 255, 0.2)',
              backdropFilter: 'blur(10px)',
              border: '1px solid rgba(255, 255, 255, 0.3)',
              borderRadius: 4,
              color: 'white',
            }}
          >
            <Stack direction="row" spacing={1} alignItems="center" mb={1}>
              <FlightTakeoff />
              <Typography variant="overline" sx={{ fontWeight: 'bold' }}>{t('next_adventure')}</Typography>
            </Stack>
            <Typography variant="body2">
              {t('login_quote')}
            </Typography>
          </Paper>
        </Box>
      )}
    </Box>
  );
};

export default LoginPage;
