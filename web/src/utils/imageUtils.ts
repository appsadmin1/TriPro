/**
 * Transforms a Cloudinary URL to include optimization parameters and ensure
 * browser-supported formats (like converting .heic to .jpg/.webp via f_auto).
 */
export const getOptimizedImageUrl = (url: string | undefined): string => {
  if (!url) return 'https://via.placeholder.com/400x200?text=No+Image';

  // If it's a Cloudinary URL, we can inject transformation parameters
  if (url.includes('res.cloudinary.com')) {
    // Cloudinary URLs usually look like: .../upload/v1234567/public_id.ext
    // We want to insert 'f_auto,q_auto/' after '/upload/'
    if (url.includes('/upload/') && !url.includes('/f_auto')) {
      return url.replace('/upload/', '/upload/f_auto,q_auto/');
    }
  }

  return url;
};
