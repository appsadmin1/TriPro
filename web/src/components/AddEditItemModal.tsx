import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  MenuItem,
  Stack,
  FormControl,
  InputLabel,
  Select,
  IconButton,
  InputAdornment,
  Divider,
  Typography,
} from '@mui/material';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import FlightTakeoffIcon from '@mui/icons-material/FlightTakeoff';
import { ItineraryItem, ItemType, TimeType, NoteType, DayPeriod, PickedPlace } from '../data/models';
import { flightService } from '../services/flightService';
import PlaceSearchDialog from './PlaceSearchDialog';

interface AddEditItemModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (item: Partial<ItineraryItem>) => void;
  existingItem?: ItineraryItem | null;
}

const AddEditItemModal: React.FC<AddEditItemModalProps> = ({
  open,
  onClose,
  onSave,
  existingItem,
}) => {
  const [formData, setFormData] = useState<Partial<ItineraryItem>>({
    title: '',
    type: ItemType.ACTIVITY,
    timeType: TimeType.EXACT,
    locationName: '',
    address: '',
    note: '',
    noteType: NoteType.NOTE,
    attachments: [],
  });

  const [placeSearchOpen, setPlaceSearchOpen] = useState(false);
  const [placeSearchTarget, setPlaceSearchTarget] = useState<'main' | 'hotel' | 'departure' | 'arrival'>('main');

  useEffect(() => {
    if (existingItem) {
      setFormData(existingItem);
    } else {
      setFormData({
        title: '',
        type: ItemType.ACTIVITY,
        timeType: TimeType.EXACT,
        locationName: '',
        address: '',
        note: '',
        noteType: NoteType.NOTE,
        attachments: [],
      });
    }
  }, [existingItem, open]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    if (name.includes('.')) {
      const [parent, child] = name.split('.');
      setFormData({
        ...formData,
        [parent]: {
          ...(formData[parent as keyof ItineraryItem] as any || {}),
          [child]: value,
        },
      });
    } else {
      setFormData({ ...formData, [name]: value });
    }
  };

  const handleSelectChange = (e: any) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = () => {
    onSave(formData);
    onClose();
  };

  const handleLookupFlight = async () => {
    if (!formData.flightInfo?.flightNumber) return;
    try {
      // For flight lookup, we might need a date. For now use a dummy or get it from props if available.
      // Assuming existingItem might have a date context or we just use today for lookup.
      const result = await flightService.lookupFlight(formData.flightInfo.flightNumber, new Date().toISOString().split('T')[0]);
      setFormData({
        ...formData,
        title: `${result.airline} ${result.flightNumber}`,
        startTime: result.departureTime,
        endTime: result.arrivalTime,
        flightInfo: {
          ...formData.flightInfo,
          airline: result.airline,
          departureAirportCode: result.departureAirportCode,
          arrivalAirportCode: result.arrivalAirportCode,
          departureTime: result.departureTime,
          arrivalTime: result.arrivalTime,
          departureAirportLat: result.departureAirportLat,
          departureAirportLng: result.departureAirportLng,
          arrivalAirportLat: result.arrivalAirportLat,
          arrivalAirportLng: result.arrivalAirportLng,
        }
      });
    } catch (error) {
      console.error('Flight lookup failed', error);
      alert('Flight lookup failed. Please enter details manually.');
    }
  };

  const openPlaceSearch = (target: 'main' | 'hotel' | 'departure' | 'arrival') => {
    setPlaceSearchTarget(target);
    setPlaceSearchOpen(true);
  };

  const handlePlacePicked = (place: PickedPlace) => {
    if (placeSearchTarget === 'main') {
      setFormData({
        ...formData,
        locationName: place.name,
        address: place.address,
        lat: place.lat,
        lng: place.lng,
      });
    } else if (placeSearchTarget === 'hotel') {
      setFormData({
        ...formData,
        locationName: place.name,
        address: place.address,
        hotelInfo: {
          ...(formData.hotelInfo || { checkIn: '', checkOut: '', arrivalTime: '', notes: '', noteType: NoteType.NOTE, attachments: [] }),
          name: place.name,
          address: place.address,
          lat: place.lat,
          lng: place.lng,
          placeId: place.placeId,
        }
      });
    } else if (placeSearchTarget === 'departure') {
      setFormData({
        ...formData,
        flightInfo: {
          ...(formData.flightInfo || { airline: '', flightNumber: '', departureAirportCode: '', arrivalAirportCode: '', departureTime: '', arrivalTime: '', notes: '', noteType: NoteType.NOTE, attachments: [] }),
          departureAirportCode: place.name, // Or try to extract IATA if possible, but place.name is a start
          departureAirportLat: place.lat,
          departureAirportLng: place.lng,
        }
      });
    } else if (placeSearchTarget === 'arrival') {
      setFormData({
        ...formData,
        flightInfo: {
          ...(formData.flightInfo || { airline: '', flightNumber: '', departureAirportCode: '', arrivalAirportCode: '', departureTime: '', arrivalTime: '', notes: '', noteType: NoteType.NOTE, attachments: [] }),
          arrivalAirportCode: place.name,
          arrivalAirportLat: place.lat,
          arrivalAirportLng: place.lng,
        }
      });
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
        <DialogTitle>{existingItem ? 'Edit Item' : 'Add to Itinerary'}</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              name="title"
              label="Title"
              fullWidth
              value={formData.title}
              onChange={handleChange}
              required
            />

            <FormControl fullWidth>
              <InputLabel>Type</InputLabel>
              <Select
                name="type"
                label="Type"
                value={formData.type}
                onChange={handleSelectChange}
              >
                {Object.values(ItemType).map((type) => (
                  <MenuItem key={type} value={type}>{type}</MenuItem>
                ))}
              </Select>
            </FormControl>

            {formData.type === ItemType.FLIGHT && (
              <Stack spacing={2} sx={{ p: 2, bgcolor: 'action.hover', borderRadius: 1 }}>
                <Typography variant="subtitle2">Flight Details</Typography>
                <Stack direction="row" spacing={2}>
                  <TextField
                    name="flightInfo.flightNumber"
                    label="Flight Number"
                    value={formData.flightInfo?.flightNumber || ''}
                    onChange={handleChange}
                    fullWidth
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <Button onClick={handleLookupFlight} size="small">Lookup</Button>
                        </InputAdornment>
                      )
                    }}
                  />
                  <TextField
                    name="flightInfo.airline"
                    label="Airline"
                    value={formData.flightInfo?.airline || ''}
                    onChange={handleChange}
                    fullWidth
                  />
                </Stack>
                <Stack direction="row" spacing={2}>
                  <TextField
                    name="flightInfo.departureAirportCode"
                    label="From (IATA)"
                    value={formData.flightInfo?.departureAirportCode || ''}
                    onChange={handleChange}
                    fullWidth
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton onClick={() => openPlaceSearch('departure')}>
                            <LocationOnIcon />
                          </IconButton>
                        </InputAdornment>
                      )
                    }}
                  />
                  <TextField
                    name="flightInfo.arrivalAirportCode"
                    label="To (IATA)"
                    value={formData.flightInfo?.arrivalAirportCode || ''}
                    onChange={handleChange}
                    fullWidth
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton onClick={() => openPlaceSearch('arrival')}>
                            <LocationOnIcon />
                          </IconButton>
                        </InputAdornment>
                      )
                    }}
                  />
                </Stack>
              </Stack>
            )}

            {formData.type === ItemType.HOTEL && (
              <Stack spacing={2} sx={{ p: 2, bgcolor: 'action.hover', borderRadius: 1 }}>
                <Typography variant="subtitle2">Hotel Details</Typography>
                <TextField
                  name="hotelInfo.name"
                  label="Hotel Name"
                  value={formData.hotelInfo?.name || ''}
                  onChange={handleChange}
                  fullWidth
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton onClick={() => openPlaceSearch('hotel')}>
                          <LocationOnIcon />
                        </IconButton>
                      </InputAdornment>
                    )
                  }}
                />
                <Stack direction="row" spacing={2}>
                  <TextField
                    name="hotelInfo.checkIn"
                    label="Check-in Date"
                    type="date"
                    value={formData.hotelInfo?.checkIn || ''}
                    onChange={handleChange}
                    fullWidth
                    InputLabelProps={{ shrink: true }}
                  />
                  <TextField
                    name="hotelInfo.checkOut"
                    label="Check-out Date"
                    type="date"
                    value={formData.hotelInfo?.checkOut || ''}
                    onChange={handleChange}
                    fullWidth
                    InputLabelProps={{ shrink: true }}
                  />
                </Stack>
              </Stack>
            )}

            <Stack direction="row" spacing={2}>
              <FormControl fullWidth>
                <InputLabel>Time Type</InputLabel>
                <Select
                  name="timeType"
                  label="Time Type"
                  value={formData.timeType}
                  onChange={handleSelectChange}
                >
                  {Object.values(TimeType).map((type) => (
                    <MenuItem key={type} value={type}>{type}</MenuItem>
                  ))}
                </Select>
              </FormControl>

              {formData.timeType === TimeType.PERIOD ? (
                <FormControl fullWidth>
                  <InputLabel>Period</InputLabel>
                  <Select
                    name="period"
                    label="Period"
                    value={formData.period || DayPeriod.MORNING}
                    onChange={handleSelectChange}
                  >
                    {Object.values(DayPeriod).map((p) => (
                      <MenuItem key={p} value={p}>{p}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              ) : (
                <>
                  <TextField
                    name="startTime"
                    label="Start Time"
                    type="time"
                    fullWidth
                    InputLabelProps={{ shrink: true }}
                    value={formData.startTime || ''}
                    onChange={handleChange}
                  />
                  {formData.timeType === TimeType.RANGE && (
                    <TextField
                      name="endTime"
                      label="End Time"
                      type="time"
                      fullWidth
                      InputLabelProps={{ shrink: true }}
                      value={formData.endTime || ''}
                      onChange={handleChange}
                    />
                  )}
                </>
              )}
            </Stack>

            <TextField
              name="locationName"
              label="Location Name"
              fullWidth
              value={formData.locationName}
              onChange={handleChange}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => openPlaceSearch('main')}>
                      <LocationOnIcon />
                    </IconButton>
                  </InputAdornment>
                )
              }}
            />

            <TextField
              name="note"
              label="Note"
              fullWidth
              multiline
              rows={3}
              value={formData.note}
              onChange={handleChange}
            />

            <FormControl fullWidth>
              <InputLabel>Note Type</InputLabel>
              <Select
                name="noteType"
                label="Note Type"
                value={formData.noteType}
                onChange={handleSelectChange}
              >
                <MenuItem value={NoteType.NOTE}>Info</MenuItem>
                <MenuItem value={NoteType.ALERT}>Alert</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" color="primary">
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <PlaceSearchDialog
        open={placeSearchOpen}
        onClose={() => setPlaceSearchOpen(false)}
        onPlacePicked={handlePlacePicked}
        title={placeSearchTarget === 'hotel' ? 'Search for Hotel' : 'Search for Place'}
      />
    </>
  );
};

export default AddEditItemModal;
