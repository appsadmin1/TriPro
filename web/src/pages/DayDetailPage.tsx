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
  Edit,
  Close,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import ItineraryItemRow from '../components/ItineraryItemRow';
import AddEditItemModal from '../components/AddEditItemModal';
import MapPreview from '../components/MapPreview';
import AttachmentViewerDialog from '../components/AttachmentViewerDialog';
import { tripService } from '../services/tripService';
import { userService } from '../services/userService';
import { authService } from '../services/authService';
import { weatherService } from '../services/weatherService';
import { activityService } from '../services/activityService';
import { ITEM_TYPE_COLORS } from '../utils/colorUtils';
import { groupByHierarchy } from '../utils/itineraryUtils';
import { ItineraryItem, TripDay, DailyWeather, WeatherStatus, Attachment, ActivityType, Trip } from '../data/models';
import { format, parseISO, isValid } from 'date-fns';
import { he } from 'date-fns/locale';
import { onSnapshot, doc } from 'firebase/firestore';
import { db } from '../firebase';
import { useTranslation } from 'react-i18next';

const DayDetailPage: React.FC = () => {
  const { tripId, date } = useParams<{ tripId: string; date: string }>();
  const [items, setItems] = useState<ItineraryItem[]>([]);
  const [day, setDay] = useState<TripDay | null>(null);
  const [trip, setTrip] = useState<Trip | null>(null);
  const [weather, setWeather] = useState<DailyWeather | null>(null);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<ItineraryItem | null>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [canEdit, setCanEdit] = useState(false);
  const [activityColors, setActivityColors] = useState<Record<string, string>>({});
  const [viewingAttachment, setViewingAttachment] = useState<{ itemId: string, att: Attachment } | null>(null);
  const { t, i18n } = useTranslation();

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

    const unsubTrip = tripService.observeTrip(tripId, (trip) => {
      setTrip(trip);
      if (trip && user) {
        const role = trip.members[user.uid];
        setCanEdit(role === 'owner' || role === 'editor');
      }
    });

    const unsubColors = user ? userService.observeActivityColors(user.uid, (colors) => {
      setActivityColors(colors);
    }) : () => {};

    return () => {
      unsubItems();
      unsubDay();
      unsubTrip();
      unsubColors();
    };
  }, [tripId, date, user]);

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
    if (!tripId || !date || !user || !trip) return;

    if (editingItem) {
      await tripService.updateItem(tripId, date, editingItem.id, itemData, user.uid);
      activityService.logActivity(tripId, trip.name, trip.memberIds, ActivityType.ITEM_UPDATED, `${user.displayName || 'Traveler'} updated "${itemData.title || editingItem.title}" on ${date}`, user.uid, user.displayName || 'Traveler', date);
    } else {
      const nextOrder = items.length > 0 ? Math.max(...items.map(i => i.order || 0)) + 1 : 0;
      const newItem = {
        ...itemData,
        tripId,
        createdBy: user.uid,
        updatedBy: user.uid,
        attachments: [],
        order: nextOrder,
      } as Omit<ItineraryItem, "id">;
      await tripService.addItem(tripId, date, newItem);
      activityService.logActivity(tripId, trip.name, trip.memberIds, ActivityType.ITEM_ADDED, `${user.displayName || 'Traveler'} added "${newItem.title}" to the itinerary on ${date}`, user.uid, user.displayName || 'Traveler', date);
    }
    setEditingItem(null);
  };

  const handleDeleteItem = async (itemId: string) => {
    if (!tripId || !date || !user || !trip) return;
    const title = items.find(i => i.id === itemId)?.title || 'An item';
    if (window.confirm(t('itinerary_delete_confirm_text', { defaultValue: 'Are you sure you want to delete this activity?' }))) {
      await tripService.deleteItem(tripId, date, itemId);
      activityService.logActivity(tripId, trip.name, trip.memberIds, ActivityType.ITEM_REMOVED, `${user.displayName || 'Traveler'} removed "${title}" from the itinerary on ${date}`, user.uid, user.displayName || 'Traveler', date);
    }
  };

  const handleMoveItem = async (itemId: string, direction: number) => {
    if (!tripId || !date) return;
    const currentIndex = items.findIndex(i => i.id === itemId);
    if (currentIndex === -1) return;

    const targetIndex = currentIndex + direction;
    if (targetIndex >= 0 && targetIndex < items.length) {
      const item1 = items[currentIndex];
      const item2 = items[targetIndex];

      if (getEffectivePeriod(item1) === getEffectivePeriod(item2)) {
        // Swap using indices to ensure distinct values
        await tripService.swapItemOrders(tripId, date, item1.id, targetIndex, item2.id, currentIndex);
      }
    }
  };

  // Helper for handleMoveItem logic
  function getEffectivePeriod(it: ItineraryItem): string {
    if (it.timeType === "PERIOD") return it.period || "MORNING";
    const parts = it.startTime?.split(":") || [];
    const hour = parseInt(parts[0], 10) || 0;
    if (hour >= 0 && hour <= 11) return "MORNING";
    if (hour >= 12 && hour <= 13) return "NOON";
    if (hour >= 14 && hour <= 17) return "AFTERNOON";
    if (hour >= 18 && hour <= 21) return "EVENING";
    return "NIGHT";
  }

  function toMinutes(hhmm: string): number {
    const [h, m] = hhmm.split(":").map(Number);
    return (h || 0) * 60 + (m || 0);
  }

  const safeFormat = (dateStr: string | undefined, formatStr: string) => {
    if (!dateStr) return 'N/A';
    const d = parseISO(dateStr);
    const locale = i18n.language.startsWith('he') ? he : undefined;
    return isValid(d) ? format(d, formatStr, { locale }) : 'N/A';
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
    if (code === undefined) return t('weather_clear');
    if (code === 0) return t('weather_clear_sky');
    if (code <= 3) return t('weather_partly_cloudy');
    if (code <= 48) return t('weather_foggy');
    if (code <= 67) return t('weather_rainy');
    if (code <= 77) return t('weather_snowy');
    if (code <= 82) return t('weather_rain_showers');
    if (code <= 86) return t('weather_snow_showers');
    if (code <= 99) return t('weather_thunderstorm');
    return t('weather_clear');
  };

  const renderWeather = () => {
    if (!weather) return null;

    switch (weather.status) {
      case WeatherStatus.AVAILABLE:
        return (
          <Paper
            variant="outlined"
            sx={{
              p: 2,
              mb: 3,
              borderRadius: 3,
              display: 'flex',
              alignItems: 'center',
              bgcolor: 'secondary.container',
              color: 'secondary.onContainer',
              border: 'none'
            }}
          >
            {getWeatherIcon(weather.weatherCode)}
            <Box>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                {getWeatherDescription(weather.weatherCode)} · {Math.round(weather.tempMaxC || 0)}°C / {Math.round(weather.tempMinC || 0)}°C
              </Typography>
              <Typography variant="caption">
                {t('weather_chance_rain', { percent: weather.precipitationProbabilityPct })}
              </Typography>
            </Box>
          </Paper>
        );
      case WeatherStatus.NOT_YET_AVAILABLE:
        return (
          <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3, display: 'flex', alignItems: 'center', bgcolor: 'action.hover' }}>
            <WbSunny sx={{ mr: 2, color: 'text.disabled' }} />
            <Box>
              <Typography variant="subtitle2" color="text.secondary">{t('weather_forecast_not_available')}</Typography>
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
              <Typography variant="subtitle2" color="text.secondary">{t('weather_no_location')}</Typography>
              <Typography variant="caption" color="text.secondary">{t('weather_add_location')}</Typography>
            </Box>
          </Paper>
        );
      case WeatherStatus.ERROR:
        return (
          <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3, display: 'flex', alignItems: 'center', bgcolor: 'error.container', color: 'error.main' }}>
            <Warning sx={{ mr: 2 }} />
            <Box>
              <Typography variant="subtitle2">{t('weather_failed')}</Typography>
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

  const pins = items
    .filter(i => i.lat != null && i.lng != null && !isNaN(Number(i.lat)) && !isNaN(Number(i.lng)))
    .map(i => ({
      title: i.title,
      lat: Number(i.lat),
      lng: Number(i.lng),
      color: activityColors[i.type] || ITEM_TYPE_COLORS[i.type]
    }));

  const editingAllowed = canEdit && isEditMode;

  return (
    <Layout title={formattedDate}>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 3 }}>
        <IconButton onClick={() => navigate(-1)}>
          <ArrowBack />
        </IconButton>
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 'bold' }}>
            {day ? t('day_label', { index: day.dayIndex }) : t('trip_itinerary')}
          </Typography>
          <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
            {formattedDate}
          </Typography>
        </Box>
        {canEdit && (
          <IconButton onClick={() => setIsEditMode(!isEditMode)} color={isEditMode ? "error" : "primary"}>
            {isEditMode ? <Close /> : <Edit />}
          </IconButton>
        )}
      </Stack>

      {renderWeather()}

      {pins.length > 0 && <MapPreview pins={pins} />}

      {/* Day Note */}
      <Paper
        variant="outlined"
        sx={{
          p: 2,
          mb: 4,
          borderRadius: 3,
          display: 'flex',
          alignItems: 'flex-start',
          cursor: editingAllowed ? 'pointer' : 'default',
          '&:hover': editingAllowed ? { bgcolor: 'action.hover' } : {}
        }}
        onClick={() => {
          if (!editingAllowed) return;
          const newNote = prompt(t('edit_day_note', { defaultValue: 'Edit day note:' }), day?.dayNote || '');
          if (newNote !== null && tripId && date && user && trip) {
            tripService.updateDayNote(tripId, date, newNote, user.uid);
            activityService.logActivity(tripId, trip.name, trip.memberIds, ActivityType.DAY_NOTE_UPDATED, `${user.displayName || 'Traveler'} updated the day note for ${date}`, user.uid, user.displayName || 'Traveler', date);
          }
        }}
      >
        <StickyNote2 sx={{ mr: 2, color: 'text.secondary' }} />
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="body2" color={day?.dayNote ? "text.primary" : "text.secondary"}>
            {day?.dayNote || (editingAllowed ? t('add_day_note', { defaultValue: 'Add a note for this day...' }) : '')}
          </Typography>
        </Box>
        {editingAllowed && <Edit fontSize="small" color="action" />}
      </Paper>

      <Typography variant="h5" color="primary" sx={{ fontWeight: 'bold', mb: 2 }}>{t('schedule')}</Typography>

      {items.length > 0 ? (
        <Stack spacing={0} sx={{ pb: 12 }}>
          {groupByHierarchy(items).map((periodGroup) => (
            <Box key={periodGroup.period}>
              <Typography variant="h5" color="primary" sx={{ fontWeight: 'bold', mt: 6, mb: 1, textTransform: 'uppercase' }}>
                {t(periodGroup.period.toLowerCase())}
              </Typography>

              {periodGroup.timeGroups.map((timeGroup, tgIdx) => (
                <Box key={`${periodGroup.period}-${tgIdx}`}>
                  {timeGroup.label && (
                    <Typography variant="subtitle1" color="text.secondary" sx={{ fontWeight: 'bold', mt: 2, mb: 1 }}>
                      {timeGroup.label}
                    </Typography>
                  )}
                  {timeGroup.items.map((item) => (
                    <ItineraryItemRow
                      key={item.id}
                      item={item}
                      canEdit={editingAllowed}
                      activityColors={activityColors}
                      onEdit={() => {
                        setEditingItem(item);
                        setModalOpen(true);
                      }}
                      onDelete={() => handleDeleteItem(item.id)}
                      onAddAttachment={() => {
                        setEditingItem(item);
                        setModalOpen(true);
                      }}
                      onAttachmentClick={(att) => setViewingAttachment({ itemId: item.id, att })}
                      onMoveUp={() => handleMoveItem(item.id, -1)}
                      onMoveDown={() => handleMoveItem(item.id, 1)}
                    />
                  ))}
                </Box>
              ))}
            </Box>
          ))}
        </Stack>
      ) : (
        <Box sx={{ py: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">{t('nothing_planned')}</Typography>
        </Box>
      )}

      {editingAllowed && (
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
      )}

      <AddEditItemModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={handleSaveItem}
        existingItem={editingItem}
      />

      <AttachmentViewerDialog
        open={!!viewingAttachment}
        attachment={viewingAttachment?.att || null}
        onClose={() => setViewingAttachment(null)}
        onRemove={editingAllowed ? () => {
          if (viewingAttachment && tripId && date) {
            tripService.removeAttachment(tripId, date, viewingAttachment.itemId, viewingAttachment.att.id);
          }
        } : undefined}
        onRename={editingAllowed ? (newName) => {
          if (viewingAttachment && tripId && date) {
            tripService.renameAttachment(tripId, date, viewingAttachment.itemId, viewingAttachment.att.id, newName);
          }
        } : undefined}
      />
    </Layout>
  );
};

export default DayDetailPage;
