import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Stack,
  IconButton,
  InputAdornment,
  Typography,
} from '@mui/material';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import DeleteIcon from '@mui/icons-material/Delete';
import { Trip, PickedPlace } from '../data/models';
import { tripService } from '../services/tripService';
import PlaceSearchDialog from './PlaceSearchDialog';

interface TripEditDialogProps {
  open: boolean;
  onClose: () => void;
  trip: Trip;
}

const TripEditDialog: React.FC<TripEditDialogProps> = ({ open, onClose, trip }) => {
  const [formData, setFormData] = useState({
    name: '',
    destination: '',
    startDate: '',
    endDate: '',
    coverImageUrl: '',
  });
  const [placeSearchOpen, setPlaceSearchOpen] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (trip) {
      setFormData({
        name: trip.name,
        destination: trip.destination,
        startDate: trip.startDate,
        endDate: trip.endDate,
        coverImageUrl: trip.coverImageUrl,
      });
    }
  }, [trip, open]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSave = async () => {
    setLoading(true);
    try {
      await tripService.updateTripDetails(
        trip.id,
        formData.name,
        formData.destination,
        formData.coverImageUrl !== trip.coverImageUrl ? formData.coverImageUrl : null,
        null, // publicId
        null, // resourceType
        formData.startDate,
        formData.endDate
      );
      onClose();
    } catch (error) {
      console.error('Failed to update trip', error);
      alert('Failed to update trip details.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    setLoading(true);
    try {
      await tripService.deleteTrip(trip.id);
      onClose();
      // Optionally redirect to dashboard
      window.location.href = '/';
    } catch (error) {
      console.error('Failed to delete trip', error);
      alert('Failed to delete trip.');
    } finally {
      setLoading(false);
    }
  };

  const handlePlacePicked = (place: PickedPlace) => {
    setFormData({ ...formData, destination: place.name });
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
        <DialogTitle>Edit Trip</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              name="name"
              label="Trip Name"
              fullWidth
              value={formData.name}
              onChange={handleChange}
              required
            />
            <TextField
              name="destination"
              label="Destination"
              fullWidth
              value={formData.destination}
              onChange={handleChange}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => setPlaceSearchOpen(true)}>
                      <LocationOnIcon />
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            <Stack direction="row" spacing={2}>
              <TextField
                name="startDate"
                label="Start Date"
                type="date"
                fullWidth
                value={formData.startDate}
                onChange={handleChange}
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                name="endDate"
                label="End Date"
                type="date"
                fullWidth
                value={formData.endDate}
                onChange={handleChange}
                InputLabelProps={{ shrink: true }}
              />
            </Stack>
            <TextField
              name="coverImageUrl"
              label="Cover Image URL"
              fullWidth
              value={formData.coverImageUrl}
              onChange={handleChange}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ justifyContent: 'space-between', px: 3, py: 2 }}>
          <Button
            startIcon={<DeleteIcon />}
            color="error"
            onClick={() => setDeleteConfirmOpen(true)}
            disabled={loading}
          >
            Delete Trip
          </Button>
          <Stack direction="row" spacing={1}>
            <Button onClick={onClose} disabled={loading}>
              Cancel
            </Button>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={loading || !formData.name || !formData.startDate || !formData.endDate}
            >
              {loading ? 'Saving...' : 'Save Changes'}
            </Button>
          </Stack>
        </DialogActions>
      </Dialog>

      <PlaceSearchDialog
        open={placeSearchOpen}
        onClose={() => setPlaceSearchOpen(false)}
        onPlacePicked={handlePlacePicked}
        title="Search for Destination"
      />

      <Dialog open={deleteConfirmOpen} onClose={() => setDeleteConfirmOpen(false)}>
        <DialogTitle>Delete Trip?</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to delete "{trip.name}"? This action cannot be undone.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteConfirmOpen(false)}>Cancel</Button>
          <Button onClick={handleDelete} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default TripEditDialog;
