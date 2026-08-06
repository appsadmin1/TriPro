import React from 'react';
import {
  Typography,
  Box,
  Avatar,
  Paper,
  Stack,
  Button,
  Divider,
} from '@mui/material';
import { Logout, Email, Person } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { authService } from '../services/authService';

const ProfilePage: React.FC = () => {
  const user = authService.getCurrentUser();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await authService.signOut();
    navigate('/login');
  };

  if (!user) return null;

  return (
    <Layout title="My Profile">
      <Box sx={{ maxWidth: 600, mx: 'auto', mt: 4 }}>
        <Paper sx={{ p: 6, borderRadius: 6, textAlign: 'center', boxShadow: 3 }}>
          <Avatar
            src={user.photoURL || undefined}
            sx={{ width: 120, height: 120, mx: 'auto', mb: 3, border: '4px solid', borderColor: 'primary.light' }}
          >
            <Person sx={{ fontSize: 60 }} />
          </Avatar>

          <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
            {user.displayName || 'Traveler'}
          </Typography>

          <Stack direction="row" spacing={1} justifyContent="center" alignItems="center" color="text.secondary" sx={{ mb: 4 }}>
            <Email fontSize="small" />
            <Typography variant="body1">{user.email}</Typography>
          </Stack>

          <Divider sx={{ my: 4 }} />

          <Stack spacing={2}>
            <Button
              variant="outlined"
              color="error"
              startIcon={<Logout />}
              onClick={handleLogout}
              fullWidth
              sx={{ borderRadius: 10, py: 1.5 }}
            >
              Sign Out
            </Button>
          </Stack>
        </Paper>
      </Box>
    </Layout>
  );
};

export default ProfilePage;
