import React, { useEffect, useState } from 'react';
import {
  Typography,
  Box,
  CircularProgress,
  Stack,
  Card,
  Avatar,
  Button,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Divider,
} from '@mui/material';
import { NotificationsActive, DoneAll, Info, Edit, Add, Delete, Person, Hotel, Flight, Note } from '@mui/icons-material';
import Layout from '../components/Layout';
import { activityService } from '../services/activityService';
import { authService } from '../services/authService';
import { ActivityEntry, ActivityType } from '../data/models';
import { formatDistanceToNow } from 'date-fns';

const AlertsPage: React.FC = () => {
  const [activities, setActivities] = useState<ActivityEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [lastSeenTimestamp, setLastSeenTimestamp] = useState<number>(0);
  const user = authService.getCurrentUser();

  useEffect(() => {
    const saved = localStorage.getItem('alerts_last_seen');
    if (saved) {
      setLastSeenTimestamp(parseInt(saved, 10));
    }

    if (!user) return;

    const unsubscribe = activityService.observeRecentActivity(user.uid, (data) => {
      setActivities(data);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [user]);

  const handleMarkAllAsSeen = () => {
    const now = Date.now();
    localStorage.setItem('alerts_last_seen', now.toString());
    setLastSeenTimestamp(now);
  };

  const getActivityIcon = (type: ActivityType) => {
    switch (type) {
      case ActivityType.ITEM_ADDED: return <Add color="primary" />;
      case ActivityType.ITEM_UPDATED: return <Edit color="primary" />;
      case ActivityType.ITEM_REMOVED: return <Delete color="error" />;
      case ActivityType.HOTEL_UPDATED: return <Hotel color="primary" />;
      case ActivityType.FLIGHT_UPDATED: return <Flight color="primary" />;
      case ActivityType.DAY_NOTE_UPDATED: return <Note color="secondary" />;
      case ActivityType.MEMBER_INVITED: return <Person color="info" />;
      case ActivityType.MEMBER_ROLE_CHANGED: return <Person color="info" />;
      case ActivityType.MEMBER_REMOVED: return <Person color="error" />;
      default: return <Info color="action" />;
    }
  };

  const getTimeString = (createdAt: any) => {
    if (!createdAt) return '';
    try {
      const date = createdAt.toDate ? createdAt.toDate() : new Date(createdAt);
      return formatDistanceToNow(date, { addSuffix: true });
    } catch (e) {
      return '';
    }
  };

  const isNew = (createdAt: any) => {
    if (!createdAt) return false;
    try {
      const date = createdAt.toDate ? createdAt.toDate() : new Date(createdAt);
      return date.getTime() > lastSeenTimestamp;
    } catch (e) {
      return false;
    }
  };

  if (loading) {
    return (
      <Layout title="Alerts">
        <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  return (
    <Layout title="Alerts">
      <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <Box>
          <Typography variant="h4" color="primary" sx={{ fontWeight: 'bold', mb: 1 }}>
            Alerts
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Keep track of changes and updates to your trips.
          </Typography>
        </Box>
        {activities.length > 0 && (
          <Button
            variant="outlined"
            startIcon={<DoneAll />}
            onClick={handleMarkAllAsSeen}
            sx={{ borderRadius: 2 }}
          >
            Mark all as seen
          </Button>
        )}
      </Box>

      {activities.length === 0 ? (
        <Box sx={{ py: 8, textAlign: 'center' }}>
          <NotificationsActive sx={{ fontSize: 60, color: 'action.disabled', mb: 2 }} />
          <Typography color="text.secondary">No recent activity found.</Typography>
        </Box>
      ) : (
        <Card variant="outlined" sx={{ borderRadius: 3 }}>
          <List sx={{ width: '100%', p: 0 }}>
            {activities.map((activity, index) => (
              <React.Fragment key={activity.id}>
                <ListItem
                  alignItems="flex-start"
                  sx={{
                    py: 2,
                    bgcolor: isNew(activity.createdAt) ? 'action.hover' : 'inherit',
                  }}
                >
                  <ListItemAvatar>
                    <Avatar sx={{ bgcolor: 'background.paper', border: '1px solid', borderColor: 'divider' }}>
                      {getActivityIcon(activity.type)}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={
                      <Stack direction="row" justifyContent="space-between" alignItems="center">
                        <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>
                          {activity.actorName}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {getTimeString(activity.createdAt)}
                        </Typography>
                      </Stack>
                    }
                    secondary={
                      <Box sx={{ mt: 0.5 }}>
                        <Typography variant="body2" color="text.primary">
                          {activity.message}
                        </Typography>
                        <Typography variant="caption" color="primary" sx={{ mt: 0.5, display: 'block' }}>
                          Trip: {activity.tripName}
                        </Typography>
                      </Box>
                    }
                  />
                </ListItem>
                {index < activities.length - 1 && <Divider component="li" />}
              </React.Fragment>
            ))}
          </List>
        </Card>
      )}
    </Layout>
  );
};

export default AlertsPage;
