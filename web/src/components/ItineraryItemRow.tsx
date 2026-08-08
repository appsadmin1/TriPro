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
  Attractions,
  Hiking,
  DirectionsCar,
  TheaterComedy,
  Event,
  Warning,
  PriorityHigh,
  ArrowForward,
  PictureAsPdf,
  UploadFile,
  EditNote,
  KeyboardArrowUp,
  KeyboardArrowDown,
} from '@mui/icons-material';
import { ItineraryItem, ItemType, NoteType, TimeType, DayPeriod, MarkerColorKey } from '../data/models';
import { getAccentColor } from '../utils/colorUtils';
import { alpha } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';

interface ItineraryItemRowProps {
  item: ItineraryItem;
  canEdit: boolean;
  onEdit: () => void;
  onDelete: () => void;
  onAddAttachment: () => void;
  onAttachmentClick?: (attachment: any) => void;
  onMoveUp?: () => void;
  onMoveDown?: () => void;
  activityColors?: Record<string, string>;
}

const getItemIcon = (type: ItemType) => {
  switch (type) {
    case ItemType.FLIGHT: return <FlightTakeoff />;
    case ItemType.HOTEL: return <Hotel />;
    case ItemType.RESTAURANT: return <Restaurant />;
    case ItemType.ATTRACTION: return <Attractions />;
    case ItemType.ACTIVITY: return <Hiking />;
    case ItemType.TRANSPORT: return <DirectionsCar />;
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
  onAttachmentClick,
  onMoveUp,
  onMoveDown,
  activityColors,
}) => {
  const { t } = useTranslation();
  const accentColor = activityColors?.[item.type] || getAccentColor(item.type);

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
            {t((item.period || DayPeriod.MORNING).toLowerCase())}
          </Typography>
        );
      default:
        return null;
    }
  };

  return (
    <Box sx={{ display: 'flex', width: '100%', mb: 3 }}>
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
                width: 36,
                height: 36,
                bgcolor: alpha(accentColor, 0.12),
                color: accentColor,
                '& .MuiSvgIcon-root': { fontSize: 20 }
              }}
            >
              {getItemIcon(item.type)}
            </Avatar>

            <Box sx={{ flexGrow: 1 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="h4" sx={{ fontSize: '1.25rem', color: 'primary.main', fontWeight: 'bold' }}>
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
                  {t('checkin_prefix', { defaultValue: 'Check-in: {{time}}', time: item.hotelInfo.checkIn })} · {t('checkout_prefix', { defaultValue: 'Check-out: {{time}}', time: item.hotelInfo.checkOut })}
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
                      onClick={() => onAttachmentClick ? onAttachmentClick(att) : window.open(att.downloadUrl, '_blank')}
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
                  {t('add_note')}
                </Button>
                <Button
                  size="small"
                  startIcon={<UploadFile sx={{ fontSize: 16 }} />}
                  onClick={onAddAttachment}
                  sx={{ fontSize: '0.75rem', color: 'text.secondary' }}
                >
                  {t('upload_file')}
                </Button>
                <Box sx={{ flexGrow: 1 }} />
                {onMoveUp && (
                  <IconButton size="small" onClick={onMoveUp}>
                    <KeyboardArrowUp fontSize="small" />
                  </IconButton>
                )}
                {onMoveDown && (
                  <IconButton size="small" onClick={onMoveDown}>
                    <KeyboardArrowDown fontSize="small" />
                  </IconButton>
                )}
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
