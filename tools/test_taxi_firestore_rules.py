"""Pruebas de permisos contra un emulador local, sin credenciales ni datos reales.

Uso: python3 tools/test_taxi_firestore_rules.py --jar /tmp/firestore-emulator.jar --java /ruta/java
El emulador se inicia en un puerto libre y se detiene al terminar. Requiere Java 21+.
"""
import argparse
import base64
import concurrent.futures
import json
from pathlib import Path
import socket
import subprocess
import tempfile
import time
import unittest
import urllib.error
import urllib.request

PROJECT = "demo-iotelito-taxi"
ROOT = Path(__file__).resolve().parents[1]
URL = ""
DOCS = f"projects/{PROJECT}/databases/(default)/documents"


def token(uid):
    def encode(value):
        return base64.urlsafe_b64encode(json.dumps(value).encode()).decode().rstrip("=")
    now = int(time.time())
    return encode({"alg": "none", "typ": "JWT"}) + "." + encode({
        "sub": uid, "user_id": uid, "aud": PROJECT,
        "iss": f"https://securetoken.google.com/{PROJECT}",
        "iat": now, "exp": now + 3600, "auth_time": now,
        "email": f"{uid}@example.test", "email_verified": True,
        "firebase": {"sign_in_provider": "password", "identities": {}},
    }) + "."


def request(method, path, data=None, auth="owner"):
    headers = {"Content-Type": "application/json"}
    if auth is not None:
        headers["Authorization"] = "Bearer " + ("owner" if auth == "owner" else token(auth))
    req = urllib.request.Request(URL + path,
        data=None if data is None else json.dumps(data).encode(), headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            content = response.read()
            return response.status, json.loads(content) if content else {}
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read())


def value(data):
    if data is None: return {"nullValue": None}
    if isinstance(data, bool): return {"booleanValue": data}
    if isinstance(data, int): return {"integerValue": str(data)}
    return {"stringValue": data}


def write(path, fields, merge=False, transforms=()):
    result = {"update": {"name": DOCS + "/" + path,
                         "fields": {k: value(v) for k, v in fields.items()}}}
    if merge: result["updateMask"] = {"fieldPaths": list(fields)}
    if transforms:
        result["updateTransforms"] = [{"fieldPath": k, "setToServerValue": "REQUEST_TIME"} for k in transforms]
    return result


def commit(writes, auth):
    return request("POST", f"/v1/projects/{PROJECT}/databases/(default)/documents:commit",
                   {"writes": writes}, auth)


