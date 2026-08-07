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
  Box,
  CircularProgress,
} from '@mui/material';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import DeleteIcon from '@mui/icons-material/Delete';
import PhotoCameraIcon from '@mui/icons-material/PhotoCamera';
import { Trip, PickedPlace } from '../data/models';
import { tripService } from '../services/tripService';
import { uploadAttachment } from '../services/cloudinaryService';
import { authService } from '../services/authService';
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
    coverImagePublicId: '',
    coverImageResourceType: '',
  });
  const [placeSearchOpen, setPlaceSearchOpen] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);

  useEffect(() => {
    if (trip) {
      setFormData({
        name: trip.name,
        destination: trip.destination,
        startDate: trip.startDate,
        endDate: trip.endDate,
        coverImageUrl: trip.coverImageUrl,
        coverImagePublicId: trip.coverImagePublicId || '',
        coverImageResourceType: trip.coverImageResourceType || '',
      });
    }
  }, [trip, open]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    const user = authService.getCurrentUser();
    if (!file || !user) return;

    setUploadingPhoto(true);
    try {
      const att = await uploadAttachment(file, user.uid);
      setFormData({
        ...formData,
        coverImageUrl: att.downloadUrl,
        coverImagePublicId: att.publicId,
        coverImageResourceType: att.resourceType,
      });
    } catch (err) {
      console.error(err);
      alert('Failed to upload cover photo');
    } finally {
      setUploadingPhoto(false);
    }
  };

  const handleSave = async () => {
    setLoading(true);
    try {
      await tripService.updateTripDetails(
        trip.id,
        formData.name,
        formData.destination,
        formData.coverImageUrl !== trip.coverImageUrl ? formData.coverImageUrl : null,
        formData.coverImagePublicId !== trip.coverImagePublicId ? formData.coverImagePublicId : null,
        formData.coverImageResourceType !== trip.coverImageResourceType ? formData.coverImageResourceType : null,
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

            <Box sx={{ mt: 1 }}>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Cover Photo</Typography>
              <Box
                sx={{
                  width: '100%',
                  height: 150,
                  borderRadius: 3,
                  border: '2px dashed',
                  borderColor: 'divider',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  bgcolor: 'action.hover',
                  overflow: 'hidden',
                  position: 'relative',
                  cursor: 'pointer'
                }}
                component="label"
              >
                {formData.coverImageUrl ? (
                  <img src={formData.coverImageUrl} alt="Cover" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                ) : (
                  <>
                    {uploadingPhoto ? <CircularProgress /> : <PhotoCameraIcon sx={{ fontSize: 32, color: 'text.secondary', mb: 1 }} />}
                    <Typography variant="body2" color="text.secondary">
                      {uploadingPhoto ? 'Uploading...' : 'Click to change cover photo'}
                    </Typography>
                  </>
                )}
                <input type="file" hidden accept="image/*" onChange={handlePhotoUpload} />
              </Box>
            </Box>
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
