import * as React from 'react';
import { useEffect, useState } from 'react';
import {
  Typography,
  Box,
  CircularProgress,
  Stack,
  Fab,
  IconButton,
  Paper,
} from '@mui/material';
import {
  Add,
  StickyNote2,
  WbSunny,
  ArrowBack,
  Cloud,
  Grain,
  Thunderstorm,
  AcUnit,
  Warning,
  LocationOn as LocationOnIcon,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import ItineraryItemRow from '../components/ItineraryItemRow';
import AddEditItemModal from '../components/AddEditItemModal';
import { tripService } from '../services/tripService';
import { authService } from '../services/authService';
import { weatherService } from '../services/weatherService';
import { ItineraryItem, TripDay, DailyWeather, WeatherStatus } from '../data/models';
import { format, parseISO, isValid } from 'date-fns';
import { onSnapshot, doc } from 'firebase/firestore';
import { db } from '../firebase';

const DayDetailPage: React.FC = () => {
  const { tripId, date } = useParams<{ tripId: string; date: string }>();
  const [items, setItems] = useState<ItineraryItem[]>([]);
  const [day, setDay] = useState<TripDay | null>(null);
  const [weather, setWeather] = useState<DailyWeather | null>(null);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<ItineraryItem | null>(null);

  const navigate = useNavigate();
  const user = authService.getCurrentUser();

  useEffect(() => {
    if (!tripId || !date) return;

    const unsubItems = tripService.observeItems(tripId, date, (data) => {
      setItems(data);
      setLoading(false);
    });

    const unsubDay = onSnapshot(doc(db, 'trips', tripId, 'days', date), (snapshot) => {
      if (snapshot.exists()) {
        setDay({ ...snapshot.data(), date: snapshot.id } as TripDay);
      }
    });

    return () => {
      unsubItems();
      unsubDay();
    };
  }, [tripId, date]);

  useEffect(() => {
    const fetchWeather = async () => {
      if (!date) return;

      // Find first item with coordinates
      const itemWithLoc = items.find(i => i.lat !== undefined && i.lng !== undefined);

      if (itemWithLoc) {
        const w = await weatherService.getDailyWeather(itemWithLoc.lat, itemWithLoc.lng, date);
        setWeather(w);
      } else {
        setWeather({ date, status: WeatherStatus.NO_LOCATION });
      }
    };

    if (items.length > 0) {
      fetchWeather();
    } else {
      setWeather({ date: date || '', status: WeatherStatus.NO_LOCATION });
    }
  }, [items, date]);

  const handleSaveItem = async (itemData: Partial<ItineraryItem>) => {
    if (!tripId || !date || !user) return;

    if (editingItem) {
      await tripService.updateItem(tripId, date, editingItem.id, itemData, user.uid);
    } else {
      const newItem = {
        ...itemData,
        tripId,
        createdBy: user.uid,
        updatedBy: user.uid,
        attachments: [],
        order: items.length,
      } as Omit<ItineraryItem, "id">;
      await tripService.addItem(tripId, date, newItem);
    }
    setEditingItem(null);
  };

  const handleDeleteItem = async (itemId: string) => {
    if (!tripId || !date) return;
    if (window.confirm('Are you sure you want to delete this activity?')) {
      await tripService.deleteItem(tripId, date, itemId);
    }
  };

  const safeFormat = (dateStr: string | undefined, formatStr: string) => {
    if (!dateStr) return 'N/A';
    const d = parseISO(dateStr);
    return isValid(d) ? format(d, formatStr) : 'N/A';
  };

  const getWeatherIcon = (code?: number) => {
    if (code === undefined) return <WbSunny sx={{ mr: 2 }} />;
    if (code === 0) return <WbSunny sx={{ mr: 2 }} />;
    if (code <= 3) return <Cloud sx={{ mr: 2 }} />;
    if (code <= 48) return <Cloud sx={{ mr: 2 }} />;
    if (code <= 67) return <Grain sx={{ mr: 2 }} />;
    if (code <= 77) return <AcUnit sx={{ mr: 2 }} />;
    if (code <= 82) return <Grain sx={{ mr: 2 }} />;
    if (code <= 86) return <AcUnit sx={{ mr: 2 }} />;
    if (code <= 99) return <Thunderstorm sx={{ mr: 2 }} />;
    return <WbSunny sx={{ mr: 2 }} />;
  };

  const getWeatherDescription = (code?: number) => {
    if (code === undefined) return 'Clear';
    if (code === 0) return 'Clear sky';
    if (code <= 3) return 'Partly cloudy';
    if (code <= 48) return 'Foggy';
    if (code <= 67) return 'Rainy';
    if (code <= 77) return 'Snowy';
    if (code <= 82) return 'Rain showers';
    if (code <= 86) return 'Snow showers';
    if (code <= 99) return 'Thunderstorm';
    return 'Clear';
  };

  const renderWeather = () => {
    if (!weather) return null;

    switch (weather.status) {
      case WeatherStatus.AVAILABLE:
        return (
          <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3, display: 'flex', alignItems: 'center', bgcolor: 'primary.container', color: 'primary.main' }}>
            {getWeatherIcon(weather.weatherCode)}
            <Box>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                {getWeatherDescription(weather.weatherCode)} · {Math.round(weather.tempMaxC || 0)}°C / {Math.round(weather.tempMinC || 0)}°C
              </Typography>
              <Typography variant="caption">
                {weather.precipitationProbabilityPct}% chance of precipitation
              </Typography>
            </Box>
          </Paper>
        );
      case WeatherStatus.NOT_YET_AVAILABLE:
        return (
          <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3, display: 'flex', alignItems: 'center', bgcolor: 'action.hover' }}>
            <WbSunny sx={{ mr: 2, color: 'text.disabled' }} />
            <Box>
              <Typography variant="subtitle2" color="text.secondary">Weather forecast not yet available</Typography>
              <Typography variant="caption" color="text.secondary">
                Forecast available from {weatherService.forecastAvailableFrom(date || '')}
              </Typography>
            </Box>
          </Paper>
        );
      case WeatherStatus.NO_LOCATION:
        return (
          <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3, display: 'flex', alignItems: 'center', bgcolor: 'action.hover' }}>
            <LocationOnIcon sx={{ mr: 2, color: 'text.disabled' }} />
            <Box>
              <Typography variant="subtitle2" color="text.secondary">No location set for weather</Typography>
              <Typography variant="caption" color="text.secondary">Add an activity with a location to see weather</Typography>
            </Box>
          </Paper>
        );
      case WeatherStatus.ERROR:
        return (
          <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3, display: 'flex', alignItems: 'center', bgcolor: 'error.container', color: 'error.main' }}>
            <Warning sx={{ mr: 2 }} />
            <Box>
              <Typography variant="subtitle2">Failed to load weather</Typography>
            </Box>
          </Paper>
        );
      default:
        return null;
    }
  };

  if (loading || !date) {
    return (
      <Layout title="Loading Day...">
        <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  const formattedDate = safeFormat(date, 'EEEE, MMM d, yyyy');

  return (
    <Layout title={formattedDate}>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 3 }}>
        <IconButton onClick={() => navigate(-1)}>
          <ArrowBack />
        </IconButton>
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 'bold' }}>
            ITINERARY
          </Typography>
          <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
            {formattedDate}
          </Typography>
        </Box>
      </Stack>

      {renderWeather()}

      {/* Day Note */}
      <Paper
        variant="outlined"
        sx={{
          p: 2,
          mb: 4,
          borderRadius: 3,
          display: 'flex',
          alignItems: 'flex-start',
          cursor: 'pointer',
          '&:hover': { bgcolor: 'action.hover' }
        }}
        onClick={() => {
          const newNote = prompt('Edit day note:', day?.dayNote || '');
          if (newNote !== null && tripId && date && user) {
            tripService.updateDayNote(tripId, date, newNote, user.uid);
          }
        }}
      >
        <StickyNote2 sx={{ mr: 2, color: 'text.secondary' }} />
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="body2" color={day?.dayNote ? "text.primary" : "text.secondary"}>
            {day?.dayNote || 'Add a note for this day...'}
          </Typography>
        </Box>
      </Paper>

      <Typography variant="h5" color="primary" sx={{ fontWeight: 'bold', mb: 2 }}>Schedule</Typography>

      {items.length > 0 ? (
        <Stack spacing={0} sx={{ pb: 12 }}>
          {items.map((item) => (
            <ItineraryItemRow
              key={item.id}
              item={item}
              canEdit={true}
              onEdit={() => {
                setEditingItem(item);
                setModalOpen(true);
              }}
              onDelete={() => handleDeleteItem(item.id)}
              onAddAttachment={() => {
                setEditingItem(item);
                setModalOpen(true);
              }}
            />
          ))}
        </Stack>
      ) : (
        <Box sx={{ py: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">Nothing planned yet for this day.</Typography>
        </Box>
      )}

      <Fab
        color="secondary"
        aria-label="add"
        sx={{ position: 'fixed', bottom: 32, right: 32 }}
        onClick={() => {
          setEditingItem(null);
          setModalOpen(true);
        }}
      >
        <Add />
      </Fab>

      <AddEditItemModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={handleSaveItem}
        existingItem={editingItem}
      />
    </Layout>
  );
};

export default DayDetailPage;
