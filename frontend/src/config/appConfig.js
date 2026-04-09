const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const wsUrl = import.meta.env.VITE_WS_URL || "ws://localhost:8080/ws";
const googleMapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || "";
const googleOAuthUrl =
  import.meta.env.VITE_GOOGLE_OAUTH_URL ||
  `${apiBaseUrl}/oauth2/authorization/google`;

export { apiBaseUrl, wsUrl, googleMapsApiKey, googleOAuthUrl };
