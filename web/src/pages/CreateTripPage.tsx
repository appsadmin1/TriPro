import React, { useState } from 'react';
import {
  Typography,
  Box,
  TextField,
  Button,
  Paper,
  Stack,
  IconButton,
  Alert,
} from '@mui/material';
import { ArrowBack, PhotoCamera } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { tripService } from '../services/tripService';
import { authService } from '../services/authService';

const CreateTripPage: React.FC = () => {
  const [name, setName] = useState('');
  const [destination, setDestination] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();
  const user = authService.getCurrentUser();

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
      // For now we use a default cover image until we add Cloudinary upload to web
      const defaultCover = "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&q=80&w=2070";

      const tripId = await tripService.createTrip(
        name,
        destination,
        defaultCover,
        "", // publicId
        "", // resourceType
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
              />

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
    </Layout>
  );
};

export default CreateTripPage;
