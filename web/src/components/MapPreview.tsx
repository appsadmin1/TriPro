import * as React from 'react';
import { useEffect, useRef } from 'react';
import { Box, Typography } from '@mui/material';
import { Loader } from '@googlemaps/js-api-loader';

interface MapPin {
  title: string;
  lat: number;
  lng: number;
  color?: string;
}

interface MapPreviewProps {
  pins: MapPin[];
  height?: number | string;
}

const MapPreview: React.FC<MapPreviewProps> = ({ pins, height = 250 }) => {
  const mapRef = useRef<HTMLDivElement>(null);
  const googleMap = useRef<any>(null);
  const markersRef = useRef<any[]>([]);
  const [retryTrigger, setRetryTrigger] = React.useState(0);
  const [error, setError] = React.useState<string | null>(null);

  useEffect(() => {
    const apiKey = import.meta.env.VITE_MAPS_API_KEY;
    if (!apiKey) {
      setError("Maps API Key missing");
      return;
    }

    if (!mapRef.current) return;

    const loader = new Loader({
      apiKey: apiKey,
      version: 'weekly',
      libraries: ['places'],
    });

    let isMounted = true;

    loader.load().then(() => {
      if (!isMounted) return;

      const container = mapRef.current;
      if (!container || container.offsetParent === null) {
        // Not in DOM or hidden, retry in a bit
        setTimeout(() => {
          if (isMounted) setRetryTrigger(prev => prev + 1);
        }, 500);
        return;
      }

      const { Map, Marker, LatLngBounds } = (window as any).google.maps;

      try {
        if (!googleMap.current) {
          googleMap.current = new Map(mapRef.current, {
            center: { lat: 0, lng: 0 },
            zoom: 12,
            mapId: 'DEMO_MAP_ID' // Helpful for some features, but optional
          });
        }

        // Clear existing markers
        markersRef.current.forEach(m => m.setMap(null));
        markersRef.current = [];

        const bounds = new LatLngBounds();
        let validPinsCount = 0;

        pins.forEach(pin => {
          if (pin.lat == null || pin.lng == null || isNaN(pin.lat) || isNaN(pin.lng)) {
            console.warn("Skipping invalid pin:", pin);
            return;
          }

          const marker = new Marker({
            position: { lat: pin.lat, lng: pin.lng },
            map: googleMap.current,
            title: pin.title,
          });
          markersRef.current.push(marker);
          bounds.extend(marker.getPosition());
          validPinsCount++;
        });

        if (validPinsCount > 0) {
          googleMap.current.fitBounds(bounds);
          if (validPinsCount === 1) {
            googleMap.current.setZoom(15);
          }
        }
      } catch (err: any) {
        console.error("Error initializing Google Map:", err);
        setError("Error initializing map: " + err.message);
      }
    }).catch(err => {
      console.error("Google Maps load error", err);
      setError("Failed to load Google Maps");
    });

    return () => {
      isMounted = false;
    };
  }, [pins, retryTrigger]);

  return (
    <Box
      ref={mapRef}
      sx={{
        width: '100%',
        height,
        borderRadius: 4,
        overflow: 'hidden',
        border: '1px solid',
        borderColor: 'divider',
        mb: 3,
        bgcolor: 'action.hover',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}
    >
      {error && (
        <Box sx={{ p: 2, textAlign: 'center' }}>
          <Typography color="error" variant="body2">{error}</Typography>
          <Typography variant="caption" color="text.secondary">
            Check console for details or ensure "Maps JavaScript API" is enabled.
          </Typography>
        </Box>
      )}
    </Box>
  );
};

export default MapPreview;
