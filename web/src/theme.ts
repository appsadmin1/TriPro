import { createTheme, alpha } from '@mui/material/styles';

// Module augmentation to add container and onContainer to palette colors
declare module '@mui/material/styles' {
  interface PaletteColor {
    container?: string;
    onContainer?: string;
  }
  interface SimplePaletteColorOptions {
    container?: string;
    onContainer?: string;
  }
}

// Exact colors from TriProColors.kt
const TriProColors = {
  Primary: '#001736',
  OnPrimary: '#FFFFFF',
  PrimaryContainer: '#002B5B',
  OnPrimaryContainer: '#7594CA',
  Secondary: '#7F5600',
  SecondaryContainer: '#F9AD00',
  OnSecondaryContainer: '#664500',
  Background: '#F8F9FF',
  Surface: '#F9FAFB',
  SurfaceContainerLowest: '#FFFFFF',
  CardBorder: '#E2E8F0',
  Success: '#10B981',
  Error: '#BA1A1A',
  ErrorContainer: '#FFFFDAD6',
  OnErrorContainer: '#93000A',
  OnSurfaceVariant: '#43474F',
};

export const themeOptions = {
  palette: {
    primary: {
      main: TriProColors.Primary,
      contrastText: TriProColors.OnPrimary,
      container: TriProColors.PrimaryContainer,
      onContainer: TriProColors.OnPrimaryContainer,
    },
    secondary: {
      main: TriProColors.Secondary,
      container: TriProColors.SecondaryContainer,
      onContainer: TriProColors.OnSecondaryContainer,
    },
    success: {
      main: TriProColors.Success,
      container: alpha(TriProColors.Success, 0.15),
      onContainer: TriProColors.Success,
    },
    error: {
      main: TriProColors.Error,
      container: TriProColors.ErrorContainer,
      onContainer: TriProColors.OnErrorContainer,
    },
    background: {
      default: TriProColors.Background,
      paper: TriProColors.SurfaceContainerLowest,
    },
    text: {
      primary: '#0B1C30',
      secondary: TriProColors.OnSurfaceVariant,
    },
    divider: TriProColors.CardBorder,
  },
  typography: {
    fontFamily: '"Work Sans", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontFamily: '"Plus Jakarta Sans", sans-serif',
      fontWeight: 700,
      fontSize: '3rem', // 48sp
    },
    h2: {
      fontFamily: '"Plus Jakarta Sans", sans-serif',
      fontWeight: 600,
      fontSize: '2rem', // 32sp
    },
    h4: {
      fontFamily: '"Plus Jakarta Sans", sans-serif',
      fontWeight: 600,
      fontSize: '1.25rem', // 20sp
    },
    h5: {
      fontFamily: '"Plus Jakarta Sans", sans-serif',
      fontWeight: 600,
      fontSize: '1.125rem',
    },
    h6: {
      fontFamily: '"Plus Jakarta Sans", sans-serif',
      fontWeight: 600,
      fontSize: '0.875rem', // 14sp (titleSmall)
    },
    body1: {
      fontSize: '1.125rem', // 18sp
      lineHeight: 1.55,
    },
    body2: {
      fontSize: '1rem', // 16sp
      lineHeight: 1.5,
    },
    caption: {
      fontSize: '0.875rem', // 14sp
    },
    button: {
      textTransform: 'none',
      fontWeight: 600,
    },
  },
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 24,
          padding: '8px 20px',
        },
        containedPrimary: {
          backgroundColor: TriProColors.Primary,
          '&:hover': {
            backgroundColor: '#002B5B',
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          border: `1px solid ${TriProColors.CardBorder}`,
          boxShadow: 'none',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        rounded: {
          borderRadius: 16,
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          fontWeight: 500,
        },
      },
    },
  },
};

export const theme = createTheme(themeOptions);
