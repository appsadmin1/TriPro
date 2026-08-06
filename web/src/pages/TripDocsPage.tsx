import React, { useEffect, useState } from 'react';
import {
  Typography,
  Box,
  CircularProgress,
  Stack,
  Card,
  IconButton,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemSecondaryAction,
  Collapse,
} from '@mui/material';
import {
  ArrowBack,
  InsertDriveFile,
  ExpandLess,
  ExpandMore,
  OpenInNew,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { tripService } from '../services/tripService';
import { Trip, TripDay, ItineraryItem, Attachment } from '../data/models';
import { format, parseISO } from 'date-fns';

interface DocEntry {
  date: string;
  itemId: string;
  itemTitle: string;
  attachment: Attachment;
}

const TripDocsPage: React.FC = () => {
  const { tripId } = useParams<{ tripId: string }>();
  const [trip, setTrip] = useState<Trip | null>(null);
  const [docsByDate, setDocsByDate] = useState<Record<string, DocEntry[]>>({});
  const [loading, setLoading] = useState(true);
  const [expandedDates, setExpandedDates] = useState<Set<string>>(new Set());
  const navigate = useNavigate();

  useEffect(() => {
    if (!tripId) return;

    const unsubTrip = tripService.observeTrip(tripId, (data) => {
      setTrip(data);
    });

    const unsubDays = tripService.observeDays(tripId, (days) => {
      const dates = days.map(d => d.date);
      const unsubItems = tripService.observeAllItemsForTrip(tripId, dates, (itemsByDate) => {
        const grouped: Record<string, DocEntry[]> = {};
        Object.entries(itemsByDate).forEach(([date, items]) => {
          const entries: DocEntry[] = [];
          items.forEach(item => {
            item.attachments?.forEach(att => {
              entries.push({ date, itemId: item.id, itemTitle: item.title, attachment: att });
            });
          });
          if (entries.length > 0) {
            grouped[date] = entries;
          }
        });
        setDocsByDate(grouped);
        setLoading(false);
      });
      return unsubItems;
    });

    return () => {
      unsubTrip();
      unsubDays();
    };
  }, [tripId]);

  const toggleExpand = (date: string) => {
    const next = new Set(expandedDates);
    if (next.has(date)) next.delete(date);
    else next.add(date);
    setExpandedDates(next);
  };

  if (loading) {
    return (
      <Layout title="Trip Documents">
        <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  const dates = Object.keys(docsByDate).sort();

  return (
    <Layout title={`Docs: ${trip?.name || 'Trip'}`}>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 4 }}>
        <IconButton onClick={() => navigate(-1)}>
          <ArrowBack />
        </IconButton>
        <Typography variant="h4" sx={{ fontWeight: 'bold' }}>Trip Documents</Typography>
      </Stack>

      {dates.length === 0 ? (
        <Box sx={{ py: 8, textAlign: 'center' }}>
          <Typography color="text.secondary">No documents found for this trip.</Typography>
        </Box>
      ) : (
        <Stack spacing={2}>
          {dates.map((date) => (
            <Box key={date}>
              <Box
                onClick={() => toggleExpand(date)}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  cursor: 'pointer',
                  py: 1,
                  px: 1,
                  borderRadius: 2,
                  '&:hover': { bgcolor: 'action.hover' }
                }}
              >
                <Typography variant="h6" color="primary" sx={{ flexGrow: 1, fontWeight: 'medium' }}>
                  {format(parseISO(date), 'EEEE, MMM d')}
                </Typography>
                {expandedDates.has(date) ? <ExpandLess /> : <ExpandMore />}
              </Box>
              <Collapse in={expandedDates.has(date) || true} timeout="auto" unmountOnExit>
                <List>
                  {docsByDate[date].map((doc, idx) => (
                    <Card key={`${doc.itemId}-${idx}`} variant="outlined" sx={{ mb: 1, borderRadius: 3 }}>
                      <ListItem>
                        <ListItemIcon>
                          <InsertDriveFile color="primary" />
                        </ListItemIcon>
                        <ListItemText
                          primary={doc.attachment.fileName}
                          secondary={doc.itemTitle}
                        />
                        <ListItemSecondaryAction>
                          <IconButton
                            edge="end"
                            href={doc.attachment.downloadUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                          >
                            <OpenInNew />
                          </IconButton>
                        </ListItemSecondaryAction>
                      </ListItem>
                    </Card>
                  ))}
                </List>
              </Collapse>
            </Box>
          ))}
        </Stack>
      )}
    </Layout>
  );
};

export default TripDocsPage;
