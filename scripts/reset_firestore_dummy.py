#!/usr/bin/env python3
"""
Reset + seed dummy data Firestore untuk kebutuhan testing/skripsi.

Menghapus SELURUH dokumen di koleksi: users, katalog, mutasi, transfer, proyek,
lalu mengisi ulang dengan dataset dummy yang konsisten (skema + scoping per proyek).

Format ID mengikuti yang app hasilkan (UuidGenerator):
  - sku_id       : "SKU-" + 8 hex UUID (mis. SKU-3F9A2B1C)
  - id_transaksi : UUID penuh
  - id_transfer  : UUID penuh
  - id_mandor    : Firebase Auth UID (opaque) — dummy mandor lain pakai id acak
  - id_proyek    : business key/kode dari registry admin (mis. PRJ-001) — SENGAJA bukan UUID
  - nama koleksi : konstanta tetap (bukan UUID)

Kredensial TIDAK disimpan di file ini:
  - API key & project_id dibaca dari google-services.json (client key, sudah publik).
  - Email & password diberikan lewat argumen atau environment variable.

Pakai:
  python scripts/reset_firestore_dummy.py <email> <password>
  # atau
  FIREBASE_EMAIL=... FIREBASE_PASSWORD=... python scripts/reset_firestore_dummy.py

PERINGATAN: destruktif — menghapus semua dokumen di kelima koleksi sebelum seeding.
Data pada Firebase Auth (akun login) TIDAK terpengaruh; hanya dokumen Firestore.
"""
import json, os, sys, time, uuid, urllib.request, urllib.error
from datetime import datetime, timezone

# ----------------------------------------------------------------------------
# Konfigurasi & kredensial
# ----------------------------------------------------------------------------
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
with open(os.path.join(ROOT, "google-services.json"), encoding="utf-8") as f:
    gs = json.load(f)
PROJECT = gs["project_info"]["project_id"]
API_KEY = gs["client"][0]["api_key"][0]["current_key"]

EMAIL = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("FIREBASE_EMAIL")
PASSWORD = sys.argv[2] if len(sys.argv) > 2 else os.environ.get("FIREBASE_PASSWORD")
if not EMAIL or not PASSWORD:
    sys.exit("Kredensial kosong. Beri lewat argumen <email> <password> atau env FIREBASE_EMAIL/FIREBASE_PASSWORD.")

COLLECTIONS = ["users", "katalog", "mutasi", "transfer", "proyek"]

# Mandor pemilik data uji (PRJ-001). uid = Firebase Auth uid akun di atas.
PJ = "Christopher Benaya"
NOW_MS = int(time.time() * 1000)
NOW_TS = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ")

