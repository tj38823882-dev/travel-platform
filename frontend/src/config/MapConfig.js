import { googleMapsApiKey } from "./appConfig.js";

export const API_KEY = googleMapsApiKey;

export const MAP_OPTIONS = {
  center: { lat: 22.6273, lng: 120.3014 },
  zoom: 13,
  disableDefaultUI: false,
  zoomControl: true,
  mapTypeControl: false,
  streetViewControl: false,
  fullscreenControl: true,
  styles: [
    { featureType: "poi", elementType: "labels", stylers: [{ visibility: "on" }] },
    { featureType: "transit", elementType: "labels.icon", stylers: [{ visibility: "on" }] }
  ]
};

export const MARKER_LABEL_STYLE = {
  color: "#fff",
  fontWeight: "700"
};

export const MARKER_ICON_URL = "http://maps.google.com/mapfiles/ms/icons/red-dot.png";
export const MARKER_ICON_SIZE = { width: 32, height: 50 };
