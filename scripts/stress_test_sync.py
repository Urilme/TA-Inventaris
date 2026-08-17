#!/usr/bin/env python3
"""
Stress test sinkronisasi — Skenario Concurrency No. 5 (CT-05).

Mensimulasikan N perangkat (default 10) yang melakukan penulisan mutasi ke
Firestore SECARA SERENTAK, lalu menghitung Transaction Success Rate (TSR) dan
memverifikasi tidak ada lost-write (jumlah dokumen yang benar-benar tersimpan
== jumlah yang dikirim). Tidak butuh 10 perangkat fisik maupun emulator.

Selaras dengan konvensi scripts/reset_firestore_dummy.py:
  - Firestore REST API (bukan firebase-admin, jadi TIDAK perlu serviceAccountKey.json).
  - project_id & API key dibaca dari google-services.json (client key, sudah publik).
  - Autentikasi via email/password (argumen atau environment variable).

Serentak sungguhan: seluruh thread menunggu di threading.Barrier lalu menembak
PATCH pada saat yang sama (bukan sekadar map berurutan).

Pakai:
  python scripts/stress_test_sync.py <email> <password>            # default 10 penulis
  python scripts/stress_test_sync.py <email> <password> 25         # 25 penulis
  python scripts/stress_test_sync.py <email> <password> 10 --cleanup   # hapus dok uji setelah selesai
  # atau lewat environment:
  FIREBASE_EMAIL=... FIREBASE_PASSWORD=... python scripts/stress_test_sync.py

Dokumen uji ditandai id_proyek = "PRJ-STRESS-CT05" dan sku_id = "SKU-UJISTRES"
agar mudah ditemukan/dihapus dan TIDAK mengganggu data dummy proyek asli.
Secara default dokumen DIBIARKAN tersimpan supaya bisa di-screenshot di Firestore
Console sebagai bukti; jalankan ulang dengan --cleanup untuk menghapusnya.
"""
import json, os, sys, time, uuid, threading, urllib.request, urllib.error
from concurrent.futures import ThreadPoolExecutor

# ----------------------------------------------------------------------------
# Konfigurasi & kredensial (identik pola dengan reset_firestore_dummy.py)
# ----------------------------------------------------------------------------
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
with open(os.path.join(ROOT, "google-services.json"), encoding="utf-8") as f:
    gs = json.load(f)
PROJECT = gs["project_info"]["project_id"]
API_KEY = gs["client"][0]["api_key"][0]["current_key"]

# Argumen posisional: <email> <password> [N]; flag: --cleanup
args = [a for a in sys.argv[1:] if not a.startswith("--")]
FLAGS = {a for a in sys.argv[1:] if a.startswith("--")}
EMAIL = args[0] if len(args) > 0 else os.environ.get("FIREBASE_EMAIL")
PASSWORD = args[1] if len(args) > 1 else os.environ.get("FIREBASE_PASSWORD")
N = int(args[2]) if len(args) > 2 else 10
CLEANUP = "--cleanup" in FLAGS
if not EMAIL or not PASSWORD:
    sys.exit("Kredensial kosong. Beri lewat argumen <email> <password> atau env FIREBASE_EMAIL/FIREBASE_PASSWORD.")

STRESS_PROYEK = "PRJ-STRESS-CT05"
STRESS_SKU = "SKU-UJISTRES"
NOW_MS = int(time.time() * 1000)

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

# Firestore value encoders
def s(v):  return {"stringValue": v}
def i(v):  return {"integerValue": str(v)}
def nul(): return {"nullValue": None}

