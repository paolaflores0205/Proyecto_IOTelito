const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

// Ejecuta el script real del selector, sin depender de internet ni del geocodificador.
const html = fs.readFileSync(path.join(__dirname, '../../main/assets/location_picker.html'), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
const listeners = {};
const reports = [];
let center;
let lastOptions;
const point = value => Array.isArray(value) ? {lat: value[0], lng: value[1]} : value;
const radians = degrees => degrees * Math.PI / 180;
const map = {
  setView(value, zoom, options) {
    center = point(value);
    lastOptions = options;
    if (listeners.moveend) listeners.moveend();
    return this;
  },
  getCenter() { return center; },
  distance(a, b) {
    a = point(a); b = point(b);
    const deltaLat = radians(b.lat - a.lat), deltaLng = radians(b.lng - a.lng);
    const h = Math.sin(deltaLat / 2) ** 2 + Math.cos(radians(a.lat))
      * Math.cos(radians(b.lat)) * Math.sin(deltaLng / 2) ** 2;
    return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, h)));
  },
  on(event, callback) { listeners[event] = callback; return this; },
  panTo(value) { return this.setView(value); }
};
const window = {
  location: {search: '?lat=-12.0464&lng=-77.0428'},
  Android: {onMapMoved(lat, lng) { reports.push([lat, lng]); }}
};
vm.runInNewContext(script, {
  window, URLSearchParams, L: {map: () => map, tileLayer: () => ({addTo() {}})}
});
assert.equal(reports.length, 1, 'La carga inicial informa el punto una sola vez');
for (let i = 0; i < 30; i++) listeners.moveend();
assert.equal(reports.length, 1, 'Un resize sin cambio de punto no reinicia la selección');
center = {lat: -12.046390, lng: -77.0428};
listeners.moveend();
assert.equal(reports.length, 1, 'El redondeo de un píxel no reinicia la selección');
window.selectPoint(-12.047, -77.043);
assert.equal(reports.length, 2, 'Buscar otra dirección informa exactamente un punto nuevo');
assert.equal(lastOptions.animate, false, 'No se geocodifican puntos intermedios de una animación');
window.selectPoint(-12.047, -77.043);
assert.equal(reports.length, 2, 'El reporte explícito y moveend no se duplican');
listeners.click({latlng: {lat: -12.048, lng: -77.044}});
assert.equal(reports.length, 3, 'Tocar otro lugar todavía cambia la selección');
console.log('Selector del mapa: 7 comprobaciones correctas.');
