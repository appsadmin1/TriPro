import * as React from 'react';
import { useState } from 'react';
import {
  Typography,
  Box,
  TextField,
  Button,
  Paper,
  Stack,
  IconButton,
  Alert,
  CircularProgress,
} from '@mui/material';
import { ArrowBack, PhotoCamera, LocationOn } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { tripService } from '../services/tripService';
import { authService } from '../services/authService';
import { uploadAttachment } from '../services/cloudinaryService';
import PlaceSearchDialog from '../components/PlaceSearchDialog';
import { PickedPlace } from '../data/models';

const CreateTripPage: React.FC = () => {
  const [name, setName] = useState('');
  const [destination, setDestination] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [coverPhoto, setCoverPhoto] = useState<{ url: string; publicId: string; resourceType: string } | null>(null);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [placeSearchOpen, setPlaceSearchOpen] = useState(false);

  const navigate = useNavigate();
  const user = authService.getCurrentUser();

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !user) return;

    setUploadingPhoto(true);
    try {
      const att = await uploadAttachment(file, user.uid);
      setCoverPhoto({
        url: att.downloadUrl,
        publicId: att.publicId,
        resourceType: att.resourceType
      });
    } catch (err) {
      console.error(err);
      setError('Failed to upload cover photo');
    } finally {
      setUploadingPhoto(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    if (!name || !destination || !startDate || !endDate) {
      setError('Please fill in all fields');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const finalCoverUrl = coverPhoto?.url || "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&q=80&w=2070";
      const finalPublicId = coverPhoto?.publicId || "";
      const finalResourceType = coverPhoto?.resourceType || "";

      const tripId = await tripService.createTrip(
        name,
        destination,
        finalCoverUrl,
        finalPublicId,
        finalResourceType,
        startDate,
        endDate,
        user.uid,
        user.displayName || 'Traveler'
      );

      navigate(`/trip/${tripId}`);
    } catch (err: any) {
      console.error(err);
      setError(err.message || 'Failed to create trip');
    } finally {
      setLoading(false);
    }
  };

  const handlePlacePicked = (place: PickedPlace) => {
    setDestination(place.name);
  };

  return (
    <Layout title="Plan New Adventure">
      <Box sx={{ maxWidth: 600, mx: 'auto', mt: 4 }}>
        <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 4 }}>
          <IconButton onClick={() => navigate(-1)}>
            <ArrowBack />
          </IconButton>
          <Typography variant="h4" sx={{ fontWeight: 'bold' }}>Plan New Trip</Typography>
        </Stack>

        <Paper sx={{ p: 4, borderRadius: 4 }}>
          <form onSubmit={handleCreate}>
            <Stack spacing={3}>
              <TextField
                label="Trip Name"
                placeholder="e.g. Summer in Japan"
                fullWidth
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
              <TextField
                label="Destination"
                placeholder="e.g. Tokyo, Kyoto"
                fullWidth
                value={destination}
                onChange={(e) => setDestination(e.target.value)}
                required
                InputProps={{
                  endAdornment: (
                    <IconButton onClick={() => setPlaceSearchOpen(true)}>
                      <LocationOn />
                    </IconButton>
                  )
                }}
              />

              <Box sx={{ mt: 2 }}>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>Cover Photo</Typography>
                <Box
                  sx={{
                    width: '100%',
                    height: 200,
                    borderRadius: 4,
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
                  {coverPhoto ? (
                    <img src={coverPhoto.url} alt="Cover" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : (
                    <>
                      {uploadingPhoto ? <CircularProgress /> : <PhotoCamera sx={{ fontSize: 40, color: 'text.secondary', mb: 1 }} />}
                      <Typography variant="body2" color="text.secondary">
                        {uploadingPhoto ? 'Uploading...' : 'Click to upload from device'}
                      </Typography>
                    </>
                  )}
                  <input type="file" hidden accept="image/*" onChange={handlePhotoUpload} />
                </Box>
              </Box>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label="Start Date"
                  type="date"
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  required
                />
                <TextField
                  label="End Date"
                  type="date"
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  required
                />
              </Stack>

              {error && <Alert severity="error">{error}</Alert>}

              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={loading}
                sx={{
                  py: 1.5,
                  borderRadius: 10,
                  fontWeight: 'bold',
                  boxShadow: 2
                }}
              >
                {loading ? 'Creating...' : 'Create Trip'}
              </Button>
            </Stack>
          </form>
        </Paper>
      </Box>

      <PlaceSearchDialog
        open={placeSearchOpen}
        onClose={() => setPlaceSearchOpen(false)}
        onPlacePicked={handlePlacePicked}
        title="Search for Destination"
      />
    </Layout>
  );
};

export default CreateTripPage;
