import React from 'react';
import {
  Box,
  Typography,
  IconButton,
  Paper,
  Chip,
  Avatar,
  Stack,
  Tooltip,
} from '@mui/material';
import {
  Edit,
  Delete,
  AttachFile,
  LocationOn,
  Flight,
  Hotel,
  Restaurant,
  Star,
  DirectionsBus,
  TheaterComedy,
  MoreHoriz,
} from '@mui/icons-material';
import { ItineraryItem, ItemType, NoteType } from '../data/models';

interface ItineraryItemRowProps {
  item: ItineraryItem;
  canEdit: boolean;
  onEdit: () => void;
  onDelete: () => void;
  onAddAttachment: () => void;
}

const getItemIcon = (type: ItemType) => {
  switch (type) {
    case ItemType.FLIGHT: return <Flight />;
    case ItemType.HOTEL: return <Hotel />;
    case ItemType.RESTAURANT: return <Restaurant />;
    case ItemType.ATTRACTION: return <Star />;
    case ItemType.ACTIVITY: return <Star />;
    case ItemType.TRANSPORT: return <DirectionsBus />;
    case ItemType.SHOW: return <TheaterComedy />;
    default: return <MoreHoriz />;
  }
};

const ItineraryItemRow: React.FC<ItineraryItemRowProps> = ({
  item,
  canEdit,
  onEdit,
  onDelete,
  onAddAttachment,
}) => {
  const getPeriodLabel = (period?: string) => {
    if (!period) return '';
    return period.charAt(0) + period.slice(1).toLowerCase();
  };

  const timeLabel = item.startTime || getPeriodLabel(item.period) || 'Anytime';

  return (
    <Paper
      elevation={0}
      sx={{
        p: 2,
        mb: 2,
        borderRadius: 3,
        border: '1px solid',
        borderColor: 'divider',
        display: 'flex',
        alignItems: 'flex-start',
        position: 'relative',
        '&:hover': {
          bgcolor: 'action.hover',
        },
      }}
    >
      <Box sx={{ minWidth: 80, mr: 2 }}>
        <Typography variant="subtitle2" color="primary" sx={{ fontWeight: 'bold' }}>
          {timeLabel}
        </Typography>
      </Box>

      <Box sx={{ flexGrow: 1 }}>
        <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
          <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.light' }}>
            {getItemIcon(item.type)}
          </Avatar>
          <Typography variant="h6" sx={{ fontWeight: 'bold', fontSize: '1.1rem' }}>
            {item.title}
          </Typography>
          {item.noteType === NoteType.ALERT && (
            <Chip label="Alert" size="small" color="error" variant="filled" />
          )}
        </Stack>

        <Typography variant="body2" color="text.secondary" sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
          <LocationOn sx={{ fontSize: 16, mr: 0.5 }} />
          {item.locationName || 'No location set'}
        </Typography>

        {item.note && (
          <Typography variant="body2" sx={{ fontStyle: 'italic', mb: 1, color: 'text.secondary' }}>
            "{item.note}"
          </Typography>
        )}

        {item.attachments && item.attachments.length > 0 && (
          <Stack direction="row" spacing={1} mt={1}>
            {item.attachments.map((att) => (
              <Chip
                key={att.id}
                icon={<AttachFile sx={{ fontSize: 14 }} />}
                label={att.fileName}
                size="small"
                variant="outlined"
                onClick={() => window.open(att.downloadUrl, '_blank')}
              />
            ))}
          </Stack>
        )}
      </Box>

      {canEdit && (
        <Box sx={{ display: 'flex', flexDirection: 'column' }}>
          <Tooltip title="Edit">
            <IconButton size="small" onClick={onEdit}>
              <Edit fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Add attachment">
            <IconButton size="small" onClick={onAddAttachment}>
              <AttachFile fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton size="small" color="error" onClick={onDelete}>
              <Delete fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      )}
    </Paper>
  );
};

export default ItineraryItemRow;
