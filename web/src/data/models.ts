export enum Role {
  OWNER = "owner",
  EDITOR = "editor",
  VIEWER = "viewer",
}

export enum ItemType {
  FLIGHT = "FLIGHT",
  HOTEL = "HOTEL",
  RESTAURANT = "RESTAURANT",
  ATTRACTION = "ATTRACTION",
  ACTIVITY = "ACTIVITY",
  TRANSPORT = "TRANSPORT",
  SHOW = "SHOW",
  CUSTOM = "CUSTOM",
}

export enum TimeType {
  EXACT = "EXACT",
  RANGE = "RANGE",
  PERIOD = "PERIOD",
}

export enum DayPeriod {
  MORNING = "MORNING",
  NOON = "NOON",
  AFTERNOON = "AFTERNOON",
  EVENING = "EVENING",
  NIGHT = "NIGHT",
}

export enum NoteType {
  ALERT = "ALERT",
  NOTE = "NOTE",
}

export interface Attachment {
  id: string;
  fileName: string;
  downloadUrl: string;
  publicId: string;
  resourceType: string;
  mimeType: string;
  uploadedBy: string;
  uploadedAtMillis: number;
}

export interface HotelInfo {
  name: string;
  address: string;
  lat?: number;
  lng?: number;
  placeId?: string;
  checkIn: string;
  checkOut: string;
  arrivalTime: string;
  notes: string;
  noteType: NoteType;
  attachments: Attachment[];
}

export interface FlightInfo {
  airline: string;
  flightNumber: string;
  departureAirportCode: string;
  arrivalAirportCode: string;
  departureAirportLat?: number;
  departureAirportLng?: number;
  arrivalAirportLat?: number;
  arrivalAirportLng?: number;
  departureTime: string;
  arrivalTime: string;
  notes: string;
  noteType: NoteType;
  attachments: Attachment[];
}

export interface Trip {
  id: string;
  name: string;
  destination: string;
  coverImageUrl: string;
  coverImagePublicId: string;
  coverImageResourceType: string;
  startDate: string; // ISO-8601 yyyy-MM-dd
  endDate: string;
  ownerId: string;
  ownerName: string;
  members: Record<string, string>;
  memberIds: string[];
  createdAt?: any; // Firestore Timestamp
}

export interface TripDay {
  date: string; // yyyy-MM-dd
  dayIndex: number;
  dayNote: string;
  updatedBy: string;
}

export interface ItineraryItem {
  id: string;
  title: string;
  type: ItemType;
  timeType: TimeType;
  startTime?: string;
  endTime?: string;
  period?: DayPeriod;
  locationName: string;
  address: string;
  lat?: number;
  lng?: number;
  note: string;
  noteType: NoteType;
  customLabel: string;
  flightInfo?: FlightInfo;
  hotelInfo?: HotelInfo;
  attachments: Attachment[];
  order: number;
  tripId: string;
  createdBy: string;
  updatedBy: string;
}
