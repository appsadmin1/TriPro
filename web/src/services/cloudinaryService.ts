import axios from 'axios';
import { Attachment } from '../data/models';
import { v4 as uuidv4 } from 'uuid';

const CLOUD_NAME = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME;
const UPLOAD_PRESET = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET;

export const uploadAttachment = async (
  file: File,
  uploadedBy: string
): Promise<Attachment> => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('upload_preset', UPLOAD_PRESET);

  const url = `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/auto/upload`;

  try {
    const response = await axios.post(url, formData);
    const data = response.data;

    return {
      id: uuidv4(),
      fileName: file.name,
      downloadUrl: data.secure_url,
      publicId: data.public_id,
      resourceType: data.resource_type || 'raw',
      mimeType: file.type,
      uploadedBy: uploadedBy,
      uploadedAtMillis: Date.now(),
    };
  } catch (error) {
    console.error('Cloudinary upload failed:', error);
    throw new Error('Cloudinary upload failed');
  }
};
