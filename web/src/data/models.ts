export interface PickedPlace {
  name: string;
  address: string;
  lat: number;
  lng: number;
  placeId?: string;
}

export enum Role {
  OWNER = "owner",
  EDITOR = "editor",
  VIEWER = "viewer",
}

export enum ItemType {
  HOTEL = "HOTEL",
  RESTAURANT = "RESTAURANT",
  ATTRACTION = "ATTRACTION",
  ACTIVITY = "ACTIVITY",
  SHOW = "SHOW",
  TRANSPORT = "TRANSPORT",
  FLIGHT = "FLIGHT",
  CUSTOM = "CUSTOM",
}

export enum MarkerColorKey {
  HOTEL = "HOTEL",
  RESTAURANT = "RESTAURANT",
  ATTRACTION = "ATTRACTION",
  ACTIVITY = "ACTIVITY",
  SHOW = "SHOW",
  TRANSPORT = "TRANSPORT",
  FLIGHT = "FLIGHT",
  CUSTOM = "CUSTOM",
  DEFAULT = "DEFAULT",
}

export const MarkerColorPalette = [
  "#3B82F6", "#EF4444", "#F59E0B", "#10B981", "#8B5CF6", "#EC4899", "#6366F1", "#14B8A6",
  "#F97316", "#06B6D4", "#84CC16", "#D946EF", "#64748B", "#475569", "#1E293B", "#000000"
];

export interface ActivityColorPrefs {
  colors: Record<string, string>;
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
  customLabel?: string;
  flightInfo?: FlightInfo;
  hotelInfo?: HotelInfo;
  attachments: Attachment[];
  order: number;
  tripId: string;
  createdBy: string;
  updatedBy: string;
}

export interface UserProfile {
  uid: string;
  email: string;
  displayName: string;
  photoUrl: string;
}

export interface NotificationPreferences {
  tripInvites: boolean;
  itineraryChanges: boolean;
  dayInfoChanges: boolean;
}

export enum WeatherStatus {
  AVAILABLE = "AVAILABLE",
  NOT_YET_AVAILABLE = "NOT_YET_AVAILABLE",
  NO_LOCATION = "NO_LOCATION",
  ERROR = "ERROR",
}

export interface DailyWeather {
  date: string;
  status: WeatherStatus;
  weatherCode?: number;
  tempMaxC?: number;
  tempMinC?: number;
  precipitationProbabilityPct?: number;
}

export enum ActivityType {
  ITEM_ADDED = "ITEM_ADDED",
  ITEM_UPDATED = "ITEM_UPDATED",
  ITEM_REMOVED = "ITEM_REMOVED",
  HOTEL_UPDATED = "HOTEL_UPDATED",
  FLIGHT_UPDATED = "FLIGHT_UPDATED",
  DAY_NOTE_UPDATED = "DAY_NOTE_UPDATED",
  MEMBER_INVITED = "MEMBER_INVITED",
  MEMBER_ROLE_CHANGED = "MEMBER_ROLE_CHANGED",
  MEMBER_REMOVED = "MEMBER_REMOVED",
}

export interface ActivityEntry {
  id: string;
  tripId: string;
  tripName: string;
  date?: string;
  type: ActivityType;
  message: string;
  actorUid: string;
  actorName: string;
  memberIds: string[];
  createdAt?: any;
}
