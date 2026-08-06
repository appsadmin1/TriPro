import React, { useEffect, useState } from 'react';
import {
  Typography,
  Box,
  CircularProgress,
  Stack,
  Fab,
  IconButton,
  Paper,
  Divider,
} from '@mui/material';
import {
  Add,
  StickyNote2,
  WbSunny,
  ArrowBack,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import ItineraryItemRow from '../components/ItineraryItemRow';
import AddEditItemModal from '../components/AddEditItemModal';
import { tripService } from '../services/tripService';
import { authService } from '../services/authService';
import { ItineraryItem, TripDay } from '../data/models';
import { format, parseISO, isValid } from 'date-fns';

const DayDetailPage: React.FC = () => {
  const { tripId, date } = useParams<{ tripId: string; date: string }>();
  const [items, setItems] = useState<ItineraryItem[]>([]);
  const [day, setDay] = useState<TripDay | null>(null);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<ItineraryItem | null>(null);

  const navigate = useNavigate();
  const user = authService.getCurrentUser();

  useEffect(() => {
    if (!tripId || !date) return;

    // In a real app, we'd fetch the specific day info from a 'days' collection or filter from trip
    // tripService.observeItems already provides the items for this day
    const unsubItems = tripService.observeItems(tripId, date, (data) => {
      setItems(data);
      setLoading(false);
    });

    return () => unsubItems();
  }, [tripId, date]);

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
        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 'bold' }}>
            ITINERARY
          </Typography>
          <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
            {formattedDate}
          </Typography>
        </Box>
      </Stack>

      {/* Weather Placeholder */}
      <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3, display: 'flex', alignItems: 'center', bgcolor: 'primary.container', color: 'primary.main' }}>
        <WbSunny sx={{ mr: 2 }} />
        <Box>
          <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Sunny · 24°C</Typography>
          <Typography variant="caption">Clear sky throughout the day</Typography>
        </Box>
      </Paper>

      {/* Day Note */}
      <Paper variant="outlined" sx={{ p: 2, mb: 4, borderRadius: 3, display: 'flex', alignItems: 'flex-start' }}>
        <StickyNote2 sx={{ mr: 2, color: 'text.secondary' }} />
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="body2" color="text.secondary">
            {day?.dayNote || 'Add a note for this day...'}
          </Typography>
        </Box>
      </Paper>

      <Typography variant="h5" color="primary" sx={{ fontWeight: 'bold', mb: 2 }}>Schedule</Typography>

      {items.length > 0 ? (
        <Stack spacing={0}>
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
              onAddAttachment={() => {}} // TODO: Implement attachment upload
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
