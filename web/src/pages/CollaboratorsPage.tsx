import React, { useEffect, useState } from 'react';
import {
  Typography,
  Box,
  CircularProgress,
  Stack,
  Card,
  Avatar,
  IconButton,
  Button,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  ListItemSecondaryAction,
  Divider,
  Alert,
  Snackbar,
} from '@mui/material';
import { Delete, PersonAdd, Email } from '@mui/icons-material';
import { useParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { tripService } from '../services/tripService';
import { userService } from '../services/userService';
import { authService } from '../services/authService';
import { activityService } from '../services/activityService';
import { Trip, Role, UserProfile, ActivityType } from '../data/models';
import { useTranslation } from 'react-i18next';

interface MemberRow {
  profile: UserProfile;
  role: Role;
}

const CollaboratorsPage: React.FC = () => {
  const { tripId } = useParams<{ tripId: string }>();
  const [trip, setTrip] = useState<Trip | null>(null);
  const [members, setMembers] = useState<MemberRow[]>([]);
  const [pendingInvites, setPendingInvites] = useState<{ email: string, role: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<Role>(Role.VIEWER);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { t } = useTranslation();
  const user = authService.getCurrentUser();
  const isOwner = trip && user && trip.members[user.uid] === Role.OWNER;

  useEffect(() => {
    if (!tripId) return;

    const unsubTrip = tripService.observeTrip(tripId, async (data) => {
      setTrip(data);
      if (data) {
        const profilesMap = await userService.getProfiles(data.memberIds);
        const memberRows = data.memberIds.map(uid => ({
          profile: profilesMap[uid] || { uid, email: '', displayName: 'Unknown', photoUrl: '' },
          role: data.members[uid] as Role
        })).sort((a, b) => {
          if (a.role === Role.OWNER) return -1;
          if (b.role === Role.OWNER) return 1;
          return a.profile.displayName.localeCompare(b.profile.displayName);
        });
        setMembers(memberRows);
        setLoading(false);
      } else {
        setLoading(false);
      }
    });

    return () => unsubTrip();
  }, [tripId]);

  useEffect(() => {
    if (tripId && isOwner) {
      const unsubInvites = tripService.observePendingInvites(tripId, (data) => {
        setPendingInvites(data);
      });
      return () => unsubInvites();
    } else {
      setPendingInvites([]);
    }
  }, [tripId, isOwner]);

  const handleInvite = async () => {
    if (!tripId || !user) return;
    if (!inviteEmail.trim() || !inviteEmail.includes('@')) {
      setError('Enter a valid email address.');
      return;
    }

    try {
      await tripService.inviteByEmail(tripId, inviteEmail, inviteRole, user.uid);
      if (trip) {
        activityService.logActivity(tripId, trip.name, trip.memberIds, ActivityType.MEMBER_INVITED, `${inviteEmail} was invited as ${inviteRole}`, user.uid, user.displayName || 'Traveler');
      }
      setSuccess(`Invite sent to ${inviteEmail}.`);
      setInviteEmail('');
      setError(null);
    } catch (e: any) {
      setError(e.message || 'Failed to send invite.');
    }
  };

  const handleRemoveMember = async (uid: string) => {
    if (!tripId || !user || !trip) return;
    const name = members.find(m => m.profile.uid === uid)?.profile.displayName || 'A member';
    if (window.confirm('Are you sure you want to remove this member?')) {
      try {
        await tripService.removeMember(tripId, uid);
        activityService.logActivity(tripId, trip.name, trip.memberIds, ActivityType.MEMBER_REMOVED, `${name} was removed from the trip`, user.uid, user.displayName || 'Traveler');
        setSuccess('Member removed.');
      } catch (e: any) {
        setError(e.message || 'Failed to remove member.');
      }
    }
  };

  const handleChangeRole = async (uid: string, newRole: Role) => {
    if (!tripId || !user || !trip) return;
    const name = members.find(m => m.profile.uid === uid)?.profile.displayName || 'A member';
    try {
      await tripService.setMemberRole(tripId, uid, newRole);
      activityService.logActivity(tripId, trip.name, trip.memberIds, ActivityType.MEMBER_ROLE_CHANGED, `${name}'s role changed to ${newRole}`, user.uid, user.displayName || 'Traveler');
      setSuccess('Role updated.');
    } catch (e: any) {
      setError(e.message || 'Failed to update role.');
    }
  };

  if (loading) {
    return (
      <Layout title={t('travelers_title')}>
        <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  if (!trip) {
    return (
      <Layout title={t('trip_not_found')}>
        <Box sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h5">{t('trip_not_found_desc')}</Typography>
        </Box>
      </Layout>
    );
  }

  return (
    <Layout title={`${t('travelers_title')} - ${trip.name}`}>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" color="primary" sx={{ fontWeight: 'bold', mb: 1 }}>
          {t('travelers_title')}
        </Typography>
        <Typography variant="body1" color="text.secondary">
          {t('travelers_subtitle')}
        </Typography>
      </Box>

      {/* Invite Form for Owners */}
      {isOwner && (
        <Card variant="outlined" sx={{ p: 3, mb: 4, borderRadius: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>{t('invite_someone')}</Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
            <TextField
              fullWidth
              label={t('email_address')}
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
              placeholder="friend@example.com"
            />
            <FormControl sx={{ minWidth: 120 }}>
              <InputLabel>{t('role')}</InputLabel>
              <Select
                value={inviteRole}
                label={t('role')}
                onChange={(e) => setInviteRole(e.target.value as Role)}
              >
                <MenuItem value={Role.EDITOR}>{t('editor')}</MenuItem>
                <MenuItem value={Role.VIEWER}>{t('viewer')}</MenuItem>
              </Select>
            </FormControl>
            <Button
              variant="contained"
              startIcon={<PersonAdd />}
              onClick={handleInvite}
              sx={{ height: 56, px: 4 }}
            >
              {t('invite')}
            </Button>
          </Stack>
        </Card>
      )}

      {/* Members List */}
      <Card variant="outlined" sx={{ borderRadius: 3, mb: 4 }}>
        <List sx={{ width: '100%' }}>
          {members.map((member, index) => (
            <React.Fragment key={member.profile.uid}>
              <ListItem sx={{ py: 2 }}>
                <ListItemAvatar>
                  <Avatar src={member.profile.photoUrl}>
                    {member.profile.displayName?.charAt(0) || '?'}
                  </Avatar>
                </ListItemAvatar>
                <ListItemText
                  primary={member.profile.displayName}
                  secondary={member.role === Role.OWNER ? t('role_owner', { defaultValue: 'Owner' }) : (member.role === Role.EDITOR ? t('editor') : t('viewer'))}
                />
                <ListItemSecondaryAction sx={{ display: 'flex', alignItems: 'center' }}>
                  {isOwner && member.role !== Role.OWNER && (
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Select
                        size="small"
                        value={member.role}
                        onChange={(e) => handleChangeRole(member.profile.uid, e.target.value as Role)}
                        sx={{ minWidth: 100 }}
                      >
                        <MenuItem value={Role.EDITOR}>{t('editor')}</MenuItem>
                        <MenuItem value={Role.VIEWER}>{t('viewer')}</MenuItem>
                      </Select>
                      <IconButton
                        edge="end"
                        color="error"
                        onClick={() => handleRemoveMember(member.profile.uid)}
                      >
                        <Delete />
                      </IconButton>
                    </Stack>
                  )}
                  {!isOwner && (
                    <Typography variant="body2" color="text.secondary" sx={{ mr: 2 }}>
                      {member.role === Role.OWNER ? t('role_owner', { defaultValue: 'Owner' }) : (member.role === Role.EDITOR ? t('editor') : t('viewer'))}
                    </Typography>
                  )}
                  {member.role === Role.OWNER && (
                    <Typography variant="body2" color="primary" sx={{ fontWeight: 'bold', mr: 2 }}>
                      {member.profile.uid === user?.uid ? `${t('role_owner', { defaultValue: 'Owner' })} (${t('you', { defaultValue: 'You' })})` : t('role_owner', { defaultValue: 'Owner' })}
                    </Typography>
                  )}
                </ListItemSecondaryAction>
              </ListItem>
              {index < members.length - 1 && <Divider component="li" />}
            </React.Fragment>
          ))}
        </List>
      </Card>

      {/* Pending Invites for Owners */}
      {isOwner && pendingInvites.length > 0 && (
        <Box>
          <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center' }}>
            <Email sx={{ mr: 1 }} fontSize="small" /> {t('pending_invites')}
          </Typography>
          <Card variant="outlined" sx={{ borderRadius: 3 }}>
            <List>
              {pendingInvites.map((invite, index) => (
                <React.Fragment key={invite.email}>
                  <ListItem>
                    <ListItemText
                      primary={invite.email}
                      secondary={t('invited_as', { defaultValue: 'Invited as {{role}}', role: invite.role === Role.EDITOR ? t('editor') : t('viewer') })}
                    />
                    <Typography variant="caption" color="text.secondary">
                      {t('waiting_for_signin')}
                    </Typography>
                  </ListItem>
                  {index < pendingInvites.length - 1 && <Divider component="li" />}
                </React.Fragment>
              ))}
            </List>
          </Card>
        </Box>
      )}

      <Snackbar
        open={!!error}
        autoHideDuration={6000}
        onClose={() => setError(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={() => setError(null)} severity="error" sx={{ width: '100%' }}>
          {error}
        </Alert>
      </Snackbar>

      <Snackbar
        open={!!success}
        autoHideDuration={6000}
        onClose={() => setSuccess(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={() => setSuccess(null)} severity="success" sx={{ width: '100%' }}>
          {success}
        </Alert>
      </Snackbar>
    </Layout>
  );
};

export default CollaboratorsPage;
