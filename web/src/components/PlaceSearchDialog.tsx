import * as React from 'react';
import { useState, useEffect, useRef } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  CircularProgress,
  Typography,
  Box,
} from '@mui/material';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import SearchIcon from '@mui/icons-material/Search';
import { Loader } from '@googlemaps/js-api-loader';
import { PickedPlace } from '../data/models';

declare const google: any;

interface PlaceSearchDialogProps {
  open: boolean;
  onClose: () => void;
  onPlacePicked: (place: PickedPlace) => void;
  title?: string;
}

const PlaceSearchDialog: React.FC<PlaceSearchDialogProps> = ({
  open,
  onClose,
  onPlacePicked,
  title = 'Search for a place',
}) => {
  const [query, setQuery] = useState('');
  const [predictions, setPredictions] = useState<google.maps.places.AutocompletePrediction[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const autocompleteService = useRef<google.maps.places.AutocompleteService | null>(null);
  const placesService = useRef<google.maps.places.PlacesService | null>(null);
  const sessionToken = useRef<google.maps.places.AutocompleteSessionToken | null>(null);

  useEffect(() => {
    if (!open) {
      setQuery('');
      setPredictions([]);
      return;
    }

    const apiKey = import.meta.env.VITE_MAPS_API_KEY;
    if (!apiKey) {
      setError('Google Maps API Key is missing. Please check your configuration.');
      return;
    }

    const loader = new Loader({
      apiKey: apiKey,
      version: 'weekly',
      libraries: ['places'],
    });

    loader.load().then(() => {
      autocompleteService.current = new google.maps.places.AutocompleteService();
      sessionToken.current = new google.maps.places.AutocompleteSessionToken();
      // PlacesService requires a DOM element, but we only use getDetails
      const dummyDiv = document.createElement('div');
      placesService.current = new google.maps.places.PlacesService(dummyDiv);
    }).catch(err => {
      console.error('Failed to load Google Maps API', err);
      setError('Failed to load Google Maps API');
    });
  }, [open]);

  useEffect(() => {
    if (!query || query.length < 2 || !autocompleteService.current) {
      setPredictions([]);
      return;
    }

    const timeoutId = setTimeout(() => {
      setLoading(true);
      autocompleteService.current?.getPlacePredictions(
        {
          input: query,
          sessionToken: sessionToken.current || undefined,
        },
        (results, status) => {
          setLoading(false);
          if (status === google.maps.places.PlacesServiceStatus.OK && results) {
            setPredictions(results);
          } else {
            setPredictions([]);
          }
        }
      );
    }, 300);

    return () => clearTimeout(timeoutId);
  }, [query]);

  const handleSelectPlace = (placeId: string) => {
    if (!placesService.current) return;

    setLoading(true);
    placesService.current.getDetails(
      {
        placeId,
        fields: ['name', 'formatted_address', 'geometry', 'place_id'],
        sessionToken: sessionToken.current || undefined,
      },
      (place, status) => {
        setLoading(false);
        if (status === google.maps.places.PlacesServiceStatus.OK && place && place.geometry?.location) {
          onPlacePicked({
            name: place.name || '',
            address: place.formatted_address || '',
            lat: place.geometry.location.lat(),
            lng: place.geometry.location.lng(),
            placeId: place.place_id,
          });
          onClose();
        } else {
          setError('Failed to get place details');
        }
      }
    );
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent dividers>
        <TextField
          autoFocus
          fullWidth
          variant="outlined"
          placeholder="Start typing..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          InputProps={{
            startAdornment: <SearchIcon color="action" sx={{ mr: 1 }} />,
          }}
        />

        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
            <CircularProgress size={24} />
          </Box>
        )}

        {error && (
          <Typography color="error" variant="body2" sx={{ mt: 1 }}>
            {error}
          </Typography>
        )}

        <List>
          {predictions.map((prediction) => (
            <ListItem
              button
              key={prediction.place_id}
              onClick={() => handleSelectPlace(prediction.place_id)}
            >
              <ListItemIcon>
                <LocationOnIcon color="primary" />
              </ListItemIcon>
              <ListItemText
                primary={prediction.structured_formatting.main_text}
                secondary={prediction.structured_formatting.secondary_text}
              />
            </ListItem>
          ))}
        </List>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
      </DialogActions>
    </Dialog>
  );
};

export default PlaceSearchDialog;
