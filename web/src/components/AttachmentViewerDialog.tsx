import * as React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  IconButton,
  TextField,
  Stack,
} from '@mui/material';
import {
  Close as CloseIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { Attachment } from '../data/models';

interface AttachmentViewerDialogProps {
  open: boolean;
  onClose: () => void;
  attachment: Attachment | null;
  onRemove?: () => void;
  onRename?: (newName: string) => void;
}

const AttachmentViewerDialog: React.FC<AttachmentViewerDialogProps> = ({
  open,
  onClose,
  attachment,
  onRemove,
  onRename,
}) => {
  const [isEditing, setIsEditing] = React.useState(false);
  const [newName, setNewName] = React.useState('');

  React.useEffect(() => {
    if (attachment) {
      setNewName(attachment.fileName);
      setIsEditing(false);
    }
  }, [attachment, open]);

  if (!attachment) return null;

  const isImage = attachment.mimeType.startsWith('image/');
  const isPdf = attachment.fileName.toLowerCase().endsWith('.pdf');

  const handleRename = () => {
    if (onRename && newName.trim()) {
      onRename(newName.trim());
      setIsEditing(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          {isEditing ? (
            <TextField
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              size="small"
              autoFocus
              onBlur={handleRename}
              onKeyPress={(e) => e.key === 'Enter' && handleRename()}
            />
          ) : (
            <Typography variant="h6" noWrap sx={{ maxWidth: '80%' }}>
              {attachment.fileName}
            </Typography>
          )}
          <Box>
            {onRename && !isEditing && (
              <IconButton onClick={() => setIsEditing(true)}>
                <EditIcon fontSize="small" />
              </IconButton>
            )}
            <IconButton onClick={onClose}>
              <CloseIcon />
            </IconButton>
          </Box>
        </Stack>
      </DialogTitle>
      <DialogContent dividers>
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          {isImage ? (
            <img
              src={attachment.downloadUrl}
              alt={attachment.fileName}
              style={{ maxWidth: '100%', maxHeight: '60vh', objectFit: 'contain' }}
            />
          ) : (
            <Box sx={{ p: 4, textAlign: 'center' }}>
              <Typography color="text.secondary">
                {isPdf ? 'PDF Document' : 'File Attachment'}
              </Typography>
              <Typography variant="caption" display="block">
                {attachment.mimeType}
              </Typography>
            </Box>
          )}
        </Box>
      </DialogContent>
      <DialogActions sx={{ justifyContent: 'space-between', px: 3 }}>
        {onRemove ? (
          <Button
            startIcon={<DeleteIcon />}
            color="error"
            onClick={() => {
              if (window.confirm('Remove this attachment?')) {
                onRemove();
                onClose();
              }
            }}
          >
            Remove
          </Button>
        ) : <Box />}
        <Button
          startIcon={<DownloadIcon />}
          href={attachment.downloadUrl}
          target="_blank"
          download={attachment.fileName}
        >
          Open/Download
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default AttachmentViewerDialog;
