import React, { useEffect, useState } from 'react';
import {
  Typography,
  Grid,
  Box,
  CircularProgress,
  Stack,
} from '@mui/material';
import { History } from '@mui/icons-material';
import Layout from '../components/Layout';
import TripCard from '../components/TripCard';
import { tripService } from '../services/tripService';
import { authService } from '../services/authService';
import { userService } from '../services/userService';
import { Trip, UserProfile } from '../data/models';
import { useNavigate } from 'react-router-dom';
import { isPast, parseISO, isValid, endOfDay } from 'date-fns';
import { useTranslation } from 'react-i18next';

const PastAdventuresPage: React.FC = () => {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [profiles, setProfiles] = useState<Record<string, UserProfile>>({});
  const [loading, setLoading] = useState(true);
  const user = authService.getCurrentUser();
  const navigate = useNavigate();
  const { t } = useTranslation();

  useEffect(() => {
    if (!user) return;
    const unsubscribe = tripService.observeUserTrips(user.uid, async (data) => {
      setTrips(data);

      const allMemberIds = Array.from(new Set(data.flatMap(t => t.memberIds)));
      if (allMemberIds.length > 0) {
        const profileMap = await userService.getProfiles(allMemberIds);
        setProfiles(profileMap);
      }

      setLoading(false);
    });
    return () => unsubscribe();
  }, [user]);

  const pastTrips = trips.filter((t) => {
    if (!t.endDate) return false;
    const d = parseISO(t.endDate);
    return isValid(d) && isPast(endOfDay(d));
  });

  if (loading) {
    return (
      <Layout title={t('past_adventures')}>
        <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  return (
    <Layout title={t('past_adventures')}>
      <Box sx={{ mb: 4 }}>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
          <History color="primary" sx={{ fontSize: 32 }} />
          <Typography variant="h4" color="primary" sx={{ fontWeight: 'bold' }}>
            {t('past_adventures')}
          </Typography>
        </Stack>
        <Typography variant="body1" color="text.secondary">
          {t('past_adventures_subtitle')}
        </Typography>
      </Box>

      {pastTrips.length > 0 ? (
        <Grid container spacing={3}>
          {pastTrips.map((trip) => (
            <Grid item xs={12} sm={6} lg={4} key={trip.id}>
              <TripCard trip={trip} profiles={profiles} isPast onClick={() => navigate(`/trip/${trip.id}`)} />
            </Grid>
          ))}
        </Grid>
      ) : (
        <Box sx={{ py: 8, textAlign: 'center' }}>
          <Typography color="text.secondary">{t('no_past_trips')}</Typography>
        </Box>
      )}
    </Layout>
  );
};

export default PastAdventuresPage;
