import * as React from 'react';
import {
  Box,
  Typography,
  IconButton,
  Paper,
  Avatar,
  Stack,
  Divider,
  Button,
} from '@mui/material';
import {
  Edit,
  Delete,
  AttachFile,
  FlightTakeoff,
  Hotel,
  Restaurant,
  Star,
  DirectionsBus,
  TheaterComedy,
  Event,
  Warning,
  PriorityHigh,
  ArrowForward,
  PictureAsPdf,
  UploadFile,
  EditNote,
} from '@mui/icons-material';
import { ItineraryItem, ItemType, NoteType, TimeType, DayPeriod } from '../data/models';
import { getAccentColor } from '../utils/colorUtils';
import { alpha } from '@mui/material/styles';

interface ItineraryItemRowProps {
  item: ItineraryItem;
  canEdit: boolean;
  onEdit: () => void;
  onDelete: () => void;
  onAddAttachment: () => void;
}

const getItemIcon = (type: ItemType) => {
  switch (type) {
    case ItemType.FLIGHT: return <FlightTakeoff />;
    case ItemType.HOTEL: return <Hotel />;
    case ItemType.RESTAURANT: return <Restaurant />;
    case ItemType.ATTRACTION: return <Star />;
    case ItemType.ACTIVITY: return <Star />;
    case ItemType.TRANSPORT: return <DirectionsBus />;
    case ItemType.SHOW: return <TheaterComedy />;
    default: return <Event />;
  }
};