# ----------------------------------------------------------------------------
# Beban kerja tiap "perangkat"
# ----------------------------------------------------------------------------
def kirim_mutasi(idx, uid, base, hdr, barrier):
    """Satu perangkat menulis satu mutasi CHECK_OUT jumlah 1 ke Firestore."""
    doc_id = str(uuid.uuid4())
    fields = {
        "id_transaksi": s(doc_id), "sku_id": s(STRESS_SKU),
        "id_mandor": s(uid), "id_proyek": s(STRESS_PROYEK),
        "jenis_mutasi": s("CHECK_OUT"), "jumlah": i(1),
        "kondisi_barang": nul(),
        "lokasi_penggunaan": s(f"Stress-{idx:03d}"),
        "penanggungjawab": s(f"Device-{idx:03d}"),
        "catatan": s("CT-05 stress test"),
        "local_timestamp": i(NOW_MS + idx), "sync_status": s("SYNCED"),
        "updated_by": s(uid),
    }
    url = f"{base}/mutasi/{doc_id}"
    barrier.wait()                      # sinkronisasi: semua thread menembak bersamaan
    t0 = time.perf_counter()
    try:
        _req("PATCH", url, {"fields": fields}, hdr)
        return (True, doc_id, (time.perf_counter() - t0) * 1000, None)
    except urllib.error.HTTPError as e:
        return (False, doc_id, (time.perf_counter() - t0) * 1000, f"HTTP {e.code}")
    except Exception as e:              # noqa: BLE001 — laporkan apa pun sebagai gagal
        return (False, doc_id, (time.perf_counter() - t0) * 1000, str(e))

def hitung_dokumen_stress(base, hdr):
    """Hitung dokumen mutasi bertanda proyek stress (untuk cek lost-write)."""
    total, ids, page_token = 0, [], None
    while True:
        url = f"{base}/mutasi?pageSize=300"
        if page_token:
            url += f"&pageToken={page_token}"
        res = _req("GET", url, headers=hdr)
        for doc in res.get("documents", []):
            fld = doc.get("fields", {})
            if fld.get("id_proyek", {}).get("stringValue") == STRESS_PROYEK:
                total += 1
                ids.append(doc["name"])
        page_token = res.get("nextPageToken")
        if not page_token:
            break
    return total, ids

# ----------------------------------------------------------------------------
def main():
    token, uid = sign_in()
    hdr = {"Authorization": f"Bearer {token}"}
    base = f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents"
    print(f"Login OK (uid={uid}) | project={PROJECT}")
    print(f"== CT-05: {N} penulisan mutasi SERENTAK ke koleksi 'mutasi' ==")

    barrier = threading.Barrier(N)
    t_start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=N) as ex:
        hasil = list(ex.map(lambda k: kirim_mutasi(k, uid, base, hdr, barrier), range(N)))
    durasi = (time.perf_counter() - t_start) * 1000

    sukses = [h for h in hasil if h[0]]
    gagal = [h for h in hasil if not h[0]]
    lat = sorted(h[2] for h in hasil)
    tsr = len(sukses) / N * 100

    print(f"\n  Berhasil dikirim : {len(sukses)}/{N}")
    print(f"  Gagal            : {len(gagal)}")
    if gagal:
        for _, did, _, err in gagal[:10]:
            print(f"    - {did[:8]}… : {err}")
    print(f"  Total durasi     : {durasi:.0f} ms (wall-clock, serentak)")
    print(f"  Latensi/req      : min {lat[0]:.0f} | median {lat[len(lat)//2]:.0f} | max {lat[-1]:.0f} ms")
    print(f"  TSR (kirim)      : {tsr:.1f}%")

    # Verifikasi lost-write: hitung dokumen yang benar-benar tersimpan
    print("\n== Verifikasi tidak ada lost-write (baca ulang dari Firestore) ==")
    tersimpan, ids = hitung_dokumen_stress(base, hdr)
    print(f"  Dokumen stress tersimpan di server: {tersimpan}")
    if tersimpan == len(sukses):
        print("  OK — jumlah tersimpan == jumlah sukses kirim (Zero Lost-Write).")
    else:
        print(f"  ! SELISIH: {len(sukses) - tersimpan} dokumen tidak konsisten — periksa manual.")

    if CLEANUP:
        print("\n== Cleanup (--cleanup) — menghapus dokumen uji ==")
        hapus = 0
        for name in ids:
            try:
                _req("DELETE", f"https://firestore.googleapis.com/v1/{name}", headers=hdr)
                hapus += 1
            except urllib.error.HTTPError as e:
                print(f"  ! gagal hapus {name.split('/documents/')[-1]}: {e.code}")
        print(f"  Terhapus: {hapus} dokumen.")
    else:
        print(f"\nDokumen uji DIBIARKAN (id_proyek={STRESS_PROYEK}) untuk bukti Firestore Console.")
        print("Jalankan ulang dengan --cleanup untuk menghapusnya.")

if __name__ == "__main__":
    main()
