// ============================================================
// File: MapPicker.jsx
// Purpose: Shared interactive map modal for picking a lat/lng pair
//          (Leaflet + OpenStreetMap tiles - no API key required).
//          Used by StationForm to fill GPS coordinates visually; it
//          only ever confirms back plain decimal numbers, matching
//          the backend's lat/lng contract, and never overrides the
//          form fields directly - the parent still owns that state.
// Author: Shalon
// ============================================================
import { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import Modal from './Modal.jsx';
import Button from './Button.jsx';
import Input from './Input.jsx';

const DEFAULT_CENTER = { lat: 6.9271, lng: 79.8612 }; // Sri Lanka
const DEFAULT_ZOOM = 8;
const SELECTED_ZOOM = 13;

// Inline SVG pin so we don't depend on Leaflet's default marker image assets, which don't
// resolve correctly out of the box under Vite's bundler without extra asset config.
const PIN_ICON = L.divIcon({
  className: '',
  html: `<svg width="28" height="36" viewBox="0 0 28 36" xmlns="http://www.w3.org/2000/svg">
    <path d="M14 0C6.27 0 0 6.27 0 14c0 10.5 14 22 14 22s14-11.5 14-22C28 6.27 21.73 0 14 0z" fill="#0F172A"/>
    <circle cx="14" cy="14" r="5.5" fill="#ffffff"/>
  </svg>`,
  iconSize: [28, 36],
  iconAnchor: [14, 36],
});

// Places the marker wherever the user clicks on the map.
function ClickHandler({ onPick }) {
  useMapEvents({
    click(event) {
      onPick({ lat: event.latlng.lat, lng: event.latlng.lng });
    },
  });
  return null;
}

// Recenters the map imperatively whenever `target` changes (e.g. after a search result is
// picked) - MapContainer's own `center` prop only applies on first mount, not on updates.
function Recenter({ target, zoom }) {
  const map = useMap();
  useEffect(() => {
    if (target) {
      map.setView([target.lat, target.lng], zoom);
    }
  }, [target, zoom, map]);
  return null;
}

// Rounds to 6 decimal places (~0.1m precision) as a plain number, matching the backend's
// lat/lng number contract (not a formatted string).
function roundCoordinate(value) {
  return Math.round(value * 1e6) / 1e6;
}

// Derives the marker's starting position from the form's current lat/lng, falling back to the
// default Sri Lanka center when they're empty/invalid.
function toStartPosition(initialLat, initialLng) {
  const hasValidStart = Number.isFinite(initialLat) && Number.isFinite(initialLng);
  return hasValidStart ? { lat: initialLat, lng: initialLng } : DEFAULT_CENTER;
}

// Modal for visually picking a station's GPS position; confirms back a { lat, lng } number pair.
// The parent (StationForm) remounts this component via a changing `key` each time it opens, so
// state below is simply derived once from props at mount - no effect-based reset needed.
export default function MapPicker({ open, initialLat, initialLng, onClose, onConfirm }) {
  const [position, setPosition] = useState(() => toStartPosition(initialLat, initialLng));
  const [recenterTarget, setRecenterTarget] = useState(() => toStartPosition(initialLat, initialLng));
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState('');

  // Looks up a place name via OpenStreetMap's free Nominatim search (no API key required).
  // Runs only on explicit submit, not per keystroke, per Nominatim's fair-use policy.
  const handleSearch = async (event) => {
    event.preventDefault();
    const query = searchQuery.trim();
    if (!query) {
      return;
    }
    setSearching(true);
    setSearchError('');
    setSearchResults([]);
    try {
      const response = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&limit=5&q=${encodeURIComponent(query)}`,
      );
      if (!response.ok) {
        throw new Error('Search request failed.');
      }
      const results = await response.json();
      setSearchResults(results);
      if (results.length === 0) {
        setSearchError('No locations found for that search.');
      }
    } catch {
      setSearchError('Could not reach the location search service.');
    } finally {
      setSearching(false);
    }
  };

  // Selects a search result: moves the marker and recenters the map there.
  const handleSelectResult = (result) => {
    const next = { lat: Number(result.lat), lng: Number(result.lon) };
    setPosition(next);
    setRecenterTarget(next);
    setSearchResults([]);
    setSearchQuery(result.display_name);
  };

  // Confirms the current marker position back to the parent form as rounded plain numbers.
  const handleConfirm = () => {
    onConfirm({ lat: roundCoordinate(position.lat), lng: roundCoordinate(position.lng) });
  };

  if (!open) {
    return null;
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Pick location on map"
      description="Click or drag the pin, or search for a place. Coordinates update live below."
    >
      <form onSubmit={handleSearch} className="mb-3 flex gap-2">
        <Input
          placeholder="Search for a place or address"
          aria-label="Search for a location"
          value={searchQuery}
          onChange={(event) => setSearchQuery(event.target.value)}
          className="flex-1"
        />
        <Button type="submit" variant="secondary" disabled={searching}>
          {searching ? 'Searching…' : 'Search'}
        </Button>
      </form>

      {searchError && <p className="mb-3 text-[13px] text-error">{searchError}</p>}

      {searchResults.length > 0 && (
        <ul className="mb-3 max-h-36 divide-y divide-line overflow-y-auto rounded-md border border-line bg-surface-alt">
          {searchResults.map((result) => (
            <li key={result.place_id}>
              <button
                type="button"
                onClick={() => handleSelectResult(result)}
                className="block w-full px-3 py-2 text-left text-[13px] text-body transition-colors hover:bg-surface hover:text-ink"
              >
                {result.display_name}
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="h-80 w-full overflow-hidden rounded-md border border-line sm:h-96">
        <MapContainer
          center={[position.lat, position.lng]}
          zoom={Number.isFinite(initialLat) && Number.isFinite(initialLng) ? SELECTED_ZOOM : DEFAULT_ZOOM}
          style={{ height: '100%', width: '100%' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <Marker
            position={[position.lat, position.lng]}
            icon={PIN_ICON}
            draggable
            eventHandlers={{
              dragend: (event) => {
                const latlng = event.target.getLatLng();
                setPosition({ lat: latlng.lat, lng: latlng.lng });
              },
            }}
          />
          <ClickHandler onPick={setPosition} />
          <Recenter target={recenterTarget} zoom={SELECTED_ZOOM} />
        </MapContainer>
      </div>

      <div className="mt-3 flex items-center justify-between rounded-md border border-line bg-surface-alt px-3 py-2">
        <span className="font-mono text-[13px] text-ink">
          {position.lat.toFixed(6)}, {position.lng.toFixed(6)}
        </span>
        <span className="text-[11px] text-muted">Click or drag the pin to adjust</span>
      </div>

      <div className="-mx-6 -mb-5 mt-5 flex justify-end gap-2 border-t border-line bg-surface-alt px-6 py-4">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button type="button" variant="primary" onClick={handleConfirm}>
          Confirm location
        </Button>
      </div>
    </Modal>
  );
}