const ItineraryItemRow: React.FC<ItineraryItemRowProps> = ({
  item,
  canEdit,
  onEdit,
  onDelete,
  onAddAttachment,
}) => {
  const accentColor = getAccentColor(item.type);

  const renderTime = () => {
    switch (item.timeType) {
      case TimeType.EXACT:
        return (
          <Typography variant="h4" color="primary" sx={{ fontWeight: 'bold' }}>
            {item.startTime || '--:--'}
          </Typography>
        );
      case TimeType.RANGE:
        return (
          <Box>
            <Typography variant="h4" color="primary" sx={{ fontWeight: 'bold' }}>
              {item.startTime || '--:--'}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {item.endTime || ''}
            </Typography>
          </Box>
        );
      case TimeType.PERIOD:
        return (
          <Typography variant="h4" color="primary" sx={{ fontWeight: 'bold', fontSize: '1rem' }}>
            {(item.period || DayPeriod.MORNING).toLowerCase()}
          </Typography>
        );
      default:
        return null;
    }
  };

  return (
    <Box sx={{ display: 'flex', width: '100%', mb: 3 }}>
      {/* Time Column */}
      <Box sx={{ minWidth: 100, pt: 1 }}>
        {renderTime()}
      </Box>

      {/* Item Card */}
      <Paper
        elevation={0}
        sx={{
          flexGrow: 1,
          borderRadius: 3,
          border: '1px solid',
          borderColor: 'divider',
          display: 'flex',
          overflow: 'hidden',
          '&:hover': {
            bgcolor: 'action.hover',
          },
        }}
      >
        {/* Accent Bar */}
        <Box sx={{ width: 4, bgcolor: accentColor }} />

        <Box sx={{ flexGrow: 1 }}>
          <Stack direction="row" spacing={2} sx={{ p: 2 }}>
            {/* Icon Circle */}
            <Avatar
              sx={{
                width: 40,
                height: 40,
                bgcolor: alpha(accentColor, 0.12),
                color: accentColor,
              }}
            >
              {getItemIcon(item.type)}
            </Avatar>

            <Box sx={{ flexGrow: 1 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="h4" sx={{ fontSize: '1.25rem' }}>
                    {item.title}
                  </Typography>
                  {item.type === ItemType.CUSTOM && item.customLabel && (
                    <Typography variant="caption" color="text.secondary">
                      {item.customLabel}
                    </Typography>
                  )}
                </Box>
                {canEdit && (
                  <IconButton size="small" onClick={onEdit}>
                    <Edit fontSize="small" color="action" />
                  </IconButton>
                )}
              </Stack>

              {/* Flight Specific */}
              {item.type === ItemType.FLIGHT && item.flightInfo && (
                <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 1 }}>
                  <Typography variant="caption" color="primary" sx={{ fontWeight: 'bold' }}>
                    {item.flightInfo.departureAirportCode} {item.flightInfo.departureTime}
                  </Typography>
                  <ArrowForward sx={{ fontSize: 14, color: 'text.secondary' }} />
                  <Typography variant="caption" color="primary" sx={{ fontWeight: 'bold' }}>
                    {item.flightInfo.arrivalAirportCode} {item.flightInfo.arrivalTime}
                  </Typography>
                </Stack>
              )}

              {/* Hotel Specific */}
              {item.type === ItemType.HOTEL && item.hotelInfo && (
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                  Check-in: {item.hotelInfo.checkIn} · Check-out: {item.hotelInfo.checkOut}
                </Typography>
              )}

              {/* Note / Alert */}
              {item.note && (
                <Box
                  sx={{
                    mt: 1.5,
                    p: '6px 12px',
                    borderRadius: 2,
                    display: 'flex',
                    alignItems: 'center',
                    bgcolor: item.noteType === NoteType.ALERT ? 'error.container' : alpha('#10B981', 0.12),
                    color: item.noteType === NoteType.ALERT ? 'error.main' : '#10B981',
                  }}
                >
                  {item.noteType === NoteType.ALERT ? <Warning sx={{ fontSize: 16, mr: 1 }} /> : <PriorityHigh sx={{ fontSize: 16, mr: 1 }} />}
                  <Typography variant="caption" sx={{ fontWeight: 'bold' }}>
                    {item.note}
                  </Typography>
                </Box>
              )}

              {/* Location */}
              {!item.note && item.locationName && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  {item.locationName}
                </Typography>
              )}

              {/* Attachments */}
              {item.attachments && item.attachments.length > 0 && (
                <Stack direction="row" spacing={1} sx={{ mt: 2, flexWrap: 'wrap', gap: 1 }}>
                  {item.attachments.map((att) => (
                    <Box
                      key={att.id}
                      onClick={() => window.open(att.downloadUrl, '_blank')}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        p: '4px 12px',
                        borderRadius: 10,
                        bgcolor: 'background.default',
                        border: '1px solid',
                        borderColor: 'divider',
                        cursor: 'pointer',
                        '&:hover': { bgcolor: 'action.hover' }
                      }}
                    >
                      {att.fileName.toLowerCase().endsWith('.pdf') ? (
                        <PictureAsPdf sx={{ fontSize: 14, color: 'error.main', mr: 1 }} />
                      ) : (
                        <AttachFile sx={{ fontSize: 14, color: 'primary.main', mr: 1 }} />
                      )}
                      <Typography variant="caption" color="primary.main">
                        {att.fileName}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              )}
            </Box>
          </Stack>

          {/* Action Footer */}
          {canEdit && (
            <>
              <Divider sx={{ opacity: 0.5 }} />
              <Box sx={{ px: 1, py: 0.5, display: 'flex', alignItems: 'center' }}>
                <Button
                  size="small"
                  startIcon={<EditNote sx={{ fontSize: 16 }} />}
                  onClick={onEdit}
                  sx={{ fontSize: '0.75rem', color: 'text.secondary' }}
                >
                  Add Note
                </Button>
                <Button
                  size="small"
                  startIcon={<UploadFile sx={{ fontSize: 16 }} />}
                  onClick={onAddAttachment}
                  sx={{ fontSize: '0.75rem', color: 'text.secondary' }}
                >
                  Upload File
                </Button>
                <Box sx={{ flexGrow: 1 }} />
                <IconButton size="small" color="error" onClick={onDelete}>
                  <Delete fontSize="small" />
                </IconButton>
              </Box>
            </>
          )}
        </Box>
      </Paper>
    </Box>
  );
};

export default ItineraryItemRow;
