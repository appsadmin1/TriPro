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
  Delete,
  UnfoldLess,
  UnfoldMore,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import AttachmentViewerDialog from '../components/AttachmentViewerDialog';
import { tripService } from '../services/tripService';
import { authService } from '../services/authService';
import { Trip, TripDay, ItineraryItem, Attachment } from '../data/models';
import { format, parseISO } from 'date-fns';
import { he } from 'date-fns/locale';
import { useTranslation } from 'react-i18next';

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
  const [canEdit, setCanEdit] = useState(false);
  const [viewing, setViewing] = useState<DocEntry | null>(null);
  const navigate = useNavigate();
  const user = authService.getCurrentUser();
  const { t, i18n } = useTranslation();

  useEffect(() => {
    if (!tripId) return;

    const unsubTrip = tripService.observeTrip(tripId, (data) => {
      setTrip(data);
      if (data && user) {
        const role = data.members[user.uid];
        setCanEdit(role === 'owner' || role === 'editor');
      }
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
  }, [tripId, user]);

  const toggleExpand = (date: string) => {
    const next = new Set(expandedDates);
    if (next.has(date)) next.delete(date);
    else next.add(date);
    setExpandedDates(next);
  };

  const handleExpandAll = () => {
    setExpandedDates(new Set(Object.keys(docsByDate)));
  };

  const handleCollapseAll = () => {
    setExpandedDates(new Set());
  };

  const handleRemoveDoc = async (date: string, itemId: string, attachmentId: string) => {
    if (!tripId) return;
    if (window.confirm(t('confirm_remove_doc'))) {
      await tripService.removeAttachment(tripId, date, itemId, attachmentId);
    }
  };

  if (loading) {
    return (
      <Layout title={t('trip_documents')}>
        <Box display="flex" justifyContent="center" alignItems="center" height="60vh">
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  const dates = Object.keys(docsByDate).sort();
  const allExpanded = expandedDates.size === dates.length && dates.length > 0;

  return (
    <Layout title={`${t('docs_saved')}: ${trip?.name || t('app_name')}`}>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 4 }}>
        <IconButton onClick={() => navigate(-1)}>
          <ArrowBack />
        </IconButton>
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="h4" sx={{ fontWeight: 'bold' }}>{t('trip_documents')}</Typography>
        </Box>
        {dates.length > 0 && (
          <IconButton onClick={allExpanded ? handleCollapseAll : handleExpandAll} color="primary">
            {allExpanded ? <UnfoldLess /> : <UnfoldMore />}
          </IconButton>
        )}
      </Stack>

      {dates.length === 0 ? (
        <Box sx={{ py: 8, textAlign: 'center' }}>
          <Typography color="text.secondary">{t('no_documents_found')}</Typography>
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
                  {format(parseISO(date), 'EEEE, MMM d', { locale: i18n.language.startsWith('he') ? he : undefined })}
                </Typography>
                {expandedDates.has(date) ? <ExpandLess /> : <ExpandMore />}
              </Box>
              <Collapse in={expandedDates.has(date)} timeout="auto" unmountOnExit>
                <List>
                  {docsByDate[date].map((doc, idx) => (
                    <Card key={`${doc.itemId}-${idx}`} variant="outlined" sx={{ mb: 1, borderRadius: 3 }}>
                      <ListItem button onClick={() => setViewing(doc)}>
                        <ListItemIcon>
                          <InsertDriveFile color="primary" />
                        </ListItemIcon>
                        <ListItemText
                          primary={doc.attachment.fileName}
                          secondary={doc.itemTitle}
                        />
                        <ListItemSecondaryAction>
                          <Stack direction="row" spacing={1}>
                            <IconButton
                              edge="end"
                              href={doc.attachment.downloadUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              onClick={(e) => e.stopPropagation()}
                            >
                              <OpenInNew />
                            </IconButton>
                            {canEdit && (
                              <IconButton
                                edge="end"
                                color="error"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleRemoveDoc(doc.date, doc.itemId, doc.attachment.id);
                                }}
                              >
                                <Delete />
                              </IconButton>
                            )}
                          </Stack>
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

      <AttachmentViewerDialog
        open={!!viewing}
        attachment={viewing?.attachment || null}
        onClose={() => setViewing(null)}
        onRemove={canEdit ? () => {
          if (viewing && tripId) {
            tripService.removeAttachment(tripId, viewing.date, viewing.itemId, viewing.attachment.id);
          }
        } : undefined}
        onRename={canEdit ? (newName) => {
          if (viewing && tripId) {
            tripService.renameAttachment(tripId, viewing.date, viewing.itemId, viewing.attachment.id, newName);
          }
        } : undefined}
      />
    </Layout>
  );
};

export default TripDocsPage;
