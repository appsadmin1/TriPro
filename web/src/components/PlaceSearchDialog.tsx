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
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const [predictions, setPredictions] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const placesLibrary = useRef<any>(null);
  const sessionToken = useRef<any>(null);

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

    loader.importLibrary('places').then((library) => {
      placesLibrary.current = library;
      sessionToken.current = new library.AutocompleteSessionToken();
    }).catch(err => {
      console.error('Failed to load Google Maps API', err);
      setError('Failed to load Google Maps API');
    });
  }, [open]);

  useEffect(() => {
    if (!query || query.length < 2 || !placesLibrary.current) {
      setPredictions([]);
      return;
    }

    const fetchSuggestions = async () => {
      try {
        setLoading(true);
        const { AutocompleteSuggestion } = placesLibrary.current;
        const { suggestions } = await AutocompleteSuggestion.fetchAutocompleteSuggestions({
          input: query,
          sessionToken: sessionToken.current,
        });
        setPredictions(suggestions || []);
      } catch (err) {
        console.error('Autocomplete error:', err);
        setPredictions([]);
      } finally {
        setLoading(false);
      }
    };

    const timeoutId = setTimeout(fetchSuggestions, 300);

    return () => clearTimeout(timeoutId);
  }, [query]);

  const handleSelectPlace = async (suggestion: any) => {
    if (!placesLibrary.current) return;

    try {
      setLoading(true);
      const { Place } = placesLibrary.current;
      const place = suggestion.placePrediction.toPlace();

      await place.fetchFields({
        fields: ['displayName', 'formattedAddress', 'location', 'id'],
      });

      if (place.location) {
        onPlacePicked({
          name: place.displayName || '',
          address: place.formattedAddress || '',
          lat: place.location.lat(),
          lng: place.location.lng(),
          placeId: place.id,
        });
        onClose();
      } else {
        setError('Failed to get place details');
      }
    } catch (err) {
      console.error('Place details error:', err);
      setError('Failed to get place details');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent dividers>
        <TextField
          autoFocus
          fullWidth
          variant="outlined"
          placeholder={t('places_search_label', { defaultValue: 'Start typing...' })}
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
          {predictions.map((suggestion) => (
            <ListItem
              button
              key={suggestion.placePrediction.placeId}
              onClick={() => handleSelectPlace(suggestion)}
            >
              <ListItemIcon>
                <LocationOnIcon color="primary" />
              </ListItemIcon>
              <ListItemText
                primary={suggestion.placePrediction.text.text}
              />
            </ListItem>
          ))}
        </List>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('action_cancel')}</Button>
      </DialogActions>
    </Dialog>
  );
};

export default PlaceSearchDialog;
