import * as React from 'react';
import { useState, useEffect } from 'react';
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
  CircularProgress,
  List,
  ListItem,
  ListItemText,
} from '@mui/material';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import DeleteIcon from '@mui/icons-material/Delete';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { ItineraryItem, ItemType, TimeType, NoteType, DayPeriod, PickedPlace } from '../data/models';
import { flightService } from '../services/flightService';
import { uploadAttachment } from '../services/cloudinaryService';
import { authService } from '../services/authService';
import PlaceSearchDialog from './PlaceSearchDialog';
import MapPreview from './MapPreview';
import { useTranslation } from 'react-i18next';

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
  const { t } = useTranslation();
  const [formData, setFormData] = useState<Partial<ItineraryItem>>({
    title: '',
    type: ItemType.HOTEL,
    timeType: TimeType.PERIOD,
    locationName: '',
    address: '',
    note: '',
    noteType: NoteType.ALERT,
    attachments: [],
    customLabel: '',
  });

  const [placeSearchOpen, setPlaceSearchOpen] = useState(false);
  const [placeSearchTarget, setPlaceSearchTarget] = useState<'main' | 'hotel' | 'departure' | 'arrival'>('main');
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    if (existingItem) {
      setFormData(existingItem);
    } else {
      setFormData({
        title: '',
        type: ItemType.HOTEL,
        timeType: TimeType.PERIOD,
        locationName: '',
        address: '',
        note: '',
        noteType: NoteType.ALERT,
        attachments: [],
        customLabel: '',
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

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    const user = authService.getCurrentUser();
    if (!file || !user) return;

    setIsUploading(true);
    try {
      const attachment = await uploadAttachment(file, user.uid);
      setFormData(prev => ({
        ...prev,
        attachments: [...(prev.attachments || []), attachment]
      }));
    } catch (error) {
      console.error('Upload failed', error);
      alert(t('failed_upload', { defaultValue: 'Failed to upload file.' }));
    } finally {
      setIsUploading(false);
    }
  };

  const handleRemoveAttachment = (id: string) => {
    setFormData(prev => ({
      ...prev,
      attachments: (prev.attachments || []).filter(a => a.id !== id)
    }));
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
      alert(t('flight_lookup_failed', { defaultValue: 'Flight lookup failed. Please enter details manually.' }));
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
        title: formData.title || place.name,
        locationName: formData.locationName || place.name,
        address: formData.address || place.address,
        lat: place.lat,
        lng: place.lng,
      });
    } else if (placeSearchTarget === 'hotel') {
      setFormData({
        ...formData,
        hotelInfo: {
          ...(formData.hotelInfo || { checkIn: '', checkOut: '', attachments: [] }),
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
          ...(formData.flightInfo || { airline: '', flightNumber: '', departureAirportCode: '', arrivalAirportCode: '', departureTime: '', arrivalTime: '', attachments: [] }),
          departureAirportCode: place.name,
          departureAirportLat: place.lat,
          departureAirportLng: place.lng,
        }
      });
    } else if (placeSearchTarget === 'arrival') {
      setFormData({
        ...formData,
        flightInfo: {
          ...(formData.flightInfo || { airline: '', flightNumber: '', departureAirportCode: '', arrivalAirportCode: '', departureTime: '', arrivalTime: '', attachments: [] }),
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
        <DialogTitle>{existingItem ? t('edit_item') : t('add_to_itinerary')}</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              name="title"
              label={t('title')}
              fullWidth
              value={formData.title}
              onChange={handleChange}
              required
            />

            <FormControl fullWidth>
              <InputLabel>{t('type')}</InputLabel>
              <Select
                name="type"
                label={t('type')}
                value={formData.type}
                onChange={handleSelectChange}
              >
                {Object.values(ItemType).map((type) => (
                  <MenuItem key={type} value={type}>{t(`item_type_${type.toLowerCase()}`, { defaultValue: type })}</MenuItem>
                ))}
              </Select>
            </FormControl>

            {formData.type === ItemType.CUSTOM && (
              <TextField
                name="customLabel"
                label={t('custom_label', { defaultValue: 'What is this? (e.g. Grocery run)' })}
                fullWidth
                value={formData.customLabel || ''}
                onChange={handleChange}
              />
            )}

            {formData.type === ItemType.FLIGHT && (
              <Stack spacing={2} sx={{ p: 2, bgcolor: 'action.hover', borderRadius: 1 }}>
                <Typography variant="subtitle2">{t('flight_details')}</Typography>
                <Stack direction="row" spacing={2}>
                  <TextField
                    name="flightInfo.flightNumber"
                    label={t('flight_number')}
                    value={formData.flightInfo?.flightNumber || ''}
                    onChange={handleChange}
                    fullWidth
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <Button onClick={handleLookupFlight} size="small">{t('look_up', { defaultValue: 'Lookup' })}</Button>
                        </InputAdornment>
                      )
                    }}
                  />
                  <TextField
                    name="flightInfo.airline"
                    label={t('airline')}
                    value={formData.flightInfo?.airline || ''}
                    onChange={handleChange}
                    fullWidth
                  />
                </Stack>
                <Stack direction="row" spacing={2}>
                  <TextField
                    name="flightInfo.departureAirportCode"
                    label={t('from_iata')}
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
                    label={t('to_iata')}
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
                <Typography variant="subtitle2">{t('hotel_details')}</Typography>
                <TextField
                  name="hotelInfo.name"
                  label={t('hotel_name')}
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
                    label={t('checkin_date')}
                    type="date"
                    value={formData.hotelInfo?.checkIn || ''}
                    onChange={handleChange}
                    fullWidth
                    InputLabelProps={{ shrink: true }}
                  />
                  <TextField
                    name="hotelInfo.checkOut"
                    label={t('checkout_date')}
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
                <InputLabel>{t('time_type')}</InputLabel>
                <Select
                  name="timeType"
                  label={t('time_type')}
                  value={formData.timeType}
                  onChange={handleSelectChange}
                >
                  {Object.values(TimeType).map((type) => (
                    <MenuItem key={type} value={type}>{t(`time_type_${type.toLowerCase()}`, { defaultValue: type })}</MenuItem>
                  ))}
                </Select>
              </FormControl>

              {formData.timeType === TimeType.PERIOD ? (
                <FormControl fullWidth>
                  <InputLabel>{t('period')}</InputLabel>
                  <Select
                    name="period"
                    label={t('period')}
                    value={formData.period || DayPeriod.MORNING}
                    onChange={handleSelectChange}
                  >
                    {Object.values(DayPeriod).map((p) => (
                      <MenuItem key={p} value={p}>{t(p.toLowerCase())}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              ) : (
                <>
                  <TextField
                    name="startTime"
                    label={t('start_time')}
                    type="time"
                    fullWidth
                    InputLabelProps={{ shrink: true }}
                    value={formData.startTime || ''}
                    onChange={handleChange}
                  />
                  {formData.timeType === TimeType.RANGE && (
                    <TextField
                      name="endTime"
                      label={t('end_time')}
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
              label={t('location_name')}
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

            {formData.lat && formData.lng && (
              <MapPreview pins={[{ title: formData.title || 'Location', lat: formData.lat, lng: formData.lng }]} height={150} />
            )}

            <TextField
              name="note"
              label={t('note')}
              fullWidth
              multiline
              rows={3}
              value={formData.note}
              onChange={handleChange}
            />

            <FormControl fullWidth>
              <InputLabel>{t('note_type')}</InputLabel>
              <Select
                name="noteType"
                label={t('note_type')}
                value={formData.noteType}
                onChange={handleSelectChange}
              >
                <MenuItem value={NoteType.NOTE}>{t('info')}</MenuItem>
                <MenuItem value={NoteType.ALERT}>{t('alert')}</MenuItem>
              </Select>
            </FormControl>

            <Divider />

            <Typography variant="subtitle2" color="text.secondary">
              {t('attachments')}
            </Typography>

            <List>
              {(formData.attachments || []).map((att) => (
                <ListItem
                  key={att.id}
                  secondaryAction={
                    <IconButton edge="end" size="small" onClick={() => handleRemoveAttachment(att.id)}>
                      <DeleteIcon color="error" fontSize="small" />
                    </IconButton>
                  }
                  sx={{ py: 0 }}
                >
                  <AttachFileIcon sx={{ fontSize: 16, mr: 1, color: 'primary.main' }} />
                  <ListItemText
                    primary={att.fileName}
                    primaryTypographyProps={{ variant: 'body2', noWrap: true }}
                  />
                </ListItem>
              ))}
            </List>

            <Button
              component="label"
              variant="outlined"
              startIcon={isUploading ? <CircularProgress size={20} /> : <UploadFileIcon />}
              disabled={isUploading}
            >
              {isUploading ? t('uploading') : t('upload_file')}
              <input
                type="file"
                hidden
                onChange={handleFileUpload}
              />
            </Button>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>{t('action_cancel')}</Button>
          <Button onClick={handleSubmit} variant="contained" color="primary">
            {t('action_save')}
          </Button>
        </DialogActions>
      </Dialog>

      <PlaceSearchDialog
        open={placeSearchOpen}
        onClose={() => setPlaceSearchOpen(false)}
        onPlacePicked={handlePlacePicked}
        title={placeSearchTarget === 'hotel' ? t('search_hotel', { defaultValue: 'Search for Hotel' }) : t('search_place', { defaultValue: 'Search for Place' })}
      />
    </>
  );
};

export default AddEditItemModal;
