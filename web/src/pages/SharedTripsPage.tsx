import React, { useEffect, useState } from 'react';
import {
  Typography,
  Grid,
  Box,
  CircularProgress,
  Avatar,
  Stack,
  IconButton,
} from '@mui/material';
import { ArrowBack } from '@mui/icons-material';
import Layout from '../components/Layout';
import TripCard from '../components/TripCard';
import { tripService } from '../services/tripService';
import { authService } from '../services/authService';
import { userService } from '../services/userService';
import { Trip, UserProfile } from '../data/models';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const SharedTripsPage: React.FC = () => {
  const { uid } = useParams<{ uid: string }>();
  const [trips, setTrips] = useState<Trip[]>([]);
  const [targetProfile, setTargetProfile] = useState<UserProfile | null>(null);
  const [profiles, setProfiles] = useState<Record<string, UserProfile>>({});
  const [loading, setLoading] = useState(true);
  const user = authService.getCurrentUser();
  const navigate = useNavigate();
  const { t } = useTranslation();

  useEffect(() => {
    if (!user || !uid) return;

    const unsubscribe = tripService.observeUserTrips(user.uid, async (data) => {
      const sharedTrips = data.filter(t => t.memberIds.includes(uid));
      setTrips(sharedTrips);

      const allMemberIds = Array.from(new Set(sharedTrips.flatMap(t => t.memberIds)));
      const profileMap = await userService.getProfiles(allMemberIds);
      setProfiles(profileMap);
      setTargetProfile(profileMap[uid] || null);

      setLoading(false);
    });

    return () => unsubscribe();
  }, [user, uid]);

  if (loading) {
    return (
      <Layout title={t('shared_trips_title')}>
        <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  return (
    <Layout title={t('shared_trips_title_with_name', { name: targetProfile?.displayName || t('traveler') })}>
      <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 4 }}>
        <IconButton onClick={() => navigate(-1)}>
          <ArrowBack />
        </IconButton>
        <Avatar src={targetProfile?.photoUrl} sx={{ width: 64, height: 64 }} />
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
            {t('shared_trips_title_with_name', { name: targetProfile?.displayName || t('traveler') })}
          </Typography>
          <Typography variant="body1" color="text.secondary">
            {t('shared_trips_count', { count: trips.length })}
          </Typography>
        </Box>
      </Stack>

      {trips.length > 0 ? (
        <Grid container spacing={3}>
          {trips.map((trip) => (
            <Grid item xs={12} sm={6} lg={4} key={trip.id}>
              <TripCard trip={trip} profiles={profiles} onClick={() => navigate(`/trip/${trip.id}`)} />
            </Grid>
          ))}
        </Grid>
      ) : (
        <Box sx={{ py: 8, textAlign: 'center' }}>
          <Typography color="text.secondary">
            {t('shared_trips_empty')}
          </Typography>
        </Box>
      )}
    </Layout>
  );
};

export default SharedTripsPage;
