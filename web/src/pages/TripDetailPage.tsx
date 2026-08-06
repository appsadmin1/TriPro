import React, { useEffect, useState } from 'react';
import {
  Typography,
  Box,
  CircularProgress,
  Stack,
  Card,
  Grid,
  Avatar,
  IconButton,
  Divider,
} from '@mui/material';
import {
  CalendarMonth,
  Folder,
  Group,
  Edit,
  ArrowForwardIos,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { tripService } from '../services/tripService';
import { Trip, TripDay, ItineraryItem } from '../data/models';
import { format, parseISO, differenceInDays, isValid } from 'date-fns';
import { getOptimizedImageUrl } from '../utils/imageUtils';

const TripDetailPage: React.FC = () => {
  const { tripId } = useParams<{ tripId: string }>();
  const [trip, setTrip] = useState<Trip | null>(null);
  const [days, setDays] = useState<TripDay[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const safeFormat = (dateStr: string | undefined, formatStr: string) => {
    if (!dateStr) return 'N/A';
    const d = parseISO(dateStr);
    return isValid(d) ? format(d, formatStr) : 'N/A';
  };

  useEffect(() => {
    if (!tripId) {
      console.log("No tripId found in TripDetailPage");
      return;
    }

    console.log("Observing trip details and days for:", tripId);
    const unsubTrip = tripService.observeTrip(tripId, (data) => {
      console.log("Trip data updated:", data?.name);
      setTrip(data);
      if (!data) {
        console.warn("Trip not found");
        setLoading(false);
      }
    });

    const unsubDays = tripService.observeDays(tripId, (data) => {
      console.log("Days updated, count:", data.length);
      setDays(data);
      setLoading(false);
    });

    return () => {
      unsubTrip();
      unsubDays();
    };
  }, [tripId]);

  if (loading || !trip) {
    return (
      <Layout title={trip?.name || 'Loading...'}>
        <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  const durationDays = trip && trip.startDate && trip.endDate
    ? (differenceInDays(parseISO(trip.endDate), parseISO(trip.startDate)) + 1)
    : 0;

  return (
    <Layout title={trip.name}>
      {/* Hero Section */}
      <Box
        sx={{
          width: '100%',
          height: 300,
          borderRadius: 4,
          overflow: 'hidden',
          position: 'relative',
          mb: 4,
          boxShadow: 3,
        }}
      >
        <Box
          component="img"
          src={getOptimizedImageUrl(trip.coverImageUrl)}
          sx={{ width: '100%', height: '100%', objectFit: 'cover' }}
          crossOrigin="anonymous"
        />
        <Box
          sx={{
            position: 'absolute',
            bottom: 0,
            left: 0,
            right: 0,
            background: 'linear-gradient(transparent, rgba(0,0,0,0.7))',
            p: 4,
            color: 'white',
          }}
        >
          <Typography variant="h2" sx={{ fontWeight: 'bold' }}>{trip.destination}</Typography>
          <Typography variant="h6">
            {safeFormat(trip.startDate, 'MMM d')} - {safeFormat(trip.endDate, 'MMM d, yyyy')}
          </Typography>
        </Box>
      </Box>

      {/* Stats Chips */}
      <Grid container spacing={2} sx={{ mb: 4 }}>
        <Grid item xs={4}>
          <Card variant="outlined" sx={{ textAlign: 'center', p: 2, borderRadius: 3 }}>
            <CalendarMonth color="primary" sx={{ mb: 1 }} />
            <Typography variant="h6">{durationDays} Days</Typography>
            <Typography variant="caption" color="text.secondary">DURATION</Typography>
          </Card>
        </Grid>
        <Grid item xs={4}>
          <Card variant="outlined" sx={{ textAlign: 'center', p: 2, borderRadius: 3, cursor: 'pointer' }} onClick={() => navigate(`/trip/${tripId}/docs`)}>
            <Folder color="primary" sx={{ mb: 1 }} />
            <Typography variant="h6">Docs</Typography>
            <Typography variant="caption" color="text.secondary">SAVED</Typography>
          </Card>
        </Grid>
        <Grid item xs={4}>
          <Card variant="outlined" sx={{ textAlign: 'center', p: 2, borderRadius: 3, cursor: 'pointer' }} onClick={() => navigate(`/trip/${tripId}/members`)}>
            <Group color="primary" sx={{ mb: 1 }} />
            <Typography variant="h6">{trip.memberIds.length}</Typography>
            <Typography variant="caption" color="text.secondary">TRAVELERS</Typography>
          </Card>
        </Grid>
      </Grid>

      {/* Travelers Section */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>TRAVELERS</Typography>
        <Card variant="outlined" sx={{ p: 2, borderRadius: 3, display: 'flex', alignItems: 'center' }}>
          <Stack direction="row" spacing={2}>
            {trip.memberIds.map(uid => (
              <Stack key={uid} alignItems="center" spacing={0.5}>
                <Avatar sx={{ bgcolor: 'primary.container', color: 'primary.main' }}>
                  {uid.substring(0, 1).toUpperCase()}
                </Avatar>
                <Typography variant="caption">Traveler</Typography>
              </Stack>
            ))}
          </Stack>
        </Card>
      </Box>

      <Typography variant="h4" color="primary" sx={{ fontWeight: 'bold', mb: 2 }}>Itinerary</Typography>

      {/* Day List */}
      <Stack spacing={2}>
        {days.map((day) => (
          <Card
            key={day.date}
            variant="outlined"
            sx={{
              p: 2,
              borderRadius: 3,
              cursor: 'pointer',
              '&:hover': { bgcolor: 'action.hover' }
            }}
            onClick={() => navigate(`/trip/${tripId}/day/${day.date}`)}
          >
            <Stack direction="row" alignItems="center" spacing={3}>
              <Box sx={{ minWidth: 60, textAlign: 'center' }}>
                <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'text.secondary' }}>
                  {safeFormat(day.date, 'EEE').toUpperCase()}
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
                  {safeFormat(day.date, 'd')}
                </Typography>
              </Box>

              <Divider orientation="vertical" flexItem />

              <Box sx={{ flexGrow: 1 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>
                  Day {day.dayIndex}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {day.dayNote || 'Free Day'}
                </Typography>
              </Box>

              <ArrowForwardIos fontSize="small" color="disabled" />
            </Stack>
          </Card>
        ))}
      </Stack>
    </Layout>
  );
};

export default TripDetailPage;
