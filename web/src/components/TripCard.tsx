import React from 'react';
import {
  Card,
  CardActionArea,
  CardContent,
  CardMedia,
  Typography,
  Box,
  Chip,
  AvatarGroup,
  Avatar,
} from '@mui/material';
import { CalendarToday, LocationOn } from '@mui/icons-material';
import { Trip } from '../data/models';
import { format, parseISO, differenceInDays, isValid } from 'date-fns';
import { he } from 'date-fns/locale';
import { getOptimizedImageUrl } from '../utils/imageUtils';
import { useTranslation } from 'react-i18next';

interface TripCardProps {
  trip: Trip;
  onClick: () => void;
  isPast?: boolean;
}

const TripCard: React.FC<TripCardProps> = ({ trip, onClick, isPast }) => {
  const { t, i18n } = useTranslation();
  console.log("Rendering TripCard for trip:", trip.id, trip.name, trip.coverImageUrl);

  const safeParseISO = (dateStr: string) => {
    if (!dateStr) return new Date(0);
    const d = parseISO(dateStr);
    return isValid(d) ? d : new Date(0);
  };

  const startDate = safeParseISO(trip.startDate);
  const endDate = safeParseISO(trip.endDate);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const daysAway = differenceInDays(startDate, today);
  const isHappeningNow = today >= startDate && today <= endDate;

  let statusLabel = '';
  if (isHappeningNow) statusLabel = t('trip_happening_now', { defaultValue: 'HAPPENING NOW' });
  else if (isPast) statusLabel = t('trip_completed', { defaultValue: 'COMPLETED' });
  else if (daysAway > 0) statusLabel = t(daysAway === 1 ? 'trip_days_away' : 'trip_days_away_plural', { count: daysAway, defaultValue: `${daysAway} DAYS AWAY` });

  return (
    <Card sx={{ maxWidth: 400, mb: 2 }}>
      <CardActionArea onClick={onClick}>
        <CardMedia
          component="img"
          height="160"
          image={getOptimizedImageUrl(trip.coverImageUrl)}
          alt={trip.destination}
          crossOrigin="anonymous"
        />
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
            <Typography variant="overline" color={isHappeningNow ? 'primary' : 'textSecondary'} sx={{ fontWeight: 'bold' }}>
              {statusLabel}
            </Typography>
          </Box>
          <Typography gutterBottom variant="h5" component="div" sx={{ fontWeight: 'bold' }}>
            {trip.destination}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
            <CalendarToday sx={{ fontSize: 16, mr: 1 }} />
            {format(startDate, 'MMM d', { locale: i18n.language.startsWith('he') ? he : undefined })} - {format(endDate, 'MMM d, yyyy', { locale: i18n.language.startsWith('he') ? he : undefined })}
          </Typography>
          <Typography variant="body1" color="text.primary" sx={{ fontWeight: 'medium' }}>
            {trip.name}
          </Typography>

          <Box display="flex" justifyContent="space-between" alignItems="center" mt={2}>
            <AvatarGroup max={4}>
              {trip.memberIds.map((id) => (
                <Avatar key={id} sx={{ width: 32, height: 32, fontSize: '0.875rem' }}>
                  {id.substring(0, 1).toUpperCase()}
                </Avatar>
              ))}
            </AvatarGroup>
          </Box>
        </CardContent>
      </CardActionArea>
    </Card>
  );
};

export default TripCard;