class TaxiRulesTest(unittest.TestCase):
    def setUp(self):
        status, body = request("DELETE", f"/emulator/v1/projects/{PROJECT}/databases/(default)/documents")
        self.assertEqual(200, status, body)
        # Contrato integrado: toda cuenta de la app tiene perfil users; taxistas es operativo.
        for uid in ("client1", "client2", "driver1", "driver2"):
            self.ok(commit([write("users/" + uid, {"uid": uid, "active": True,
                "role": "TAXISTA" if uid.startswith("driver") else "CLIENT",
                "name": uid, "email": uid + "@example.test"})], "owner"))
        for uid in ("driver1", "driver2"):
            self.ok(commit([write("taxistas/" + uid,
                {"nombre": uid, "activo": True, "disponible": True})], "owner"))
        for rid in (1000001, 1000002):
            self.ok(commit([write(f"reservations/{rid}", {"clientUid": "client1", "id": rid})], "owner"))

    def ok(self, result):
        self.assertEqual(200, result[0], result[1])

    def denied(self, result):
        self.assertEqual(403, result[0], result[1])

    def create(self, rid=1000001, auth="client1", **changes):
        fields = dict(id=rid, reserva=rid, clientUid="client1", cliente="client1@example.test",
            nombre="Cliente", hotel="Hotel", origen="Origen", destino="Aeropuerto Jorge Chávez",
            hora="12:30", pasajeros=2, maletas=1, estado="SOLICITADO", taxistaUid="",
            taxista="", conductor="", finalizado=None)
        fields.update(changes)
        return commit([write(f"taxiRequests/{rid}", fields, transforms=("creado",))], auth)

    def accept(self, rid=1000001, driver="driver1"):
        return commit([
            write(f"taxiRequests/{rid}", {"estado": "ASIGNADO", "taxistaUid": driver,
                "taxista": f"{driver}@example.test", "conductor": driver}, merge=True),
            write(f"taxiDrivers/{driver}", {"activeRequestId": str(rid)}),
        ], driver)

    def advance(self, state, rid=1000001, driver="driver1"):
        writes = [write(f"taxiRequests/{rid}", {"estado": state}, merge=True,
                        transforms=("finalizado",) if state == "FINALIZADO" else ())]
        if state == "FINALIZADO": writes.append(write(f"taxiDrivers/{driver}", {"activeRequestId": ""}))
        return commit(writes, driver)

    def test_cliente_sin_perfil_taxista_puede_consultar_su_documento_inexistente(self):
        status, body = request("GET", f"/v1/{DOCS}/taxistas/client1", auth="client1")
        self.assertEqual(404, status, body)  # Lectura autorizada, documento ausente.

    def test_permisos_previos_de_catalogo_y_reservas_se_conservan(self):
        self.ok(commit([write("hotels/1", {"name": "Hotel de prueba"})], "owner"))
        self.ok(request("GET", f"/v1/{DOCS}/hotels/1", auth="client1"))
        self.denied(request("GET", f"/v1/{DOCS}/hotels/1", auth=None))
        self.ok(request("GET", f"/v1/{DOCS}/reservations/1000001", auth="client1"))
        self.denied(request("GET", f"/v1/{DOCS}/reservations/1000001", auth="client2"))
        self.denied(commit([write("hotels/1", {"name": "Modificado"}, merge=True)], "client1"))

    def test_cliente_crea_y_lee_su_solicitud(self):
        self.ok(self.create())
        self.ok(request("GET", f"/v1/{DOCS}/taxiRequests/1000001", auth="client1"))
        self.denied(request("GET", f"/v1/{DOCS}/taxiRequests/1000001", auth="client2"))

    def test_no_crea_solicitudes_sin_reserva_propia(self):
        self.denied(self.create(auth="client2"))
        self.denied(self.create(rid=1999999))
        self.denied(self.create(auth=None))

    def test_no_inventa_estado_ni_taxista_ni_hora_invalida(self):
        self.denied(self.create(estado="ASIGNADO"))
        self.denied(self.create(taxistaUid="driver1"))
        self.denied(self.create(hora="99:99"))

    def test_solo_taxista_activo_disponible_acepta(self):
        self.ok(self.create())
        self.ok(commit([write("taxistas/driver1", {"disponible": False}, merge=True)], "owner"))
        self.denied(self.accept())
        self.denied(self.accept(driver="client1"))
        self.ok(commit([write("taxistas/driver1", {"activo": False, "disponible": True}, merge=True)], "owner"))
        self.denied(self.accept())

    def test_aceptacion_exige_bloqueo_atomico(self):
        self.ok(self.create())
        self.denied(commit([write("taxiRequests/1000001", {"estado": "ASIGNADO", "taxistaUid": "driver1",
            "taxista": "driver1@example.test", "conductor": "Driver"}, merge=True)], "driver1"))
        self.ok(self.accept())

    def test_viaje_completo_libera_bloqueo(self):
        self.ok(self.create()); self.ok(self.accept())
        self.ok(self.advance("EN_CAMINO")); self.ok(self.advance("EN_TRASLADO")); self.ok(self.advance("FINALIZADO"))
        self.ok(self.create(1000002)); self.ok(self.accept(1000002))

    def test_no_salta_pasos_ni_avanza_servicio_ajeno(self):
        self.ok(self.create()); self.ok(self.accept())
        self.denied(self.advance("FINALIZADO"))
        self.denied(self.advance("EN_CAMINO", driver="driver2"))
        self.denied(commit([write("taxiRequests/1000001", {"estado": "EN_CAMINO"}, merge=True)], "client1"))

    def test_no_modifica_datos_del_pasajero(self):
        self.ok(self.create()); self.ok(self.accept())
        self.denied(commit([write("taxiRequests/1000001", {"estado": "EN_CAMINO", "origen": "Otro"}, merge=True)], "driver1"))

    def test_finalizar_exige_liberar_el_bloqueo(self):
        self.ok(self.create()); self.ok(self.accept()); self.ok(self.advance("EN_CAMINO")); self.ok(self.advance("EN_TRASLADO"))
        self.denied(commit([write("taxiRequests/1000001", {"estado": "FINALIZADO"}, merge=True,
                                 transforms=("finalizado",))], "driver1"))

    def test_solo_un_taxista_gana_el_mismo_pedido(self):
        self.ok(self.create())
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(lambda driver: self.accept(driver=driver), ("driver1", "driver2")))
        self.assertEqual([200, 403], sorted(status for status, _ in results), results)

    def test_un_taxista_no_acepta_dos_viajes_concurrentes(self):
        self.ok(self.create()); self.ok(self.create(1000002))
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(self.accept, (1000001, 1000002)))
        self.assertEqual([200, 403], sorted(status for status, _ in results), results)

    def test_disponibilidad_propia_solo_booleano(self):
        self.ok(commit([write("taxistas/driver1", {"disponible": False}, merge=True)], "driver1"))
        self.denied(commit([write("taxistas/driver1", {"disponible": "true"}, merge=True)], "driver1"))
        self.denied(commit([write("taxistas/driver1", {"activo": False}, merge=True)], "driver1"))
        self.denied(commit([write("taxistas/driver2", {"disponible": False}, merge=True)], "driver1"))

    def test_disponibilidad_apagada_no_impide_terminar_viaje_actual(self):
        self.ok(self.create()); self.ok(self.accept())
        self.ok(commit([write("taxistas/driver1", {"disponible": False}, merge=True)], "driver1"))
        self.ok(self.advance("EN_CAMINO")); self.ok(self.advance("EN_TRASLADO")); self.ok(self.advance("FINALIZADO"))

    def test_consultas_solicitudes_e_historial_y_rechaza_listado_global(self):
        self.ok(self.create())
        for field, term in (("estado", "SOLICITADO"), ("taxistaUid", "driver1")):
            query = {"structuredQuery": {"from": [{"collectionId": "taxiRequests"}], "where": {
                "fieldFilter": {"field": {"fieldPath": field}, "op": "EQUAL", "value": value(term)}}}}
            self.ok(request("POST", f"/v1/{DOCS}:runQuery", query, "driver1"))
        self.denied(request("POST", f"/v1/{DOCS}:runQuery",
            {"structuredQuery": {"from": [{"collectionId": "taxiRequests"}]}}, "driver1"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", required=True)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0)); port = sock.getsockname()[1]
    URL = f"http://127.0.0.1:{port}"
    with tempfile.TemporaryDirectory(prefix="iotelito-rules-") as directory:
        with open(Path(directory) / "emulator.log", "w+") as log:
            process = subprocess.Popen([args.java, "-Duser.language=en", "-Duser.country=US", "-jar", str(Path(args.jar).resolve()),
                "--host", "127.0.0.1", "--port", str(port), "--project_id", PROJECT,
                "--rules", str(ROOT / "firestore.rules")], stdout=log, stderr=log, cwd=directory)
            try:
                for _ in range(100):
                    if process.poll() is not None:
                        log.seek(0); raise RuntimeError(log.read())
                    try:
                        with urllib.request.urlopen(URL, timeout=1): pass
                        break
                    except urllib.error.HTTPError: break
                    except OSError: time.sleep(0.2)
                else: raise RuntimeError("El emulador no inició")
                status, body = request("PUT", f"/emulator/v1/projects/{PROJECT}:securityRules",
                    {"rules": {"files": [{"name": "firestore.rules", "content": (ROOT / "firestore.rules").read_text(encoding="utf-8")}]}})
                if status != 200: raise RuntimeError(body)
                result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(TaxiRulesTest))
                if not result.wasSuccessful(): raise SystemExit(1)
            finally:
                process.terminate()
                try: process.wait(timeout=10)
                except subprocess.TimeoutExpired: process.kill(); process.wait()
