import { auth } from "../firebase";

export interface FlightLookupResult {
  airline: string;
  flightNumber: string;
  departureAirportCode: string;
  departureAirportLat?: number;
  departureAirportLng?: number;
  departureTime: string; // "HH:mm"
  arrivalAirportCode: string;
  arrivalAirportLat?: number;
  arrivalAirportLng?: number;
  arrivalTime: string; // "HH:mm"
}

export const flightService = {
  lookupFlight: async (flightNumber: string, date: string): Promise<FlightLookupResult> => {
    const user = auth.currentUser;
    if (!user) {
      throw new Error("You need to be signed in to look up a flight.");
    }

    const idToken = await user.getIdToken(true);
    const response = await fetch("/.netlify/functions/flight-lookup", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${idToken}`,
      },
      body: JSON.stringify({ flightNumber, date }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `HTTP ${response.status}`);
    }

    const data = await response.json();
    return parseFlight(data.flight);
  },
};

function parseFlight(flight: any): FlightLookupResult {
  const departure = flight.departure;
  const arrival = flight.arrival;
  const departureAirport = departure.airport;
  const arrivalAirport = arrival.airport;
  const airline = flight.airline;

  return {
    airline: airline?.name || "",
    flightNumber: (flight.number || "").trim(),
    departureAirportCode: (departureAirport?.iata || "").toUpperCase(),
    departureAirportLat: departureAirport?.location?.lat,
    departureAirportLng: departureAirport?.location?.lon,
    departureTime: localTimeOf(departure.scheduledTime?.local),
    arrivalAirportCode: (arrivalAirport?.iata || "").toUpperCase(),
    arrivalAirportLat: arrivalAirport?.location?.lat,
    arrivalAirportLng: arrivalAirport?.location?.lon,
    arrivalTime: localTimeOf(arrival.scheduledTime?.local),
  };
}

function localTimeOf(local?: string): string {
  if (!local) return "";
  // AeroDataBox "local": "2026-08-02 15:35+02:00" -> "15:35"
  const parts = local.split(" ");
  if (parts.length < 2) return "";
  return parts[1].substring(0, 5);
}
