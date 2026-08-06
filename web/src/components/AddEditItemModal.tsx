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
} from '@mui/material';
import { ItineraryItem, ItemType, TimeType, NoteType, DayPeriod } from '../data/models';

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
    note: '',
    noteType: NoteType.NOTE,
  });

  useEffect(() => {
    if (existingItem) {
      setFormData(existingItem);
    } else {
      setFormData({
        title: '',
        type: ItemType.ACTIVITY,
        timeType: TimeType.EXACT,
        locationName: '',
        note: '',
        noteType: NoteType.NOTE,
      });
    }
  }, [existingItem, open]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSelectChange = (e: any) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = () => {
    onSave(formData);
    onClose();
  };

  return (
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
              <TextField
                name="startTime"
                label="Start Time"
                type="time"
                fullWidth
                InputLabelProps={{ shrink: true }}
                value={formData.startTime || ''}
                onChange={handleChange}
              />
            )}
          </Stack>

          <TextField
            name="locationName"
            label="Location Name"
            fullWidth
            value={formData.locationName}
            onChange={handleChange}
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
  );
};

export default AddEditItemModal;
