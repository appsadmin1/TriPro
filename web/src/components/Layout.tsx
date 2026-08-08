import React, { useState, useEffect } from 'react';
import {
  AppBar,
  Box,
  CssBaseline,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Avatar,
  Menu,
  MenuItem,
  useTheme,
  useMediaQuery,
  Badge,
  Stack,
  Button,
} from '@mui/material';
import {
  Menu as MenuIcon,
  FlightTakeoff,
  History,
  AccountCircle,
  Settings,
  Logout,
  Notifications,
} from '@mui/icons-material';
import { useNavigate, useLocation } from 'react-router-dom';
import { authService } from '../services/authService';
import { activityService } from '../services/activityService';
import { useTranslation } from 'react-i18next';

const drawerWidth = 280;

interface Props {
  children: React.ReactNode;
  title?: string;
}

const Layout: React.FC<Props> = ({ children, title }) => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [unreadAlertsCount, setUnreadAlertsCount] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
  const user = authService.getCurrentUser();
  const { t, i18n } = useTranslation();
  const direction = i18n.language.startsWith('he') ? 'rtl' : 'ltr';

  useEffect(() => {
    if (!user) return;

    const unsubscribe = activityService.observeRecentActivity(user.uid, (activities) => {
      const lastSeen = localStorage.getItem('alerts_last_seen');
      const lastSeenTime = lastSeen ? parseInt(lastSeen, 10) : 0;

      const unread = activities.filter(a => {
        if (!a.createdAt) return false;
        const time = a.createdAt.toDate ? a.createdAt.toDate().getTime() : new Date(a.createdAt).getTime();
        return time > lastSeenTime;
      }).length;

      setUnreadAlertsCount(unread);
    });

    return () => unsubscribe();
  }, [user, location.pathname]);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = async () => {
    await authService.signOut();
    navigate('/login');
  };

  const handleLanguageChange = (lang: string) => {
    i18n.changeLanguage(lang);
  };

  const currentLanguage = i18n.language.startsWith('he') ? 'he' : 'en';

  const menuItems = [
    { text: t('nav_trips'), icon: <FlightTakeoff />, path: '/' },
    {
      text: t('nav_alerts'),
      icon: (
        <Badge badgeContent={unreadAlertsCount} color="error">
          <Notifications />
        </Badge>
      ),
      path: '/alerts'
    },
    { text: t('trips_section_past'), icon: <History />, path: '/past' },
  ];

  const drawer = (
    <div>
      <Toolbar>
        <Typography variant="h6" color="primary" sx={{ fontWeight: 'bold' }}>
          {t('app_name')}
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {menuItems.map((item) => (
          <ListItem key={item.path} disablePadding>
            <ListItemButton
              selected={location.pathname === item.path}
              onClick={() => {
                navigate(item.path);
                setMobileOpen(false);
              }}
              sx={{
                borderRadius: direction === 'rtl' ? '24px 0 0 24px' : '0 24px 24px 0',
                ml: direction === 'rtl' ? 1 : 0,
                mr: direction === 'rtl' ? 0 : 1,
                '&.Mui-selected': {
                  bgcolor: 'primary.container',
                  color: 'primary.main',
                  '& .MuiListItemIcon-root': { color: 'primary.main' },
                },
              }}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
      <Divider sx={{ mt: 'auto' }} />
      <List>
        <ListItem disablePadding>
          <ListItem sx={{ py: 1, px: 2 }}>
            <Stack direction="row" spacing={1} sx={{ width: '100%' }}>
              <Button
                fullWidth
                size="small"
                variant={currentLanguage === 'en' ? 'contained' : 'outlined'}
                onClick={() => handleLanguageChange('en')}
                sx={{ fontSize: '0.75rem' }}
              >
                English
              </Button>
              <Button
                fullWidth
                size="small"
                variant={currentLanguage === 'he' ? 'contained' : 'outlined'}
                onClick={() => handleLanguageChange('he')}
                sx={{ fontSize: '0.75rem' }}
              >
                עברית
              </Button>
            </Stack>
          </ListItem>
        </ListItem>
        <ListItem disablePadding>
          <ListItemButton onClick={() => navigate('/settings')}>
            <ListItemIcon><Settings /></ListItemIcon>
            <ListItemText primary={t('settings_title')} />
          </ListItemButton>
        </ListItem>
        <ListItem disablePadding>
          <ListItemButton onClick={handleLogout}>
            <ListItemIcon><Logout /></ListItemIcon>
            <ListItemText primary={t('settings_sign_out')} />
          </ListItemButton>
        </ListItem>
      </List>
    </div>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <CssBaseline />
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          zIndex: (theme) => theme.zIndex.drawer + 1,
          bgcolor: 'background.default',
          color: 'text.primary',
          borderBottom: '1px solid',
          borderColor: 'divider',
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1, fontWeight: 'medium' }}>
            {title || 'TriPro'}
          </Typography>
          <IconButton onClick={handleMenuOpen} sx={{ p: 0 }}>
            <Avatar alt={user?.displayName || 'User'} src={user?.photoURL || undefined}>
              {!user?.photoURL && <AccountCircle />}
            </Avatar>
          </IconButton>
          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
            onClick={handleMenuClose}
          >
            <MenuItem onClick={() => navigate('/profile')}>{t('nav_profile')}</MenuItem>
            <MenuItem onClick={handleLogout}>{t('settings_sign_out')}</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box
        component="nav"
        sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${drawerWidth}px)` },
          minHeight: '100vh',
          bgcolor: 'background.default',
        }}
      >
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
};

export default Layout;
