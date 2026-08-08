import React, { useState } from 'react';
import {
  Typography,
  Box,
  Avatar,
  Paper,
  Stack,
  Button,
  Divider,
  TextField,
  IconButton,
  Alert,
  CircularProgress,
} from '@mui/material';
import { Logout, Email, Person, Edit, PhotoCamera, Close, Save } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { authService } from '../services/authService';
import { userService } from '../services/userService';
import { uploadAttachment } from '../services/cloudinaryService';
import { useTranslation } from 'react-i18next';

const ProfilePage: React.FC = () => {
  const user = authService.getCurrentUser();
  const [editing, setEditing] = useState(false);
  const [editName, setEditName] = useState(user?.displayName || '');
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleLogout = async () => {
    await authService.signOut();
    navigate('/login');
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !user) return;

    setUploadingPhoto(true);
    setError(null);
    try {
      const att = await uploadAttachment(file, user.uid);
      // Update Auth and Firestore
      await authService.updateUserProfile(user.displayName || '', att.downloadUrl);
      await userService.updateUserProfile(user.uid, user.displayName || '', att.downloadUrl);
      window.location.reload(); // Force refresh to show changes
    } catch (err) {
      console.error(err);
      setError('Failed to upload profile photo');
    } finally {
      setUploadingPhoto(false);
    }
  };

  const handleUpdateName = async () => {
    if (!user || !editName) return;

    setUpdating(true);
    setError(null);
    try {
      await authService.updateUserProfile(editName, user.photoURL || '');
      await userService.updateUserProfile(user.uid, editName, user.photoURL || '');
      setEditing(false);
      window.location.reload(); // Force refresh to show changes
    } catch (err) {
      console.error(err);
      setError('Failed to update name');
    } finally {
      setUpdating(false);
    }
  };

  if (!user) return null;

  return (
    <Layout title={t('my_profile')}>
      <Box sx={{ maxWidth: 600, mx: 'auto', mt: 4 }}>
        <Paper sx={{ p: 6, borderRadius: 6, textAlign: 'center', boxShadow: 3, position: 'relative' }}>
          {!editing && (
            <IconButton
              onClick={() => setEditing(true)}
              sx={{ position: 'absolute', top: 16, right: 16 }}
            >
              <Edit />
            </IconButton>
          )}

          <Box sx={{ position: 'relative', width: 120, height: 120, mx: 'auto', mb: 3 }}>
            <Avatar
              src={user.photoURL || undefined}
              sx={{ width: 120, height: 120, border: '4px solid', borderColor: 'primary.light' }}
            >
              <Person sx={{ fontSize: 60 }} />
            </Avatar>
            <IconButton
              component="label"
              sx={{
                position: 'absolute',
                bottom: 0,
                right: 0,
                bgcolor: 'primary.main',
                color: 'white',
                '&:hover': { bgcolor: 'primary.dark' },
                width: 32,
                height: 32
              }}
              disabled={uploadingPhoto}
            >
              <input type="file" hidden accept="image/*" onChange={handlePhotoUpload} />
              {uploadingPhoto ? <CircularProgress size={20} color="inherit" /> : <PhotoCamera sx={{ fontSize: 18 }} />}
            </IconButton>
          </Box>

          {editing ? (
            <Stack spacing={2} alignItems="center">
              <TextField
                label={t('display_name')}
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                fullWidth
                autoFocus
              />
              <Stack direction="row" spacing={2} sx={{ width: '100%' }}>
                <Button
                  variant="outlined"
                  onClick={() => setEditing(false)}
                  fullWidth
                  sx={{ borderRadius: 10 }}
                  startIcon={<Close />}
                >
                  {t('action_cancel')}
                </Button>
                <Button
                  variant="contained"
                  onClick={handleUpdateName}
                  disabled={updating || !editName}
                  fullWidth
                  sx={{ borderRadius: 10 }}
                  startIcon={updating ? <CircularProgress size={20} color="inherit" /> : <Save />}
                >
                  {updating ? t('updating') : t('action_save')}
                </Button>
              </Stack>
            </Stack>
          ) : (
            <>
              <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
                {user.displayName || t('traveler')}
              </Typography>

              <Stack direction="row" spacing={1} justifyContent="center" alignItems="center" color="text.secondary" sx={{ mb: 4 }}>
                <Email fontSize="small" />
                <Typography variant="body1">{user.email}</Typography>
              </Stack>
            </>
          )}

          {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

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
              {t('settings_sign_out')}
            </Button>
          </Stack>
        </Paper>
      </Box>
    </Layout>
  );
};

export default ProfilePage;
