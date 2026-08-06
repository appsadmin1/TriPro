import React, { useState } from 'react';
import {
  Typography,
  Box,
  Paper,
  Stack,
  Switch,
  FormControlLabel,
  Divider,
  Button,
  Alert,
} from '@mui/material';
import { Notifications, Security, Palette, Logout } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { authService } from '../services/authService';

const SettingsPage: React.FC = () => {
  const [notifications, setNotifications] = useState({
    tripInvites: true,
    itineraryChanges: true,
    dayUpdates: true,
  });

  const navigate = useNavigate();

  const handleToggle = (key: keyof typeof notifications) => {
    setNotifications(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const handleLogout = async () => {
    await authService.signOut();
    navigate('/login');
  };

  return (
    <Layout title="Settings">
      <Box sx={{ maxWidth: 700, mx: 'auto', mt: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 4 }}>Settings</Typography>

        <Stack spacing={4}>
          {/* Notifications Section */}
          <Paper sx={{ p: 4, borderRadius: 4 }}>
            <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
              <Notifications color="primary" />
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>Notifications</Typography>
            </Stack>

            <Stack spacing={2}>
              <FormControlLabel
                control={<Switch checked={notifications.tripInvites} onChange={() => handleToggle('tripInvites')} />}
                label={
                  <Box>
                    <Typography variant="body1" sx={{ fontWeight: 'medium' }}>Trip Invites</Typography>
                    <Typography variant="body2" color="text.secondary">Get notified when someone invites you to a trip.</Typography>
                  </Box>
                }
                sx={{ width: '100%', justifyContent: 'space-between', ml: 0 }}
                labelPlacement="start"
              />
              <Divider />
              <FormControlLabel
                control={<Switch checked={notifications.itineraryChanges} onChange={() => handleToggle('itineraryChanges')} />}
                label={
                  <Box>
                    <Typography variant="body1" sx={{ fontWeight: 'medium' }}>Itinerary Changes</Typography>
                    <Typography variant="body2" color="text.secondary">Get notified when a trip plan is updated.</Typography>
                  </Box>
                }
                sx={{ width: '100%', justifyContent: 'space-between', ml: 0 }}
                labelPlacement="start"
              />
              <Divider />
              <FormControlLabel
                control={<Switch checked={notifications.dayUpdates} onChange={() => handleToggle('dayUpdates')} />}
                label={
                  <Box>
                    <Typography variant="body1" sx={{ fontWeight: 'medium' }}>Day Updates</Typography>
                    <Typography variant="body2" color="text.secondary">Get notified about daily notes and weather.</Typography>
                  </Box>
                }
                sx={{ width: '100%', justifyContent: 'space-between', ml: 0 }}
                labelPlacement="start"
              />
            </Stack>
          </Paper>

          {/* Account Section */}
          <Paper sx={{ p: 4, borderRadius: 4 }}>
            <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
              <Security color="primary" />
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>Account</Typography>
            </Stack>

            <Button
              variant="outlined"
              color="error"
              fullWidth
              startIcon={<Logout />}
              onClick={handleLogout}
              sx={{ borderRadius: 3, py: 1 }}
            >
              Sign Out from all devices
            </Button>
          </Paper>

          <Alert severity="info" sx={{ borderRadius: 3 }}>
            More settings like Marker Colors and Currency will be available soon in the web app.
          </Alert>
        </Stack>
      </Box>
    </Layout>
  );
};

export default SettingsPage;
