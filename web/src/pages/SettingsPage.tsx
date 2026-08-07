import React, { useState, useEffect } from 'react';
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
  Avatar,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
} from '@mui/material';
import { Notifications, Security, Palette, Logout, ChevronRight } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { authService } from '../services/authService';
import { userService } from '../services/userService';
import { ITEM_TYPE_COLORS } from '../utils/colorUtils';
import { MarkerColorKey, MarkerColorPalette, NotificationPreferences, ItemType } from '../data/models';

const SettingsPage: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationPreferences>({
    tripInvites: true,
    itineraryChanges: true,
    dayInfoChanges: true,
  });
  const [activityColors, setActivityColors] = useState<Record<string, string>>({});
  const [colorPickerKey, setColorPickerKey] = useState<MarkerColorKey | null>(null);

  const navigate = useNavigate();
  const user = authService.getCurrentUser();

  useEffect(() => {
    if (!user) return;

    const unsubNotifs = userService.observeNotificationPreferences(user.uid, (prefs) => {
      setNotifications(prefs);
    });

    const unsubColors = userService.observeActivityColors(user.uid, (colors) => {
      setActivityColors(colors);
    });

    return () => {
      unsubNotifs();
      unsubColors();
    };
  }, [user]);

  const handleToggle = (key: keyof NotificationPreferences) => {
    if (!user) return;
    const updated = { ...notifications, [key]: !notifications[key] };
    userService.updateNotificationPreferences(user.uid, updated);
  };

  const handleColorSelect = (hex: string) => {
    if (!user || !colorPickerKey) return;
    userService.updateActivityColor(user.uid, colorPickerKey, hex);
    setColorPickerKey(null);
  };

  const handleLogout = async () => {
    await authService.signOut();
    navigate('/login');
  };

  const getMarkerColor = (key: MarkerColorKey) => {
    return activityColors[key] || ITEM_TYPE_COLORS[key as unknown as ItemType] || MarkerColorPalette[0];
  };

  return (
    <Layout title="Settings">
      <Box sx={{ maxWidth: 700, mx: 'auto', mt: 4, pb: 8 }}>
        <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 4 }}>Settings</Typography>

        <Stack spacing={4}>
          {/* Profile Header */}
          <Paper sx={{ p: 3, borderRadius: 4, display: 'flex', alignItems: 'center' }}>
            <Avatar src={user?.photoURL || ''} sx={{ width: 64, height: 64, mr: 3 }} />
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>{user?.displayName || 'Traveler'}</Typography>
              <Typography variant="body2" color="text.secondary">{user?.email}</Typography>
            </Box>
          </Paper>

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
                control={<Switch checked={notifications.dayInfoChanges} onChange={() => handleToggle('dayInfoChanges')} />}
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

          {/* Marker Colors Section */}
          <Paper sx={{ p: 4, borderRadius: 4 }}>
            <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
              <Palette color="primary" />
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>Marker Colors</Typography>
            </Stack>

            <Stack spacing={1}>
              {Object.values(MarkerColorKey).map((key) => (
                <Box
                  key={key}
                  onClick={() => setColorPickerKey(key)}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    p: 1.5,
                    borderRadius: 2,
                    cursor: 'pointer',
                    '&:hover': { bgcolor: 'action.hover' }
                  }}
                >
                  <Typography variant="body1" sx={{ textTransform: 'capitalize' }}>
                    {key.toLowerCase()}
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Box
                      sx={{
                        width: 24,
                        height: 24,
                        borderRadius: '50%',
                        bgcolor: getMarkerColor(key),
                        border: '1px solid',
                        borderColor: 'divider',
                        mr: 1
                      }}
                    />
                    <ChevronRight color="action" />
                  </Box>
                </Box>
              ))}
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
        </Stack>
      </Box>

      {/* Color Picker Dialog */}
      <Dialog open={!!colorPickerKey} onClose={() => setColorPickerKey(null)} fullWidth maxWidth="xs">
        <DialogTitle sx={{ textTransform: 'capitalize' }}>
          Choose Color for {colorPickerKey?.toLowerCase()}
        </DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} justifyContent="center" sx={{ p: 1 }}>
            {MarkerColorPalette.map((hex) => (
              <Grid item key={hex}>
                <Box
                  onClick={() => handleColorSelect(hex)}
                  sx={{
                    width: 48,
                    height: 48,
                    borderRadius: '50%',
                    bgcolor: hex,
                    cursor: 'pointer',
                    border: '3px solid',
                    borderColor: activityColors[colorPickerKey || ''] === hex ? 'primary.main' : 'transparent',
                    boxShadow: 1,
                    '&:hover': { transform: 'scale(1.1)' },
                    transition: 'transform 0.2s'
                  }}
                />
              </Grid>
            ))}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setColorPickerKey(null)}>Cancel</Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
};

export default SettingsPage;
