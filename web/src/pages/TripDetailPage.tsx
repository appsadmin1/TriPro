import * as React from 'react';
import { useEffect, useState } from 'react';
import {
  Typography,
  Box,
  CircularProgress,
  Stack,
  Card,
  Grid,
  Avatar,
  IconButton,
} from '@mui/material';
import {
  CalendarMonth,
  Folder,
  Group,
  Edit,
  ArrowForwardIos,
  AccountCircle,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import TripEditDialog from '../components/TripEditDialog';
import { tripService } from '../services/tripService';
import { userService } from '../services/userService';
import { authService } from '../services/authService';
import { Trip, TripDay, UserProfile, ItineraryItem, ItemType } from '../data/models';
import { format, parseISO, differenceInDays, isValid, isToday as isDateToday } from 'date-fns';
import { getOptimizedImageUrl } from '../utils/imageUtils';
import { ITEM_TYPE_COLORS } from '../utils/colorUtils';

const TripDetailPage: React.FC = () => {
  const { tripId } = useParams<{ tripId: string }>();
  const [trip, setTrip] = useState<Trip | null>(null);
  const [days, setDays] = useState<TripDay[]>([]);
  const [itemsByDate, setItemsByDate] = useState<Record<string, ItineraryItem[]>>({});
  const [activityColors, setActivityColors] = useState<Record<string, string>>({});
  const [profiles, setProfiles] = useState<Record<string, UserProfile>>({});
  const [loading, setLoading] = useState(true);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const navigate = useNavigate();
  const user = authService.getCurrentUser();

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
    const unsubTrip = tripService.observeTrip(tripId, async (data) => {
      console.log("Trip data updated:", data?.name);
      setTrip(data);
      if (data) {
        // Fetch traveler profiles
        const userProfiles = await userService.getProfiles(data.memberIds);
        setProfiles(userProfiles);
      } else {
        console.warn("Trip not found");
        setLoading(false);
      }
    });

    const unsubDays = tripService.observeDays(tripId, (data) => {
      console.log("Days updated, count:", data.length);
      setDays(data);
      setLoading(false);
    });

    const unsubColors = user ? userService.observeActivityColors(user.uid, (colors) => {
      setActivityColors(colors);
    }) : () => {};

    return () => {
      unsubTrip();
      unsubDays();
      unsubColors();
    };
  }, [tripId, user]);

  useEffect(() => {
    if (!tripId || days.length === 0) return;

    const unsubItems = tripService.observeAllItemsForTrip(tripId, days.map(d => d.date), (data) => {
      setItemsByDate(data);
    });

    return () => unsubItems();
  }, [tripId, days]);

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
            display: 'flex',
            justify: 'space-between',
            alignItems: 'flex-end',
          }}
        >
          <Box>
            <Typography variant="h2" sx={{ fontWeight: 'bold' }}>{trip.destination}</Typography>
            <Typography variant="h6">
              {safeFormat(trip.startDate, 'MMM d')} - {safeFormat(trip.endDate, 'MMM d, yyyy')}
            </Typography>
          </Box>
          <IconButton
            onClick={() => setEditDialogOpen(true)}
            sx={{ color: 'white', bgcolor: 'rgba(255,255,255,0.2)', '&:hover': { bgcolor: 'rgba(255,255,255,0.4)' } }}
          >
            <Edit />
          </IconButton>
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
        <Typography variant="h5" color="primary" sx={{ fontWeight: 'bold', mb: 2 }}>Travelers</Typography>
        <Card variant="outlined" sx={{ p: 2, borderRadius: 3, display: 'flex', alignItems: 'center', bgcolor: 'background.paper' }}>
          <Stack direction="row" spacing={3} sx={{ overflowX: 'auto', pb: 1 }}>
            {trip.memberIds.map(uid => {
              const profile = profiles[uid];
              return (
                <Stack key={uid} alignItems="center" spacing={1} sx={{ minWidth: 64 }}>
                  <Avatar
                    src={profile?.photoUrl}
                    sx={{
                      bgcolor: 'primary.container',
                      color: 'primary.main',
                      width: 56,
                      height: 56,
                      border: '2px solid',
                      borderColor: 'primary.main'
                    }}
                  >
                    {!profile?.photoUrl && (profile?.displayName?.charAt(0) || <AccountCircle />)}
                  </Avatar>
                  <Typography variant="caption" sx={{ fontWeight: 'bold', textAlign: 'center' }}>
                    {profile?.displayName || 'Traveler'}
                  </Typography>
                </Stack>
              );
            })}
          </Stack>
        </Card>
      </Box>

      <Typography variant="h4" color="primary" sx={{ fontWeight: 'bold', mb: 2 }}>Itinerary</Typography>

      {/* Day List */}
      <Stack spacing={1} sx={{ pb: 8 }}>
        {days.map((day) => {
          const items = itemsByDate[day.date] || [];
          const firstHotel = items.find(i => i.type === ItemType.HOTEL);
          const isToday = isDateToday(parseISO(day.date));

          return (
            <Card
              key={day.date}
              variant="outlined"
              sx={{
                p: 2,
                borderRadius: 4,
                cursor: 'pointer',
                bgcolor: isToday ? 'action.selected' : 'background.paper',
                border: isToday ? '1px solid' : '1px solid',
                borderColor: isToday ? 'primary.container' : 'divider',
                '&:hover': { bgcolor: 'action.hover' }
              }}
              onClick={() => navigate(`/trip/${tripId}/day/${day.date}`)}
            >
              <Stack direction="row" alignItems="center" spacing={2} sx={{ height: 80 }}>
                {/* Date Side */}
                <Box sx={{ minWidth: 50, textAlign: 'center' }}>
                  <Typography variant="caption" sx={{ fontWeight: 'bold', color: isToday ? 'primary.main' : 'text.secondary' }}>
                    {safeFormat(day.date, 'EEE').toUpperCase()}
                  </Typography>
                  <Typography variant="h5" sx={{ fontWeight: 'bold', color: isToday ? 'primary.main' : 'text.primary' }}>
                    {safeFormat(day.date, 'd')}
                  </Typography>
                </Box>

                {/* Timeline Line/Dot */}
                <Box sx={{ height: '100%', position: 'relative', px: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                  <Box sx={{ flexGrow: 1, width: 2, bgcolor: 'divider' }} />
                  <Box
                    sx={{
                      width: isToday ? 12 : 8,
                      height: isToday ? 12 : 8,
                      borderRadius: '50%',
                      bgcolor: isToday ? 'secondary.container' : 'primary.container',
                      border: isToday ? '2px solid white' : 'none',
                      position: 'absolute',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      zIndex: 1,
                    }}
                  />
                  <Box sx={{ flexGrow: 1, width: 2, bgcolor: 'divider' }} />
                </Box>

                <Box sx={{ flexGrow: 1, ml: 1 }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: isToday ? 'primary.main' : 'text.primary' }}>
                    {firstHotel?.title || 'Free Day'}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
                    Day {day.dayIndex}
                  </Typography>

                  {/* Indicators */}
                  <Stack direction="row" spacing={0.5}>
                    {items.length > 0 ? (
                      items.slice(0, 10).map((item, idx) => (
                        <Box
                          key={`${item.id}-${idx}`}
                          sx={{
                            width: 16,
                            height: 4,
                            borderRadius: 1,
                            bgcolor: activityColors[item.type] || ITEM_TYPE_COLORS[item.type] || 'grey.300'
                          }}
                        />
                      ))
                    ) : (
                      <Box sx={{ width: 24, height: 4, borderRadius: 1, bgcolor: 'action.disabledBackground' }} />
                    )}
                  </Stack>
                </Box>

                <ArrowForwardIos fontSize="inherit" sx={{ color: 'text.disabled', fontSize: 12 }} />
              </Stack>
            </Card>
          );
        })}
      </Stack>

      {trip && (
        <TripEditDialog
          open={editDialogOpen}
          onClose={() => setEditDialogOpen(false)}
          trip={trip}
        />
      )}
    </Layout>
  );
};

export default TripDetailPage;
