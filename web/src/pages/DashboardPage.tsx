import React, { useEffect, useState } from 'react';
import {
  Typography,
  Grid,
  Box,
  CircularProgress,
  Fab,
  Stack,
} from '@mui/material';
import { Add, FlightTakeoff, History } from '@mui/icons-material';
import Layout from '../components/Layout';
import TripCard from '../components/TripCard';
import { tripService } from '../services/tripService';
import { authService } from '../services/authService';
import { Trip } from '../data/models';
import { useNavigate } from 'react-router-dom';
import { isPast, parseISO } from 'date-fns';

const DashboardPage: React.FC = () => {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [loading, setLoading] = useState(true);
  const user = authService.getCurrentUser();
  const navigate = useNavigate();

  useEffect(() => {
    console.log("DashboardPage: User state:", user?.uid);
    if (!user) return;

    console.log("DashboardPage: Observing trips for user:", user.uid);
    let stillLoading = true;
    const unsubscribe = tripService.observeUserTrips(user.uid, (data) => {
      console.log("DashboardPage: Received trips:", data.length);
      setTrips(data);
      stillLoading = false;
      setLoading(false);
    });

    // Safety timeout to stop loading if nothing happens
    const timer = setTimeout(() => {
      if (stillLoading) {
        console.warn("DashboardPage: Firestore listener timed out after 10s");
        setLoading(false);
      }
    }, 10000);

    return () => {
      unsubscribe();
      clearTimeout(timer);
    };
  }, [user]);

  const upcomingTrips = trips.filter((t) => {
    try {
      return t.endDate ? !isPast(parseISO(t.endDate)) : true;
    } catch (e) {
      return true;
    }
  });
  const pastTrips = trips.filter((t) => {
    try {
      return t.endDate ? isPast(parseISO(t.endDate)) : false;
    } catch (e) {
      return false;
    }
  });

  if (loading) {
    return (
      <Layout title="My Trips">
        <Box display="flex" flexDirection="column" justifyContent="center" alignItems="center" height="60vh">
          <CircularProgress sx={{ mb: 2 }} />
          <Typography color="text.secondary">Loading your trips...</Typography>
        </Box>
      </Layout>
    );
  }

  return (
    <Layout title="My Trips">
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" color="primary" sx={{ fontWeight: 'bold', mb: 1 }}>
          My Trips
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Your upcoming and past adventures.
        </Typography>
      </Box>

      {upcomingTrips.length > 0 ? (
        <>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
            <FlightTakeoff color="primary" />
            <Typography variant="h5" sx={{ fontWeight: 'medium' }}>Upcoming</Typography>
          </Stack>
          <Grid container spacing={3} sx={{ mb: 6 }}>
            {upcomingTrips.map((trip) => (
              <Grid item xs={12} sm={6} lg={4} key={trip.id}>
                <TripCard trip={trip} onClick={() => navigate(`/trip/${trip.id}`)} />
              </Grid>
            ))}
          </Grid>
        </>
      ) : (
        <Box sx={{ py: 8, textAlign: 'center' }}>
          <Typography color="text.secondary">No upcoming trips — tap the + button to plan your first one.</Typography>
        </Box>
      )}

      {pastTrips.length > 0 && (
        <>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
            <History color="disabled" />
            <Typography variant="h5" color="text.secondary" sx={{ fontWeight: 'medium' }}>Past Adventures</Typography>
          </Stack>
          <Grid container spacing={3}>
            {pastTrips.map((trip) => (
              <Grid item xs={12} sm={6} lg={4} key={trip.id}>
                <TripCard trip={trip} isPast onClick={() => navigate(`/trip/${trip.id}`)} />
              </Grid>
            ))}
          </Grid>
        </>
      )}

      <Fab
        color="secondary"
        aria-label="add"
        sx={{ position: 'fixed', bottom: 32, right: 32 }}
        onClick={() => navigate('/create-trip')}
      >
        <Add />
      </Fab>
    </Layout>
  );
};

export default DashboardPage;