# ----------------------------------------------------------------------------
# HTTP helpers
# ----------------------------------------------------------------------------
def _req(method, url, payload=None, headers=None):
    data = json.dumps(payload).encode() if payload is not None else None
    r = urllib.request.Request(url, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    for k, v in (headers or {}).items():
        r.add_header(k, v)
    with urllib.request.urlopen(r) as resp:
        body = resp.read().decode()
        return json.loads(body) if body else {}

def sign_in():
    res = _req("POST",
        f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={API_KEY}",
        {"email": EMAIL, "password": PASSWORD, "returnSecureToken": True})
    return res["idToken"], res["localId"]

# ----------------------------------------------------------------------------
# Firestore value encoders + generator ID (mengikuti UuidGenerator app)
# ----------------------------------------------------------------------------
def s(v):  return {"stringValue": v}
def i(v):  return {"integerValue": str(v)}
def ts(v): return {"timestampValue": v}
def nul(): return {"nullValue": None}

def new_sku(): return "SKU-" + uuid.uuid4().hex[:8].upper()   # == UuidGenerator.generateSkuId()
def new_id():  return str(uuid.uuid4())                        # == UuidGenerator.generateId()

# ----------------------------------------------------------------------------
# Delete + seed
# ----------------------------------------------------------------------------
def delete_all(base, coll, hdr):
    """Hapus semua dokumen di koleksi (dengan paginasi)."""
    deleted = 0
    page_token = None
    while True:
        url = f"{base}/{coll}?pageSize=300"
        if page_token:
            url += f"&pageToken={page_token}"
        res = _req("GET", url, headers=hdr)
        for doc in res.get("documents", []):
            name = doc["name"]  # full resource path
            del_url = f"https://firestore.googleapis.com/v1/{name}"
            try:
                _req("DELETE", del_url, headers=hdr)
                deleted += 1
            except urllib.error.HTTPError as e:
                print(f"  ! gagal hapus {name.split('/documents/')[-1]}: {e.code}")
        page_token = res.get("nextPageToken")
        if not page_token:
            break
    print(f"  hapus {coll}: {deleted} dokumen")

def put(base, coll, doc_id, fields, hdr):
    _req("PATCH", f"{base}/{coll}/{doc_id}", {"fields": fields}, hdr)

# ----------------------------------------------------------------------------
# Dataset dummy (ubah di sini bila perlu)
# ----------------------------------------------------------------------------
def seed(base, uid, hdr):
    def P(coll, doc_id, fields):
        put(base, coll, doc_id, fields, hdr)

    # id_mandor dummy (mandor PRJ-002) — bukan akun login, jadi id acak menyerupai UID
    dummy_mandor = uuid.uuid4().hex

    # --- proyek (registry) — id_proyek SENGAJA kode/business key, bukan UUID ---
    for pid, nama in [
        ("PRJ-001", "Proyek Tol Trans-Sumatera Seksi 2"),
        ("PRJ-002", "Pembangunan Jembatan Musi VI"),
        ("PRJ-003", "Gedung Pemerintahan Kota Bandar Lampung"),
        ("PRJ-004", "Bendungan Way Sekampung"),
        ("PROYEK-UTAMA", "Proyek Utama"),
    ]:
        P("proyek", pid, {"id_proyek": s(pid), "nama_proyek": s(nama)})

    # --- users ---
    P("users", uid, {"nama": s(PJ), "role": s("mandor"),
                     "id_proyek": s("PRJ-001"), "nama_proyek": s("Proyek Tol Trans-Sumatera Seksi 2")})
    P("users", dummy_mandor, {"nama": s("Andi Saputra"), "role": s("mandor"),
                     "id_proyek": s("PRJ-002"), "nama_proyek": s("Pembangunan Jembatan Musi VI")})

    # --- katalog: sku_id = UUID (format app). key = rujukan internal antar-koleksi ---
    # (key, kategori, nama, satuan, stok, min, proyek, updated_by)  — sebagian stok < min utk uji peringatan
    katalog_defs = [
        ("bor",        "alat",        "Bor Listrik Bosch GBM 13 RE", "unit",   8,   3, "PRJ-001", uid),
        ("gerinda",    "alat",        "Gerinda Tangan Makita 4 inch","unit",   2,   4, "PRJ-001", uid),
        ("genset",     "alat",        "Genset Honda 5 kVA",          "unit",   1,   1, "PRJ-001", uid),
        ("semen",      "bahan",       "Semen Portland 50kg",         "sak",  120,  50, "PRJ-001", uid),
        ("besi",       "bahan",       "Besi Beton Ulir 12mm",        "batang",40, 100, "PRJ-001", uid),
        ("pasir",      "bahan",       "Pasir Cor",                   "m3",    15,  10, "PRJ-001", uid),
        ("cat",        "bahan",       "Cat Tembok Dulux 25kg",       "pail",   6,   8, "PRJ-001", uid),
        ("mainframe",  "scaffolding", "Main Frame 1.7m",             "set",   60,  30, "PRJ-001", uid),
        ("crossbrace", "scaffolding", "Cross Brace 220cm",           "batang",25,  40, "PRJ-001", uid),
        ("jackbase",   "scaffolding", "Jack Base U-Head",            "buah", 100,  50, "PRJ-001", uid),
        # PRJ-002 — uji scoping (tidak boleh muncul utk mandor PRJ-001)
        ("p2_exca",    "alat",        "Excavator Bucket Cadangan",   "unit",   3,   1, "PRJ-002", dummy_mandor),
        ("p2_aspal",   "bahan",       "Aspal Hotmix",                "ton",   25,  10, "PRJ-002", dummy_mandor),
    ]
    sku = {}
    for key, kat, nama, satuan, stok, minim, proyek, by in katalog_defs:
        sid = new_sku()
        sku[key] = sid
        P("katalog", sid, {
            "sku_id": s(sid), "kategori": s(kat), "nama_barang": s(nama), "satuan": s(satuan),
            "stok_lokal": i(stok), "stok_minimum": i(minim), "id_proyek": s(proyek),
            "local_timestamp": i(NOW_MS), "sync_status": s("SYNCED"),
            "server_timestamp": ts(NOW_TS), "updated_by": s(by)})

    # --- mutasi PRJ-001: id_transaksi = UUID; sku_id merujuk katalog di atas ---
    # (katalog_key, jenis, jumlah, kondisi|None, lokasi, catatan|None, menit_lalu)
    mut = [
        ("semen",     "CHECK_IN",  100, None,          "",                    "Kedatangan dari supplier", 600),
        ("besi",      "CHECK_OUT",  20, None,          "Pier P12",            None,                       420),
        ("bor",       "CHECK_IN",    2, "BAIK",        "",                    None,                       300),
        ("gerinda",   "CHECK_OUT",   1, "RUSAK_RINGAN","Zona Girder",         "Perlu servis",             120),
        ("mainframe", "CHECK_OUT",  10, None,          "Bekisting Bentang 3", None,                        45),
    ]
    for key, jenis, jml, kondisi, lokasi, catatan, back in mut:
        idt = new_id()
        P("mutasi", idt, {
            "id_transaksi": s(idt), "sku_id": s(sku[key]), "id_mandor": s(uid), "id_proyek": s("PRJ-001"),
            "jenis_mutasi": s(jenis), "jumlah": i(jml),
            "kondisi_barang": (s(kondisi) if kondisi else nul()),
            "lokasi_penggunaan": s(lokasi), "penanggungjawab": s(PJ),
            "catatan": (s(catatan) if catatan else nul()),
            "local_timestamp": i(NOW_MS - back * 60_000), "sync_status": s("SYNCED"),
            "server_timestamp": ts(NOW_TS), "updated_by": s(uid)})

    # --- transfer: id_transfer = UUID ---
    # (katalog_key, proyek_asal, proyek_tujuan, jumlah, status, menit_lalu)
    trf = [
        ("mainframe", "Pembangunan Jembatan Musi VI",      "Proyek Tol Trans-Sumatera Seksi 2", 10, "IN_TRANSIT", 200),
        ("bor",       "Proyek Tol Trans-Sumatera Seksi 2", "Bendungan Way Sekampung",            2, "SELESAI",    900),
    ]
    for key, asal, tujuan, jml, status, back in trf:
        idt = new_id()
        P("transfer", idt, {
            "id_transfer": s(idt), "sku_id": s(sku[key]),
            "proyek_asal": s(asal), "proyek_tujuan": s(tujuan), "jumlah": i(jml),
            "status_transfer": s(status),
            "local_timestamp": i(NOW_MS - back * 60_000), "sync_status": s("SYNCED"),
            "server_timestamp": ts(NOW_TS), "updated_by": s(uid)})

# ----------------------------------------------------------------------------
def main():
    token, uid = sign_in()
    hdr = {"Authorization": f"Bearer {token}"}
    base = f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents"
    print(f"Login OK (uid={uid}) | project={PROJECT}")

    print("== Menghapus dokumen lama ==")
    for c in COLLECTIONS:
        delete_all(base, c, hdr)

    print("== Menulis dummy bersih ==")
    seed(base, uid, hdr)

    print("== Verifikasi ==")
    for c in COLLECTIONS:
        res = _req("GET", f"{base}/{c}?pageSize=300", headers=hdr)
        print(f"  {c}: {len(res.get('documents', []))} dokumen")
    print("Selesai. server_timestamp katalog =", NOW_TS)

if __name__ == "__main__":
    main()
